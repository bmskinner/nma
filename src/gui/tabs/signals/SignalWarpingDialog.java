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
package gui.tabs.signals;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import io.UnloadableImageException;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import utility.Constants;
import components.Cell;
import components.ICell;
import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.OutlineChartFactory;
import charting.charts.panels.ExportableChartPanel;
import charting.options.ChartOptions;
import charting.options.DefaultChartOptions;
import charting.options.ChartOptionsBuilder;
import analysis.AnalysisDataset;
import analysis.IAnalysisDataset;
import analysis.mesh.NucleusMesh;
import analysis.mesh.NucleusMeshImage;
import analysis.signals.SignalManager;
import gui.LoadingIconDialog;
import gui.components.panels.DatasetSelectionPanel;
import gui.components.panels.SignalGroupSelectionPanel;

@SuppressWarnings("serial")
public class SignalWarpingDialog extends LoadingIconDialog implements PropertyChangeListener, ActionListener{
	
	private List<IAnalysisDataset> datasets;
	private ExportableChartPanel chartPanel;
	
	private DatasetSelectionPanel datasetBoxOne;
	private DatasetSelectionPanel datasetBoxTwo;
	
	private SignalGroupSelectionPanel signalBox;

	private JButton runButton;
	private JCheckBox cellsWithSignalsBox;
	private JCheckBox straightenMeshBox;
	
	private SignalWarper warper;
	
	private JProgressBar progressBar = new JProgressBar(0, 100);
	
	private int totalCells = 0; // The cells in the signal group being processed
	private int cellsDone  = 0; // Progress through cells in the signal group

	
	public SignalWarpingDialog(List<IAnalysisDataset> datasets){
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
		
		upperPanel.add(new JLabel("Source dataset"));
		upperPanel.add(datasetBoxOne);
		
		SignalManager m =  datasets.get(0).getCollection().getSignalManager();

		signalBox = new SignalGroupSelectionPanel(datasetBoxOne.getSelectedDataset());
		
		if(signalBox.hasSelection()){
			UUID id   = signalBox.getSelectedID();
			totalCells = m.getNumberOfCellsWithNuclearSignals(id);
		} else {
			signalBox.setEnabled(false);
		}

		upperPanel.add(new JLabel("Signal group"));
		upperPanel.add(signalBox);		
		finest("Added signal group box");
				
		signalBox.addActionListener(this);
		
		cellsWithSignalsBox = new JCheckBox("Only include cells with signals", true);
		cellsWithSignalsBox.addActionListener(this);
		upperPanel.add(cellsWithSignalsBox);
		
		straightenMeshBox = new JCheckBox("Straighten meshes", false);
		straightenMeshBox.addActionListener(this);
		upperPanel.add(straightenMeshBox);
		
		
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
		progressBar.setString("0 of "+totalCells);
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
						
		finest("Signal group: "+signalBox.getSelectedGroup());
		try {
			setStatusLoading();
			setEnabled(false);

			progressBar.setStringPainted(true);
			
			progressBar.setVisible(true);
			
			

			warper = new SignalWarper( signalBox.getSelectedID(), cellsWithSignals, straighten, chartPanel);
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
	}
	
	
	public void finished(){
		try {
//			updateChart();
			
			setEnabled(true);
			setStatusLoaded();
			
		} catch (Exception e) {
			error("Error getting warp results", e);
		}
	}
	
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {

//		if(evt.getNewValue() instanceof Integer){
//			int value = (Integer) evt.getNewValue(); // should be percent
//			finest("Property change: "+value);
//		}
		

		if(evt.getPropertyName().equals("Finished")){
			finest("Worker signaled finished");
			progressBar.setValue(0);
			progressBar.setVisible(false);
			cellsDone = 0;
			finished();
		}
		
	}
	
	private void updateOutlineChart(){
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
						
		boolean cellsWithSignals = cellsWithSignalsBox.isSelected();
		
		totalCells = cellsWithSignals 
				? m.getNumberOfCellsWithNuclearSignals(signalBox.getSelectedID()) 
				: datasets.get(0).getCollection().size();
				
		
	}

	
	private class SignalWarper extends SwingWorker<Boolean, Integer> {
		
		private IAnalysisDataset sourceDataset;
		private IAnalysisDataset targetDataset;
		private UUID signalGroup;
		private boolean cellsWithSignals; // Only warp the cell images with detected signals
		private boolean straighten; // Straighten the meshes
		ImageProcessor[] warpedImages;
		private ChartPanel chartPanel;
		
		ImageProcessor mergedImage = null;
			
		public SignalWarper(UUID signalGroup, boolean cellsWithSignals, boolean straighten, ChartPanel chartPanel){
			
			this.sourceDataset    = datasetBoxOne.getSelectedDataset();
			this.targetDataset    = datasetBoxTwo.getSelectedDataset();
			this.signalGroup      = signalGroup;
			this.cellsWithSignals = cellsWithSignals;
			this.straighten       = straighten;
			this.chartPanel       = chartPanel;
			
			// Count the number of cells to include

			Set<ICell> cells;
			if(cellsWithSignals){
				SignalManager m =  sourceDataset.getCollection().getSignalManager();
				cells = m.getCellsWithNuclearSignals(signalGroup, true);
				
			} else {
				cells = sourceDataset.getCollection().getCells();
			}
			int count = cells.size();

			
			warpedImages = new ImageProcessor[ count ];
			
			
			fine("Created signal warper for "+sourceDataset.getName()+" signal group "+signalGroup+" with "+count+" cells");
		}
		

		@Override
		protected Boolean doInBackground() throws Exception {

			try {
				finer("Running warper");
				
				if( ! targetDataset.getCollection().hasConsensusNucleus()){
					warn("No consensus nucleus in dataset");
					return false;
				} else {
					generateImages();
				}
				
			} catch (Exception e){
				error("Error in signal warper", e);
				return false;
			} 
			
			return true;
			
		}
		
		
		@Override
	    protected void process( List<Integer> chunks ) {
	        
			
	        for(Integer i : chunks){
	        	
	        	int percent = (int) ( (double) i / (double) totalCells * 100);
		        
		        if(percent >= 0 && percent <=100){
		        	setProgress(percent); // the integer representation of the percent
		        							
						if(progressBar.isIndeterminate()){
							progressBar.setIndeterminate(false);
						}
						progressBar.setValue(percent);
						int cellNumber = i+1;
						progressBar.setString(cellNumber+" of "+totalCells);	
		        }
		        
	        	
	        }
	        
	        updateChart();
	                
	        
	    }
		
		@Override
	    public void done() {
	    	
	    	finest("Worker completed task");

	    	 try {
	            if(this.get()){
	            	finest("Firing trigger for sucessful task");
	                firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());            

	            } else {
	            	finest("Firing trigger for failed task");
	                firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
	            }
	        } catch (InterruptedException e) {
	        	error("Interruption error in worker", e);
	        	firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
	        } catch (ExecutionException e) {
	        	error("Execution error in worker", e);
	        	firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
	       }

	    } 
		
		private void updateChart(){
			
			Runnable task = () -> { 
								
				boolean straighten = straightenMeshBox.isSelected();
				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(datasetBoxTwo.getSelectedDataset())
					.setShowXAxis(false)
					.setShowYAxis(false)
					.setShowBounds(false)
					.setStraightenMesh(straighten)
					.build();

				final JFreeChart chart = new OutlineChartFactory(options).makeSignalWarpChart(mergedImage);
						
				Runnable update = () -> { 
					chartPanel.setChart(chart);
					chartPanel.restoreAutoBounds();
				};
				SwingUtilities.invokeLater( update );
				
			};
			Thread thr = new Thread(task);
			thr.start();		
		}
		
	
		
		private void generateImages(){
			finer("Generating warped images for "+sourceDataset.getName());
			finest("Fetching consensus nucleus from target dataset");
			NucleusMesh meshConsensus = new NucleusMesh( targetDataset.getCollection().getConsensusNucleus());
			
			if(straighten){
				meshConsensus = meshConsensus.straighten();
			}
			

			
			Set<ICell> cells = getCells(cellsWithSignals);

			
			int cellNumber = 0;
			
			
			for(ICell cell : cells){
				fine("Drawing signals for cell "+cell.getNucleus().getNameAndNumber());

				NucleusMesh cellMesh;
				try {
					cellMesh = new NucleusMesh(cell.getNucleus(), meshConsensus);
					
					if(straighten){
						cellMesh = cellMesh.straighten();
					}
					
					// Get the image with the signal
					ImageProcessor ip = cell.getNucleus().getSignalCollection().getImage(signalGroup);
					finest("Image for "+cell.getNucleus().getNameAndNumber()+" is "+ip.getWidth()+"x"+ip.getHeight());
					
					// Create NucleusMeshImage from nucleus.
					finer("Making nucleus mesh image");
					NucleusMeshImage im = new NucleusMeshImage(cellMesh,ip);
					
					// Draw NucleusMeshImage onto consensus mesh.
					finer("Warping image onto consensus mesh");
					ImageProcessor warped = im.meshToImage(meshConsensus);
					finest("Warped image is "+ip.getWidth()+"x"+ip.getHeight());
					warpedImages[cellNumber] = warped;
					
					
				} catch(IllegalArgumentException e){
					
					fine("Error creating warping mesh");
					warn(e.getMessage());
					
					// Make a blank image for the array
					warpedImages[cellNumber] = createBlankProcessor();
					
					
				} catch (UnloadableImageException e) {
					warn("Unable to load signal image for signal group "+signalGroup+" in cell "+cell.getNucleus().getNameAndNumber());
					fine("Unable to load signal image for signal group "+signalGroup+" in cell "+cell.getNucleus().getNameAndNumber(), e);
					warpedImages[cellNumber] = createBlankProcessor();
				} finally {
					
					mergedImage = combineImages();
					mergedImage = rescaleImageIntensity();
					
					finer("Completed cell "+cellNumber);
					publish(cellNumber++);
				}
				
			}
			
		}
		
		private Set<ICell> getCells(boolean withSignalsOnly){
			
			SignalManager m =  sourceDataset.getCollection().getSignalManager();
			Set<ICell> cells;
			if(withSignalsOnly){
				finer("Only fetching cells with signals");
				cells = m.getCellsWithNuclearSignals(signalGroup, true);
			} else {
				finer("Fetching all cells");
				cells = sourceDataset.getCollection().getCells();
				
			}
			return cells;
		}
		
		
		private ImageProcessor createBlankProcessor(){
			int w = warpedImages[0].getWidth();
			int h = warpedImages[0].getHeight();
			
			// Create an empty white processor
			ImageProcessor ip = new ByteProcessor(w, h);
			for(int i=0; i<ip.getPixelCount(); i++){
				ip.set(i, 255); // set all to white initially
			}
			
			return ip;
		}
		
		/**
		 * Create a new image processor with the average of all warped images
		 * @return
		 */
		private ImageProcessor combineImages(){
			int w = warpedImages[0].getWidth();
			int h = warpedImages[0].getHeight();
			
			// Create an empty white processor
			ImageProcessor mergeProcessor = createBlankProcessor();
			
			int nonNull = 0;
			
			// check sizes match
			for(ImageProcessor ip : warpedImages){
				if(ip==null){
					continue;
				}
				nonNull++;
				if(ip.getHeight()!=h && ip.getWidth()!=w){
					return null;
				}
			}
			
			// Average the pixels
			
			for(int x=0; x<w; x++){
				for(int y=0; y<h; y++){

					int pixelTotal = 0;
					for(ImageProcessor ip : warpedImages){
						if(ip==null){
							continue;
						}
						pixelTotal += ip.get(x, y);
					}
					
					pixelTotal /= nonNull; // scale back down to 0-255;
					
					if(pixelTotal<255){// Ignore anything that is not signal - the background is already white
						mergeProcessor.set(x, y, pixelTotal);
					} 
				}
			}
			return mergeProcessor;
		}
		
		/**
		 * Adjust the merged image so that the brightet pixel is at 255
		 * @return
		 */
		private ImageProcessor rescaleImageIntensity(){
			finer("Rescaling image intensities to take full range");
			ImageProcessor result = new ByteProcessor(mergedImage.getWidth(), mergedImage.getHeight());
			// Find the range in the image	
			
			double maxIntensity = 0;
			double minIntensity = 255;
			for(int i=0; i<mergedImage.getPixelCount(); i++){
				int pixel = mergedImage.get(i);
				maxIntensity = pixel > maxIntensity ? pixel : maxIntensity;
				minIntensity = pixel < minIntensity ? pixel : minIntensity;
			}
			
			if(maxIntensity==0){
				return mergedImage;
			}
			
			double range        = maxIntensity - minIntensity;
			finest("Image intensity runs "+minIntensity+"-"+maxIntensity);
			
			// Adjust each pixel to the proportion in range 0-255
			for(int i=0; i<mergedImage.getPixelCount(); i++){
				int pixel = mergedImage.get(i);

				double proportion = ( (double) pixel - minIntensity) / range;
				
				int newPixel  = (int) (255 * proportion);
				finest("Converting pixel: "+pixel+" -> "+newPixel);
				result.set(i, newPixel);
			}
			return result;
		}

	}
	
}
