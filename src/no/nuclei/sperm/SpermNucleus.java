/*
  -----------------------
  SPERM NUCLEUS CLASS
  -----------------------
  Sperm have a head and a tail, hence can be oriented
  in one axis. This is inherited from the AsymmetricNucleus.
  Mostly empty for now, but analyses involving
  segments such as acrosomes may need common methods.
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
	
  /**
  * Constructor using a Nucleus; passes up
  * to the Nucleus constructor
  *
  * @param n the Nucleus to construct from
  * @return a SpermNucleus
  */
  public SpermNucleus(Nucleus n){
  	super(n);
  }

  /**
  * Empty constructor. Can be used for class
  * identification (as in AnalysisCreator) 
  *
  * @return an empty SpermNucleus
  */
  public SpermNucleus(){
    
  }

  /*
    -----------------------
    Get sperm nucleus features
    -----------------------
  */


  /*
    -----------------------
    Set sperm nucleus features
    -----------------------
  */


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