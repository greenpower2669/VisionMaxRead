package fr.visionmaxread.core;
/** Cancellation scopes for the visible source, edited document and history requests. */
public final class ReaderSessions {
    private final SessionGate documents=new SessionGate();
    private final SessionGate sources=new SessionGate(),history=new SessionGate();
    public long replaceSource(){sources.next();return documents.next();}
    public long editText(){return documents.next();}
    public void clearHistory(){history.next();}
    public long documentTicket(){return documents.snapshot();}
    public long sourceTicket(){return sources.snapshot();}
    public long historyTicket(){return history.snapshot();}
    public boolean documentCurrent(long ticket){return documents.current(ticket);}
    public boolean sourceCurrent(long ticket){return sources.current(ticket);}
    public boolean maskCurrent(long document,long privacy){return documents.current(document)&&history.current(privacy);}
}
