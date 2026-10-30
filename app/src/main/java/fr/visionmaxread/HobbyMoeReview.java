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
        final int cappedSegments;
        Result(String raw,String proposed,String preview,boolean numbersKept,String status,long ms,int cappedSegments){
            this.raw=raw;this.proposed=proposed;this.preview=preview;
            this.numbersKept=numbersKept;this.status=status;this.ms=ms;this.cappedSegments=cappedSegments;
        }
    }
    interface SegmentListener {void onSegment(int index,int total,String language,int budget,int contextChars);}
    private HobbyMoeReview(){}
    static Result run(Context context,String original,OcrTrace trace,String language,
                      HobbyMoeNative.TokenListener display,SegmentListener progress)throws Exception {
        return run(context,original,trace,language,true,display,progress);
    }
    static Result run(Context context,String original,OcrTrace trace,String language,boolean useContext,
                      HobbyMoeNative.TokenListener display,SegmentListener progress)throws Exception {
        if(original==null||original.isBlank())throw new IllegalArgumentException("No OCR content");
        if(!language.equals("fr")&&!language.equals("en"))throw new IllegalArgumentException("Language must be fr or en");
        final long started=SystemClock.elapsedRealtime();
        // Lines are independently checked: do not split inside a pension/reference.
        String[] lines=original.split("\n",-1);
        List<Integer> indexes=new ArrayList<>();
        for(int i=0;i<lines.length&&indexes.size()<3;i++){
            if(!lines[i].trim().isEmpty())indexes.add(i);
        }
        String[] suggestions=lines.clone();
        String[] candidates=lines.clone();
        DocumentContextMemory memory=new DocumentContextMemory();
        int configuredCap=context.getSharedPreferences("settings",Context.MODE_PRIVATE)
            .getInt("moeTokenCap",AdaptiveTokenBudget.DEFAULT_SAFETY_CAP);
        if(configuredCap<128||configuredCap>512)configuredCap=AdaptiveTokenBudget.DEFAULT_SAFETY_CAP;
        int good=0,bad=0,capped=0;
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
                final String previous=useContext?memory.previousSummary():"";
                final String opening=useContext?memory.openingExcerpt():"";
                final boolean contextAttached=!previous.isBlank()||
                    (!opening.isBlank()&&!opening.equals(previous));
                final int contextChars=previous.length()+
                    (!opening.equals(previous)?opening.length():0);
                final int budget=AdaptiveTokenBudget.forOcrCorrection(source,configuredCap);
                progress.onSegment(n+1,indexes.size(),language,budget,contextChars);
                final int[] tokenCount={0};
                StringBuilder partial=new StringBuilder();
                long tic=SystemClock.elapsedRealtime();
                final long[] firstTokenMs={-1};
                String output;
                try{
                    output=session.generate(buildPrompt(source,language,previous,opening),budget,(delta,count)->{
                        if(firstTokenMs[0]<0)firstTokenMs[0]=SystemClock.elapsedRealtime()-tic;
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
                memory.observeOriginal(source);
                suggestions[row]=output;
                if(stream.length()>0)stream.append('\n');
                stream.append(output);
                boolean digits=LiquidOcrReview.numbers(source).equals(LiquidOcrReview.numbers(output));
                OcrProposalGuard.Verdict fidelity=OcrProposalGuard.verify(source,output);
                // A full token allowance is ambiguous without the upstream EOS reason:
                // preserve the source, do not silently approve potentially truncated text.
                boolean mayBeCapped=AdaptiveTokenBudget.outputMayBeCapped(tokenCount[0],budget);
                if(mayBeCapped)capped++;
                boolean valid=digits&&fidelity.accepted&&!mayBeCapped;
                if(!digits)allDigits=false;
                if(valid){good++;candidates[row]=output;}else bad++;
                trace.add(context,"P4_MOE_SEG_"+(n+1),"HobbyLM-1B-Instruct-Q8",
                    "executed",output,ms,pss(),
                    "language="+language+" ctx_sent="+contextAttached+" ctx_chars="+contextChars+
                    " budget="+budget+" tokens="+tokenCount[0]+
                    " finish="+(mayBeCapped?"budget_or_eos_unknown":"upstream_unknown")+
                    " guard="+fidelity.reason+" candidate="+valid+
                    " first_token_ms="+firstTokenMs[0]+
                    " token_per_second="+(ms>0?Math.round(tokenCount[0]*1000f/ms):0)+
                    " numbers_preserved="+digits);
            }
        } // critical: release all owned expert weights after last short line
        String proposal=String.join("\n",suggestions);
        String accepted=String.join("\n",candidates);
        boolean any=good>0;
        trace.add(context,"P5_MOE_GUARD","lexical-and-numeric-safety-check",
            "executed",accepted,0,pss(),
            "language="+language+" accepted_lines="+good+" rejected_lines="+bad+
            " source_preserved_on_reject=true");
        return new Result(original,proposal,accepted,allDigits,
            any?"candidate":"raw-fallback",SystemClock.elapsedRealtime()-started,capped);
    }
    static String buildPrompt(String line,String language){
        return MoePromptBuilder.build(line,language,"");
    }
    static String buildPrompt(String line,String language,String previousRawOcr){
        return MoePromptBuilder.build(line,language,previousRawOcr);
    }
    static String buildPrompt(String line,String language,String previousRawOcr,String openingRawOcr){
        return MoePromptBuilder.build(line,language,previousRawOcr,openingRawOcr);
    }
    static String buildPrompt(String text){return buildPrompt(text,"fr");}
    private static float pss(){
        Debug.MemoryInfo m=new Debug.MemoryInfo();Debug.getMemoryInfo(m);
        return m.getTotalPss()/1024f;
    }
}
