package fr.visionmaxread;

import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Real routed sparse MoE on CPU via a separate Rust JNI engine, not Liquid. */
final class HobbyMoeNative {
    private static final String FILE="hobbylm-1b-sft-3450-Q4_K_M.gguf";
    private static volatile boolean enabled=true;
    static{try{System.loadLibrary("vmr_moe");}catch(UnsatisfiedLinkError e){enabled=false;}}
    private HobbyMoeNative(){}
    interface TokenListener {
        /** Invoked synchronously by Rust on the background inference thread. */
        void onToken(String delta,int tokenCount);
    }
    private static native byte[] infer(String model,String prompt,int maxTokens,TokenListener listener);
    private static final java.util.concurrent.atomic.AtomicBoolean cancelling=
        new java.util.concurrent.atomic.AtomicBoolean(false);
    private static native void reset();
    private static native void nativeStop();
    static void stop(){
        cancelling.set(true);
        if(enabled)nativeStop();
    }
    static boolean available(){return enabled;}
    static String generate(Context context,String prompt,int maxTokens,TokenListener listener)throws Exception{
        if(!enabled)throw new IllegalStateException("Rust MoE engine unavailable");
        if(prompt.isEmpty()||prompt.length()>3500||maxTokens<1||maxTokens>128||listener==null)
            throw new IllegalArgumentException("MoE input invalid");
        cancelling.set(false);
        reset();
        File model=verifiedModel(context);
        if(cancelling.get())throw new IllegalStateException("Cancelled");
        byte[] tokens=infer(model.getAbsolutePath(),prompt,maxTokens,listener);
        if(tokens==null)throw new IllegalStateException("MoE produced no output");
        return new String(tokens,StandardCharsets.UTF_8).trim();
    }
    private static File verifiedModel(Context context)throws Exception{
        JSONObject info;
        try(InputStream in=context.getAssets().open("moe/manifest.json")){
            ByteArrayOutputStream b=new ByteArrayOutputStream();
            byte[] chunk=new byte[4096];int n;
            while((n=in.read(chunk))>=0){if(b.size()>16384)throw new IOException("MoE manifest too long");b.write(chunk,0,n);}
            info=new JSONObject(b.toString("UTF-8"));
        }
        long expectedSize=info.getLong("size");
        String sha=info.getString("sha256");
        File dir=new File(context.getNoBackupFilesDir(),"moe");
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("MoE storage unavailable");
        File model=new File(dir,FILE);
        // Validate any previous copy before executing source, never blindly reuse a damaged GGUF.
        if(!model.isFile()||model.length()!=expectedSize||!checksum(model).equals(sha)){
            File tmp=new File(dir,FILE+".part");
            tmp.delete();
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(InputStream in=context.getAssets().open("moe/"+FILE);
                FileOutputStream out=new FileOutputStream(tmp)){
                byte[] buf=new byte[1<<16];int n;long total=0;
                while((n=in.read(buf))!=-1){
                    total+=n;if(total>expectedSize)throw new IOException("Oversize MoE asset");
                    digest.update(buf,0,n);out.write(buf,0,n);
                }
                out.getFD().sync();
            }catch(Exception e){tmp.delete();throw e;}
            if(tmp.length()!=expectedSize||!hex(digest.digest()).equals(sha)){
                tmp.delete();throw new IOException("MoE GGUF hash mismatch");
            }
            if(!tmp.renameTo(model)){tmp.delete();throw new IOException("MoE install failed");}
        }
        return model;
    }
    private static String checksum(File file)throws Exception{
        MessageDigest d=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(file)){
            byte[] b=new byte[1<<16];int n;while((n=in.read(b))!=-1)d.update(b,0,n);
        }
        return hex(d.digest());
    }
    private static String hex(byte[] values){
        StringBuilder s=new StringBuilder(values.length*2);
        for(byte v:values)s.append(String.format(Locale.ROOT,"%02x",v&255));
        return s.toString();
    }
}
