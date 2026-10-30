use hobby_rs::{Engine, GenOpts};
use jni::objects::{JClass,JString,JObject,JValue};
use jni::sys::{jbyteArray,jint,jlong};
use jni::JNIEnv;
use std::path::Path;
use std::sync::atomic::{AtomicBool,Ordering};
use std::sync::{Mutex,OnceLock};
use std::time::Instant;
use std::ptr;

static STOP:AtomicBool=AtomicBool::new(false);

fn inference(env:&mut JNIEnv,model_path:JString,text:JString,limit:jint,listener:&JObject)
  ->Result<Vec<u8>,String>{
    let path:String=env.get_string(&model_path).map_err(|e|e.to_string())?.into();
    let prompt:String=env.get_string(&text).map_err(|e|e.to_string())?.into();
    if path.is_empty()||prompt.is_empty()||prompt.len()>3500||limit<1||limit>128{
        return Err("Invalid MoE path/prompt/token count".to_string());
    }
    // Avoid running an excessive number of Rust workers on Android.
    let _=rayon::ThreadPoolBuilder::new().num_threads(2).build_global();
    if STOP.load(Ordering::Acquire){return Err("Cancelled".to_string());}
    let engine=Engine::load(Path::new(&path),true).map_err(|e|format!("MoE weights: {e}"))?;
    if engine.cfg.n_experts<2 || engine.cfg.top_k<1 {
        return Err("Not a real routed MoE model".to_string());
    }
    if STOP.load(Ordering::Acquire){return Err("Cancelled".to_string());}
    let opts=GenOpts{
        max_new:limit as usize,
        temp:0.0,top_p:0.95,
        seed:1234,
        rep_penalty:1.25,
    };
    let mut out=String::new();
    let mut token_index=0i32;
    let mut callback_error:Option<String>=None;
    let generated=engine.generate(&prompt,&[],&opts,|delta|{
        if STOP.load(Ordering::Acquire){return false;}
        token_index+=1;
        // Hobby-rs supplies a UTF-8-safe delta, even when a token produces no
        // visible text. The UI receives every token boundary without buffering
        // the model's output until the end. No text goes into Android logcat.
        if out.len()+delta.len()>10000 {
            callback_error=Some("MoE output exceeded safety limit".to_string());
            return false;
        }
        out.push_str(delta);
        match env.new_string(delta){
            Ok(fragment)=>{
                let object=JObject::from(fragment);
                let result=env.call_method(listener,"onToken",
                    "(Ljava/lang/String;I)V",
                    &[JValue::Object(&object),JValue::Int(token_index)]);
                let _=env.delete_local_ref(object);
                if let Err(e)=result {
                    callback_error=Some(format!("MoE token listener failed: {e}"));
                    return false;
                }
            }
            Err(e)=>{
                callback_error=Some(format!("Cannot allocate MoE token: {e}"));
                return false;
            }
        }
        !STOP.load(Ordering::Acquire)
    });
    if let Some(e)=callback_error{return Err(e);}
    if STOP.load(Ordering::Acquire){return Err("Cancelled".to_string());}
    // Count includes invisible token pieces. This is an actual count from
    // Hobby-rs generation; EOS vs length-limit is not distinguishable here.
    let _=generated;
    Ok(out.into_bytes())
}

#[no_mangle]
pub extern "system" fn Java_fr_visionmaxread_HobbyMoeNative_infer(
    mut env:JNIEnv,_class:JClass,model_path:JString,prompt:JString,tokens:jint,listener:JObject
)->jbyteArray{
    let result=std::panic::catch_unwind(std::panic::AssertUnwindSafe(||
        inference(&mut env,model_path,prompt,tokens,&listener)
    ));
    let data=match result {
        Ok(Ok(bytes))=>bytes,
        Ok(Err(msg))=>{let _=env.throw_new("java/lang/IllegalStateException",msg);return ptr::null_mut();}
        Err(_)=>{let _=env.throw_new("java/lang/IllegalStateException","MoE native panic");return ptr::null_mut();}
    };
    match env.byte_array_from_slice(&data){
        Ok(bytes)=>bytes.into_raw(),
        Err(_)=>ptr::null_mut()
    }
}

#[no_mangle]
pub extern "system" fn Java_fr_visionmaxread_HobbyMoeNative_nativeStop(_env:JNIEnv,_class:JClass){
    STOP.store(true,Ordering::Release);
}

#[no_mangle]
pub extern "system" fn Java_fr_visionmaxread_HobbyMoeNative_reset(_env:JNIEnv,_class:JClass){
    STOP.store(false,Ordering::Release);
}


// VMR-016: One owned model reused across short French/English OCR sentences.
// No model or KV cache persists after nativeClose; each generate gets a fresh
// context from hobby-rs. Protect against concurrent JNI requests.
struct Loaded { engine:Engine, path:String, weight_bytes:usize }
static SESSION:OnceLock<Mutex<Option<Loaded>>>=OnceLock::new();
fn slot()->&'static Mutex<Option<Loaded>>{SESSION.get_or_init(||Mutex::new(None))}

#[no_mangle]
pub extern "system" fn Java_fr_visionmaxread_HobbyMoeNative_nativeOpen(
 mut env:JNIEnv,_class:JClass,jpath:JString
)->jlong {
 let result=std::panic::catch_unwind(std::panic::AssertUnwindSafe(||{
  let path:String=env.get_string(&jpath).map_err(|e|e.to_string())?.into();
  if path.is_empty(){return Err("Missing GGUF path".to_string());}
  if STOP.load(Ordering::Acquire){return Err("Cancelled".to_string());}
  let start=Instant::now();
  let mut lock=slot().lock().map_err(|_|"MoE session mutex poisoned".to_string())?;
  if lock.as_ref().map(|x|x.path.as_str())!=Some(path.as_str()){
   // Ensure the old model is released before allocating the new one.
   *lock=None;
   let engine=Engine::load(Path::new(&path),true)
     .map_err(|e|format!("MoE session weights: {e}"))?;
   if engine.cfg.n_experts<2 || engine.cfg.top_k==0{
    return Err("Not a sparse routed MoE".to_string());
   }
   if STOP.load(Ordering::Acquire){return Err("Cancelled".to_string());}
   let weight_bytes=engine.weight_bytes();
   *lock=Some(Loaded{engine,path,weight_bytes});
  }
  Ok(start.elapsed().as_millis() as jlong)
 }));
 match result{
  Ok(Ok(elapsed))=>elapsed,
  Ok(Err(err))=>{let _=env.throw_new("java/lang/IllegalStateException",err);-1},
  Err(_)=>{let _=env.throw_new("java/lang/IllegalStateException","MoE loading panic");-1}
 }
}

#[no_mangle]
pub extern "system" fn Java_fr_visionmaxread_HobbyMoeNative_nativeWeightBytes(
 mut env:JNIEnv,_class:JClass
)->jlong {
 match slot().lock(){
  Ok(lock)=>lock.as_ref().map(|x|x.weight_bytes as jlong).unwrap_or(0),
  Err(_)=>{let _=env.throw_new("java/lang/IllegalStateException","MoE lock poisoned");0}
 }
}

#[no_mangle]
pub extern "system" fn Java_fr_visionmaxread_HobbyMoeNative_nativeGenerateLoaded(
 mut env:JNIEnv,_class:JClass,jprompt:JString,limit:jint,listener:JObject
)->jbyteArray {
 let result=std::panic::catch_unwind(std::panic::AssertUnwindSafe(||{
  let prompt:String=env.get_string(&jprompt).map_err(|e|e.to_string())?.into();
  if prompt.is_empty()||prompt.len()>3500||limit<1||limit>128{
   return Err("Invalid segmented prompt".to_string());
  }
  if STOP.load(Ordering::Acquire){return Err("Cancelled".to_string());}
  let lock=slot().lock().map_err(|_|"MoE model mutex poisoned".to_string())?;
  let loaded=lock.as_ref().ok_or("MoE session not opened".to_string())?;
  let opts=GenOpts{max_new:limit as usize,temp:0.0,top_p:0.95,seed:1234,rep_penalty:1.15};
  let mut out=String::new();let mut count=0i32;
  let mut callback_error:Option<String>=None;
  let actual=loaded.engine.generate(&prompt,&[],&opts,|delta|{
   if STOP.load(Ordering::Acquire){return false;}
   if out.len()+delta.len()>10000{callback_error=Some("MoE output too long".to_string());return false;}
   out.push_str(delta);count+=1;
   match env.new_string(delta){
    Ok(piece)=>{
     let object=JObject::from(piece);
     let ok=env.call_method(&listener,"onToken","(Ljava/lang/String;I)V",
          &[JValue::Object(&object),JValue::Int(count)]);
     let _=env.delete_local_ref(object);
     if let Err(e)=ok{callback_error=Some(format!("MoE UI callback: {e}"));return false;}
    }
    Err(e)=>{callback_error=Some(format!("MoE text memory: {e}"));return false;}
   }
   !STOP.load(Ordering::Acquire)
  });
  if let Some(err)=callback_error{return Err(err);}
  if STOP.load(Ordering::Acquire){return Err("Cancelled".to_string());}
  // Full reason is not provided by upstream. A short output is *likely*
  // EOS on our 260-character prompts, not a confirmed explicit EOS event.
  let _=actual;
  Ok(out.into_bytes())
 }));
 let data=match result{
  Ok(Ok(data))=>data,
  Ok(Err(err))=>{let _=env.throw_new("java/lang/IllegalStateException",err);return ptr::null_mut();},
  Err(_)=>{let _=env.throw_new("java/lang/IllegalStateException","MoE generation panic");return ptr::null_mut();}
 };
 match env.byte_array_from_slice(&data){Ok(x)=>x.into_raw(),Err(_)=>ptr::null_mut()}
}

#[no_mangle]
pub extern "system" fn Java_fr_visionmaxread_HobbyMoeNative_nativeClose(
 _env:JNIEnv,_class:JClass
){
 if let Ok(mut lock)=slot().lock(){*lock=None;}
}
