/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
/* 
  -----------------------
  NUCLEUS COLLECTION CLASS
  -----------------------
  This class contains the nuclei that pass detection criteria
  Provides aggregate stats
  It enables offsets to be calculated based on the median normalised curves
*/

package components;

import ij.IJ;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import utility.Constants;
import utility.Stats;
import utility.Utils;
import analysis.AnalysisDataset;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileCollectionType;
import components.nuclear.NuclearSignal;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.NucleusStatistic;
import components.nuclear.NucleusType;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;

public class CellCollection implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private File 	folder; 		// the source of the nuclei
	private String 	outputFolder;	// the location to save out data
	private File 	debugFile;		// the location of the debug file 
	private String 	collectionType; // for annotating image names
	private String 	name;			// the name of the collection
	private UUID 	guid;			// the collection id
	
	private NucleusType nucleusType; // the type of nuclei this collection contains
		
	//this holds the mapping of tail indexes etc in the median profile arrays
	protected Map<ProfileCollectionType, ProfileCollection> profileCollections = new HashMap<ProfileCollectionType, ProfileCollection>();
		
	private ConsensusNucleus consensusNucleus; 	// the refolded consensus nucleus
	
	private Map<UUID, Cell> mappedCollection  = new HashMap<UUID, Cell>();	// store all the nuclei analysed

	/**
	 * Constructor.
	 * @param folder the folder of images
	 * @param outputFolder a name for the outputs (usually the analysis date). Can be null
	 * @param type the type of collection (e.g. analysable)
	 * @param debugFile the location of the log file 
	 * @param nucleusClass the class of nucleus to be held
	 */
	public CellCollection(File folder, String outputFolder, String type, File debugFile, NucleusType nucleusType){
		this.folder = folder;
		this.outputFolder = outputFolder;
		this.debugFile = debugFile;
		this.collectionType = type;
		this.name = outputFolder == null ? type : type+" - "+outputFolder;
		this.guid = java.util.UUID.randomUUID();
		this.nucleusType = nucleusType;
		profileCollections.put(ProfileCollectionType.REGULAR, new ProfileCollection());
	}
  
  /**
   * Construct from a template dataset
   * @param template the dataset to base on for analysis options, folders
   * @param name the collection name
   */
  public CellCollection(AnalysisDataset template, String name){

	  this(template.getCollection().getFolder(), 
			  template.getCollection().getOutputFolderName(), 
			  name, 
			  template.getCollection().getDebugFile(),
			  template.getCollection().getNucleusType()
			  );
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
  
  /**
   * Get the UUIDs of all the cells in the collection
   * @return
   */
  public List<UUID> getCellIds(){
	  List<UUID> result = new ArrayList<UUID>(0);

	  for(UUID id : mappedCollection.keySet()){
		  result.add(id);
	  }
	  return  result;
  }

  public void addCell(Cell r){
	  this.mappedCollection.put(r.getId(), r);
  }
  
  public void removeCell(Cell c){
	  this.mappedCollection.remove(c.getId());
  }
  
  public int size(){
	  return mappedCollection.size();
  }
    
  public boolean hasConsensusNucleus(){
	  if(this.consensusNucleus==null){
		  return false;
	  } else {
		  return true;
	  }
  }


  public void addConsensusNucleus(ConsensusNucleus n){
	  this.consensusNucleus = n;
  }
    

  /**
   * Get the cell with the given UUID
   * @param id
   * @return
   */
  public Cell getCell(UUID id){
	  return this.mappedCollection.get(id);
  }
  
  
  public NucleusType getNucleusType(){
	  return this.nucleusType;
  }
    
  /**
   * Get the cell with the given path
   * @param path the path to the cell (uses the path-and-number format)
   * @return
   * @see Nucleus.getPathAndNumber()
   */
  public Cell getCell(String path){
	  for(Cell c : this.getCells()){
		  Nucleus n = c.getNucleus();
		  if(n.getPathAndNumber().equals(path)){
			  return c;
		  }
	  }
	  return null;
  }
  
  public ConsensusNucleus getConsensusNucleus(){
	  return this.consensusNucleus;
  }
  
  public String getPoint(BorderTag tag){
	  
	  return this.nucleusType.getPoint(tag);
  }
  
//  public String getReferencePoint(){
//	  	  
//	  return this.getPoint(BorderTag.REFERENCE_POINT);
//  }
  
  /**
   * Get the profile collection of the given type
   * @param type
   * @return
   */
  public ProfileCollection getProfileCollection(ProfileCollectionType type){
	  if(this.profileCollections.containsKey(type)){
		  return this.profileCollections.get(type);
	  } else {
		  throw new IllegalArgumentException("ProfileCollection key "+type.toString()+" not present");
	  }
  }

  public void setProfileCollection(ProfileCollectionType type, ProfileCollection p){
	  this.profileCollections.put(type, p);
  }
  
  public File getFolder(){
    return this.folder;
  }

  public String getOutputFolderName(){
    return this.outputFolder;
  }
  
  
  /**
   * Get the output folder (e.g. to save the dataset into).
   * If an output folder name (such as a date) has been input, it will be included 
   * @return the folder
   */
  public File getOutputFolder(){
	  File result = null;
	  if(this.getOutputFolderName()==null){
		  result = this.getFolder();
	  } else {
		  result = new File(this.getFolder()+File.separator+this.getOutputFolderName());
	  }
	  return result;
  }
  
  

  public File getDebugFile(){
    return this.debugFile;
  }
  
  /**
   * Allow the collection to update the debug file location
   * @param f the new file
   */
  public void setDebugFile(File f){
	  try {
		  if(!f.exists()){
			  f.createNewFile();
		  }
		  if(f.canWrite()){
			  this.debugFile = f;
		  }
	  } catch (IOException e) {
		  IJ.log("Unable to update debug file location");
	  }
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
  
  /**
   * Get the areas of the nuclei in this collection as
   * an array at the appropriate scale
   * @return
   */
  public double[] getAreas(MeasurementScale scale){

	  List<Double> list = new ArrayList<Double>();
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  
		  double area = n.getArea();
		  
		  if(scale.equals(MeasurementScale.MICRONS)){
			  area = Utils.micronArea(area, n.getScale());
		  }
		  list.add(area);
	  }
	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }
    
  /**
   * Get the perimeters of the nuclei in this collection as
   * an array
   * @return
   */
  public double[] getPerimeters(MeasurementScale scale){

	  List<Double> list = new ArrayList<Double>();
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  
		  double value = n.getPerimeter();
		  
		  if(scale.equals(MeasurementScale.MICRONS)){
			  value = Utils.micronLength(value, n.getScale());
		  }
		  list.add(value);
	  }
	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }
  
  /**
   * Get the circularities of the nuclei in this collection as
   * an array
   * @return
   */
  public double[] getCircularities(){

	  List<Double> list = new ArrayList<Double>();
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  list.add(n.getCircularity());
	  }
	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }

  /**
   * Get the aspect ratios of the nuclei in this collection as
   * an array
   * @return
   */
  public double[] getAspectRatios(){

	  List<Double> list = new ArrayList<Double>();
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  list.add(n.getAspectRatio());
	  }
	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }
  
  /**
   * Get the ferets of the nuclei in this collection as
   * an array
   * @return
   */
  public double[] getFerets(MeasurementScale scale){

	  List<Double> list = new ArrayList<Double>();
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  double value = n.getFeret();
		  
		  if(scale.equals(MeasurementScale.MICRONS)){
			  value = Utils.micronLength(value, n.getScale());
		  }
		  list.add(value);
	  }
	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }
    
  /**
   * Get the minimum diameters of the nuclei in this collection as
   * an array
   * @return
   */
  public double[] getMinFerets(MeasurementScale scale){

	  List<Double> list = new ArrayList<Double>();
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  double value = n.getNarrowestDiameter();
		  
		  if(scale.equals(MeasurementScale.MICRONS)){
			  value = Utils.micronLength(value, n.getScale());
		  }
		  list.add(value);
	  }
	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }

  /**
   * Get the path lengths of the nuclei in this collection as
   * an array
   * @return
   */
  public double[] getPathLengths(){

	  List<Double> list = new ArrayList<Double>();
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  list.add(n.getPathLength());
	  }
	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }

  /**
   * Get the array lengths of the nuclei in this collection as
   * an array
   * @return
   */
  public double[] getArrayLengths(){

	  List<Double> list = new ArrayList<Double>();
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  list.add(  (double) n.getLength());
	  }
	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }
  
  public double[] getMedianDistanceBetweenPoints(){

	  List<Double> list = new ArrayList<Double>();
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  list.add(  (double) n.getMedianDistanceBetweenPoints());
	  }
	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }
  
  public String[] getNucleusImagePaths(){

	  List<String> list = new ArrayList<String>();
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  list.add(  n.getPath());
	  }
	  return list.toArray(new String[0]);
  }
  
  public String[] getNucleusPathsAndNumbers(){

	  List<String> list = new ArrayList<String>();
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  list.add(  n.getPathAndNumber());
	  }
	  return list.toArray(new String[0]);
  }
  
  public String[] getCleanNucleusPaths(){

	  List<String> list = new ArrayList<String>();
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  list.add(  n.getPath());
	  }
	  return list.toArray(new String[0]);
  }

  public double[][] getPositions(){
	  double[][] s = new double[size()][4];
	  int i = 0;
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  s[i] = n.getPosition();
		  i++;
	  }
	  return s;
  }

  public int getNucleusCount(){
    return this.size();
  }
  
  
  /**
   * Get the cells in this collection
   * @return
   */
  public List<Cell> getCells(){
	  List<Cell> result = new ArrayList<Cell>(0);
	  for(Cell cell : mappedCollection.values()){
		  result.add(cell);
	  }
	  return result;
  }

  /**
   * Get the nuclei in this collection
   * @return
   */
  public List<Nucleus> getNuclei(){
	  List<Nucleus> result = new ArrayList<Nucleus>(0);
	  for(Cell c : this.getCells()){
		  result.add(c.getNucleus());
	  }

	  return result;
  }

//  public Cell getCell(int i){
//    return this.cellCollection.get(i);
//  }
  
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
	  for(Nucleus n : this.getNuclei()){
		  result.addAll(n.getSignals(channel));
	  }
	  return result;
  }
  
  // allow for refiltering of nuclei based on nuclear parameters after looking at the rest of the data
  public double getMedianNuclearArea(){
    double[] areas = this.getAreas(MeasurementScale.PIXELS);
    double median = Stats.quartile(areas, 50);
    return median;
  }

  public double getMedianNuclearPerimeter(){
    double[] p = this.getPerimeters(MeasurementScale.PIXELS);
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
    double[] p = this.getFerets(MeasurementScale.PIXELS);
    double median = Stats.quartile(p, 50);
    return median;
  }

  public double getMaxProfileLength(){
	  return Stats.max(this.getArrayLengths());
  }
  
//  public int getProfileWindowSize(){
//	  this.mappedCollection.
//	  return this.getCell(0).getNucleus().getAngleProfileWindowSize();
//  }
  
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

	  for(Cell c : this.getCells()){
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
	  ProfileCollection pc = this.getProfileCollection(ProfileCollectionType.REGULAR);
	  List<NucleusBorderSegment> segs = pc.getSegments(BorderTag.ORIENTATION_POINT);
	  for(NucleusBorderSegment segment : segs){
		  result.add(segment.getName());
	  }
	  return result;
  }

  /**
   * Get the differences to the median profile for each nucleus
   * @param pointType the point to fetch profiles from
   * @return an array of differences
   */
  public double[] getDifferencesToMedianFromPoint(BorderTag pointType) throws Exception {
	  List<Double> list = new ArrayList<Double>();
	  Profile medianProfile = this.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(pointType, Constants.MEDIAN);
	  for(Nucleus n : this.getNuclei()){
		  list.add(n.getAngleProfile().offset(n.getBorderIndex(pointType)).absoluteSquareDifference(medianProfile));
	  }
	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }
  
  /**
   * Get the differences to the median profile for each nucleus, normalised to the
   * perimeter of the nucleus. This is the sum-of-squares difference, rooted and divided by
   * the nuclear perimeter
   * @param pointType the point to fetch profiles from
   * @return an array of normalised differences
   */
  public double[] getNormalisedDifferencesToMedianFromPoint(BorderTag pointType) throws Exception {
	  List<Double> list = new ArrayList<Double>();

	  Profile medianProfile = this.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(pointType, Constants.MEDIAN);
//	  Profile medianProfile = this.getProfileCollection().getProfile(pointType);
	  for(Nucleus n : this.getNuclei()){
		  
		  double diff = n.getAngleProfile(pointType).absoluteSquareDifference(medianProfile);										 
//		  double diff = n.getAngleProfile().offset(n.getBorderIndex(pointType)).absoluteSquareDifference(medianProfile);										 
		  double rootDiff = Math.sqrt(diff); // use the differences in degrees, rather than square degrees  
		  double var = (rootDiff / n.getPerimeter()  ); // normalise to the number of points in the perimeter (approximately 1 point per pixel)
		  
		  list.add(var);
	  }

	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }

  public double compareProfilesToMedian(BorderTag pointType) throws Exception{
	  double[] scores = this.getDifferencesToMedianFromPoint(pointType);
	  double result = 0;
	  for(double s : scores){
		  result += s;
	  }
	  return result;
  }


  public int[] getPointIndexes(BorderTag pointType){
	  List<Integer> list = new ArrayList<Integer>();

	  for(Nucleus n : this.getNuclei()){
		  list.add(n.getBorderIndex(pointType));
	  }
	  return Utils.getintFromInteger(list.toArray(new Integer[0]));
  }

  /**
   * Get the distances between two border tags for each nucleus
   * @param pointTypeA
   * @param pointTypeB
   * @return
   */
  public double[] getPointToPointDistances(BorderTag pointTypeA, BorderTag pointTypeB){
	  List<Double> list = new ArrayList<Double>();
	  for(Nucleus n : this.getNuclei()){
		  list.add(n.getPoint(pointTypeA).getLengthTo(n.getPoint(pointTypeB)));
	  }
	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }

  /**
   * Get the nucleus with the lowest difference score to the median profile
   * @param pointType the point to compare profiles from
   * @return the best nucleus
   * @throws Exception
   */
  public Nucleus getNucleusMostSimilarToMedian(BorderTag pointType) throws Exception {

	  Profile medianProfile = this.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(pointType, 50); // the profile we compare the nucleus to
	  Nucleus n = this.getNuclei().get(0); // default to the first nucleus

	  double difference = Stats.max(getDifferencesToMedianFromPoint(pointType));
	  for(Nucleus p : this.getNuclei()){
		  double nDifference = p.getAngleProfile(pointType).absoluteSquareDifference(medianProfile);
		  if(nDifference<difference){
			  difference = nDifference;
			  n = p;
		  }
	  }
	  return n;
  }


  /**
   * Get the name of the log file for this collection
   * @param filename
   * @return
   */
  public String getLogFileName(String filename){
	  String file = this.getFolder()+File.separator+this.getOutputFolderName()+File.separator+filename+"."+getType()+".txt";
	  File f = new File(file);
	  if(f.exists()){
		  f.delete();
	  }
	  return file;
  }
  
  /**
   * Get the perimeter normalised veriabililty of a nucleus angle profile compared to the
   * median profile of the collection
   * @param n the nucleus to test
   * @return the variabililty score of the nucleus
   * @throws Exception
   */
  public double calculateVariabililtyOfNucleusProfile(Nucleus n) throws Exception {
	  BorderTag pointType = BorderTag.REFERENCE_POINT;
	  Profile medianProfile = this.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(pointType,50);
	  double diff = n.getAngleProfile().offset(n.getBorderIndex(pointType)).absoluteSquareDifference(medianProfile);										 
	  double rootDiff = Math.sqrt(diff); // use the differences in degrees, rather than square degrees  
	  double var = (rootDiff / n.getPerimeter()  ); // normalise to the number of points in the perimeter (approximately 1 point per pixel)
	  return var;
  }
  
  public double[] getNuclearStatistics(NucleusStatistic stat, MeasurementScale scale) throws Exception {
	  
	  double[] result = null;
	  switch (stat) {
	  
	  		case AREA: {
	  			result = this.getAreas(scale);
	  			break;
	  		}
	  		
	  		case PERIMETER:{
	  			result = this.getPerimeters(scale);
	  			break;
	  		}
	  		
	  		case MAX_FERET:{
	  			result = this.getFerets(scale);
	  			break;
	  		}
	  		
	  		case MIN_DIAMETER:{
	  			result = this.getMinFerets(scale);
	  			break;
	  		}
	  		
	  		case ASPECT:{
	  			result = this.getAspectRatios();
	  			break;
	  		}
	  		
	  		case CIRCULARITY:{
	  			result = this.getCircularities();
	  			break;
	  		}
	  		
	  		case VARIABILITY:{
	  			result = this.getNormalisedDifferencesToMedianFromPoint(BorderTag.ORIENTATION_POINT);
	  			break;
	  		}

	  }
	  return result;
  }
  
  public double[] getSegmentLengths(String segName, MeasurementScale scale) throws Exception{
	  List<Double> list = new ArrayList<Double>();

	  for(Nucleus n : this.getNuclei()){
		  NucleusBorderSegment segment = n.getAngleProfile(BorderTag.REFERENCE_POINT).getSegment(segName);
		  
		  int indexLength = segment.length();
		  double fractionOfPerimeter = (double) indexLength / (double) segment.getTotalLength();
		  double perimeterLength = fractionOfPerimeter * n.getStatistic(NucleusStatistic.PERIMETER, scale);
		  
		  list.add(perimeterLength);
	  }
	  return Utils.getdoubleFromDouble( list.toArray(new Double[0]));
  }

}