package com.bmskinner.nuclear_morphology.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.bmskinner.nuclear_morphology.logging.Loggable;

public class ThreadManager implements Loggable {
    private static volatile ThreadManager instance   = null;
    private static final Object           lockObject = new Object(); // synchronisation
    /*
     * Handle threading
     */

    public static final int corePoolSize    = 8;
    public static final int maximumPoolSize = 100;
    public static final int keepAliveTime   = 10000;

    private final ExecutorService executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    Map<CancellableRunnable, Future<?>> cancellableFutures = new HashMap<>();

    AtomicInteger queueLength = new AtomicInteger();

    protected ThreadManager() {
    }

    /**
     * Fetch an instance
     * 
     * @return
     */
    public static ThreadManager getInstance() {

        if (instance != null) {
            return instance;
        } else {

            synchronized (lockObject) {
                if (instance == null) {
                    instance = new ThreadManager();
                }
            }

            return instance;
        }

    }

    public synchronized Future<?> submit(Runnable r) {
        // executorService.submit(r);
        queueLength.incrementAndGet(); // Increment queue when submitting task.
        Future<?> f = executorService.submit(new Runnable() {
            public void run() {
                r.run();
                queueLength.decrementAndGet(); // Decrement queue when task
                                               // done.
            }
        });
        fine("Submitted runnable. Queue is " + queueLength.get());
        return f;
    }

    public synchronized Future<?> submit(Callable r) {

        return executorService.submit(r);
    }

    public synchronized void execute(Runnable r) {
        executorService.execute(r);
    }

    /**
     * Request an update of a cencellable process. If an update is already in
     * progress, it will be cancelled. Designed for dataset updates - cancel an
     * in progress update in favour of the new dataset list
     */
    public synchronized Future<?> submitAndCancelUpdate(CancellableRunnable r) {

        // Cancel previous updates
        for (CancellableRunnable c : cancellableFutures.keySet()) {

            c.cancel();

            fine("Removing future");
            // Future<?> future = cancellableFutures.get(c);
            // if( ! future.isDone()){
            //// log("Cancelling runnable");
            // c.cancel();
            // future.cancel(true);
            // }

            cancellableFutures.remove(c);
        }

        Future<?> future = executorService.submit(r);
        // log("Submitting runnable");
        cancellableFutures.put(r, future);
        return future;

    }

    /**
     * Request an update of a cencellable process. If an update is already in
     * progress, it will be cancelled.
     */
    public void executeAndCancelUpdate(CancellableRunnable r) {

        // Cancel previous updates
        for (CancellableRunnable c : cancellableFutures.keySet()) {

            c.cancel();

            fine("Removing future");
            // Future<?> future = cancellableFutures.get(c);
            // if( ! future.isDone()){
            //// log("Cancelling runnable");
            // c.cancel();
            // future.cancel(true);
            // }

            cancellableFutures.remove(c);
        }

        Future<?> future = executorService.submit(r);
        // log("Submitting runnable");
        cancellableFutures.put(r, future);

    }
}
