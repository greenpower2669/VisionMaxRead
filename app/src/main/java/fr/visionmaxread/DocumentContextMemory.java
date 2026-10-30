package fr.visionmaxread;

import java.util.*;

/** Ephemeral per-document context. Unverified OCR is never promoted into a fact. */
final class DocumentContextMemory {
    enum EvidenceKind { OCR_UNVERIFIED, MODEL_HYPOTHESIS, USER_CONFIRMED }
    static final class Observation {
        final String value;
        final EvidenceKind kind;
        Observation(String value,EvidenceKind kind){this.value=value;this.kind=kind;}
    }
    private String previousOcr="";
    private String subject="";
    private final LinkedHashMap<String,Observation> observations=new LinkedHashMap<>();
    private final ArrayList<String> contradictions=new ArrayList<>();
    private int position;
    // Never serialize, persist, log or export raw values without explicit consent.
    String previousSummary(){return clip(previousOcr,150);}
    String confirmedOverview(){return clip(subject,300);}
    int position(){return position;}
    void observeOriginal(String source){
        // This is a quoted OCR observation, not a validated assertion.
        previousOcr=clip(source==null?"":source.strip(),150);
        position++;
    }
    void proposeObservation(String key,String value){
        if(key==null||key.isBlank()||value==null||value.isBlank())return;
        observations.put(key,new Observation(clip(value,150),EvidenceKind.MODEL_HYPOTHESIS));
    }
    void confirmUserObservation(String key,String value){
        if(key==null||key.isBlank()||value==null||value.isBlank())return;
        observations.put(key,new Observation(clip(value,150),EvidenceKind.USER_CONFIRMED));
    }
    void setConfirmedSubject(String approved){
        subject=clip(approved==null?"":approved,300);
    }
    Map<String,Observation> observations(){
        return Collections.unmodifiableMap(new LinkedHashMap<>(observations));
    }
    void addContradiction(String detail){if(detail!=null&&!detail.isBlank()&&contradictions.size()<16)
        contradictions.add(clip(detail,150));}
    List<String> contradictions(){return Collections.unmodifiableList(new ArrayList<>(contradictions));}
    void reset(){
        previousOcr="";subject="";observations.clear();contradictions.clear();position=0;
    }
    private static String clip(String raw,int max){
        String oneLine=raw.replaceAll("[\\r\\n\\t]+"," ").trim();
        return oneLine.length()<=max?oneLine:oneLine.substring(0,max);
    }
}
