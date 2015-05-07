/* 
  -----------------------
  NUCLEUS COLLECTION CLASS
  -----------------------
  This class contains the nuclei that pass detection criteria
  Provides aggregate stats
  It enables offsets to be calculated based on the median normalised curves
*/

package no.collections;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import no.collections.NucleusCollection;
import no.components.AnalysisOptions;
import no.components.NuclearSignal;
import no.components.Profile;
import no.components.ProfileCollection;
import no.components.ProfileFeature;
import no.components.ShellResult;
import no.nuclei.Nucleus;
import no.utility.Stats;

public class RoundNucleusCollection
implements NucleusCollection, Serializable 
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private File folder; // the source of the nuclei
	private String outputFolder;
	private File debugFile;
	private String collectionType; // for annotating image names
	private String name;
	private UUID guid;
		
	private String DEFAULT_REFERENCE_POINT = "head";
	private String DEFAULT_ORIENTAITION_POINT = "tail";

	//this holds the mapping of tail indexes etc in the median profile arrays
	protected ProfileCollection profileCollection = new ProfileCollection("regular");
	protected ProfileCollection frankenCollection = new ProfileCollection("franken");
	
	private Nucleus consensusNucleus;
	
	private AnalysisOptions analysisOptions;
	private Map<Integer, ShellResult> shellResults = new HashMap<Integer, ShellResult>(0); // store shell analysis for each channel

	private List<Nucleus> nucleiCollection = new ArrayList<Nucleus>(0); // store all the nuclei analysed
	private Map<UUID, Nucleus> mappedCollection  = new HashMap<UUID, Nucleus>();

  public RoundNucleusCollection(File folder, String outputFolder, String type, File debugFile){
	  this.folder = folder;
	  this.outputFolder = outputFolder;
	  this.debugFile = debugFile;
	  this.collectionType = type;
	  this.name = outputFolder+" - "+type;
	  this.guid = java.util.UUID.randomUUID();
  }

  // used only for getting classes in setup of analysis
  public RoundNucleusCollection(){

  }

  /*
    -----------------------
    Define adders for all
    types of nucleus eligable
    -----------------------
  */
  
  public void setName(String s){
	  this.name = s;
  }
  
  public String getName(){
	  return this.name;
  }
  
  public UUID getID(){
	  return this.guid;
  }

  public void addNucleus(Nucleus r){
	  this.nucleiCollection.add(r);
	  this.mappedCollection.put(r.getID(), r);
  }
  
  public void addShellResult(int channel, ShellResult result){
	  this.shellResults.put(channel, result);
  }
  
  public ShellResult getShellResult(int channel){
	  return this.shellResults.get(channel);
  }

  /**
   * Test if the collection has a ShellResult in any channel
   * 
   */
  public boolean hasShellResult(){
	  for(Integer channel : this.getSignalChannels()){
		  if(this.shellResults.containsKey(channel)){
			  return true;
		  }
	  }
	  return false;
  }
  
  public boolean hasConsensusNucleus(){
	  if(this.consensusNucleus==null){
		  return false;
	  } else {
		  return true;
	  }
  }


  public AnalysisOptions getAnalysisOptions() {
	  return analysisOptions;
  }

  public void setAnalysisOptions(AnalysisOptions analysisOptions) {
	  this.analysisOptions = analysisOptions;
  }

public void addConsensusNucleus(Nucleus n){
	  this.consensusNucleus = n;
  }
  
  public void calculateOffsets(){

	  Profile medianToCompare = this.profileCollection.getProfile(DEFAULT_REFERENCE_POINT); // returns a median profile with head at 0

	  for(int i= 0; i<this.getNucleusCount();i++){ // for each roi
		  Nucleus n = this.getNucleus(i);

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
    Getters
    -----------------------
  */
  public Nucleus getNucleus(UUID id){
	  return this.mappedCollection.get(id);
  }
  
  public Nucleus getConsensusNucleus(){
	  return this.consensusNucleus;
  }
  
  public String getReferencePoint(){
	  return this.DEFAULT_REFERENCE_POINT;
  }
  
  public String getOrientationPoint(){
	  return this.DEFAULT_ORIENTAITION_POINT;
  }
  
  public ProfileCollection getProfileCollection(){
	  return this.profileCollection;
  }
  
  public ProfileCollection getFrankenCollection(){
	  return this.frankenCollection;
  }

  public File getFolder(){
    return this.folder;
  }

  public String getOutputFolderName(){
    return this.outputFolder;
  }
  
  public File getOutputFolder(){
	    return new File(this.getFolder()+File.separator+this.getOutputFolderName());
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
      Nucleus n = nucleiCollection.get(i);
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

  public List<Nucleus> getNuclei(){
    return this.nucleiCollection;
  }

  public Nucleus getNucleus(int i){
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
		  Nucleus n = this.getNucleus(i);
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
		  Nucleus n = this.getNucleus(i);
		  count += n.getSignalCount(channel);

	  } // end nucleus iterations
	  return count;
  }

  /**
   * Test whether the current population has signals in any channel
   * @return
   */
  public boolean hasSignals(){
	  for(int i : this.getSignalChannels()){
		  if(this.hasSignals(i)){
			  return true;
		  }
	  }
	  return false;
  }

  /**
   * Test whether the current population has signals in the given channel
   * @return
   */
  public boolean hasSignals(int channel){
	  if(this.getSignalCount(channel)>0){
		  return true;
	  } else{
		  return false;
	  }

  }

  /**
   * Get all the signals from all nuclei in the given channel
   * @param channel the channel to search
   * @return a list of signals
   */
  public List<NuclearSignal> getSignals(int channel){

	  List<NuclearSignal> result = new ArrayList<NuclearSignal>(0);

	  for(int i= 0; i<this.getNucleusCount();i++){
		  Nucleus n = this.getNucleus(i);
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
  
  public int getProfileWindowSize(){
	  return this.getNucleus(0).getAngleProfileWindowSize();
  }
  
  /**
   * Get the median area of the signals in the given channel
   * @param channel
   * @return the median area
   */
  public double getMedianSignalArea(int channel){
	  List<Nucleus> nuclei = getNucleiWithSignals(channel);
	  List<Double> a = new ArrayList<Double>(0);
	  for(Nucleus n : nuclei){
		  a.addAll(n.getSignalCollection().getAreas(channel));

	  }
	  return Stats.quartile(a.toArray(new Double[0]), 50);
  }
  
  /**
   * Get the median angle of the signals in the given channel
   * @param channel
   * @return the median angle
   */
  public double getMedianSignalAngle(int channel){
	  List<Nucleus> nuclei = getNucleiWithSignals(channel);
	  List<Double> a = new ArrayList<Double>(0);
	  for(Nucleus n : nuclei){
		  a.addAll(n.getSignalCollection().getAngles(channel));

	  }
	  return Stats.quartile(a.toArray(new Double[0]), 50);
  }
  
  /**
   * Get the median feret of the signals in the given channel
   * @param channel
   * @return the median feret
   */
  public double getMedianSignalFeret(int channel){
	  List<Nucleus> nuclei = getNucleiWithSignals(channel);
	  List<Double> a = new ArrayList<Double>(0);
	  for(Nucleus n : nuclei){
		  a.addAll(n.getSignalCollection().getFerets(channel));

	  }
	  return Stats.quartile(a.toArray(new Double[0]), 50);
  }
  
  /**
   * Get the median fractional distance from the nucleus CoM of the signals in the given channel
   * @param channel
   * @return the median distance
   */
  public double getMedianSignalDistance(int channel){
	  List<Nucleus> nuclei = getNucleiWithSignals(channel);
	  List<Double> a = new ArrayList<Double>(0);
	  for(Nucleus n : nuclei){
		  a.addAll(n.getSignalCollection().getDistances(channel));

	  }
	  return Stats.quartile(a.toArray(new Double[0]), 50);
  }

  /** 
   * Return the nuclei that have signals in the given channel. If negative, this will give the nuclei
   * that do NOT have signals in the given channel.
   * @param channel the channel 
   * @return a list of nuclei
   */
  public List<Nucleus> getNucleiWithSignals(int channel){
	  List<Nucleus> result = new ArrayList<Nucleus>(0);

	  for(Nucleus n : this.nucleiCollection){

		  if(channel>0){
			  if(n.hasSignal(channel)){
				  result.add(n);
			  }
		  }
		  if(channel<0){
			  if(!n.hasSignal(Math.abs(channel))){
				  result.add(n);
			  }
		  }
	  }
	  return result;
  }

  public void setFrankenCollection (ProfileCollection frankenCollection){
	  this.frankenCollection = frankenCollection;
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
			  Nucleus n = this.getNucleus(i);
			  try{
				  d[i] = n.getAngleProfile().offset(n.getBorderIndex(pointType)).differenceToProfile(medianProfile);
			  } catch(Exception e){
//				  logger.log("Unable to get difference to median profile: "+i+": "+pointType, Logger.ERROR);
			  }
		  }
	  } catch(Exception e){
//		  logger.log("Error getting differences from point "+pointType+": "+e.getMessage(), Logger.ERROR);
		  this.profileCollection.printKeys(this.debugFile);
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
      Nucleus n = this.getNucleus(i);
      d[i] = n.getBorderIndex(pointType);
    }
    return d;
  }

  public double[] getPointToPointDistances(String pointTypeA, String pointTypeB){
    double[] d = new double[this.getNucleusCount()];
    for(int i=0;i<this.getNucleusCount();i++){
      Nucleus n = this.getNucleus(i);
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
    Profile functions
    -----------------
  */

  public Nucleus getNucleusMostSimilarToMedian(String pointType){
    
    Profile medianProfile = this.profileCollection.getProfile(pointType); // the profile we compare the nucleus to
    Nucleus n = (Nucleus) this.getNucleus(0); // default to the first nucleus

    double difference = Stats.max(getDifferencesToMedianFromPoint(pointType));
    for(Nucleus p : this.getNuclei()){
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
    String file = this.getFolder()+File.separator+this.getOutputFolderName()+File.separator+filename+"."+getType()+".txt";
    File f = new File(file);
    if(f.exists()){
      f.delete();
    }
    return file;
  }
}