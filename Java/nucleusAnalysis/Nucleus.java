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

  private int nucleusNumber; // the number of the nucleus in the current image
  private int windowSize = 23; // default size, can be overridden if needed
  private int minimaCount; // the number of local minima detected in the array
  private int maximaCount; // the number of local minima detected in the array
  private int length;  // the length of the array; shortcut to this.array.length
  private int smoothLength = 0; // the length of the smoothed array; shortcut to this.smoothedArray.length
  private int minimaLookupDistance = 5; // the points ahead and behind to check when finding local minima and maxima
  private int blockCount = 0; // the number of delta blocks detected
  private int DELTA_WINDOW_MIN = 5; // the minimum number of points required in a delta block

  private int failureCode = 0; // stores a code to explain why the nucleus failed filters

  private double medianAngle; // the median angle from XYPoint[] smoothedArray
  private double perimeter; // the nuclear perimeter
  private double pathLength; // the angle path length
  private double feret; // the maximum diameter
  private double area; // the nuclear area

  private XYPoint[] array; // the points from the polygon made from the input roi. Not currently used.
  private XYPoint[] smoothedArray; // the interpolated points from the input polygon. Most calculations use this.

  private XYPoint centreOfMass;
  private XYPoint minFeretPoint1; // debugging tool used for identification of narrowest width across CoM. Stores the border point
  private XYPoint minFeretPoint2;
  
  private String imagePath; // the path to the image being analysed

  private boolean minimaCalculated = false; // has detectLocalMinima been run
  private boolean maximaCalculated = false; // has detectLocalMaxima been run
  private boolean anglesCalculated = false; // has makeAngleProfile been run
  private boolean offsetCalculated = false; // has calculateOffsets been run
  
  private Roi roi; // the original ROI
  private Polygon polygon; // the ROI converted to a polygon; source of XYPoint[] array

  private ArrayList<NuclearSignal> redSignals    = new ArrayList<NuclearSignal>(0); // an array to hold any signals detected
  private ArrayList<NuclearSignal> greenSignals  = new ArrayList<NuclearSignal>(0); // an array to hold any signals detected

  private FloatPolygon smoothedPolygon; // the interpolated polygon; source of XYPoint[] smoothedArray // can probably be removed

  private double[] distanceProfile;
  
  public Nucleus (Roi roi) { // construct from an roi

    // get the polygon from the roi
    this.roi = roi;
    this.polygon = roi.getPolygon();
    this.array = new XYPoint[this.polygon.npoints];
    this.length = this.array.length;
    for(int i=0; i<this.polygon.npoints; i++){
      array[i] = new XYPoint(this.polygon.xpoints[i],this.polygon.ypoints[i]);
    }
   
   try{
      this.smoothedPolygon = roi.getInterpolatedPolygon(1,true); // interpolate and smooth the roi, 1 pixel spacing

      this.smoothedArray = new XYPoint[this.smoothedPolygon.npoints];
      this.smoothLength = this.smoothedArray.length; // shortcult for functions

      for(int i=0; i<this.smoothedPolygon.npoints; i++){
        smoothedArray[i] = new XYPoint(this.smoothedPolygon.xpoints[i],this.smoothedPolygon.ypoints[i]);
      }
    } catch(Exception e){
      IJ.log("Cannot create ROI array: "+e);
    } 
  }

  public Roi getRoi(){
  	return this.roi;
  }

  public double getMaxX(){
  	double d = 0;
    for(int i=0;i<smoothLength;i++){
    	if(smoothedArray[i].getX()>d){
      	d = smoothedArray[i].getX();
    	}
    }
    return d;
  }

  public double getMinX(){
  	double d = getMaxX();
    for(int i=0;i<smoothLength;i++){
    	if(smoothedArray[i].getX()<d){
      	d = smoothedArray[i].getX();
      }
    }
    return d;
  }

  public double getMaxY(){
  	double d = 0;
    for(int i=0;i<smoothLength;i++){
    	if(smoothedArray[i].getY()>d){
      	d = smoothedArray[i].getY();
    	}
    }
    return d;
  }

  public double getMinY(){
  	double d = getMaxY();
    for(int i=0;i<smoothLength;i++){
    	if(smoothedArray[i].getY()<d){
      	d = smoothedArray[i].getY();
      }
    }
    return d;
  }

  /* 
    Fetch the angles in the smoothed array; will be ordered from the tip
  */
  public double[] getAngles(){
    double[] d = new double[this.smoothLength];
    for(int i=0;i<this.smoothLength;i++){
      d[i] = this.smoothedArray[i].getInteriorAngle();
    }
    return d;
  }

  public void addRedSignal(NuclearSignal n){
    this.redSignals.add(n);
  }

  public void addGreenSignal(NuclearSignal n){
    this.greenSignals.add(n);
  }

  public Polygon getPolygon(){
    return this.polygon;
  }

  public void setWindowSize(int i){
  	this.windowSize = i;
  }

  public int getWindowSize(){
  	return this.windowSize;
  }
  /* 
  Find the smoothed length of the array
  */
  public int getLength(){
  	return this.smoothLength;
  }

  public void setPath(String path){
    this.imagePath = path;
  }

  public void setNucleusNumber(int n){
    this.nucleusNumber = n;
  }

  public XYPoint getPoint(int i){
    return this.array[i];
  }

  public XYPoint getSmoothedPoint(int i){
    return this.smoothedArray[i];
  }

  public String getPath(){
    return this.imagePath;
  }

  public String getDirectory(){
    File f = new File(this.imagePath);
    return f.getParent();
  }

  public String getPathWithoutExtension(){
    
    String extension = "";
    String trimmed = "";

    int i = this.imagePath.lastIndexOf('.');
    if (i > 0) {
        extension = this.imagePath.substring(i+1);
        trimmed = this.imagePath.substring(0,i);
    }
    return trimmed;
  }  

  public FloatPolygon getSmoothedPolygon(){
    return this.smoothedPolygon;
  }

  public void setSmoothedPolygon(FloatPolygon p){
    this.smoothedPolygon = p;
  }

  public String getImageName(){
  	File f = new File(this.imagePath);
    return f.getName();
  }

  public int getNucleusNumber(){
    return this.nucleusNumber;
  }

  public String getPathAndNumber(){
    return this.imagePath+"\\"+this.nucleusNumber;
  }

  public XYPoint getCentreOfMass(){
    return this.centreOfMass;
  }

  public void setCentreOfMass(XYPoint p){
    this.centreOfMass = p;
  }

  public double getPerimeter(){
    return this.perimeter;
  }

  public void setPerimeter(double d){
    this.perimeter = d;
  }

  public double getArea(){
    return this.area;
  }

  public void setArea(double d){
    this.area = d;
  }

  public double getFeret(){
    return this.feret;
  }

  public void setFeret(double d){
    this.feret = d;
  }

  public double getPathLength(){
    return this.pathLength;
  }

  public void setPathLength(double d){
    this.pathLength = d;
  }

  public void setDistanceProfile( double[] d){
  	this.distanceProfile = d;
  }

  public double[] getDistanceProfile(){
  	return this.distanceProfile;
  }

  public ArrayList<NuclearSignal> getRedSignals(){
    return this.redSignals;
  }

  public ArrayList<NuclearSignal> getGreenSignals(){
    return this.greenSignals;
  }

  public int getRedSignalCount(){
    return redSignals.size();
  }

  public int getGreenSignalCount(){
    return greenSignals.size();
  }

  public void reverseArray(){

    XYPoint tmp;

    for (int i = 0; i < this.smoothLength / 2; i++) {
        tmp = this.smoothedArray[i];
        this.smoothedArray[i] = this.smoothedArray[this.smoothedArray.length - 1 - i];
        this.smoothedArray[this.smoothedArray.length - 1 - i] = tmp;
    }

  }

  public void flipXAroundPoint(XYPoint p){

    double xCentre = p.getX();

    for(int i = 0; i<this.smoothLength;i++){

      double dx = xCentre - this.smoothedArray[i].getX();
      double xNew = xCentre + dx;
      this.smoothedArray[i].setX(xNew);
    }

  }

  /*
    For two XYPoints in a Nucleus, find the point that lies halfway between them
    Used for obtaining a consensus between potential tail positions
  */
  public int getPositionBetween(XYPoint pointA, XYPoint pointB){

    int a = 0;
    int b = 0;
    // find the indices that correspond on the array
    for(int i = 0; i<this.smoothLength; i++){
        if(this.smoothedArray[i].overlaps(pointA)){
          a = i;
        }
        if(this.smoothedArray[i].overlaps(pointB)){
          b = i;
        }
    }
    // get the midpoint
    int mid = (int)Math.floor( (a+b) /2);
    return mid;
  }

	public float[] getProfileArray(){

		float[] newArray = new float[this.smoothLength+1];
		for(int i=0; i<this.smoothLength;i++) {
			float normalisedX = ((float)i/(float)this.smoothLength)*100; // normalise to 100 length
			newArray[i] = normalisedX;
		}
		newArray[this.smoothLength] = newArray[0];
		return newArray;
	}

  public double[] getProfileAngles(){

    double[] d = new double[this.smoothLength]; // allow the first and last element to be duplicated
    for(int i=0;i<this.smoothLength;i++){
      d[i] = this.smoothedArray[i].getInteriorAngle();
    }
    return d;
  }


  // For a position in the roi, draw a line through the CoM and get the intersection point
  public XYPoint findOppositeBorder(XYPoint p){

    int minDeltaYIndex = 0;
    double minAngle = 180;

    for(int i = 0; i<smoothLength;i++){

        double angle = findAngleBetweenXYPoints(p, this.getCentreOfMass(), smoothedArray[i]);

        if(Math.abs(180 - angle) < minAngle){
          minDeltaYIndex = i;
          minAngle = 180 - angle;
        }
    }
    return smoothedArray[minDeltaYIndex];
  }

  /*
    Given three XYPoints, measure the angle a-b-c
      a   c
       \ /
        b
  */
  public double findAngleBetweenXYPoints(XYPoint a, XYPoint b, XYPoint c){

    float[] xpoints = { (float) a.getX(), (float) b.getX(), (float) c.getX()};
    float[] ypoints = { (float) a.getY(), (float) b.getY(), (float) c.getY()};
    PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);
   return roi.getAngle();
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

  public int wrapIndex(int i, int length){
    if(i<0)
      i = length + i; // if i = -1, in a 200 length array,  will return 200-1 = 199
    if(Math.floor(i / length)>0)
      i = i - ( ((int)Math.floor(i / length) )*length);

    if(i<0 || i>length){
      IJ.log("Warning: array out of bounds: "+i);
    }
    
    return i;
  }

  /*
    Change the smoothed array order to put the selected index at the beginning
    only works for smoothed array - indexes are different for normal array
    Input: int the index to move to the start
  */
  public void moveIndexToArrayStart(int i){

    // copy the array to refer to
    XYPoint[] tempSmooth = new XYPoint[this.smoothLength];
    System.arraycopy(this.smoothedArray, 0, tempSmooth, 0 , this.smoothLength);
   
    System.arraycopy(tempSmooth, i, this.smoothedArray, 0 , this.smoothLength-i); // copy over the i to end values
    System.arraycopy(tempSmooth, 0, this.smoothedArray, this.smoothLength-i, i); // copy over index 0 to i
   
    if(tempSmooth.length != this.smoothedArray.length){
      IJ.log("    Unequal array size");
    }     
  }

  /*
    For a given index in the smoothed angle array: 
      Draw a line between this point, and the points <window> ahead and <window> behind.
      Measure the angle between these points and store as minAngle
      Determine if the angle lies inside or outside the shape. Adjust the angle to always report the interior angle.
      Store interior angle as interiorAngle.
    Automatically wraps the array.
    Input: int index in the array, int window the points ahead and behind to look
  */
  public void findAngleBetweenPoints(int index, int window){

    // wrap the array
    int indexBefore = wrapIndex(index - window, this.smoothLength);
    int indexAfter  = wrapIndex(index + window, this.smoothLength);

    XYPoint pointBefore = this.getSmoothedPoint(indexBefore);
    XYPoint pointAfter = this.getSmoothedPoint(indexAfter);
    XYPoint point = this.getSmoothedPoint(index);


    double angle = findAngleBetweenXYPoints(pointBefore, point, pointAfter);

    // find the halfway point between the first and last points.
    // is this within the roi?
    // if yes, keep min angle as interior angle
    // if no, 360-min is interior
    double midX = (pointBefore.getX()+pointAfter.getX())/2;
    double midY = (pointBefore.getY()+pointAfter.getY())/2;

    this.smoothedArray[index].setMinAngle(angle);
    if(this.smoothedPolygon.contains( (float) midX, (float) midY)){
      this.smoothedArray[index].setInteriorAngle(angle);
    } else {
      this.smoothedArray[index].setInteriorAngle(360-angle);
    }
  }


  // Make an angle array for the current coordinates in the XYPoint array
  // Will need to be rerun on each index order change
  public void makeAngleProfile(){
  	// go through points
  	// find angle
  	// assign to angle array

    for(int i=0; i<this.smoothLength;i++){

      // use a window size of 25 for now
      findAngleBetweenPoints(i, this.getWindowSize());
      // this.smoothedArray[i].setIndex(i);
    }
    this.calculateMedianAngle();
    this.anglesCalculated = true;
  }

  /*
    Retrieves an XYPoint array of the points designated as local minima.
    If the local minimum detection has not yet been run, calculates local minima
  */
  public XYPoint[] getLocalMinima(){

    if(!this.minimaCalculated){
      this.detectLocalMinima();
    }

    XYPoint[] newArray = new XYPoint[this.minimaCount];
    int j = 0;

    try{
      for (int i=0; i<this.smoothLength; i++) {
        if(this.smoothedArray[i].isLocalMin()){
          newArray[j] = this.smoothedArray[i];
          j++;
        }
      }
    } catch(Exception e){
      IJ.log("    Error in minima detection: "+e);
    }

    // IJ.log("    Detected "+j+" local minima with lookup size "+this.minimaLookupDistance);
    return newArray;
  }

  /*
    Retrieves an XYPoint array of the points designated as local maxima.
    If the local maximum detection has not yet been run, calculates local maxima
  */
  public XYPoint[] getLocalMaxima(){

    if(!this.maximaCalculated){
      this.detectLocalMaxima();
    }

    XYPoint[] newArray = new XYPoint[this.maximaCount];
    int j = 0;
    try{  
      for (int i=0; i<this.smoothLength; i++) {
        if(this.smoothedArray[i].isLocalMax()){
          newArray[j] = this.smoothedArray[i];
          j++;
        }
      }
    } catch(Exception e){
      IJ.log("    Error in maxima detection: "+e);
    }

    // IJ.log("    Detected "+j+" local maxima with lookup size "+this.minimaLookupDistance);
    return newArray;
  }

  /*
    For each point in the smoothed angle array, test for a local minimum.
    The angles of the points <minimaLookupDistance> ahead and behind are checked.
    Each should be greater than the angle before.
    One exception is allowed, to account for noisy data.
  */
  private void detectLocalMinima(){
    // go through angle array (with tip at start)
    // look at 1-2-3-4-5 points ahead and behind.
    // if all greater, local minimum
    
    double[] prevAngles = new double[this.minimaLookupDistance]; // slots for previous angles
    double[] nextAngles = new double[this.minimaLookupDistance]; // slots for next angles

    int count = 0;

    for (int i=0; i<this.smoothLength; i++) { // for each position in sperm

      // go through each lookup position and get the appropriate angles
      for(int j=0;j<prevAngles.length;j++){

        int prev_i = i-(j+1); // the index j+1 before i
        int next_i = i+(j+1); // the index j+1 after i

        // handle beginning of array - wrap around
        if(prev_i < 0){
          prev_i = this.smoothLength + prev_i; // length of array - appropriate value
        }

        // handle end of array - wrap
        if(next_i >= this.smoothLength){
          next_i = next_i - this.smoothLength;
        }

        // fill the lookup array
        prevAngles[j] = this.smoothedArray[prev_i].getInteriorAngle();
        nextAngles[j] = this.smoothedArray[next_i].getInteriorAngle();
      }
      
      // with the lookup positions, see if minimum at i
      // return a 1 if all higher than last, 0 if not
      // prev_l = 0;
      boolean ok = true;
      for(int l=0;l<prevAngles.length;l++){

        // for the first position in prevAngles, compare to the current index
        if(l==0){
          if(prevAngles[l] < this.smoothedArray[i].getInteriorAngle() || nextAngles[l] < this.smoothedArray[i].getInteriorAngle()){
            ok = false;
          }
        } else { // for the remainder of the positions in prevAngles, compare to the prior prevAngle
          
          if(prevAngles[l] < prevAngles[l-1] || nextAngles[l] < nextAngles[l-1]){
            ok = false;
          }
        }

        if( this.smoothedArray[i].getInteriorAngle()-180 > -20){ // ignore any values close to 180 degrees
          ok = false;
        }
      }

      if(ok){
        count++;
      }

      // put oks into array to put into multiarray
      smoothedArray[i].setLocalMin(ok);
    }
    this.minimaCalculated = true;
    this.minimaCount =  count;
  }

  /*
    For each point in the smoothed angle array, test for a local maximum.
    The angles of the points <minimaLookupDistance> ahead and behind are checked.
      *Note that this uses the same variable as detectLocalMinima()*
    Each should be lower than the angle before.
    One exception is allowed, to account for noisy data.
  */
  private void detectLocalMaxima(){
    // go through interior angle array (with tip at start)
    // look at 1-2-3-4-5 points ahead and behind.
    // if all lower, local maximum
    
    double[] prevAngles = new double[this.minimaLookupDistance]; // slots for previous angles
    double[] nextAngles = new double[this.minimaLookupDistance]; // slots for next angles

    int count = 0;

    for (int i=0; i<this.smoothLength; i++) { // for each position in sperm

      // go through each lookup position and get the appropriate angles
      for(int j=0;j<prevAngles.length;j++){

        int prev_i = i-(j+1);
        int next_i = i+(j+1);

        // handle beginning and end of array - wrap around
        if(prev_i < 0){
          prev_i = this.smoothLength + prev_i;
        }
        if(next_i >= this.smoothLength){
          next_i = next_i - this.smoothLength;
        }

        // fill the lookup array
        prevAngles[j] = this.smoothedArray[prev_i].getInteriorAngle();
        nextAngles[j] = this.smoothedArray[next_i].getInteriorAngle();
      }
      
      // with the lookup positions, see if maximum at i
      // return true if all lower than last, false if not
      // prev_l = 0;
      boolean ok = true;
      boolean ignoreOne = true; // allow a single value to be out of place (account for noise in pixel data)
      for(int l=0;l<prevAngles.length;l++){

        // not ok if the outer entries are not higher than inner entries
        if(l==0){
          if( prevAngles[l] > this.smoothedArray[i].getInteriorAngle() ||
                 nextAngles[l] > this.smoothedArray[i].getInteriorAngle() ){

            if( !ignoreOne){
              ok = false;
            }
            
            ignoreOne = false;
          }
        } else {
          
          if(  prevAngles[l] > prevAngles[l-1] || nextAngles[l] > nextAngles[l-1] )  {
            
            if( !ignoreOne){
              ok = false;
            }
            
            ignoreOne = false;
          }
        }

        // we want the angle of a maximum to be higher than the median angle of the array set
        // if( this.smoothedArray[i].getInteriorAngle()-180 < -10){
        if( this.smoothedArray[i].getInteriorAngle() < this.medianAngle){
          ok = false;
        }
      }

      if(ok){
        count++;
      }

      // put oks into array to put into multiarray
      smoothedArray[i].setLocalMax(ok);
    }
    this.maximaCalculated = true;
    this.maximaCount =  count;
  }


  /*
    For the interior angles in the smoothed angle array:
      Calculate the median angle in the array.
    Stores in medianAngle
  */    
  public void calculateMedianAngle() {

      double[] m = new double[this.smoothLength];
      for(int i = 0; i<this.smoothLength; i++){
        m[i] = this.smoothedArray[i].getInteriorAngle();
      }
      Arrays.sort(m);

      int middle = m.length/2;
      if (m.length%2 == 1) {
          this.medianAngle = m[middle];
      } else {
          this.medianAngle = (m[middle-1] + m[middle]) / 2.0;
      }
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
    
  //   for(int i=0;i<this.smoothLength;i++){

  //     double normalisedIAngle = smoothedArray[i].getInteriorAngle()-180;
  //     // double length = this.smoothLength;
  //     double normalisedX = ((double)i/(double)this.smoothLength)*100; // normalise to 100 length
      
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

  public double[] getInteriorAngles(){

    double[] ypoints = new double[this.smoothLength];

    for(int j=0;j<ypoints.length;j++){
        ypoints[j] = this.smoothedArray[j].getInteriorAngle();
    }
    return ypoints;
  }

  public void calculateSignalDistances(){

    ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
    signals.add(redSignals);
    signals.add(greenSignals);

    for( ArrayList<NuclearSignal> signalGroup : signals ){

    	if(signalGroup.size()>0){
        for(int i=0;i<signalGroup.size();i++){
        	NuclearSignal n = signalGroup.get(i);

        	double distance = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());
        	n.setDistance(distance);
        }
      }
    }
  }


  public double[] findLineEquation(XYPoint a, XYPoint b){

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

  public double getXFromEquation(double[] eq, double y){
    // x = (y-c)/m
    double x = (y - eq[1]) / eq[0];
    return x;
  }

  public double getYFromEquation(double[] eq, double x){
    // x = (y-c)/m
    double y = (eq[0] * x) + eq[1];
    return y;
  }

  /*
    Calculate the distance from the nuclear centre of
    mass as a fraction of the distance from the nuclear CoM, through the 
    signal CoM, to the nuclear border
  */
  public void calculateFractionalSignalDistances(){

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

          for(int j = 0; j<smoothLength;j++){
              double x = smoothedArray[j].getX();
              double y = smoothedArray[j].getY();
              double yOnLine = getYFromEquation(eq, x);
              double distanceToSignal = smoothedArray[j].getLengthTo(n.getCentreOfMass()); // fetch


              double deltaY = Math.abs(y - yOnLine);
              // find the point closest to the line; this could find either intersection
              // hence check it is as close as possible to the signal CoM also
              if(deltaY < minDeltaY && distanceToSignal < minDistanceToSignal){
                minDeltaY = deltaY;
                minDeltaYIndex = j;
                minDistanceToSignal = distanceToSignal;
              }
          }
          XYPoint borderPoint = smoothedArray[minDeltaYIndex];
          double nucleusCoMToBorder = borderPoint.getLengthTo(this.getCentreOfMass());
          double signalCoMToNucleusCoM = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());
          double fractionalDistance = signalCoMToNucleusCoM / nucleusCoMToBorder;
          n.setFractionalDistance(fractionalDistance);
        }
      }
    }
  }

  /*
    Go through the signals in the nucleus, and find the point on
    the nuclear ROI that is closest to the signal centre of mass.
  */
  public void calculateClosestBorderToSignal(){

    ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
    signals.add(redSignals);
    signals.add(greenSignals);

    for( ArrayList<NuclearSignal> signalGroup : signals ){
    
      if(signalGroup.size()>0){
        for(int i=0;i<signalGroup.size();i++){
          NuclearSignal n = signalGroup.get(i);

          int minIndex = 0;
          double minDistanceToSignal = 1000;

          for(int j = 0; j<smoothLength;j++){
              XYPoint p = smoothedArray[j];
              double distanceToSignal = p.getLengthTo(n.getCentreOfMass());

              // find the point closest to the CoM
              if(distanceToSignal < minDistanceToSignal){
                minIndex = j;
                minDistanceToSignal = distanceToSignal;
              }
          }
          // XYPoint borderPoint = smoothedArray[minIndex];
          n.setClosestBorderPoint(smoothedArray[minIndex]);
        }
      }
    }
  }

  public void calculateDistanceProfile(){

    double[] profile = new double[smoothLength];

    for(int i = 0; i<smoothLength;i++){

    		XYPoint p = smoothedArray[i];
    		XYPoint opp = findOppositeBorder(p);

        profile[i] = p.getLengthTo(opp);
    }
    this.setDistanceProfile(profile);
  }
}