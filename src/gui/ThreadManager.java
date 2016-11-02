package gui;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import logging.Loggable;

public class ThreadManager implements Loggable {
	private static ThreadManager instance = null;
	
	/*
	 * Handle threading
	 */
	
	public static final int corePoolSize    = 16;
	public static final int maximumPoolSize = 32;
	public static final int keepAliveTime = 10000;

	private final ExecutorService executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
			keepAliveTime, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>());
	
	Map<CancellableRunnable, Future<?>> cancellableFutures = new HashMap<>();
	
	AtomicInteger queueLength = new AtomicInteger();
	

	protected ThreadManager(){}

	/**
	 * Fetch an instance of the factory
	 * @return
	 */
	public static ThreadManager getInstance(){
		if(instance==null){
			instance = new ThreadManager();
		}
		return instance;
	}
	
	public synchronized Future<?> submit(Runnable r){
//		executorService.submit(r);
		queueLength.incrementAndGet(); // Increment queue when submitting task.
		Future<?> f = executorService.submit(new Runnable() {
            public void run() {
                r.run();
                queueLength.decrementAndGet(); // Decrement queue when task done.
            }
        });
		fine("Submitted runnable. Queue is "+queueLength.get());
		return f;
	}
	
	public synchronized void execute(Runnable r){
		executorService.execute(r);
	}
	
	/**
	 * Request an update of a cencellable process. If an update is 
	 * already in progress, it will be cancelled.
	 */
	public void executeAndCancelUpdate(CancellableRunnable r){
		

		// Cancel previous updates
		for( CancellableRunnable c : cancellableFutures.keySet() ){
//			log("Removing future");
			Future<?> future = cancellableFutures.get(c);
			if( ! future.isDone()){
//				log("Cancelling runnable");
				c.cancel();
				future.cancel(true);
			}

			cancellableFutures.remove(c);
		}
		
		Future<?> future = executorService.submit(r);
//		log("Submitting runnable");
		cancellableFutures.put(r, future);
		
	}
}
