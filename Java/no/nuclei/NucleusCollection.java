/* 
  -----------------------
  NUCLEUS COLLECTION CLASS
  -----------------------
  This class contains the nuclei that pass detection criteria
  Provides aggregate stats
  It enables offsets to be calculated based on the median normalised curves
*/

package no.nuclei;
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


public class NucleusCollection {

	private File folder; // the source of the nuclei
  private File debugFile;
  private String collectionType; // for annotating image names

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

	private ArrayList<Nucleus> nucleiCollection = new ArrayList<Nucleus>(0); // store all the nuclei analysed

	public NucleusCollection(File folder, String type){
		this.folder = folder;
    this.debugFile = new File(folder.getAbsolutePath()+File.separator+"logDebug.txt");
    this.collectionType = type;
	}

  /*
    -----------------------
    Define adders for all
    types of nucleus eligable
    -----------------------
  */

	public void addNucleus(Nucleus r){
		this.nucleiCollection.add(r);
	}

  /*
    -----------------------
    Getters for aggregate stats
    -----------------------
  */

  public File getFolder(){
    return this.folder;
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

  public String[] getNucleusPaths(){
    String[] s = new String[nucleiCollection.size()];

    for(int i=0;i<nucleiCollection.size();i++){
      s[i] = nucleiCollection.get(i).getPath()+"-"+nucleiCollection.get(i).getNucleusNumber();
    }
    return s;
  }

  public int getNucleusCount(){
    return this.nucleiCollection.size();
  }

  public ArrayList<Nucleus> getNuclei(){
    return this.nucleiCollection;
  }

  public Nucleus getNucleus(int i){
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
    double median = quartile(areas, 50);
    return median;
  }

  public double getMedianNuclearPerimeter(){
    double[] p = this.getPerimeters();
    double median = quartile(p, 50);
    return median;
  }

  public double getMedianPathLength(){
    double[] p = this.getPathLengths();
    double median = quartile(p, 50);
    return median;
  }

  public double getMedianArrayLength(){
    double[] p = this.getArrayLengths();
    double median = quartile(p, 50);
    return median;
  }

  public double getMedianFeretLength(){
    double[] p = this.getFerets();
    double median = quartile(p, 50);
    return median;
  }

  public ArrayList<Nucleus> getNucleiWithSignals(int channel){
    ArrayList<Nucleus> result = new ArrayList<Nucleus>(0);

    for(Nucleus n : this.nucleiCollection){

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

  public double[] getDifferencesToMedian(){
    double[] d = new double[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){
      d[i] = this.getNucleus(i).getDifferenceToMedianCurve();
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
  public void refilterNuclei(NucleusCollection failedCollection){

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
      Nucleus n = this.getNucleus(i);
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
    -----------------
    Profile functions
    -----------------
  */

  /*
    Calculate median angles at each bin within an angle profile
  */
  protected ArrayList<Double[]> calculateMediansAndQuartilesOfProfile(Map<Double, Collection<Double>> profile){

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

  protected void updateProfileAggregate(double[] xvalues, double[] yvalues, Map<Double, Collection<Double>> profileAggregate){

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


  /*
    Finds, as a list of index integers, the points
    of local minimum in the median profile line
  */
  private ArrayList<Integer> detectLocalMinimaInMedian(double[] medianProfile){
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

        int prev_i = wrapIndex( i-(j+1), medianProfile.length ); // the index j+1 before i
        int next_i = wrapIndex( i+(j+1), medianProfile.length ); // the index j+1 after i

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
    medianIndexLower  = wrapIndex(medianIndexLower , medianProfile.length);
    medianIndexHigher = wrapIndex(medianIndexHigher, medianProfile.length);

    // get the angle values in the median profile at the given indices
    double medianAngleLower  = medianProfile[medianIndexLower ];
    double medianAngleHigher = medianProfile[medianIndexHigher];

    // interpolate on a stright line between the points
    double medianAngleDifference = medianAngleHigher - medianAngleLower;
    double positionToFind = medianIndexHigher - normIndex;
    double interpolatedMedianAngle = (medianAngleDifference * positionToFind) + medianAngleLower;
    return interpolatedMedianAngle;
  }


  /*
    -----------------
    Export functions
    -----------------
  */

  /*
    Export the signal parameters of the nucleus to the designated log file
  */
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

  public void exportDistancesBetweenSingleSignals(){

    File logFile = new File(this.folder.getAbsolutePath()+File.separator+"logDistances."+collectionType+".txt");

    if(logFile.exists()){
      logFile.delete();
    }
    IJ.append("DISTANCE_BETWEEN_SIGNALS\tRED_DISTANCE_TO_COM\tGREEN_DISTANCE_TO_COM\tNUCLEAR_FERET\tRED_FRACTION_OF_FERET\tGREEN_FRACTION_OF_FERET\tDISTANCE_BETWEEN_SIGNALS_FRACTION_OF_FERET\tNORMALISED_DISTANCE\tPATH", logFile.getAbsolutePath());

    for(int i=0; i<this.getNucleusCount();i++){

      Nucleus n = this.nucleiCollection.get(i);
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
                    n.getPath(), logFile.getAbsolutePath());
      }
    }
  }

  public void exportAnnotatedNuclei(){
    for(int i=0; i<this.getNucleusCount();i++){
      Nucleus n = this.getNucleus(i);
      n.exportAnnotatedImage();
    }
  }

  public void exportMediansAndQuartilesOfProfile(ArrayList<Double[]> profile, String filename){

    String logFile = this.getFolder()+File.separator+filename+"."+this.getType()+".txt";
    File f = new File(logFile);
    if(f.exists()){
      f.delete();
    }

    String outLine = "# X_POSITION\tANGLE_MEDIAN\tQ25\tQ75\tQ10\tQ90\tNUMBER_OF_POINTS\n";
    

    for(int i =0;i<profile.get(0).length;i++){
      outLine +=  profile.get(0)[i]+"\t"+
                  profile.get(1)[i]+"\t"+
                  profile.get(2)[i]+"\t"+
                  profile.get(3)[i]+"\t"+
                  profile.get(4)[i]+"\t"+
                  profile.get(5)[i]+"\t"+
                  profile.get(6)[i]+"\n";
    }
    IJ.append(outLine, logFile); 
  }

  public void exportNuclearStats(String filename){
  
    String statsFile = this.getFolder()+File.separator+filename+"."+getType()+".txt";
    File f = new File(statsFile);
    if(f.exists()){
      f.delete();
    }

    String outLine = "# AREA\tPERIMETER\tFERET\tPATH_LENGTH\tDIFFERENCE\tFAILURE_CODE\tPATH\n";

    IJ.log("Exporting stats for "+this.getNucleusCount()+" nuclei ("+this.getType()+")");
    double[] areas        = this.getAreas();
    double[] perims       = this.getPerimeters();
    double[] ferets       = this.getFerets();
    double[] pathLengths  = this.getPathLengths();
    double[] differences  = this.getDifferencesToMedian();
    String[] paths        = this.getNucleusPaths();


    for(int i=0; i<this.getNucleusCount();i++){
      int j = i+1;

      outLine = outLine + areas[i]+"\t"+
                          perims[i]+"\t"+
                          ferets[i]+"\t"+
                          pathLengths[i]+"\t"+
                          differences[i]+"\t"+
                          this.getNucleus(i).getFailureCode()+"\t"+
                          paths[i]+"\n";

      // Include tip, CoM, tail
      // this.getNucleus(i).printLogFile(this.getNucleus(i).getNucleusFolder()+File.separator+this.getNucleus(i).getNucleusNumber()+".log");
    }
    IJ.append(  outLine, statsFile);
    IJ.log("Export complete");
  }

  public void exportFilterStats(){

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
      
      Nucleus n = this.getNucleus(i);
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

  public void exportProfilePlot(Plot plot, String name){
    ImagePlus image = plot.getImagePlus();
    IJ.saveAsTiff(image, this.getFolder()+File.separator+name+"."+this.getType()+".tiff");
  }

  /*
    -----------------
    General functions
    -----------------
  */

  /*
    Calculate the <lowerPercent> quartile from a Double[] array
  */
  protected static double quartile(double[] values, double lowerPercent) {

      if (values == null || values.length == 0) {
          throw new IllegalArgumentException("The data array either is null or does not contain any data.");
      }

      // Rank order the values
      double[] v = new double[values.length];
      System.arraycopy(values, 0, v, 0, values.length);
      Arrays.sort(v);

      int n = (int) Math.round(v.length * lowerPercent / 100);
      
      return (double)v[n];
  }

  protected static double quartile(Double[] values, double lowerPercent) {

    if (values == null || values.length == 0) {
        throw new IllegalArgumentException("The data array either is null or does not contain any data.");
    }

    // Rank order the values
    Double[] v = new Double[values.length];
    System.arraycopy(values, 0, v, 0, values.length);
    Arrays.sort(v);

    int n = (int) Math.round(v.length * lowerPercent / 100);
    
    return (double)v[n];
  }

  /*
    Turn a Double[] into a double[]
  */
  protected static double[] getDoubleFromDouble(Double[] d){
    double[] results = new double[d.length];
    for(int i=0;i<d.length;i++){
      results[i] = d[i];
    }
    return results;
  }

  protected static int wrapIndex(int i, int length){
    if(i<0)
      i = length + i; // if i = -1, in a 200 length array,  will return 200-1 = 199
    if(Math.floor(i / length)>0)
      i = i - ( ((int)Math.floor(i / length) )*length);

    if(i<0 || i>length){
      IJ.log("Warning: array out of bounds: "+i);
    }
    
    return i;
  }

  protected double getMin(double[] d){
    double min = getMax(d);
    for(int i=0;i<d.length;i++){
      if( d[i]<min)
        min = d[i];
    }
    return min;
  }

  protected double getMax(double[] d){
    double max = 0;
    for(int i=0;i<d.length;i++){
      if( d[i]>max)
        max = d[i];
    }
    return max;
  }
}