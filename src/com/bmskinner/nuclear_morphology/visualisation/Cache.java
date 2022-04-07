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

import java.util.List;

import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;

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
     * Remove caches containing any of the given datasets. These will be
     * recalculated at next call
     * 
     * @param list
     */
    void clear(List<IAnalysisDataset> list);

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

}
