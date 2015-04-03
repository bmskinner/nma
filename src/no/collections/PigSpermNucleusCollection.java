/* 
  -----------------------
  RODENT SPERM NUCLEUS COLLECTION CLASS
  -----------------------
  This class enables filtering for the nucleus type
  It enables offsets to be calculated based on the median normalised curves
*/

package no.collections;

import ij.IJ;

import java.io.File;

import no.nuclei.sperm.*;
import no.components.*;

public class PigSpermNucleusCollection
    extends no.collections.AsymmetricNucleusCollection
{

  public PigSpermNucleusCollection(File folder, String outputFolder, String type){
      super(folder, outputFolder, type);
  }

  public PigSpermNucleusCollection(){
    
  }

//  @Override
//  public void measureProfilePositions(){
//    this.measureProfilePositions("head");
//  }

  /*
    -----------------------
    Identify tail in median profile
    and offset nuclei profiles
    -----------------------
  */
  @Override
  public void findTailIndexInMedianCurve(){
	  // can't use regular tail detector, because it's based on NucleusBorderPoints

	  Profile medianProfile = this.profileCollection.getProfile("head");

	  Profile minima = medianProfile.getLocalMaxima(5); // window size 5

	  //    double minDiff = medianProfile.size();
	  double minAngle = 180;
	  int tailIndex = 0;

	  int tipExclusionIndex1 = (int) (medianProfile.size() * 0.2);
	  int tipExclusionIndex2 = (int) (medianProfile.size() * 0.6);

	  if(minima.size()==0){
		  IJ.log("    Error: no minima found in median line");
		  tailIndex = 100; // set to roughly the middle of the array for the moment

	  } else{

		  for(int i = 0; i<minima.size();i++){
			  if(minima.get(i)==1){
				  int index = (int)minima.get(i);

				  //          int toEnd = medianProfile.size() - index;
				  //          int diff = Math.abs(index - toEnd);

				  double angle = medianProfile.get(index);
				  if(angle>minAngle && index > tipExclusionIndex1 && index < tipExclusionIndex2){ // get the lowest point that is not near the tip
					  minAngle = angle;
					  tailIndex = index;
				  }
			  }
		  }
	  }
	  // IJ.log("    Tail in median profile is at index "+tailIndex+", angle "+minAngle);
	  Profile tailProfile = medianProfile.offset(tailIndex);
	  this.profileCollection.addProfile("tail", tailProfile);
	  this.profileCollection.addFeature("head", new ProfileFeature("tail", tailIndex));

  }

  /*
    Calculate the offsets needed to corectly assign the tail positions
    compared to ideal median curves
  */
  @Override
  public void calculateOffsets(){

    Profile medianToCompare = this.profileCollection.getProfile("head"); // returns a median profile with head at 0

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi
      PigSpermNucleus n = (PigSpermNucleus)this.getNucleus(i);

      // returns the positive offset index of this profile which best matches the median profile
      int newHeadIndex = n.getAngleProfile().getSlidingWindowOffset(medianToCompare);

      n.addBorderTag("head", newHeadIndex);

      // also update the head position
      int tailIndex = n.getIndex(n.findOppositeBorder( n.getPoint(newHeadIndex) ));
      n.addBorderTag("tail", tailIndex);
    }
  }

}