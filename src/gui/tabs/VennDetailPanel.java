/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package gui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import analysis.AnalysisDataset;
import charting.datasets.NucleusTableDatasetCreator;

public class VennDetailPanel extends DetailPanel {


	private static final long serialVersionUID = 1L;
	
	private JTable vennTable;

	public VennDetailPanel(Logger programLogger) {
		super(programLogger);
		this.setLayout(new BorderLayout());
		
		vennTable = new JTable(NucleusTableDatasetCreator.createVennTable(null));
		this.add(vennTable, BorderLayout.CENTER);
		vennTable.setEnabled(false);
		this.add(vennTable.getTableHeader(), BorderLayout.NORTH);

	}
	
	/**
	 * Update the venn panel with data from the given datasets
	 * @param list the datasets
	 */
	public void update(final List<AnalysisDataset> list){
		programLogger.log(Level.FINE, "Updating venn panel");
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
			
				
				// format the numbers and make into a tablemodel
				TableModel model = NucleusTableDatasetCreator.createVennTable(null);
				
				if(!list.isEmpty() && list!=null){
					model = NucleusTableDatasetCreator.createVennTable(list);
				}
				vennTable.setModel(model);
				
				int columns = vennTable.getColumnModel().getColumnCount();

				if(columns>1){
					for(int i=1;i<columns;i++){
						vennTable.getColumnModel().getColumn(i).setCellRenderer(new VennTableCellRenderer());
					}
				}
				programLogger.log(Level.FINEST, "Updated venn panel");
		}});
	}
	
	/**
	 * Colour table cell background to show pairwise comparisons. All cells are white, apart
	 * from the diagonal, which is made light grey
	 */
	class VennTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	      //Cells are by default rendered as a JLabel.
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        String cellContents = l.getText();
	        if(cellContents!=null && !cellContents.equals("")){ // ensure value
//	        	IJ.log(cellContents);
	        	String[] array = cellContents.split("%");
//	        	 IJ.log(array[0]);
		        String[] array2 = array[0].split("\\(");
//		        IJ.log(array2[1]);
		        double pct = Double.valueOf(array2[1]);
		        
//		        IJ.log("Pct: "+pct);
		        double colourIndex = 255 - ((pct/100) * 255);
		        
		        Color colour = new Color((int) colourIndex,(int) colourIndex, 255);
		        l.setBackground(colour);
		        
		        if(pct>60){
		        	l.setForeground(Color.WHITE);
		        } else {
		        	l.setForeground(Color.black);
		        }
		        
	        } else {
	            l.setBackground(Color.LIGHT_GRAY);
	        }

	      //Return the JLabel which renders the cell.
	      return l;
	    }
	}

}
