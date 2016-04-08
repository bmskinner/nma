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

import stats.NucleusStatistic;
import stats.PlottableStatistic;
import stats.SegmentStatistic;
import stats.SignalStatistic;
import stats.Stats;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import utility.Constants;
import utility.Utils;
import analysis.AnalysisDataset;
import analysis.NucleusStatisticFetchingTask;
import analysis.ProfileManager;
import analysis.SegmentStatisticFetchingTask;
import analysis.SignalManager;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.NucleusType;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;

/**
 * @author bms41
 *
 */
public class CellCollection implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	// TODO: this needs reworking
	private final UUID 	uuid;			// the collection id
	
	private File 	    folder; 		// the source of the nuclei
	private String     	outputFolder;	// the location to save out data
	private String 	    name;			// the name of the collection
	
	
	private NucleusType nucleusType; // the type of nuclei this collection contains
	
	
	/**
	 * Cache statistics from the cells in the collection. This should be updated if a cell is added or lost 
	 */
	private StatsCache statsCache = new StatsCache();
		
	//this holds the mapping of tail indexes etc in the median profile arrays
	protected Map<ProfileType, ProfileCollection> profileCollections = new HashMap<ProfileType, ProfileCollection>();
		
	private ConsensusNucleus consensusNucleus; 	// the refolded consensus nucleus
	
	private Map<UUID, Cell> mappedCollection  = new HashMap<UUID, Cell>();	// store all the nuclei analysed
	
	private transient boolean isRefolding = false;

	/**
	 * Constructor.
	 * @param folder the folder of images
	 * @param outputFolder a name for the outputs (usually the analysis date). Can be null
	 * @param name the name of the collection
	 * @param nucleusClass the class of nucleus to be held
	 */
	public CellCollection(File folder, String outputFolder, String name, NucleusType nucleusType){
		this( folder,  outputFolder,  name,  nucleusType, java.util.UUID.randomUUID());		
	}
	
	/**
	 * Constructor with non-random id. Use only when copying an old collection. Can cause ID conflicts!
	 * @param folder the folder of images
	 * @param outputFolder a name for the outputs (usually the analysis date). Can be null
	 * @param name the name of the collection
	 * @param nucleusClass the class of nucleus to be held
	 * @param id specify an id for the collection, rather than generating randomly.
	 */
	public CellCollection(File folder, String outputFolder, String name, NucleusType nucleusType, UUID id){
		
		this.uuid         = id;
		this.folder       = folder;
		this.outputFolder = outputFolder;
		this.name         = name == null ? folder.getName() : name;// if name is null, use the image folder name
		this.nucleusType  = nucleusType;
		
		for(ProfileType type : ProfileType.values()){
			profileCollections.put(type, new ProfileCollection());
		}
		
	}
  
  /**
   * Construct an empty collection from a template dataset
   * @param template the dataset to base on for folders and type
   * @param name the collection name
   */
  public CellCollection(AnalysisDataset template, String name){

	  this(template.getCollection(), name );
  }
  
  /**
   * Construct an empty collection from a template collection
   * @param template
   * @param name
   */
  public CellCollection(CellCollection template, String name){
	  this(template.getFolder(), 
			  template.getOutputFolderName(), 
			  name, 
			  template.getNucleusType()
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
	  return this.uuid;
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

  public void addCell(Cell r) throws Exception{
	  if(mappedCollection.containsKey(r.getId())){
		  return;
	  } else {
		  this.mappedCollection.put(r.getId(), r);
	  }
  }

  public void removeCell(Cell c) throws Exception{
	  this.mappedCollection.remove(c.getId());
  }
  
  public int cellCount(){
	  return mappedCollection.size();
  }
    
  public boolean hasConsensusNucleus(){
	  if(this.consensusNucleus==null){
		  return false;
	  } else {
		  return true;
	  }
  }
  
  /**
   * Check if the collection contains cells
   * @return
   */
  public boolean hasCells(){
	  if(this.mappedCollection.isEmpty()){
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
	  Iterator<Cell> it = this.getCellIterator();

	  while(it.hasNext()){
		  Cell c = it.next();
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
    
  /**
   * Get the profile collection of the given type
   * @param type
   * @return
   */
  public ProfileCollection getProfileCollection(ProfileType type){
	  if(this.profileCollections.containsKey(type)){
		  return this.profileCollections.get(type);
	  } else {
		  throw new IllegalArgumentException("ProfileCollection key "+type.toString()+" not present");
	  }
  }

  public void setProfileCollection(ProfileType type, ProfileCollection p){
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

//  public String getType(){
//    return this.collectionType;
//  }
  
  /**
   * Get the distinct source image file list for all nuclei in the collection 
   * @return
   */
  public Set<File> getImageFiles(){
	  Set<File> result = new HashSet<File>(0);
	  for(Nucleus n : this.getNuclei()){
		  result.add(n.getSourceFile());
	  }
	  return result;
  }
  
  /**
   * Get the path lengths of the nuclei in this collection as
   * an array
   * @return
 * @throws Exception 
   */
  public double[] getPathLengths() throws Exception{

	  int count = this.getNucleusCount();
	  double[] result = new double[count];
	  int i=0;
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  result[i++] =  n.getPathLength();
	  }
	  return result;
  }

  /**
   * Get the array lengths of the nuclei in this collection as
   * an array
   * @return
   */
  public double[] getArrayLengths(){

	  int count = this.getNucleusCount();
	  double[] result = new double[count];
			  
//	  List<Double> list = new ArrayList<Double>();
	  int i=0;
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
//		  list.add(  (double) n.getBorderLength());
		  result[i++] =  n.getBorderLength();
	  }
//	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
	  return result;
  }
  
  public double[] getMedianDistanceBetweenPoints(){

	  int count = this.getNucleusCount();
	  double[] result = new double[count];
	  
	  int i=0;
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  result[i++] =  n.getMedianDistanceBetweenPoints();
	  }
	  return result;
  }
  
  public String[] getNucleusImagePaths(){

	  int count = this.getNucleusCount();
	  String[] result = new String[count];
	  int i =0;
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  result[i++] = n.getSourceFile().getAbsolutePath();
	  }
	  return result;
  }
  
  public String[] getNucleusPathsAndNumbers(){

	  int count = this.getNucleusCount();
	  String[] result = new String[count];
	  int i =0;
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  result[i++] = n.getPathAndNumber();
	  }
	  return result;
  }
  
  public double[][] getPositions(){
	  double[][] s = new double[cellCount()][4];
	  int i = 0;
	  for(Cell cell : getCells() ){ 
		  Nucleus n = cell.getNucleus();
		  s[i] = n.getPosition();
		  i++;
	  }
	  return s;
  }

  public int getNucleusCount(){
    return this.cellCount();
  }
  
  public Iterator<Cell> getCellIterator(){
	  return mappedCollection.values().iterator();
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
   * Get the cells in this collection
   * @return
   */
  public List<Cell> getCells(File imageFile){
	  List<Cell> result = new ArrayList<Cell>(0);
	  for(Cell cell : this.getCells()){
		  if(cell.getNucleus().getSourceFile().equals(imageFile)){
			  result.add(cell);
		  }
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
   * Create a SignalManager with responsibility for 
   * aggregate nuclear signal methods.
   * @return
   */
  public SignalManager getSignalManager(){
	  return new SignalManager(this);
  }
  
  public ProfileManager getProfileManager(){
	  return new ProfileManager(this);
  }
  
  public double getMedianPathLength() throws Exception{
    double[] p = this.getPathLengths();
    double median = Stats.quartile(p, Constants.MEDIAN);
    return median;
  }

  public double getMedianArrayLength(){
    double[] p = this.getArrayLengths();
    double median = Stats.quartile(p, Constants.MEDIAN);
    return median;
  }

  public double getMaxProfileLength(){
//	  return Stats.max(this.getArrayLengths());
	  return Arrays.stream(this.getArrayLengths()).max().orElse(0); //Stats.max(values);
  }
  
    
  /**
   * Get the median of the signal statistic in the given signal group
   * @param  signalGroup
   * @return the median
 * @throws Exception 
   */
  public double getMedianSignalStatistic(SignalStatistic stat, MeasurementScale scale, int signalGroup) throws Exception{

		  double[] values = this.getSignalStatistics(stat, scale, signalGroup);
		  double median =  Stats.quartile(values, Constants.MEDIAN);
		  return median;
  }
	  

  public double[] getSignalStatistics(SignalStatistic stat, MeasurementScale scale, int signalGroup) throws Exception{

	  List<Cell> cells = this.getSignalManager().getCellsWithNuclearSignals(signalGroup, true);
	  List<Double> a = new ArrayList<Double>(0);
	  for(Cell c : cells){
		  Nucleus n = c.getNucleus();
		  a.addAll(n.getSignalCollection().getStatistics(stat, scale, signalGroup));

	  }
	  return Utils.getdoubleFromDouble(a.toArray(new Double[0]));
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
  public List<String> getSegmentNames() throws Exception {

	  List<String> result = new ArrayList<String>(0);
	  ProfileCollection pc = this.getProfileCollection(ProfileType.REGULAR);
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
	  
	  int count = this.getNucleusCount();
	  double[] result = new double[count];
	  int i=0;

	  Profile medianProfile = this.getProfileCollection(ProfileType.REGULAR).getProfile(pointType, Constants.MEDIAN);
	  for(Nucleus n : this.getNuclei()){
		  Profile angleProfile = n.getProfile(ProfileType.REGULAR);
		  result[i++] = angleProfile.offset(n.getBorderIndex(pointType)).absoluteSquareDifference(medianProfile);
	  }
	  return result;
  }
  
  /**
   * Get the differences to the median profile for each nucleus, normalised to the
   * perimeter of the nucleus. This is the sum-of-squares difference, rooted and divided by
   * the nuclear perimeter
   * @param pointType the point to fetch profiles from
   * @return an array of normalised differences
   */
  public double[] getNormalisedDifferencesToMedianFromPoint(BorderTag pointType) throws Exception {
//	  List<Double> list = new ArrayList<Double>();
	  int count = this.getNucleusCount();
	  double[] result = new double[count];
	  int i=0;
	  Profile medianProfile = this.getProfileCollection(ProfileType.REGULAR).getProfile(pointType, Constants.MEDIAN);

	  for(Nucleus n : this.getNuclei()){
		  
		  Profile angleProfile = n.getProfile(ProfileType.REGULAR, pointType);
		  double diff = angleProfile.absoluteSquareDifference(medianProfile);		
		  diff /= n.getStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS); // normalise to the number of points in the perimeter (approximately 1 point per pixel)
		  double rootDiff = Math.sqrt(diff); // use the differences in degrees, rather than square degrees  
		  result[i++] = rootDiff;

	  }
	  return result;
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
//	  List<Integer> list = new ArrayList<Integer>();

	  int count = this.getNucleusCount();
	  int[] result = new int[count];
	  int i=0;
	  
	  for(Nucleus n : this.getNuclei()){
		  result[i++] = n.getBorderIndex(pointType);
//		  list.add(n.getBorderIndex(pointType));
	  }
	  return result;
//	  return Utils.getintFromInteger(list.toArray(new Integer[0]));
  }

  /**
   * Get the distances between two border tags for each nucleus
   * @param pointTypeA
   * @param pointTypeB
   * @return
   */
  public double[] getPointToPointDistances(BorderTag pointTypeA, BorderTag pointTypeB){
//	  List<Double> list = new ArrayList<Double>();
	  int count = this.getNucleusCount();
	  double[] result = new double[count];
	  int i=0;
	  for(Nucleus n : this.getNuclei()){
		  result[i++] = n.getBorderPoint(pointTypeA).getLengthTo(n.getBorderPoint(pointTypeB));
//		  list.add(n.getBorderPoint(pointTypeA).getLengthTo(n.getBorderPoint(pointTypeB)));
	  }
	  return result;
//	  return Utils.getdoubleFromDouble(list.toArray(new Double[0]));
  }

  /**
   * Get the nucleus with the lowest difference score to the median profile
   * @param pointType the point to compare profiles from
   * @return the best nucleus
   * @throws Exception
   */
  public Nucleus getNucleusMostSimilarToMedian(BorderTag pointType) throws Exception {

	  Profile medianProfile = this.getProfileCollection(ProfileType.REGULAR).getProfile(pointType, 50); // the profile we compare the nucleus to
	  Nucleus n = this.getNuclei().get(0); // default to the first nucleus

	  double difference = Arrays.stream(getDifferencesToMedianFromPoint(pointType)).max().orElse(0);
	  for(Nucleus p : this.getNuclei()){
		  Profile angleProfile = p.getProfile(ProfileType.REGULAR, pointType);
		  double nDifference = angleProfile.absoluteSquareDifference(medianProfile);
		  if(nDifference<difference){
			  difference = nDifference;
			  n = p;
		  }
	  }
	  return n;
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
	  Profile medianProfile = this.getProfileCollection(ProfileType.REGULAR).getProfile(pointType,50);
	  Profile angleProfile = n.getProfile(ProfileType.REGULAR, pointType);
	  double diff = angleProfile.absoluteSquareDifference(medianProfile);										 
	  double rootDiff = Math.sqrt(diff); // use the differences in degrees, rather than square degrees  
	  double var = (rootDiff / n.getStatistic(NucleusStatistic.PERIMETER)  ); // normalise to the number of points in the perimeter (approximately 1 point per pixel)
	  return var;
  }
  
 
  private double getMedianStatistic(PlottableStatistic stat, MeasurementScale scale, int signalGroup, UUID id)  throws Exception {
	  
	  if(stat.getClass()==NucleusStatistic.class){
		  return getMedianNucleusStatistic((NucleusStatistic) stat, scale);
	  }
	  
	  if(stat.getClass()==SignalStatistic.class){
		  return getMedianSignalStatistic((SignalStatistic) stat, scale, signalGroup);
	  }
	  
	  if(stat.getClass()==SegmentStatistic.class){
		  return getMedianSegmentStatistic((SegmentStatistic) stat, scale, id);
	  }
	  
	  
	  return 0;
	  
  }
  
  private Nucleus[] getNucleusArray(){
	 return this.getNuclei().toArray(new Nucleus[0]);
  }
  
  public double getMedianStatistic(PlottableStatistic stat, MeasurementScale scale)  throws Exception {
	  return getMedianStatistic(stat, scale, 0, null);
  }
  
  public double getMedianStatistic(PlottableStatistic stat, MeasurementScale scale, int signalGroup)  throws Exception {
	  return getMedianStatistic(stat, scale, signalGroup, null);
  }
  
  public double getMedianStatistic(PlottableStatistic stat, MeasurementScale scale, UUID id)  throws Exception {
	  return getMedianStatistic(stat, scale, 0, id);
  }
  
  /**
   * Get the median value of the given statistic
   * @param stat
   * @param scale
   * @return
   * @throws Exception
   */
  private double getMedianNucleusStatistic(NucleusStatistic stat, MeasurementScale scale)  throws Exception {
	  
	  if(this.statsCache.hasStatistic(stat, scale)){
		  return(this.statsCache.getStatistic(stat, scale));
	  } else {
		  
		  double[] values = this.getNuclearStatistics(stat, scale);
		  double median =  Stats.quartile(values, Constants.MEDIAN);
		  statsCache.setStatistic(stat, scale, median);
		  return median;
	  }
	  
	  
  }
  
  /**
   * Get a list of the given statistic values for each nucleus in the collection
   * @param stat the statistic to use
   * @param scale the measurement scale
   * @return a list of values
   * @throws Exception
   */
  public double[] getNuclearStatistics(NucleusStatistic stat, MeasurementScale scale) throws Exception {

	  double[] result = null;
	  switch (stat) {

		  case VARIABILITY:{
			  result = this.getNormalisedDifferencesToMedianFromPoint(BorderTag.ORIENTATION_POINT);
			  break;
		  }
	
		  default: {
			  NucleusStatisticFetchingTask task = new NucleusStatisticFetchingTask(getNucleusArray(),
					  stat,
					  scale);
			  result = task.invoke();
			  break;
		  }

	  }
	  return result;
  }

  /**
   * Get the median value of the given statistic
   * @param stat
   * @param scale
   * @return
   * @throws Exception
   */
  private double getMedianSegmentStatistic(SegmentStatistic stat, MeasurementScale scale, UUID id)  throws Exception {
 
	  double[] values = this.getSegmentStatistics(stat, scale, id);
	  double median =  Stats.quartile(values, Constants.MEDIAN);
	  return median;

  }
  
  /**
   * Calculate the length of the segment with the given name in each nucleus
   * of the collection
   * @param segName the segment name
   * @param scale the scale to use
   * @return a list of segment lengths
   * @throws Exception
   */
  public double[] getSegmentStatistics(SegmentStatistic stat, MeasurementScale scale, UUID id) throws Exception{

	  SegmentStatisticFetchingTask task = new SegmentStatisticFetchingTask(getNucleusArray(),
			  stat,
			  scale, 
			  id);
	  return task.invoke();
  }
  
  /**
   * Create a new CellCollection based on this as a template. Filter the nuclei by the given statistic
   * between a lower and upper bound.
 * @param stat the statistic to filter on
 * @param scale the scale the values are in
 * @param lower include values above this
 * @param upper include values below this
 * @return a new collection
 * @throws Exception 
 */
  public CellCollection filterCollection(NucleusStatistic stat, MeasurementScale scale, double lower, double upper) throws Exception{
	  DecimalFormat df = new DecimalFormat("#.##");
	  CellCollection subCollection = new CellCollection(this, "Filtered_"+stat.toString()+"_"+df.format(lower)+"-"+df.format(upper));

	  List<Cell> filteredCells;
	  
	  if(stat.equals(NucleusStatistic.VARIABILITY)){
		  filteredCells = new ArrayList<Cell>();
		  for(Cell c : this.getCells()){
			  Nucleus n = c.getNucleus();

			  double value = this.calculateVariabililtyOfNucleusProfile(n);

			  if(value>= lower && value<= upper){
				  filteredCells.add(c);
			  }  
		  }
		  
	  } else {
		  filteredCells = getCells()
				    .parallelStream()
				    .filter(p -> p.getNucleus().getSafeStatistic(stat, scale) >= lower)
				    .filter(p -> p.getNucleus().getSafeStatistic(stat, scale) <= upper)
				    .collect(Collectors.toList());
	  }

//	  for(Cell c : this.getCells()){
//		  Nucleus n = c.getNucleus();
//
//		  double value = n.getStatistic(stat, scale);
//
//
//		  // variability must be calculated from the collection, not the nucleus
//		  if(stat.equals(NucleusStatistic.VARIABILITY)){
//
//			  value = this.calculateVariabililtyOfNucleusProfile(n);
//		  }
//
//		  if(value>= lower && value<= upper){
//			  subCollection.addCell(new Cell(c));
//		  }
//		  
//		  
//	  }
	  
	  for(Cell cell : filteredCells){
		  subCollection.addCell(new Cell(cell));
	  }
	  
	  this.getProfileManager().copyCollectionOffsets(subCollection);
	  return subCollection;
  }
  
  /**
   * Invalidate the existing cached vertically rotated nuclei,
	 and recalculate.
   */
  public void updateVerticalNuclei(){
	  for(Nucleus n : this.getNuclei()){
		  n.updateVerticallyRotatedNucleus();
	  }
  }

  public boolean updateSourceFolder(File newFolder) throws Exception{
		File oldFile = this.getFolder();
		boolean ok = false;

		if(newFolder.exists()){
			
			try {
				this.folder = newFolder;

				for(Nucleus n : this.getNuclei()){
					n.updateSourceFolder(newFolder);
				}
				ok = true;
				
			} catch (Exception e){
				// one of the nuclei failed to update
				// reset all to previous
				this.folder = oldFile;

				for(Nucleus n : this.getNuclei()){
					n.updateSourceFolder(oldFile);
				}
				ok = false;
			}
		}
		return ok;
	}
  
  public boolean isRefolding(){
	  return this.isRefolding;
  }
  
  public void setRefolding(boolean b){
	  this.isRefolding = b;
  }
  
  /**
   * Test if the collection contains the given cell
   * @param c
   * @return
   */
  public boolean contains(Cell c){
	  for(Cell cell : this.getCells()){
		  if (cell.equals(c)){
			  return true;
		  }
	  }
	  return false;
  }
  
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	    in.defaultReadObject();
	    isRefolding = false;
	}
  
  /**
   * Store plottable statistics for the collection
   * @author bms41
   *
   */
  private class StatsCache implements Serializable {

	private static final long serialVersionUID = 1L;
	private Map<PlottableStatistic, Map<MeasurementScale, Double>> cache = new HashMap<PlottableStatistic,  Map<MeasurementScale, Double>>();

	  public StatsCache(){

	  }

	  /**
	   * Store the given statistic
	   * @param stat
	   * @param scale
	   * @param d
	   */
	  public void setStatistic(PlottableStatistic stat, MeasurementScale scale, double d){

		  Map<MeasurementScale, Double> map;

		  if(cache.containsKey(stat)){

			  map = cache.get(stat);

		  } else {

			  map = new HashMap<MeasurementScale, Double>();
			  cache.put(stat, map);

		  }

		  map.put(scale, d);

	  }

	  public double getStatistic(PlottableStatistic stat, MeasurementScale scale){

		  if(this.hasStatistic(stat, scale)){

			  return cache.get(stat).get(scale);


		  } else  {
			  return 0;

		  }
	  }

	  public boolean hasStatistic(PlottableStatistic stat, MeasurementScale scale){
		  Map<MeasurementScale, Double> map;

		  if(cache.containsKey(stat)){

			  map = cache.get(stat);

		  } else {

			  return false;

		  }

		  if(map.containsKey(scale)){

			  return true;
		  } else {
			  return false;
		  }

	  }
	  
	  /**
	   * Empty the cache - all values must be recalculated
	   */
	  public void clear(){
		  cache = null;
		  cache = new HashMap<PlottableStatistic,  Map<MeasurementScale, Double>>();

	  }
	  
	  /**
	   * Recalculate the values in the cache based on the given collection
	   * @param collection
	   * @throws Exception
	   */
	  public void recalculate(CellCollection collection) throws Exception{
		  this.clear();
		  for(NucleusStatistic stat  : NucleusStatistic.values()){

			  for(MeasurementScale scale : MeasurementScale.values()){
				  // This will automatically refill the cache
				  collection.getMedianNucleusStatistic(stat, scale);
			  }
		  }
	  }
  }

}