package fr.visionmaxread.core;
import java.util.function.BooleanSupplier;
import java.util.concurrent.CancellationException;
/** A speech operation can be cancelled by UI generation or audio focus loss. */
public final class Cancellation {
    private final BooleanSupplier superseded;
    private final java.util.concurrent.atomic.AtomicBoolean stopped=new java.util.concurrent.atomic.AtomicBoolean();
    public Cancellation(BooleanSupplier superseded){this.superseded=superseded;}
    public void cancel(){stopped.set(true);}
    public boolean cancelled(){return stopped.get()||superseded.getAsBoolean();}
    public void check(){if(cancelled())throw new CancellationException();}
}
