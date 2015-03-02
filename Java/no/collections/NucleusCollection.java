/* 
  -----------------------
  NUCLEUS COLLECTION CLASS
  -----------------------
  This class contains the nuclei that pass detection criteria
  Provides aggregate stats
  It enables offsets to be calculated based on the median normalised curves
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
import ij.measure.Calibration;
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

import no.analysis.Analysable;
import no.nuclei.*;
import no.nuclei.INuclearFunctions;
import no.components.*;
import no.utility.NuclearOrganisationUtility;



public class NucleusCollection {

	private File folder; // the source of the nuclei
  private String outputFolder;
  private File debugFile;
  private String collectionType; // for annotating image names

  public static final int CHART_WINDOW_HEIGHT     = 400;
  public static final int CHART_WINDOW_WIDTH      = 500;
  public static final int CHART_TAIL_BOX_Y_MIN    = 325;
  public static final int CHART_TAIL_BOX_Y_MID    = 340;
  public static final int CHART_TAIL_BOX_Y_MAX    = 355;
  public static final int CHART_SIGNAL_Y_LINE_MIN = 275;
  public static final int CHART_SIGNAL_Y_LINE_MAX = 315;

  public static final int FAILURE_THRESHOLD = 1;
  public static final int FAILURE_FERET     = 2;
  public static final int FAILURE_ARRAY     = 4;
  public static final int FAILURE_AREA      = 8;
  public static final int FAILURE_PERIM     = 16;
  public static final int FAILURE_OTHER     = 32;
  public static final int FAILURE_SIGNALS   = 64;

  public static final double PROFILE_INCREMENT = 0.5;

  private double maxDifferenceFromMedian = 1.5; // used to filter the nuclei, and remove those too small, large or irregular to be real
  private double maxWibblinessFromMedian = 1.2; // filter for the irregular borders more stringently

  private HashMap<String, HashMap<String, Plot>> plotCollection = new HashMap<String, HashMap<String, Plot>>();

  // this holds the mapping of tail indexes etc in the median profile arrays
  // the tail index point in the head normalised array would be head, <tail, int>
  private HashMap<String, HashMap<String, Integer>> medianProfileFeatureIndexes = new HashMap<String, HashMap<String, Integer>>();

	private ArrayList<INuclearFunctions> nucleiCollection = new ArrayList<INuclearFunctions>(0); // store all the nuclei analysed

  private HashMap<String, Double[]> normalisedMedianProfileFromPoint = new HashMap<String, Double[]>();// the type of point and the array

  // store the calculated median profiles centred on the given border point

  private HashMap<String, HashMap<Double, Collection<Double>>> profileCollection = new HashMap<String, HashMap<Double, Collection<Double>>>();

	public NucleusCollection(File folder, String outputFolder, String type){
		this.folder = folder;
    this.outputFolder = outputFolder;
    this.debugFile = new File(folder.getAbsolutePath()+File.separator+outputFolder+File.separator+"logDebug.txt");
    this.collectionType = type;
	}

  /*
    -----------------------
    Define adders for all
    types of nucleus eligable
    -----------------------
  */

	public void addNucleus(INuclearFunctions r){
		this.nucleiCollection.add(r);
	}

  public void exportStatsFiles(){
    this.exportNuclearStats("logStats");
    this.exportImagePaths("logImagePaths");
    this.exportAngleProfiles();
  }

  public void annotateAndExportNuclei(){
    this.exportAnnotatedNuclei();
    this.exportCompositeImage("composite");
  }

  /*
    -----------------------
    Getters for aggregate stats
    -----------------------
  */

  public File getFolder(){
    return this.folder;
  }

  public String getOutputFolder(){
    return this.outputFolder;
  }

  public File getDebugFile(){
    return this.debugFile;
  }

  public String getType(){
    return this.collectionType;
  }

  public double[] getPerimeters(){

    double[] d = new double[nucleiCollection.size()];

    for(int i=0;i<nucleiCollection.size();i++){
      d[i] = nucleiCollection.get(i).getPerimeter();
    }
    return d;
  }

  public double[] getAreas(){

    double[] d = new double[nucleiCollection.size()];

    for(int i=0;i<nucleiCollection.size();i++){
      d[i] = nucleiCollection.get(i).getArea();
    }
    return d;
  }

  public double[] getFerets(){

    double[] d = new double[nucleiCollection.size()];

    for(int i=0;i<nucleiCollection.size();i++){
      d[i] = nucleiCollection.get(i).getFeret();
    }
    return d;
  }

  public double[] getPathLengths(){

    double[] d = new double[nucleiCollection.size()];

    for(int i=0;i<nucleiCollection.size();i++){
      d[i] = nucleiCollection.get(i).getPathLength();
    }
    return d;
  }

  public double[] getArrayLengths(){

    double[] d = new double[nucleiCollection.size()];

    for(int i=0;i<nucleiCollection.size();i++){
      d[i] = nucleiCollection.get(i).getLength();
    }
    return d;
  }

  public double[] getMedianDistanceBetweenPoints(){

    double[] d = new double[nucleiCollection.size()];

    for(int i=0;i<nucleiCollection.size();i++){
      d[i] = nucleiCollection.get(i).getAngleProfile().getMedianDistanceBetweenPoints();
    }
    return d;
  }

  public String[] getNucleusPaths(){
    String[] s = new String[nucleiCollection.size()];

    for(int i=0;i<nucleiCollection.size();i++){
      s[i] = nucleiCollection.get(i).getPath()+"-"+nucleiCollection.get(i).getNucleusNumber();
    }
    return s;
  }

  public String[] getCleanNucleusPaths(){
    String[] s = new String[nucleiCollection.size()];

    for(int i=0;i<nucleiCollection.size();i++){
      INuclearFunctions n = nucleiCollection.get(i);
      s[i] = n.getPath();
    }
    return s;
  }

  public String[] getPositions(){
    String[] s = new String[nucleiCollection.size()];

    for(int i=0;i<nucleiCollection.size();i++){
      s[i] = nucleiCollection.get(i).getPosition();
    }
    return s;
  }

  public int getNucleusCount(){
    return this.nucleiCollection.size();
  }

  public ArrayList<INuclearFunctions> getNuclei(){
    return this.nucleiCollection;
  }

  public INuclearFunctions getNucleus(int i){
    return this.nucleiCollection.get(i);
  }

  public int getRedSignalCount(){
    int count = 0;
    for(int i=0;i<nucleiCollection.size();i++){
      count += nucleiCollection.get(i).getRedSignalCount();
    }
    return count;
  }

  public int getGreenSignalCount(){
    int count = 0;
    for(int i=0;i<nucleiCollection.size();i++){
      count += nucleiCollection.get(i).getGreenSignalCount();
    }
    return count;
  }

  // allow for refiltering of nuclei based on nuclear parameters after looking at the rest of the data
  public double getMedianNuclearArea(){
    double[] areas = this.getAreas();
    double median = NuclearOrganisationUtility.quartile(areas, 50);
    return median;
  }

  public double getMedianNuclearPerimeter(){
    double[] p = this.getPerimeters();
    double median = NuclearOrganisationUtility.quartile(p, 50);
    return median;
  }

  public double getMedianPathLength(){
    double[] p = this.getPathLengths();
    double median = NuclearOrganisationUtility.quartile(p, 50);
    return median;
  }

  public double getMedianArrayLength(){
    double[] p = this.getArrayLengths();
    double median = NuclearOrganisationUtility.quartile(p, 50);
    return median;
  }

  public double getMedianFeretLength(){
    double[] p = this.getFerets();
    double median = NuclearOrganisationUtility.quartile(p, 50);
    return median;
  }

  public double getMaxProfileLength(){
    return NuclearOrganisationUtility.getMax(this.getArrayLengths());
  }

  public Set<String> getNamesOfPointsOfInterest(){

    INuclearFunctions n = this.nucleiCollection.get(0);
    Set<String> headings = n.getBorderPointsOfInterest().keySet();
    return headings;
  }

  public ArrayList<INuclearFunctions> getNucleiWithSignals(int channel){
    ArrayList<INuclearFunctions> result = new ArrayList<INuclearFunctions>(0);

    for(INuclearFunctions n : this.nucleiCollection){

      switch (channel) {
        case Nucleus.RED_CHANNEL:
          if(n.hasRedSignal()){
            result.add(n);
          }
          break;
        case Nucleus.GREEN_CHANNEL:
          if(n.hasGreenSignal()){
            result.add(n);
          }
          break;
        case Nucleus.NOT_RED_CHANNEL:  
          if(!n.hasRedSignal()){
              result.add(n);
          }
          break;
        case Nucleus.NOT_GREEN_CHANNEL:  
          if(!n.hasGreenSignal()){
              result.add(n);
          }
          break;
      }
    }
    return result;
  }

  public HashMap<Double, Collection<Double>> getProfileAggregate(String pointType){
    return this.profileCollection.get(pointType);
  }

  public void addProfileAggregate(String pointType , HashMap<Double, Collection<Double>> profile){
    this.profileCollection.put(pointType, profile);
  }

  public double[] getNormalisedMedianProfileFromPoint(String pointType ){
    Double[] profile = this.normalisedMedianProfileFromPoint.get(pointType);
    return NuclearOrganisationUtility.getdoubleFromDouble(profile);
  }

  public void addNormalisedMedianProfileFromPoint(String pointType , double[] profile){
    Double[] result = NuclearOrganisationUtility.getDoubleFromdouble(profile);
    this.normalisedMedianProfileFromPoint.put(pointType, result);
  }

  public double[] getDifferencesToMedianFromPoint(String pointType){
    double[] d = new double[this.getNucleusCount()];
    for(int i=0;i<this.getNucleusCount();i++){
      INuclearFunctions n = this.getNucleus(i);
      try{
        d[i] = n.getDifferenceToMedianProfile(pointType);
      } catch(Exception e){
        IJ.log("    Unable to get difference to median profile: "+i+": "+pointType);
        IJ.append("    Unable to get difference to median profile: "+i+": "+pointType, this.debugFile.getAbsolutePath());
      }
    }
    return d;
  }

  // get the plot from the collection corresponding to the given pointType of interest
  public Plot getPlot(String pointType, String plotType){
    return this.plotCollection.get(pointType).get(plotType);
  }

  public void addMedianProfileFeatureIndex(String profile, String indexType, int index){

    HashMap<String, Integer> indexHash = this.medianProfileFeatureIndexes.get(profile);
    if(indexHash==null){
      indexHash = new HashMap<String, Integer>();
    }
    indexHash.put(indexType, index);
    this.medianProfileFeatureIndexes.put(profile, indexHash);
  }

  public int getMedianProfileFeatureIndex(String profile, String indexType){
    return this.medianProfileFeatureIndexes.get(profile).get(indexType);
  }

  public int[] getPointIndexes(String pointType){
    int[] d = new int[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){
      INuclearFunctions n = this.getNucleus(i);
      d[i] = n.getBorderIndexOfInterest(pointType);
    }
    return d;
  }

  public double[] getPointToPointDistances(String pointTypeA, String pointTypeB){
    double[] d = new double[this.getNucleusCount()];
    for(int i=0;i<this.getNucleusCount();i++){
      INuclearFunctions n = this.getNucleus(i);
      d[i] = n.getBorderPointOfInterest(pointTypeA).getLengthTo(n.getBorderPointOfInterest(pointTypeB));
    }
    return d;
  }

  /*
    -----------------
    Basic filtering of population
    -----------------
  */

  /*
    The filters needed to separate out objects from nuclei
    Filter on: nuclear area, perimeter and array length to find
    conjoined nuclei and blobs too small to be nuclei
    Use path length to remove poorly thresholded nuclei
  */
  public void refilterNuclei(Analysable failedCollection){

    IJ.log("    Filtering nuclei...");
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
      INuclearFunctions n = this.getNucleus(i);
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

    int afterSize = this.getNucleusCount();
    int removed = beforeSize - afterSize;

    IJ.append("Postfiltered:", this.getDebugFile().getAbsolutePath());
    this.exportFilterStats();
    IJ.log("    Removed due to size or length issues: "+removed+" nuclei");
    IJ.append("  Due to area outside bounds "+(int)minArea+"-"+(int)maxArea+": "+area+" nuclei", this.getDebugFile().getAbsolutePath());
    IJ.append("  Due to perimeter outside bounds "+(int)minPerim+"-"+(int)maxPerim+": "+perim+" nuclei", this.getDebugFile().getAbsolutePath());
    IJ.append("  Due to wibbliness >"+(int)maxPathLength+" : "+(int)pathlength+" nuclei", this.getDebugFile().getAbsolutePath());
    IJ.append("  Due to array length: "+arraylength+" nuclei", this.getDebugFile().getAbsolutePath());
    IJ.append("  Due to feret length: "+feretlength+" nuclei", this.getDebugFile().getAbsolutePath());
    // IJ.append("  Due to curve shape: "+curveShape+" nuclei", this.getDebugFile().getAbsolutePath());
    IJ.log("    Remaining: "+this.getNucleusCount()+" nuclei");
  }

  /*
    -----------------
    Profile functions
    -----------------
  */

  /*
    Calculate median angles at each bin within an angle profile
  */
  protected ArrayList<Double[]> calculateMediansAndQuartilesOfProfile(HashMap<Double, Collection<Double>> profile){

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
            double median = NuclearOrganisationUtility.quartile(d, 50.0);
            double q1     = NuclearOrganisationUtility.quartile(d, 25.0);
            double q3     = NuclearOrganisationUtility.quartile(d, 75.0);
            double q10    = NuclearOrganisationUtility.quartile(d, 10.0);
            double q90    = NuclearOrganisationUtility.quartile(d, 90.0);
           
            xmedians[m] = k;
            ymedians[m] = median;
            lowQuartiles[m] = q1;
            uppQuartiles[m] = q3;
            tenQuartiles[m] = q10;
            ninetyQuartiles[m] = q90;
            numberOfPoints[m] = (double)n;
          }
        } catch(Exception e){
             // IJ.log("    Cannot calculate median for "+k);
             IJ.append("Cannot calculate median for "+k+": "+e, this.getDebugFile().getAbsolutePath());
             IJ.append("\tFolder: "+this.getFolder().getAbsolutePath(), this.getDebugFile().getAbsolutePath());
             IJ.append("\tCollection: "+this.getType(), this.getDebugFile().getAbsolutePath());
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

        // IJ.log("    Repaired medians at "+i+" with values from  "+replacementIndex);
        IJ.append("\tRepaired medians at "+i+" with values from  "+replacementIndex, this.getDebugFile().getAbsolutePath());
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

  protected void updateProfileAggregate(double[] xvalues, double[] yvalues, HashMap<Double, Collection<Double>> profileAggregate){

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

  public void createProfileAggregateFromPoint(String pointType){

    HashMap<Double, Collection<Double>> profileAggregate = new HashMap<Double, Collection<Double>>();
    this.addProfileAggregate(pointType, profileAggregate);

    for(int i=0;i<this.getNucleusCount();i++){

      INuclearFunctions n = this.getNucleus(i);

      double[] xvalues = n.getNormalisedProfilePositions();

      NucleusBorderPoint indexPoint = n.getBorderPointOfInterest(pointType);
      int index = n.getAngleProfile().getIndexOfPoint(indexPoint);
      double[] yvalues = n.getAngleProfile().getInteriorAngles(index);

      updateProfileAggregate(xvalues, yvalues, profileAggregate); 
    }
  }

  public void createProfileAggregates(){

    Set<String> headings = this.getNamesOfPointsOfInterest();
    for( String pointType : headings ){
      createProfileAggregateFromPoint(pointType);
    }
  }


  /*
    Finds, as a list of index integers, the points
    of local minimum in the median profile line
  */
  protected ArrayList<Integer> detectLocalMinimaInMedian(double[] medianProfile){
    // go through angle array (with tip at start)
    // look at 1-2-3-4-5 points ahead and behind.
    // if all greater, local minimum
    int lookupDistance = 5;
    
    double[] prevAngles = new double[lookupDistance]; // slots for previous angles
    double[] nextAngles = new double[lookupDistance]; // slots for next angles

    // int count = 0;

    ArrayList<Integer> medianIndexMinima = new ArrayList<Integer>(0);

    for (int i=0; i<medianProfile.length; i++) { // for each position in sperm

      // go through each lookup position and get the appropriate angles
      for(int j=0;j<prevAngles.length;j++){

        int prev_i = NuclearOrganisationUtility.wrapIndex( i-(j+1), medianProfile.length ); // the index j+1 before i
        int next_i = NuclearOrganisationUtility.wrapIndex( i+(j+1), medianProfile.length ); // the index j+1 after i

        // fill the lookup array
        prevAngles[j] = medianProfile[prev_i];
        nextAngles[j] = medianProfile[next_i];
      }
      
      // with the lookup positions, see if minimum at i
      // return a 1 if all higher than last, 0 if not
      // prev_l = 0;
      int errors = 2; // allow two positions to be out of place; better handling of noisy data
      boolean ok = true;
      for(int l=0;l<prevAngles.length;l++){

        // for the first position in prevAngles, compare to the current index
        if(l==0){
          if(prevAngles[l] < medianProfile[i] || nextAngles[l] < medianProfile[i]){
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

  public static double[] interpolateMedianToLength(int newLength, double[] medianProfile){

    int oldLength = medianProfile.length;
    
    double[] newMedianCurve = new double[newLength];
    // where in the old curve index is the new curve index?
    for (int i=0; i<newLength; i++) {
      // we have a point in the new curve.
      // we want to know which points it lay between in the old curve
      double oldIndex = ( (double)i / (double)newLength)*oldLength; // get the frational index position needed
      double interpolatedMedian = interpolateNormalisedMedian(oldIndex, medianProfile);
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
  public static double interpolateNormalisedMedian(double normIndex, double[] medianProfile){

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
    medianIndexLower  = NuclearOrganisationUtility.wrapIndex(medianIndexLower , medianProfile.length);
    medianIndexHigher = NuclearOrganisationUtility.wrapIndex(medianIndexHigher, medianProfile.length);

    // get the angle values in the median profile at the given indices
    double medianAngleLower  = medianProfile[medianIndexLower ];
    double medianAngleHigher = medianProfile[medianIndexHigher];

    // interpolate on a stright line between the points
    double medianAngleDifference = medianAngleHigher - medianAngleLower;
    double positionToFind = medianIndexHigher - normIndex;
    double interpolatedMedianAngle = (medianAngleDifference * positionToFind) + medianAngleLower;
    return interpolatedMedianAngle;
  }

  // /*
  //   For each nucleus in the collection see if there is a differences to the given median
  // */
  public void calculateDifferencesToMedianProfiles(){

    Set<String> headings = this.getNamesOfPointsOfInterest();
    for( String pointType : headings ){

      double[] medianProfile = getNormalisedMedianProfileFromPoint(pointType);

      for(int i= 0; i<this.getNucleusCount();i++){ // for each nucleus
        INuclearFunctions n = this.getNucleus(i);
        double difference = n.calculateDifferenceToMedianProfile(medianProfile);
        n.addDifferenceToMedianProfile(pointType, difference);
      } 
    }
  }

  /*
    -----------------
    Annotate images
    -----------------
  */

  

  public void measureNuclearOrganisation(){

    if(this.getRedSignalCount()>0 || this.getGreenSignalCount()>0){

      for(int i= 0; i<this.getNucleusCount();i++){
        INuclearFunctions n = this.getNucleus(i);
        n.exportSignalDistanceMatrix();

      }
      this.exportSignalStats();
      this.exportDistancesBetweenSingleSignals();
      this.addSignalsToProfileCharts();
    }
  }

  /*
    -----------------
    Export functions
    -----------------
  */

  public String makeGlobalLogFile(String filename){
    String file = this.getFolder()+File.separator+this.getOutputFolder()+File.separator+filename+"."+getType()+".txt";
    File f = new File(file);
    if(f.exists()){
      f.delete();
    }
    return file;
  }

  /*
    Export the signal parameters of the nucleus to the designated log file
  */
  public void exportSignalStats(){

    String redLogFile   = makeGlobalLogFile( "logRedSignals"  );
    String greenLogFile = makeGlobalLogFile( "logGreenSignals");

    IJ.append("NUCLEUS_NUMBER\tSIGNAL_AREA\tSIGNAL_ANGLE\tSIGNAL_FERET\tSIGNAL_DISTANCE\tFRACTIONAL_DISTANCE\tSIGNAL_PERIMETER\tSIGNAL_RADIUS\tPATH", redLogFile);
    IJ.append("NUCLEUS_NUMBER\tSIGNAL_AREA\tSIGNAL_ANGLE\tSIGNAL_FERET\tSIGNAL_DISTANCE\tFRACTIONAL_DISTANCE\tSIGNAL_PERIMETER\tSIGNAL_RADIUS\tPATH", greenLogFile);
    
    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi

      INuclearFunctions n = this.getNucleus(i);

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
                       s.getRadius()                   +"\t"+
                       s.getPerimeter()                +"\t"+
                       path, log);
          } // end for
        } // end if
        signalCount++;
      } // end for
    } // end for
  }

  public void exportDistancesBetweenSingleSignals(){

    String logFile = makeGlobalLogFile("logDistances");

    IJ.append("DISTANCE_BETWEEN_SIGNALS\tRED_DISTANCE_TO_COM\tGREEN_DISTANCE_TO_COM\tNUCLEAR_FERET\tRED_FRACTION_OF_FERET\tGREEN_FRACTION_OF_FERET\tDISTANCE_BETWEEN_SIGNALS_FRACTION_OF_FERET\tNORMALISED_DISTANCE\tPATH", logFile);

    for(int i=0; i<this.getNucleusCount();i++){

      INuclearFunctions n = this.nucleiCollection.get(i);
      if(n.getRedSignalCount()==1 && n.getGreenSignalCount()==1){

        NuclearSignal r = n.getRedSignals().get(0);
        NuclearSignal g = n.getGreenSignals().get(0);

        XYPoint rCoM = r.getCentreOfMass();
        XYPoint gCoM = g.getCentreOfMass();

        double distanceBetween = rCoM.getLengthTo(gCoM);    
        double rDistanceToCoM = rCoM.getLengthTo(n.getCentreOfMass());
        double gDistanceToCoM = gCoM.getLengthTo(n.getCentreOfMass());
        double nFeret = n.getFeret();

        double rFractionOfFeret = rDistanceToCoM / nFeret;
        double gFractionOfFeret = gDistanceToCoM / nFeret;
        double distanceFractionOfFeret = distanceBetween / nFeret;
        double normalisedPosition = distanceFractionOfFeret / rFractionOfFeret / gFractionOfFeret;

        IJ.append(  distanceBetween+"\t"+
                    rDistanceToCoM+"\t"+
                    gDistanceToCoM+"\t"+
                    nFeret+"\t"+
                    rFractionOfFeret+"\t"+
                    gFractionOfFeret+"\t"+
                    distanceFractionOfFeret+"\t"+
                    normalisedPosition+"\t"+
                    n.getPath(), logFile);
      }
    }
  }

  public void exportAnnotatedNuclei(){
    for(int i=0; i<this.getNucleusCount();i++){
      INuclearFunctions n = this.getNucleus(i);
      n.exportAnnotatedImage();
    }
  }

  public void exportAngleProfiles(){
    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi

      INuclearFunctions n = this.getNucleus(i);
      n.exportAngleProfile();
    }
  }

  public void exportMediansOfProfile(double[] profile, String filename){

    String logFile = makeGlobalLogFile(filename);

    String outLine = "X_POSITION\tANGLE_MEDIAN\r\n";
    
    for(int i =0;i<profile.length;i++){
      outLine +=  i+"\t"+profile[i]+"\r\n";
    }
    IJ.append(outLine, logFile); 
  }

  public void exportMediansAndQuartilesOfProfile(ArrayList<Double[]> profile, String filename){

    String logFile = makeGlobalLogFile(filename);

    String outLine = "X_POSITION\tANGLE_MEDIAN\tQ25\tQ75\tQ10\tQ90\tNUMBER_OF_POINTS\r\n";
    

    for(int i =0;i<profile.get(0).length;i++){
      outLine +=  profile.get(0)[i]+"\t"+
                  profile.get(1)[i]+"\t"+
                  profile.get(2)[i]+"\t"+
                  profile.get(3)[i]+"\t"+
                  profile.get(4)[i]+"\t"+
                  profile.get(5)[i]+"\t"+
                  profile.get(6)[i]+"\r\n";
    }
    IJ.append(outLine, logFile); 
  }

  /*
    To hold the nuclear stats (and any stats), we want a structure that can 
    hold: a column of data. Any arbitrary other numbers of columns of data.
  */
  public Map<String, List<String>> calculateNuclearStats(){

    Map<String, List<String>> stats = new LinkedHashMap<String, List<String>>();

    String[] sAreas        = NuclearOrganisationUtility.getStringFromDouble(this.getAreas());
    String[] sPerims       = NuclearOrganisationUtility.getStringFromDouble(this.getPerimeters());
    String[] sFerets       = NuclearOrganisationUtility.getStringFromDouble(this.getFerets());
    String[] sPathlengths  = NuclearOrganisationUtility.getStringFromDouble(this.getPathLengths());
    String[] sDistances    = NuclearOrganisationUtility.getStringFromDouble(this.getMedianDistanceBetweenPoints());
    String[] sPaths        = this.getNucleusPaths();

    stats.put("AREA",        Arrays.asList(  sAreas));
    stats.put("PERIMETER",   Arrays.asList(  sPerims));
    stats.put("FERET",       Arrays.asList(  sFerets));
    stats.put("PATH_LENGTH", Arrays.asList(  sPathlengths));
    stats.put("MEDIAN_DISTANCE_BETWEEN_POINTS", Arrays.asList(sDistances ) );
    stats.put("PATH",        Arrays.asList(  sPaths));

    return stats;
  }

  public void exportStats(Map<String, List<String>> stats, String filename){
    String statsFile = makeGlobalLogFile(filename);

    StringBuilder outLine = new StringBuilder();

    Set<String> headings = stats.keySet();
    for(String heading : headings){
      outLine.append(heading+"\t");
    }
    outLine.append("\r\n");


    for(int i=0;i<this.getNucleusCount();i++){
      for(String heading : headings){
        List<String> column = stats.get(heading);
        outLine.append(column.get(i)+"\t");
      }
      outLine.append("\r\n");
    }
    IJ.append(  outLine.toString(), statsFile);
  }
  
  // this is for the mapping of image to path for 
  // identifying FISHed nuclei in prefish images
  public void exportImagePaths(String filename){
    Map<String, List<String>> stats = new LinkedHashMap<String, List<String>>();
    String[] sPaths     = this.getCleanNucleusPaths();
    String[] sPositions = this.getPositions();
    stats.put("PATH",    Arrays.asList(  sPaths    ));
    stats.put("POSITION",Arrays.asList(  sPositions));
    exportStats(stats, filename);
  }

  public void exportNuclearStats(String filename){
  
    Map<String, List<String>> stats = this.calculateNuclearStats();
    exportStats(stats, filename);
  }

  public void exportFilterStats(){

    double medianArea = this.getMedianNuclearArea();
    double medianPerimeter = this.getMedianNuclearPerimeter();
    double medianPathLength = this.getMedianPathLength();
    double medianArrayLength = this.getMedianArrayLength();
    double medianFeretLength = this.getMedianFeretLength();
    // double medianDifferenceToMedianCurve = NuclearOrganisationUtility.quartile(this.getDifferencesToMedian(),50);

    IJ.append("    Area: "        +(int)medianArea,                    this.getDebugFile().getAbsolutePath());
    IJ.append("    Perimeter: "   +(int)medianPerimeter,               this.getDebugFile().getAbsolutePath());
    IJ.append("    Path length: " +(int)medianPathLength,              this.getDebugFile().getAbsolutePath());
    IJ.append("    Array length: "+(int)medianArrayLength,             this.getDebugFile().getAbsolutePath());
    IJ.append("    Feret length: "+(int)medianFeretLength,             this.getDebugFile().getAbsolutePath());
    // IJ.append("    Curve: "       +(int)medianDifferenceToMedianCurve, this.getDebugFile().getAbsolutePath());
  }

  public void exportCompositeImage(String filename){

    // foreach nucleus
    // createProcessor (500, 500)
    // sertBackgroundValue(0)
    // paste in old image at centre
    // insert(ImageProcessor ip, int xloc, int yloc)
    // rotate about CoM (new position)
    // display.
    if(this.getNucleusCount()==0){
      return;
    }
    IJ.log("    Creating composite image...");
    

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
      
      INuclearFunctions n = this.getNucleus(i);
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
        // newProcessor.rotate( n.findRotationAngle() );
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
        IJ.log("    Error adding image to composite");
        IJ.append("Error adding image to composite: "+e, this.getDebugFile().getAbsolutePath());
        IJ.append("  "+getType(), this.getDebugFile().getAbsolutePath());
        IJ.append("  "+path, this.getDebugFile().getAbsolutePath());
      }     
    }
    // finalImage.show();
    IJ.saveAsTiff(finalImage, this.getFolder()+File.separator+this.getOutputFolder()+File.separator+filename+"."+getType()+".tiff");
    IJ.log("    Composite image created");
  }

  public void exportProfilePlot(Plot plot, String name){
    ImagePlus image = plot.getImagePlus();
    Calibration cal = image.getCalibration();
    cal.setUnit("pixels");
    cal.pixelWidth = 1;
    cal.pixelHeight = 1;
    IJ.saveAsTiff(image, this.getFolder()+File.separator+this.getOutputFolder()+File.separator+name+"."+this.getType()+".tiff");
  }

  /*
    Create the charts of the profiles of the nuclei within this collecion.
  */
  public void preparePlots(){

    Set<String> headings = this.getNamesOfPointsOfInterest();
    for( String pointType : headings ){

      Plot  rawPlot = new Plot( "Raw "       +pointType+"-indexed plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);
      Plot normPlot = new Plot( "Normalised "+pointType+"-indexed plot", "Position", "Angle", Plot.Y_GRID | Plot.X_GRID);

      rawPlot.setLimits(0,this.getMaxProfileLength(),-50,360);
      rawPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
      rawPlot.setYTicks(true);
      rawPlot.setColor(Color.BLACK);
      rawPlot.drawLine(0, 180, this.getMaxProfileLength(), 180); 
      rawPlot.setColor(Color.LIGHT_GRAY);

      normPlot.setLimits(0,100,-50,360);
      normPlot.setSize(CHART_WINDOW_WIDTH,CHART_WINDOW_HEIGHT);
      normPlot.setYTicks(true);
      normPlot.setColor(Color.BLACK);
      normPlot.drawLine(0, 180, 100, 180); 
      normPlot.setColor(Color.LIGHT_GRAY);

      HashMap<String, Plot> plotHash = new HashMap<String, Plot>();
      plotHash.put("raw" , rawPlot );
      plotHash.put("norm", normPlot);
      
      this.plotCollection.put(pointType, plotHash);
    }
  }    

  /*
    Draw the charts of the profiles of the nuclei within this collecion.
  */
  public void drawProfilePlots(){

    this.preparePlots();

    Set<String> headings = this.plotCollection.keySet();

    for( String pointType : headings ){
      Plot  rawPlot = getPlot(pointType, "raw" );
      Plot normPlot = getPlot(pointType, "norm");

      for(int i=0;i<this.getNucleusCount();i++){

        INuclearFunctions n = this.getNucleus(i);

        double[] xPointsRaw  = n.getRawProfilePositions();
        double[] xPointsNorm = n.getNormalisedProfilePositions();

        NucleusBorderPoint indexPoint = n.getBorderPointOfInterest(pointType);
        int index = n.getAngleProfile().getIndexOfPoint(indexPoint);
        double[] anglesFromPoint = n.getAngleProfile().getInteriorAngles(index);

        rawPlot.setColor(Color.LIGHT_GRAY);
        rawPlot.addPoints(xPointsRaw, anglesFromPoint, Plot.LINE);

        normPlot.setColor(Color.LIGHT_GRAY);
        normPlot.addPoints(xPointsNorm, anglesFromPoint, Plot.LINE);
      }
    }   
  }

  public void calculateNormalisedMedianLineFromPoint(String pointType){
    HashMap<Double, Collection<Double>> profileAggregate = this.getProfileAggregate(pointType);

    ArrayList<Double[]> medians = calculateMediansAndQuartilesOfProfile( profileAggregate );
    double[] ymedians        =  NuclearOrganisationUtility.getdoubleFromDouble( medians.get(1) );
    this.addNormalisedMedianProfileFromPoint(pointType, ymedians);
  }

  /*
    Draw a median profile on the normalised plots.
  */
  public void drawNormalisedMedianLineFromPoint(String pointType, Plot plot){

    HashMap<Double, Collection<Double>> profileAggregate = this.getProfileAggregate(pointType);

    ArrayList<Double[]> medians = calculateMediansAndQuartilesOfProfile( profileAggregate );
    this.exportMediansAndQuartilesOfProfile(medians, "logMediansFrom"+pointType); // needs to be "logMediansFrom<pointname>"

    double[] xmedians        =  NuclearOrganisationUtility.getdoubleFromDouble( medians.get(0) );
    double[] ymedians        =  NuclearOrganisationUtility.getdoubleFromDouble( medians.get(1) );
    double[] lowQuartiles    =  NuclearOrganisationUtility.getdoubleFromDouble( medians.get(2) );
    double[] uppQuartiles    =  NuclearOrganisationUtility.getdoubleFromDouble( medians.get(3) );
    // double[] tenQuartiles    =  NuclearOrganisationUtility.getdoubleFromDouble( medians.get(4) );
    // double[] ninetyQuartiles =  NuclearOrganisationUtility.getdoubleFromDouble( medians.get(5) );

    // add the median lines to the chart
    plot.setColor(Color.BLACK);
    plot.setLineWidth(3);
    plot.addPoints(xmedians, ymedians, Plot.LINE);

    plot.setColor(Color.DARK_GRAY);
    plot.setLineWidth(2);
    plot.addPoints(xmedians, lowQuartiles, Plot.LINE);
    plot.addPoints(xmedians, uppQuartiles, Plot.LINE);

    this.addNormalisedMedianProfileFromPoint(pointType, ymedians);
  }

  /*
    Draw a boxplot on the normalised plots. Specify which BorderPointOfInterest is to be plotted.
  */
  public void drawBoxplotOnNormalisedMedianLineFromPoint(String profilePointType, Plot plot, String boxPointType){

    // get the tail positions with the head offset applied
    double[] xPoints = new double[this.getNucleusCount()];
    for(int i= 0; i<this.getNucleusCount();i++){

      INuclearFunctions n = this.getNucleus(i);
      xPoints[i] =  ((double) n.getOffsetIndex(n.getBorderPointOfInterest(boxPointType), profilePointType) / (double)n.getLength()) *100;
    }
    double[] yPoints = new double[xPoints.length];
    Arrays.fill(yPoints, CHART_TAIL_BOX_Y_MID); // all dots at y=300
    plot.setColor(Color.LIGHT_GRAY);
    plot.addPoints(xPoints, yPoints, Plot.DOT);

    // median tail positions
    double tailQ50 = NuclearOrganisationUtility.quartile(xPoints, 50);
    double tailQ25 = NuclearOrganisationUtility.quartile(xPoints, 25);
    double tailQ75 = NuclearOrganisationUtility.quartile(xPoints, 75);

    plot.setColor(Color.DARK_GRAY);
    plot.setLineWidth(1);
    plot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MAX, tailQ75, CHART_TAIL_BOX_Y_MAX);
    plot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MIN);
    plot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ25, CHART_TAIL_BOX_Y_MAX);
    plot.drawLine(tailQ75, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MAX);
    plot.drawLine(tailQ50, CHART_TAIL_BOX_Y_MIN, tailQ50, CHART_TAIL_BOX_Y_MAX);
  }

  public void drawNormalisedMedianLines(){

    Set<String> headings = this.plotCollection.keySet();
    for( String pointType : headings ){

        Plot normPlot = getPlot(pointType, "norm");
        drawNormalisedMedianLineFromPoint(pointType, normPlot);
        drawBoxplotOnNormalisedMedianLineFromPoint(pointType, normPlot, "tail");
    }
  }

  public void addSignalsToProfileCharts(){

    Set<String> headings = this.plotCollection.keySet();

    for( String pointType : headings ){
      Plot normPlot = getPlot(pointType, "norm");
      this.addSignalsToProfileChartFromPoint(pointType, normPlot);

    }    
  }

  public void addSignalsToProfileChartFromPoint(String pointType, Plot plot){
    // for each signal in each nucleus, find index of point. Draw dot

    plot.setColor(Color.LIGHT_GRAY);
    plot.setLineWidth(1);
    plot.drawLine(0,CHART_SIGNAL_Y_LINE_MIN,100,CHART_SIGNAL_Y_LINE_MIN);
    plot.drawLine(0,CHART_SIGNAL_Y_LINE_MAX,100,CHART_SIGNAL_Y_LINE_MAX);

    for(int i= 0; i<this.getNucleusCount();i++){

      INuclearFunctions n = this.getNucleus(i);
      ArrayList<ArrayList<NuclearSignal>> signals = new ArrayList<ArrayList<NuclearSignal>>(0);
      signals.add(n.getRedSignals());
      signals.add(n.getGreenSignals());

      int signalCount = 0;
      for( ArrayList<NuclearSignal> signalGroup : signals ){
        
        if(signalGroup.size()>0){

          Color colour = signalCount == Nucleus.RED_CHANNEL ? Color.RED : Color.GREEN;

          ArrayList<Double> xPoints = new ArrayList<Double>(0);
          ArrayList<Double> yPoints = new ArrayList<Double>(0);

          for(int j=0; j<signalGroup.size();j++){

            NucleusBorderPoint border = signalGroup.get(j).getClosestBorderPoint();
            for(int k=0; k<n.getLength();k++){

              // We want to get the profile position, offset to the pointType 
              if(n.getBorderPoint(k).overlaps(border)){
                int rawIndex = n.getOffsetIndex(n.getBorderPoint(k), pointType);
                double normIndex = ((double) rawIndex / (double) n.getLength()) *100;
                xPoints.add( normIndex );
                double yPosition = CHART_SIGNAL_Y_LINE_MIN + ( signalGroup.get(j).getFractionalDistanceFromCoM() * ( CHART_SIGNAL_Y_LINE_MAX - CHART_SIGNAL_Y_LINE_MIN) ); // 
                yPoints.add(yPosition);
              }
            }
          }
          plot.setColor(colour);
          plot.setLineWidth(2);
          plot.addPoints(xPoints, yPoints, Plot.DOT);
        }
        signalCount++;
      }
    }
  }

  public void exportProfilePlots(){

    Set<String> headings = this.plotCollection.keySet();
    for( String pointType : headings ){

      Plot normPlot = getPlot(pointType, "norm");
      Plot  rawPlot = getPlot(pointType, "raw" );

      exportProfilePlot(normPlot, "plot"+pointType+"Norm");
      exportProfilePlot(rawPlot , "plot"+pointType+"Raw");
    }  
  }
}