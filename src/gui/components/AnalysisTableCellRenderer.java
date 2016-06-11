/*******************************************************************************
 *      Copyright (C) 2015, 2016 Ben Skinner
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
package gui.components;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JTable;

/**
 * Colour analysis parameter table cell background. If all the datasets selected
 * have the same value, colour them light green
 */
@SuppressWarnings("serial")
public class AnalysisTableCellRenderer extends ConsistentRowTableCellRenderer {

    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        
      //Cells are by default rendered as a JLabel.
        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if(isRowConsistentAcrossColumns(table, row)){
            
            Color colour = new Color(178, 255, 102);
            l.setBackground(colour);
            
        } else {
            l.setBackground(Color.WHITE);
        }

      //Return the JLabel which renders the cell.
      return l;
    }
    
}

