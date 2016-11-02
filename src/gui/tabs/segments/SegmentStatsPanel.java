/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.tabs.segments;

import gui.GlobalOptions;
import gui.components.ExportableTable;
import gui.components.ColourSelecter;
import gui.tabs.DetailPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;
import java.util.logging.Level;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import utility.Constants;
import charting.charts.AbstractChartFactory;
import charting.charts.panels.ExportableChartPanel;
import charting.datasets.AbstractDatasetCreator;
import charting.datasets.AnalysisDatasetTableCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import components.nuclear.IBorderSegment;
import components.nuclear.NucleusBorderSegment;

public class SegmentStatsPanel extends DetailPanel {
	
	private static final long serialVersionUID = 1L;
	private ExportableTable table; // individual segment stats
			
	private JScrollPane scrollPane;
			
	public SegmentStatsPanel(){
		super();
		
		this.setLayout(new BorderLayout());

		scrollPane = new JScrollPane();
					
		try {
						
			TableModel model = AnalysisDatasetTableCreator.createBlankTable();
			table = new ExportableTable(model);

		} catch (Exception e) {
			log(Level.SEVERE, "Error in segment table", e);
		}
		table.setEnabled(false);
					
		scrollPane.setViewportView(table);
		scrollPane.setColumnHeaderView(table.getTableHeader());
		
		this.add(scrollPane, BorderLayout.CENTER);
	}
	
	@Override
	protected void updateSingle() {
		
		table.setModel(AnalysisDatasetTableCreator.createLoadingTable());
		
		TableOptions options = makeOptions();
		setTable(options);
	}
	
	@Override
	protected void updateMultiple() {
		
		table.setModel(AnalysisDatasetTableCreator.createLoadingTable());
		
		TableOptions options = makeOptions();
		setTable(options);
		
		if(IBorderSegment.segmentCountsMatch(getDatasets())){
			table.setToolTipText("Mean and range for 95% confidence interval");

		} else {
			finest("Segment counts don't match");
			table.setToolTipText(null);
		}
	}

	@Override
	protected void updateNull() {
		
		TableModel model = getTable(makeOptions());

		table.setModel(model);

		table.setToolTipText(null);


	}
	
	@Override
	public void setChartsAndTablesLoading(){
		
		table.setModel(AbstractDatasetCreator.createLoadingTable());
		
	}
	
	private TableOptions makeOptions(){
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setScale(GlobalOptions.getInstance().getScale())
			.setSwatch(GlobalOptions.getInstance().getSwatch())
			.setTarget(table)
			.setRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new SegmentTableCellRenderer())
			.build();
		return options;
	}

	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception {
		return new AnalysisDatasetTableCreator(options).createMedianProfileStatisticTable();
	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options)	throws Exception {
		return null;
	}
	
	private class SegmentTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			//Cells are by default rendered as a JLabel.
			Component l = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			// default cell colour is white
			Color colour = Color.WHITE;

			final String colName = table.getColumnName(column); // will be Seg_x

			// only apply to first row, after the first column
			if(column>0 && row==0){

				int segment;
				try {
					segment = Integer.valueOf(colName.replace("Seg_", ""));
				} catch (Exception e){
					log(Level.FINEST, "Error getting segment name: "+colName);
					segment = 0;
				}

				colour = (Color) ColourSelecter.getColor(segment);
				log(Level.FINEST, "SegmentTableCellRenderer for segment "+segment+" uses color "+colour);

			}

			String rowName = table.getModel().getValueAt(row, 0).toString();
			if(rowName.equals("Length p(unimodal)") && column > 0){

				String cellContents = ((JLabel) l).getText();

				double pval;
				try {

					NumberFormat nf = NumberFormat.getInstance();
					pval = nf.parse(cellContents).doubleValue();
				} catch (Exception e){
					log(Level.FINEST, "Error getting value: "+cellContents+" in column "+colName, e);
					pval = 0;
				}

				if(  pval < Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL){
					colour = Color.YELLOW;
				}
				if(  pval < Constants.ONE_PERCENT_SIGNIFICANCE_LEVEL){
					colour = Color.GREEN;
				}

			}


			l.setBackground(colour);

			//Return the JLabel which renders the cell.
			return l;
		}
	}

	

}
