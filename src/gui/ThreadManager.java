package gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadManager {
	private static ThreadManager instance = null;
	
	/*
	 * Handle threading
	 */
	
	public static final int corePoolSize    = 8;
	public static final int maximumPoolSize = 16;
	public static final int keepAliveTime = 10000;

	private final ExecutorService executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
			keepAliveTime, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>());
	

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
	
	public synchronized void submit(Runnable r){
		executorService.submit(r);
	}
	
	public synchronized void execute(Runnable r){
		executorService.execute(r);
	}
}
