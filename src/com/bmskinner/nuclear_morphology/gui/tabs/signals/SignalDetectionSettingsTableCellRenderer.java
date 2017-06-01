/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.Color;

import javax.swing.JLabel;

import com.bmskinner.nuclear_morphology.charting.datasets.SignalTableCell;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ConsistentRowTableCellRenderer;

/**
 * Colour analysis parameter table cell background. If all the datasets selected
 * have the same value, colour them light green. Takes into consideration the
 * background colous from signal groups
 */
@SuppressWarnings("serial")
public class SignalDetectionSettingsTableCellRenderer extends ConsistentRowTableCellRenderer {

    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        // default cell colour is white
        Color colour = Color.WHITE;

        try {

            // Highlight consistent rows
            if (isRowConsistentAcrossColumns(table, row)) {

                colour = ConsistentRowTableCellRenderer.CONSISTENT_CELL_COLOUR;

            }

            // Set colour of signal groups

            if (row < table.getModel().getRowCount() - 1) {

                // get the value in the first column of the row below
                String nextRowHeader = table.getModel().getValueAt(row + 1, 0).toString();

                int signalGroupCount = Integer.valueOf(table.getModel().getValueAt(0, column).toString());

                if (nextRowHeader.equals(Labels.SIGNAL_GROUP_LABEL)) {

                    if (signalGroupCount > 0) {
                        // we want to colour this cell preemptively
                        // get the signal table cell from the table
                        String nextRowValue = table.getModel().getValueAt(row + 1, column).toString();
                        if (!nextRowValue.equals("")) {
                            SignalTableCell cell = (SignalTableCell) table.getModel().getValueAt(row + 1, column);

                            colour = cell.getColor();
                        }

                    } else {
                        colour = Color.WHITE; // don't allow consitentcy colours
                                              // in the signal group line
                    }

                }
            }

        } catch (Exception e) {
            warn("Error in signal detection table renderer");
            stack("Error in signal detection table renderer", e);
        }

        // Cells are by default rendered as a JLabel.
        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        l.setBackground(colour);

        // Return the JLabel which renders the cell.
        return l;
    }
}
