package fr.visionmaxread;

import android.content.Context;
import android.net.Uri;
import android.os.StatFs;
import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;

/** Pinned OFFLINE opt-in GGUF models; import with Android SAF, no networking. */
final class OfflineCorrectionModels {
    static final int OCR_ONLY=0, LIQUID_1200=1, QWEN_1700=2;
    static final class Spec {
        final int id;
        final String name,filename,sha256,repo,license,quant;
        final long lowerBytes,upperBytes;
        Spec(int id,String name,String file,String sha,long low,long high,
             String repo,String license,String quant){
            this.id=id;this.name=name;filename=file;sha256=sha;lowerBytes=low;upperBytes=high;
            this.repo=repo;this.license=license;this.quant=quant;
        }
    }
    // Hugging Face Xet SHA256 from each named GGUF page, verified 2026-10-30.
    private static final Spec LIQUID=new Spec(LIQUID_1200,"Liquid LFM2.5-1.2B",
        "LFM2.5-1.2B-Instruct-QAD-Q4_0.gguf",
        "bb741ebb106d543e9de114b843a3d3d73d51c74b5801e69da2abde821a0cb3e1",
        650_000_000L,760_000_000L,"LiquidAI/LFM2.5-1.2B-Instruct-GGUF",
        "LFM Open 1.0","QAD-Q4_0");
    private static final Spec QWEN=new Spec(QWEN_1700,"Qwen3-1.7B textuel",
        "Qwen3-1.7B-Q4_K_M.gguf",
        "d2387ca2dbfee2ffabce7120d3770dadca0b293052bc2f0e138fdc940d9bc7b5",
        1_180_000_000L,1_380_000_000L,"ggml-org/Qwen3-1.7B-GGUF",
        "Apache-2.0","Q4_K_M");
    private OfflineCorrectionModels(){}
    static Spec spec(int id){if(id==LIQUID_1200)return LIQUID;if(id==QWEN_1700)return QWEN;
        throw new IllegalArgumentException("OCR-only has no GGUF model");}
    static String label(int id){return id==OCR_ONLY?"Aucun correcteur — OCR uniquement":spec(id).name;}
    private static File folder(Context c)throws IOException{
        File d=new File(c.getNoBackupFilesDir(),"text-corrector");
        if(!d.isDirectory()&&!d.mkdirs())throw new IOException("Local model directory unavailable");
        return d;
    }
    static boolean installed(Context c,int id){
        if(id==OCR_ONLY)return true;
        try{
            Spec s=spec(id);File f=new File(folder(c),s.filename);
            return f.isFile()&&f.length()>=s.lowerBytes&&f.length()<=s.upperBytes;
        }catch(IOException e){return false;}
    }
    /** Rechecks content SHA256 before native execution, including after prior installs. */
    static File verified(Context c,int id)throws Exception{
        Spec s=spec(id);File file=new File(folder(c),s.filename);
        if(!file.isFile()||file.length()<s.lowerBytes||file.length()>s.upperBytes)
            throw new IOException("Missing/off-size "+s.name+" GGUF, import the pinned file");
        if(!s.sha256.equals(checksum(file)))
            throw new IOException("GGUF integrity failure for "+s.name+" (reimport required)");
        return file;
    }
    static void importFromDocument(Context c,int id,Uri uri)throws Exception{
        Spec s=spec(id);File d=folder(c),target=new File(d,s.filename);
        File partial=new File(d,s.filename+".part");
        partial.delete();
        long free=new StatFs(d.getAbsolutePath()).getAvailableBytes();
        if(free<s.upperBytes+100_000_000L)
            throw new IOException("Insufficient free local storage for the GGUF import");
        long bytes=0;
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=c.getContentResolver().openInputStream(uri);
            FileOutputStream out=new FileOutputStream(partial)){
            if(in==null)throw new IOException("Unable to read selected file");
            byte[] buf=new byte[1<<20];int n;
            while((n=in.read(buf))>=0){
                if(n==0)continue;
                bytes+=n;
                if(bytes>s.upperBytes)throw new IOException("Incorrect file size");
                digest.update(buf,0,n);out.write(buf,0,n);
            }
            out.getFD().sync();
            if(bytes<s.lowerBytes||!s.sha256.equals(hex(digest.digest())))
                throw new IOException("Model SHA256 does not match pinned GGUF; nothing installed");
        }catch(Exception e){partial.delete();throw e;}
        if(target.exists()&&!target.delete()){partial.delete();throw new IOException("Cannot replace old model");}
        if(!partial.renameTo(target)){partial.delete();throw new IOException("Could not complete model import");}
    }
    private static String checksum(File f)throws Exception{
        MessageDigest d=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(f)){
            byte[] b=new byte[1<<20];int n;
            while((n=in.read(b))!=-1)d.update(b,0,n);
        }
        return hex(d.digest());
    }
    private static String hex(byte[] bs){
        StringBuilder x=new StringBuilder();
        for(byte b:bs)x.append(String.format(Locale.ROOT,"%02x",b&255));
        return x.toString();
    }
}
