#include <jni.h>
#include <android/log.h>
#include <llama.h>
#include <string>
#include <vector>
#include <mutex>
#include <algorithm>
#include <stdexcept>
#include <atomic>

namespace {
std::mutex g_mutex;
llama_model *g_model=nullptr;
const llama_vocab *g_vocab=nullptr;
std::atomic<bool> g_cancel{false};
std::string g_error;
void fail(const char *message){throw std::runtime_error(message);}
void cleanup(){
    if(g_model){llama_model_free(g_model);g_model=nullptr;g_vocab=nullptr;}
}
std::string utf8(JNIEnv *env,jstring input){
    if(!input)return {};
    const char *p=env->GetStringUTFChars(input,nullptr);
    if(!p)fail("JNI UTF8 allocation");
    std::string s(p);env->ReleaseStringUTFChars(input,p);return s;
}
void batch_set(llama_batch_ext *batch,const llama_token *tokens,int count,int position){
    llama_batch_ext_clear(batch);
    for(int i=0;i<count;i++){
        int slot=llama_batch_ext_add_token(batch,0,tokens[i]);
        llama_pos pos=position+i;
        llama_batch_ext_set_pos(batch,slot,&pos);
    }
    llama_batch_ext_set_output_logits(batch,count-1,true);
}
std::string generate(const std::string &prompt,int limit){
    if(!g_model||!g_vocab)fail("Model not loaded");
    if(prompt.empty()||prompt.size()>10000||limit<1||limit>320)fail("Invalid prompt size");
    // The QAD chat template uses the special im_start / im_end role delimiters.
    std::string formatted="<|im_start|>user\n"+prompt+"<|im_end|>\n<|im_start|>assistant\n";
    int count=llama_tokenize(g_vocab,formatted.data(),(int)formatted.size(),nullptr,0,true,true);
    if(count>=0)fail("Unexpected tokenizer sizing API");
    count=-count;
    if(count<1||count>2000)fail("Context limit");
    std::vector<llama_token> tokens(count);
    if(llama_tokenize(g_vocab,formatted.data(),(int)formatted.size(),tokens.data(),count,true,true)<0)fail("Tokenization failed");
    llama_context_params ctx=llama_context_default_params();
    ctx.n_ctx=std::max(512,count+limit+8);
    ctx.n_batch=std::max(512,count);
    ctx.n_ubatch=256;
    ctx.n_threads=2;
    llama_context *context=llama_init_from_model(g_model,ctx);
    if(!context)fail("CPU inference context failed");
    llama_batch_ext *batch=llama_batch_ext_init(context);
    llama_sampler *sampler=llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler,llama_sampler_init_greedy());
    std::string output;
    try{
        batch_set(batch,tokens.data(),count,0);
        int currentCount=count;
        int position=0;
        for(int i=0;i<limit&&!g_cancel.load();i++){
            if(llama_process(context,LLAMA_PROCESS_TYPE_DECODE,batch))fail("Native LFM2 decode error");
            position+=currentCount;
            llama_token token=llama_sampler_sample(sampler,context,-1);
            if(llama_vocab_is_eog(g_vocab,token))break;
            char buf[2048];
            int n=llama_token_to_piece(g_vocab,token,buf,sizeof(buf),0,true);
            if(n<0||n>=(int)sizeof(buf))fail("Token output overflow");
            output.append(buf,n);
            batch_set(batch,&token,1,position);
            currentCount=1;
        }
    }catch(...){
        llama_sampler_free(sampler);llama_batch_ext_free(batch);llama_free(context);throw;
    }
    llama_sampler_free(sampler);llama_batch_ext_free(batch);llama_free(context);
    if(g_cancel.load())fail("Cancelled");
    return output;
}
void throwJava(JNIEnv *env,const char *msg){
    jclass cls=env->FindClass("java/lang/IllegalStateException");
    if(cls)env->ThrowNew(cls,msg);
}
}
extern "C" JNIEXPORT jboolean JNICALL
Java_fr_visionmaxread_LiquidNative_load(JNIEnv *env,jclass,jstring path){
    std::lock_guard<std::mutex> lock(g_mutex);
    try{
        cleanup();
        g_cancel=false;
        std::string file=utf8(env,path);
        if(file.empty())fail("GGUF path missing");
        llama_backend_init();
        llama_model_params p=llama_model_default_params();
        p.n_gpu_layers=0;
        g_model=llama_model_load_from_file(file.c_str(),p);
        if(!g_model)fail("Unable to load GGUF model");
        g_vocab=llama_model_get_vocab(g_model);
        return JNI_TRUE;
    }catch(const std::exception &e){cleanup();throwJava(env,e.what());return JNI_FALSE;}
}
extern "C" JNIEXPORT jstring JNICALL
Java_fr_visionmaxread_LiquidNative_infer(JNIEnv *env,jclass,jstring prompt,jint maxTokens){
    std::lock_guard<std::mutex> lock(g_mutex);
    try{
        g_cancel=false;
        std::string s=generate(utf8(env,prompt),(int)maxTokens);
        return env->NewStringUTF(s.c_str());
    }catch(const std::exception &e){throwJava(env,e.what());return nullptr;}
}
extern "C" JNIEXPORT void JNICALL
Java_fr_visionmaxread_LiquidNative_stop(JNIEnv *,jclass){g_cancel=true;}
extern "C" JNIEXPORT void JNICALL
Java_fr_visionmaxread_LiquidNative_unload(JNIEnv *,jclass){
    std::lock_guard<std::mutex> lock(g_mutex);cleanup();
}
