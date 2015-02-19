/*
  -----------------------
  NUCLEUS CLASS
  -----------------------
  Contains the variables for storing a nucleus,
  plus the functions for calculating aggregate stats
  within a nucleus
*/  
package nucleusAnalysis;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.ProgressBar;
import ij.gui.TextRoi;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;
import ij.measure.ResultsTable;
import ij.measure.SplineFitter;
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
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;

public class Nucleus {

  private static final int RED_CHANNEL   = 0;
  private static final int GREEN_CHANNEL = 1;
  private static final int BLUE_CHANNEL  = 2;

  private int nucleusNumber; // the number of the nucleus in the current image
  private int failureCode = 0; // stores a code to explain why the nucleus failed filters

  private double medianAngle; // the median angle from XYPoint[] smoothedArray
  private double perimeter; // the nuclear perimeter
  private double pathLength; // the angle path length
  private double feret; // the maximum diameter
  private double area; // the nuclear area

  // private XYPoint[] array; // the points from the polygon made from the input roi. Not currently used.
  // private XYPoint[] smoothedArray; // the interpolated points from the input polygon. Most calculations use this.

  private AngleProfile angleProfile; // new class to replace smoothedArray

  private XYPoint centreOfMass;

  private File sourceFile; // the image from which the nucleus came
  private File nucleusFolder; // the folder to store nucleus information
  private File profileLog; // unused. Store output if needed
  
  private Roi roi; // the original ROI
  // private Polygon polygon; // the ROI converted to a polygon; source of XYPoint[] array

  private ImagePlus sourceImage;

  private ArrayList<NuclearSignal> redSignals    = new ArrayList<NuclearSignal>(0); // an array to hold any signals detected
  private ArrayList<NuclearSignal> greenSignals  = new ArrayList<NuclearSignal>(0); // an array to hold any signals detected

  private FloatPolygon smoothedPolygon; // the interpolated polygon; source of XYPoint[] smoothedArray // can probably be removed

  private double[] distanceProfile;

  private double[][] distancesBetweenSignals;
  
  public Nucleus (Roi roi, File file, ImagePlus image) { // construct from an roi

    // assign main features
    this.roi             = roi;
    this.sourceImage     = image;
    this.sourceFile      = file;
    this.nucleusFolder   = new File(this.getDirectory()+File.separator+this.getImageNameWithoutExtension());
    
    if (!this.nucleusFolder.exists()) {
      try{
        this.nucleusFolder.mkdir();
      } catch(Exception e) {
        IJ.log("Failed to create directory: "+e);
        IJ.log("Attempt: "+this.nucleusFolder.toString());
      }
    }

    this.smoothedPolygon = roi.getInterpolatedPolygon(1,true);

    // calculate angle profile
    try{
      angleProfile = new AngleProfile(this.smoothedPolygon);
     } catch(Exception e){
       IJ.log("Cannot create angle profile: "+e);
     } 

     // calculate nuclear parameters
     try{
      this.calculateNuclearParameters();
     } catch(Exception e){
       IJ.log("Cannot calculate nuclear parameters: "+e);
     } 

     // calc distances around nucleus through CoM
     this.calculateDistanceProfile();

     try{
      IJ.saveAsTiff(image, this.nucleusFolder+File.separator+getImageName());
     } catch(Exception e){
        IJ.log("Error saving image: "+e);
     }

  }

  /*
    -----------------------
    Getters for basic values within nucleus
    -----------------------
  */
  public Roi getRoi(){
  	return this.roi;
  }

  public String getPath(){
    return this.sourceFile.getAbsolutePath();
  }

  public String getImageName(){
    return this.sourceFile.getName();
  }

  public String getImageNameWithoutExtension(){
    String extension = "";
    String trimmed = "";

    int i = this.getImageName().lastIndexOf('.');
    if (i > 0) {
        extension = this.getImageName().substring(i+1);
        trimmed   = this.getImageName().substring(0,i);
    }
    return trimmed;
  }

  public String getDirectory(){
    return this.sourceFile.getParent();
  }

  public String getPathWithoutExtension(){
    
    String extension = "";
    String trimmed = "";

    int i = this.getPath().lastIndexOf('.');
    if (i > 0) {
        extension = this.getPath().substring(i+1);
        trimmed = this.getPath().substring(0,i);
    }
    return trimmed;
  }  

  public int getNucleusNumber(){
    return this.nucleusNumber;
  }

  public String getPathAndNumber(){
    return this.sourceFile+File.separator+this.nucleusNumber;
  }

  public XYPoint getCentreOfMass(){
    return this.centreOfMass;
  }

  public NucleusBorderPoint getPoint(int i){
    return this.angleProfile.getBorderPoint(i);
  }

  public FloatPolygon getSmoothedPolygon(){
    return this.smoothedPolygon;
  }
  
  public double getArea(){
    return this.area;
  }

  public double getFeret(){
    return this.feret;
  }

  public double getPathLength(){
    return this.pathLength;
  }

  public double getPerimeter(){
    return this.perimeter;
  }

  public AngleProfile getAngleProfile(){
    return this.angleProfile;
  }

  public double[] getDistanceProfile(){
    return this.distanceProfile;
  }

  public int getLength(){
    return this.angleProfile.size();
  }

  public double[] getInteriorAngles(){
    return this.angleProfile.getInteriorAngles();
  }

  public double getMedianInteriorAngle(){
    return this.angleProfile.getMedianInteriorAngle();
  }

  public NucleusBorderPoint getBorderPoint(int i){
    return this.angleProfile.getBorderPoint(i);
  }


  /*
    -----------------------
    Get aggregate values
    -----------------------
  */
  public double getMaxX(){
  	double d = 0;
    for(int i=0;i<getLength();i++){
    	if(this.getBorderPoint(i).getX()>d){
      	d = this.getBorderPoint(i).getX();
    	}
    }
    return d;
  }

  public double getMinX(){
  	double d = getMaxX();
    for(int i=0;i<getLength();i++){
    	if(this.getBorderPoint(i).getX()<d){
      	d = this.getBorderPoint(i).getX();
      }
    }
    return d;
  }

  public double getMaxY(){
  	double d = 0;
    for(int i=0;i<getLength();i++){
    	if(this.getBorderPoint(i).getY()>d){
      	d = this.getBorderPoint(i).getY();
    	}
    }
    return d;
  }

  public double getMinY(){
  	double d = getMaxY();
    for(int i=0;i<getLength();i++){
    	if(this.getBorderPoint(i).getY()<d){
      	d = this.getBorderPoint(i).getY();
      }
    }
    return d;
  }

  public int getRedSignalCount(){
    return redSignals.size();
  }

  public int getGreenSignalCount(){
    return greenSignals.size();
  }

  /*
    -----------------------
    Set miscellaneous features
    -----------------------
  */

  public void setNucleusNumber(int n){
    this.nucleusNumber = n;
  }

  public void setPathLength(double d){
    this.pathLength = d;
  }

  /*
    -----------------------
    Process signals
    -----------------------
  */

  public ArrayList<NuclearSignal> getRedSignals(){
    return this.redSignals;
  }

  public ArrayList<NuclearSignal> getGreenSignals(){
    return this.greenSignals;
  }

  public void addRedSignal(NuclearSignal n){
    this.redSignals.add(n);
  }

  public void addGreenSignal(NuclearSignal n){
    this.greenSignals.add(n);
  }

   /*
    For each signal within the nucleus, calculate the distance to the nCoM
    and update the signal
  */
  public void calculateSignalDistances(){

    ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
    signals.add(redSignals);
    signals.add(greenSignals);

    for( ArrayList<NuclearSignal> signalGroup : signals ){

      if(signalGroup.size()>0){
        for(int i=0;i<signalGroup.size();i++){
          NuclearSignal n = signalGroup.get(i);

          double distance = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());
          n.setDistanceFromCoM(distance);
        }
      }
    }
  }

  /*
    Calculate the distance from the nuclear centre of
    mass as a fraction of the distance from the nuclear CoM, through the 
    signal CoM, to the nuclear border
  */
  public void calculateFractionalSignalDistances(){

    this.calculateClosestBorderToSignals();
    ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
    signals.add(redSignals);
    signals.add(greenSignals);

    for( ArrayList<NuclearSignal> signalGroup : signals ){
    
      if(signalGroup.size()>0){
        for(int i=0;i<signalGroup.size();i++){
          NuclearSignal n = signalGroup.get(i);

          // double distance = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());

          // get the line equation
          double eq[] = findLineEquation(n.getCentreOfMass(), this.getCentreOfMass());

          // using the equation, get the y postion on the line for each X point around the roi
          double minDeltaY = 100;
          int minDeltaYIndex = 0;
          double minDistanceToSignal = 1000;

          for(int j = 0; j<getLength();j++){
              double x = this.getBorderPoint(j).getX();
              double y = this.getBorderPoint(j).getY();
              double yOnLine = getYFromEquation(eq, x);
              double distanceToSignal = this.getBorderPoint(j).getLengthTo(n.getCentreOfMass()); // fetch


              double deltaY = Math.abs(y - yOnLine);
              // find the point closest to the line; this could find either intersection
              // hence check it is as close as possible to the signal CoM also
              if(deltaY < minDeltaY && distanceToSignal < minDistanceToSignal){
                minDeltaY = deltaY;
                minDeltaYIndex = j;
                minDistanceToSignal = distanceToSignal;
              }
          }
          NucleusBorderPoint borderPoint = this.getBorderPoint(minDeltaYIndex);
          double nucleusCoMToBorder = borderPoint.getLengthTo(this.getCentreOfMass());
          double signalCoMToNucleusCoM = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());
          double fractionalDistance = signalCoMToNucleusCoM / nucleusCoMToBorder;
          n.setFractionalDistanceFromCoM(fractionalDistance);
        }
      }
    }
  }

  /*
    Go through the signals in the nucleus, and find the point on
    the nuclear ROI that is closest to the signal centre of mass.
  */
  private void calculateClosestBorderToSignals(){

    ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
    signals.add(redSignals);
    signals.add(greenSignals);

    for( ArrayList<NuclearSignal> signalGroup : signals ){
    
      if(signalGroup.size()>0){
        for(int i=0;i<signalGroup.size();i++){
          NuclearSignal n = signalGroup.get(i);

          int minIndex = 0;
          double minDistanceToSignal = 1000;

          for(int j = 0; j<getLength();j++){
              XYPoint p = this.getBorderPoint(j);
              double distanceToSignal = p.getLengthTo(n.getCentreOfMass());

              // find the point closest to the CoM
              if(distanceToSignal < minDistanceToSignal){
                minIndex = j;
                minDistanceToSignal = distanceToSignal;
              }
          }
          // XYPoint borderPoint = smoothedArray[minIndex];
          n.setClosestBorderPoint(this.getBorderPoint(minIndex));
        }
      }
    }
  }

  public void exportSignalDistanceMatrix(){

    this.calculateDistancesBetweenSignals();

    File f = new File(this.nucleusFolder+File.separator+"signalDistanceMatrix.txt");
    if(f.exists()){
      f.delete();
    }

    int matrixSize = this.getRedSignalCount()+this.getGreenSignalCount();

    // Prepare the header line and append to file
    String outLine = "RED\t";
    for(int i=0;i<this.getRedSignalCount();i++){
      outLine = outLine + i + "\t";
    }
    outLine += "GREEN\t"; // distinguish red from green signals in headers
    
    for(int i=0;i<this.getGreenSignalCount();i++){
      outLine = outLine + i + "\t";
    }
    
    // IJ.append(outLine+"\n", f.getAbsolutePath());
    outLine += "\r\n";

    // for each row
    for(int i=0;i<this.getRedSignalCount();i++){
      // for each column of red
      outLine += i+"\t";
      for(int j=0; j<getRedSignalCount();j++){
        outLine += this.distancesBetweenSignals[i][j]+"\t";
      }
      outLine += "|\t";
      // for each column of green
      for(int j=0; j<getGreenSignalCount();j++){
        int k = j+this.getRedSignalCount();
        outLine += this.distancesBetweenSignals[i][k]+"\t";
      }
      // next line
      outLine += "\r\n";
    }
    // add separator line
    outLine += "GREEN\t";
    for(int i=0; i<matrixSize;i++){
      outLine += "--\t";
    }
     outLine += "\r\n";

    // add green signals
    // for each row
    for(int i=0;i<this.getGreenSignalCount();i++){

      outLine += i+"\t";
      int m = i+this.getRedSignalCount(); // offset for matrix

      // for each column of red
      for(int j=0; j<getRedSignalCount();j++){
        outLine += this.distancesBetweenSignals[m][j]+"\t";
      }
      outLine += "|\t";
      // for each column of green
      for(int j=0; j<getGreenSignalCount();j++){
        int k = j+this.getRedSignalCount();
        outLine += this.distancesBetweenSignals[m][k]+"\t";
      }
      // next line
      outLine += "\r\n";
    }
    IJ.append(outLine, f.getAbsolutePath());
  }

  public double[][] getSignalDistanceMatrix(){
    this.calculateDistancesBetweenSignals();
    return this.distancesBetweenSignals;
  }

  private void calculateDistancesBetweenSignals(){

    // create a matrix to hold the data
    // needs to be between every signal and every other signal, irrespective of colour
    int matrixSize = this.getRedSignalCount()+this.getGreenSignalCount();

    this.distancesBetweenSignals = new double[matrixSize][matrixSize];

    // go through the red signals
    for(int i=0;i<this.getRedSignalCount();i++){

      XYPoint aCoM = this.redSignals.get(i).getCentreOfMass();

      // compare to all red
      for(int j=0; j<getRedSignalCount();j++){

        XYPoint bCoM = this.redSignals.get(j).getCentreOfMass();
        this.distancesBetweenSignals[i][j] = aCoM.getLengthTo(bCoM);
      }

      // compare to all green
      for(int j=0; j<getGreenSignalCount();j++){

        int k = j+this.getRedSignalCount(); // offset for matrix

        XYPoint bCoM = this.greenSignals.get(j).getCentreOfMass();

        double distance = 
        this.distancesBetweenSignals[i][k] = aCoM.getLengthTo(bCoM);
      }
    }

    // go through the green signals
    for(int i=0;i<this.getGreenSignalCount();i++){

      int m = i+this.getRedSignalCount(); // offset for matrix

      XYPoint aCoM = this.greenSignals.get(i).getCentreOfMass();

      // and compare to all red
      for(int j=0; j<getRedSignalCount();j++){

        XYPoint bCoM = this.redSignals.get(j).getCentreOfMass();
        this.distancesBetweenSignals[m][j] = aCoM.getLengthTo(bCoM);
      }

      // compare to all green
      for(int j=0; j<getGreenSignalCount();j++){

        int k = j+this.getRedSignalCount(); // offset for matrix

        XYPoint bCoM = this.greenSignals.get(j).getCentreOfMass();

        double distance = 
        this.distancesBetweenSignals[m][k] = aCoM.getLengthTo(bCoM);
      }
    }
  }

  
  /*
    -----------------------
    Determine positions of points
    -----------------------
  */

  /*
    For two NucleusBorderPoints in a Nucleus, find the point that lies halfway between them
    Used for obtaining a consensus between potential tail positions
  */
  public int getPositionBetween(NucleusBorderPoint pointA, NucleusBorderPoint pointB){

    int a = 0;
    int b = 0;
    // find the indices that correspond on the array
    for(int i = 0; i<this.getLength(); i++){
        if(this.getPoint(i).overlaps(pointA)){
          a = i;
        }
        if(this.getPoint(i).overlaps(pointB)){
          b = i;
        }
    }
    // get the midpoint
    int mid = (int)Math.floor( (a+b) /2);
    return mid;
  }

  // For a position in the roi, draw a line through the CoM and get the intersection point
  public NucleusBorderPoint findOppositeBorder(NucleusBorderPoint p){

    int minDeltaYIndex = 0;
    double minAngle = 180;

    for(int i = 0; i<this.getLength();i++){
        double angle = findAngleBetweenXYPoints(p, this.getCentreOfMass(), this.getPoint(i));
        if(Math.abs(180 - angle) < minAngle){
          minDeltaYIndex = i;
          minAngle = 180 - angle;
        }
    }
    return this.getPoint(minDeltaYIndex);
  }

  /*
    Given three XYPoints, measure the angle a-b-c
      a   c
       \ /
        b
  */
  private double findAngleBetweenXYPoints(XYPoint a, XYPoint b, XYPoint c){

    float[] xpoints = { (float) a.getX(), (float) b.getX(), (float) c.getX()};
    float[] ypoints = { (float) a.getY(), (float) b.getY(), (float) c.getY()};
    PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);
   return roi.getAngle();
  }

 

  private void calculateDistanceProfile(){

    double[] profile = new double[this.getLength()];

    for(int i = 0; i<this.getLength();i++){

    		NucleusBorderPoint p   = this.getPoint(i);
    		NucleusBorderPoint opp = findOppositeBorder(p);

        profile[i] = p.getLengthTo(opp);
    }
    this.distanceProfile = profile;
  }

  /*
    Get measurements of the blue channel, using the nuclear roi
    Store the values.
  */
  private void calculateNuclearParameters(){

    ResultsTable results = calculateMeasurements(this.sourceImage, this.roi, this.BLUE_CHANNEL);
    XYPoint nucleusCoM = new XYPoint(results.getValue("XM", 0),  results.getValue("YM", 0) );
    this.centreOfMass = nucleusCoM;
    this.perimeter = results.getValue("Perim.",0);
    this.area = results.getValue("Area",0);
    this.feret = results.getValue("Feret",0);
  }

  /*
    Take an roi, image of interest, and channel.
    Calculate parameters of interest and return a ResultsTable.
  */
  private ResultsTable calculateMeasurements(ImagePlus imp, Roi roi, int channel){

    ChannelSplitter cs = new ChannelSplitter();
    ImagePlus[] channels = cs.split(imp);
    ImagePlus signalChannel = channels[channel];

    signalChannel.setRoi(roi);
    double feretDiameter = roi.getFeretsDiameter();

    ResultsTable rt = new ResultsTable();

    Analyzer an = new Analyzer(signalChannel, Analyzer.CENTER_OF_MASS | Analyzer.PERIMETER | Analyzer.AREA | Analyzer.FERET, rt);
    an.measure();
    return rt;
  }

  /*
    -----------------------
    Basic internal functions
    -----------------------
  */

  private double[] findLineEquation(XYPoint a, XYPoint b){

    // y=mx+c
    double deltaX = a.getX() - b.getX();
    double deltaY = a.getY() - b.getY();
      
    double m = deltaY / deltaX;
      
    // y - y1 = m(x - x1)
    double c = a.getY() -  ( m * a.getX() );
      
    // double testY = (m * position_2[0]) + c;
      
    // write("y = "+m+"x + "+c);
    // result=newArray(m, c);
    return new double[] { m, c };
  }

  private double getXFromEquation(double[] eq, double y){
    // x = (y-c)/m
    double x = (y - eq[1]) / eq[0];
    return x;
  }

  private double getYFromEquation(double[] eq, double x){
    // x = (y-c)/m
    double y = (eq[0] * x) + eq[1];
    return y;
  }

  private int wrapIndex(int i, int length){
    if(i<0)
      i = length + i; // if i = -1, in a 200 length array,  will return 200-1 = 199
    if(Math.floor(i / length)>0)
      i = i - ( ((int)Math.floor(i / length) )*length);

    if(i<0 || i>length){
      IJ.log("Warning: array out of bounds: "+i);
    }
    
    return i;
  }

  private double getXComponentOfAngle(double length, double angle){
    // cos(angle) = x / h
    // x = cos(a)*h
    double x = length * Math.cos(Math.toRadians(angle));
    return x;
  }

  private double getYComponentOfAngle(double length, double angle){
    double y = length * Math.sin(Math.toRadians(angle));
    return y;
  }

  private double getMin(double[] d){
    double min = getMax(d);
    for(int i=0;i<d.length;i++){
      if( d[i]<min)
        min = d[i];
    }
    return min;
  }

  private double getMax(double[] d){
    double max = 0;
    for(int i=0;i<d.length;i++){
      if( d[i]>max)
        max = d[i];
    }
    return max;
  }




  /*
    Print key data to the image log file
    Overwrites any existing log
  */   
  // public void printLogFile(String path){

  //   // String path = this.getPathWithoutExtension()+"\\"+this.getNucleusNumber()+".log";
  //   File f = new File(path);
  //   if(f.exists()){
  //     f.delete();
  //   }

  //   String outLine = "SX\tSY\tFX\tFY\tIA\tMA\tI_NORM\tI_DELTA\tI_DELTA_S\tBLOCK_POSITION\tBLOCK_NUMBER\tL_MIN\tL_MAX\tIS_MIDPOINT\tIS_BLOCK\tPROFILE_X\tDISTANCE_PROFILE\n";

  //   // IJ.append("SX\tSY\tFX\tFY\tIA\tMA\tI_NORM\tI_DELTA\tI_DELTA_S\tBLOCK_POSITION\tBLOCK_NUMBER\tL_MIN\tL_MAX\tIS_MIDPOINT\tIS_BLOCK\tPROFILE_X\tDISTANCE_PROFILE", path);
    
  //   for(int i=0;i<this.getLength();i++){

  //     double normalisedIAngle = smoothedArray[i].getInteriorAngle()-180;
  //     // double length = this.getLength();
  //     double normalisedX = ((double)i/(double)this.getLength())*100; // normalise to 100 length
      
  //     outLine = outLine + smoothedArray[i].getXAsInt()+"\t"+
  //                         smoothedArray[i].getYAsInt()+"\t"+
  //                         smoothedArray[i].getX()+"\t"+
  //                         smoothedArray[i].getY()+"\t"+
  //                         smoothedArray[i].getInteriorAngle()+"\t"+
  //                         smoothedArray[i].getMinAngle()+"\t"+
  //                         normalisedIAngle+"\t"+
  //                         smoothedArray[i].getInteriorAngleDelta()+"\t"+
  //                         smoothedArray[i].getInteriorAngleDeltaSmoothed()+"\t"+
  //                         smoothedArray[i].getPositionWithinBlock()+"\t"+
  //                         smoothedArray[i].getBlockNumber()+"\t"+
  //                         smoothedArray[i].isLocalMin()+"\t"+
  //                         smoothedArray[i].isLocalMax()+"\t"+
  //                         smoothedArray[i].isMidpoint()+"\t"+
  //                         smoothedArray[i].isBlock()+"\t"+
  //                         normalisedX+"\t"+
  //                         distanceProfile[i]+"\n";
  //   }
  //   IJ.append( outLine, path);
  // }
}