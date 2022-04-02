/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
/* 
  -----------------------
  NUCLEUS COLLECTION CLASS
  -----------------------
  This class contains the nuclei that pass detection criteria
  Provides aggregate stats
  It enables offsets to be calculated based on the median normalised curves
 */

package com.bmskinner.nuclear_morphology.components.datasets;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.DefaultCell;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.measure.StatsCache;
import com.bmskinner.nuclear_morphology.components.measure.VennCache;
import com.bmskinner.nuclear_morphology.components.nuclei.Consensus;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultConsensusNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultProfile;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultProfileAggregate;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultSegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileAggregate;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.LandmarkType;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileManager;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.signals.DefaultSignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.SignalManager;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Store cells and their profiles
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultCellCollection implements ICellCollection {
	
	private static final Logger LOGGER = Logger.getLogger(DefaultCellCollection.class.getName());

	/** Unique collection id */
	private final UUID uuid;
	
	/** The name of the collection */
	private String name;

	/** Aggregated profiles from cells, plus medians */
	private IProfileCollection profileCollection = new DefaultProfileCollection();

	/** Refolded consensus nucleus */
	private Consensus consensusNucleus; 

	/** All the cells in this collection */
	private final List<ICell> cells = new ArrayList<>(20);

	/** Groups of nuclear signals, with detection and display settings,
	 * Note that actual signals are stored within cells */
	private final Set<ISignalGroup> signalGroups = new HashSet<>(0);

	/** Rules used to identify border points */
	private RuleSetCollection ruleSets;

	/**
	 * Cache statistics from the cells in the collection. This should be updated
	 * if a cell is added or lost
	 */
	private StatsCache statsCache = new StatsCache();

	/** cache the number of shared cells with other datasets */
	private VennCache vennCache = new VennCache();

	private SignalManager  signalManager  = new SignalManager(this);
	private ProfileManager profileManager = new ProfileManager(this);


	/**
	 * Constructor
	 * 
	 * @param ruleSets the rules used to identify landmarks
	 * @param name the name of the collection
	 * @param id specify an id for the collection, rather than generating
	 *            randomly. Leave null to use a random id
	 */
	public DefaultCellCollection(@NonNull RuleSetCollection ruleSets,  
			@Nullable String name, 
			@Nullable UUID id) {

		this.uuid = id==null ? UUID.randomUUID() : id;
		this.name = name;
		this.ruleSets = ruleSets;
	}

	/**
	 * Construct an empty collection from a template dataset
	 * 
	 * @param template the dataset to base on for folders and type
	 * @param name the collection name
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
		this(template.getRuleSetCollection(), name, null);
	}

    /**
     * Construct from an XML element. Use for 
     * unmarshalling. The element should conform
     * to the specification in {@link XmlSerializable}.
     * @param e the XML element containing the data.
     */
	public DefaultCellCollection(@NonNull Element e) throws ComponentCreationException {	
		uuid = UUID.fromString(e.getAttributeValue("id"));
		name = e.getAttributeValue("name");
		
		profileCollection = new DefaultProfileCollection(e.getChild("ProfileCollection"));
		
		if(e.getChild("ConsensusNucleus")!=null)
			consensusNucleus = new DefaultConsensusNucleus(e.getChild("ConsensusNucleus"));

		for(Element el : e.getChildren("Cell"))
			cells.add(new DefaultCell(el));
		
		
		for(Element el : e.getChildren("SignalGroup")) {
			signalGroups.add(new DefaultSignalGroup(el));
		}
		ruleSets = new RuleSetCollection(e.getChild("RuleSetCollection"));
		
		try {
			profileCollection.calculateProfiles();
		} catch(ProfileException | MissingLandmarkException | MissingProfileException e1) {
			throw new ComponentCreationException(e1);
		}
	}
	

	@Override
	public Element toXmlElement() {
		Element e = new Element("CellCollection").setAttribute("id", uuid.toString()).setAttribute("name", name);
		e.addContent(profileCollection.toXmlElement());
		
		if(consensusNucleus!=null)
			e.addContent(consensusNucleus.toXmlElement());
		
		for(ICell c : cells)
			e.addContent(c.toXmlElement());
		
		for(ISignalGroup c : signalGroups)
			e.addContent(c.toXmlElement());
		
		
		e.addContent(ruleSets.toXmlElement());
		
		return e;
	}

	@Override
	public ICellCollection duplicate() {
		DefaultCellCollection result = new DefaultCellCollection(ruleSets, name, uuid);
		
		for(ICell c : this)
			result.addCell(c.duplicate());
		
		result.consensusNucleus = consensusNucleus==null? null : consensusNucleus.duplicate();
		result.profileCollection = profileCollection.duplicate();
		
		// copy the signals
        for(ISignalGroup s : getSignalGroups())
        	result.addSignalGroup(s.duplicate());
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
	public UUID getId() {
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

	@Override
	public Set<UUID> getCellIDs() {
		return cells.parallelStream().map(ICell::getId).collect(Collectors.toSet());
	}

	@Override
	public void addCell(final @NonNull ICell r) {
		cells.add(r);
	}

	@Override
	public void replaceCell(@NonNull ICell r) {
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
		cells.removeIf(cell->cell.getId().equals(c.getId()));
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

	@Override
	public ICell getCell(@NonNull UUID id) {
		return cells.parallelStream()
				.filter(c->c.getId().equals(id))
				.findFirst().orElse(null);
	}
	
	@Override
	public Optional<Nucleus> getNucleus(@NonNull UUID id) {
		for(ICell c : this) {
			for(Nucleus n : c.getNuclei()) {
				if(n.getID().equals(id))
					return Optional.ofNullable(n);
			}
		}
		return Optional.empty();
	}

	/*  METHODS IMPLEMENTING THE REFOLDABLE INTERFACE  */

	@Override
	public boolean hasConsensus() {
		return consensusNucleus != null;
	}

	@Override
	public void setConsensus(@Nullable Consensus n) {
		consensusNucleus = n;
	}
	
	@Override
	public Consensus getRawConsensus() {
		return consensusNucleus;
	}

	@Override
	public Nucleus getConsensus() throws MissingLandmarkException {
		return consensusNucleus.getOrientedNucleus();
	}
	
	@Override
	public void offsetConsensus(double xOffset, double yOffset) {
		if(consensusNucleus!=null)
			consensusNucleus.offset(xOffset, yOffset);
	}

	@Override
	public void rotateConsensus(double degrees) {
		if(consensusNucleus!=null)
			consensusNucleus.addRotation(degrees);
	}
	
	@Override
	public IPoint currentConsensusOffset() {
		return consensusNucleus.currentOffset();
	}
	
	@Override
	public double currentConsensusRotation() {
		return consensusNucleus.currentRotation();
	}
	
	@Override
	public IProfileCollection getProfileCollection() {
		return profileCollection;
	}

	@Override
	public synchronized Set<File> getImageFiles() {
		return getNuclei().stream().map(Nucleus::getSourceFile).collect(Collectors.toSet());
	}

	/**
	 * Get the border lengths of the nuclei in this collection as an array
	 * 
	 * @return
	 */
	private synchronized int[] getArrayLengths() {
		return getNuclei().stream().mapToInt(Nucleus::getBorderLength).toArray();
	}

	@Override
	public int getNucleusCount() {
		return this.getNuclei().size();
	}

	@Override
	public List<ICell> getCells() {
		return cells;
	}


	@Override
	public Stream<ICell> streamCells() {
		return cells.stream();
	}

	@Override
	public synchronized Set<ICell> getCells(@NonNull File imageFile) {
		return cells.stream().filter(c->c.getPrimaryNucleus().getSourceFile().equals(imageFile))
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

	/**
	 * Get the differences between the median profile and each nucleus. This is the sum-of-squares difference,
	 * rooted and divided by the nuclear perimeter. Each profile is normalised to {@link ICellCollection#FIXED_PROFILE_LENGTH}
	 * 
	 * @param pointType the tag to zero profiles against
	 * @return an array of normalised differences
	 */
	private synchronized double[] getNormalisedDifferencesToMedianFromPoint(Landmark pointType) {
		IProfile medianProfile;
		try {
			medianProfile = this.getProfileCollection().getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN)
					.interpolate(FIXED_PROFILE_LENGTH);
		} catch (MissingComponentException | ProfileException e) {
			LOGGER.warning("Cannot get median profile for collection");
			LOGGER.log(Loggable.STACK, "Error getting median profile", e);
			double[] result = new double[size()];
			Arrays.fill(result, Double.MAX_VALUE);
			return result;
		}
		

		return getNuclei().stream().mapToDouble(n->{
			try {

				IProfile angleProfile = n.getProfile(ProfileType.ANGLE, pointType);
				double diff = angleProfile.absoluteSquareDifference(medianProfile, FIXED_PROFILE_LENGTH);
				return Math.sqrt(diff/FIXED_PROFILE_LENGTH); // differences in degrees, rather than square degrees

			} catch (ProfileException | MissingLandmarkException | MissingProfileException e) {
			 LOGGER.log(Loggable.STACK, "Error getting nucleus profile", e);
				return  Double.NaN;
			} 
		}).toArray();
	}

	/**
	 * Get the perimeter normalised veriabililty of a nucleus angle profile
	 * compared to the median profile of the collection
	 * 
	 * @param pointType the tag to use as index 0
	 * @param c the cell to test
	 * @return the variabililty score of the nucleus
	 * @throws Exception
	 */
	@Override
	public double getNormalisedDifferenceToMedian(Landmark pointType, Taggable t) {
		IProfile medianProfile;
		try {
			medianProfile = profileCollection.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN).interpolate(FIXED_PROFILE_LENGTH);
		} catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
			LOGGER.log(Loggable.STACK, "Error getting median profile for collection", e);
			return 0;
		}

		try {
			IProfile angleProfile = t.getProfile(ProfileType.ANGLE, pointType);

			double diff = angleProfile.absoluteSquareDifference(medianProfile, FIXED_PROFILE_LENGTH);
			return Math.sqrt(diff/FIXED_PROFILE_LENGTH); // use the differences in degrees divided by the pixel interplation length
		} catch (ProfileException | MissingComponentException e) {
			LOGGER.log(Loggable.STACK, "Error getting nucleus profile", e);
			return 0;
		}
	}

	/**
	 * Get the distances between two border tags for each nucleus
	 * 
	 * @param pointTypeA
	 * @param pointTypeB
	 * @return
	 */
	public double[] getPointToPointDistances(Landmark pointTypeA, Landmark pointTypeB) {
		int count = this.getNucleusCount();
		double[] result = new double[count];
		int i = 0;
		for (Nucleus n : this.getNuclei()) {
			try {
				result[i] = n.getBorderPoint(pointTypeA).getLengthTo(n.getBorderPoint(pointTypeB));
			} catch (MissingLandmarkException e) {
				LOGGER.log(Loggable.STACK, "Tag not present: " + pointTypeA + " or " + pointTypeB, e);
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
	 * @param pointType the point to compare profiles from
	 * @return the best nucleus
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 */
	@Override
	public Nucleus getNucleusMostSimilarToMedian(Landmark pointType)
			throws ProfileException, MissingLandmarkException, MissingProfileException {
		
		Set<Nucleus> list = this.getNuclei();

		// No need to check profiles if there is only one nucleus
		if (this.size() == 1)
			return list.stream().findFirst().get();

		IProfile medianProfile = profileCollection.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN).interpolate(FIXED_PROFILE_LENGTH);

		Nucleus result = null;

		double difference = Double.MAX_VALUE;
		for (Nucleus p : list) {
			IProfile profile = p.getProfile(ProfileType.ANGLE, pointType);
			double nDifference = profile.absoluteSquareDifference(medianProfile, FIXED_PROFILE_LENGTH);
			if (nDifference < difference) {
				difference = nDifference;
				result = p;
			}
		}

		if (result == null)
			throw new ProfileException("Error finding nucleus similar to median");
		return result;
	}

	/*
	 * 
	 * METHODS IMPLEMENTING THE STATISTICAL COLLECTION INTERFACE
	 * 
	 */

	@Override
	public void clear(Measurement stat, String component) {
		statsCache.clear(stat, component, null);
	}

	@Override
	public void clear(Measurement stat, String component, UUID id) {
		statsCache.clear(stat, component, id);
	}

	@Override
	public void clear(MeasurementScale scale) {
		statsCache.clear(scale);
	}

	@Override
	public synchronized double getMedian(@NonNull Measurement stat, String component, MeasurementScale scale) {
		return getMedianStatistic(stat, component, scale, null);
	}

	@Override
	public synchronized double getMedian(@NonNull Measurement stat, String component, MeasurementScale scale,
			UUID id){

		if (CellularComponent.NUCLEAR_SIGNAL.equals(component)) {
			return getMedianStatistic(stat, component, scale, id);
		}

		if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)) {
			return getMedianStatistic(stat, component, scale, id);
		}
		return 0;
	}

	@Override
	public synchronized double[] getRawValues(@NonNull Measurement stat, String component,
			MeasurementScale scale) {

		return getRawValues(stat, component, scale, null);

	}

	@Override
	public synchronized double[] getRawValues(@NonNull Measurement stat, String component, MeasurementScale scale,
			UUID id) {

		switch(component) {
		case CellularComponent.WHOLE_CELL: return getCellStatistics(stat, scale);
		case CellularComponent.NUCLEUS: return getNuclearStatistics(stat, scale);
		case CellularComponent.NUCLEAR_BORDER_SEGMENT: return getSegmentStatistics(stat, scale, id);
		default: {
			LOGGER.warning("No component of type " + component + " can be handled");
			return new double[0];
		}
		}
	}

	private synchronized double getMedianStatistic(Measurement stat, String component, MeasurementScale scale,
			UUID id) {

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
	public synchronized double getMin(@NonNull Measurement stat, String component, MeasurementScale scale) {
		return getMinStatistic(stat, component, scale, null);
	}

	@Override
	public synchronized double getMin(@NonNull Measurement stat, String component, MeasurementScale scale,
			UUID id){

		// Handle old segment and SignalStatistic enums
		if (CellularComponent.NUCLEAR_SIGNAL.equals(component)) {
			return getMinStatistic(stat, CellularComponent.NUCLEAR_SIGNAL, scale, id);
		}

		if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)) {
			return getMinStatistic(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);
		}
		return getMinStatistic(stat, component, scale, id);
	}

	private synchronized double getMinStatistic(@NonNull Measurement stat, String component, MeasurementScale scale,
			UUID id) {

		double[] values = getRawValues(stat, component, scale, id);
		return Arrays.stream(values).min().orElse(Statistical.ERROR_CALCULATING_STAT);
	}

	@Override
	public synchronized double getMax(@NonNull Measurement stat, String component, MeasurementScale scale) {
		return getMaxStatistic(stat, component, scale, null);
	}

	@Override
	public synchronized double getMax(@NonNull Measurement stat, String component, MeasurementScale scale,
			UUID id){

		// Handle old segment andSignalStatistic enums
		if (CellularComponent.NUCLEAR_SIGNAL.equals(component))
			return getMaxStatistic(stat, CellularComponent.NUCLEAR_SIGNAL, scale, id);
		if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component))
			return getMaxStatistic(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);
		return getMaxStatistic(stat, component, scale, id);
	}

	private synchronized double getMaxStatistic(@NonNull Measurement stat, String component, MeasurementScale scale,
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
	private synchronized double[] getCellStatistics(@NonNull Measurement stat, @NonNull MeasurementScale scale) {

		double[] result = null;

		if (statsCache.hasValues(stat, CellularComponent.WHOLE_CELL, scale, null))
			return statsCache.getValues(stat, CellularComponent.WHOLE_CELL, scale, null);
		result = cells.parallelStream().mapToDouble(c -> c.getStatistic(stat, scale)).sorted().toArray();
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
	private synchronized double[] getNuclearStatistics(@NonNull Measurement stat, @NonNull MeasurementScale scale) {

		double[] result = null;

		if (statsCache.hasValues(stat, CellularComponent.NUCLEUS, scale, null)) {
			return statsCache.getValues(stat, CellularComponent.NUCLEUS, scale, null);

		}
		if (Measurement.VARIABILITY.equals(stat)) {
			result = this.getNormalisedDifferencesToMedianFromPoint(Landmark.REFERENCE_POINT);
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
	private synchronized double[] getSegmentStatistics(@NonNull Measurement stat, @NonNull MeasurementScale scale, @NonNull UUID id){

		double[] result = null;
		if (statsCache.hasValues(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id)) {
			return statsCache.getValues(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);
		}
		AtomicInteger errorCount= new AtomicInteger(0);
		result = getNuclei().parallelStream().mapToDouble(n -> {
			IProfileSegment segment;
			try {
				segment = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT).getSegment(id);
			} catch (ProfileException | MissingComponentException e) {
				LOGGER.log(Loggable.STACK, String.format("Error getting segment %s from nucleus %s in DefaultCellCollection::getSegmentStatistics", id, n.getNameAndNumber()), e);
				errorCount.incrementAndGet();
				return 0;
			}
			double perimeterLength = 0;
			if (segment != null) {
				int indexLength = segment.length();
				double fractionOfPerimeter = (double) indexLength / (double) segment.getProfileLength();
				perimeterLength = fractionOfPerimeter * n.getStatistic(Measurement.PERIMETER, scale);
			}
			return perimeterLength;

		}).sorted().toArray();

		statsCache.setValues(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id, result);
		if(errorCount.get()>0)
			LOGGER.warning(String.format("Problem calculating segment stats for segment %s: %d nuclei had errors getting this segment", id, errorCount.get()));
		return result;
	}

	/*
	 * 
	 * METHODS IMPLEMENTING THE FILTERABLE INTERFACE
	 * 
	 */

//	@Override
//	public ICellCollection filter(@NonNull Predicate<ICell> predicate) {
//
//		String newName = "Filtered_" + predicate.toString();
//
//		ICellCollection subCollection = new DefaultCellCollection(this, newName);
//
//		List<ICell> list = cells.parallelStream().filter(predicate).collect(Collectors.toList());
//
//		for (ICell cell : list)
//			subCollection.addCell(cell);
//
//		if (subCollection.size() == 0) {
//			LOGGER.warning("No cells in collection");
//			return subCollection;
//		}
//
//		try {
//			subCollection.createProfileCollection();
//			this.getProfileManager().copySegmentsAndLandmarksTo(subCollection);
//			this.getSignalManager().copySignalGroupsTo(subCollection);
//
//		} catch (ProfileException e) {
//			LOGGER.warning("Error copying collection offsets");
//			LOGGER.log(Loggable.STACK, "Error in offsetting", e);
//		}
//
//		return subCollection;
//	}

//	@Override
//	public ICellCollection filterCollection(@NonNull Measurement stat, MeasurementScale scale, 
//			double lower,
//			double upper) {
//		DecimalFormat df = new DecimalFormat("#.##");
//
//		Predicate<ICell> pred = new Predicate<ICell>() {
//			@Override
//			public boolean test(ICell t) {
//
//				for (Nucleus n : t.getNuclei()) {
//
//					double value = stat.equals(Measurement.VARIABILITY)
//							? getNormalisedDifferenceToMedian(Landmark.REFERENCE_POINT, n) : n.getStatistic(stat, scale);
//
//							if (value < lower) {
//								return false;
//							}
//
//							if (value > upper) {
//								return false;
//							}
//
//				}
//				return true;
//			}
//
//			@Override
//			public String toString() {
//				return stat.toString() + "_" + df.format(lower) + "-" + df.format(upper);
//			}
//
//		};
//
//		return filter(pred);
//	}


	@Override
	public void setSourceFolder(@NonNull File newFolder) {
		if(!newFolder.exists())
			return;   

		cells.stream()
		.flatMap(c->c.getNuclei().stream())
		.forEach(n->{
			File oldFolder = n.getSourceFolder();
			n.setSourceFolder(newFolder);
			// Update signals in the same file
			n.getSignalCollection().getAllSignals().stream()
			.forEach(s->{
				if(s.getSourceFolder().equals(oldFolder))
					s.setSourceFolder(newFolder);
			});

		});

	}

	@Override
	public boolean contains(ICell c) {
		if(c==null)
			return false;
		return contains(c.getId());
	}
	
	@Override
	public boolean contains(Nucleus nucleus) {
		if(nucleus==null)
			return false;
		
		for(Nucleus n : getNuclei()) {
			if(n.getID().equals(nucleus.getID()))
				return true;
		}
		
		return false;
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

		Set<UUID> ids = signalGroups.stream().map(s->s.getId()).collect(Collectors.toSet());
		ids.remove(IShellResult.RANDOM_SIGNAL_ID);
		return ids;
	}

	@Override
	public Collection<ISignalGroup> getSignalGroups() {
		return signalGroups;
	}

	/**
	 * Fetch the signal group with the given ID
	 * 
	 * @param id
	 * @return
	 */
	@Override
	public Optional<ISignalGroup> getSignalGroup(@NonNull UUID id) {
		return signalGroups.stream().filter(s->s.getId().equals(id)).findFirst();
	}

	@Override
	public void addSignalGroup(@NonNull ISignalGroup group) {
		signalGroups.add(group);
	}

	@Override
	public boolean hasSignalGroup(@NonNull UUID id) {
		return signalGroups.stream().anyMatch(s->s.getId().equals(id));
	}

	/**
	 * Remove the given group
	 * 
	 * @param id
	 */
	@Override
	public void removeSignalGroup(@NonNull UUID id) {
		signalGroups.removeIf(s->s.getId().equals(id));
        cells.stream().flatMap(c->c.getNuclei().stream())
        .forEach(n->n.getSignalCollection().removeSignals(id));
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
		

		// Ensure cells use the same rule
		if (!d2.getRuleSetCollection().equals(ruleSets))
			return 0;

		Set<UUID> toSearch = new HashSet<>(d2.getCellIDs());
		toSearch.retainAll(getCellIDs());
		return toSearch.size();
	}

	@Override
	public String toString() {

		String newLine = System.getProperty("line.separator");

		StringBuilder b = new StringBuilder("Collection:" + getName() + newLine)
				.append("Class: "+this.getClass().getSimpleName()+newLine)
				.append("Nuclei: " + this.getNucleusCount() + newLine)
				.append("Profile collections:" + newLine)
				.append(profileCollection.toString()+newLine);

		b.append(this.ruleSets.toString() + newLine);

		b.append("Signal groups:" + newLine);
		for(ISignalGroup entry : signalGroups) {
			UUID signalGroupID = entry.getId();
			int count = this.getSignalManager().getSignalCount(signalGroupID);
			b.append(entry.toString() + " | " + count +" signals across all cells"+ newLine);
		}

		if(this.hasConsensus()){
			b.append("Consensus:" + newLine);
			
			try {
				b.append(getConsensus().toString()+newLine);
			} catch (MissingLandmarkException e) {
				b.append("Cannot orient consensus; "+e.getMessage()+newLine);
			}
		}

		return b.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((consensusNucleus == null) ? 0 : consensusNucleus.hashCode());
		result = prime * result + ((cells == null) ? 0 : cells.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((profileCollection == null) ? 0 : profileCollection.hashCode());
		result = prime * result + ((ruleSets == null) ? 0 : ruleSets.hashCode());
		result = prime * result + ((signalGroups == null) ? 0 : signalGroups.hashCode());
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
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		
		if (consensusNucleus == null) {
			if (other.consensusNucleus != null)
				return false;
		} else if (!consensusNucleus.equals(other.consensusNucleus))
			return false;		
		if (cells == null) {
			if (other.cells != null)
				return false;
		} else if (!cells.equals(other.cells))
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
		} else {
			if(signalGroups.size()!=other.signalGroups.size())
				return false;
			for(ISignalGroup s : signalGroups) {
				if(!other.signalGroups.contains(s))
					return false;
			}
		}
		

		return true;
	}
	
	
	/**
	 * Store the median profiles 
	 * @author ben
	 * @since 2.0.0
	 *
	 */
	public class DefaultProfileCollection implements IProfileCollection {
		
	    /** indexes of landmarks in the median profile with RP at zero */
	    private Map<Landmark, Integer> indexes  = new HashMap<>();
	    
	    /** segments in the median profile with RP at zero */
	    private List<IProfileSegment> segments = new ArrayList<>();

	    /** cached median profiles for quicker access */
	    private ProfileCache cache = new ProfileCache();

	    /**
	     * Create an empty profile collection. The RP is set to the zero index by default.
	     */
	    public DefaultProfileCollection() {
	        indexes.put(Landmark.REFERENCE_POINT, ZERO_INDEX);
	    }
	        
	    /**
	     * Construct from an XML element. Use for 
	     * unmarshalling. The element should conform
	     * to the specification in {@link XmlSerializable}.
	     * @param e the XML element containing the data.
	     */
	    public DefaultProfileCollection(Element e) {
	    	
	    	for(Element el : e.getChildren("Landmark")){
	    		indexes.put(Landmark.of(el.getAttribute("name").getValue(), 
						LandmarkType.valueOf(el.getAttribute("type").getValue())), 
						Integer.parseInt(el.getText()));
			}
			
			for(Element el : e.getChildren("Segment")){
				segments.add(new DefaultProfileSegment(el));
			}

	    }
	    
	    /**
	     * Used for duplicating
	     * @param p
	     */
	    private DefaultProfileCollection(DefaultProfileCollection p) {
	    	for(Landmark t : p.indexes.keySet())
				indexes.put(t, p.indexes.get(t));
	    	
	    	for(IProfileSegment s : p.segments)
				segments.add(s.duplicate());
	    		    	
	    	cache = p.cache.duplicate();
	    }

		@Override
		public IProfileCollection duplicate() {
			return new DefaultProfileCollection(this);
		}
	    
	    @Override
	    public int segmentCount() {
	    	if(segments==null)
	    		return 0;
	    	return segments.size();
	    }
	    
	    @Override
	    public boolean hasSegments() {
	    	return segmentCount()>0;
	    }

	    @Override
	    public int getLandmarkIndex(@NonNull Landmark pointType) throws MissingLandmarkException {
	        if(indexes.containsKey(pointType))
	            return indexes.get(pointType);
			throw new MissingLandmarkException(pointType+" is not present in this profile collection");
	    }

	    @Override
	    public List<Landmark> getLandmarks() {
	        List<Landmark> result = new ArrayList<>();
	        for (Landmark s : indexes.keySet()) {
	            result.add(s);
	        }
	        return result;
	    }

	    @Override
	    public boolean hasLandmark(@NonNull Landmark tag) {
	        return indexes.keySet().contains(tag);
	    }

	    @Override
	    public IProfile getProfile(@NonNull ProfileType type, @NonNull Landmark tag, int quartile)
	            throws MissingLandmarkException, ProfileException, MissingProfileException {
	        if (!this.hasLandmark(tag))
	            throw new MissingLandmarkException("Tag is not present: " + tag.toString());

	        if(!cache.hasProfile(type, quartile, tag)) {
	        	IProfileAggregate agg = createProfileAggregate(type, DefaultCellCollection.this.getMedianArrayLength());
	        	
	        	IProfile p = agg.getQuartile(quartile);
	            int offset = indexes.get(tag);
	            p = p.startFrom(offset);
	            cache.addProfile(type, quartile, tag, p);
	        }
	        	
	        return cache.getProfile(type, quartile, tag);
	    }

	    @Override
	    public ISegmentedProfile getSegmentedProfile(@NonNull ProfileType type, @NonNull Landmark tag, 
	    		int quartile)
	            throws MissingLandmarkException, ProfileException, MissingProfileException {

	        if (quartile < 0 || quartile > 100)
	            throw new IllegalArgumentException("Quartile must be between 0-100");

	        // get the profile array
	        IProfile p = getProfile(type, tag, quartile);
	        if (segments.isEmpty())
	        	throw new UnsegmentedProfileException("No segments assigned to profile collection");
	        
	        return new DefaultSegmentedProfile(p, getSegments(tag));
	    }
	    
	    @Override
	    public void calculateProfiles() throws MissingLandmarkException, MissingProfileException, ProfileException {
	    	cache.clear();
	    	for(ProfileType t : ProfileType.values()) {
	    		for(Landmark lm : indexes.keySet()) {
	    			getProfile(t, lm, Stats.MEDIAN);
	    			getProfile(t, lm, Stats.LOWER_QUARTILE);
	    			getProfile(t, lm, Stats.UPPER_QUARTILE);
	    		}
	    	}
	    }

	    @Override
	    public synchronized List<UUID> getSegmentIDs() {
	        List<UUID> result = new ArrayList<>();
	        if (segments == null)
	            return result;
	        for (IProfileSegment seg : this.segments) {
	            result.add(seg.getID());
	        }
	        return result;
	    }

	    @Override
	    public synchronized IProfileSegment getSegmentAt(@NonNull Landmark tag, int position) throws MissingLandmarkException {
	        return this.getSegments(tag).get(position);
	    }

	    @Override
	    public synchronized List<IProfileSegment> getSegments(@NonNull Landmark tag) throws MissingLandmarkException {

	        // this must be negative offset for segments
	        // since we are moving the pointIndex back to the beginning
	        // of the array
	        int offset = -getLandmarkIndex(tag);
	        
	        List<IProfileSegment> result = new ArrayList<>();        
	        
	        for(IProfileSegment s : segments) {
	        	result.add(s.duplicate().offset(offset));
	        }
	        
	        try {
	        	IProfileSegment.linkSegments(result);
	        	return result;
	        } catch (ProfileException e) {
	        	LOGGER.log(Loggable.STACK, "Could not get segments from "+tag, e);
	        	e.printStackTrace();
	        	 return new ArrayList<>();     
	        }
	    }

	    @Override
	    public IProfileSegment getSegmentContaining(@NonNull Landmark tag) throws ProfileException, MissingLandmarkException {
	        List<IProfileSegment> segments = this.getSegments(tag);

	        IProfileSegment result = null;
	        for (IProfileSegment seg : segments) {
	            if (seg.contains(ZERO_INDEX)) 
	                return seg;
	        }

	        return result;
	    }

	    @Override
	    public void setLandmark(@NonNull Landmark tag, int newIndex) {
	        // Cannot move the RP from zero
	        if (tag.equals(Landmark.REFERENCE_POINT))
	            return;
	        cache.remove(tag);
	        indexes.put(tag, newIndex);

	    }

	    @Override
	    public void setSegments(@NonNull List<IProfileSegment> n) throws MissingLandmarkException {
	        if (n.isEmpty())
	            throw new IllegalArgumentException("String or segment list is empty");

	        int length = DefaultCellCollection.this.getMedianArrayLength();
	        if (length != n.get(0).getProfileLength())
	        	throw new IllegalArgumentException(String.format("Segment profile length (%d) does not fit aggregate length (%d)",
	        			n.get(0).getProfileLength(), length));

	        /*
	         * The segments coming in are zeroed to the given pointType pointIndex
	         * This means the indexes must be moved forwards appropriately. Hence,
	         * add a positive offset.
	         */
	        int offset = getLandmarkIndex(Landmark.REFERENCE_POINT);

	    	for(IProfileSegment s : n) {
	    		s.offset(offset);
	    	}
	    	
	    	
	    	segments = new ArrayList<>();
	    	for(IProfileSegment s : n) {
	    		segments.add(s.duplicate());
	    	}
	    }

	    private IProfileAggregate createProfileAggregate(@NonNull ProfileType type, int length) throws ProfileException, MissingLandmarkException, MissingProfileException {
	        if (length <= 0)
	            throw new IllegalArgumentException("Requested profile aggregate length is zero or negative");
	        
	        cache.clear();
	        
	        // If there are segments, not just the default segment, and the segment 
	        // profile length is different to the required length. Interpolation needed.
	        if (segments.size()>1 && length != segments.get(0).getProfileLength()) {
	        	LOGGER.fine("Segments already exist, interpolating");
	        	return createProfileAggregateOfDifferentLength(type, length);
	        }
	        
	        // No current segments are present. Make a default segment spanning the entire profile
	        if(segments.isEmpty()) {
	        	segments.add(new DefaultProfileSegment(0, 0, length, IProfileCollection.DEFAULT_SEGMENT_ID));
	        }

	        IProfileAggregate agg = new DefaultProfileAggregate(length, DefaultCellCollection.this.size());

	    	for (Nucleus n : DefaultCellCollection.this.getNuclei())
	    		agg.addValues(n.getProfile(type, Landmark.REFERENCE_POINT));
	    	return agg;

	    }
	    
	    /**
	     * Allow a profile aggregate to be created and segments copied when median profile lengths have
	     * changed.
	     * @param collection
	     * @param length
	     * @throws MissingProfileException 
	     * @throws MissingLandmarkException 
	     */
	    private IProfileAggregate createProfileAggregateOfDifferentLength(@NonNull ProfileType type, int length) throws ProfileException, MissingLandmarkException, MissingProfileException {
	    	indexes.put(Landmark.REFERENCE_POINT, ZERO_INDEX);

	    	// We have no profile to use to interpolate segments.
	    	// Create an arbitrary profile with the original length.

	    	List<IProfileSegment> originalSegList = new ArrayList<>();
	    	for(IProfileSegment s : segments)
	    		originalSegList.add(s);
	    	IProfile template = new DefaultProfile(0, segments.get(0).getProfileLength());
	    	ISegmentedProfile segTemplate = new DefaultSegmentedProfile(template, originalSegList);

	    	// Now use the interpolation method to adjust the segment lengths
	    	List<IProfileSegment> interpolatedSegments = segTemplate.interpolate(length).getSegments();

	    	IProfileAggregate agg = new DefaultProfileAggregate(length, DefaultCellCollection.this.size());

	    	for (Nucleus n : DefaultCellCollection.this.getNuclei())
	    		agg.addValues(n.getProfile(type, Landmark.REFERENCE_POINT));

	    	setSegments(interpolatedSegments);
	    	
	    	return agg;
	    }

	    @Override
	    public String toString() {
	    	String newLine = System.getProperty("line.separator");
	    	StringBuilder builder = new StringBuilder();
	    	builder.append("Point types:"+newLine);
	    	for (Entry<Landmark, Integer> e : indexes.entrySet()) {
	    		builder.append(e.getKey() + ": " + e.getValue()+newLine);
	    	}
	    	
	    	// Show segments from RP
	    	try {
	    		builder.append("Segments from RP:"+newLine);
	    		for (IProfileSegment s : this.getSegments(Landmark.REFERENCE_POINT)) {
	    			builder.append(s.toString() + newLine);
	    		}

	    	} catch (Exception e) {
	    		builder.append("Exception fetching segments: "+e.getMessage());
	    	}

	    	return builder.toString();

	    }

	    @Override
	    public IProfile getIQRProfile(@NonNull ProfileType type, @NonNull Landmark tag)
	            throws MissingLandmarkException, ProfileException, MissingProfileException {

	        IProfile q25 = getProfile(type, tag, Stats.LOWER_QUARTILE);
	        IProfile q75 = getProfile(type, tag, Stats.UPPER_QUARTILE);

	        if (q25 == null || q75 == null)
	        	throw new ProfileException("Could not create q25 or q75 profile");

	        return q75.subtract(q25);
	    }

	    @Override
		public int hashCode() {
			return Objects.hash(indexes, segments);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DefaultProfileCollection other = (DefaultProfileCollection) obj;
			return Objects.equals(indexes, other.indexes) && Objects.equals(segments, other.segments);
		}

		@Override
		public Element toXmlElement() {
			Element e = new Element("ProfileCollection");
					
			for(IProfileSegment s : segments) {
				e.addContent(s.toXmlElement());
			}
			
			for(Entry<Landmark, Integer> entry : indexes.entrySet()) {
				e.addContent(new Element("Landmark")
						.setAttribute("name", entry.getKey().toString())
						.setAttribute("type", entry.getKey().type().toString())
						.setText(String.valueOf(entry.getValue())));
			}
			
			return e;
		}

		/**
	     * The cache for profiles
	     * 
	     * @author bms41
	     * @since 1.13.4
	     *
	     */
	    private class ProfileCache {

	        /**
	         * The key used to store values in the cache
	         * 
	         * @author bms41
	         * @since 1.13.4
	         *
	         */
	        private class ProfileKey {
	            private final ProfileType type;
	            private final double      quartile;
	            private final Landmark         tag;

	            public ProfileKey(final ProfileType type, final double quartile, final Landmark tag) {

	                this.type = type;
	                this.quartile = quartile;
	                this.tag = tag;
	            }

	            public boolean has(Landmark t) {
	                return tag.equals(t);
	            }

	            @Override
	            public int hashCode() {
	                final int prime = 31;
	                int result = 1;
	                result = prime * result + getOuterType().hashCode();
	                long temp;
	                temp = Double.doubleToLongBits(quartile);
	                result = prime * result + (int) (temp ^ (temp >>> 32));
	                result = prime * result + ((tag == null) ? 0 : tag.hashCode());
	                result = prime * result + ((type == null) ? 0 : type.hashCode());
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
	                ProfileKey other = (ProfileKey) obj;
	                if (!getOuterType().equals(other.getOuterType()))
	                    return false;
	                if (Double.doubleToLongBits(quartile) != Double.doubleToLongBits(other.quartile))
	                    return false;
	                if (tag == null) {
	                    if (other.tag != null)
	                        return false;
	                } else if (!tag.equals(other.tag))
	                    return false;
	                if (type != other.type)
	                    return false;
	                return true;
	            }

	            private DefaultProfileCollection getOuterType() {
	                return DefaultProfileCollection.this;
	            }

	        }

	        private Map<ProfileKey, IProfile> map = new HashMap<>();

	        public ProfileCache() { // no default data
	        }
	        
	        public ProfileCache duplicate() {
	        	ProfileCache result = new ProfileCache();
	        	try {
	        		for(ProfileKey k : map.keySet()) {
	        			IProfile p = map.get(k);
	        			if(p!=null)
	        				result.map.put(k, p.copy());
	        		}
	        	} catch(ProfileException e) {

	        	}
	        	return result;
	        }

	        /**
	         * Add a profile with the given keys
	         * 
	         * @param type the profile type
	         * @param quartile the quartile of the dataset
	         * @param tag the tag
	         * @param profile the profile to save
	         */
	        public void addProfile(final ProfileType type, final double quartile, final Landmark tag, IProfile profile) {
	            ProfileKey key = new ProfileKey(type, quartile, tag);
	            map.put(key, profile);
	        }

	        public boolean hasProfile(final ProfileType type, final double quartile, final Landmark tag) {
	            ProfileKey key = new ProfileKey(type, quartile, tag);
	            return map.containsKey(key);
	        }

	        public IProfile getProfile(final ProfileType type, final double quartile, final Landmark tag) {
	            ProfileKey key = new ProfileKey(type, quartile, tag);
	            return map.get(key);
	        }

	        public void remove(final ProfileType type, final double quartile, final Landmark tag) {
	            ProfileKey key = new ProfileKey(type, quartile, tag);
	            map.remove(key);
	        }
	        
	        /**
	         * Remove all profiles from the cache
	         */
	        public void clear() {
	        	map.clear();
	        }

	        public void remove(final Landmark t) {

	            Iterator<ProfileKey> it = map.keySet().iterator();
	            while (it.hasNext()) {
	                ProfileKey k = it.next();
	                if (k.has(t)) {
	                    it.remove();
	                }
	            }

	        }
	    }

		@Override
		public double getProportionOfIndex(int index) {
			int length = DefaultCellCollection.this.getMedianArrayLength();
			if (index < 0 || index >= length)
	            throw new IllegalArgumentException("Index out of bounds: " + index);
			if(index==0)
				return 0;
			if(index==length-1)
				return 1;
	        return (double) index / (double) (length-1);
		}

		@Override
		public double getProportionOfIndex(@NonNull Landmark tag) throws MissingLandmarkException {
			return getProportionOfIndex(getLandmarkIndex(tag));
		}

		@Override
		public int getIndexOfProportion(double proportion) {
			if (proportion < 0 || proportion > 1)
				throw new IllegalArgumentException("Proportion must be between 0-1: " + proportion);
			if(proportion==0)
				return 0;
			
			int length = DefaultCellCollection.this.getMedianArrayLength();
			if(proportion==1)
				return length-1;
			
			double desiredDistanceFromStart = (double) length * proportion;
			int target = (int) desiredDistanceFromStart;
			return target;
		}
	}
}
