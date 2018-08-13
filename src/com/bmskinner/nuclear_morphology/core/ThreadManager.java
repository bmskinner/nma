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
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class ThreadManager implements Loggable {
    private static volatile ThreadManager instance   = null;
    private static final Object           lockObject = new Object(); // synchronisation
   
    /*
     * Handle threading
     */

//    public static final int corePoolSize    = 100;
//    public static final int maximumPoolSize = 100;
    public static final int keepAliveTime   = 10000;
    
    private final BlockingQueue<Runnable> executorQueue = new LinkedBlockingQueue<>(1024);

    private final ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), 
    		Runtime.getRuntime().availableProcessors()+1, keepAliveTime,
            TimeUnit.MILLISECONDS, executorQueue);

    Map<CancellableRunnable, Future<?>> cancellableFutures = new HashMap<>();

    AtomicInteger queueLength = new AtomicInteger();

    protected ThreadManager() {
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
        return executorService.submit(makeSubmitableRunnable(r));
    }

    public synchronized Future<?> submit(Callable r) {
    	queueLength.incrementAndGet();
        return executorService.submit(makeSubmitableCallable(r));
    }
    
    public synchronized void execute(Runnable r) {
        queueLength.incrementAndGet();
        executorService.execute(makeSubmitableRunnable(r));
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
    
    private synchronized Runnable makeSubmitableRunnable(Runnable r){
    	return () -> {
    		r.run();
    		queueLength.decrementAndGet();
//    		if(queueLength.decrementAndGet()==0){
////    			log("Queue is empty");
//    		}
    		
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
}
