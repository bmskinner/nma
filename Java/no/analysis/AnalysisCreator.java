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

    HashMap<File, NucleusCollection> folderCollection = detector.getNucleiCollections();

    IJ.log("Imported folder(s)");
    return folderCollection;
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
}
