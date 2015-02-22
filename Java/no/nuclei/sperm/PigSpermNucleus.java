  /*
  -----------------------
  PIG SPERM NUCLEUS CLASS
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

public class PigSpermNucleus 
    extends SpermNucleus 
  {

    public PigSpermNucleus(SpermNucleus n){
      super(n);
      setSpermTail(n.getSpermTail());
      setHeadPoint(n.getHeadPoint());
    }

    /*
      -----------------------
      Methods for detecting the head and tail
      -----------------------
    */

    public void findTailByMinima(){

      NucleusBorderPoint[] minima = this.getAngleProfile().getLocalMinima();

      // sort minima by interior angle
      NucleusBorderPoint lowestMinima = minima[0];
      NucleusBorderPoint secondLowestMinima = minima[0];

      for( NucleusBorderPoint n : minima){
        if (n.getInteriorAngle()<lowestMinima.getInteriorAngle()){
          secondLowestMinima = lowestMinima;
          lowestMinima = n;
        }
      }
      for( NucleusBorderPoint n : minima){
        if (n.getInteriorAngle()<secondLowestMinima.getInteriorAngle() && 
            n.getInteriorAngle()>lowestMinima.getInteriorAngle()){
          secondLowestMinima = n;
        }
      }
      this.setSpermTail(this.getBorderPoint(this.getPositionBetween(lowestMinima, secondLowestMinima)));
    }

    public void findTailByMaxima(){
      // the tail is the ?only local maximum with an interior angle above the median
      // distance on the distance profile

      // the CoM is also more towards the tail. Use this.
      NucleusBorderPoint[] maxima = this.getAngleProfile().getLocalMaxima();
      double medianProfileDistance= this.getMedianDistanceFromProfile();

      for( NucleusBorderPoint n : maxima){
        if (n.getDistanceAcrossCoM()>medianProfileDistance){
          this.setSpermTail(n);
        }
      }
    }
  }