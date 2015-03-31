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
import ij.gui.TextRoi;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

import no.collections.INuclearCollection;
import no.analysis.ProfileSegmenter;
import no.analysis.SegmentFitter;
import no.analysis.ShellAnalyser;
import no.analysis.ShellCounter;
import no.nuclei.*;
import no.components.*;
import no.export.Logger;
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

	//this holds the mapping of tail indexes etc in the median profile arrays
	protected ProfileCollection profileCollection = new ProfileCollection("regular");
	protected ProfileCollection frankensteinProfiles = new ProfileCollection("frankenstein");

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
	for(INuclearFunctions n : this.nucleiCollection){
		n.annotateNucleusImage();
	}
    this.exportAnnotatedNuclei();
    this.exportCompositeImage("composite");
  }

  public void measureProfilePositions(){
    this.measureProfilePositions("head");
  }

  public void measureProfilePositions(String pointType){

	  	// create an initial profile aggregate from the estimated points
	  this.createProfileAggregateFromPoint(pointType);

	  // use the median profile of this aggregate to find the tail point
	  this.findTailIndexInMedianCurve();

	  // carry out iterative offsetting to refine the tail point estimate
	  double score = this.compareProfilesToMedian(pointType);
	  double prevScore = score+1;
	  IJ.log("    Profile alignment score: "+(int)score);
//	  int cycles = 10; // see what happens when we force it
	  while(score < prevScore){
		  this.createProfileAggregateFromPoint(pointType);
		  this.findTailIndexInMedianCurve();
		  this.calculateOffsets();

		  prevScore = score;
		  score = this.compareProfilesToMedian(pointType);

		  IJ.log("    Reticulating splines: score: "+(int)score);
//		  cycles--;
	  }

	  // assign and revise segments
	  IJ.log("    Segmenting profile...");
	  this.assignSegments(pointType);

	  this.createProfileAggregates();

	  // export the profiles
	  this.drawProfilePlots();
	  this.profileCollection.addMedianLinesToPlots();

	  this.profileCollection.exportProfilePlots(this.getFolder()+
			  File.separator+
			  this.getOutputFolder(), this.getType());
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
  
  public void assignSegments(String pointType){
	  // get the segments within the median curve
	  try{
		  Profile medianToCompare = this.profileCollection.getProfile(pointType);

		  ProfileSegmenter segmenter = new ProfileSegmenter(medianToCompare);		  
		  List<NucleusBorderSegment> segments = segmenter.segment();
		  
		  String segmentedProfileName = this.getFolder()+
										File.separator+
										this.getOutputFolder()+
										File.separator+"plot.Segments"+
										"."+
										this.getType()+".tiff";
		  segmenter.draw(segmentedProfileName);
		  
		  IJ.log("    Found "+segments.size()+" segments in profile");
		  this.profileCollection.addSegments(pointType, segments);
		  IJ.log("    Assigning segments to nuclei...");
		  
		  // find the corresponding point in each Nucleus
		  for(int i= 0; i<this.getNucleusCount();i++){ // for each roi
			  Nucleus n = (Nucleus)this.getNucleus(i);
			  n.clearSegments();

			  int j=0;
			  for(NucleusBorderSegment b : segments){
				  int startIndexInMedian = b.getStartIndex();
				  int endIndexInMedian = b.getEndIndex();
				  
				  Profile startOffsetMedian = medianToCompare.offset(startIndexInMedian);
				  Profile endOffsetMedian = medianToCompare.offset(endIndexInMedian);

				  int startIndex = n.getAngleProfile().getSlidingWindowOffset(startOffsetMedian);
				  int endIndex = n.getAngleProfile().getSlidingWindowOffset(endOffsetMedian);

				  NucleusBorderSegment seg = new NucleusBorderSegment(startIndex, endIndex);
				  seg.setSegmentType("Seg_"+j);
				  n.addSegment(seg);
				  n.addSegmentTag("Seg_"+j, j);
				  j++;
			  }
		  }
		  
		  this.reviseSegments(pointType, segments);

	  } catch(Exception e){
		  IJ.log("    Error segmenting: "+e.getMessage());
		  this.profileCollection.printKeys();
	  }
  }
  
  public void reviseSegments(String pointType, List<NucleusBorderSegment> segments){
	  IJ.log("    Refining segment assignments...");
	  
	 
	  this.frankensteinProfiles.addAggregate( pointType, new ProfileAggregate((int)this.getMedianArrayLength()));//pointType, recombinedProfile);
	  this.frankensteinProfiles.preparePlots(CHART_WINDOW_WIDTH, CHART_WINDOW_HEIGHT, getMaxProfileLength());
	  SegmentFitter fitter = new SegmentFitter(this.profileCollection.getProfile(pointType), segments);
	  List<Profile> frankenProfiles = new ArrayList<Profile>(0);
	  
	  for(int i= 0; i<this.getNucleusCount();i++){ // for each roi
		  INuclearFunctions n = this.getNucleus(i);
		  fitter.fit(n);
		  
		  // recombine the segments at the lengths of the median profile segments
		  // what does it look like?
		  Profile recombinedProfile = fitter.recombine(n);
		  this.frankensteinProfiles.getAggregate(pointType).addValues(recombinedProfile);
		  frankenProfiles.add(recombinedProfile);
	  }
	  this.frankensteinProfiles.createProfileAggregateFromPoint(    pointType, (int) this.getMedianArrayLength()    );
	  this.frankensteinProfiles.drawProfilePlots(pointType, frankenProfiles);
	  this.frankensteinProfiles.addMedianLinesToPlots();
	  this.frankensteinProfiles.exportProfilePlots(this.getFolder()+
    	                      			        	File.separator+
    					                            this.getOutputFolder(), this.getType());
	  
	  this.exportSegments(pointType);
  }
  
  public void exportSegments(String pointType){
	  // export the individual segment files for each nucleus
	  for(INuclearFunctions n : this.getNuclei()){
		  n.exportSegments();
	  }

	  // also export the group stats for each segment
	  Logger logger = new Logger(this.getFolder()+File.separator+this.getOutputFolder());
	  
	  IJ.log("    Exporting segments...");
	  try{
		  List<NucleusBorderSegment> segments = this.profileCollection.getSegments(pointType);
		  if(!segments.isEmpty()){
	
			  for(NucleusBorderSegment seg : segments){
				  logger.addColumnHeading(seg.getSegmentType());
//				  IJ.log("    Heading made: "+seg.getSegmentType());
			  }
			  
			  for(INuclearFunctions n : this.getNuclei()){
	
				  for(NucleusBorderSegment seg : segments){
					  NucleusBorderSegment nucSeg = n.getSegmentTag(seg.getSegmentType());
					  logger.addRow(seg.getSegmentType(), nucSeg.length(n.getLength()));
				  }
			  }
//			  IJ.log("    Values added");
			  logger.export("log.segments."+getType());
			  IJ.log("    Segments exported");
		  }
	  }catch(Exception e){
		  IJ.log("    Error exporting segments: "+e.getMessage());
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
      count += nucleiCollection.get(i).getSignalCount(1);
    }
    return count;
  }

  public int getGreenSignalCount(){
    int count = 0;
    for(int i=0;i<nucleiCollection.size();i++){
      count += nucleiCollection.get(i).getSignalCount(2);
    }
    return count;
  }
  
  /**
   * Find the signal channels present within the nuclei of the collection
   * @return the list of channels. Order is not guaranteed
   */
  public List<Integer> getSignalChannels(){
	  List<Integer> result = new ArrayList<Integer>(0);
	  for(int i= 0; i<this.getNucleusCount();i++){
		  INuclearFunctions n = this.getNucleus(i);
		  for( int channel : n.getSignalCollection().getChannels()){
			  if(!result.contains(channel)){
				  result.add(channel);
			  }
		  }
	  } // end nucleus iterations
	  return result;
  }
  
  /**
   * Get the number of signals in the given channel
   * @param channel the channel to search
   * @return the count
   */
  public int getSignalCount(int channel){
	  int count = 0;
	  for(int i= 0; i<this.getNucleusCount();i++){
		  INuclearFunctions n = this.getNucleus(i);
		  count += n.getSignalCount(channel);

	  } // end nucleus iterations
	  return count;
  }

  /**
   * Get all the signals from all nuclei in the given channel
   * @param channel the channel to search
   * @return a list of signals
   */
  public List<NuclearSignal> getSignals(int channel){

	  List<NuclearSignal> result = new ArrayList<NuclearSignal>(0);

	  for(int i= 0; i<this.getNucleusCount();i++){
		  INuclearFunctions n = this.getNucleus(i);
		  result.addAll(n.getSignals(channel));

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
	  try{

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
	  } catch(Exception e){
		  IJ.log("    Error getting differences from point "+pointType+": "+e.getMessage());
		  this.profileCollection.printKeys();
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
    
	int tailIndex = (int) Math.floor(medianProfile.size()/2);
	  
	  Profile tailProfile = medianProfile.offset(tailIndex);
	  this.profileCollection.addProfile("tail", tailProfile);
	  this.profileCollection.addFeature("head", new ProfileFeature("tail", tailIndex));
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
	  try{
		  for( String pointType : this.profileCollection.getProfileKeys() ){
//			  for(int i=0;i<this.getNucleusCount();i++){
//				  INuclearFunctions n = this.getNucleus(i);
//				  ProfileAggregate profileAggregate = this.profileCollection.getAggregate(pointType);
//				  profileAggregate.addValues(n.getAngleProfile(pointType));
//			  }
			  this.createProfileAggregateFromPoint(pointType);   
		  }
	  } catch(Exception e){
		  IJ.log("Error creating profile aggregates: "+e.getMessage());
		  this.profileCollection.printKeys();
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
      this.profileCollection.exportProfilePlots(this.getFolder()+
			  File.separator+
			  this.getOutputFolder(), this.getType());
    }
  }

  public void doShellAnalysis(){
	  IJ.log("    Performing shell analysis...");
	  String redLogFile   = getLogFileName( "logShellsRed"  );
	  String greenLogFile = getLogFileName( "logShellsGreen");

	  ShellCounter   redCounter = new ShellCounter(5);
	  ShellCounter greenCounter = new ShellCounter(5);

	  // make the shells and measure the values
	  for(int i= 0; i<this.getNucleusCount();i++){
		  INuclearFunctions n = this.getNucleus(i);
		  ShellAnalyser shellAnalyser = new ShellAnalyser(n);
		  shellAnalyser.createShells();
		  shellAnalyser.exportImage();

		  List<List<NuclearSignal>> signals = new ArrayList<List<NuclearSignal>>(0);
		  signals.add(n.getSignals(1));
		  signals.add(n.getSignals(2));

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
	  IJ.log("    Shell analysis complete");
  }
  

  /*
    -----------------
    Export functions
    -----------------
  */

  public String getLogFileName(String filename){
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
	  
	  for(int i : this.getSignalChannels()){
		  Logger logger = new Logger(this.getFolder()+File.separator+this.getOutputFolder());
		  logger.addColumnHeading("SIGNAL_AREA");
		  logger.addColumnHeading("SIGNAL_ANGLE");
		  logger.addColumnHeading("SIGNAL_FERET");
		  logger.addColumnHeading("SIGNAL_DISTANCE");
		  logger.addColumnHeading("FRACT_DISTANCE");
		  logger.addColumnHeading("SIGNAL_PERIM.");
		  logger.addColumnHeading("SIGNAL_RADIUS");
		  logger.addColumnHeading("CLOSEST_BORDER_INDEX");
		  logger.addColumnHeading("SOURCE");
		  
		  for(NuclearSignal s : this.getSignals(i)){
			  logger.addRow("SIGNAL_AREA"         , s.getArea());
			  logger.addRow("SIGNAL_ANGLE"        , s.getAngle());
			  logger.addRow("SIGNAL_FERET"        , s.getFeret());
			  logger.addRow("SIGNAL_DISTANCE"     , s.getDistanceFromCoM());
			  logger.addRow("FRACT_DISTANCE"     , s.getFractionalDistanceFromCoM());
			  logger.addRow("SIGNAL_PERIM."       , s.getPerimeter());
			  logger.addRow("SIGNAL_RADIUS"       , s.getRadius());
			  logger.addRow("CLOSEST_BORDER_INDEX", s.getClosestBorderPoint());
			  logger.addRow("SOURCE"              , s.getOrigin());
		  }
		  logger.export("log.signals."+i+"."+getType()); // TODO: get channel names
	  }
  }

  public void exportDistancesBetweenSingleSignals(){
	  
	Logger logger = new Logger(this.getFolder()+File.separator+this.getOutputFolder());
	logger.addColumnHeading("DISTANCE_BETWEEN_SIGNALS");
	logger.addColumnHeading("RED_DISTANCE_TO_COM");
	logger.addColumnHeading("GREEN_DISTANCE_TO_COM");
	logger.addColumnHeading("NUCLEAR_FERET");
	logger.addColumnHeading("RED_FRACTION_OF_FERET");
	logger.addColumnHeading("GREEN_FRACTION_OF_FERET");
	logger.addColumnHeading("DIST_BETWEEN_SIGNALS_FRACT_FERET");
	logger.addColumnHeading("NORMALISED_DISTANCE");
	logger.addColumnHeading("PATH");

    for(int i=0; i<this.getNucleusCount();i++){

      INuclearFunctions n = this.nucleiCollection.get(i);
      if(n.getSignalCount(1)==1 && n.getSignalCount(2)==1){

        NuclearSignal r = n.getSignals(1).get(0);
        NuclearSignal g = n.getSignals(2).get(0);

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
        
        logger.addRow("DISTANCE_BETWEEN_SIGNALS"		, distanceBetween);
    	logger.addRow("RED_DISTANCE_TO_COM"				, rDistanceToCoM);
    	logger.addRow("GREEN_DISTANCE_TO_COM"			, gDistanceToCoM);
    	logger.addRow("NUCLEAR_FERET"					, nFeret);
    	logger.addRow("RED_FRACTION_OF_FERET"			, rFractionOfFeret);
    	logger.addRow("GREEN_FRACTION_OF_FERET"			, gFractionOfFeret);
    	logger.addRow("DIST_BETWEEN_SIGNALS_FRACT_FERET", distanceFractionOfFeret);
    	logger.addRow("NORMALISED_DISTANCE"    			, normalisedPosition);
    	logger.addRow("PATH"                			, n.getPath());
      }
    }
    logger.export("logSingleSignalDistances");
//    IJ.append(outLine.toString(), logFile);
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
      n.exportProfilePlotImage();
    }
  }

  public void exportMediansOfProfile(Profile profile, String filename){

    Logger logger = new Logger(this.getFolder()+File.separator+this.getOutputFolder());
    logger.addColumn("X_POSITION",   profile.getPositions(profile.size()).asArray());
    logger.addColumn("ANGLE_MEDIAN", profile.asArray());
    logger.export(filename+"."+getType());
  }

  public void exportMediansAndQuartilesOfProfile(ProfileAggregate profileAggregate, String filename){

	  Logger logger = new Logger(this.getFolder()+File.separator+this.getOutputFolder());
	  logger.addColumn("X_POSITION",       profileAggregate.getXPositions().asArray());
	  logger.addColumn("ANGLE_MEDIAN",     profileAggregate.getMedian().asArray());
	  logger.addColumn("Q25", 	 		   profileAggregate.getQuartile(25).asArray());
	  logger.addColumn("Q75", 	 		   profileAggregate.getQuartile(75).asArray());
	  logger.addColumn("Q10", 	 		   profileAggregate.getQuartile(10).asArray());
	  logger.addColumn("Q90", 	 		   profileAggregate.getQuartile(90).asArray());
	  logger.addColumn("NUMBER_OF_POINTS", profileAggregate.getNumberOfPoints().asArray());
	  logger.export(filename);
  }

  // this is for the mapping of image to path for 
  // identifying FISHed nuclei in prefish images
  public void exportImagePaths(String filename){
	Logger logger = new Logger(this.getFolder()+File.separator+this.getOutputFolder());
	logger.addColumn("PATH",     this.getCleanNucleusPaths());
	logger.addColumn("POSITION", this.getPositions());
	logger.export(filename);
  }

  public void exportNuclearStats(String filename){
	  
	Logger nuclearStats = new Logger(this.getFolder()+File.separator+this.getOutputFolder());
	nuclearStats.addColumn("AREA",                       this.getAreas());
	nuclearStats.addColumn("PERIMETER",                  this.getPerimeters());
	nuclearStats.addColumn("FERET",                      this.getFerets());
	nuclearStats.addColumn("PATH_LENGTH",                this.getPathLengths());
	nuclearStats.addColumn("MEDIAN_DIST_BETWEEN_POINTS", this.getMedianDistanceBetweenPoints());
	nuclearStats.addColumn("MIN_FERET",                  this.getMinFerets());
	nuclearStats.addColumn("NORM_TAIL_INDEX",            this.getPointIndexes("tail"));
	nuclearStats.addColumn("DIFFERENCE_TO_MEDIAN",       this.getDifferencesToMedianFromPoint("tail"));
	nuclearStats.addColumn("PATH",                       this.getNucleusPaths());
	nuclearStats.export(filename+"."+getType());
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

  /*
    Draw the charts of the profiles of the nuclei within this collecion.
   */
  public void drawProfilePlots(){

	  this.profileCollection.preparePlots(CHART_WINDOW_WIDTH, CHART_WINDOW_HEIGHT, this.getMaxProfileLength());

	  for( String pointType : this.profileCollection.getPlotKeys() ){

		  List<Profile> profiles = new ArrayList<Profile>(0);

		  for(int i=0;i<this.getNucleusCount();i++){

			  INuclearFunctions n = this.getNucleus(i);

			  profiles.add(n.getAngleProfile(pointType));
		  }
		  this.profileCollection.drawProfilePlots(pointType, profiles);
	  }   
  }
  
  /** 
   * For each of the point types in the profile collection, add boxplot showing
   * the tail position
   */
  public void drawBoxplots(){
	  for( String pointType : this.profileCollection.getPlotKeys() ){
		  drawBoxplotFromPoint(pointType, "tail");
	  }
  }

  /*
    Draw a boxplot on the normalised plots. Specify which BorderPointOfInterest is to be plotted.
   */
  private void drawBoxplotFromPoint(String profilePointType, String boxPointType){

	  // get the tail positions with the head offset applied
	  List<Double> pointIndexes = new ArrayList<Double>(0);
	  //	  double[] xPoints = new double[this.getNucleusCount()];
	  for(int i= 0; i<this.getNucleusCount();i++){

		  INuclearFunctions n = this.getNucleus(i);

		  // get the index of the NBP with the boxPointType
		  int boxIndex = n.getBorderIndex(boxPointType);
		  // get the index of the NBP with the profilePointType; the new zero
		  int profileIndex = n.getBorderIndex(profilePointType);

		  // find the offset position of boxPoint, using profilePoint as a zero. 
		  int offsetIndex = Utils.wrapIndex( boxIndex - profileIndex , n.getLength() );

		  // normalise to 100
		  //		  xPoints[i] =  (   (double) offsetIndex / (double) n.getLength()  ) * 100;
		  pointIndexes.add((   (double) offsetIndex / (double) n.getLength()  ) * 100);
	  }
	  this.profileCollection.addBoxplot(profilePointType, pointIndexes);
  }

  /** 
   * For each of the point types in the profile collection, add nuclear signals
   */
  public void addSignalsToProfileCharts(){

	  Set<String> headings = this.profileCollection.getPlotKeys();

	  for( String pointType : headings ){
		  //      Plot normPlot = this.profileCollection.getPlots(pointType).get("norm");
		  this.addSignalsToProfileChartFromPoint(pointType);

	  }    
  }

  private void addSignalsToProfileChartFromPoint(String pointType){
	  // for each signal in each nucleus, find index of point. Draw dot

	  List<List<XYPoint>> points = new ArrayList<List<XYPoint>>(0);
	  points.add( new ArrayList<XYPoint>(0)); // red signals
	  points.add( new ArrayList<XYPoint>(0)); // green signals

	  for(int i= 0; i<this.getNucleusCount();i++){

		  INuclearFunctions n = this.getNucleus(i);
		  int profileIndex = n.getBorderIndex(pointType); 
		  List<List<NuclearSignal>> signals = new ArrayList<List<NuclearSignal>>(0);
		  signals.add(n.getSignals(1));
		  signals.add(n.getSignals(2));

		  int channel = 0;
		  for( List<NuclearSignal> channelSignals : signals ){

			  if(!channelSignals.isEmpty()){

				  List<XYPoint> channelPoints = points.get(channel);

				  for(int j=0; j<channelSignals.size();j++){

					  // get the index of the point closest to the signal
					  int borderIndex = channelSignals.get(j).getClosestBorderPoint();

					  // offset the index relative to the current profile type, and normalise
					  int offsetIndex = Utils.wrapIndex( borderIndex - profileIndex , n.getLength() );
					  double normIndex = (  (double) offsetIndex / (double) n.getLength()  ) * 100;

					  double yPosition = CHART_SIGNAL_Y_LINE_MIN + ( channelSignals.get(j).getFractionalDistanceFromCoM() * ( CHART_SIGNAL_Y_LINE_MAX - CHART_SIGNAL_Y_LINE_MIN) ); // 

					  // make a point, and add to the appropriate list
					  channelPoints.add(new XYPoint(normIndex, yPosition));
				  }
			  }
			  channel++;
		  }
	  }

	  // all points are assigned to lists
	  // draw the lists
	  int channel = 0;
	  for( List<XYPoint> channelPoints : points ){
		  Color colour = channel == Nucleus.RED_CHANNEL ? Color.RED : Color.GREEN;
		  this.profileCollection.addSignalsToProfileChart(pointType, channelPoints, colour);
		  channel++;
	  }
  }
}