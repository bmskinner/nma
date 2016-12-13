package com.bmskinner.nuclear_morphology.analysis.signals;

import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.AnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshImage;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class SignalWarper extends AnalysisWorker {
	
	private IAnalysisDataset targetDataset;
	private UUID signalGroup;
	private boolean cellsWithSignals; // Only warp the cell images with detected signals
	private boolean straighten; // Straighten the meshes
	ImageProcessor[] warpedImages;
	
	ImageProcessor mergedImage = null;
		
	public SignalWarper(IAnalysisDataset dataset, IAnalysisDataset target, UUID signalGroup, boolean cellsWithSignals, boolean straighten){
		super(dataset);
		this.targetDataset    = target;
		this.signalGroup      = signalGroup;
		this.cellsWithSignals = cellsWithSignals;
		this.straighten       = straighten;
		
		// Count the number of cells to include

		Set<ICell> cells;
		if(cellsWithSignals){
			SignalManager m =  getDataset().getCollection().getSignalManager();
			cells = m.getCellsWithNuclearSignals(signalGroup, true);
			
		} else {
			cells = getDataset().getCollection().getCells();
		}
		int count = cells.size();

		
		warpedImages = new ImageProcessor[ count ];
		this.setProgressTotal(count);
		
		
		fine("Created signal warper for "+dataset.getName()+" signal group "+signalGroup+" with "+count+" cells");
	}
	

	@Override
	protected Boolean doInBackground() throws Exception {
		boolean result = false;
		try {
			finer("Running warper");
			
			if( ! targetDataset.getCollection().hasConsensusNucleus()){
				warn("No consensus nucleus in dataset");
				return false;
			}
			
			generateImages();
			result = true;
			
		} catch (Exception e){
			result = false;
		}
		return result;
	}
	
	/**
	 * Get the images with warped signals
	 * @return
	 */
	public ImageProcessor getResult(){
		return mergedImage;
	} 
	
	public boolean hasResult(){
		return  mergedImage!=null;
	}
	
	private void generateImages(){
		finer("Generating warped images for "+getDataset().getName());
		finest("Fetching consensus nucleus from target dataset");
		NucleusMesh meshConsensus = new NucleusMesh( targetDataset.getCollection().getConsensusNucleus());
		
		if(straighten){
			meshConsensus = meshConsensus.straighten();
		}
		
		SignalManager m =  getDataset().getCollection().getSignalManager();
		
		
		
		Set<ICell> cells;
		if(cellsWithSignals){
			finer("Only fetching cells with signals");
			cells = m.getCellsWithNuclearSignals(signalGroup, true);
		} else {
			finer("Fetching all cells");
			cells = getDataset().getCollection().getCells();
			
		}
		
		int cellNumber = 0;
		
		
		for(ICell cell : cells){
			fine("Drawing signals for cell "+cell.getNucleus().getNameAndNumber());
			// Get each nucleus. Make a mesh.
			NucleusMesh cellMesh = new NucleusMesh(cell.getNucleus(), meshConsensus);
			
			if(straighten){
				cellMesh = cellMesh.straighten();
			}
			
			// Get the image with the signal
			ImageProcessor ip;
			try {
				ip = cell.getNucleus().getSignalCollection().getImage(signalGroup);

				finest("Image for "+cell.getNucleus().getNameAndNumber()+" is "+ip.getWidth()+"x"+ip.getHeight());

				// Create NucleusMeshImage from nucleus.
				finer("Making nucleus mesh image");
				NucleusMeshImage im = new NucleusMeshImage(cellMesh,ip);

				// Draw NucleusMeshImage onto consensus mesh.
				finer("Warping image onto consensus mesh");
				ImageProcessor warped = im.meshToImage(meshConsensus);
				finest("Warped image is "+ip.getWidth()+"x"+ip.getHeight());
				warpedImages[cellNumber] = warped;
				mergedImage = combineImages();
				mergedImage = rescaleImageIntensity();
				finer("Completed cell "+cellNumber);
			} catch (UnloadableImageException e) {
				warn("Unable to load signal image for signal group "+signalGroup+" in cell "+cell.getNucleus().getNameAndNumber());
				stack("Unable to load signal image for signal group "+signalGroup+" in cell "+cell.getNucleus().getNameAndNumber(), e);
				
			}

			publish(cellNumber++);


		}
		
	}
	
	/**
	 * Create a new image processor with the average of all warped images
	 * @return
	 */
	private ImageProcessor combineImages(){
		int w = warpedImages[0].getWidth();
		int h = warpedImages[0].getHeight();
		
		// Create an empty white processor
		ImageProcessor mergeProcessor = new ByteProcessor(w, h);
		for(int i=0; i<mergeProcessor.getPixelCount(); i++){
			mergeProcessor.set(i, 255); // set all to white initially
		}
		
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
		
		double range        = maxIntensity - minIntensity;
		finer("Image intensity runs "+minIntensity+"-"+maxIntensity);
		
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
