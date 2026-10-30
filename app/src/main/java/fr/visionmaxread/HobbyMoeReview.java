package fr.visionmaxread;
import android.content.Context;
import android.os.Debug;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.List;

/** VMR-017: bounded FR/EN segments, shared model weights and fresh KV per segment. */
final class HobbyMoeReview {
    /** Exact OCR/candidate pairs; kept in RAM only. */
    static final class SegmentProposal{
        final int ordinal;final String source,proposal,decision;
        final boolean accepted;
        SegmentProposal(int ordinal,String source,String proposal,String decision,boolean accepted){
            this.ordinal=ordinal;this.source=source;this.proposal=proposal;
            this.decision=decision;this.accepted=accepted;
        }
    }
    static final class Result{
        final String raw,proposed,preview,status;
        final boolean numbersKept;
        final long ms;
        final int cappedSegments,reviewOnlySegments,processedSegments;
        final List<SegmentProposal> comparisons;
        Result(String raw,String proposed,String preview,boolean numbersKept,String status,long ms,
               int cappedSegments,int reviewOnlySegments,int processedSegments,
               List<SegmentProposal> comparisons){
            this.raw=raw;this.proposed=proposed;this.preview=preview;
            this.numbersKept=numbersKept;this.status=status;this.ms=ms;
            this.cappedSegments=cappedSegments;this.reviewOnlySegments=reviewOnlySegments;
            this.processedSegments=processedSegments;
            this.comparisons=java.util.Collections.unmodifiableList(new ArrayList<>(comparisons));
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
        int requestedSegments=context.getSharedPreferences("settings",Context.MODE_PRIVATE)
            .getInt("moeSegmentLimit",MoeSegmentPlanner.DEFAULT_SEGMENTS);
        if(requestedSegments<1||requestedSegments>MoeSegmentPlanner.HARD_SEGMENTS)
            requestedSegments=MoeSegmentPlanner.DEFAULT_SEGMENTS;
        List<MoeSegmentPlanner.Segment> segments=MoeSegmentPlanner.plan(original,requestedSegments);
        StringBuilder suggestions=new StringBuilder(original.length()+1000);
        StringBuilder candidates=new StringBuilder(original.length()+1000);
        int cursor=0;
        List<SegmentProposal> comparisons=new ArrayList<>();
        DocumentContextMemory memory=new DocumentContextMemory();
        int configuredCap=context.getSharedPreferences("settings",Context.MODE_PRIVATE)
            .getInt("moeTokenCap",AdaptiveTokenBudget.DEFAULT_SAFETY_CAP);
        if(configuredCap<128||configuredCap>512)configuredCap=AdaptiveTokenBudget.DEFAULT_SAFETY_CAP;
        int good=0,bad=0,capped=0,reviewOnly=0;
        boolean allDigits=true;
        try(HobbyMoeNative.Session session=new HobbyMoeNative.Session(context)){
            trace.add(context,"P4_MOE_SESSION","HobbyLM-1B-Q8-session","executed","",
                session.loadMs,session.pssAfterLoad,
                "load_ms="+session.loadMs+" weight_MiB="+(session.weightBytes/1048576L)+
                " pss_after_load="+Math.round(session.pssAfterLoad)+" segments="+segments.size());
            for(int n=0;n<segments.size();n++){
                MoeSegmentPlanner.Segment segment=segments.get(n);
                String source=segment.source(original);
                suggestions.append(original,cursor,segment.start);
                candidates.append(original,cursor,segment.start);
                cursor=segment.end;
                final String previous=useContext?memory.previousSummary():"";
                final String opening=useContext?memory.openingExcerpt():"";
                final boolean contextAttached=!previous.isBlank()||
                    (!opening.isBlank()&&!opening.equals(previous));
                final int contextChars=previous.length()+
                    (!opening.equals(previous)?opening.length():0);
                final int budget=AdaptiveTokenBudget.forOcrCorrection(source,configuredCap);
                progress.onSegment(n+1,segments.size(),language,budget,contextChars);
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
                memory.observeOriginal(source);
                suggestions.append(output);
                boolean digits=LiquidOcrReview.numbers(source).equals(LiquidOcrReview.numbers(output));
                OcrProposalGuard.Verdict fidelity=OcrProposalGuard.verify(source,output);
                // A full token allowance is ambiguous without the upstream EOS reason:
                // preserve the source, do not silently approve potentially truncated text.
                boolean mayBeCapped=AdaptiveTokenBudget.outputMayBeCapped(tokenCount[0],budget);
                if(mayBeCapped)capped++;
                boolean valid=digits&&fidelity.accepted&&!mayBeCapped;
                comparisons.add(new SegmentProposal(n+1,source,output,
                    mayBeCapped?"budget_exhausted_unknown":
                    !digits?"numbers_changed":fidelity.reason,valid));
                if(!digits)allDigits=false;
                if(valid){good++;candidates.append(output);}
                else {
                    bad++;candidates.append(source);
                    if(fidelity.reviewRecommended&&!mayBeCapped)reviewOnly++;
                }
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
        suggestions.append(original,cursor,original.length());
        candidates.append(original,cursor,original.length());
        String proposal=suggestions.toString();
        String accepted=candidates.toString();
        boolean any=good>0;
        trace.add(context,"P5_MOE_GUARD","lexical-and-numeric-safety-check",
            "executed",accepted,0,pss(),
            "language="+language+" accepted_segments="+good+" rejected_segments="+bad+
            " manual_review="+reviewOnly+" source_preserved_on_reject=true");
        return new Result(original,proposal,accepted,allDigits,
            any?"candidate":"raw-fallback",SystemClock.elapsedRealtime()-started,capped,
            reviewOnly,segments.size(),comparisons);
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
