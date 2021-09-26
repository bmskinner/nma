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
package com.bmskinner.nuclear_morphology.components.signals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Collects values from individual nuclei for the shortest pairwise distances
 * between signals
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class PairwiseSignalDistanceCollection {

    private Map<UUID, Map<UUID, List<Double>>> values = new HashMap<UUID, Map<UUID, List<Double>>>();

    public PairwiseSignalDistanceCollection() {
    }

    public void addValue(PairwiseSignalDistanceValue v) {

        if (values.containsKey(v.getGroup1())) {
            // add with group 1 first
            addValue(v.getGroup1(), v.getGroup2(), v.getValue());
            return;
        }

        // if(values.containsKey(v.getGroup2())){
        // // add with group 2 first
        // addValue(v.getGroup2(), v.getGroup1(), v.getValue());
        // return;
        // }

        // Neither present. Create new group from id1
        Map<UUID, List<Double>> map = new HashMap<UUID, List<Double>>();
        values.put(v.getGroup1(), map);

        addValue(v.getGroup1(), v.getGroup2(), v.getValue());

    }

    private void addValue(UUID id1, UUID id2, double value) {
        Map<UUID, List<Double>> map = values.get(id1);

        if (!map.containsKey(id2)) {
            // Add new list
            List<Double> list = new ArrayList<Double>();
            map.put(id2, list);
        }

        map.get(id2).add(value);

    }

    /**
     * Get the primary keys
     * 
     * @return
     */
    public Set<UUID> getPrimaryIDs() {
        return values.keySet();
    }

    /**
     * Get the secondary keys
     * 
     * @return
     */
    public Set<UUID> getIDs(UUID id) {
        if (values.containsKey(id)) {
            return values.get(id).keySet();
        }
        throw new IllegalArgumentException("Primary ID is not present");
    }

    /**
     * Get all unique ids in the collection
     * 
     * @return
     */
    public Set<UUID> getIDs() {
        Set<UUID> ids = new HashSet<UUID>();
        ids.addAll(getPrimaryIDs());

        for (UUID p : getPrimaryIDs()) {
            ids.addAll(getIDs(p));
        }
        return ids;
    }

    public List<Double> getValues(UUID id1, UUID id2) {

        if (!values.containsKey(id1)) {
            return null;
        }

        return values.get(id1).get(id2);
    }

}
