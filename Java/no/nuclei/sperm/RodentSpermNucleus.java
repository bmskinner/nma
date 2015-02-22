/*
  -----------------------
  RODENT SPERM NUCLEUS CLASS
  -----------------------
  Contains the variables for storing a rodentsperm nucleus.
  Sperm have a hook, hump and tip, hence can be oriented
  in two axes.
*/  
package no.nuclei.sperm;

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
import no.nuclei.*;

public class RodentSpermNucleus
	extends SpermNucleus
{
  private static final int MAX_INTERIOR_ANGLE_TO_CALL_TIP = 110;

  private int offsetForTail = 0;

  private int tailIndex; // the index in the smoothedArray that has been designated the tail
  private int tipIndex; // the index in the smoothedArray that has been designated the tip [should be 0]

  private double differenceToMedianCurve; // store the difference between curves

  private ArrayList<NucleusBorderPoint> intialSpermTails = new ArrayList<NucleusBorderPoint>(0); // holds the points considered to be sperm tails before filtering

  private NucleusBorderPoint spermTip; // differs from the headpoint, which in other sperm is opposite the tail
  private NucleusBorderPoint intersectionPoint; // the point through the centre of mass directly opposite the sperm tail. Used for dividing hook/hump Rois
  private NucleusBorderPoint initialConsensusTail; // the point initially chosen as the tail. Used to draw tail position box plots
  private NucleusBorderPoint minFeretPoint1; // debugging tool used for identification of narrowest width across CoM. Stores the border point
  private NucleusBorderPoint minFeretPoint2;
  
  private FloatPolygon hookRoi;
  private FloatPolygon humpRoi;

  private ArrayList<Double> normalisedXPositionsFromTip  = new ArrayList<Double>(0); // holds the x values only after normalisation
  private ArrayList<Double> normalisedYPositionsFromTail = new ArrayList<Double>(0);
  private ArrayList<Double> normalisedXPositionsFromTail = new ArrayList<Double>(0);
  private ArrayList<Double> rawXPositionsFromTail        = new ArrayList<Double>(0);
  private ArrayList<Double> rawXPositionsFromTip         = new ArrayList<Double>(0);

  // Requires a sperm nucleus object to construct from
  public RodentSpermNucleus(Nucleus n){
  	super(n);
    this.findPointsAroundBorder();
    this.performNormalisation();
  }

  /*
    Identify key points: tip, estimated tail position
  */
  private void findPointsAroundBorder(){
    

    // find tip - use the least angle method
    NucleusBorderPoint spermTip = this.getAngleProfile().getPointWithMinimumAngle();
    int tipIndex = this.getAngleProfile().getIndexOfPoint(spermTip);
    this.getAngleProfile().moveIndexToArrayStart(tipIndex);
    this.setSpermTip(spermTip);

    // decide if the profile is right or left handed; flip if needed
    if(!this.isProfileOrientationOK()){
      this.getAngleProfile().reverseAngleProfile();
    }
    
    /*
      Find the tail point using multiple independent methods. 
      Find a consensus point

      Method 1: Use the list of local minima to detect the tail corner
                This is the corner furthest from the tip.
                Can be confused as to which side of the sperm head is chosen
    */  
    NucleusBorderPoint spermTail2 = findTailPointFromMinima();
    this.addTailEstimatePosition(spermTail2);

    /*
      Method 2: Look at the 2nd derivative - rate of change of angles
                Perform a 5win average smoothing of the deltas
                Count the number of consecutive >1 degree blocks
                Wide block far from tip = tail
    */  
    NucleusBorderPoint spermTail3 = this.findTailFromDeltas();
    this.addTailEstimatePosition(spermTail3);

    /*    
      Method 3: Find the narrowest diameter around the nuclear CoM
                Draw a line orthogonal, and pick the intersecting border points
                The border furthest from the tip is the tail
    */  
    NucleusBorderPoint spermTail1 = this.findTailByNarrowestWidthMethod();
    this.addTailEstimatePosition(spermTail1);


    /*
      Given distinct methods for finding a tail,
      take a position between them on roi
    */
    int consensusTailIndex = this.getPositionBetween(spermTail2, spermTail3);
    NucleusBorderPoint consensusTail = this.getBorderPoint(consensusTailIndex);
    consensusTailIndex = this.getPositionBetween(consensusTail, spermTail1);
    this.tailIndex = consensusTailIndex;
    this.setInitialConsensusTail(consensusTail);
    this.setSpermTail(consensusTail);
    
  }

  public void performNormalisation(){
    double pathLength = 0;
    double normalisedTailIndex = ((double)this.getTailIndex()/(double)this.getLength())*100;

    XYPoint prevPoint = new XYPoint(0,0);
     
    for (int i=0; i<this.getLength();i++ ) {
        double normalisedX = ((double)i/(double)this.getLength())*100; // normalise to 100 length
        double rawXFromTail = (double)i - (double)this.getTailIndex(); // offset the raw array based on the calculated tail position

        this.normalisedXPositionsFromTip.add(normalisedX);
        this.rawXPositionsFromTail.add(rawXFromTail);
        this.rawXPositionsFromTip.add( (double)i); 

        // calculate the path length
        XYPoint thisPoint = new XYPoint(normalisedX,this.getBorderPoint(i).getInteriorAngle());
        pathLength += thisPoint.getLengthTo(prevPoint);
        prevPoint = thisPoint;
    }
    this.setPathLength(pathLength);
  }

  /*
    -----------------------
    Get and set sperm nucleus features
    -----------------------
  */

  public double[] getNormalisedXPositionsFromTip(){
    double[] d = new double[normalisedXPositionsFromTip.size()];
    for(int i=0;i<normalisedXPositionsFromTip.size();i++){
      d[i] = normalisedXPositionsFromTip.get(i);
    }
    return d;
  }

  public double[] getNormalisedYPositionsFromTail(){
    double[] d = new double[normalisedYPositionsFromTail.size()];
    for(int i=0;i<normalisedYPositionsFromTail.size();i++){
      d[i] = normalisedYPositionsFromTail.get(i);
    }
    return d;
  }

  public double[] getNormalisedXPositionsFromTail(){
    double[] d = new double[normalisedXPositionsFromTail.size()];
    for(int i=0;i<normalisedXPositionsFromTail.size();i++){
      d[i] = normalisedXPositionsFromTail.get(i);
    }
    return d;
  }

  public double[] getRawXPositionsFromTail(){
    double[] d = new double[rawXPositionsFromTail.size()];
    for(int i=0;i<rawXPositionsFromTail.size();i++){
      d[i] = rawXPositionsFromTail.get(i);
    }
    return d;
  }

  public double[] getRawXPositionsFromTip(){
    double[] d = new double[rawXPositionsFromTip.size()];
    for(int i=0;i<rawXPositionsFromTip.size();i++){
      d[i] = rawXPositionsFromTip.get(i);
    }
    return d;
  }

  public double getMaxRawXFromTail(){
    double d = 0;
    for(int i=0;i<rawXPositionsFromTail.size();i++){
      if(rawXPositionsFromTail.get(i) > d){
        d = rawXPositionsFromTail.get(i);
      }
    }
    return d;
  }

  public double getMinRawXFromTail(){
    double d = 0;
    for(int i=0;i<rawXPositionsFromTail.size();i++){
      if(rawXPositionsFromTail.get(i) < d){
        d = rawXPositionsFromTail.get(i);
      }
    }
    return d;
  }

  public double getMaxRawXFromTip(){
    double d = 0;
    for(int i=0;i<rawXPositionsFromTip.size();i++){
      if(rawXPositionsFromTip.get(i) > d){
        d = rawXPositionsFromTip.get(i);
      }
    }
    return d;
  }

  public double getMinRawXFromTip(){
    double d = 0;
    for(int i=0;i<rawXPositionsFromTip.size();i++){
      if(rawXPositionsFromTip.get(i) < d){
        d = rawXPositionsFromTip.get(i);
      }
    }
    return d;
  }

  public double getDifferenceToMedianCurve(){
    return this.differenceToMedianCurve;
  }

  public void setDifferenceToMedianCurve(double d){
    this.differenceToMedianCurve = d;
  }

  public NucleusBorderPoint getSpermTip(){
    return this.spermTip;
  }

  public void setSpermTip(NucleusBorderPoint p){
    this.spermTip = p;
  }

  public void setInitialConsensusTail(NucleusBorderPoint p){
    this.initialConsensusTail = p;
  }

  public NucleusBorderPoint getInitialConsensusTail(){
    return this.initialConsensusTail;
  }


  public int getTailIndex(){
    return this.tailIndex;
  }

  public void setTailIndex(int i){
    this.tailIndex = i;
  }

  public int getOffsetForTail(){
    return this.offsetForTail;
  }

  public void setOffsetForTail(int i){
    this.offsetForTail = i;
  }


  public void addTailEstimatePosition(NucleusBorderPoint p){
    this.intialSpermTails.add(p);
  }

  /*
    -----------------------
    Qualities of a sperm head
    -----------------------
  */

  public boolean isHookSide(XYPoint p){
    if(hookRoi.contains( (float)p.getX(), (float)p.getY() ) ){
      return true;
    } else { 
      return false;
    }
  }

  public boolean isHumpSide(XYPoint p){
    if(humpRoi.contains( (float)p.getX(), (float)p.getY() ) ){
      return true;
    } else { 
      return false;
    }
  }    

  /*
    Checks if the smoothed array nuclear shape profile has the acrosome to the rear of the array
    If acrosome is at the beginning:
      returns true
    else returns false
  */
  public boolean isProfileOrientationOK(){

    double maxAngle = 0.0;
    int maxIndex = 0;
    for(int i=0; i<this.getLength();i++){

        double angle = this.getBorderPoint(i).getInteriorAngle();
        if(angle>maxAngle){
          maxAngle = angle;
          maxIndex = i;
        }
    }

    if(this.getLength() - maxIndex < maxIndex){ // if the maxIndex is closer to the end than the beginning
      return false;
    } else{ 
      return true;
    }
  }

  /*
    -----------------------
    Methods for detecting the tail
    -----------------------
  */

  /*
    Detect the tail based on a list of local minima in an NucleusBorderPoint array.
    The putative tail is the point furthest from the sum of the distances from the CoM and the tip
  */
  public NucleusBorderPoint findTailPointFromMinima(){
  
    // we cannot be sure that the greatest distance between two points will be the endpoints
    // because the hook may begin to curve back on itself. We supplement this basic distance with
    // the distances of each point from the centre of mass. The points with the combined greatest
    // distance are both far from each other and far from the centre, and are a more robust estimate
    // of the true ends of the signal
    double tipToCoMDistance = this.getSpermTip().getLengthTo(this.getCentreOfMass());
    NucleusBorderPoint[] array = this.getAngleProfile().getLocalMinima();

    double maxDistance = 0;
    NucleusBorderPoint tail = this.getSpermTip();

    for(NucleusBorderPoint a : array){
            
      double distanceAcrossCoM = tipToCoMDistance + this.getCentreOfMass().getLengthTo(a);
      double distanceBetweenEnds = this.getSpermTip().getLengthTo(a);
      
      double totalDistance = distanceAcrossCoM + distanceBetweenEnds;

      if(totalDistance > maxDistance){
        maxDistance = totalDistance;
        tail = a;
      }
    }
    return tail;
  }


  /*
    This is a method for finding a tail point independent of local minima:
      Find the narrowest diameter around the nuclear CoM
      Draw a line orthogonal, and pick the intersecting border points
      The border furthest from the tip is the tail
  */
  public NucleusBorderPoint findTailByNarrowestWidthMethod(){

    // Find the narrowest point around the CoM
    // For a position in teh roi, draw a line through the CoM to the intersection point
    // Measure the length; if < min length..., store equation and border(s)

    double minDistance = this.getFeret();
    NucleusBorderPoint reference = this.getSpermTip();

    for(int i=0;i<this.getLength();i++){

      NucleusBorderPoint p = this.getBorderPoint(i);
      NucleusBorderPoint opp = this.findOppositeBorder(p);
      double distance = p.getLengthTo(opp);

      if(distance<minDistance){
        minDistance = distance;
        reference = p;
      }
    }
    this.minFeretPoint1 = reference;
    this.minFeretPoint2 = this.findOppositeBorder(reference);
    
    // Using the point, draw a line from teh CoM to the border. Measure the angle to an intersection point
    // if close to 90, and the distance to the tip > CoM-tip, keep the point
    // return the best point
    double difference = 90;
    NucleusBorderPoint tail = new NucleusBorderPoint(0,0);
    for(int i=0;i<this.getLength();i++){

      NucleusBorderPoint p = this.getBorderPoint(i);
      double angle = findAngleBetweenXYPoints(reference, this.getCentreOfMass(), p);
      if(  Math.abs(90-angle)<difference && p.getLengthTo(this.getSpermTip()) > this.getCentreOfMass().getLengthTo( this.getSpermTip() ) ){
        difference = 90-angle;
        tail = p;
      }
    }
    return tail;
  }

  /*
    Go through the deltas marked as consecutive blocks
    Find the midpoints of each block
    Return the point furthest from the tip
  */
  public NucleusBorderPoint findTailFromDeltas(){

    // get the midpoint of each block
    ArrayList<NucleusBorderPoint> results = new ArrayList<NucleusBorderPoint>(0);
    int maxIndex = 0;
  
    // remember that block 0 is not assigned; start from 1
    try{
      for(int i=1; i<this.getAngleProfile().getBlockCount();i++){

        // number of points in each block
        NucleusBorderPoint[] points = this.getAngleProfile().getBlockOfBorderPoints(i);
        for(NucleusBorderPoint p : points){
          if(p.isMidpoint()){ // will ignore any blocks without a midpoint established - <2 members
            results.add(p);
          }
        }
      }
    } catch(Exception e){
      IJ.log("    Error in finding midpoints: findTailFromDeltas(): "+e);
    }
    
    NucleusBorderPoint tail = new NucleusBorderPoint(0,0);
    try{
      // go through the midpoints, get the max distance from tip
      double maxLength = 0;
      
      for( NucleusBorderPoint p : results){
        // NucleusBorderPoint p = (NucleusBorderPoint)o;
        if(p.getLengthTo(this.getSpermTip()) > maxLength){
          maxLength = p.getLengthTo(this.getSpermTip());
          tail = p;
        }
      }
    } catch(Exception e){
      IJ.log("    Error in finding lengths: findTailFromDeltas(): "+e);
    }
    return tail;
  }

  /*
    To create the normalised tail-centred index, we want to take the 
    normalised tip-centred index, and move the tail index position to 
    the start. 
  */
  public void createNormalisedYPositionsFromTail(){

    double[] tipCentredAngles = this.getAngleProfile().getAngleArray();
    double[] tipCentredXPositions = this.getNormalisedXPositionsFromTip();
    int tailIndex = this.getTailIndex();

    double[] tempArray = new double[tipCentredAngles.length];

    System.arraycopy(tipCentredAngles, tailIndex, tempArray, 0 , tipCentredAngles.length-tailIndex); // copy over the tailIndex to end values
    System.arraycopy(tipCentredAngles, 0, tempArray, tipCentredAngles.length-tailIndex, tailIndex); // copy over index 0 to tailIndex

    double[] tempXArray = new double[tipCentredAngles.length];
    System.arraycopy(tipCentredXPositions, tailIndex, tempXArray, 0 , tipCentredAngles.length-tailIndex); // copy over the tailIndex to end values
    System.arraycopy(tipCentredXPositions, 0, tempXArray, tipCentredAngles.length-tailIndex, tailIndex); // copy over index 0 to tailIndex


    for(int i=0; i<this.getLength();i++){
        this.normalisedYPositionsFromTail.add(tempArray[i]);
        this.normalisedXPositionsFromTail.add(tempXArray[i]);
    }
  }


  /*
    For the given nucleus index:
    Go through the raw X positions centred on the tail, 
    and apply the calculated offset.
  */
  public double[] createOffsetRawProfile(){

    double offset = this.offsetForTail;

    double[] xRawCentredOnTail = this.getRawXPositionsFromTail();
    double[] offsetX = new double[xRawCentredOnTail.length];

    for(int j=0;j<xRawCentredOnTail.length;j++){
        offsetX[j] = xRawCentredOnTail[j]+offset;
    }
    return offsetX;
  }

  /*
    -----------------------
    Methods for dividing the nucleus to hook
    and hump sides
    -----------------------
  */

  /*
    In order to split the nuclear roi into hook and hump sides,
    we need to get an intersection point of the line through the 
    tail and centre of mass with the opposite border of the nucleus.
  */
  private int findIntersectionPointForNuclearSplit(){
    // test if each point from the tail intersects the splitting line
    // determine the coordinates of the point intersected as int
    // for each xvalue of each point in array, get the line y value
    // at the point the yvalues are closest and not the tail point is the intersesction
    double[] lineEquation = findLineEquation(this.getCentreOfMass(), this.getSpermTail());
    double minDeltaY = 100;
    int minDeltaYIndex = 0;

    for(int i = 0; i<this.getLength();i++){
        double x = this.getBorderPoint(i).getX();
        double y = this.getBorderPoint(i).getY();
        double yOnLine = getYFromEquation(lineEquation, x);

        double distanceToTail = this.getBorderPoint(i).getLengthTo(this.getSpermTail());

        double deltaY = Math.abs(y - yOnLine);
        if(deltaY < minDeltaY && distanceToTail > this.getFeret()/2){ // exclude points too close to the tail
          minDeltaY = deltaY;
          minDeltaYIndex = i;
        }
    }
    return minDeltaYIndex;
  }

  public void splitNucleusToHeadAndHump(){

    int intersectionPointIndex = findIntersectionPointForNuclearSplit();
    this.intersectionPoint = this.getBorderPoint( intersectionPointIndex );

    // get an array of points from tip to tail
    ArrayList<NucleusBorderPoint> roi1 = new ArrayList<NucleusBorderPoint>(0);
    ArrayList<NucleusBorderPoint> roi2 = new ArrayList<NucleusBorderPoint>(0);
    boolean changeRoi = false;

    for(int i = 0; i<this.getLength();i++){

      int currentIndex = wrapIndex(tailIndex+i, this.getLength()); // start at the tail, and go around the array
      
      NucleusBorderPoint p = getBorderPoint(currentIndex);

      if(currentIndex != intersectionPointIndex && !changeRoi){   // starting at the tip, assign points to roi1
        roi1.add(p);
      }
      if(currentIndex==intersectionPointIndex && !changeRoi){ // until we hit the intersection point. Then, close the polygon of roi1 back to the tip. Switch to roi2
        roi1.add(p);
        roi1.add(this.getSpermTail());
        roi2.add(this.intersectionPoint);
        changeRoi = true;
      }
      if(currentIndex != intersectionPointIndex && currentIndex != tailIndex && changeRoi){   // continue with roi2, adjusting the index numbering as needed
        roi2.add(p);
      }

      if(currentIndex==tailIndex && changeRoi){ // after reaching the tail again, close the polygon back to the intersection point
        roi2.add(this.intersectionPoint);
      }

    }

    // Construct new float polygons
    float[] roi1X = new float[ roi1.size()];
    float[] roi2X = new float[ roi2.size()];
    float[] roi1Y = new float[ roi1.size()];
    float[] roi2Y = new float[ roi2.size()];

    for(int i=0;i<roi1.size();i++){
      roi1X[i] = (float) roi1.get(i).getX();
      roi1Y[i] = (float) roi1.get(i).getY();
    }

    for(int i=0;i<roi2.size();i++){
      roi2X[i] = (float) roi2.get(i).getX();
      roi2Y[i] = (float) roi2.get(i).getY();
    }

    for(int i=0;i<roi1.size();i++){
      if(roi1.get(i).overlaps(spermTip)){
        this.hookRoi = new FloatPolygon( roi1X, roi1Y);
        this.humpRoi = new FloatPolygon( roi2X, roi2Y);
        break;
      }
    }

    for(int i=0;i<roi2.size();i++){
      if(roi2.get(i).overlaps(spermTip)){
        this.hookRoi = new FloatPolygon( roi2X, roi2Y);
        this.humpRoi = new FloatPolygon( roi1X, roi1Y);
         break;
      }
    }
  }

  /*
    -----------------------
    Methods for measuring signal positions
    -----------------------
  */

  public void calculateSignalAnglesFromTail(){

    ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
    signals.add(this.getRedSignals());
    signals.add(this.getGreenSignals());

    for( ArrayList<NuclearSignal> signalGroup : signals ){

      if(signalGroup.size()>0){

        for(int i=0;i<signalGroup.size();i++){
          NuclearSignal n = signalGroup.get(i);
          double angle = findAngleBetweenXYPoints(this.getSpermTail(), this.getCentreOfMass(), n.getCentreOfMass());

          // hook or hump?
          if( this.isHookSide(n.getCentreOfMass()) ){ // hookRoi.contains((float) n.centreOfMass.getX() , (float) n.centreOfMass.getY())  
            angle = 360 - angle;
          }

          // set the final angle
          n.setAngle(angle);
        }
      }
    }
    // if(greenSignals.size()>0){

    //   for(int i=0;i<greenSignals.size();i++){
    //     NuclearSignal n = greenSignals.get(i);
    //     double angle = findAngleBetweenXYPoints(this.getSpermTail(), this.getCentreOfMass(), n.getCentreOfMass());

    //     // hook or hump?
    //     if( this.isHookSide(n.getCentreOfMass()) ){
    //       angle = 360 - angle;
    //     }

    //     // set the final angle
    //     n.setAngle(angle);
    //   }
    // }
  }


  /*
    -----------------------
    Methods for exporting data
    -----------------------
  */

   /*
    Print key data to the image log file
    Overwrites any existing log
    Replaces exportAngleProfile in Nucleus
  */   
  public void exportAngleProfile(){

    File f = new File(this.getNucleusFolder()+File.separator+this.getNucleusNumber()+".log");
    if(f.exists()){
      f.delete();
    }

    // NucleusBorderPoint[] points = this.getAngleProfile().getBorderPointArray();
    String outLine =  "X_INT\t"+
                      "Y_INT\t"+
                      "X_DOUBLE\t"+
                      "Y_DOUBLE\t"+
                      "INTERIOR_ANGLE\t"+
                      "MIN_ANGLE\t"+
                      "INTERIOR_ANGLE_DELTA\t"+
                      "INTERIOR_ANGLE_DELTA_SMOOTHED\t"+
                      "BLOCK_POSITION\t"+
                      "BLOCK_NUMBER\t"+
                      "IS_LOCAL_MIN\t"+
                      "IS_LOCAL_MAX\t"+
                      "IS_MIDPOINT\t"+
                      "IS_BLOCK\t"+
                      "NORMALISED_PROFILE_X\t"+
                      "DISTANCE_PROFILE\n";

    for(int i=0;i<this.getLength();i++){

      double normalisedX = ((double)i/(double)this.getLength())*100; // normalise to 100 length
      
      outLine +=  this.getBorderPoint(i).getXAsInt()                      +"\t"+
                  this.getBorderPoint(i).getYAsInt()                      +"\t"+
                  this.getBorderPoint(i).getX()                           +"\t"+
                  this.getBorderPoint(i).getY()                           +"\t"+
                  this.getBorderPoint(i).getInteriorAngle()               +"\t"+
                  this.getBorderPoint(i).getMinAngle()                    +"\t"+
                  this.getBorderPoint(i).getInteriorAngleDelta()          +"\t"+
                  this.getBorderPoint(i).getInteriorAngleDeltaSmoothed()  +"\t"+
                  this.getBorderPoint(i).getPositionWithinBlock()         +"\t"+
                  this.getBorderPoint(i).getBlockNumber()                 +"\t"+
                  this.getBorderPoint(i).isLocalMin()                     +"\t"+
                  this.getBorderPoint(i).isLocalMax()                     +"\t"+
                  this.getBorderPoint(i).isMidpoint()                     +"\t"+
                  this.getBorderPoint(i).isBlock()                        +"\t"+
                  normalisedX                                             +"\t"+
                  this.getBorderPoint(i).getDistanceAcrossCoM()           +"\n";
    }
    IJ.append( outLine, f.getAbsolutePath());
  }

  public void annotateSpermFeatures(){

    try{

      ImageProcessor ip = this.getAnnotatedImage().getProcessor();

      //draw the sperm tip 
      ip.setLineWidth(5);
      ip.setColor(Color.YELLOW);
      ip.drawDot(this.getSpermTip().getXAsInt(), this.getSpermTip().getYAsInt());

      // draw the points considered as sperm tails on a per-nucleus basis
      ip.setLineWidth(3);
      ip.setColor(Color.GRAY);
      for(int j=0; j<this.intialSpermTails.size();j++){
        NucleusBorderPoint p = this.intialSpermTails.get(j);
        ip.drawDot(p.getXAsInt(), p.getYAsInt());
      }

      // Draw the original consensus tail
      ip.setLineWidth(5);
      ip.setColor(Color.CYAN);
      ip.drawDot(this.getInitialConsensusTail().getXAsInt(), this.getInitialConsensusTail().getYAsInt());

      // line from tail to intsersection point; should pass through CoM   
      if(this.intersectionPoint!=null){ // handle failed nuclei in which this analysis was not performed
        ip.setLineWidth(1);
        ip.setColor(Color.YELLOW);
        ip.drawLine(this.getSpermTail().getXAsInt(), this.getSpermTail().getYAsInt(), this.intersectionPoint.getXAsInt(), this.intersectionPoint.getYAsInt());
      }

      // The narrowest part of the sperm head
      ip.setLineWidth(1);
      ip.setColor(Color.MAGENTA);
      ip.drawLine(this.minFeretPoint1.getXAsInt(), this.minFeretPoint1.getYAsInt(), this.minFeretPoint2.getXAsInt(), this.minFeretPoint2.getYAsInt());
      ip.setLineWidth(3);
      ip.drawDot(this.minFeretPoint1.getXAsInt(), this.minFeretPoint1.getYAsInt());
      
    } catch(Exception e){
      IJ.log("Error annotating nucleus: "+e);
    }
  }
}