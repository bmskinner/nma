/* 
  -----------------------
  RODENT SPERM NUCLEUS COLLECTION CLASS
  -----------------------
  Rodent sperm differ from other mammalian sperm in that
  they have a hook shape. Consequently, thre is a left and right
  plus a tip after the acrosomal curve that is more useful 
  to detect on than the head point (which is within the acrosome)
*/

package no.collections;

import ij.IJ;

import java.io.File;

import no.nuclei.*;
import no.nuclei.sperm.*;
import no.components.*;
import no.utility.*;

public class RodentSpermNucleusCollection 
	extends no.collections.AsymmetricNucleusCollection
{


  // failure  codes
//  public static final int FAILURE_TIP = 512;
  
  private String DEFAULT_REFERENCE_POINT = "tip";

  public RodentSpermNucleusCollection(File folder, String outputFolder, String type, File debugFile){
	  super(folder, outputFolder, type, debugFile);
  }

  public RodentSpermNucleusCollection(){
    
  }

//  @Override
//  public void measureProfilePositions(){
//    this.measureProfilePositions("tip");
//  }
  
  @Override
  public String getReferencePoint(){
	  return this.DEFAULT_REFERENCE_POINT;
  }


  /*
    -----------------------
    Create and manipulate
    aggregate profiles. These
    can be centred on tip  or tail
    -----------------------
  */
  @Override
  public void findTailIndexInMedianCurve(){
	  // can't use regular tail detector, because it's based on NucleusBorderPoints
	  // get minima in curve, then find the lowest minima / minima furthest from both ends

	  Profile medianProfile = this.profileCollection.getProfile(this.getReferencePoint());

	  Profile minima = medianProfile.smooth(2).getLocalMinima(5); // window size 5

	  //		double minDiff = medianProfile.size();
	  double minAngle = 180;
	  int tailIndex = 0;
	  
	  int tipExclusionIndex1 = (int) (medianProfile.size() * 0.2);
	  int tipExclusionIndex2 = (int) (medianProfile.size() * 0.6);

	  for(int i = 0; i<minima.size();i++){
		  if( (int)minima.get(i)==1){
			  int index = i;

			  double angle = medianProfile.get(index);
			  if(angle<minAngle && index > tipExclusionIndex1 && index < tipExclusionIndex2){ // get the lowest point that is not near the tip
				  minAngle = angle;
				  tailIndex = index;
			  }
		  }
	  }
	  Profile tailProfile = medianProfile.offset(tailIndex);
	  this.profileCollection.addProfile(this.getOrientationPoint(), tailProfile);
	  this.profileCollection.addFeature(this.getReferencePoint(), new ProfileFeature(this.getOrientationPoint(), tailIndex)); // set the tail-index in the tip normalised profile
  }

  /*
    Calculate the offsets needed to correctly assign the tail positions
    compared to ideal median curves
  */
  @Override
  public void calculateOffsets(){

    Profile medianToCompare = this.profileCollection.getProfile(this.getOrientationPoint()); // returns a median profile

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi
      RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);


      // THE NEW WAY
      int newTailIndex = n.getAngleProfile().getSlidingWindowOffset(medianToCompare);

      n.addBorderTag("tail", newTailIndex);

      // also update the head position
      int headIndex = n.getIndex(n.findOppositeBorder( n.getPoint(newTailIndex) ));
      n.addBorderTag("head", headIndex);
      n.splitNucleusToHeadAndHump();
    }
  }


	/*
    -----------------------
    Export data
    -----------------------
  */

  /*
    -----------------------
    Export data
    -----------------------
  */

  @Override
  public void exportClusteringProfiles(String filename){
    String statsFile = getLogFileName(filename);

    StringBuilder outLine = new StringBuilder();
    outLine.append("PATH\tPOSITION\tAREA\tPERIMETER\tFERET\tPATH_LENGTH\tDIFFERENCE\tFAILURE_CODE\tHEAD_TO_TAIL\tTIP_TO_TAIL\tHEAD_TO_TIP\t");

    IJ.log("    Exporting clustering profiles...");
    double[] areas        = this.getAreas();
    double[] perims       = this.getPerimeters();
    double[] ferets       = this.getFerets();
    double[] pathLengths  = this.getPathLengths();
    double[] differences  = this.getDifferencesToMedianFromPoint("tail");
    double[] headToTail   = this.getPointToPointDistances("head", "tail");
    double[] headToTip    = this.getPointToPointDistances("head", "tip");
    double[] tipToTail    = this.getPointToPointDistances("tail", "tip");
    String[] paths        = this.getNucleusPaths();

    double maxPerim = Stats.max(perims); // add column headers
    for(int i=0;i<maxPerim;i++){
      outLine.append(i+"\t");
    }
    outLine.append("\r\n");

    // export the profiles for each nucleus
    for(int i=0; i<this.getNucleusCount();i++){

      Nucleus n = (Nucleus)this.getNucleus(i);

      outLine.append(paths[i]      +"\t"+
                      n.getPosition() +"\t"+
                  areas[i]      +"\t"+
                  perims[i]     +"\t"+
                  ferets[i]     +"\t"+
                  pathLengths[i]+"\t"+
                  differences[i]+"\t"+
                  headToTail[i] +"\t"+
                  tipToTail[i]  +"\t"+
                  headToTip[i]  +"\t");

      
      double[] profile = n.getAngleProfile("tail").interpolate((int)maxPerim).asArray();
      for(int j=0;j<profile.length;j++){
        outLine.append(profile[j]+"\t");
      }
      outLine.append("\r\n");
    }
    IJ.append(  outLine.toString(), statsFile);
    IJ.log("    Cluster export complete");
  }
}