/* 
  -----------------------
  RODENT SPERM NUCLEUS COLLECTION CLASS
  -----------------------
  This class enables filtering for the nucleus type
  It enables offsets to be calculated based on the median normalised curves
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
import no.nuclei.sperm.*;

public class PigSpermNucleusCollection
    extends no.nuclei.AsymmetricNucleusCollection
{

    public PigSpermNucleusCollection(File folder, String type){
      super(folder, type);
    }

    public PigSpermNucleus getNucleus(int i){
      return (PigSpermNucleus)this.getNuclei().get(i);
    }

  /*
    -----------------------
    Create and manipulate
    aggregate profiles. These
    can be centred on head or tail
    -----------------------
  */

  // public void measureProfilePositions(){

  //   this.createProfileAggregateFromTail();
  //   this.createProfileAggregateFromHead();
  //   this.drawProfilePlots();
  //   this.drawNormalisedMedianLineFromTail();
  //   this.drawNormalisedMedianLineFromHead();
  //   this.calculateDifferencesToMedianProfiles();
  //   this.exportProfilePlots();

  //   // Use the median profile to refine head / tail point
  //   // this.findTailIndexInMedianCurve();
  //   // this.calculateOffsets();

  // }

  // public void annotateAndExportNuclei(){
  //   this.exportNuclearStats("logStats");
  //   this.exportClusteringProfiles("logClusters");
  //   this.annotateImagesOfNuclei();
  //   this.exportAnnotatedNuclei();
  //   this.exportCompositeImage("composite");
  // }

  /*
    -----------------------
    Get aggregate values
    -----------------------
  */

  public int[] getTailIndexes(){
    int[] d = new int[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){
      d[i] = this.getNucleus(i).getTailIndex();
    }
    return d;
  }

  /*
    -----------------------
    Identify tail in median profile
    and offset nuclei profiles
    -----------------------
  */

  /*
    -----------------------
    Annotate sperm
    -----------------------
  */

  /*
    Draw the features of interest on the images of the nuclei created earlier
  */
  public void annotateImagesOfNuclei(){
    IJ.log("Annotating images ("+this.getType()+")...");
    for(int i=0; i<this.getNucleusCount();i++){
      PigSpermNucleus n = (PigSpermNucleus)this.getNucleus(i);
      n.annotateFeatures();
    }
  }

}