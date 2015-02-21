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
  public RodentSpermNucleus(SpermNucleus n){
  	super(n);
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
    for(int i=0; i<this.smoothLength;i++){

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
  public NucleusBorderPoint findTailFromDeltas(NucleusBorderPoint tip){

    // get the midpoint of each block
    ArrayList<NucleusBorderPoint> results = new ArrayList<NucleusBorderPoint>(0);
    int maxIndex = 0;
  
    // remember that block 0 is not assigned; start from 1
    try{
      for(int i=1; i<this.getAngleProfile().getBlockCount();i++){

        // number of points in each block
        NucleusBorderPoint[] points = this.angleProfile.getBlockOfBorderPoints(i);
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
      
      for( (NucleusBorderPoint) p : results){
        // NucleusBorderPoint p = (NucleusBorderPoint)o;
        if(p.getLengthTo(tip) > maxLength){
          maxLength = p.getLengthTo(tip);
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


    for(int i=0; i<this.smoothLength;i++){
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

        double distanceToTail = this.getBorderPoint(i).getLengthTo(spermTail);

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
        roi1.add(spermTail);
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
    signals.add(redSignals);
    signals.add(greenSignals);

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

    NucleusBorderPoint[] points = this.angleProfile.getBorderPointArray();
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
                  distanceProfile[i]                                      +"\n";
    }
    IJ.append( outLine, path);
  }
}