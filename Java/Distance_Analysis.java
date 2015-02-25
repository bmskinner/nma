/*
-------------------------------------------------
SUBTELOMERIC PROBE ANALYSIS: IMAGEJ PLUGIN
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

public class Distance_Analysis
  extends ImagePlus
  implements PlugIn
{

  // colour channels
  private static final int RED_CHANNEL   = 0;
  private static final int GREEN_CHANNEL = 1;
  private static final int BLUE_CHANNEL  = 2;

  private static final String[] fileTypes = {".tif", ".tiff", ".jpg"};

  // Values for deciding whether an object is a signal
  private static final int    SIGNAL_THRESHOLD = 70;
  private static final double MIN_SIGNAL_SIZE  = 4; // how small can a signal be
  private static final double MAX_SIGNAL_FRACTION  = 0.1; // how large can a signal be
  
  // private static final double ANGLE_THRESHOLD = 40.0; // when calculating local minima, ignore angles above this

  /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private static final int NUCLEUS_THRESHOLD    = 40;
  private static final double MIN_NUCLEAR_SIZE  = 500;
  private static final double MAX_NUCLEAR_SIZE  = 10000;
  private static final double MIN_NUCLEAR_CIRC  = 0.3;
  private static final double MAX_NUCLEAR_CIRC  = 1;

  private ArrayList<NucleusCollection> nuclearPopulations = new ArrayList<NucleusCollection>(0);
  private ArrayList<NucleusCollection> failedPopulations  = new ArrayList<NucleusCollection>(0);
  
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
    analysisCreator.setNucleusThreshold(NUCLEUS_THRESHOLD);
    analysisCreator.setMinNucleusCirc(  MIN_NUCLEAR_CIRC );
    analysisCreator.setMaxNucleusCirc(  MAX_NUCLEAR_CIRC );
    analysisCreator.setMinSignalSize(   MIN_SIGNAL_SIZE  );
    analysisCreator.setMaxSignalFraction(   MAX_SIGNAL_FRACTION  );

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
      NucleusCollection complete = new NucleusCollection(key, "complete");
      IJ.log(key.getAbsolutePath()+"   Nuclei: "+collection.getNucleusCount());

      for(int i=0;i<collection.getNucleusCount();i++){
        Nucleus n = collection.getNucleus(i);
        complete.addNucleus(n);
      }
      this.nuclearPopulations.add(complete);
    }
  }

  public void analysePopulations(){
    IJ.log("Beginning analysis");

    for(NucleusCollection r : this.nuclearPopulations){

      if(r.getDebugFile().exists()){
        r.getDebugFile().delete();
      }

      File folder = r.getFolder();
      IJ.log("  ----------------------------- ");
      IJ.log("  Analysing: "+folder.getName());
      IJ.log("  ----------------------------- ");

      NucleusCollection failedNuclei = new NucleusCollection(folder, "failed");

      r.refilterNuclei(failedNuclei); // put fails into failedNuclei, remove from r

      IJ.log("    ----------------------------- ");
      IJ.log("    Analysing population: "+r.getType());
      IJ.log("    ----------------------------- ");
      IJ.log("    Total nuclei: "+r.getNucleusCount());
      IJ.log("    Red signals: "+r.getRedSignalCount());
      IJ.log("    Green signals: "+r.getGreenSignalCount());

      r.measureNuclearOrganisation();
      r.annotateAndExportNuclei();

      IJ.log("    ----------------------------- ");
      IJ.log("    Exporting failed nuclei");
      IJ.log("    ----------------------------- ");

      failedNuclei.annotateAndExportNuclei();

      ArrayList<NucleusCollection> signalPopulations = dividePopulationBySignals(r);
      
      for(NucleusCollection p : signalPopulations){

        IJ.log("    ----------------------------- ");
        IJ.log("    Analysing population: "+p.getType());
        IJ.log("    ----------------------------- ");

        p.annotateAndExportNuclei();
      }
    }
  }

  /*
    Given a complete collection of nuclei, split it into up to 4 populations;
      nuclei with red signals, with green signals, without red signals and without green signals
    Only include the 'without' populations if there is a 'with' population.
  */
  public ArrayList<NucleusCollection> dividePopulationBySignals(NucleusCollection r){

    ArrayList<NucleusCollection> signalPopulations = new ArrayList<NucleusCollection>(0);

    ArrayList<Nucleus> redList = r.getNucleiWithSignals(Nucleus.RED_CHANNEL);
    if(redList.size()>0){
      NucleusCollection redNuclei = new NucleusCollection(r.getFolder(), "red");
      for(Nucleus n : redList){
        redNuclei.addNucleus( (Nucleus)n );
      }
      signalPopulations.add(redNuclei);
      ArrayList<Nucleus> notRedList = r.getNucleiWithSignals(Nucleus.NOT_RED_CHANNEL);
      if(notRedList.size()>0){
        NucleusCollection notRedNuclei = new NucleusCollection(r.getFolder(), "not_red");
        for(Nucleus n : notRedList){
          notRedNuclei.addNucleus( (Nucleus)n );
        }
        signalPopulations.add(notRedNuclei);
      }
    }

    ArrayList<Nucleus> greenList = r.getNucleiWithSignals(Nucleus.GREEN_CHANNEL);
    if(greenList.size()>0){
      NucleusCollection greenNuclei = new NucleusCollection(r.getFolder(), "green");
      for(Nucleus n : greenList){
        greenNuclei.addNucleus( (Nucleus)n );
      }
      signalPopulations.add(greenNuclei);
      ArrayList<Nucleus> notGreenList = r.getNucleiWithSignals(Nucleus.NOT_GREEN_CHANNEL);
      if(notGreenList.size()>0){
        NucleusCollection notGreenNuclei = new NucleusCollection(r.getFolder(), "not_green");
        for(Nucleus n : notGreenList){
          notGreenNuclei.addNucleus( (Nucleus)n );
        }
        signalPopulations.add(notGreenNuclei);
      }
    }
    return signalPopulations;
  }

}
