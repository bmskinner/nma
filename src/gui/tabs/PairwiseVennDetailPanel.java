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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.TableOptions.TableType;
import charting.options.TableOptionsBuilder;
import gui.components.ExportableTable;

@SuppressWarnings("serial")
public class PairwiseVennDetailPanel extends DetailPanel {
	
	private JPanel mainPanel = new JPanel();

	private ExportableTable pairwiseVennTable;

	public PairwiseVennDetailPanel(Logger programLogger) {
		super(programLogger);
		this.setLayout(new BorderLayout());
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		try {
				
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(mainPanel);
		
		this.add(scrollPane, BorderLayout.CENTER);
		
		
		JPanel pairwisePanel = new JPanel(new BorderLayout());
		
		TableOptions options = new TableOptionsBuilder()
		.setDatasets(null)
		.setLogger(programLogger)
		.setType(TableType.VENN)
		.build();

		TableModel model = getTable(options);

		pairwiseVennTable = new ExportableTable(model);
				
		pairwisePanel.add(pairwiseVennTable, BorderLayout.CENTER);
		pairwisePanel.add(pairwiseVennTable.getTableHeader(), BorderLayout.NORTH);
		mainPanel.add(pairwisePanel);
		pairwiseVennTable.setEnabled(false);
		
		} catch (Exception e){
			programLogger.log(Level.SEVERE, "Error updating pairwise venn table", e);
		}
		
	}
	
	@Override
	protected void updateSingle() throws Exception {
		updateNull();
	}
	

	@Override
	protected void updateMultiple() throws Exception {
		programLogger.log(Level.FINE, "Updating pairwise venn table");


		// format the numbers and make into a tablemodel
		
		
		TableOptions options = new TableOptionsBuilder()
		.setDatasets(getDatasets())
		.setLogger(programLogger)
		.setType(TableType.PAIRWISE_VENN)
		.build();

		TableModel model = getTable(options);
		pairwiseVennTable.setModel(model);
		setRenderer(pairwiseVennTable, new PairwiseVennTableCellRenderer());
		
		programLogger.log(Level.FINEST, "Updated pairwise venn panel");
	}
	
	@Override
	protected void updateNull() throws Exception {
		TableOptions options = new TableOptionsBuilder()
		.setDatasets(null)
		.setLogger(programLogger)
		.setType(TableType.PAIRWISE_VENN)
		.build();

		TableModel model = getTable(options);
		pairwiseVennTable.setModel(model);
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return  NucleusTableDatasetCreator.createPairwiseVennTable(options);
	}
		
	/**
	 * Colour table cell background to show pairwise comparisons. All cells are white, apart
	 * from the diagonal, which is made light grey
	 */
	class PairwiseVennTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	      //Cells are by default rendered as a JLabel.
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        String cellContents = l.getText();
	        
	        String columnName = table.getColumnName(column);
	        if(  (columnName.equals("Unique %") || columnName.equals("Shared %"))){
	        	
		        double pct;
		        try {
		        	
		        	NumberFormat nf = NumberFormat.getInstance();
		        	pct = nf.parse(cellContents).doubleValue();
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
		       
	        }
//	        } else {
//	            l.setBackground(Color.LIGHT_GRAY);
//	        }

	      //Return the JLabel which renders the cell.
	      return l;
	    }
	}
}

