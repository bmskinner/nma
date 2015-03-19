/*
  -----------------------
  NUCLEUS REFINDER
  -----------------------
  This takes a list of image paths, and nucleus
  coordinates. Trim the path to just the source image name
  Check if there is the point is within a nucleus.
*/  
package no.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Scanner;
import no.components.XYPoint;

public class NucleusRefinder
  extends no.analysis.NucleusDetector
{

  private File pathList; // the file of paths and coordinates

  private int xOffset;
  private int yOffset;

  // a structure to hold the image names, and the extracted nucleus coordinates
  private ArrayList< HashMap<String, XYPoint> > nucleiToFind = new ArrayList< HashMap<String, XYPoint> >(0);


  /*
    Constructors
  */
	public NucleusRefinder(File inputFolder, String outputFolder, File pathList){
		super(inputFolder, outputFolder);
    this.pathList = pathList;

    // get the image names and coordinates from the pathList
    try{
      parsePathList(this.pathList);
    } catch(IOException e){
      IJ.log("IOException in NucleusRefinder.parsePathList(): "+e.getMessage());
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
      return;
    }
    File imagePath = new File(path);
    String name = imagePath.getName();

    Scanner positionScanner = new Scanner(position);
    double x = 0;
    double y = 0;
    positionScanner.useDelimiter("-");
    if (positionScanner.hasNext()){
      x = Double.parseDouble(positionScanner.next());
      y = Double.parseDouble(positionScanner.next());
    }

    // IJ.log("Found image: "+name+" x:"+x+" y:"+y);
    XYPoint point = new XYPoint(x+xOffset, y+yOffset);

    HashMap<String, XYPoint> map = new HashMap<String, XYPoint>();
    map.put(name, point);
    nucleiToFind.add(map);
  }


  /*
    An addition to the check file, that ensures the image 
    is in the list of images with nuclei to recapture
  */
  @Override
  protected boolean checkFile(File file){
    boolean ok = super.checkFile(file);
            
    if(ok){

      ok = false;
      //check that the image is in the list to be analysed
      for( HashMap<String, XYPoint> hash : nucleiToFind ){
        if(hash.containsKey(file.getName())){
          ok = true;
        }
      }
    }
    return ok;
  }

  /*
    Detects nuclei within the image.
    For each nucleus, perform the analysis step
  */
  @Override
  protected void processImage(ImagePlus image, File path){

    IJ.log("File:  "+path.getName());

    Map<Roi, HashMap<String, Double>> map = getROIs(image);
    int i = 0;

    Set<Roi> keys = map.keySet();

    for(Roi roi : keys){
       try{

        // if the point is within the roi
        boolean ok = false;
        for( HashMap<String, XYPoint> hash : nucleiToFind ){
          if(hash.containsKey(path.getName())){
            XYPoint p = hash.get(path.getName());
            if(roi.getBounds().contains(p.getXAsInt(), p.getYAsInt())){
              ok = true;
              IJ.log("  Acquiring nucleus at "+p.getXAsInt()+","+p.getYAsInt());
            }
          }
        }

        if(ok){
          analyseNucleus(roi, image, i, path, map.get(roi)); // get the profile data back for the nucleus
          this.totalNuclei++;
        }
      } catch(Exception e){
        IJ.log("  Error acquiring nucleus: "+e);
      }
      i++;
    } 
  }
}