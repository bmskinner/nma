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


package com.bmskinner.nuclear_morphology.gui.components;

import java.awt.Color;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.bmskinner.nuclear_morphology.logging.Loggable;

@SuppressWarnings("serial")
public abstract class ConsistentRowTableCellRenderer extends DefaultTableCellRenderer
        implements Loggable {

    public static final Color CONSISTENT_CELL_COLOUR = new Color(178, 255, 102);

    /**
     * Test if the values across the given row are consistent between columns
     * 
     * @param table
     * @param row
     * @return
     */
    protected boolean isRowConsistentAcrossColumns(JTable table, int row) {

        if (table.getColumnCount() <= 2) // don't colour single datasets
        	return false;

        Object test = table.getModel().getValueAt(row, 1);
        for (int col = 1; col < table.getColumnCount(); col++) {
        	Object value = table.getModel().getValueAt(row, col);

        	// Ignore empty cells
        	if(value==null || value.toString().equals(""))
        		return false;

        	if (!test.equals(value))
        		return false;
        }
        return true;
    }
    
    /**
     * Get the text representation of the value in the first column of the row
     * @param row
     * @param table
     * @return
     */
    protected String getFirstColumnText(int row, JTable table) {
    	return table.getValueAt(row, 0).toString();    	
    }
}
