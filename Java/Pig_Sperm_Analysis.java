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
import no.collections.*;

public class Pig_Sperm_Analysis
  extends ImagePlus
  implements PlugIn
{

  // failure codes - not in use, keep to add back to logFailed in refilter
  private static final String IMAGE_PREFIX = "export.";

  private static final int    NUCLEUS_THRESHOLD = 40;
  private static final double MIN_NUCLEAR_SIZE  = 1000;
  private static final double MAX_NUCLEAR_SIZE  = 10000;
  private static final double MIN_NUCLEAR_CIRC  = 0.2;
  private static final double MAX_NUCLEAR_CIRC  = 1.0;

  private static final double MIN_SIGNAL_SIZE = 50;

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

    AnalysisCreator analysisCreator = new AnalysisCreator(folder);

    analysisCreator.setMinNucleusSize(  MIN_NUCLEAR_SIZE );
    analysisCreator.setMaxNucleusSize(  MAX_NUCLEAR_SIZE );
    analysisCreator.setMaxNucleusSize(  MAX_NUCLEAR_SIZE );
    analysisCreator.setNucleusThreshold(NUCLEUS_THRESHOLD);
    analysisCreator.setMinNucleusCirc(  MIN_NUCLEAR_CIRC );
    analysisCreator.setMaxNucleusCirc(  MAX_NUCLEAR_CIRC );
    analysisCreator.setMinSignalSize(   MIN_SIGNAL_SIZE  );

    HashMap<File, NucleusCollection> folderCollection = analysisCreator.runAnalysis();

    getPopulations(folderCollection);
    analysePopulations();

    IJ.log("----------------------------- ");
    IJ.log("All done!");
    IJ.log("----------------------------- ");
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
      IJ.log("  Population converted to Pig Sperm Nuclei");
    }
  }

  public void analysePopulations(){
    IJ.log("Beginning analysis");

    for(PigSpermNucleusCollection r : this.nuclearPopulations){

      if(r.getDebugFile().exists()){
        r.getDebugFile().delete();
      }

      File folder = r.getFolder();
      IJ.log("  ----------------------------- ");
      IJ.log("  Analysing: "+folder.getName());
      IJ.log("  ----------------------------- ");

      PigSpermNucleusCollection failedNuclei = new PigSpermNucleusCollection(folder, "failed");

      r.refilterNuclei(failedNuclei); // put fails into failedNuclei, remove from r

      IJ.log("    ----------------------------- ");
      IJ.log("    Analysing population: "+r.getType());
      IJ.log("    ----------------------------- ");
      IJ.log("    Total nuclei: "+r.getNucleusCount());
      IJ.log("    Red signals: "+r.getRedSignalCount());
      IJ.log("    Green signals: "+r.getGreenSignalCount());


      r.measureProfilePositions();
      r.measureNuclearOrganisation();
      r.annotateAndExportNuclei();

      // r.refilterNuclei(failedNuclei);
      IJ.log("    ----------------------------- ");
      IJ.log("    Exporting failed nuclei");
      IJ.log("    ----------------------------- ");
      failedNuclei.annotateAndExportNuclei();

      IJ.log("    ----------------------------- ");
      IJ.log("    Refolding nucleus");
      IJ.log("    ----------------------------- ");

      attemptRefoldingConsensusNucleus(r);

      ArrayList<PigSpermNucleusCollection> signalPopulations = dividePopulationBySignals(r);
      
      for(PigSpermNucleusCollection p : signalPopulations){

        IJ.log("    ----------------------------- ");
        IJ.log("    Analysing population: "+p.getType());
        IJ.log("    ----------------------------- ");
        p.measureProfilePositions();
        p.annotateAndExportNuclei();
        attemptRefoldingConsensusNucleus(p);
      }
    }
  }

  public void attemptRefoldingConsensusNucleus(PigSpermNucleusCollection collection){

    try{ 
      PigSpermNucleus refoldCandidate = (PigSpermNucleus)collection.getNucleusMostSimilarToMedian();
      double[] targetProfile = collection.getMedianTargetCurve(refoldCandidate);

      CurveRefolder refolder = new CurveRefolder(targetProfile, refoldCandidate);
      refolder.refoldCurve();

      // orient refolded nucleus to put tail at the bottom
      refolder.putPointAtBottom(refoldCandidate.getSpermTail());

      // draw signals on the refolded nucleus
      refolder.addSignalsToConsensus(collection);
      refolder.exportImage(collection);

    } catch(Exception e){
      IJ.log("    Unable to refold nucleus: "+e);
    }

  }

  /*
    Given a complete collection of nuclei, split it into up to 4 populations;
      nuclei with red signals, with green signals, without red signals and without green signals
    Only include the 'without' populations if there is a 'with' population.
  */
  public ArrayList<PigSpermNucleusCollection> dividePopulationBySignals(PigSpermNucleusCollection r){

    ArrayList<PigSpermNucleusCollection> signalPopulations = new ArrayList<PigSpermNucleusCollection>(0);

    ArrayList<Nucleus> redList = r.getNucleiWithSignals(Nucleus.RED_CHANNEL);
    if(redList.size()>0){
      PigSpermNucleusCollection redNuclei = new PigSpermNucleusCollection(r.getFolder(), "red");
      for(Nucleus n : redList){
        redNuclei.addNucleus( (PigSpermNucleus)n );
      }
      signalPopulations.add(redNuclei);
      ArrayList<Nucleus> notRedList = r.getNucleiWithSignals(Nucleus.NOT_RED_CHANNEL);
      if(notRedList.size()>0){
        PigSpermNucleusCollection notRedNuclei = new PigSpermNucleusCollection(r.getFolder(), "not_red");
        for(Nucleus n : notRedList){
          notRedNuclei.addNucleus( (PigSpermNucleus)n );
        }
        signalPopulations.add(notRedNuclei);
      }
    }

    ArrayList<Nucleus> greenList = r.getNucleiWithSignals(Nucleus.GREEN_CHANNEL);
    if(greenList.size()>0){
      PigSpermNucleusCollection greenNuclei = new PigSpermNucleusCollection(r.getFolder(), "green");
      for(Nucleus n : greenList){
        greenNuclei.addNucleus( (PigSpermNucleus)n );
      }
      signalPopulations.add(greenNuclei);
      ArrayList<Nucleus> notGreenList = r.getNucleiWithSignals(Nucleus.NOT_GREEN_CHANNEL);
      if(notGreenList.size()>0){
        PigSpermNucleusCollection notGreenNuclei = new PigSpermNucleusCollection(r.getFolder(), "not_green");
        for(Nucleus n : notGreenList){
          notGreenNuclei.addNucleus( (PigSpermNucleus)n );
        }
        signalPopulations.add(notGreenNuclei);
      }
    }
    return signalPopulations;
  }
}