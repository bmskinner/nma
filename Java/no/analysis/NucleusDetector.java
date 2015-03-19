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
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.io.File;
import java.util.*;
import no.nuclei.*;
import no.utility.*;
import no.collections.*;
import no.components.*;

public class NucleusDetector {

	// colour channels
  public static final int RED_CHANNEL   = 0;
  public static final int GREEN_CHANNEL = 1;
  public static final int BLUE_CHANNEL  = 2;

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
  private Map<File, NucleusCollection> collectionGroup = new HashMap<File, NucleusCollection>();


  /**
  * Construct a detector on the given folder, and output the results to 
  * the given output folder
  *
  * @param inputFolder the folder to analyse
  * @param outputFolder the name of the folder for results
  */
	public NucleusDetector(File inputFolder, String outputFolder){
		this.inputFolder = inputFolder;
    this.outputFolder = outputFolder;
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
    return this.fileTypes;
  }

  /**
  * Get the filename prefixes to ignore.
  * These prevent exports of previously analyses
  * being included in an analysis
  *
  *  @return the array of prefixes
  */
  public String[] getPrefixesToIgnore(){
    return this.prefixesToIgnore;
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
    this.minNucleusSize = d;
  } 

  /**
  * Set maximum size of a nucleus in pixels
  *
  *  @param d the maximum size
  */
  public void setMaxNucleusSize(double d){
    this.maxNucleusSize = d;
  }   

  /**
  * Set minimum circularity of a nucleus
  *
  *  @param d the minimum circularity
  */
  public void setMinNucleusCirc(double d){
    this.minNucleusCirc = d;
  }

  /**
  * Set maximum circularity of a nucleus
  *
  *  @param d the maximum circularity
  */
  public void setMaxNucleusCirc(double d){
    this.maxNucleusCirc = d;
  }

  /**
  * Set the image thresholding for detecting nuclei
  *
  *  @param i the threshold
  */
  public void setThreshold(int i){
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
  * Given a greyscale image, convert it to RGB, and set the greyscale to 
  * the blue channel. 
  *
  * @param image the ImagePlus to convert
  * @return a COLOR_RGB ImagePlus
  */
  private ImagePlus makeRGB(ImagePlus image) throws Exception{
    
    ImagePlus mergedImage = new ImagePlus();
    if(image.getType()==ImagePlus.GRAY8){

      byte[] blank = new byte[image.getWidth() * image.getHeight()];
      for( byte b : blank){
        b = -128;
      }

      ImagePlus[] images = new ImagePlus[3];
      images[0] = new ImagePlus("red",   new ByteProcessor(image.getWidth(), image.getHeight(), blank));
      images[1] = new ImagePlus("green", new ByteProcessor(image.getWidth(), image.getHeight(), blank));
      images[2] = image;      

      RGBStackMerge merger = new RGBStackMerge();
      // IJ.log("  Merger created");
      mergedImage = merger.mergeChannels(images, false); 
      // IJ.log("  Merged");
    } else{
      IJ.log("  Cannot convert at present; please convert to RGB manually");
      throw new Exception("Error converting image to RGB: wrong type");
    }
    return mergedImage.flatten();
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
        IJ.log("Failed to create directory: "+e);
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
          // handle the image
          if(image.getType()!=ImagePlus.COLOR_RGB){ // convert to RGB
            IJ.log("Converting image to RGB");
            image = this.makeRGB(image);
          } 
          // put folder creation here so we don't make folders we won't use (e.g. empty directory analysed)
          File output = makeFolder(folder);
          processImage(image, file);
          image.close();

        } catch (Exception e) { // end try
            IJ.log("Error in image processing: "+e);
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
  protected Map<Roi, HashMap<String, Double>> getROIs(ImagePlus image){
    Detector detector = new Detector();
    detector.setMaxSize(this.maxNucleusSize);
    detector.setMinSize(this.minNucleusSize);
    detector.setMinCirc(this.minNucleusCirc);
    detector.setMaxCirc(this.maxNucleusCirc);
    detector.setThreshold(this.nucleusThreshold);
    detector.setChannel(BLUE_CHANNEL);
    detector.run(image);
    return detector.getRoiMap();
  }

  /**
  * Call the nucleus detector on the given image.
  * For each nucleus, perform the analysis step
  *
  * @param image the ImagePlus to be analysed
  * @param path the full path of the image
  */
  protected void processImage(ImagePlus image, File path){

    IJ.log("File:  "+path.getName());
    Map<Roi, HashMap<String, Double>> map = getROIs(image);

    int i = 0;

    Set<Roi> keys = map.keySet();

    for(Roi roi : keys){
      
      IJ.log("  Acquiring nucleus "+i);
      try{
      	analyseNucleus(roi, image, i, path, map.get(roi)); // get the profile data back for the nucleus
      	this.totalNuclei++;
      } catch(Exception e){
      	IJ.log("  Error acquiring nucleus: "+e);
      }
      i++;
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
  * @param values the Map holding stats for this nucleus
  */
  protected void analyseNucleus(Roi nucleus, ImagePlus image, int nucleusNumber, File path, Map<String, Double> values){
    
    // save the position of the roi, for later use
    double xbase = nucleus.getXBase();
    double ybase = nucleus.getYBase();

    Rectangle bounds = nucleus.getBounds();
    double xCentre = xbase+(bounds.getWidth()/2);
    double yCentre = ybase+(bounds.getHeight()/2);
    String position = xCentre+"-"+yCentre;

    // Enlarge the ROI, so we can do nucleus detection on the resulting original images
    RoiEnlarger enlarger = new RoiEnlarger();
    Roi enlargedRoi = enlarger.enlarge(nucleus, 20);

    // make a copy of the nucleus only for saving out and processing
    image.setRoi(enlargedRoi);
    image.copy();
    ImagePlus largeRegion = ImagePlus.getClipboard();
    image.setRoi(nucleus);
    image.copy();
    ImagePlus smallRegion = ImagePlus.getClipboard();

    nucleus.setLocation(0,0); // translate the roi to the new image coordinates
    smallRegion.setRoi(nucleus);

    // turn roi into Nucleus for manipulation
    Nucleus currentNucleus = new Nucleus(nucleus, path, smallRegion, largeRegion, nucleusNumber, position);

    currentNucleus.setCentreOfMass(new XYPoint(values.get("XM")-xbase, values.get("YM")-ybase)); // need to offset
    currentNucleus.setArea(values.get("Area")); 
    currentNucleus.setFeret(values.get("Feret"));
    currentNucleus.setPerimeter(values.get("Perim"));

    currentNucleus.setOutputFolder(this.outputFolder);
    currentNucleus.intitialiseNucleus(this.angleProfileWindowSize);

    currentNucleus.setSignalThreshold(this.signalThreshold);
    currentNucleus.setMinSignalSize(this.minSignalSize);
    currentNucleus.setMaxSignalFraction(this.maxSignalFraction);

    currentNucleus.detectSignalsInNucleus();
    currentNucleus.annotateNucleusImage();

    // if everything checks out, add the measured parameters to the global pool
    NucleusCollection collectionToAddTo = collectionGroup.get( new File(currentNucleus.getDirectory()));
    collectionToAddTo.addNucleus(currentNucleus);
  }
}