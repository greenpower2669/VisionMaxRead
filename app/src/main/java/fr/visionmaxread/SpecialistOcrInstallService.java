package fr.visionmaxread;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * VMR-021: user-initiated, foreground installation of public model weights only.
 * Does not hold documents, bitmaps or recognized text. All files remain private.
 * The service is independent from MainActivity and may be redelivered after process death.
 */
public final class SpecialistOcrInstallService extends Service {
    static final String EVENT = "fr.visionmaxread.MODEL_INSTALL_STATE";
    static final String PREFS = "specialist_install";
    static final String KEY_STATE = "state", KEY_MESSAGE = "message";
    static final String ACTION_START = "fr.visionmaxread.START_MODELS";
    static final String ACTION_CANCEL = "fr.visionmaxread.CANCEL_MODELS";
    private static final String EXTRA_TARGETS = "targets";
    private static final String CHANNEL = "ocr_model_install";
    private static final int NOTIFICATION_ID = 21021;
    private static final AtomicBoolean WORKING = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean cancelled;
    private volatile Future<?> task;
    private long lastNotify;

    static boolean isWorking(Context c) {
        return WORKING.get() || "running".equals(c.getSharedPreferences(PREFS, MODE_PRIVATE)
                                                        .getString(KEY_STATE, ""));
    }

    static void install(Context context, SpecialistOcrModels.Spec[] specs) {
        Intent i = new Intent(context, SpecialistOcrInstallService.class).setAction(ACTION_START);
        String[] names = new String[specs.length];
        for (int n=0;n<specs.length;n++) names[n] = specs[n].engine.name();
        i.putExtra(EXTRA_TARGETS, names);
        // Called only from the positive button of a foreground Activity.
        context.startForegroundService(i);
    }

    static void cancel(Context context) {
        context.startService(new Intent(context, SpecialistOcrInstallService.class).setAction(ACTION_CANCEL));
    }

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Installation des OCR",
                NotificationManager.IMPORTANCE_LOW));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        if (ACTION_CANCEL.equals(intent.getAction())) {
            cancelled = true;
            Future<?> f = task;
            if (f != null) f.cancel(true);
            if (!WORKING.get()) stopSelf();
            return START_NOT_STICKY;
        }
        if (!ACTION_START.equals(intent.getAction())) return START_NOT_STICKY;
        if (!WORKING.compareAndSet(false, true)) return START_REDELIVER_INTENT;
        cancelled = false;
        // Must start foreground promptly even if Wi-Fi is temporarily unavailable.
        startForeground(NOTIFICATION_ID, notification("Préparation des modèles OCR", true),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        update("running", "Installation des poids en cours sur Wi-Fi.", true);
        final String[] targets = intent.getStringArrayExtra(EXTRA_TARGETS);
        task = executor.submit(() -> runInstall(targets));
        return START_REDELIVER_INTENT;
    }

    private void runInstall(String[] names) {
        String state = "done", message = "Modèles vérifiés, disponibles hors connexion.";
        try {
            if (names == null || names.length == 0 || names.length > 2)
                throw new IOException("Sélection des modèles invalide");
            for (String name : names) {
                checkCancel();
                SpecialistOcrModels.Engine engine;
                try { engine = SpecialistOcrModels.Engine.valueOf(name); }
                catch (IllegalArgumentException e) { throw new IOException("Moteur inconnu"); }
                final SpecialistOcrModels.Spec spec = SpecialistOcrModels.get(engine);
                for (int attempt=0;;attempt++) {
                    checkCancel();
                    awaitNetwork();
                    try {
                        SpecialistOcrAutoInstaller.ensure(getApplicationContext(), spec,
                            (title, part, bytes, maximum) -> {
                                String msg = title+" · "+part+" : "+(bytes/(1024*1024))+
                                    " Mo reçus (maximum "+(maximum/(1024*1024))+" Mo)";
                                update("running", msg, true);
                            }, () -> cancelled || Thread.currentThread().isInterrupted());
                        break;
                    } catch (IOException e) {
                        // A failed read retains its .part file; resume only on non-metered Wi-Fi.
                        if (attempt >= 5) throw e;
                        update("running", "Transfert interrompu : reprise automatique sur Wi-Fi…", true);
                        pause(5000L);
                    }
                }
            }
        } catch (CancellationException e) {
            state = "cancelled"; message = "Installation arrêtée. Reprise possible avec les fichiers partiels.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            state = "cancelled"; message = "Installation interrompue. Fichiers partiels conservés.";
        } catch (Exception e) {
            state = "error"; message = "Installation incomplète ("+e.getClass().getSimpleName()+
                "). Vérifie le Wi-Fi et l'espace libre ; Indra reste disponible.";
            DebugLog.append(this, "VMR021_INSTALL error="+e.getClass().getSimpleName());
        } finally {
            update(state, message, false);
            WORKING.set(false);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    private void checkCancel() {
        if (cancelled || Thread.currentThread().isInterrupted())
            throw new CancellationException("Arrêt demandé");
    }

    private void pause(long ms) throws InterruptedException {
        Thread.sleep(ms);
        checkCancel();
    }

    private void awaitNetwork() throws Exception {
        // Hold the service while offline without falling back to mobile data.
        // Android 15+ applies a cumulative 6h / 24h dataSync foreground limit.
        for (int checks = 0; checks < 480; checks++) {
            checkCancel();
            try { SpecialistOcrAutoInstaller.requireUnmeteredNetwork(this); return; }
            catch (IOException e) {
                if (checks == 479) throw e;
                update("running", "Wi-Fi non facturé absent : téléchargement en pause.", true);
                pause(15000L);
            }
        }
    }

    private Notification notification(String message, boolean active) {
        Intent open = new Intent(this, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent launch = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("VisionMaxRead · modèles OCR")
            .setContentText(message).setStyle(new Notification.BigTextStyle().bigText(message))
            .setContentIntent(launch).setOnlyAlertOnce(true).setOngoing(active);
        if (active) {
            PendingIntent cancel = PendingIntent.getService(this, 1,
                new Intent(this, SpecialistOcrInstallService.class).setAction(ACTION_CANCEL),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            b.addAction(new Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel, "Arrêter", cancel).build());
        }
        return b.build();
    }

    private void update(String state, String message, boolean active) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putString(KEY_STATE, state).putString(KEY_MESSAGE, message).apply();
        // Explicit-package broadcast: state only, never document/OCR content.
        sendBroadcast(new Intent(EVENT).setPackage(getPackageName())
            .putExtra(KEY_STATE, state).putExtra(KEY_MESSAGE, message));
        long now = SystemClock.elapsedRealtime();
        if (now-lastNotify >= 1000 || !active) {
            lastNotify=now;
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            nm.notify(NOTIFICATION_ID, notification(message, active));
        }
    }

    @Override public void onTimeout(int startId, int fgsType) {
        cancelled=true;
        Future<?> f=task;
        if (f!=null) f.cancel(true);
        update("error", "Limite Android atteinte. Relance l'installation sur Wi-Fi.", false);
        stopSelf(startId);
    }

    @Override public void onDestroy() {
        cancelled=true;
        Future<?> f=task; if (f!=null) f.cancel(true);
        executor.shutdownNow();
        WORKING.set(false);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
