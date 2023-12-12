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

package com.bmskinner.nma.components.datasets;

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

import com.bmskinner.nma.analysis.ProgressEvent;
import com.bmskinner.nma.analysis.ProgressListener;
import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Consensus;
import com.bmskinner.nma.components.cells.DefaultCell;
import com.bmskinner.nma.components.cells.DefaultConsensusNucleus;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.measure.MeasurementCache;
import com.bmskinner.nma.components.measure.VennCache;
import com.bmskinner.nma.components.profiles.DefaultLandmark;
import com.bmskinner.nma.components.profiles.DefaultProfile;
import com.bmskinner.nma.components.profiles.DefaultProfileAggregate;
import com.bmskinner.nma.components.profiles.DefaultProfileSegment;
import com.bmskinner.nma.components.profiles.DefaultSegmentedProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileAggregate;
import com.bmskinner.nma.components.profiles.IProfileCollection;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileManager;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.profiles.UnsegmentedProfileException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.components.signals.DefaultSignalGroup;
import com.bmskinner.nma.components.signals.IShellResult;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.components.signals.SignalManager;
import com.bmskinner.nma.io.XmlSerializable;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

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
	@NonNull
	private final UUID uuid;

	/** The name of the collection */
	@NonNull
	private String name;

	/** Aggregated profiles from cells, plus medians */
	@NonNull
	private IProfileCollection profileCollection;

	/** Refolded consensus nucleus */
	private Consensus consensusNucleus;

	/** All the cells in this collection */
	private final List<ICell> cells = new ArrayList<>(20);

	/**
	 * Groups of nuclear signals, with detection and display settings, Note that
	 * actual signals are stored within cells
	 */
	@NonNull
	private final Set<ISignalGroup> signalGroups = new HashSet<>(0);

	/** Rules used to identify border points */
	private RuleSetCollection ruleSets;

	/**
	 * Cache statistics from the cells in the collection. This should be updated if
	 * a cell is added or lost
	 */
	@NonNull
	private final MeasurementCache statsCache = new MeasurementCache();

	/** cache the number of shared cells with other datasets */
	@NonNull
	private final VennCache vennCache = new VennCache();

	@NonNull
	private final SignalManager signalManager = new SignalManager(this);

	@NonNull
	private final ProfileManager profileManager = new ProfileManager(this);

	/**
	 * Constructor
	 * 
	 * @param ruleSets the rules used to identify landmarks
	 * @param name     the name of the collection
	 * @param id       specify an id for the collection, rather than generating
	 *                 randomly. Leave null to use a random id
	 */
	public DefaultCellCollection(@NonNull RuleSetCollection ruleSets, @Nullable String name,
			@Nullable UUID id) {

		this.uuid = id == null ? UUID.randomUUID() : id;
		this.name = name == null ? XMLNames.XML_UNNAMED : name;
		this.ruleSets = ruleSets;
		profileCollection = new DefaultProfileCollection();
	}

	/**
	 * Construct an empty collection from a template dataset
	 * 
	 * @param template the dataset to base on for folders and type
	 * @param name     the collection name
	 */
	public DefaultCellCollection(@NonNull IAnalysisDataset template, String name) {
		this(template.getCollection(), name);
	}

	/**
	 * Construct an empty collection from a template collection
	 * 
	 * @param template
	 * @param name
	 */
	public DefaultCellCollection(@NonNull ICellCollection template, String name) {
		this(template.getRuleSetCollection(), name, null);
	}

	/**
	 * Construct from an XML element. Use for unmarshalling. The element should
	 * conform to the specification in {@link XmlSerializable}.
	 * 
	 * @param e the XML element containing the data.
	 * @param l an optional listener for progress updates
	 */
	public DefaultCellCollection(@NonNull Element e, @Nullable ProgressListener l)
			throws ComponentCreationException {

		uuid = UUID.fromString(e.getAttributeValue(XMLNames.XML_ID));
		name = e.getAttributeValue(XMLNames.XML_NAME);

		// Determine how many items in the progress bar
		List<Element> cellElements = e.getChildren(XMLNames.XML_CELL);
		List<Element> signalElements = e.getChildren(XMLNames.XML_SIGNAL_GROUP);

		// Alert listeners how many elements need to be unpacked
		long totalElements = cellElements.size() + signalElements.size();

		if (l != null)
			l.progressEventReceived(
					new ProgressEvent(this, ProgressEvent.SET_TOTAL_PROGRESS, totalElements));

		profileCollection = new DefaultProfileCollection(
				e.getChild(XMLNames.XML_PROFILE_COLLECTION));

		if (e.getChild(XMLNames.XML_CONSENSUS_NUCLEUS) != null)
			consensusNucleus = new DefaultConsensusNucleus(
					e.getChild(XMLNames.XML_CONSENSUS_NUCLEUS));

		for (Element el : cellElements) {
			cells.add(new DefaultCell(el));

			// Fire progress update if available
			if (l != null)
				l.progressEventReceived(new ProgressEvent(this));
		}

		for (Element el : signalElements) {
			signalGroups.add(new DefaultSignalGroup(el));

			// Fire progress update if available
			if (l != null)
				l.progressEventReceived(new ProgressEvent(this));
		}
		ruleSets = new RuleSetCollection(e.getChild(XMLNames.XML_RULESET_COLLECTION));
	}

	@Override
	public Element toXmlElement() {
		Element e = new Element(XMLNames.XML_CELL_COLLECTION)
				.setAttribute(XMLNames.XML_ID, uuid.toString())
				.setAttribute(XMLNames.XML_NAME, name);
		e.addContent(profileCollection.toXmlElement());

		if (consensusNucleus != null)
			e.addContent(consensusNucleus.toXmlElement());

		for (ICell c : cells)
			e.addContent(c.toXmlElement());

		for (ISignalGroup c : signalGroups)
			e.addContent(c.toXmlElement());

		e.addContent(ruleSets.toXmlElement());

		return e;
	}

	@Override
	public ICellCollection duplicate() throws ComponentCreationException {
		DefaultCellCollection result = new DefaultCellCollection(ruleSets, name, uuid);

		for (ICell c : this)
			result.add(c.duplicate());

		result.consensusNucleus = consensusNucleus == null ? null : consensusNucleus.duplicate();
		result.profileCollection = profileCollection.duplicate();

		// copy the signals
		for (ISignalGroup s : getSignalGroups())
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
		boolean b = cells.add(e);
		if (b)
			statsCache.clear();
		return b;
	}

	@Override
	public boolean addAll(Collection<? extends ICell> c) {
		boolean b = cells.addAll(c);
		if (b)
			statsCache.clear();
		return b;
	}

	@Override
	public void clear() {
		statsCache.clear();
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
		boolean b = cells.remove(o);
		if (b)
			statsCache.clear();
		return b;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean b = cells.removeAll(c);
		if (b)
			statsCache.clear();
		return b;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean b = cells.retainAll(c);
		if (b)
			statsCache.clear();
		return b;
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
		return cells.parallelStream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
	}

	@Override
	public Optional<Nucleus> getNucleus(@NonNull UUID id) {
		for (ICell c : this) {
			for (Nucleus n : c.getNuclei()) {
				if (n.getID().equals(id))
					return Optional.ofNullable(n);
			}
		}
		return Optional.empty();
	}

	/* METHODS IMPLEMENTING THE REFOLDABLE INTERFACE */

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
	public Nucleus getConsensus() throws MissingLandmarkException, ComponentCreationException {
		return consensusNucleus.getOrientedNucleus();
	}

	@Override
	public void offsetConsensus(double xOffset, double yOffset) {
		if (consensusNucleus != null)
			consensusNucleus.setOffset(xOffset, yOffset);
	}

	@Override
	public void rotateConsensus(double degrees) {
		if (consensusNucleus != null)
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
	public Set<File> getImageFiles() {
		return getNuclei().stream().map(Nucleus::getSourceFile).collect(Collectors.toSet());
	}

	/**
	 * Get the border lengths of the nuclei in this collection as an array
	 * 
	 * @return
	 */
	private int[] getArrayLengths() {
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
	public Set<ICell> getCells(@NonNull File imageFile) {
		return cells.stream().filter(c -> c.getPrimaryNucleus().getSourceFile().equals(imageFile))
				.collect(Collectors.toSet());
	}

	@Override
	public boolean hasCells(@NonNull File imageFile) {
		return getCells(imageFile).size() > 0;
	}

	@Override
	public boolean hasNuclei(@NonNull File imageFile) {
		return getNuclei(imageFile).size() > 0;
	}

	@Override
	public List<Nucleus> getNuclei() {
		return cells.stream().flatMap(c -> c.getNuclei().stream()).toList();
	}

	@Override
	public Set<Nucleus> getNuclei(@NonNull File imageFile) {
		return getNuclei().stream().filter(n -> n.getSourceFile().equals(imageFile))
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
	 * Get the differences between the median profile and each nucleus. This is the
	 * sum-of-squares difference, rooted and divided by the nuclear perimeter. Each
	 * profile is normalised to {@link ICellCollection#FIXED_PROFILE_LENGTH}
	 * 
	 * @param om the tag to zero profiles against
	 * @return an array of normalised differences
	 */
	private synchronized double[] getNormalisedDifferencesToMedianFromPoint(OrientationMark om) {
		IProfile medianProfile;
		try {
			medianProfile = this.getProfileCollection()
					.getProfile(ProfileType.ANGLE, om, Stats.MEDIAN)
					.interpolate(FIXED_PROFILE_LENGTH);
		} catch (MissingComponentException | ProfileException e) {
			LOGGER.warning("Cannot get median profile for collection");
			LOGGER.log(Loggable.STACK, "Error getting median profile", e);
			double[] result = new double[size()];
			Arrays.fill(result, Double.MAX_VALUE);
			return result;
		}

		return getNuclei().stream().mapToDouble(n -> {
			try {

				IProfile angleProfile = n.getProfile(ProfileType.ANGLE, om);
				double diff = angleProfile.absoluteSquareDifference(medianProfile,
						FIXED_PROFILE_LENGTH);
				return Math.sqrt(diff / FIXED_PROFILE_LENGTH); // differences in degrees, rather
																// than square degrees

			} catch (ProfileException | MissingLandmarkException | MissingProfileException e) {
				LOGGER.log(Loggable.STACK, "Error getting nucleus profile", e);
				return Double.NaN;
			}
		}).toArray();
	}

	/**
	 * Get the perimeter normalised veriabililty of a nucleus angle profile compared
	 * to the median profile of the collection
	 * 
	 * @param om the tag to use as index 0
	 * @param c  the cell to test
	 * @return the variabililty score of the nucleus
	 * @throws Exception
	 */
	@Override
	public double getNormalisedDifferenceToMedian(@NonNull OrientationMark om, Taggable t) {

		try {
			IProfile medianProfile = profileCollection
					.getProfile(ProfileType.ANGLE, om, Stats.MEDIAN)
					.interpolate(FIXED_PROFILE_LENGTH);

			IProfile angleProfile = t.getProfile(ProfileType.ANGLE, om);

			double diff = angleProfile.absoluteSquareDifference(medianProfile,
					FIXED_PROFILE_LENGTH);
			return Math.sqrt(diff / FIXED_PROFILE_LENGTH); // use the differences in degrees divided
															// by the pixel
															// interplation length
		} catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
			LOGGER.log(Loggable.STACK, "Error getting median profile for collection", e);
			return 0;
		}
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
	public Nucleus getNucleusMostSimilarToMedian(OrientationMark pointType)
			throws ProfileException, MissingLandmarkException, MissingProfileException {

		List<Nucleus> list = this.getNuclei();

		// No need to check profiles if there is only one nucleus
		if (this.size() == 1)
			return list.stream().findFirst().orElseThrow(ProfileException::new);

		IProfile medianProfile = profileCollection
				.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN)
				.interpolate(FIXED_PROFILE_LENGTH);

		Nucleus result = null;

		double difference = Double.MAX_VALUE;
		for (Nucleus p : list) {
			IProfile profile = p.getProfile(ProfileType.ANGLE, pointType);
			double nDifference = profile.absoluteSquareDifference(medianProfile,
					FIXED_PROFILE_LENGTH);
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
	public void clear(@NonNull Measurement stat, @NonNull String component) {
		statsCache.clear(stat, component, null);
	}

	@Override
	public void clear(@NonNull Measurement stat, @NonNull String component, @NonNull UUID id) {
		statsCache.clear(stat, component, id);
	}

	@Override
	public void clear(MeasurementScale scale) {
		statsCache.clear(scale);
	}

	@Override
	public synchronized double getMedian(@NonNull Measurement stat, String component,
			MeasurementScale scale) {
		return getMedianStatistic(stat, component, scale, null);
	}

	@Override
	public synchronized double getMedian(@NonNull Measurement stat, String component,
			MeasurementScale scale, UUID id) {
		return getMedianStatistic(stat, component, scale, id);
	}

	@Override
	public synchronized double[] getRawValues(@NonNull Measurement stat, String component,
			MeasurementScale scale) {

		return getRawValues(stat, component, scale, null);

	}

	@Override
	public synchronized double[] getRawValues(@NonNull Measurement stat, String component,
			MeasurementScale scale,
			UUID id) {

		switch (component) {
		case CellularComponent.WHOLE_CELL:
			return getCellStatistics(stat, scale);
		case CellularComponent.NUCLEUS:
			return getNuclearStatistics(stat, scale);
		case CellularComponent.NUCLEAR_BORDER_SEGMENT:
			return getSegmentStatistics(stat, scale, id);
		default: {
			LOGGER.warning(() -> "No component of type " + component + " can be handled");
			return new double[0];
		}
		}
	}

	private synchronized double getMedianStatistic(@NonNull Measurement stat, String component,
			MeasurementScale scale,
			UUID id) {

		if (!statsCache.has(stat, component, scale, id)) {
			double[] values = getRawValues(stat, component, scale, id);
			statsCache.set(stat, component, scale, id, values);

		}

		return statsCache.getMedian(stat, component, scale, id);

	}

	@Override
	public synchronized double getMin(@NonNull Measurement stat, String component,
			MeasurementScale scale) {
		return getMinStatistic(stat, component, scale, null);
	}

	@Override
	public synchronized double getMin(@NonNull Measurement stat, String component,
			MeasurementScale scale, UUID id) {
		return getMinStatistic(stat, component, scale, id);
	}

	private synchronized double getMinStatistic(@NonNull Measurement stat, String component,
			MeasurementScale scale,
			UUID id) {

		if (!statsCache.has(stat, component, scale, id)) {
			double[] values = getRawValues(stat, component, scale, id);
			statsCache.set(stat, component, scale, id, values);

		}

		return statsCache.getMin(stat, component, scale, id);
	}

	@Override
	public synchronized double getMax(@NonNull Measurement stat, String component,
			MeasurementScale scale) {
		return getMaxStatistic(stat, component, scale, null);
	}

	@Override
	public synchronized double getMax(@NonNull Measurement stat, String component,
			MeasurementScale scale, UUID id) {
		return getMaxStatistic(stat, component, scale, id);
	}

	private synchronized double getMaxStatistic(@NonNull Measurement stat, String component,
			MeasurementScale scale,
			UUID id) {

		if (!statsCache.has(stat, component, scale, id)) {
			double[] values = getRawValues(stat, component, scale, id);
			statsCache.set(stat, component, scale, id, values);
		}

		return statsCache.getMax(stat, component, scale, id);
	}

	/**
	 * Get a sorted list of the given statistic values for each nucleus in the
	 * collection, and add summary to the cache
	 * 
	 * @param stat  the measurement to use
	 * @param scale the measurement scale
	 * @return a list of values
	 * @throws Exception
	 */
	private synchronized double[] getCellStatistics(@NonNull Measurement stat,
			@NonNull MeasurementScale scale) {

		return cells.parallelStream()
				.mapToDouble(c -> c.getMeasurement(stat, scale))
				.sorted()
				.toArray();
	}

	/**
	 * Get a list of the given statistic values for each nucleus in the collection
	 * 
	 * @param stat  the measurement to use
	 * @param scale the measurement scale
	 * @return a list of values
	 * @throws Exception
	 */
	private synchronized double[] getNuclearStatistics(@NonNull Measurement stat,
			@NonNull MeasurementScale scale) {

		double[] result = null;

		if (Measurement.VARIABILITY.equals(stat)) {
			result = this.getNormalisedDifferencesToMedianFromPoint(OrientationMark.REFERENCE);
		} else {
			result = this.getNuclei().parallelStream()
					.mapToDouble(n -> n.getMeasurement(stat, scale))
					.sorted()
					.toArray();
		}
		return result;
	}

	/**
	 * Calculate the length of the segment with the given name in each nucleus of
	 * the collection
	 * 
	 * @param segName the segment name
	 * @param scale   the scale to use
	 * @return a list of segment lengths
	 * @throws Exception
	 */
	private synchronized double[] getSegmentStatistics(@NonNull Measurement stat,
			@NonNull MeasurementScale scale,
			@NonNull UUID id) {

		double[] result = null;

		AtomicInteger errorCount = new AtomicInteger(0);
		result = getNuclei().parallelStream().mapToDouble(n -> {
			IProfileSegment segment;
			try {
				segment = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE).getSegment(id);
			} catch (ProfileException | MissingComponentException e) {
				LOGGER.log(Loggable.STACK, String.format(
						"Error getting segment %s from nucleus %s in DefaultCellCollection::getSegmentStatistics",
						id,
						n.getNameAndNumber()), e);
				errorCount.incrementAndGet();
				return 0;
			}
			double perimeterLength = 0;
			if (segment != null) {
				int indexLength = segment.length();
				double fractionOfPerimeter = (double) indexLength
						/ (double) segment.getProfileLength();
				perimeterLength = fractionOfPerimeter
						* n.getMeasurement(Measurement.PERIMETER, scale);
			}
			return perimeterLength;

		}).sorted().toArray();

		if (errorCount.get() > 0)
			LOGGER.warning(String.format(
					"Problem calculating segment stats for segment %s: %d nuclei had errors getting this segment",
					id,
					errorCount.get()));
		return result;
	}

	@Override
	public void setSourceFolder(@NonNull File newFolder) {
		if (!newFolder.exists())
			return;

		cells.stream().flatMap(c -> c.getNuclei().stream()).forEach(n -> {
			File oldFolder = n.getSourceFolder();
			n.setSourceFolder(newFolder);
			// Update signals in the same file
			n.getSignalCollection().getAllSignals().stream().forEach(s -> {
				if (s.getSourceFolder().equals(oldFolder))
					s.setSourceFolder(newFolder);
			});

		});

	}

	@Override
	public boolean contains(Nucleus nucleus) {
		if (nucleus == null)
			return false;

		for (Nucleus n : getNuclei()) {
			if (n.getID().equals(nucleus.getID()))
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

		Set<UUID> ids = signalGroups.stream().map(s -> s.getId()).collect(Collectors.toSet());
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
		return signalGroups.stream().filter(s -> s.getId().equals(id)).findFirst();
	}

	@Override
	public void addSignalGroup(@NonNull ISignalGroup group) {
		signalGroups.add(group);
	}

	@Override
	public boolean hasSignalGroup(@NonNull UUID id) {
		return signalGroups.stream().anyMatch(s -> s.getId().equals(id));
	}

	/**
	 * Remove the given group
	 * 
	 * @param id
	 */
	@Override
	public void removeSignalGroup(@NonNull UUID id) {
		signalGroups.removeIf(s -> s.getId().equals(id));
		cells.stream().flatMap(c -> c.getNuclei().stream())
				.forEach(n -> n.getSignalCollection().removeSignals(id));
	}

	/**
	 * Get the RuleSetCollection with the index finding rules for this nucleus type
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
				.append("Class: " + this.getClass().getSimpleName() + newLine)
				.append("Nuclei: " + this.getNucleusCount() + newLine)
				.append("Profile collections:" + newLine)
				.append(profileCollection.toString() + newLine);

		b.append(this.ruleSets.toString() + newLine);

		b.append("Signal groups:" + newLine);
		for (ISignalGroup entry : signalGroups) {
			UUID signalGroupID = entry.getId();
			int count = this.getSignalManager().getSignalCount(signalGroupID);
			b.append(entry.toString() + " | " + count + " signals across all cells" + newLine);
		}

		if (this.hasConsensus()) {
			b.append("Consensus:" + newLine);

			try {
				b.append(getConsensus().toString() + newLine);
			} catch (MissingLandmarkException | ComponentCreationException e) {
				b.append("Cannot orient consensus; " + e.getMessage() + newLine);
			}
		}

		return b.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((consensusNucleus == null) ? 0 : consensusNucleus.hashCode());
		result = prime * result + cells.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((profileCollection == null) ? 0 : profileCollection.hashCode());
		result = prime * result + ((ruleSets == null) ? 0 : ruleSets.hashCode());
		result = prime * result + signalGroups.hashCode();
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
		if (!uuid.equals(other.uuid))
			return false;

		if (!name.equals(other.name))
			return false;

		if (consensusNucleus == null) {
			if (other.consensusNucleus != null)
				return false;
		} else if (!consensusNucleus.equals(other.consensusNucleus))
			return false;
		if (!cells.equals(other.cells))
			return false;

		if (!profileCollection.equals(other.profileCollection))
			return false;
		if (ruleSets == null) {
			if (other.ruleSets != null)
				return false;
		} else if (!ruleSets.equals(other.ruleSets))
			return false;

		if (signalGroups.size() != other.signalGroups.size())
			return false;
		for (ISignalGroup s : signalGroups) {
			if (!other.signalGroups.contains(s))
				return false;
		}

		return true;
	}

	/**
	 * Store the median profiles
	 * 
	 * @author ben
	 * @since 2.0.0
	 *
	 */
	public class DefaultProfileCollection implements IProfileCollection {

//		private static final String XML_VALUE_ATTRIBUTE = "value";
//
//		private static final String XML_INDEX_ATTRIBUTE = "index";
//
//		private static final String XML_NAME_ATTRIBUTE = "name";
//
//		private static final String XML_LANDMARK = "Landmark";
//
//		private static final String XML_ORIENT = "Orient";

		/** The indexes of landmarks in the profiles and border list */
		private Map<Landmark, Integer> landmarks = new HashMap<>();

		/** segments in the median profile with RP at zero */
		private List<IProfileSegment> segments = new ArrayList<>();

		/** cached median profiles for quicker access */
		private ProfileCache cache = new ProfileCache();

		/**
		 * Create an empty profile collection. The RP is set to the zero index by
		 * default.
		 */
		public DefaultProfileCollection() {
			Landmark lm = ruleSets.getLandmark(OrientationMark.REFERENCE).get();
			landmarks.put(lm, ZERO_INDEX);
		}

		/**
		 * Construct from an XML element. Use for unmarshalling. The element should
		 * conform to the specification in {@link XmlSerializable}.
		 * 
		 * @param e the XML element containing the data.
		 */
		public DefaultProfileCollection(Element e) {

			for (Element el : e.getChildren(XMLNames.XML_LANDMARK)) {
				landmarks.put(new DefaultLandmark(el.getAttributeValue(XMLNames.XML_NAME)),
						Integer.parseInt(el.getAttributeValue(XMLNames.XML_INDEX)));
			}

			for (Element el : e.getChildren(XMLNames.XML_ORIENT)) {
				OrientationMark name = OrientationMark
						.valueOf(el.getAttributeValue(XMLNames.XML_NAME));
				Landmark l = landmarks.keySet().stream()
						.filter(lm -> lm.getName()
								.equals(el.getAttributeValue(XMLNames.XML_VALUE)))
						.findFirst()
						.get();
			}

			for (Element el : e.getChildren(XMLNames.XML_SEGMENT)) {
				segments.add(new DefaultProfileSegment(el));
			}

		}

		/**
		 * Used for duplicating
		 * 
		 * @param p
		 */
		private DefaultProfileCollection(DefaultProfileCollection p) {
			for (Landmark l : p.landmarks.keySet())
				landmarks.put(l, p.landmarks.get(l));

			for (IProfileSegment s : p.segments)
				segments.add(s.duplicate());

			cache = p.cache.duplicate();
		}

		@Override
		public IProfileCollection duplicate() {
			return new DefaultProfileCollection(this);
		}

		@Override
		public int segmentCount() {
			if (segments == null)
				return 0;
			return segments.size();
		}

		@Override
		public boolean hasSegments() {
			return segmentCount() > 0;
		}

		@Override
		public Landmark getLandmark(@NonNull OrientationMark om) {
			return ruleSets.getLandmark(om).get();
		}

		@Override
		public int getLandmarkIndex(@NonNull OrientationMark om) throws MissingLandmarkException {
			Landmark lm = getLandmark(om);
			return getLandmarkIndex(lm);
		}

		@Override
		public int getLandmarkIndex(@NonNull Landmark lm) throws MissingLandmarkException {
			if (landmarks.containsKey(lm))
				return landmarks.get(lm);
			throw new MissingLandmarkException(lm + " is not present in this profile collection");
		}

		@Override
		public List<Landmark> getLandmarks() {
			List<Landmark> result = new ArrayList<>();
			for (Landmark s : landmarks.keySet()) {
				result.add(s);
			}
			return result;
		}

		@Override
		public List<OrientationMark> getOrientationMarks() {
			List<OrientationMark> result = new ArrayList<>();
			for (OrientationMark s : ruleSets.getOrientionMarks()) {
				result.add(s);
			}
			return result;
		}

		@Override
		public boolean hasLandmark(@NonNull OrientationMark om) {
			return ruleSets.getLandmark(om).isPresent();
		}

		@Override
		public synchronized IProfile getProfile(@NonNull ProfileType type,
				@NonNull OrientationMark om, int quartile)
				throws MissingLandmarkException, ProfileException, MissingProfileException {
			if (!this.hasLandmark(om))
				throw new MissingLandmarkException(
						"Orientation point is not present: " + om.toString());

			Landmark lm = getLandmark(om);

			return getProfile(type, lm, quartile);
		}

		@Override
		public synchronized IProfile getProfile(@NonNull ProfileType type,
				@NonNull Landmark lm, int quartile)
				throws MissingLandmarkException, ProfileException, MissingProfileException {

			if (!cache.hasProfile(type, quartile, lm)) {
				IProfileAggregate agg = createProfileAggregate(type,
						DefaultCellCollection.this.getMedianArrayLength());

				IProfile p = agg.getQuartile(quartile);
				int offset = landmarks.get(lm);
				p = p.startFrom(offset);
				cache.addProfile(type, quartile, lm, p);
			}

			return cache.getProfile(type, quartile, lm);
		}

		@Override
		public ISegmentedProfile getSegmentedProfile(@NonNull ProfileType type,
				@NonNull OrientationMark tag,
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
		public void calculateProfiles()
				throws MissingLandmarkException, MissingProfileException, ProfileException {
			cache.clear();
			for (ProfileType t : ProfileType.values()) {
				for (Landmark lm : landmarks.keySet()) {
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
		public synchronized IProfileSegment getSegmentAt(@NonNull OrientationMark tag, int position)
				throws MissingLandmarkException {
			return this.getSegments(tag).get(position);
		}

		@Override
		public synchronized List<IProfileSegment> getSegments(@NonNull OrientationMark tag)
				throws MissingLandmarkException {

			// this must be negative offset for segments
			// since we are moving the pointIndex back to the beginning
			// of the array
			int offset = -getLandmarkIndex(tag);

			List<IProfileSegment> result = new ArrayList<>();

			for (IProfileSegment s : segments) {
				result.add(s.duplicate().offset(offset));
			}

			try {
				IProfileSegment.linkSegments(result);
				return result;
			} catch (ProfileException e) {
				LOGGER.log(Loggable.STACK, "Could not get segments from " + tag, e);
				e.printStackTrace();
				return new ArrayList<>();
			}
		}

		@Override
		public IProfileSegment getSegmentContaining(@NonNull OrientationMark tag)
				throws ProfileException, MissingLandmarkException {
			List<IProfileSegment> segs = this.getSegments(tag);

			IProfileSegment result = null;
			for (IProfileSegment seg : segs) {
				if (seg.contains(ZERO_INDEX))
					return seg;
			}

			return result;
		}

		@Override
		public void setLandmark(@NonNull Landmark lm, int newIndex) {
			// Cannot move the RP from zero
			Landmark rp = getLandmark(OrientationMark.REFERENCE);

			if (rp.equals(lm))
				return;

			cache.remove(lm);
			landmarks.put(lm, newIndex);

		}

		@Override
		public void setSegments(@NonNull List<IProfileSegment> n) throws MissingLandmarkException {
			if (n.isEmpty())
				throw new IllegalArgumentException("String or segment list is empty");

			int length = DefaultCellCollection.this.getMedianArrayLength();
			if (length != n.get(0).getProfileLength())
				throw new IllegalArgumentException(
						String.format(
								"Segment profile length (%d) does not fit aggregate length (%d)",
								n.get(0).getProfileLength(), length));

			/*
			 * The segments coming in are zeroed to the given pointType pointIndex This
			 * means the indexes must be moved forwards appropriately. Hence, add a positive
			 * offset.
			 */
			int offset = getLandmarkIndex(OrientationMark.REFERENCE);

			for (IProfileSegment s : n) {
				s.offset(offset);
			}

			segments = new ArrayList<>();
			for (IProfileSegment s : n) {
				segments.add(s.duplicate());
			}
		}

		private IProfileAggregate createProfileAggregate(@NonNull ProfileType type, int length)
				throws ProfileException, MissingLandmarkException, MissingProfileException {
			if (length <= 0)
				throw new IllegalArgumentException(
						"Requested profile aggregate length is zero or negative");
			cache.clear();
			Landmark lm = getLandmark(OrientationMark.REFERENCE);
			landmarks.put(lm, ZERO_INDEX);

			// If there are segments, not just the default segment, and the segment
			// profile length is different to the required length. Interpolation needed.
			if (segments.size() > 1 && length != segments.get(0).getProfileLength()) {
				LOGGER.fine("Segments already exist, interpolating");
				return createProfileAggregateOfDifferentLength(type, length);
			}

			// No current segments are present. Make a default segment spanning the entire
			// profile
			if (segments.isEmpty()) {
				segments.add(new DefaultProfileSegment(0, 0, length,
						IProfileCollection.DEFAULT_SEGMENT_ID));
			}

			IProfileAggregate agg = new DefaultProfileAggregate(length,
					DefaultCellCollection.this.size());

			for (Nucleus n : DefaultCellCollection.this.getNuclei())
				agg.addValues(n.getProfile(type, OrientationMark.REFERENCE));
			return agg;

		}

		/**
		 * Allow a profile aggregate to be created and segments copied when median
		 * profile lengths have changed.
		 * 
		 * @param collection
		 * @param length
		 * @throws MissingProfileException
		 * @throws MissingLandmarkException
		 */
		private IProfileAggregate createProfileAggregateOfDifferentLength(@NonNull ProfileType type,
				int length)
				throws ProfileException, MissingLandmarkException, MissingProfileException {
			cache.clear();
			Landmark lm = getLandmark(OrientationMark.REFERENCE);
			landmarks.put(lm, ZERO_INDEX);

			// We have no profile to use to interpolate segments.
			// Create an arbitrary profile with the original length.

			List<IProfileSegment> originalSegList = new ArrayList<>();
			for (IProfileSegment s : segments)
				originalSegList.add(s);
			IProfile template = new DefaultProfile(0, segments.get(0).getProfileLength());
			ISegmentedProfile segTemplate = new DefaultSegmentedProfile(template, originalSegList);

			// Now use the interpolation method to adjust the segment lengths
			List<IProfileSegment> interpolatedSegments = segTemplate.interpolate(length)
					.getSegments();

			IProfileAggregate agg = new DefaultProfileAggregate(length,
					DefaultCellCollection.this.size());

			for (Nucleus n : DefaultCellCollection.this.getNuclei())
				agg.addValues(n.getProfile(type, OrientationMark.REFERENCE));

			setSegments(interpolatedSegments);

			return agg;
		}

		@Override
		public String toString() {
			String newLine = System.getProperty("line.separator");
			StringBuilder builder = new StringBuilder();
			builder.append("Point types:" + newLine);
			for (Entry<Landmark, Integer> e : landmarks.entrySet()) {
				builder.append(e.getKey() + ": " + e.getValue() + newLine);
			}

			// Show segments from RP
			try {
				builder.append("Segments from RP:" + newLine);
				for (IProfileSegment s : this.getSegments(OrientationMark.REFERENCE)) {
					builder.append(s.toString() + newLine);
				}

			} catch (Exception e) {
				builder.append("Exception fetching segments: " + e.getMessage());
			}

			return builder.toString();

		}

		@Override
		public IProfile getIQRProfile(@NonNull ProfileType type, @NonNull OrientationMark om)
				throws MissingLandmarkException, ProfileException, MissingProfileException {

			IProfile q25 = getProfile(type, om, Stats.LOWER_QUARTILE);
			IProfile q75 = getProfile(type, om, Stats.UPPER_QUARTILE);

			if (q25 == null)
				throw new ProfileException("Could not create q25 profile; was null");

			if (q75 == null)
				throw new ProfileException("Could not create q75 profile; was null");

			return q75.subtract(q25);
		}

		@Override
		public int hashCode() {
			return Objects.hash(landmarks, segments);
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
			return Objects.equals(landmarks, other.landmarks)
					&& Objects.equals(segments, other.segments);
		}

		@Override
		public Element toXmlElement() {
			Element e = new Element("ProfileCollection");

			for (IProfileSegment s : segments) {
				e.addContent(s.toXmlElement());
			}

			for (Entry<Landmark, Integer> entry : landmarks.entrySet()) {
				e.addContent(new Element(XMLNames.XML_LANDMARK)
						.setAttribute(XMLNames.XML_NAME, entry.getKey().toString())
						.setAttribute(XMLNames.XML_INDEX, String.valueOf(entry.getValue())));
			}
			return e;
		}

		@Override
		public double getProportionOfIndex(int index) {
			int length = DefaultCellCollection.this.getMedianArrayLength();
			if (index < 0 || index >= length)
				throw new IllegalArgumentException("Index out of bounds: " + index);
			if (index == 0)
				return 0;
			if (index == length - 1)
				return 1;
			return (double) index / (double) (length - 1);
		}

		@Override
		public double getProportionOfIndex(@NonNull OrientationMark tag)
				throws MissingLandmarkException {
			return getProportionOfIndex(getLandmarkIndex(tag));
		}

		@Override
		public double getProportionOfIndex(@NonNull Landmark tag)
				throws MissingLandmarkException {
			return getProportionOfIndex(getLandmarkIndex(tag));
		}

		@Override
		public int getIndexOfProportion(double proportion) {
			if (proportion < 0 || proportion > 1)
				throw new IllegalArgumentException("Proportion must be between 0-1: " + proportion);
			if (proportion == 0)
				return 0;

			int length = DefaultCellCollection.this.getMedianArrayLength();
			if (proportion == 1)
				return length - 1;

			double desiredDistanceFromStart = length * proportion;
			return (int) desiredDistanceFromStart;
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
				private final double quartile;
				private final Landmark tag;

				public ProfileKey(final ProfileType type, final double quartile,
						final Landmark tag) {

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
					if (Double.doubleToLongBits(quartile) != Double
							.doubleToLongBits(other.quartile))
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
					for (ProfileKey k : map.keySet()) {
						IProfile p = map.get(k);
						if (p != null)
							result.map.put(k, p.duplicate());
					}
				} catch (ProfileException e) {

				}
				return result;
			}

			/**
			 * Add a profile with the given keys
			 * 
			 * @param type     the profile type
			 * @param quartile the quartile of the dataset
			 * @param tag      the tag
			 * @param profile  the profile to save
			 */
			public void addProfile(final ProfileType type, final double quartile,
					final Landmark tag,
					IProfile profile) {
				ProfileKey key = new ProfileKey(type, quartile, tag);
				map.put(key, profile);
			}

			public boolean hasProfile(final ProfileType type, final double quartile,
					final Landmark tag) {
				ProfileKey key = new ProfileKey(type, quartile, tag);
				return map.containsKey(key);
			}

			public IProfile getProfile(final ProfileType type, final double quartile,
					final Landmark tag) {
				ProfileKey key = new ProfileKey(type, quartile, tag);
				return map.get(key);
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

	}
}
