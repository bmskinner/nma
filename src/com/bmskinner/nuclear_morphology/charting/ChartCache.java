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

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.io.Exporter;

/*
 * Store rendered charts in a cache, to avoid slowdowns when reselecting datasets
 * Internally uses a Map<ChartOptions, JFreeChart>.
 */
public class ChartCache implements Cache {

    private Map<ChartOptions, JFreeChart> chartMap = new HashMap<ChartOptions, JFreeChart>();

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Chart cache:\n");
        for (ChartOptions op : chartMap.keySet()) {
            b.append(op.hashCode() + Exporter.TAB);
            for (ChartOptions op2 : chartMap.keySet()) {
                b.append(op.equals(op2) + Exporter.TAB);
            }
            b.append(Exporter.NEWLINE);
        }
        return b.toString();
    }

    @Override
    public synchronized void add(ChartOptions options, JFreeChart chart) {
        chartMap.put(options, chart);
    }

    @Override
    public synchronized void add(TableOptions options, TableModel model) {
    }

    @Override
    public synchronized JFreeChart get(ChartOptions options) {
        return chartMap.get(options);

    }

    @Override
    public synchronized boolean has(ChartOptions options) {
        return chartMap.containsKey(options);
    }
    
    @Override
    public boolean has(TableOptions options) {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see charting.Cache#purge()
     */
    @Override
    public synchronized void purge() {
        chartMap = new HashMap<ChartOptions, JFreeChart>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see charting.Cache#refresh()
     */
    @Override
    public synchronized void clear() {
        this.purge();
    }

    @Override
    public synchronized void clear(List<IAnalysisDataset> list) {

        // If the list is malformed, clear everything
        if (list == null || list.isEmpty()) {
            purge();
            return;
        }

        // Make a list of the options that need removed
        // These are the options that contain the datasets in the list
        Set<ChartOptions> toRemove = new HashSet<ChartOptions>();

        // Find the options with the datasets
        for (IAnalysisDataset d : list) {
            for (ChartOptions op : this.chartMap.keySet()) {
                if (op.hasDatasets()) {
                    if (op.getDatasets().contains(d)) {
                        toRemove.add(op);
                        finest("Need to remove options with dataset " + d.getName());
                    }
                }
            }
        }

        // Remove the options with the datasets
        for (ChartOptions op : toRemove) {
            finest("Clearing options");
            chartMap.remove(op);
        }
    }

    @Override
    public synchronized void clear(ICell cell) {
        if (cell == null) {
            return;
        }

        // Make a list of the options that need removed
        // These are the options that contain the datasets in the list
        // Set<ChartOptions> toRemove = new HashSet<ChartOptions>();

        Iterator<ChartOptions> it = chartMap.keySet().iterator();

        while (it.hasNext()) {
            ChartOptions op = it.next();
            if (op.getCell() == cell) {

                chartMap.remove(op);
            }
        }

    }

    @Override
    public TableModel get(TableOptions options) {
        return null;
    }
}
