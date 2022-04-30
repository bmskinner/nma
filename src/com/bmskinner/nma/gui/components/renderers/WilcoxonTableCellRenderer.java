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
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.JLabel;
import javax.swing.JTable;

import com.bmskinner.nma.stats.SignificanceTest;

/**
 * Colour a table cell background based on its value to show statistical
 * significance. Shows yellow for values below a Bonferroni-corrected cutoff of
 * 0.05, and green for values below a Bonferroni-corrected cutoff of 0.01
 */
public class WilcoxonTableCellRenderer extends PairwiseTableCellRenderer {

    private static final long serialVersionUID = 1L;

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

    	Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    	Color bg = c.getBackground();
    	
    
        String cellContents = ((JLabel)c).getText();
        if (cellContents != null && !cellContents.equals("")) { // ensure value

            NumberFormat nf = NumberFormat.getInstance();
            double pvalue = 1;

            try {
                pvalue = nf.parse(cellContents).doubleValue();
            } catch (ParseException e) { 
            	e.printStackTrace();
            }

            if (pvalue <= SignificanceTest.FIVE_PERCENT_SIGNIFICANCE_LEVEL)
            	bg = Color.YELLOW;

            if (pvalue <= SignificanceTest.ONE_PERCENT_SIGNIFICANCE_LEVEL)
            	bg = Color.GREEN;
            
        }
        c.setBackground(bg);
        return c;
    }
}
