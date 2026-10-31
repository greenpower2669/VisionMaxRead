package fr.visionmaxread;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.SystemClock;
import java.io.*;
import java.net.*;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Automatic, consent-gated first-use installation of two public GGUF image OCR models.
 * Only pinned model URLs are contacted; document pixels and recognized text stay local.
 * Files are streamed to private storage, resumable and authenticated by SHA-256.
 */
final class SpecialistOcrAutoInstaller {
    interface Progress {
        void update(String engine, String component, long receivedBytes, long maximumBytes);
    }
    private static final long RESERVE_BYTES = 300L * 1024L * 1024L;
    private static final int BUFFER = 1024 * 1024;
    private SpecialistOcrAutoInstaller(){}

    static void requireUnmeteredNetwork(Context context) throws IOException {
        selectedUnmeteredNetwork(context);
    }

    // Bind each HTTPS socket to the checked Wi-Fi/Ethernet network. Otherwise
    // Android could transparently use mobile data after the default route changes.
    private static Network selectedUnmeteredNetwork(Context context) throws IOException {
        ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm==null)throw new IOException("Gestionnaire réseau indisponible");
        Network n=cm.getActiveNetwork();
        NetworkCapabilities capabilities=n==null?null:cm.getNetworkCapabilities(n);
        if(capabilities==null||!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                ||!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                ||!(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                     ||capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))
            throw new IOException("Wi-Fi non facturé ou réseau non limité requis pour installer les poids");
        return n;
    }

    static void ensure(Context context,SpecialistOcrModels.Spec spec,Progress progress,
                       BooleanSupplier cancelled) throws Exception {
        synchronized(SpecialistOcrAutoInstaller.class) {
            for(SpecialistOcrModels.Part part:SpecialistOcrModels.Part.values()){
                check(cancelled);
                if(SpecialistOcrModels.verified(context,spec,part))continue;
                requireUnmeteredNetwork(context);
                fetchPart(context,spec,part,progress,cancelled);
            }
        }
    }

    private static void check(BooleanSupplier cancelled) {
        if(Thread.currentThread().isInterrupted()||(cancelled!=null&&cancelled.getAsBoolean()))
            throw new CancellationException("Installation annulée");
    }

    private static boolean trustedHost(URL url){
        if(!"https".equalsIgnoreCase(url.getProtocol()))return false;
        String host=url.getHost().toLowerCase(Locale.ROOT);
        return host.equals("huggingface.co")||host.endsWith(".huggingface.co")||
            host.equals("xethub.hf.co")||host.endsWith(".xethub.hf.co")||
            host.endsWith(".hf.co")|| // Includes the official cdn-lfs*.hf.co mirrors.
            host.endsWith(".cloudfront.net")||host.endsWith(".amazonaws.com");
    }
    private static HttpURLConnection connect(Context context,URL original,long resume,BooleanSupplier cancel)throws Exception {
        URL at=original;
        for(int hop=0;hop<8;hop++){
            check(cancel);
            if(!trustedHost(at))throw new SecurityException("Hôte du modèle non autorisé");
            HttpURLConnection c=(HttpURLConnection)selectedUnmeteredNetwork(context).openConnection(at);
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(30000);
            c.setReadTimeout(60000);
            c.setRequestProperty("Accept-Encoding","identity");
            if(resume>0)c.setRequestProperty("Range","bytes="+resume+"-");
            int status=c.getResponseCode();
            if(status==301||status==302||status==303||status==307||status==308){
                String location=c.getHeaderField("Location");
                c.disconnect();
                if(location==null)throw new IOException("Redirection de poids incomplète");
                at=new URL(at,location);
                continue;
            }
            if(status!=200&&status!=206){
                c.disconnect();throw new IOException("Serveur du modèle : HTTP "+status);
            }
            return c;
        }
        throw new IOException("Trop de redirections de téléchargement");
    }

    private static void fetchPart(Context context,SpecialistOcrModels.Spec spec,SpecialistOcrModels.Part part,
                                  Progress progress,BooleanSupplier cancelled)throws Exception {
        File target=SpecialistOcrModels.path(context,spec,part);
        File folder=target.getParentFile();
        if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Stockage privé indisponible");
        File partial=new File(folder,target.getName()+".part");
        long max=part==SpecialistOcrModels.Part.MODEL?spec.maxModel:spec.maxProjector;
        long have=partial.exists()?partial.length():0;
        if(have>max){if(!partial.delete())throw new IOException("Ancien fichier partiel invalide");have=0;}
        long available=folder.getUsableSpace();
        if(available<Math.max(0,max-have)+RESERVE_BYTES)
            throw new IOException("Espace libre insuffisant pour "+spec.title+
                " : libère de la place ou conserve Indra OCR");

        String label=part==SpecialistOcrModels.Part.MODEL?"modèle":"projecteur";
        HttpURLConnection conn=connect(context,new URL(spec.url(part)),have,cancelled);
        try{
            int status=conn.getResponseCode();
            boolean append=have>0&&status==206;
            if(append){
                String range=conn.getHeaderField("Content-Range");
                if(range==null||!range.startsWith("bytes "+have+"-"))
                    throw new IOException("Réponse HTTP Range incohérente");
            }
            long count=append?have:0;
            try(InputStream in=new BufferedInputStream(conn.getInputStream());
                OutputStream out=new BufferedOutputStream(new FileOutputStream(partial,append))){
                byte[] block=new byte[BUFFER];int n;
                long last=0;
                while((n=in.read(block))!=-1){
                    check(cancelled);
                    count+=n;
                    if(count>max)throw new IOException("Taille du modèle supérieure à la limite");
                    out.write(block,0,n);
                    long now=SystemClock.elapsedRealtime();
                    if(progress!=null&&(now-last>=900)){progress.update(spec.title,label,count,max);last=now;}
                }
            }
            check(cancelled);
            if(partial.length()<1_000_000L)throw new IOException("Poids incomplets");
            try(InputStream in=new FileInputStream(partial)){
                if(in.read()!='G'||in.read()!='G'||in.read()!='U'||in.read()!='F')
                    throw new IOException("Le modèle ne possède pas d'en-tête GGUF");
            }
            String expected=part==SpecialistOcrModels.Part.MODEL?spec.modelSha:spec.projectorSha;
            if(!SpecialistOcrModels.sha256(partial).equals(expected)){
                partial.delete();throw new SecurityException("Empreinte SHA-256 incorrecte : modèle rejeté");
            }
            if(target.exists()&&!target.delete())throw new IOException("Impossible de remplacer le modèle périmé");
            if(!partial.renameTo(target))throw new IOException("Installation locale atomique impossible");
            if(progress!=null)progress.update(spec.title,label,target.length(),max);
        } finally{
            conn.disconnect();
        }
    }
}
