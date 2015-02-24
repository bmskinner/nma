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
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;
import no.nuclei.*;
import no.nuclei.sperm.*;
import no.analysis.*;

public class Pig_Sperm_Analysis
  extends ImagePlus
  implements PlugIn
{

  // failure codes - not in use, keep to add back to logFailed in refilter
  private static final String IMAGE_PREFIX = "export.";

  private static final int NUCLEUS_THRESHOLD = 40;
  private static final double MIN_NUCLEAR_SIZE = 1000;
  private static final double MAX_NUCLEAR_SIZE = 10000;
  private static final double MIN_NUCLEAR_CIRC = 0.2;
  private static final double MAX_NUCLEAR_CIRC = 1.0;

  private NucleusCollection completeCollection;
  private NucleusCollection failedNuclei;

  private ArrayList<PigSpermNucleusCollection> nuclearPopulations = new ArrayList<PigSpermNucleusCollection>(0);
  private ArrayList<PigSpermNucleusCollection> failedPopulations  = new ArrayList<PigSpermNucleusCollection>(0);
    
  public void run(String paramString)  {

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    if(folderName==null){
      return;
    }

    IJ.log("Directory: "+folderName);

    File folder = new File(folderName);
    NucleusDetector detector = new NucleusDetector(folder, MIN_NUCLEAR_SIZE, MAX_NUCLEAR_SIZE, NUCLEUS_THRESHOLD);
    detector.runDetector();

    HashMap<File, NucleusCollection> folderCollection = detector.getNucleiCollections();

    IJ.log("Imported folder(s)");
    getPopulations(folderCollection);
    analysePopulations();

    IJ.log("Analysis complete");
  }

  public void getPopulations(HashMap<File, NucleusCollection> folderCollection){
    Set<File> keys = folderCollection.keySet();

    for (File key : keys) {
      NucleusCollection collection = folderCollection.get(key);
      PigSpermNucleusCollection spermNuclei = new PigSpermNucleusCollection(key, "complete");
      IJ.log(key.getAbsolutePath()+"   Nuclei: "+collection.getNucleusCount());

      for(int i=0;i<collection.getNucleusCount();i++){
        Nucleus n = collection.getNucleus(i);
        PigSpermNucleus p = new PigSpermNucleus(n);
        spermNuclei.addNucleus(p);
      }
      this.nuclearPopulations.add(spermNuclei);
    }
  }

  public void analysePopulations(){
    IJ.log("Beginning analysis");

    for(PigSpermNucleusCollection r : this.nuclearPopulations){

      if(r.getDebugFile().exists()){
        r.getDebugFile().delete();
      }

      File folder = r.getFolder();
      IJ.log("  Analysing: "+folder.getName());

      PigSpermNucleusCollection failedNuclei = new PigSpermNucleusCollection(folder, "failed");

      r.refilterNuclei(failedNuclei); // put fails into failedNuclei, remove from r

      IJ.log("Analysing population: "+r.getType());
      IJ.log("  Total nuclei: "+r.getNucleusCount());
      IJ.log("  Red signals: "+r.getRedSignalCount());
      IJ.log("  Green signals: "+r.getGreenSignalCount());

      r.measureProfilePositions();

      r.measureNuclearOrganisation();
      r.annotateAndExportNuclei();

      failedNuclei.annotateAndExportNuclei();

      attemptRefoldingConsensusNucleus(r);

      // split complete set by signals and analyse
      // PigSpermNucleusCollection redNuclei = new PigSpermNucleusCollection(folder, "red");
      // ArrayList<Nucleus> redList = r.getNucleiWithSignals(Nucleus.RED_CHANNEL);
      // for(Nucleus n : redList){
      //   redNuclei.addNucleus( (PigSpermNucleus)n );
      // }
      // redNuclei.annotateAndExportNuclei();
      // attemptRefoldingConsensusNucleus(redNuclei);


    }
  }

  public void attemptRefoldingConsensusNucleus(PigSpermNucleusCollection collection){

    PigSpermNucleus refoldCandidate = (PigSpermNucleus)collection.getNucleusMostSimilarToMedian();
    double[] targetProfile = collection.getMedianTargetCurve(refoldCandidate);

    CurveRefolder refolder = new CurveRefolder(targetProfile, refoldCandidate);
    refolder.refoldCurve();

    // orient refolded nucleus to put tail at the bottom
    refolder.putPointAtBottom(refoldCandidate.getSpermTail());

    // draw signals on the refolded nucleus
    refolder.addSignalsToConsensus(collection);
    refolder.exportImage(collection);

  }
}
