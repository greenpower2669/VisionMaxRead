package fr.visionmaxread;
import java.util.*;

/**
 * Compact Java port of the Indra CTC prefix beam search, based on
 * https://github.com/datalab-to/indra/blob/main/indra_ocr/lm.py
 * Line-specific: it never changes certain OCR observations without model evidence.
 */
final class IndraCtcBeam {
    private static final double NEG=Double.NEGATIVE_INFINITY;
    private static final double LOG_FLOOR=Math.log(.003),LOG_SKIP=Math.log(.999);
    private static final int WIDTH=8;
    private IndraCtcBeam(){}
    static final class Hypothesis {
        final int[] tokens;final double score;
        Hypothesis(int[] tokens,double score){this.tokens=tokens;this.score=score;}
    }
    private static final class State {
        double blank=NEG,nonblank=NEG; final double lmScore;
        State(double b,double n,double score){blank=b;nonblank=n;lmScore=score;}
        double rank(){return logsum(blank,nonblank)+lmScore;}
    }
    static Hypothesis decode(double[] blank,double[][] topv,long[][] topi,IndraNgramLM lm,
                             double alpha,double beta){
        if(blank.length==0||blank.length>4096||blank.length!=topv.length||blank.length!=topi.length)
            throw new IllegalArgumentException("Indra beam dimensions");
        Map<String,State> beams=new HashMap<>();
        beams.put("",new State(0,NEG,0));
        for(int t=0;t<blank.length;t++){
            if(blank[t]>LOG_SKIP){
                for(State b:beams.values()){b.blank=logsum(b.blank,b.nonblank)+blank[t];b.nonblank=NEG;}
                continue;
            }
            Map<String,State> next=new HashMap<>();
            int choices=Math.min(topv[t].length,topi[t].length);
            for(Map.Entry<String,State> entry:beams.entrySet()){
                String prefix=entry.getKey();State v=entry.getValue();
                put(next,prefix,logsum(v.blank,v.nonblank)+blank[t],NEG,v.lmScore);
                int prev=prefix.isEmpty()?-1:prefix.charAt(prefix.length()-1)-1;
                for(int k=0;k<choices;k++){
                    long token=topi[t][k];double logP=topv[t][k];
                    if(token<=0||token>65534||logP<LOG_FLOOR||!Double.isFinite(logP))continue;
                    int c=(int)token;
                    if(c==prev){
                        // Repeated same CTC class can preserve prefix, or extend
                        // prefix if and only if previous alignment was blank.
                        put(next,prefix,NEG,v.nonblank+logP,v.lmScore);
                    }
                    double from=c==prev?v.blank:logsum(v.blank,v.nonblank);
                    if(from==NEG)continue;
                    String extended=prefix+(char)(c+1);
                    int[] hist=new int[prefix.length()];
                    for(int i=0;i<hist.length;i++)hist[i]=prefix.charAt(i)-1;
                    double language=lm.logp(hist,c);
                    put(next,extended,NEG,from+logP,v.lmScore+alpha*language+beta);
                }
            }
            List<Map.Entry<String,State>> ordered=new ArrayList<>(next.entrySet());
            ordered.sort((a,b)->Double.compare(b.getValue().rank(),a.getValue().rank()));
            beams.clear();
            for(int i=0;i<Math.min(WIDTH,ordered.size());i++)beams.put(ordered.get(i).getKey(),ordered.get(i).getValue());
            if(beams.isEmpty())throw new IllegalArgumentException("Empty CTC beam");
        }
        Map.Entry<String,State> best=Collections.max(beams.entrySet(),Comparator.comparingDouble(a->a.getValue().rank()));
        String s=best.getKey();int[] ids=new int[s.length()];
        for(int i=0;i<ids.length;i++)ids[i]=s.charAt(i)-1;
        return new Hypothesis(ids,best.getValue().rank());
    }
    private static void put(Map<String,State> map,String prefix,double blank,double nonblank,double score){
        State old=map.get(prefix);
        if(old==null)map.put(prefix,new State(blank,nonblank,score));
        else{old.blank=logsum(old.blank,blank);old.nonblank=logsum(old.nonblank,nonblank);}
    }
    private static double logsum(double a,double b){
        if(a==NEG)return b;if(b==NEG)return a;
        double m=Math.max(a,b);return m+Math.log(Math.exp(a-m)+Math.exp(b-m));
    }
}
