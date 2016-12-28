package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.bmskinner.nuclear_morphology.gui.dialogs.prober.NucleusDetectionImageProber.NucleusImageType;

/**
 * Hold a set of detection image types and their position
 * in a table
 * @author ben
 *
 */
public class NucleusImageSet implements ImageSet {
		
	private Map<ImageType, Integer> values;
	
	public NucleusImageSet(){
		values = new HashMap<ImageType, Integer>();
		
		values.put(DetectionImageType.KUWAHARA,          0);
		values.put(DetectionImageType.FLATTENED,         1);
		values.put(DetectionImageType.EDGE_DETECTION,    2);
		values.put(DetectionImageType.MORPHOLOGY_CLOSED, 3);
		values.put(DetectionImageType.DETECTED_OBJECTS,  4);
		
	}
	
	@Override
	public int size(){
		return values.size();
	}
	
	/**
	 * Get the image type at the given position
	 * @param i
	 * @return
	 */
	@Override
	public ImageType getType(int i){
		
		if(i<0 || i>= values.size()){
			throw new IllegalArgumentException("Index out of bounds");
		}
		for(ImageType s : values.keySet()){
			if(values.get(s)==i){
				return s;
			}
		}
		return null;
	}

	@Override
	public Set<ImageType> values() {
		return values.keySet();
	}

	@Override
	public int getPosition(ImageType type) {
		// TODO Auto-generated method stub
		return values.get(type);
	}
	
	

}
