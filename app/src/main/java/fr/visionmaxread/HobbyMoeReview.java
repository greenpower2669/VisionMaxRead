package fr.visionmaxread;
import android.content.Context;
import android.os.Debug;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.List;

/** VMR-016: small FR/EN lines, single loaded MoE, separate fresh KV context per line. */
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
    interface SegmentListener {void onSegment(int index,int total,String language);}
    private HobbyMoeReview(){}
    static Result run(Context context,String original,OcrTrace trace,String language,
                      HobbyMoeNative.TokenListener display,SegmentListener progress)throws Exception {
        if(original==null||original.isBlank())throw new IllegalArgumentException("No OCR content");
        if(!language.equals("fr")&&!language.equals("en"))throw new IllegalArgumentException("Language must be fr or en");
        final long started=SystemClock.elapsedRealtime();
        // Lines are independently checked: do not split inside a pension/reference.
        String[] lines=original.split("\\n",-1);
        List<Integer> indexes=new ArrayList<>();
        for(int i=0;i<lines.length&&indexes.size()<3;i++){
            if(!lines[i].trim().isEmpty())indexes.add(i);
        }
        String[] suggestions=lines.clone();
        String[] candidates=lines.clone();
        int good=0,bad=0;
        long totalTokens=0;
        boolean allDigits=true;
        StringBuilder stream=new StringBuilder();
        try(HobbyMoeNative.Session session=new HobbyMoeNative.Session(context)){
            trace.add(context,"P4_MOE_SESSION","HobbyLM-1B-Q8-session","executed","",
                session.loadMs,session.pssAfterLoad,
                "load_ms="+session.loadMs+" weight_MiB="+(session.weightBytes/1048576L)+
                " pss_after_load="+Math.round(session.pssAfterLoad)+" segments="+indexes.size());
            for(int n=0;n<indexes.size();n++){
                int row=indexes.get(n);
                String source=lines[row];
                // Do not silently truncate a source line or construct a different result.
                if(source.length()>150){
                    bad++;
                    trace.add(context,"P4_MOE_SEG_"+(n+1),"HobbyLM","skipped","",0,pss(),
                        "reason=long_line length="+source.length());
                    continue;
                }
                progress.onSegment(n+1,indexes.size(),language);
                final int[] tokenCount={0};
                StringBuilder partial=new StringBuilder();
                long tic=SystemClock.elapsedRealtime();
                String output;
                try{
                    output=session.generate(buildPrompt(source,language),72,(delta,count)->{
                        partial.append(delta);
                        tokenCount[0]=count;
                        display.onToken(delta,count);
                    });
                }catch(Exception e){
                    boolean cancelled="Cancelled".equals(e.getMessage());
                    trace.add(context,"P4_MOE_SEG_"+(n+1),"HobbyLM-Q8",cancelled?"interrupted":"error",
                        cancelled?partial.toString():"",SystemClock.elapsedRealtime()-tic,pss(),
                        "tokens="+tokenCount[0]+" reason="+(cancelled?"user_stop":e.getClass().getSimpleName()));
                    throw e;
                }
                long ms=SystemClock.elapsedRealtime()-tic;
                totalTokens+=tokenCount[0];
                suggestions[row]=output;
                if(stream.length()>0)stream.append('\n');
                stream.append(output);
                boolean digits=LiquidOcrReview.numbers(source).equals(LiquidOcrReview.numbers(output));
                boolean valid=digits&&!output.isBlank()&&output.length()>=Math.max(2,source.length()/2)
                    &&output.length()<=source.length()*2+20
                    &&!output.toLowerCase(java.util.Locale.ROOT).contains("texte proposé");
                if(!digits)allDigits=false;
                if(valid){good++;candidates[row]=output;}else bad++;
                trace.add(context,"P4_MOE_SEG_"+(n+1),"HobbyLM-1B-Instruct-Q8",
                    "executed",output,ms,pss(),
                    "language="+language+" generated_tokens="+tokenCount[0]+
                    " numbers_preserved="+digits+" accepted_candidate="+valid+
                    " first_token_delay_not_yet_isolated");
            }
        } // critical: release all owned expert weights after last short line
        String proposal=String.join("\n",suggestions);
        String accepted=String.join("\n",candidates);
        boolean any=good>0;
        trace.add(context,"P5_MOE_GUARD","per-line-number-and-length-check",
            "executed",accepted,0,pss(),
            "language="+language+" accepted_lines="+good+" rejected_lines="+bad+
            " source_preserved_on_reject=true");
        return new Result(original,proposal,accepted,allDigits,
            any?"candidate":"raw-fallback",SystemClock.elapsedRealtime()-started);
    }
    static String buildPrompt(String line,String language){
        if("en".equals(language))
            return "<|user|>\\nFix obvious OCR spelling errors in this English line. "+
                "Keep numbers, names and dates unchanged. Output ONLY the corrected line.\\n"+
                line+"\\n<|assistant|>\\n";
        return "<|user|>\\nCorrige les fautes OCR évidentes dans cette seule ligne française. "+
            "Conserve exactement les nombres, noms propres et dates. Réponds uniquement par la ligne.\\n"+
            line+"\\n<|assistant|>\\n";
    }
    // Legacy one-language builder is retained for old contract callers/test.
    static String buildPrompt(String text){return buildPrompt(text,"fr");}
    private static float pss(){
        Debug.MemoryInfo m=new Debug.MemoryInfo();Debug.getMemoryInfo(m);
        return m.getTotalPss()/1024f;
    }
}
