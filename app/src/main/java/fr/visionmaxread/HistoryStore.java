package fr.visionmaxread;
import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** The API accepts only a MaskedText capability produced after successful NER. */
final class HistoryStore {
    static final class MaskedText {
        final String value;
        private MaskedText(String value){this.value=value;}
        static MaskedText analyze(String original,ModelFiles files,java.util.function.BooleanSupplier cancelled) throws Exception {
            return new MaskedText(LocalAnonymizer.mask(original,files,cancelled));
        }
    }
    private final AtomicFile file;
    HistoryStore(Context context){file=new AtomicFile(new File(context.getNoBackupFilesDir(),"masked-history.json"));}
    synchronized JSONArray read() throws IOException,JSONException {
        if(!file.getBaseFile().exists())return new JSONArray();
        return new JSONArray(new String(file.readFully(),StandardCharsets.UTF_8));
    }
    synchronized void add(MaskedText text) throws Exception {
        if(text.value.trim().isEmpty())return;
        JSONArray old=read(),next=new JSONArray();
        next.put(new JSONObject().put("time",System.currentTimeMillis()).put("text",text.value));
        for(int i=0;i<Math.min(old.length(),99);i++)next.put(old.getJSONObject(i));
        FileOutputStream stream=file.startWrite();
        try{stream.write(next.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(stream);}
        catch(Exception e){file.failWrite(stream);throw e;}
    }
    synchronized void clear(){file.delete();}
}
