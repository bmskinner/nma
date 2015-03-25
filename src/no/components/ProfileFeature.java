package no.components;

import java.util.HashMap;
import java.util.Map;

// this holds the index of a feature in a median profile
public class ProfileFeature {
	 
	private Map<String, HashMap<String, Integer>> featureIndex = new HashMap<String, HashMap<String, Integer>>();

	public ProfileFeature(){
		
	}
	
	public void add(String profileType, String indexType, int i){
		
		HashMap<String, Integer> indexHash = featureIndex.get(profileType);
	    if(indexHash==null){
	      indexHash = new HashMap<String, Integer>();
	    }
	    indexHash.put(indexType, i);
	    featureIndex.put(profileType, indexHash);
	}
	
	public int get(String profileType, String indexType){
		return featureIndex.get(profileType).get(indexType);
	}
}
