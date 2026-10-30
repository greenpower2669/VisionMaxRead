package fr.visionmaxread;
import android.content.Context;
import android.os.Debug;
import android.os.SystemClock;
import java.util.List;

/** Cross-check OCR against a *separately trained* sparse MoE, never rebrand Liquid. */
final class HobbyMoeReview {
    static final class Result{
        final String raw,proposed,preview,status;
        final boolean numbersKept;
        final long ms;
        Result(String raw,String proposed,String preview,boolean numbersKept,String status,long ms){
            this.raw=raw;this.proposed=proposed;this.preview=preview;
            this.numbersKept=numbersKept;this.status=status;this.ms=ms;
        }
    }
    private HobbyMoeReview(){}
    static Result run(Context context,String original,OcrTrace trace,
                      HobbyMoeNative.TokenListener display)throws Exception {
        if(original==null||original.isBlank())throw new IllegalArgumentException("No OCR content");
        // Short independent probe: no Liquid suggestion to contaminate the result.
        int end=Math.min(260,original.length());
        String chunk=original.substring(0,end);
        String prompt=buildPrompt(chunk);
        long start=SystemClock.elapsedRealtime();
        StringBuilder streamed=new StringBuilder();
        int[] tokens={0};
        String output;
        try {
            output=HobbyMoeNative.generate(context,prompt,110,(delta,count)->{
                streamed.append(delta);
                tokens[0]=count;
                display.onToken(delta,count);
            });
        }catch(Exception e){
            long elapsed=SystemClock.elapsedRealtime()-start;
            boolean cancelled="Cancelled".equals(e.getMessage());
            trace.add(context,"P4_REAL_MOE_HOBBY","HobbyLM-1B-Instruct-Q4_K_M",
                cancelled?"interrupted":"error",
                cancelled?streamed.toString():"",
                elapsed,pss(),"tokens="+tokens[0]+" reason="+(cancelled?"user_stop":e.getClass().getSimpleName()));
            throw e;
        }
        long elapsed=SystemClock.elapsedRealtime()-start;
        float pss=pss();
        trace.add(context,"P4_REAL_MOE_HOBBY","HobbyLM-1B-Instruct-Q4_K_M",
            "executed",output,elapsed,pss,
            "routed=64-top8 processed_chars="+chunk.length()+" generated_tokens="+tokens[0]+" stop=eos_or_limit");
        boolean digits=LiquidOcrReview.numbers(chunk).equals(LiquidOcrReview.numbers(output));
        boolean valid=digits&&!output.isBlank()
            &&output.length()>chunk.length()/2&&output.length()<chunk.length()*2+120;
        String body=valid?output:chunk;
        String rest=original.substring(end);
        String preview=body+rest;
        trace.add(context,"P5_MOE_GUARD","MoE-constraint-check","executed",preview,0,pss,
            "numbers_kept="+digits+" proposal_valid="+valid+" source_preserved="+!valid);
        return new Result(original,output,preview,digits,valid?"candidate":"raw-fallback",elapsed);
    }
    /** Published HobbyLM-1B-Instruct chat markers, not generic USER/ASSISTANT. */
    static String buildPrompt(String ocr){
        return "<|user|>\n"+
            "Corrige uniquement les lettres OCR clairement erronées en français. "+
            "Conserve les chiffres, noms et dates. Si tu hésites, recopie exactement. "+
            "Réponds seulement par le texte corrigé.\n"+
            "TEXTE OCR :\n"+ocr+"\n<|assistant|>\n";
    }
    private static float pss(){Debug.MemoryInfo m=new Debug.MemoryInfo();Debug.getMemoryInfo(m);return m.getTotalPss()/1024f;}
}
