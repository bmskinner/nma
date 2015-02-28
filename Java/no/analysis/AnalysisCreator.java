/*
-------------------------------------------------
ANALYSIS CREATOR
-------------------------------------------------
Copyright (C) Ben Skinner 2015

This class allows easy setup of the parameters that
can be varied in the nucleus and signal detection

*/
package no.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;
import ij.plugin.PlugIn;
import java.awt.Color;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import no.nuclei.*;
import no.analysis.*;
import no.utility.*;
import no.collections.*;


public class AnalysisCreator {

	 // /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private  int    nucleusThreshold = 36;
  private  int    signalThreshold  = 70;
  private  double minNucleusSize   = 500;
  private  double maxNucleusSize   = 10000;
  private  double minNucleusCirc   = 0.0;
  private  double maxNucleusCirc   = 1.0;

  private  double minSignalSize = 5;
  private  double maxSignalFraction = 0.5;

  private File folder;
  private File nucleiToFind;

  private Class nucleusClass;
  private Class collectionClass;

  // the raw input from nucleus detector
  private HashMap<File, NucleusCollection> folderCollection;

  private ArrayList<Analysable> nuclearPopulations = new ArrayList<Analysable>(0);
  private ArrayList<Analysable> failedPopulations  = new ArrayList<Analysable>(0);
  

  /*
    -----------------------
    Constructors
    -----------------------
  */
  public AnalysisCreator(File folder){
  	// create with default permissive parameters
    this.folder = folder;
  }

  /*
    -----------------------
    Run the analysis
    -----------------------
  */

  /**
   * Returns a HashMap<File, NucleusCollection> object. This 
   * contains the nuclei found, keyed to the folder in which
   * they were found. 
   *
   * @return      the nuclei in each folder analysed
   * @see         NucleusCollection
   */

  public HashMap<File, NucleusCollection> runAnalysis(){
    NucleusDetector detector = new NucleusDetector(this.folder);

    setDetectionParameters(detector);
    detector.runDetector();

    // HashMap<File, NucleusCollection> folderCollection = detector.getNucleiCollections();
    this.folderCollection = detector.getNucleiCollections();



    IJ.log("Imported folder(s)");
    return this.folderCollection;
  }

  /**
   * Returns a HashMap<File, NucleusCollection> object. This 
   * contains the nuclei found, keyed to the folder in which
   * they were found. It filters the nuclei based on whether they
   * are present in a list of previously captured images.
   *
   * @param  nucleiToFind  a File containing the image names and nuclear coordinates
   * @return      the nuclei in each folder analysed
   * @see         NucleusCollection
   */
  public HashMap<File, NucleusCollection> runReAnalysis(File nucleiToFind){
    NucleusRefinder detector = new NucleusRefinder(this.folder, nucleiToFind);
    setDetectionParameters(detector);
    detector.runDetector();

    HashMap<File, NucleusCollection> folderCollection = detector.getNucleiCollections();

    IJ.log("Imported folder(s)");
    return folderCollection;
  }

  private void  setDetectionParameters(NucleusDetector detector){
    detector.setMinNucleusSize(this.getMinNucleusSize()); 
    detector.setMaxNucleusSize(this.getMaxNucleusSize());
    detector.setThreshold(this.getNucleusThreshold());
    detector.setMinNucleusCirc(this.getMinNucleusCirc());
    detector.setMaxNucleusCirc(this.getMaxNucleusCirc());

    detector.setSignalThreshold(this.getSignalThreshold());
    detector.setMinSignalSize(this.getMinSignalSize());
    detector.setMaxSignalFraction(this.getMaxSignalFraction());
  }

  /*
    -----------------------
    Getters
    -----------------------
  */

  public int getNucleusThreshold(){
    return this.nucleusThreshold;
  }

  public int getSignalThreshold(){
    return this.signalThreshold;
  }

  public double getMinNucleusSize(){
    return this.minNucleusSize;
  }

  public double getMaxNucleusSize(){
    return this.maxNucleusSize;
  }

  public double getMinNucleusCirc(){
    return this.minNucleusCirc;
  }

  public double getMaxNucleusCirc(){
    return this.maxNucleusCirc;
  }

  public double getMinSignalSize(){
    return this.minSignalSize;
  }

  public double getMaxSignalFraction(){
    return this.maxSignalFraction;
  }

  /*
    -----------------------
    Setters
    -----------------------
  */

  public void setNucleusThreshold(int i){
    this.nucleusThreshold = i;
  }

  public void setSignalThreshold(int i){
    this.signalThreshold = i;
  }

  public void setMinNucleusSize(double d){
    this.minNucleusSize = d;
  }

  public void setMaxNucleusSize(double d){
    this.maxNucleusSize = d;
  }

  public void setMinNucleusCirc(double d){
    this.minNucleusCirc = d;
  }

  public void setMaxNucleusCirc(double d){
    this.maxNucleusCirc = d;
  }

  public void setMinSignalSize(double d){
    this.minSignalSize = d;
  }

  public void setMaxSignalFraction(double d){
    this.maxSignalFraction = d;
  }

  public void setNucleusClass(Nucleus n){
    this.nucleusClass = n.getClass();
  }

  public void setNucleusCollectionClass(NucleusCollection n){
    this.collectionClass = n.getClass();
  }

  /*
    Use reflection to assign the correct class to the nuclei and populations
  */
  public void assignNucleusTypes(){
    
    Set<File> keys = this.folderCollection.keySet();

    try{
      Constructor collectionConstructor = this.collectionClass.getConstructor(new Class[]{File.class, String.class});
      Constructor nucleusConstructor = this.nucleusClass.getConstructor(new Class[]{Nucleus.class});
    
      for (File key : keys) {
        NucleusCollection collection = folderCollection.get(key);

        try{
          Analysable spermNuclei = (Analysable) collectionConstructor.newInstance(key, "complete");
          
          // RodentSpermNucleusCollection spermNuclei = new RodentSpermNucleusCollection(key, "complete");
          IJ.log(key.getAbsolutePath()+"   Nuclei: "+collection.getNucleusCount());

          for(int i=0;i<collection.getNucleusCount();i++){
            Nucleus p = collection.getNucleus(i);

            INuclearFunctions subNucleus  = (INuclearFunctions) nucleusConstructor.newInstance(p);

            // RodentSpermNucleus p = new RodentSpermNucleus(n);
            spermNuclei.addNucleus(p);
          }
          this.nuclearPopulations.add(spermNuclei);
          IJ.log("  Population converted to "+nucleusClass.getName()+" in "+spermNuclei.getClass().getName());
        } catch(InstantiationException e){
          IJ.log("Cannot create collection: "+e.getMessage());
        } catch(IllegalAccessException e){
          IJ.log("Cannot access constructor: "+e.getMessage());
        } catch(InvocationTargetException e){
          IJ.log("Cannot invoke constructor: "+e.getMessage());
        }
      }
    } catch(NoSuchMethodException e){
      IJ.log("Cannot find constructor: "+e.getMessage());
    }

  }

  public void analysePopulations(){
    IJ.log("Beginning analysis");

    for(Analysable r : this.nuclearPopulations){

      if(r.getDebugFile().exists()){
        r.getDebugFile().delete();
      }

      File folder = r.getFolder();
      IJ.log("  ----------------------------- ");
      IJ.log("  Analysing: "+folder.getName());
      IJ.log("  ----------------------------- ");

      try{

        Constructor collectionConstructor = this.collectionClass.getConstructor(new Class[]{File.class, String.class});
        Analysable failedNuclei = (Analysable) collectionConstructor.newInstance(folder, "failed");

        r.refilterNuclei(failedNuclei); // put fails into failedNuclei, remove from r
        IJ.log("    ----------------------------- ");
        IJ.log("    Exporting failed nuclei"       );
        IJ.log("    ----------------------------- ");
        failedNuclei.annotateAndExportNuclei();

      } catch(InstantiationException e){
        IJ.log("Cannot create collection: "+e.getMessage());
      } catch(IllegalAccessException e){
        IJ.log("Cannot access constructor: "+e.getMessage());
      } catch(InvocationTargetException e){
        IJ.log("Cannot invoke constructor: "+e.getMessage());
      } catch(NoSuchMethodException e){
        IJ.log("Cannot find constructor: "+e.getMessage());
      }

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

      // attemptRefoldingConsensusNucleus(r);

    

      // ArrayList<Analysable> signalPopulations = dividePopulationBySignals(r);
      
      // for(Analysable p : signalPopulations){

      //   IJ.log("    ----------------------------- ");
      //   IJ.log("    Analysing population: "+p.getType()+" : "+p.getNucleusCount()+" nuclei");
      //   IJ.log("    ----------------------------- ");
      //   p.measureProfilePositions();
      //   p.exportStatsFiles();
      //   p.annotateAndExportNuclei();
      //   attemptRefoldingConsensusNucleus(p);
      // }
    }
  }

}
