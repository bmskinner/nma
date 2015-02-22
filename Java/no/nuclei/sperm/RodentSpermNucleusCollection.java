/* 
  -----------------------
  RODENT SPERM NUCLEUS COLLECTION CLASS
  -----------------------
  This class enables filtering for the nucleus type
  It enables offsets to be calculated based on the median normalised curves
*/

package no.nuclei.sperm;

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

public class RodentSpermNucleusCollection 
	extends no.nuclei.NucleusCollection
{

	// Chart drawing parameters
  private static final int CHART_WINDOW_HEIGHT     = 300;
  private static final int CHART_WINDOW_WIDTH      = 400;
  private static final int CHART_TAIL_BOX_Y_MIN    = 325;
  private static final int CHART_TAIL_BOX_Y_MID    = 340;
  private static final int CHART_TAIL_BOX_Y_MAX    = 355;
  private static final int CHART_SIGNAL_Y_LINE_MIN = 275;
  private static final int CHART_SIGNAL_Y_LINE_MAX = 315;

  // failure  codes
  public static final int FAILURE_TIP       = 1;
  public static final int FAILURE_TAIL      = 2;

  private static final double PROFILE_INCREMENT = 0.5;

	private String logMedianFromTipFile  = "logMediansFromTip"; // output medians
  private String logMedianFromTailFile = "logMediansFromTail"; // output medians

	// private ArrayList<RodentSpermNucleus> nucleiCollection = new ArrayList<RodentSpermNucleus>(0); // store all the nuclei analysed

	private double[] normalisedMedianLineFromTip; // this is an array of 200 angles
  private double[] normalisedMedianLineFromTail; // this is an array of 200 angles

	private boolean differencesCalculated = false;

  private Map<Double, Collection<Double>> normalisedProfilesFromTip  = new HashMap<Double, Collection<Double>>();
  private Map<Double, Collection<Double>> normalisedProfilesFromTail = new HashMap<Double, Collection<Double>>();

	private int offsetCount = 20;
	private int medianLineTailIndex;

  private Plot  rawXFromTipPlot;
  private Plot normXFromTipPlot;
  private Plot  rawXFromTailPlot;
  private Plot normXFromTailPlot;

  private PlotWindow rawXFromTipWindow;
  private PlotWindow normXFromTipWindow;
  private PlotWindow rawXFromTailWindow;
  private PlotWindow normXFromTailWindow;

  private double maxDifferenceFromMedian = 1.5; // used to filter the nuclei, and remove those too small, large or irregular to be real
  private double maxWibblinessFromMedian = 1.2; // filter for the irregular borders more stringently

  public RodentSpermNucleusCollection(File folder, String type){
  		super(folder, type);
  }

  /*
    -----------------------
    Get values relating to sperm
    profiles
    -----------------------
  */

  public ArrayList<RodentSpermNucleus> getNucleiAsSperm(){
  	ArrayList<RodentSpermNucleus> result = new ArrayList<RodentSpermNucleus>(0);
  	ArrayList<Nucleus> simple = this.getNuclei();
  	for( Nucleus n : simple){
  		result.add( (RodentSpermNucleus)n);
  	}
    return result;
  }

  public RodentSpermNucleus getNucleus(int i){
    return (RodentSpermNucleus)this.getNuclei().get(i);
  }

  public int[] getTailIndexes(){
    int[] d = new int[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){
      d[i] = this.getNucleus(i).getTailIndex();
    }
    return d;
  }

  public double[] getNormalisedTailIndexes(){
    double[] d = new double[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){
      d[i] = ( (double) this.getNucleus(i).getTailIndex() / (double) this.getNucleus(i).getLength() ) * 100;
    }
    return d;
  }

  public void createNormalisedTailPositions(){
    for(int i=0;i<this.getNucleusCount();i++){
      this.getNucleus(i).createNormalisedYPositionsFromTail();
    }
  }

  public double[] getDifferencesToMedian(){
    double[] d = new double[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){
      d[i] = this.getNucleus(i).getDifferenceToMedianCurve();
    }
    return d;
  }

  public RodentSpermNucleus getNucleusMostSimilarToMedian(){
  	RodentSpermNucleus n = (RodentSpermNucleus) this.getNuclei().get(0); // default to the first nucleus
  	double difference = 7000;
  	for(int i=0;i<this.getNucleusCount();i++){
      if(this.getNucleus(i).getDifferenceToMedianCurve()<difference){
      	difference = this.getNucleus(i).getDifferenceToMedianCurve();
      	n = this.getNucleus(i);
      }
    }
    return n;
  }

  public double getMaxRawXFromTails(){
    double d = 0;
    for(int i=0;i<this.getNucleusCount();i++){
      if(this.getNucleus(i).getMaxRawXFromTail() > d){
        d = this.getNucleus(i).getMaxRawXFromTail();
      }
    }
    return d;
  }

  public double getMinRawXFromTails(){
    double d = 0;
    for(int i=0;i<this.getNucleusCount();i++){
      if(this.getNucleus(i).getMinRawXFromTail() < d){
        d = this.getNucleus(i).getMinRawXFromTail();
      }
    }
    return d;
  }

  public double getMaxRawXFromTips(){
    double d = 0;
    for(int i=0;i<this.getNucleusCount();i++){
      if(this.getNucleus(i).getMaxRawXFromTip() > d){
        d = this.getNucleus(i).getMaxRawXFromTip();
      }
    }
    return d;
  }

  public double getMinRawXFromTips(){
    double d = 0;
    for(int i=0;i<this.getNucleusCount();i++){
      if(this.getNucleus(i).getMaxRawXFromTip() < d){
        d = this.getNucleus(i).getMaxRawXFromTip();
      }
    }
    return d;
  }

  /*
		Interpolate the median profile to match the length of the most-median nucleus
		Store the angle profile as a double[] to feed into the curve refolder
  */
	public double[] getMedianTargetCurve(Nucleus n){
		double[] targetMedianCurve = interpolateMedianToLength(n.getLength());
		return targetMedianCurve;
	}	

  /*
    -----------------------
    Setters
    -----------------------
  */

  public void setNormalisedMedianLineFromTip(double[] d){
		this.normalisedMedianLineFromTip = d;
	}

  public void setNormalisedMedianLineFromTail(double[] d){
    this.normalisedMedianLineFromTail = d;
  }

  /*
    -----------------------
    Filter nuclei before processing
    -----------------------
  */

  /*
    The filters needed to separate out the objects from nuclei
    Filter on: nuclear area, perimeter and array length to find
    conjoined nuclei and blobs too small to be nuclei
    Use path length to remove poorly thresholded nuclei
  */
  public void refilterNuclei(RodentSpermNucleusCollection failedCollection){

  	IJ.log("  Filtering nuclei...");
    double medianArea = this.getMedianNuclearArea();
    double medianPerimeter = this.getMedianNuclearPerimeter();
    double medianPathLength = this.getMedianPathLength();
    double medianArrayLength = this.getMedianArrayLength();
    double medianFeretLength = this.getMedianFeretLength();

    int beforeSize = this.getNucleusCount();

    double maxPathLength = medianPathLength * this.maxWibblinessFromMedian;
    double minArea = medianArea / this.maxDifferenceFromMedian;
    double maxArea = medianArea * this.maxDifferenceFromMedian;
    double maxPerim = medianPerimeter * this.maxDifferenceFromMedian;
    double minPerim = medianPerimeter / this.maxDifferenceFromMedian;
    double minFeret = medianFeretLength / this.maxDifferenceFromMedian;

    int area = 0;
    int perim = 0;
    int pathlength = 0;
    int arraylength = 0;
    int curveShape = 0;
    int feretlength = 0;

    IJ.append("Prefiltered:", this.getDebugFile().getAbsolutePath());
    this.exportFilterStats();

    for(int i=0;i<this.getNucleusCount();i++){
      RodentSpermNucleus n = (RodentSpermNucleus) this.getNucleus(i);
      boolean dropNucleus = false;

      if(n.getArea() > maxArea || n.getArea() < minArea ){
        n.updateFailureCode(FAILURE_AREA);
        area++;
      }
      if(n.getPerimeter() > maxPerim || n.getPerimeter() < minPerim ){
        n.updateFailureCode(FAILURE_PERIM);
        perim++;
      }
      if(n.getPathLength() > maxPathLength){ // only filter for values too big here - wibbliness detector
        n.updateFailureCode(FAILURE_THRESHOLD);
        pathlength++;
      }
      if(n.getLength() > medianArrayLength * maxDifferenceFromMedian || n.getLength() < medianArrayLength / maxDifferenceFromMedian ){
        n.updateFailureCode(FAILURE_ARRAY);
         arraylength++;
      }

      if(n.getFeret() < minFeret){
        n.updateFailureCode(FAILURE_FERET);
        feretlength++;
      }
      
      if(n.getFailureCode() > 0){
        failedCollection.addNucleus(n);
        this.getNuclei().remove(this.getNucleus(i));
        i--; // the array index automatically shifts to account for the removed nucleus. Compensate to avoid skipping nuclei
      }
    }

    medianArea = this.getMedianNuclearArea();
    medianPerimeter = this.getMedianNuclearPerimeter();
    medianPathLength = this.getMedianPathLength();
    medianArrayLength = this.getMedianArrayLength();
    medianFeretLength = this.getMedianFeretLength();
    // medianDifferenceToMedianCurve = quartile(this.getDifferencesToMedian(),50);

    int afterSize = this.getNucleusCount();
    int removed = beforeSize - afterSize;

    IJ.append("Postfiltered:", this.getDebugFile().getAbsolutePath());
    this.exportFilterStats();
    IJ.log("  Removed due to size or length issues: "+removed+" nuclei");
    IJ.append("  Due to area outside bounds "+(int)minArea+"-"+(int)maxArea+": "+area+" nuclei", this.getDebugFile().getAbsolutePath());
    IJ.append("  Due to perimeter outside bounds "+(int)minPerim+"-"+(int)maxPerim+": "+perim+" nuclei", this.getDebugFile().getAbsolutePath());
    IJ.append("  Due to wibbliness >"+(int)maxPathLength+" : "+(int)pathlength+" nuclei", this.getDebugFile().getAbsolutePath());
    IJ.append("  Due to array length: "+arraylength+" nuclei", this.getDebugFile().getAbsolutePath());
    IJ.append("  Due to feret length: "+feretlength+" nuclei", this.getDebugFile().getAbsolutePath());
    // IJ.append("  Due to curve shape: "+curveShape+" nuclei", this.getDebugFile().getAbsolutePath());
    IJ.log("  Remaining: "+this.getNucleusCount()+" nuclei");
  }

  /*
    -----------------------
    Create and manipulate
    aggregate profiles. These
    can be centred on tip  or tail
    -----------------------
  */

  public void recalculateTailPositions(){

  	this.createProfileAggregateFromTip();
  	this.drawProfilePlots();
  	this.drawNormalisedMedianLineFromTip();
    this.findTailIndexInMedianCurve();
    this.calculateOffsets();
    this.createNormalisedTailPositions();
    this.createProfileAggregateFromTail();

    this.drawRawPositionsFromTailChart();
    this.drawNormalisedPositionsFromTailChart();

    this.drawNormalisedMedianLineFromTail();
  }

  public void measureAndExportNuclei(){
    this.exportNuclearStats("logStats");
    this.annotateImagesOfNuclei();
    this.exportAnnotatedNuclei();
    this.exportCompositeImage("composite");
  }

  /*
    We need to calculate the median angle profile. This requires binning the normalised profiles
    into bins of size PROFILE_INCREMENT to generate a table such as this:
          k   0.0   0.5   1.0   1.5   2.0 ... 99.5   <- normalised profile bins
    NUCLEUS1  180   185  170    130   120 ... 50     <- angle within those bins
    NUCLEUS2  180   185  170    130   120 ... 50

    The median of each bin can then be calculated. 
    Depending on the length of the profile arrays and the chosen increment, there may
    be >1 or <1 angle within each bin for any given nucleus. We rely on large numbers of 
    nuclei to average this problem away; further methods interpolate values from surrounding
    bins to plug any holes left over

    The data are stored as a Map<Double, Collection<Double>>
  */

  private void updateProfileAggregate(double[] xvalues, double[] yvalues, Map<Double, Collection<Double>> profileAggregate){

    for(double k=0.0;k<100;k+=PROFILE_INCREMENT){ // cover all the bin positions across the profile

      for(int j=0;j<xvalues.length;j++){
       
        if( xvalues[j] > k && xvalues[j] < k+PROFILE_INCREMENT){

          Collection<Double> values = profileAggregate.get(k);
          
          if (values==null) { // this this profile increment has not yet been encountered, create it
              values = new ArrayList<Double>();
              profileAggregate.put(k, values);
          }
          values.add(yvalues[j]);
        }
      }
    }        
  }

  public void createProfileAggregateFromTip(){

  	for(int i=0;i<this.getNucleusCount();i++){

	  	double[] xvalues = this.getNucleus(i).getNormalisedXPositionsFromTip();
	  	double[] yvalues = this.getNucleus(i).getAngleProfile().getAngleArray();
	  	updateProfileAggregate(xvalues, yvalues, this.normalisedProfilesFromTip); 
	  }
  }

  public void createProfileAggregateFromTail(){

  	for(int i=0;i<this.getNucleusCount();i++){

	  	double[] xvalues = this.getNucleus(i).getNormalisedXPositionsFromTip();
	  	double[] yvalues = this.getNucleus(i).getNormalisedYPositionsFromTail();
	  	updateProfileAggregate(xvalues, yvalues, this.normalisedProfilesFromTail); 
	  }
  }

  /*
    Calculate median angles at each bin
  */

  public ArrayList<Double[]> calculateMediansAndQuartilesOfProfile(Map<Double, Collection<Double>> profile){

    ArrayList<Double[]>  medianResults = new ArrayList<Double[]>(0);
    int arraySize = (int)Math.round(100/PROFILE_INCREMENT);
    Double[] xmedians = new Double[arraySize];
    Double[] ymedians = new Double[arraySize];
    Double[] lowQuartiles = new Double[arraySize];
    Double[] uppQuartiles = new Double[arraySize];
    Double[] tenQuartiles = new Double[arraySize];
    Double[] ninetyQuartiles = new Double[arraySize];
    Double[] numberOfPoints = new Double[arraySize];

    int m = 0;
    for(double k=0.0;k<100;k+=PROFILE_INCREMENT){

      try{
          Collection<Double> values = profile.get(k);

          if(values.size()> 0){
            Double[] d = values.toArray(new Double[0]);
            int n = d.length;

            Arrays.sort(d);
            double median = quartile(d, 50.0);
            double q1     = quartile(d, 25.0);
            double q3     = quartile(d, 75.0);
            double q10    = quartile(d, 10.0);
            double q90    = quartile(d, 90.0);
           
            xmedians[m] = k;
            ymedians[m] = median;
            lowQuartiles[m] = q1;
            uppQuartiles[m] = q3;
            tenQuartiles[m] = q10;
            ninetyQuartiles[m] = q90;
            numberOfPoints[m] = (double)n;
          }
        } catch(Exception e){
             IJ.log("Cannot calculate median for "+k);
             IJ.append("Cannot calculate median for "+k+": "+e, this.getDebugFile().getAbsolutePath());
             xmedians[m] = k;
             ymedians[m] = 0.0;
             lowQuartiles[m] = 0.0;
             uppQuartiles[m] = 0.0;
             tenQuartiles[m] = 0.0;
             ninetyQuartiles[m] = 0.0;
        } finally {
          m++;
      }
    }

    // repair medians with no points by interpolation
    for(int i=0;i<xmedians.length;i++){
      if(ymedians[i] == 0 && lowQuartiles[i] == 0 && uppQuartiles[i] == 0){

        int replacementIndex = 0;

        if(xmedians[i]<1)
          replacementIndex = i+1;
        if(xmedians[i]>99)
          replacementIndex = i-1;

        ymedians[i]        = ymedians[replacementIndex]    ;
        lowQuartiles[i]    = lowQuartiles[replacementIndex];
        uppQuartiles[i]    = uppQuartiles[replacementIndex];
        tenQuartiles[i]    = tenQuartiles[replacementIndex];
        ninetyQuartiles[i] = ninetyQuartiles[replacementIndex];

        IJ.log("Repaired medians at "+i+" with values from  "+replacementIndex);
        IJ.append("Repaired medians at "+i+" with values from  "+replacementIndex, this.getDebugFile().getAbsolutePath());
      }
    }

    medianResults.add(xmedians);
    medianResults.add(ymedians);
    medianResults.add(lowQuartiles);
    medianResults.add(uppQuartiles);
    medianResults.add(tenQuartiles);
    medianResults.add(ninetyQuartiles);
    medianResults.add(numberOfPoints);
    return medianResults;
  }


  /*
    Calculate the offsets needed to corectly assign the tail positions
    compared to ideal median curves
  */
	public void calculateOffsets(){

		for(int i= 0; i<this.getNucleusCount();i++){ // for each roi
			RodentSpermNucleus r = this.getNucleus(i);
      int curveTailIndex = r.getTailIndex();

      // the curve needs to be matched to the median 
      // hence the median array needs to be the same curve length
      double[] medianInterpolatedArray = interpolateMedianToLength(r.getLength());

      // alter the median tail index to the interpolated curve equivalent
      int medianTailIndex = (int)Math.round(( (double)this.medianLineTailIndex / (double)this.normalisedMedianLineFromTip.length )* r.getLength());
			
      if(medianInterpolatedArray.length != r.getLength()){
        IJ.log("    Error: interpolated median array is not the right length");
      }

      int offset = curveTailIndex - medianTailIndex;

      // for comparisons between sperm, get the difference between the offset curve and the median
			double totalDifference = 0;

			for(int j=0; j<r.getLength(); j++){ // for each point round the array

	      // IJ.log("j="+j);
	      // find the next point in the array, given the tail point is our 0
	      int curveIndex = wrapIndex(curveTailIndex+j-offset, r.getLength());
	      // IJ.log("Curve index: "+curveIndex);

	      // get the angle at this point
	      double curveAngle = r.getBorderPoint(curveIndex).getInteriorAngle();

	      // get the next median index position, given the tail point is 0
	      int medianIndex = wrapIndex(medianTailIndex+j, medianInterpolatedArray.length); // DOUBLE CHECK THE LOGIC HERE - CAUSING NPE WHEN USING  this.normalisedMedianLineFromTip.length
	      // IJ.log("Median index: "+medianIndex);
	      double medianAngle = medianInterpolatedArray[medianIndex];
	      // IJ.log("j="+j+" Curve index: "+curveIndex+" Median index: "+medianIndex+" Median: "+medianAngle);
	      // double difference = 
	      totalDifference += Math.abs(curveAngle - medianAngle);
			}

			this.getNucleus(i).setOffsetForTail(offset);

      // r.offsetCalculated = true;
      r.setTailIndex(r.getTailIndex()-offset); // update the tail position
      r.setSpermTail(r.getBorderPoint(r.getTailIndex())); // ensure the spermTail is updated
      r.setDifferenceToMedianCurve(totalDifference);
		}

		this.differencesCalculated = true;
	}

  public double[] interpolateMedianToLength(int newLength){

    int oldLength = normalisedMedianLineFromTip.length;
    
    double[] newMedianCurve = new double[newLength];
    // where in the old curve index is the new curve index?
    for (int i=0; i<newLength; i++) {
      // we have a point in the new curve.
      // we want to know which points it lay between in the old curve
      double oldIndex = ( (double)i / (double)newLength)*oldLength; // get the frational index position needed
      double interpolatedMedian = interpolateNormalisedMedian(oldIndex, oldLength);
      newMedianCurve[i] = interpolatedMedian;
    }
    return newMedianCurve;
  }

	/*
		Take an index position from a non-normalised profile
		Normalise it
		Find the corresponding angle in the median curve
		Interpolate as needed
	*/
	public double interpolateNormalisedMedian(double normIndex, int length){

		// convert index to 1 window boundaries
		int medianIndex1 = (int)Math.round(normIndex);
		int medianIndex2 = medianIndex1 > normIndex
												? medianIndex1 - 1
												: medianIndex1 + 1;

		int medianIndexLower = medianIndex1 < medianIndex2
											 	? medianIndex1
											 	: medianIndex2;

		int medianIndexHigher = medianIndex2 < medianIndex1
														 ? medianIndex2
														 : medianIndex1;

		// wrap the arrays
    medianIndexLower  = wrapIndex(medianIndexLower , length);
    medianIndexHigher = wrapIndex(medianIndexHigher, length);

		// get the angle values in the median profile at the given indices
		double medianAngleLower  = this.normalisedMedianLineFromTip[medianIndexLower ];
		double medianAngleHigher = this.normalisedMedianLineFromTip[medianIndexHigher];

		// interpolate on a stright line between the points
		double medianAngleDifference = medianAngleHigher - medianAngleLower;
		double positionToFind = medianIndexHigher - normIndex;
		double interpolatedMedianAngle = (medianAngleDifference * positionToFind) + medianAngleLower;
		return interpolatedMedianAngle;
	}

	public void findTailIndexInMedianCurve(){
		// can't use regular tail detector, because it's based on NucleusBorderPoints
		// get minima in curve, then find the lowest minima / minima furthest from both ends

		ArrayList<Integer> minima = this.detectLocalMinimaInMedian();

		double minDiff = this.normalisedMedianLineFromTip.length;
		double minAngle = 180;
		int tailIndex = 0;

    if(minima.size()==0){
      IJ.log("  Error: no minima found in median line");
      tailIndex = 100; // set to roughly the middle of the array for the moment

    } else{

  		for(int i = 0; i<minima.size();i++){
  			Integer index = (Integer)minima.get(i);

  			int toEnd = this.normalisedMedianLineFromTip.length - index;
  			int diff = Math.abs(index - toEnd);

  			double angle = this.normalisedMedianLineFromTip[index];
  			if(angle<minAngle && index > 40 && index < 120){ // get the lowest point that is not near the tip
  				minAngle = angle;
  				tailIndex = index;
  			}
  		}
  	}
  	this.medianLineTailIndex = tailIndex;
	}

	/*
		Finds, as a list of index integers, the points
		of local minimum in the median profile line
	*/
	private ArrayList<Integer> detectLocalMinimaInMedian(){
    // go through angle array (with tip at start)
    // look at 1-2-3-4-5 points ahead and behind.
    // if all greater, local minimum
    int lookupDistance = 5;
    
    double[] prevAngles = new double[lookupDistance]; // slots for previous angles
    double[] nextAngles = new double[lookupDistance]; // slots for next angles

    // int count = 0;

    ArrayList<Integer> medianIndexMinima = new ArrayList<Integer>(0);

    for (int i=0; i<this.normalisedMedianLineFromTip.length; i++) { // for each position in sperm

      // go through each lookup position and get the appropriate angles
      for(int j=0;j<prevAngles.length;j++){

        int prev_i = wrapIndex( i-(j+1), this.normalisedMedianLineFromTip.length ); // the index j+1 before i
        int next_i = wrapIndex( i+(j+1), this.normalisedMedianLineFromTip.length ); // the index j+1 after i

        // fill the lookup array
        prevAngles[j] = this.normalisedMedianLineFromTip[prev_i];
        nextAngles[j] = this.normalisedMedianLineFromTip[next_i];
      }
      
      // with the lookup positions, see if minimum at i
      // return a 1 if all higher than last, 0 if not
      // prev_l = 0;
      int errors = 2; // allow two positions to be out of place; better handling of noisy data
      boolean ok = true;
      for(int l=0;l<prevAngles.length;l++){

        // for the first position in prevAngles, compare to the current index
        if(l==0){
          if(prevAngles[l] < this.normalisedMedianLineFromTip[i] || nextAngles[l] < this.normalisedMedianLineFromTip[i]){
            errors--;
          }
        } else { // for the remainder of the positions in prevAngles, compare to the prior prevAngle
          
          if(prevAngles[l] < prevAngles[l-1] || nextAngles[l] < nextAngles[l-1]){
            errors--;
          }
        }
        if(errors<0){
          ok = false;
        }
      }

      if(ok){
        medianIndexMinima.add(i);
      }
    }
    return medianIndexMinima;
  }

 
  public void measureNuclearOrganisation(){

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi

       this.getNucleus(i).splitNucleusToHeadAndHump();
       this.getNucleus(i).calculateSignalAnglesFromTail();
    }
    this.exportSignalStats();
    this.addSignalsToProfileChartFromTip();
    this.addSignalsToProfileChartFromTail();

    IJ.log("Red signals: "  + this.getRedSignalCount());
    IJ.log("Green signals: "+ this.getGreenSignalCount());
  }

  public void exportCompositeImage(String filename){

    // foreach nucleus
    // createProcessor (500, 500)
    // sertBackgroundValue(0)
    // paste in old image at centre
    // insert(ImageProcessor ip, int xloc, int yloc)
    // rotate about CoM (new position)
    // display.
    IJ.log("Creating composite image...");
    

    int totalWidth = 0;
    int totalHeight = 0;

    int boxWidth  = (int)(this.getMedianNuclearPerimeter()/1.4);
    int boxHeight = (int)(this.getMedianNuclearPerimeter()/1.2);

    int maxBoxWidth = boxWidth * 5;
    int maxBoxHeight = (boxHeight * (int)(Math.ceil(this.getNucleusCount()/5)) + boxHeight );

    ImagePlus finalImage = new ImagePlus("Final image", new BufferedImage(maxBoxWidth, maxBoxHeight, BufferedImage.TYPE_INT_RGB));
    ImageProcessor finalProcessor = finalImage.getProcessor();
    finalProcessor.setBackgroundValue(0);

    for(int i=0; i<this.getNucleusCount();i++){
      
      RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);
      String path = n.getAnnotatedImagePath();

      try {
        Opener localOpener = new Opener();
        ImagePlus image = localOpener.openImage(path);
        ImageProcessor ip = image.getProcessor();
        int width  = ip.getWidth();
        int height = ip.getHeight();
        ip.setRoi(n.getRoi());


        ImageProcessor newProcessor = ip.createProcessor(boxWidth, boxHeight);

        newProcessor.setBackgroundValue(0);
        newProcessor.insert(ip, (int)boxWidth/4, (int)boxWidth/4); // put the original halfway in
        newProcessor.setInterpolationMethod(ImageProcessor.BICUBIC);
        newProcessor.rotate( n.findRotationAngle() );
        newProcessor.setBackgroundValue(0);

        if(totalWidth>maxBoxWidth-boxWidth){
        	totalWidth=0;
        	totalHeight+=(int)(boxHeight);
        }
        int newX = totalWidth;
        int newY = totalHeight;
        totalWidth+=(int)(boxWidth);
        
        finalProcessor.insert(newProcessor, newX, newY);
        TextRoi label = new TextRoi(newX, newY, n.getImageName()+"-"+n.getNucleusNumber());
        Overlay overlay = new Overlay(label);
        finalProcessor.drawOverlay(overlay);  
      } catch(Exception e){
        IJ.log("Error adding image to composite");
        IJ.append("Error adding image to composite: "+e, this.getDebugFile().getAbsolutePath());
        IJ.append("  "+getType(), this.getDebugFile().getAbsolutePath());
        IJ.append("  "+path, this.getDebugFile().getAbsolutePath());
      }     
    }
  	// finalImage.show();
  	IJ.saveAsTiff(finalImage, this.getFolder()+File.separator+filename+"."+getType()+".tiff");
  	IJ.log("Composite image created");
  }

 

	/*
    -----------------------
    Export data
    -----------------------
  */
  public void exportMediansAndQuartilesOfProfile(ArrayList<Double[]> profile, String filename){

  	String logFile = this.getFolder()+File.separator+filename+"."+this.getType()+".txt";
    File f = new File(logFile);
    if(f.exists()){
      f.delete();
    }

    String outLine = "# X_POSITION\tANGLE_MEDIAN\tQ25\tQ75\tQ10\tQ90\tNUMBER_OF_POINTS\n";
    

    for(int i =0;i<profile.get(0).length;i++){
			outLine += 	profile.get(0)[i]+"\t"+
				          profile.get(1)[i]+"\t"+
				          profile.get(2)[i]+"\t"+
				          profile.get(3)[i]+"\t"+
				          profile.get(4)[i]+"\t"+
				          profile.get(5)[i]+"\t"+
				          profile.get(6)[i]+"\n";
  	}
  	IJ.append(outLine, logFile); 
  }

  public void exportInterpolatedMedians(double[] d){

    String logFile = this.getFolder()+File.separator+"logInterpolatedMedians.txt";
    File f = new File(logFile);
    if(f.exists()){
      f.delete();
    }

    IJ.append("INDEX\tANGLE", logFile);
    for(int i=0;i<d.length;i++){
      IJ.append(i+"\t"+d[i], logFile);
    }
    IJ.append("", logFile);

  }

  public void exportOffsets(double[] d){

  	String logFile = this.getFolder()+File.separator+"logOffsets.txt";
    File f = new File(logFile);
    if(f.exists()){
      f.delete();
    }

    IJ.append("OFFSET\tDIFFERENCE", logFile);

    for(int i=0;i<d.length;i++){
      IJ.append(i+"\t"+d[i], logFile);
    }
    IJ.append("", logFile);
  }

  private void exportFilterStats(){

  	double medianArea = this.getMedianNuclearArea();
    double medianPerimeter = this.getMedianNuclearPerimeter();
    double medianPathLength = this.getMedianPathLength();
    double medianArrayLength = this.getMedianArrayLength();
    double medianFeretLength = this.getMedianFeretLength();
    // double medianDifferenceToMedianCurve = quartile(this.getDifferencesToMedian(),50);

    IJ.append("    Area: "        +(int)medianArea,                    this.getDebugFile().getAbsolutePath());
    IJ.append("    Perimeter: "   +(int)medianPerimeter,               this.getDebugFile().getAbsolutePath());
    IJ.append("    Path length: " +(int)medianPathLength,              this.getDebugFile().getAbsolutePath());
    IJ.append("    Array length: "+(int)medianArrayLength,             this.getDebugFile().getAbsolutePath());
    IJ.append("    Feret length: "+(int)medianFeretLength,             this.getDebugFile().getAbsolutePath());
    // IJ.append("    Curve: "       +(int)medianDifferenceToMedianCurve, this.getDebugFile().getAbsolutePath());
  }

  public void exportSignalStats(){

    String redLogFile = this.getFolder()+File.separator+"logRedSignals."+getType()+".txt";
    File r = new File(redLogFile);
    if(r.exists()){
      r.delete();
    }

    String greenLogFile = this.getFolder()+File.separator+"logGreenSignals."+getType()+".txt";
    File g = new File(greenLogFile);
    if(g.exists()){
      g.delete();
    }

    IJ.append("# NUCLEUS_NUMBER\tSIGNAL_AREA\tSIGNAL_ANGLE\tSIGNAL_FERET\tSIGNAL_DISTANCE\tFRACTIONAL_DISTANCE\tSIGNAL_PERIMETER\tSIGNAL_RADIUS\tPATH", redLogFile);
    IJ.append("# NUCLEUS_NUMBER\tSIGNAL_AREA\tSIGNAL_ANGLE\tSIGNAL_FERET\tSIGNAL_DISTANCE\tFRACTIONAL_DISTANCE\tSIGNAL_PERIMETER\tSIGNAL_RADIUS\tPATH", greenLogFile);
    
    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi

      Nucleus n = this.getNucleus(i);

      int nucleusNumber = n.getNucleusNumber();
      String path = n.getPath();

      ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
      signals.add(n.getRedSignals());
      signals.add(n.getGreenSignals());

      int signalCount = 0;
      for( ArrayList<NuclearSignal> signalGroup : signals ){

        String log = signalCount == Nucleus.RED_CHANNEL ? redLogFile : greenLogFile;
        
        if(signalGroup.size()>0){
          for(int j=0; j<signalGroup.size();j++){
             NuclearSignal s = signalGroup.get(j);
             IJ.append(nucleusNumber                   +"\t"+
                       s.getArea()                     +"\t"+
                       s.getAngle()                    +"\t"+
                       s.getFeret()                    +"\t"+
                       s.getDistanceFromCoM()          +"\t"+
                       s.getFractionalDistanceFromCoM()+"\t"+
                       s.getPerimeter()                +"\t"+
                       path, log);
          } // end for
        } // end if
        signalCount++;
      } // end for
    } // end for
  }

  public void exportNuclearStats(String filename){
  
    String statsFile = this.getFolder()+File.separator+filename+"."+getType()+".txt";
    File f = new File(statsFile);
    if(f.exists()){
      f.delete();
    }

    String outLine = "# AREA\tPERIMETER\tFERET\tPATH_LENGTH\tNORM_TAIL_INDEX\tDIFFERENCE\tFAILURE_CODE\tPATH\n";

    IJ.log("Exporting stats for "+this.getNucleusCount()+" nuclei ("+this.getType()+")");
    double[] areas        = this.getAreas();
    double[] perims       = this.getPerimeters();
    double[] ferets       = this.getFerets();
    double[] pathLengths  = this.getPathLengths();
    int[] tails           = this.getTailIndexes();
    double[] differences  = this.getDifferencesToMedian();
    String[] paths        = this.getNucleusPaths();


    for(int i=0; i<this.getNucleusCount();i++){
    	int j = i+1;

      outLine = outLine + areas[i]+"\t"+
                          perims[i]+"\t"+
                          ferets[i]+"\t"+
                          pathLengths[i]+"\t"+
                          tails[i]+"\t"+
                          differences[i]+"\t"+
                          this.getNucleus(i).getFailureCode()+"\t"+
                          paths[i]+"\n";

      // Include tip, CoM, tail
  		// this.getNucleus(i).printLogFile(this.getNucleus(i).getNucleusFolder()+File.separator+this.getNucleus(i).getNucleusNumber()+".log");
    }
    IJ.append(  outLine, statsFile);
    IJ.log("Export complete");
  }

  /*
    Draw the features of interest on the images of the nuclei created earlier
  */
  public void annotateImagesOfNuclei(){
  	IJ.log("Annotating images ("+this.getType()+")...");
  	for(int i=0; i<this.getNucleusCount();i++){
      int m = i+1;
  		RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);
  		n.annotateSpermFeatures();
  	}
  	 IJ.log("Annotation complete");
  }

  /*
    -----------------------
    Draw plots
    -----------------------
  */

  /*
    Create the plots that we will be using
    Get the x max and min as needed from aggregate stats
  */
  private void preparePlots(){

    this.rawXFromTipPlot = new Plot( "Raw tip-centred plot",
                                "Position",
                                "Angle", Plot.Y_GRID | Plot.X_GRID);
    rawXFromTipPlot.setLimits(0,this.getMaxRawXFromTips(),-50,360);
    rawXFromTipPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
    rawXFromTipPlot.setYTicks(true);
    rawXFromTipPlot.setColor(Color.BLACK);
    rawXFromTipPlot.drawLine(0, 180, this.getMaxRawXFromTips(), 180); 
    rawXFromTipPlot.setColor(Color.LIGHT_GRAY);


    normXFromTipPlot = new Plot("Normalised tip-centred plot",
                                "Position",
                                "Angle", Plot.Y_GRID | Plot.X_GRID);
    normXFromTipPlot.setLimits(0,100,-50,360);
    normXFromTipPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
    normXFromTipPlot.setYTicks(true);
    normXFromTipPlot.setColor(Color.BLACK);
    normXFromTipPlot.drawLine(0, 180, 100, 180); 
    normXFromTipPlot.setColor(Color.LIGHT_GRAY);


    this.rawXFromTailPlot = new Plot( "Raw tail-centred plot",
                                "Position",
                                "Angle", Plot.Y_GRID | Plot.X_GRID);
    rawXFromTailPlot.setLimits( this.getMinRawXFromTails(),
                                this.getMaxRawXFromTails(),
                                -50,360);
    rawXFromTailPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
    rawXFromTailPlot.setYTicks(true);
    rawXFromTailPlot.setColor(Color.BLACK);
    rawXFromTailPlot.drawLine(this.getMinRawXFromTails(), 180, this.getMaxRawXFromTails(), 180); 
    rawXFromTailPlot.setColor(Color.LIGHT_GRAY);

    this.normXFromTailPlot = new Plot("Normalised tail-centred plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);
    normXFromTailPlot.setLimits(0,100,-50,360);
    normXFromTailPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
    normXFromTailPlot.setYTicks(true);
    normXFromTailPlot.setColor(Color.BLACK);
    normXFromTailPlot.drawLine(0, 180, 100, 180); 
    normXFromTailPlot.setColor(Color.LIGHT_GRAY);
  }

  /*
    Create the charts of the profiles of the nuclei within this collecion.
    Currently drawing: 
      Tip-aligned raw X
      Tail-aligned raw X
      Tip-aligned normalised X
  */
  public void drawProfilePlots(){

    preparePlots();

    for(int i=0;i<this.getNucleusCount();i++){
      
      double[] rawXpoints         = this.getNucleus(i).getRawXPositionsFromTip();
      double[] yPoints            = this.getNucleus(i).getAngleProfile().getAngleArray();
      double[] normalisedXFromTip = this.getNucleus(i).getNormalisedXPositionsFromTip();
      double[] rawXFromTail       = this.getNucleus(i).getRawXPositionsFromTail();

      this.rawXFromTipPlot.setColor(Color.LIGHT_GRAY);
      this.rawXFromTipPlot.addPoints(rawXpoints, yPoints, Plot.LINE);

      this.normXFromTipPlot.setColor(Color.LIGHT_GRAY);
      this.normXFromTipPlot.addPoints(normalisedXFromTip, yPoints, Plot.LINE);

      this.rawXFromTailPlot.setColor(Color.LIGHT_GRAY);
      this.rawXFromTailPlot.addPoints(rawXFromTail, yPoints, Plot.LINE);
      
    }

    // this.rawXFromTipPlot.draw();
    
    rawXFromTipWindow.noGridLines = true; 
    // rawXFromTipWindow = rawXFromTipPlot.show();
    
    normXFromTipWindow.noGridLines = true; 
    normXFromTipWindow = normXFromTipPlot.show();
    
    rawXFromTailWindow.noGridLines = true; 
    // rawXFromTailWindow = rawXFromTailPlot.show();

    exportProfilePlot(rawXFromTipPlot, "plotTipRaw");
    exportProfilePlot(rawXFromTailPlot, "plotTailRaw");
  }

  /*
		Draw the median line on the normalised profile
		chart, aligned to the sperm tip
  */
  public void drawNormalisedMedianLineFromTip(){
    // output the final results: calculate median positions

    ArrayList<Double[]> medians = calculateMediansAndQuartilesOfProfile( this.normalisedProfilesFromTip );
    this.exportMediansAndQuartilesOfProfile(medians, this.logMedianFromTipFile);

    double[] xmedians        =  getDoubleFromDouble( medians.get(0) );
    double[] ymedians        =  getDoubleFromDouble( medians.get(1) );
    double[] lowQuartiles    =  getDoubleFromDouble( medians.get(2) );
    double[] uppQuartiles    =  getDoubleFromDouble( medians.get(3) );
    double[] tenQuartiles    =  getDoubleFromDouble( medians.get(4) );
    double[] ninetyQuartiles =  getDoubleFromDouble( medians.get(5) );

    setNormalisedMedianLineFromTip(ymedians);

    // add the median lines to the chart
    normXFromTipPlot.setColor(Color.BLACK);
    normXFromTipPlot.setLineWidth(3);
    normXFromTipPlot.addPoints(xmedians, ymedians, Plot.LINE);
    normXFromTipPlot.setColor(Color.DARK_GRAY);
    normXFromTipPlot.setLineWidth(2);
    normXFromTipPlot.addPoints(xmedians, lowQuartiles, Plot.LINE);
    normXFromTipPlot.addPoints(xmedians, uppQuartiles, Plot.LINE);

    // handle the normalised tail position mapping
    double[] xTails = this.getNormalisedTailIndexes();

    double[] yTails = new double[xTails.length];
    Arrays.fill(yTails, CHART_TAIL_BOX_Y_MID); // all dots at y=300
    normXFromTipPlot.setColor(Color.LIGHT_GRAY);
    normXFromTipPlot.addPoints(xTails, yTails, Plot.DOT);

    // median tail positions
    double tailQ50 = quartile(xTails, 50);
    double tailQ25 = quartile(xTails, 25);
    double tailQ75 = quartile(xTails, 75);

    normXFromTipPlot.setColor(Color.DARK_GRAY);
    normXFromTipPlot.setLineWidth(1);
    normXFromTipPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MAX, tailQ75, CHART_TAIL_BOX_Y_MAX);
    normXFromTipPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MIN);
    normXFromTipPlot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ25, CHART_TAIL_BOX_Y_MAX);
    normXFromTipPlot.drawLine(tailQ75, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MAX);
    normXFromTipPlot.drawLine(tailQ50, CHART_TAIL_BOX_Y_MIN, tailQ50, CHART_TAIL_BOX_Y_MAX);
    normXFromTipWindow.drawPlot(normXFromTipPlot);
  }

  /*
		Draw the median line on the normalised profile
		chart, aligned to the sperm tail
  */
  public void drawNormalisedMedianLineFromTail(){

    ArrayList<Double[]> medians = calculateMediansAndQuartilesOfProfile( this.normalisedProfilesFromTail );
    this.exportMediansAndQuartilesOfProfile(medians, this.logMedianFromTailFile);

    double[] xmedians        =  getDoubleFromDouble( medians.get(0) );
    double[] ymedians        =  getDoubleFromDouble( medians.get(1) );
    double[] lowQuartiles    =  getDoubleFromDouble( medians.get(2) );
    double[] uppQuartiles    =  getDoubleFromDouble( medians.get(3) );
    double[] tenQuartiles    =  getDoubleFromDouble( medians.get(4) );
    double[] ninetyQuartiles =  getDoubleFromDouble( medians.get(5) );

    setNormalisedMedianLineFromTail(ymedians);

    // add the median lines to the chart
    normXFromTailPlot.setColor(Color.BLACK);
    normXFromTailPlot.setLineWidth(3);
    normXFromTailPlot.addPoints(xmedians, ymedians, Plot.LINE);
    normXFromTailPlot.setColor(Color.DARK_GRAY);
    normXFromTailPlot.setLineWidth(2);
    normXFromTailPlot.addPoints(xmedians, lowQuartiles, Plot.LINE);
    normXFromTailPlot.addPoints(xmedians, uppQuartiles, Plot.LINE);

    normXFromTailWindow.drawPlot(normXFromTailPlot);
  }

  private void addSignalsToProfileChartFromTip(){
    // PlotWindow normXFromTipWindow; normXFromTipPlot
    // for each signal in each nucleus, find index of point. Draw dot at index at y=-30 (for now)
    // Add the signals to the tip centred profile plot

    normXFromTipPlot.setColor(Color.LIGHT_GRAY);
    normXFromTipPlot.setLineWidth(1);
    normXFromTipPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MIN,100,CHART_SIGNAL_Y_LINE_MIN);
    normXFromTipPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MAX,100,CHART_SIGNAL_Y_LINE_MAX);

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi

      RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);

      ArrayList<NuclearSignal> redSignals = n.getRedSignals();
      if(redSignals.size()>0){

        ArrayList<Double> redPoints = new ArrayList<Double>(0);
        ArrayList<Double> yPoints   = new ArrayList<Double>(0);

        for(int j=0; j<redSignals.size();j++){

          NucleusBorderPoint border = redSignals.get(j).getClosestBorderPoint();
          for(int k=0; k<n.getLength();k++){

            if(n.getBorderPoint(k).overlaps(border)){
              redPoints.add( n.getNormalisedXPositionsFromTip()[k] );
              double yPosition = CHART_SIGNAL_Y_LINE_MIN + ( redSignals.get(j).getFractionalDistanceFromCoM() * ( CHART_SIGNAL_Y_LINE_MAX - CHART_SIGNAL_Y_LINE_MIN) ); // 
              yPoints.add(yPosition);
            }
          }
        }
        normXFromTipPlot.setColor(Color.RED);
        normXFromTipPlot.setLineWidth(2);
        normXFromTipPlot.addPoints(redPoints, yPoints, Plot.DOT);
      }
    }
    normXFromTipWindow.drawPlot(normXFromTipPlot);
    exportProfilePlot(normXFromTipPlot, "plotTipNorm");
  }

  public void addSignalsToProfileChartFromTail(){
  	// Add the signals to the tail centred profile plot
    normXFromTailPlot.setColor(Color.LIGHT_GRAY);
    normXFromTailPlot.setLineWidth(1);
    normXFromTailPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MIN,100,CHART_SIGNAL_Y_LINE_MIN);
    normXFromTailPlot.drawLine(0,CHART_SIGNAL_Y_LINE_MAX,100,CHART_SIGNAL_Y_LINE_MAX);

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi

      RodentSpermNucleus n = (RodentSpermNucleus)this.getNucleus(i);

      ArrayList<NuclearSignal> redSignals = n.getRedSignals();
      if(redSignals.size()>0){

        ArrayList<Double> redPoints = new ArrayList<Double>(0);
        ArrayList<Double> yPoints   = new ArrayList<Double>(0);

        for(int j=0; j<redSignals.size();j++){

          NucleusBorderPoint border = redSignals.get(j).getClosestBorderPoint();
          for(int k=0; k<n.getLength();k++){

            if(n.getBorderPoint(k).overlaps(border)){
              // IJ.log("Found closest border: "+i+" : "+j);
              redPoints.add( n.getNormalisedXPositionsFromTail()[k] );
              double yPosition = CHART_SIGNAL_Y_LINE_MIN + ( redSignals.get(j).getFractionalDistanceFromCoM() * ( CHART_SIGNAL_Y_LINE_MAX - CHART_SIGNAL_Y_LINE_MIN) ); // make between 220 and 260
              yPoints.add(yPosition);
            }
          }
        }
        normXFromTailPlot.setColor(Color.RED);
        normXFromTailPlot.setLineWidth(2);
        normXFromTailPlot.addPoints(redPoints, yPoints, Plot.DOT);
      }
    }
    normXFromTailWindow.drawPlot(normXFromTailPlot);
    exportProfilePlot(normXFromTailPlot, "plotTailNorm");
  }

  private void exportProfilePlot(Plot plot, String name){
  	ImagePlus image = plot.getImagePlus();
    IJ.saveAsTiff(image, this.getFolder()+File.separator+name+"."+this.getType()+".tiff");
  }

  public void drawRawPositionsFromTailChart(){

    Plot offsetRawPlot = new Plot("Raw corrected tail-centred plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);
    PlotWindow offsetRawPlotWindow;

    offsetRawPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
    offsetRawPlot.setYTicks(true);
    
    double minX = 0;
    double maxX = 0;
    for(int i=0;i<this.getNucleusCount();i++){
      double[] xRawCentredOnTail = this.getNucleus(i).createOffsetRawProfile();
      if(getMin(xRawCentredOnTail)<minX){
        minX = getMin(xRawCentredOnTail);
      }
      if(getMax(xRawCentredOnTail)>maxX){
        maxX = getMax(xRawCentredOnTail);
      }
    }
    offsetRawPlot.setLimits( (int) minX-1, (int) maxX+1,-50,360);
    offsetRawPlot.setColor(Color.BLACK);
    offsetRawPlot.drawLine((int) minX-1, 180, (int) maxX+1, 180); 
    offsetRawPlot.setColor(Color.LIGHT_GRAY);
   
    for(int i=0;i<this.getNucleusCount();i++){
      double[] xRawCentredOnTail = this.getNucleus(i).createOffsetRawProfile();
      double[] ypoints = this.getNucleus(i).getInteriorAngles();

      offsetRawPlot.setColor(Color.LIGHT_GRAY);
      offsetRawPlot.addPoints(xRawCentredOnTail, ypoints, Plot.LINE);
    }
    
    offsetRawPlot.draw();
    offsetRawPlotWindow = offsetRawPlot.show();
    offsetRawPlotWindow.noGridLines = true; // I have no idea why this makes the grid lines appear on work PC, when they appear by default at home
    offsetRawPlotWindow.drawPlot(offsetRawPlot);  
  }

  public void drawNormalisedPositionsFromTailChart(){
   
    for(int i=0;i<this.getNucleusCount();i++){
      double[] xpoints = this.getNucleus(i).getNormalisedXPositionsFromTip();
      double[] ypoints = this.getNucleus(i).getNormalisedYPositionsFromTail();
      normXFromTailPlot.addPoints(xpoints, ypoints, Plot.LINE);
    }
    normXFromTailPlot.draw();
    normXFromTailWindow = normXFromTailPlot.show();
    normXFromTailWindow.drawPlot(normXFromTailPlot);  
  }

  /*
    -----------------------
    Utility functions
    -----------------------
  */
  private double[] getDoubleFromDouble(Double[] d){
    double[] results = new double[d.length];
    for(int i=0;i<d.length;i++){
      results[i] = d[i];
    }
    return results;
  }

}