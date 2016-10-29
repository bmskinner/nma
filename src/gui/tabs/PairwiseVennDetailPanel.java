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
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.datasets.AnalysisDatasetTableCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.DefaultTableOptions.TableType;
import charting.options.TableOptionsBuilder;
import gui.components.ExportableTable;

@SuppressWarnings("serial")
public class PairwiseVennDetailPanel extends DetailPanel {
	
	private JPanel mainPanel = new JPanel();

	private ExportableTable pairwiseVennTable;

	public PairwiseVennDetailPanel() {
		super();
		this.setLayout(new BorderLayout());
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		try {
				
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(mainPanel);
		
		this.add(scrollPane, BorderLayout.CENTER);
		
		
		JPanel pairwisePanel = new JPanel(new BorderLayout());
		
		TableOptions options = new TableOptionsBuilder()
		.setDatasets(null)
		.setType(TableType.PAIRWISE_VENN)
		.build();

		TableModel model = new AnalysisDatasetTableCreator(options).createPairwiseVennTable();

		pairwiseVennTable = new ExportableTable(model);
				
		pairwisePanel.add(pairwiseVennTable, BorderLayout.CENTER);
		pairwisePanel.add(pairwiseVennTable.getTableHeader(), BorderLayout.NORTH);
		mainPanel.add(pairwisePanel);
		pairwiseVennTable.setEnabled(false);
		
		} catch (Exception e){
			log(Level.SEVERE, "Error updating pairwise venn table", e);
		}
		
	}
	
	@Override
	protected void updateSingle() {
		updateNull();
	}
	

	@Override
	protected void updateMultiple() {
		fine("Updating pairwise venn table for multiple datasets");
	
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setType(TableType.PAIRWISE_VENN)
			.setTarget(pairwiseVennTable)
			.build();
		
		setTable(options);

//		TableModel model = getTable(options);
//		pairwiseVennTable.setModel(model);
		setRenderer(pairwiseVennTable, new PairwiseVennTableCellRenderer());
		
		finest("Updated pairwise venn panel");
	}
	
	@Override
	protected void updateNull() {
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(null)
			.setType(TableType.PAIRWISE_VENN)
			.build();
		
		setTable(options);
//
//		TableModel model = getTable(options);
//		pairwiseVennTable.setModel(model);
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return new AnalysisDatasetTableCreator(options).createPairwiseVennTable();
	}
		
	/**
	 * Colour table cell background to show pairwise comparisons. All cells are white, apart
	 * from the diagonal, which is made light grey
	 */
	class PairwiseVennTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
			Color backColour = Color.WHITE;
			Color foreColour = Color.BLACK;
			
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        String cellContents = l.getText();
	        
	        String columnName = table.getColumnName(column);
	        if(  (columnName.equals("Unique %") || columnName.equals("Shared %"))){
//	        	finest("Column name: "+columnName);
		        double pct;
		        try {
		        	
		        	NumberFormat nf = NumberFormat.getInstance();
		        	pct = nf.parse(cellContents).doubleValue();
		        } catch (Exception e){
		        	log(Level.FINEST, "Error getting value: "+cellContents+" in column "+columnName, e);
		        	pct = 0;
		        }
		        
		        double colourIndex = 255 - ((pct/100) * 255);
		        
		        backColour = new Color((int) colourIndex,(int) colourIndex, 255);

		        
		        if(pct>60){
		        	foreColour = Color.WHITE;
		        	
		        }
		       
	        }
	        
	        l.setBackground(backColour);
	        l.setForeground(foreColour);

	      //Return the JLabel which renders the cell.
	      return l;
	    }
	}
}

