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
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
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

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImageCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.UncomparableMeshImageException;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.charting.charts.ConsensusNucleusChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.LoadingIconDialog;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.components.panels.DatasetSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.SignalGroupSelectionPanel;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

@SuppressWarnings("serial")
public class SignalWarpingDialog extends LoadingIconDialog implements PropertyChangeListener, ActionListener{
	
	private static final String SOURCE_DATASET_LBL  = "Source dataset";
	private static final String TARGET_DATASET_LBL  = "Target dataset";
	private static final String SIGNAL_GROUP_LBL    = "Signal group";
	private static final String INCLUDE_CELLS_LBL   = "Only include cells with signals";
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
	
	private SignalWarper warper;
	
	private JProgressBar progressBar = new JProgressBar(0, 100);
	
	private int totalCells = 0; // The cells in the signal group being processed
	private int cellsDone  = 0; // Progress through cells in the signal group
	
	private boolean isAddToImage = false;
	
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
		
		// Can warp onto any dataset with consensus
//		List<IAnalysisDataset> targets = DatasetListManager.getInstance()
//				.getAllDatasets().stream()
//				.filter( d -> d.getCollection().hasConsensus())
//				.collect(Collectors.toList());
//		
//		datasetBoxTwo = new DatasetSelectionPanel(targets);
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
		
		addToImage = new JCheckBox("Add to image", false);
		addToImage.addActionListener(this);
		lowerPanel.add(addToImage);
		
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
		addToImage.setEnabled(b);
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
				
				if( ! targetDataset.getCollection().hasConsensus()){
					warn("No consensus nucleus in dataset");
					return false;
				} else {
					generateImages();
				}
				
			} catch (Exception e){
				warn("Error in warper");
				stack("Error in signal warper", e);
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
	        
//	        updateChart();
	                
	        
	    }
		
		@Override
	    public void done() {
	    	
	    	finest("Worker completed task");
	    	updateChart();
	    	 try {
	            if(this.get()){
	            	finest("Firing trigger for sucessful task");
	                firePropertyChange("Finished", getProgress(), IAnalysisWorker.FINISHED);            

	            } else {
	            	finest("Firing trigger for failed task");
	                firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
	            }
	        } catch (InterruptedException e) {
	        	error("Interruption error in worker", e);
	        	firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
	        } catch (ExecutionException e) {
	        	error("Execution error in worker", e);
	        	firePropertyChange("Error", getProgress(), IAnalysisWorker.ERROR);
	       }

	    } 
		
		private void updateChart(){
			
			Runnable task = () -> { 
				
				Color colour = Color.WHITE;
				try {
					colour = sourceDataset.getCollection().getSignalGroup(signalGroup).getGroupColour();
					if(colour==null){
						colour = Color.WHITE;
					}
				} catch (UnavailableSignalGroupException e) {
					stack(e);
					colour = Color.WHITE;
				}
				
				ImageProcessor recoloured = recolorImage(mergedImage, colour);
								
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
				ImageProcessor averaged = mergeImages(mergableImages);

				final JFreeChart chart = new OutlineChartFactory(options).makeSignalWarpChart(averaged);
				Runnable update = () -> { 
					chartPanel.setChart(chart);
					chartPanel.restoreAutoBounds();
				};
				SwingUtilities.invokeLater( update );

			};
			Thread thr = new Thread(task);
			thr.start();		
		}
		
	
		/**
		 * Recolour the given image to use the given colour, weighting the
		 * greyscale values by the HSB saturation level
		 * @param ip
		 * @param colour
		 * @return
		 */
		private ImageProcessor recolorImage(ImageProcessor ip, Color colour){
			
			float[] hsb = Color.RGBtoHSB(colour.getRed(), colour.getGreen(), colour.getBlue(), null);
			
			// Scale the brightness from 0-bri across the image
			ColorProcessor cp = new ColorProcessor(ip.getWidth(), ip.getHeight());
			
			for(int i=0; i<mergedImage.getPixelCount(); i++){
				int pixel = mergedImage.get(i);
				
				if(pixel==255){ // skip fully white pixels
					int full = 16777215;
					cp.set(i, full);
				} else {
					float pct = (float)(255f - (255f-pixel )) / 255f;
					pct = 1f-pct;
					int full = Color.HSBtoRGB(hsb[0], pct, 1); // if issues, replace 1 with the hsb[2] - for now it keeps the white border
					cp.set(i, full);
				}
				
				
			}

			return cp;
		}
		
		/**
		 * Merge the given list of images by averaging the RGB values
		 * @param list
		 * @return
		 */
		private ImageProcessor mergeImages(List<ImageProcessor> list){
			
			if(list==null || list.isEmpty()){
				throw new IllegalArgumentException("List null or empty");
			}
			
			// Check images are same dimensions
			int w = list.get(0).getWidth();
			int h = list.get(0).getHeight();
			
			for(ImageProcessor ip : list){
				if(w!=ip.getWidth() || h!=ip.getHeight()){
					throw new IllegalArgumentException("Dimensions do not match");
				}
			}
			
			ImageProcessor cp = new ColorProcessor(w, h);
			
			// Average the colours at each pixel
			int pixelCount = w*h;
			for(int i=0; i<pixelCount; i++){
				
				int r=0, g=0, b=0;
				for(ImageProcessor ip : list){
					int pixel = ip.get(i);
					
					if(ip instanceof ColorProcessor){
						r += (pixel >> 16) & 0xFF;
						g += (pixel >> 8) & 0xFF;
						b += pixel & 0xFF;
					} else {
						r+=pixel;
						g+=pixel;
						b+=pixel;
					}
					
				}
				
				r/=list.size();
				g/=list.size();
				b/=list.size();
					
				int rgb = r;
				rgb = (rgb << 8) + g;
				rgb = (rgb << 8) + b;
				cp.set(i, rgb);
				
			}
			
			return cp;
		}
		
	
		
		private void generateImages(){
			finer("Generating warped images for "+sourceDataset.getName());

			Mesh<Nucleus> meshConsensus;
			try {
				meshConsensus = new NucleusMesh( targetDataset.getCollection().getConsensus());
			} catch (MeshCreationException e2) {
				stack("Error creating mesh",e2);
				return;
			}
			
			if(straighten){
				meshConsensus = meshConsensus.straighten();
			}
			
			Rectangle r = meshConsensus.toPath().getBounds();
			
			// The new image size
			int w = r.width  ;
			int h = r.height ;
			

			
			Set<ICell> cells = getCells(cellsWithSignals);

			
			int cellNumber = 0;
			
			
			for(ICell cell : cells){
				
				for(Nucleus n : cell.getNuclei()){
					fine("Drawing signals for "+n.getNameAndNumber());

					Mesh<Nucleus> cellMesh;
					try {
						cellMesh = new NucleusMesh(n, meshConsensus);

						if(straighten){
							cellMesh = cellMesh.straighten();
						}

						// Get the image with the signal
						ImageProcessor ip = n.getSignalCollection().getImage(signalGroup);
						finest("Image for "+n.getNameAndNumber()+" is "+ip.getWidth()+"x"+ip.getHeight());

						// Create NucleusMeshImage from nucleus.
						finer("Making nucleus mesh image");
						ImageProcessor warped;
						try {
							MeshImage<Nucleus> im = new NucleusMeshImage(cellMesh,ip);

							// Draw NucleusMeshImage onto consensus mesh.
							finer("Warping image onto consensus mesh");
							warped = im.drawImage(meshConsensus);
							
						} catch (UncomparableMeshImageException | MeshImageCreationException e) {
							stack("Cannot make mesh for "+n.getNameAndNumber(), e);
							warped = null;
						}

						warpedImages[cellNumber] = warped;


					} catch(IllegalArgumentException e){

						stack(e.getMessage(), e);
						warn(e.getMessage());

						// Make a blank image for the array
						warpedImages[cellNumber] = ImageFilterer.createBlankByteProcessor(w, h);


					} catch (UnloadableImageException e) {
						stack("Unable to load signal image for signal group "+signalGroup+" in nucleus "+n.getNameAndNumber(), e);
						warpedImages[cellNumber] = ImageFilterer.createBlankByteProcessor(w, h);
					} catch (MeshCreationException e1) {
						stack("Error creating mesh",e1);
						warpedImages[cellNumber] = ImageFilterer.createBlankByteProcessor(w, h);
					} finally {

						mergedImage = combineImages(w, h);
						mergedImage = rescaleImageIntensity();
						publish(cellNumber++);
					}
				}
				
			}
			
		}
		
		/**
		 * Get the cells to be used for the warping
		 * @param withSignalsOnly
		 * @return
		 */
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
		
		/**
		 * Create a new image processor with the average of all warped images
		 * @return
		 */
		private ImageProcessor combineImages(int w, int h){
			
			
			// Create an empty white processor of the correct dimensions
			ImageProcessor mergeProcessor = ImageFilterer.createBlankByteProcessor(w, h);
			
			int nonNull = 0;
			
			// check sizes match
			for(ImageProcessor ip : warpedImages){
				if(ip==null){
					continue;
				}
				nonNull++;
				if(ip.getHeight()!=h && ip.getWidth()!=w){
					warn("Sizes of warped images do not match");
					return mergeProcessor;
				}
			}
			

			
			if(nonNull==0){
				return mergeProcessor;
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
