package com.bmskinner.nma.components.signals;

/**
 * Interface for nuclear signal collections to inform their host that their
 * contents has changed
 * 
 * @author Ben Skinner
 *
 */
public interface NuclearSignalAddedListener {

	/**
	 * Inform the listener that the nucleear signals in the collection have changed
	 */
	void nuclearSignalAdded();

}
