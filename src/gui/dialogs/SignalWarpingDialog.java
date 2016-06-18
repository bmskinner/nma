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
package gui.dialogs;

import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.jfree.chart.JFreeChart;

import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.OutlineChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.nuclear.SignalGroup;
import analysis.AnalysisDataset;
import analysis.signals.SignalManager;
import analysis.signals.SignalWarper;
import gui.LoadingIconDialog;
import gui.components.FixedAspectRatioChartPanel;
import gui.components.panels.DatasetSelectionPanel;
import gui.components.panels.SignalGroupSelectionPanel;

@SuppressWarnings("serial")
public class SignalWarpingDialog extends LoadingIconDialog implements PropertyChangeListener, ActionListener{
	
	private List<AnalysisDataset> datasets;
	private FixedAspectRatioChartPanel chartPanel;
	
	private DatasetSelectionPanel datasetBoxOne;
	private DatasetSelectionPanel datasetBoxTwo;
	
	private SignalGroupSelectionPanel signalBox;

	private JButton runButton;
	private JCheckBox cellsWithSignalsBox;
	
	private SignalWarper warper;
	
	private JProgressBar progressBar = new JProgressBar(0, 100);
	
	private int totalCells = 0; // The cells in the signal group being processed
	private int cellsDone  = 0; // Progress through cells in the signal group

	
	public SignalWarpingDialog(List<AnalysisDataset> datasets){
		super();
		finest("Creating signal warping dialog");
		this.datasets = datasets;
		createUI();
		this.setModal(false);
		this.pack();
		chartPanel.restoreAutoBounds();
		this.setVisible(true);
		finest("Created signal warping dialog");
	}
	
	private void createUI(){
		this.setLayout(new BorderLayout());
		this.setTitle("Signal warping");
		
		
		JPanel header = createHeader();
		this.add(header, BorderLayout.NORTH);
		finest("Created header");
		
		JFreeChart chart = ConsensusNucleusChartFactory.getInstance().makeNucleusOutlineChart(datasets.get(0));
		chartPanel = new FixedAspectRatioChartPanel(chart);
		

		finest("Created empty chart");
		this.add(chartPanel, BorderLayout.CENTER);
		
	}
	
	private JPanel createHeader(){
		
		JPanel headerPanel = new JPanel();
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
		
		JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel lowerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		datasetBoxOne = new DatasetSelectionPanel(datasets);
		datasetBoxTwo = new DatasetSelectionPanel(datasets);
		
		datasetBoxOne.setSelectedDataset(datasets.get(0));
		datasetBoxTwo.setSelectedDataset(datasets.get(0));
		
		datasetBoxOne.addActionListener(this);
		datasetBoxTwo.addActionListener(this);
		
		upperPanel.add(new JLabel("Source dataset"));
		upperPanel.add(datasetBoxOne);
		
		SignalManager m =  datasets.get(0).getCollection().getSignalManager();

		signalBox = new SignalGroupSelectionPanel(datasetBoxOne.getSelectedDataset());
		UUID id   = signalBox.getSelectedID();

		totalCells = m.getNumberOfCellsWithNuclearSignals(id);
		
		upperPanel.add(new JLabel("Signal group"));
		upperPanel.add(signalBox);		
		finest("Added signal group box");
				
		signalBox.addActionListener(this);
		
		cellsWithSignalsBox = new JCheckBox("Only include cells with signals", true);
		cellsWithSignalsBox.addActionListener(this);
		upperPanel.add(cellsWithSignalsBox);
		
		lowerPanel.add(new JLabel("Target dataset"));
		lowerPanel.add(datasetBoxTwo);
		
		runButton = new JButton("Run");
		
		runButton.addActionListener( e -> {

				Runnable task = () -> { 
					runWarping();
				};
				Thread thr = new Thread(task);
				thr.start();
				
			
		});	

		lowerPanel.add(runButton);
		
		lowerPanel.add(progressBar);
		progressBar.setVisible(false);
		
		lowerPanel.add(this.getLoadingLabel());
		
		headerPanel.add(upperPanel);
		headerPanel.add(lowerPanel);
				
		return headerPanel;
	} 
	
	private void runWarping(){
		
		finest("Running warping");
		
		AnalysisDataset sourceDataset = datasetBoxOne.getSelectedDataset();
		AnalysisDataset targetDataset = datasetBoxTwo.getSelectedDataset();
		
//		SignalIDToGroup group    = (SignalIDToGroup) signalGroupSelectedBox.getSelectedItem();
		boolean cellsWithSignals = cellsWithSignalsBox.isSelected();
		
		totalCells = cellsWithSignals 
				? sourceDataset.getCollection().getSignalManager().getNumberOfCellsWithNuclearSignals(signalBox.getSelectedID()) 
				: sourceDataset.getCollection().getNucleusCount();
				
//		log("Found "+totalCells+" using signals only = "+cellsWithSignals);
						
		finest("Signal group: "+signalBox.getSelectedGroup());
		try {
			setStatusLoading();
			setEnabled(false);

			progressBar.setStringPainted(true);
			
			progressBar.setString("0 of "+totalCells);
			progressBar.setVisible(true);
			
			

			warper = new SignalWarper(sourceDataset, targetDataset, signalBox.getSelectedID(), cellsWithSignals);
			warper.addPropertyChangeListener(this);
			warper.execute();
			
		} catch (Exception e) {
			error("Error running warping", e);
			JFreeChart chart = ConsensusNucleusChartFactory.getInstance().makeNucleusOutlineChart(targetDataset);
			chartPanel.setChart(chart);
			setEnabled(true);
		} 
	}
	
	@Override
	public void setEnabled(boolean b){
		signalBox.setEnabled(b);
		cellsWithSignalsBox.setEnabled(b);
		runButton.setEnabled(b);
		datasetBoxOne.setEnabled(b);
		datasetBoxTwo.setEnabled(b);
	}
	
	private void updateChart(){
		
		Runnable task = () -> { 
			ImageProcessor image = warper.getResult();
			
			ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(datasetBoxTwo.getSelectedDataset())
				.setShowXAxis(false)
				.setShowYAxis(false)
				.setShowBounds(false)
				.build();

			final JFreeChart chart = OutlineChartFactory.getInstance().makeSignalWarpChart(options, image);
					
			Runnable update = () -> { 
				chartPanel.setChart(chart);
				chartPanel.restoreAutoBounds();
			};
			SwingUtilities.invokeLater( update );
			
		};
		Thread thr = new Thread(task);
		thr.start();		
	}

	
	public void finished(){
		try {
			updateChart();
			
			setEnabled(true);
			setStatusLoaded();
			
		} catch (Exception e) {
			error("Error getting warp results", e);
		}
	}
	
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {

		int value = (Integer) evt.getNewValue(); // should be percent
		finest("Property change: "+value);
		
		if(value >=0 && value <=100){
			
			if(this.progressBar.isIndeterminate()){
				this.progressBar.setIndeterminate(false);
			}
			this.progressBar.setValue(value);
			cellsDone++;
			progressBar.setString(cellsDone+" of "+totalCells);
			updateChart();
		}

		if(evt.getPropertyName().equals("Finished")){
			finest("Worker signaled finished");
			progressBar.setValue(0);
			progressBar.setVisible(false);
			cellsDone = 0;
			finished();
		}
		
	}
	

	@Override
	public void actionPerformed(ActionEvent e) {
		AnalysisDataset sourceDataset = datasetBoxOne.getSelectedDataset();
		
		SignalManager m =  sourceDataset.getCollection().getSignalManager();
		if( ! m.hasSignals()){
			signalBox.setEnabled(false);
			cellsWithSignalsBox.setEnabled(false);
			runButton.setEnabled(false);
			datasetBoxTwo.setEnabled(false);
			
		} else {
			signalBox.setEnabled(true);
			cellsWithSignalsBox.setEnabled(true);
			runButton.setEnabled(true);
			datasetBoxTwo.setEnabled(true);
		}
		
		if(e.getSource()==datasetBoxOne){
			
			if( m.hasSignals()){
				
				signalBox.setDataset(sourceDataset);
			}

			
		}
		
		if(e.getSource()==datasetBoxTwo){
			JFreeChart chart = ConsensusNucleusChartFactory.getInstance().makeNucleusOutlineChart(datasetBoxTwo.getSelectedDataset());
			chartPanel.setChart(chart);
		}
		
//		SignalIDToGroup group = (SignalIDToGroup) signalGroupSelectedBox.getSelectedItem();
				
		boolean cellsWithSignals = cellsWithSignalsBox.isSelected();
		
		totalCells = cellsWithSignals 
				? m.getNumberOfCellsWithNuclearSignals(signalBox.getSelectedID()) 
				: datasets.get(0).getCollection().getNucleusCount();
				
		
	}

}
