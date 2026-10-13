package fr.visionmaxread;

import android.content.Context;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import fr.visionmaxread.core.TextChunks;

/**
 * Explicit experimental speech path. Never accept a voice that declares it needs
 * the network; never silently fall back to an online or non-French voice.
 * No user text is stored by this class. TextToSpeech is supplied by Android.
 */
final class OfflineAndroidTts {
    private volatile TextToSpeech current;

    void stop() {
        TextToSpeech engine=current;
        if(engine!=null)engine.stop();
    }

    void speak(Context context, String text, float speed, BooleanSupplier cancelled,
               Consumer<String> status) throws Exception {
        CountDownLatch initialized=new CountDownLatch(1);
        AtomicInteger initResult=new AtomicInteger(TextToSpeech.ERROR);
        TextToSpeech engine=new TextToSpeech(context.getApplicationContext(),result->{
            initResult.set(result);initialized.countDown();
        });
        current=engine;
        try {
            long initBegin=SystemClock.elapsedRealtime();
            while(!initialized.await(100,TimeUnit.MILLISECONDS))check(cancelled);
            check(cancelled);
            if(initResult.get()!=TextToSpeech.SUCCESS)
                throw new IllegalStateException("Moteur vocal Android indisponible");
            Voice selected=selectFrenchOfflineVoice(engine);
            if(selected==null)
                throw new IllegalStateException("Aucune voix française hors ligne installée");
            if(engine.setVoice(selected)!=TextToSpeech.SUCCESS)
                throw new IllegalStateException("Impossible de sélectionner la voix hors ligne");
            if(engine.setSpeechRate(speed)!=TextToSpeech.SUCCESS)
                throw new IllegalStateException("Vitesse vocale indisponible");
            status.accept("Voix Android hors ligne : "+selected.getName()
                    +" · initialisation "+(SystemClock.elapsedRealtime()-initBegin)+" ms");

            List<String> chunks=TextChunks.split(text,240);
            for(int i=0;i<chunks.size();i++){
                check(cancelled);
                String segment=chunks.get(i);
                if(segment.trim().isEmpty())continue;
                CountDownLatch completed=new CountDownLatch(1);
                AtomicInteger result=new AtomicInteger(0);
                AtomicLong started=new AtomicLong(-1);
                long requested=SystemClock.elapsedRealtime();
                String id="vmr-offline-"+i;
                engine.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                    @Override public void onStart(String utteranceId) {
                        started.compareAndSet(-1,SystemClock.elapsedRealtime()-requested);
                    }
                    @Override public void onDone(String utteranceId){
                        result.set(1);completed.countDown();
                    }
                    @Override public void onError(String utteranceId){
                        result.set(-1);completed.countDown();
                    }
                    @Override public void onError(String utteranceId,int errorCode){
                        result.set(-1);completed.countDown();
                    }
                    @Override public void onStop(String utteranceId,boolean interrupted){
                        result.set(-1);completed.countDown();
                    }
                });
                status.accept("Voix Android · passage "+(i+1)+" / "+chunks.size());
                if(engine.speak(segment,TextToSpeech.QUEUE_FLUSH,null,id)!=TextToSpeech.SUCCESS)
                    throw new IllegalStateException("Échec du démarrage vocal Android");
                boolean first=true;
                while(!completed.await(100,TimeUnit.MILLISECONDS)){
                    check(cancelled);
                    if(first&&started.get()>=0){
                        first=false;
                        status.accept("Synthèse commencée en "+started.get()+" ms (début de lecture, pas audio audible mesuré)");
                    }
                }
                check(cancelled);
                if(result.get()!=1)throw new IllegalStateException("Erreur de synthèse vocale Android");
            }
        } finally {
            if(current==engine)current=null;
            engine.stop();
            engine.shutdown();
        }
    }

    private static Voice selectFrenchOfflineVoice(TextToSpeech engine) {
        Set<Voice> voices=engine.getVoices();
        if(voices==null)return null;
        List<Voice> candidates=new ArrayList<>();
        for(Voice voice:voices) {
            if(voice.isNetworkConnectionRequired()||!"fr".equals(voice.getLocale().getLanguage()))
                continue;
            if(voice.getFeatures()!=null
                    && voice.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
                continue;
            candidates.add(voice);
        }
        // Prefer fr-FR first, then highest declared quality.
        candidates.sort(Comparator
                .comparingInt((Voice v)->Locale.FRANCE.equals(v.getLocale())?0:1)
                .thenComparing(Comparator.comparingInt(Voice::getQuality).reversed()));
        return candidates.isEmpty()?null:candidates.get(0);
    }

    private static void check(BooleanSupplier cancelled){
        if(cancelled.getAsBoolean())throw new CancellationException();
    }
}
