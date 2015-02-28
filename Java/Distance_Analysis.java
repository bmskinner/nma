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
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;

import no.analysis.AnalysisCreator;
import no.nuclei.Nucleus;
import no.collections.NucleusCollection;

public class Distance_Analysis
  extends ImagePlus
  implements PlugIn
{

  // Values for deciding whether an object is a signal
  private static final int    SIGNAL_THRESHOLD = 70;
  private static final double MIN_SIGNAL_SIZE  = 4; // how small can a signal be
  private static final double MAX_SIGNAL_FRACTION  = 0.1; // how large can a signal be
  
  /* VALUES FOR DECIDING IF AN OBJECT IS A NUCLEUS */
  private static final int NUCLEUS_THRESHOLD    = 40;
  private static final double MIN_NUCLEAR_SIZE  = 500;
  private static final double MAX_NUCLEAR_SIZE  = 10000;
  private static final double MIN_NUCLEAR_CIRC  = 0.3;
  private static final double MAX_NUCLEAR_CIRC  = 1;

  public void run(String paramString)  {

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    if(folderName==null){
      return;
    }
    IJ.log("Directory: "+folderName);
    File folder = new File(folderName);

    AnalysisCreator analysisCreator = new AnalysisCreator(folder);

    analysisCreator.setNucleusClass(new Nucleus());
    analysisCreator.setNucleusCollectionClass(new NucleusCollection(folder, ""));

    analysisCreator.setMinNucleusSize(  MIN_NUCLEAR_SIZE );
    analysisCreator.setMaxNucleusSize(  MAX_NUCLEAR_SIZE );
    analysisCreator.setMaxNucleusSize(  MAX_NUCLEAR_SIZE );
    analysisCreator.setNucleusThreshold(NUCLEUS_THRESHOLD);
    analysisCreator.setMinNucleusCirc(  MIN_NUCLEAR_CIRC );
    analysisCreator.setMaxNucleusCirc(  MAX_NUCLEAR_CIRC );
    analysisCreator.setMinSignalSize(   MIN_SIGNAL_SIZE  );

    analysisCreator.runAnalysis();
    analysisCreator.assignNucleusTypes();
    analysisCreator.analysePopulations();

    IJ.log("----------------------------- ");
    IJ.log("All done!");
    IJ.log("----------------------------- ");
  }  
}
