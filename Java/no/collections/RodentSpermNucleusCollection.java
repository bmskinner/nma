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
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.ProgressBar;
import ij.gui.TextRoi;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;
import ij.measure.ResultsTable;
import ij.measure.SplineFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;
import no.nuclei.*;
import no.nuclei.sperm.*;
import no.components.*;
import no.utility.*;

public class RodentSpermNucleusCollection 
	extends no.collections.AsymmetricNucleusCollection
{


  // failure  codes
  public static final int FAILURE_TIP = 512;

	// private String logMedianFromTipFile  = "logMediansFromTip"; // output medians

	// private double[] normalisedMedianProfileFromTip; // this is an array of 200 angles

  // private Map<Double, Collection<Double>> normalisedProfilesFromTip  = new HashMap<Double, Collection<Double>>();

  public RodentSpermNucleusCollection(File folder, String type){
  		super(folder, type);
  }

  @Override
  public void measureProfilePositions(){

    this.createProfileAggregateFromPoint("tip");

    calculateNormalisedMedianLineFromPoint("tip");

    this.findTailIndexInMedianCurve();
    this.calculateOffsets();

    this.createProfileAggregates();

    this.drawProfilePlots();
    this.drawNormalisedMedianLines();

    this.calculateDifferencesToMedianProfiles();
    this.exportProfilePlots();
  }

  /*
    -----------------------
    Get values relating to sperm
    profiles
    -----------------------
  */

  // public double getMaxRawXFromTips(){
  //   double d = 0;
  //   for(int i=0;i<this.getNucleusCount();i++){
  //     RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);
  //     if(n.getMaxRawXFromTip() > d){
  //       d = n.getMaxRawXFromTip();
  //     }
  //   }
  //   return d;
  // }

  // public double getMinRawXFromTips(){
  //   double d = 0;
  //   for(int i=0;i<this.getNucleusCount();i++){
  //     RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);
  //     if(n.getMaxRawXFromTip() < d){
  //       d = n.getMaxRawXFromTip();
  //     }
  //   }
  //   return d;
  // }

  // public double[] getHeadToTipDistances(){
  //   double[] d = new double[this.getNucleusCount()];
  //   for(int i=0;i<this.getNucleusCount();i++){
  //     RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);
  //     d[i] = n.getBorderPointOfInterest("tip").getLengthTo(n.getBorderPointOfInterest("head"));
  //   }
  //   return d;
  // }

  // public double[] getTailToTipDistances(){
  //   double[] d = new double[this.getNucleusCount()];
  //   for(int i=0;i<this.getNucleusCount();i++){
  //     RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);
  //     d[i] = n.getBorderPointOfInterest("tip").getLengthTo(n.getBorderPointOfInterest("tail"));
  //   }
  //   return d;
  // }

  /*
    -----------------------
    Setters
    -----------------------
  */

  /*
    -----------------------
    Filters for nuclei
    -----------------------
  */


  /*
    -----------------------
    Create and manipulate
    aggregate profiles. These
    can be centred on tip  or tail
    -----------------------
  */

	public void findTailIndexInMedianCurve(){
		// can't use regular tail detector, because it's based on NucleusBorderPoints
		// get minima in curve, then find the lowest minima / minima furthest from both ends

    double[] medianProfile = this.getNormalisedMedianProfileFromPoint("tip");

		ArrayList<Integer> minima = this.detectLocalMinimaInMedian(medianProfile);

		double minDiff = medianProfile.length;
		double minAngle = 180;
		int tailIndex = 0;

    if(minima.size()==0){
      IJ.log("    Error: no minima found in median line");
      tailIndex = 100; // set to roughly the middle of the array for the moment

    } else{

  		for(int i = 0; i<minima.size();i++){
  			Integer index = (Integer)minima.get(i);

  			int toEnd = medianProfile.length - index;
  			int diff = Math.abs(index - toEnd);

  			double angle = medianProfile[index];
  			if(angle<minAngle && index > 40 && index < 120){ // get the lowest point that is not near the tip
  				minAngle = angle;
  				tailIndex = index;
  			}
  		}
  	}
    addMedianProfileFeatureIndex("tip", "tail", tailIndex); // set the tail-index in the tip normalised profile
	}

  /*
    Calculate the offsets needed to corectly assign the tail positions
    compared to ideal median curves
  */
  public void calculateOffsets(){

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi
      RodentSpermNucleus n = (RodentSpermNucleus) this.getNucleus(i);

      // the curve needs to be matched to the median 
      // hence the median array needs to be the same curve length
      double[] medianToCompare = this.getNormalisedMedianProfileFromPoint("tip");

      double[] interpolatedMedian = NucleusCollection.interpolateMedianToLength(n.getLength(), medianToCompare);

      // find the median tail index position in the interplolated median profile
      int medianTailIndex = getMedianProfileFeatureIndex("tip", "tail");
      medianTailIndex = (int)Math.round(( (double)medianTailIndex / (double)medianToCompare.length )* n.getLength());
      
      int offset = n.getBorderIndexOfInterest("tail") - medianTailIndex;
      IJ.log("Offset: "+offset);

      // n.setOffsetForTail(offset);

      int newTailIndex = NuclearOrganisationUtility.wrapIndex(n.getTailIndex()-offset, n.getLength());

      // n.setTailIndex(newTailIndex); // update the tail position
      // addMedianProfileFeatureIndex("tip", "tail", tailIndex);
      n.addBorderPointOfInterest("tail", n.getBorderPoint(newTailIndex));
      // n.setSpermTail(n.getBorderPoint(n.getTailIndex())); // ensure the spermTail is updated

      // also update the head position
      n.addBorderPointOfInterest("head", n.findOppositeBorder( n.getBorderPoint(newTailIndex) ));
    }
  }


	/*
    -----------------------
    Export data
    -----------------------
  */

  /*
    Draw the features of interest on the images of the nuclei created earlier
  */
  public void annotateImagesOfNuclei(){
  	IJ.log("    Annotating images ("+this.getType()+")...");
  	for(int i=0; i<this.getNucleusCount();i++){
  		RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);
  		n.annotateFeatures();
  	}
  	 IJ.log("    Annotation complete");
  }

  /*
    -----------------------
    Export data
    -----------------------
  */

  @Override
  public void exportClusteringProfiles(String filename){
    String statsFile = makeGlobalLogFile(filename);

    StringBuilder outLine = new StringBuilder();
    outLine.append("PATH\tAREA\tPERIMETER\tFERET\tPATH_LENGTH\tDIFFERENCE\tFAILURE_CODE\tHEAD_TO_TAIL\tTIP_TO_TAIL\tHEAD_TO_TIP\t");

    IJ.log("    Exporting clustering profiles ("+this.getType()+")...");
    double[] areas        = this.getAreas();
    double[] perims       = this.getPerimeters();
    double[] ferets       = this.getFerets();
    double[] pathLengths  = this.getPathLengths();
    double[] differences  = this.getDifferencesToMedianFromPoint("tail");
    double[] headToTail   = this.getPointToPointDistances("head", "tail");
    double[] headToTip    = this.getPointToPointDistances("head", "tip");
    double[] tipToTail    = this.getPointToPointDistances("tail", "tip");
    String[] paths        = this.getNucleusPaths();

    double maxPerim = NuclearOrganisationUtility.getMax(perims); // add column headers
    for(int i=0;i<maxPerim;i++){
      outLine.append(i+"\t");
    }
    outLine.append("\n");

    // export the profiles for each nucleus
    for(int i=0; i<this.getNucleusCount();i++){

      outLine.append(paths[i]      +"\t"+
                  areas[i]      +"\t"+
                  perims[i]     +"\t"+
                  ferets[i]     +"\t"+
                  pathLengths[i]+"\t"+
                  differences[i]+"\t"+
                  headToTail[i] +"\t"+
                  tipToTail[i]  +"\t"+
                  headToTip[i]  +"\t");

      AsymmetricNucleus n = (AsymmetricNucleus)this.getNucleus(i);
      double[] profile = n.getAngleProfile().getInteriorAngles(n.getTailIndex());
      for(int j=0;j<profile.length;j++){
        outLine.append(profile[j]+"\t");
      }
      outLine.append("\n");
    }
    IJ.append(  outLine.toString(), statsFile);
    IJ.log("    Cluster export complete");
  }
}