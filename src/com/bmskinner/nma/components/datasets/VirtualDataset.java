package com.bmskinner.nma.components.datasets;

import java.awt.Color;
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

import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.Statistical;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.Version.UnsupportedVersionException;
import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Consensus;
import com.bmskinner.nma.components.cells.DefaultConsensusNucleus;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.measure.StatsCache;
import com.bmskinner.nma.components.options.DefaultAnalysisOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
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
import com.bmskinner.nma.components.signals.DefaultShellResult;
import com.bmskinner.nma.components.signals.DefaultSignalGroup;
import com.bmskinner.nma.components.signals.DefaultWarpedSignal;
import com.bmskinner.nma.components.signals.IShellResult;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.components.signals.IWarpedSignal;
import com.bmskinner.nma.components.signals.SignalManager;
import com.bmskinner.nma.io.XmlSerializable;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

/**
 * A combination cell collection and dataset used for storing child datasets and
 * other sub-datasets
 * 
 * @author ben
 * @since 2.0.0
 *
 */
public class VirtualDataset extends AbstractAnalysisDataset
		implements IAnalysisDataset, ICellCollection {

	private static final Logger LOGGER = Logger.getLogger(VirtualDataset.class.getName());

	/** the collection id */
	private final UUID uuid;

	/** the name of the collection */
	private String name;

	/** the cells that belong to this collection */
	private final Set<UUID> cellIDs = new HashSet<>(0);

	/** this holds the mapping of tail indexes etc in the median profile arrays */
	private IProfileCollection profileCollection;

	/** the refolded consensus nucleus */
	private Consensus consensusNucleus;

	/**
	 * Store shell results and warped signals separately to allow separate analysis
	 * between parentDataset.getCollection() and child datasets
	 */
	private Map<UUID, IShellResult> shellResults = new HashMap<>(0);

	private Map<UUID, List<IWarpedSignal>> warpedSignals = new HashMap<>(0);

	/*
	 * TRANSIENT FIELDS
	 */

	protected Map<UUID, Integer> vennCache = new HashMap<>();

	private ProfileManager profileManager = new ProfileManager(this);
	private SignalManager signalManager = new SignalManager(this);
	private StatsCache statsCache = new StatsCache();

	/**
	 * Construct from a parent dataset (of which this will be a child). The new
	 * dataset will have a random UUID
	 * 
	 * @param parent the parent dataset to this
	 * @param name   the name for this new dataset
	 */
	public VirtualDataset(@NonNull IAnalysisDataset parent, String name) {
		this(parent, name, null);
	}

	/**
	 * Construct from a parentDataset.getCollection() dataset (of which this will be
	 * a child) and a cell collection
	 * 
	 * @param parent the parent dataset to this
	 * @param name   the name for this new dataset
	 * @param id     the id for the this dataset. Random if null
	 */
	public VirtualDataset(@NonNull IAnalysisDataset parent, String name, @Nullable UUID id) {
		super();
		this.uuid = id == null ? UUID.randomUUID() : id;
		this.name = name;
		this.parentDataset = parent;
		profileCollection = new DefaultProfileCollection();
	}

	/**
	 * Construct from a parentDataset.getCollection() dataset (of which this will be
	 * a child) and a cell collection
	 * 
	 * @param parent the parent dataset to this
	 * @param name   the name for this new dataset
	 * @param id     the id for the this dataset. Random if null
	 * @param cells  the collection from which to copy cells
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 */
	public VirtualDataset(@NonNull IAnalysisDataset parent, String name, @Nullable UUID id,
			Collection<ICell> cells)
			throws ProfileException, MissingProfileException, MissingLandmarkException {
		this(parent, name, id);
		addAll(cells);
		profileCollection.calculateProfiles();
		parent.getCollection().getProfileManager().copySegmentsAndLandmarksTo(this);
	}

	public VirtualDataset(@NonNull Element e)
			throws ComponentCreationException, UnsupportedVersionException {
		super(e);
		uuid = UUID.fromString(e.getAttributeValue(XMLNames.XML_ID));
		name = e.getAttributeValue(XMLNames.XML_NAME);

		profileCollection = new DefaultProfileCollection(
				e.getChild(XMLNames.XML_PROFILE_COLLECTION));

		if (e.getChild(XMLNames.XML_CONSENSUS_NUCLEUS) != null)
			consensusNucleus = new DefaultConsensusNucleus(
					e.getChild(XMLNames.XML_CONSENSUS_NUCLEUS));

		for (Element el : e.getChildren(XMLNames.XML_CELL))
			cellIDs.add(UUID.fromString(el.getAttributeValue(XMLNames.XML_ID)));

		// Shells are not stored in signal groups for child datasets
		// becuase signal groups can only be added to root datasets
		for (Element el : e.getChildren(XMLNames.XML_SIGNAL_SHELL_RESULT)) {
			UUID id = UUID.fromString(el.getAttributeValue(XMLNames.XML_ID));
			IShellResult s = new DefaultShellResult(el);
			shellResults.put(id, s);
		}
		// Warped signals are not stored in signal groups for child datasets
		// becuase signal groups can only be added to root datasets
		for (Element el : e.getChildren(XMLNames.XML_WARPED_SIGNAL)) {
			UUID id = UUID.fromString(el.getAttributeValue(XMLNames.XML_ID));
			IWarpedSignal s = new DefaultWarpedSignal(el);
			warpedSignals.computeIfAbsent(id, k -> new ArrayList<>());
			warpedSignals.get(id).add(s);
		}

		// Note we cannot calculate profiles at this stage because the parent objects
		// are not fully constructed yet
	}

	/**
	 * Construct from an existing dataset. Used internally for duplication.
	 * 
	 * @param v
	 * @throws ComponentCreationException
	 */
	private VirtualDataset(VirtualDataset v) throws ComponentCreationException {
		super(v);
		uuid = v.uuid;
		name = v.name;

		cellIDs.addAll(v.cellIDs);
		profileCollection = v.profileCollection.duplicate();
		parentDataset = v.parentDataset;
		if (v.consensusNucleus != null)
			consensusNucleus = v.consensusNucleus.duplicate();
		for (Entry<UUID, IShellResult> e : v.shellResults.entrySet()) {
			shellResults.put(e.getKey(), e.getValue().duplicate());
		}
		for (Entry<UUID, List<IWarpedSignal>> c : v.warpedSignals.entrySet()) {
			warpedSignals.computeIfAbsent(c.getKey(), k -> new ArrayList<>());
			for (IWarpedSignal s : c.getValue())
				warpedSignals.get(c.getKey()).add(s.duplicate());
		}

	}

	@Override
	public Element toXmlElement() {
		Element e = super.toXmlElement().setName(XMLNames.XML_VIRTUAL_DATASET)
				.setAttribute(XMLNames.XML_ID, uuid.toString())
				.setAttribute(XMLNames.XML_NAME, name);

		// Collection content
		e.addContent(profileCollection.toXmlElement());

		if (consensusNucleus != null)
			e.addContent(consensusNucleus.toXmlElement());

		for (UUID c : cellIDs)
			e.addContent(
					new Element(XMLNames.XML_CELL).setAttribute(XMLNames.XML_ID, c.toString()));

		for (Entry<UUID, IShellResult> c : shellResults.entrySet()) {
			e.addContent(c.getValue().toXmlElement().setAttribute(XMLNames.XML_ID,
					c.getKey().toString()));
		}

		for (Entry<UUID, List<IWarpedSignal>> c : warpedSignals.entrySet()) {
			for (IWarpedSignal s : c.getValue())
				e.addContent(s.toXmlElement().setAttribute(XMLNames.XML_ID, c.getKey().toString()));
		}

		return e;
	}

	@Override
	public ICellCollection getCollection() {
		return this;
	}

	@Override
	public void setName(@NonNull String s) {
		this.name = s;

	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public UUID getId() {
		return uuid;
	}

	@Override
	public boolean isReal() {
		return false;
	}

	@Override
	public boolean isVirtual() {
		return true;
	}

	@Override
	public boolean add(ICell e) {
		boolean b = cellIDs.add(e.getId());
		if (b)
			statsCache.clear();
		return b;
	}

	@Override
	public boolean addAll(Collection<? extends ICell> c) {
		boolean b = cellIDs.addAll(c.stream().map(ICell::getId).collect(Collectors.toSet()));
		if (b)
			statsCache.clear();
		return b;
	}

	@Override
	public void clear() {
		cellIDs.clear();
		statsCache.clear();
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof ICell c) {
			return cellIDs.contains(c.getId());
		}

		if (o instanceof UUID u)
			return cellIDs.contains(u);
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		boolean b = false;
		for (Object o : c)
			b |= contains(o);
		return b;
	}

	@Override
	public boolean isEmpty() {
		return getCells().isEmpty();
	}

	@Override
	public Iterator<ICell> iterator() {
		return getCells().iterator();
	}

	@Override
	public boolean remove(Object o) {
		if (o instanceof ICell c) {
			boolean b = cellIDs.remove(c.getId());
			if (b)
				statsCache.clear();
			return b;
		}
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean b = false;
		for (Object o : c)
			b |= remove(o);
		if (b)
			statsCache.clear();
		return b;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO operate on the real list
		return getCells().retainAll(c);
	}

	@Override
	public Object[] toArray() {
		return getCells().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return getCells().toArray(a);
	}

	@Override
	public List<ICell> getCells() {
		return parentDataset.getCollection().getCells().parallelStream()
				.filter(c -> cellIDs.contains(c.getId()))
				.collect(Collectors.toList());
	}

	@Override
	public Stream<ICell> streamCells() {
		return getCells().stream();
	}

	@Override
	public Set<ICell> getCells(@NonNull File f) {
		return stream().filter(c -> c.getPrimaryNucleus().getSourceFile().equals(f))
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
	public Set<UUID> getCellIDs() {
		return new HashSet<>(cellIDs);
	}

	@Override
	public List<Nucleus> getNuclei() {
		return parentDataset.getCollection().getCells().parallelStream()
				.filter(c -> cellIDs.contains(c.getId()))
				.flatMap(c -> c.getNuclei().stream()).toList();
	}

	@Override
	public int getNucleusCount() {
		return this.getNuclei().size();
	}

	@Override
	public Set<Nucleus> getNuclei(@NonNull File imageFile) {

		return cellIDs.stream().map(id -> parentDataset.getCollection().getCell(id))
				.flatMap(c -> c.getNuclei().stream())
				.filter(n -> n.getSourceFile().equals(imageFile))
				.collect(Collectors.toSet());
	}

	@Override
	public ICell getCell(@NonNull UUID id) {
		if (cellIDs.contains(id))
			return parentDataset.getCollection().getCell(id);
		return null;
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

	@Override
	public int size() {
		return cellIDs.size();
	}

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
			consensusNucleus.offset(xOffset, yOffset);
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
	public boolean hasCells() {
		return !cellIDs.isEmpty();
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
	public boolean contains(UUID cellID) {
		return cellIDs.contains(cellID);
	}

	@Override
	public boolean containsExact(@NonNull ICell cell) {
		return parentDataset.getCollection().containsExact(cell);
	}

	@Override
	public boolean hasLockedCells() {
		return parentDataset.getCollection().hasLockedCells();
	}

	@Override
	public void setCellsLocked(boolean b) {
		for (Nucleus n : this.getNuclei())
			n.setLocked(b);
	}

	@Override
	public IProfileCollection getProfileCollection() {
		return profileCollection;
	}

	@Override
	public Set<File> getImageFiles() {
		return this.stream().map(ICell::getPrimaryNucleus).map(Nucleus::getSourceFile)
				.collect(Collectors.toSet());
	}

	@Override
	public Set<UUID> getSignalGroupIDs() {
		return parentDataset.getCollection().getSignalGroupIDs();
	}

	@Override
	public void removeSignalGroup(@NonNull UUID id) {
		parentDataset.getCollection().removeSignalGroup(id);
	}

	@Override
	public Optional<ISignalGroup> getSignalGroup(@NonNull UUID signalGroup) {

		if (!parentDataset.getCollection().hasSignalGroup(signalGroup))
			return Optional.empty();

		ISignalGroup result = new DefaultSignalGroup(
				parentDataset.getCollection().getSignalGroup(signalGroup).get()) {

			@Override
			public boolean hasWarpedSignals() {
				if (warpedSignals.containsKey(signalGroup))
					return !warpedSignals.get(signalGroup).isEmpty();
				return false;
			}

			@Override
			public List<IWarpedSignal> getWarpedSignals() {
				if (!warpedSignals.containsKey(signalGroup))
					return new ArrayList<>();
				return warpedSignals.get(signalGroup);
			}

			@Override
			public void addWarpedSignal(@NonNull IWarpedSignal result) {
				warpedSignals.computeIfAbsent(signalGroup, s -> new ArrayList<>());
				warpedSignals.get(signalGroup).add(result);
			}

			@Override
			public void clearWarpedSignals() {
				warpedSignals.clear();
			}

			@Override
			public void setShellResult(@NonNull IShellResult result) {
				shellResults.put(signalGroup, result);
			}

			@Override
			public boolean hasShellResult() {
				return shellResults.containsKey(signalGroup);
			}

			@Override
			public Optional<IShellResult> getShellResult() {
				return Optional.ofNullable(shellResults.get(signalGroup));
			}

			@Override
			public void setGroupColour(@NonNull Color newColor) {
				parentDataset.getCollection().getSignalGroup(signalGroup).get()
						.setGroupColour(newColor);
			}

			@Override
			public void setVisible(boolean b) {
				parentDataset.getCollection().getSignalGroup(signalGroup).get().setVisible(b);
			}

		};

		return Optional.of(result);
	}

	@Override
	public boolean hasSignalGroup(@NonNull UUID signalGroup) {
		return parentDataset.getCollection().hasSignalGroup(signalGroup);
	}

	@Override
	public Collection<ISignalGroup> getSignalGroups() {
		List<ISignalGroup> result = new ArrayList<>();

		for (UUID id : this.getSignalGroupIDs())
			if (getSignalGroup(id).isPresent())
				result.add(getSignalGroup(id).get());

		return result;
	}

	@Override
	public void addSignalGroup(@NonNull ISignalGroup newGroup) {
		parentDataset.getCollection().addSignalGroup(newGroup);
	}

	@Override
	public SignalManager getSignalManager() {
		return signalManager;
	}

	@Override
	public RuleSetCollection getRuleSetCollection() {
		return parentDataset.getCollection().getRuleSetCollection();
	}

	@Override
	public void setSourceFolder(@NonNull File expectedImageDirectory) {
		parentDataset.getCollection().setSourceFolder(expectedImageDirectory);
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
		if (list.size() == 1) {
			return list.get(0);
		}

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

	@Override
	public ProfileManager getProfileManager() {
		return profileManager;
	}

	@Override
	public int countShared(@NonNull IAnalysisDataset d2) {
		return countShared(d2.getCollection());

	}

	@Override
	public int countShared(@NonNull ICellCollection d2) {
		if (this.vennCache.containsKey(d2.getId())) {
			return vennCache.get(d2.getId());
		}
		int shared = countSharedNuclei(d2);
		vennCache.put(d2.getId(), shared);
		d2.setSharedCount(this, shared);
		return shared;
	}

	@Override
	public void setSharedCount(@NonNull ICellCollection d2, int i) {
		vennCache.put(d2.getId(), i);
	}

	/**
	 * Count the number of nuclei from this dataset that are present in d2
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private int countSharedNuclei(ICellCollection d2) {

		if (d2 == this)
			return cellIDs.size();

		if (parentDataset.getCollection() == d2)
			return cellIDs.size();

		// Ensure cells use the same rule
		if (!d2.getRuleSetCollection().equals(parentDataset.getCollection().getRuleSetCollection()))
			return 0;

		Set<UUID> toSearch = new HashSet<>(d2.getCellIDs());
		toSearch.retainAll(cellIDs);
		return toSearch.size();
	}

	@Override
	public int getMedianArrayLength() {
		if (size() == 0) {
			return 0;
		}

		int[] p = this.getArrayLengths();
		return Stats.quartile(p, Stats.MEDIAN);
	}

	/**
	 * Get the array lengths of the nuclei in this collection as an array
	 * 
	 * @return
	 */
	private int[] getArrayLengths() {

		int[] result = new int[this.getNuclei().size()];

		int i = 0;

		for (Nucleus n : this.getNuclei()) {
			result[i++] = n.getBorderLength();
		}

		return result;
	}

	@Override
	public int getMaxProfileLength() {
		return Arrays.stream(this.getArrayLengths()).max().orElse(0); // Stats.max(values);
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
	public double getMedian(@NonNull Measurement stat, String component, MeasurementScale scale) {
		if (this.size() == 0) {
			return 0;
		}
		return getMedianStatistic(stat, component, scale, null);
	}

	@Override
	public double[] getRawValues(@NonNull Measurement stat, String component,
			MeasurementScale scale) {
		return getRawValues(stat, component, scale, null);
	}

	@Override
	public double[] getRawValues(@NonNull Measurement stat, String component,
			MeasurementScale scale, UUID id) {

		switch (component) {
		case CellularComponent.WHOLE_CELL:
			return getCellStatistics(stat, scale);
		case CellularComponent.NUCLEUS:
			return getNuclearStatistics(stat, scale);
		case CellularComponent.NUCLEAR_BORDER_SEGMENT:
			return getSegmentStatistics(stat, scale, id);
		default: {
			LOGGER.warning("No component of type " + component + " can be handled");
			return null;
		}
		}
	}

	@Override
	public double getMedian(@NonNull Measurement stat, String component, MeasurementScale scale,
			UUID id) {

		return getMedianStatistic(stat, component, scale, id);
	}

	@Override
	public double getMin(@NonNull Measurement stat, String component, MeasurementScale scale) {
		return getMinStatistic(stat, component, scale, null);
	}

	@Override
	public double getMin(@NonNull Measurement stat, String component, MeasurementScale scale,
			UUID id) {

		// Handle old segment andSignalStatistic enums
		if (CellularComponent.NUCLEAR_SIGNAL.equals(component)) {
			return getMinStatistic(stat, CellularComponent.NUCLEAR_SIGNAL, scale, id);
		}

		if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)) {
			return getMinStatistic(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);
		}
		return getMinStatistic(stat, component, scale, id);
	}

	private synchronized double getMinStatistic(Measurement stat, String component,
			MeasurementScale scale, UUID id) {

		double[] values = getRawValues(stat, component, scale, id);
		return Arrays.stream(values).min().orElse(Statistical.ERROR_CALCULATING_STAT);
	}

	@Override
	public synchronized double getMax(@NonNull Measurement stat, String component,
			MeasurementScale scale) {
		return getMaxStatistic(stat, component, scale, null);
	}

	@Override
	public double getMax(@NonNull Measurement stat, String component, MeasurementScale scale,
			UUID id) {

		// Handle old segment andSignalStatistic enums
		if (CellularComponent.NUCLEAR_SIGNAL.equals(component)) {
			return getMaxStatistic(stat, CellularComponent.NUCLEAR_SIGNAL, scale, id);
		}

		if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)) {
			return getMaxStatistic(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);
		}
		return getMaxStatistic(stat, component, scale, id);
	}

	private double getMaxStatistic(Measurement stat, String component, MeasurementScale scale,
			UUID id) {

		double[] values = getRawValues(stat, component, scale, id);
		return Arrays.stream(values).max().orElse(Statistical.ERROR_CALCULATING_STAT);
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
	private double[] getSegmentStatistics(Measurement stat, MeasurementScale scale, UUID id) {

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

	/**
	 * Get a list of the given statistic values for each nucleus in the collection
	 * 
	 * @param stat  the statistic to use
	 * @param scale the measurement scale
	 * @return a list of values
	 * @throws Exception
	 */
	private double[] getNuclearStatistics(Measurement stat, MeasurementScale scale) {

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
	 * Get the given statistic values for each cell in the collection
	 * 
	 * @param stat  the statistic to fetch
	 * @param scale the measurement scale
	 * @return a list of values
	 */
	private double[] getCellStatistics(Measurement stat, MeasurementScale scale) {
		return this.getCells().parallelStream()
				.mapToDouble(c -> c.getMeasurement(stat, scale))
				.sorted()
				.toArray();
	}

	/**
	 * Get the differences to the median profile for each nucleus, normalised to the
	 * perimeter of the nucleus. This is the sum-of-squares difference, rooted and
	 * divided by the nuclear perimeter
	 * 
	 * @param pointType the point to fetch profiles from
	 * @return an array of normalised differences
	 */
	private synchronized double[] getNormalisedDifferencesToMedianFromPoint(
			OrientationMark pointType) {

		IProfile medianProfile;
		try {
			medianProfile = this.getProfileCollection()
					.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN)
					.interpolate(FIXED_PROFILE_LENGTH);
		} catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
			LOGGER.warning("Cannot get median profile for collection");
			LOGGER.log(Loggable.STACK, "Error getting median profile", e);
			double[] result = new double[size()];
			Arrays.fill(result, Double.MAX_VALUE);
			return result;
		}

		return getNuclei().stream().mapToDouble(n -> {
			try {

				IProfile angleProfile = n.getProfile(ProfileType.ANGLE, pointType);
				double diff = angleProfile.absoluteSquareDifference(medianProfile,
						FIXED_PROFILE_LENGTH);
				return Math.sqrt(diff / FIXED_PROFILE_LENGTH); // differences in degrees, rather
																// than square degrees

			} catch (ProfileException | MissingLandmarkException | MissingProfileException e) {
				LOGGER.log(Loggable.STACK, "Error getting nucleus profile", e);
				return Double.MAX_VALUE;
			}
		}).toArray();
	}

	private double getMedianStatistic(Measurement stat, String component, MeasurementScale scale,
			UUID id) {

		if (!this.statsCache.has(stat, component, scale, id)) {
			double[] values = getRawValues(stat, component, scale, id);
			statsCache.set(stat, component, scale, id, values);
		}

		return statsCache.getMedian(stat, component, scale, id);

	}

	@Override
	public double getNormalisedDifferenceToMedian(OrientationMark pointType, Taggable t) {
		IProfile medianProfile;
		try {
			medianProfile = profileCollection.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN)
					.interpolate(FIXED_PROFILE_LENGTH);
		} catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
			LOGGER.log(Loggable.STACK, "Error getting median profile for collection", e);
			return 0;
		}

		try {
			IProfile angleProfile = t.getProfile(ProfileType.ANGLE, pointType);

			double diff = angleProfile.absoluteSquareDifference(medianProfile,
					FIXED_PROFILE_LENGTH);
			return Math.sqrt(diff / FIXED_PROFILE_LENGTH);
		} catch (ProfileException | MissingComponentException e) {
			LOGGER.log(Loggable.STACK, "Error getting nucleus profile", e);
			return Double.NaN;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(cellIDs, consensusNucleus, name, profileCollection,
				shellResults, uuid);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		VirtualDataset other = (VirtualDataset) obj;
		return Objects.equals(cellIDs, other.cellIDs)
				&& Objects.equals(consensusNucleus, other.consensusNucleus)
				&& Objects.equals(name, other.name)
				&& Objects.equals(profileCollection, other.profileCollection)
				&& Objects.equals(shellResults, other.shellResults)
				&& Objects.equals(uuid, other.uuid);
	}

	@Override
	public IAnalysisDataset addChildCollection(@NonNull ICellCollection collection)
			throws MissingProfileException, MissingLandmarkException, ProfileException {
		VirtualDataset c = new VirtualDataset(this, collection.getName(), null, collection);
		addChildDataset(c);
		return c;
	}

	@Override
	public IAnalysisDataset addChildDataset(@NonNull IAnalysisDataset dataset) {
		// Ensure no duplicate dataset names
		// If the name is the same as this dataset, or one of the child datasets,
		// apply a suffix
		if (getName().equals(dataset.getName())
				|| childDatasets.stream().map(IAnalysisDataset::getName)
						.anyMatch(s -> s.equals(dataset.getName()))) {
			String newName = chooseSuffix(dataset.getName());
			dataset.setName(newName);
		}
		childDatasets.add(dataset);
		return dataset;

	}

	@Override
	public File getSavePath() {
		return parentDataset.getSavePath();
	}

	@Override
	public void setSavePath(@NonNull File file) {
		// We can't set a path for a child
	}

	@Override
	public void setScale(double scale) {
		if (scale <= 0) // don't allow a scale to cause divide by zero errors
			return;

		Optional<IAnalysisOptions> op = getAnalysisOptions();
		if (op.isPresent()) {
			Set<String> detectionOptions = op.get().getDetectionOptionTypes();
			for (String detectedComponent : detectionOptions) {
				Optional<HashOptions> subOptions = op.get().getDetectionOptions(detectedComponent);
				if (subOptions.isPresent())
					subOptions.get().setDouble(HashOptions.SCALE, scale);
			}
		}

		for (IAnalysisDataset child : getChildDatasets()) {
			child.setScale(scale);
		}

		for (ICell c : getCells())
			c.setScale(scale);

		if (hasConsensus())
			consensusNucleus.setScale(scale);

		clear(MeasurementScale.MICRONS);
	}

	@Override
	public Set<UUID> getChildUUIDs() {
		Set<UUID> result = new HashSet<>(childDatasets.size());
		for (IAnalysisDataset c : childDatasets) {
			result.add(c.getId());
		}

		return result;
	}

	@Override
	public Set<UUID> getAllChildUUIDs() {
		Set<UUID> result = new HashSet<>();

		Set<UUID> idlist = getChildUUIDs();
		result.addAll(idlist);

		for (UUID id : idlist) {
			IAnalysisDataset d = getChildDataset(id);

			result.addAll(d.getAllChildUUIDs());
		}
		return result;
	}

	@Override
	public IAnalysisDataset getChildDataset(@NonNull UUID id) {
		if (this.hasDirectChild(id)) {

			for (IAnalysisDataset c : childDatasets) {
				if (c.getId().equals(id)) {
					return c;
				}
			}

		} else {
			for (IAnalysisDataset child : this.getAllChildDatasets()) {
				if (child.getId().equals(id)) {
					return child;
				}
			}
		}
		return null;
	}

	@Override
	public int getChildCount() {
		return childDatasets.size();
	}

	@Override
	public boolean hasChildren() {
		return !childDatasets.isEmpty();
	}

	@Override
	public Collection<IAnalysisDataset> getChildDatasets() {
		return childDatasets;
	}

	@Override
	public List<IAnalysisDataset> getAllChildDatasets() {
		List<IAnalysisDataset> result = new ArrayList<>();
		if (!childDatasets.isEmpty()) {

			for (IAnalysisDataset d : childDatasets) {
				result.add(d);
				result.addAll(d.getAllChildDatasets());
			}
		}
		return result;
	}

	@Override
	public Optional<IAnalysisOptions> getAnalysisOptions() {
		if (analysisOptions == null)
			return parentDataset.getAnalysisOptions();
		return Optional.ofNullable(analysisOptions);
	}

	@Override
	public boolean hasAnalysisOptions() {
		if (analysisOptions == null)
			return parentDataset.hasAnalysisOptions();
		return true;
	}

	@Override
	public void setAnalysisOptions(@NonNull IAnalysisOptions analysisOptions) {
		this.analysisOptions = new DefaultAnalysisOptions(analysisOptions);
	}

	@Override
	public void refreshClusterGroups() {
		if (this.hasClusters()) {
			// Find the groups that need removing
			List<IClusterGroup> groupsToDelete = new ArrayList<>();
			for (IClusterGroup g : this.getClusterGroups()) {
				boolean clusterRemains = false;

				for (UUID childID : g.getUUIDs()) {
					if (this.hasDirectChild(childID)) {
						clusterRemains = true;
					}
				}
				if (!clusterRemains) {
					groupsToDelete.add(g);
				}
			}

			// Remove the groups
			for (IClusterGroup g : groupsToDelete) {
				this.deleteClusterGroup(g);
			}

		}

	}

	@Override
	public boolean isRoot() {
		return false;
	}

	@Override
	public void deleteChild(@NonNull UUID id) {
		Iterator<IAnalysisDataset> it = childDatasets.iterator();

		while (it.hasNext()) {
			IAnalysisDataset child = it.next();

			if (child.getId().equals(id)) {
				for (IClusterGroup g : clusterGroups) {
					if (g.hasDataset(id)) {
						g.removeDataset(id);
					}
				}
				it.remove();
				break;
			}
		}
	}

	@Override
	public void deleteClusterGroup(@NonNull final IClusterGroup group) {

		if (hasClusterGroup(group)) {
			UUID[] groupIds = group.getUUIDs().toArray(new UUID[0]);

			for (UUID id : groupIds)
				deleteChild(id);

			// Remove saved values associated with the cluster group
			// e.g. tSNE, PCA
			for (Nucleus n : getCollection().getNuclei()) {
				for (Measurement s : n.getMeasurements()) {
					if (s.toString().endsWith(group.getId().toString()))
						n.clearMeasurement(s);
				}
			}
			this.clusterGroups.remove(group);
		}
	}

	@Override
	public void deleteClusterGroups() {
		LOGGER.fine("Deleting all cluster groups in " + getName());
		// Use arrays to avoid concurrent modifications to cluster groups
		Object[] ids = clusterGroups.parallelStream().map(IClusterGroup::getId).toArray();
		for (Object id : ids) {
			Optional<IClusterGroup> optg = clusterGroups.stream()
					.filter(group -> group.getId().equals(id)).findFirst();
			if (optg.isPresent())
				deleteClusterGroup(optg.get());
		}
	}

	@Override
	public void deleteMergeSource(@NonNull UUID id) {
		// No action
	}

	@Override
	public boolean hasDirectChild(@NonNull UUID id) {

		for (IAnalysisDataset child : childDatasets) {
			if (child.getId().equals(id)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void updateSourceImageDirectory(@NonNull File expectedImageDirectory) {
		parentDataset.updateSourceImageDirectory(expectedImageDirectory);

	}

	@Override
	public ICellCollection duplicate() throws ComponentCreationException {
		return new VirtualDataset(this);
	}

	@Override
	public IAnalysisDataset copy() throws ComponentCreationException {
		return new VirtualDataset(this);
	}

	/**
	 * Store the median profiles
	 * 
	 * @author ben
	 * @since 2.0.0
	 *
	 */
	public class DefaultProfileCollection implements IProfileCollection {

		private static final String XML_VALUE_ATTRIBUTE = "value";

		private static final String XML_INDEX_ATTRIBUTE = "index";

		private static final String XML_NAME_ATTRIBUTE = "name";

		private static final String XML_LANDMARK = "Landmark";

		private static final String XML_ORIENT = "Orient";

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
			Landmark lm = parentDataset.getCollection().getRuleSetCollection()
					.getLandmark(OrientationMark.REFERENCE).get();
			landmarks.put(lm, ZERO_INDEX);
		}

		/**
		 * Construct from an XML element. Use for unmarshalling. The element should
		 * conform to the specification in {@link XmlSerializable}.
		 * 
		 * @param e the XML element containing the data.
		 */
		public DefaultProfileCollection(Element e) {

			for (Element el : e.getChildren(XML_LANDMARK)) {
				landmarks.put(new DefaultLandmark(el.getAttributeValue(XML_NAME_ATTRIBUTE)),
						Integer.parseInt(el.getAttributeValue(XML_INDEX_ATTRIBUTE)));
			}

			for (Element el : e.getChildren(XML_ORIENT)) {
				OrientationMark name = OrientationMark
						.valueOf(el.getAttributeValue(XML_NAME_ATTRIBUTE));
				Landmark l = landmarks.keySet().stream()
						.filter(lm -> lm.getName()
								.equals(el.getAttributeValue(XML_VALUE_ATTRIBUTE)))
						.findFirst()
						.get();
			}

			for (Element el : e.getChildren("Segment")) {
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
			return parentDataset.getCollection().getRuleSetCollection().getLandmark(om).get();
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
			for (OrientationMark s : parentDataset.getCollection().getRuleSetCollection()
					.getOrientionMarks()) {
				result.add(s);
			}
			return result;
		}

		@Override
		public boolean hasLandmark(@NonNull OrientationMark om) {
			return parentDataset.getCollection().getRuleSetCollection().getLandmark(om).isPresent();
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
						VirtualDataset.this.getMedianArrayLength());

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

			int length = VirtualDataset.this.getMedianArrayLength();
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
					VirtualDataset.this.size());

			for (Nucleus n : VirtualDataset.this.getNuclei())
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
					VirtualDataset.this.size());

			for (Nucleus n : VirtualDataset.this.getNuclei())
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
				e.addContent(new Element(XML_LANDMARK)
						.setAttribute(XML_NAME_ATTRIBUTE, entry.getKey().toString())
						.setAttribute(XML_INDEX_ATTRIBUTE, String.valueOf(entry.getValue())));
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

		@Override
		public double getProportionOfIndex(int index) {
			int length = VirtualDataset.this.getMedianArrayLength();
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

			int length = VirtualDataset.this.getMedianArrayLength();
			if (proportion == 1)
				return length - 1;

			double desiredDistanceFromStart = length * proportion;
			int target = (int) desiredDistanceFromStart;
			return target;
		}
	}

}
