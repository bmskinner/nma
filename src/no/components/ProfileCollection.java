package no.components;

import ij.IJ;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ProfileCollection {
	
	private Map<String, ProfileFeature> features = new HashMap<String, ProfileFeature>();
	private Map<String, Profile> profiles = new HashMap<String, Profile>(0); 
	private Map<String, ProfileAggregate> aggregates = new HashMap<String, ProfileAggregate>();
	private Map<String, ProfilePlot> plots = new HashMap<String, ProfilePlot>();
	
	public ProfileCollection(){
		
	}
	
	// Get features
	
	public ProfileFeature getFeature(String s){
		return features.get(s);
	}
	
	public Profile getProfile(String s){
		return profiles.get(s);
	}
	
	public ProfileAggregate getAggregate(String s){
		return aggregates.get(s);
	}
	
	public ProfilePlot getPlots(String s){
		return plots.get(s);
	}
	
	// Add or update features
	
	public void addFeature(String s, ProfileFeature p){
		features.put(s, p);
	}
	
	public void addProfile(String s, Profile p){
		profiles.put(s, p);
	}
	
	public void addAggregate(String s, ProfileAggregate p){
		aggregates.put(s, p);
	}
	
	public void addPlots(String s, ProfilePlot p){
		plots.put(s, p);
	}
	
	public void printProfiles(){
		Set<String> keys = profiles.keySet();
		for(String s : keys){
			IJ.log("   "+s);
		}
	}
	
	// Get keys
	
	public Set<String> getPlotKeys(){
		return plots.keySet();
	}
	
	public Set<String> getProfileKeys(){
		return profiles.keySet();
	}
	
	public Set<String> getAggregateKeys(){
		return aggregates.keySet();
	}
	
	public Set<String> getFeatureKeys(){
		return features.keySet();
	}


}
