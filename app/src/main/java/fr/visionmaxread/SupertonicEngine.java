package fr.visionmaxread;
import ai.onnxruntime.*;
import android.content.Context;
import android.media.*;
import org.json.*;
import java.nio.FloatBuffer;
import java.text.Normalizer;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import fr.visionmaxread.core.TextChunks;
import fr.visionmaxread.core.Cancellation;
/** Android port of the official Supertonic 3 CPU pipeline. No Android/cloud TTS fallback. */
final class SupertonicEngine {
    private volatile AudioTrack playing;
    void stop(){AudioTrack t=playing;if(t!=null)try{t.pause();t.flush();}catch(IllegalStateException ignored){}}
    void speak(Context context,String text,String voice,float speed,ModelFiles files,BooleanSupplier cancelled,Consumer<String> status) throws Exception {
        Cancellation operation=new Cancellation(cancelled);
        OrtEnvironment env=OrtEnvironment.getEnvironment();
        JSONObject config=new JSONObject(files.readAsset("models/tts/onnx/tts.json"));
        int rate=config.getJSONObject("ae").getInt("sample_rate"),chunk=config.getJSONObject("ae").getInt("base_chunk_size")*config.getJSONObject("ttl").getInt("chunk_compress_factor");
        int dims=config.getJSONObject("ttl").getInt("latent_dim")*config.getJSONObject("ttl").getInt("chunk_compress_factor");
        JSONArray index=new JSONArray(files.readAsset("models/tts/onnx/unicode_indexer.json"));
        JSONObject styles=new JSONObject(files.readAsset("models/tts/voice_styles/"+(voice.equals("M1")?"M1":"F1")+".json"));
        List<OrtSession> sessions=new ArrayList<>();
        AudioManager manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        AudioFocusRequest focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs).setOnAudioFocusChangeListener(change->{if(change<0){operation.cancel();stop();}}).build();
        boolean focused=false;
        try(OrtSession.SessionOptions options=new OrtSession.SessionOptions();
            OnnxTensor styleTtl=style(env,styles.getJSONObject("style_ttl"));
            OnnxTensor styleDp=style(env,styles.getJSONObject("style_dp"))){
            options.setIntraOpNumThreads(2);options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            for(String name:new String[]{"duration_predictor","text_encoder","vector_estimator","vocoder"}){
                operation.check();sessions.add(env.createSession(files.file("tts/onnx/"+name+".onnx").getPath(),options));
            }
            operation.check();
            if(manager.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)throw new IllegalStateException("Lecture audio indisponible");
            focused=true;
            List<String> chunks=TextChunks.split(text,240);int count=0;
            for(String segment:chunks){
                operation.check();if(segment.trim().isEmpty())continue;
                status.accept("Préparation de la voix · passage "+(++count)+" / "+chunks.size());
                String processed=normalize(segment);int[] cps=processed.codePoints().toArray();
                long[][] ids=new long[1][cps.length];float[][][] mask=new float[1][1][cps.length];
                for(int i=0;i<cps.length;i++){if(cps[i]>=index.length())throw new IllegalArgumentException("Caractère non pris en charge");ids[0][i]=index.getLong(cps[i]);mask[0][0][i]=1;}
                float duration;
                try(OnnxTensor idTensor=OnnxTensor.createTensor(env,ids);OnnxTensor textMask=OnnxTensor.createTensor(env,mask)){
                    try(OrtSession.Result result=sessions.get(0).run(Map.of("text_ids",idTensor,"style_dp",styleDp,"text_mask",textMask))){
                        FloatBuffer b=((OnnxTensor)result.get(0)).getFloatBuffer();duration=b.get(0)/speed;
                    }
                    if(!Float.isFinite(duration)||duration<=0||duration>90)throw new IllegalStateException("Durée de voix invalide");
                    int samples=(int)(duration*rate),length=(samples+chunk-1)/chunk;
                    float[][][] latent=new float[1][dims][length],latentMask=new float[1][1][length];
                    Arrays.fill(latentMask[0][0],1);Random rng=new Random(2026);
                    for(int d=0;d<dims;d++)for(int t=0;t<length;t++)latent[0][d][t]=(float)(Math.sqrt(-2*Math.log(Math.max(1e-10,rng.nextDouble())))*Math.cos(2*Math.PI*rng.nextDouble()));
                    try(OrtSession.Result encoded=sessions.get(1).run(Map.of("text_ids",idTensor,"style_ttl",styleTtl,"text_mask",textMask));
                        OnnxTensor lm=OnnxTensor.createTensor(env,latentMask);
                        OnnxTensor total=OnnxTensor.createTensor(env,new float[]{8})){
                        OnnxTensor embedded=(OnnxTensor)encoded.get(0);
                        for(int step=0;step<8;step++){
                            operation.check();
                            try(OnnxTensor noisy=OnnxTensor.createTensor(env,latent);OnnxTensor current=OnnxTensor.createTensor(env,new float[]{step});
                                OrtSession.Result result=sessions.get(2).run(Map.of("noisy_latent",noisy,"text_emb",embedded,"style_ttl",styleTtl,"latent_mask",lm,"text_mask",textMask,"current_step",current,"total_step",total))){
                                latent=(float[][][])result.get(0).getValue();
                            }
                        }
                        operation.check();
                        try(OnnxTensor finalLatent=OnnxTensor.createTensor(env,latent);OrtSession.Result result=sessions.get(3).run(Map.of("latent",finalLatent))){
                            float[][] wav=(float[][])result.get(0).getValue();operation.check();
                            status.accept("Lecture · passage "+count+" / "+chunks.size());
                            play(wav[0],Math.min(samples,wav[0].length),rate,attrs,operation::cancelled);
                        }
                    }
                }
            }
        }finally{
            stop();for(OrtSession session:sessions)session.close();if(focused)manager.abandonAudioFocusRequest(focus);
        }
    }
    private void play(float[] wav,int length,int rate,AudioAttributes attrs,BooleanSupplier cancelled) throws Exception {
        short[] pcm=new short[length];for(int i=0;i<length;i++)pcm[i]=(short)(Math.max(-1,Math.min(1,wav[i]))*32767);
        int buffer=Math.max(16384,AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT));
        AudioTrack track=new AudioTrack.Builder().setAudioAttributes(attrs).setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setBufferSizeInBytes(buffer).setTransferMode(AudioTrack.MODE_STREAM).build();
        if(track.getState()!=AudioTrack.STATE_INITIALIZED){track.release();throw new IllegalStateException("Sortie audio indisponible");}
        playing=track;
        try{
            check(cancelled);track.play();int offset=0;
            while(offset<pcm.length){check(cancelled);if(track.getPlayState()!=AudioTrack.PLAYSTATE_PLAYING)throw new java.util.concurrent.CancellationException();int n=track.write(pcm,offset,Math.min(4096,pcm.length-offset),AudioTrack.WRITE_NON_BLOCKING);if(n<0)throw new IllegalStateException("Erreur audio");offset+=n;if(n==0)Thread.sleep(10);}
            while(Integer.toUnsignedLong(track.getPlaybackHeadPosition())<pcm.length){check(cancelled);if(track.getPlayState()!=AudioTrack.PLAYSTATE_PLAYING)throw new java.util.concurrent.CancellationException();Thread.sleep(20);}
        }finally{playing=null;track.release();}
    }
    private static OnnxTensor style(OrtEnvironment env,JSONObject object) throws Exception {
        JSONArray dims=object.getJSONArray("dims"),data=object.getJSONArray("data");long[] shape={dims.getLong(0),dims.getLong(1),dims.getLong(2)};
        float[] flat=new float[(int)(shape[0]*shape[1]*shape[2])];int k=0;
        for(int b=0;b<data.length();b++)for(int r=0;r<data.getJSONArray(b).length();r++){
            JSONArray row=data.getJSONArray(b).getJSONArray(r);for(int c=0;c<row.length();c++)flat[k++]=(float)row.getDouble(c);
        }
        if(k!=flat.length)throw new IllegalStateException("Style vocal invalide");
        return OnnxTensor.createTensor(env,FloatBuffer.wrap(flat),shape);
    }
    private static void check(BooleanSupplier cancelled){if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();}
    static String normalize(String text){
        text=Normalizer.normalize(text,Normalizer.Form.NFKD);
        StringBuilder filtered=new StringBuilder();text.codePoints().filter(c->!((c>=0x1f000&&c<=0x1faff)||(c>=0x2600&&c<=0x27bf))).forEach(filtered::appendCodePoint);
        text=filtered.toString().replaceAll("[–‑—]","-").replaceAll("[_\\[\\]|/#→←]"," ")
            .replace('“','"').replace('”','"').replace('‘','\'').replace('’','\'').replace('´','\'').replace('`','\'')
            .replaceAll("[♥☆♡©\\\\]","").replace("@"," arobase ").replaceAll("\\s+"," ").replaceAll(" +([,.!?;:'])","$1").trim();
        if(!text.matches("(?s).*[.!?;:,'\")\\]}…»]$"))text+=".";
        return "<fr>"+text+"</fr>";
    }
}
