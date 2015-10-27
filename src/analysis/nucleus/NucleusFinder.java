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

import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class NucleusFinder {
	
	private static Logger fileLogger;
	private static Logger programLogger;
	
	/**
	 * Get a list of cells found in this image
	 * @param image the image
	 * @param options the detection options
	 * @param logfile the debug file
	 * @param sourceFile the file the nuclei were found in
	 * @return
	 */
	public static List<Cell> getCells(ImageStack image, AnalysisOptions options, Logger programLogger, File sourceFile, String outputFolderName){
//		logger = new Logger(logfile, "NucleusFinder");
		NucleusFinder.programLogger = programLogger;
		NucleusFinder.fileLogger = null;
		List<Cell> result = processImage(image, sourceFile, options, outputFolderName);
		return result;
	}
	
	public static List<Cell> getCells(ImageStack image, AnalysisOptions options, Logger programLogger, Logger fileLogger, File sourceFile, String outputFolderName){
		NucleusFinder.programLogger = programLogger;
		NucleusFinder.fileLogger = fileLogger;
		List<Cell> result = processImage(image, sourceFile, options, outputFolderName);
		return result;
	}
	
	/**
	 * Detects nuclei within the given image.
	 *
	 * @param image the ImagePlus to be analysed
	 * @param analysisOptions the detection parameters
	 * @param closed should the detector get only closed polygons, or open lines
	 * @return the Map linking an roi to its stats
	 */
	protected static List<Roi> getROIs(ImageStack image, AnalysisOptions analysisOptions, boolean closed){
		Detector detector = new Detector();
		detector.setMaxSize(analysisOptions.getMaxNucleusSize());
		
		if(closed){
			detector.setMinSize(analysisOptions.getMinNucleusSize()); // get polygon rois
		} else {
			detector.setMinSize(0); // get line rois
		}
		detector.setMinCirc(analysisOptions.getMinNucleusCirc());
		detector.setMaxCirc(analysisOptions.getMaxNucleusCirc());
		detector.setThreshold(analysisOptions.getNucleusThreshold());
		detector.setStackNumber(Constants.COUNTERSTAIN);
		try{
			detector.run(image);
		} catch(Exception e){
			programLogger.log(Level.SEVERE, "Error in nucleus detection", e);
			fileLogger.log(Level.SEVERE, "Error in nucleus detection", e);
		}
		return detector.getRoiList();
	}

  /**
  * Call the nucleus detector on the given image.
  * For each nucleus, perform the analysis step
  *
  * @param image the ImagePlus to be analysed
  * @param path the full path of the image
  */
	protected static List<Cell> processImage(ImageStack image, File path, AnalysisOptions analysisOptions, String outputFolderName){

		if(analysisOptions==null){
			throw new IllegalArgumentException("Analysis options are null");
		}

		if(fileLogger!=null){
			fileLogger.log(Level.FINE, "File:  "+path.getName());
		}
		List<Cell> result = new ArrayList<Cell>();
				
		CannyOptions nucleusCannyOptions = analysisOptions.getCannyOptions("nucleus");
		
		// here before running the thresholding, do an edge detection, then pass on
		ImageStack searchStack = null;
		
		if( nucleusCannyOptions.isUseCanny()) {
			
			// before passing to edge detection
			// run a Kuwahara filter to enhance edges in the image
			if(nucleusCannyOptions.isUseKuwahara()){
				int kernel = nucleusCannyOptions.getKuwaharaKernel();
				ImageProcessor ip = ImageFilterer.runKuwaharaFiltering(image, Constants.COUNTERSTAIN, kernel);
				image.setProcessor(ip, Constants.COUNTERSTAIN);
//				runKuwaharaFiltering(image, kernel);
			}
			
			// flatten chromocentres
			if(nucleusCannyOptions.isUseFlattenImage()){
				int threshold = nucleusCannyOptions.getFlattenThreshold();
				ImageProcessor ip = ImageFilterer.squashChromocentres(image, Constants.COUNTERSTAIN, threshold);
				image.setProcessor(ip, Constants.COUNTERSTAIN);
//				squashChromocentres(image, threshold);
			}
			searchStack = ImageFilterer.runEdgeDetector(image, Constants.COUNTERSTAIN, nucleusCannyOptions);
		} else {
			searchStack = image;
		}

		// get polygon rois of correct size
		
		List<Roi> roiList = getROIs(searchStack, analysisOptions, true);
						
		if(roiList.isEmpty()){
			if(fileLogger!=null){
				fileLogger.log(Level.FINE, "No usable nuclei in image");
			} else {
				programLogger.log(Level.FINE, "No usable nuclei in image");
			}
			
		}

		int nucleusNumber = 0;

		for(Roi roi : roiList){
			if(fileLogger!=null){
				fileLogger.log(Level.FINE, "Acquiring nucleus "+nucleusNumber);
			} else {
				programLogger.log(Level.FINEST, "Acquiring nucleus "+nucleusNumber);
			}
			
			try{
				Cell cell = makeCell(roi, image, nucleusNumber, path, analysisOptions, outputFolderName); // get the profile data back for the nucleus
				result.add(cell);
			} catch(Exception e){
				if(fileLogger!=null){
					fileLogger.log(Level.SEVERE, "Error acquiring nucleus", e);
				} else {
					programLogger.log(Level.SEVERE, "Error acquiring nucleus", e);
				}
			}
			nucleusNumber++;
		} 
		return result;
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
	private static Cell makeCell(Roi nucleus, ImageStack image, int nucleusNumber, File path, AnalysisOptions analysisOptions, String outputFolderName){

		Cell result = null;
		  // measure the area, density etc within the nucleus
		  Detector detector = new Detector();
		  detector.setStackNumber(Constants.COUNTERSTAIN);
		  StatsMap values = detector.measure(nucleus, image);

		  // save the position of the roi, for later use
		  double xbase = nucleus.getXBase();
		  double ybase = nucleus.getYBase();

		  Rectangle bounds = nucleus.getBounds();

		  double[] originalPosition = {xbase, ybase, bounds.getWidth(), bounds.getHeight() };

		  try{
		  	// Enlarge the ROI, so we can do nucleus detection on the resulting original images
//			  ImageStack smallRegion = getRoiAsStack(nucleus, image);
//			  Roi enlargedRoi = RoiEnlarger.enlarge(nucleus, 20);
//			  ImageStack largeRegion = getRoiAsStack(enlargedRoi, image);
		
			  nucleus.setLocation(0,0); // translate the roi to the new image coordinates
			  
			  // turn roi into Nucleus for manipulation
			  Nucleus currentNucleus = createNucleus(nucleus, path, nucleusNumber, originalPosition, analysisOptions.getNucleusType());
			  		
			  currentNucleus.setCentreOfMass(new XYPoint(values.get("XM")-xbase, values.get("YM")-ybase)); // need to offset
			  currentNucleus.setArea(values.get("Area")); 
			  currentNucleus.setFeret(values.get("Feret"));
			  currentNucleus.setPerimeter(values.get("Perim"));
			  currentNucleus.setScale(analysisOptions.getScale());
		
			  currentNucleus.setOutputFolder(outputFolderName);
			  currentNucleus.intitialiseNucleus(analysisOptions.getAngleProfileWindowSize());
			  
			  currentNucleus.findPointsAroundBorder();
		
			  // if everything checks out, add the measured parameters to the global pool
			  result = new Cell();
			  result.setNucleus(currentNucleus);		  
			  
		  }catch(Exception e){
			  if(fileLogger!=null){
				  fileLogger.log(Level.SEVERE, " Error in nucleus assignment", e);
			  } else {
				  programLogger.log(Level.SEVERE, " Error in nucleus assignment", e);
			  }
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
	private static Nucleus createNucleus(Roi roi, File path, int nucleusNumber, double[] originalPosition, NucleusType nucleusType){

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
			  if(fileLogger!=null){
				  fileLogger.log(Level.SEVERE, "Error creating nucleus", e);
			  } else {
				  programLogger.log(Level.SEVERE, " Error creating nucleus", e);
			  }
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
