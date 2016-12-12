package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.awt.Color;
import java.awt.Component;
import java.awt.Paint;
import java.util.HashMap;
import java.util.Map;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * @author ben
 *
 */
@SuppressWarnings("serial")
public /**
 * Allows for cell background to be coloured based on position in the population list
*
*/
class PopulationTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer implements Loggable {

	
	/**
	 * Stores the row index of a cell that was selected as a key, and the
	 * order in which it was selected as a value
	 */
	Map<Integer, Integer> indexList = new HashMap<Integer, Integer>(0);
	
	
	
	public PopulationTableCellRenderer(Map<Integer, Integer> list){
		super();
		this.indexList = list;
	}
	
	public PopulationTableCellRenderer(){
		super();
	}
	
	public void update(Map<Integer, Integer> list){
		indexList = list;
	}

	public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
       
       Component l = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

       l.setBackground(Color.WHITE); // only colour the selected rows
       
       if (indexList.containsKey(row)) {
    	  
       	    	   
    	   // check if the row is a cluster group
    	   Object columnOneObject = table.getModel().getValueAt(row, PopulationTreeTable.COLUMN_NAME);

    	   if(columnOneObject instanceof IAnalysisDataset){
    		   
    		   IAnalysisDataset dataset = (IAnalysisDataset) columnOneObject;      	

    		   // if a preferred colour is specified, use it, otherwise go for defaults
    		   Paint colour = dataset.hasDatasetColour()
    				        ? dataset.getDatasetColour()
    				        : ColourSelecter.getColor(indexList.get(row));
    						

    		   l.setBackground((Color) colour);
    	   }


       } 

     //Return the JLabel which renders the cell.
     return l;
   }
}
