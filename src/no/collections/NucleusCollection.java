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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

import no.analysis.PopulationProfiler;
import no.collections.INuclearCollection;
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

	public static final int FAILURE_THRESHOLD = 1;
	public static final int FAILURE_FERET     = 2;
	public static final int FAILURE_ARRAY     = 4;
	public static final int FAILURE_AREA      = 8;
	public static final int FAILURE_PERIM     = 16;
	public static final int FAILURE_OTHER     = 32;
	public static final int FAILURE_SIGNALS   = 64;
	
	private final  String DEFAULT_REFERENCE_POINT = "head";

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
  
  public void exportProfiles(){
	  PopulationProfiler.createProfileAggregates(this);

	  // export the profiles
	  this.drawProfilePlots();
	  this.profileCollection.addMedianLinesToPlots();

	  this.profileCollection.exportProfilePlots(this.getFolder()+
			  File.separator+
			  this.getOutputFolder(), this.getType());
  }

  public void calculateOffsets(){

	  Profile medianToCompare = this.profileCollection.getProfile(DEFAULT_REFERENCE_POINT); // returns a median profile with head at 0

	  for(int i= 0; i<this.getNucleusCount();i++){ // for each roi
		  Nucleus n = (Nucleus)this.getNucleus(i);

		  // returns the positive offset index of this profile which best matches the median profile
		  int newHeadIndex = n.getAngleProfile().getSlidingWindowOffset(medianToCompare);
		  n.addBorderTag(DEFAULT_REFERENCE_POINT, newHeadIndex);

		  // check if flipping the profile will help

		  double differenceToMedian1 = n.getAngleProfile(DEFAULT_REFERENCE_POINT).differenceToProfile(medianToCompare);
		  n.reverse();
		  double differenceToMedian2 = n.getAngleProfile(DEFAULT_REFERENCE_POINT).differenceToProfile(medianToCompare);

		  if(differenceToMedian1<differenceToMedian2){
			  n.reverse(); // put it back if no better
		  }

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
  
  public String getReferencePoint(){
	  return this.DEFAULT_REFERENCE_POINT;
  }
  
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
   * Find the total number of signals within all nuclei of the collection.
   * @return the total
   */
  public int getSignalCount(){
	  int count = 0;
	  for(int i : this.getSignalChannels()){
		  count+= this.getSignalCount(i);
	  }
	  return count;
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
//      n.exportProfilePlotImage();
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

//	  this.profileCollection.preparePlots(CHART_WINDOW_WIDTH, CHART_WINDOW_HEIGHT, this.getMaxProfileLength());

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
}