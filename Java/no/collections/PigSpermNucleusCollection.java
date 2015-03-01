/* 
  -----------------------
  RODENT SPERM NUCLEUS COLLECTION CLASS
  -----------------------
  This class enables filtering for the nucleus type
  It enables offsets to be calculated based on the median normalised curves
*/

package no.collections;

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
import no.components.*;
import no.utility.*;

public class PigSpermNucleusCollection
    extends no.collections.AsymmetricNucleusCollection
{

    public PigSpermNucleusCollection(File folder, String outputFolder, String type){
      super(folder, outputFolder, type);
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
  // public void annotateImagesOfNuclei(){
  //   IJ.log("Annotating images ("+this.getType()+")...");
  //   for(int i=0; i<this.getNucleusCount();i++){
  //     INuclearFunctions n = (INuclearFunctions)this.getNucleus(i);
  //     n.annotateFeatures();
  //   }
  // }

}