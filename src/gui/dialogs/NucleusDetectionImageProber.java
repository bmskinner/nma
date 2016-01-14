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
package gui.dialogs;

import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import analysis.AnalysisOptions;
import analysis.ImageFilterer;
import analysis.AnalysisOptions.CannyOptions;
import analysis.nucleus.NucleusFinder;
import components.Cell;
import components.nuclei.Nucleus;
import gui.ImageType;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;
import utility.Constants;
import utility.Utils;

@SuppressWarnings("serial")
public class NucleusDetectionImageProber extends ImageProber {
	
	public NucleusDetectionImageProber(AnalysisOptions options, Logger logger, File folder) {
		super(options, logger, NucleusImageType.DETECTED_OBJECTS, folder);
		createFileList(folder);
		this.setVisible(true);
	}
	
	/**
	 * Hold the stages of the detection pipeline to display 
	 */
	private enum NucleusImageType implements ImageType {
		KUWAHARA ("Kuwahara filtered"),
		FLATTENED ("Flattened"),
		EDGE_DETECTION ("Edge detection"),
		MORPHOLOGY_CLOSED ("Morphology closed"),
		DETECTED_OBJECTS ("Detected objects");
		
		private String name;
		
		NucleusImageType(String name){
			this.name = name;
		}
		public String toString(){
			return this.name;
		}
		
		public ImageType[] getValues(){
//			NucleusImageType[] a = NucleusImageType.values();
//			ImageType[] r = new ImageType[a.length];
//			for(int i=0; i<a.length; i++){
//				r[i] = a[i];
//			}
			return NucleusImageType.values();
		}
	}
	
	/**
	 * Import the given file as an image, detect nuclei and
	 * display the image with annotated nuclear outlines
	 * @param imageFile
	 */
	@Override
	protected void importAndDisplayImage(File imageFile){

		try {
			setStatusLoading();
			this.setLoadingLabelText("Probing image "+index+": "+imageFile.getAbsolutePath()+"...");
			
			ImageStack imageStack = ImageImporter.importImage(imageFile, programLogger);
			programLogger.log(Level.FINEST, "Imported image as stack");
			
			/*
			 * Insert steps to show each applied filter in the same order as from analysis
			 * Kuwahara filtering
			 * Chromocentre flattening
			 * Edge detector
			 *    Morphology closing
			 * Final image
			 * 
			 * Make an icon from each
			 */
			programLogger.log(Level.FINEST, "Creating processed images");
			
			CannyOptions cannyOptions = options.getCannyOptions("nucleus");
//			ImageProcessor openProcessor = ImageExporter.convertToRGB(imageStack).getProcessor();
			ImageProcessor openProcessor = ImageExporter.makeGreyRGBImage(imageStack).getProcessor();
			openProcessor.invert();
						
			if( cannyOptions.isUseCanny()) { //TODO: Turning off Canny causes error
				
				// Make a copy of the counterstain to use at each processing step
				ImageProcessor processedImage = imageStack.getProcessor(Constants.COUNTERSTAIN).duplicate();
				
				// before passing to edge detection
				// run a Kuwahara filter to enhance edges in the image
				//TODO: Turning off Kuwahara causes error
				if(cannyOptions.isUseKuwahara()){
					programLogger.log(Level.FINEST, "Applying Kuwahara filter");
					ImageProcessor kuwaharaProcessor = ImageFilterer.runKuwaharaFiltering(imageStack, Constants.COUNTERSTAIN, cannyOptions.getKuwaharaKernel());
					processedImage = kuwaharaProcessor.duplicate(); 
					kuwaharaProcessor.invert();
					procMap.put(NucleusImageType.KUWAHARA, kuwaharaProcessor);
					iconMap.get(NucleusImageType.KUWAHARA).setText(NucleusImageType.KUWAHARA.toString());
				} else {
					procMap.put(NucleusImageType.KUWAHARA, processedImage.duplicate());
					iconMap.get(NucleusImageType.KUWAHARA).setText(NucleusImageType.KUWAHARA.toString()+" (disabled)");
				}
				
				if(cannyOptions.isUseFlattenImage()){
					programLogger.log(Level.FINEST, "Applying flattening filter");
					ImageProcessor flattenProcessor = ImageFilterer.squashChromocentres(processedImage, cannyOptions.getFlattenThreshold());
					processedImage = flattenProcessor.duplicate();
					flattenProcessor.invert();
					procMap.put(NucleusImageType.FLATTENED, flattenProcessor);
					iconMap.get(NucleusImageType.FLATTENED).setText(NucleusImageType.FLATTENED.toString());
				} else {
					procMap.put(NucleusImageType.FLATTENED, processedImage.duplicate());
					iconMap.get(NucleusImageType.FLATTENED).setText(NucleusImageType.FLATTENED.toString()+" (disabled)");
				}
				
				programLogger.log(Level.FINEST, "Detecting edges");
				ImageProcessor edgesProcessor = ImageFilterer.runEdgeDetector(processedImage, cannyOptions);
				procMap.put(NucleusImageType.EDGE_DETECTION, edgesProcessor);
				
				ImageProcessor closedProcessor = ImageFilterer.morphologyClose(edgesProcessor, cannyOptions.getClosingObjectRadius());
				procMap.put(NucleusImageType.MORPHOLOGY_CLOSED, closedProcessor);
				
				procMap.put(NucleusImageType.DETECTED_OBJECTS, openProcessor);
				
				edgesProcessor.invert();
				closedProcessor.invert();
							
			} else {
				// Threshold option selected - do not run edge detection
				for(ImageType key : NucleusImageType.values()){
					procMap.put(key, openProcessor);
				}
			}

			programLogger.log(Level.FINEST, "Processed images created");
						
			/*
			 * Store the size and circularity options, and set them to allow all
			 * Get the objects in the image
			 * Restore size and circ options
			 * Outline the objects that fail 
			 */
			List<Cell> cells = getCells(imageStack, imageFile);
		
			for(Cell cell : cells){

				drawNucleus(cell, openProcessor);
			}

			programLogger.log(Level.FINE, "Displaying nuclei");
			
			// update the map of icons
			updateImageThumbnails();

			this.setLoadingLabelText("Showing "+cells.size()+" nuclei in "+imageFile.getAbsolutePath());
			this.setStatusLoaded();
//			headerLabel.setIcon(null);
//			headerLabel.repaint();

		} catch (Exception e) { // end try
			programLogger.log(Level.SEVERE, "Error in image processing", e);
			setStatusError();
		} // end catch
	}

	
	/**
	 * Get the cells in the given stack without the 
	 * size and circularity parameters
	 * @param imageStack
	 * @param imageFile
	 * @return
	 * @throws Exception 
	 */
	private List<Cell> getCells(ImageStack imageStack, File imageFile) throws Exception{
		double minSize = options.getMinNucleusSize();
		double maxSize = options.getMaxNucleusSize();
		double minCirc = options.getMinNucleusCirc();
		double maxCirc = options.getMaxNucleusCirc();
		
		programLogger.log(Level.FINEST, "Widening detection parameters");

		options.setMinNucleusSize(50);
		options.setMaxNucleusSize(imageStack.getWidth()*imageStack.getHeight());
		options.setMinNucleusCirc(0);
		options.setMaxNucleusCirc(1);
		
		programLogger.log(Level.FINEST, "Finding cells");
		
		List<Cell> cells = NucleusFinder.getCells(imageStack, 
				options, 
				programLogger, 
				imageFile, 
				null);
		
		programLogger.log(Level.FINEST, "Resetting detetion parameters");
		
		options.setMinNucleusSize(minSize);
		options.setMaxNucleusSize(maxSize);
		options.setMinNucleusCirc(minCirc);
		options.setMaxNucleusCirc(maxCirc);
		return cells;
	}
	
	/**
	 * Draw the outline of a nucleus on the given processor
	 * @param cell
	 * @param ip
	 */
	private void drawNucleus(Cell cell, ImageProcessor ip) throws Exception {
		if(cell==null){
			throw new IllegalArgumentException("Input cell is null");
		}
		
		Nucleus n = cell.getNucleus();
		// annotate the image processor with the nucleus outline
		
		if(checkNucleus(n)){
			ip.setColor(Color.ORANGE);
		} else {
			ip.setColor(Color.RED);
		}
		
		
		double[] positions = n.getPosition();
		FloatPolygon polygon = Utils.createPolygon(n.getBorderList());
		PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
		roi.setLocation(positions[Nucleus.X_BASE], positions[Nucleus.Y_BASE]);
		ip.setLineWidth(2);
		ip.draw(roi);
	}
		
	/**
	 * Check the given nucleus size and circ parameters against options
	 * @param n the nucleus to check
	 * @return boolean ok
	 */
	private boolean checkNucleus(Nucleus n){
		boolean result = true;
		
		if(n.getArea() < options.getMinNucleusSize()){
			
			result = false;
		}
		
		if(n.getArea() > options.getMaxNucleusSize()){
			
			result = false;
		}
		
		if(n.getCircularity() < options.getMinNucleusCirc()){
			
			result = false;
		}
		
		if(n.getCircularity() > options.getMaxNucleusCirc()){
			
			result = false;
		}
		
		return result;
	}

}
