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
import com.bmskinner.nuclear_morphology.charting.charts.NuclearSignalChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.datasets.AbstractDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.NuclearSignalDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.PValueTableCellRenderer;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;


@SuppressWarnings("serial")
public class SignalShellsPanel extends DetailPanel implements ActionListener {

	private ExportableChartPanel 	chartPanel; 
	
	private JRadioButton withinSignalsBtn = new JRadioButton("Within signals");
	private JRadioButton withinNucleiBtn  = new JRadioButton("Within nuclei");
	private ButtonGroup  coverageGroup    = new ButtonGroup();
	
	private JButton 	newAnalysis	 = new JButton("Run new");
	
	private JCheckBox dapiNormalise = new JCheckBox("DAPI normalise", true);
	private JCheckBox showRandomCheckbox = new JCheckBox("Show random", false);
	
	protected ExportableTable table;

	public SignalShellsPanel(){
		super();
		this.setLayout(new BorderLayout());
		
		JPanel header          = createHeader();
		JPanel mainPanel       = createChartPanel();
		JScrollPane tablePanel = createTablePanel();
		
			
		this.add(tablePanel, BorderLayout.WEST);
		this.add(mainPanel,  BorderLayout.CENTER);		
		this.add(header,     BorderLayout.NORTH);

	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel();
		
		newAnalysis.addActionListener( e -> {
			fireSignalChangeEvent("RunShellAnalysis");
		});
		

		panel.add(newAnalysis);

		
		// Add the coverage options
		
		coverageGroup.add(withinSignalsBtn);
		coverageGroup.add(withinNucleiBtn);
		withinSignalsBtn.addActionListener(this);
		withinNucleiBtn.addActionListener(this);
		withinSignalsBtn.setToolTipText("Analyse only pixels that are within defined signals");
		withinNucleiBtn.setToolTipText("Analyse any pixels that are within the nucleus");
		
		withinSignalsBtn.setSelected(true);
		
		panel.add(withinSignalsBtn);
		panel.add(withinNucleiBtn);
		
		// Add the DAPI normalisation box
		
		dapiNormalise.addActionListener(this);
		dapiNormalise.setToolTipText("Apply a correction for nuclear flattening based on the DNA counterstain");
		panel.add(dapiNormalise);
		
		showRandomCheckbox.addActionListener(this);
		showRandomCheckbox.setToolTipText("Show a random distribution of signals in the consensus nucleus");
		panel.add(showRandomCheckbox);
		
		setEnabled(false);
		
		return panel;
	}
	
	public void setEnabled(boolean b){
		newAnalysis.setEnabled(b);
		withinNucleiBtn.setEnabled(b);
		withinSignalsBtn.setEnabled(b);
		dapiNormalise.setEnabled(b);
		showRandomCheckbox.setEnabled(b);
	}
	
	private JScrollPane createTablePanel(){
		JPanel tablePanel = new JPanel(new BorderLayout());
		
		TableModel model = AnalysisDatasetTableCreator.createBlankTable();
		table = new ExportableTable(model);
		table.setEnabled(false);
		tablePanel.add(table, BorderLayout.CENTER);
		
		JScrollPane scrollPane  = new JScrollPane();
		scrollPane.setViewportView(tablePanel);
		scrollPane.setColumnHeaderView(table.getTableHeader());
		Dimension size = new Dimension(300, 200);
		scrollPane.setMinimumSize(size);
		scrollPane.setPreferredSize(size);

		return scrollPane;
	}
	
	private JPanel createChartPanel() {
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
		
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setTarget(chartPanel)
			.setNormalised(dapiNormalise.isSelected())
			.setShowAnnotations(showRandom) // proxy for random signal distribution 
			.setCountType(type)
			.build();

		setChart(options);

		chartPanel.setVisible(true);


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
			if( ! activeDataset().getCollection().hasConsensusNucleus()){
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
		table.setModel(AbstractDatasetCreator.createLoadingTable());	
		
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) {
		return new NuclearSignalChartFactory(options).createShellChart();
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return new NuclearSignalDatasetCreator().createShellChiSquareTable(options);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		updateChartAndTable();
		
	}
}
