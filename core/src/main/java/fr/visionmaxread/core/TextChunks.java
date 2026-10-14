package fr.visionmaxread.core;
import java.util.*;
public final class TextChunks {
    private TextChunks() {}

    /** Original exact-lossless text splitter, used by historical contracts. */
    public static List<String> split(String text, int limit) {
        if(limit<2)throw new IllegalArgumentException("Chunk limit too small");
        List<String> out=new ArrayList<>();int start=0;
        while(start<text.length()) {
            int end=text.offsetByCodePoints(start,Math.min(limit,text.codePointCount(start,text.length())));
            if(end<text.length()) {
                int preferred=-1;
                for(int i=start;i<end;i++)if(Character.isWhitespace(text.charAt(i))) {
                    if(i>start && ".!?;\n".indexOf(text.charAt(i-1))>=0)preferred=i+1;
                }
                if(preferred<start+(end-start)/3) {
                    for(int i=end-1;i>start+(end-start)/2;i--)if(Character.isWhitespace(text.charAt(i))){preferred=i+1;break;}
                }
                if(preferred>start)end=preferred;
            }
            out.add(text.substring(start,end));start=end;
        }
        return out;
    }

    /** User-facing speech segments. The pause is only for a real paragraph break. */
    public record SpeechSegment(String text,int pauseAfterMs) {
        public SpeechSegment {
            if(text==null || text.isBlank() || pauseAfterMs<0)throw new IllegalArgumentException();
        }
    }

    /**
     * Prioritizes sentence endings, then punctuation, and only then whitespace.
     * Never cuts inside a word or UTF-16 surrogate pair. Single newlines in OCR
     * text are treated as layout wraps; blank lines are spoken as paragraphs.
     * This is a speech representation, not a substitute for the original text.
     */
    public static List<SpeechSegment> speech(String source,int maxCodePoints) {
        if(maxCodePoints<32)throw new IllegalArgumentException("Speech chunk limit too small");
        if(source==null || source.isBlank())return List.of();
        String normalized=source.replace("\r\n","\n").replace('\r','\n').trim();
        String[] blocks=normalized.split("\\n[ \\t\\f]*\\n+");
        ArrayList<String> paragraphs=new ArrayList<>();
        for(String block:blocks) {
            String clean=block.replaceAll("\\s+"," ").trim();
            if(!clean.isEmpty())paragraphs.add(clean);
        }
        ArrayList<SpeechSegment> out=new ArrayList<>();
        for(int p=0;p<paragraphs.size();p++) {
            String paragraph=paragraphs.get(p);
            ArrayList<String> pieces=new ArrayList<>();
            int start=0;
            while(start<paragraph.length()) {
                while(start<paragraph.length() && Character.isWhitespace(paragraph.charAt(start)))start++;
                if(start>=paragraph.length())break;
                int count=paragraph.codePointCount(start,paragraph.length());
                int end=count<=maxCodePoints ? paragraph.length()
                    : paragraph.offsetByCodePoints(start,maxCodePoints);
                if(end<paragraph.length())end=naturalCut(paragraph,start,end);
                if(end<=start)throw new IllegalStateException("Speech segment did not advance");
                String piece=paragraph.substring(start,end).trim();
                if(!piece.isEmpty())pieces.add(piece);
                start=end;
            }
            for(int i=0;i<pieces.size();i++) {
                boolean paragraphEnd=i==pieces.size()-1 && p<paragraphs.size()-1;
                out.add(new SpeechSegment(pieces.get(i),paragraphEnd?180:0));
            }
        }
        return List.copyOf(out);
    }

    private static int naturalCut(String s,int start,int hardEnd) {
        int sentence=-1,clause=-1,space=-1;
        for(int i=start+1;i<hardEnd;i++) {
            char c=s.charAt(i);
            if(Character.isWhitespace(c))space=i;
            if(".!?…;,:".indexOf(c)<0)continue;
            if(c=='.' && isAbbreviation(s,start,i))continue;
            int next=i+1;
            while(next<s.length() && "\"'»”’)]}".indexOf(s.charAt(next))>=0)next++;
            if(next<s.length() && !Character.isWhitespace(s.charAt(next)))continue;
            if(i-start<10)continue;
            if(".!?…".indexOf(c)>=0)sentence=next;
            else clause=next;
        }
        if(sentence>start)return sentence;
        if(clause>start)return clause;
        if(space>start)return space;
        // An uninterrupted giant token: ensure we do not bisect an emoji surrogate.
        if(hardEnd<s.length() && Character.isLowSurrogate(s.charAt(hardEnd)))
            return hardEnd-1;
        return hardEnd;
    }

    private static boolean isAbbreviation(String s,int start,int period) {
        int i=period-1;
        while(i>=start && Character.isLetter(s.charAt(i)))i--;
        String word=s.substring(i+1,period).toLowerCase(Locale.ROOT);
        return word.length()==1 || Set.of("mme","mlle","mmes","mr","m","dr","pr",
            "st","ste","etc","art","ex","av","env","vol","n").contains(word);
    }
}
