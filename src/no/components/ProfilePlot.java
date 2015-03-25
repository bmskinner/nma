package no.components;

import ij.gui.Plot;

import java.util.HashMap;
import java.util.Map;

// holds e.g. the normalised and raw plots
public class ProfilePlot {
	private Map<String, Plot> collection = new HashMap<String, Plot>();
	
	public ProfilePlot(){
		
	}
	
	public void add(String s, Plot p){
		collection.put(s, p);
	}
	
	public Plot get(String s){
		return this.collection.get(s);
	}

}
