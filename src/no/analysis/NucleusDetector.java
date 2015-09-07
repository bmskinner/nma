/*
  -----------------------
  NUCLEUS DETECTOR
  -----------------------
  Contains the variables for opening
  folders and files and detecting nuclei
  within them
*/  
package no.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.io.Opener;
import ij.plugin.RoiEnlarger;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import cell.Cell;
import utility.CannyEdgeDetector;
import utility.Constants;
import utility.Logger;
import utility.Stats;
import utility.StatsMap;
import mmorpho.MorphoProcessor;
import mmorpho.StructureElement;
import no.nuclei.*;
import no.collections.*;
import no.components.*;
import no.components.AnalysisOptions.CannyOptions;
import no.export.CompositeExporter;
import no.export.ImageExporter;
import no.gui.MainWindow;
import no.imports.ImageImporter;

public class NucleusDetector extends SwingWorker<Boolean, Integer> {
  
  private static final String spacerString = "---------";

  // counts of nuclei processed
  protected int totalNuclei        = 0;
  
  protected int totalImages;
  
  private int progress;
  
  private boolean debug = false;


  private File inputFolder;
  protected String outputFolder;
  protected File debugFile;

  protected AnalysisOptions analysisOptions;

  protected Logger logger;

  protected MainWindow mw;
  private Map<File, CellCollection> collectionGroup = new HashMap<File, CellCollection>();
  
  List<AnalysisDataset> datasets;


  /**
  * Construct a detector on the given folder, and output the results to 
  * the given output folder
  *
  * @param inputFolder the folder to analyse
  * @param outputFolder the name of the folder for results
  */
  public NucleusDetector(String outputFolder, MainWindow mw, File debugFile, AnalysisOptions options){
	  this.inputFolder 		= options.getFolder();
	  this.outputFolder 	= outputFolder;
	  this.mw 				= mw;
	  this.debugFile 		= debugFile;
	  this.analysisOptions 	= options;
	  
	  logger = new Logger(debugFile, "NucleusDetector");
  }

  
  @Override
	protected void process( List<Integer> integers ) {
		//update the number of entries added
		int amount = integers.get( integers.size() - 1 );
		int percent = (int) ( (double) amount / (double) totalImages * 100);
		setProgress(percent); // the integer representation of the percent
	}
	
	@Override
	protected Boolean doInBackground() throws Exception {

		boolean result = false;
		progress = 0;
		
		this.totalImages = NucleusDetector.countSuitableImages(analysisOptions.getFolder());
		try{
			  logger.log("Running nucleus detector");
			  processFolder(this.inputFolder);
			  
			  if(debug){
				  logger.log("Folder processed", Logger.DEBUG);
			  }
			  firePropertyChange("Cooldown", getProgress(), Constants.Progress.COOLDOWN.code());
			  
			  if(debug){
				  logger.log("Getting collections", Logger.DEBUG);
			  }
			  List<CellCollection> folderCollection = this.getNucleiCollections();

			  // Run the analysis pipeline
			  if(debug){
				  logger.log("Analysing collections", Logger.DEBUG);
			  }
			  datasets = analysePopulations(folderCollection);		
			 
			  result = true;
			  
			  
		  } catch(Exception e){
			  result = false;
			  logger.error("Error in processing folder", e);
		  }
		return result;
	}
	
	@Override
	public void done() {

		try {
			if(this.get()){
				firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());			
				
			} else {
				firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
			}
		} catch (InterruptedException e) {
			logger.error("Error in nucleus detection", e);

		} catch (ExecutionException e) {
			logger.error("Error in nucleus detection", e);
		}

	} 
	
	
	public List<AnalysisDataset> getDatasets(){
		return this.datasets;
	}
	
	public List<AnalysisDataset> analysePopulations(List<CellCollection> folderCollection){
		mw.log("Beginning analysis");
		 if(debug){
			 logger.log("Beginning analysis", Logger.DEBUG);
		 }
		List<AnalysisDataset> result = new ArrayList<AnalysisDataset>();

		for(CellCollection r : folderCollection){
			

			AnalysisDataset dataset = new AnalysisDataset(r);
			dataset.setAnalysisOptions(analysisOptions);
			dataset.setRoot(true);

			File folder = r.getFolder();
			logger.log("Analysing: "+folder.getName());

			LinkedHashMap<String, Integer> nucleusCounts = new LinkedHashMap<String, Integer>();

			try{

				nucleusCounts.put("input", r.getNucleusCount());
				CellCollection failedNuclei = new CellCollection(folder, r.getOutputFolderName(), "failed", logger.getLogfile(), analysisOptions.getNucleusClass());

//				boolean ok;
				mw.logc("Filtering collection...");
				boolean ok = CollectionFilterer.run(r, failedNuclei); // put fails into failedNuclei, remove from r
				if(ok){
					mw.log("OK");
				} else {
					mw.log("Error");
				}

				if(failedNuclei.getNucleusCount()>0){
					mw.logc("Exporting failed nuclei...");
					ok = CompositeExporter.run(failedNuclei);
					if(ok){
						mw.log("OK");
					} else {
						mw.log("Error");
					}
					nucleusCounts.put("failed", failedNuclei.getNucleusCount());
				}
				
			} catch(Exception e){
				logger.log("Cannot create collection: "+e.getMessage(), Logger.ERROR);
			}

			 mw.log(spacerString);
			mw.log("Population: "+r.getType());
			mw.log("Population: "+r.getNucleusCount()+" nuclei");
			logger.log("Population: "+r.getType()+" : "+r.getNucleusCount()+" nuclei");
			mw.log(spacerString);

			result.add(dataset);

		}
		return result;
	}

  /**
  * Add a NucleusCollection to the group, using the source folder
  * name as a key.
  *
  *  @param file a folder to be analysed
  *  @param collection the collection of nuclei found
  */
  public void addNucleusCollection(File file, CellCollection collection){
    this.collectionGroup.put(file, collection);
  }


  /**
  * Get the Map of NucleusCollections to the folder from
  * which they came. Any folders with no nuclei are removed
  * before returning.
  *
  *  @return a Map of a folder to its nuclei
  */
  public List<CellCollection> getNucleiCollections(){
	  // remove any empty collections before returning
	  if(debug){
		  logger.log("Getting all collections", Logger.DEBUG);
	  }
	  List<File> toRemove = new ArrayList<File>(0);
	  
	  if(debug){
		  logger.log("Testing nucleus counts", Logger.DEBUG);
	  }
	  Set<File> keys = collectionGroup.keySet();
	  for (File key : keys) {
		  CellCollection collection = collectionGroup.get(key);
		  if(collection.size()==0){
			  logger.log("Removing collection "+key.toString(), Logger.DEBUG);
			  toRemove.add(key);
		  }    
	  }
	  if(debug){
		  logger.log("Got collections to remove", Logger.DEBUG);
	  }

	  Iterator<File> iter = toRemove.iterator();
	  while(iter.hasNext()){
		  collectionGroup.remove(iter.next());
	  }
	  if(debug){
		  logger.log("Removed collections", Logger.DEBUG);
	  }
	  List<CellCollection> result = new ArrayList<CellCollection>();
	  for(CellCollection c : collectionGroup.values()){
		  result.add(c);
	  }
	  return result;

  }

  /**
  *  Checks that the given file is suitable for analysis.
  *  Is the file an image. Also check if it is in the 'banned list'.
  *  These are prefixes that are attached to exported images
  *  at later stages of analysis. This prevents exported images
  *  from previous runs being analysed.
  *
  *  @param file the File to check
  *  @return a true or false of whether the file passed checks
  */
  protected static boolean checkFile(File file){
    boolean ok = false;
    if (file.isFile()) {

      String fileName = file.getName();

      for( String fileType : Constants.IMPORTABLE_FILE_TYPES){
        if( fileName.endsWith(fileType) ){
          ok = true;
        }
      }

      for( String prefix : Constants.PREFIXES_TO_IGNORE){
        if(fileName.startsWith(prefix)){
          ok = false;
        }
      }
    }
    return ok;
  }
  
  /**
   * Count the number of images in the given folder
   * that are suitable for analysis. Rcursive over 
   * subfolders.
   * @param folder the folder to count
   * @return the number of analysable image files
   */
  public static int countSuitableImages(File folder){

	  File[] listOfFiles = folder.listFiles();

	  int result = 0;

	  for (File file : listOfFiles) {

		  boolean ok = checkFile(file);

		  if(ok){
			  result++;

		  } else { 
			  if(file.isDirectory()){ // recurse over any sub folders
				  result += countSuitableImages(file);
			  }
		  } 
	  } 
	  return result;
  }

  /**
  * Create the output folder for the analysis if required
  *
  * @param folder the folder in which to create the analysis folder
  * @return a File containing the created folder
  */
  private File makeFolder(File folder){
    File output = new File(folder.getAbsolutePath()+File.separator+this.outputFolder);
    if(!output.exists()){
      try{
        output.mkdir();
      } catch(Exception e) {
        logger.error("Failed to create directory", e);
      }
    }
    return output;
  }
  
  /**
   * Create a Nucleus from an ROI.
 * @param roi the ROI
 * @param path the path to the image
 * @param nucleusNumber the number of the nucleus in the image
 * @param originalPosition the bounding box position of the nucleus
 * @return a new nucleus of the appropriate class
 */
private Nucleus createNucleus(Roi roi, File path, int nucleusNumber, double[] originalPosition){

	  Nucleus n = null;
	  try {
		  
		  Constructor<?> nucleusConstructor = null;
		  
		  Constructor<?>[]  list = analysisOptions.getNucleusClass().getConstructors();
		  for(Constructor<?> c : list){
			  Class<?>[] classes = c.getParameterTypes();

			  if(classes.length==4){
				  nucleusConstructor = analysisOptions
				  .getNucleusClass()
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
  * Go through the input folder. Check if each file is
  * suitable for analysis, and if so, call the analyser.
  *
  * @param folder the folder of images to be analysed
  */
  protected void processFolder(File folder){

	  File[] listOfFiles = folder.listFiles();
	  
	  CellCollection folderCollection = new CellCollection(folder, 
			  outputFolder, 
			  folder.getName(), 
			  this.debugFile,
			  analysisOptions.getNucleusClass());
	  
//	  CellCollection folderCollection = createNewCollection(folder);
//	  RoundNucleusCollection folderCollection = new RoundNucleusCollection(folder, this.outputFolder, folder.getName(), this.debugFile);
	  this.collectionGroup.put(folder, folderCollection);

	  for (File file : listOfFiles) {

		  boolean ok = checkFile(file);

		  if(ok){
			  try {
				  Opener localOpener = new Opener();
				  ImagePlus image = localOpener.openImage(file.getAbsolutePath());   

				  //          ImageStack imageStack = ImageImporter.convert(image);
				  ImageStack imageStack = ImageImporter.importImage(file, logger.getLogfile());

				  // put folder creation here so we don't make folders we won't use (e.g. empty directory analysed)
				  makeFolder(folder);
				  processImage(imageStack, file);
				  image.close();

			  } catch (Exception e) { // end try
				  logger.log("Error in image processing: "+e.getMessage(), Logger.ERROR);
			  } // end catch
			  
			  progress++;
			  publish(progress);
		  } else { // if !ok
			  if(file.isDirectory()){ // recurse over any sub folders
				  processFolder(file);
			  } 
		  } // end else if !ok
	  } // end for (File)
  } // end function

	/**
	 * Detects nuclei within the given image.
	 *
	 * @param image the ImagePlus to be analysed
	 * @param closed should the detector get only closed polygons, or open lines
	 * @return the Map linking an roi to its stats
	 */
	protected List<Roi> getROIs(ImageStack image, boolean closed){
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
	protected void processImage(ImageStack image, File path){

		mw.log("File:  "+path.getName());
		logger.log("File:  "+path.getName(), Logger.DEBUG);
		
		// here before running the thresholding, do an edge detection, then pass on
		ImageStack searchStack = null;
		if( this.analysisOptions.getCannyOptions("nucleus").isUseCanny()) {
			searchStack = runEdgeDetector(image);
		} else {
			searchStack = image;
		}

		// get polygon rois of correct size
		
		List<Roi> roiList = getROIs(searchStack, true);
						
		if(roiList.isEmpty()){
			mw.log("  No usable nuclei in image");
			logger.log("No usable nuclei in image", Logger.DEBUG);
		}

		int nucleusNumber = 0;

		for(Roi roi : roiList){

			mw.log("  Acquiring nucleus "+nucleusNumber);
			logger.log("Acquiring nucleus "+nucleusNumber, Logger.DEBUG);
			try{
				analyseNucleus(roi, image, nucleusNumber, path); // get the profile data back for the nucleus
				this.totalNuclei++;
			} catch(Exception e){
				mw.log("  Error acquiring nucleus: "+e.getMessage());
				logger.log("Error acquiring nucleus: "+e.getMessage(), Logger.ERROR);
			}
			nucleusNumber++;
		} 
	}
	
	
	/**
	 * Use Canny edge detection to produce an image with potential edges highlighted
	 * for the detector
	 * @param image the stack to process
	 * @return a stack with edges highlighted
	 */
	private ImageStack runEdgeDetector(ImageStack image){

		//		bi.show();
		ImageStack searchStack = null;
		try {
			// using canny detector
			CannyOptions nucleusCannyOptions = analysisOptions.getCannyOptions("nucleus");

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

			morphologyClose( bp);
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
	private void autoDetectCannyThresholds(CannyOptions nucleusCannyOptions, ImageStack image){
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
	private double getMedianIntensity(ImageStack image){
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
	
	private void morphologyClose(ImageProcessor ip){
		try {
			
			int shift=1;
			int radius = analysisOptions.getCannyOptions("nucleus").getClosingObjectRadius();
			int[] offset = {0,0};
			int eltype = 0; //circle
			logger.log("Closing objects with circle of radius "+radius, Logger.DEBUG);
			
			StructureElement se = new StructureElement(eltype,  shift,  radius, offset);
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


  /**
  * Save the region of the input image containing the nucleus
  * Create a Nucleus and add it to the collection
  *
  * @param nucleus the ROI within the image
  * @param image the ImagePlus containing the nucleus
  * @param nucleusNumber the count of the nuclei in the image
  * @param path the full path to the image
  */
  protected void analyseNucleus(Roi nucleus, ImageStack image, int nucleusNumber, File path){

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
		  ImageStack smallRegion = getRoiAsStack(nucleus, image);
		  Roi enlargedRoi = RoiEnlarger.enlarge(nucleus, 20);
		  ImageStack largeRegion = getRoiAsStack(enlargedRoi, image);
	
		  nucleus.setLocation(0,0); // translate the roi to the new image coordinates
		  
		  // turn roi into Nucleus for manipulation
		  Nucleus currentNucleus = createNucleus(nucleus, path, nucleusNumber, originalPosition);
		  
		  CellCollection collectionToAddTo = collectionGroup.get( new File(currentNucleus.getDirectory()));
		  
//		  RoundNucleus currentNucleus = new RoundNucleus(nucleus, path, nucleusNumber, originalPosition);
	
		  currentNucleus.setCentreOfMass(new XYPoint(values.get("XM")-xbase, values.get("YM")-ybase)); // need to offset
		  currentNucleus.setArea(values.get("Area")); 
		  currentNucleus.setFeret(values.get("Feret"));
		  currentNucleus.setPerimeter(values.get("Perim"));
		  currentNucleus.setScale(analysisOptions.getScale());
	
		  currentNucleus.setOutputFolder(this.outputFolder);
		  currentNucleus.intitialiseNucleus(analysisOptions.getAngleProfileWindowSize());
		  
		  // save out the image stacks rather than hold within the nucleus
		  try{
			  IJ.saveAsTiff(ImageExporter.convert(smallRegion), currentNucleus.getOriginalImagePath());
			  IJ.saveAsTiff(ImageExporter.convert(largeRegion), currentNucleus.getEnlargedImagePath());
			  IJ.saveAsTiff(ImageExporter.convert(smallRegion), currentNucleus.getAnnotatedImagePath());
		  } catch(Exception e){
			  logger.error("Error saving original, enlarged or annotated image",e);
		  }

		  
		  currentNucleus.findPointsAroundBorder();
	
		  // if everything checks out, add the measured parameters to the global pool
		  Cell c = new Cell();
		  c.setNucleus(currentNucleus);
		  if(debug){
			  logger.log("Adding cell");
		  }
		  collectionToAddTo.addCell(c);
		  
		  
	  }catch(Exception e){
		  logger.error(" Error in nucleus assignment", e);
		  mw.log("    Error in nucleus assignment: "+e.getMessage());
	  }
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