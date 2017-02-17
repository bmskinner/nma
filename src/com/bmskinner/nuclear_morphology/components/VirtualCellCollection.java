/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.components;

import java.awt.Color;
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

import com.bmskinner.nuclear_morphology.analysis.NucleusStatisticFetchingTask;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileManager;
import com.bmskinner.nuclear_morphology.analysis.profiles.SegmentStatisticFetchingTask;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.DefaultProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SegmentStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.components.stats.StatsCache;
import com.bmskinner.nuclear_morphology.stats.Quartile;

/**
 * This class provides access to child dataset ICell lists
 * and statistics
 * @author ben
 * @since 1.13.3
 *
 */
public class VirtualCellCollection implements ICellCollection {
	
	private static final long serialVersionUID = 1L;
	
	private final IAnalysisDataset parent;
	
	private final Set<UUID> cellIDs = new HashSet<UUID>(0);

	private final UUID 	uuid;			// the collection id

	private String 	    name;			// the name of the collection

	//this holds the mapping of tail indexes etc in the median profile arrays
	private IProfileCollection profileCollection = new DefaultProfileCollection();
	
//	protected final Map<ProfileType, IProfileCollection> profileCollections = new HashMap<ProfileType, IProfileCollection>();

	private Nucleus consensusNucleus; 	// the refolded consensus nucleus
	
	// We need to store signal groups separately to allow shell results etc to be kept
	private Map<UUID, IShellResult> shellResults = new HashMap<UUID, IShellResult>(0);
	
	/*
	 * TRANSIENT FIELDS
	 */
	
	private transient boolean isRefolding = false;
	
	protected volatile transient Map<UUID, Integer> vennCache = new HashMap<UUID, Integer>();
	
	private transient ProfileManager profileManager = new ProfileManager(this);
	private transient SignalManager  signalManager  = new SignalManager(this); //TODO: integrate
	
	private volatile transient StatsCache statsCache = new StatsCache();
	
	/**
	 * Create from a parent dataset, and provide a name
	 * @param parent
	 * @param name
	 */
	public VirtualCellCollection(IAnalysisDataset parent, String name){
		this(parent, name, java.util.UUID.randomUUID() );
	}
	
	/**
	 * Create from a parent dataset, and provide a name and UUID
	 * @param parent
	 * @param name
	 * @param id
	 */
	public VirtualCellCollection(IAnalysisDataset parent, String name, UUID id){
		this.parent = parent;
		this.name = name == null ? "Undefined dataset name" : name;
		this.uuid = id;
		
		// Add the signal groups from the parent
		
//		for(UUID signalGroup : parent.getCollection().getSignalGroupIDs()){
//			try {
//				ISignalGroup group = parent.getCollection().getSignalGroup(signalGroup);
//				
//				if(group.hasShellResult()){
//					shellResults.put(signalGroup, group.)
//				}
//				ISignalGroup newGroup = new SignalGroup(group);
//				newGroup.setShellResult(null);
//				this.addSignalGroup(signalGroup, newGroup);
//				
//			} catch (UnavailableSignalGroupException e) {
//				stack("Error copying signal group to virtual collection", e);
//			}
//		}
	}
	
	/**
	 * Create from a parent dataset, spcifying the collection name and id, and providing a 
	 * collection of cells to populate the new collection.
	 * @param parent the dataset to which this will belong
	 * @param name the name of the collection
	 * @param id the id of the collection
	 * @param cells the collection of cells to add to this collection
	 */
	public VirtualCellCollection(IAnalysisDataset parent, String name, UUID id, ICellCollection cells){
		this(parent, name, id);
		for(ICell cell : cells.getCells()){
			this.addCell(cell);
		}
	}
	
	public IAnalysisDataset getParent(){
		return parent;
	}


	@Override
	public void setName(String s) {
		this.name = s;
		
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public UUID getID() {
		return uuid;
	}
	
	public boolean isReal(){
		return false;
	}
	
	public boolean isVirtual(){
		return true;
	}

	@Override
	public synchronized Set<ICell> getCells() {
		
		Set<ICell> result = new HashSet<ICell>(cellIDs.size());
		ICellCollection parentCollection = parent.getCollection();
		if(parentCollection == null){
			warn("Cannot access parent collection");
			return result;
		}
		for(ICell cell : parentCollection.getCells()){
			if(cellIDs.contains(cell.getId())){
				result.add(cell);
			}
		}
		return result;
	}

	@Override
	public synchronized Set<ICell> getCells(File f) {
		Set<ICell> result = new HashSet<ICell>(cellIDs.size());
		
		for(ICell cell : parent.getCollection().getCells()){
			if(cellIDs.contains(cell.getId())){
				if(cell.getNucleus().getSourceFile().equals(f)){
					result.add(cell);
				}
			}
		}
		return result;
	}

	@Override
	public synchronized Set<UUID> getCellIDs() {
		return new HashSet<UUID>(cellIDs);
	}

	@Override
	public synchronized Set<Nucleus> getNuclei() {
		Set<Nucleus> result = new HashSet<Nucleus>(cellIDs.size());
		
		ICellCollection parentCollection = parent.getCollection();
		
		if(parentCollection==null){
			warn("Parent collection not restored!");
		}
		
		for(UUID id : cellIDs){
			ICell c = parentCollection.getCell(id);
			Nucleus n = c.getNucleus();
			result.add(n);
		}

		return result;
	}

	@Override
	public synchronized Set<Nucleus> getNuclei(File imageFile) {
		Set<Nucleus> result = new HashSet<Nucleus>(cellIDs.size());
		for(UUID id : cellIDs){
			Nucleus n = parent.getCollection().getCell(id).getNucleus();
			
			if(n.getSourceFile().equals(imageFile)){
				result.add(n);
			}
		}

		return result;
	}

	@Override
	public void addCell(ICell c) {
		if( ! parent.getCollection().contains(c.getId())){
			throw new IllegalArgumentException("Parent does not contain cell");
		}
		cellIDs.add(c.getId());
		
	}

	@Override
	public void replaceCell(ICell c) {}

	@Override
	public ICell getCell(UUID id) {
		if(cellIDs.contains(id)){
			return parent.getCollection().getCell(id);
		}
		return null;
	}

	@Override
	public NucleusType getNucleusType() {
		return parent.getCollection().getNucleusType();
	}

	@Override
	public void removeCell(ICell c) {
		cellIDs.remove(c.getId());
		
	}

	@Override
	public int size() {
		return cellIDs.size();
	}

	@Override
	public boolean hasConsensusNucleus() {
		return this.consensusNucleus!=null;
	}

	@Override
	public void setConsensusNucleus(Nucleus n) {
		consensusNucleus = n;
	}

	@Override
	public Nucleus getConsensusNucleus() {
		return consensusNucleus;
	}

	@Override
	public void setRefolding(boolean b) {
		isRefolding = b;
	}

	@Override
	public boolean isRefolding() {
		return isRefolding;
	}

	@Override
	public boolean hasCells() {
		return !cellIDs.isEmpty();
	}

	@Override
	public boolean contains(ICell cell) {
		return cellIDs.contains(cell.getId());
	}
	
	@Override
	public boolean contains(UUID cellID) {
		return cellIDs.contains(cellID);
	}

	@Override
	public boolean containsExact(ICell cell) {
		return parent.getCollection().containsExact(cell);
	}

	@Override
	public boolean hasLockedCells() {
		return parent.getCollection().hasLockedCells();
	}

	@Override
	public void setCellsLocked(boolean b) {}

	@Override
	public IProfileCollection getProfileCollection() {
		return profileCollection;
	}
	
	/**
	 * Create the profile collections to hold angles from nuclear
	 * profiles based on the current nucleus profiles. The ProfileAggregate
	 * for each ProfileType is recalculated. The resulting median profiles
	 * will have the same length at the parent collection after this update
	 * @return
	 */
	public void createProfileCollection() {
		profileCollection.createProfileAggregate(this, parent.getCollection().getMedianArrayLength());
	}
//
//	@Override
//	public void setProfileCollection(ProfileType type, IProfileCollection p) {
//		profileCollections.put(type, p);
//		
//	}
//
//	@Override
//	public void removeProfileCollection(ProfileType type) {
//		profileCollections.remove(type);
//		
//	}

	@Override
	public File getFolder() {
		return parent.getCollection().getFolder();
	}

	@Override
	public String getOutputFolderName() {
		return parent.getCollection().getOutputFolderName();
	}

	@Override
	public File getOutputFolder() {
		return parent.getCollection().getOutputFolder();
	}

	@Override
	public Set<File> getImageFiles() {
		
		Set<File> result = new HashSet<File>(cellIDs.size());
		
		for(ICell c : getCells()){
			result.add(c.getNucleus().getSourceFile());
		}
		return result;
	}

	@Override
	public Set<UUID> getSignalGroupIDs() {
		return parent.getCollection().getSignalGroupIDs();
	}

	@Override
	public void removeSignalGroup(UUID id) {
		shellResults.remove(id);
	}

	@Override
	public ISignalGroup getSignalGroup(UUID signalGroup) throws UnavailableSignalGroupException {
		
		// Override the shell storage to point to this collection, not the shell result
		// This SignalGroup is never saved to file, so does not need serialising
		@SuppressWarnings("serial")
		ISignalGroup result = new SignalGroup(parent.getCollection().getSignalGroup(signalGroup)){
			
			@Override
			public void setShellResult(IShellResult result){
				shellResults.put(signalGroup, result);			
			}
			
			@Override
			public boolean hasShellResult(){
				return shellResults.containsKey(signalGroup);
			}
			
			@Override
			public IShellResult getShellResult(){
				return shellResults.get(signalGroup);
			}
			
			@Override
			public void setGroupColour(Color newColor){
				try {
					parent.getCollection().getSignalGroup(signalGroup).setGroupColour(newColor);
				} catch (UnavailableSignalGroupException e) {
					stack("Signal group not found", e);
				}
			}
			
		};
		
//		if(shellResults.containsKey(signalGroup)){
//			result.setShellResult(shellResults.get(signalGroup));
//		} else {
//			result.setShellResult(null);
//		}
		return result;
	}

	@Override
	public boolean hasSignalGroup(UUID signalGroup) {
		return parent.getCollection().hasSignalGroup(signalGroup);
	}

	@Override
	public Collection<ISignalGroup> getSignalGroups() {
//		return signalGroups.values();
		return parent.getCollection().getSignalGroups();
	}

	@Override
	public void addSignalGroup(UUID newID, ISignalGroup newGroup) {
//		signalGroups.put(newID, newGroup);
		parent.getCollection().addSignalGroup(newID, newGroup);
	}

	@Override
	public SignalManager getSignalManager() {
		return signalManager;
	}

	@Override
	public RuleSetCollection getRuleSetCollection() {
		return parent.getCollection().getRuleSetCollection();
	}

	@Override
	public void updateVerticalNuclei() {
		parent.getCollection().updateVerticalNuclei();
	}

	@Override
	public boolean updateSourceFolder(File expectedImageDirectory) {
		return parent.getCollection().updateSourceFolder(expectedImageDirectory);
	}

	@Override
	public Nucleus getNucleusMostSimilarToMedian(Tag pointType) throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException {
		if(size()==1){
			for(ICell c : getCells()){
				return c.getNucleus();
			}
		}

		IProfile medianProfile = this.getProfileCollection().getProfile(ProfileType.ANGLE, pointType, Quartile.MEDIAN); // the profile we compare the nucleus to

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
	
	/**
	 * Get the differences to the median profile for each nucleus
	 * @param pointType the point to fetch profiles from
	 * @return an array of differences
	 */
	private double[] getDifferencesToMedianFromPoint(Tag pointType) {

		int count = this.size();
		double[] result = new double[count];
		int i=0;

		IProfile medianProfile;
		try {
			medianProfile = this.getProfileCollection().getProfile(ProfileType.ANGLE, pointType, Quartile.MEDIAN);
		} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
			fine("Error getting median profile for collection", e);
			for(int j=0; i<result.length; j++){
				result[j] = 0;	
			}
			return result;
		}
		for(Nucleus n : this.getNuclei()){
			
			try {
				IProfile angleProfile = n.getProfile(ProfileType.ANGLE);
				result[i] = angleProfile.offset(n.getBorderIndex(pointType)).absoluteSquareDifference(medianProfile);
			} catch (ProfileException | UnavailableProfileTypeException e) {
				fine("Error getting nucleus profile", e);
				result[i] = Double.MAX_VALUE;
			} finally {
				i++;
			}
		}
		return result;
	}

	@Override
	public ProfileManager getProfileManager() {
		return profileManager;
	}
	
	public IAnalysisDataset getRootParent(){
		if(parent.isRoot()){
			return  parent;
		} else {
			
			if(parent.getCollection() instanceof VirtualCellCollection){
				VirtualCellCollection v = (VirtualCellCollection) parent.getCollection();
				
				return v.getRootParent();
			} else {
				return null;
			}
			
		}
	}
	
	private ICellCollection chooseNewCollectionType(ICellCollection other, String name){
		boolean makeVirtual = false;
		if(other instanceof VirtualCellCollection){
			// Decide if the other collectionis also a child of the root parent
			IAnalysisDataset rootParent = this.getRootParent();
			IAnalysisDataset rootOther  = ((VirtualCellCollection)other).getRootParent();
			
			if (rootParent==rootOther){
				makeVirtual = true;
			}
		}
		
		ICellCollection newCollection;
		if(makeVirtual){
			newCollection = new VirtualCellCollection(this.getRootParent(), name);
		} else {
			newCollection = new DefaultCellCollection(this, name);
		}
		return newCollection;
	}

	@Override
	public ICellCollection and(ICellCollection other) {
		
		
		ICellCollection newCollection = chooseNewCollectionType(other, "AND operation");

		for(ICell c : other.getCells()){

			if(this.contains(c)){
				newCollection.addCell(new DefaultCell(c));
			}
		}

		return newCollection;
	}

	@Override
	public ICellCollection not(ICellCollection other) {
		ICellCollection newCollection = chooseNewCollectionType(other, "NOT operation");

		for(ICell c : getCells()){

			if( ! other.contains(c)){
				newCollection.addCell(new DefaultCell(c));
			}
		}

		return newCollection;
	}

	@Override
	public ICellCollection xor(ICellCollection other) {
		ICellCollection newCollection = chooseNewCollectionType(other, "XOR operation");

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
	
	@Override
	public ICellCollection or(ICellCollection other) {
		ICellCollection newCollection = chooseNewCollectionType(other, "OR operation");

		for(ICell c : getCells()){
				newCollection.addCell(new DefaultCell(c));
		}

		for(ICell c : other.getCells()){

			if( ! this.contains(c)){
				newCollection.addCell(new DefaultCell(c));
			}
		}

		return newCollection;
	}

	
//	public ICellCollection filterCollection(PlottableStatistic stat, MeasurementScale scale, double lower,
//			double upper) {
//		if(stat instanceof NucleusStatistic){
//			return filterCollection(  (NucleusStatistic) stat, scale, lower, upper);
//		} 
//
//		if(stat.getClass()==SignalStatistic.class){
//			return null;
//		} 
//
//		if(stat.getClass()==SegmentStatistic.class){
//			return null;
//		}
//		return null;
//	}
	
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
	@Override
	public ICellCollection filterCollection(PlottableStatistic stat, MeasurementScale scale, double lower, double upper) {
				
		DecimalFormat df = new DecimalFormat("#.##");
		ICellCollection subCollection = new DefaultCellCollection(this, "Filtered_"+stat.toString()+"_"+df.format(lower)+"-"+df.format(upper));

		List<ICell> filteredCells;

		if(stat.equals(PlottableStatistic.VARIABILITY)){
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

	@Override
	public int countShared(IAnalysisDataset d2) {
		return countShared(d2.getCollection());
		
	}

	@Override
	public int countShared(ICellCollection d2) {
		if(this.vennCache.containsKey(d2.getID())){
			return vennCache.get(d2.getID());
		}
		int shared  = countSharedNuclei(d2);
		vennCache.put(d2.getID(), shared);
		d2.setSharedCount(this, shared);
		return shared;
	}
	
	@Override
	public void setSharedCount(ICellCollection d2, int i){
		vennCache.put(d2.getID(), i);
	}
	
	/**
	 * Count the number of nuclei from this dataset that are present in d2
	 * @param d1
	 * @param d2
	 * @return
	 */
	private synchronized int countSharedNuclei(ICellCollection d2){

		if(d2==this){
			return this.size();
		}

		if(d2.getNucleusType() != this.getNucleusType()){
			return 0;
		}
				
		Set<UUID> toSearch1 = this.getCellIDs();
		Set<UUID> toSearch2 = d2.getCellIDs();
		
		finest("Beginning search for shared cells");
//		toSearch1.retainAll(toSearch2);
//		int shared = toSearch1.size();
		
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

	@Override
	public int getMedianArrayLength() {
		if(size()==0){
			return 0;
		}

		int[] p = this.getArrayLengths();
		double median = new Quartile(p, Quartile.MEDIAN).doubleValue();
		return (int) median;
	}
	
	private int[] getArrayLengths(){
		int[] result = new int[size()];

		int i=0;
		for(ICell cell : getCells() ){ 
			Nucleus n = cell.getNucleus();
			result[i++] =  n.getBorderLength();
		}
		return result;
	}

	@Override
	public int getMaxProfileLength() {
		return Arrays.stream(this.getArrayLengths()).max().orElse(0); //Stats.max(values);
	}

	@Override
	public double getMedianPathLength() {
		if(size()==0){
			return 0;
		}

		double[] p = this.getPathLengths();
		double median = new Quartile(p, Quartile.MEDIAN).doubleValue();
		return median;
	}
	
	/**
	 * Get the path lengths of the nuclei in this collection as
	 * an array
	 * @return
	 */
	private double[] getPathLengths() {

		int count = size();
		double[] result = new double[count];
		int i=0;
		for(ICell cell : getCells() ){ 
			Nucleus n = cell.getNucleus();
			try {
				result[i] =  n.getPathLength(ProfileType.ANGLE);
			} catch (UnavailableProfileTypeException e) {
				fine("Cannot get path lengths", e);
			}
			i++;
		}
		return result;
	}

	@Override
	public synchronized double getMedianStatistic(PlottableStatistic stat, String component, MeasurementScale scale) throws Exception {
		if(this.size()==0){
			return 0;
		}
		return getMedianStatistic(stat, component, scale, null, null);
	}

	@Override
	public synchronized double[] getMedianStatistics(PlottableStatistic stat, String component, MeasurementScale scale) {
		return getMedianStatistics(stat, component, scale, null);
	}

	@Override
	public synchronized double[] getMedianStatistics(PlottableStatistic stat, String component, MeasurementScale scale, UUID id) {
		try {
			if(CellularComponent.NUCLEUS.equals(component)){
				return getNuclearStatistics(stat, scale);
			}

			if(CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)){
				return getSegmentStatistics( stat, scale, id);

			}
		} catch (Exception e){
			return new double[0];
		}
			
		return new double[0];
	}
	
	/**
	 * Calculate the length of the segment with the given name in each nucleus
	 * of the collection
	 * @param segName the segment name
	 * @param scale the scale to use
	 * @return a list of segment lengths
	 * @throws Exception
	 */
	private synchronized double[] getSegmentStatistics(PlottableStatistic stat, MeasurementScale scale, UUID id) throws Exception{

		SegmentStatisticFetchingTask task = new SegmentStatisticFetchingTask(getNucleusArray(),
				stat,
				scale, 
				id);
		return task.invoke();
	}
	
	/**
	 * Get a list of the given statistic values for each nucleus in the collection
	 * @param stat the statistic to use
	 * @param scale the measurement scale
	 * @return a list of values
	 * @throws Exception
	 */
	private synchronized double[] getNuclearStatistics(PlottableStatistic stat, MeasurementScale scale) {

		double[] result = null;
		// Keep the nucleus statistic for legacy comparability. Changing to GenericStatistics from 1.13.4
		if(stat.equals(PlottableStatistic.VARIABILITY)){
			result = this.getNormalisedDifferencesToMedianFromPoint(Tag.REFERENCE_POINT);
		} else{
			finest("Making statistic fetching task for "+stat);
			NucleusStatisticFetchingTask task = new NucleusStatisticFetchingTask(getNucleusArray(),
					stat,
					scale);
			result = task.invoke();
			finest("Fetched statistic result for "+stat);
		}
		return result;
	}
	
	private Nucleus[] getNucleusArray(){
		return this.getNuclei().toArray(new Nucleus[0]);
	}
	
	/**
	 * Get the differences to the median profile for each nucleus, normalised to the
	 * perimeter of the nucleus. This is the sum-of-squares difference, rooted and divided by
	 * the nuclear perimeter
	 * @param pointType the point to fetch profiles from
	 * @return an array of normalised differences
	 */
	private double[] getNormalisedDifferencesToMedianFromPoint(BorderTagObject pointType) {
		//	  List<Double> list = new ArrayList<Double>();
		int count = this.size();
		double[] result = new double[count];
		
		IProfile medianProfile;
		try {
			medianProfile = this.getProfileCollection().getProfile(ProfileType.ANGLE, pointType, Quartile.MEDIAN);
		} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
			warn("Cannot get median profile for collection");
			fine("Error getting median profile", e);
			for(int j=0; j<count; j++){
				result[j] = Double.MAX_VALUE;
			}
			return result;
			
		}
		
		int i=0;
		for(Nucleus n : this.getNuclei()){

			try {
				
				IProfile angleProfile = n.getProfile(ProfileType.ANGLE, pointType);
				double diff = angleProfile.absoluteSquareDifference(medianProfile);		
				diff /= n.getStatistic(PlottableStatistic.PERIMETER, MeasurementScale.PIXELS); // normalise to the number of points in the perimeter (approximately 1 point per pixel)
				double rootDiff = Math.sqrt(diff); // use the differences in degrees, rather than square degrees  
				result[i] = rootDiff;

			} catch(ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e){
				fine("Error getting nucleus profile", e);
				result[i] = Double.MAX_VALUE;
			} finally {
				i++;
			}

		}
		return result;
	}
	
	private double getMedianStatistic(PlottableStatistic stat, String component, MeasurementScale scale, UUID signalGroup, UUID segId)  throws Exception {
		if(CellularComponent.NUCLEUS.equals(component) ){
			return getMedianNucleusStatistic( stat, scale);
		}

		if(CellularComponent.NUCLEAR_SIGNAL.equals(component) || stat instanceof SignalStatistic){
			return getSignalManager().getMedianSignalStatistic(stat, scale, signalGroup);
		}

		if(CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component) || stat instanceof SegmentStatistic){
			return getMedianSegmentStatistic((SegmentStatistic) stat, scale, segId);
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
	private double getMedianSegmentStatistic(PlottableStatistic stat, MeasurementScale scale, UUID id)  throws Exception {

		if(cellIDs.isEmpty()){
			return 0;
		}

		double[] values = this.getSegmentStatistics(stat, scale, id);
		return new Quartile(values, Quartile.MEDIAN).doubleValue();
	}
	
	/**
	 * Get the median value of the given statistic
	 * @param stat
	 * @param scale
	 * @return
	 * @throws Exception
	 */
	private double getMedianNucleusStatistic(PlottableStatistic stat, MeasurementScale scale)  throws Exception {

		if(this.statsCache.hasStatistic(stat, CellularComponent.NUCLEUS, scale)){
			return(this.statsCache.getStatistic(stat, CellularComponent.NUCLEUS, scale));
		} else {

			double median = 0;
			if(this.size()>0){
				double[] values = this.getNuclearStatistics(stat, scale);
				median =  new Quartile(values, Quartile.MEDIAN).doubleValue();
			}

			statsCache.setStatistic(stat, CellularComponent.NUCLEUS, scale, median);
			return median;
		}


	}

	@Override
	public double getNormalisedDifferenceToMedian(Tag pointType, ICell c) {
		
		try {
			IProfile medianProfile = this.getProfileCollection().getProfile(ProfileType.ANGLE, pointType, Quartile.MEDIAN);
			IProfile angleProfile = c.getNucleus().getProfile(ProfileType.ANGLE, pointType);
			double diff = angleProfile.absoluteSquareDifference(medianProfile);		
			diff /= c.getNucleus().getStatistic(PlottableStatistic.PERIMETER, MeasurementScale.PIXELS); // normalise to the number of points in the perimeter (approximately 1 point per pixel)
			double rootDiff = Math.sqrt(diff); // use the differences in degrees, rather than square degrees  
			return rootDiff;
		} catch(ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e){
			fine("Error getting difference to median profile for cell "+c.getNucleus().getNameAndNumber());
			return Double.MAX_VALUE;
		}
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}
			
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

		in.defaultReadObject();
		isRefolding = false;
		vennCache   = new HashMap<UUID, Integer>(); // cache the number of shared nuclei with other datasets
		
		statsCache = new StatsCache();
				
		signalManager  = new SignalManager(this);
		profileManager = new ProfileManager(this);
		
		isRefolding = false;
		vennCache   = new HashMap<UUID, Integer>(); // cache the number of shared nuclei with other datasets

		// Don't try to restore profile aggregates here - the parent collection has
		// not finished loading, and will be null. Do the restore in the importing class
		// after reading has finished.


	}

}
