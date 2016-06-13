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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.datasets.NucleusTableDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import charting.options.TableOptions.TableType;
import charting.options.TableOptionsBuilder;
import gui.components.AnalysisTableCellRenderer;
import gui.components.ExportableTable;

/**
 * Holds the nuclear detection parameters
 *
 */
@SuppressWarnings("serial")
public class AnalysisDetailPanel extends DetailPanel {

	private ExportableTable tableAnalysisParameters;

	public AnalysisDetailPanel() {
		
		super();
		
		this.setLayout(new BorderLayout());
				
		JScrollPane parametersPanel = createAnalysisParametersPanel();

		this.add(parametersPanel, BorderLayout.CENTER);
		
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return NucleusTableDatasetCreator.getInstance().createAnalysisTable(options);
	}
	
	@Override
	protected void updateSingle() {
		updateMultiple() ;
	}
	

	@Override
	protected void updateMultiple() {
		updateAnalysisParametersPanel();
		log(Level.FINEST, "Updated analysis parameter panel");
	}
	
	@Override
	protected void updateNull() {
		updateMultiple() ;
	}
			
	
	/**
	 * Update the analysis panel with data from the given datasets
	 * @param list the datasets
	 * @throws Exception 
	 */
	private void updateAnalysisParametersPanel() {


		TableOptions options = new TableOptionsBuilder()
		.setDatasets(getDatasets())
		.setType(TableType.ANALYSIS_PARAMETERS)
		.build();

		TableModel model = getTable(options);

		tableAnalysisParameters.setModel(model);
	
		if(options.hasDatasets()){
			setRenderer(tableAnalysisParameters, new AnalysisTableCellRenderer());
		}
	}
	

	private JScrollPane createAnalysisParametersPanel() {
		JScrollPane scrollPane = new JScrollPane();
		
		try {
					
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout(0, 0));

			tableAnalysisParameters = new ExportableTable();
			panel.add(tableAnalysisParameters, BorderLayout.CENTER);
			tableAnalysisParameters.setEnabled(false);

			scrollPane.setViewportView(panel);
			scrollPane.setColumnHeaderView(tableAnalysisParameters.getTableHeader());

			TableOptions options = new TableOptionsBuilder()
			.setDatasets(null)
			.setType(TableType.ANALYSIS_PARAMETERS)
			.build();
			
			TableModel model = getTable(options);
			tableAnalysisParameters.setModel(model);

		}catch(Exception e){
			log(Level.SEVERE, "Error creating stats panel", e);
		}
		return scrollPane;
	}
}
