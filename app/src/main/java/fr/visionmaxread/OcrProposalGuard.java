package fr.visionmaxread;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Conservative, independent OCR proposal filter. Never treats plausible length as proof. */
final class OcrProposalGuard {
    static final class Verdict{
        final boolean accepted;
        final String reason;
        Verdict(boolean accepted,String reason){this.accepted=accepted;this.reason=reason;}
    }
    private static final Pattern WORDS=Pattern.compile("[\\p{L}]{2,}");
    private static final Pattern NUMBERS=Pattern.compile("\\d+(?:[.,/\\-]\\d+)*");
    private static final Pattern SUSPICIOUS=Pattern.compile(
        "(?i)(cite this article as|references?:|bibliography|as an ai|\\bsource:\\b|\\[\\d+\\]|<\\|[^>]+\\|>|^\\s*#|\\bI cannot\\b)");
    private OcrProposalGuard(){}
    static Verdict verify(String original,String suggestion){
        if(original==null||original.isBlank()||suggestion==null||suggestion.isBlank())
            return reject("empty_source_or_proposal");
        String candidate=suggestion.trim();
        if(candidate.indexOf('\n')>=0||candidate.indexOf('\r')>=0)return reject("unexpected_multiline_output");
        if(SUSPICIOUS.matcher(candidate).find()||candidate.contains("```"))
            return reject("off_topic_instruction_or_citation");
        if(!numericTokens(original).equals(numericTokens(candidate)))
            return reject("numbers_or_references_changed");
        if(candidate.length()<Math.max(2,original.length()/2)||
           candidate.length()>original.length()*17/10+20)
            return reject("unexpected_length");
        List<String> a=words(original),b=words(candidate);
        if(a.isEmpty()||b.isEmpty())return reject("insufficient_lexical_evidence");
        // Each output content word must resemble a source word. This deliberately
        // rejects uncertain OCR repairs rather than inventing names or citations.
        int matches=0;
        for(String token:b){
            boolean plausible=false;
            for(String src:a){
                if(token.equals(src)||near(token,src)){plausible=true;break;}
            }
            if(plausible)matches++;
        }
        if(matches*100<b.size()*70)return reject("unsupported_lexical_additions");
        if(repetitive(b))return reject("degenerate_repetition");
        // Preserve capitalized interior entities where the OCR spelling is legible.
        Matcher names=Pattern.compile("(?U)\\b[\\p{Lu}][\\p{L}]{2,}\\b").matcher(original);
        while(names.find()){
            if(names.start()==0)continue;
            String entity=names.group().toLowerCase(Locale.ROOT);
            if(!b.contains(normalize(entity)))return reject("proper_name_changed");
        }
        return new Verdict(true,"lexically_supported_candidate_manual_review");
    }
    private static Verdict reject(String why){return new Verdict(false,why);}
    private static List<String> numericTokens(String s){
        ArrayList<String> o=new ArrayList<>();
        Matcher m=NUMBERS.matcher(s);
        while(m.find())o.add(m.group());
        return o;
    }
    private static String normalize(String s){
        return Normalizer.normalize(s,Normalizer.Form.NFKD).replaceAll("\\p{M}+","")
            .toLowerCase(Locale.ROOT);
    }
    private static List<String> words(String s){
        ArrayList<String> result=new ArrayList<>();
        Matcher m=WORDS.matcher(s);
        while(m.find())result.add(normalize(m.group()));
        return result;
    }
    private static boolean near(String a,String b){
        if(Math.abs(a.length()-b.length())>Math.max(1,Math.min(a.length(),b.length())/3))return false;
        int allowed=Math.max(1,Math.min(2,Math.max(a.length(),b.length())/3));
        int[] d=new int[b.length()+1];
        for(int j=0;j<=b.length();j++)d[j]=j;
        for(int i=1;i<=a.length();i++){
            int diag=d[0];d[0]=i;
            for(int j=1;j<=b.length();j++){
                int old=d[j];
                d[j]=Math.min(Math.min(d[j]+1,d[j-1]+1),
                    diag+(a.charAt(i-1)==b.charAt(j-1)?0:1));
                diag=old;
            }
        }
        return d[b.length()]<=allowed;
    }
    private static boolean repetitive(List<String> words){
        for(int i=3;i<words.size();i++){
            if(words.get(i).equals(words.get(i-1))&&words.get(i).equals(words.get(i-2))
               &&words.get(i).equals(words.get(i-3)))return true;
        }
        for(int i=0;i+8<words.size();i++){
            boolean repeat=true;
            for(int k=0;k<3;k++)
                if(!words.get(i+k).equals(words.get(i+k+3))||
                   !words.get(i+k).equals(words.get(i+k+6)))repeat=false;
            if(repeat)return true;
        }
        return false;
    }
}
