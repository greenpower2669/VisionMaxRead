package fr.visionmaxread;

import java.util.*;

/** Non-destructive OCR line planner: bounded, word-aligned and source-offset based. */
final class MoeSegmentPlanner {
    static final int MAX_CHARACTERS=150, DEFAULT_SEGMENTS=12, HARD_SEGMENTS=40;
    static final class Segment {
        final int start,end;
        Segment(int start,int end){this.start=start;this.end=end;}
        String source(String original){return original.substring(start,end);}
    }
    private MoeSegmentPlanner(){}
    static List<Segment> plan(String original,int limit){
        if(original==null)throw new IllegalArgumentException("Null OCR");
        if(limit<1||limit>HARD_SEGMENTS)throw new IllegalArgumentException("Segment limit");
        ArrayList<Segment> segments=new ArrayList<>();
        int lineBegin=0;
        while(lineBegin<original.length()&&segments.size()<limit){
            int lineEnd=original.indexOf('\n',lineBegin);
            if(lineEnd<0)lineEnd=original.length();
            int pos=lineBegin;
            while(pos<lineEnd&&segments.size()<limit){
                while(pos<lineEnd&&Character.isWhitespace(original.charAt(pos)))pos++;
                if(pos>=lineEnd)break;
                int hard=Math.min(lineEnd,pos+MAX_CHARACTERS);
                if(hard<lineEnd&&Character.isLowSurrogate(original.charAt(hard)))hard--;
                int end=hard;
                if(end<lineEnd){
                    // Prefer not to divide dates, administrative numbers or words.
                    int natural=-1;
                    for(int i=end;i>pos+MAX_CHARACTERS/3;i--){
                        if(Character.isWhitespace(original.charAt(i-1))){natural=i-1;break;}
                    }
                    if(natural>pos)end=natural;
                    else {
                        // An uninterrupted token too long for safe correction is
                        // preserved unchanged, not split into misleading fragments.
                        int next=end;
                        while(next<lineEnd&&!Character.isWhitespace(original.charAt(next)))next++;
                        pos=next;
                        continue;
                    }
                }
                while(end>pos&&Character.isWhitespace(original.charAt(end-1)))end--;
                if(end>pos)segments.add(new Segment(pos,end));
                pos=Math.max(end,pos+1);
            }
            lineBegin=lineEnd+1;
        }
        return Collections.unmodifiableList(segments);
    }
}
