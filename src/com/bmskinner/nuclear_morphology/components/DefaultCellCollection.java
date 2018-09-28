/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
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
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileManager;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SegmentStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.components.stats.StatsCache;
import com.bmskinner.nuclear_morphology.components.stats.VennCache;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * This is a more efficient replacement for the <=1.13.2 cell collections
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultCellCollection implements ICellCollection {

	private static final long serialVersionUID = 1L;

	private final UUID uuid; // the collection id

	private File   folder;       // the source of the nuclei
	private String outputFolder; // the location to save out data
	private String name;         // the name of the collection

	private NucleusType nucleusType; // the type of nuclei this collection
	// contains

	// this holds the mapping of tail indexes etc in the median profile arrays
	private IProfileCollection profileCollection = IProfileCollection.makeNew();

	private Nucleus consensusNucleus; // the refolded consensus nucleus

	private Set<ICell> cells = new HashSet<>(100); // store all the cells
	// analysed

	private Map<UUID, ISignalGroup> signalGroups = new HashMap<>(0);

	private RuleSetCollection ruleSets = new RuleSetCollection();

	/*
	 * 
	 * TRANSIENT FIELDS
	 * 
	 */

//	/**
//	 * Set when the consensus nucleus is refolding
//	 */
//	private volatile transient boolean isRefolding = false;

	/**
	 * Cache statistics from the cells in the collection. This should be updated
	 * if a cell is added or lost
	 */
	private volatile transient StatsCache statsCache = new StatsCache();

	// cache the number of shared cells with other datasets
	protected volatile transient VennCache vennCache = new VennCache();

	private transient SignalManager  signalManager  = new SignalManager(this);
	private transient ProfileManager profileManager = new ProfileManager(this);

	/**
	 * Constructor.
	 * 
	 * @param folder the folder of images
	 * @param outputFolder a name for the outputs (usually the analysis date). Can be
	 *            null
	 * @param name the name of the collection
	 * @param nucleusType the class of nucleus to be held
	 */
	public DefaultCellCollection(File folder, @Nullable String outputFolder, @Nullable String name, NucleusType nucleusType) {
		this(folder, outputFolder, name, nucleusType, java.util.UUID.randomUUID());
	}

	/**
	 * Constructor with non-random id. Use only when copying an old collection.
	 * Can cause ID conflicts!
	 * 
	 * @param folder the folder of images
	 * @param outputFolder a name for the outputs (usually the analysis date). Can be
	 *            null
	 * @param name the name of the collection
	 * @param nucleusClass the class of nucleus to be held
	 * @param id specify an id for the collection, rather than generating
	 *            randomly.
	 */
	public DefaultCellCollection(File folder, @Nullable String outputFolder, @Nullable String name, NucleusType nucleusType, UUID id) {

		this.uuid = id;
		this.folder = folder;
		this.outputFolder = outputFolder;
		this.name = name == null ? folder.getName() : name;// if name is null,
		// use the image
		// folder name
		this.nucleusType = nucleusType;

		ruleSets = RuleSetCollection.createDefaultRuleSet(nucleusType);
		
	}

	/**
	 * Construct an empty collection from a template dataset
	 * 
	 * @param template
	 *            the dataset to base on for folders and type
	 * @param name
	 *            the collection name
	 */
	public DefaultCellCollection(IAnalysisDataset template, String name) {

		this(template.getCollection(), name);
	}

	/**
	 * Construct an empty collection from a template collection
	 * 
	 * @param template
	 * @param name
	 */
	public DefaultCellCollection(ICellCollection template, String name) {
		this(template.getFolder(), template.getOutputFolderName(), name, template.getNucleusType());
	}
	

	@Override
	public ICellCollection duplicate() {
		DefaultCellCollection result = new DefaultCellCollection(folder, outputFolder, name, nucleusType, uuid);
		result.ruleSets = ruleSets;
		
		for(ICell c : this)
			result.addCell(c.duplicate());
		
		result.consensusNucleus = consensusNucleus==null? null : consensusNucleus.duplicate();
		result.profileCollection = profileCollection.duplicate();
		
		// copy the signals
        for(UUID id : getSignalGroupIDs())
        	result.addSignalGroup(id, getSignalGroup(id).get().duplicate());
		return result;
	}

	@Override
	public void setName(@NonNull String s) {
		this.name = s;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public UUID getID() {
		return this.uuid;
	}

	@Override
	public boolean isReal() {
		return true;
	}

	@Override
	public boolean isVirtual() {
		return false;
	}
	
	@Override
	public boolean add(ICell e) {
		return cells.add(e);
	}

	@Override
	public boolean addAll(Collection<? extends ICell> c) {
		return cells.addAll(c);
	}

	@Override
	public void clear() {
		cells.clear();
	}

	@Override
	public boolean contains(Object o) {
		return cells.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return cells.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return cells.isEmpty();
	}

	@Override
	public Iterator<ICell> iterator() {
		return cells.iterator();
	}

	@Override
	public boolean remove(Object o) {
		return cells.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return cells.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return cells.retainAll(c);
	}

	@Override
	public Object[] toArray() {
		return cells.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return cells.toArray(a);
	}

	/**
	 * Get the UUIDs of all the cells in the collection
	 * 
	 * @return
	 */
	@Override
	public Set<UUID> getCellIDs() {

		return cells.parallelStream().map(c -> c.getId()).collect(Collectors.toSet());

	}

	@Override
	public void addCell(final @NonNull ICell r) {
		cells.add(r);
	}

	@Override
	public void replaceCell(@NonNull ICell r) {
		if (r == null)
			throw new IllegalArgumentException("Cell is null");

		boolean found = false;
		Iterator<ICell> it = cells.iterator();
		while (it.hasNext()) {
			ICell test = it.next();

			if (r.getId().equals(test.getId())) {
				it.remove();
				found = true;
				break;
			}
		}

		// only put the cell in if it was removed
		if (found)
			addCell(r);
	}

	@Override
	public synchronized void removeCell(@NonNull ICell c) {
		if(c==null)
			return;

		// Since cell hashcodes change during profiling and segmentation,
		// we can't just use cells.remove(c).

		Set<ICell> newCells = new HashSet<>();
		for(ICell cell : cells)
			if(!cell.getId().equals(c.getId()))
				newCells.add(cell);
		
		cells = newCells;
	}

	@Override
	public int size() {
		return cells.size();
	}

	@Override
	public boolean hasCells() {
		return !cells.isEmpty();
	}

	@Override
	public boolean hasLockedCells() {
		for (Nucleus n : this.getNuclei()) {
			if (n.isLocked()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setCellsLocked(boolean b) {
		for (Nucleus n : this.getNuclei()) {
			n.setLocked(b);
		}
	}

	/**
	 * Get the cell with the given UUID
	 * 
	 * @param id
	 * @return
	 */
	@Override
	public ICell getCell(@NonNull UUID id) {
		return cells.parallelStream()
				.filter(c->c.getId().equals(id))
				.findFirst().orElse(null);
	}

	@Override
	public NucleusType getNucleusType() {
		return this.nucleusType;
	}

	/*
	 * 
	 * METHODS IMPLEMENTING THE REFOLDABLE INTERFACE
	 * 
	 */

	@Override
	public boolean hasConsensus() {
		return consensusNucleus != null;
	}

	@Override
	public void setConsensus(@Nullable Nucleus n) {
		consensusNucleus = n;
	}

	@Override
	public Nucleus getConsensus() {
		return consensusNucleus;
	}

	/**
	 * Get the profile collection of the given type
	 * 
	 * @param type
	 * @return
	 */
	@Override
	public IProfileCollection getProfileCollection() {
		return profileCollection;
	}

	@Override
	public File getFolder() {
		return folder;
	}

	@Override
	public String getOutputFolderName() {
		return outputFolder;
	}

	/**
	 * Get the output folder (e.g. to save the dataset into). If an output
	 * folder name (such as a date) has been input, it will be included
	 * 
	 * @return the folder
	 */
	@Override
	public File getOutputFolder() {
		if (outputFolder == null)
			return getFolder();
		return new File(this.getFolder(), outputFolder);
	}

	@Override
	public void setOutputFolder(@NonNull File folder) {
		this.folder = folder;
		this.outputFolder = null;
	}

	@Override
	public synchronized Set<File> getImageFiles() {
		return getNuclei().stream().map(n->n.getSourceFile()).collect(Collectors.toSet());
	}

	/**
	 * Get the border lengths of the nuclei in this collection as an array
	 * 
	 * @return
	 */
	private synchronized int[] getArrayLengths() {
		return getNuclei().stream().mapToInt(n->n.getBorderLength()).toArray();
	}

	public synchronized double[] getMedianDistanceBetweenPoints() {
		return getNuclei().stream().mapToDouble(n->n.getMedianDistanceBetweenPoints()).toArray();
	}

	@Override
	public int getNucleusCount() {
		return this.getNuclei().size();
	}

	@Override
	public Set<ICell> getCells() {
		return cells;
	}


	@Override
	public Stream<ICell> streamCells() {
		return cells.stream();
	}

	@Override
	public synchronized Set<ICell> getCells(@NonNull File imageFile) {
		return cells.stream().filter(c->c.getNucleus().getSourceFile().equals(imageFile))
				.collect(Collectors.toSet());
	}

	@Override
	public synchronized boolean hasCells(@NonNull File imageFile) {
		return getCells(imageFile).size() > 0;
	}

	@Override
	public synchronized boolean hasNuclei(@NonNull File imageFile) {
		return getNuclei(imageFile).size() > 0;
	}

	@Override
	public synchronized Set<Nucleus> getNuclei() {
		return cells.stream().flatMap(c->c.getNuclei().stream())
				.collect(Collectors.toSet());
	}

	@Override
	public synchronized Set<Nucleus> getNuclei(@NonNull File imageFile) {
		return getNuclei().stream().filter(n->n.getSourceFile().equals(imageFile))
				.collect(Collectors.toSet());
	}

	@Override
	public SignalManager getSignalManager() {
		return signalManager;
	}

	@Override
	public ProfileManager getProfileManager() {
		return profileManager;
	}

	@Override
	public int getMedianArrayLength() {
		if (size() == 0)
			return 0;
		int[] p = this.getArrayLengths();
		return Stats.quartile(p, Stats.MEDIAN);
	}

	@Override
	public int getMaxProfileLength() {
		return Arrays.stream(getArrayLengths()).max().orElse(0);
	}

	/*
	 * -------------------- Profile methods --------------------
	 */

	@Override
	public void createProfileCollection() throws ProfileException {
		createProfileCollection(getMedianArrayLength());
	}
	
	/*
	 * Build a set of profile aggregates Default is to make profile
	 * aggregate from reference point
	 * 
	 */
	public void createProfileCollection(int length) throws ProfileException {
		profileCollection.createProfileAggregate(this, length);
	}

	/**
	 * Get the differences to the median profile for each nucleus
	 * 
	 * @param pointType the point to fetch profiles from
	 * @return an array of differences
	 */
	private double[] getDifferencesToMedianFromPoint(Tag pointType) {

		int count = this.getNucleusCount();
		double[] result = new double[count];
		int i = 0;

		IProfile medianProfile;
		try {
			medianProfile = this.getProfileCollection().getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN);
		} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
			fine("Error getting median profile for collection", e);
			for (int j = 0; i < result.length; j++) {
				result[j] = 0;
			}
			return result;
		}

		for (Nucleus n : this.getNuclei()) {

			try {
				IProfile angleProfile = n.getProfile(ProfileType.ANGLE);
				result[i] = angleProfile.offset(n.getBorderIndex(pointType)).absoluteSquareDifference(medianProfile);
			} catch (ProfileException | UnavailableProfileTypeException | UnavailableBorderTagException e) {
				fine("Error getting nucleus profile", e);
				result[i] = Double.MAX_VALUE;
			} finally {
				i++;
			}
		}
		return result;
	}

	/**
	 * Get the differences to the median profile for each nucleus, normalised to
	 * the perimeter of the nucleus. This is the sum-of-squares difference,
	 * rooted and divided by the nuclear perimeter
	 * 
	 * @param pointType the point to fetch profiles from
	 * @return an array of normalised differences
	 */
	private synchronized double[] getNormalisedDifferencesToMedianFromPoint(BorderTagObject pointType) {
		IProfile medianProfile;
		try {
			medianProfile = this.getProfileCollection().getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN);
		} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
			warn("Cannot get median profile for collection");
			fine("Error getting median profile", e);
			double[] result = new double[size()];
			Arrays.fill(result, Double.MAX_VALUE);
			return result;
		}

		return getNuclei().stream().mapToDouble(n->{
			try {

				IProfile angleProfile = n.getProfile(ProfileType.ANGLE, pointType);
				double diff = angleProfile.absoluteSquareDifference(medianProfile);

				// normalise to the number of points in the perimeter
				diff /= n.getStatistic(PlottableStatistic.PERIMETER, MeasurementScale.PIXELS);
				return Math.sqrt(diff); // differences in degrees, rather than square degrees

			} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
				fine("Error getting nucleus profile", e);
				return  Double.MAX_VALUE;
			} 
		}).toArray();
	}

	/**
	 * Get the perimeter normalised veriabililty of a nucleus angle profile
	 * compared to the median profile of the collection
	 * 
	 * @param pointType
	 *            the tag to use as index 0
	 * @param c
	 *            the cell to test
	 * @return the variabililty score of the nucleus
	 * @throws Exception
	 */
	@Override
	public double getNormalisedDifferenceToMedian(Tag pointType, Taggable t) {
		IProfile medianProfile;
		try {
			medianProfile = profileCollection.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN);
		} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
			fine("Error getting median profile for collection", e);
			return 0;
		}
		IProfile angleProfile;
		try {
			angleProfile = t.getProfile(ProfileType.ANGLE, pointType);

			double diff = angleProfile.absoluteSquareDifference(medianProfile);
			diff /= t.getStatistic(PlottableStatistic.PERIMETER, MeasurementScale.PIXELS); // normalise
			// to
			// the
			// number
			// of
			// points
			// in
			// the
			// perimeter
			// (approximately
			// 1
			// point
			// per
			// pixel)
			double rootDiff = Math.sqrt(diff); // use the differences in
			// degrees, rather than square
			// degrees
			return rootDiff;
		} catch (ProfileException | UnavailableComponentException e) {
			fine("Error getting nucleus profile", e);
			return 0;
		}
	}

	public double compareProfilesToMedian(BorderTagObject pointType) throws Exception {
		double[] scores = this.getDifferencesToMedianFromPoint(pointType);
		double result = 0;
		for (double s : scores) {
			result += s;
		}
		return result;
	}

	/**
	 * Get the distances between two border tags for each nucleus
	 * 
	 * @param pointTypeA
	 * @param pointTypeB
	 * @return
	 */
	public double[] getPointToPointDistances(Tag pointTypeA, Tag pointTypeB) {
		int count = this.getNucleusCount();
		double[] result = new double[count];
		int i = 0;
		for (Nucleus n : this.getNuclei()) {
			try {
				result[i] = n.getBorderPoint(pointTypeA).getLengthTo(n.getBorderPoint(pointTypeB));
			} catch (UnavailableBorderTagException e) {
				fine("Tag not present: " + pointTypeA + " or " + pointTypeB);
				result[i] = 0;
			} finally {
				i++;
			}

		}
		return result;
	}

	/**
	 * Get the nucleus with the lowest difference score to the median profile
	 * 
	 * @param pointType
	 *            the point to compare profiles from
	 * @return the best nucleus
	 * @throws UnavailableBorderTagException
	 * @throws UnavailableProfileTypeException
	 */
	@Override
	public Nucleus getNucleusMostSimilarToMedian(Tag pointType)
			throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException {
		Set<Nucleus> list = this.getNuclei();

		// No need to check profiles if there is only one nucleus
		if (list.size() == 1) {
			for (Nucleus p : list) {
				return p;
			}
		}

		IProfile medianProfile = profileCollection.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN);

		// the profile we compare the nucleus to
		// Nucleus n = this.getNuclei()..get(0); // default to the first nucleus
		Nucleus n = null;

		double difference = Arrays.stream(getDifferencesToMedianFromPoint(pointType)).max().orElse(0);
		for (Nucleus p : list) {
			IProfile angleProfile = p.getProfile(ProfileType.ANGLE, pointType);
			double nDifference = angleProfile.absoluteSquareDifference(medianProfile);
			if (nDifference < difference) {
				difference = nDifference;
				n = p;
			}
		}

		if (n == null) {
			throw new ProfileException("Error finding nucleus similar to median");
		}

		return n;
	}

	/*
	 * 
	 * METHODS IMPLEMENTING THE STATISTICAL COLLECTION INTERFACE
	 * 
	 */

	@Override
	public void clear(PlottableStatistic stat, String component) {
		statsCache.clear(stat, component, null);
	}

	@Override
	public void clear(PlottableStatistic stat, String component, UUID id) {
		statsCache.clear(stat, component, id);
	}

	@Override
	public void clear(MeasurementScale scale) {
		statsCache.clear(scale);
	}

	@Override
	public double getMedian(PlottableStatistic stat, String component, MeasurementScale scale)
			throws Exception {
		return getMedianStatistic(stat, component, scale, null);
	}

	@Override
	public double getMedian(PlottableStatistic stat, String component, MeasurementScale scale,
			UUID id) throws Exception {

		if (CellularComponent.NUCLEAR_SIGNAL.equals(component) || stat.getClass() == SignalStatistic.class) {
			return getMedianStatistic(stat, component, scale, id);
		}

		if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component) || stat.getClass() == SegmentStatistic.class) {
			return getMedianStatistic(stat, component, scale, id);
		}
		return 0;
	}

	@Override
	public double[] getRawValues(PlottableStatistic stat, String component,
			MeasurementScale scale) {

		return getRawValues(stat, component, scale, null);

	}

	@Override
	public double[] getRawValues(PlottableStatistic stat, String component, MeasurementScale scale,
			UUID id) {

		switch(component) {
		case CellularComponent.WHOLE_CELL: return getCellStatistics(stat, scale);
		case CellularComponent.NUCLEUS: return getNuclearStatistics(stat, scale);
		case CellularComponent.NUCLEAR_BORDER_SEGMENT: return getSegmentStatistics(stat, scale, id);
		default: {
			warn("No component of type " + component + " can be handled");
			return null;
		}
		}
	}

	private synchronized double getMedianStatistic(PlottableStatistic stat, String component, MeasurementScale scale,
			UUID id) throws Exception {

		if (statsCache.hasMedian(stat, component, scale, id))
			return statsCache.getMedian(stat, component, scale, id);

		double median = Statistical.ERROR_CALCULATING_STAT;

		if (this.hasCells()) {
			double[] values = getRawValues(stat, component, scale, id);
			median = Stats.quartile(values, Stats.MEDIAN);
		}
		statsCache.setMedian(stat, component, scale, id, median);
		return median;
	}


	@Override
	public synchronized double getMin(PlottableStatistic stat, String component, MeasurementScale scale) {
		return getMinStatistic(stat, component, scale, null);
	}

	@Override
	public synchronized double getMin(PlottableStatistic stat, String component, MeasurementScale scale,
			UUID id){

		// Handle old segment and SignalStatistic enums
		if (CellularComponent.NUCLEAR_SIGNAL.equals(component) || stat.getClass() == SignalStatistic.class) {
			return getMinStatistic(stat, CellularComponent.NUCLEAR_SIGNAL, scale, id);
		}

		if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component) || stat.getClass() == SegmentStatistic.class) {
			return getMinStatistic(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);
		}
		return getMinStatistic(stat, component, scale, id);
	}

	private synchronized double getMinStatistic(PlottableStatistic stat, String component, MeasurementScale scale,
			UUID id) {

		double[] values = getRawValues(stat, component, scale, id);
		return Arrays.stream(values).min().orElse(Statistical.ERROR_CALCULATING_STAT);
	}

	@Override
	public synchronized double getMax(PlottableStatistic stat, String component, MeasurementScale scale) {
		return getMaxStatistic(stat, component, scale, null);
	}

	@Override
	public synchronized double getMax(PlottableStatistic stat, String component, MeasurementScale scale,
			UUID id){

		// Handle old segment andSignalStatistic enums
		if (CellularComponent.NUCLEAR_SIGNAL.equals(component) || stat.getClass() == SignalStatistic.class)
			return getMaxStatistic(stat, CellularComponent.NUCLEAR_SIGNAL, scale, id);
		if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component) || stat.getClass() == SegmentStatistic.class)
			return getMaxStatistic(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);
		return getMaxStatistic(stat, component, scale, id);
	}

	private synchronized double getMaxStatistic(PlottableStatistic stat, String component, MeasurementScale scale,
			UUID id) {

		double[] values = getRawValues(stat, component, scale, id);
		return Arrays.stream(values).max().orElse(Statistical.ERROR_CALCULATING_STAT);
	}

	/**
	 * Get a sorted list of the given statistic values for each nucleus in the
	 * collection
	 * 
	 * @param stat the statistic to use
	 * @param scale the measurement scale
	 * @return a list of values
	 * @throws Exception
	 */
	private double[] getCellStatistics(PlottableStatistic stat, MeasurementScale scale) {

		double[] result = null;

		if (statsCache.hasValues(stat, CellularComponent.WHOLE_CELL, scale, null))
			return statsCache.getValues(stat, CellularComponent.WHOLE_CELL, scale, null);
		result = cells.stream().mapToDouble(c -> c.getStatistic(stat, scale)).sorted().toArray();
		statsCache.setValues(stat, CellularComponent.WHOLE_CELL, scale, null, result);
		return result;

	}

	/**
	 * Get a list of the given statistic values for each nucleus in the
	 * collection
	 * 
	 * @param stat the statistic to use
	 * @param scale the measurement scale
	 * @return a list of values
	 * @throws Exception
	 */
	private double[] getNuclearStatistics(PlottableStatistic stat, MeasurementScale scale) {

		double[] result = null;

		if (statsCache.hasValues(stat, CellularComponent.NUCLEUS, scale, null)) {
			return statsCache.getValues(stat, CellularComponent.NUCLEUS, scale, null);

		}
		if (PlottableStatistic.VARIABILITY.equals(stat)) {
			result = this.getNormalisedDifferencesToMedianFromPoint(Tag.REFERENCE_POINT);
		} else {
			result = this.getNuclei().parallelStream().mapToDouble(n -> n.getStatistic(stat, scale)).toArray();
		}
		Arrays.sort(result);
		statsCache.setValues(stat, CellularComponent.NUCLEUS, scale, null, result);

		return result;
	}

	/**
	 * Calculate the length of the segment with the given name in each nucleus
	 * of the collection
	 * 
	 * @param segName the segment name
	 * @param scale the scale to use
	 * @return a list of segment lengths
	 * @throws Exception
	 */
	private double[] getSegmentStatistics(PlottableStatistic stat, MeasurementScale scale, UUID id){

		double[] result = null;
		if (statsCache.hasValues(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id)) {
			return statsCache.getValues(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);

		}
		AtomicInteger errorCount= new AtomicInteger(0);
		result = getNuclei().stream().mapToDouble(n -> {
			IBorderSegment segment;
			try {
				segment = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegment(id);
			} catch (ProfileException | UnavailableComponentException e) {
				errorCount.incrementAndGet();
				return 0;
			}
			double perimeterLength = 0;
			if (segment != null) {
				int indexLength = segment.length();
				double fractionOfPerimeter = (double) indexLength / (double) segment.getProfileLength();
				perimeterLength = fractionOfPerimeter * n.getStatistic(PlottableStatistic.PERIMETER, scale);
			}
			return perimeterLength;

		}).sorted().toArray();

		statsCache.setValues(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id, result);
		if(errorCount.get()>0)
			warn(String.format("Problem calculating segment stats for segment %s: %d nuclei had errors getting this segment", id, errorCount.get()));
		return result;
	}

	/*
	 * 
	 * METHODS IMPLEMENTING THE FILTERABLE INTERFACE
	 * 
	 */

	@Override
	public ICellCollection filter(@NonNull Predicate<ICell> predicate) {

		String name = "Filtered_" + predicate.toString();

		ICellCollection subCollection = new DefaultCellCollection(this, name);

		List<ICell> list = cells.parallelStream().filter(predicate).collect(Collectors.toList());

		for (ICell cell : list) {
			subCollection.addCell(new DefaultCell(cell));
		}

		if (subCollection.size() == 0) {
			warn("No cells in collection");
			return subCollection;
		}

		try {

			// TODO - this fails on converted collections from (at least) 1.13.0
			// with no profiles in aggregate
			this.getProfileManager().copyCollectionOffsets(subCollection);
			this.getSignalManager().copySignalGroups(subCollection);

		} catch (ProfileException e) {
			warn("Error copying collection offsets");
			stack("Error in offsetting", e);
		}

		return subCollection;
	}

	@Override
	public ICellCollection filterCollection(PlottableStatistic stat, MeasurementScale scale, double lower,
			double upper) {
		DecimalFormat df = new DecimalFormat("#.##");

		Predicate<ICell> pred = new Predicate<ICell>() {
			@Override
			public boolean test(ICell t) {

				for (Nucleus n : t.getNuclei()) {

					double value = stat.equals(PlottableStatistic.VARIABILITY)
							? getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, n) : n.getStatistic(stat, scale);

							if (value < lower) {
								return false;
							}

							if (value > upper) {
								return false;
							}

				}
				return true;
			}

			@Override
			public String toString() {
				return stat.toString() + "_" + df.format(lower) + "-" + df.format(upper);
			}

		};

		return filter(pred);
	}

	@Override
	public ICellCollection and(ICellCollection other) {

		ICellCollection newCollection = chooseNewCollectionType(this, "AND operation");

		other.streamCells()
		.filter(c->contains(c))
		.forEach(c->newCollection.addCell(new DefaultCell(c)));

		return newCollection;
	}

	@Override
	public ICellCollection not(ICellCollection other) {

		ICellCollection newCollection = chooseNewCollectionType(this, "NOT operation");

		streamCells()
		.filter(c->!other.contains(c))
		.forEach(c->newCollection.addCell(new DefaultCell(c)));

		return newCollection;
	}

	@Override
	public ICellCollection xor(ICellCollection other) {

		ICellCollection newCollection = chooseNewCollectionType(this, "XOR operation");

		streamCells()
		.filter(c->!other.contains(c))
		.forEach(c->newCollection.addCell(new DefaultCell(c)));

		other.streamCells()
		.filter(c->!contains(c))
		.forEach(c->newCollection.addCell(new DefaultCell(c)));

		return newCollection;
	}

	@Override
	public ICellCollection or(ICellCollection other) {

		ICellCollection newCollection = chooseNewCollectionType(this, "OR operation");

		getCells().forEach(c->newCollection.addCell(new DefaultCell(c)));

		other.getCells().forEach(c->newCollection.addCell(new DefaultCell(c)));

		return newCollection;
	}

	private ICellCollection chooseNewCollectionType(ICellCollection other, String name) {

		// Decide if the other collection is also a child of the root parent
		IAnalysisDataset rootParent = getDatasetOfRealCollection(this);
		IAnalysisDataset rootOther = other.isVirtual() ?
				((VirtualCellCollection) other).getRootParent() : getDatasetOfRealCollection(other);
				return rootParent == rootOther 
						? new VirtualCellCollection(rootParent, name)
								: new DefaultCellCollection(this, name);
	}

	private IAnalysisDataset getDatasetOfRealCollection(ICellCollection other){
		for(IAnalysisDataset d : DatasetListManager.getInstance().getRootDatasets()){
			if(d.getCollection().equals(other) 
					|| d.getAllChildDatasets().stream().map(t->t.getCollection()).anyMatch(c->c.getID().equals(other.getID())))
				return d;
		}
		return null;
	}

	/**
	 * Invalidate the existing cached vertically rotated nuclei, and
	 * recalculate.
	 */
	@Override
	public void updateVerticalNuclei() {

		try {
			getNuclei().parallelStream().forEach(n -> {
				n.updateVerticallyRotatedNucleus();
				n.updateDependentStats();
			});

			statsCache.clear(PlottableStatistic.BODY_WIDTH, CellularComponent.NUCLEUS, null);
			statsCache.clear(PlottableStatistic.HOOK_LENGTH, CellularComponent.NUCLEUS, null);
		} catch (Exception e) {
			warn("Cannot update all vertical nuclei");
			stack("Error updating vertical nuclei", e);
		}

	}

	@Override
	public void setSourceFolder(@NonNull File newFolder) {
		File oldFile = getFolder();
		if(!newFolder.exists())
			return;   
		folder = newFolder;
		cells.stream()
		.flatMap(c->c.getNuclei().stream())
		.forEach(n->n.setSourceFolder(newFolder));

	}

	@Override
	public boolean contains(ICell c) {
		if(c==null)
			return false;
		return contains(c.getId());
	}

	@Override
	public boolean contains(UUID id) {
		return cells.parallelStream().anyMatch(cell -> cell.getId().equals(id));
	}

	@Override
	public boolean containsExact(@NonNull ICell c) {
		return cells.parallelStream().anyMatch(cell -> cell == c);
	}

	@Override
	public Set<UUID> getSignalGroupIDs() {

		Set<UUID> ids = new HashSet<>(signalGroups.keySet());
		ids.remove(IShellResult.RANDOM_SIGNAL_ID);
		return ids;
	}

	@Override
	public Collection<ISignalGroup> getSignalGroups() {
		return this.signalGroups.values();
	}

	/**
	 * Fetch the signal group with the given ID
	 * 
	 * @param id
	 * @return
	 */
	@Override
	public Optional<ISignalGroup> getSignalGroup(@NonNull UUID id) {
		return Optional.ofNullable(this.signalGroups.get(id));
	}

	@Override
	public void addSignalGroup(@NonNull UUID id, @NonNull ISignalGroup group) {
		this.signalGroups.put(id, group);
	}

	@Override
	public boolean hasSignalGroup(@NonNull UUID id) {
		return this.signalGroups.containsKey(id);
	}

	/**
	 * Remove the given group
	 * 
	 * @param id
	 */
	@Override
	public void removeSignalGroup(@NonNull UUID id) {
		this.signalGroups.remove(id);
	}

	/**
	 * Get the RuleSetCollection with the index finding rules for this nucleus
	 * type
	 * 
	 * @return
	 */
	@Override
	public RuleSetCollection getRuleSetCollection() {
		return this.ruleSets;
	}

	/**
	 * Get the number of nuclei shared with the given dataset
	 * 
	 * @param d2
	 * @return
	 */
	@Override
	public synchronized int countShared(@NonNull IAnalysisDataset d2) {
		return countShared(d2.getCollection());
	}

	/**
	 * Get the number of nuclei shared with the given dataset
	 * 
	 * @param d2
	 * @return
	 */
	@Override
	public synchronized int countShared(@NonNull ICellCollection d2) {

		if (vennCache.hasCount(d2))
			return vennCache.getCount(d2);
		int shared = countSharedNuclei(d2);
		d2.setSharedCount(this, shared);
		vennCache.addCount(d2, shared);

		return shared;

	}

	@Override
	public void setSharedCount(@NonNull ICellCollection d2, int i) {
		vennCache.addCount(d2, i);
	}

	/**
	 * Count the number of nuclei from this dataset that are present in d2
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private synchronized int countSharedNuclei(ICellCollection d2) {

		if (d2 == this)
			return cells.size();

		if (d2.getNucleusType() != nucleusType)
			return 0;

		Set<UUID> toSearch = new HashSet<>(d2.getCellIDs());
		toSearch.retainAll(getCellIDs());
		return toSearch.size();
	}

	public int countClockWiseRPNuclei() {
		int count = 0;
		for (Nucleus n : getNuclei()) {
			if (n.isClockwiseRP()) {
				count++;
			}
		}
		return count;
	}

	@Override
	public void setScale(double scale){
		clear(MeasurementScale.MICRONS);

		for (ICell c : cells) {
			c.setScale(scale);
		}

		if (hasConsensus())
			consensusNucleus.setScale(scale);
	}

	@Override
	public String toString() {

		String newLine = System.getProperty("line.separator");

		StringBuilder b = new StringBuilder("Collection:" + getName() + newLine)
				.append("Collection:" + getName() + newLine).append("Nuclei: " + this.getNucleusCount() + newLine)
				.append("Clockwise: " + this.countClockWiseRPNuclei() + newLine)
				.append("Source folder: " + this.getFolder().getAbsolutePath() + newLine)
				.append("Nucleus type: " + this.nucleusType + newLine).append("Profile collections:" + newLine);

		IProfileCollection pc = this.getProfileCollection();
		b.append(pc.toString() + newLine);

		b.append(this.ruleSets.toString() + newLine);

		b.append("Signal groups:" + newLine);
		for (UUID signalGroupID : this.signalGroups.keySet()) {
			ISignalGroup group = this.signalGroups.get(signalGroupID);
			int count = this.getSignalManager().getSignalCount(signalGroupID);
			b.append(signalGroupID.toString() + ": " + group.toString() + " | " + count + newLine);
		}

		if(this.hasConsensus()){
			b.append("Consensus:" + newLine);
			b.append(getConsensus().toString()+newLine);
		}

		return b.toString();
	}

	private synchronized void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

		in.defaultReadObject();
		
		
//		isRefolding = false;

		if (ruleSets == null || ruleSets.isEmpty()) {
			log("Creating default ruleset for collection");
			ruleSets = RuleSetCollection.createDefaultRuleSet(nucleusType);
		}

		statsCache = new StatsCache();
		vennCache = new VennCache();

		signalManager = new SignalManager(this);
		profileManager = new ProfileManager(this);

		// Make sure any profile aggregates match the length of saved segments
		try {
			this.profileCollection.createAndRestoreProfileAggregate(this);
		}catch(ProfileException e) {
			warn("Unable to restore profile aggregate");
			stack(e);
		}
		
		if(this.hasConsensus())
			this.getConsensus().alignVertically();

	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((consensusNucleus == null) ? 0 : consensusNucleus.hashCode());
		result = prime * result + ((folder == null) ? 0 : folder.hashCode());
		result = prime * result + ((cells == null) ? 0 : cells.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((nucleusType == null) ? 0 : nucleusType.hashCode());
		result = prime * result + ((outputFolder == null) ? 0 : outputFolder.hashCode());
		result = prime * result + ((profileCollection == null) ? 0 : profileCollection.hashCode());
		result = prime * result + ((ruleSets == null) ? 0 : ruleSets.hashCode());
		result = prime * result + ((signalGroups == null) ? 0 : signalGroups.hashCode());
		result = prime * result + ((statsCache == null) ? 0 : statsCache.hashCode());
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
}
