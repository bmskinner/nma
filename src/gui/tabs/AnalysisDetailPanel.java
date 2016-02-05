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
 * Holds the nuclear detection parameters and basic stats about the
 * population
 *
 */
public class AnalysisDetailPanel extends DetailPanel {


	private static final long serialVersionUID = 1L;
	
	private ExportableTable tablePopulationStats;
	private ExportableTable tableAnalysisParamters;
	private JTabbedPane tabPane;

	public AnalysisDetailPanel(Logger programLogger) {
		
		super(programLogger);
		
		this.setLayout(new BorderLayout());
		tabPane = new JTabbedPane();
		this.add(tabPane, BorderLayout.CENTER);
		
		JScrollPane statsPanel = createStatsPanel();
		tabPane.addTab("Nuclear statistics", statsPanel);
		
		JScrollPane parametersPanel = createAnalysisParametersPanel();
		tabPane.addTab("Nucleus detection", parametersPanel);


	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return NucleusTableDatasetCreator.createAnalysisTable(options);
	}
	
	@Override
	protected void updateSingle() throws Exception {
		updateMultiple() ;
	}
	

	@Override
	protected void updateMultiple() throws Exception {
		updateAnalysisParametersPanel();
		programLogger.log(Level.FINEST, "Updated analysis parameter panel");
		
		updateStatsPanel();
		programLogger.log(Level.FINEST, "Updated analysis stats panel");
	}
	
	@Override
	protected void updateNull() throws Exception {
		updateMultiple() ;
	}
			
	
	/**
	 * Update the analysis panel with data from the given datasets
	 * @param list the datasets
	 * @throws Exception 
	 */
	private void updateAnalysisParametersPanel() throws Exception{


		TableOptions options = new TableOptionsBuilder()
		.setDatasets(getDatasets())
		.setLogger(programLogger)
		.setType(TableType.ANALYSIS_PARAMETERS)
		.build();

		TableModel model = getTable(options);

		tableAnalysisParamters.setModel(model);
		tableAnalysisParamters.createDefaultColumnsFromModel();
		setRenderer(tableAnalysisParamters, new AnalysisTableCellRenderer());
	}
	
	
	
	/**
	 * Update the stats panel with data from the given datasets
	 * @param list the datasets
	 */
	private void updateStatsPanel(){
		try{

			TableOptions options = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setLogger(programLogger)
			.setType(TableType.ANALYSIS_STATS)
			.build();

			TableModel model = getTable(options);


			tablePopulationStats.setModel(model);
		} catch(Exception e){
			programLogger.log(Level.SEVERE, "Error updating stats panel", e);
		}
	}
	
	private JScrollPane createStatsPanel(){
		JScrollPane scrollPane = new JScrollPane();
		try {

			
			JPanel panelGeneralStats = new JPanel();

			panelGeneralStats.setLayout(new BorderLayout(0, 0));

			tablePopulationStats = new ExportableTable();
			panelGeneralStats.add(tablePopulationStats, BorderLayout.CENTER);
			tablePopulationStats.setEnabled(false);

			scrollPane.setViewportView(panelGeneralStats);
			scrollPane.setColumnHeaderView(tablePopulationStats.getTableHeader());
			
			TableOptions options = new TableOptionsBuilder()
			.setDatasets(null)
			.setLogger(programLogger)
			.setType(TableType.ANALYSIS_STATS)
			.build();

			TableModel model = getTable(options);
			
			tablePopulationStats.setModel(model);
			
		}catch(Exception e){
			programLogger.log(Level.SEVERE, "Error creating stats panel", e);
		}
		return scrollPane;
	}
	
	private JScrollPane createAnalysisParametersPanel() {
		JScrollPane scrollPane = new JScrollPane();
		
		try {
			TableOptions options = new TableOptionsBuilder()
			.setDatasets(null)
			.setLogger(programLogger)
			.setType(TableType.ANALYSIS_PARAMETERS)
			.build();

			TableModel model = getTable(options);


			JPanel panel = new JPanel();

			panel.setLayout(new BorderLayout(0, 0));

			tableAnalysisParamters = new ExportableTable();
			tableAnalysisParamters.setAutoCreateColumnsFromModel(false);
			tableAnalysisParamters.setModel(model);
			tableAnalysisParamters.setEnabled(false);
			panel.add(tableAnalysisParamters, BorderLayout.CENTER);

			scrollPane.setViewportView(panel);
			scrollPane.setColumnHeaderView(tableAnalysisParamters.getTableHeader());
		}catch(Exception e){
			programLogger.log(Level.SEVERE, "Error creating stats panel", e);
		}
		return scrollPane;
	}
}
