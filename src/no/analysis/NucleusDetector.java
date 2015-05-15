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

import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import no.nuclei.*;
import no.utility.Logger;
import no.utility.StatsMap;
import no.collections.*;
import no.components.*;
import no.export.ImageExporter;
import no.gui.MainWindow;
import no.imports.ImageImporter;

public class NucleusDetector {

  protected static final String IMAGE_PREFIX = "export.";

  private static final String[] prefixesToIgnore = { IMAGE_PREFIX, "composite", "plot"};

  private static final String[] fileTypes = {".tif", ".tiff", ".jpg"};

  // counts of nuclei processed
  protected int totalNuclei        = 0;


  private File inputFolder;
  protected String outputFolder;
  protected File debugFile;

  protected AnalysisOptions analysisOptions;

  protected Logger logger;

  protected MainWindow mw;
  private Map<File, NucleusCollection> collectionGroup = new HashMap<File, NucleusCollection>();


  /**
  * Construct a detector on the given folder, and output the results to 
  * the given output folder
  *
  * @param inputFolder the folder to analyse
  * @param outputFolder the name of the folder for results
  */
  public NucleusDetector(String outputFolder, MainWindow mw, File debugFile, AnalysisOptions options){
	  this.inputFolder = options.getFolder();
	  this.outputFolder = outputFolder;
	  this.mw = mw;
	  this.debugFile = debugFile;
	  this.analysisOptions = options;
	  logger = new Logger(debugFile, "NucleusDetector");
  }


  /**
  * Run the detector on the input folder
  */
  public void runDetector(){
	  try{
		  logger.log("Running nucleus detector");
		  processFolder(this.inputFolder);
	  } catch(Exception e){
		  logger.log("Error in processing folder: "+e.getMessage(), Logger.ERROR);
		  for(StackTraceElement el : e.getStackTrace()){
			  logger.log(el.toString(), Logger.STACK);
		  }
	  }
  }

  /*
    -------------------
    Getters
    -------------------
  */

  /**
  * Get the image filetypes to analyse.
  *
  *  @return the array of filetypes
  */
  public String[] getFileTypes(){
    return NucleusDetector.fileTypes;
  }

  /**
  * Get the filename prefixes to ignore.
  * These prevent exports of previously analyses
  * being included in an analysis
  *
  *  @return the array of prefixes
  */
  public String[] getPrefixesToIgnore(){
    return NucleusDetector.prefixesToIgnore;
  }

  /**
  * Add a NucleusCollection to the group, using the source folder
  * name as a key.
  *
  *  @param file a folder to be analysed
  *  @param collection the collection of nuclei found
  */
  public void addNucleusCollection(File file, RoundNucleusCollection collection){
    this.collectionGroup.put(file, collection);
  }


  /**
  * Get the Map of NucleusCollections to the folder from
  * which they came. Any folders with no nuclei are removed
  * before returning.
  *
  *  @return a Map of a folder to its nuclei
  */
  public Map<File, NucleusCollection> getNucleiCollections(){
    // remove any empty collections before returning
    List<File> toRemove = new ArrayList<File>(0);
    Set<File> keys = collectionGroup.keySet();
    for (File key : keys) {
    	NucleusCollection collection = collectionGroup.get(key);
      if(collection.getNucleusCount()==0){
        toRemove.add(key);
      }    
    }

    Iterator<File> iter = toRemove.iterator();
    while(iter.hasNext()){
      collectionGroup.remove(iter.next());
    }
    return this.collectionGroup;
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
  protected boolean checkFile(File file){
    boolean ok = false;
    if (file.isFile()) {

      String fileName = file.getName();

      for( String fileType : fileTypes){
        if( fileName.endsWith(fileType) ){
          ok = true;
        }
      }

      for( String prefix : prefixesToIgnore){
        if(fileName.startsWith(prefix)){
          ok = false;
        }
      }
    }
    return ok;
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
//        mw.log("Failed to create directory: "+e);
        logger.log("Failed to create directory: "+e.getMessage(), Logger.ERROR);
      }
    }
    return output;
  }
  
  private NucleusCollection createNewCollection(File folder){

	  NucleusCollection newCollection = null;

	  try {

		  Constructor<?> collectionConstructor =  analysisOptions
				  .getCollectionClass()
				  .getConstructor(new Class<?>[]{File.class, String.class, String.class, File.class});

		  newCollection = (NucleusCollection) collectionConstructor.newInstance(folder, 
				  outputFolder, 
				  "analysable", 
				  this.debugFile);

	  } catch (NoSuchMethodException e) {
		  logger.log("Error creating collection: "+e.getMessage(), Logger.ERROR);
		  for(StackTraceElement el : e.getStackTrace()){
			  logger.log(el.toString(), Logger.STACK);
		  }
	  } catch (SecurityException e) {
		  logger.log("Error creating collection: "+e.getMessage(), Logger.ERROR);
		  for(StackTraceElement el : e.getStackTrace()){
			  logger.log(el.toString(), Logger.STACK);
		  }
	  } catch (InstantiationException e) {
		  logger.log("Error creating collection: "+e.getMessage(), Logger.ERROR);
		  for(StackTraceElement el : e.getStackTrace()){
			  logger.log(el.toString(), Logger.STACK);
		  }
	  } catch (IllegalAccessException e) {
		  logger.log("Error creating collection: "+e.getMessage(), Logger.ERROR);
		  for(StackTraceElement el : e.getStackTrace()){
			  logger.log(el.toString(), Logger.STACK);
		  }
	  } catch (IllegalArgumentException e) {
		  logger.log("Error creating collection: "+e.getMessage(), Logger.ERROR);
		  for(StackTraceElement el : e.getStackTrace()){
			  logger.log(el.toString(), Logger.STACK);
		  }
	  } catch (InvocationTargetException e) {
		  logger.log("Error creating collection: "+e.getMessage(), Logger.ERROR);
		  for(StackTraceElement el : e.getStackTrace()){
			  logger.log(el.toString(), Logger.STACK);
		  }
	  } catch (NullPointerException e) {
		  logger.log("Error creating collection: "+e.getMessage(), Logger.ERROR);
		  for(StackTraceElement el : e.getStackTrace()){
			  logger.log(el.toString(), Logger.STACK);
		  }
	  }
	  return newCollection;
  }
  
  private Nucleus createNucleus(Roi roi, File path, int nucleusNumber, double[] originalPosition){

	  Nucleus n = null;
//	  RoundNucleus currentNucleus = new RoundNucleus(nucleus, path, nucleusNumber, originalPosition);
//	  Class<double[]> arrayClass = double[].class;
	  
	  try {
		  
		  Constructor<?> nucleusConstructor = null;
		  
		  Constructor<?>[]  list = analysisOptions.getNucleusClass().getConstructors();
		  for(Constructor<?> c : list){
			  Class<?>[] classes = c.getParameterTypes();
//			  for(Class<?> cl : classes){
////				  IJ.log(cl.getSimpleName());
//			  }
			  if(classes.length==4){
				  nucleusConstructor = analysisOptions
				  .getNucleusClass()
				  .getConstructor(classes);
			  }
//			  IJ.log("");
		  }



		  n = (Nucleus) nucleusConstructor.newInstance(roi, 
				  path, 
				  nucleusNumber, 
				  originalPosition);

	  } catch (NoSuchMethodException e) {
		  IJ.log(e.getMessage());
		  for(StackTraceElement el : e.getStackTrace()){
			  IJ.log(el.toString());
		  }
	  } catch (SecurityException e) {
		  IJ.log(e.getMessage());
		  for(StackTraceElement el : e.getStackTrace()){
			  IJ.log(el.toString());
		  }
	  } catch (InstantiationException e) {
		  IJ.log(e.getMessage());
		  for(StackTraceElement el : e.getStackTrace()){
			  IJ.log(el.toString());
		  }
	  } catch (IllegalAccessException e) {
		  IJ.log(e.getMessage());
		  for(StackTraceElement el : e.getStackTrace()){
			  IJ.log(el.toString());
		  }
	  } catch (IllegalArgumentException e) {
		  IJ.log(e.getMessage());
		  for(StackTraceElement el : e.getStackTrace()){
			  IJ.log(el.toString());
		  }
	  } catch (InvocationTargetException e) {
		  IJ.log(e.getMessage());
		  for(StackTraceElement el : e.getStackTrace()){
			  IJ.log(el.toString());
		  }
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
	  
	  NucleusCollection folderCollection = createNewCollection(folder);
//	  RoundNucleusCollection folderCollection = new RoundNucleusCollection(folder, this.outputFolder, folder.getName(), this.debugFile);
	  this.collectionGroup.put(folder, folderCollection);

	  for (File file : listOfFiles) {

		  boolean ok = this.checkFile(file);

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
	 * @return the Map linking an roi to its stats
	 */
	protected List<Roi> getROIs(ImageStack image){
		Detector detector = new Detector();
		detector.setMaxSize(analysisOptions.getMaxNucleusSize());
		detector.setMinSize(analysisOptions.getMinNucleusSize());
		detector.setMinCirc(analysisOptions.getMinNucleusCirc());
		detector.setMaxCirc(analysisOptions.getMaxNucleusCirc());
		detector.setThreshold(analysisOptions.getNucleusThreshold());
		detector.setChannel(ImageImporter.COUNTERSTAIN);
		try{
			detector.run(image);
		} catch(Exception e){
			logger.log("Error in nucleus detection: "+e.getMessage(), Logger.ERROR);
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
		
		List<Roi> roiList = getROIs(image);
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
	  detector.setChannel(ImageImporter.COUNTERSTAIN);
	  StatsMap values = detector.measure(nucleus, image);

	  // save the position of the roi, for later use
	  double xbase = nucleus.getXBase();
	  double ybase = nucleus.getYBase();

	  Rectangle bounds = nucleus.getBounds();
//	  double xCentre = xbase+(bounds.getWidth()/2);
//	  double yCentre = ybase+(bounds.getHeight()/2);
	  double[] originalPosition = {xbase, ybase, bounds.getWidth(), bounds.getHeight() };
//	  String position = xCentre+"-"+yCentre; // store the centre of the rectangle for remapping

	  try{
	  	// Enlarge the ROI, so we can do nucleus detection on the resulting original images
		  ImageStack smallRegion = getRoiAsStack(nucleus, image);
		  Roi enlargedRoi = RoiEnlarger.enlarge(nucleus, 20);
		  ImageStack largeRegion = getRoiAsStack(enlargedRoi, image);
	
		  nucleus.setLocation(0,0); // translate the roi to the new image coordinates
		  
		  // turn roi into Nucleus for manipulation
		  Nucleus currentNucleus = createNucleus(nucleus, path, nucleusNumber, originalPosition);
		  
//		  RoundNucleus currentNucleus = new RoundNucleus(nucleus, path, nucleusNumber, originalPosition);
	
		  currentNucleus.setCentreOfMass(new XYPoint(values.get("XM")-xbase, values.get("YM")-ybase)); // need to offset
		  currentNucleus.setArea(values.get("Area")); 
		  currentNucleus.setFeret(values.get("Feret"));
		  currentNucleus.setPerimeter(values.get("Perim"));
	
		  currentNucleus.setOutputFolder(this.outputFolder);
		  currentNucleus.intitialiseNucleus(analysisOptions.getAngleProfileWindowSize());
		  
		  // save out the image stacks rather than hold within the nucleus
		  try{
			  IJ.saveAsTiff(ImageExporter.convert(smallRegion), currentNucleus.getOriginalImagePath());
			  IJ.saveAsTiff(ImageExporter.convert(largeRegion), currentNucleus.getEnlargedImagePath());
			  IJ.saveAsTiff(ImageExporter.convert(smallRegion), currentNucleus.getAnnotatedImagePath());
			  

		  } catch(Exception e){
			  mw.log("Error saving original, enlarged or annotated image: "+e.getMessage());
			  logger.log("Error saving original, enlarged or annotated image: "+e.getMessage(), Logger.ERROR);
		  }
		  
		  SignalDetector signalDetector = new SignalDetector(analysisOptions.getSignalThreshold(), 
				  												analysisOptions.getMinSignalSize(), 
				  												analysisOptions.getMaxSignalFraction());
		  signalDetector.run(currentNucleus, smallRegion);
		  
		  currentNucleus.findPointsAroundBorder();
	
		  // if everything checks out, add the measured parameters to the global pool
		  NucleusCollection collectionToAddTo = collectionGroup.get( new File(currentNucleus.getDirectory()));
		  collectionToAddTo.addNucleus(currentNucleus);
	  }catch(Exception e){
		  logger.log(" Error in nucleus assignment: "+e.getMessage(), Logger.ERROR);
		  mw.log("    Error in nucleus assignment: "+e.getMessage());
		  for(StackTraceElement element : e.getStackTrace()){
			  mw.log("    "+element.toString());
		  }
	  }
  }
  
  
  /**
   * Given an roi and a stack, get a stack containing just the roi
   * @param roi
   * @param stack
 * @throws Exception 
   */
  private ImageStack getRoiAsStack(Roi roi, ImageStack stack) throws Exception{
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
	  for(int i=ImageImporter.COUNTERSTAIN; i<=stack.getSize();i++){ // ImageStack starts at 1
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