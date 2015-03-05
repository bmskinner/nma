/* 
  -----------------------
  RODENT SPERM NUCLEUS COLLECTION CLASS
  -----------------------
  This class enables filtering for the nucleus type
  It enables offsets to be calculated based on the median normalised curves
*/

package no.collections;

import ij.IJ;
import java.util.*;
import no.nuclei.*;
import no.nuclei.sperm.*;
import no.components.*;
import no.utility.*;

public class PigSpermNucleusCollection
    extends no.collections.AsymmetricNucleusCollection
{

  public PigSpermNucleusCollection(File folder, String outputFolder, String type){
      super(folder, outputFolder, type);
  }

  public PigSpermNucleusCollection(){
    
  }

  /*
    -----------------------
    Identify tail in median profile
    and offset nuclei profiles
    -----------------------
  */

  //TO BE WRITTEN


}