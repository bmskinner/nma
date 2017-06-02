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

/**
 * Colour analysis parameter table cell background. If all the datasets selected
 * have the same value, colour them light green
 */
@SuppressWarnings("serial")
public class AnalysisTableCellRenderer extends ConsistentRowTableCellRenderer {

    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        // Cells are by default rendered as a JLabel.
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (isRowConsistentAcrossColumns(table, row)) {

            Color colour = new Color(178, 255, 102);
            setBackground(colour);

        } else {
            setBackground(Color.WHITE);
        }

        // Return the JLabel which renders the cell.
        return this;
    }

}
