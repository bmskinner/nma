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
import no.components.*;
import no.utility.*;

public class RodentSpermNucleus
	extends SpermNucleus
{
  private static final int MAX_INTERIOR_ANGLE_TO_CALL_TIP = 110;

  private int tipIndex; // the index in the smoothedArray that has been designated the tip [should be 0]

  private NucleusBorderPoint spermTip; // differs from the headpoint, which in other sperm is opposite the tail
  private NucleusBorderPoint intersectionPoint; // the point through the centre of mass directly opposite the sperm tail. Used for dividing hook/hump Rois
  private NucleusBorderPoint initialConsensusTail; // the point initially chosen as the tail. Used to draw tail position box plots
  private NucleusBorderPoint minFeretPoint1; // debugging tool used for identification of narrowest width across CoM. Stores the border point
  private NucleusBorderPoint minFeretPoint2;
  
  private FloatPolygon hookRoi;
  private FloatPolygon humpRoi;

  // Requires a sperm nucleus object to construct from
  public RodentSpermNucleus(Nucleus n){
  	super(n);
    // this.findPointsAroundBorder();
  }

  // empty object
  public RodentSpermNucleus(){
  }

  /*
    Identify key points: tip, estimated tail position
  */
  @Override
  public void findPointsAroundBorder(){
    
    // find tip - use the least angle method
    int tipIndex = this.getAngleProfile().getIndexOfMin();
    addBorderTag("tip", tipIndex);

    // decide if the profile is right or left handed; flip if needed
    // IJ.log("    Nucleus "+this.getNucleusNumber());
    if(!this.isProfileOrientationOK()){
      this.reverse(); // reverses all profiles, border array and tagged points
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
    // addBorderTag("spermTail2", this.getIndex(spermTail2));

    /*
      Method 2: Look at the 2nd derivative - rate of change of angles
                Perform a 5win average smoothing of the deltas
                Count the number of consecutive >1 degree blocks
                Wide block far from tip = tail
    */  
    // NucleusBorderPoint spermTail3 = this.findTailFromDeltas();
    // this.addTailEstimatePosition(spermTail3);

    /*    
      Method 3: Find the narrowest diameter around the nuclear CoM
                Draw a line orthogonal, and pick the intersecting border points
                The border furthest from the tip is the tail
    */  
    NucleusBorderPoint spermTail1 = this.findTailByNarrowestWidthMethod();
    this.addTailEstimatePosition(spermTail1);
    // addBorderTag("spermTail1", this.getIndex(spermTail1));


    /*
      Given distinct methods for finding a tail,
      take a position between them on roi
    */
    int consensusTailIndex = this.getPositionBetween(spermTail2, spermTail1);
    NucleusBorderPoint consensusTail = this.getBorderPoint(consensusTailIndex);
    // consensusTailIndex = this.getPositionBetween(consensusTail, spermTail1);

    // this.setInitialConsensusTail(consensusTail);

    // addBorderTag("initialConsensusTail", consensusTailIndex);

    addBorderTag("tail", consensusTailIndex);

    addBorderTag("head", this.getIndex(this.findOppositeBorder(consensusTail)));
  }

  /*
    -----------------------
    Get and set sperm nucleus features
    -----------------------
  */
  // public void setInitialConsensusTail(NucleusBorderPoint p){
  //   this.initialConsensusTail = p;
  // }

  // public NucleusBorderPoint getInitialConsensusTail(){
  //   return this.initialConsensusTail;
  // }

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
    counts the number of points above 180 degrees in each half of the array
  */
  public boolean isProfileOrientationOK(){

    int frontPoints = 0;
    int rearPoints = 0;

    Profile profile = this.getAngleProfile("tip");

    int midPoint = (int) (this.getLength()/2) ;
    for(int i=0; i<this.getLength();i++){ // integrate points over 180

        if(profile.get(i)>180 && i<midPoint){
          frontPoints += profile.get(i);
        }
        if(profile.get(i)>180 && i>midPoint){
          rearPoints  += profile.get(i);
        }
    }

    if(frontPoints > rearPoints){ // if the maxIndex is closer to the end than the beginning
      return true;
    } else{ 
      return false;
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
    double tipToCoMDistance = this.getBorderTag("tip").getLengthTo(this.getCentreOfMass());
    List<Integer> array = this.getAngleProfile().getLocalMinima(5);

    double maxDistance = 0;
    NucleusBorderPoint tail = this.getBorderTag("tip"); // start at tip, move round

    for(int a : array){
            
      double distanceAcrossCoM = tipToCoMDistance + this.getCentreOfMass().getLengthTo(getPoint(a));
      double distanceBetweenEnds = this.getBorderTag("tip").getLengthTo(getPoint(a));
      
      double totalDistance = distanceAcrossCoM + distanceBetweenEnds;

      if(totalDistance > maxDistance){
        maxDistance = totalDistance;
        tail = getPoint(a);
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
    NucleusBorderPoint reference = this.getBorderTag("tip");

    for(int i=0;i<this.getLength();i++){

      NucleusBorderPoint p = this.getPoint(i);
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
      if(  Math.abs(90-angle)<difference && 
          p.getLengthTo(this.getBorderTag("tip")) > this.getCentreOfMass().getLengthTo( this.getBorderTag("tip") ) ){
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
  // public NucleusBorderPoint findTailFromDeltas(){

  //   // get the midpoint of each block
  //   ArrayList<NucleusBorderPoint> results = new ArrayList<NucleusBorderPoint>(0);
  //   int maxIndex = 0;
  
  //   // remember that block 0 is not assigned; start from 1
  //   try{
  //     for(int i=1; i<this.getAngleProfile().getBlockCount();i++){

  //       // number of points in each block
  //       NucleusBorderPoint[] points = this.getAngleProfile().getBlockOfBorderPoints(i);
  //       for(NucleusBorderPoint p : points){
  //         if(p.isMidpoint()){ // will ignore any blocks without a midpoint established - <2 members
  //           results.add(p);
  //         }
  //       }
  //     }
  //   } catch(Exception e){
  //     IJ.log("    Error in finding midpoints: findTailFromDeltas(): "+e);
  //   }
    
  //   NucleusBorderPoint tail = new NucleusBorderPoint(0,0);
  //   try{
  //     // go through the midpoints, get the max distance from tip
  //     double maxLength = 0;
      
  //     for( NucleusBorderPoint p : results){
  //       // NucleusBorderPoint p = (NucleusBorderPoint)o;
  //       if(p.getLengthTo(this.getBorderPointOfInterest("tip")) > maxLength){
  //         maxLength = p.getLengthTo(this.getBorderPointOfInterest("tip"));
  //         tail = p;
  //       }
  //     }
  //   } catch(Exception e){
  //     IJ.log("    Error in finding lengths: findTailFromDeltas(): "+e);
  //   }
  //   return tail;
  // }

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
    Equation lineEquation = new Equation(this.getCentreOfMass(), this.getBorderTag("tail"));
    double minDeltaY = 100;
    int minDeltaYIndex = 0;

    for(int i = 0; i<this.getLength();i++){
        double x = this.getBorderPoint(i).getX();
        double y = this.getBorderPoint(i).getY();
        double yOnLine = lineEquation.getY(x);

        double distanceToTail = this.getBorderPoint(i).getLengthTo(this.getBorderTag("tail"));

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
    // this.intersectionPoint = this.getBorderPoint( intersectionPointIndex );
    this.addBorderTag("intersectionPoint", intersectionPointIndex );

    // get an array of points from tip to tail
    List<NucleusBorderPoint> roi1 = new ArrayList<NucleusBorderPoint>(0);
    List<NucleusBorderPoint> roi2 = new ArrayList<NucleusBorderPoint>(0);
    boolean changeRoi = false;

    for(int i = 0; i<this.getLength();i++){

      int currentIndex = NuclearOrganisationUtility.wrapIndex(this.getBorderIndex("tail")+i, this.getLength()); // start at the tail, and go around the array
      
      NucleusBorderPoint p = getPoint(currentIndex);

      if(currentIndex != intersectionPointIndex && !changeRoi){   // starting at the tip, assign points to roi1
        roi1.add(p);
      }
      if(currentIndex==intersectionPointIndex && !changeRoi){ // until we hit the intersection point. Then, close the polygon of roi1 back to the tip. Switch to roi2
        roi1.add(p);
        roi1.add(this.getBorderTag("tail"));
        roi2.add(this.getBorderTag("intersectionPoint"));
        changeRoi = true;
      }
      if(currentIndex != intersectionPointIndex && currentIndex != this.getBorderIndex("tail") && changeRoi){   // continue with roi2, adjusting the index numbering as needed
        roi2.add(p);
      }

      if(currentIndex==this.getBorderIndex("tail") && changeRoi){ // after reaching the tail again, close the polygon back to the intersection point
        roi2.add(this.getBorderTag("intersectionPoint"));
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
      if(roi1.get(i).overlaps(this.getBorderTag("tip"))){
        this.hookRoi = new FloatPolygon( roi1X, roi1Y);
        this.humpRoi = new FloatPolygon( roi2X, roi2Y);
        break;
      }
    }

    for(int i=0;i<roi2.size();i++){
      if(roi2.get(i).overlaps(this.getBorderTag("tip"))){
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

  // needs to override AsymmetricNucleus version because hook/hump
  @Override
  public void calculateSignalAnglesFromPoint(NucleusBorderPoint p){

    super.calculateSignalAnglesFromPoint(p);

    // update signal angles with hook or hump side

    List<List<NuclearSignal>> signals = new ArrayList<List<NuclearSignal>>(0);
    signals.add(this.getRedSignals());
    signals.add(this.getGreenSignals());

    for( List<NuclearSignal> signalGroup : signals ){

      if(signalGroup.size()>0){

        for(int i=0;i<signalGroup.size();i++){
          NuclearSignal n = signalGroup.get(i);

          // hook or hump?
          double angle = n.getAngle();
          if( this.isHookSide(n.getCentreOfMass()) ){ 
            angle = 360 - angle;
          }

          // set the final angle
          n.setAngle(angle);
        }
      }
    }
  }


  /*
    -----------------------
    Methods for exporting data
    -----------------------
  */


  /*
    -----------------------
    Annotate nucleus
    -----------------------
  */

  public void annotateFeatures(){

    ImageProcessor ip = this.getAnnotatedImage().getProcessor();

    //draw the sperm tip 
    ip.setLineWidth(5);
    ip.setColor(Color.YELLOW);
    ip.drawDot( this.getBorderTag("tip").getXAsInt(), 
                this.getBorderTag("tip").getYAsInt());


    this.annotateEstimatedTailPoints();
    this.annotateTail();

    // line from tail to intsersection point; should pass through CoM   
    if(this.getBorderTag("intersectionPoint").getX()!=0 && this.getBorderTag("intersectionPoint").getY()!=0){ // handle failed nuclei in which this analysis was not performed
      ip.setLineWidth(1);
      ip.setColor(Color.YELLOW);
      ip.drawLine(this.getBorderTag("tail").getXAsInt(),
                  this.getBorderTag("tail").getYAsInt(), 
                  this.getBorderTag("intersectionPoint").getXAsInt(), 
                  this.getBorderTag("intersectionPoint").getYAsInt());
    }
  }
}