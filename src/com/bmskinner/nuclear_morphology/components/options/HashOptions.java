/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.components.options;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * The interface for options classes. Store options as key value pairs
 * @author bms41
 *
 */
public interface HashOptions extends Serializable {
	
    /**
     * Get the double value with the given key.
     * 
     * @param s
     * @return
     */
    double getDouble(String s);

    /**
     * Get the int value with the given key.
     * 
     * @param s
     * @return
     */
    int getInt(String s);

    /**
     * Get the boolean value with the given key.
     * 
     * @param s
     * @return
     */
    boolean getBoolean(String s);

    /**
     * Set the double value with the given key.
     * 
     * @param s
     * @param d
     */
    void setDouble(String s, double d);

    /**
     * ] Set the int value with the given key.
     * 
     * @param s
     * @param i
     */
    void setInt(String s, int i);

    /**
     * Set the boolean value with the given key.
     * 
     * @param s
     * @param b
     */
    void setBoolean(String s, boolean b);

    /**
     * Get the float value with the given key.
     * 
     * @param s
     * @return
     */
    float getFloat(String s);

    /**
     * Set the float value with the given key.
     * 
     * @param s
     * @param f
     */
    void setFloat(String s, float f);
    
    /**
     * Get the string value with the given key
     * @param s
     * @return
     */
    String getString(String s);
    
    /**
     * Set the string value with the given key
     * @param k
     * @param v
     */
    void setString(String k, String v);

    /**
     * Get the keys to all the boolean values in this options.
     * 
     * @return
     */
    List<String> getBooleanKeys();
    
    /**
     * Get the keys to all the integer values in this options.
     * 
     * @return
     */
    List<String> getIntegerKeys();
    
    /**
     * Get the keys to all the double values in this options.
     * 
     * @return
     */
    List<String> getDoubleKeys();
    
    /**
     * Get the keys to all the float values in this options.
     * 
     * @return
     */
    List<String> getFloatKeys();
    
    /**
     * Get the keys to all the straing values in this options.
     * 
     * @return
     */
    List<String> getStringKeys();
    
    /**
     * Get the keys to all the values in this options.
     * 
     * @return
     */
    List<String> getKeys();
    
    /**
     * Get the complete set of keys and value objects within the options.
     * @return
     */
    Map<String, Object> getEntries();

     /**
     * Get the object stored with the given key
     * @param key
     * @return
     */
     Object getValue(String key);
     
     /**
      * Set to the values in the given options. Shared keys will be updated,
      * keys not present will be added. Keys not shared will be unaffected.
     * @param o
     */
    void set(HashOptions o);

}
