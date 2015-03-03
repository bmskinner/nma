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
// import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;

import no.analysis.AnalysisCreator;
import no.nuclei.sperm.RodentSpermNucleus;
import no.collections.RodentSpermNucleusCollection;

public class Mouse_Sperm_Analysis
  implements PlugIn
{
   
  /* 
    Command line parameters to expect
  */
  private static final String NUCLEUS_THRESHOLD_OPTION = "nt";
  private static final String SIGNAL_THRESHOLD_OPTION  = "st";
  private static final String MIN_NUCLEAR_SIZE_OPTION  = "minN";
  private static final String MAX_NUCLEAR_SIZE_OPTION  = "maxN";
  private static final String MIN_NUCLEAR_CIRC_OPTION  = "minC";
  private static final String MAX_NUCLEAR_CIRC_OPTION  = "maxC";
  private static final String MIN_SIGNAL_SIZE_OPTION   = "minS" ;
  private static final String MAX_SIGNAL_FRACT_OPTION  = "maxSf" ;
  private static final String ANGLE_PROFILE_OPTION     = "aw" ;
  private static final String INPUT_FOLDER_OPTION      = "folder" ;
  private static final String MAPPING_FILE_OPTION      = "mf" ;

  /* 
    Analysis parameters, with defaults
    if no command line parameters are given
  */
  private double minNucleusSize  = 2000;
  private double maxNucleusSize  = 10000;
  private double minNucleusCirc  = 0.3;
  private double maxNucleusCirc  = 0.8;

  private int     nucleusThreshold = 36;
  private int      signalThreshold = 70;
  private double     minSignalSize = 50;
  private double maxSignalFraction = 0.5;

  private int angleProfileWindowSize  = 23;

  private File mappingFile;

  private boolean performReanalysis = false;

   /* 
    The first method to be run when the plugin starts.
  */
  public void run(String paramString)  {
    
    AnalysisCreator analysisCreator = new AnalysisCreator();

    analysisCreator.setNucleusClass(new RodentSpermNucleus());
    analysisCreator.setNucleusCollectionClass(new RodentSpermNucleusCollection(new File("test"), "", ""));

    analysisCreator.run();

    analysisCreator.assignNucleusTypes();
    analysisCreator.analysePopulations();
    analysisCreator.exportAnalysisLog();

    IJ.log("----------------------------- ");
    IJ.log("All done!"                     );
    IJ.log("----------------------------- ");
  }  
}
