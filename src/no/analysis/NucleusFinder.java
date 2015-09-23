package no.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.RoiEnlarger;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import cell.Cell;
import mmorpho.MorphoProcessor;
import mmorpho.StructureElement;

import no.components.AnalysisOptions;
import no.components.XYPoint;
import no.components.AnalysisOptions.CannyOptions;
import no.export.ImageExporter;
import no.imports.ImageImporter;
import no.nuclei.Nucleus;
import utility.CannyEdgeDetector;
import utility.Constants;
import utility.Kuwahara_Filter;
import utility.Logger;
import utility.Stats;
import utility.StatsMap;


/**
 * This takes images, and runs the appropriate nucleus detection 
 * filters on them 
 *
 */
public class NucleusFinder {
	
	private static Logger logger;
	
	/**
	 * Get a list of cells found in this image
	 * @param image the image
	 * @param options the detection options
	 * @param logfile the debug file
	 * @param sourceFile the file the nuclei were found in
	 * @return
	 */
	public static List<Cell> getCells(ImageStack image, AnalysisOptions options, File logfile, File sourceFile, String outputFolderName){
		logger = new Logger(logfile, "NucleusFinder");
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
			logger.error("Error in nucleus detection", e);
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
		logger.log("File:  "+path.getName(), Logger.DEBUG);
		List<Cell> result = new ArrayList<Cell>();
				
		CannyOptions nucleusCannyOptions = analysisOptions.getCannyOptions("nucleus");
		
		// here before running the thresholding, do an edge detection, then pass on
		ImageStack searchStack = null;
		
		if( nucleusCannyOptions.isUseCanny()) {
			
			// before passing to edge detection
			// run a Kuwahara filter to enhance edges in the image
			if(nucleusCannyOptions.isUseKuwahara()){
				int kernel = nucleusCannyOptions.getKuwaharaKernel();
				runKuwaharaFiltering(image, kernel);
			}
			
			// flatten chromocentres
			if(nucleusCannyOptions.isUseFlattenImage()){
				int threshold = nucleusCannyOptions.getFlattenThreshold();
				squashChromocentres(image, threshold);
			}
			searchStack = runEdgeDetector(image, nucleusCannyOptions);
		} else {
			searchStack = image;
		}

		// get polygon rois of correct size
		
		List<Roi> roiList = getROIs(searchStack, analysisOptions, true);
						
		if(roiList.isEmpty()){
			logger.log("No usable nuclei in image", Logger.DEBUG);
		}

		int nucleusNumber = 0;

		for(Roi roi : roiList){

			logger.log("Acquiring nucleus "+nucleusNumber, Logger.DEBUG);
			try{
				Cell cell = makeCell(roi, image, nucleusNumber, path, analysisOptions, outputFolderName); // get the profile data back for the nucleus
				result.add(cell);
			} catch(Exception e){
				logger.error("Error acquiring nucleus", e);
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
			  Nucleus currentNucleus = createNucleus(nucleus, path, nucleusNumber, originalPosition, analysisOptions.getNucleusClass());
			  		
			  currentNucleus.setCentreOfMass(new XYPoint(values.get("XM")-xbase, values.get("YM")-ybase)); // need to offset
			  currentNucleus.setArea(values.get("Area")); 
			  currentNucleus.setFeret(values.get("Feret"));
			  currentNucleus.setPerimeter(values.get("Perim"));
			  currentNucleus.setScale(analysisOptions.getScale());
		
			  currentNucleus.setOutputFolder(outputFolderName);
			  currentNucleus.intitialiseNucleus(analysisOptions.getAngleProfileWindowSize());
			  
//			  // save out the image stacks rather than hold within the nucleus
//			  try{
//				  IJ.saveAsTiff(ImageExporter.convert(smallRegion), currentNucleus.getOriginalImagePath());
//				  IJ.saveAsTiff(ImageExporter.convert(largeRegion), currentNucleus.getEnlargedImagePath());
//				  IJ.saveAsTiff(ImageExporter.convert(smallRegion), currentNucleus.getAnnotatedImagePath());
//			  } catch(Exception e){
//				  logger.error("Error saving original, enlarged or annotated image", e);
//			  }

			  
			  currentNucleus.findPointsAroundBorder();
		
			  // if everything checks out, add the measured parameters to the global pool
			  result = new Cell();
			  result.setNucleus(currentNucleus);		  
			  
		  }catch(Exception e){
			  logger.error(" Error in nucleus assignment", e);
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
	private static Nucleus createNucleus(Roi roi, File path, int nucleusNumber, double[] originalPosition, Class<?> nucleusClass){

		  Nucleus n = null;
		  try {
			  
			  Constructor<?> nucleusConstructor = null;
			  
			  Constructor<?>[]  list = nucleusClass.getConstructors();
			  for(Constructor<?> c : list){
				  Class<?>[] classes = c.getParameterTypes();

				  if(classes.length==4){
					  nucleusConstructor = nucleusClass
					  .getConstructor(classes);
				  }
			  }

			  n = (Nucleus) nucleusConstructor.newInstance(roi, 
					  path, 
					  nucleusNumber, 
					  originalPosition);
			  
		  } catch(Exception e){
			  logger.error("Error creating nucleus", e);

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
	
	/**
	 * Run a Kuwahara filter to enhance edges in the image
	 * @param stack the image
	 * @param filterSize the radius of the kernel
	 */
	private static void runKuwaharaFiltering(ImageStack stack, int filterSize){
		logger.log("Applying Kuwahara filter with radius "+filterSize);
		Kuwahara_Filter kw = new Kuwahara_Filter();
		ImagePlus img = ImageExporter.convert(stack);
		kw.setup("", img);
		
		kw.filter(stack.getProcessor(Constants.COUNTERSTAIN), filterSize);
	}
	
	/**
	 * The chromocentre can cause 'skipping' of the edge detection
	 * from the edge to the interior of the nucleus. Make any pixel
	 * over threshold equal threshold to remove internal structures
	 * @param stack the stack to adjust
	 * @return
	 */
	private static void squashChromocentres(ImageStack stack, int threshold){		
		logger.log("Compressing internal structures to max intensity of "+threshold);
		// fetch a copy of the int array
		ImageProcessor ip = stack.getProcessor(Constants.COUNTERSTAIN);
		int[][] array = ip.getIntArray();
		
		// threshold
		for(int x = 0; x<ip.getWidth(); x++){
			for( int y=0; y<ip.getHeight(); y++){
				if(array[x][y] > threshold ){
					array[x][y] = threshold;
				}
			}
		}
		
		ip.setIntArray(array);
	}
	
	
	/**
	 * Use Canny edge detection to produce an image with potential edges highlighted
	 * for the detector
	 * @param image the stack to process
	 * @return a stack with edges highlighted
	 */
	private static ImageStack runEdgeDetector(ImageStack image, CannyOptions nucleusCannyOptions){

		//		bi.show();
		ImageStack searchStack = null;
		try {
			// using canny detector
//			CannyOptions nucleusCannyOptions = analysisOptions.getCannyOptions("nucleus");

//			// calculation of auto threshold
			if(nucleusCannyOptions.isCannyAutoThreshold()){
				autoDetectCannyThresholds(nucleusCannyOptions, image);
			}

			logger.log("Creating edge detector", Logger.DEBUG);
			CannyEdgeDetector canny = new CannyEdgeDetector();
			canny.setSourceImage(image.getProcessor(Constants.COUNTERSTAIN).getBufferedImage());
			canny.setLowThreshold( nucleusCannyOptions.getLowThreshold() );
			canny.setHighThreshold( nucleusCannyOptions.getHighThreshold());
			canny.setGaussianKernelRadius(nucleusCannyOptions.getKernelRadius());
			canny.setGaussianKernelWidth(nucleusCannyOptions.getKernelWidth());

			canny.process();
			BufferedImage edges = canny.getEdgesImage();
			ImagePlus searchImage = new ImagePlus(null, edges);


			// add morphological closing
			ByteProcessor bp = searchImage.getProcessor().convertToByteProcessor();

			morphologyClose( bp  , nucleusCannyOptions.getClosingObjectRadius()) ;
			ImagePlus bi= new ImagePlus(null, bp);
			searchStack = ImageImporter.convert(bi);

			bi.close();
			searchImage.close();

			logger.log("Edge detection complete", Logger.DEBUG);
		} catch (Exception e) {
			logger.error("Error in edge detection", e);
		}
		return searchStack;
	}
	
	/**
	 * Try to detect the optimal settings for the edge detector based on the 
	 * median image pixel intensity.
	 * @param nucleusCannyOptions the options
	 * @param image the image to analyse
	 */
	private static void autoDetectCannyThresholds(CannyOptions nucleusCannyOptions, ImageStack image){
		// calculation of auto threshold

		// find the median intensity of the image
		double medianPixel = getMedianIntensity(image);

		// if the median is >128, this is probably an inverted image.
		// invert it so the thresholds will work
		if(medianPixel>128){
			logger.log("Detected high median ("+medianPixel+"); inverting");
			image.getProcessor(Constants.COUNTERSTAIN).invert();
			medianPixel = getMedianIntensity(image);
		}

		// set the thresholds either side of the median
		double sigma = 0.33; // default value - TODO: enable change
		double lower = Math.max(0  , (1.0 - (2.5 * sigma)  ) * medianPixel  ) ;
		lower = lower < 0.1 ? 0.1 : lower; // hard limit
		double upper = Math.min(255, (1.0 + (0.6 * sigma)  ) * medianPixel  ) ;
		upper = upper < 0.3 ? 0.3 : upper; // hard limit
		nucleusCannyOptions.setLowThreshold(  (float)  lower);
		nucleusCannyOptions.setHighThreshold( (float)  upper);
		logger.log("Auto thresholding: low: "+lower+"  high: "+upper, Logger.DEBUG);

	}
	
	/**
	 * Get the median pixel intensity in the image. Used in auto-selection
	 * of Canny thresholds.
	 * @param image the image to process
	 * @return the median pixel intensity
	 */
	private static double getMedianIntensity(ImageStack image){
		ImageProcessor median = image.getProcessor(Constants.COUNTERSTAIN);
		double[] values = new double[ median.getWidth()*median.getHeight() ];
		try {
			int i=0;
			for(int w = 0; w<median.getWidth();w++){
				for(int h = 0; h<median.getHeight();h++){
					values[i] = (double) median.get(w, h);

					i++;
				}
			}
		} catch (Exception e) {
			logger.error("Error getting median image intensity", e);
		}
		return Stats.quartile(values, 50);
	}
	
	/**
	 * Close holes in the nuclear borders
	 * @param ip the image processor
	 * @param closingRadius the radius of the circle
	 */
	private static void morphologyClose(ImageProcessor ip, int closingRadius){
		try {
			
			int shift=1;
//			int radius = analysisOptions.getCannyOptions("nucleus").getClosingObjectRadius();
			int[] offset = {0,0};
			int eltype = 0; //circle
			logger.log("Closing objects with circle of radius "+closingRadius, Logger.DEBUG);
			
			StructureElement se = new StructureElement(eltype,  shift,  closingRadius, offset);
//			IJ.log("Made se");
			MorphoProcessor mp = new MorphoProcessor(se);
//			IJ.log("Made mp");
			mp.fclose(ip);
			logger.log("Objects closed", Logger.DEBUG);
//			IJ.log("Closed");
		} catch (Exception e) {
			logger.error("Error in morphology closing", e);
		}
		
	}

}
