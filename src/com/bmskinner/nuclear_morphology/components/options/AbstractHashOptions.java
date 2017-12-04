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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A base for all the options classes that need to store options as a key value
 * pair
 * 
 * @author ben
 * @since 1.13.4
 */
public abstract class AbstractHashOptions implements Serializable, HashOptions {

    private static final long      serialVersionUID = 1L;
    protected Map<String, Integer> intMap           = new HashMap<String, Integer>();
    protected Map<String, Double>  dblMap           = new HashMap<String, Double>();
    protected Map<String, Float>   fltMap           = new HashMap<String, Float>();
    protected Map<String, Boolean> boolMap          = new HashMap<String, Boolean>();

    protected AbstractHashOptions() {
    }

    protected AbstractHashOptions(AbstractHashOptions o) {
        intMap = o.intMap;
        fltMap = o.fltMap;
        dblMap = o.dblMap;
        boolMap = o.boolMap;
    }

    /**
     * Get the double value with the given key
     * 
     * @param s
     * @return
     */
    public double getDouble(String s) {
        return dblMap.get(s).doubleValue();
    }

    /**
     * Get the int value with the given key
     * 
     * @param s
     * @return
     */
    public int getInt(String s) {
        return intMap.get(s).intValue();
    }

    /**
     * Get the boolean value with the given key
     * 
     * @param s
     * @return
     */
    public boolean getBoolean(String s) {
        return boolMap.get(s).booleanValue();
    }

    /**
     * Set the double value with the given key
     * 
     * @param s
     * @param d
     */
    public void setDouble(String s, double d) {
        dblMap.put(s, d);
    }

    /**
     * ] Set the int value with the given key
     * 
     * @param s
     * @param i
     */
    public void setInt(String s, int i) {
        intMap.put(s, i);
    }

    /**
     * Set the boolean value with the given key
     * 
     * @param s
     * @param b
     */
    public void setBoolean(String s, boolean b) {
        boolMap.put(s, b);
    }

    /**
     * Get the float value with the given key
     * 
     * @param s
     * @return
     */
    public float getFloat(String s) {
        return fltMap.get(s).floatValue();
    }

    /**
     * Set the float value with the given key
     * 
     * @param s
     * @param f
     */
    public void setFloat(String s, float f) {
        fltMap.put(s, f);
    }
    
    public List<String> getBooleanKeys() {
    	return sortedKeyList(boolMap);
    }
    
    public List<String> getIntegerKeys() {
    	return sortedKeyList(intMap);
    }
    
    public List<String> getDoubleKeys() {
    	return sortedKeyList(dblMap);
    }
    
    public List<String> getFloatKeys() {
    	return sortedKeyList(fltMap);
    }
    
    private List<String> sortedKeyList(Map<String, ?> map){
    	return map.keySet().stream().sorted().collect(Collectors.toList());
    }

    public List<String> getKeys() {
        List<String> list = new ArrayList<String>();
        
        list.addAll(getBooleanKeys());
        list.addAll(getIntegerKeys());
        list.addAll(getDoubleKeys());
        list.addAll(getFloatKeys());

        Collections.sort(list);
        return list;
    }

    public Object getValue(String key) {

        if (intMap.containsKey(key)) {
            return intMap.get(key);
        }

        if (dblMap.containsKey(key)) {
            return dblMap.get(key);
        }

        if (boolMap.containsKey(key)) {
            return boolMap.get(key);
        }

        if (fltMap.containsKey(key)) {
            return fltMap.get(key);
        }
        return "N/A";
    }
    
    public Map<String, Object> getEntries(){
    	Map<String, Object> result = new HashMap<>();
    	addEntries(intMap, result);
    	addEntries(dblMap, result);
    	addEntries(boolMap, result);
    	addEntries(fltMap, result);
    	return result;
    }
    
    private void addEntries(Map<String, ?> source, Map<String, Object> target){
    	for (String s : source.keySet()) {
            target.put(s, source.get(s));
        }
    }
      
    
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        
        Map<String, Object> map = getEntries();

        for (String s : map.keySet()) {
            sb.append("\t" + s + ": " + map.get(s) + IDetectionOptions.NEWLINE);
        }

        return sb.toString();

    }

}
