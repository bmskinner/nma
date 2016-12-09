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
import java.util.ArrayList;
import java.util.List;

import utility.Constants;
import utility.StatsMap;
import analysis.IAnalysisOptions;
import analysis.ICannyOptions;
import analysis.IDetectionOptions;
import analysis.detection.Detector;
import analysis.image.ImageConverter;
import analysis.image.ImageFilterer;
import components.DefaultCell;
import components.ICell;
import components.generic.IPoint;
import components.nuclei.Nucleus;
import components.nuclei.NucleusFactory;
import components.nuclei.NucleusFactory.NucleusCreationException;


/**
 * This is based on the Detector. It takes images, and runs the appropriate nucleus detection 
 * filters on them
 *
 */
public class NucleusDetector extends Detector {
	
	private final IAnalysisOptions options;
	private final String outputFolderName;
	
	private final NucleusFactory factory = new NucleusFactory();
	
	public NucleusDetector(final IAnalysisOptions options, final String outputFolderName){
		
		if(options==null){
			throw new IllegalArgumentException("Options is null");
		}

		this.options = options;
		this.outputFolderName = outputFolderName;
		
		IDetectionOptions nucleusOptions = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);
		
		setMaxSize(  nucleusOptions.getMaxSize());
		setMinCirc(  nucleusOptions.getMinCirc());
		setMaxCirc(  nucleusOptions.getMaxCirc());
		setThreshold(nucleusOptions.getThreshold());
	}
	
	/**
	 * Get a list of cells found in this image
	 * @param image the image
	 * @param sourceFile the file the nuclei were found in
	 * @return a list of cells detected in the image. Can be empty
	 */
	public List<ICell> getCells(ImageStack image, File sourceFile) {
		return createCellsFromImage(image, sourceFile, false);
	}
	
	/**
	 * Create cells with dummy components. The nucleus has a border list and
	 * stats, but profiling is not run, and the cell cannot be used for analysis. 
	 * This is designed to speed up the image prober.
	 * @param image
	 * @param sourceFile
	 * @return a list of cells detected. Can be empty.
	 */
	public List<ICell> getDummyCells(ImageStack image, File sourceFile) {
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
		
		IDetectionOptions nucleusOptions = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);

		if(closed==Detector.CLOSED_OBJECTS){
			setMinSize(nucleusOptions.getMinSize()); // get polygon rois
		} else {
			setMinSize(0); // get line rois
		}

		try{
			
			ImageProcessor ip = image.getProcessor(Constants.rgbToStack(nucleusOptions.getChannel()));
			roiList = detectRois(ip);
		
		} catch(Exception e){
			stack("Error in nucleus detection", e);
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
	private List<ICell> createCellsFromImage(ImageStack image, File path, boolean makeDummy) {

		fine("File:  "+path.getName());
		
		List<ICell> result = new ArrayList<ICell>(0);
						
		ImageStack searchStack;
		try {
			searchStack = preprocessImage(image);
		} catch (Exception e1) {
			stack("Error processing image", e1);
			warn("Unable to process image "+path.getAbsolutePath());
			return result;
		}

		// get polygon rois of correct size
		
		List<Roi> roiList = getROIs(searchStack, Detector.CLOSED_OBJECTS);
						
		if(roiList.isEmpty()){

			fine("No usable nuclei in image "+path.getAbsolutePath());
			
			return result;
		}

		finer("Image has "+roiList.size()+" ROIs");
		for(int i=0; i<roiList.size(); i++){
			
			Roi roi = roiList.get(i);

			finest( "Acquiring nucleus "+i+" in "+path.getAbsolutePath());

			ICell cell;
			try {
				cell = makeCell(roi, image, i, path, makeDummy); // get the profile data back for the nucleus
			}catch(NucleusCreationException e){
				stack("Cannot create nucleus from ROI "+i, e);
				continue;
			}
			result.add(cell);
			finer("Cell created");

		} 
		fine("Returning list of "+result.size()+" cells");
		return result;
	}
	
	/**
	 * Run the appropriate filters on the given image
	 * @param image
	 * @return
	 * @throws Exception
	 */
	private ImageStack preprocessImage(ImageStack image) {
		
		IDetectionOptions nucleusOptions = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);
		
		finer("Preprocessing image");
		ICannyOptions nucleusCannyOptions = nucleusOptions.getCannyOptions();

		ImageStack searchStack = null;
		
		

		if( nucleusCannyOptions.isUseCanny()) {
			
			if(nucleusCannyOptions.isAddBorder()){
				image = new ImageConverter(image)
					.addBorder(10)
					.toStack();

				finer("Added border");
			}

			// before passing to edge detection
			// run a Kuwahara filter to enhance edges in the image
			if(nucleusCannyOptions.isUseKuwahara()){
				int kernel = nucleusCannyOptions.getKuwaharaKernel();
				ImageProcessor ip = new ImageFilterer(image)
					.runKuwaharaFiltering( Constants.rgbToStack(nucleusOptions.getChannel())  , kernel)
					.toProcessor();
				image.setProcessor(ip, Constants.rgbToStack(nucleusOptions.getChannel()));
				finer("Run Kuwahara");
			}

			// flatten chromocentres
			if(nucleusCannyOptions.isUseFlattenImage()){
				int threshold = nucleusCannyOptions.getFlattenThreshold();
				ImageProcessor ip = new ImageFilterer(image)
					.squashChromocentres( Constants.rgbToStack(nucleusOptions.getChannel()), threshold)
					.toProcessor();
				image.setProcessor(ip, Constants.rgbToStack(nucleusOptions.getChannel()));
				finer("Run flattening");
			}
			searchStack = new ImageFilterer(image)
				.runEdgeDetector(Constants.rgbToStack(nucleusOptions.getChannel()), nucleusCannyOptions).toStack();
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
	 * @throws NucleusCreationException 
	  */
	private ICell makeCell(Roi roi, ImageStack image, int nucleusNumber, File path, boolean makeDummyCell) 
			throws NucleusCreationException{

		IDetectionOptions nucleusOptions = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);
		
		ICell result = null;
		
		  // measure the area, density etc within the nucleus
		ImageProcessor ip = image.getProcessor(Constants.rgbToStack(nucleusOptions.getChannel()));
		StatsMap values   = measure(roi, ip);

		  // save the position of the roi, for later use
		int xbase = (int) roi.getXBase();
		int ybase = (int) roi.getYBase();

		Rectangle bounds = roi.getBounds();

		int[] originalPosition = {xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };

		// create a Nucleus from the roi
		IPoint centreOfMass = IPoint.makeNew(values.get("XM"), values.get("YM"));
//
//		log("CoM is at "+centreOfMass.toString());
//		log("Roi: "+bounds.toString());

		Nucleus currentNucleus = factory.createNucleus(roi, 
				path, 
				nucleusOptions.getChannel(),
				nucleusNumber, 
				originalPosition, 
				options.getNucleusType(), 
				centreOfMass);

		// Move the nucleus xbase and ybase to 0,0 coordinates for charting
		IPoint offsetCoM = IPoint.makeNew( centreOfMass.getX() - xbase, centreOfMass.getY() - ybase  );

		fine("Offsetting CoM to point "+offsetCoM.toString());

		currentNucleus.moveCentreOfMass(offsetCoM);

		currentNucleus.setStatistic(NucleusStatistic.AREA,      values.get("Area"));
		currentNucleus.setStatistic(NucleusStatistic.MAX_FERET, values.get("Feret"));
		currentNucleus.setStatistic(NucleusStatistic.PERIMETER, values.get("Perim"));

		currentNucleus.setScale(nucleusOptions.getScale());

//		if ( ! makeDummyCell) {

			currentNucleus.initialise(options.getProfileWindowProportion());

			currentNucleus.findPointsAroundBorder();
//		}

		// if everything checks out, add the measured parameters to the global pool
		result = new DefaultCell(currentNucleus);
//		result.setNucleus(currentNucleus);		  


		return result;
	}  
	  
}
