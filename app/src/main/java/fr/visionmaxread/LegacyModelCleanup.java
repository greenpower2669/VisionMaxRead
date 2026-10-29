package fr.visionmaxread;
import android.content.Context;
import java.io.File;

/** Deletes only the obsolete OCR model installed by pre-VMR010 LAB versions. */
final class LegacyModelCleanup {
    static void run(Context context){
        File folder=new File(context.getFilesDir(),"qwen");
        int removed=0;long bytes=0;
        for(String name:new String[]{"Qwen3.5-0.8B-VL_int8.litertlm","Qwen3.5-0.8B-VL_int8.litertlm.part"}){
            File file=new File(folder,name);
            if(file.isFile()){
                long size=file.length();
                if(file.delete()){removed++;bytes+=size;}
            }
        }
        if(folder.isDirectory()){
            File[] left=folder.listFiles();
            if(left!=null&&left.length==0)folder.delete();
        }
        DebugLog.append(context,"LEGACY_MODEL_CLEANUP removed="+removed+" bytes="+bytes);
    }
}
