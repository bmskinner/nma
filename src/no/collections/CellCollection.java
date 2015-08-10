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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cell.Cell;
import utility.Constants;
import utility.Stats;
import no.collections.CellCollection;
import no.components.NuclearSignal;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.ProfileCollection;
import no.nuclei.Nucleus;
import no.nuclei.sperm.PigSpermNucleus;
import no.nuclei.sperm.RodentSpermNucleus;

public class CellCollection
implements Serializable 
{

	public static final String REGULAR_PROFILE = "regular";
	public static final String FRANKEN_PROFILE = "franken";
	
	private static final long serialVersionUID = 1L;
	private File folder; // the source of the nuclei
	private String outputFolder;
	private File debugFile;
	private String collectionType; // for annotating image names
	private String name;
	private UUID guid;
	
	private Class<?> nucleusClass;
		
	//this holds the mapping of tail indexes etc in the median profile arrays
	protected Map<String, ProfileCollection> profileCollections = new HashMap<String, ProfileCollection>();
		
	private Nucleus consensusNucleus;
	
	private List<Cell> cellCollection = new ArrayList<Cell>(0); // store all the nuclei analysed
	private Map<UUID, Cell> mappedCollection  = new HashMap<UUID, Cell>();

  public CellCollection(File folder, String outputFolder, String type, File debugFile, Class<?> nucleusClass){
	  this.folder = folder;
	  this.outputFolder = outputFolder;
	  this.debugFile = debugFile;
	  this.collectionType = type;
	  this.name = outputFolder+" - "+type;
	  this.guid = java.util.UUID.randomUUID();
	  this.nucleusClass = nucleusClass;
	  profileCollections.put(CellCollection.REGULAR_PROFILE, new ProfileCollection());
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
  
  public List<UUID> getCellIds(){
	  List<UUID> result = new ArrayList<UUID>(0);
	  for(Cell c : cellCollection){
		  result.add(c.getCellId());
	  }
	  return  result;
  }
  
  public List<String> getCellIdsAsStrings(){
	  List<String> result = new ArrayList<String>(0);
	  for(Cell c : cellCollection){
		  result.add(c.getCellId().toString());
	  }
	  return  result;
  }

  public void addCell(Cell r){
	  this.cellCollection.add(r);
	  this.mappedCollection.put(r.getCellId(), r);
  }
  
  public void removeCell(Cell c){
	  this.mappedCollection.remove(c);
	  this.cellCollection.remove(c);
  }
    
  public boolean hasConsensusNucleus(){
	  if(this.consensusNucleus==null){
		  return false;
	  } else {
		  return true;
	  }
  }


  public void addConsensusNucleus(Nucleus n){
	  this.consensusNucleus = n;
  }
    
  /*
    -----------------------
    Getters
    -----------------------
  */
  public Cell getCell(UUID id){
	  return this.mappedCollection.get(id);
  }
  
  public Class<?> getNucleusClass(){
	  return this.nucleusClass;
  }
  
  public Cell getCell(String path){
	  for(Cell c : this.getCells()){
		  Nucleus n = c.getNucleus();
		  if(n.getPathAndNumber().equals(path)){
			  return c;
		  }
	  }
	  return null;
  }
  
  public Nucleus getConsensusNucleus(){
	  return this.consensusNucleus;
  }
  
  public String getReferencePoint(){
	  	  
	  if(this.nucleusClass == PigSpermNucleus.class){
		  return Constants.PIG_SPERM_NUCLEUS_REFERENCE_POINT;
	  }
	  
	  if(this.nucleusClass == RodentSpermNucleus.class){
		  return Constants.RODENT_SPERM_NUCLEUS_REFERENCE_POINT;
	  }
	  
	  // default if not defined above
	  return Constants.ROUND_NUCLEUS_REFERENCE_POINT;

  }
  
  public String getOrientationPoint(){
	  if(this.nucleusClass == PigSpermNucleus.class){
		  return Constants.PIG_SPERM_NUCLEUS_ORIENTATION_POINT;
	  }
	  
	  if(this.nucleusClass == RodentSpermNucleus.class){
		  return Constants.RODENT_SPERM_NUCLEUS_ORIENTATION_POINT;
	  }
	  
	  // default if not defined above
	  return Constants.ROUND_NUCLEUS_ORIENTATION_POINT;
  }
  
  public ProfileCollection getProfileCollection(String type){
	  if(this.profileCollections.containsKey(type)){
		  return this.profileCollections.get(type);
	  } else {
		  throw new IllegalArgumentException("ProfileCollection key "+type+" not present");
	  }
  }
  
  public void setProfileCollection(String type, ProfileCollection p){
	  this.profileCollections.put(type, p);
  }
  
  public ProfileCollection getProfileCollection(){
	  return getProfileCollection(CellCollection.REGULAR_PROFILE);
  }
  
  public ProfileCollection getFrankenCollection(){
	  return getProfileCollection(CellCollection.FRANKEN_PROFILE);
  }
  
  public void setFrankenCollection (ProfileCollection frankenCollection){
	  this.setProfileCollection(CellCollection.FRANKEN_PROFILE, frankenCollection);
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
  
  /**
   * Get the distinct source image file list for all nuclei in the collection 
   * @return
   */
  public List<File> getImageFiles(){
	  List<File> result = new ArrayList<File>(0);
	  for(Nucleus n : this.getNuclei()){
		  
		  if(!result.contains( n.getSourceFile() )){
			  result.add(n.getSourceFile());
		  }
	  }
	  return result;
  }

  public double[] getPerimeters(){

    double[] d = new double[cellCollection.size()];

    for(int i=0;i<cellCollection.size();i++){
      d[i] = cellCollection.get(i).getNucleus().getPerimeter();
    }
    return d;
  }

  public double[] getAreas(){

    double[] d = new double[cellCollection.size()];

    for(int i=0;i<cellCollection.size();i++){
      d[i] = cellCollection.get(i).getNucleus().getArea();
    }
    return d;
  }

  public double[] getFerets(){

    double[] d = new double[cellCollection.size()];

    for(int i=0;i<cellCollection.size();i++){
      d[i] = cellCollection.get(i).getNucleus().getFeret();
    }
    return d;
  }
  
  public double[] getMinFerets(){

	    double[] d = new double[cellCollection.size()];

	    for(int i=0;i<cellCollection.size();i++){
	      d[i] = cellCollection.get(i).getNucleus().getNarrowestDiameter();
	    }
	    return d;
	  }

  public double[] getPathLengths(){

    double[] d = new double[cellCollection.size()];

    for(int i=0;i<cellCollection.size();i++){
      d[i] = cellCollection.get(i).getNucleus().getPathLength();
    }
    return d;
  }

  public double[] getArrayLengths(){

    double[] d = new double[cellCollection.size()];

    for(int i=0;i<cellCollection.size();i++){
      d[i] = cellCollection.get(i).getNucleus().getLength();
    }
    return d;
  }

  public double[] getMedianDistanceBetweenPoints(){

    double[] d = new double[cellCollection.size()];

    for(int i=0;i<cellCollection.size();i++){
      d[i] = cellCollection.get(i).getNucleus().getMedianDistanceBetweenPoints();
    }
    return d;
  }
  
  public String[] getNucleusImagePaths(){
	    String[] s = new String[cellCollection.size()];

	    for(int i=0;i<cellCollection.size();i++){
	    	Nucleus n = cellCollection.get(i).getNucleus();
	      s[i] = n.getPath();
	    }
	    return s;
	  }

  public String[] getNucleusPathsAndNumbers(){
    String[] s = new String[cellCollection.size()];

    for(int i=0;i<cellCollection.size();i++){
    	Nucleus n = cellCollection.get(i).getNucleus();
      s[i] = n.getPathAndNumber(); //+"-"+nucleiCollection.get(i).getNucleusNumber();
    }
    return s;
  }

  public String[] getCleanNucleusPaths(){
    String[] s = new String[cellCollection.size()];

    for(int i=0;i<cellCollection.size();i++){
      Nucleus n = cellCollection.get(i).getNucleus();
      s[i] = n.getPath();
    }
    return s;
  }

  public double[][] getPositions(){
    double[][] s = new double[cellCollection.size()][4];

    for(int i=0;i<cellCollection.size();i++){
      s[i] = cellCollection.get(i).getNucleus().getPosition();
    }
    return s;
  }

  public int getNucleusCount(){
    return this.cellCollection.size();
  }
  
  public List<Cell> getCells(){
	    return this.cellCollection;
	  }

  public List<Nucleus> getNuclei(){
	  List<Nucleus> result = new ArrayList<Nucleus>(0);
	  for(Cell c : this.cellCollection){
		  result.add(c.getNucleus());
	  }
	  
    return result;
  }

  public Cell getCell(int i){
    return this.cellCollection.get(i);
  }
  
  /**
   * Get the nuclei within the specified image
   * @param image the file to search
   * @return the list of nuclei
   */
  public List<Nucleus> getNuclei(File imageFile){
	  List<Nucleus> result = new ArrayList<Nucleus>(0);
	  for(Nucleus n : this.getNuclei()){
		  if(n.getSourceFile().equals(imageFile)){
			  result.add(n);
		  }
	  }
	  return result;
  }
  
  /**
   * Find the signal groups present within the nuclei of the collection
   * @return the list of groups. Order is not guaranteed
   */
  public List<Integer> getSignalGroups(){
	  List<Integer> result = new ArrayList<Integer>(0);
	  for(Nucleus n : this.getNuclei()){
		  for( int group : n.getSignalCollection().getSignalGroups()){
			  if(!result.contains(group)){
				  result.add(group);
			  }
		  }
	  } // end nucleus iterations
	  return result;
  }
  
  public String getSignalGroupName(int signalGroup){
	  String result = null;
	  
	  for(Nucleus n : this.getNuclei()){
		  if(n.hasSignal(signalGroup)){
			  result = n.getSignalCollection().getSignalGroupName(signalGroup);
		  }
	  }
	  return result;
  }
  
  public int getSignalChannel(int signalGroup){
	  int result = 0;
	  
	  for(Nucleus n : this.getNuclei()){
		  if(n.hasSignal(signalGroup)){
			  result = n.getSignalCollection().getSignalChannel(signalGroup);
		  }
	  }
	  return result;
  }
  
  public String getSignalSourceFolder(int signalGroup){
	  String result = null;
	  
	  for(Nucleus n : this.getNuclei()){
		  if(n.hasSignal(signalGroup)){
			  File file = n.getSignalCollection().getSourceFile(signalGroup);
			  result = file.getParentFile().getAbsolutePath();
		  }
	  }
	  return result;
  }
  
  /**
   * Find the total number of signals within all nuclei of the collection.
   * @return the total
   */
  public int getSignalCount(){
	  int count = 0;
	  for(int signalGroup : this.getSignalGroups()){
		  count+= this.getSignalCount(signalGroup);
	  }
	  return count;
  }
  
  /**
   * Get the number of signals in the given group
   * @param signalGroup the group to search
   * @return the count
   */
  public int getSignalCount(int signalGroup){
	  int count = 0;
	  for(Nucleus n : this.getNuclei()){
		  count += n.getSignalCount(signalGroup);

	  } // end nucleus iterations
	  return count;
  }

  /**
   * Test whether the current population has signals in any channel
   * @return
   */
  public boolean hasSignals(){
	  for(int i : this.getSignalGroups()){
		  if(this.hasSignals(i)){
			  return true;
		  }
	  }
	  return false;
  }

  /**
   * Test whether the current population has signals in the given group
   * @return
   */
  public boolean hasSignals(int signalGroup){
	  if(this.getSignalCount(signalGroup)>0){
		  return true;
	  } else{
		  return false;
	  }

  }
  
  /**
   * Check the signal groups for all nuclei in the colleciton, and
   * return the highest signal group present, or 0 if no signal groups
   * are present
 * @return the highest signal group
 */
  public int getHighestSignalGroup(){
	  int maxGroup = 0;
	  for(Nucleus n : this.getNuclei()){
		  for(int group : n.getSignalCollection().getSignalGroups()){
			  maxGroup = group > maxGroup ? group : maxGroup;
		  }
	  }
	  return maxGroup;
  }

  /**
   * Get all the signals from all nuclei in the given channel
   * @param channel the channel to search
   * @return a list of signals
   */
  public List<NuclearSignal> getSignals(int channel){

	  List<NuclearSignal> result = new ArrayList<NuclearSignal>(0);

	  for(int i= 0; i<this.getNucleusCount();i++){
		  Nucleus n = this.getCell(i).getNucleus();
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
	  return this.getCell(0).getNucleus().getAngleProfileWindowSize();
  }
  
  /**
   * Get the median area of the signals in the given channel
   * @param channel
   * @return the median area
   */
  public double getMedianSignalArea(int channel){
	  List<Cell> cells = getCellsWithNuclearSignals(channel, true);
	  List<Double> a = new ArrayList<Double>(0);
	  for(Cell c : cells){
		  Nucleus n = c.getNucleus();
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
	  List<Cell> cells = getCellsWithNuclearSignals(channel, true);
	  List<Double> a = new ArrayList<Double>(0);
	  for(Cell c : cells){
		  Nucleus n = c.getNucleus();
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
	  List<Cell> cells = getCellsWithNuclearSignals(channel, true);
	  List<Double> a = new ArrayList<Double>(0);
	  for(Cell c : cells){
		  Nucleus n = c.getNucleus();
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
	  List<Cell> cells = getCellsWithNuclearSignals(channel, true);
	  List<Double> a = new ArrayList<Double>(0);
	  for(Cell c : cells){
		  Nucleus n = c.getNucleus();
		  a.addAll(n.getSignalCollection().getDistances(channel));

	  }
	  return Stats.quartile(a.toArray(new Double[0]), 50);
  }

  /** 
   * Return the nuclei that have signals in the given channel. If 
   * prepended with '-' , this will give the nuclei
   * that do NOT have signals in the given channel.
   * @param channel the channel 
   * @param with return cells with a signal?
   * @return a list of nuclei
   */
  public List<Cell> getCellsWithNuclearSignals(int signalGroup, boolean withSignal){
	  List<Cell> result = new ArrayList<Cell>(0);

	  for(Cell c : this.cellCollection){
		  Nucleus n = c.getNucleus();

		  if(withSignal){
			  if(n.hasSignal(signalGroup)){
				  result.add(c);
			  }
		  } else {
			  if(!n.hasSignal(Math.abs(signalGroup))){
				  result.add(c);
			  }
		  }
	  }
	  return result;
  }

  /*
    --------------------
    Profile methods
    --------------------
   */

  /**
   * Get a list of all the segments currently within the profile collection
   * @return
   */
  public List<String> getSegmentNames(){

	  List<String> result = new ArrayList<String>(0);
	  ProfileCollection pc = this.getProfileCollection();
	  List<NucleusBorderSegment> segs = pc.getSegments(this.getOrientationPoint());
	  for(NucleusBorderSegment segment : segs){
		  result.add(segment.getSegmentType());
	  }
	  return result;
  }

  /**
   * Get the differences to the median profile for each nucleus
   * @param pointType the point to fetch profiles from
   * @return an array of differences
   */
  public double[] getDifferencesToMedianFromPoint(String pointType){
	  double[] d = new double[this.getNucleusCount()];
	  try{

		  Profile medianProfile = this.getProfileCollection().getProfile(pointType);
		  for(int i=0;i<this.getNucleusCount();i++){
			  Nucleus n = this.getCell(i).getNucleus();
			  try{
				  d[i] = n.getAngleProfile().offset(n.getBorderIndex(pointType)).differenceToProfile(medianProfile);
			  } catch(Exception e){
//				  logger.log("Unable to get difference to median profile: "+i+": "+pointType, Logger.ERROR);
			  }
		  }
	  } catch(Exception e){
//		  logger.log("Error getting differences from point "+pointType+": "+e.getMessage(), Logger.ERROR);
//		  this.profileCollection.printKeys(this.debugFile);
	  }
	  return d;
  }
  
  /**
   * Get the differences to the median profile for each nucleus, normalised to the
   * perimeter of the nucleus
   * @param pointType the point to fetch profiles from
   * @return an array of normalised differences
   */
  public double[] getNormalisedDifferencesToMedianFromPoint(String pointType){
	  double[] d = new double[this.getNucleusCount()];
	  try{

		  Profile medianProfile = this.getProfileCollection().getProfile(pointType);
		  for(int i=0;i<this.getNucleusCount();i++){
			  Nucleus n = this.getCell(i).getNucleus();
			  try{
				  double diff = n.getAngleProfile().offset(n.getBorderIndex(pointType)).differenceToProfile(medianProfile);
				  d[i] = diff / n.getPerimeter();
			  } catch(Exception e){
				  IJ.log("Unable to get normalised difference to median profile: "+i+": "+pointType);
			  }
		  }
	  } catch(Exception e){
		  IJ.log("Error getting differences from point "+pointType+": "+e.getMessage());
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


  public int[] getPointIndexes(String pointType){
    int[] d = new int[this.getNucleusCount()];

    for(int i=0;i<this.getNucleusCount();i++){
      Nucleus n = this.getCell(i).getNucleus();
      d[i] = n.getBorderIndex(pointType);
    }
    return d;
  }

  public double[] getPointToPointDistances(String pointTypeA, String pointTypeB){
    double[] d = new double[this.getNucleusCount()];
    for(int i=0;i<this.getNucleusCount();i++){
      Nucleus n = this.getCell(i).getNucleus();
      d[i] = n.getBorderTag(pointTypeA).getLengthTo(n.getBorderTag(pointTypeB));
    }
    return d;
  }

  /*
    -----------------
    Profile functions
    -----------------
  */

  public Nucleus getNucleusMostSimilarToMedian(String pointType){
    
    Profile medianProfile = this.getProfileCollection().getProfile(pointType); // the profile we compare the nucleus to
    Nucleus n = (Nucleus) this.getCell(0).getNucleus(); // default to the first nucleus

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