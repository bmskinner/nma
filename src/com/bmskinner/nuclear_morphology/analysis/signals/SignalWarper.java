package com.bmskinner.nuclear_morphology.analysis.signals;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.jfree.chart.ChartPanel;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImageCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.UncomparableMeshImageException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * Warps signals from nuclei in a collection onto a target nucleus using
 * meshes. This is implemneted as a SwingWorker that returns the merged
 * overlay of all the signals in the collection
 * @author ben
 * @since 1.13.6
 *
 */
public class SignalWarper extends SwingWorker<ImageProcessor, Integer> implements Loggable {
	
	private IAnalysisDataset sourceDataset;
	private Nucleus target;
	private UUID signalGroup;
	private boolean cellsWithSignals; // Only warp the cell images with detected signals
	private boolean straighten; // Straighten the meshes
	ImageProcessor[] warpedImages;
//	private ChartPanel chartPanel;
	private int totalCells;
	
	ImageProcessor mergedImage = null;
		
	/**
	 * Constructor
	 * @param source the dataset with signals to be warped
	 * @param target the nucleus to warp signals onto
	 * @param signalGroup the signal group id to be warped
	 * @param cellsWithSignals if true, only cells with defined signals will be included
	 * @param straighten if true, the signals will be warped onto a straightened mesh
	 */
	public SignalWarper(IAnalysisDataset source, Nucleus target, UUID signalGroup, boolean cellsWithSignals, boolean straighten){
		
		if(source==null){
			throw new IllegalArgumentException("Must have source dataset");
		}
		
		if(target==null){
			throw new IllegalArgumentException("Must have target nucleus");
		}
		
		this.sourceDataset    = source;
		this.target   		  = target;
		this.signalGroup      = signalGroup;
		this.cellsWithSignals = cellsWithSignals;
		this.straighten       = straighten;
		
		// Count the number of cells to include

		Set<ICell> cells;
		if(cellsWithSignals){
			SignalManager m =  sourceDataset.getCollection().getSignalManager();
			cells = m.getCellsWithNuclearSignals(signalGroup, true);
			
		} else {
			cells = sourceDataset.getCollection().getCells();
		}
		totalCells = cells.size();

		
		warpedImages = new ImageProcessor[ totalCells ];
		
		
		fine("Created signal warper for "+sourceDataset.getName()+" signal group "+signalGroup+" with "+totalCells+" cells");
	}
	

	@Override
	protected ImageProcessor doInBackground() throws Exception {

		try {
			finer("Running warper");
			
			generateImages();
			
		} catch (Exception e){
			warn("Error in warper");
			stack("Error in signal warper", e);
			return null;
		} 
		
		return mergedImage;
		
	}
	
	
	@Override
    protected void process( List<Integer> chunks ) {
        
		
        for(Integer i : chunks){
        	
        	int percent = (int) ( (double) i / (double) totalCells * 100);
	        
	        if(percent >= 0 && percent <=100){
	        	setProgress(percent); // the integer representation of the percent
	        							
//					if(progressBar.isIndeterminate()){
//						progressBar.setIndeterminate(false);
//					}
//					progressBar.setValue(percent);
//					int cellNumber = i+1;
//					progressBar.setString(cellNumber+" of "+totalCells);	
	        }
	        
        	
        }
        
//        updateChart();
                
        
    }
	
	@Override
    public void done() {
    	
    	finest("Worker completed task");
//    	updateChart();
    	 try {
            if(this.get()!=null){
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
	
//	private void updateChart(){
//		
//		Runnable task = () -> { 
//			
//			Color colour = Color.WHITE;
//			try {
//				colour = sourceDataset.getCollection().getSignalGroup(signalGroup).getGroupColour();
//				if(colour==null){
//					colour = Color.WHITE;
//				}
//			} catch (UnavailableSignalGroupException e) {
//				stack(e);
//				colour = Color.WHITE;
//			}
//			
//			ImageProcessor recoloured = ImageFilterer.recolorImage(mergedImage, colour);
//							
//			boolean straighten = straightenMeshBox.isSelected();
//			
//			ChartOptions options = new ChartOptionsBuilder()
//				.setDatasets(datasetBoxTwo.getSelectedDataset())
//				.setShowXAxis(false)
//				.setShowYAxis(false)
//				.setShowBounds(false)
//				.setStraightenMesh(straighten)
//				.build();
//			
//			
//			if(!isAddToImage){
//				mergableImages.clear();
//			} 
//			
//			mergableImages.add(recoloured);
//			ImageProcessor averaged = ImageConverter.averageRGBImages(mergableImages);
//
//			final JFreeChart chart = new OutlineChartFactory(options).makeSignalWarpChart(averaged);
//
//			chartPanel.setChart(chart);
//			chartPanel.restoreAutoBounds();
//
//
//		};
//		Thread thr = new Thread(task);
//		thr.start();		
//	}
	
				
	private void generateImages(){
		finer("Generating warped images for "+sourceDataset.getName());

		Mesh<Nucleus> meshConsensus;
		try {
			meshConsensus = new NucleusMesh( target);
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

					List<ImageProcessor> list = Arrays.asList(warpedImages);
					mergedImage = ImageFilterer.averageByteImages(list);
					mergedImage = ImageFilterer.rescaleImageIntensity(mergedImage);
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
	
	
	
}
