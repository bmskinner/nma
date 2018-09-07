/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.options;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

/**
 * A base for all the options classes that need to store options as a key value
 * pair
 * 
 * @author ben
 * @since 1.13.4
 */
public abstract class AbstractHashOptions implements Serializable, HashOptions {

    private static final long      serialVersionUID = 1L;
    protected Map<String, Integer> intMap    = new HashMap<>();
    protected Map<String, Double>  dblMap    = new HashMap<>();
    protected Map<String, Float>   fltMap    = new HashMap<>();
    protected Map<String, Boolean> boolMap   = new HashMap<>();
    protected Map<String, String>  stringMap = new HashMap<>();

    protected AbstractHashOptions() { }

    /**
     * Create by copying another options object
     * @param o
     */
    protected AbstractHashOptions(@NonNull AbstractHashOptions o) {
    	set(o);
    }
    
    @Override
	public void set(HashOptions o) {
    	for(String s : o.getBooleanKeys())
    		boolMap.put(s, o.getBoolean(s));    
    	for(String s : o.getIntegerKeys())
    		intMap.put(s, o.getInt(s));
    	for(String s : o.getDoubleKeys())
    		dblMap.put(s, o.getDouble(s));
    	for(String s : o.getFloatKeys())
    		fltMap.put(s, o.getFloat(s));
    	for(String s : o.getStringKeys())
    		stringMap.put(s, o.getString(s));
    }
    
    
    /**
     * Get the double value with the given key
     * 
     * @param s
     * @return
     */
    @Override
	public double getDouble(String s) {
        return dblMap.get(s).doubleValue();
    }

    /**
     * Get the int value with the given key
     * 
     * @param s
     * @return
     */
    @Override
	public int getInt(String s) {
        return intMap.get(s).intValue();
    }

    /**
     * Get the boolean value with the given key
     * 
     * @param s
     * @return
     */
    @Override
	public boolean getBoolean(String s) {
        return boolMap.get(s).booleanValue();
    }

    /**
     * Set the double value with the given key
     * 
     * @param s
     * @param d
     */
    @Override
	public void setDouble(String s, double d) {
        dblMap.put(s, d);
    }

    /**
     * ] Set the int value with the given key
     * 
     * @param s
     * @param i
     */
    @Override
	public void setInt(String s, int i) {
        intMap.put(s, i);
    }

    /**
     * Set the boolean value with the given key
     * 
     * @param s
     * @param b
     */
    @Override
	public void setBoolean(String s, boolean b) {
        boolMap.put(s, b);
    }


    @Override
	public float getFloat(String s) {
        return fltMap.get(s).floatValue();
    }

    @Override
	public void setFloat(String s, float f) {
        fltMap.put(s, f);
    }
    
    @Override
	public String getString(String s) {
        return stringMap.get(s);
    }

    @Override
	public void setString(String k, String v) {
    	stringMap.put(k, v);
    }
    
    @Override
	public List<String> getBooleanKeys() {
    	return sortedKeyList(boolMap);
    }
    
    @Override
	public List<String> getIntegerKeys() {
    	return sortedKeyList(intMap);
    }
    
    @Override
	public List<String> getDoubleKeys() {
    	return sortedKeyList(dblMap);
    }
    
    @Override
	public List<String> getFloatKeys() {
    	return sortedKeyList(fltMap);
    }
    
    @Override
	public List<String> getStringKeys() {
    	return sortedKeyList(stringMap);
    }
    
    private List<String> sortedKeyList(Map<String, ?> map){
    	return map.keySet().stream().sorted().collect(Collectors.toList());
    }

    @Override
	public List<String> getKeys() {
        List<String> list = new ArrayList<String>();
        
        list.addAll(getBooleanKeys());
        list.addAll(getIntegerKeys());
        list.addAll(getDoubleKeys());
        list.addAll(getFloatKeys());
        list.addAll(getStringKeys());

        Collections.sort(list);
        return list;
    }

    @Override
	public Object getValue(String key) {

        if (intMap.containsKey(key))
            return intMap.get(key);
        if (dblMap.containsKey(key))
            return dblMap.get(key);
        if (boolMap.containsKey(key))
            return boolMap.get(key);
        if (fltMap.containsKey(key))
            return fltMap.get(key);
        if(stringMap.containsKey(key))
        	 return stringMap.get(key);
        return "N/A";
    }
    
    @Override
	public Map<String, Object> getEntries(){
    	Map<String, Object> result = new HashMap<>();
    	addEntries(intMap, result);
    	addEntries(dblMap, result);
    	addEntries(boolMap, result);
    	addEntries(fltMap, result);
    	addEntries(stringMap, result);
    	return result;
    }
    
    private void addEntries(Map<String, ?> source, Map<String, Object> target){
    	for (String s : source.keySet()) {
            target.put(s, source.get(s));
        }
    }
    
    @Override
    public int hashCode() {
    	 final int prime = 31;
         int result = super.hashCode();

         result = prime * result + intMap.hashCode();
         result = prime * result + dblMap.hashCode();
         result = prime * result + boolMap.hashCode();
         result = prime * result + fltMap.hashCode();
         result = prime * result + stringMap.hashCode();
         return result;
    }
    
    @Override
    public boolean equals(Object o) {
    	if(this==o)
    		return true;
    	if(o==null)
    		return false;
    	if(!(o instanceof AbstractHashOptions))
    		return false;
    	AbstractHashOptions other = (AbstractHashOptions)o;
    	List<String> keys = getKeys();
    	if(!other.getKeys().equals(keys))
    		return false;
    	
    	for(String key : getBooleanKeys()) {
    		if(getBoolean(key)!=other.getBoolean(key))
    			return false;
    	}
    	
    	for(String key : getIntegerKeys()) {
    		if(getInt(key)!=other.getInt(key))
    			return false;
    	}
    	
    	for(String key : getDoubleKeys()) {
    		if(Double.doubleToLongBits(getDouble(key))!=Double.doubleToLongBits(other.getDouble(key)))
    			return false;
    	}
    	
    	for(String key : getFloatKeys()) {
    		if(Double.doubleToLongBits(getFloat(key))!=Double.doubleToLongBits(other.getFloat(key)))
    			return false;
    	}
    	for(String key : getStringKeys()) {
    		if(!getString(key).equals(other.getString(key)))
    			return false;
    	}
    	return true;
    }
      
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> map = getEntries();
        for (String s : map.keySet())
            sb.append("\t" + s + ": " + map.get(s) + IDetectionOptions.NEWLINE);
        return sb.toString();
    }
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (stringMap == null) {
        	stringMap = new HashMap<>();
        }
    }
    

}
