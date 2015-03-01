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

  private ArrayList<NucleusBorderPoint> tailEstimatePoints = new ArrayList<NucleusBorderPoint>(0); // holds the points considered to be sperm tails before filtering

  // Requires a nucleus object to construct from
  public AsymmetricNucleus(Nucleus n){
    this.setRoi(n.getRoi());
    this.setPosition(n.getPosition());
    this.setSourceImage(n.getSourceImage());
    this.setSourceFile(n.getSourceFile());
    this.setAnnotatedImage(n.getAnnotatedImage());
    this.setEnlargedImage(n.getEnlargedImage());
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
    this.setBorderPointsOfInterest(n.getBorderPointsOfInterest());
    this.setOutputFolder(n.getOutputFolderName());
  }

  public AsymmetricNucleus(){

  }

  /*
    -----------------------
    Get nucleus features
    -----------------------
  */

  public ArrayList<NucleusBorderPoint> getEstimatedTailPoints(){
    return this.tailEstimatePoints;
  }

  /*
    -----------------------
    Set nucleus features
    -----------------------
  */

  protected void addTailEstimatePosition(NucleusBorderPoint p){
    this.tailEstimatePoints.add(p);
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
    ip.drawDot( this.getBorderPointOfInterest("tail").getXAsInt(), 
                this.getBorderPointOfInterest("tail").getYAsInt());
  }

  public void annotateHead(){
    ImageProcessor ip = this.getAnnotatedImage().getProcessor();
    ip.setColor(Color.YELLOW);
    ip.setLineWidth(3);
    ip.drawDot( this.getBorderPointOfInterest("head").getXAsInt(), 
                this.getBorderPointOfInterest("head").getYAsInt());
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
  @Override
  public double findRotationAngle(){
    XYPoint end = new XYPoint(this.getBorderPointOfInterest("tail").getXAsInt(),this.getBorderPointOfInterest("tail").getYAsInt()-50);

    double angle = findAngleBetweenXYPoints(end, this.getBorderPointOfInterest("tail"), this.getCentreOfMass());

    if(this.getCentreOfMass().getX() < this.getBorderPointOfInterest("tail").getX()){
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
}