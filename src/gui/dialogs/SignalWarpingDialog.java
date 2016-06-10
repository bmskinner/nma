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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.JButton;
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
import analysis.AnalysisDataset;
import analysis.signals.SignalManager;
import analysis.signals.SignalWarper;
import gui.LoadingIconDialog;
import gui.MainWindow;
import gui.actions.ClusterAnalysisAction;
import gui.components.FixedAspectRatioChartPanel;

@SuppressWarnings("serial")
public class SignalWarpingDialog extends LoadingIconDialog implements PropertyChangeListener{
	
	private List<AnalysisDataset> datasets;
	private FixedAspectRatioChartPanel chartPanel;
	

	private JComboBox<Integer> signalGroupSelectedBox;
	private JLabel signalGroupNameLabel;
	private JButton runButton;
	
	private SignalWarper warper;
	
	private JProgressBar progressBar = new JProgressBar(0, 100);
	
	private int totalCells = 0;
	private int cellsDone  = 0;

	
	public SignalWarpingDialog(List<AnalysisDataset> datasets){
		super();
		finest("Creating signal warping dialog");
		this.datasets = datasets;
		createUI();
		this.setModal(false);
		this.pack();
		this.setVisible(true);
		finest("Created signal warping dialog");
	}
	
	private void createUI(){
		this.setLayout(new BorderLayout());
		this.setTitle("Signal warping: "+datasets.get(0).getName());
		
		
		JPanel header = createHeader();
		this.add(header, BorderLayout.NORTH);
		finest("Created header");
		
		JFreeChart chart = ConsensusNucleusChartFactory.getInstance().makeNucleusOutlineChart(datasets.get(0));
		chartPanel = new FixedAspectRatioChartPanel(chart);

		finest("Created empty chart");
		this.add(chartPanel, BorderLayout.CENTER);
		
	}
	
	private JPanel createHeader(){
		JPanel panel = new JPanel(new FlowLayout());
		
		SignalManager m =  datasets.get(0).getCollection().getSignalManager();
		Set<Integer> signalGroups = m.getSignalGroups();
		
		signalGroupSelectedBox = new JComboBox<Integer>(signalGroups.toArray( new Integer[0] ));
		signalGroupSelectedBox.setSelectedIndex(0);
		int signalGroup = (int) signalGroupSelectedBox.getSelectedItem();

		totalCells = m.getNumberOfCellsWithNuclearSignals(signalGroup);
		
		panel.add(new JLabel("Signal group"));
		panel.add(signalGroupSelectedBox);		
		finest("Added signal group box");
		
		signalGroupNameLabel = new JLabel(m.getSignalGroupName(signalGroup));
		panel.add(signalGroupNameLabel);
		
		signalGroupSelectedBox.addActionListener( e ->{
			
			int group = (int) signalGroupSelectedBox.getSelectedItem();
			signalGroupNameLabel.setText(m.getSignalGroupName(group));
			totalCells = m.getNumberOfCellsWithNuclearSignals(group);
		});

		
		runButton = new JButton("Run");
		
		runButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				Runnable task = () -> { 
					runWarping();
				};
				SwingUtilities.invokeLater(task);
								
			}
		});	

		panel.add(runButton);
		
		panel.add(progressBar);
		progressBar.setVisible(false);
		
		panel.add(this.getLoadingLabel());
		
		if(datasets.size()>1){
			signalGroupSelectedBox.setEnabled(false);
			runButton.setEnabled(false);
		}
		
		return panel;
	} 
	
	private void runWarping(){
		int signalGroup = (int) signalGroupSelectedBox.getSelectedItem();
				
		try {
			setStatusLoading();
			setEnabled(false);
			progressBar.setStringPainted(true);
			
			
			progressBar.setString("0 of "+totalCells);
			progressBar.setVisible(true);
			
			

			warper = new SignalWarper(datasets.get(0), signalGroup);
			warper.addPropertyChangeListener(this);
			warper.execute();
			
		} catch (Exception e) {
			error("Error running warping", e);
			setEnabled(true);
		}
	}
	
	private void updateChart(){
		ImageProcessor[] images = warper.getResults();

		JFreeChart chart = null;
		
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(datasets)
			.build();

		chart = OutlineChartFactory.getInstance().makeSignalWarpChart(options, images);
				
		chartPanel.setChart(chart);
		chartPanel.restoreAutoBounds();
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
			progressBar.setVisible(false);
			cellsDone = 0;
			finished();
		}
		
	}
	
	

}
