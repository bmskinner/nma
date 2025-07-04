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
package com.bmskinner.nma.gui.components.renderers;

import java.awt.Color;
import java.awt.Component;

import javax.swing.table.DefaultTableCellRenderer;

/**
 * Colour a table cell grey if it is null or empty. Use for diagonals in
 * pairwise tables
 */
public class PairwiseTableCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        // Cells are by default rendered as a JLabel.
    	Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        String cellContents = value.toString();
        if (cellContents == null || cellContents.equals("")) {
            c.setBackground(Color.LIGHT_GRAY);
        } else {
            c.setBackground(Color.WHITE);
        }

        // Return the JLabel which renders the cell.
        return c;
    }
}
