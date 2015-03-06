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
import no.components.*;

public class PigSpermNucleus 
    extends SpermNucleus 
  {

    private NucleusBorderPoint orthPoint1;

    public PigSpermNucleus(Nucleus n){
      super(n);
      // this.findPointsAroundBorder();
      // this.exportAngleProfile();
    }

    public PigSpermNucleus(){

    }

    @Override
    public void findPointsAroundBorder(){

      // NucleusBorderPoint tailPoint1 = this.findTailByMinima();
      // NucleusBorderPoint tailPoint2 = this.findTailByMaxima();
      NucleusBorderPoint tailPoint3 = this.findTailByNarrowestPoint();

      // this.addTailEstimatePosition(tailPoint1);
      // this.addTailEstimatePosition(tailPoint2);
      this.addTailEstimatePosition(tailPoint3);


      // of the three methods, method 3 seems most accurate
      int tailIndex = this.getIndex(tailPoint3);
      addBorderTag("tail", tailIndex);

      int headIndex = getIndex(this.findOppositeBorder(tailPoint3));
      addBorderTag("head", headIndex);
    }

    /*
      -----------------------
      Methods for detecting the head and tail
      -----------------------
    */

    // public NucleusBorderPoint findTailByMinima(){

    //   NucleusBorderPoint[] minima = this.getAngleProfile().getLocalMinima();

    //   // sort minima by interior angle
    //   NucleusBorderPoint lowestMinima = minima[0];
    //   NucleusBorderPoint secondLowestMinima = minima[0];

    //   for( NucleusBorderPoint n : minima){
    //     if (n.getInteriorAngle()<lowestMinima.getInteriorAngle()){
    //       secondLowestMinima = lowestMinima;
    //       lowestMinima = n;
    //     }
    //   }
    //   for( NucleusBorderPoint n : minima){
    //     if (n.getInteriorAngle()<secondLowestMinima.getInteriorAngle() && 
    //         n.getInteriorAngle()>lowestMinima.getInteriorAngle()){
    //       secondLowestMinima = n;
    //     }
    //   }

    //   NucleusBorderPoint tailPoint = this.getBorderPoint(this.getPositionBetween(lowestMinima, secondLowestMinima));
    //   return tailPoint;
    // }

    // public NucleusBorderPoint findTailByMaxima(){
    //   // the tail is the ?only local maximum with an interior angle above the median
    //   // distance on the distance profile

    //   // the CoM is also more towards the tail. Use this.
    //   Integer[] maxima = this.getAngleProfile().getLocalMaxima();
    //   // NucleusBorderPoint[] maxima = this.getAngleProfile().getLocalMaxima();
    //   double medianProfileDistance= this.getMedianDistanceFromProfile();
    //   NucleusBorderPoint tailPoint = maxima[0];

    //   for( NucleusBorderPoint n : maxima){
    //     if (n.getDistanceAcrossCoM()>medianProfileDistance){
    //       tailPoint = n;
    //     }
    //   }
    //   return tailPoint;
    // }

    /*
      The narrowest diameter through the CoM
      is orthogonal to the tail. Of the two
      orthogonal border points, the closest to the
      CoM is the tail
    */
    public NucleusBorderPoint findTailByNarrowestPoint(){

      NucleusBorderPoint narrowPoint = this.getNarrowestDiameterPoint();
      this.orthPoint1  = this.findOrthogonalBorderPoint(narrowPoint);
      NucleusBorderPoint orthPoint2  = this.findOppositeBorder(orthPoint1);

      // NucleusBorderPoint[] array = { orthPoint1, orthPoint2 };

      // the tail should be a maximum, hence have a high angle
      NucleusBorderPoint tailPoint  = getAngle(this.getIndex(orthPoint1)) >
                                      getAngle(this.getIndex(orthPoint2))
                                    ? orthPoint1
                                    : orthPoint2;
      return tailPoint;
    }
}