package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Hold a set of detection image types and their position
 * in a table
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultImageSet implements ImageSet {
		
	private Map<ImageType, Integer> values;
	
	public DefaultImageSet(){
		values = new HashMap<ImageType, Integer>();		
	}
	
	public ImageSet add(ImageType t){
		int i = values.size();
		values.put(t, i);
		return this;
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
		return values.get(type);
	}
	
	

}
