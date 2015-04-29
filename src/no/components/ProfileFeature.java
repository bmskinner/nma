package no.components;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// this holds the index of a feature in a median profile
public class ProfileFeature implements Serializable {
	 
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<String, Integer> featureIndex = new HashMap<String, Integer>();

	public ProfileFeature(){
	}
	
	public ProfileFeature(String indexType, int i){
		featureIndex.put(indexType, i);	
	}
	
	public void add(String indexType, int i){
		featureIndex.put(indexType, i);
	}
	
	public int get(String profileType){
		return featureIndex.get(profileType);
	}
}
