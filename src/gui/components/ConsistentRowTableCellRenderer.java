package gui.components;

import javax.swing.JTable;

@SuppressWarnings("serial")
public abstract class ConsistentRowTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

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
