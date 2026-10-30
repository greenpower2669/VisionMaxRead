package fr.visionmaxread;

import android.content.Context;
import android.os.Debug;
import android.os.SystemClock;
import java.io.File;
import java.util.*;

/** Experimental Liquid1.2/Qwen1.7 correction over identical immutable Indra OCR. */
final class TextualDocumentReview {
    static final class Result{
        final String original,proposal,verifiedCandidates,engine;
        final int total,processed,accepted,rejected;
        final long elapsedMs,loadMs;
        Result(String a,String b,String c,String d,int t,int p,int ok,int no,long ms,long loading){
            original=a;proposal=b;verifiedCandidates=c;engine=d;total=t;processed=p;accepted=ok;
            rejected=no;elapsedMs=ms;loadMs=loading;
        }
    }
    interface Progress {
        void onPhrase(int ordinal,int total,String message);
        default void onToken(int ordinal,int total,String cumulative,int tokenCount){}
    }
    private TextualDocumentReview(){}
    static Result run(Context context,String ocr,OcrTrace trace,int chosen,String language,
                      boolean useContext,Progress progress)throws Exception{
        if(chosen==OfflineCorrectionModels.OCR_ONLY)
            throw new IllegalArgumentException("OCR-only mode has no model");
        if(!language.equals("fr")&&!language.equals("en"))throw new IllegalArgumentException("language");
        if(ocr==null||ocr.isBlank())throw new IllegalArgumentException("No OCR source");
        OfflineCorrectionModels.Spec spec=OfflineCorrectionModels.spec(chosen);
        File verified=OfflineCorrectionModels.verified(context,chosen);
        long start=SystemClock.elapsedRealtime();
        ArrayList<DocumentSentenceChunks.Segment> spans=new ArrayList<>(DocumentSentenceChunks.split(ocr));
        DocumentContextMemory memory=new DocumentContextMemory();
        StringBuilder proposal=new StringBuilder(),acceptedText=new StringBuilder();
        int ok=0,no=0,done=0;
        long loadMs;
        final int maxSegments=40;
        try(LiquidNative session=new LiquidNative(verified)){
            loadMs=SystemClock.elapsedRealtime()-start;
            trace.add(context,"VMR017_TEXT_SESSION",spec.name,"executed","",loadMs,pss(),
                "quant="+spec.quant+" load_ms="+loadMs+" selected="+spec.repo+" total_segments="+spans.size()+
                " context="+useContext+" same_weights_session=true");
            for(int index=0;index<spans.size();index++){
                DocumentSentenceChunks.Segment span=spans.get(index);
                String source=span.raw.trim();
                if(index>=maxSegments||source.isBlank()||source.codePointCount(0,source.length())>620){
                    proposal.append(span.raw);acceptedText.append(span.raw);
                    if(!source.isBlank()&&index<maxSegments)no++;
                    continue;
                }
                progress.onPhrase(index+1,Math.min(maxSegments,spans.size()),spec.name);
                int cap=context.getSharedPreferences("settings",Context.MODE_PRIVATE)
                    .getInt("moeTokenCap",512);
                if(cap<128||cap>512)cap=512;
                int budget=AdaptiveTokenBudget.forOcrCorrection(source,cap);
                String prompt=buildPrompt(source,language,chosen,useContext?memory.previousSummary():"");
                long tic=SystemClock.elapsedRealtime();
                final long[] firstToken={-1};
                final int ordinal=index+1;
                String candidate=session.generate(prompt,budget,(cumulative,emitted)->{
                    if(firstToken[0]<0)firstToken[0]=SystemClock.elapsedRealtime()-tic;
                    progress.onToken(ordinal,Math.min(maxSegments,spans.size()),
                        new String(cumulative,java.nio.charset.StandardCharsets.UTF_8),emitted);
                });
                long ms=SystemClock.elapsedRealtime()-tic;
                int produced=LiquidNative.lastGeneratedTokens();
                String finish=LiquidNative.lastFinishReason();
                boolean natural="eos".equals(finish);
                OcrProposalGuard.Verdict guard=OcrProposalGuard.verify(source,candidate);
                boolean valid=natural&&guard.accepted;
                done++;
                if(valid)ok++;else no++;
                int at=span.raw.indexOf(source);
                String prefix=span.raw.substring(0,at);
                String suffix=span.raw.substring(at+source.length());
                // Preserve leading and trailing whitespace, line breaks and punctuation.
                // OCR remains the authority: proposals are never automatically applied.
                proposal.append(prefix).append(candidate).append(suffix);
                acceptedText.append(prefix).append(valid?candidate:source).append(suffix);
                trace.add(context,"VMR017_TEXT_SEG_"+(index+1),spec.name,
                    "executed",candidate,ms,pss(),
                    "language="+language+" context="+useContext+" tokens="+produced+
                    " budget="+budget+" finish="+finish+" first_token_ms="+firstToken[0]+
                    " token_per_second="+(ms>0?Math.round(produced*1000f/ms):0)+
                    " accepted_candidate="+valid+" guard="+guard.reason);
                memory.observeOriginal(source);
            }
        }
        return new Result(ocr,proposal.toString(),acceptedText.toString(),spec.name,
            spans.size(),done,ok,no,SystemClock.elapsedRealtime()-start,loadMs);
    }
    static String buildPrompt(String source,String lang,int model,String previous){
        StringBuilder p=new StringBuilder();
        if("fr".equals(lang))
            p.append("Corrige uniquement les erreurs OCR certaines de cette phrase française. ")
             .append("Préserve chiffres, noms, dates et références. ")
             .append("Ne crée aucune information. Renvoie exclusivement la phrase corrigée.\n");
        else
            p.append("Fix only certain OCR errors in this English sentence. ")
             .append("Preserve all names, dates, numbers and references. ")
             .append("Add no information. Output ONLY the corrected sentence.\n");
        if(previous!=null&&!previous.isBlank()){
            p.append("fr".equals(lang)?
                "OCR précédent INCERTAIN (contexte seulement, ne pas recopier): ":
                "Previous UNVERIFIED OCR (context only, do not copy): ");
            p.append(previous.substring(0,Math.min(150,previous.length()))).append('\n');
        }
        p.append("fr".equals(lang)?"Texte OCR : ":"OCR source: ").append(source);
        if(model==OfflineCorrectionModels.QWEN_1700)p.append("\n/no_think");
        return p.toString();
    }
    private static float pss(){
        Debug.MemoryInfo m=new Debug.MemoryInfo();Debug.getMemoryInfo(m);
        return m.getTotalPss()/1024f;
    }
}
