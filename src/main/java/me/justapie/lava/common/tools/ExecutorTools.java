package me.justapie.lava.common.tools;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.LoggerFactory;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;

public class ExecutorTools
{
    private static final Logger log;
    private static final long WAIT_TIME = 1000L;
    public static final CompletedVoidFuture COMPLETED_VOID;
    
    public static void shutdownExecutor(final ExecutorService executorService, final String description) {
        if (executorService == null) {
            return;
        }
        ExecutorTools.log.debug("Shutting down executor {}", (Object)description);
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(1000L, TimeUnit.MILLISECONDS)) {
                ExecutorTools.log.debug("Executor {} did not shut down in {}", (Object)description, (Object)1000L);
            }
            else {
                ExecutorTools.log.debug("Executor {} successfully shut down", (Object)description);
            }
        }
        catch (InterruptedException e) {
            ExecutorTools.log.debug("Received an interruption while shutting down executor {}", (Object)description);
            Thread.currentThread().interrupt();
        }
    }
    
    public static ThreadPoolExecutor createEagerlyScalingExecutor(final int coreSize, final int maximumSize, final long timeout, final int queueCapacity, final ThreadFactory threadFactory) {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(coreSize, maximumSize, timeout, TimeUnit.MILLISECONDS, new EagerlyScalingTaskQueue(queueCapacity), threadFactory);
        executor.setRejectedExecutionHandler(new EagerlyScalingRejectionHandler());
        return executor;
    }
    
    static {
        log = LoggerFactory.getLogger((Class)ExecutorTools.class);
        COMPLETED_VOID = new CompletedVoidFuture();
    }
    
    private static class EagerlyScalingTaskQueue extends LinkedBlockingQueue<Runnable>
    {
        public EagerlyScalingTaskQueue(final int capacity) {
            super(capacity);
        }
        
        @Override
        public boolean offer(final Runnable runnable) {
            return this.isEmpty() && super.offer(runnable);
        }
        
        public boolean offerDirectly(final Runnable runnable) {
            return super.offer(runnable);
        }
    }
    
    private static class EagerlyScalingRejectionHandler implements RejectedExecutionHandler
    {
        @Override
        public void rejectedExecution(final Runnable runnable, final ThreadPoolExecutor executor) {
            if (!((EagerlyScalingTaskQueue)executor.getQueue()).offerDirectly(runnable)) {
                throw new RejectedExecutionException("Task " + runnable.toString() + " rejected from " + runnable.toString());
            }
        }
    }
    
    private static class CompletedVoidFuture implements Future<Void>
    {
        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            return false;
        }
        
        @Override
        public boolean isCancelled() {
            return false;
        }
        
        @Override
        public boolean isDone() {
            return true;
        }
        
        @Override
        public Void get() throws InterruptedException, ExecutionException {
            return null;
        }
        
        @Override
        public Void get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}
