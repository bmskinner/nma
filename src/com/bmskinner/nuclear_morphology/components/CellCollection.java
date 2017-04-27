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

package com.bmskinner.nuclear_morphology.components;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileManager;
import com.bmskinner.nuclear_morphology.analysis.profiles.Taggable;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SegmentStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.stats.Quartile;

/**
 * @author bms41
 * @deprecated since 1.13.3
 *
 */
@Deprecated
public class CellCollection implements ICellCollection {

	private static final long serialVersionUID = 1L;

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

	private Nucleus consensusNucleus; 	// the refolded consensus nucleus

	private Map<UUID, Cell> mappedCollection  = new HashMap<UUID, Cell>();	// store all the nuclei analysed

	private Map<UUID, SignalGroup> signalGroups = new HashMap<UUID, SignalGroup>(0);


	private volatile transient boolean isRefolding = false;

	private RuleSetCollection ruleSets = new RuleSetCollection();

	/*
	 * Cache the number of shared cells with other datasets.
	 * The UUID is the other dataset ID, and the int is the number of shared cells
	 */
	protected transient Map<UUID, Integer> vennCache = new HashMap<UUID, Integer>();

	/**
	 * Constructor.
	 * @param folder the folder of images
	 * @param outputFolder a name for the outputs (usually the analysis date). Can be null
	 * @param name the name of the collection
	 * @param nucleusClass the class of nucleus to be held
	 */
	private CellCollection(File folder, String outputFolder, String name, NucleusType nucleusType){
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
	private CellCollection(File folder, String outputFolder, String name, NucleusType nucleusType, UUID id){

		this.uuid         = id;
		this.folder       = folder;
		this.outputFolder = outputFolder;
		this.name         = name == null ? folder.getName() : name;// if name is null, use the image folder name
		this.nucleusType  = nucleusType;

		for(ProfileType type : ProfileType.values()){
			profileCollections.put(type, new ProfileCollection());
		}

		ruleSets = RuleSetCollection.createDefaultRuleSet(nucleusType); 

	}

	/**
	 * Construct an empty collection from a template dataset
	 * @param template the dataset to base on for folders and type
	 * @param name the collection name
	 */
	private CellCollection(IAnalysisDataset template, String name){

		this(template.getCollection(), name );
	}

	/**
	 * Construct an empty collection from a template collection
	 * @param template
	 * @param name
	 */
	private CellCollection(ICellCollection template, String name){
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
	
	public boolean isReal(){
		return true;
	}
	
	public boolean isVirtual(){
		return false;
	}

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
		return new HashSet<UUID>( mappedCollection.keySet());
	}

	public void addCell(ICell r) {
		if(r == null){
			throw new IllegalArgumentException("Cell is null");
		}

		if(r instanceof Cell){
			if(mappedCollection.containsKey(r.getId())){
				return;
			} else {
				this.mappedCollection.put(r.getId(), (Cell) r);
			}
		} else {
			throw new IllegalArgumentException("Can only add Cell to this collection type");
		}

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


		if(r instanceof Cell){
			if( ! mappedCollection.containsKey(r.getId())){
				return;
			} else {
				this.mappedCollection.put(r.getId(), (Cell) r);
			}
		} else {
			throw new IllegalArgumentException("Can only add Cell to this collection type");
		}
	}

	/**
	 * Remove the given cell from the collection. If the cell is
	 * null, has no effect. If the cell is not in the collection, has
	 * no effect. 
	 * @param c the cell to remove
	 */
	public void removeCell(ICell c) {

		if(c == null){
			return;
		}
		this.mappedCollection.remove(c.getId());
	}

	public int size(){
		return mappedCollection.size();
	}

	public boolean hasConsensus(){
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
	
	@Override
	public boolean hasCells(File imageFile){
		return getCells(imageFile).size()>0;
	}
	
	@Override
	public boolean hasNuclei(File imageFile){
		return getNuclei(imageFile).size()>0;
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

	public void setConsensus(Nucleus n){
		this.consensusNucleus = n;
	}


	/**
	 * Get the cell with the given UUID
	 * @param id
	 * @return
	 */
	public ICell getCell(UUID id){
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
	public ICell getCell(String path){

		for(ICell c : mappedCollection.values()){
			Nucleus n = c.getNucleus();
			if(n.getPathAndNumber().equals(path)){
				return c;
			}
		}
		return null;
	}

	public Nucleus getConsensus(){
		return this.consensusNucleus;
	}

	/**
	 * Get the profile collection of the given type
	 * @param type
	 * @return
	 */
	public IProfileCollection getProfileCollection(ProfileType type){
		if(this.profileCollections.containsKey(type)){
			return this.profileCollections.get(type);
		} else {
			throw new IllegalArgumentException("ProfileCollection key "+type.toString()+" not present");
		}
	}

	public void setProfileCollection(ProfileType type, IProfileCollection p){
		this.profileCollections.put(type, (ProfileCollection) p);
	}
	
	public void createProfileCollection(){
		/*
		 * Build a first set of profile aggregates
		 * Default is to make profile aggregate from reference point
		 * Do not build an aggregate for the non-existent frankenprofile
		 */
		IProfileCollection pc = getProfileCollection();
//		pc.createProfileAggregate(this, pc.length());
		
		
		for(ProfileType type : ProfileType.values()){
			
			if(type.equals(ProfileType.FRANKEN)){
				continue;
			}
			
			fine("Creating profile aggregate: "+type);
			
			int length = pc.length();
						
			if(length>0){ // failsafe in case some idiot (me) tries to maintain length on an empty aggregate
				
			
				finer(type+" length before update: "+pc.length());

				pc.createProfileAggregate(this, length);

				finer(type+" length after update: "+pc.length());
			} else {
				pc.createProfileAggregate(this, this.getMedianArrayLength());
			}
		}
	}

	/**
	 * Remove the given profile collection
	 * @param type
	 */
	public void removeProfileCollection(ProfileType type){
		this.profileCollections.remove(type);
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
//	private double[] getPathLengths() {
//
//		int count = this.getNucleusCount();
//		double[] result = new double[count];
//		int i=0;
//		for(ICell cell : getCells() ){ 
//			Nucleus n = cell.getNucleus();
//			try {
//				result[i] =  n.getPathLength(ProfileType.ANGLE);
//			} catch (UnavailableProfileTypeException e) {
//				result[i] = 0;
//			}
//			i++;
//		}
//		return result;
//	}

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

//	public int[][] getPositions(){
//		int[][] s = new int[size()][4];
//		int i = 0;
//		for(ICell cell : getCells() ){ 
//			Nucleus n = cell.getNucleus();
//			s[i] = n.getPosition();
//			i++;
//		}
//		return s;
//	}

	public int getNucleusCount(){
		return this.size();
	}

	//  public Iterator<Cell> getCellIterator(){
		//	  return mappedCollection.values().iterator();
	//  }


	/**
	 * Get the cells in this collection
	 * @return
	 */
	public Set<ICell> getCells(){
		Set<ICell> result = new HashSet<ICell>(mappedCollection.size());
		for(ICell cell : mappedCollection.values()){
			result.add(cell);
		}
		return result;
	}

	/**
	 * Get the cells in this collection
	 * @return
	 */
	public Set<ICell> getCells(File imageFile){
		Set<ICell> result = new HashSet<ICell>(mappedCollection.size());
		for(ICell cell : this.getCells()){
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
	public List<ICell> getCellsNotIn(CellCollection collection){

		List<ICell> result = new ArrayList<ICell>(0);
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
		Set<Nucleus> result = new HashSet<Nucleus>(mappedCollection.size());
		for(ICell c : this.getCells()){
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
		Set<Nucleus> result = new HashSet<Nucleus>(mappedCollection.size());
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

//	public double getMedianPathLength() {
//
//		if(size()==0){
//			return 0;
//		}
//
//		double[] p = this.getPathLengths();
//		double median = new Quartile(p, Quartile.MEDIAN).doubleValue();
//		return median;
//	}

	public int getMedianArrayLength(){
		if(size()==0){
			return 0;
		}

		int[] p = this.getArrayLengths();
		double median = new Quartile(p, Quartile.MEDIAN).doubleValue();
		return (int) median;
	}

	public int getMaxProfileLength(){

		return Arrays.stream(this.getArrayLengths()).max().orElse(0);
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
		IProfileCollection pc = this.getProfileCollection(ProfileType.ANGLE);
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
	 * @throws UnavailableBorderTagException 
	 */
	public double[] getDifferencesToMedianFromPoint(Tag pointType) throws UnavailableBorderTagException {

		int count = this.getNucleusCount();
		double[] result = new double[count];
		int i=0;

		IProfile medianProfile = profileCollections.get(ProfileType.ANGLE).getProfile(pointType, Quartile.MEDIAN);
		for(Nucleus n : this.getNuclei()){
			
			try {
				IProfile angleProfile = n.getProfile(ProfileType.ANGLE);
				result[i] = angleProfile.offset(n.getBorderIndex(pointType)).absoluteSquareDifference(medianProfile);
			} catch (ProfileException | UnavailableProfileTypeException e) {
				fine("Error getting angle profile", e);
				result[i] = 0;
			} finally{
				i++;
			}
		}
		return result;
	}

	/**
	 * Get the differences to the median profile for each nucleus, normalised to the
	 * perimeter of the nucleus. This is the sum-of-squares difference, rooted and divided by
	 * the nuclear perimeter
	 * @param pointType the point to fetch profiles from
	 * @return an array of normalised differences
	 * @throws UnavailableBorderTagException 
	 */
	public double[] getNormalisedDifferencesToMedianFromPoint(BorderTagObject pointType) throws UnavailableBorderTagException {
		//	  List<Double> list = new ArrayList<Double>();
		int count = this.getNucleusCount();
		double[] result = new double[count];
		int i=0;
		IProfile medianProfile = profileCollections.get(ProfileType.ANGLE).getProfile(pointType, Quartile.MEDIAN);

		for(Nucleus n : this.getNuclei()){

			try {
			
				IProfile angleProfile = n.getProfile(ProfileType.ANGLE, pointType);
				double diff = angleProfile.absoluteSquareDifference(medianProfile);		
				diff /= n.getStatistic(PlottableStatistic.PERIMETER, MeasurementScale.PIXELS); // normalise to the number of points in the perimeter (approximately 1 point per pixel)
				double rootDiff = Math.sqrt(diff); // use the differences in degrees, rather than square degrees  
				result[i] = rootDiff;
			
			} catch(ProfileException | UnavailableProfileTypeException e){
				fine("Error getting angle profile", e);
				result[i] = Double.MAX_VALUE;
			} finally{
				i++;
			}

		}
		return result;
	}

	/**
	 * Get the perimeter normalised veriabililty of a nucleus angle profile compared to the
	 * median profile of the collection
	 * @param pointType the tag to use as index 0
	 * @param c the cell to test
	 * @return the variabililty score of the nucleus
	 * @throws UnavailableBorderTagException 
	 * @throws Exception
	 */
	@Override
	public double getNormalisedDifferenceToMedian(Tag pointType, Taggable t) throws UnavailableBorderTagException{
		
		IProfile medianProfile = profileCollections.get(ProfileType.ANGLE).getProfile(pointType, Quartile.MEDIAN);
		IProfile angleProfile;
		
		try {
			angleProfile = t.getProfile(ProfileType.ANGLE, pointType);

			double diff = angleProfile.absoluteSquareDifference(medianProfile);		
			diff /= t.getStatistic(PlottableStatistic.PERIMETER, MeasurementScale.PIXELS); // normalise to the number of points in the perimeter (approximately 1 point per pixel)
			double rootDiff = Math.sqrt(diff); // use the differences in degrees, rather than square degrees  
			return rootDiff;
		} catch (ProfileException | UnavailableComponentException e) {
			stack("Cannot get angle profile", e);
			return Double.MAX_VALUE;
		}
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
			try {
				result[i++] = n.getBorderPoint(pointTypeA).getLengthTo(n.getBorderPoint(pointTypeB));
			} catch (UnavailableBorderTagException e) {
				fine("Tag not present: "+pointTypeA+" or "+pointTypeB);
			}
		}
		return result;
	}

	/**
	 * Get the nucleus with the lowest difference score to the median profile
	 * @param pointType the point to compare profiles from
	 * @return the best nucleus
	 * @throws ProfileException 
	 * @throws UnavailableBorderTagException 
	 * @throws UnavailableProfileTypeException 
	 */
	public Nucleus getNucleusMostSimilarToMedian(Tag pointType) throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException {

		if(this.size()==1){
			for(ICell c : this.mappedCollection.values()){
				return c.getNucleus();
			}
		}

		IProfile medianProfile = profileCollections.get(ProfileType.ANGLE).getProfile(pointType, Quartile.MEDIAN); // the profile we compare the nucleus to
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
	
	@Override
	public void clear(PlottableStatistic stat, String component){
//		statsCache.clear(stat, component);
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
		//	  return getMedianStatistic(stat, scale, signalGroup, null);
	}

	//  public double getMedianStatistic(PlottableStatistic stat, MeasurementScale scale, UUID id)  throws Exception {
	//	  return getMedianStatistic(stat, scale, null, id);
	//  }

	/**
	 * Get the median value of the given statistic
	 * @param stat
	 * @param scale
	 * @return
	 * @throws Exception
	 */
	private double getMedianNucleusStatistic(PlottableStatistic stat, MeasurementScale scale)  throws Exception {

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
	private double[] getNuclearStatistics(PlottableStatistic stat, MeasurementScale scale) {

		double[] result = null;
		
		if(PlottableStatistic.VARIABILITY.equals(stat)){
			try {
				result = this.getNormalisedDifferencesToMedianFromPoint(Tag.REFERENCE_POINT);
			} catch (UnavailableBorderTagException e) {
				result = new double[this.size()];
				for(int i=0; i<size(); i++){
					result[i] = Double.MAX_VALUE;
				}
			}
		} else {


			result = this.getNuclei().parallelStream()
					.mapToDouble( n -> n.getStatistic(stat, scale)  )
					.toArray();
		
			Arrays.sort(result);

//			finest("Making statistic fetching task for "+stat);
////			NucleusStatisticFetchingTask task = new NucleusStatisticFetchingTask(getNucleusArray(),
////					stat,
////					scale);
////			result = task.invoke();
//			finest("Fetched statistic result for "+stat);

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

		if(mappedCollection.size()==0){
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

		
		double[] result = getNuclei().parallelStream()
				.mapToDouble( n-> {
						IBorderSegment segment;
						try {
							segment = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegment(id);
						} catch (ProfileException | UnavailableComponentException e) {
							stack(e);
							return 0;
						}
						double perimeterLength = 0;
						if(segment!=null){
							int indexLength = segment.length();
							double fractionOfPerimeter = (double) indexLength / (double) segment.getTotalLength();
							perimeterLength = fractionOfPerimeter * n.getStatistic(PlottableStatistic.PERIMETER, scale);
						}
						return perimeterLength;
		
					})
				.toArray();
			Arrays.sort(result);
			return result;
//		SegmentStatisticFetchingTask task = new SegmentStatisticFetchingTask(getNucleusArray(),
//				stat,
//				scale, 
//				id);
//		return task.invoke();
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
	public ICellCollection filterCollection(PlottableStatistic stat, MeasurementScale scale, double lower, double upper) {
		DecimalFormat df = new DecimalFormat("#.##");
		CellCollection subCollection = new CellCollection(this, "Filtered_"+stat.toString()+"_"+df.format(lower)+"-"+df.format(upper));

		List<ICell> filteredCells;

		if(stat.equals(PlottableStatistic.VARIABILITY)){
			filteredCells = new ArrayList<ICell>();
			for(ICell c : this.getCells()){

				double value;
				try {
					value = getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, c.getNucleus());
				} catch (UnavailableBorderTagException e) {
					warn("Cannot get variability score");
					fine("Error getting difference to median profile", e);
					value = Double.MAX_VALUE;
				}

				if(value>= lower && value<= upper){
					filteredCells.add(c);
				}  
			}

		} else {
			filteredCells = getCells()
					.parallelStream()
					.filter(p -> p.getNucleus().getStatistic(stat, scale) >= lower)
					.filter(p -> p.getNucleus().getStatistic(stat, scale) <= upper)
					.collect(Collectors.toList());
		}

		for(ICell cell : filteredCells){
			subCollection.addCell(new DefaultCell(cell));
		}

		try {
			this.getProfileManager().copyCollectionOffsets(subCollection);
		} catch (ProfileException e) {
			warn("Error copying collection offsets");
			fine("Error in offsetting", e);
		}

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

		CellCollection newCollection = new CellCollection(this, "AND operation");

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

		CellCollection newCollection = new CellCollection(this, "NOT operation");

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

		CellCollection newCollection = new CellCollection(this, "XOR operation");

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
	 * Return a collection of cells present this collection or the other
	 * @param other the other collection
	 * @return
	 * @throws Exception 
	 */
	public ICellCollection or(ICellCollection other) {

		CellCollection newCollection = new CellCollection(this, "OR operation");

		for(ICell c : getCells()){

			newCollection.addCell(new DefaultCell(c));
			
		}

		for(ICell c : other.getCells()){
			
			if(! this.contains(c)){
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

		Collection<ISignalGroup> result = new ArrayList<ISignalGroup>(signalGroups.size());

		for(ISignalGroup s : signalGroups.values()){
			result.add(s);
		}
		return result;

		//      return this.signalGroups.values();
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

		if( !( group instanceof SignalGroup)){
			throw new IllegalArgumentException("Cannot cast to signal group");
		}
		this.signalGroups.put(id, (SignalGroup) group);
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
//		d2.countShared(this);
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
			return size();
		}

		if(d2.getNucleusType() != this.nucleusType){
			return 0;
		}
				
		Set<UUID> toSearch1 = this.getCellIDs();
		Set<UUID> toSearch2 = d2.getCellIDs();
		
		finest("Beginning search for shared cells");

		// choose the smaller to search within
		
		int shared = 0;
		for(UUID id1 : toSearch1){
			
			Iterator<UUID> it = toSearch2.iterator();
			
			while(it.hasNext()){
				UUID id2 = it.next();
				
				if(id1.equals(id2)){
					it.remove();
					shared++;
					break;
				}
			}
			
		}	
		finest("Completed search for shared cells");
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

		for(ProfileType type : ProfileType.values() ){
			b.append("Profile type: "+type+newLine);
			IProfileCollection pc = this.getProfileCollection(type);
			b.append( pc.toString()+ newLine);
		}

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
		//	  finest("Writing cell collection");
		out.defaultWriteObject();
		//	  finest("Wrote cell collection");
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		//	  finest("Reading cell collection");
		in.defaultReadObject();
		isRefolding = false;
		vennCache   = new HashMap<UUID, Integer>(); // cache the number of shared nuclei with other datasets

		if(ruleSets==null || ruleSets.isEmpty()){
			log("Creating default ruleset for collection");
			ruleSets = RuleSetCollection.createDefaultRuleSet(nucleusType); 
		}
		//	  finest("Creating default ruleset for nucleus type "+nucleusType);
		//	  ruleSets = RuleSetCollection.createDefaultRuleSet(nucleusType); 
		//	  finest("Read cell collection");
	}




	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((consensusNucleus == null) ? 0 : consensusNucleus.hashCode());
		result = prime * result + ((folder == null) ? 0 : folder.hashCode());
		result = prime * result
				+ ((mappedCollection == null) ? 0 : mappedCollection.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((nucleusType == null) ? 0 : nucleusType.hashCode());
		result = prime * result
				+ ((outputFolder == null) ? 0 : outputFolder.hashCode());
		result = prime
				* result
				+ ((profileCollections == null) ? 0 : profileCollections.hashCode());
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
		CellCollection other = (CellCollection) obj;
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
		if (mappedCollection == null) {
			if (other.mappedCollection != null)
				return false;
		} else if (!mappedCollection.equals(other.mappedCollection))
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
		if (profileCollections == null) {
			if (other.profileCollections != null)
				return false;
		} else if (!profileCollections.equals(other.profileCollections))
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
	public class StatsCache implements Serializable {

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




	@Override
	public IProfileCollection getProfileCollection() {
		// TODO Auto-generated method stub
		return profileCollections.get(ProfileType.ANGLE);
	}

	@Override
	public void setSharedCount(ICellCollection d2, int i) {
		vennCache.put(d2.getID(), i);
		
	}

	@Override
	public boolean contains(UUID cellID) {
		return this.getCellIDs().contains(cellID);
	}

	@Override
	public double getMedianStatistic(PlottableStatistic stat, String component,
			MeasurementScale scale) throws Exception {
		warn("Unimplemented method in "+this.getClass().getName());
		return 0;
	}

	@Override
	public double[] getMedianStatistics(PlottableStatistic stat,
			String component, MeasurementScale scale) {
		warn("Unimplemented method in "+this.getClass().getName());
		return null;
	}

	@Override
	public double[] getMedianStatistics(PlottableStatistic stat,
			String component, MeasurementScale scale, UUID id) {
		warn("Unimplemented method in "+this.getClass().getName());
		return null;
	}

	@Override
	public double getMedianStatistic(PlottableStatistic stat, String component,
			MeasurementScale scale, UUID id) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

}