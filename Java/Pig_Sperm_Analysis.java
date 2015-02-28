/*
-------------------------------------------------
PIG SPERM MORPHOLOGY ANALYSIS: IMAGEJ PLUGIN
-------------------------------------------------
Copyright (C) Ben Skinner 2015

  

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
import no.nuclei.sperm.PigSpermNucleus;
import no.collections.PigSpermNucleusCollection;

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

public void run(String paramString)  {

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    if(folderName==null){
      return;
    }
    IJ.log("Directory: "+folderName);
    File folder = new File(folderName);


    AnalysisCreator analysisCreator = new AnalysisCreator(folder);

    analysisCreator.setNucleusClass(new PigSpermNucleus());
    analysisCreator.setNucleusCollectionClass(new PigSpermNucleusCollection(folder, ""));

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
    IJ.log("All done!"                     );
    IJ.log("----------------------------- ");
  }  
}