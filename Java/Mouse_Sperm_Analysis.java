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

    boolean ok = displayOptionsDialog();

    if(!ok) return;

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    if(folderName==null) return;
    File folder = new File(folderName);

    if(performReanalysis){
      OpenDialog fileDialog = new OpenDialog("Select a mapping file...");
      String fileName = fileDialog.getPath();
      if(mappingFile==null) return;
      mappingFile = new File(fileName);
    }

    IJ.log("Directory: "+folderName);
    
    AnalysisCreator analysisCreator = new AnalysisCreator(folder);

    analysisCreator.setNucleusClass(new RodentSpermNucleus());
    analysisCreator.setNucleusCollectionClass(new RodentSpermNucleusCollection(folder, "", ""));

    analysisCreator.setMinNucleusSize(  minNucleusSize );
    analysisCreator.setMaxNucleusSize(  maxNucleusSize );
    analysisCreator.setNucleusThreshold(nucleusThreshold);
    analysisCreator.setMinNucleusCirc(  minNucleusCirc );
    analysisCreator.setMaxNucleusCirc(  maxNucleusCirc );
    analysisCreator.setMinSignalSize(   minSignalSize  );
    analysisCreator.setSignalThreshold( signalThreshold);
    analysisCreator.setAngleProfileWindowSize( angleProfileWindowSize);
    analysisCreator.setMaxSignalFraction( maxSignalFraction);


    if(!performReanalysis){
      analysisCreator.runAnalysis();
    } else {
      analysisCreator.runReAnalysis(mappingFile);
    }

    analysisCreator.assignNucleusTypes();
    analysisCreator.analysePopulations();
    analysisCreator.exportAnalysisLog();

    IJ.log("----------------------------- ");
    IJ.log("All done!"                     );
    IJ.log("----------------------------- ");
  }  

  public boolean displayOptionsDialog(){
    GenericDialog gd = new GenericDialog("New mophology analysis");
    gd.addNumericField("Nucleus threshold: ", nucleusThreshold, 0);
    gd.addNumericField("Signal threshold: ", signalThreshold, 0);
    gd.addNumericField("Min nuclear size: ", minNucleusSize, 0);
    gd.addNumericField("Max nuclear size: ", maxNucleusSize, 0);
    gd.addNumericField("Min nuclear circ: ", minNucleusCirc, 2);
    gd.addNumericField("Max nuclear circ: ", maxNucleusCirc, 2);
    gd.addNumericField("Min signal size: ", minSignalSize, 0);
    gd.addNumericField("Max signal fraction: ", maxSignalFraction, 2);
    gd.addNumericField("Profile window size: ", angleProfileWindowSize, 0);
    gd.addCheckbox("Re-analysis?", false);
    gd.showDialog();
    if (gd.wasCanceled()) return false;

    nucleusThreshold = (int) gd.getNextNumber();
    signalThreshold = (int) gd.getNextNumber();
    minNucleusSize = gd.getNextNumber();
    maxNucleusSize = gd.getNextNumber();
    minNucleusCirc = gd.getNextNumber();
    maxNucleusCirc = gd.getNextNumber();
    minSignalSize = gd.getNextNumber();
    maxSignalFraction = gd.getNextNumber();
    angleProfileWindowSize = (int) gd.getNextNumber();
    performReanalysis = gd.getNextBoolean();
    return true;
  }
}
