package fr.visionmaxread;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
/** Only model assets are copied. User text and images are never passed to this store. */
final class ModelFiles {
    private final Context context;
    private final Map<String,JSONObject> entries=new HashMap<>();
    ModelFiles(Context context) throws Exception {
        this.context=context.getApplicationContext();
        JSONArray list=new JSONObject(readAsset("models/manifest.json")).getJSONArray("files");
        for(int i=0;i<list.length();i++){JSONObject e=list.getJSONObject(i);entries.put(e.getString("path"),e);}
    }
    String readAsset(String path) throws IOException {
        try(InputStream in=context.getAssets().open(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
    synchronized File file(String path) throws Exception {
        JSONObject entry=entries.get(path);if(entry==null)throw new IOException("Modèle absent du manifeste");
        File file=new File(context.getNoBackupFilesDir(),"models/"+path);
        if(file.isFile()&&file.length()==entry.getLong("size"))return file;
        File parent=file.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Espace modèles indisponible");
        File temp=new File(file.getPath()+".part");MessageDigest hash=MessageDigest.getInstance("SHA-256");
        boolean packed=entry.optString("packaging").equals("xz-shuffle4");
        try(InputStream in=ModelArchive.open(context.getAssets().open("models/"+path+(packed?".xz":"")),packed);FileOutputStream out=new FileOutputStream(temp)){
            byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);hash.update(buf,0,n);}out.getFD().sync();
        }catch(Exception e){temp.delete();throw e;}
        StringBuilder hex=new StringBuilder();for(byte b:hash.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));
        if(temp.length()!=entry.getLong("size")||!hex.toString().equals(entry.getString("sha256"))){temp.delete();throw new IOException("Modèle incomplet");}
        if(!temp.renameTo(file)){temp.delete();throw new IOException("Copie du modèle impossible");}return file;
    }
}
