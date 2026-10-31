package fr.visionmaxread;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Consent-triggered foreground acquisition of public OCR weights only.
 * The service never receives image pixels, recognized text, or document URIs.
 * Android may still prevent restart after a user force-stop or exhaust the
 * platform's data-sync foreground-service time quota.
 */
public final class SpecialistOcrDownloadService extends Service {
    private static final String START="fr.visionmaxread.MODEL_INSTALL";
    private static final String CANCEL="fr.visionmaxread.MODEL_CANCEL";
    private static final String CHANNEL="vmr_ocr_weights";
    private static final int NOTIFICATION_ID=21021;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy=new AtomicBoolean();
    private final AtomicBoolean cancelled=new AtomicBoolean();
    private long lastProgressMs;

    static SharedPreferences state(Context c) {
        return c.getApplicationContext().getSharedPreferences("ocr_model_install",Context.MODE_PRIVATE);
    }
    static boolean active(Context c) { return state(c).getBoolean("active",false); }

    static long start(Context c,SpecialistOcrModels.Spec[] specs) {
        if(specs==null||specs.length==0)throw new IllegalArgumentException("Aucun moteur");
        if(active(c))throw new IllegalStateException("Une installation est déjà active");
        StringBuilder engines=new StringBuilder();
        for(SpecialistOcrModels.Spec spec:specs){
            if(engines.length()>0)engines.append(',');
            engines.append(spec.engine.name());
        }
        SharedPreferences pref=state(c);
        long id=pref.getLong("requestId",0)+1;
        pref.edit().putLong("requestId",id).putString("engines",engines.toString())
            .putString("status","Démarrage du service de téléchargement…")
            .putBoolean("active",true).putBoolean("complete",false).commit();
        Intent intent=new Intent(c,SpecialistOcrDownloadService.class).setAction(START);
        try {
            if(Build.VERSION.SDK_INT>=26)c.startForegroundService(intent);
            else c.startService(intent);
        }catch(RuntimeException ex){
            pref.edit().putBoolean("active",false).putString("status","Service Android indisponible").commit();
            throw ex;
        }
        return id;
    }
    static void cancel(Context c) {
        Intent intent=new Intent(c,SpecialistOcrDownloadService.class).setAction(CANCEL);
        c.startService(intent);
    }

    @Override public void onCreate(){
        super.onCreate();
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel channel=new NotificationChannel(CHANNEL,
                "Installation des modèles OCR",NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Téléchargement des poids publics, sans transfert de documents");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }
    private Notification notification(String message,boolean ongoing){
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent click=PendingIntent.getActivity(this,0,open,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent stop=new Intent(this,SpecialistOcrDownloadService.class).setAction(CANCEL);
        PendingIntent stopAction=PendingIntent.getService(this,1,stop,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder n=Build.VERSION.SDK_INT>=26?
            new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        n.setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("VisionMaxRead · modèles OCR")
            .setContentText(message).setContentIntent(click)
            .setOnlyAlertOnce(true).setOngoing(ongoing)
            .setCategory(Notification.CATEGORY_PROGRESS);
        if(ongoing)n.addAction(android.R.drawable.ic_menu_close_clear_cancel,
            "Annuler",stopAction);
        return n.build();
    }
    private void foreground(){
        Notification n=notification("Préparation des modèles OCR…",true);
        if(Build.VERSION.SDK_INT>=29)
            startForeground(NOTIFICATION_ID,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(NOTIFICATION_ID,n);
    }
    private void report(String message){
        state(this).edit().putString("status",message).apply();
        long now=SystemClock.elapsedRealtime();
        if(now-lastProgressMs>2000){
            lastProgressMs=now;
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_ID,notification(message,true));
        }
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&CANCEL.equals(intent.getAction())){
            cancelled.set(true);
            report("Annulation demandée ; reprise possible ultérieurement.");
            if(!busy.get()){
                state(this).edit().putBoolean("active",false).putBoolean("complete",false).commit();
                stopSelf();
            }
            return START_NOT_STICKY;
        }
        SharedPreferences p=state(this);
        if(!p.getBoolean("active",false)){stopSelf(startId);return START_NOT_STICKY;}
        foreground(); // Called before starting any potentially long network or disk operation.
        if(busy.compareAndSet(false,true)){
            cancelled.set(false);
            final String engines=p.getString("engines","");
            executor.execute(()->install(engines,startId));
        }
        return START_REDELIVER_INTENT;
    }
    private void install(String engines,int startId){
        boolean success=false;
        String outcome="Installation interrompue";
        try{
            for(String name:engines.split(",")){
                if(cancelled.get())throw new CancellationException();
                SpecialistOcrModels.Engine engine=SpecialistOcrModels.Engine.valueOf(name);
                SpecialistOcrModels.Spec spec=SpecialistOcrModels.get(engine);
                int attempts=0;
                while(true){
                    if(cancelled.get()||Thread.currentThread().isInterrupted())
                        throw new CancellationException();
                    try{
                        SpecialistOcrAutoInstaller.ensure(this,spec,(title,part,bytes,max)->{
                            if(cancelled.get())throw new CancellationException();
                            report(title+" · "+part+" : "+(bytes/(1024*1024))+" Mio reçus");
                        },cancelled::get);
                        break;
                    }catch(IOException e){
                        attempts++;
                        if(attempts>=6)throw e;
                        report(spec.title+" : connexion interrompue, reprise automatique ("+
                            attempts+"/5)");
                        // Partial .part is preserved; every retry rechecks Wi-Fi and SHA-256.
                        for(int i=0;i<15*attempts;i++){
                            if(cancelled.get())throw new CancellationException();
                            Thread.sleep(1000);
                        }
                    }
                }
            }
            success=true;
            outcome="Modèles installés et vérifiés ; OCR utilisable hors connexion.";
        }catch(CancellationException|InterruptedException ex){
            Thread.currentThread().interrupt();
            outcome="Installation interrompue ; fichiers partiels conservés.";
        }catch(Exception ex){
            DebugLog.append(this,"VMR021_INSTALL failed type="+ex.getClass().getSimpleName());
            outcome="Installation incomplète ("+ex.getClass().getSimpleName()+
                "). Relancer sur Wi-Fi ; Indra reste disponible.";
        }finally{
            state(this).edit().putBoolean("complete",success).putBoolean("active",false)
                .putString("status",outcome).commit();
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(
                NOTIFICATION_ID,notification(outcome,false));
            stopForeground(STOP_FOREGROUND_DETACH);
            busy.set(false);
            stopSelf(startId);
        }
    }
    @Override public void onDestroy(){
        executor.shutdownNow();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
