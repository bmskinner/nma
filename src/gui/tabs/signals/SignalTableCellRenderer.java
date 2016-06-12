package gui.tabs.signals;

import java.awt.Color;

import javax.swing.JLabel;

import charting.datasets.SignalTableCell;
import gui.components.ConsistentRowTableCellRenderer;

/**
 * This allows a blank cell in a table to be coloured with a signal colour
 * based on the colour tag in the following cell, as stored within a SignalTableCell
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class SignalTableCellRenderer extends ConsistentRowTableCellRenderer {


	public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {

		// default cell colour is white
		Color colour = Color.WHITE;

		
		if(row<table.getModel().getRowCount()-1){
			
			int nextRow = row+1;
			// get the value in the first column of the row below
			String nextRowHeader = table.getModel().getValueAt(nextRow, 0).toString();

			if(nextRowHeader.equals("Signal group")){
				// we want to colour this cell preemptively
				// get the signal table cell from the table
				
				if( ! table.getModel().getValueAt(nextRow, column).toString().equals("")){
					SignalTableCell cell = (SignalTableCell) table.getModel().getValueAt(nextRow, column);

					colour = cell.getColor();
				}
				
				
			}
		}
		//Cells are by default rendered as a JLabel.
		JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		l.setBackground(colour);

		//Return the JLabel which renders the cell.
		return l;
	}
}
