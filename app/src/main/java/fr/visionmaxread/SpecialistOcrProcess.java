package fr.visionmaxread;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
/** Local CPU-only image OCR using the pinned, NDK-built llama-mtmd-cli. */
final class SpecialistOcrProcess {
    static final class Result {final String text;final long elapsedMs;Result(String t,long ms){text=t;elapsedMs=ms;}}
    private Process running;
    synchronized void stop(){if(running!=null){running.destroyForcibly();running=null;}}
    private File executable(Context c){return new File(c.getApplicationInfo().nativeLibraryDir,"libvmr_mtmd_cli.so");}
    boolean available(Context c){File f=executable(c);return f.isFile()&&f.canExecute();}
    Result recognize(Context c,SpecialistOcrModels.Spec s,Bitmap bitmap)throws Exception {
        if(bitmap==null||bitmap.isRecycled())throw new IOException("Image indisponible");
        if(!available(c))throw new IOException("Runtime multimodal ARM64 absent");
        if(!SpecialistOcrModels.verified(c,s,SpecialistOcrModels.Part.MODEL)||
           !SpecialistOcrModels.verified(c,s,SpecialistOcrModels.Part.PROJECTOR))
            throw new SecurityException("Vérification SHA-256 des modèles échouée");
        File dir=new File(c.getCacheDir(),"ocr-native");
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cache indisponible");
        File image=new File(dir,"document.jpg"),stdout=new File(dir,"stdout.txt"),stderr=new File(dir,"stderr.txt");
        long start=SystemClock.elapsedRealtime();
        Process process=null;
        try {
            try(OutputStream o=new FileOutputStream(image)){
                if(!bitmap.compress(Bitmap.CompressFormat.JPEG,92,o))throw new IOException("JPEG impossible");
            }
            List<String> args=Arrays.asList(executable(c).getAbsolutePath(),"-m",
                SpecialistOcrModels.path(c,s,SpecialistOcrModels.Part.MODEL).getAbsolutePath(),
                "--mmproj",SpecialistOcrModels.path(c,s,SpecialistOcrModels.Part.PROJECTOR).getAbsolutePath(),
                "--image",image.getAbsolutePath(),"-p",s.prompt,
                "-n","1536","-c","8192","-t","4","-ngl","0",
                "--no-mmproj-offload","--simple-io");
            ProcessBuilder pb=new ProcessBuilder(args).redirectOutput(stdout).redirectError(stderr);
            pb.environment().put("LD_LIBRARY_PATH",c.getApplicationInfo().nativeLibraryDir);
            process=pb.start();synchronized(this){running=process;}
            if(!process.waitFor(8,TimeUnit.MINUTES)){process.destroyForcibly();throw new IOException("OCR interrompu après 8 minutes");}
            if(process.exitValue()!=0)throw new IOException("Runtime OCR : erreur native "+process.exitValue());
            if(stdout.length()==0||stdout.length()>500000)throw new IOException("Sortie OCR vide ou trop longue");
            byte[] bytes=new byte[(int)stdout.length()];
            try(InputStream in=new FileInputStream(stdout)){
                int done=0,n;while(done<bytes.length&&(n=in.read(bytes,done,bytes.length-done))>0)done+=n;
                if(done!=bytes.length)throw new EOFException("Résultat incomplet");
            }
            String text=new String(bytes,StandardCharsets.UTF_8).trim();
            if(text.isEmpty())throw new IOException("Aucun texte OCR");
            return new Result(text,SystemClock.elapsedRealtime()-start);
        }finally{
            if(process!=null&&process.isAlive())process.destroyForcibly();
            synchronized(this){if(running==process)running=null;}
            image.delete();stdout.delete();stderr.delete();
        }
    }
}
