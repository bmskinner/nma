package no.gui;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import no.analysis.AnalysisDataset;
import datasets.NucleusDatasetCreator;
import datasets.NucleusTableDatasetCreator;

/**
 * Holds the nuclear detection parameters and basic stats about the
 * population
 *
 */
public class AnalysisDetailPanel extends JPanel {


	private static final long serialVersionUID = 1L;
	
	private JTable tablePopulationStats;
	private JTable tableAnalysisParamters;
	private JTabbedPane tabPane;


	public AnalysisDetailPanel() {
		
		this.setLayout(new BorderLayout());
		tabPane = new JTabbedPane();
		this.add(tabPane, BorderLayout.CENTER);
		
		JScrollPane statsPanel = createStatsPanel();
		tabPane.addTab("Nuclear statistics", statsPanel);
		
		JScrollPane parametersPanel = createAnalysisParametersPanel();
		tabPane.addTab("Nucleus detection", parametersPanel);


	}
	
	public void update(List<AnalysisDataset> list){
		updateAnalysisParametersPanel(list);
		updateStatsPanel(list);
	}
	
	
	/**
	 * Update the analysis panel with data from the given datasets
	 * @param list the datasets
	 */
	private void updateAnalysisParametersPanel(List<AnalysisDataset> list){
		// format the numbers and make into a tablemodel
		TableModel model = NucleusTableDatasetCreator.createAnalysisParametersTable(list);
		tableAnalysisParamters.setModel(model);
	}
	
	
	
	/**
	 * Update the stats panel with data from the given datasets
	 * @param list the datasets
	 */
	private void updateStatsPanel(List<AnalysisDataset> list){
		// format the numbers and make into a tablemodel
		TableModel model = NucleusTableDatasetCreator.createStatsTable(list);
		tablePopulationStats.setModel(model);
	}
	
	private JScrollPane createStatsPanel(){
		
		JScrollPane scrollPane = new JScrollPane();
		JPanel panelGeneralStats = new JPanel();
		
		panelGeneralStats.setLayout(new BorderLayout(0, 0));

		tablePopulationStats = new JTable();
		panelGeneralStats.add(tablePopulationStats, BorderLayout.CENTER);
		tablePopulationStats.setEnabled(false);

		scrollPane.setViewportView(panelGeneralStats);
		scrollPane.setColumnHeaderView(tablePopulationStats.getTableHeader());
		tablePopulationStats.setModel(NucleusTableDatasetCreator.createStatsTable(null));
		return scrollPane;
	}
	
	private JScrollPane createAnalysisParametersPanel(){
		
		JScrollPane scrollPane = new JScrollPane();
		JPanel panel = new JPanel();
		
		panel.setLayout(new BorderLayout(0, 0));

		tableAnalysisParamters = new JTable();
		tableAnalysisParamters.setModel(NucleusTableDatasetCreator.createAnalysisParametersTable(null));
		tableAnalysisParamters.setEnabled(false);
		panel.add(tableAnalysisParamters, BorderLayout.CENTER);

		scrollPane.setViewportView(panel);
		scrollPane.setColumnHeaderView(tableAnalysisParamters.getTableHeader());
		return scrollPane;
	}

}
