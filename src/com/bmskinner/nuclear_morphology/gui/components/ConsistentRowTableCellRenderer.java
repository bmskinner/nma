package com.bmskinner.nuclear_morphology.gui.components;

import java.awt.Color;

import javax.swing.JTable;

import com.bmskinner.nuclear_morphology.logging.Loggable;

@SuppressWarnings("serial")
public abstract class ConsistentRowTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer implements Loggable {

	public static final Color CONSISTENT_CELL_COLOUR = new Color(178, 255, 102);
	
    /**
     * Test if the values across the given row are consistent between columns
     * @param table
     * @param row
     * @return
     */
    protected boolean isRowConsistentAcrossColumns(JTable table, int row){
         
        boolean ok = true;
        if(table.getColumnCount()>2){ // don't colour single datasets
            
            Object test = table.getModel().getValueAt(row, 1);
            for(int col = 1; col<table.getColumnCount(); col++){
                Object value = table.getModel().getValueAt(row, col);
                
                // Ignore empty cells
                if(value==null){
//               
                	ok = false;
                } else {
                	 if(value.toString().equals("")){
                		 ok = false;
                	 }
                }
                
                if( ! test.equals(value)){
                    ok = false;
                }
            }

        } else{
            ok = false;
        }
        return ok;
    }
}
