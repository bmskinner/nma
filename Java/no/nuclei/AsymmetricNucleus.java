/*
  -----------------------
  ASYMMETRIC NUCLEUS CLASS
  -----------------------
  Contains the variables for storing a non-circular nucleus.
  They have a head and a tail, hence can be oriented
  in one axis.

  A tail is the point determined via profile analysis. The
  head is assigned as the point opposite through the CoM.
*/  
package no.nuclei;

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

import no.components.*;
import no.collections.NucleusCollection;

public class AsymmetricNucleus
	extends Nucleus
{
	private NucleusBorderPoint tailPoint;
  private NucleusBorderPoint headPoint;

  private ArrayList<NucleusBorderPoint> tailEstimatePoints = new ArrayList<NucleusBorderPoint>(0); // holds the points considered to be sperm tails before filtering

  private int tailIndex; // the index in the angleProfile that has been designated the tail
  private int headIndex; // the index in the angleProfile that has been designated the head

  private int offsetForTail = 0; // the offset to apply to the angleProfile to start it from the tail
  private int offsetForHead = 0; // the offset to apply to the angleProfile to start it from the head

  private double differenceToMedianProfileFromHead; // store the difference between curves
  private double differenceToMedianProfileFromTail; // store the difference between curves

  // Requires a nucleus object to construct from
  public AsymmetricNucleus(Nucleus n){
  	this.setRoi(n.getRoi());
    this.setSourceImage(n.getSourceImage());
    this.setSourceFile(n.getSourceFile());
    this.setAnnotatedImage(n.getAnnotatedImage());
    this.setNucleusNumber(n.getNucleusNumber());
    this.setNucleusFolder(n.getNucleusFolder());
    this.setPerimeter(n.getPerimeter());
    this.setPathLength(n.getPathLength());
    this.setFeret(n.getFeret());
    this.setArea(n.getArea());
    this.setAngleProfile(n.getAngleProfile());
    this.setCentreOfMass(n.getCentreOfMass());
    this.setRedSignals(n.getRedSignals());
    this.setGreenSignals(n.getGreenSignals());
    this.setPolygon(n.getSmoothedPolygon());
    this.setDistanceProfile(n.getDistanceProfile());
    this.setSignalDistanceMatrix(n.getSignalDistanceMatrix());
  }

  /*
    -----------------------
    Get nucleus features
    -----------------------
  */

  public NucleusBorderPoint getHead(){
  	return this.headPoint;
  }

  public NucleusBorderPoint getTail(){
  	return this.tailPoint;
  }

  public int getTailIndex(){
    return this.tailIndex;
  }

  public int getHeadIndex(){
    return this.headIndex;
  }

  public int getOffsetForTail(){
    return this.offsetForTail;
  }

  public int getOffsetForHead(){
    return this.offsetForHead;
  }

  public ArrayList<NucleusBorderPoint> getEstimatedTailPoints(){
    return this.tailEstimatePoints;
  }

  public double getDifferenceToMedianProfileFromHead(){
    return this.differenceToMedianProfileFromHead;
  }

  public double getDifferenceToMedianProfileFromTail(){
    return this.differenceToMedianProfileFromTail;
  }

  /*
    -----------------------
    Set sperm nucleus features
    -----------------------
  */

  public void setHead(NucleusBorderPoint p){
    this.headPoint = p;
  }

  public void setTail(NucleusBorderPoint p){
    this.tailPoint = p;
  }

  public void setTailIndex(int i){
    this.tailIndex = i;
  }

  public void setHeadIndex(int i){
    this.headIndex = i;
  }

  public void setOffsetForTail(int i){
    this.offsetForTail = i;
  }

  public void setOffsetForHead(int i){
    this.offsetForHead = i;
  }

  protected void addTailEstimatePosition(NucleusBorderPoint p){
    this.tailEstimatePoints.add(p);
  }

  public void setDifferenceToMedianProfileFromHead(double d){
    this.differenceToMedianProfileFromHead = d;
  }

  public void setDifferenceToMedianProfileFromTail(double d){
    this.differenceToMedianProfileFromTail = d;
  }

  /*
    See if there is a differences to the given median
  */
  public double calculateDifferenceToMedianProfile(double[] medianProfile){

    // the curve needs to be matched to the median 
    // hence the median array needs to be the same curve length
    double[] interpolatedMedian = NucleusCollection.interpolateMedianToLength(this.getLength(), medianProfile);

    // for comparisons between sperm, get the difference between the offset curve and the median
    double totalDifference = 0;

    for(int j=0; j<this.getLength(); j++){ // for each point round the array

      double curveAngle  = this.getBorderPoint(j).getInteriorAngle();
      double medianAngle = interpolatedMedian[j];

      totalDifference += Math.abs(curveAngle - medianAngle);
    }
    return totalDifference;
  }

  /*
    -----------------------
    Annotate features of the nucleus
    -----------------------
  */

  public void annotateTail(){
    ImageProcessor ip = this.getAnnotatedImage().getProcessor();
    ip.setColor(Color.CYAN);
    ip.setLineWidth(3);
    ip.drawDot( this.tailPoint.getXAsInt(), 
                this.tailPoint.getYAsInt());
  }

  public void annotateHead(){
    ImageProcessor ip = this.getAnnotatedImage().getProcessor();
    ip.setColor(Color.YELLOW);
    ip.setLineWidth(3);
    ip.drawDot( this.headPoint.getXAsInt(), 
                this.headPoint.getYAsInt());
  }

  // draw the points considered as sperm tails
  public void annotateEstimatedTailPoints(){
    ImageProcessor ip = this.getAnnotatedImage().getProcessor();
    ip.setLineWidth(3);
    ip.setColor(Color.GRAY);
    for(int j=0; j<this.getEstimatedTailPoints().size();j++){
      NucleusBorderPoint p = this.getEstimatedTailPoints().get(j);
      ip.drawDot(p.getXAsInt(), p.getYAsInt());
    }
  }

  public void annotateFeatures(){
    this.annotateTail();
    this.annotateHead();
    this.annotateEstimatedTailPoints();
  }

  /*
    -----------------------
    Find rotations based on tail point
    -----------------------
  */

  /*
    Find the angle that the nucleus must be rotated to make the CoM-tail vertical.
    Uses the angle between [sperm tail x,0], sperm tail, and sperm CoM
    Returns an angle
  */
  public double findRotationAngle(){
    XYPoint end = new XYPoint(this.getTail().getXAsInt(),this.getTail().getYAsInt()-50);

    double angle = findAngleBetweenXYPoints(end, this.getTail(), this.getCentreOfMass());

    if(this.getCentreOfMass().getX() < this.getTail().getX()){
      return angle;
    } else {
      return 0-angle;
    }
  }

  /*
    -----------------------
    Measure signal positions
    -----------------------
  */

  public void calculateSignalAnglesFromTail(){
    this.calculateSignalAnglesFromPoint(this.getTail());
  }

  public void calculateSignalAnglesFromPoint(NucleusBorderPoint p){

    ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
    signals.add(this.getRedSignals());
    signals.add(this.getGreenSignals());

    for( ArrayList<NuclearSignal> signalGroup : signals ){

      if(signalGroup.size()>0){

        for(int i=0;i<signalGroup.size();i++){
          NuclearSignal n = signalGroup.get(i);
          double angle = findAngleBetweenXYPoints(p, this.getCentreOfMass(), n.getCentreOfMass());

          // set the final angle
          n.setAngle(angle);
        }
      }
    }
  }

  /*
    -----------------------
    Export data
    -----------------------
  */

  
}