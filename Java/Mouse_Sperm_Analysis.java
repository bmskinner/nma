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
    The first method to be run when the plugin starts.
  */
  public void run(String paramString)  {
    
    AnalysisCreator analysisCreator = new AnalysisCreator();

    analysisCreator.setNucleusClass(new RodentSpermNucleus());
    analysisCreator.setNucleusCollectionClass(new RodentSpermNucleusCollection(new File("test"), "", ""));

    analysisCreator.run();
  }  
}
