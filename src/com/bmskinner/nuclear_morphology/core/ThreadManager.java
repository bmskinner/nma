/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.bmskinner.nuclear_morphology.gui.CancellableRunnable;
import com.bmskinner.nuclear_morphology.gui.main.AbstractMainWindow.PanelUpdater;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class ThreadManager implements Loggable {
    private static volatile ThreadManager instance   = null;
    private static final Object           lockObject = new Object(); // synchronisation
   
    /*
     * Handle threading
     */
    public static final int keepAliveTime   = 10000;
    
    private final BlockingQueue<Runnable> executorQueue = new LinkedBlockingQueue<>(1024);

    private final ExecutorService executorService;

    Map<CancellableRunnable, Future<?>> cancellableFutures = new HashMap<>();

    AtomicInteger queueLength = new AtomicInteger();

    protected ThreadManager() {
    	int maxThreads = Runtime.getRuntime().availableProcessors();
    	if(maxThreads>1)
    		maxThreads-=1; // leave something for the OS, EDT etc.
    	log("Creating thread manager with max pool size of "+maxThreads);
    	executorService = new ThreadPoolExecutor(maxThreads, maxThreads, keepAliveTime,
                TimeUnit.MILLISECONDS, executorQueue);
    }
    
    public int queueLength(){
    	return queueLength.get();
    }

    /**
     * Fetch an instance
     * 
     * @return
     */
    public static ThreadManager getInstance() {

        if (instance != null)
            return instance;
		synchronized (lockObject) {
		    if (instance == null) {
		        instance = new ThreadManager();
		    }
		}
		return instance;
    }
    
    @Override
	public String toString() {
    	return executorQueue.toString();
    }

    public synchronized Future<?> submit(Runnable r) {
        queueLength.incrementAndGet(); // Increment queue when submitting task.
        return executorService.submit(new TrackedRunnable(r));
    }

    public synchronized Future<?> submit(Callable r) {
    	queueLength.incrementAndGet();
        return executorService.submit(makeSubmitableCallable(r));
    }
    
    /**
     * Add the given task to the executor service queue. If the job is a panel 
     * update, any existing queued panel updates will be cancelled.
     * @param r
     */
    public synchronized void execute(Runnable r) {
    	 // if a new update is requested, clear older queued updates
    	if(r instanceof PanelUpdater) {
    		executorQueue.removeIf(e->{
    			if(e instanceof TrackedRunnable) {
    				boolean b = ((TrackedRunnable)e).getSubmittedRunnable() instanceof PanelUpdater;
    				if(b) 
    					queueLength.decrementAndGet();
    				return b;
    			}
    			return false;
    		});
    	}

        queueLength.incrementAndGet(); // Increment queue when submitting task.
        executorService.execute(new TrackedRunnable(r));
    }
    
    private synchronized Callable makeSubmitableCallable(Callable r){
    	return () -> {
    		try {
				Object o = r.call();
				queueLength.decrementAndGet();
//				if(queueLength.decrementAndGet()==0){
////	    			log("Queue is empty");
//	    		}
	    		return o;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				queueLength.decrementAndGet();
				return null;
			}
    		
    	};
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
            cancellableFutures.remove(c);
        }
        Future<?> future = executorService.submit(r);
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
            cancellableFutures.remove(c);
        }
        Future<?> future = executorService.submit(r);
        cancellableFutures.put(r, future);
    }
    
    /**
     * Wrap a Runnable in another Runnable that updates the job queue
     * when done, and allows access to the original Runnable for checking
     * the class.
     * @author ben
     *
     */
    private class TrackedRunnable implements Runnable {
    	private Runnable r;
    	
		public TrackedRunnable(Runnable r) {
			this.r = r;
		}
    	
    	@Override
		public void run() {
			r.run();
    		queueLength.decrementAndGet();
		}
    	
    	public Runnable getSubmittedRunnable() {
    		return r;
    	}
    }
}
