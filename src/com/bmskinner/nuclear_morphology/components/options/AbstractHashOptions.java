package com.bmskinner.nuclear_morphology.components.options;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A base for all the options classes that need to store 
 * options as a key value pair
 * @author ben
 * @since 1.13.4
 */
public abstract class AbstractHashOptions implements Serializable {

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
	protected double getDouble(String s){
		return dblMap.get(s).doubleValue();
	}
	
	
	/**
	 * Get the int value with the given key
	 * @param s
	 * @return
	 */
	protected int getInt(String s){
		return intMap.get(s).intValue();
	}
	
	/**
	 * Get the boolean value with the given key
	 * @param s
	 * @return
	 */
	protected boolean getBoolean(String s){
		return boolMap.get(s).booleanValue();
	}
	
	/**
	 * Set the double value with the given key
	 * @param s
	 * @param d
	 */
	protected void setDouble(String s, double d){
		dblMap.put(s, d);
	}
	
	/**]
	 * Set the int value with the given key
	 * @param s
	 * @param i
	 */
	protected void setInt(String s, int i){
		intMap.put(s, i);
	}
	
	/**
	 * Set the boolean value with the given key
	 * @param s
	 * @param b
	 */
	protected void setBoolean(String s, boolean b){
		boolMap.put(s, b);
	}
	
	/**
	 * Get the float value with the given key
	 * @param s
	 * @return
	 */
	protected float getFloat(String s){
		return fltMap.get(s).floatValue();
	}
	
	/**
	 * Set the float value with the given key
	 * @param s
	 * @param f
	 */
	protected void setFloat(String s, float f){
		fltMap.put(s, f);
	}
	
	
	
	
	
}
