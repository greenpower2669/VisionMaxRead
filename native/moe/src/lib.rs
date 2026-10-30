use hobby_rs::{Engine, GenOpts};
use jni::objects::{JClass,JString,JObject,JValue};
use jni::sys::{jbyteArray,jint};
use jni::JNIEnv;
use std::path::Path;
use std::sync::atomic::{AtomicBool,Ordering};
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
