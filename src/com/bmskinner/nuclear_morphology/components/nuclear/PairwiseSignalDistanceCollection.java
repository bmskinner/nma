package com.bmskinner.nuclear_morphology.components.nuclear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PairwiseSignalDistanceCollection {
	
	private Map<UUID, Map<UUID, List<Double>>> values = new HashMap< UUID, Map<UUID, List<Double> > >();
	
	public PairwiseSignalDistanceCollection() {}
		
	public void addValue(PairwiseSignalDistanceValue v){
		
		if(values.containsKey(v.getGroup1())){
			// add with group 1 first
			addValue(v.getGroup1(), v.getGroup2(), v.getValue());
			return;
		} 

		if(values.containsKey(v.getGroup2())){
			// add with group 2 first
			addValue(v.getGroup2(), v.getGroup1(), v.getValue());
			return;
		}
			
		
		// Neither present. Create new group from id1
		Map<UUID, List<Double>> map = new HashMap<UUID, List<Double>>();
		values.put(v.getGroup1(), map);
		
		addValue(v.getGroup1(), v.getGroup2(), v.getValue());
		
	}
	
	private void addValue(UUID id1, UUID id2, double value){
		Map<UUID, List<Double>> map = values.get(id1);
		
		if( ! map.containsKey(id2)){
			// Add new list
			List<Double> list = new ArrayList<Double>();
			map.put(id2, list);
		}
		
		map.get(id2).add(value);
		
	}

	
	public List<Double> getValues(UUID id1, UUID id2){
		return values.get(id1).get(id2);
	}
	
	
	

}
