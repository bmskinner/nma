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
package com.bmskinner.nuclear_morphology.components.measure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Store plottable statistics for the collection
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class StatsCache {

    // Need to be able to store the same stat for different components of the
    // cell.
    // This requires keys on component and stat
    public class Key {

        private final Measurement stat;
        private final String             component;
        private final MeasurementScale   scale;
        private final UUID 				 id;

        public Key(Measurement stat, String component, MeasurementScale scale, UUID id) {
            this.stat = stat;
            this.component = component;
            this.scale = scale;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Key))
                return false;
            Key key = (Key) o;

            if (!stat.equals(key.stat))
                return false;

            if (!component.equals(key.component))
                return false;

            if (!scale.equals(key.scale))
                return false;
            
            if(id!=null)
            	if(!id.equals(key.id))
            		return false;
            
            if(id==null && key.id!=null)
            	return false;

            return true;
        }

        @Override
        public int hashCode() {

            final int prime = 31;
            int result = 1;
            result = prime * result + stat.hashCode();

            result = prime * result + component.hashCode();

            result = prime * result + scale.hashCode();
            
            if(id!=null)
            	result = prime * result + id.hashCode();

            return result;
        }

    }

    private Map<Key, Double>   cache  = new HashMap<>();   // median
                                                                      // values
    private Map<Key, double[]> values = new HashMap<>(); // individual
                                                                      // component
                                                                      // values

    public StatsCache() {
    }

    /**
     * Store the given statistic
     * 
     * @param stat
     * @param scale
     * @param d
     */
    public void setMedian(Measurement stat, String component, MeasurementScale scale, UUID id, double d) {

        Key key = new Key(stat, component, scale, id);

        cache.put(key, d);

    }

    /**
     * Store the given statistic
     * 
     * @param stat
     * @param scale
     * @param d
     */
    public void setValues(Measurement stat, String component, MeasurementScale scale, UUID id, double[] list) {

        Key key = new Key(stat, component, scale, id);
        values.put(key, list);
    }

    public double getMedian(Measurement stat, String component, MeasurementScale scale, UUID id) {

        if (this.hasMedian(stat, component, scale, id)) {
            Key key = new Key(stat, component, scale, id);
            return cache.get(key);
        }
        return 0;
    }

    /**
     * Clear the values for the given stat
     * 
     * @param stat
     * @param component
     * @param scale
     */
    public void clear(Measurement stat, String component, MeasurementScale scale, UUID id) {
        Key key = new Key(stat, component, scale, id);
        values.remove(key);
        cache.remove(key);
    }

    /**
     * Clear the values for the given stat
     * 
     * @param stat
     * @param component
     * @param scale
     */
    public void clear(Measurement stat, String component, UUID id) {

        for (MeasurementScale s : MeasurementScale.values()) {
            Key key = new Key(stat, component, s, id);
            values.remove(key);
            cache.remove(key);
        }
    }

    /**
     * Clear the values for the given scale
     * 
     * @param scale
     * @param scale
     */
    public void clear(MeasurementScale scale) {

        Iterator<Key> it = values.keySet().iterator();
        while (it.hasNext()) {
            Key key = it.next();

            if (key.scale.equals(scale)) {
                it.remove();
                cache.remove(key);
            }
        }
    }

    /**
     * Get the raw values from the cache
     * 
     * @param stat
     * @param component
     * @param scale
     * @return
     */
    public double[] getValues(Measurement stat, String component, MeasurementScale scale, UUID id) {

        if (this.hasValues(stat, component, scale, id)) {
            Key key = new Key(stat, component, scale, id);
            return values.get(key);
        }
        return new double[0];
    }

    public boolean hasMedian(Measurement stat, String component, MeasurementScale scale, UUID id) {

        Key key = new Key(stat, component, scale, id);
        return cache.containsKey(key);

    }

    /**
     * Check if the cache has raw values for the give stat
     * 
     * @param stat
     * @param component
     * @param scale
     * @return
     */
    public boolean hasValues(Measurement stat, String component, MeasurementScale scale, UUID id) {

        Key key = new Key(stat, component, scale, id);
        return values.containsKey(key);

    }
}
