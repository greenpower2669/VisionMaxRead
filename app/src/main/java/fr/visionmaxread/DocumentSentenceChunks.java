package fr.visionmaxread;
import java.util.*;

/** Lossless sentence views over the OCR source. No text rewritten or persisted. */
final class DocumentSentenceChunks {
    static final class Segment{
        final int start,end;
        final String raw;
        Segment(int start,int end,String raw){this.start=start;this.end=end;this.raw=raw;}
    }
    private static final Set<String> ABBREV=Set.of("m","mme","mlle","mm","mmes",
        "mr","mrs","dr","pr","st","ste","etc","art","ex","av","env","vol",
        "n","no","n°","prof","janv","févr","sept","oct","nov","déc","vs","e.g","i.e");
    private DocumentSentenceChunks(){}
    static List<Segment> split(String source){
        if(source==null||source.isEmpty())return List.of();
        ArrayList<Segment> pieces=new ArrayList<>();
        int start=0;
        for(int i=0;i<source.length();i++){
            char c=source.charAt(i);
            if(c=='\r')continue;
            if(c=='\n'&&i>start&&source.charAt(i-1)=='\n'){
                int end=i+1;pieces.add(new Segment(start,end,source.substring(start,end)));
                start=end;continue;
            }
            if(".!?…".indexOf(c)<0)continue;
            if(c=='.' && (abbreviation(source,start,i)||numericPeriod(source,i)
                         ||source.substring(Math.max(0,i-5),i).contains("://")))continue;
            int end=i+1;
            while(end<source.length()&&"\"'»”’)".indexOf(source.charAt(end))>=0)end++;
            if(end<source.length()&&!Character.isWhitespace(source.charAt(end)))continue;
            if(end-start<2)continue;
            // Include adjacent whitespace with the previous fragment; concatenation remains exact.
            while(end<source.length()&&Character.isWhitespace(source.charAt(end)))end++;
            pieces.add(new Segment(start,end,source.substring(start,end)));
            start=end;i=end-1;
        }
        if(start<source.length())pieces.add(new Segment(start,source.length(),source.substring(start)));
        return Collections.unmodifiableList(pieces);
    }
    private static boolean numericPeriod(String s,int at){
        return at>0&&at+1<s.length()&&Character.isDigit(s.charAt(at-1))&&Character.isDigit(s.charAt(at+1));
    }
    private static boolean abbreviation(String s,int start,int at){
        int p=at-1;while(p>=start&&Character.isLetter(s.charAt(p)))p--;
        String word=s.substring(p+1,at).toLowerCase(Locale.ROOT);
        return word.length()==1||ABBREV.contains(word);
    }
}
