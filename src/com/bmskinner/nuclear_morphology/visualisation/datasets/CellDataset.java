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
package com.bmskinner.nuclear_morphology.visualisation.datasets;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.components.cells.ICell;

/**
 * Holds the information needed to draw a cell outline
 * 
 * @author bms41
 *
 */
public class CellDataset {

    private ICell                       cell;
    private Map<String, OutlineDataset<?>> outlines = new HashMap<>();

    private Map<String, XYDataset> tags = new HashMap<>();

    private Map<String, XYDataset> lobes = new HashMap<>();

    public CellDataset(ICell cell) {
        this.cell = cell;
    }

    public ICell getCell() {
        return cell;
    }

    public void addOutline(String key, OutlineDataset<?> ds) {
        outlines.put(key, ds);
    }

    public void addTags(String key, XYDataset ds) {
        tags.put(key, ds);
    }

    public void addLobes(String key, OutlineDataset<?> ds) {
        outlines.put(key, ds);
    }

    public Collection<OutlineDataset<?>> getDatasets() {
        return outlines.values();
    }

    public int getDatasetCount() {
        return outlines.size() + tags.size() + lobes.size();
    }

    public Set<String> getKeys() {
        Set<String> keys = new HashSet<>();

        keys.addAll(outlines.keySet());
        keys.addAll(tags.keySet());
        keys.addAll(lobes.keySet());
        return keys;
    }

    public XYDataset getDataset(String key) {
        if (outlines.containsKey(key)) {
            return outlines.get(key);
        }

        if (tags.containsKey(key)) {
            return tags.get(key);
        }

        if (lobes.containsKey(key)) {
            return lobes.get(key);
        }
        throw new IllegalArgumentException("Key not present: " + key);
    }

}
