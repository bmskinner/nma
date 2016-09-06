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

package analysis.detection;

import gui.ImageType;
import gui.dialogs.NucleusDetectionImageProber.NucleusImageType;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;

import java.awt.Color;
import java.io.File;
import java.util.List;
import javax.swing.table.TableModel;

import stats.NucleusStatistic;
import utility.Constants;
import components.Cell;
import components.CellularComponent;
import components.nuclei.Nucleus;
import analysis.AnalysisOptions;
import analysis.AnalysisOptions.CannyOptions;
import analysis.nucleus.NucleusFinder;

public class NucleusProberWorker extends ImageProberWorker {

	public NucleusProberWorker(File f, AnalysisOptions options, ImageType type, TableModel model) {
		super(f, options, type, model);
		
	}
	
	
	protected void analyseImages() throws Exception {
		ImageStack imageStack = ImageImporter.getInstance().importImage(file);
		finer("Imported image as stack");
		
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
		finer("Creating processed images");
		
		CannyOptions cannyOptions = options.getCannyOptions("nucleus");

		ImageProcessor openProcessor = ImageExporter.getInstance().makeGreyRGBImage(imageStack).getProcessor();
		openProcessor.invert();
					
		if( cannyOptions.isUseCanny()) { //TODO: Turning off Canny causes error
			
			// Make a copy of the counterstain to use at each processing step
			ImageProcessor processedImage = imageStack.getProcessor(Constants.COUNTERSTAIN).duplicate();
			
			
			// Run a Kuwahara filter to enhance edges in the image
			ImageProcessor kuwaharaProcessor = processedImage.duplicate();
			
			if(cannyOptions.isUseKuwahara()){
				finer("Applying Kuwahara filter");
				kuwaharaProcessor = ImageFilterer.runKuwaharaFiltering(processedImage, cannyOptions.getKuwaharaKernel());
				processedImage = kuwaharaProcessor.duplicate(); 
				
			}
			kuwaharaProcessor.invert();
			
			IconCell iconCell = makeIconCell(kuwaharaProcessor, NucleusImageType.KUWAHARA);
			iconCell.setEnabled(cannyOptions.isUseKuwahara());
			publish(iconCell);
			
			
			// Flatten the chromocentres
			
			ImageProcessor flattenProcessor = processedImage.duplicate();
			if(cannyOptions.isUseFlattenImage()){
				
				finer("Applying flattening filter");
				flattenProcessor = ImageFilterer.squashChromocentres(processedImage, cannyOptions.getFlattenThreshold());
				processedImage = flattenProcessor.duplicate();
			} 
			flattenProcessor.invert();
			
			IconCell iconCell1 = makeIconCell(flattenProcessor, NucleusImageType.FLATTENED);
			iconCell1.setEnabled(cannyOptions.isUseFlattenImage());
			publish(iconCell1);
			
			
			// Run the edge detection
			
			finer("Detecting edges");
			processedImage = ImageFilterer.runEdgeDetector(processedImage, cannyOptions);
			ImageProcessor invertedEdges = processedImage.duplicate(); // make a copy for display only
			invertedEdges.invert();
			
			IconCell iconCell2 = makeIconCell(invertedEdges, NucleusImageType.EDGE_DETECTION);
			publish(iconCell2);
			

			// Run morhological closing
			
			processedImage = ImageFilterer.morphologyClose(processedImage, cannyOptions.getClosingObjectRadius());
			ImageProcessor closedIP = processedImage.duplicate(); // make a copy for display only
			closedIP.invert();
			IconCell iconCell3 = makeIconCell(closedIP, NucleusImageType.MORPHOLOGY_CLOSED);
			publish(iconCell3);


//			Show the detected objects
			
			/*
			 * Store the size and circularity options, and set them to allow all
			 * Get the objects in the image
			 * Restore size and circ options
			 * Outline the objects that fail 
			 */
			List<Cell> cells = getCells(imageStack, file);
		
			for(Cell cell : cells){

				drawNucleus(cell, openProcessor);
			}
			
			IconCell iconCell4 = makeIconCell(openProcessor, NucleusImageType.DETECTED_OBJECTS);
			publish(iconCell4);
			
						
		} else {
			// Threshold option selected - do not run edge detection
			
			List<Cell> cells = getCells(imageStack, file);
			
			for(Cell cell : cells){

				drawNucleus(cell, openProcessor);
			}
			
			for(ImageType key : NucleusImageType.values()){
				
				IconCell iconCell = makeIconCell(openProcessor, key);
				publish(iconCell );
			}
		}
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
		
		finer("Widening detection parameters");

		options.setMinNucleusSize(50);
		options.setMaxNucleusSize(imageStack.getWidth()*imageStack.getHeight());
		options.setMinNucleusCirc(0);
		options.setMaxNucleusCirc(1);
		
		finer("Finding cells");
		
		 NucleusFinder finder = new NucleusFinder(options, null);
		
		List<Cell> cells = finder.getCells(imageStack, imageFile);
		
		finer("Resetting detetion parameters");
		
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
		FloatPolygon polygon = n.createPolygon();
		PolygonRoi roi = new PolygonRoi(polygon, PolygonRoi.POLYGON);
		roi.setLocation(positions[CellularComponent.X_BASE], positions[CellularComponent.Y_BASE]);
		ip.setLineWidth(2);
		ip.draw(roi);
	}
	
	/**
	 * Check the given nucleus size and circ parameters against options
	 * @param n the nucleus to check
	 * @return boolean ok
	 * @throws Exception 
	 */
	private boolean checkNucleus(Nucleus n) throws Exception{
		boolean result = true;
		
		if(n.getStatistic(NucleusStatistic.AREA) < options.getMinNucleusSize()){
			
			result = false;
		}
		
		if(n.getStatistic(NucleusStatistic.AREA) > options.getMaxNucleusSize()){
			
			result = false;
		}
		
		if(n.getStatistic(NucleusStatistic.CIRCULARITY) < options.getMinNucleusCirc()){
			
			result = false;
		}
		
		if(n.getStatistic(NucleusStatistic.CIRCULARITY) > options.getMaxNucleusCirc()){
			
			result = false;
		}
		
		return result;
	}
	
}
