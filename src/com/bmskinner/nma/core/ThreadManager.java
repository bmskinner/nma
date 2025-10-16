/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the threading and task queue. Analysis methods and UI updates are
 * treated separately for smoother UI refreshes. Access as a singleton.
 * 
 * @author Ben Skinner
 * @since 1.13.0
 *
 */
public class ThreadManager {

	private static final Logger LOGGER = Logger.getLogger(ThreadManager.class.getName());
	private static ThreadManager instance = null;

	/** Object to lock on for synchronisation */
	private static final Object lockObject = new Object();

	public static final int keepAliveTime = 10000;

	/** A queue for analysis update tasks */
	private final BlockingQueue<Runnable> methodQueue = new LinkedBlockingQueue<>(1024);

	/** A queue for UI update tasks */
	private final BlockingQueue<Runnable> uiQueue = new LinkedBlockingQueue<>(1024);

	/** Thread pool for method update tasks */
	private final ThreadPoolExecutor methodExecutorService;

	/** Thread pool for UI update tasks */
	private final ThreadPoolExecutor uiExecutorService;

	/**
	 * Mark tracked runnables as being dispatched to a specific thread pool
	 */
	private enum ThreadPoolType {
		UI,
		METHOD
	}
	
	/**
	 * Link a runnable and the future it generates in thread pool
	 */
	public record TrackedFuture(Runnable runnable, Future<?> future) {
	}

	/**
	 * Private constructor since this should be accessed as a singleton
	 */
	private ThreadManager() {
		int maxThreads = Runtime.getRuntime().availableProcessors();
		if (maxThreads > 2)
		 { // if this is a dual core machine, we can't afford to be nice
			maxThreads -= 1; // otherwise, leave something for the OS, EDT etc.
		}

		int maxMethodThreads = 2; // if on a low core system, have two threads to prevent blocking
		if (maxThreads > 10)
		 {
			maxMethodThreads = maxThreads / 3; // if we're on a server, go wild
		}

		// The bulk of threads should still be devoted to redrawing charts
		final int maxUiThreads = Math.max(1, maxThreads - maxMethodThreads);

		final int maxForkJoinThreads = Math.max(1, maxUiThreads - 1); // ensure FJPs don't block the ui
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
				String.valueOf(maxForkJoinThreads));

		// Create the thread pools
		methodExecutorService = new ThreadPoolExecutor(maxMethodThreads, maxMethodThreads,
				keepAliveTime,
				TimeUnit.MILLISECONDS, methodQueue);
		uiExecutorService = new ThreadPoolExecutor(maxUiThreads, maxUiThreads, keepAliveTime,
				TimeUnit.MILLISECONDS,
				uiQueue);

		uiExecutorService.prestartAllCoreThreads();

		LOGGER.config("Allowed processors: %d, split %d for UI, %d for methods".formatted(
				maxThreads, maxUiThreads, maxMethodThreads));

		final long maxMemory = Runtime.getRuntime().maxMemory();
		long maxMemoryHuman = maxMemory / (1024 * 1024);

		// Pretty format for readability
		String units = "MiB";
		if (maxMemoryHuman > 10000) {
			maxMemoryHuman /= 1024;
			units = "GiB";
		}

		LOGGER.config(String.format("Maximum memory: %s %s (%s bytes)", maxMemoryHuman,
				units, maxMemory));
	}

	/**
	 * Fetch the manager instance
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

	public int uiQueueLength() {
		return uiQueue.size();
	}

	public int methodQueueLength() {
		return methodQueue.size();
	}

	@Override
	public String toString() {
		return uiQueue.toString();
	}

	/**
	 * Submit the given runnable to the UI update thread pool
	 * 
	 * @param r the runnable
	 * @return a future for the result of the task
	 */
	public Future<?> submitUIUpdate(Runnable r) {
		return submitUITask(r);
	}

	/**
	 * Submit the given runnable to a queue. If the runnable is an instance of
	 * {@link InterfaceUpdater}, the method will be run on the UI thread pool.
	 * 
	 * @param r the runnable
	 * @return a future for the result of the task
	 */
	public Future<?> submit(Runnable r) {
		if (r instanceof InterfaceUpdater)
			return submitUITask(r);
		return methodExecutorService.submit(r);
	}

	/**
	 * Submit a UI update task to the UI executor. Also track the datasets that this
	 * UI update applies to. If a new update applies to different datasets, then the
	 * queued tasks for other datasets will be cancelled.
	 * 
	 * @param r
	 * @return
	 */
	private synchronized Future<?> submitUITask(Runnable r) {
		return uiExecutorService.submit(r);
	}

	/**
	 * Submit a callable method task to the method thread pool executor
	 * 
	 * @param r
	 * @return
	 */
	public Future<?> submit(Callable<?> r) {
		return methodExecutorService.submit(makeSubmitableCallable(r));
	}

	private Callable<?> makeSubmitableCallable(Callable<?> r) {
		return () -> {

			Object o = null;
			try {
				o = r.call();
			} catch (final Exception e) {
				LOGGER.log(Level.SEVERE, "Error calling submittable callable: %s".formatted(e.getMessage()), e);
				return null;
			}
			return o;

		};
	}
}
