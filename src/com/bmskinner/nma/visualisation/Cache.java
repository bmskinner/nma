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
package com.bmskinner.nma.visualisation;

import java.util.List;

import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.TableOptions;

public interface Cache {

	/**
	 * Remove all cached charts
	 */
	void purge();

	/*
	 * Removes all stored entries from the cache
	 */
	void clear();

	/**
	 * Remove caches containing any of the given objects. These will be recalculated
	 * at next call
	 * 
	 * @param list
	 */
	void clear(@NonNull List<?> list);

	/**
	 * Remove caches containing any of the given dataset. These will be recalculated
	 * at next call.
	 * 
	 * @param dataset
	 */
	void clear(@Nullable IAnalysisDataset dataset);

	/**
	 * Remove caches containing the given cell
	 * 
	 * @param cell
	 */
	void clear(ICell cell);

	boolean has(TableOptions options);

	TableModel get(TableOptions options);

	boolean has(ChartOptions options);

	JFreeChart get(ChartOptions options);

	void add(@NonNull ChartOptions options, @NonNull JFreeChart chart);

	void add(@NonNull TableOptions options, @NonNull TableModel model);

	/**
	 * Remove the cached chart for the given options if present
	 * 
	 * @param options
	 */
	void clear(ChartOptions options);

	/**
	 * Remove the cached table for the given options if present
	 * 
	 * @param options
	 */
	void clear(TableOptions options);

}
