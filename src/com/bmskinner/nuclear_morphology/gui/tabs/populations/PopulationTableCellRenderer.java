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
package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.awt.Color;
import java.awt.Component;
import java.awt.Paint;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;

/**
 * Allows for cell background to be coloured based on position in the
 * population list
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class PopulationTableCellRenderer extends DefaultTableCellRenderer {

    /**
     * Stores the row index of a cell that was selected as a key, and the order
     * in which it was selected as a value
     */
    private Map<Integer, Integer> indexList = new HashMap<>();

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

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Component l = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        l.setBackground(Color.WHITE); // only colour the selected rows

        if (indexList.containsKey(row)) {

            // check if the row is a cluster group
            Object columnOneObject = table.getModel().getValueAt(row, PopulationTreeTable.COLUMN_NAME);

            if (columnOneObject instanceof IAnalysisDataset) {

                IAnalysisDataset dataset = (IAnalysisDataset) columnOneObject;
                Paint colour = dataset.getDatasetColour().orElse(ColourSelecter.getColor(indexList.get(row)));

                l.setBackground((Color) colour);
            }

        }

        // Return the JLabel which renders the cell.
        return l;
    }
}
