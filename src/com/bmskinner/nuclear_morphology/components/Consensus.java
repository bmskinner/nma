package com.bmskinner.nuclear_morphology.components;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * Wrapper for consensus objects allowing offsets and rotations
 * @author ben
 * @since 1.15.0
 *
 * @param <E> the type of object this is a consensus of 
 */
public interface Consensus<E extends CellularComponent> {

	void offset(double xOffset, double yOffset);

	void addRotation(double angle);

	double currentRotation();

	IPoint currentOffset();
	
	Consensus<E> duplicateConsensus();
	
	E component();
}
