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
package com.bmskinner.nuclear_morphology.visualisation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;

/*
 * Store rendered charts in a cache, to avoid slowdowns when reselecting datasets
 * Internally uses a Map<ChartOptions, JFreeChart>.
 */
public class ChartCache implements Cache {

	private final Map<ChartOptions, JFreeChart> chartMap = new ConcurrentHashMap<>();

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("Chart cache:\n");
		for (ChartOptions op : chartMap.keySet()) {
			b.append(op.hashCode() + Io.TAB);
			for (ChartOptions op2 : chartMap.keySet()) {
				b.append(op.equals(op2) + Io.TAB);
			}
			b.append(Io.NEWLINE);
		}
		return b.toString();
	}

	@Override
	public synchronized void add(@NonNull final ChartOptions options, @NonNull final JFreeChart chart) {
		chartMap.put(options, chart);
	}

	@Override
	public synchronized void add(@NonNull final TableOptions options, @NonNull final TableModel model) {
	}

	@Override
	public synchronized JFreeChart get(final ChartOptions options) {
		return chartMap.get(options);

	}

	@Override
	public synchronized boolean has(final ChartOptions options) {
		return chartMap.containsKey(options);
	}

	@Override
	public boolean has(final TableOptions options) {
		return false;
	}

	@Override
	public synchronized void purge() {
		chartMap.clear();
	}

	@Override
	public synchronized void clear() {
		this.purge();
	}

	@Override
	public void clear(IAnalysisDataset dataset) {

		// Make a list of the options that need removal
		// These are the options that contain the dataset
		Set<ChartOptions> toRemove = new HashSet<>();

		// Find the options with the dataset
		for (ChartOptions op : chartMap.keySet()) {
			if (op.hasDatasets() && op.getDatasets().contains(dataset)) {
				toRemove.add(op);
			}
		}

		// Remove the options with the dataset
		for (final ChartOptions op : toRemove) {
			chartMap.remove(op);
		}
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
		Set<ChartOptions> toRemove = new HashSet<>();

		// Find the options with the datasets
		for (IAnalysisDataset d : list) {
			for (ChartOptions op : chartMap.keySet()) {
				if (op.hasDatasets() && op.getDatasets().contains(d)) {
					toRemove.add(op);
				}
			}
		}

		// Remove the options with the datasets
		for (final ChartOptions op : toRemove) {
			chartMap.remove(op);
		}
	}

	@Override
	public synchronized void clear(final ICell cell) {
		if (cell == null)
			return;

		// Make a list of the options that need removed
		// These are the options that contain the datasets in the list
		Iterator<ChartOptions> it = chartMap.keySet().iterator();

		while (it.hasNext()) {
			ChartOptions op = it.next();
			if (op.getCell() == cell) {
				chartMap.remove(op);
			}
		}

	}

	@Override
	public TableModel get(final TableOptions options) {
		return null;
	}

}
