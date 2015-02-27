/*
  -----------------------
  NUCLEUS REFINDER
  -----------------------
  This takes a list of image paths, and nucleus
  coordinates. Trim the path to just the source image name
  Check if there is the point is within a nucleus.
  Eventually modify the NucleusDectector so we can use it
  for the detection step
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Scanner;
import no.nuclei.*;
import no.utility.*;
import no.collections.*;
import no.components.*;

public class NucleusRefinder {

	// colour channels
  private static final int RED_CHANNEL   = 0;
  private static final int GREEN_CHANNEL = 1;
  private static final int BLUE_CHANNEL  = 2;

  private static final String IMAGE_PREFIX = "export.";

  private static final String[] prefixesToIgnore = { IMAGE_PREFIX, "composite", "plot"};

  private static final String[] fileTypes = {".tif", ".tiff", ".jpg"};

  private final static Charset ENCODING = StandardCharsets.UTF_8;  

  /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private double minNucleusSize  = 500;
  private double maxNucleusSize  = 10000;
  private double minNucleusCirc  = 0.4;
  private double maxNucleusCirc  = 1;

  private int nucleusThreshold = 36;

  // counts of nuclei processed
  private int totalNuclei        = 0;
  private int nucleiFailedOnTip  = 0;
  private int nucleiFailedOnTail = 0;
  private int nucleiFailedOther  = 0; // generic reasons for failure

  private  int    signalThreshold = 70;
  private  double   minSignalSize = 5;
  private  double   maxSignalFraction = 0.5;

	private File folder;
  private File pathList; // the file of paths and coordinates

	// private NucleusCollection collection;
  private HashMap<File, NucleusCollection> collectionGroup = new HashMap<File, NucleusCollection>();

  // a structure to hold the image names, and the extracted nucleus coordinates
  private ArrayList< HashMap<String, XYPoint> > nucleiToFind = new ArrayList< HashMap<String, XYPoint> >();


  /*
    Constructors
  */
	public NucleusRefinder(File folder, File pathList){
		this.folder = folder;
    this.pathList = pathList;

    // get the image names and coordinates from the pathList
    parsePathList(this.pathList);
	}

  public void runDetector(){
    try{
      processFolder(this.folder);
    } catch(Exception e){
      IJ.log("Error in processing folder: "+e);
    }
  }

  public void parsePathList(File file) throws IOException {
    try (Scanner scanner =  new Scanner(file)){
      while (scanner.hasNextLine()){
        processLine(scanner.nextLine());
      }      
    }
  }

  protected void processLine(String line){
    //use a second Scanner to parse the content of each line 
    Scanner scanner = new Scanner(line);
    scanner.useDelimiter("\t");
    if (scanner.hasNext()){
      String path     = scanner.next();
      String position = scanner.next();
    }
    File imagePath = new File(path);
    String name = imagePath.getName();

    Scanner positionScanner = new Scanner(position);
    positionScanner.useDelimiter("-");
    if (positionScanner.hasNext()){
      double x = double.parseDouble(positionScanner.next());
      double y = double.parseDouble(positionScanner.next());
    }

    IJ.log("Found image: "+name+" x:"+x+" y:"+y);
    XYPoint point = new XYPoint(x, y);

    HashMap<String, XYPoint> map = new HashMap<String, XYPoint>();
    map.put(name, point);
    nucleiToFind.add(map);
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



  public HashMap<File, NucleusCollection> getNucleiCollections(){
    // remove any empty collections before returning
    ArrayList<File> toRemove = new ArrayList<File>(0);
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
		Go through the input folder. Check if each file
		is an image. Also check if it is in the 'banned list'.
		These are prefixes that are attached to exported images
		at later stages of analysis. This prevents exported images
		from previous runs being analysed.
	*/
	private void processFolder(File folder){

    File[] listOfFiles = folder.listFiles();
    NucleusCollection folderCollection = new NucleusCollection(folder, folder.getName());
    this.collectionGroup.put(folder, folderCollection);
 
    for (File file : listOfFiles) {

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
            
        if(ok){

          ok = false;
          //check that the image is in the list to be analysed
          for( HashMap<String, XYPoint> hash : nucleiToFind ){
            if(hash.keySet().get(0).equals(filename)){
              ok = true;
            }
          }

          if(ok){

            try {
              Opener localOpener = new Opener();
              ImagePlus localImagePlus = localOpener.openImage(file.getAbsolutePath());             
              // handle the image
              if(localImagePlus.getType()==ImagePlus.COLOR_RGB){ // convert to RGB
                processImage(localImagePlus, file);
                localImagePlus.close();
              } else {
                IJ.log("Cannot analyse - RGB image required");
              }
            } catch (Exception e) { 
                IJ.log("Error in image processing: "+e);
            }
          }
        }
      } else {
        if(file.isDirectory()){ // recurse over any sub folders
          processFolder(file);
        }
      }
    }
  }

  /*
    Detects nuclei within the image.
    For each nucleus, perform the analysis step
  */
  private void processImage(ImagePlus image, File path){

    IJ.log("File:  "+path.getName());
    RoiManager nucleiInImage = findNucleiInImage(image);

    Roi[] roiArray = nucleiInImage.getSelectedRoisAsArray();
    int i = 0;

    for(Roi roi : roiArray){
      
      IJ.log("  Acquiring nucleus "+i);
      try{

        // if the point is within the roi
        boolean ok = false;
        for( HashMap<String, XYPoint> hash : nucleiToFind ){
          if(hash.keySet().get(0).equals(filename)){
            XYPoint p = hash.get(path.getName());
            if(roi.contains(p.getXAsInt(), p.getYAsInt())){
              ok = true;
            }
          }
        }

        if(ok){
        	analyseNucleus(roi, image, i, path); // get the profile data back for the nucleus
        	this.totalNuclei++;
        }
      } catch(Exception e){
      	IJ.log("  Error acquiring nucleus: "+e);
      }
      i++;
    } 
  }

  /*
    Within a given image, look for nuclei using the particle analyser.
    Return an RoiManager containing the outlines of all potential nuclei
  */
  private RoiManager findNucleiInImage(ImagePlus image){

    RoiManager manager = new RoiManager(true);

    // split out blue channel
    ChannelSplitter cs = new ChannelSplitter();
    ImagePlus[] channels = cs.split(image);
    ImagePlus blue = channels[BLUE_CHANNEL];
    
    // threshold
    ImageProcessor ip = blue.getChannelProcessor();
    ip.smooth();
    ip.threshold(this.nucleusThreshold);
    ip.invert();
    // blue.show();

    // run the particle analyser
    ResultsTable rt = new ResultsTable();
    ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, 
                ParticleAnalyzer.CENTER_OF_MASS | ParticleAnalyzer.AREA ,
                 rt, this.minNucleusSize, this.maxNucleusSize, this.minNucleusCirc, this.maxNucleusCirc);
    try {
      pa.setRoiManager(manager);
      boolean success = pa.analyze(blue);
      if(success){
        String plural = manager.getCount() == 1 ? "nucleus" : "nuclei";
        IJ.log("  Found "+manager.getCount()+ " "+plural);
      } else {
        IJ.log("  Unable to perform particle analysis");
      }
    } catch(Exception e){
       IJ.log("  Error in particle analyser: "+e);
    } finally {
      blue.close();
    }
   
   return manager;
  }

  /*
  	Save the region of the input image containing the nucleus
    Add to collection
  */
  private void analyseNucleus(Roi nucleus, ImagePlus image, int nucleusNumber, File path){
    
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