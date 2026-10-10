package fr.visionmaxread.core;
public final class SessionGate {
    private final java.util.concurrent.atomic.AtomicLong generation=new java.util.concurrent.atomic.AtomicLong();
    public long next() { return generation.incrementAndGet(); }
    public boolean current(long ticket) { return generation.get()==ticket; }
}
