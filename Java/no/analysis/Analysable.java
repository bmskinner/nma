package no.analysis;

import java.lang.*;
import no.nuclei.Nucleus;

public interface Analysable
{
   
   public void measureProfilePositions();
   public void measureNuclearOrganisation();
   public void exportStatsFiles();
   public void annotateAndExportNuclei();

   public void addNucleus(Nucleus r);

}