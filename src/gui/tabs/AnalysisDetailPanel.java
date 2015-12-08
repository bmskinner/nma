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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import analysis.AnalysisDataset;
import charting.DefaultTableOptions;
import charting.DefaultTableOptions.TableType;
import charting.TableOptions;
import charting.datasets.NucleusTableDatasetCreator;
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
	
	/**
	 * Trigger an update of the panel with the given list of datasets
	 * to display
	 * @param list
	 */
	public void update(final List<AnalysisDataset> list){
		programLogger.log(Level.FINE, "Updating analysis panel");
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				updateAnalysisParametersPanel(list);
				programLogger.log(Level.FINEST, "Updated analysis parameter panel");
				updateStatsPanel(list);
				programLogger.log(Level.FINEST, "Updated analysis stats panel");
			
		}});
	}
		
	
	/**
	 * Update the analysis panel with data from the given datasets
	 * @param list the datasets
	 */
	private void updateAnalysisParametersPanel(List<AnalysisDataset> list){
		
		TableModel model = NucleusTableDatasetCreator.createAnalysisParametersTable(null);
		if(list!=null && !list.isEmpty()){

			TableOptions options = new DefaultTableOptions(list, TableType.ANALYSIS_PARAMETERS);

			if(getTableCache().hasTable(options)){
				model = getTableCache().getTable(options);
				programLogger.log(Level.FINEST, "Fetched cached analysis parameters table");
			} else {
				model = NucleusTableDatasetCreator.createAnalysisParametersTable(list);
				getTableCache().addTable(options, model);
				programLogger.log(Level.FINEST, "Added cached analysis parameters table");
			}
		}
		
		
		tableAnalysisParamters.setModel(model);
		tableAnalysisParamters.createDefaultColumnsFromModel();
	}
	
	
	
	/**
	 * Update the stats panel with data from the given datasets
	 * @param list the datasets
	 */
	private void updateStatsPanel(List<AnalysisDataset> list){
		try{
			TableModel model = NucleusTableDatasetCreator.createStatsTable(null);

			if(list!=null && !list.isEmpty()){
				
				TableOptions options = new DefaultTableOptions(list, TableType.ANALYSIS_STATS);

				if(getTableCache().hasTable(options)){
					model = getTableCache().getTable(options);
					programLogger.log(Level.FINEST, "Fetched cached analysis stats table");
				} else {
					model = NucleusTableDatasetCreator.createStatsTable(list);
					getTableCache().addTable(options, model);
					programLogger.log(Level.FINEST, "Added cached analysis stats table");
				}
				
			}
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
			tablePopulationStats.setModel(NucleusTableDatasetCreator.createStatsTable(null));
			
		}catch(Exception e){
			programLogger.log(Level.SEVERE, "Error creating stats panel", e);
		}
		return scrollPane;
	}
	
	private JScrollPane createAnalysisParametersPanel(){
		
		JScrollPane scrollPane = new JScrollPane();
		JPanel panel = new JPanel();
		
		panel.setLayout(new BorderLayout(0, 0));

		tableAnalysisParamters = new ExportableTable();
		tableAnalysisParamters.setAutoCreateColumnsFromModel(false);
		tableAnalysisParamters.setModel(NucleusTableDatasetCreator.createAnalysisParametersTable(null));
		tableAnalysisParamters.setEnabled(false);
		panel.add(tableAnalysisParamters, BorderLayout.CENTER);

		scrollPane.setViewportView(panel);
		scrollPane.setColumnHeaderView(tableAnalysisParamters.getTableHeader());
		return scrollPane;
	}

}
