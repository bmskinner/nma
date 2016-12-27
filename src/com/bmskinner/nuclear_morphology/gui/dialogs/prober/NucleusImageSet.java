package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NucleusImageSet implements ImageSet {

	public static final String KUWAHARA          = "Kuwahara filtering";
	public static final String FLATTENED         = "Chromocentre flattening";
	public static final String EDGE_DETECTION    = "Edge detection";
	public static final String MORPHOLOGY_CLOSED = "Gap closing";
	public static final String DETECTED_OBJECTS  = "Detected objects";
		
	private Map<String, Integer> values;
	
	public NucleusImageSet(){
		values = new HashMap<String, Integer>();
		
		values.put(KUWAHARA,          0);
		values.put(FLATTENED,         1);
		values.put(EDGE_DETECTION,    2);
		values.put(MORPHOLOGY_CLOSED, 3);
		values.put(DETECTED_OBJECTS,  4);
		
	}
	
	public int size(){
		return values.size();
	}
	
	public String get(int i){
		for(String s : values.keySet()){
			if(values.get(s)==i){
				return s;
			}
		}
		return null;
	}

	public Set<String> getValues() {
		// TODO Auto-generated method stub
		return values.keySet();
	}

	public int getPosition(String type) {
		// TODO Auto-generated method stub
		return values.get(type);
	}
	
	

}
