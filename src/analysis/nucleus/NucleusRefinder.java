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
/*
  -----------------------
  NUCLEUS REFINDER
  -----------------------
  This takes a list of image paths, and nucleus
  coordinates. Trim the path to just the source image name
  Check if there is the point is within a nucleus.
*/  
package analysis.nucleus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


//import utility.Logger;
import analysis.AnalysisOptions;
import components.generic.XYPoint;

public class NucleusRefinder  extends NucleusDetectionWorker
{

  private File pathList; // the file of paths and coordinates

  private int xOffset;
  private int yOffset;

  private boolean realignMode;

  private Map<File, File> fileMap =  new HashMap<File, File>(); // map from old file to new file

  private Map<File, XYPoint> offsets = new HashMap<File, XYPoint>(); // hold the calculated offsets for each image

  // a structure to hold the image names, and the extracted nucleus coordinates
  private ArrayList< HashMap<File, XYPoint> > nucleiToFind = new ArrayList< HashMap<File, XYPoint> >(0);


  /*
    Constructors
  */
  public NucleusRefinder(String outputFolder, File pathList, File debugFile, AnalysisOptions options){
	  super( outputFolder,  debugFile, options);
	  this.pathList = pathList;
	  fileLogger = Logger.getLogger(NucleusRefinder.class.getName());

	  // get the image names and coordinates from the pathList
	  try{
		  parsePathList(this.pathList);
	  } catch(IOException e){
		  fileLogger.log(Level.SEVERE, "Error parsing path list: "+e.getMessage(), e);
	  }
  }


  public void parsePathList(File file) throws IOException {
    Scanner scanner =  new Scanner(file);
    int i=0;
    while (scanner.hasNextLine()){
      if(i>0){
        processLine(scanner.nextLine());
      }
      i++;
    }
    scanner.close();
  }

  public int getMappingCount(){
    return this.nucleiToFind.size()-1; // for some reason it gives one more than expected
  }

  public void setXOffset(int i){
    this.xOffset = i;
  }

  public void setYOffset(int i){
    this.yOffset = i;
  }

  public void setRealignMode(boolean b){
    this.realignMode = b;
  }

  protected void processLine(String line){
    // IJ.log("Processing line: "+line);
    //use a second Scanner to parse the content of each line 
    Scanner scanner = new Scanner(line);
    scanner.useDelimiter("\t");
    String path = "";
    String position = "";
    if (scanner.hasNext()){
      path     = scanner.next();
      position = scanner.next();
    }
    if(position.equals("POSITION")){
    	scanner.close();
    	return;
    }
    File imagePath = new File(path);
//    String name = imagePath.getName();

    Scanner positionScanner = new Scanner(position);
    double x = 0;
    double y = 0;
    positionScanner.useDelimiter("-");
    if (positionScanner.hasNext()){
      x = Double.parseDouble(positionScanner.next());
      y = Double.parseDouble(positionScanner.next());
    }
    positionScanner.close();

    // IJ.log("Found image: "+name+" x:"+x+" y:"+y);
    XYPoint point = new XYPoint(x, y);

    HashMap<File, XYPoint> map = new HashMap<File, XYPoint>();
    map.put(imagePath, point);
    nucleiToFind.add(map);
    scanner.close();
  }


  /*
    An addition to the check file, that ensures the image 
    is in the list of images with nuclei to recapture
  */
//  protected boolean checkFiles(File file){
//    boolean ok = NucleusDetector.checkFile(file);
//            
//    if(ok){
//
//      ok = false;
//      //check that the image is in the list to be analysed
//      for( HashMap<File, XYPoint> hash : nucleiToFind ){
//        Set<File> fileSet = hash.keySet();
//        for(File oldFile : fileSet){ // will only be one entry in this hash
//          if(oldFile.getName().equals(file.getName() ) ){
//            ok = true;
//          }
//        }
//      }
//    }
//    return ok;
//  }

//  private ImagePlus makeGreyscaleFromBlue(ImagePlus image){
//
//    ColorProcessor icp = (ColorProcessor) image.getChannelProcessor().convertToRGB();
//    ImagePlus newImage = new ImagePlus("image",new ByteProcessor( image.getWidth(), 
//                                                                  image.getHeight(), 
//                                                                  icp.getChannel(3)));
//    return newImage;
//
//  } 

//  private void updateFileMap(File file){
//    for( HashMap<File, XYPoint> hash : nucleiToFind ){ // the hash holds the tmeplate image path and position
//      Set<File> fileSet = hash.keySet();
//      for(File oldFile : fileSet){ // will only be one entry in this hash
//        if(oldFile.getName().equals(file.getName() ) ){
//          fileMap.put(file, oldFile);
//        }
//      }
//    }
//  }
//
//  private void alignImages(File newPath, File oldPath){
//    
//    if(this.realignMode==true){    
//      // this is the template file that matches the file we are checking
//      ImagePlus template = new ImagePlus(oldPath.getAbsolutePath());
//      ImagePlus source   = new ImagePlus(newPath.getAbsolutePath());
//
//      // CONVERT BOTH BLUE CHANNELS TO 8-bit GREYSCALE
//      ImagePlus imageToOffset = makeGreyscaleFromBlue(source);
//      ImagePlus templateImage = makeGreyscaleFromBlue(template);
//
//      // INSERT IMAGE ALIGNMENT HERE
//      ImageAligner aligner = new ImageAligner(templateImage, imageToOffset, analysisOptions.getNucleusThreshold());
//      aligner.setXOffset(this.xOffset);
//      aligner.setYOffset(this.yOffset);
//      aligner.run();
//      offsets.put(newPath, new XYPoint(aligner.getXOffset(), aligner.getYOffset()));
//    } else {
//      offsets.put(newPath, new XYPoint(this.xOffset, this.yOffset));
//    }
//  }

  /*
    Detects nuclei within the image.
    For each nucleus, perform the analysis step
  */
//  @Override
//  protected void processImage(ImageStack image, File path){
//
//    mw.log("File:  "+path.getName());
//    updateFileMap(path); // map from new to old
//    if(!fileMap.containsKey(path)){ return; } // skip images with no nuclei to catch
//
//    List<Roi> roiList = getROIs(image, true);
//    int i = 0;
//
//    if(!offsets.containsKey(path)){
//      alignImages(path, fileMap.get(path));
//    }   
//
//    for(Roi roi : roiList){
//       try{
//
//        boolean ok = checkRoi(roi, path);
//
//        if(ok){
//          analyseNucleus(roi, image, i, path); // get the profile data back for the nucleus
//          this.totalNuclei++;
//        }
//      } catch(Exception e){
//        mw.log("  Error acquiring nucleus: "+e);
//      }
//      i++;
//    } 
//  }
//  
//  private boolean checkRoi(Roi roi, File path){
//	  boolean result = false;
//	  for( HashMap<File, XYPoint> hash : nucleiToFind ){ // the hash holds the tmeplate image path and position
//
//		  for(File oldFile : hash.keySet()){ // will only be one entry in this hash
//
//			  if(oldFile.getName().equals(path.getName() ) ){
//				  XYPoint p = hash.get(oldFile);
//
//				  // APPLY THE CALCULATED OFFSET HERE
//				  XYPoint imageOffset = offsets.get(path);
//
//				  int xToFind = p.getXAsInt()-imageOffset.getXAsInt();
//				  int yToFind = p.getYAsInt()-imageOffset.getYAsInt();
//
//				  if(roi.getBounds().contains( xToFind, yToFind )){
//					  result = true;
//					  log(Level.INFO, "  Acquiring nucleus at: "+xToFind+","+yToFind);
//				  }
//			  }
//
//		  }
//	  }
//	  return result;
//  }
}