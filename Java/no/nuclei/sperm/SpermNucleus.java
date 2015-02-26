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
import no.components.*;
import no.utility.*;

public class SpermNucleus
	extends AsymmetricNucleus
{
	
  // Requires a nucleus object to construct from
  public SpermNucleus(Nucleus n){
  	super(n);
  }

  /*
    -----------------------
    Get sperm nucleus features
    -----------------------
  */

  // public NucleusBorderPoint getSpermHead(){
  // 	return this.getHead();
  // }

  // public NucleusBorderPoint getSpermTail(){
  // 	return this.getTail();
  // }

  /*
    -----------------------
    Set sperm nucleus features
    -----------------------
  */

  // public void setSpermHead(NucleusBorderPoint p){
  //   this.setHead(p);
  // }

  // public void setSpermTail(NucleusBorderPoint p){
  //   this.setTail(p);
  // }

  /*
    -----------------------
    Get raw and normalised profile and values
    -----------------------
  */



  /*
    -----------------------
    Annotate features of the nucleus
    -----------------------
  */


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

  /*
    -----------------------
    Measure signal positions
    -----------------------
  */

}