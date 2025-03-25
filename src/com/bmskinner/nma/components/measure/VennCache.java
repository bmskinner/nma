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
package com.bmskinner.nma.components.measure;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;

/**
 * Store the number of cells overlapping between cell collections
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
public class VennCache {

    private Map<UUID, Integer> map = new HashMap<>();

    /**
     * Default constructor
     */
    public VennCache() { // No values on construction
    }

    /**
     * Add the number of shared nuclei with the given dataset
     * 
     * @param d the dataset
     * @param i the number of shared nuclei
     */
    public void addCount(@NonNull IAnalysisDataset d, int i) {
        addCount(d.getCollection(), i);
    }

    /**
     * Add the number of shared nuclei with the given collection
     * 
     * @param c the collection
     * @param i the number of shared nuclei
     */
    public void addCount(@NonNull ICellCollection c, int i) {
         map.put(c.getId(), i);
    }

    /**
     * Get the number of shared nuclei with the given dataset
     * 
     * @param d the dataset
     * @return the shared count
     */
    public int getCount(@NonNull IAnalysisDataset d) {
        return getCount(d.getCollection());
    }

    /**
     * Get the number of shared nuclei with the given collection
     * 
     * @param d the collection
     * @return the shared count
     */
    public int getCount(@NonNull ICellCollection c) {
    	if(!map.containsKey(c.getId()))
    		return 0;
        return map.get(c.getId());
    }

    /**
     * Check if the given dataset is present in the cache
     * 
     * @param d the dataset
     */
    public boolean hasCount(@NonNull IAnalysisDataset d) {
        return hasCount(d.getCollection());
    }

    /**
     * Check if the given collection is present in the cache
     * 
     * @param c the collection
     */
    public boolean hasCount(@NonNull ICellCollection c) {
        return map.containsKey(c.getId());
    }

}
