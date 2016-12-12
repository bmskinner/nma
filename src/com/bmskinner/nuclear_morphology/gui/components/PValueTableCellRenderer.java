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
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.table.DefaultTableCellRenderer;

import com.bmskinner.nuclear_morphology.utility.Constants;

@SuppressWarnings("serial")
public class PValueTableCellRenderer extends DefaultTableCellRenderer {

	public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {

		//Cells are by default rendered as a JLabel.
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		Color colour = Color.WHITE;

		NumberFormat nf = NumberFormat.getInstance();
		double pvalue = 1; 

		try {
			pvalue = nf.parse(value.toString()).doubleValue();

			if(pvalue <= Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL){
				colour = Color.YELLOW;
			}

			if(pvalue <= Constants.ONE_PERCENT_SIGNIFICANCE_LEVEL){
				colour = Color.GREEN;
			}
		} catch (ParseException e) {

		}

		setBackground(colour);

		return this;
	}



}

