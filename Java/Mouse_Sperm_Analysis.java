/*
  -------------------------------------------------
  MOUSE SPERM CARTOGRAPHY IMAGEJ PLUGIN
  -------------------------------------------------
  Copyright (C) Ben Skinner 2015

  This plugin allows for automated detection of FISH
  signals in a mouse sperm nucleus, and measurement of
  the signal position relative to the nuclear centre of
  mass (CoM) and sperm tip. Works with both red and green channels.
  It also generates a profile of the nuclear shape, allowing
  morphology comparisons
*/
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import java.io.File;
import java.io.IOException;
import java.util.*;
import no.nuclei.*;
import no.nuclei.sperm.*;
import no.analysis.*;
import no.collections.*;

public class Mouse_Sperm_Analysis
  extends ImagePlus
  implements PlugIn
{
   // /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private static final int NUCLEUS_THRESHOLD = 36;
  private static final double MIN_NUCLEAR_SIZE = 2000;
  private static final double MAX_NUCLEAR_SIZE = 10000;
  private static final double MIN_NUCLEAR_CIRC = 0.3;
  private static final double MAX_NUCLEAR_CIRC = 0.8;

  private static final double MIN_SIGNAL_SIZE = 50;

  private ArrayList<RodentSpermNucleusCollection> nuclearPopulations = new ArrayList<RodentSpermNucleusCollection>(0);
  private ArrayList<RodentSpermNucleusCollection> failedPopulations  = new ArrayList<RodentSpermNucleusCollection>(0);
  
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
    IJ.log("All done!"                     );
    IJ.log("----------------------------- ");
  }  

  public void getPopulations(HashMap<File, NucleusCollection> folderCollection){
    Set<File> keys = folderCollection.keySet();

    for (File key : keys) {
      NucleusCollection collection = folderCollection.get(key);
      RodentSpermNucleusCollection spermNuclei = new RodentSpermNucleusCollection(key, "complete");
      IJ.log(key.getAbsolutePath()+"   Nuclei: "+collection.getNucleusCount());

      for(int i=0;i<collection.getNucleusCount();i++){
        Nucleus n = collection.getNucleus(i);
        RodentSpermNucleus p = new RodentSpermNucleus(n);
        spermNuclei.addNucleus(p);
      }
      this.nuclearPopulations.add(spermNuclei);
      IJ.log("  Population converted to Rodent Sperm Nuclei");
    }
  }

  public void analysePopulations(){
    IJ.log("Beginning analysis");

    for(RodentSpermNucleusCollection r : this.nuclearPopulations){

      if(r.getDebugFile().exists()){
        r.getDebugFile().delete();
      }

      File folder = r.getFolder();
      IJ.log("  ----------------------------- ");
      IJ.log("  Analysing: "+folder.getName());
      IJ.log("  ----------------------------- ");

      RodentSpermNucleusCollection failedNuclei = new RodentSpermNucleusCollection(folder, "failed");

      r.refilterNuclei(failedNuclei); // put fails into failedNuclei, remove from r

      IJ.log("    ----------------------------- ");
      IJ.log("    Analysing population: "+r.getType()+" : "+r.getNucleusCount()+" nuclei");
      IJ.log("    ----------------------------- ");

      r.measureProfilePositions();
      r.measureNuclearOrganisation();
      r.exportStatsFiles();
      r.annotateAndExportNuclei();

      IJ.log("    ----------------------------- ");
      IJ.log("    Refolding nucleus"             );
      IJ.log("    ----------------------------- ");

      attemptRefoldingConsensusNucleus(r);

      IJ.log("    ----------------------------- ");
      IJ.log("    Exporting failed nuclei"       );
      IJ.log("    ----------------------------- ");
      failedNuclei.annotateAndExportNuclei();


      ArrayList<RodentSpermNucleusCollection> signalPopulations = dividePopulationBySignals(r);
      
      for(RodentSpermNucleusCollection p : signalPopulations){

        IJ.log("    ----------------------------- ");
        IJ.log("    Analysing population: "+p.getType()+" : "+p.getNucleusCount()+" nuclei");
        IJ.log("    ----------------------------- ");
        p.measureProfilePositions();
        p.exportStatsFiles();
        p.annotateAndExportNuclei();
        attemptRefoldingConsensusNucleus(p);
      }
    }
  }

  public void attemptRefoldingConsensusNucleus(RodentSpermNucleusCollection collection){

    try{ 
      RodentSpermNucleus refoldCandidate = (RodentSpermNucleus)collection.getNucleusMostSimilarToMedian();
      if(refoldCandidate==null){
        throw new Exception();
      }
      double[] targetProfile = collection.getMedianTargetCurve(refoldCandidate);

      CurveRefolder refolder = new CurveRefolder(targetProfile, refoldCandidate);
      refolder.refoldCurve();

      // orient refolded nucleus to put tail at the bottom
      refolder.putPointAtBottom(refoldCandidate.getBorderPointOfInterest("tail"));

      // draw signals on the refolded nucleus
      refolder.addSignalsToConsensus(collection);
      refolder.exportImage(collection);

    } catch(Exception e){
      IJ.log("    Unable to refold nucleus: "+e.getMessage());
    }

  }

  /*
    Given a complete collection of nuclei, split it into up to 4 populations;
      nuclei with red signals, with green signals, without red signals and without green signals
    Only include the 'without' populations if there is a 'with' population.
  */
  public ArrayList<RodentSpermNucleusCollection> dividePopulationBySignals(RodentSpermNucleusCollection r){

    ArrayList<RodentSpermNucleusCollection> signalPopulations = new ArrayList<RodentSpermNucleusCollection>(0);

    ArrayList<Nucleus> redList = r.getNucleiWithSignals(Nucleus.RED_CHANNEL);
    if(redList.size()>0){
      RodentSpermNucleusCollection redNuclei = new RodentSpermNucleusCollection(r.getFolder(), "red");
      for(Nucleus n : redList){
        redNuclei.addNucleus( (RodentSpermNucleus)n );
      }
      signalPopulations.add(redNuclei);
      ArrayList<Nucleus> notRedList = r.getNucleiWithSignals(Nucleus.NOT_RED_CHANNEL);
      if(notRedList.size()>0){
        RodentSpermNucleusCollection notRedNuclei = new RodentSpermNucleusCollection(r.getFolder(), "not_red");
        for(Nucleus n : notRedList){
          notRedNuclei.addNucleus( (RodentSpermNucleus)n );
        }
        signalPopulations.add(notRedNuclei);
      }
    }

    ArrayList<Nucleus> greenList = r.getNucleiWithSignals(Nucleus.GREEN_CHANNEL);
    if(greenList.size()>0){
      RodentSpermNucleusCollection greenNuclei = new RodentSpermNucleusCollection(r.getFolder(), "green");
      for(Nucleus n : greenList){
        greenNuclei.addNucleus( (RodentSpermNucleus)n );
      }
      signalPopulations.add(greenNuclei);
      ArrayList<Nucleus> notGreenList = r.getNucleiWithSignals(Nucleus.NOT_GREEN_CHANNEL);
      if(notGreenList.size()>0){
        RodentSpermNucleusCollection notGreenNuclei = new RodentSpermNucleusCollection(r.getFolder(), "not_green");
        for(Nucleus n : notGreenList){
          notGreenNuclei.addNucleus( (RodentSpermNucleus)n );
        }
        signalPopulations.add(notGreenNuclei);
      }
    }
    return signalPopulations;
  }
}
