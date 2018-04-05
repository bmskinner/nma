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


package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.awt.Color;
import java.awt.Component;
import java.awt.Paint;
import java.util.HashMap;
import java.util.Map;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * @author ben
 *
 */
@SuppressWarnings("serial")
public /**
        * Allows for cell background to be coloured based on position in the
        * population list
        *
        */
class PopulationTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer implements Loggable {

    /**
     * Stores the row index of a cell that was selected as a key, and the order
     * in which it was selected as a value
     */
    Map<Integer, Integer> indexList = new HashMap<Integer, Integer>(0);

    public PopulationTableCellRenderer(Map<Integer, Integer> list) {
        super();
        this.indexList = list;
    }

    public PopulationTableCellRenderer() {
        super();
    }

    public void update(Map<Integer, Integer> list) {
        indexList = list;
    }

    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Component l = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        l.setBackground(Color.WHITE); // only colour the selected rows

        if (indexList.containsKey(row)) {

            // check if the row is a cluster group
            Object columnOneObject = table.getModel().getValueAt(row, PopulationTreeTable.COLUMN_NAME);

            if (columnOneObject instanceof IAnalysisDataset) {

                IAnalysisDataset dataset = (IAnalysisDataset) columnOneObject;

                // if a preferred colour is specified, use it, otherwise go for
                // defaults
                Paint colour = dataset.hasDatasetColour() ? dataset.getDatasetColour()
                        : ColourSelecter.getColor(indexList.get(row));

                l.setBackground((Color) colour);
            }

        }

        // Return the JLabel which renders the cell.
        return l;
    }
}
