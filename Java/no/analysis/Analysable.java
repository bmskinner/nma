package no.analysis;

import java.lang.*;
import ij.gui.Roi;
import no.nuclei.Nucleus;
import java.io.File;

public interface Analysable
{
   
   public void measureProfilePositions();
   public void measureNuclearOrganisation();
   public void exportStatsFiles();
   public void annotateAndExportNuclei();
   public void refilterNuclei(Analysable failedCollection);

   public void addNucleus(Nucleus r);
   public File getFolder();
   public String getType();
   public File getDebugFile();

   public int getNucleusCount();

}