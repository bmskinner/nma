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
package gui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import charting.DefaultTableOptions;
import charting.DefaultTableOptions.TableType;
import charting.TableOptions;
import charting.datasets.NucleusTableDatasetCreator;
import gui.components.ExportableTable;

public class VennDetailPanel extends DetailPanel {


	private static final long serialVersionUID = 1L;
	
	private JPanel mainPanel = new JPanel();
	
	private ExportableTable vennTable;

	public VennDetailPanel(Logger programLogger) {
		super(programLogger);
		this.setLayout(new BorderLayout());
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		JPanel vennPanel = new JPanel(new BorderLayout());
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(mainPanel);
		
		this.add(scrollPane, BorderLayout.CENTER);
		
		vennTable = new ExportableTable(NucleusTableDatasetCreator.createVennTable(null));
		vennPanel.add(vennTable, BorderLayout.CENTER);
		vennPanel.add(vennTable.getTableHeader(), BorderLayout.NORTH);
		mainPanel.add(vennPanel);
		vennTable.setEnabled(false);
		
	}
	
	/**
	 * Update the venn panel with data from the given datasets
	 * @param getDatasets() the datasets
	 */
	@Override
	public void updateDetail(){
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				updateVennTable();
				setUpdating(false);
			}});
	}

	private void updateVennTable(){
		programLogger.log(Level.FINE, "Updating venn panel");


		// format the numbers and make into a tablemodel
		TableModel model = NucleusTableDatasetCreator.createVennTable(null);

		if(hasDatasets()){

			TableOptions options = new DefaultTableOptions(getDatasets(), TableType.VENN);
			if(getTableCache().hasTable(options)){
				model = getTableCache().getTable(options);
			} else {
				model = NucleusTableDatasetCreator.createVennTable(getDatasets());
				getTableCache().addTable(options, model);
			}

		}
		vennTable.setModel(model);
		setRenderer(vennTable, new VennTableCellRenderer());

		programLogger.log(Level.FINEST, "Updated venn panel");
	}
	
	/**
	 * Colour table cell backsground to show pairwise comparisons. All cells are white, apart
	 * from the diagonal, which is made light grey
	 */
	class VennTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	      //Cells are by default rendered as a JLabel.
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        String cellContents = l.getText();
	        if(cellContents!=null && !cellContents.equals("")){ // ensure value

	        	String columnName = table.getColumnName(column);
	        	String[] array = cellContents.split("%");
		        String[] array2 = array[0].split("\\(");
		        
		        double pct;
		        try {
		        	
		        	NumberFormat nf = NumberFormat.getInstance();
		        	pct = nf.parse(array2[1]).doubleValue();
//		        	pct = Double.valueOf(array2[1]);
		        } catch (Exception e){
		        	programLogger.log(Level.FINEST, "Error getting value: "+cellContents+" in column "+columnName, e);
		        	pct = 0;
		        }
		        		        
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
