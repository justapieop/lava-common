package me.justapie.lava.common.tools;

import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory implements ThreadFactory
{
    private static final Logger log;
    private static final AtomicInteger poolNumber;
    private final ThreadGroup group;
    private final AtomicInteger threadNumber;
    private final String namePrefix;
    private final Runnable exitCallback;
    
    public DaemonThreadFactory(final String name) {
        this(name, null);
    }
    
    public DaemonThreadFactory(final String name, final Runnable exitCallback) {
        this.threadNumber = new AtomicInteger(1);
        final SecurityManager securityManager = System.getSecurityManager();
        this.group = ((securityManager != null) ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup());
        this.namePrefix = "lava-daemon-pool-" + name + "-" + DaemonThreadFactory.poolNumber.getAndIncrement() + "-thread-";
        this.exitCallback = exitCallback;
    }
    
    @Override
    public Thread newThread(final Runnable runnable) {
        final Thread thread = new Thread(this.group, this.getThreadRunnable(runnable), this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
        thread.setDaemon(true);
        thread.setPriority(5);
        return thread;
    }
    
    private Runnable getThreadRunnable(final Runnable target) {
        if (this.exitCallback == null) {
            return target;
        }
        return new ExitCallbackRunnable(target);
    }
    
    static {
        log = LoggerFactory.getLogger((Class)DaemonThreadFactory.class);
        poolNumber = new AtomicInteger(1);
    }
    
    private class ExitCallbackRunnable implements Runnable
    {
        private final Runnable original;
        
        private ExitCallbackRunnable(final Runnable original) {
            this.original = original;
        }
        
        @Override
        public void run() {
            try {
                if (this.original != null) {
                    this.original.run();
                }
            }
            finally {
                this.wrapExitCallback();
            }
        }
        
        private void wrapExitCallback() {
            final boolean wasInterrupted = Thread.interrupted();
            try {
                DaemonThreadFactory.this.exitCallback.run();
            }
            catch (Throwable throwable) {
                DaemonThreadFactory.log.error("Thread exit notification threw an exception.", throwable);
            }
            finally {
                if (wasInterrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
