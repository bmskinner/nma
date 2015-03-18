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
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ProgressBar;
import ij.gui.TextRoi;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;
import ij.measure.ResultsTable;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.RoiEnlarger;
import ij.plugin.frame.RoiManager;
import ij.plugin.RGBStackMerge;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
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

  /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  protected double minNucleusSize  = 500;
  protected double maxNucleusSize  = 10000;
  protected double minNucleusCirc  = 0.4;
  protected double maxNucleusCirc  = 1;

  private int angleProfileWindowSize  = 23;

  protected int nucleusThreshold = 36;

  // counts of nuclei processed
  protected int totalNuclei        = 0;
  // protected int nucleiFailedOnTip  = 0;
  // protected int nucleiFailedOnTail = 0;
  // protected int nucleiFailedOther  = 0; // generic reasons for failure

  private  int    signalThreshold = 70;
  private  double   minSignalSize = 5;
  private  double   maxSignalFraction = 0.5;

	private File inputFolder;
  protected String outputFolder;
	// private NucleusCollection collection;
  private Map<File, NucleusCollection> collectionGroup = new HashMap<File, NucleusCollection>();


  /*
    Constructors
  */
	public NucleusDetector(File inputFolder, String outputFolder){
		this.inputFolder = inputFolder;
    this.outputFolder = outputFolder;
	}


  public void runDetector(){
    try{
      processFolder(this.inputFolder);
    } catch(Exception e){
      IJ.log("Error in processing folder: "+e);
    }
  }

  /*
    Getters
  */
  public String[] getFileTypes(){
    return this.fileTypes;
  }

  public String[] getPrefixesToIgnore(){
    return this.prefixesToIgnore;
  }

	/*
    Settings for nucleus detection
  */

  public void setMinNucleusSize(double d){
    this.minNucleusSize = d;
  } 

  public void setMaxNucleusSize(double d){
    this.maxNucleusSize = d;
  }   

  public void setMinNucleusCirc(double d){
    this.minNucleusCirc = d;
  }

  public void setMaxNucleusCirc(double d){
    this.maxNucleusCirc = d;
  }

  public void setThreshold(int i){
    this.nucleusThreshold = i;
  } 

  /*
    Settings for signal detection
  */

  public void setSignalThreshold(int i){
    this.signalThreshold = i;
  }

  public void setMinSignalSize(double d){
    this.minSignalSize = d;
  }

  // this is a fraction of the nuclear area
  public void setMaxSignalFraction(double d){
    this.maxSignalFraction = d;
  }

  public void setAngleProfileWindowSize(int i){
    this.angleProfileWindowSize = i;
  }

  public void addNucleusCollection(File file, NucleusCollection collection){
    this.collectionGroup.put(file, collection);
  }



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

	/*
    Check the the file in in question is suitable
    for analysis.
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

  private ImagePlus makeRGB(ImagePlus image) throws Exception{
    
    ImagePlus mergedImage = new ImagePlus();
    if(image.getType()==ImagePlus.GRAY8){

      ImagePlus[] images = new ImagePlus[3];
      images[0] = new ImagePlus();
      images[1] = new ImagePlus();
      images[2] = image;

      IJ.log("Attempting to merge");

      RGBStackMerge merger = new RGBStackMerge();
      IJ.log("Merger created");
      mergedImage = merger.mergeChannels(images, false); 
      IJ.log("Merged");
    } else{
      IJ.log("Cannot convert at present; please convert to RGB manually");
      throw new Exception("Error converting image to RGB: wrong type");
    }
    return mergedImage;
  }

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

  /*
    Go through the input folder. Check if each file
    is an image. Also check if it is in the 'banned list'.
    These are prefixes that are attached to exported images
    at later stages of analysis. This prevents exported images
    from previous runs being analysed.
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
            IJ.log("Found image type "+image.getType()+"; converting to RGB");
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

  /*
    Detects nuclei within the image.
    For each nucleus, perform the analysis step
  */
  protected void processImage(ImagePlus image, File path){

    IJ.log("File:  "+path.getName());

    Detector detector = new Detector();
    detector.setMaxSize(this.maxNucleusSize);
    detector.setMinSize(this.minNucleusSize);
    detector.setMinCirc(this.minNucleusCirc);
    detector.setMaxCirc(this.maxNucleusCirc);
    detector.setThreshold(this.nucleusThreshold);
    detector.setChannel(BLUE_CHANNEL);
    detector.run(image);
    Map<Roi, HashMap<String, Double>> map = detector.getRoiMap();

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

  /*
  	Save the region of the input image containing the nucleus
    Add to collection
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