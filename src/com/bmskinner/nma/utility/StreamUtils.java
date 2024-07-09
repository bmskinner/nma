package com.bmskinner.nma.utility;

import java.util.concurrent.Callable;

/**
 * Helpers for stream handling
 * 
 * @author Ben Skinner
 *
 */
public class StreamUtils {

	/**
	 * Call a callable without throwing an exception
	 * 
	 * @param <T>
	 * @param callable
	 * @return
	 */
	public static <T> T uncheckCall(Callable<T> callable) {
		try {
			return callable.call();
		} catch (Exception e) {
			sneakyThrow(e);
			return null; // Unreachable but needed to satisfy compiler
		}
	}

	/**
	 * Run a runnable without throwing an exception
	 * 
	 * @param <T>
	 * @param callable
	 * @return
	 */
	public static void uncheckRun(RunnableExc r) {
		try {
			r.run();
		} catch (Exception e) {
			sneakyThrow(e);
		}
	}

	/**
	 * Interface for runnables being used in unchecked runs
	 *
	 */
	public interface RunnableExc {
		void run() throws Exception;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
		throw (T) t;
	}
}
