package com.bmskinner.nuclear_morphology.components.nuclear;

import java.util.UUID;

/**
 * Holds the shortest distance between signals in two
 * signal groups within a nucleus.
 * @author ben
 * @since 1.13.4
 *
 */
public class PairwiseSignalDistanceValue extends PairwiseSignalDistance {
	
	private double value;
	
	public PairwiseSignalDistanceValue(UUID id1, UUID id2, double value){
		super(id1, id2);
		this.value = value;
	}
	
	public double getValue(){
		return value;
	}

}
