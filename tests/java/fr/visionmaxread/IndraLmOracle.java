package fr.visionmaxread;
import java.io.*;
/** Synthetic fixture, no private OCR data and no Android dependency. */
public final class IndraLmOracle {
    public static void main(String[] args)throws Exception{
        try(FileInputStream input=new FileInputStream(args[0])){
            IndraNgramLM lm=IndraNgramLM.load(input);
            if(lm.order!=2||lm.bits!=11||lm.vocabSize!=3)throw new AssertionError("Metadata");
            assertClose(lm.logp(new int[]{0},1),-.01,"bigram");
            assertClose(lm.logp(new int[]{0},2),-1.65,"backoff");
            assertClose(lm.logp(new int[]{},0),-2.0,"unigram");
            double[] blank={Math.log(.10),Math.log(.10)};
            double[][] v={{Math.log(.85),Math.log(.05)},{Math.log(.88),Math.log(.02)}};
            long[][] ids={{1,2},{2,1}};
            IndraCtcBeam.Hypothesis out=IndraCtcBeam.decode(blank,v,ids,lm,.5,1.5);
            if(out.tokens.length==0)throw new AssertionError("No CTC tokens");
            System.out.println("LM ORACLE PASS order=2 CTC="+out.tokens.length);
        }
    }
    private static void assertClose(double got,double expected,String label){
        if(Math.abs(got-expected)>1e-5)throw new AssertionError(label+" got "+got+" expected "+expected);
    }
}
