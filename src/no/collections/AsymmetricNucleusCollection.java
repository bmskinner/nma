/* 
  -----------------------
  ASYMMETRIC NUCLEUS COLLECTION CLASS
  -----------------------
  An asymetric nucleus has a head and a tail; sperm nuclei
  are an example of this. This class enables offsets to be 
  calculated based on median normalised curves from head and
  tail points, plus rotation of nuclei for display in composite
  images
*/

package no.collections;

import ij.IJ;
import java.io.File;
import no.nuclei.*;
import no.components.Profile;
import no.utility.*;

public class AsymmetricNucleusCollection 
	extends no.collections.RoundNucleusCollection
  implements no.collections.NucleusCollection
{

	// failure  codes
//	public static final int FAILURE_HEAD = 128;
//	public static final int FAILURE_TAIL = 256;


	private boolean differencesCalculated = false;



	public AsymmetricNucleusCollection(File folder, String outputFolder, String type, File debugFile){
		super(folder, outputFolder, type, debugFile);
	}

  
  public AsymmetricNucleusCollection(){

  }


  /*
    -----------------------
    General getters
    -----------------------
  */

  public boolean isDifferencesCalculated(){
    return this.differencesCalculated;
  }

  public void setDifferencesCalculated(boolean b){
    this.differencesCalculated = b;
  }

	/*
    -----------------------
    Export data
    -----------------------
  */

  public void exportInterpolatedMedians(Profile medianProfile){

    String logFile = getLogFileName("logOffsets");

    IJ.append("INDEX\tANGLE", logFile);
    for(int i=0;i<medianProfile.size();i++){
      IJ.append(i+"\t"+medianProfile.get(i), logFile);
    }
    IJ.append("", logFile);
  }

  public void exportOffsets(double[] d){

  	String logFile = getLogFileName("logOffsets");

    IJ.append("OFFSET\tDIFFERENCE", logFile);

    for(int i=0;i<d.length;i++){
      IJ.append(i+"\t"+d[i], logFile);
    }
    IJ.append("", logFile);
  }

  public void exportClusteringProfiles(String filename){

    String statsFile = getLogFileName(filename);

    StringBuilder outLine = new StringBuilder();
    outLine.append( "PATH\t"+
                    "POSITION\t"+
                    "AREA\t"+
                    "PERIMETER\t"+
                    "FERET\t"+
                    "PATH_LENGTH\t"+
                    "DIFFERENCE\t"+
                    "FAILURE_CODE\t"+
                    "HEAD_TO_TAIL\t");

    IJ.log("    Exporting clustering profiles...");
    double[] areas        = this.getAreas();
    double[] perims       = this.getPerimeters();
    double[] ferets       = this.getFerets();
    double[] pathLengths  = this.getPathLengths();
    double[] differences  = this.getDifferencesToMedianFromPoint("tail");
    double[] headToTail   = this.getPointToPointDistances("head", "tail");
    String[] paths        = this.getNucleusPaths();

    double maxPerim = Stats.max(perims); // add column headers
    for(int i=0;i<maxPerim;i++){
      outLine.append(i+"\t");
    }
    outLine.append("\r\n");


    // export the profiles for each nucleus
    for(int i=0; i<this.getNucleusCount();i++){

      Nucleus n = (Nucleus)this.getNucleus(i);

      outLine.append( paths[i]        +"\t"+
                      n.getPosition() +"\t"+
                      areas[i]        +"\t"+
                      perims[i]       +"\t"+
                      ferets[i]       +"\t"+
                      pathLengths[i]  +"\t"+
                      differences[i]  +"\t"+
                      headToTail[i]   +"\t");

      
      Profile profile = n.getAngleProfile("tail");
      for(int j=0;j<profile.size();j++){
        outLine.append(profile.get(j)+"\t");
      }
      outLine.append("\r\n");
    }
    IJ.append(  outLine.toString(), statsFile);
    IJ.log("    Cluster export complete");
  }
}