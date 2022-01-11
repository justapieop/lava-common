package me.justapie.lava.common.natives;

import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

public abstract class NativeResourceHolder
{
    private static final Logger log;
    private final AtomicBoolean released;
    
    public NativeResourceHolder() {
        this.released = new AtomicBoolean();
    }
    
    protected void checkNotReleased() {
        if (this.released.get()) {
            throw new IllegalStateException("Cannot use the decoder after closing it.");
        }
    }
    
    public void close() {
        this.closeInternal(false);
    }
    
    protected abstract void freeResources();
    
    private synchronized void closeInternal(final boolean inFinalizer) {
        if (this.released.compareAndSet(false, true)) {
            if (inFinalizer) {
                NativeResourceHolder.log.warn("Should have been closed before finalization ({}).", (Object)this.getClass().getName());
            }
            this.freeResources();
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.closeInternal(true);
    }
    
    static {
        log = LoggerFactory.getLogger((Class)NativeResourceHolder.class);
    }
}
