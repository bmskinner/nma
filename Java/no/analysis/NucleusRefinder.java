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

public class NucleusRefinder
  extends NucleusDectector{

  // private final static Charset ENCODING = StandardCharsets.UTF_8;  

	// private File folder;
  private File pathList; // the file of paths and coordinates

  // a structure to hold the image names, and the extracted nucleus coordinates
  private ArrayList< HashMap<String, XYPoint> > nucleiToFind = new ArrayList< HashMap<String, XYPoint> >();


  /*
    Constructors
  */
	public NucleusRefinder(File folder, File pathList){
		super(folder);
    this.pathList = pathList;

    // get the image names and coordinates from the pathList
    parsePathList(this.pathList);
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
		Go through the input folder. Check if each file
		is an image. Also check if it is in the 'banned list'.
		These are prefixes that are attached to exported images
		at later stages of analysis. This prevents exported images
		from previous runs being analysed.
	*/
  @Override
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
  @Override
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
              IJ.log("Found a nucleus at "+p.getXAsInt()+","+p.getYAsInt());
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
}