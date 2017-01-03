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

import javax.swing.JLabel;

import com.bmskinner.nuclear_morphology.stats.SignificanceTest;

/**
 * Colour a table cell background based on its value to show statistical 
 * significance. Shows yellow for values below a Bonferroni-corrected cutoff
 * of 0.05, and green for values below a Bonferroni-corrected cutoff
 * of 0.01
 */
public class WilcoxonTableCellRenderer extends PairwiseTableCellRenderer {

	private static final long serialVersionUID = 1L;

	public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        
      //Cells are by default rendered as a JLabel.
        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        String cellContents = l.getText();
        if(cellContents!=null && !cellContents.equals("")){ // ensure value
//        	
        	
        	NumberFormat nf = NumberFormat.getInstance();
        	double pvalue = 1; 
        	
        	try {
        		pvalue = nf.parse(cellContents).doubleValue();
        	} catch (ParseException e) {

        		// Do nothing for now, just use the default color
//        		programLogger.log(Level.FINEST, "Parsing error in Wilcoxon renederer", e);
        	}
	        
	        Color colour = Color.WHITE; // default
	        
	        int numberOfTests = 5; // correct for the different variables measured;
	        double divisor = (double) (   (table.getColumnCount()-2)  * numberOfTests); // for > 2 datasets with numberOFtests tests per dataset
	        
	        double fivePct = SignificanceTest.FIVE_PERCENT_SIGNIFICANCE_LEVEL / divisor; // Bonferroni correction
	        double onePct = SignificanceTest.ONE_PERCENT_SIGNIFICANCE_LEVEL /   divisor;
//	        IJ.log("Columns: "+table.getColumnCount());
	        
	        if(pvalue<=fivePct){
	        	colour = Color.YELLOW;
	        }
	        
	        if(pvalue<=onePct){
	        	colour = Color.GREEN;
	        }
	        l.setBackground(colour);

        }

      //Return the JLabel which renders the cell.
      return l;
    }
}
