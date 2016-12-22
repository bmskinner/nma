package com.bmskinner.nuclear_morphology.components.nuclear;

import java.util.UUID;

public abstract class PairwiseSignalDistance {
	private UUID group1;
	private UUID group2;

	
	public PairwiseSignalDistance(UUID id1, UUID id2){
		group1 = id1;
		group2 = id2;
	}
	
	public UUID getGroup1(){
		return group1;
	}
	
	public UUID getGroup2(){
		return group2;
	}
}
