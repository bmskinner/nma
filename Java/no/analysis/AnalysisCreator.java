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
  private  double minNucleusSize   = 500;
  private  double maxNucleusSize   = 10000;
  private  double minNucleusCirc   = 0.0;
  private  double maxNucleusCirc   = 1.0;

  private  double minSignalSize = 5;
  private  double maxSignalSize = 500;

  private ArrayList<PigSpermNucleusCollection> nuclearPopulations = new ArrayList<PigSpermNucleusCollection>(0);
  private ArrayList<PigSpermNucleusCollection> failedPopulations  = new ArrayList<PigSpermNucleusCollection>(0);
  
  public AnalysisCreator(){
  	// create with default permissive parameters
  }


}
