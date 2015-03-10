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
import java.io.File;
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

  @Override
  public void measureProfilePositions(){

    this.createProfileAggregateFromPoint("head");

    calculateNormalisedMedianLineFromPoint("head");

    this.findTailIndexInMedianCurve();
    this.calculateOffsets();

    this.createProfileAggregates();

    this.drawProfilePlots();
    this.drawNormalisedMedianLines();

    this.exportProfilePlots();
  }

  /*
    -----------------------
    Identify tail in median profile
    and offset nuclei profiles
    -----------------------
  */

  public void findTailIndexInMedianCurve(){
    // can't use regular tail detector, because it's based on NucleusBorderPoints

    Profile medianProfile = this.getMedianProfile("head");

    List<Integer> minima = medianProfile.getLocalMaxima(5); // window size 5

    double minDiff = medianProfile.size();
    double minAngle = 180;
    int tailIndex = 0;

    if(minima.size()==0){
      IJ.log("    Error: no minima found in median line");
      tailIndex = 100; // set to roughly the middle of the array for the moment

    } else{

      for(int i = 0; i<minima.size();i++){
        Integer index = (Integer)minima.get(i);

        int toEnd = medianProfile.size() - index;
        int diff = Math.abs(index - toEnd);

        double angle = medianProfile.get(index);
        if(angle>minAngle && index > 40 && index < 120){ // get the lowest point that is not near the tip
          minAngle = angle;
          tailIndex = index;
        }
      }
    }
    // IJ.log("    Tail in median profile is at index "+tailIndex+", angle "+minAngle);
    addMedianProfileFeatureIndex("head", "tail", tailIndex); // set the tail-index in the head normalised profile
  }

  /*
    Calculate the offsets needed to corectly assign the tail positions
    compared to ideal median curves
  */
  public void calculateOffsets(){

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi
      PigSpermNucleus n = (PigSpermNucleus)this.getNucleus(i);

      // the curve needs to be matched to the median 
      // hence the median array needs to be the same curve length
      Profile medianToCompare = this.getMedianProfile("head"); // returns a median profile with tip at 0
      // medianToCompare.print();

      Profile interpolatedMedian = medianToCompare.interpolate(n.getLength());
      // interpolatedMedian.print();

      // find the median tail index position in the interplolated median profile
      int medianTailIndex = getMedianProfileFeatureIndex("head", "tail");
      medianTailIndex = (int)Math.round(( (double)medianTailIndex / (double)medianToCompare.size() )* n.getLength());


      int differenceTipToTailInMedianProfile = medianTailIndex;
      int differenceTipToTailInNucleus = n.getBorderIndex("tail") - n.getBorderIndex("head"); // tail index should be larger than tip index because we oriented the array
      int offset = differenceTipToTailInNucleus - differenceTipToTailInMedianProfile;

      int newTailIndex = NuclearOrganisationUtility.wrapIndex(n.getBorderIndex("tail")-offset, n.getLength());

      n.addBorderTag("tail", newTailIndex);

      // also update the head position
      int headIndex = n.getIndex(n.findOppositeBorder( n.getPoint(newTailIndex) ));
      n.addBorderTag("head", headIndex);
    }
  }


}