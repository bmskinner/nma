package gui.components;

import java.awt.Color;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.JLabel;

import utility.Constants;

/**
 * Colour a table cell background based on its value to show statistical 
 * significance. Shows yellow for values below a Bonferroni-corrected cutoff
 * of 0.05, and green for values below a Bonferroni-corrected cutoff
 * of 0.01
 */
public class WilcoxonTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

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
	        
	        double fivePct = Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL / divisor; // Bonferroni correction
	        double onePct = Constants.ONE_PERCENT_SIGNIFICANCE_LEVEL /   divisor;
//	        IJ.log("Columns: "+table.getColumnCount());
	        
	        if(pvalue<=fivePct){
	        	colour = Color.YELLOW;
	        }
	        
	        if(pvalue<=onePct){
	        	colour = Color.GREEN;
	        }
	        l.setBackground(colour);

        } else {
            l.setBackground(Color.LIGHT_GRAY);
        }

      //Return the JLabel which renders the cell.
      return l;
    }
}
