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


package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.Color;

import com.bmskinner.nuclear_morphology.charting.datasets.SignalTableCell;
import com.bmskinner.nuclear_morphology.gui.components.ConsistentRowTableCellRenderer;

/**
 * This allows a blank cell in a table to be coloured with a signal colour based
 * on the colour tag in the following cell, as stored within a SignalTableCell
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class SignalTableCellRenderer extends ConsistentRowTableCellRenderer {

    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        Color colour = Color.WHITE;

        try {
            if (row < table.getModel().getRowCount() - 1) {

                int nextRow = row + 1;

                if (nextRow < table.getModel().getRowCount()) { // ignore if no
                                                                // data

                    // get the value in the first column of the row below
                    String nextRowHeader = table.getModel().getValueAt(nextRow, 0).toString();

                    if (nextRowHeader.equals("Signal group")) {

                        // Check if the signal block has a signal group by
                        // looking at the next row
                        if (table.getModel().getValueAt(nextRow, column) != null) {
                            if (!table.getModel().getValueAt(nextRow, column).toString().equals("")) {

                                if (table.getModel().getValueAt(nextRow, column) instanceof SignalTableCell) {
                                    SignalTableCell cell = (SignalTableCell) table.getModel().getValueAt(nextRow,
                                            column);

                                    colour = cell.getColor();
                                }
                            }
                        }

                    }
                }
            }
        } catch (Exception e) {
            stack("Error in signal renderer", e);
            colour = Color.WHITE;
        }

        setBackground(colour);

        return this;
    }
}
