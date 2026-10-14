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

        String article="M. Martin retrouve Mme Dubois à 14 h 30. Nous avons lu le texte ensemble, mais la phrase reste assez longue pour être découpée naturellement. Quelle belle nouvelle !";
        List<TextChunks.SpeechSegment> spoken=TextChunks.speech(article,70);
        check(spoken.size()>=2,"semantic speech has multiple segments");
        check(String.join(" ",spoken.stream().map(TextChunks.SpeechSegment::text).toList()).equals(article),"speech plan preserves words and punctuation");
        check(spoken.stream().noneMatch(x->x.text().equals("M.") || x.text().equals("Mme")),"abbreviations remain with their names");
        for(var segment:spoken) {
            check(segment.text().codePointCount(0,segment.text().length())<=70,"semantic speech limit");
            check(!Character.isHighSurrogate(segment.text().charAt(segment.text().length()-1)),"semantic speech keeps UTF16 pairs");
        }
        var paragraphs=TextChunks.speech("Premier paragraphe avec une idée entière.\n\nDeuxième paragraphe, sans interruption.",320);
        check(paragraphs.size()==2 && paragraphs.get(0).pauseAfterMs()==180 && paragraphs.get(1).pauseAfterMs()==0,"real paragraphs get a natural pause");
        var wrapped=TextChunks.speech("Bonjour, voici une phrase\nretournée automatiquement à la ligne.",320);
        check(wrapped.size()==1 && wrapped.get(0).text().contains("phrase retournée"),"OCR line wraps do not force voice restarts");
        check(TextChunks.speech("   \n\t ",320).isEmpty(),"empty speech plan");
        boolean tinySpeech=false;try{TextChunks.speech("bonjour",3);}catch(IllegalArgumentException e){tinySpeech=true;}
        check(tinySpeech,"reject tiny speech segment limit");
        for(var segment:TextChunks.speech("😀Bonjour le monde. ".repeat(50),140))
            check(!Character.isLowSurrogate(segment.text().charAt(0)) && !Character.isHighSurrogate(segment.text().charAt(segment.text().length()-1)),"emoji voice boundaries");
        SessionGate gate=new SessionGate();long first=gate.next();check(gate.current(first),"new ticket");gate.next();check(!gate.current(first),"stale callback");
        WordPiece wp=new WordPiece(List.of("[UNK]","[CLS]","[SEP]","Été","Jean","Du","##pont",",","中"));
        List<WordPiece.Token> tokens=wp.tokenize("Été Jean Dupont, 中 😀");
        check(tokens.size()==7,"cased wordpieces and Chinese punctuation");
        check(tokens.get(0).id==3 && tokens.get(3).id==6,"accents and greedy subword");
        check(tokens.get(2).start==9 && tokens.get(3).end==15,"whole-word original offsets");
        check(tokens.get(6).end-tokens.get(6).start==2,"unknown emoji offsets");
        Cancellation operation=new Cancellation(()->false);operation.cancel();
        check(operation.cancelled(),"audio focus loss cancels synthesis and writes even while source is current");
        boolean cancelled=false;try{operation.check();}catch(java.util.concurrent.CancellationException e){cancelled=true;}
        check(cancelled,"next inference step cannot run after focus loss");
        ReaderSessions sessions=new ReaderSessions();long document=sessions.replaceSource(),source=sessions.sourceTicket(),privacy=sessions.historyTicket();
        sessions.clearHistory();
        check(sessions.documentCurrent(document),"clear history preserves current text");
        check(sessions.sourceCurrent(source),"clear history preserves live camera capture");
        check(!sessions.maskCurrent(document,privacy),"clear invalidates pending masked save");
        check(sessions.maskCurrent(sessions.documentTicket(),sessions.historyTicket()),"explicit save works after clear or disabling auto history");
        sessions.editText();check(sessions.sourceCurrent(source),"text edits leave an explicit camera freeze valid");
        check(!sessions.documentCurrent(document),"text edits invalidate old OCR/masking result");
        sessions.replaceSource();check(!sessions.sourceCurrent(source),"source switch drops old camera frame");
        System.out.println("PASS "+checks+" contract checks");
    }
}
