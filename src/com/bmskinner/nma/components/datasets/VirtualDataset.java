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
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Element;

import com.bmskinner.nma.components.MissingDataException;
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
import com.bmskinner.nma.components.measure.MeasurementCache;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.measure.MissingMeasurementException;
import com.bmskinner.nma.components.measure.VennCache;
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
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileManager;
import com.bmskinner.nma.components.profiles.ProfileType;
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
import com.bmskinner.nma.stats.Stats;
import com.bmskinner.nma.utility.StreamUtils;

/**
 * A combination cell collection and dataset used for storing child datasets and
 * other sub-datasets
 * 
 * @author Ben Skinner
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
	private final Map<UUID, IShellResult> shellResults = new HashMap<>(0);

	private final Map<UUID, List<IWarpedSignal>> warpedSignals = new HashMap<>(0);

	/*
	 * TRANSIENT FIELDS
	 */

	protected VennCache vennCache = new VennCache();

	private final ProfileManager profileManager = new ProfileManager(this);
	private final SignalManager signalManager = new SignalManager(this);
	private final MeasurementCache statsCache = new MeasurementCache();
	
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
		isRecalcHashcode = true;
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
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 */
	public VirtualDataset(@NonNull IAnalysisDataset parent, String name, @Nullable UUID id,
			Collection<ICell> cells)
			throws MissingDataException, SegmentUpdateException {
		this(parent, name, id);
		addAll(cells);
		profileCollection.calculateProfiles();
		parent.getCollection().getProfileManager().copySegmentsAndLandmarksTo(this);
		isRecalcHashcode = true;
	}

	public VirtualDataset(@NonNull Element e)
			throws ComponentCreationException, UnsupportedVersionException {
		super(e);
		uuid = UUID.fromString(e.getAttributeValue(XMLNames.XML_ID));
		name = e.getAttributeValue(XMLNames.XML_NAME);

		profileCollection = new DefaultProfileCollection(
				e.getChild(XMLNames.XML_PROFILE_COLLECTION));

		if (e.getChild(XMLNames.XML_CONSENSUS_NUCLEUS) != null) {
			consensusNucleus = new DefaultConsensusNucleus(
					e.getChild(XMLNames.XML_CONSENSUS_NUCLEUS));
		}

		for (final Element el : e.getChildren(XMLNames.XML_CELL)) {
			cellIDs.add(UUID.fromString(el.getAttributeValue(XMLNames.XML_ID)));
		}

		// Shells are not stored in signal groups for child datasets
		// becuase signal groups can only be added to root datasets
		for (final Element el : e.getChildren(XMLNames.XML_SIGNAL_SHELL_RESULT)) {
			final UUID id = UUID.fromString(el.getAttributeValue(XMLNames.XML_ID));
			final IShellResult s = new DefaultShellResult(el);
			shellResults.put(id, s);
		}
		// Warped signals are not stored in signal groups for child datasets
		// becuase signal groups can only be added to root datasets
		for (final Element el : e.getChildren(XMLNames.XML_WARPED_SIGNAL)) {
			final UUID id = UUID.fromString(el.getAttributeValue(XMLNames.XML_ID));
			final IWarpedSignal s = new DefaultWarpedSignal(el);
			warpedSignals.computeIfAbsent(id, k -> new ArrayList<>());
			warpedSignals.get(id).add(s);
		}
		isRecalcHashcode = true;
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

		try {
			cellIDs.addAll(v.cellIDs);
			profileCollection = v.profileCollection.duplicate();
			parentDataset = v.parentDataset;
			if (v.consensusNucleus != null) {
				consensusNucleus = v.consensusNucleus.duplicate();
			}
			for (final Entry<UUID, IShellResult> e : v.shellResults.entrySet()) {
				shellResults.put(e.getKey(), e.getValue().duplicate());
			}
			for (final Entry<UUID, List<IWarpedSignal>> c : v.warpedSignals.entrySet()) {
				warpedSignals.computeIfAbsent(c.getKey(), k -> new ArrayList<>());
				for (final IWarpedSignal s : c.getValue()) {
					warpedSignals.get(c.getKey()).add(s.duplicate());
				}
			}
			isRecalcHashcode = true;
		} catch (final SegmentUpdateException e) {
			throw new ComponentCreationException("Could not duplicate profile collection", e);
		}
	}

	@Override
	@NonNull public Element toXmlElement() {
		final Element e = super.toXmlElement().setName(XMLNames.XML_VIRTUAL_DATASET)
				.setAttribute(XMLNames.XML_ID, uuid.toString())
				.setAttribute(XMLNames.XML_NAME, name);

		// Collection content
		e.addContent(profileCollection.toXmlElement());

		if (consensusNucleus != null) {
			e.addContent(consensusNucleus.toXmlElement());
		}

		for (final UUID c : cellIDs) {
			e.addContent(
					new Element(XMLNames.XML_CELL).setAttribute(XMLNames.XML_ID, c.toString()));
		}

		for (final Entry<UUID, IShellResult> c : shellResults.entrySet()) {
			e.addContent(c.getValue().toXmlElement().setAttribute(XMLNames.XML_ID,
					c.getKey().toString()));
		}

		for (final Entry<UUID, List<IWarpedSignal>> c : warpedSignals.entrySet()) {
			for (final IWarpedSignal s : c.getValue()) {
				e.addContent(s.toXmlElement().setAttribute(XMLNames.XML_ID, c.getKey().toString()));
			}
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
		isRecalcHashcode = true;
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
		final boolean b = cellIDs.add(e.getId());
		if (b) {
			statsCache.clear();
			isRecalcHashcode = true;
		}
		return b;
	}

	@Override
	public boolean addAll(Collection<? extends ICell> c) {
		final boolean b = cellIDs.addAll(c.stream().map(ICell::getId).collect(Collectors.toSet()));
		if (b) {
			statsCache.clear();
		}
		isRecalcHashcode = true;
		return b;
	}

	@Override
	public void clear() {
		cellIDs.clear();
		statsCache.clear();
		isRecalcHashcode = true;
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof final ICell c)
			return cellIDs.contains(c.getId());

		if (o instanceof final UUID u)
			return cellIDs.contains(u);
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		boolean b = false;
		for (final Object o : c) {
			b |= contains(o);
		}
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
		if (o instanceof final ICell c) {
			final boolean b = cellIDs.remove(c.getId());
			if (b) {
				statsCache.clear();
			}
			isRecalcHashcode = true;
			return b;
		}
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean b = false;
		for (final Object o : c) {
			b |= remove(o);
		}
		if (b) {
			statsCache.clear();
		}
		isRecalcHashcode = true;
		return b;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO operate on the real list
		final boolean b = getCells().retainAll(c);
		isRecalcHashcode = true;
		return b;
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
		for (final ICell c : this) {
			for (final Nucleus n : c.getNuclei()) {
				if (n.getId().equals(id))
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
		isRecalcHashcode = true;
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
		if (consensusNucleus != null) {
			consensusNucleus.offset(xOffset, yOffset);
		}
		isRecalcHashcode = true;
	}

	@Override
	public void rotateConsensus(double degrees) {
		if (consensusNucleus != null) {
			consensusNucleus.addRotation(degrees);
		}
		isRecalcHashcode = true;
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

		for (final Nucleus n : getNuclei()) {
			if (n.getId().equals(nucleus.getId()))
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
		for (final Nucleus n : this.getNuclei()) {
			n.setLocked(b);
		}
		isRecalcHashcode = true;
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
		isRecalcHashcode = true;
	}

	@Override
	public Optional<ISignalGroup> getSignalGroup(@NonNull UUID signalGroup) {

		if (!parentDataset.getCollection().hasSignalGroup(signalGroup))
			return Optional.empty();

		final ISignalGroup result = new DefaultSignalGroup(
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
			public void addWarpedSignal(@NonNull IWarpedSignal result1) {
				warpedSignals.computeIfAbsent(signalGroup, s -> new ArrayList<>());
				warpedSignals.get(signalGroup).add(result1);
			}

			@Override
			public void clearWarpedSignals() {
				warpedSignals.clear();
			}

			@Override
			public void setShellResult(@NonNull IShellResult result1) {
				shellResults.put(signalGroup, result1);
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
		final List<ISignalGroup> result = new ArrayList<>();

		for (final UUID id : this.getSignalGroupIDs())
			if (getSignalGroup(id).isPresent()) {
				result.add(getSignalGroup(id).get());
			}

		return result;
	}

	@Override
	public void addSignalGroup(@NonNull ISignalGroup newGroup) {
		parentDataset.getCollection().addSignalGroup(newGroup);
		isRecalcHashcode = true;
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
		isRecalcHashcode = true;
	}

	/**
	 * Get the nucleus with the lowest difference score to the median profile
	 * 
	 * @param pointType the point to compare profiles from
	 * @return the best nucleus
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	@Override
	public Nucleus getNucleusMostSimilarToMedian(OrientationMark pointType)
			throws SegmentUpdateException, MissingDataException {
		final List<Nucleus> list = this.getNuclei();

		// No need to check profiles if there is only one nucleus
		if (list.size() == 1)
			return list.get(0);

		final IProfile medianProfile = profileCollection
				.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN)
				.interpolate(FIXED_PROFILE_LENGTH);

		Nucleus result = null;

		double difference = Double.MAX_VALUE;
		for (final Nucleus p : list) {
			final IProfile profile = p.getProfile(ProfileType.ANGLE, pointType);
			final double nDifference = profile.absoluteSquareDifference(medianProfile,
					FIXED_PROFILE_LENGTH);
			if (nDifference < difference) {
				difference = nDifference;
				result = p;
			}
		}

		if (result == null)
			throw new MissingDataException("Error finding nucleus similar to median");
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
		if (vennCache.hasCount(d2))
			return vennCache.getCount(d2);
		final int shared = countSharedNuclei(d2);
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
	private int countSharedNuclei(ICellCollection d2) {

		if (d2 == this)
			return cellIDs.size();

		if (parentDataset.getCollection() == d2)
			return cellIDs.size();

		// Ensure cells use the same rule
		if (!d2.getRuleSetCollection().equals(parentDataset.getCollection().getRuleSetCollection()))
			return 0;

		final Set<UUID> toSearch = new HashSet<>(d2.getCellIDs());
		toSearch.retainAll(cellIDs);
		return toSearch.size();
	}

	@Override
	public int getMedianArrayLength() {
		if (size() == 0)
			return 0;

		final int[] p = this.getArrayLengths();
		return Stats.quartile(p, Stats.MEDIAN);
	}

	/**
	 * Get the array lengths of the nuclei in this collection as an array
	 * 
	 * @return
	 */
	private int[] getArrayLengths() {

		final int[] result = new int[this.getNuclei().size()];

		int i = 0;

		for (final Nucleus n : this.getNuclei()) {
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
	public double getMedian(@NonNull Measurement stat, String component, MeasurementScale scale)
			throws MissingDataException, SegmentUpdateException {
		if (this.size() == 0)
			return 0;
		return getMedianStatistic(stat, component, scale, null);
	}

	@Override
	public double[] getRawValues(@NonNull Measurement stat, String component,
			MeasurementScale scale)
			throws MissingDataException, SegmentUpdateException {
		return getRawValues(stat, component, scale, null);
	}

	@Override
	public double[] getRawValues(@NonNull Measurement stat, String component,
			MeasurementScale scale, UUID id)
			throws MissingDataException, SegmentUpdateException {

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
			UUID id)
			throws MissingDataException, SegmentUpdateException {

		return getMedianStatistic(stat, component, scale, id);
	}

	@Override
	public double getMin(@NonNull Measurement stat, String component, MeasurementScale scale)
			throws MissingDataException, SegmentUpdateException {
		return getMinStatistic(stat, component, scale, null);
	}

	@Override
	public double getMin(@NonNull Measurement stat, String component, MeasurementScale scale,
			UUID id)
			throws MissingDataException, SegmentUpdateException {

		// Handle old segment andSignalStatistic enums
		if (CellularComponent.NUCLEAR_SIGNAL.equals(component))
			return getMinStatistic(stat, CellularComponent.NUCLEAR_SIGNAL, scale, id);

		if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component))
			return getMinStatistic(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);
		return getMinStatistic(stat, component, scale, id);
	}

	private synchronized double getMinStatistic(Measurement stat, String component,
			MeasurementScale scale, UUID id)
			throws MissingDataException, SegmentUpdateException {
		final double[] values = getRawValues(stat, component, scale, id);
		return Arrays.stream(values).min().getAsDouble();
	}

	@Override
	public synchronized double getMax(@NonNull Measurement stat, String component,
			MeasurementScale scale)
			throws MissingDataException, MissingDataException, SegmentUpdateException {
		return getMaxStatistic(stat, component, scale, null);
	}

	@Override
	public double getMax(@NonNull Measurement stat, String component, MeasurementScale scale,
			UUID id)
			throws MissingDataException, SegmentUpdateException {

		// Handle old segment andSignalStatistic enums
		if (CellularComponent.NUCLEAR_SIGNAL.equals(component))
			return getMaxStatistic(stat, CellularComponent.NUCLEAR_SIGNAL, scale, id);

		if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component))
			return getMaxStatistic(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);
		return getMaxStatistic(stat, component, scale, id);
	}

	private double getMaxStatistic(Measurement stat, String component, MeasurementScale scale,
			UUID id)
			throws MissingDataException, SegmentUpdateException {

		final double[] values = getRawValues(stat, component, scale, id);
		return Arrays.stream(values).max().getAsDouble();
	}

	/**
	 * Calculate the length of the segment with the given name in each nucleus of
	 * the collection
	 * 
	 * @param stat  the measurement to fetch
	 * @param scale the scale to use
	 * @param id    the segment id to fetch
	 * @return the segment measurements for each nucleus in the collection
	 * @throws MissingMeasurementException via the unchecked stream
	 * @throws ProfileException            via the unchecked stream
	 * @throws MissingDataException        via the unchecked stream
	 */
	private synchronized double[] getSegmentStatistics(@NonNull Measurement stat,
			@NonNull MeasurementScale scale,
			@NonNull UUID id)
			throws MissingDataException {

		double[] result = null;

		result = getNuclei().parallelStream().mapToDouble(n -> StreamUtils.uncheckCall(callSegMeasurement(stat, scale, id, n))).sorted().toArray();

		return result;
	}

	private Callable<Double> callSegMeasurement(@NonNull Measurement stat,
			@NonNull MeasurementScale scale,
			@NonNull UUID id, Nucleus n) {

		return () -> {
			IProfileSegment segment;
			segment = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE).getSegment(id);
			double perimeterLength = 0;
			if (segment != null) {
				final int indexLength = segment.length();
				final double fractionOfPerimeter = (double) indexLength
						/ (double) segment.getProfileLength();
				perimeterLength = fractionOfPerimeter
						* n.getMeasurement(Measurement.PERIMETER, scale);
			}
			return perimeterLength;
		};

	}

	/**
	 * Get a list of the given statistic values for each nucleus in the collection
	 * 
	 * @param stat  the statistic to use
	 * @param scale the measurement scale
	 * @return a list of values
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 * @throws Exception
	 */
	private double[] getNuclearStatistics(Measurement stat, MeasurementScale scale)
			throws SegmentUpdateException, MissingDataException {

		double[] result = null;

		if (Measurement.VARIABILITY.equals(stat)) {
			result = this.getNormalisedDifferencesToMedianFromPoint(OrientationMark.REFERENCE);
		} else {
			result = this.getNuclei().parallelStream()
					.mapToDouble(n -> StreamUtils.uncheckCall(() -> n.getMeasurement(stat, scale)))
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
	private double[] getCellStatistics(Measurement stat, MeasurementScale scale)
			throws MissingDataException {
		return this.getCells().parallelStream()
				.mapToDouble(c -> StreamUtils.uncheckCall(callCellMeasurement(stat, scale, c)))
				.sorted()
				.toArray();
	}

	private Callable<Double> callCellMeasurement(@NonNull Measurement stat,
			@NonNull MeasurementScale scale,
			ICell c) {

		return () -> c.getMeasurement(stat, scale);

	}

	/**
	 * Get the differences to the median profile for each nucleus, normalised to the
	 * perimeter of the nucleus. This is the sum-of-squares difference, rooted and
	 * divided by the nuclear perimeter
	 * 
	 * @param pointType the point to fetch profiles from
	 * @return an array of normalised differences
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 */
	private synchronized double[] getNormalisedDifferencesToMedianFromPoint(
			OrientationMark pointType) throws SegmentUpdateException, MissingDataException {

		final IProfile medianProfile = this.getProfileCollection()
				.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN)
				.interpolate(FIXED_PROFILE_LENGTH);

		return getNuclei().stream().mapToDouble(n -> StreamUtils
				.uncheckCall(makeDifferenceToMedianCallable(pointType, n, medianProfile))).toArray();
	}

	private Callable<Double> makeDifferenceToMedianCallable(OrientationMark pointType, Nucleus n,
			@NonNull IProfile medianProfile) {
		return () -> {
			final IProfile angleProfile = n.getProfile(ProfileType.ANGLE, pointType);
			final double diff = angleProfile.absoluteSquareDifference(medianProfile,
					FIXED_PROFILE_LENGTH);
			return Math.sqrt(diff / FIXED_PROFILE_LENGTH); // differences in degrees, rather
															// than square degrees
		};
	}

	private double getMedianStatistic(Measurement stat, String component, MeasurementScale scale,
			UUID id) throws MissingDataException, SegmentUpdateException {

		if (!this.statsCache.has(stat, component, scale, id)) {
			final double[] values = getRawValues(stat, component, scale, id);
			statsCache.set(stat, component, scale, id, values);
		}

		return statsCache.getMedian(stat, component, scale, id);

	}

	@Override
	public double getNormalisedDifferenceToMedian(@NonNull OrientationMark pointType, Taggable t)
			throws SegmentUpdateException, MissingDataException {
		IProfile medianProfile;
		medianProfile = profileCollection.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN)
				.interpolate(FIXED_PROFILE_LENGTH);

		final IProfile angleProfile = t.getProfile(ProfileType.ANGLE, pointType);

		final double diff = angleProfile.absoluteSquareDifference(medianProfile,
				FIXED_PROFILE_LENGTH);
		return Math.sqrt(diff / FIXED_PROFILE_LENGTH);

	}
	
	@Override
	protected int recalculateHashcodeCache() {
		final int prime = 31;
		int result = super.recalculateHashcodeCache();
		result = prime * result + Objects.hash(cellIDs, consensusNucleus, name, profileCollection,
				shellResults, uuid);
		return result;
	}
	
	@Override
	public int hashCode() {
		if(isRecalcHashcode) { // default undeclared value
			hashcodeCache = recalculateHashcodeCache();
			isRecalcHashcode = false;
		}
		return hashcodeCache;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final VirtualDataset other = (VirtualDataset) obj;
		return Objects.equals(cellIDs, other.cellIDs)
				&& Objects.equals(consensusNucleus, other.consensusNucleus)
				&& Objects.equals(name, other.name)
				&& Objects.equals(profileCollection, other.profileCollection)
				&& Objects.equals(shellResults, other.shellResults)
				&& Objects.equals(uuid, other.uuid);
	}

	@Override
	public IAnalysisDataset addChildCollection(@NonNull ICellCollection collection)
			throws MissingDataException, SegmentUpdateException {
		final VirtualDataset c = new VirtualDataset(this, collection.getName(), null, collection);
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
			final String newName = chooseSuffix(dataset.getName());
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

		final Optional<IAnalysisOptions> op = getAnalysisOptions();
		if (op.isPresent()) {
			final Set<String> detectionOptions = op.get().getDetectionOptionTypes();
			for (final String detectedComponent : detectionOptions) {
				final Optional<HashOptions> subOptions = op.get().getDetectionOptions(detectedComponent);
				if (subOptions.isPresent()) {
					subOptions.get().setDouble(HashOptions.SCALE, scale);
				}
			}
		}

		for (final IAnalysisDataset child : getChildDatasets()) {
			child.setScale(scale);
		}

		for (final ICell c : getCells()) {
			c.setScale(scale);
		}

		if (hasConsensus()) {
			consensusNucleus.setScale(scale);
		}

		clear(MeasurementScale.MICRONS);
	}

	@Override
	public Set<UUID> getChildUUIDs() {
		final Set<UUID> result = new HashSet<>(childDatasets.size());
		for (final IAnalysisDataset c : childDatasets) {
			result.add(c.getId());
		}

		return result;
	}

	@Override
	public Set<UUID> getAllChildUUIDs() {
		final Set<UUID> result = new HashSet<>();

		final Set<UUID> idlist = getChildUUIDs();
		result.addAll(idlist);

		for (final UUID id : idlist) {
			final IAnalysisDataset d = getChildDataset(id);

			result.addAll(d.getAllChildUUIDs());
		}
		return result;
	}

	@Override
	public IAnalysisDataset getChildDataset(@NonNull UUID id) {
		if (this.hasDirectChild(id)) {

			for (final IAnalysisDataset c : childDatasets) {
				if (c.getId().equals(id))
					return c;
			}

		} else {
			for (final IAnalysisDataset child : this.getAllChildDatasets()) {
				if (child.getId().equals(id))
					return child;
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
		final List<IAnalysisDataset> result = new ArrayList<>();
		if (!childDatasets.isEmpty()) {

			for (final IAnalysisDataset d : childDatasets) {
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
			final List<IClusterGroup> groupsToDelete = new ArrayList<>();
			for (final IClusterGroup g : this.getClusterGroups()) {
				boolean clusterRemains = false;

				for (final UUID childID : g.getUUIDs()) {
					if (this.hasDirectChild(childID)) {
						clusterRemains = true;
					}
				}
				if (!clusterRemains) {
					groupsToDelete.add(g);
				}
			}

			// Remove the groups
			for (final IClusterGroup g : groupsToDelete) {
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
		final Iterator<IAnalysisDataset> it = childDatasets.iterator();

		while (it.hasNext()) {
			final IAnalysisDataset child = it.next();

			if (child.getId().equals(id)) {
				for (final IClusterGroup g : clusterGroups) {
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
			final UUID[] groupIds = group.getUUIDs().toArray(new UUID[0]);

			for (final UUID id : groupIds) {
				deleteChild(id);
			}

			// Remove saved values associated with the cluster group
			// e.g. tSNE, PCA
			for (final Nucleus n : getCollection().getNuclei()) {
				for (final Measurement s : n.getMeasurements()) {
					if (s.toString().endsWith(group.getId().toString())) {
						n.clearMeasurement(s);
					}
				}
			}
			this.clusterGroups.remove(group);
		}
	}

	@Override
	public void deleteClusterGroups() {
		LOGGER.fine("Deleting all cluster groups in " + getName());
		// Use arrays to avoid concurrent modifications to cluster groups
		final Object[] ids = clusterGroups.parallelStream().map(IClusterGroup::getId).toArray();
		for (final Object id : ids) {
			final Optional<IClusterGroup> optg = clusterGroups.stream()
					.filter(group -> group.getId().equals(id)).findFirst();
			if (optg.isPresent()) {
				deleteClusterGroup(optg.get());
			}
		}
	}

	@Override
	public void deleteMergeSource(@NonNull UUID id) {
		// No action
	}

	@Override
	public boolean hasDirectChild(@NonNull UUID id) {

		for (final IAnalysisDataset child : childDatasets) {
			if (child.getId().equals(id))
				return true;
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
	 * @author Ben Skinner
	 * @since 2.0.0
	 *
	 */
	public class DefaultProfileCollection implements IProfileCollection {

//		private static final String XML_INDEX_ATTRIBUTE = "index";
//
//		private static final String XML_NAME_ATTRIBUTE = "name";
//
//		private static final String XML_LANDMARK = "Landmark";

		/** The indexes of landmarks in the profiles and border list */
		private final Map<Landmark, Integer> landmarks = new HashMap<>();

		/** segments in the median profile with RP at zero */
		private List<IProfileSegment> segments = new ArrayList<>();

		/** cached median profiles for quicker access */
		private ProfileCache cache = new ProfileCache();

		/**
		 * Create an empty profile collection. The RP is set to the zero index by
		 * default.
		 */
		public DefaultProfileCollection() {
			final Landmark lm = parentDataset.getCollection().getRuleSetCollection()
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

			for (final Element el : e.getChildren(XMLNames.XML_LANDMARK)) {
				landmarks.put(new DefaultLandmark(el.getAttributeValue(XMLNames.XML_NAME)),
						Integer.parseInt(el.getAttributeValue(XMLNames.XML_INDEX)));
			}

			for (final Element el : e.getChildren(XMLNames.XML_SEGMENT)) {
				segments.add(new DefaultProfileSegment(el));
			}

		}

		/**
		 * Used for duplicating
		 * 
		 * @param p
		 * @throws SegmentUpdateException
		 */
		private DefaultProfileCollection(DefaultProfileCollection p) throws SegmentUpdateException {
			for (final Landmark l : p.landmarks.keySet()) {
				landmarks.put(l, p.landmarks.get(l));
			}

			for (final IProfileSegment s : p.segments) {
				segments.add(s.duplicate());
			}

			cache = p.cache.duplicate();
		}

		@Override
		public IProfileCollection duplicate() throws SegmentUpdateException {
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
			final Landmark lm = getLandmark(om);
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
			final List<Landmark> result = new ArrayList<>();
			result.addAll(landmarks.keySet());
			return result;
		}

		@Override
		public List<OrientationMark> getOrientationMarks() {
			final List<OrientationMark> result = new ArrayList<>();
			result.addAll(parentDataset.getCollection().getRuleSetCollection()
					.getOrientionMarks());
			return result;
		}

		@Override
		public boolean hasLandmark(@NonNull OrientationMark om) {
			return parentDataset.getCollection().getRuleSetCollection().getLandmark(om).isPresent();
		}

		@Override
		public synchronized IProfile getProfile(@NonNull ProfileType type,
				@NonNull OrientationMark om, int quartile)
				throws MissingDataException, SegmentUpdateException {
			if (!this.hasLandmark(om))
				throw new MissingLandmarkException(
						"Orientation point is not present: " + om.toString());

			final Landmark lm = getLandmark(om);

			return getProfile(type, lm, quartile);
		}

		@Override
		public synchronized IProfile getProfile(@NonNull ProfileType type,
				@NonNull Landmark lm, int quartile)
				throws MissingDataException, SegmentUpdateException {

			if (!cache.hasProfile(type, quartile, lm)) {
				final IProfileAggregate agg = createProfileAggregate(type,
						VirtualDataset.this.getMedianArrayLength());

				IProfile p = agg.getQuartile(quartile);
				final int offset = landmarks.get(lm);
				p = p.startFrom(offset);
				cache.addProfile(type, quartile, lm, p);
			}

			return cache.getProfile(type, quartile, lm);
		}

		@Override
		public ISegmentedProfile getSegmentedProfile(@NonNull ProfileType type,
				@NonNull OrientationMark tag,
				int quartile)
				throws MissingDataException, SegmentUpdateException {

			if (quartile < 0 || quartile > 100)
				throw new IllegalArgumentException("Quartile must be between 0-100");

			// get the profile array
			final IProfile p = getProfile(type, tag, quartile);
			if (segments.isEmpty())
				throw new MissingDataException("No segments assigned to profile collection");

			return new DefaultSegmentedProfile(p, getSegments(tag));
		}

		@Override
		public void calculateProfiles()
				throws MissingDataException, SegmentUpdateException {
			cache.clear();
			for (final ProfileType t : ProfileType.values()) {
				for (final Landmark lm : landmarks.keySet()) {
					getProfile(t, lm, Stats.MEDIAN);
					getProfile(t, lm, Stats.LOWER_QUARTILE);
					getProfile(t, lm, Stats.UPPER_QUARTILE);
				}
			}
		}

		@Override
		public synchronized List<UUID> getSegmentIDs() {
			final List<UUID> result = new ArrayList<>();
			if (segments == null)
				return result;
			for (final IProfileSegment seg : this.segments) {
				result.add(seg.getID());
			}
			return result;
		}

		@Override
		public synchronized IProfileSegment getSegmentAt(@NonNull OrientationMark tag, int position)
				throws MissingLandmarkException, SegmentUpdateException {
			return this.getSegments(tag).get(position);
		}

		@Override
		public synchronized List<IProfileSegment> getSegments(@NonNull OrientationMark tag)
				throws MissingLandmarkException, SegmentUpdateException {

			// this must be negative offset for segments
			// since we are moving the pointIndex back to the beginning
			// of the array
			final int offset = -getLandmarkIndex(tag);

			final List<IProfileSegment> result = new ArrayList<>();

			for (final IProfileSegment s : segments) {
				result.add(s.duplicate().offset(offset));
			}

			IProfileSegment.linkSegments(result);
			return result;
		}

		@Override
		public IProfileSegment getSegmentContaining(@NonNull OrientationMark tag)
				throws MissingLandmarkException, SegmentUpdateException {
			final List<IProfileSegment> segs = this.getSegments(tag);

			final IProfileSegment result = null;
			for (final IProfileSegment seg : segs) {
				if (seg.contains(ZERO_INDEX))
					return seg;
			}

			return result;
		}

		@Override
		public void setLandmark(@NonNull Landmark lm, int newIndex) {
			// Cannot move the RP from zero
			final Landmark rp = getLandmark(OrientationMark.REFERENCE);

			if (rp.equals(lm))
				return;
			cache.remove(lm);
			landmarks.put(lm, newIndex);

		}

		@Override
		public void setSegments(@NonNull List<IProfileSegment> n) throws MissingLandmarkException {
			if (n.isEmpty())
				throw new IllegalArgumentException("String or segment list is empty");

			final int length = VirtualDataset.this.getMedianArrayLength();
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
			final int offset = getLandmarkIndex(OrientationMark.REFERENCE);

			for (final IProfileSegment s : n) {
				s.offset(offset);
			}

			segments = new ArrayList<>();
			for (final IProfileSegment s : n) {
				segments.add(s.duplicate());
			}
		}

		private IProfileAggregate createProfileAggregate(@NonNull ProfileType type, int length)
				throws MissingDataException, SegmentUpdateException {
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

			final IProfileAggregate agg = new DefaultProfileAggregate(length,
					VirtualDataset.this.size());

			for (final Nucleus n : VirtualDataset.this.getNuclei()) {
				agg.addValues(n.getProfile(type, OrientationMark.REFERENCE));
			}
			return agg;

		}

		/**
		 * Allow a profile aggregate to be created and segments copied when median
		 * profile lengths have changed.
		 * 
		 * @param collection
		 * @param length
		 * @throws SegmentUpdateException
		 * @throws MissingDataException
		 */
		private IProfileAggregate createProfileAggregateOfDifferentLength(@NonNull ProfileType type,
				int length)
				throws MissingDataException, SegmentUpdateException {
			final Landmark lm = getLandmark(OrientationMark.REFERENCE);
			landmarks.put(lm, ZERO_INDEX);

			// We have no profile to use to interpolate segments.
			// Create an arbitrary profile with the original length.

			final List<IProfileSegment> originalSegList = new ArrayList<>();
			originalSegList.addAll(segments);
			final IProfile template = new DefaultProfile(0, segments.get(0).getProfileLength());
			final ISegmentedProfile segTemplate = new DefaultSegmentedProfile(template, originalSegList);

			// Now use the interpolation method to adjust the segment lengths
			final List<IProfileSegment> interpolatedSegments = segTemplate.interpolate(length)
					.getSegments();

			final IProfileAggregate agg = new DefaultProfileAggregate(length,
					VirtualDataset.this.size());

			for (final Nucleus n : VirtualDataset.this.getNuclei()) {
				agg.addValues(n.getProfile(type, OrientationMark.REFERENCE));
			}

			setSegments(interpolatedSegments);

			return agg;
		}

		@Override
		public String toString() {
			final String newLine = System.getProperty("line.separator");
			final StringBuilder builder = new StringBuilder();
			builder.append("Point types:" + newLine);
			for (final Entry<Landmark, Integer> e : landmarks.entrySet()) {
				builder.append(e.getKey() + ": " + e.getValue() + newLine);
			}

			// Show segments from RP
			try {
				builder.append("Segments from RP:" + newLine);
				for (final IProfileSegment s : this.getSegments(OrientationMark.REFERENCE)) {
					builder.append(s.toString() + newLine);
				}

			} catch (final Exception e) {
				builder.append("Exception fetching segments: " + e.getMessage());
			}

			return builder.toString();

		}

		@Override
		public IProfile getIQRProfile(@NonNull ProfileType type, @NonNull OrientationMark om)
				throws MissingDataException, SegmentUpdateException {

			final IProfile q25 = getProfile(type, om, Stats.LOWER_QUARTILE);
			final IProfile q75 = getProfile(type, om, Stats.UPPER_QUARTILE);

			if (q25 == null)
				throw new MissingDataException("Could not create q25 profile; was null");

			if (q75 == null)
				throw new MissingDataException("Could not create q75 profile; was null");

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
			final DefaultProfileCollection other = (DefaultProfileCollection) obj;
			return Objects.equals(landmarks, other.landmarks)
					&& Objects.equals(segments, other.segments);
		}

		@Override
		@NonNull public Element toXmlElement() {
			final Element e = new Element(XMLNames.XML_PROFILE_COLLECTION);

			for (final IProfileSegment s : segments) {
				e.addContent(s.toXmlElement());
			}

			for (final Entry<Landmark, Integer> entry : landmarks.entrySet()) {
				e.addContent(
						new Element(XMLNames.XML_LANDMARK).setAttribute(XMLNames.XML_NAME, entry.getKey().toString())
								.setAttribute(XMLNames.XML_INDEX, String.valueOf(entry.getValue())));
			}
			return e;
		}

		/**
		 * The cache for profiles
		 * 
		 * @author Ben Skinner
		 * @since 1.13.4
		 *
		 */
		private class ProfileCache {

			/**
			 * The key used to store values in the cache
			 * 
			 * @author Ben Skinner
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
					final ProfileKey other = (ProfileKey) obj;
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

			private final Map<ProfileKey, IProfile> map = new HashMap<>();

			public ProfileCache() { // no default data
			}

			public ProfileCache duplicate() throws SegmentUpdateException {
				final ProfileCache result = new ProfileCache();
				for (final ProfileKey k : map.keySet()) {
					final IProfile p = map.get(k);
					if (p != null) {
						result.map.put(k, p.duplicate());
					}
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
				final ProfileKey key = new ProfileKey(type, quartile, tag);
				map.put(key, profile);
			}

			public boolean hasProfile(final ProfileType type, final double quartile,
					final Landmark tag) {
				final ProfileKey key = new ProfileKey(type, quartile, tag);
				return map.containsKey(key);
			}

			public IProfile getProfile(final ProfileType type, final double quartile,
					final Landmark tag) {
				final ProfileKey key = new ProfileKey(type, quartile, tag);
				return map.get(key);
			}

			/**
			 * Remove all profiles from the cache
			 */
			public void clear() {
				map.clear();
			}

			public void remove(final Landmark t) {

				final Iterator<ProfileKey> it = map.keySet().iterator();
				while (it.hasNext()) {
					final ProfileKey k = it.next();
					if (k.has(t)) {
						it.remove();
					}
				}

			}
		}

		@Override
		public double getProportionOfIndex(int index) {
			final int length = VirtualDataset.this.getMedianArrayLength();
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

			final int length = VirtualDataset.this.getMedianArrayLength();
			if (proportion == 1)
				return length - 1;

			final double desiredDistanceFromStart = length * proportion;
			final int target = (int) desiredDistanceFromStart;
			return target;
		}
	}

}
