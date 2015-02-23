/*
  -----------------------
  SPERM NUCLEUS CLASS
  -----------------------
  Contains the variables for storing a sperm nucleus.
  Sperm have a head and a tail, hence can be oriented
  in one axis.
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

public class SpermNucleus
	extends Nucleus
{
	private NucleusBorderPoint tailPoint;
  private NucleusBorderPoint headPoint;

  private int tailIndex; // the index in the smoothedArray that has been designated the tail
  private int offsetForTail = 0;

  private ArrayList<Double> normalisedXPositionsFromHead = new ArrayList<Double>(0); // holds the x values only after normalisation
  private ArrayList<Double> rawXPositionsFromHead        = new ArrayList<Double>(0);
  private ArrayList<Double> normalisedYPositionsFromTail = new ArrayList<Double>(0);
  private ArrayList<Double> normalisedXPositionsFromTail = new ArrayList<Double>(0);
  private ArrayList<Double> rawXPositionsFromTail        = new ArrayList<Double>(0);

  // Requires a nucleus object to construct from
  public SpermNucleus(Nucleus n){
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
    Get and set sperm nucleus features
    -----------------------
  */

  public NucleusBorderPoint getSpermHead(){
  	return this.headPoint;
  }

  public NucleusBorderPoint getSpermTail(){
  	return this.tailPoint;
  }

  protected void setSpermHead(NucleusBorderPoint p){
  	this.headPoint = p;
  }

  protected void setSpermTail(NucleusBorderPoint p){
  	this.tailPoint = p;
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


  public addRawXPositionFromTail(double d){
    this.rawXPositionsFromTail.add(d);
  }

  public addNormalisedXPositionFromTail(double d){
    this.normalisedXPositionsFromTail.add(d);
  }

  /*
    -----------------------
    Get raw and normalised profiles
    -----------------------
  */

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

  /*
    -----------------------
    Annotate features of the nucleus
    -----------------------
  */

  public void annotateTail(){
    ImageProcessor ip = this.getAnnotatedImage().getProcessor();
    ip.setColor(Color.LIGHT_GRAY);
    ip.setLineWidth(3);
    ip.drawDot( this.tailPoint.getXAsInt(), 
                this.tailPoint.getYAsInt());
  }

  public void annotateHead(){
    ImageProcessor ip = this.getAnnotatedImage().getProcessor();
    ip.setColor(Color.LIGHT_GRAY);
    ip.setLineWidth(3);
    ip.drawDot( this.headPoint.getXAsInt(), 
                this.headPoint.getYAsInt());
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
    XYPoint end = new XYPoint(this.tailPoint.getXAsInt(),this.tailPoint.getYAsInt()-50);

    double angle = findAngleBetweenXYPoints(end, this.tailPoint, this.getCentreOfMass());

    if(this.getCentreOfMass().getX() < this.tailPoint.getX()){
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

    ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
    signals.add(this.getRedSignals());
    signals.add(this.getGreenSignals());

    for( ArrayList<NuclearSignal> signalGroup : signals ){

      if(signalGroup.size()>0){

        for(int i=0;i<signalGroup.size();i++){
          NuclearSignal n = signalGroup.get(i);
          double angle = findAngleBetweenXYPoints(this.getSpermTail(), this.getCentreOfMass(), n.getCentreOfMass());

          // set the final angle
          n.setAngle(angle);
        }
      }
    }
  }
}