package com.bmskinner.nuclear_morphology.io;

/**
 * Listener for count events in a CountedInputStream or CountedOutputStream
 * @author ben
 * @since 1.13.8
 *
 */
public interface CountListener {
	void countChanged(long newCount);
}
