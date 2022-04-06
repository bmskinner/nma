package com.bmskinner.nuclear_morphology.components.cells;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

/**
 * Wrapper for consensus objects allowing offsets and rotations
 * @author ben
 * @since 1.15.0
 *
 * @param <E> the type of object this is a consensus of 
 */
public interface Consensus extends XmlSerializable, Nucleus {

	/**
	 * Move the consensus by the given amount in X and Y axes
	 * @param xOffset
	 * @param yOffset
	 */
	@Override
	void offset(double xOffset, double yOffset);

	/**
	 * Add a rotation by the given number of degrees clockwise
	 * @param angle
	 */
	void addRotation(double angle);

	/**
	 * Get the current rotation applied
	 * @return
	 */
	double currentRotation();

	/**
	 * Get the current offset applied
	 * @return
	 */
	IPoint currentOffset();
	
	/**
	 * Create a duplicate of this consensus
	 * @return
	 */
	@Override
	Consensus duplicate();
}
