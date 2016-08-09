package gui.tabs.populations;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import analysis.AnalysisDataset;
import gui.components.ColourSelecter;
import logging.Loggable;

@SuppressWarnings("serial")
public /**
 * Allows for cell background to be coloured based on position in the population list
*
*/
class PopulationTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer implements Loggable {

	List<Integer> indexList = new ArrayList<Integer>(0);
	
	
	
	public PopulationTableCellRenderer(List<Integer> list){
		super();
		this.indexList = list;
	}
	
	public PopulationTableCellRenderer(){
		super();
	}
	
	public void update(List<Integer> list){
		indexList = list;
	}

	public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
       
       Component l = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

       l.setBackground(Color.WHITE); // only colour the selected rows
       
       if (indexList.contains(row)) {
    	  
       	    	   
    	   // check if the row is a cluster group
    	   Object columnOneObject = table.getModel().getValueAt(row, PopulationsPanel.COLUMN_NAME);

    	   if(columnOneObject instanceof AnalysisDataset){
    		   
    		   AnalysisDataset dataset = (AnalysisDataset) columnOneObject;      	

    		   // if a preferred colour is specified, use it, otherwise go for defaults
    		   Color colour = dataset.hasDatasetColour()
    				        ? dataset.getDatasetColour()
    				        : ColourSelecter.getColor(indexList.indexOf(row));
    						

    		   l.setBackground(colour);
    	   }


       } 

     //Return the JLabel which renders the cell.
     return l;
   }
}
