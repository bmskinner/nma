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
import java.awt.Component;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.bmskinner.nuclear_morphology.stats.SignificanceTest;

@SuppressWarnings("serial")
public class PValueTableCellRenderer extends DefaultTableCellRenderer {

	@Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        // Cells are by default rendered as a JLabel.
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        Color bg = Color.WHITE;

        NumberFormat nf = NumberFormat.getInstance();
        double pvalue = 1;

        try {
            pvalue = nf.parse(value.toString()).doubleValue();

            if (pvalue <= SignificanceTest.FIVE_PERCENT_SIGNIFICANCE_LEVEL) {
            	bg = Color.YELLOW;
            }

            if (pvalue <= SignificanceTest.ONE_PERCENT_SIGNIFICANCE_LEVEL) {
            	bg = Color.GREEN;
            }
        } catch (ParseException e) {
        	bg = Color.WHITE;
        }

        c.setBackground(bg);

        return c;
    }

}
