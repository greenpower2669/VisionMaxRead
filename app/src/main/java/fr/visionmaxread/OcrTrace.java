package fr.visionmaxread;
import android.content.Context;
import android.os.SystemClock;
import org.json.*;
import java.util.*;

/**
 * Per-document, in-memory OCR evidence. DebugLog contains only safe metrics.
 * Full texts are exposed only after a user explicitly opts into copying them.
 */
final class OcrTrace {
    static final class Pass {
        final String key,engine,status,text,detail;
        final long ms;final float pss;
        Pass(String key,String engine,String status,String text,long ms,float pss,String detail){
            this.key=key;this.engine=engine;this.status=status;this.text=text==null?"":text;
            this.ms=ms;this.pss=pss;this.detail=detail;
        }
    }
    private final List<Pass> passes=new ArrayList<>();
    private final String sessionTag;
    OcrTrace(String tag){sessionTag=tag;}
    synchronized void add(Context context,String key,String engine,String status,String text,long ms,float pss,String detail) {
        Pass pass=new Pass(key,engine,status,text,ms,pss,detail);
        passes.add(pass);
        DebugLog.append(context,"PIPELINE stage="+key+" engine="+engine+" outcome="+status+
            " ms="+ms+" chars="+pass.text.length()+" pssMb="+String.format(Locale.ROOT,"%.1f",pss)+
            " note="+detail.substring(0,Math.min(120,detail.length())).replaceAll("[^a-zA-Z0-9_.=:-]","_"));
    }
    /** Immutable snapshot for the UI: a running semantic pass must not mutate a dialog list. */
    synchronized List<Pass> snapshot(){return new ArrayList<>(passes);}
    synchronized String passReport(int index){
        if(index<0||index>=passes.size())throw new IndexOutOfBoundsException("pass");
        return "VISIONMAXREAD VMR-013 — PASSE SENSIBLE (partage volontaire)\n"+
            "session="+sessionTag+"\n"+
            "Attention : contient des textes OCR potentiellement privés. Anonymiser avant partage.\n\n"+
            render(passes.get(index));
    }
    synchronized String metricsReport(){
        StringBuilder b=new StringBuilder("VISIONMAXREAD VMR-013 — LOG TECHNIQUE SANS TEXTE\n")
            .append("session=").append(sessionTag).append("\n")
            .append("Les durées, statuts et noms de moteurs ne sont pas des sorties neuronales.\n\n");
        for(Pass p:passes){
            b.append(p.key).append(" engine=").append(p.engine)
            .append(" status=").append(p.status).append(" time_ms=").append(p.ms)
            .append(" PSS_MB=").append(String.format(Locale.ROOT,"%.1f",p.pss))
            .append(" chars=").append(p.text.length()).append("\n");
        }
        return b.toString();
    }
    private static String render(Pass p){
        StringBuilder b=new StringBuilder("=== ").append(p.key).append(" ===\n")
            .append("engine=").append(p.engine).append(" status=").append(p.status)
            .append(" time_ms=").append(p.ms).append(" PSS_MB=")
            .append(String.format(Locale.ROOT,"%.1f",p.pss))
            .append(" chars=").append(p.text.length()).append("\n")
            .append("note=").append(p.detail).append("\n");
        if(p.status.equals("executed"))b.append(p.text);
        else b.append("[étape non exécutée : ").append(p.status).append("]");
        return b.append("\n").toString();
    }
    synchronized String summary(){
        StringBuilder b=new StringBuilder("Passes OCR — "+passes.size()+" étapes\n");
        for(Pass p:passes)b.append(p.key).append(" | ").append(p.engine).append(" | ")
                .append(p.status).append(" | ").append(p.ms).append(" ms | ")
                .append(p.text.length()).append(" caractères | ")
                .append(String.format(Locale.FRANCE,"%.0f Mo",p.pss)).append("\n");
        return b.toString();
    }
    synchronized String fullReport(){
        StringBuilder b=new StringBuilder("VISIONMAXREAD VMR-013 — RAPPORT SENSIBLE (partage volontaire)\n")
            .append("session=").append(sessionTag).append("\n")
            .append("Attention : contient les textes reconnus, relire/anonymiser AVANT tout partage.\n\n");
        for(Pass p:passes)b.append(render(p)).append("\n");
        return b.toString();
    }
}
