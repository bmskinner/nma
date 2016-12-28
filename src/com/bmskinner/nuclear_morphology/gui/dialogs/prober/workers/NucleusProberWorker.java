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

package com.bmskinner.nuclear_morphology.gui.dialogs.prober.workers;

import ij.ImageStack;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.io.File;
import java.util.List;

import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.analysis.image.NucleusAnnotator;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetector;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.DetectionImageType;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageProberTableCell;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageSet;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageType;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.utility.Constants;

public class NucleusProberWorker extends ImageProberWorker {

	public NucleusProberWorker(final File f, final IDetectionOptions options, final ImageSet type, final TableModel model) {
		super(f, options, type, model);
		
	}
	
	
	protected void analyseImages() throws Exception {
//		log("Analysing images");
		ImageStack imageStack =  new ImageImporter(file).importImage();
		finer("Imported image as stack");
		
		int stackNumber = Constants.rgbToStack(options.getChannel());
		
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
		
		ICannyOptions cannyOptions = options.getCannyOptions();

		ImageConverter conv = new ImageConverter(imageStack);
		
		conv = conv.convertToGreyscale();
		
		if(cannyOptions.isAddBorder()){	
			conv = conv.addBorder(10);
		}
		
		ImageProcessor openProcessor = conv.invert()
				.toProcessor();

					
		if( cannyOptions.isUseCanny()) { //TODO: Turning off Canny causes error
			
			if(cannyOptions.isAddBorder()){
				imageStack = new ImageConverter(imageStack)
					.addBorder(10)
					.toStack();

				finer("Added border");
			}
			
			// Make a copy of the counterstain to use at each processing step
			ImageProcessor processedImage = imageStack.getProcessor(stackNumber).duplicate();
			
			
			// Run a Kuwahara filter to enhance edges in the image
			ImageProcessor kuwaharaProcessor = processedImage.duplicate();
			
			if(cannyOptions.isUseKuwahara()){
				finer("Applying Kuwahara filter");
				kuwaharaProcessor = new ImageFilterer(processedImage)
					.runKuwaharaFiltering( cannyOptions.getKuwaharaKernel())
					.toProcessor();
				processedImage = kuwaharaProcessor.duplicate(); 
				
			}
			kuwaharaProcessor.invert();
			
			ImageProberTableCell iconCell = makeIconCell(kuwaharaProcessor, cannyOptions.isUseKuwahara(), DetectionImageType.KUWAHARA);
			publish(iconCell);
			
			
			// Flatten the chromocentres
			
			ImageProcessor flattenProcessor = processedImage.duplicate();
			if(cannyOptions.isUseFlattenImage()){
				
				finer("Applying flattening filter");
				flattenProcessor =  new ImageFilterer(processedImage)
					.squashChromocentres( cannyOptions.getFlattenThreshold())
					.toProcessor();
				
				processedImage = flattenProcessor.duplicate();
			} 
			flattenProcessor.invert();
			
			ImageProberTableCell iconCell1 = makeIconCell(flattenProcessor, cannyOptions.isUseFlattenImage(), DetectionImageType.FLATTENED);
			publish(iconCell1);
			
			
			// Run the edge detection
			
			finer("Detecting edges");
			processedImage = new ImageFilterer(processedImage).runEdgeDetector( cannyOptions).toProcessor();
			ImageProcessor invertedEdges = processedImage.duplicate(); // make a copy for display only
			invertedEdges.invert();
			
			ImageProberTableCell iconCell2 = makeIconCell(invertedEdges, true, DetectionImageType.EDGE_DETECTION);
			publish(iconCell2);
			

			// Run morhological closing
			
			processedImage = new ImageFilterer(processedImage)
				.morphologyClose(cannyOptions.getClosingObjectRadius()).toProcessor();
			ImageProcessor closedIP = processedImage.duplicate(); // make a copy for display only
			closedIP.invert();
			ImageProberTableCell iconCell3 = makeIconCell(closedIP, true, DetectionImageType.MORPHOLOGY_CLOSED);
			publish(iconCell3);


//			Show the detected objects
			
			/*
			 * Store the size and circularity options, and set them to allow all
			 * Get the objects in the image
			 * Restore size and circ options
			 * Outline the objects that fail 
			 */
			List<ICell> cells = getCells(imageStack, file);
		
			for(ICell cell : cells){

				drawNucleus(cell, openProcessor);
			}
			
			ImageProberTableCell iconCell4 = makeIconCell(openProcessor, true, DetectionImageType.DETECTED_OBJECTS);
			publish(iconCell4);
			
						
		} else {
			// Threshold option selected - do not run edge detection
			
			List<ICell> cells = getCells(imageStack, file);
			
			for(ICell cell : cells){

				drawNucleus(cell, openProcessor);
			}
			
			for(ImageType key : imageSet.values()){
				
				ImageProberTableCell iconCell = makeIconCell(openProcessor, true, key);
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
	private List<ICell> getCells(ImageStack imageStack, File imageFile) {
		
		IMutableDetectionOptions nucleusOptions = (IMutableDetectionOptions) options;
		
		double minSize = nucleusOptions.getMinSize();
		double maxSize = nucleusOptions.getMaxSize();
		double minCirc = nucleusOptions.getMinCirc();
		double maxCirc = nucleusOptions.getMaxCirc();
		boolean addBorder = nucleusOptions.getCannyOptions().isAddBorder();
		
		finer("Widening detection parameters");

		nucleusOptions.setMinSize(50);
		nucleusOptions.setMaxSize(imageStack.getWidth()*imageStack.getHeight());
		nucleusOptions.setMinCirc(0);
		nucleusOptions.setMaxCirc(1);
		nucleusOptions.getCannyOptions().setAddBorder(false);
		
		finer("Finding cells");
		
		NucleusDetector finder = new NucleusDetector(nucleusOptions, NucleusType.ROUND, 0.05);
		
		List<ICell> cells = finder.getDummyCells(imageStack, imageFile);
		
		finer("Resetting detetion parameters");
		
		nucleusOptions.setMinSize(minSize);
		nucleusOptions.setMaxSize(maxSize);
		nucleusOptions.setMinCirc(minCirc);
		nucleusOptions.setMaxCirc(maxCirc);
		nucleusOptions.getCannyOptions().setAddBorder(addBorder);
		return cells;
	}
	
	/**
	 * Draw the outline of a nucleus on the given processor
	 * @param cell
	 * @param ip
	 */
	private void drawNucleus(ICell cell, ImageProcessor ip) {
		if(cell==null){
			throw new IllegalArgumentException("Input cell is null");
		}
		
		
		Nucleus n = cell.getNucleus();
		// annotate the image processor with the nucleus outline
		
		Color colour = options.isValid(n) ? Color.ORANGE : Color.RED;

		ip = new NucleusAnnotator(ip).annotateBorder(n, colour).toProcessor();

	}	
}
