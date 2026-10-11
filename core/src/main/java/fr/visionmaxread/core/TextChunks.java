package fr.visionmaxread.core;
import java.util.*;
public final class TextChunks {
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
}
