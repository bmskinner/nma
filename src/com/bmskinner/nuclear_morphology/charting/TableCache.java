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


package com.bmskinner.nuclear_morphology.charting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.DisplayOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;

public class TableCache implements Cache {
    private Map<TableOptions, TableModel> tableMap = new HashMap<TableOptions, TableModel>();

    public TableCache() {

    }

    @Override
    public synchronized void add(@NonNull TableOptions options, @NonNull TableModel model) {
        tableMap.put(options, model);
    }

    @Override
    public synchronized void add(@NonNull ChartOptions options, @NonNull JFreeChart chart) {
    }

    @Override
    public TableModel get(TableOptions options) {
        return tableMap.get(options);
    }

    @Override
    public boolean has(TableOptions options) {
        return tableMap.containsKey(options);
    }

    /**
     * Remove all cached charts
     */
    @Override
    public void purge() {
        tableMap = new HashMap<TableOptions, TableModel>();
    }

    /**
     * Remove all cached charts
     */
    @Override
    public void clear() {
        this.purge();
    }

    /**
     * Remove caches containing any of the given datasets. These will be
     * recalculated at next call
     * 
     * @param list
     */
    @Override
    public void clear(List<IAnalysisDataset> list) {

        if (list == null || list.isEmpty()) {
            purge();
            return;
        }

        Set<DisplayOptions> toRemove = new HashSet<DisplayOptions>();

        // Find the options with the datasets
        for (IAnalysisDataset d : list) {
            for (DisplayOptions op : tableMap.keySet()) {

                if (!op.hasDatasets()) {
                    continue;
                }
                if (op.getDatasets().contains(d)) {
                    toRemove.add(op);
                }
            }
        }

        // Remove the options with the datasets
        for (DisplayOptions op : toRemove) {
            tableMap.remove(op);
        }
    }

    @Override
    public boolean has(ChartOptions options) {
        return false;

    }

    @Override
    public JFreeChart get(ChartOptions options) {
        return null;
    }

    @Override
    public synchronized void clear(ICell cell) {
        if (cell == null)
            return;

        // Make a list of the options that need removed
        // These are the options that contain the datasets in the list
        // Set<ChartOptions> toRemove = new HashSet<ChartOptions>();

        Iterator<TableOptions> it = tableMap.keySet().iterator();

        while (it.hasNext()) {
            TableOptions op = it.next();
            if (op.getCell() == cell) {

                tableMap.remove(op);
            }
        }

    }
}
