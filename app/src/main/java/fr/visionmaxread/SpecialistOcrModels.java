package fr.visionmaxread;
import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
/** Pinned offline GGUF image-model/projector pairs; no network permissions. */
final class SpecialistOcrModels {
    enum Engine { SURYA2, LIGHTON3 }
    enum Part { MODEL, PROJECTOR }
    static final class Spec {
        final Engine engine;
        final String title, repo, modelName, modelSha, projectorName, projectorSha, prompt;
        final long maxModel, maxProjector;
        Spec(Engine e,String title,String repo,String name,String sha,long max,String proj,String psha,long pmax,String prompt) {
            this.engine=e;this.title=title;this.repo=repo;this.modelName=name;this.modelSha=sha;this.maxModel=max;
            this.projectorName=proj;this.projectorSha=psha;this.maxProjector=pmax;this.prompt=prompt;
        }
        String url(Part p){return "https://huggingface.co/"+repo+"/resolve/main/"+(p==Part.MODEL?modelName:projectorName);}
    }
    static final Spec SURYA = new Spec(Engine.SURYA2,"Surya OCR 2","datalab-to/surya-ocr-2-gguf",
        "surya-2.gguf","1f18abe17b1ed8b4e47ee9b1ad0e274c93daf5efbb6b29a04ff1712e37051e05",1450000000L,
        "surya-2-mmproj.gguf","98c0563673b1657ff6d021d1e5f04af06cbf61bb40c63ac613e8bb71b42fb2c0",260000000L,
        "OCR this image to HTML. Each block is a div with data-label and data-bbox (x0 y0 x1 y1, normalized 0-1000).");
    static final Spec LIGHTON = new Spec(Engine.LIGHTON3,"LightOnOCR-3 0.8B","staghado/LightOnOCR-3-0.8B-GGUF",
        "model-Q4_K_M.gguf","bb57c9769af18d02f0bde4dd0840737cae170d7340cc0435eddcc36b443459df",680000000L,
        "mmproj-F16.gguf","101acc9466a587a615bbf7b29c0ecb6744e309363eba8b615fe3e09f5e1c5d5f",250000000L,
        " "); // mtmd-cli requires a non-empty prompt for single-turn mode.
    static Spec get(Engine e){return e==Engine.SURYA2?SURYA:LIGHTON;}
    private static File directory(Context c,Spec s){return new File(new File(c.getFilesDir(),"ocr-specialists"),s.engine.name());}
    static File path(Context c,Spec s,Part p){return new File(directory(c,s),p==Part.MODEL?"model.gguf":"projector.gguf");}
    static boolean present(Context c,Spec s,Part p){
        File f=path(c,s,p);return f.isFile()&&f.length()>1000000L&&f.length()<= (p==Part.MODEL?s.maxModel:s.maxProjector);
    }
    static boolean ready(Context c,Spec s){return present(c,s,Part.MODEL)&&present(c,s,Part.PROJECTOR);}
    private static String hex(byte[] d){StringBuilder b=new StringBuilder();for(byte c:d)b.append(String.format(Locale.ROOT,"%02x",c&255));return b.toString();}
    static String sha256(File f)throws Exception {
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new BufferedInputStream(new FileInputStream(f))){
            byte[] buf=new byte[1024*1024];int n;while((n=in.read(buf))!=-1)md.update(buf,0,n);
        }
        return hex(md.digest());
    }
    static boolean verified(Context c,Spec s,Part p)throws Exception{
        return present(c,s,p)&&sha256(path(c,s,p)).equals(p==Part.MODEL?s.modelSha:s.projectorSha);
    }
    static void importFile(Context c,Spec s,Part p,Uri uri)throws Exception {
        File d=directory(c,s);if(!d.isDirectory()&&!d.mkdirs())throw new IOException("Stockage indisponible");
        File temp=new File(d,p==Part.MODEL?"model.partial":"projector.partial");
        File dest=path(c,s,p);MessageDigest md=MessageDigest.getInstance("SHA-256");
        long limit=p==Part.MODEL?s.maxModel:s.maxProjector;
        try {
            try(InputStream in=c.getContentResolver().openInputStream(uri);
                OutputStream out=new BufferedOutputStream(new FileOutputStream(temp))){
                if(in==null)throw new IOException("Fichier introuvable");
                byte[] buf=new byte[1024*1024],header=new byte[4];int h=0,n;long count=0;
                while((n=in.read(buf))!=-1){
                    count+=n;if(count>limit)throw new IOException("Poids excessifs");
                    if(h<4){int k=Math.min(n,4-h);System.arraycopy(buf,0,header,h,k);h+=k;}
                    md.update(buf,0,n);out.write(buf,0,n);
                }
                if(count<1000000||h!=4||header[0]!='G'||header[1]!='G'||header[2]!='U'||header[3]!='F')
                    throw new IOException("Format GGUF incorrect");
                if(!hex(md.digest()).equals(p==Part.MODEL?s.modelSha:s.projectorSha))
                    throw new SecurityException("SHA-256 différent de la source officielle");
            }
            if(!temp.renameTo(dest))throw new IOException("Copie atomique impossible");
        }finally{temp.delete();}
    }
    private SpecialistOcrModels(){}
}
