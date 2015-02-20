/*
  -----------------------
  NUCLEUS DETECTOR
  -----------------------
  Contains the variables for opening
  folders and files and detecting nuclei
  within them
*/  
package nucleusAnalysis;

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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;

public class NucleusDetector {

	// colour channels
  private static final int RED_CHANNEL   = 0;
  private static final int GREEN_CHANNEL = 1;
  private static final int BLUE_CHANNEL  = 2;

  private static final String IMAGE_PREFIX = "export.";

  private static final String[] prefixesToIgnore = { IMAGE_PREFIX, "composite", "plot"};

  private static final String[] fileTypes = {".tif", ".tiff", ".jpg"};

  /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private static final int    NUCLEUS_THRESHOLD = 36;
  private static final double MIN_NUCLEAR_SIZE  = 500;
  private static final double MAX_NUCLEAR_SIZE  = 10000;
  private static final double MIN_NUCLEAR_CIRC  = 0.3;
  private static final double MAX_NUCLEAR_CIRC  = 1;

  // counts of nuclei processed
  private int totalNuclei        = 0;
  private int nucleiFailedOnTip  = 0;
  private int nucleiFailedOnTail = 0;
  private int nucleiFailedOther  = 0; // generic reasons for failure

	private File folder;
	// private NucleusCollection collection;
  private HashMap<File, NucleusCollection> collectionGroup = new HashMap<File, NucleusCollection>();

	public NucleusDetector(File folder){
		this.folder = folder;

		try{
			processFolder(this.folder);
		} catch(Exception e){
			IJ.log("Error in processing folder: "+e);
		}

	}

	// public NucleusCollection getNucleiInFolder(){
	// 	return this.collection;
	// }

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
          try {
            Opener localOpener = new Opener();
            ImagePlus localImagePlus = localOpener.openImage(file.getAbsolutePath());             
            // handle the image
            processImage(localImagePlus, file);
            localImagePlus.close();

          } catch (Exception e) { 
              IJ.log("Error in image processing: "+e);
          }
        }
      } else {
        if(file.isDirectory()){ // recurse over any sub folders
          processFolder(file);
        }
      }
    }
    // this.collectionGroup.add(folderCollection);
    // IJ.log("Within folder:");
    // IJ.log("Total nuclei  : "+this.totalNuclei);
  }

  /*
    Detects nuclei within the image.
    For each nucleus, perform the analysis step
  */
  private void processImage(ImagePlus image, File path){

    IJ.log("File:  "+path.getName());
    RoiManager nucleiInImage;

    nucleiInImage = findNucleiInImage(image);

    Roi[] roiArray = nucleiInImage.getSelectedRoisAsArray();
    int i = 0;

    for(Roi roi : roiArray){
      
      IJ.log("  Analysing nucleus "+i);
      try{
      	analyseNucleus(roi, image, i, path); // get the profile data back for the nucleus
      	this.totalNuclei++;
      } catch(Exception e){
      	IJ.log("  Error analysing nucleus: "+e);
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
    ip.threshold(NUCLEUS_THRESHOLD);
    ip.invert();
    // blue.show();

    // run the particle analyser
    ResultsTable rt = new ResultsTable();
    ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, 
                ParticleAnalyzer.CENTER_OF_MASS | ParticleAnalyzer.AREA ,
                 rt, MIN_NUCLEAR_SIZE, MAX_NUCLEAR_SIZE, MIN_NUCLEAR_CIRC, MAX_NUCLEAR_CIRC);
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
    
    // make a copy of the nucleus only for saving out and processing
    image.setRoi(nucleus);
    image.copy();
    ImagePlus smallRegion = ImagePlus.getClipboard();

    nucleus.setLocation(0,0); // translate the roi to the new image coordinates
    smallRegion.setRoi(nucleus);

    // turn roi into Nucleus for manipulation
    Nucleus currentNucleus = new Nucleus(nucleus, path, smallRegion, nucleusNumber);

    // if everything checks out, add the measured parameters to the global pool
    NucleusCollection collectionToAddTo = collectionGroup.get( new File(currentNucleus.getDirectory()));
    collectionToAddTo.addNucleus(currentNucleus);
  }
}