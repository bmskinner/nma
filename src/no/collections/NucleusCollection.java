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
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.TextRoi;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

import no.collections.INuclearCollection;
import no.analysis.ShellAnalyser;
import no.analysis.ShellCounter;
import no.nuclei.*;
import no.components.*;
import no.utility.Stats;
import no.utility.Utils;



public class NucleusCollection
  implements INuclearCollection 
{

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

  private double maxDifferenceFromMedian = 1.6; // used to filter the nuclei, and remove those too small, large or irregular to be real
  private double maxWibblinessFromMedian = 1.4; // filter for the irregular borders more stringently

//  private Map<String, HashMap<String, Plot>> plotCollection = new HashMap<String, HashMap<String, Plot>>();

  //this holds the mapping of tail indexes etc in the median profile arrays
  protected ProfileCollection profileCollection = new ProfileCollection();

  private List<INuclearFunctions> nucleiCollection = new ArrayList<INuclearFunctions>(0); // store all the nuclei analysed

  public NucleusCollection(File folder, String outputFolder, String type){
	  this.folder = folder;
	  this.outputFolder = outputFolder;
	  this.debugFile = new File(folder.getAbsolutePath()+File.separator+outputFolder+File.separator+"logDebug.txt");
	  this.collectionType = type;
  }

  // used only for getting classes in setup of analysis
  public NucleusCollection(){

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

  public void measureProfilePositions(){
    this.measureProfilePositions("head");
  }

  public void measureProfilePositions(String pointType){

    this.createProfileAggregateFromPoint(pointType);

    this.findTailIndexInMedianCurve();

    double score = this.compareProfilesToMedian(pointType);
    double prevScore = score+1;

    while(score < prevScore){

      this.createProfileAggregateFromPoint(pointType);
      this.findTailIndexInMedianCurve();
      this.calculateOffsets();

      prevScore = score;
      score = this.compareProfilesToMedian(pointType);

      IJ.log("    Reticulating splines: score: "+(int)score);
    }

    this.createProfileAggregates();

    this.drawProfilePlots();
    this.drawNormalisedMedianLines();

    this.exportProfilePlots();
  }

  public void calculateOffsets(){

    Profile medianToCompare = this.profileCollection.getProfile("head"); // returns a median profile with head at 0

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi
      Nucleus n = (Nucleus)this.getNucleus(i);

      // returns the positive offset index of this profile which best matches the median profile
      int newHeadIndex = n.getAngleProfile().getSlidingWindowOffset(medianToCompare);
      n.addBorderTag("head", newHeadIndex);
      
      // check if flipping the profile will help
      
      double differenceToMedian1 = n.getAngleProfile("head").differenceToProfile(medianToCompare);
      n.reverse();
      double differenceToMedian2 = n.getAngleProfile("head").differenceToProfile(medianToCompare);
      
      if(differenceToMedian1<differenceToMedian2){
    	  n.reverse(); // put it back if no better
      }
      
//      int newHeadIndex = n.getAngleProfile().getSlidingWindowOffset(medianToCompare);
//      n.addBorderTag("head", newHeadIndex);

      // also update the tail position
      int tailIndex = n.getIndex(n.findOppositeBorder( n.getPoint(newHeadIndex) ));
      n.addBorderTag("tail", tailIndex);
    }
  }

  /*
    -----------------------
    Getters for aggregate stats
    -----------------------
  */
  
  public ProfileCollection getProfileCollection(){
	  return this.profileCollection;
  }

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
  
  public double[] getMinFerets(){

	    double[] d = new double[nucleiCollection.size()];

	    for(int i=0;i<nucleiCollection.size();i++){
	      d[i] = nucleiCollection.get(i).getNarrowestDiameter();
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
      d[i] = nucleiCollection.get(i).getMedianDistanceBetweenPoints();
    }
    return d;
  }

  public String[] getNucleusPaths(){
    String[] s = new String[nucleiCollection.size()];

    for(int i=0;i<nucleiCollection.size();i++){
      s[i] = nucleiCollection.get(i).getPath(); //+"-"+nucleiCollection.get(i).getNucleusNumber();
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

  public List<INuclearFunctions> getNuclei(){
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

  
  public List<NuclearSignal> getSignals(int channel){
	  
	  List<NuclearSignal> result = new ArrayList<NuclearSignal>(0);
	  
	  for(int i= 0; i<this.getNucleusCount();i++){
	      INuclearFunctions n = this.getNucleus(i);
	      
	      List<NuclearSignal> signals = channel == Nucleus.RED_CHANNEL 
	    		  						? n.getRedSignals()
	    		  						: n.getGreenSignals();

	      for( NuclearSignal s : signals ){ 
	    	  result.add(s);
		  } 
	 } // end nucleus iterations
	 return result;
  }
  
  // allow for refiltering of nuclei based on nuclear parameters after looking at the rest of the data
  public double getMedianNuclearArea(){
    double[] areas = this.getAreas();
    double median = Stats.quartile(areas, 50);
    return median;
  }

  public double getMedianNuclearPerimeter(){
    double[] p = this.getPerimeters();
    double median = Stats.quartile(p, 50);
    return median;
  }

  public double getMedianPathLength(){
    double[] p = this.getPathLengths();
    double median = Stats.quartile(p, 50);
    return median;
  }

  public double getMedianArrayLength(){
    double[] p = this.getArrayLengths();
    double median = Stats.quartile(p, 50);
    return median;
  }

  public double getMedianFeretLength(){
    double[] p = this.getFerets();
    double median = Stats.quartile(p, 50);
    return median;
  }

  public double getMaxProfileLength(){
    return Stats.max(this.getArrayLengths());
  }

  public Set<String> getTags(){

    INuclearFunctions n = this.nucleiCollection.get(0);
    Set<String> headings = n.getTags();
    return headings;
  }

  public List<INuclearFunctions> getNucleiWithSignals(int channel){
    List<INuclearFunctions> result = new ArrayList<INuclearFunctions>(0);

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

  /*
    --------------------
    Profile methods
    --------------------
  */

  public double[] getDifferencesToMedianFromPoint(String pointType){
    double[] d = new double[this.getNucleusCount()];
    Profile medianProfile = this.profileCollection.getProfile(pointType);
    for(int i=0;i<this.getNucleusCount();i++){
      INuclearFunctions n = this.getNucleus(i);
      try{
        d[i] = n.getAngleProfile().offset(n.getBorderIndex(pointType)).differenceToProfile(medianProfile);
      } catch(Exception e){
        IJ.log("    Unable to get difference to median profile: "+i+": "+pointType);
        IJ.append("    Unable to get difference to median profile: "+i+": "+pointType, this.debugFile.getAbsolutePath());
      }
    }
    return d;
  }

  public double compareProfilesToMedian(String pointType){
    double[] scores = this.getDifferencesToMedianFromPoint(pointType);
    double result = 0;
    for(double s : scores){
      result += s;
    }
    return result;
  }

  // get the plot from the collection corresponding to the given pointType of interest
//  public Plot getPlot(String pointType, String plotType){
//    return this.plotCollection.get(pointType).get(plotType);
//  }

  public int[] getPointIndexes(String pointType){
    int[] d = new int[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){
      INuclearFunctions n = this.getNucleus(i);
      d[i] = n.getBorderIndex(pointType);
    }
    return d;
  }

  public double[] getPointToPointDistances(String pointTypeA, String pointTypeB){
    double[] d = new double[this.getNucleusCount()];
    for(int i=0;i<this.getNucleusCount();i++){
      INuclearFunctions n = this.getNucleus(i);
      d[i] = n.getBorderTag(pointTypeA).getLengthTo(n.getBorderTag(pointTypeB));
    }
    return d;
  }

  /*
    Identify tail in median profile
    and offset nuclei profiles. For a 
    regular round nucleus, the tail is one
    of the points of longest diameter, and 
    lowest angle
  */

  public void findTailIndexInMedianCurve(){

    Profile medianProfile = this.profileCollection.getProfile("head");
    int headIndex = medianProfile.getIndexOfMin();
    ProfileFeature headFeature = new ProfileFeature();
    headFeature.add("head", headIndex);
    this.profileCollection.addFeature("tail", headFeature);
    
    ProfileFeature tailFeature = new ProfileFeature();
    tailFeature.add("head", headIndex);
    this.profileCollection.addFeature("head", tailFeature);
//    medianProfileFeatureIndexes.add("tail", "head", headIndex); 
//    medianProfileFeatureIndexes.add("head", "tail", headIndex);// set the tail-index in the head normalised profile
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
  public void refilterNuclei(INuclearCollection failedCollection){

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
    int feretlength = 0;

    IJ.append("Prefiltered:\r\n", this.getDebugFile().getAbsolutePath());
    this.exportFilterStats();

    for(int i=0;i<this.getNucleusCount();i++){
      INuclearFunctions n = this.getNucleus(i);
      
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
      }
    }

    for( INuclearFunctions f : failedCollection.getNuclei()){ // should be safer than the i-- above
      this.getNuclei().remove(f);
    }
      

    medianArea = this.getMedianNuclearArea();
    medianPerimeter = this.getMedianNuclearPerimeter();
    medianPathLength = this.getMedianPathLength();
    medianArrayLength = this.getMedianArrayLength();
    medianFeretLength = this.getMedianFeretLength();

    int afterSize = this.getNucleusCount();
    int removed = beforeSize - afterSize;

    IJ.append("Postfiltered:\r\n", this.getDebugFile().getAbsolutePath());
    this.exportFilterStats();
    IJ.log("    Removed due to size or length issues: "+removed+" nuclei");
    IJ.append("  Due to area outside bounds "+(int)minArea+"-"+(int)maxArea+": "+area+" nuclei\r\n", this.getDebugFile().getAbsolutePath());
    IJ.append("  Due to perimeter outside bounds "+(int)minPerim+"-"+(int)maxPerim+": "+perim+" nuclei\r\n", this.getDebugFile().getAbsolutePath());
    IJ.append("  Due to wibbliness >"+(int)maxPathLength+" : "+(int)pathlength+" nuclei\r\n", this.getDebugFile().getAbsolutePath());
    IJ.append("  Due to array length: "+arraylength+" nuclei\r\n", this.getDebugFile().getAbsolutePath());
    IJ.append("  Due to feret length: "+feretlength+" nuclei\r\n", this.getDebugFile().getAbsolutePath());
    IJ.log("    Remaining: "+this.getNucleusCount()+" nuclei");
    
  }

  /*
    -----------------
    Profile functions
    -----------------
  */

  protected void createProfileAggregateFromPoint(String pointType){

	  ProfileAggregate profileAggregate = new ProfileAggregate((int)this.getMedianArrayLength());
	  this.profileCollection.addAggregate(pointType, profileAggregate);

	  for(int i=0;i<this.getNucleusCount();i++){
		  INuclearFunctions n = this.getNucleus(i);
		  profileAggregate.addValues(n.getAngleProfile(pointType));
	  }

	  Profile medians = profileAggregate.getMedian();
	  Profile q25     = profileAggregate.getQuartile(25);
	  Profile q75     = profileAggregate.getQuartile(75);
	  this.profileCollection.addProfile(pointType, medians);
	  this.profileCollection.addProfile(pointType+"25", q25);
	  this.profileCollection.addProfile(pointType+"75", q75);

  }

  public void createProfileAggregates(){

    Set<String> headings = this.getTags();
    for( String pointType : headings ){
      createProfileAggregateFromPoint(pointType);   
    }
  }

  public INuclearFunctions getNucleusMostSimilarToMedian(String pointType){
    
    Profile medianProfile = this.profileCollection.getProfile(pointType); // the profile we compare the nucleus to
    INuclearFunctions n = (INuclearFunctions) this.getNucleus(0); // default to the first nucleus

    double difference = Stats.max(getDifferencesToMedianFromPoint(pointType));
    for(int i=0;i<this.getNucleusCount();i++){
      INuclearFunctions p = (INuclearFunctions)this.getNucleus(i);
      int index = n.getBorderIndex(pointType);
      double nDifference = p.getAngleProfile().offset(index).differenceToProfile(medianProfile);
      if(nDifference<difference){
        difference = nDifference;
        n = p;
      }
    }
    return n;
  }

  /*
    -----------------
    Perform nuclear organisation analyses
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
      this.doShellAnalysis();
    }
  }

  public void doShellAnalysis(){

    String redLogFile   = makeGlobalLogFile( "logShellsRed"  );
    String greenLogFile = makeGlobalLogFile( "logShellsGreen");
    
    ShellCounter   redCounter = new ShellCounter(5);
    ShellCounter greenCounter = new ShellCounter(5);

    // make the shells and measure the values
    for(int i= 0; i<this.getNucleusCount();i++){
      INuclearFunctions n = this.getNucleus(i);
      ShellAnalyser shellAnalyser = new ShellAnalyser(n);
      shellAnalyser.createShells();
      shellAnalyser.exportImage();

      List<List<NuclearSignal>> signals = new ArrayList<List<NuclearSignal>>(0);
      signals.add(n.getRedSignals());
      signals.add(n.getGreenSignals());

      int channel = 0;
      
      // put each signal in the correct counter
      for( List<NuclearSignal> signalGroup : signals ){

    	  if(signalGroup.size()>0){
    		  ShellCounter counter = channel == Nucleus.RED_CHANNEL ? redCounter : greenCounter;
    		  
    		  for(NuclearSignal s : signalGroup){
    			  try {
    				  double[] signalPerShell = shellAnalyser.findShell(s, channel);
    				  counter.addValues(signalPerShell);
    			  } catch (Exception e) {
    				  IJ.log("    Error in shell analysis: "+e.getMessage());;
    			  }
    		  } // end for signals
    	  } // end if signals
    	  channel++;
      } // end for signal group
    } // end nucleus iterations
    
    // get stats and export
      redCounter.export(new File(redLogFile  ));
    greenCounter.export(new File(greenLogFile));

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

    String redLogFile   = makeGlobalLogFile( "logSignalsRed"  );
    String greenLogFile = makeGlobalLogFile( "logSignalsGreen");

    StringBuilder redLog = new StringBuilder();
    StringBuilder greenLog = new StringBuilder();

    String header = "NUCLEUS_NUMBER\t"+
                    "SIGNAL_AREA\t"+
                    "SIGNAL_ANGLE\t"+
                    "SIGNAL_FERET\t"+
                    "SIGNAL_DISTANCE\t"+
                    "FRACTIONAL_DISTANCE\t"+
                    "SIGNAL_PERIMETER\t"+
                    "SIGNAL_RADIUS\t"+
                    "CLOSEST_BORDER_INDEX\t"+
                    "PATH\r\n";

    redLog.append( header );
    greenLog.append(header);

    for(int i= 0; i<this.getNucleusCount();i++){ // for each roi

      INuclearFunctions n = this.getNucleus(i);

      int nucleusNumber = n.getNucleusNumber();
      String path = n.getPath();

      List<List<NuclearSignal>> signals = new ArrayList<List<NuclearSignal>>(0);
      signals.add(n.getRedSignals());
      signals.add(n.getGreenSignals());

      int signalCount = 0;
      for( List<NuclearSignal> signalGroup : signals ){

        StringBuilder log = signalCount == Nucleus.RED_CHANNEL ? redLog : greenLog;
        
        if(signalGroup.size()>0){
          for(int j=0; j<signalGroup.size();j++){
             NuclearSignal s = signalGroup.get(j);
             log.append(nucleusNumber                  +"\t"+
                       s.getArea()                     +"\t"+
                       s.getAngle()                    +"\t"+
                       s.getFeret()                    +"\t"+
                       s.getDistanceFromCoM()          +"\t"+
                       s.getFractionalDistanceFromCoM()+"\t"+
                       s.getPerimeter()                +"\t"+
                       s.getRadius()                   +"\t"+
                       s.getClosestBorderPoint()       +"\t"+
                       path                            +"\r\n");
          } // end for
        } // end if
        signalCount++;
      } // end for
    } // end for

    IJ.append(redLog.toString(), redLogFile);
    IJ.append(greenLog.toString(), greenLogFile);
  }

  public void exportDistancesBetweenSingleSignals(){

    String logFile = makeGlobalLogFile("logSignalDistances");
    StringBuilder outLine = new StringBuilder();

    outLine.append( "DISTANCE_BETWEEN_SIGNALS\t"+
                    "RED_DISTANCE_TO_COM\t"+
                    "GREEN_DISTANCE_TO_COM\t"+
                    "NUCLEAR_FERET\t"+
                    "RED_FRACTION_OF_FERET\t"+
                    "GREEN_FRACTION_OF_FERET\t"+
                    "DISTANCE_BETWEEN_SIGNALS_FRACTION_OF_FERET\t"+
                    "NORMALISED_DISTANCE\t"+
                    "PATH\r\n");

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

        outLine.append(  distanceBetween+"\t"+
                    rDistanceToCoM+"\t"+
                    gDistanceToCoM+"\t"+
                    nFeret+"\t"+
                    rFractionOfFeret+"\t"+
                    gFractionOfFeret+"\t"+
                    distanceFractionOfFeret+"\t"+
                    normalisedPosition+"\t"+
                    n.getPath()+"\r\n");
      }
    }
    IJ.append(outLine.toString(), logFile);
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

  public void exportMediansOfProfile(Profile profile, String filename){

    String logFile = makeGlobalLogFile(filename);

    double[] profileArray = profile.asArray();

    String outLine = "X_POSITION\tANGLE_MEDIAN\r\n";
    
    for(int i =0;i<profileArray.length;i++){
      outLine +=  i+"\t"+profileArray[i]+"\r\n";
    }
    IJ.append(outLine, logFile); 
  }

  public void exportMediansAndQuartilesOfProfile(ProfileAggregate profileAggregate, String filename){

	  String logFile = makeGlobalLogFile(filename);

	  StringBuilder outLine = new StringBuilder("X_POSITION\tANGLE_MEDIAN\tQ25\tQ75\tQ10\tQ90\tNUMBER_OF_POINTS\r\n");

	  double[] xmedians       =  profileAggregate.getXPositions().asArray();
	  double[] ymedians       =  profileAggregate.getMedian().asArray();
	  double[] lowQuartiles   =  profileAggregate.getQuartile(25).asArray();
	  double[] uppQuartiles   =  profileAggregate.getQuartile(75).asArray();
	  double[] quartiles10    =  profileAggregate.getQuartile(10).asArray();
	  double[] quartiles90    =  profileAggregate.getQuartile(90).asArray();
	  double[] numberOfPoints =  profileAggregate.getNumberOfPoints().asArray();

	  for(int i =0;i<xmedians.length;i++){
		  outLine.append( xmedians[i]      +"\t"+
				  ymedians[i]      +"\t"+
				  lowQuartiles[i]  +"\t"+
				  uppQuartiles[i]  +"\t"+
				  quartiles10[i]   +"\t"+
				  quartiles90[i]   +"\t"+
				  numberOfPoints[i]+"\r\n");
	  }
	  IJ.append(outLine.toString(), logFile); 
  }

  /*
    To hold the nuclear stats (and any stats), we want a structure that can 
    hold: a column of data. Any arbitrary other numbers of columns of data.
  */
  public Map<String, List<String>> calculateNuclearStats(){

    Map<String, List<String>> stats = new LinkedHashMap<String, List<String>>();

    String[] sAreas        = Utils.getStringFromDouble(this.getAreas());
    String[] sPerims       = Utils.getStringFromDouble(this.getPerimeters());
    String[] sFerets       = Utils.getStringFromDouble(this.getFerets());
    String[] sPathlengths  = Utils.getStringFromDouble(this.getPathLengths());
    String[] sDistances    = Utils.getStringFromDouble(this.getMedianDistanceBetweenPoints());
    String[] sMinFerets    = Utils.getStringFromDouble(this.getMinFerets());
    String[] sPaths        = this.getNucleusPaths();

    stats.put("AREA",        Arrays.asList(  sAreas));
    stats.put("PERIMETER",   Arrays.asList(  sPerims));
    stats.put("FERET",       Arrays.asList(  sFerets));
    stats.put("PATH_LENGTH", Arrays.asList(  sPathlengths));
    stats.put("MEDIAN_DISTANCE_BETWEEN_POINTS", Arrays.asList(sDistances ) );
    stats.put("MIN_FERET",   Arrays.asList(  sMinFerets));
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

    IJ.append("    Area: "        +(int)medianArea       +"\r\n",  this.getDebugFile().getAbsolutePath());
    IJ.append("    Perimeter: "   +(int)medianPerimeter  +"\r\n",  this.getDebugFile().getAbsolutePath());
    IJ.append("    Path length: " +(int)medianPathLength +"\r\n",  this.getDebugFile().getAbsolutePath());
    IJ.append("    Array length: "+(int)medianArrayLength+"\r\n",  this.getDebugFile().getAbsolutePath());
    IJ.append("    Feret length: "+(int)medianFeretLength+"\r\n",  this.getDebugFile().getAbsolutePath());
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
//        int width  = ip.getWidth();
//        int height = ip.getHeight();
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

    Set<String> headings = this.getTags();
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
      
      ProfilePlot plotHash = new ProfilePlot();
//      HashMap<String, Plot> plotHash = new HashMap<String, Plot>();
      plotHash.add("raw" , rawPlot );
      plotHash.add("norm", normPlot);
      this.profileCollection.addPlots(pointType, plotHash);
      
//      this.plotCollection.put(pointType, plotHash);
    }
  }    

  /*
    Draw the charts of the profiles of the nuclei within this collecion.
  */
  public void drawProfilePlots(){

    this.preparePlots();

    Set<String> headings = this.profileCollection.getPlotKeys();

    for( String pointType : headings ){
      Plot  rawPlot = this.profileCollection.getPlots(pointType).get("raw");
      Plot normPlot = this.profileCollection.getPlots(pointType).get("norm");

      for(int i=0;i<this.getNucleusCount();i++){

        INuclearFunctions n = this.getNucleus(i);

        double[] xPointsRaw  = n.getRawProfilePositions();
        double[] xPointsNorm = n.getNormalisedProfilePositions();

        Profile anglesFromPoint = n.getAngleProfile(pointType);

        rawPlot.setColor(Color.LIGHT_GRAY);
        rawPlot.addPoints(xPointsRaw, anglesFromPoint.asArray(), Plot.LINE);

        normPlot.setColor(Color.LIGHT_GRAY);
        normPlot.addPoints(xPointsNorm, anglesFromPoint.asArray(), Plot.LINE);
      }
    }   
  }

  /*
    Draw a median profile on the normalised plots.
  */
  public void drawMedianLine(String pointType, Plot plot){

	  ProfileAggregate profileAggregate = profileCollection.getAggregate(pointType);

	  this.exportMediansAndQuartilesOfProfile(profileAggregate, "logMediansFrom"+pointType); // needs to be "logMediansFrom<pointname>"

	  double[] xmedians        =  profileAggregate.getXPositions().asArray();
	  double[] ymedians        =  profileAggregate.getMedian().asArray();
	  double[] lowQuartiles    =  profileAggregate.getQuartile(25).asArray();
	  double[] uppQuartiles    =  profileAggregate.getQuartile(75).asArray();

	  // add the median lines to the chart
	  plot.setColor(Color.BLACK);
	  plot.setLineWidth(3);
	  plot.addPoints(xmedians, ymedians, Plot.LINE);

	  plot.setColor(Color.DARK_GRAY);
	  plot.setLineWidth(2);
	  plot.addPoints(xmedians, lowQuartiles, Plot.LINE);
	  plot.addPoints(xmedians, uppQuartiles, Plot.LINE);
  }

  /*
    Draw a boxplot on the normalised plots. Specify which BorderPointOfInterest is to be plotted.
  */
  public void drawBoxplot(String profilePointType, Plot plot, String boxPointType){

    // get the tail positions with the head offset applied
    double[] xPoints = new double[this.getNucleusCount()];
    for(int i= 0; i<this.getNucleusCount();i++){

      INuclearFunctions n = this.getNucleus(i);

      // get the index of the NBP with the boxPointType
      int boxIndex = n.getBorderIndex(boxPointType);
      // get the index of the NBP with the profilePointType; the new zero
      int profileIndex = n.getBorderIndex(profilePointType);

      // find the offset position of boxPoint, using profilePoint as a zero. 
      int offsetIndex = Utils.wrapIndex( boxIndex - profileIndex , n.getLength() );

      // normalise to 100
      xPoints[i] =  (   (double) offsetIndex / (double) n.getLength()  ) * 100;
    }
    double[] yPoints = new double[xPoints.length];
    Arrays.fill(yPoints, CHART_TAIL_BOX_Y_MID); // all dots at y=CHART_TAIL_BOX_Y_MID
    plot.setColor(Color.LIGHT_GRAY);
    plot.addPoints(xPoints, yPoints, Plot.DOT);

    // median tail positions
    double tailQ50 = Stats.quartile(xPoints, 50);
    double tailQ25 = Stats.quartile(xPoints, 25);
    double tailQ75 = Stats.quartile(xPoints, 75);

    plot.setColor(Color.DARK_GRAY);
    plot.setLineWidth(1);
    plot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MAX, tailQ75, CHART_TAIL_BOX_Y_MAX);
    plot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MIN);
    plot.drawLine(tailQ25, CHART_TAIL_BOX_Y_MIN, tailQ25, CHART_TAIL_BOX_Y_MAX);
    plot.drawLine(tailQ75, CHART_TAIL_BOX_Y_MIN, tailQ75, CHART_TAIL_BOX_Y_MAX);
    plot.drawLine(tailQ50, CHART_TAIL_BOX_Y_MIN, tailQ50, CHART_TAIL_BOX_Y_MAX);
  }

  public void drawNormalisedMedianLines(){

    Set<String> headings = this.profileCollection.getPlotKeys();
    for( String pointType : headings ){

        Plot normPlot = this.profileCollection.getPlots(pointType).get("norm");
        drawMedianLine(pointType, normPlot);
        drawBoxplot(pointType, normPlot, "tail");
    }
  }

  public void addSignalsToProfileCharts(){

    Set<String> headings = this.profileCollection.getPlotKeys();

    for( String pointType : headings ){
      Plot normPlot = this.profileCollection.getPlots(pointType).get("norm");
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
      int profileIndex = n.getBorderIndex(pointType); 
      List<List<NuclearSignal>> signals = new ArrayList<List<NuclearSignal>>(0);
      signals.add(n.getRedSignals());
      signals.add(n.getGreenSignals());

      int signalCount = 0;
      for( List<NuclearSignal> signalGroup : signals ){
        
        if(signalGroup.size()>0){

          Color colour = signalCount == Nucleus.RED_CHANNEL ? Color.RED : Color.GREEN;

          double[] xPoints = new double[signalGroup.size()];
          double[] yPoints = new double[signalGroup.size()];

          for(int j=0; j<signalGroup.size();j++){

            // get the index of the point closest to the signal
            int borderIndex = signalGroup.get(j).getClosestBorderPoint();

            // offset the index relative to the current profile type, and normalise
            int offsetIndex = Utils.wrapIndex( borderIndex - profileIndex , n.getLength() );
            double normIndex = (  (double) offsetIndex / (double) n.getLength()  ) * 100;

            xPoints[j] = normIndex;
            double yPosition = CHART_SIGNAL_Y_LINE_MIN + ( signalGroup.get(j).getFractionalDistanceFromCoM() * ( CHART_SIGNAL_Y_LINE_MAX - CHART_SIGNAL_Y_LINE_MIN) ); // 
            yPoints[j] = yPosition;

            // IJ.log("Nucleus "+i+": Signal: "+j+": "+normIndex+"  "+yPosition);

          }

          plot.setColor(colour);
          plot.setLineWidth(2);
          plot.addPoints( xPoints, yPoints, Plot.DOT);
        }
        signalCount++;
      }
    }
  }

  public void exportProfilePlots(){

    Set<String> headings = this.profileCollection.getPlotKeys();
    for( String pointType : headings ){

      Plot normPlot = this.profileCollection.getPlots(pointType).get("norm");
      Plot  rawPlot = this.profileCollection.getPlots(pointType).get("raw" );

      exportProfilePlot(normPlot, "plot"+pointType+"Norm");
      exportProfilePlot(rawPlot , "plot"+pointType+"Raw");
    }  
  }
}