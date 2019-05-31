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
package com.bmskinner.nuclear_morphology.components;

import java.awt.Color;
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
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.generic.DefaultProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
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
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SegmentStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.components.stats.StatsCache;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * This class provides access to child dataset ICell lists and statistics
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class VirtualCellCollection implements ICellCollection {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

    private static final long serialVersionUID = 1L;

    /** the dataset this is a child of */
    private final IAnalysisDataset parent;

    /** the cells that belong to this collection */
    private final Set<UUID> cellIDs = new HashSet<>(0);

    /** the collection id */
    private final UUID uuid;

    /** the name of the collection */
    private String name;

    /** this holds the mapping of tail indexes etc in the median profile arrays */
    private IProfileCollection profileCollection = new DefaultProfileCollection();

    /** the refolded consensus nucleus */
    private Consensus<Nucleus> consensusNucleus;

    /** Store signal groups separately to allow shell results to be kept */
    private Map<UUID, IShellResult> shellResults = new HashMap<>(0);

    /*
     * TRANSIENT FIELDS
     */

    protected volatile transient Map<UUID, Integer> vennCache = new HashMap<>();

    private transient ProfileManager profileManager = new ProfileManager(this);
    private transient SignalManager  signalManager  = new SignalManager(this);
    private volatile transient StatsCache statsCache = new StatsCache();

    /**
     * Create from a parent dataset, and provide a name
     * 
     * @param parent
     * @param name
     */
    public VirtualCellCollection(@NonNull IAnalysisDataset parent, @NonNull String name) {
        this(parent, name, UUID.randomUUID());
    }

    /**
     * Create from a parent dataset, and provide a name and UUID
     * 
     * @param parent
     * @param name
     * @param id
     */
    public VirtualCellCollection(@NonNull IAnalysisDataset parent, @NonNull String name, @NonNull UUID id) {
        this.parent = parent;
        this.name = name == null ? "Undefined dataset name" : name;
        this.uuid = id;

    }
    
    /**
     * Create for a parent dataset, providing a collection of cells 
     * to populate the new collection. The name and ID are copied from
     * the cell collection.
     * 
     * @param parent the dataset to which this will belong
     * @param cells the collection of cells to add to this collection
     */
    public VirtualCellCollection(@NonNull IAnalysisDataset parent, @NonNull ICellCollection cells) {
        this(parent, cells.getName(), cells.getID(), cells);
    }

    /**
     * Create from a parent dataset, spcifying the collection name and id, and
     * providing a collection of cells to populate the new collection.
     * 
     * @param parent the dataset to which this will belong
     * @param name the name of the collection
     * @param id the id of the collection
     * @param cells the collection of cells to add to this collection
     */
    public VirtualCellCollection(@NonNull IAnalysisDataset parent, @NonNull String name, @NonNull UUID id, @NonNull ICellCollection cells) {
        this(parent, name, id);
        for (ICell cell : cells.getCells()) {
            this.addCell(cell);
        }
    }
    
	@Override
	public ICellCollection duplicate() {
		VirtualCellCollection result = new VirtualCellCollection(parent, this);
		
		result.consensusNucleus = consensusNucleus.duplicateConsensus();
		result.profileCollection = profileCollection.duplicate();
		
		return result;
	}

    public IAnalysisDataset getParent() {
        return parent;
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
    public UUID getID() {
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
		return cellIDs.add(e.getId());
	}

	@Override
	public boolean addAll(Collection<? extends ICell> c) {
		return cellIDs.addAll(c.stream().map(cell->cell.getId()).collect(Collectors.toSet()));
	}

	@Override
	public void clear() {
		cellIDs.clear();
	}

	@Override
	public boolean contains(Object o) {
		return getCells().contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return getCells().containsAll(c);
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
		return getCells().remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return getCells().removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
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
    public synchronized Set<ICell> getCells() {

        Set<ICell> result = new HashSet<ICell>(cellIDs.size());
        ICellCollection parentCollection = parent.getCollection();
        if (parentCollection == null) {
            LOGGER.warning("Cannot access parent collection");
            return result;
        }
        return parentCollection.getCells().parallelStream()
	        .filter(c->cellIDs.contains(c.getId()))
	        .collect(Collectors.toSet());
//        for (ICell cell : parentCollection.getCells()) {
//            if (cellIDs.contains(cell.getId())) {
//                result.add(cell);
//            }
//        }
//        return result;
    }
    
    @Override
    public synchronized Stream<ICell> streamCells() {
        return getCells().stream();
    }

    @Override
    public synchronized Set<ICell> getCells(@NonNull File f) {
        Set<ICell> result = new HashSet<ICell>(cellIDs.size());

        for (ICell cell : parent.getCollection().getCells()) {
            if (cellIDs.contains(cell.getId())) {
                if (cell.getNucleus().getSourceFile().equals(f)) {
                    result.add(cell);
                }
            }
        }
        return result;
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
    public synchronized Set<UUID> getCellIDs() {
        return new HashSet<UUID>(cellIDs);
    }

    @Override
    public synchronized Set<Nucleus> getNuclei() {

        ICellCollection parentCollection = parent.getCollection();

        if (parentCollection == null) {
            LOGGER.warning("Parent collection not restored!");
            return new HashSet<Nucleus>(cellIDs.size());
        }
        
        return parentCollection.getCells().parallelStream()
	        .filter(c->cellIDs.contains(c.getId()))
	        .flatMap(c -> c.getNuclei().stream())
	        .collect(Collectors.toSet());
    }

    @Override
    public int getNucleusCount() {
        return this.getNuclei().size();
    }

    @Override
    public synchronized Set<Nucleus> getNuclei(@NonNull File imageFile) {

        ICellCollection parentCollection = parent.getCollection();
        if (parentCollection == null) {
            LOGGER.warning("Parent collection not restored!");
            return new HashSet<Nucleus>();
        }

        return cellIDs.stream().map(id -> parentCollection.getCell(id))
                .flatMap(c -> c.getNuclei().stream())
                .filter(n -> n.getSourceFile().equals(imageFile))
                .collect(Collectors.toSet());
    }

    @Override
    public void addCell(@NonNull ICell c) {
        if (!parent.getCollection().contains(c.getId()))
            throw new IllegalArgumentException("Cannot add a cell to a virtual collection that is not in the parent");
        cellIDs.add(c.getId());
    }

    @Override
    public void replaceCell(@NonNull ICell c) {
        parent.getCollection().replaceCell(c);
    }

    @Override
    public ICell getCell(@NonNull UUID id) {
        if (cellIDs.contains(id))
            return parent.getCollection().getCell(id);
        return null;
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

    @Override
    public NucleusType getNucleusType() {
        return parent.getCollection().getNucleusType();
    }

    @Override
    public void removeCell(@NonNull ICell c) {
        cellIDs.remove(c.getId());

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
	public void setConsensus(@Nullable Consensus<Nucleus> n) {
		consensusNucleus = n;
	}
	
	@Override
	public Consensus<Nucleus> getRawConsensus() {
		return consensusNucleus;
	}

	@Override
	public Nucleus getConsensus() {
		return consensusNucleus.component().getVerticallyRotatedNucleus();
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
    public boolean hasCells() {
        return !cellIDs.isEmpty();
    }

    @Override
    public boolean contains(ICell cell) {
    	if(cell==null)
			return false;
        return cellIDs.contains(cell.getId());
    }

    @Override
    public boolean contains(UUID cellID) {
        return cellIDs.contains(cellID);
    }

    @Override
    public boolean containsExact(@NonNull ICell cell) {
        return parent.getCollection().containsExact(cell);
    }

    @Override
    public boolean hasLockedCells() {
        return parent.getCollection().hasLockedCells();
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

    /**
     * Create the profile collections to hold angles from nuclear profiles based
     * on the current nucleus profiles. The ProfileAggregate for each
     * ProfileType is recalculated. The resulting median profiles will have the
     * same length at the parent collection after this update
     * 
     * @return
     * @throws ProfileException 
     */
    @Override
	public void createProfileCollection() throws ProfileException {
        profileCollection.createProfileAggregate(this, parent.getCollection().getMedianArrayLength());
    }

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
    public void setOutputFolder(@NonNull File folder) {
        parent.getCollection().setOutputFolder(folder);
    }

    @Override
    public Set<File> getImageFiles() {

        Set<File> result = new HashSet<File>(cellIDs.size());

        for (ICell c : getCells()) {
            result.add(c.getNucleus().getSourceFile());
        }
        return result;
    }

    @Override
    public Set<UUID> getSignalGroupIDs() {
        return parent.getCollection().getSignalGroupIDs();
    }

    @Override
    public void removeSignalGroup(@NonNull UUID id) {
    	parent.getCollection().removeSignalGroup(id);
    }

    @Override
    public  Optional<ISignalGroup> getSignalGroup(@NonNull UUID signalGroup) {

    	if(!parent.getCollection().hasSignalGroup(signalGroup))
    		return Optional.empty();
    	    
        // Override the shell storage to point to this collection, not the shell
        // result
        // This SignalGroup is never saved to file, so does not need serialising
        @SuppressWarnings("serial")
        ISignalGroup result = new SignalGroup(parent.getCollection().getSignalGroup(signalGroup).get(), true) {

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
                parent.getCollection().getSignalGroup(signalGroup).get().setGroupColour(newColor);
            }

            @Override
            public void setVisible(boolean b) {
                parent.getCollection().getSignalGroup(signalGroup).get().setVisible(b);
            }

        };

        return Optional.of(result);
    }

    @Override
    public boolean hasSignalGroup(@NonNull UUID signalGroup) {
        return parent.getCollection().hasSignalGroup(signalGroup);
    }

    @Override
    public Collection<ISignalGroup> getSignalGroups() {
        return parent.getCollection().getSignalGroups();
    }

    @Override
    public void addSignalGroup(@NonNull UUID newID, @NonNull ISignalGroup newGroup) {
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
    public void setSourceFolder(@NonNull File expectedImageDirectory) {
        parent.getCollection().setSourceFolder(expectedImageDirectory);
    }

	/**
	 * Get the nucleus with the lowest difference score to the median profile
	 * 
	 * @param pointType the point to compare profiles from
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
			for (Nucleus p : list)
				return p;
		}

		IProfile medianProfile = profileCollection.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN).interpolate(FIXED_PROFILE_LENGTH);

		Nucleus n = null;

		double difference = Arrays.stream(getNormalisedDifferencesToMedianFromPoint(pointType)).max().orElse(0);
		for (Nucleus p : list) {
			IProfile angleProfile = p.getProfile(ProfileType.ANGLE, pointType);
			double nDifference = angleProfile.absoluteSquareDifference(medianProfile, FIXED_PROFILE_LENGTH);
			if (nDifference < difference) {
				difference = nDifference;
				n = p;
			}
		}

		if (n == null)
			throw new ProfileException("Error finding nucleus similar to median");
		return n;
	}

    @Override
    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public IAnalysisDataset getRootParent() {
        if (parent.isRoot()) {
            return parent;
        }
		if (parent.getCollection() instanceof VirtualCellCollection) {
		    VirtualCellCollection v = (VirtualCellCollection) parent.getCollection();
		    return v.getRootParent();
		}
		return null;
    }

	/**
	 * Choose if the merged collection for this and another collection should be a child of this,
	 * a child of the other collection, or a new real collection.
	 * @param other the other collection which will be merged
	 * @param newName the name of the new collection
	 * @return the new collection of the correct type
	 */
	private ICellCollection chooseNewCollectionType(@NonNull ICellCollection other, String newName) {

		// Decide if the other collection is also a child of the same root parent
		IAnalysisDataset rootThis  = DatasetListManager.getInstance().getRootParent(this);
		IAnalysisDataset rootOther = DatasetListManager.getInstance().getRootParent(other);
		
		// If the two datasets have different root parents, return a new real collection
		return rootThis==rootOther ? new VirtualCellCollection(rootThis, newName)
								   : new DefaultCellCollection(this, newName);
	}

    @Override
    public ICellCollection and(@NonNull ICellCollection other) {

        ICellCollection newCollection = chooseNewCollectionType(other, "AND operation");

        for (ICell c : other.getCells()) {

            if (this.contains(c)) {
                newCollection.addCell(new DefaultCell(c));
            }
        }

        return newCollection;
    }

    @Override
    public ICellCollection not(@NonNull ICellCollection other) {
        ICellCollection newCollection = chooseNewCollectionType(other, "NOT operation");

        for (ICell c : getCells()) {

            if (!other.contains(c)) {
                newCollection.addCell(new DefaultCell(c));
            }
        }

        return newCollection;
    }

    @Override
    public ICellCollection xor(@NonNull ICellCollection other) {
        ICellCollection newCollection = chooseNewCollectionType(other, "XOR operation");

        for (ICell c : getCells()) {

            if (!other.contains(c)) {
                newCollection.addCell(new DefaultCell(c));
            }
        }

        for (ICell c : other.getCells()) {

            if (!this.contains(c)) {
                newCollection.addCell(new DefaultCell(c));
            }
        }

        return newCollection;
    }

    @Override
    public ICellCollection or(@NonNull ICellCollection other) {
        ICellCollection newCollection = chooseNewCollectionType(other, "OR operation");

        for (ICell c : getCells()) {
            newCollection.addCell(new DefaultCell(c));
        }

        for (ICell c : other.getCells()) {

            if (!this.contains(c)) {
                newCollection.addCell(new DefaultCell(c));
            }
        }

        return newCollection;
    }

    @Override
    public ICellCollection filter(@NonNull Predicate<ICell> predicate) {

        String newName = "Filtered_" + predicate.toString();

        ICellCollection subCollection = new DefaultCellCollection(this, newName);

        List<ICell> list = getCells().stream().filter(predicate).collect(Collectors.toList());

        LOGGER.finest( "Adding cells to new collection");
        for (ICell cell : list)
            subCollection.addCell(new DefaultCell(cell));

        if (subCollection.size() == 0) {
            LOGGER.warning("No cells in collection");
        } else {
        	try {

                // TODO - this fails on converted collections from (at least) 1.13.0
                // with no profiles in aggregate
                this.getProfileManager().copyCollectionOffsets(subCollection);
                this.getSignalManager().copySignalGroups(subCollection);

            } catch (ProfileException e) {
                LOGGER.warning("Error copying collection offsets");
                LOGGER.log(Loggable.STACK, "Error in offsetting", e);
            }
        }
        return subCollection;
    }

    @Override
    public ICellCollection filterCollection(@NonNull PlottableStatistic stat, MeasurementScale scale, double lower,
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
    public int countShared(@NonNull IAnalysisDataset d2) {
        return countShared(d2.getCollection());

    }

    @Override
    public int countShared(@NonNull ICellCollection d2) {
        if (this.vennCache.containsKey(d2.getID())) {
            return vennCache.get(d2.getID());
        }
        int shared = countSharedNuclei(d2);
        vennCache.put(d2.getID(), shared);
        d2.setSharedCount(this, shared);
        return shared;
    }

    @Override
    public void setSharedCount(@NonNull ICellCollection d2, int i) {
        vennCache.put(d2.getID(), i);
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
            return this.size();

        if (parent.getCollection() == d2)
            return this.size();
        

        if (d2.getNucleusType() != this.getNucleusType())
            return 0;
        
        Set<UUID> toSearch = new HashSet<>(d2.getCellIDs());
        toSearch.retainAll(getCellIDs());
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
    public synchronized double getMedian(PlottableStatistic stat, String component, MeasurementScale scale)
            throws Exception {
        if (this.size() == 0) {
            return 0;
        }
        return getMedianStatistic(stat, component, scale, null);
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
			LOGGER.warning("No component of type " + component + " can be handled");
			return null;
		}
	}
    }

    @Override
    public double getMedian(PlottableStatistic stat, String component, MeasurementScale scale, UUID id)
            throws Exception {

        return getMedianStatistic(stat, component, scale, id);
    }
    
    @Override
    public synchronized double getMin(PlottableStatistic stat, String component, MeasurementScale scale) {
        return getMinStatistic(stat, component, scale, null);
    }
    
    @Override
    public synchronized double getMin(PlottableStatistic stat, String component, MeasurementScale scale,
            UUID id){

    	// Handle old segment andSignalStatistic enums
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
        if (CellularComponent.NUCLEAR_SIGNAL.equals(component) || stat.getClass() == SignalStatistic.class) {
            return getMaxStatistic(stat, CellularComponent.NUCLEAR_SIGNAL, scale, id);
        }

        if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component) || stat.getClass() == SegmentStatistic.class) {
            return getMaxStatistic(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);
        }
        return getMaxStatistic(stat, component, scale, id);
    }
    
    private synchronized double getMaxStatistic(PlottableStatistic stat, String component, MeasurementScale scale,
    		UUID id) {

    	double[] values = getRawValues(stat, component, scale, id);
    	return Arrays.stream(values).max().orElse(Statistical.ERROR_CALCULATING_STAT);
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
    private double[] getSegmentStatistics(PlottableStatistic stat, MeasurementScale scale, UUID id) {

    	double[] result = null;
    	if (statsCache.hasValues(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id)) {
    		return statsCache.getValues(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);

    	}
		result = getNuclei().parallelStream().mapToDouble(n -> {
			IBorderSegment segment;
			try {
				segment = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegment(id);
			} catch (ProfileException | UnavailableComponentException e) {
				return 0;
			}
			double perimeterLength = 0;
			if (segment != null) {
				int indexLength = segment.length();
				double fractionOfPerimeter = (double) indexLength / (double) segment.getProfileLength();
				perimeterLength = fractionOfPerimeter * n.getStatistic(PlottableStatistic.PERIMETER, scale);
			}
			return perimeterLength;

		}).toArray();
		Arrays.sort(result);
		statsCache.setValues(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id, result);
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
     * Get the given statistic values for each cell in the
     * collection
     * 
     * @param stat the statistic to fetch
     * @param scale the measurement scale
     * @return a list of values
     */
    private double[] getCellStatistics(PlottableStatistic stat, MeasurementScale scale) {
    	double[] result = null;
		if (statsCache.hasValues(stat, CellularComponent.WHOLE_CELL, scale, null))
			return statsCache.getValues(stat, CellularComponent.WHOLE_CELL, scale, null);
		result = getCells().parallelStream().mapToDouble(c -> c.getStatistic(stat, scale)).sorted().toArray();
		statsCache.setValues(stat, CellularComponent.WHOLE_CELL, scale, null, result);
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
    private synchronized double[] getNormalisedDifferencesToMedianFromPoint(Tag pointType) {

        IProfile medianProfile;
        try {
            medianProfile = this.getProfileCollection().getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN).interpolate(FIXED_PROFILE_LENGTH);
        } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
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

            } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
                LOGGER.log(Loggable.STACK, "Error getting nucleus profile", e);
                return  Double.MAX_VALUE;
            } 
        }).toArray();
    }

    private double getMedianStatistic(PlottableStatistic stat, String component, MeasurementScale scale,
            UUID id) throws Exception {

        if (this.statsCache.hasMedian(stat, component, scale, id)) {
            return statsCache.getMedian(stat, component, scale,id);

        }
		double median = Statistical.ERROR_CALCULATING_STAT;

		if (this.hasCells()) {
			
			double[] values = getRawValues(stat, component, scale, id);

		    DescriptiveStatistics  s = new DescriptiveStatistics ();
		    for(double v  : values){
		    	s.addValue(v);
		    }
		    median = s.getPercentile(Stats.MEDIAN);
		}

		statsCache.setMedian(stat, component, scale, id, median);
		return median;

    }
    
    @Override
    public void setScale(double scale){
    	
        for (ICell c : getCells())
            c.setScale(scale);

        if (hasConsensus())
        	consensusNucleus.component().setScale(scale);

        clear(MeasurementScale.MICRONS);
    }

    @Override
    public double getNormalisedDifferenceToMedian(Tag pointType, Taggable t) {
        IProfile medianProfile;
        try {
            medianProfile = profileCollection.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN).interpolate(FIXED_PROFILE_LENGTH);
        } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
            LOGGER.log(Loggable.STACK, "Error getting median profile for collection", e);
            return 0;
        }
        
        try {
        	IProfile angleProfile = t.getProfile(ProfileType.ANGLE, pointType);

            double diff = angleProfile.absoluteSquareDifference(medianProfile, FIXED_PROFILE_LENGTH);
            return Math.sqrt(diff/FIXED_PROFILE_LENGTH);
        } catch (ProfileException | UnavailableComponentException e) {
            LOGGER.log(Loggable.STACK, "Error getting nucleus profile", e);
            return Double.NaN;
        }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        in.defaultReadObject();
        vennCache  = new HashMap<>(); // cache the number of shared nuclei with other datasets
        statsCache = new StatsCache();

        signalManager = new SignalManager(this);
        profileManager = new ProfileManager(this);

        if(this.hasConsensus()) {
			rotateConsensus(0);
			offsetConsensus(0, 0);
        }
        
        // Don't try to restore profile aggregates here - the parent collection has
        // not finished loading, and so calls to parent will be null. Do the restore in the
        // parent class after reading this object has finished.
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cellIDs == null) ? 0 : cellIDs.hashCode());
		result = prime * result + ((consensusNucleus == null) ? 0 : consensusNucleus.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((profileCollection == null) ? 0 : profileCollection.hashCode());
		result = prime * result + ((shellResults == null) ? 0 : shellResults.hashCode());
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
		VirtualCellCollection other = (VirtualCellCollection) obj;
		if (cellIDs == null) {
			if (other.cellIDs != null)
				return false;
		} else if (!cellIDs.equals(other.cellIDs))
			return false;
		if (consensusNucleus == null) {
			if (other.consensusNucleus != null)
				return false;
		} else if (!consensusNucleus.equals(other.consensusNucleus))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (profileCollection == null) {
			if (other.profileCollection != null)
				return false;
		} else if (!profileCollection.equals(other.profileCollection))
			return false;
		if (shellResults == null) {
			if (other.shellResults != null)
				return false;
		} else if (!shellResults.equals(other.shellResults))
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
    
	@Override
	public String toString() {

		String newLine = System.getProperty("line.separator");

		StringBuilder b = new StringBuilder("Collection:" + getName() + newLine)
				.append("Nuclei: " + this.getNucleusCount() + newLine)
				.append("Source folder: " + this.getFolder().getAbsolutePath() + newLine)
				.append("Profile collections:" + newLine)
				.append("Parent: "+parent.getName());

		IProfileCollection pc = this.getProfileCollection();
		b.append(pc.toString() + newLine);

		if(this.hasConsensus()){
			b.append("Consensus:" + newLine);
			b.append(getConsensus().toString()+newLine);
		}

		return b.toString();
	}
    

}
