package fr.visionmaxread.core;
import java.util.*;
import java.util.regex.*;
public final class PrivacyRules {
    public static final class Span {
        public final int start, end; public final String label;
        public Span(int start, int end, String label) { this.start=start;this.end=end;this.label=label; }
    }
    private static final String[][] RULES = {
        {"COURRIEL", "[\\p{L}\\p{N}._%+\\-]+@[\\p{L}\\p{N}.\\-]+\\.[\\p{L}]{2,}"},
        {"IBAN", "(?i)\\b[A-Z]{2}\\d{2}(?:[ \\u00a0]?[A-Z0-9]){11,30}\\b"},
        {"TÉLÉPHONE", "(?<!\\d)(?:(?:\\+|00)33[ .\\-]?(?:\\(0\\)[ .\\-]?)?|0)[1-9](?:[ .\\-]?\\d{2}){4}(?!\\d)"},
        {"NUMÉRO", "(?<!\\d)\\d(?:[ \\u00a0.\\-]?\\d){8,18}(?!\\d)"},
        {"DATE", "\\b(?:0?[1-9]|[12]\\d|3[01])[/.\\-](?:0?[1-9]|1[0-2])[/.\\-](?:\\d{4}|\\d{2})\\b"},
        {"ADRESSE", "(?iu)\\b\\d{1,4}\\s*(?:bis|ter)?\\s+(?:rue|avenue|boulevard|chemin|impasse|allée|place|route|quai)\\s+[^\\n,;.!?]{2,100}"},
        {"LIEN", "(?i)https?://[^\\s<>]+"}
    };
    public static String mask(String text, List<Span> entities) {
        Objects.requireNonNull(text); Objects.requireNonNull(entities);
        List<Span> spans = new ArrayList<>(entities);
        for (Span s: spans) {
            if (s.start<0 || s.end>safelen(text) || s.start>=s.end || s.label==null || !s.label.matches("[A-ZÉ]+"))
                throw new IllegalArgumentException("Invalid masking span");
            if (s.start>0 && Character.isLowSurrogate(text.charAt(s.start)) || s.end<text.length() && Character.isLowSurrogate(text.charAt(s.end)))
                throw new IllegalArgumentException("Split Unicode character");
        }
        for(String[] rule:RULES) {
            Matcher m=Pattern.compile(rule[1]).matcher(text);
            while(m.find())spans.add(new Span(m.start(),m.end(),rule[0]));
        }
        spans.sort(Comparator.comparingInt((Span s)->s.start).thenComparingInt(s->-s.end));
        List<Span> merged=new ArrayList<>();
        for(Span s:spans) {
            if(!merged.isEmpty() && s.start<merged.get(merged.size()-1).end) {
                Span prev=merged.remove(merged.size()-1);
                merged.add(new Span(prev.start,Math.max(prev.end,s.end),prev.label));
            } else merged.add(s);
        }
        StringBuilder out=new StringBuilder();int pos=0;
        for(Span s:merged){out.append(text,pos,s.start).append('[').append(s.label).append(']');pos=s.end;}
        return out.append(text,pos,text.length()).toString();
    }
    private static int safelen(String s){return s.length();}
}
