package com.bmskinner.nma.analysis;

/**
 * Interface for methods that can progress
 * 
 * @author Ben Skinner
 *
 */
public interface Progressable {

	/**
	 * Inform all listeners that the number of steps in the task has updated
	 * 
	 * @param totalProgress the new total number of steps
	 */
	void fireUpdateProgressTotalLength(int totalProgress);

	/**
	 * Alert progress listeners that the task length has become indeterminate
	 */
	void fireIndeterminateState();

	/**
	 * Fire a progress event to listeners, indicating a step of the task has been
	 * completed
	 */
	void fireProgressEvent();

	/**
	 * Fire a progress event to listeners, indicating a total number of steps of the
	 * task have been completed.
	 * 
	 * @param stepsNewlyCompleted the number of steps of the task completed
	 */
	void fireProgressEvent(long stepsNewlyCompleted);

	/**
	 * Fire the given progress event to all listeners
	 * 
	 * @param e
	 */
	void fireProgressEvent(ProgressEvent e);

}
