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

import gui.components.ExportableTable;
import gui.components.ColourSelecter.ColourSwatch;
import gui.components.panels.MeasurementUnitSettingsPanel;
import gui.tabs.DetailPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import utility.Constants;
import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.TableOptionsBuilder;
import components.generic.MeasurementScale;

public class SegmentStatsPanel extends DetailPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private ExportableTable table; // individual segment stats
	private MeasurementUnitSettingsPanel measurementUnitSettingsPanel = new MeasurementUnitSettingsPanel() ;
			
	private JScrollPane scrollPane;
			
	public SegmentStatsPanel(){
		super();
		
		this.setLayout(new BorderLayout());
		measurementUnitSettingsPanel.addActionListener(this);
		scrollPane = new JScrollPane();
					
		try {
			
			TableOptions options = new TableOptionsBuilder()
			.setDatasets(null)
			.setScale(MeasurementScale.PIXELS)
			.build();
			
			TableModel model = NucleusTableDatasetCreator.createMedianProfileStatisticTable(options);
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
	protected void updateSingle() throws Exception {
		TableModel model = getTable(makeOptions());
		table.setModel(model);
		
		table.setToolTipText(null);
		setRenderer(table, new SegmentTableCellRenderer());
	}
	
	@Override
	protected void updateMultiple() throws Exception {
		TableModel model = getTable(makeOptions());
		
		table.setModel(model);

		if(checkSegmentCountsMatch(getDatasets())){
			table.setToolTipText("Mean and range for 95% confidence interval");
			setRenderer(table, new SegmentTableCellRenderer());

		} else {
			log(Level.FINEST, "Segment counts don't match");
			table.setToolTipText(null);
		}
	}

	@Override
	protected void updateNull() throws Exception {
		
		TableModel model = getTable(makeOptions());

		table.setModel(model);

		table.setToolTipText(null);


	}
	
	private TableOptions makeOptions(){
		MeasurementScale scale = measurementUnitSettingsPanel.getSelected();
		
		TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setScale(scale)
			.build();
		return options;
	}

	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception {
		return NucleusTableDatasetCreator.createMedianProfileStatisticTable(options);
	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options)	throws Exception {
		return null;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		update(getDatasets());
		
	}
	
	private class SegmentTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			//Cells are by default rendered as a JLabel.
			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
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

				ColourSwatch swatch = activeDataset().getSwatch() == null ? ColourSwatch.REGULAR_SWATCH : activeDataset().getSwatch();
				colour = swatch.color(segment);
				log(Level.FINEST, "SegmentTableCellRenderer for segment "+segment+" uses color "+colour);

			}

			String rowName = (String) table.getModel().getValueAt(row, 0);
			if(rowName.equals("Length p(unimodal)") && column > 0){

				String cellContents = l.getText();

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
