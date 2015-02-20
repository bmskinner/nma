/*
-------------------------------------------------
PIG SPERM MORPHOLOGY ANALYSIS: IMAGEJ PLUGIN
-------------------------------------------------
Copyright (C) Ben Skinner 2015



  ---------------
  PLOT AND IMAGE FILES
  ---------------

  ---------------
  LOG FILES
  ---------------
  

*/
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
import nucleusAnalysis.*;

public class Pig_Sperm_Analysis
  extends ImagePlus
  implements PlugIn
{

  // failure codes - not in use, keep to add back to logFailed in refilter
  private static final int FAILURE_TIP       = 1;
  private static final int FAILURE_TAIL      = 2;
  private static final int FAILURE_THRESHOLD = 4;
  private static final int FAILURE_FERET     = 8;
  private static final int FAILURE_ARRAY     = 16;
  private static final int FAILURE_AREA      = 32;
  private static final int FAILURE_PERIM     = 64;
  private static final int FAILURE_OTHER     = 128;

  private NucleusCollection completeCollection;
  private NucleusCollection failedNuclei;
    
  public void run(String paramString)  {

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    if(folderName==null){
      return;
    }

    IJ.log("Directory: "+folderName);

    File folder = new File(folderName);
    NucleusDetector detector = new NucleusDetector(folder);

    HashMap<File, NucleusCollection> folderCollection = detector.getNucleiCollections();

    Set<File> keys = folderCollection.keySet();

    for (File key : keys) {
      NucleusCollection collection = folderCollection.get(key);
      IJ.log(key.getAbsolutePath()+"   Nuclei: "+collection.getNucleusCount());
      // Export profiles
      for(int i=0;i<collection.getNucleusCount();i++){
        Nucleus n = collection.getNucleus(i);
        n.exportAngleProfile();
        n.exportAnnotatedImage();
      }
    }


    // for alignment of profiles: 
    // get the narrowest point, and extend out.
    // get the two local minima and choose a maximum between them

    IJ.log("Analysis complete");
  }

  class PigSpermNucleus 
    extends Nucleus 
  {

      private NucleusBorderPoint tailPoint;
      private NucleusBorderPoint headPoint;

      public PigSpermNucleus(Nucleus n){
        super(n.getRoi(), n.getSourceFile(), n.getSourceImage(), n.getNucleusNumber());
      }

      public void findTailByMinima(){

        NucleusBorderPoint[] minima = this.getAngleProfile().getLocalMinima();
      }
      // get the narrowest point, and extend out.
      // get the two local minima and choose a maximum between them
  }
}
