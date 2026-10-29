use hobby_rs::{Engine, GenOpts};
use jni::objects::{JClass,JString};
use jni::sys::{jbyteArray,jint};
use jni::JNIEnv;
use std::path::Path;
use std::sync::atomic::{AtomicBool,Ordering};
use std::ptr;

static STOP:AtomicBool=AtomicBool::new(false);

fn inference(env:&mut JNIEnv,model_path:JString,text:JString,limit:jint)
  ->Result<Vec<u8>,String>{
    let path:String=env.get_string(&model_path).map_err(|e|e.to_string())?.into();
    let prompt:String=env.get_string(&text).map_err(|e|e.to_string())?.into();
    if path.is_empty()||prompt.is_empty()||prompt.len()>3500||limit<1||limit>128{
        return Err("Invalid MoE path/prompt/token count".to_string());
    }
    // Avoid running an excessive number of Rust workers on Android.
    let _=rayon::ThreadPoolBuilder::new().num_threads(2).build_global();
    STOP.store(false,Ordering::Release);
    let engine=Engine::load(Path::new(&path),true).map_err(|e|format!("MoE weights: {e}"))?;
    if engine.cfg.n_experts<2 || engine.cfg.top_k<1 {
        return Err("Not a real routed MoE model".to_string());
    }
    let opts=GenOpts{
        max_new:limit as usize,
        temp:0.0,top_p:0.95,
        seed:1234,
        rep_penalty:1.25,
    };
    let mut out=String::new();
    engine.generate(&prompt,&[],&opts,|delta|{
        out.push_str(delta);
        !STOP.load(Ordering::Acquire)
    });
    if STOP.load(Ordering::Acquire){return Err("Cancelled".to_string());}
    if out.len()>10000 {return Err("MoE output exceeded safety limit".to_string());}
    Ok(out.into_bytes())
}

#[no_mangle]
pub extern "system" fn Java_fr_visionmaxread_HobbyMoeNative_infer(
    mut env:JNIEnv,_class:JClass,model_path:JString,prompt:JString,tokens:jint
)->jbyteArray{
    let result=std::panic::catch_unwind(std::panic::AssertUnwindSafe(||
        inference(&mut env,model_path,prompt,tokens)
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
pub extern "system" fn Java_fr_visionmaxread_HobbyMoeNative_stop(_env:JNIEnv,_class:JClass){
    STOP.store(true,Ordering::Release);
}
