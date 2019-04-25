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
package com.bmskinner.nuclear_morphology.gui.components.renderers;

import java.awt.Color;
import java.awt.Component;
import java.io.File;

import javax.swing.JTable;

import com.bmskinner.nuclear_morphology.gui.Labels;

/**
 * Colour analysis parameter table cell background. If all the datasets selected
 * have the same value, colour them light green
 */
@SuppressWarnings("serial")
public class AnalysisTableCellRenderer extends ConsistentRowTableCellRenderer {

    @Override
	public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        Color bg = Color.WHITE;
        Color fg = Color.BLACK;
        
        if (isRowConsistentAcrossColumns(table, row))
        	bg = CONSISTENT_CELL_COLOUR;
        
        String header = getFirstColumnText(row, table);
        if(header.equals(Labels.AnalysisParameters.COLLECTION_SOURCE) && column>0) {
        	if(!header.equals(Labels.NA_MERGE)) {
        		File f = new File(value.toString());
        		if(!f.exists())
        			fg = Color.RED;
        	}
        }

        c.setBackground(bg);
        c.setForeground(fg);
        return c;
    }

}
