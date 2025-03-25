package com.bmskinner.nma.components.cells;

import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.io.XmlSerializable;

/**
 * Wrapper for consensus objects allowing offsets and rotations
 * 
 * @author Ben Skinner
 * @since 1.15.0
 *
 * @param <E> the type of object this is a consensus of
 */
public interface Consensus extends XmlSerializable, Nucleus {

	/**
	 * Set the consensus to be moved to the given offset when drawing
	 * 
	 * @param xOffset
	 * @param yOffset
	 */
	void setOffset(double xOffset, double yOffset);

	/**
	 * Add a rotation by the given number of degrees clockwise
	 * 
	 * @param angle
	 */
	void addRotation(double angle);

	/**
	 * Get the current rotation applied
	 * 
	 * @return
	 */
	double currentRotation();

	/**
	 * Get the current offset applied
	 * 
	 * @return
	 */
	IPoint currentOffset();

	/**
	 * Create a duplicate of this consensus
	 * 
	 * @return
	 */
	@Override
	Consensus duplicate();
}
