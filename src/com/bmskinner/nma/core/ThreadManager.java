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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

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

	/**
	 * Store the UI update futures for a selected dataset order. Use to cancel
	 * unneeded tasks when selections change
	 **/
	private final Map<List<IAnalysisDataset>, List<Future<?>>> uiFutures = new ConcurrentHashMap<>();

	/** Thread pool for method update tasks */
	private final ExecutorService methodExecutorService;

	/** Thread pool for UI update tasks */
	private final ExecutorService uiExecutorService;

	/**
	 * Mark tracked runnables as being dispatched to a specific thread pool
	 */
	private enum ThreadPoolType {
		UI,
		METHOD
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
	public synchronized Future<?> submitUIUpdate(Runnable r) {
		return submitUITask(r);
	}

	/**
	 * Submit the given runnable to a queue. If the runnable is an instance of
	 * {@link InterfaceUpdater}, the method will be run on the UI thread pool.
	 * 
	 * @param r the runnable
	 * @return a future for the result of the task
	 */
	public synchronized Future<?> submit(Runnable r) {
		if (r instanceof InterfaceUpdater)
			return submitUITask(r);

		return methodExecutorService.submit(new TrackedRunnable(r, ThreadPoolType.METHOD));
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
		final TrackedRunnable t = new TrackedRunnable(r, ThreadPoolType.UI);
		// Add the future to a list associated with a dataset order
		final Future<?> f = uiExecutorService.submit(t);

		if (t.datasetsAffected().isEmpty())
			return f;

		final List<Future<?>> futures = uiFutures.computeIfAbsent(t.datasetsAffected(),
				k -> new ArrayList<Future<?>>());

		// Remove any futures from the executor service that have different datasets
		final Iterator<Entry<List<IAnalysisDataset>, List<Future<?>>>> it = uiFutures.entrySet().iterator();
		while (it.hasNext()) {
			final Entry<List<IAnalysisDataset>, List<Future<?>>> entry = it.next();
			
			// If a queued future has the same dataset order as the current update, keep
			// it
			if (entry.getKey().equals(t.datasetsAffected())) {
				LOGGER.finer("Same datasets %s as current queued request, keeping %s futures".formatted(
						t.datasetsAffected().stream().map(IAnalysisDataset::getName)
								.collect(Collectors.joining(", ")),
						entry.getValue().size()));
				
				// Remove any completed futures that are no longer needed
				final Iterator<Future<?>> fit = entry.getValue().iterator();
				while (fit.hasNext()) {
					final Future<?> fut = fit.next();
					if (fut.isDone() || fut.isCancelled()) {
						fit.remove();
					}
				}
				continue;
			}

			// Cancel the futures we don't need
			final List<Future<?>> queuedFutures = entry.getValue();
			for (final Future<?> fut : queuedFutures) {
				fut.cancel(true);
			}

			LOGGER.finer("Cancelled %s futures from %s datasets, now %s tasks".formatted(
					queuedFutures,
					t.datasetsAffected().stream().map(IAnalysisDataset::getName).collect(Collectors.joining(", ")),
					uiQueue.size()));

			// Remove the entire list of cancelled futures
			it.remove();
		}

		// Add the new future task
		futures.add(f);
		return f;
	}

	/**
	 * Submit a callable method task to the method thread pool executor
	 * 
	 * @param r
	 * @return
	 */
	public synchronized Future<?> submit(Callable<?> r) {
		return methodExecutorService.submit(makeSubmitableCallable(r));
	}

	/**
	 * Add the given task to the executor service queue for execution.
	 * 
	 * @param r
	 */
	public synchronized void execute(Runnable r) {
		// if a new update is requested, clear older queued updates
		if (r instanceof InterfaceUpdater) {
			final TrackedRunnable t = new TrackedRunnable(r, ThreadPoolType.UI);
			uiExecutorService.execute(t);
		} else {
			methodExecutorService.execute(new TrackedRunnable(r, ThreadPoolType.METHOD));
		}
	}

	private synchronized Callable<?> makeSubmitableCallable(Callable<?> r) {
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

	/**
	 * Wrap a Runnable in another Runnable that allows access to the original
	 * Runnable for checking the class, and tracks datasets affected by UI update
	 * runnables.
	 * 
	 * @author Ben Skinner
	 * @since 1.14.0
	 *
	 */
	private class TrackedRunnable implements Runnable {
		private final Runnable r;
		private final ThreadPoolType t;
		private final List<IAnalysisDataset> affectedDatasets;

		public TrackedRunnable(Runnable r, ThreadPoolType t) {
			this.t = t;
			this.r = r;

			if (r instanceof final InterfaceUpdater i) {
				affectedDatasets = i.datasetsAffected();
			} else {
				affectedDatasets = new ArrayList<>();
			}
		}

		@Override
		public void run() {
			r.run();
		}

		public List<IAnalysisDataset> datasetsAffected() {
			return affectedDatasets;
		}
	}
}
