/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package analysis.nucleus;

import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import stats.NucleusStatistic;

import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import utility.Constants;
//import utility.Logger;


import utility.StatsMap;
import analysis.IAnalysisOptions;
import analysis.ICannyOptions;
import analysis.detection.Detector;
import analysis.image.ImageFilterer;
import components.ICell;
import components.active.DefaultCell;
import components.active.generic.FloatPoint;
import components.generic.IPoint;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;


/**
 * This is based on the Detector. It takes images, and runs the appropriate nucleus detection 
 * filters on them
 *
 */
public class NucleusDetector extends Detector {
	
	private final IAnalysisOptions options;
	private final String outputFolderName;
	
	public NucleusDetector(final IAnalysisOptions options, final String outputFolderName){
		
		if(options==null){
			throw new IllegalArgumentException("Options is null");
		}

		this.options = options;
		this.outputFolderName = outputFolderName;
		
		setMaxSize(  options.getMaxNucleusSize());
		setMinCirc(  options.getMinNucleusCirc());
		setMaxCirc(  options.getMaxNucleusCirc());
		setThreshold(options.getNucleusThreshold());
	}
	
	/**
	 * Get a list of cells found in this image
	 * @param image the image
	 * @param sourceFile the file the nuclei were found in
	 * @return
	 * @throws Exception 
	 */
	public List<ICell> getCells(ImageStack image, File sourceFile) throws Exception{
		return createCellsFromImage(image, sourceFile, false);
	}
	
	/**
	 * Create cells with dummy components. The nucleus has a border list and
	 * stats, but profiling is not run, and the cell cannot be used for analysis. 
	 * This is designed to speed up the image prober.
	 * @param image
	 * @param sourceFile
	 * @return
	 * @throws Exception
	 */
	public List<ICell> getDummyCells(ImageStack image, File sourceFile) throws Exception{
		return createCellsFromImage(image, sourceFile, true);
	}
	
		
	/*
	 * PROTECTED AND PRIVATE METHODS
	 * 
	 */

	
	/**
	 * Detects nuclei within the given image.
	 *
	 * @param image the ImagePlus to be analysed
	 * @param closed should the detector get only closed polygons, or open lines
	 * @return the detected ROIs
	 */
	private List<Roi> getROIs(ImageStack image, int closed){
		finer("Detecting ROIs");
		
		List<Roi> roiList = new ArrayList<Roi>();

		if(closed==Detector.CLOSED_OBJECTS){
			setMinSize(options.getMinNucleusSize()); // get polygon rois
		} else {
			setMinSize(0); // get line rois
		}

		try{
			
			ImageProcessor ip = image.getProcessor(Constants.rgbToStack(options.getChannel()));
			roiList = detectRois(ip);
		
		} catch(Exception e){
			error("Error in nucleus detection", e);
		}
		
		finer("Detected ROIs");
		return roiList;
	}

  /**
  * Call the nucleus detector on the given image.
  * For each nucleus, perform the analysis step
  *
  * @param image the ImagePlus to be analysed
  * @param path the full path of the image
 * @throws Exception 
  */
	private List<ICell> createCellsFromImage(ImageStack image, File path, boolean makeDummy) throws Exception{

		fine("File:  "+path.getName());
		
		List<ICell> result = new ArrayList<ICell>();
						
		ImageStack searchStack = preprocessImage(image);

		// get polygon rois of correct size
		
		List<Roi> roiList = getROIs(searchStack, Detector.CLOSED_OBJECTS);
						
		if(roiList.isEmpty()){

			fine("No usable nuclei in image");
			
			return result;
		}

		int nucleusNumber = 0;

		for(Roi roi : roiList){

			finest( "Acquiring nucleus "+nucleusNumber);

			ICell cell = null;
			try{	
				
				if(makeDummy){
					
				}
				
				cell = makeCell(roi, image, nucleusNumber++, path, makeDummy); // get the profile data back for the nucleus
				
				
				
			} catch(Exception e){

				error("Error acquiring nucleus", e);

			}

			if(cell!=null){
				result.add(cell);
				finer("Cell created");
			}
			
		} 
		return result;
	}
	
	/**
	 * Run the appropriate filters on the given image
	 * @param image
	 * @return
	 * @throws Exception
	 */
	private ImageStack preprocessImage(ImageStack image) throws Exception{
		
		finer("Preprocessing image");
		ICannyOptions nucleusCannyOptions = options.getCannyOptions("nucleus");

		ImageStack searchStack = null;

		if( nucleusCannyOptions.isUseCanny()) {

			// before passing to edge detection
			// run a Kuwahara filter to enhance edges in the image
			if(nucleusCannyOptions.isUseKuwahara()){
				int kernel = nucleusCannyOptions.getKuwaharaKernel();
				ImageProcessor ip = new ImageFilterer(image)
					.runKuwaharaFiltering( Constants.rgbToStack(options.getChannel())  , kernel)
					.toProcessor();
				image.setProcessor(ip, Constants.rgbToStack(options.getChannel()));
				finer("Run Kuwahara");
			}

			// flatten chromocentres
			if(nucleusCannyOptions.isUseFlattenImage()){
				int threshold = nucleusCannyOptions.getFlattenThreshold();
				ImageProcessor ip = new ImageFilterer(image)
					.squashChromocentres( Constants.rgbToStack(options.getChannel()), threshold)
					.toProcessor();
				image.setProcessor(ip, Constants.rgbToStack(options.getChannel()));
				finer("Run flattening");
			}
			searchStack = new ImageFilterer(image).runEdgeDetector(Constants.rgbToStack(options.getChannel()), nucleusCannyOptions).toStack();
			finer("Run edge detection");
		} else {
			searchStack = image;
		}
		return searchStack;
	}
	

	
	
	/**
	  * Save the region of the input image containing the nucleus
	  * Create a Nucleus from the Roi and add it to a new Cell 
	  *
	  * @param roi the ROI within the image
	  * @param image the ImagePlus containing the nucleus
	  * @param nucleusNumber the count of the nuclei in the image
	  * @param path the full path to the image
	  * @param makeDummyCell should the cell be profiled, or a placeholder
	  */
	private ICell makeCell(Roi roi, ImageStack image, int nucleusNumber, File path, boolean makeDummyCell){

		ICell result = null;
		
		  // measure the area, density etc within the nucleus
		ImageProcessor ip = image.getProcessor(Constants.rgbToStack(options.getChannel()));
		StatsMap values   = measure(roi, ip);

		  // save the position of the roi, for later use
		int xbase = (int) roi.getXBase();
		int ybase = (int) roi.getYBase();

		  Rectangle bounds = roi.getBounds();

		  int[] originalPosition = {xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };

		  try{
		
			  roi.setLocation(0,0); // translate the roi to the new image coordinates
			  
			  // create a Nucleus from the roi
			  IPoint centreOfMass = IPoint.makeNew(values.get("XM")-xbase, values.get("YM")-ybase);

			  Nucleus currentNucleus = createNucleus(roi, 
					  path, 
					  options.getChannel(),
					  nucleusNumber, 
					  originalPosition, 
					  options.getNucleusType(), 
					  centreOfMass);

			  currentNucleus.setStatistic(NucleusStatistic.AREA,      values.get("Area"));
			  currentNucleus.setStatistic(NucleusStatistic.MAX_FERET, values.get("Feret"));
			  currentNucleus.setStatistic(NucleusStatistic.PERIMETER, values.get("Perim"));

			  currentNucleus.setScale(options.getScale());

			  if ( !makeDummyCell) {

//				  currentNucleus.setOutputFolder(outputFolderName);
				  currentNucleus.initialise(options.getAngleWindowProportion());

				  currentNucleus.findPointsAroundBorder();
			  }
		
			  // if everything checks out, add the measured parameters to the global pool
			  result = new DefaultCell();
			  result.setNucleus(currentNucleus);		  
			  
		  }catch(Exception e){
	
			  error(" Error in nucleus assignment", e);
			  
		  }
		  return result;
	  }
	
	/**
	 * Create a Nucleus from an ROI. Fetches the appropriate constructor based
	 * on the nucleus type
	 * @param roi the ROI
	 * @param path the path to the image
	 * @param nucleusNumber the number of the nucleus in the image
	 * @param originalPosition the bounding box position of the nucleus
	 * @param nucleusType the class of nucleus
	 * @return a new nucleus of the appropriate class
	 */
	private Nucleus createNucleus(Roi roi, File path, int channel, int nucleusNumber, int[] originalPosition, NucleusType nucleusType, IPoint centreOfMass){

		  Nucleus n = null;
		  try {

			  // The classes for the constructor
			  Class<?>[] classes = {Roi.class, File.class, int.class, int[].class, int.class, IPoint.class };
			  
			  Constructor<?> nucleusConstructor = nucleusType.getNucleusClass()
					  .getConstructor(classes);
			  

			  n = (Nucleus) nucleusConstructor.newInstance(roi, 
					  path, 
					  channel, 
					  originalPosition,
					  nucleusNumber,
					  centreOfMass);
			  
		  } catch(Exception e){

			  error( "Error creating nucleus", e);
			  
		  }
		  return n;
	  }
	  
	  
}
