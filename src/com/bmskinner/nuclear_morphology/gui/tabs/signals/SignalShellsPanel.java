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
package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.signals.ShellCounter.CountType;
import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.NuclearSignalChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.datasets.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.NuclearSignalTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.PValueTableCellRenderer;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;


/**
 * Holds information on shell analysis results, and allows new shell analyses
 * to be run
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class SignalShellsPanel extends DetailPanel implements ActionListener {
	
	private static final String WITHIN_SIGNALS_LBL = "Within signals";
	private static final String WITHIN_NUCLEI_LBL  = "Within nuclei";
	private static final String RUN_ANALYSIS_LBL   = "Run new";
	private static final String DAPI_NORM_LBL      = "DAPI normalise";
	private static final String SHOW_RANDOM_LBL    = "Show random";
	
	private static final String RUN_ANALYSIS_TOOLTIP   = "Run a shell analysis on all signal groups, replacing any existing analysis";
	private static final String WITHIN_SIGNALS_TOOLTIP = "Analyse only pixels that are within defined signals";
	private static final String WITHIN_NUCLEI_TOOLTIP  = "Analyse any pixels that are within the nucleus";
	private static final String DAPI_NORM_TOOLTIP      = "Apply a correction for nuclear flattening based on the DNA counterstain";
	private static final String SHOW_RANDOM_TOOLTIP    = "Show a random distribution of signals in the consensus nucleus";

	private ExportableChartPanel chartPanel; 
	private ExportableChartPanel consensusPanel;
	
	private JRadioButton withinSignalsBtn = new JRadioButton(WITHIN_SIGNALS_LBL);
	private JRadioButton withinNucleiBtn  = new JRadioButton(WITHIN_NUCLEI_LBL);
	private ButtonGroup  coverageGroup    = new ButtonGroup();
	
	private JButton 	newAnalysis	 = new JButton(RUN_ANALYSIS_LBL);
	
	private JCheckBox dapiNormalise = new JCheckBox(DAPI_NORM_LBL, true);
	private JCheckBox showRandomCheckbox = new JCheckBox(SHOW_RANDOM_LBL, false);
	
	protected ExportableTable table;
	
	
	public SignalShellsPanel(){
		super();
		this.setLayout(new BorderLayout());
				
		JPanel header    = createHeader();
		JPanel mainPanel = createMainPanel();
		
		this.add(mainPanel, BorderLayout.CENTER);		
		this.add(header,    BorderLayout.NORTH);
		
		this.updateSize();
		
	}
	
	public void setEnabled(boolean b){
		newAnalysis.setEnabled(b);
		withinNucleiBtn.setEnabled(b);
		withinSignalsBtn.setEnabled(b);
		dapiNormalise.setEnabled(b);
		showRandomCheckbox.setEnabled(b);
	}
	
	/**
	 * Create the main display panel, containing all elements except the header 
	 * @return
	 */
	private JPanel createMainPanel(){
		
		JPanel panel = new JPanel(new GridBagLayout());

		JPanel shellBarPanel = createShellBarPanel();
		JPanel westPanel = createWestPanel();

		// Set layout for west panel
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 0; // Start at left
		constraints.gridy = 0; // Start at top
		constraints.gridheight = GridBagConstraints.REMAINDER; // Take up 2 cells in height
		constraints.gridwidth = 3; // Take up 1 cell in width
		constraints.weightx = 0.3;
		constraints.weighty = 1;
		constraints.anchor = GridBagConstraints.CENTER;

		panel.add(westPanel, constraints);

		// Set layout for bar chart panel
		constraints.gridx = 3; // Start after centre
		constraints.gridy = 0; // Start at top
		constraints.gridheight = GridBagConstraints.REMAINDER;
		constraints.gridwidth = 7;
		constraints.weightx = 0.7;
		
		panel.add(shellBarPanel, constraints);

		return panel;
	}
	
	/**
	 * Create the panel containing the table and consensus chart
	 * @return
	 */
	private JPanel createWestPanel(){
		JPanel panel = new JPanel(new BorderLayout());
		
		JScrollPane tablePanel = createTablePanel();
		
		consensusPanel = createConsensusPanel();
		
		panel.add(tablePanel, BorderLayout.NORTH);
		panel.add(consensusPanel, BorderLayout.CENTER);
				
		return panel;
		
	}
	
	private ExportableChartPanel createConsensusPanel(){
		
		JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyChart();
		
		ExportableChartPanel chartPanel = new ExportableChartPanel(chart);
		chartPanel.setFixedAspectRatio(true);
		
		return chartPanel;
	}
	
	/**
	 * Create the header panel
	 * @return
	 */
	private JPanel createHeader(){
		JPanel panel = new JPanel();
		
		newAnalysis.addActionListener( e -> {
			fireDatasetEvent(DatasetEvent.RUN_SHELL_ANALYSIS, activeDataset());
		});
		newAnalysis.setToolTipText(RUN_ANALYSIS_TOOLTIP);

		panel.add(newAnalysis);
		
		
		
		// Add the coverage options
		
		coverageGroup.add(withinSignalsBtn);
		coverageGroup.add(withinNucleiBtn);
		withinSignalsBtn.addActionListener(this);
		withinNucleiBtn.addActionListener(this);
		withinSignalsBtn.setToolTipText(WITHIN_SIGNALS_TOOLTIP);
		withinNucleiBtn.setToolTipText(WITHIN_NUCLEI_TOOLTIP);
		
		withinSignalsBtn.setSelected(true);
		
		panel.add(withinSignalsBtn);
		panel.add(withinNucleiBtn);
		
		// Add the DAPI normalisation box
		
		dapiNormalise.addActionListener(this);
		dapiNormalise.setToolTipText(DAPI_NORM_TOOLTIP);
		panel.add(dapiNormalise);
		
		showRandomCheckbox.addActionListener(this);
		showRandomCheckbox.setToolTipText(SHOW_RANDOM_TOOLTIP);
		panel.add(showRandomCheckbox);
		
		setEnabled(false);
		
		return panel;
	}
	
	
	/**
	 * Create the table panel
	 * @return
	 */
	private JScrollPane createTablePanel(){
		JPanel tablePanel = new JPanel(new BorderLayout());
		
		TableModel model = AnalysisDatasetTableCreator.createBlankTable();
		table = new ExportableTable(model);
		table.setEnabled(false);
		tablePanel.add(table, BorderLayout.CENTER);
		
		JScrollPane scrollPane  = new JScrollPane();
		scrollPane.setViewportView(tablePanel);
		scrollPane.setColumnHeaderView(table.getTableHeader());
		Dimension size = new Dimension(200, 150);
		scrollPane.setMinimumSize(size);
		scrollPane.setPreferredSize(size);

		return scrollPane;
	}
	
	/**
	 * Create the panel holding the shell bar chart
	 * @return
	 */
	private JPanel createShellBarPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		
		ChartOptions options = new ChartOptionsBuilder()
			.build();
	
		JFreeChart chart = getChart(options);
		
		chartPanel = new ExportableChartPanel(chart);
		
		panel.add(chartPanel, BorderLayout.CENTER);
				
		return panel;
	}

	
	private void updateChartAndTable(){
		
		CountType type = withinNucleiBtn.isSelected() ? CountType.NUCLEUS : CountType.SIGNAL;
		
		boolean showRandom = showRandomCheckbox.isSelected();
		
		ChartOptions barChartOptions = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setTarget(chartPanel)
			.setNormalised(dapiNormalise.isSelected())
			.setShowAnnotations(showRandom) // proxy for random signal distribution 
			.setCountType(type)
			.build();

		setChart(barChartOptions);

		ChartOptions consensusChartOptions = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setTarget(consensusPanel)
			.setNormalised(dapiNormalise.isSelected())
			.setShowAnnotations(showRandom) // proxy for random signal distribution 
			.setShowXAxis(false)
			.setShowYAxis(false)
			.setCountType(type)
			.build();

		setChart(consensusChartOptions);


		TableOptions tableOptions = new TableOptionsBuilder()
			.setDatasets(getDatasets())
			.setCountType(type)
			.setTarget(table)
			.setRenderer(2, new PValueTableCellRenderer())
			.build();
		
		setTable(tableOptions);

	}

	@Override
	protected void updateSingle() {

		updateChartAndTable();
		setEnabled(false);
		
		if(activeDataset().getCollection().getSignalManager().hasSignals()){
			setEnabled(true);
			if( ! activeDataset().getCollection().hasConsensus()){
				showRandomCheckbox.setEnabled(false);
			}
		}
		
	}

	@Override
	protected void updateMultiple() {
		setEnabled(false);
		updateChartAndTable();
		
	}

	@Override
	protected void updateNull() {
		setEnabled(false);
		updateChartAndTable();
		
	}
	
	@Override
	public void setChartsAndTablesLoading(){
		super.setChartsAndTablesLoading();
		chartPanel.setChart(AbstractChartFactory.createLoadingChart());	
		consensusPanel.setChart(AbstractChartFactory.createLoadingChart());	
		table.setModel(AbstractTableCreator.createLoadingTable());	
		
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) {
		if(options.getTarget()==chartPanel){
			return new NuclearSignalChartFactory(options).createShellChart();
		} else {
			return new NuclearSignalChartFactory(options).createShellConsensusChart();
		}
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return new NuclearSignalTableCreator(options).createShellChiSquareTable();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		updateChartAndTable();
		
	}
}
