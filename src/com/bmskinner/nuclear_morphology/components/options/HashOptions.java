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
import java.util.List;
import java.util.Map;

/**
 * The interface for all hash options classes
 * @author bms41
 *
 */
public interface HashOptions extends Serializable {

    static final String TOP_HAT_RADIUS = "Top hat radius";

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
     * Get the object stored with the given key as a string.
     * @param key
     * @return
     */
     Object getValue(String key);

}
