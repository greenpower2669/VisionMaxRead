package fr.visionmaxread.core;
import java.util.*;
public final class Contracts {
    static int checks;
    static void check(boolean value, String message) { checks++;if(!value)throw new AssertionError(message); }
    public static void main(String[] args) {
        String original="Bonjour Exemple, écris à test@example.invalid ou au 06 12 34 56 78. Merci.";
        String masked=PrivacyRules.mask(original,List.of(new PrivacyRules.Span(8,15,"PERSONNE")));
        check(masked.contains("[PERSONNE]"),"entity must be replaced");
        check(!masked.contains("test@") && !masked.contains("06 12"),"contact details must be masked");
        check(masked.startsWith("Bonjour ") && masked.endsWith(" Merci."),"preserve readable context");
        check(!PrivacyRules.mask("IBAN FR76 3000 6000 0112 3456 7890 189",List.of()).contains("FR76"),"IBAN");
        check(!PrivacyRules.mask("Au 12 rue des Exemples, le 15/04/1980.",List.of()).contains("Exemples"),"postal address");
        check(PrivacyRules.mask("😀Jean Dupont parle",List.of(new PrivacyRules.Span(2,6,"PERSONNE"),new PrivacyRules.Span(2,13,"PERSONNE"))).equals("😀[PERSONNE] parle"),"overlaps and UTF16 offsets");
        boolean invalid=false;try{PrivacyRules.mask("court",List.of(new PrivacyRules.Span(0,90,"PERSONNE")));}catch(IllegalArgumentException e){invalid=true;}
        check(invalid,"invalid entity cannot produce a supposedly safe record");
        String text="Été ! "+"😀Bonjour le monde. ".repeat(40);
        List<String> chunks=TextChunks.split(text,80);
        check(String.join("",chunks).equals(text),"chunking loses no character");
        check(chunks.size()>1,"must chunk");
        for(String chunk:chunks){check(chunk.codePointCount(0,chunk.length())<=80,"chunk limit");check(!Character.isHighSurrogate(chunk.charAt(chunk.length()-1)),"no split surrogate");}
        check(TextChunks.split("",80).isEmpty(),"empty text");
        SessionGate gate=new SessionGate();long first=gate.next();check(gate.current(first),"new ticket");gate.next();check(!gate.current(first),"stale callback");
        WordPiece wp=new WordPiece(List.of("[UNK]","[CLS]","[SEP]","Été","Jean","Du","##pont",",","中"));
        List<WordPiece.Token> tokens=wp.tokenize("Été Jean Dupont, 中 😀");
        check(tokens.size()==7,"cased wordpieces and Chinese punctuation");
        check(tokens.get(0).id==3 && tokens.get(3).id==6,"accents and greedy subword");
        check(tokens.get(2).start==9 && tokens.get(3).end==15,"whole-word original offsets");
        check(tokens.get(6).end-tokens.get(6).start==2,"unknown emoji offsets");
        System.out.println("PASS "+checks+" contract checks");
    }
}
