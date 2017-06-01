/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.components;

import java.awt.Color;

import javax.swing.JLabel;

/**
 * Colour a table cell grey if it is null or empty. Use for diagonals in
 * pairwise tables
 */
public class PairwiseTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        // Cells are by default rendered as a JLabel.
        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        String cellContents = l.getText();
        if (cellContents == null || cellContents.equals("")) {
            l.setBackground(Color.LIGHT_GRAY);
        } else {
            l.setBackground(Color.WHITE);
        }

        // Return the JLabel which renders the cell.
        return l;
    }
}
