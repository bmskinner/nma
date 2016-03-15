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

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import logging.Loggable;
import stats.NucleusStatistic;

import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import utility.Constants;
//import utility.Logger;


import utility.StatsMap;
import analysis.AnalysisOptions;
import analysis.AnalysisOptions.CannyOptions;
import analysis.Detector;
import analysis.ImageFilterer;
import components.Cell;
import components.generic.XYPoint;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;


/**
 * This takes images, and runs the appropriate nucleus detection 
 * filters on them 
 *
 */
public class NucleusFinder implements Loggable {
	
	private final AnalysisOptions options;
	private final String outputFolderName;
	private final Detector detector;
	
	public NucleusFinder(final AnalysisOptions options, final String outputFolderName){
		if(options==null){
			throw new IllegalArgumentException("Options is null");
		}

		this.options = options;
		this.outputFolderName = outputFolderName;
		
		detector = new Detector();
		detector.setMaxSize(options.getMaxNucleusSize());
		detector.setMinCirc(  options.getMinNucleusCirc());
		detector.setMaxCirc(  options.getMaxNucleusCirc());
		detector.setThreshold(options.getNucleusThreshold());
	}
	
	/**
	 * Get a list of cells found in this image
	 * @param image the image
	 * @param options the detection options
	 * @param logfile the debug file
	 * @param sourceFile the file the nuclei were found in
	 * @return
	 * @throws Exception 
	 */
	public List<Cell> getCells(ImageStack image, File sourceFile) throws Exception{
		return processImage(image, sourceFile);
	}

	
	/**
	 * Detects nuclei within the given image.
	 *
	 * @param image the ImagePlus to be analysed
	 * @param analysisOptions the detection parameters
	 * @param closed should the detector get only closed polygons, or open lines
	 * @return the Map linking an roi to its stats
	 */
	protected List<Roi> getROIs(ImageStack image, boolean closed){
		log(Level.FINER, "Running Detector");

		if(closed){
			detector.setMinSize(options.getMinNucleusSize()); // get polygon rois
		} else {
			detector.setMinSize(0); // get line rois
		}

		try{
			ImageProcessor ip = image.getProcessor(Constants.rgbToStack(options.getChannel()));
			detector.run(ip);
		} catch(Exception e){
			logError( "Error in nucleus detection", e);
		}
		log(Level.FINER, "Finished Detector");
		return detector.getRoiList();
	}

  /**
  * Call the nucleus detector on the given image.
  * For each nucleus, perform the analysis step
  *
  * @param image the ImagePlus to be analysed
  * @param path the full path of the image
 * @throws Exception 
  */
	protected List<Cell> processImage(ImageStack image, File path) throws Exception{

		log(Level.FINE, "File:  "+path.getName());
		
		List<Cell> result = new ArrayList<Cell>();
						
		ImageStack searchStack = preprocessImage(image);

		// get polygon rois of correct size
		
		List<Roi> roiList = getROIs(searchStack, true);
						
		if(roiList.isEmpty()){

			log(Level.FINE, "No usable nuclei in image");
			
			return result;
		}

		int nucleusNumber = 0;

		for(Roi roi : roiList){

			log(Level.FINEST, "Acquiring nucleus "+nucleusNumber);
			
			
			try{
				Cell cell = makeCell(roi, image, nucleusNumber++, path); // get the profile data back for the nucleus
				result.add(cell);
				log(Level.FINER, "Cell created");
			} catch(Exception e){

				logError("Error acquiring nucleus", e);
				
			}
		} 
		return result;
	}
	
	/**
	 * Run the appropriate filters on the given image
	 * @param image
	 * @param analysisOptions
	 * @return
	 * @throws Exception
	 */
	private ImageStack preprocessImage(ImageStack image) throws Exception{
		
		log(Level.FINER, "Preprocessing image");
		CannyOptions nucleusCannyOptions = options.getCannyOptions("nucleus");

		ImageStack searchStack = null;

		if( nucleusCannyOptions.isUseCanny()) {

			// before passing to edge detection
			// run a Kuwahara filter to enhance edges in the image
			if(nucleusCannyOptions.isUseKuwahara()){
				int kernel = nucleusCannyOptions.getKuwaharaKernel();
				ImageProcessor ip = ImageFilterer.runKuwaharaFiltering(image, Constants.rgbToStack(options.getChannel())  , kernel);
				image.setProcessor(ip, Constants.rgbToStack(options.getChannel()));
				log(Level.FINER, "Run Kuwahara");
			}

			// flatten chromocentres
			if(nucleusCannyOptions.isUseFlattenImage()){
				int threshold = nucleusCannyOptions.getFlattenThreshold();
				ImageProcessor ip = ImageFilterer.squashChromocentres(image, Constants.rgbToStack(options.getChannel()), threshold);
				image.setProcessor(ip, Constants.rgbToStack(options.getChannel()));
				log(Level.FINER, "Run flattening");
			}
			searchStack = ImageFilterer.runEdgeDetector(image, Constants.rgbToStack(options.getChannel()), nucleusCannyOptions);
			log(Level.FINER, "Run edge detection");
		} else {
			searchStack = image;
		}
		return searchStack;
	}
	

	/**
	  * Save the region of the input image containing the nucleus
	  * Create a Nucleus and add it to the collection
	  *
	  * @param nucleus the ROI within the image
	  * @param image the ImagePlus containing the nucleus
	  * @param nucleusNumber the count of the nuclei in the image
	  * @param path the full path to the image
	  */
	private Cell makeCell(Roi nucleus, ImageStack image, int nucleusNumber, File path){

		Cell result = null;
		  // measure the area, density etc within the nucleus
//		  Detector detector = new Detector();
//		  detector.setStackNumber(Constants.rgbToStack(options.getChannel()));
		ImageProcessor ip = image.getProcessor(Constants.rgbToStack(options.getChannel()));
		  StatsMap values = detector.measure(nucleus, ip);

		  // save the position of the roi, for later use
		  double xbase = nucleus.getXBase();
		  double ybase = nucleus.getYBase();

		  Rectangle bounds = nucleus.getBounds();

		  double[] originalPosition = {xbase, ybase, bounds.getWidth(), bounds.getHeight() };

		  try{
		
			  nucleus.setLocation(0,0); // translate the roi to the new image coordinates
			  
			  // turn roi into Nucleus for manipulation
			  Nucleus currentNucleus = createNucleus(nucleus, path, nucleusNumber, originalPosition, options.getNucleusType());
			  		
			  currentNucleus.setCentreOfMass(new XYPoint(values.get("XM")-xbase, values.get("YM")-ybase)); // need to offset
			  
			  currentNucleus.setStatistic(NucleusStatistic.AREA,      values.get("Area"));
			  currentNucleus.setStatistic(NucleusStatistic.MAX_FERET, values.get("Feret"));
			  currentNucleus.setStatistic(NucleusStatistic.PERIMETER, values.get("Perim"));
			  currentNucleus.setChannel(options.getChannel());
			  
			  currentNucleus.setScale(options.getScale());
		
			  currentNucleus.setOutputFolder(outputFolderName);
			  currentNucleus.intitialiseNucleus(options.getAngleProfileWindowSize());
			  
			  currentNucleus.findPointsAroundBorder();
		
			  // if everything checks out, add the measured parameters to the global pool
			  result = new Cell();
			  result.setNucleus(currentNucleus);		  
			  
		  }catch(Exception e){
	
			  logError(" Error in nucleus assignment", e);
			  
		  }
		  return result;
	  }
	
	/**
	 * Create a Nucleus from an ROI.
	 * @param roi the ROI
	 * @param path the path to the image
	 * @param nucleusNumber the number of the nucleus in the image
	 * @param originalPosition the bounding box position of the nucleus
	 * @return a new nucleus of the appropriate class
	 */
	private Nucleus createNucleus(Roi roi, File path, int nucleusNumber, double[] originalPosition, NucleusType nucleusType){

		  Nucleus n = null;
		  try {
			  
			  Constructor<?> nucleusConstructor = null;
			  
			  Constructor<?>[]  list = nucleusType.getNucleusClass().getConstructors();
			  for(Constructor<?> c : list){
				  Class<?>[] classes = c.getParameterTypes();

				  if(classes.length==4){
					  nucleusConstructor = nucleusType.getNucleusClass()
					  .getConstructor(classes);
				  }
			  }

			  n = (Nucleus) nucleusConstructor.newInstance(roi, 
					  path, 
					  nucleusNumber, 
					  originalPosition);
			  
		  } catch(Exception e){

			  logError( "Error creating nucleus", e);
			  
		  }
		  return n;
	  }
	  
	  
	  /**
	   * Given an roi and a stack, get a stack containing just the roi
	   * @param roi
	   * @param stack
	 * @throws Exception 
	   */
	  public static ImageStack getRoiAsStack(Roi roi, ImageStack stack) throws Exception {
		  if(roi==null || stack == null){
			  throw new IllegalArgumentException("ROI or stack is null");
		  }
		  
		  int x = (int) roi.getXBase();
		  int y = (int) roi.getYBase();
		  int w = (int) roi.getBounds().getWidth();
		  int h = (int) roi.getBounds().getHeight();
		  
		// correct for enlarged ROIs that go offscreen
		  if(y<0){
			  h=h+y;
			  y=0;
		  }
		  if(y+h>=stack.getHeight()){
			  h=stack.getHeight()-y;
		  }	  
		  if(x<0){
			 w=w+y;
			 x=0;
		  }
		  if(x+w>=stack.getWidth()){
			  w=stack.getWidth()-x;
		  }	  
		  Roi rectangle = new Roi(x, y, w, h);
		  
		  ImageStack result = new ImageStack(w, h);
		  for(int i=Constants.COUNTERSTAIN; i<=stack.getSize();i++){ // ImageStack starts at 1
			  ImagePlus image = new ImagePlus(null, stack.getProcessor(i));
			  
			  image.setRoi(rectangle);
			  image.copy();
			  ImagePlus region = ImagePlus.getClipboard();
			  if(region.getWidth()!=w || region.getHeight()!=h){
				  throw new Exception("Size mismatch from ROI ("+region.getWidth()+","+region.getHeight()+") to stack ("+w+","+h+"). ROI at "+x+","+y);
			  }
			  result.addSlice(region.getProcessor());
		  }
		  return result;
	  }
}
