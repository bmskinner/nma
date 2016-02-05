package gui.components;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JTable;

/**
 * Colour analysis parameter table cell background. If all the datasets selected
 * have the same value, colour them light green
 */
@SuppressWarnings("serial")
public class AnalysisTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

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
    
    /**
     * Test if the values across the given row are consistent between columns
     * @param table
     * @param row
     * @return
     */
    private boolean isRowConsistentAcrossColumns(JTable table, int row){
         
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

