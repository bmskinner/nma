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
package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalWarper;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.panels.DatasetSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.SignalGroupSelectionPanel;

import ij.process.ImageProcessor;

@SuppressWarnings("serial")
public class SignalWarpingDialog extends LoadingIconDialog implements PropertyChangeListener, ActionListener{
	
	private static final String SOURCE_DATASET_LBL  = "Source dataset";
	private static final String TARGET_DATASET_LBL  = "Target dataset";
	private static final String SIGNAL_GROUP_LBL    = "Signal group";
	private static final String INCLUDE_CELLS_LBL   = "Only include cells with signals";
	private static final String ADD_TO_IMAGE_LBL    = "Add to image";
	private static final String STRAIGHTEN_MESH_LBL = "Straighten meshes";
	private static final String RUN_LBL             = "Run";
	
	private List<IAnalysisDataset> datasets;
	private ExportableChartPanel chartPanel;
	
	private DatasetSelectionPanel datasetBoxOne;
	private DatasetSelectionPanel datasetBoxTwo;
	
	private SignalGroupSelectionPanel signalBox;

	private JButton runButton;
	private JCheckBox cellsWithSignalsBox;
	private JCheckBox straightenMeshBox;
	private JCheckBox addToImage;
	
//	private SignalWarper warper;
	private SignalWarper warper;
	
	private JProgressBar progressBar = new JProgressBar(0, 100);
	
	private int totalCells = 0; // The cells in the signal group being processed
	private int cellsDone  = 0; // Progress through cells in the signal group
	
	private boolean isAddToImage = true;
	
	final private List<ImageProcessor> mergableImages = new ArrayList<>(); 

	
	public SignalWarpingDialog(final List<IAnalysisDataset> datasets){
		super();
		this.datasets = datasets;
		createUI();
		this.setModal(false);
		this.pack();
		
		chartPanel.restoreAutoBounds();
		this.setVisible(true);
	}
	
	private void createUI(){
		this.setLayout(new BorderLayout());
		this.setTitle("Signal warping");
		
		
		JPanel header = createHeader();
		this.add(header, BorderLayout.NORTH);
		finest("Created header");
		
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(datasets.get(0))
			.build();
		
		JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();
		chartPanel = new ExportableChartPanel(chart);
		chartPanel.setFixedAspectRatio(true);

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
		
		upperPanel.add(new JLabel(SOURCE_DATASET_LBL));
		upperPanel.add(datasetBoxOne);
		
		SignalManager m =  datasets.get(0).getCollection().getSignalManager();

		signalBox = new SignalGroupSelectionPanel(datasetBoxOne.getSelectedDataset());
		
		if(signalBox.hasSelection()){
			UUID id   = signalBox.getSelectedID();
			totalCells = m.getNumberOfCellsWithNuclearSignals(id);
		} else {
			signalBox.setEnabled(false);
		}

		upperPanel.add(new JLabel(SIGNAL_GROUP_LBL));
		upperPanel.add(signalBox);		

				
		signalBox.addActionListener(this);
		
		cellsWithSignalsBox = new JCheckBox(INCLUDE_CELLS_LBL, true);
		cellsWithSignalsBox.addActionListener(this);
		upperPanel.add(cellsWithSignalsBox);
		
		addToImage = new JCheckBox(ADD_TO_IMAGE_LBL, true);
		addToImage.addActionListener(this);
		upperPanel.add(addToImage);
		
		straightenMeshBox = new JCheckBox(STRAIGHTEN_MESH_LBL, false);
		straightenMeshBox.addActionListener(this);
		
		
		lowerPanel.add(new JLabel(TARGET_DATASET_LBL));
		lowerPanel.add(datasetBoxTwo);
				
		runButton = new JButton(RUN_LBL);
		
		runButton.addActionListener( e -> {

				Runnable task = () -> { 
					runWarping();
				};
				
				ThreadManager.getInstance().submit(task);			
			
		});	

		lowerPanel.add(runButton);
		

		
		if(! signalBox.hasSelection()){
			runButton.setEnabled(false);
		}
		
		lowerPanel.add(progressBar);
		progressBar.setVisible(false);
		
		lowerPanel.add(this.getLoadingLabel());
		
		headerPanel.add(upperPanel);
		headerPanel.add(lowerPanel);
				
		return headerPanel;
	} 
	
	private void runWarping(){
		
		finest("Running warping");
		if(!isAddToImage){
			mergableImages.clear();
		} 
//		progressBar.setString("0 of "+totalCells);
		progressBar.setValue(0);
		
		IAnalysisDataset sourceDataset = datasetBoxOne.getSelectedDataset();
		IAnalysisDataset targetDataset = datasetBoxTwo.getSelectedDataset();
		
//		SignalIDToGroup group    = (SignalIDToGroup) signalGroupSelectedBox.getSelectedItem();
		boolean cellsWithSignals = cellsWithSignalsBox.isSelected();
		boolean straighten       = straightenMeshBox.isSelected();
		
		totalCells = cellsWithSignals 
				? sourceDataset.getCollection().getSignalManager().getNumberOfCellsWithNuclearSignals(signalBox.getSelectedID()) 
				: sourceDataset.getCollection().size();
				
//		log("Found "+totalCells+" using signals only = "+cellsWithSignals);
				
		Nucleus target = targetDataset.getCollection().getConsensus();
						
		finest("Signal group: "+signalBox.getSelectedGroup());
		try {
			setStatusLoading();
			setEnabled(false);

			progressBar.setStringPainted(true);
			
			progressBar.setVisible(true);
			
			
			warper = new SignalWarper( sourceDataset, target, signalBox.getSelectedID(), cellsWithSignals, straighten);
//			warper = new SignalWarper( signalBox.getSelectedID(), cellsWithSignals, straighten, chartPanel);
			warper.addPropertyChangeListener(this);
			warper.execute();
			
		} catch (Exception e) {
			error("Error running warping", e);
			
			ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(targetDataset)
			.build();
		
			JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();

			chartPanel.setChart(chart);
			setEnabled(true);
		} 
	}
	
	@Override
	public void setEnabled(boolean b){
		signalBox.setEnabled(b);
		cellsWithSignalsBox.setEnabled(b);
		straightenMeshBox.setEnabled(b);
		runButton.setEnabled(b);
		datasetBoxOne.setEnabled(b);
		datasetBoxTwo.setEnabled(b);
		addToImage.setEnabled(b);
	}
	
	
	public void finished(){
		try {
			updateChart(warper.get());
			
			setEnabled(true);
			setStatusLoaded();
			
		} catch (Exception e) {
			error("Error getting warp results", e);
		}
	}
	
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {

		if(evt.getNewValue() instanceof Integer){
			int percent = (Integer) evt.getNewValue(); // should be percent
			if(percent >= 0 && percent <=100){       							
					if(progressBar.isIndeterminate()){
						progressBar.setIndeterminate(false);
					}
					progressBar.setValue(percent);
//					int cellNumber = i+1;
//					progressBar.setString(cellNumber+" of "+totalCells);	
	        }
		}
		

		if(evt.getPropertyName().equals("Finished")){
			finest("Worker signaled finished");
			progressBar.setValue(0);
			progressBar.setVisible(false);
			cellsDone = 0;
			finished();
		}
		
	}
	
	private void updateChart(ImageProcessor mergedImage){
		
		Runnable task = () -> { 
			
			Color colour = Color.WHITE;
			try {
				colour = datasetBoxOne.getSelectedDataset().getCollection()
						.getSignalGroup(signalBox.getSelectedID())
						.getGroupColour();
				if(colour==null){
					colour = Color.WHITE;
				}
			} catch (UnavailableSignalGroupException e) {
				stack(e);
				colour = Color.WHITE;
			}
			
			ImageProcessor recoloured = ImageFilterer.recolorImage(mergedImage, colour);
							
			boolean straighten = straightenMeshBox.isSelected();
			
			ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(datasetBoxTwo.getSelectedDataset())
				.setShowXAxis(false)
				.setShowYAxis(false)
				.setShowBounds(false)
				.setStraightenMesh(straighten)
				.build();
			
			
			if(!isAddToImage){
				mergableImages.clear();
			} 
			
			mergableImages.add(recoloured);
			ImageProcessor averaged = ImageConverter.averageRGBImages(mergableImages);

			final JFreeChart chart = new OutlineChartFactory(options).makeSignalWarpChart(averaged);

			chartPanel.setChart(chart);
			chartPanel.restoreAutoBounds();


		};
		Thread thr = new Thread(task);
		thr.start();		
	}
	
	private void updateOutlineChart(){

		if(isAddToImage){
			return;
		}
		JFreeChart chart = null;
		if(straightenMeshBox.isSelected()){
			
			ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(datasetBoxTwo.getSelectedDataset())
					.setShowMesh(true)
					.setStraightenMesh(true)
					.setShowAnnotations(false)
					.build();
			
			try{
				chart = new ConsensusNucleusChartFactory(options).makeConsensusChart();
			} catch(Exception ex){
				error("Error making straight mesh chart", ex);
				chart = ConsensusNucleusChartFactory.makeErrorChart();
			}
			
		} else {
			
			ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(datasetBoxTwo.getSelectedDataset())
			.build();
		
			chart = new ConsensusNucleusChartFactory(options).makeNucleusOutlineChart();
			
		}
		chartPanel.setChart(chart);
	}
	

	@Override
	public void actionPerformed(ActionEvent e) {
		IAnalysisDataset sourceDataset = datasetBoxOne.getSelectedDataset();
		
		SignalManager m =  sourceDataset.getCollection().getSignalManager();
		if( ! m.hasSignals()){
			signalBox.setEnabled(false);
			cellsWithSignalsBox.setEnabled(false);
			straightenMeshBox.setEnabled(false);
			runButton.setEnabled(false);
			datasetBoxTwo.setEnabled(false);
			
		} else {
			signalBox.setEnabled(true);
			cellsWithSignalsBox.setEnabled(true);
			straightenMeshBox.setEnabled(true);
			runButton.setEnabled(true);
			datasetBoxTwo.setEnabled(true);
		}
		
		if(e.getSource()==datasetBoxOne){
			
			if( m.hasSignals()){
				
				signalBox.setDataset(sourceDataset);
			}

			
		}
		
		if(e.getSource()==straightenMeshBox){
			
			updateOutlineChart();
			
		}
		
		if(e.getSource()==datasetBoxTwo){
			updateOutlineChart();
		}
		
		if(e.getSource()==addToImage){
			isAddToImage = addToImage.isSelected();
		}
								
		boolean cellsWithSignals = cellsWithSignalsBox.isSelected();
		
		totalCells = cellsWithSignals 
				? m.getNumberOfCellsWithNuclearSignals(signalBox.getSelectedID()) 
				: datasets.get(0).getCollection().size();
				
		
	}

	
}
