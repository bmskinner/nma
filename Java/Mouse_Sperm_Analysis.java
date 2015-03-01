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

  public void run(String paramString)  {

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    if(folderName==null){
      return;
    }
    IJ.log("Directory: "+folderName);
    File folder = new File(folderName);

    AnalysisCreator analysisCreator = new AnalysisCreator(folder);

    analysisCreator.setNucleusClass(new RodentSpermNucleus());
    analysisCreator.setNucleusCollectionClass(new RodentSpermNucleusCollection(folder, "", ""));

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
    analysisCreator.exportAnalysisLog();

    IJ.log("----------------------------- ");
    IJ.log("All done!"                     );
    IJ.log("----------------------------- ");
  }  
}
