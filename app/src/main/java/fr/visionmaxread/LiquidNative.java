package fr.visionmaxread;
import android.content.Context;
import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;
import org.json.JSONObject;

/** One real native llama.cpp/LFM2 session at a time, offline and strictly bounded. */
final class LiquidNative implements AutoCloseable {
    private static final String ASSET="liquid/LFM2.5-230M-QAD-Q4_0.gguf";
    private static volatile boolean enabled=true;
    private boolean loaded;
    static {
        try{System.loadLibrary("vmr_liquid");}
        catch(UnsatisfiedLinkError e){enabled=false;}
    }
    private static native boolean load(String path);
    private static native byte[] infer(String prompt,int maxTokens);
    static native void stop();
    private static native void unload();
    static boolean available(){return enabled;}
    LiquidNative(Context context)throws Exception{
        if(!enabled)throw new IllegalStateException("llama.cpp JNI unavailable");
        JSONObject manifest;
        try(InputStream in=context.getAssets().open("liquid/manifest.json")){
            manifest=new JSONObject(readFully(in));
        }
        String sha=manifest.getString("sha256");
        long size=manifest.getLong("size");
        File folder=new File(context.getNoBackupFilesDir(),"liquid");
        if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Storage unavailable");
        File destination=new File(folder,"LFM2.5-230M-QAD-Q4_0.gguf");
        if(!destination.isFile()||destination.length()!=size||!sha256(destination).equals(sha)){
            File part=new File(folder,destination.getName()+".part");
            part.delete();MessageDigest d=MessageDigest.getInstance("SHA-256");
            try(InputStream in=context.getAssets().open(ASSET);
                FileOutputStream out=new FileOutputStream(part)){
                byte[] buf=new byte[65536];int n;long count=0;
                while((n=in.read(buf))!=-1){
                    count+=n;if(count>size)throw new IOException("Oversized GGUF");
                    d.update(buf,0,n);out.write(buf,0,n);
                }
                out.getFD().sync();
            }catch(Exception e){part.delete();throw e;}
            if(part.length()!=size||!hex(d.digest()).equals(sha)){part.delete();throw new IOException("GGUF integrity mismatch");}
            if(!part.renameTo(destination)){part.delete();throw new IOException("GGUF install failed");}
        }
        loaded=load(destination.getAbsolutePath());
        if(!loaded)throw new IllegalStateException("LFM model load failed");
    }
    /** VMR-017: reuse the same pinned llama.cpp runtime for an authenticated Q4 GGUF.
     * No embedded-model path, download or second simultaneous native session. */
    LiquidNative(File authenticatedGguf)throws Exception{
        if(!enabled)throw new IllegalStateException("llama.cpp JNI unavailable");
        if(authenticatedGguf==null||!authenticatedGguf.isFile())
            throw new FileNotFoundException("GGUF not imported");
        loaded=load(authenticatedGguf.getAbsolutePath());
        if(!loaded)throw new IllegalStateException("Text GGUF load failed");
    }
    String generate(String prompt,int maxTokens){
        if(!loaded)throw new IllegalStateException("Model closed");
        if(maxTokens<1||maxTokens>320||prompt.length()>9000)throw new IllegalArgumentException("Prompt oversized");
        byte[] result=infer(prompt,maxTokens);
        if(result==null)throw new IllegalStateException("Empty inference");
        return new String(result,java.nio.charset.StandardCharsets.UTF_8).trim();
    }
    @Override public void close(){
        if(loaded){loaded=false;unload();}
    }
    private static String readFully(InputStream in)throws IOException{
        ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] a=new byte[8192];int n;
        while((n=in.read(a))!=-1){if(b.size()>8192)throw new IOException("Invalid manifest");b.write(a,0,n);}
        return b.toString("UTF-8");
    }
    private static String sha256(File file)throws Exception{
        MessageDigest d=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(file)){byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1)d.update(buf,0,n);}
        return hex(d.digest());
    }
    private static String hex(byte[] digest){
        StringBuilder b=new StringBuilder();for(byte x:digest)b.append(String.format(Locale.ROOT,"%02x",x&255));return b.toString();
    }
}
