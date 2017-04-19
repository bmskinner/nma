package com.bmskinner.nuclear_morphology.components.options;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A base for all the options classes that need to store 
 * options as a key value pair
 * @author ben
 * @since 1.13.4
 */
public abstract class AbstractHashOptions implements Serializable, HashOptions {

	private static final long serialVersionUID = 1L;
	protected Map<String, Integer> intMap  = new HashMap<String, Integer>();
	protected Map<String, Double> dblMap   = new HashMap<String, Double>();
	protected Map<String, Float> fltMap    = new HashMap<String, Float>();
	protected Map<String, Boolean> boolMap = new HashMap<String, Boolean>();
		
	protected AbstractHashOptions(){}
	
	protected AbstractHashOptions(AbstractHashOptions o){
		intMap = o.intMap;
		fltMap = o.fltMap;
		dblMap = o.dblMap;
		boolMap= o.boolMap;
	}
	
	/**
	 * Get the double value with the given key
	 * @param s
	 * @return
	 */
	public double getDouble(String s){
		return dblMap.get(s).doubleValue();
	}
	
	
	/**
	 * Get the int value with the given key
	 * @param s
	 * @return
	 */
	public int getInt(String s){
		return intMap.get(s).intValue();
	}
	
	/**
	 * Get the boolean value with the given key
	 * @param s
	 * @return
	 */
	public boolean getBoolean(String s){
		return boolMap.get(s).booleanValue();
	}
	
	/**
	 * Set the double value with the given key
	 * @param s
	 * @param d
	 */
	public void setDouble(String s, double d){
		dblMap.put(s, d);
	}
	
	/**]
	 * Set the int value with the given key
	 * @param s
	 * @param i
	 */
	public void setInt(String s, int i){
		intMap.put(s, i);
	}
	
	/**
	 * Set the boolean value with the given key
	 * @param s
	 * @param b
	 */
	public void setBoolean(String s, boolean b){
		boolMap.put(s, b);
	}
	
	/**
	 * Get the float value with the given key
	 * @param s
	 * @return
	 */
	public float getFloat(String s){
		return fltMap.get(s).floatValue();
	}
	
	/**
	 * Set the float value with the given key
	 * @param s
	 * @param f
	 */
	public void setFloat(String s, float f){
		fltMap.put(s, f);
	}
	
	public List<String> getKeys(){
		List<String> list = new ArrayList<String>();
		for(String s : intMap.keySet()){
			list.add(s);
		}
		
		for(String s : dblMap.keySet()){
			list.add(s);
		}
		
		for(String s : boolMap.keySet()){
			list.add(s);
		}
		
		for(String s : fltMap.keySet()){
			list.add(s);
		}
		Collections.sort(list); 
		return list;
	}
	
	public Object getValue(String key){
		
		if(intMap.containsKey(key)){
			return intMap.get(key);
		}
		
		if(dblMap.containsKey(key)){
			return dblMap.get(key);
		}
		
		if(boolMap.containsKey(key)){
			return boolMap.get(key);
		}
		
		if(fltMap.containsKey(key)){
			return fltMap.get(key);
		}
		return "N/A";
	}
	
	
	public String toString(){

		StringBuilder sb = new StringBuilder();
			
		for(String s : intMap.keySet()){
			sb.append("\t"+s+": "+ intMap.get(s)+IDetectionOptions.NEWLINE);
		}
		
		for(String s : dblMap.keySet()){
			sb.append("\t"+s+": "+ dblMap.get(s)+IDetectionOptions.NEWLINE);
		}
		
		for(String s : boolMap.keySet()){
			sb.append("\t"+s+": "+ boolMap.get(s)+IDetectionOptions.NEWLINE);
		}
		
		for(String s : fltMap.keySet()){
			sb.append("\t"+s+": "+ fltMap.get(s)+IDetectionOptions.NEWLINE);
		}
		
		return sb.toString();
		
	}
	
	
}
