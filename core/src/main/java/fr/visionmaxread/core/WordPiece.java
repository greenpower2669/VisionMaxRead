package fr.visionmaxread.core;
import java.util.*;
/** Cased BERT basic + WordPiece tokenizer, retaining offsets in the original UTF16 text. */
public final class WordPiece {
    public static final class Token {
        public final long id; public final int start,end;
        Token(long id,int start,int end){this.id=id;this.start=start;this.end=end;}
    }
    private final Map<String,Integer> vocab=new HashMap<>();
    public WordPiece(List<String> vocabulary){for(int i=0;i<vocabulary.size();i++)vocab.put(vocabulary.get(i),i);}
    public long id(String token){return vocab.getOrDefault(token,vocab.getOrDefault("[UNK]",100));}
    public List<Token> tokenize(String text){
        List<Token> result=new ArrayList<>();int i=0;
        while(i<text.length()) {
            int cp=text.codePointAt(i),type=Character.getType(cp),start=i;
            i+=Character.charCount(cp);
            if(Character.isWhitespace(cp)||type==Character.CONTROL||type==Character.FORMAT)continue;
            if(word(cp)&&!chinese(cp))while(i<text.length()) {
                int next=text.codePointAt(i);if(!word(next)||chinese(next))break;i+=Character.charCount(next);
            }
            String w=text.substring(start,i);List<Long> ids=new ArrayList<>();int pos=0;
            if(w.codePointCount(0,w.length())<=100)while(pos<w.length()) {
                int end=w.length();Integer found=null;
                while(end>pos) {
                    found=vocab.get((pos==0?"":"##")+w.substring(pos,end));
                    if(found!=null)break;
                    end=w.offsetByCodePoints(end,-1);
                }
                if(found==null){ids.clear();break;}ids.add((long)found);pos=end;
            }
            if(ids.isEmpty())ids.add(id("[UNK]"));
            for(long token:ids)result.add(new Token(token,start,i));
        }
        return result;
    }
    private static boolean word(int c){int t=Character.getType(c);return Character.isLetterOrDigit(c)||t==Character.NON_SPACING_MARK||t==Character.COMBINING_SPACING_MARK;}
    private static boolean chinese(int c){return (c>=0x4e00&&c<=0x9fff)||(c>=0x3400&&c<=0x4dbf)||(c>=0x20000&&c<=0x2fa1f);}
}
