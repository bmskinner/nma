package com.bmskinner.nuclear_morphology.gui;

/**
 * Adds a cancel method to a Runnable 
 * @author ben
 *
 */
public interface CancellableRunnable extends Runnable {
	
	/**
	 * Attempt to cancel the Runnable
	 */
	void cancel();
}
