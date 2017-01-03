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
package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.charting.datasets.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.DefaultTableOptions.TableType;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;

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

		TableModel model = AbstractTableCreator.createBlankTable();

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
	public void setChartsAndTablesLoading(){
//		log("Updating venn detail panel with loading state");
		pairwiseVennTable.setModel(AbstractTableCreator.createLoadingTable());
	}
	
	@Override
	protected void updateSingle() {
//		log("Updating venn detail panel with single state");
		pairwiseVennTable.setModel(AbstractTableCreator.createBlankTable());
	}
	

	@Override
	protected void updateMultiple() {
//		log("Updating pairwise venn table for multiple datasets");
//		Exception e = new Exception("Updating venn detail panel with multiple state");
//		error("Cause of multiple update", e);
//		pairwiseVennTable.setModel(AbstractDatasetCreator.createLoadingTable());
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setType(TableType.PAIRWISE_VENN)
			.setTarget(pairwiseVennTable)
			.setRenderer(TableOptions.ALL_COLUMNS, new PairwiseVennTableCellRenderer(getDatasets()))
			.build();
		
		setTable(options);
		
		finest("Updated pairwise venn panel");
	}
	
	@Override
	protected void updateNull() {
//		log("Updating venn detail panel with null state");
//		Exception e = new Exception("Updating venn detail panel with null state");
//		error("Cause of null update", e);
		pairwiseVennTable.setModel(AbstractTableCreator.createBlankTable());
	}
		
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return new AnalysisDatasetTableCreator(options).createPairwiseVennTable();
	}
		
	/**
	 * Colour table cell background to show pairwise comparisons. All cells are white, apart
	 * from the diagonal, which is made light grey
	 */
	class PairwiseVennTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
		
		private List<IAnalysisDataset> list;
		
		public PairwiseVennTableCellRenderer(List<IAnalysisDataset> list){
			super();
			this.list = list;
		}
		
		private int getIndex(IAnalysisDataset d){
			if(list.contains(d)){
				for(int i=0; i<list.size(); i++){
					if(list.get(i)==d){
						return i;
					}
				}
			}
			return -1;
		}
		
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
		        	fine("Error getting value: "+cellContents+" in column "+columnName, e);
		        	pct = 0;
		        }
		        
		        double colourIndex = 255 - ((pct/100) * 255);
		        
		        backColour = new Color((int) colourIndex,(int) colourIndex, (int) colourIndex);

		        
		        if(pct>50){
		        	foreColour = Color.WHITE;
		        	
		        }
		       
	        }
	        
	        if(columnName.equals("Population 1") || columnName.equals("Population 2")){
	        	IAnalysisDataset d1 = (IAnalysisDataset) value;
	        	if(d1.hasDatasetColour()){
	        		backColour = (Color) d1.getDatasetColour();
	        	} else {
	        		backColour = (Color) ColourSelecter.getColor(getIndex(d1));
	        		if(backColour.getBlue()>60){
	        			foreColour = Color.WHITE;
	        		}
	        	}
	        }
	        
	        l.setBackground(backColour);
	        l.setForeground(foreColour);

	      //Return the JLabel which renders the cell.
	      return l;
	    }
	}
}

