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
import java.util.*;

import no.nuclei.*;
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

  /* DEFAULT VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  protected double minNucleusSize  = 500;
  protected double maxNucleusSize  = 10000;
  protected double minNucleusCirc  = 0.4;
  protected double maxNucleusCirc  = 1;

  private int angleProfileWindowSize  = 23;

  protected int nucleusThreshold = 36;

  // counts of nuclei processed
  protected int totalNuclei        = 0;

  private  int    signalThreshold = 70;
  private  double   minSignalSize = 5;
  private  double   maxSignalFraction = 0.5;

	private File inputFolder;
  protected String outputFolder;
  
  protected MainWindow mw;
  private Map<File, NucleusCollection> collectionGroup = new HashMap<File, NucleusCollection>();


  /**
  * Construct a detector on the given folder, and output the results to 
  * the given output folder
  *
  * @param inputFolder the folder to analyse
  * @param outputFolder the name of the folder for results
  */
  public NucleusDetector(File inputFolder, String outputFolder, MainWindow mw){
	  this.inputFolder = inputFolder;
	  this.outputFolder = outputFolder;
	  this.mw = mw;
  }


  /**
  * Run the detector on the input folder
  */
  public void runDetector(){
    try{
      processFolder(this.inputFolder);
    } catch(Exception e){
      IJ.log("Error in processing folder: "+e);
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

  public int getThreshold(){
    return this.nucleusThreshold;
  }

	/*
    -------------------
    Settings for nucleus detection
    -------------------
  */

  /**
  * Set minimum size of a nucleus in pixels
  *
  *  @param d the minimum size
  */
  public void setMinNucleusSize(double d){
	  if(Double.valueOf(d)==null){
		  throw new IllegalArgumentException("Value is null");
	  }
	  if(d<=0){
		  throw new IllegalArgumentException("Min nucleus size is 0 or less");
	  }
	  this.minNucleusSize = d;
  } 

  /**
  * Set maximum size of a nucleus in pixels
  *
  *  @param d the maximum size
  */
  public void setMaxNucleusSize(double d){
	  if(Double.valueOf(d)==null){
		  throw new IllegalArgumentException("Value is null");
	  }
	  if(d<=0){
		  throw new IllegalArgumentException("Max nucleus size is 0 or less");
	  }
	  this.maxNucleusSize = d;
  }   

  /**
  * Set minimum circularity of a nucleus
  *
  *  @param d the minimum circularity
  */
  public void setMinNucleusCirc(double d){
	  if(Double.valueOf(d)==null){
		  throw new IllegalArgumentException("Value is null");
	  }
	  if(d<0 || d>1){
		  throw new IllegalArgumentException("Value is outside range 0-1");
	  }
	  this.minNucleusCirc = d;
  }

  /**
  * Set maximum circularity of a nucleus
  *
  *  @param d the maximum circularity
  */
  public void setMaxNucleusCirc(double d){
	  if(Double.valueOf(d)==null){
		  throw new IllegalArgumentException("Value is null");
	  }
	  if(d<0 || d>1){
		  throw new IllegalArgumentException("Value is outside range 0-1");
	  }
	  this.maxNucleusCirc = d;
  }

  /**
  * Set the image thresholding for detecting nuclei
  *
  *  @param i the threshold
  */
  public void setThreshold(int i){
	  if(Integer.valueOf(i)==null){
		  throw new IllegalArgumentException("Value is null");
	  }
	  if(i<0 || i>255){
		  throw new IllegalArgumentException("Value is outside range 0-255");
	  }
	  this.nucleusThreshold = i;
  } 

  /*
    -------------------
    Settings for signal detection
    -------------------
  */


  /**
  * Set the image thresholding for detecting signals
  *
  *  @param i the threshold
  */
  public void setSignalThreshold(int i){
    this.signalThreshold = i;
  }

  /**
  * Set minimum size of a signal in pixels
  *
  *  @param d the signal size
  */
  public void setMinSignalSize(double d){
    this.minSignalSize = d;
  }

  /**
  * Set maximum fraction of nuclear area a signal can take up
  *
  *  @param d the signal size
  */
  public void setMaxSignalFraction(double d){
    this.maxSignalFraction = d;
  }

  /**
  * Set the window size for angle profiling in the nuclei
  *
  *  @param i the window size
  */
  public void setAngleProfileWindowSize(int i){
    this.angleProfileWindowSize = i;
  }

  /**
  * Add a NucleusCollection to the group, using the source folder
  * name as a key.
  *
  *  @param file a folder to be analysed
  *  @param collection the collection of nuclei found
  */
  public void addNucleusCollection(File file, NucleusCollection collection){
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
        mw.log("Failed to create directory: "+e);
      }
    }
    return output;
  }


  /**
  * Go through the input folder. Check if each file is
  * suitable for analysis, and if so, call the analyser.
  *
  * @param folder the folder of images to be analysed
  */
	protected void processFolder(File folder){

    File[] listOfFiles = folder.listFiles();
    NucleusCollection folderCollection = new NucleusCollection(folder, this.outputFolder, folder.getName());
    this.collectionGroup.put(folder, folderCollection);
 
    for (File file : listOfFiles) {

      boolean ok = this.checkFile(file);
            
      if(ok){
        try {
          Opener localOpener = new Opener();
          ImagePlus image = localOpener.openImage(file.getAbsolutePath());   
          
          ImageStack imageStack = ImageImporter.convert(image);
          
          // put folder creation here so we don't make folders we won't use (e.g. empty directory analysed)
          makeFolder(folder);
          processImage(imageStack, file);
          image.close();

        } catch (Exception e) { // end try
        	mw.log("Error in image processing: "+e.getMessage());
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
		detector.setMaxSize(this.maxNucleusSize);
		detector.setMinSize(this.minNucleusSize);
		detector.setMinCirc(this.minNucleusCirc);
		detector.setMaxCirc(this.maxNucleusCirc);
		detector.setThreshold(this.nucleusThreshold);
		detector.setChannel(ImageImporter.COUNTERSTAIN);
		try{
			detector.run(image);
		} catch(Exception e){
			mw.log("Error in nucleus detection: "+e.getMessage());
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
    List<Roi> roiList = getROIs(image);
    if(roiList.isEmpty()){
    	mw.log("  No usable nuclei in image");
    }

    int nucleusNumber = 0;

    for(Roi roi : roiList){
      
    	mw.log("  Acquiring nucleus "+nucleusNumber);
      try{
      	analyseNucleus(roi, image, nucleusNumber, path); // get the profile data back for the nucleus
      	this.totalNuclei++;
      } catch(Exception e){
    	  mw.log("  Error acquiring nucleus: "+e.getMessage());
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
	  double xCentre = xbase+(bounds.getWidth()/2);
	  double yCentre = ybase+(bounds.getHeight()/2);
	  String position = xCentre+"-"+yCentre; // store the centre of the rectangle for remapping

	  try{
	  	// Enlarge the ROI, so we can do nucleus detection on the resulting original images
		  ImageStack smallRegion = getRoiAsStack(nucleus, image);
		  Roi enlargedRoi = RoiEnlarger.enlarge(nucleus, 20);
		  ImageStack largeRegion = getRoiAsStack(enlargedRoi, image);
	
		  nucleus.setLocation(0,0); // translate the roi to the new image coordinates
		  
		  // turn roi into Nucleus for manipulation
		  Nucleus currentNucleus = new Nucleus(nucleus, path, nucleusNumber, position);
	
		  currentNucleus.setCentreOfMass(new XYPoint(values.get("XM")-xbase, values.get("YM")-ybase)); // need to offset
		  currentNucleus.setArea(values.get("Area")); 
		  currentNucleus.setFeret(values.get("Feret"));
		  currentNucleus.setPerimeter(values.get("Perim"));
	
		  currentNucleus.setOutputFolder(this.outputFolder);
		  currentNucleus.intitialiseNucleus(this.angleProfileWindowSize);
		  
		  // save out the image stacks rather than hold within the nucleus
		  try{
			  IJ.saveAsTiff(new ImagePlus(null, smallRegion), currentNucleus.getOriginalImagePath());
			  IJ.saveAsTiff(new ImagePlus(null, largeRegion), currentNucleus.getEnlargedImagePath());
			  IJ.saveAsTiff(ImageExporter.convert(smallRegion), currentNucleus.getAnnotatedImagePath());

		  } catch(Exception e){
			  mw.log("Error saving original, enlarged or annotated image: "+e.getMessage());
		  }
		  
		  SignalDetector signalDetector = new SignalDetector(this.signalThreshold, this.minSignalSize, this.maxSignalFraction);
		  signalDetector.run(currentNucleus, smallRegion);
	
		  // if everything checks out, add the measured parameters to the global pool
		  NucleusCollection collectionToAddTo = collectionGroup.get( new File(currentNucleus.getDirectory()));
		  collectionToAddTo.addNucleus(currentNucleus);
	  }catch(Exception e){
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