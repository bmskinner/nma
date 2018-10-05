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
package com.bmskinner.nuclear_morphology.charting.datasets.tables;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.charting.datasets.AbstractDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.gui.Labels;

/**
 * Abstract class for making tables
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public abstract class AbstractTableCreator extends AbstractDatasetCreator<TableOptions> {

    /**
     * Create with a set of table options
     */
    public AbstractTableCreator(@NonNull final TableOptions o) {
        super(o);
    }

    /**
     * Create an empty table declaring no data is loaded
     * 
     * @return
     */
    @NonNull public static TableModel createBlankTable() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn(Labels.NO_DATA_LOADED);
        return model;
    }

    /**
     * Create an empty table declaring no data is loaded
     * 
     * @return
     */
    @NonNull public static TableModel createLoadingTable() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn(Labels.LOADING_DATA);
        return model;
    }

}
