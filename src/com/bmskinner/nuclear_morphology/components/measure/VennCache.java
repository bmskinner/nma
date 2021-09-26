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
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;

/**
 * Store the number of cells overlapping between datasets
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class VennCache {

    /**
     * The key to the venn cache
     * 
     * @author bms41
     * @since 1.13.4
     *
     */
    private class Key {
        private UUID id;

        public Key(UUID id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }

        private VennCache getOuterType() {
            return VennCache.this;
        }

    }

    private Map<Key, Integer> map = new HashMap<Key, Integer>();

    /**
     * Default constructor
     */
    public VennCache() {
    }

    /**
     * Add the number of shared nuclei with the given dataset
     * 
     * @param d
     *            the dataset
     * @param i
     *            the number of shared nuclei
     */
    public void addCount(@NonNull IAnalysisDataset d, int i) {
        Key k = new Key(d.getId());
        map.put(k, i);
    }

    /**
     * Add the number of shared nuclei with the given collection
     * 
     * @param d
     *            the collection
     * @param i
     *            the number of shared nuclei
     */
    public void addCount(@NonNull ICellCollection d, int i) {
        Key k = new Key(d.getID());
        map.put(k, i);
    }

    /**
     * Get the number of shared nuclei with the given dataset
     * 
     * @param d
     *            the dataset
     * @return the shared count
     */
    public int getCount(@NonNull IAnalysisDataset d) {
        Key k = new Key(d.getId());
        return map.get(k);
    }

    /**
     * Get the number of shared nuclei with the given collection
     * 
     * @param d
     *            the collection
     * @return the shared count
     */
    public int getCount(@NonNull ICellCollection d) {
        Key k = new Key(d.getID());
        return map.get(k);
    }

    /**
     * Check if the given dataset is present in the cache
     * 
     * @param d
     *            the dataset
     */
    public boolean hasCount(@NonNull IAnalysisDataset d) {
        Key k = new Key(d.getId());
        return map.containsKey(k);
    }

    /**
     * Check if the given collection is present in the cache
     * 
     * @param d
     *            the collection
     */
    public boolean hasCount(@NonNull ICellCollection d) {
        Key k = new Key(d.getID());
        return map.containsKey(k);
    }

}
