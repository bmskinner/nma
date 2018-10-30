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
package com.bmskinner.nuclear_morphology.charting.options;

import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.bmskinner.nuclear_morphology.charting.options.DefaultTableOptions.TableType;

/**
 * This interface describes the values that should be checkable by table dataset
 * creators. Implementing classes must provide sensible defaults.
 * 
 * @author bms41
 *
 */
public interface TableOptions extends DisplayOptions {

    /**
     * A renderer is applied to all columns in a table
     */
    static final int ALL_COLUMNS = 0;

    /**
     * A renderer is applied to only the first column in a table
     */
    static final int FIRST_COLUMN = -1;

    /**
     * A renderer is applied to all columns in a table apart from the first
     * column
     */
    static final int ALL_EXCEPT_FIRST_COLUMN = -2;

    /**
     * Get the table type to be drawn
     * 
     * @return
     */
    TableType getType();

    /**
     * Get the table the resulting model should be loaded into. Used by the
     * TableFactoryWorker in a DetailPanel
     * 
     * @return
     */
    JTable getTarget();

    /**
     * Check if a target has been set for the table model created from this
     * options
     * 
     * @return
     */
    boolean hasTarget();

    /**
     * Get the renderer for the given column to apply to the final table model
     * 
     * @return
     */
    TableCellRenderer getRenderer(int i);

    /**
     * Get the rendering options. These are detailed as the column index, with special
     * indices for {@code ALL_COLUMNS}, {@code FIRST_COLUMN}, and {@code ALL_EXCEPT_FIRST_COLUMN}
     * 
     * @return
     */
    Set<Integer> getRendererColumns();
        
    boolean isNormalised();

}
