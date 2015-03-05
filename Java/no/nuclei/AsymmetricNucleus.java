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

  private List<NucleusBorderPoint> tailEstimatePoints = new ArrayList<NucleusBorderPoint>(0); // holds the points considered to be sperm tails before filtering

  public AsymmetricNucleus(){

  }

  /*
    -----------------------
    Get nucleus features
    -----------------------
  */

  public List<NucleusBorderPoint> getEstimatedTailPoints(){
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
    XYPoint end = new XYPoint(this.getBorderTag("tail").getXAsInt(),this.getBorderTag("tail").getYAsInt()-50);

    double angle = findAngleBetweenXYPoints(end, this.getBorderTag("tail"), this.getCentreOfMass());

    if(this.getCentreOfMass().getX() < this.getBorderTag("tail").getX()){
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