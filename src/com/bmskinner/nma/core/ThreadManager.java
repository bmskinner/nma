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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the threading and task queue. Analysis methods and UI updates are
 * treated separately for smoother UI refreshes.
 * 
 * @author bms41
 * @since 1.13.0
 *
 */
public class ThreadManager {

	private static final Logger LOGGER = Logger.getLogger(ThreadManager.class.getName());
	private static ThreadManager instance = null;

	/** Object to lock on for synchronisation */
	private static final Object lockObject = new Object();

	public static final int keepAliveTime = 10000;

	/** A queue for UI update tasks */
	private final BlockingQueue<Runnable> methodQueue = new LinkedBlockingQueue<>(1024);

	/** A queue for analysis method update tasks */
	private final BlockingQueue<Runnable> uiQueue = new LinkedBlockingQueue<>(1024);

	/** Thread pool for method update tasks */
	private final ExecutorService methodExecutorService;

	/** Thread pool for UI update tasks */
	private final ExecutorService uiExecutorService;

	private AtomicInteger uiQueueLength = new AtomicInteger();
	private AtomicInteger methodQueueLength = new AtomicInteger();

	/**
	 * Private constructor since this should be accessed as a singleton
	 */
	private ThreadManager() {
		int maxThreads = Runtime.getRuntime().availableProcessors();
		if (maxThreads > 2) // if this is a dual core machine, we can't afford to be nice
			maxThreads -= 1; // otherwise, leave something for the OS, EDT etc.

		int maxMethodThreads = 2; // if on a low core system, have two threads to prevent blocking
		if (maxThreads > 10)
			maxMethodThreads = maxThreads / 3; // if we're on a server, go wild

		// The bulk of threads should still be devoted to redrawing charts
		int maxUiThreads = Math.max(1, maxThreads - maxMethodThreads);

		int maxForkJoinThreads = Math.max(1, maxUiThreads - 1); // ensure FJPs don't block the ui
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",
				String.valueOf(maxForkJoinThreads));

		// Create the thread pools
		methodExecutorService = new ThreadPoolExecutor(maxMethodThreads, maxMethodThreads,
				keepAliveTime,
				TimeUnit.MILLISECONDS, methodQueue);
		uiExecutorService = new ThreadPoolExecutor(maxUiThreads, maxUiThreads, keepAliveTime,
				TimeUnit.MILLISECONDS,
				uiQueue);

		LOGGER.config("Allowed processors: %d, split %d for UI, %d for methods".formatted(
				maxThreads, maxUiThreads, maxMethodThreads));

		long maxMemory = Runtime.getRuntime().maxMemory();
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

	public int uiQueueLength() {
		return uiQueueLength.get();
	}

	public int methodQueueLength() {
		return methodQueueLength.get();
	}

	@Override
	public String toString() {
		return uiQueue.toString();
	}

	/**
	 * Submit the given runnable to the UI update thread pool
	 * 
	 * @param r
	 * @return
	 */
	public synchronized Future<?> submitUIUpdate(Runnable r) {
		TrackedRunnable t = new TrackedRunnable(r);
		return uiExecutorService.submit(t);
	}

	public synchronized Future<?> submit(Runnable r) {
		TrackedRunnable t = new TrackedRunnable(r);
		if (r instanceof InterfaceUpdater)
			return uiExecutorService.submit(t);
		return methodExecutorService.submit(t);
	}

	public synchronized Future<?> submit(Callable<?> r) {
		methodQueueLength.incrementAndGet();
		return methodExecutorService.submit(makeSubmitableCallable(r));
	}

	/**
	 * Add the given task to the executor service queue. If the job is a panel
	 * update, any existing queued panel updates will be cancelled.
	 * 
	 * @param r
	 */
	public synchronized void execute(Runnable r) {
		// if a new update is requested, clear older queued updates
		if (r instanceof InterfaceUpdater) {
			uiExecutorService.execute(new TrackedRunnable(r));
		} else {
			methodExecutorService.execute(new TrackedRunnable(r));
		}
	}

	private synchronized Callable<?> makeSubmitableCallable(Callable<?> r) {
		return () -> {

			Object o = null;
			try {
				o = r.call();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error calling submittable callable", e);
				return null;
			} finally {
				methodQueueLength.decrementAndGet();
			}
			return o;

		};
	}

	/**
	 * Wrap a Runnable in another Runnable that updates the job queue when done, and
	 * allows access to the original Runnable for checking the class.
	 * 
	 * @author ben
	 * @since 1.14.0
	 *
	 */
	private class TrackedRunnable implements Runnable {
		private Runnable r;

		public TrackedRunnable(Runnable r) {
			if (r instanceof InterfaceUpdater) // Increment queue when submitting task.
				uiQueueLength.incrementAndGet();
			else
				methodQueueLength.incrementAndGet();
			this.r = r;
		}

		@Override
		public void run() {
			r.run();
			if (r instanceof InterfaceUpdater)
				uiQueueLength.decrementAndGet();
			else
				methodQueueLength.decrementAndGet();
		}

		public Runnable getSubmittedRunnable() {
			return r;
		}
	}
}
