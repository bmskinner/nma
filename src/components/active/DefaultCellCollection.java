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

package components.active;

import stats.NucleusStatistic;
import stats.PlottableStatistic;
import stats.Quartile;
import stats.SegmentStatistic;
import stats.SignalStatistic;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import utility.Constants;
import analysis.IAnalysisDataset;
import analysis.NucleusStatisticFetchingTask;
import analysis.profiles.ProfileException;
import analysis.profiles.ProfileManager;
import analysis.profiles.RuleSetCollection;
import analysis.profiles.SegmentStatisticFetchingTask;
import analysis.signals.SignalManager;
import components.ICell;
import components.ICellCollection;
import components.active.generic.DefaultProfileCollection;
import components.generic.BorderTagObject;
import components.generic.IProfile;
import components.generic.IProfileCollection;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.IBorderSegment;
import components.nuclear.ISignalGroup;
import components.nuclear.NucleusType;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;

/**
 * This is a more efficient replacement for the <=1.13.2 cell collections 
 * @author bms41
 *
 */
public class DefaultCellCollection 
implements ICellCollection {

	private static final long serialVersionUID = 1L;

	private final UUID 	uuid;			// the collection id

	private File 	    folder; 		// the source of the nuclei
	private String     	outputFolder;	// the location to save out data
	private String 	    name;			// the name of the collection


	private NucleusType nucleusType; // the type of nuclei this collection contains


	//this holds the mapping of tail indexes etc in the median profile arrays
	private IProfileCollection profileCollection = new DefaultProfileCollection();
	
//	protected Map<ProfileType, IProfileCollection> profileCollections = new HashMap<ProfileType, IProfileCollection>();

	private ConsensusNucleus consensusNucleus; 	// the refolded consensus nucleus

	private Set<ICell> cells  = new HashSet<ICell>(100);	// store all the cells analysed

	private Map<UUID, ISignalGroup> signalGroups = new HashMap<UUID, ISignalGroup>(0);


	private RuleSetCollection ruleSets = new RuleSetCollection();

	/*
	 * 
	 * TRANSIENT FIELDS
	 * 
	 */

	/**
	 * Set when the consensus nucleus is refolding
	 */
	private volatile transient boolean isRefolding = false;

	/**
	 * Cache statistics from the cells in the collection. This should be updated if a cell is added or lost 
	 */
	private transient StatsCache statsCache = new StatsCache();

	// cache the number of shared cells with other datasets
	protected transient Map<UUID, Integer> vennCache = new HashMap<UUID, Integer>();

	private transient SignalManager  signalManager  = new SignalManager(this);
	private transient ProfileManager profileManager = new ProfileManager(this);

	/**
	 * Constructor.
	 * @param folder the folder of images
	 * @param outputFolder a name for the outputs (usually the analysis date). Can be null
	 * @param name the name of the collection
	 * @param nucleusClass the class of nucleus to be held
	 */
	public DefaultCellCollection(File folder, String outputFolder, String name, NucleusType nucleusType){
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
	public DefaultCellCollection(File folder, String outputFolder, String name, NucleusType nucleusType, UUID id){

		this.uuid         = id;
		this.folder       = folder;
		this.outputFolder = outputFolder;
		this.name         = name == null ? folder.getName() : name;// if name is null, use the image folder name
		this.nucleusType  = nucleusType;

//		for(ProfileType type : ProfileType.values()){
//			profileCollections.put(type, new DefaultProfileCollection());
//		}

		ruleSets = RuleSetCollection.createDefaultRuleSet(nucleusType); 

	}

	/**
	 * Construct an empty collection from a template dataset
	 * @param template the dataset to base on for folders and type
	 * @param name the collection name
	 */
	public DefaultCellCollection(IAnalysisDataset template, String name){

		this(template.getCollection(), name );
	}

	/**
	 * Construct an empty collection from a template collection
	 * @param template
	 * @param name
	 */
	public DefaultCellCollection(ICellCollection template, String name){
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
	public Set<UUID> getCellIDs(){

		Set<UUID> result = new HashSet<UUID>(size());

		for(ICell c : cells){
			result.add(c.getId());
		}

		return result;
	}

	public void addCell(ICell r) {

		if(r == null){
			throw new IllegalArgumentException("Cell is null");
		}

		cells.add(r);
	}

	/**
	 * Replace the cell with the same ID as the given cell with
	 * the new copy
	 * @param r
	 */
	public void replaceCell(ICell r) {
		if(r == null){
			throw new IllegalArgumentException("Cell is null");
		}

		boolean found = false;
		Iterator<ICell> it = cells.iterator();
		while(it.hasNext()){
			ICell test = it.next();


			if(r.getId().equals(test.getId())){
				it.remove();
				found = true;
				break;
			}
		}

		// only put the cell in if it was removed
		if(found){
			addCell(r);
		}

	}

	/**
	 * Remove the given cell from the collection. If the cell is
	 * null, has no effect. If the cell is not in the collection, has
	 * no effect. 
	 * @param c the cell to remove
	 */
	public void removeCell(ICell c) {
		cells.remove(c);

	}

	public int size(){
		return cells.size();
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
		return !cells.isEmpty();
	}

	public boolean hasLockedCells(){
		for(Nucleus n : this.getNuclei()){
			if(n.isLocked()){
				return true;
			}	  
		}
		return false;
	}

	public void setCellsLocked(boolean b){
		for(Nucleus n : this.getNuclei()){
			n.setLocked(b); 
		}
	}

	public void setConsensusNucleus(ConsensusNucleus n){
		this.consensusNucleus = n;
	}


	/**
	 * Get the cell with the given UUID
	 * @param id
	 * @return
	 */
	public ICell getCell(UUID id){

		for(ICell c : cells){
			if(c.getId().equals(id)){
				return c;
			}
		}
		return null;
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
	public ICell getCell(String path){
		Iterator<ICell> it = cells.iterator();

		while(it.hasNext()){
			ICell c = it.next();
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

	/**
	 * Get the profile collection of the given type
	 * @param type
	 * @return
	 */
	public IProfileCollection getProfileCollection(){
		return profileCollection;
//		if(this.profileCollections.containsKey(type)){
//			return this.profileCollections.get(type);
//		} else {
//			throw new IllegalArgumentException("ProfileCollection key "+type.toString()+" not present");
//		}
	}

//	public void setProfileCollection(ProfileType type, IProfileCollection p){
//		this.profileCollections.put(type, p);
//	}

	/**
	 * Remove the given profile collection
	 * @param type
	 */
//	public void removeProfileCollection(ProfileType type){
//		this.profileCollections.remove(type);
//	}

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
	private double[] getPathLengths() {

		int count = this.getNucleusCount();
		double[] result = new double[count];
		int i=0;
		for(ICell cell : getCells() ){ 
			Nucleus n = cell.getNucleus();
			result[i] =  n.getPathLength(ProfileType.ANGLE);
			i++;
		}
		return result;
	}

	/**
	 * Get the array lengths of the nuclei in this collection as
	 * an array
	 * @return
	 */
	private int[] getArrayLengths(){


		int[] result = new int[size()];

		int i=0;
		for(ICell cell : getCells() ){ 
			Nucleus n = cell.getNucleus();
			result[i++] =  n.getBorderLength();
		}
		return result;
	}

	public double[] getMedianDistanceBetweenPoints(){

		int count = this.getNucleusCount();
		double[] result = new double[count];

		int i=0;
		for(ICell cell : getCells() ){ 
			Nucleus n = cell.getNucleus();
			result[i++] =  n.getMedianDistanceBetweenPoints();
		}
		return result;
	}

	public String[] getNucleusImagePaths(){

		int count = this.getNucleusCount();
		String[] result = new String[count];
		int i =0;
		for(ICell cell : getCells() ){ 
			Nucleus n = cell.getNucleus();
			result[i++] = n.getSourceFile().getAbsolutePath();
		}
		return result;
	}

	public String[] getNucleusPathsAndNumbers(){

		int count = this.getNucleusCount();
		String[] result = new String[count];
		int i =0;
		for(ICell cell : getCells() ){ 
			Nucleus n = cell.getNucleus();
			result[i++] = n.getPathAndNumber();
		}
		return result;
	}

	public int getNucleusCount(){
		return this.size();
	}

	public Iterator<ICell> getCellIterator(){
		return cells.iterator();
	}


	/**
	 * Get the cells in this collection
	 * @return
	 */
	public Set<ICell> getCells(){
		return cells;
	}

	/**
	 * Get the cells within the given image file
	 * @return
	 */
	public Set<ICell> getCells(File imageFile){
		Set<ICell> result = new HashSet<ICell>(cells.size());

		for(ICell cell : cells){
			if(cell.getNucleus().getSourceFile().equals(imageFile)){
				result.add(cell);
			}
		}
		return result;
	}

	/**
	 * Fetch all the cells in the collection that are not members of
	 * the given collection 
	 * @param collection
	 * @return
	 */
	public List<ICell> getCellsNotIn(ICellCollection collection){

		List<ICell> result = new ArrayList<ICell>(size());
		for(ICell cell : this.getCells()){
			if( ! collection.contains(cell)){
				result.add(cell);
			}
		}
		return result;

	}

	/**
	 * Get the nuclei in this collection
	 * @return
	 */
	public Set<Nucleus> getNuclei(){
		Set<Nucleus> result = new HashSet<Nucleus>(cells.size());
		for(ICell c : cells){
			result.add(c.getNucleus());
		}

		return result;
	}

	/**
	 * Get the nuclei within the specified image
	 * @param image the file to search
	 * @return the list of nuclei
	 */
	public Set<Nucleus> getNuclei(File imageFile){
		Set<Nucleus> result = new HashSet<Nucleus>(cells.size());
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
		return signalManager;
	}

	public ProfileManager getProfileManager(){
		return profileManager;
	}

	public double getMedianPathLength() {

		if(size()==0){
			return 0;
		}

		double[] p = this.getPathLengths();
		double median = new Quartile(p, Quartile.MEDIAN).doubleValue();
		return median;
	}
	
	public int getMedianArrayLength(){
		if(size()==0){
			return 0;
		}

		int[] p = this.getArrayLengths();
		double median = new Quartile(p, Quartile.MEDIAN).doubleValue();
		return (int) median;
	}

	public int getMaxProfileLength(){

		return Arrays.stream(this.getArrayLengths()).max().orElse(0); //Stats.max(values);
	}


	/*
    --------------------
    Profile methods
    --------------------
	 */

	
	/**
	 * Create the profile collections to hold angles from nuclear
	 * profiles based on the current nucleus profiles. The ProfileAggregate
	 * for each ProfileType is recalculated. The resulting median profiles
	 * will have the same length after this update
	 * @param keepLength when recalculating the profile aggregate, should the previous length be kept
	 * @return
	 * @throws Exception
	 */
	public void createProfileCollection() {

		/*
		 * Build a set of profile aggregates
		 * Default is to make profile aggregate from reference point
		 * 
		 */
		profileCollection.createProfileAggregate(this, this.getMedianArrayLength());
	}
	
	/**
	 * Get a list of all the segments currently within the profile collection
	 * @return
	 */
	public List<String> getSegmentNames() throws Exception {

		List<String> result = new ArrayList<String>(0);
		IProfileCollection pc = this.getProfileCollection();
		List<IBorderSegment> segs = pc.getSegments(Tag.ORIENTATION_POINT);
		for(IBorderSegment segment : segs){
			result.add(segment.getName());
		}
		return result;
	}

	/**
	 * Get the differences to the median profile for each nucleus
	 * @param pointType the point to fetch profiles from
	 * @return an array of differences
	 */
	private double[] getDifferencesToMedianFromPoint(Tag pointType) {

		int count = this.getNucleusCount();
		double[] result = new double[count];
		int i=0;

		IProfile medianProfile = this.getProfileCollection().getProfile(ProfileType.ANGLE, pointType, Quartile.MEDIAN);
		for(Nucleus n : this.getNuclei()){
			IProfile angleProfile = n.getProfile(ProfileType.ANGLE);
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
	public double[] getNormalisedDifferencesToMedianFromPoint(BorderTagObject pointType) {
		//	  List<Double> list = new ArrayList<Double>();
		int count = this.getNucleusCount();
		double[] result = new double[count];
		int i=0;
		IProfile medianProfile = profileCollection.getProfile(ProfileType.ANGLE, pointType, Quartile.MEDIAN);

		for(Nucleus n : this.getNuclei()){

			IProfile angleProfile = n.getProfile(ProfileType.ANGLE, pointType);
			double diff = angleProfile.absoluteSquareDifference(medianProfile);		
			diff /= n.getStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS); // normalise to the number of points in the perimeter (approximately 1 point per pixel)
			double rootDiff = Math.sqrt(diff); // use the differences in degrees, rather than square degrees  
			result[i++] = rootDiff;

		}
		return result;
	}

	/**
	 * Get the perimeter normalised veriabililty of a nucleus angle profile compared to the
	 * median profile of the collection
	 * @param pointType the tag to use as index 0
	 * @param c the cell to test
	 * @return the variabililty score of the nucleus
	 * @throws Exception
	 */
	public double getNormalisedDifferenceToMedian(Tag pointType, ICell c){
		IProfile medianProfile = profileCollection.getProfile(ProfileType.ANGLE, pointType, Quartile.MEDIAN);
		IProfile angleProfile = c.getNucleus().getProfile(ProfileType.ANGLE, pointType);
		double diff = angleProfile.absoluteSquareDifference(medianProfile);		
		diff /= c.getNucleus().getStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS); // normalise to the number of points in the perimeter (approximately 1 point per pixel)
		double rootDiff = Math.sqrt(diff); // use the differences in degrees, rather than square degrees  
		return rootDiff;
	}

	public double compareProfilesToMedian(BorderTagObject pointType) throws Exception{
		double[] scores = this.getDifferencesToMedianFromPoint(pointType);
		double result = 0;
		for(double s : scores){
			result += s;
		}
		return result;
	}


	public int[] getPointIndexes(Tag pointType){

		int count = this.getNucleusCount();
		int[] result = new int[count];
		int i=0;

		for(Nucleus n : this.getNuclei()){
			result[i++] = n.getBorderIndex(pointType);
		}
		return result;
	}

	/**
	 * Get the distances between two border tags for each nucleus
	 * @param pointTypeA
	 * @param pointTypeB
	 * @return
	 */
	public double[] getPointToPointDistances(Tag pointTypeA, Tag pointTypeB){
		int count = this.getNucleusCount();
		double[] result = new double[count];
		int i=0;
		for(Nucleus n : this.getNuclei()){
			result[i++] = n.getBorderPoint(pointTypeA).getLengthTo(n.getBorderPoint(pointTypeB));
		}
		return result;
	}

	/**
	 * Get the nucleus with the lowest difference score to the median profile
	 * @param pointType the point to compare profiles from
	 * @return the best nucleus
	 * @throws Exception 
	 */
	public Nucleus getNucleusMostSimilarToMedian(Tag pointType) throws ProfileException {

		if(cells.size()==1){
			for(ICell c : cells){
				return c.getNucleus();
			}
		}

		IProfile medianProfile = profileCollection.getProfile(ProfileType.ANGLE, pointType, Quartile.MEDIAN); // the profile we compare the nucleus to
		//	  Nucleus n = this.getNuclei()..get(0); // default to the first nucleus
		Nucleus n=null;
		double difference = Arrays.stream(getDifferencesToMedianFromPoint(pointType)).max().orElse(0);
		for(Nucleus p : this.getNuclei()){
			IProfile angleProfile = p.getProfile(ProfileType.ANGLE, pointType);
			double nDifference = angleProfile.absoluteSquareDifference(medianProfile);
			if(nDifference<difference){
				difference = nDifference;
				n = p;
			}
		}

		if(n==null){
			throw new ProfileException("Error finding nucleus similar to median");
		}

		return n;
	}


	private double getMedianStatistic(PlottableStatistic stat, MeasurementScale scale, UUID signalGroup, UUID segId)  throws Exception {

		if(stat.getClass()==NucleusStatistic.class){
			return getMedianNucleusStatistic((NucleusStatistic) stat, scale);
		}

		if(stat.getClass()==SignalStatistic.class){
			return getSignalManager().getMedianSignalStatistic((SignalStatistic) stat, scale, signalGroup);

			//		  return getMedianSignalStatistic((SignalStatistic) stat, scale, signalGroup);
		}

		if(stat.getClass()==SegmentStatistic.class){
			return getMedianSegmentStatistic((SegmentStatistic) stat, scale, segId);
		}


		return 0;

	}

	private Nucleus[] getNucleusArray(){
		return this.getNuclei().toArray(new Nucleus[0]);
	}

	public double getMedianStatistic(PlottableStatistic stat, MeasurementScale scale)  throws Exception {
		if(this.getNucleusCount()==0){
			return 0;
		}
		return getMedianStatistic(stat, scale, null, null);
	}

	/**
	 * Get the median stat for a value with an ID
	 * @param stat
	 * @param scale
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public double getMedianStatistic(PlottableStatistic stat, MeasurementScale scale, UUID id)  throws Exception {

		if(stat.getClass()==SignalStatistic.class){
			return getMedianStatistic(stat, scale, id, null);
		} 

		if(stat.getClass()==SegmentStatistic.class){
			return getMedianStatistic(stat, scale, null, id);
		}
		return 0;
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

			double median = 0;
			if(this.getNucleusCount()>0){
				double[] values = this.getNuclearStatistics(stat, scale);
				median =  new Quartile(values, Quartile.MEDIAN).doubleValue();
			}

			statsCache.setStatistic(stat, scale, median);
			return median;
		}


	}
	
	public double[] getMedianStatistics(PlottableStatistic stat, MeasurementScale scale) {
		
		
		return getMedianStatistics(stat, scale, null);
		
		
		
	}
	
	public double[] getMedianStatistics(PlottableStatistic stat, MeasurementScale scale, UUID id) {

		try {
			if(stat instanceof NucleusStatistic){
				return getNuclearStatistics((NucleusStatistic) stat, scale);
			}

			if(stat instanceof SegmentStatistic){
				return getSegmentStatistics((SegmentStatistic) stat, scale, id);

			}
		} catch (Exception e){
			return null;
		}
			
		return null;
	}


	/**
	 * Get a list of the given statistic values for each nucleus in the collection
	 * @param stat the statistic to use
	 * @param scale the measurement scale
	 * @return a list of values
	 * @throws Exception
	 */
	private double[] getNuclearStatistics(NucleusStatistic stat, MeasurementScale scale) {

		double[] result = null;
		switch (stat) {

		case VARIABILITY:{
			result = this.getNormalisedDifferencesToMedianFromPoint(Tag.REFERENCE_POINT);
			break;
		}

		default: {
			finest("Making statistic fetching task for "+stat);
			NucleusStatisticFetchingTask task = new NucleusStatisticFetchingTask(getNucleusArray(),
					stat,
					scale);
			result = task.invoke();
			finest("Fetched statistic result for "+stat);
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

		if(cells.isEmpty()){
			return 0;
		}

		double[] values = this.getSegmentStatistics(stat, scale, id);
		return new Quartile(values, Quartile.MEDIAN).doubleValue();
	}

	/**
	 * Calculate the length of the segment with the given name in each nucleus
	 * of the collection
	 * @param segName the segment name
	 * @param scale the scale to use
	 * @return a list of segment lengths
	 * @throws Exception
	 */
	private double[] getSegmentStatistics(SegmentStatistic stat, MeasurementScale scale, UUID id) throws Exception{

		SegmentStatisticFetchingTask task = new SegmentStatisticFetchingTask(getNucleusArray(),
				stat,
				scale, 
				id);
		return task.invoke();
	}

	public ICellCollection filterCollection(PlottableStatistic stat, MeasurementScale scale, double lower, double upper) {

		if(stat.getClass()==NucleusStatistic.class){
			return filterCollection(  (NucleusStatistic) stat, scale, lower, upper);
		} 

		if(stat.getClass()==SignalStatistic.class){
			return null;
		} 

		if(stat.getClass()==SegmentStatistic.class){
			return null;
		}
		return null;

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
	private ICellCollection filterCollection(NucleusStatistic stat, MeasurementScale scale, double lower, double upper) {
		DecimalFormat df = new DecimalFormat("#.##");
		ICellCollection subCollection = new DefaultCellCollection(this, "Filtered_"+stat.toString()+"_"+df.format(lower)+"-"+df.format(upper));

		List<ICell> filteredCells;

		if(stat.equals(NucleusStatistic.VARIABILITY)){
			filteredCells = new ArrayList<ICell>();
			for(ICell c : this.getCells()){
				//			  Nucleus n = c.getNucleus();

				double value = getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, c);

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

		for(ICell cell : filteredCells){
			subCollection.addCell(new DefaultCell(cell));
		}

		this.getProfileManager().copyCollectionOffsets(subCollection);

		this.getSignalManager().copySignalGroups(subCollection);

		return subCollection;
	}


	/**
	 * Return a collection of cells present in both collections
	 * @param other the other collection
	 * @return
	 * @throws Exception 
	 */
	public ICellCollection and(ICellCollection other) {

		ICellCollection newCollection = new DefaultCellCollection(this, "AND operation");

		for(ICell c : other.getCells()){

			if(this.contains(c)){
				newCollection.addCell(new DefaultCell(c));
			}
		}

		return newCollection;
	}

	/**
	 * Return a collection of cells present this collection but not the other
	 * @param other the other collection
	 * @return
	 * @throws Exception 
	 */
	public ICellCollection not(ICellCollection other) {

		ICellCollection newCollection = new DefaultCellCollection(this, "NOT operation");

		for(ICell c : getCells()){

			if( ! other.contains(c)){
				newCollection.addCell(new DefaultCell(c));
			}
		}

		return newCollection;
	}

	/**
	 * Return a collection of cells present this collection or the other but not both
	 * @param other the other collection
	 * @return
	 * @throws Exception 
	 */
	public ICellCollection xor(ICellCollection other) {

		ICellCollection newCollection = new DefaultCellCollection(this, "XOR operation");

		for(ICell c : getCells()){

			if( ! other.contains(c)){
				newCollection.addCell(new DefaultCell(c));
			}
		}

		for(ICell c : other.getCells()){

			if( ! this.contains(c)){
				newCollection.addCell(new DefaultCell(c));
			}
		}

		return newCollection;
	}

	/**
	 * Invalidate the existing cached vertically rotated nuclei,
	 and recalculate.
	 */
	public void updateVerticalNuclei(){

		getNuclei().parallelStream().forEach( n -> {
			n.updateVerticallyRotatedNucleus();
		});

	}

	public boolean updateSourceFolder(File newFolder) {
		File oldFile = this.getFolder();
		boolean ok = false;

		if(newFolder.exists()){

			try {
				this.folder = newFolder;

				for(Nucleus n : this.getNuclei()){
					n.updateSourceFolder(newFolder);
				}
				ok = true;

			} catch (IllegalArgumentException e){
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

	public synchronized boolean isRefolding(){
		return this.isRefolding;
	}

	public synchronized void setRefolding(boolean b){
		this.isRefolding = b;
	}

	/**
	 * Test if the collection contains a cell with
	 * the same id as the given cell
	 * @param c
	 * @return
	 */
	public boolean contains(ICell c){
		for(ICell cell : this.getCells()){
			if (cell.equals(c)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Test if the collection contains the given cell
	 * (this must be the same object, not just a cell
	 * with the same id)
	 * @param c
	 * @return
	 */
	public boolean containsExact(ICell c){
		for(ICell cell : this.getCells()){
			if (cell==c){
				return true;
			}
		}
		return false;
	}

	/**
	 * Fetch the signal group ids in this collection
	 * @param id
	 * @return
	 */
	public Set<UUID> getSignalGroupIDs(){
		return this.signalGroups.keySet();
	}

	/**
	 * Fetch the signal groups in this collection
	 * @param id
	 * @return
	 */
	public Collection<ISignalGroup> getSignalGroups(){
		return this.signalGroups.values();
	}

	/**
	 * Fetch the signal group with the given ID
	 * @param id
	 * @return
	 */
	public ISignalGroup getSignalGroup(UUID id){
		if(this.signalGroups.get(id)==null){
			return null; // placeholder
		}
		return this.signalGroups.get(id);
	}

	public void addSignalGroup(UUID id, ISignalGroup group){
		this.signalGroups.put(id, group);
	}

	public boolean hasSignalGroup(UUID id){
		return this.signalGroups.containsKey(id);
	}

	/**
	 * Remove the given group
	 * @param id
	 */
	public void removeSignalGroup(UUID id){
		this.signalGroups.remove(id);
	}




	/**
	 * Get the RuleSetCollection with the index finding rules for this nucleus type
	 * @return
	 */
	public RuleSetCollection getRuleSetCollection(){
		return this.ruleSets;
	}

	/**
	 * Get the number of nuclei shared with the given dataset
	 * @param d2
	 * @return
	 */
	public int countShared(IAnalysisDataset d2){

		return countShared(d2.getCollection());

	}

	/**
	 * Get the number of nuclei shared with the given dataset
	 * @param d2
	 * @return
	 */
	public int countShared(ICellCollection d2){

		if(this.vennCache.containsKey(d2.getID())){
			return vennCache.get(d2.getID());
		}
		int shared  = countSharedNuclei(d2);
		vennCache.put(d2.getID(), shared);
		d2.countShared(this);
		return shared;

	}
	
	/**
	 * Count the number of nuclei from this dataset that are present in d2
	 * @param d1
	 * @param d2
	 * @return
	 */
	private int countSharedNuclei(ICellCollection d2){

		if(d2==this){
			cells.size();
		}

		if(d2.getNucleusType() != this.nucleusType){
			return 0;
		}


		int shared = 0;
		for(ICell c : cells){
			UUID n1id = c.getNucleus().getID();

			for(Nucleus n2 : d2.getNuclei()){
				if( n2.getID().equals(n1id)){
					shared++;
				}
			}
		}
		return shared;
	}

	public int countClockWiseRPNuclei(){
		int count=0;
		for(Nucleus n : getNuclei()){
			if(n.isClockwiseRP()){
				count++;
			}
		}
		return count;
	}

	public String toString(){

		String newLine = System.getProperty("line.separator");
		StringBuilder b = new StringBuilder();

		b.append("Collection:" + getName() + newLine);
		b.append("Nuclei: "+ this.getNucleusCount() + newLine);

		b.append("Clockwise: "+ this.countClockWiseRPNuclei() + newLine);

		b.append("Source folder: "+this.getFolder().getAbsolutePath()+newLine);
		b.append("Nucleus type: "+this.nucleusType+newLine);
		b.append("Profile collections:"+newLine);
		
		IProfileCollection pc = this.getProfileCollection();
		b.append( pc.toString()+ newLine);
//		for(ProfileType type : ProfileType.values() ){
//			b.append("Profile type: "+type+newLine);
//			
//			b.append( pc.toString()+ newLine);
//		}

		b.append( this.ruleSets.toString()+newLine);

		b.append("Signal groups:"+newLine);
		for(UUID signalGroupID : this.signalGroups.keySet() ){
			ISignalGroup group = this.signalGroups.get(signalGroupID);
			int count = this.getSignalManager().getSignalCount(signalGroupID);
			b.append( signalGroupID.toString()+": "+group.toString()+" | "+count+newLine);
		}

		return b.toString();
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

		in.defaultReadObject();
		isRefolding = false;
		vennCache   = new HashMap<UUID, Integer>(); // cache the number of shared nuclei with other datasets

		if(ruleSets==null || ruleSets.isEmpty()){
			log("Creating default ruleset for collection");
			ruleSets = RuleSetCollection.createDefaultRuleSet(nucleusType); 
		}
		
		statsCache = new StatsCache();
		
		signalManager  = new SignalManager(this);
		profileManager = new ProfileManager(this);
		
		this.profileCollection.createProfileAggregate(this, getMedianArrayLength());
	
	}




	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((consensusNucleus == null) ? 0 : consensusNucleus.hashCode());
		result = prime * result + ((folder == null) ? 0 : folder.hashCode());
		result = prime * result
				+ ((cells == null) ? 0 : cells.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((nucleusType == null) ? 0 : nucleusType.hashCode());
		result = prime * result
				+ ((outputFolder == null) ? 0 : outputFolder.hashCode());
		result = prime
				* result
				+ ((profileCollection == null) ? 0 : profileCollection.hashCode());
		result = prime * result + ((ruleSets == null) ? 0 : ruleSets.hashCode());
		result = prime * result
				+ ((signalGroups == null) ? 0 : signalGroups.hashCode());
		result = prime * result
				+ ((statsCache == null) ? 0 : statsCache.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultCellCollection other = (DefaultCellCollection) obj;
		if (consensusNucleus == null) {
			if (other.consensusNucleus != null)
				return false;
		} else if (!consensusNucleus.equals(other.consensusNucleus))
			return false;
		if (folder == null) {
			if (other.folder != null)
				return false;
		} else if (!folder.equals(other.folder))
			return false;
		if (cells == null) {
			if (other.cells != null)
				return false;
		} else if (!cells.equals(other.cells))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (nucleusType != other.nucleusType)
			return false;
		if (outputFolder == null) {
			if (other.outputFolder != null)
				return false;
		} else if (!outputFolder.equals(other.outputFolder))
			return false;
		if (profileCollection == null) {
			if (other.profileCollection != null)
				return false;
		} else if (!profileCollection.equals(other.profileCollection))
			return false;
		if (ruleSets == null) {
			if (other.ruleSets != null)
				return false;
		} else if (!ruleSets.equals(other.ruleSets))
			return false;
		if (signalGroups == null) {
			if (other.signalGroups != null)
				return false;
		} else if (!signalGroups.equals(other.signalGroups))
			return false;
		if (statsCache == null) {
			if (other.statsCache != null)
				return false;
		} else if (!statsCache.equals(other.statsCache))
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}




	/**
	 * Store plottable statistics for the collection
	 * @author bms41
	 *
	 */
	private class StatsCache {

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

				finest("Fetching cached stat: "+stat);
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
	}

}