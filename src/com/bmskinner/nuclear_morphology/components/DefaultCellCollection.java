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
import java.util.ArrayList;
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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
//import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileManager;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileSegmenter;
import com.bmskinner.nuclear_morphology.analysis.profiles.Taggable;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileSegmenter.UnsegmentableProfileException;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileManager;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.generic.BorderTag.BorderTagType;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SegmentStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.components.stats.StatsCache;
import com.bmskinner.nuclear_morphology.components.stats.VennCache;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.main.DatasetListManager;
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

    private Set<ICell> cells = new HashSet<ICell>(100); // store all the cells
                                                        // analysed

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
     * @param folder
     *            the folder of images
     * @param outputFolder
     *            a name for the outputs (usually the analysis date). Can be
     *            null
     * @param name
     *            the name of the collection
     * @param nucleusClass
     *            the class of nucleus to be held
     */
    public DefaultCellCollection(File folder, @Nullable String outputFolder, @Nullable String name, NucleusType nucleusType) {
        this(folder, outputFolder, name, nucleusType, java.util.UUID.randomUUID());
    }

    /**
     * Constructor with non-random id. Use only when copying an old collection.
     * Can cause ID conflicts!
     * 
     * @param folder
     *            the folder of images
     * @param outputFolder
     *            a name for the outputs (usually the analysis date). Can be
     *            null
     * @param name
     *            the name of the collection
     * @param nucleusClass
     *            the class of nucleus to be held
     * @param id
     *            specify an id for the collection, rather than generating
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

        // for(ProfileType type : ProfileType.values()){
        // profileCollections.put(type, new DefaultProfileCollection());
        // }

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

    public void setName(String s) {
        this.name = s;
    }

    public String getName() {
        return this.name;
    }

    public UUID getID() {
        return this.uuid;
    }

    public boolean isReal() {
        return true;
    }

    public boolean isVirtual() {
        return false;
    }

    /**
     * Get the UUIDs of all the cells in the collection
     * 
     * @return
     */
    public Set<UUID> getCellIDs() {

        return cells.parallelStream().map(c -> c.getId()).collect(Collectors.toSet());

    }

    public void addCell(final ICell r) {

        if (r == null) {
            throw new IllegalArgumentException("Cell is null");
        }

        cells.add(r);
    }

    /**
     * Replace the cell with the same ID as the given cell with the new copy
     * 
     * @param r
     */
    public void replaceCell(ICell r) {
        if (r == null) {
            throw new IllegalArgumentException("Cell is null");
        }

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
        if (found) {
            addCell(r);
        }

    }

    /**
     * Remove the given cell from the collection. If the cell is null, has no
     * effect. If the cell is not in the collection, has no effect.
     * 
     * @param c
     *            the cell to remove
     */
    public void removeCell(ICell c) {
        cells.remove(c);
    }

    public int size() {
        return cells.size();
    }

    /**
     * Check if the collection contains cells
     * 
     * @return
     */
    public boolean hasCells() {
        return !cells.isEmpty();
    }

    public boolean hasLockedCells() {
        for (Nucleus n : this.getNuclei()) {
            if (n.isLocked()) {
                return true;
            }
        }
        return false;
    }

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
    public ICell getCell(UUID id) {
    	return cells.parallelStream()
    			.filter(c->c.getId().equals(id))
    			.findFirst().orElse(null);
    }

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
        return this.consensusNucleus != null;
    }

    @Override
    public void setConsensus(@Nullable Nucleus n) {
        this.consensusNucleus = n;
    }

    @Override
    public Nucleus getConsensus() {
        return this.consensusNucleus;
    }

    @Override
    public synchronized boolean isRefolding() {
        return this.isRefolding;
    }

    @Override
    public synchronized void setRefolding(boolean b) {
        this.isRefolding = b;
    }

    /**
     * Get the profile collection of the given type
     * 
     * @param type
     * @return
     */
    public IProfileCollection getProfileCollection() {
        return profileCollection;
    }

    public File getFolder() {
        return this.folder;
    }

    public String getOutputFolderName() {
        return this.outputFolder;
    }

    /**
     * Get the output folder (e.g. to save the dataset into). If an output
     * folder name (such as a date) has been input, it will be included
     * 
     * @return the folder
     */
    public File getOutputFolder() {
        File result = null;
        if (outputFolder == null) {
            result = this.getFolder();
        } else {
            result = new File(this.getFolder(), outputFolder);
        }
        return result;
    }

    @Override
    public void setOutputFolder(File folder) {
        this.folder = folder;
        this.outputFolder = null;
    }

    /**
     * Get the distinct source image file list for all nuclei in the collection
     * 
     * @return
     */
    public Set<File> getImageFiles() {
        Set<File> result = new HashSet<File>(0);
        for (Nucleus n : this.getNuclei()) {
            result.add(n.getSourceFile());
        }
        return result;
    }

    /**
     * Get the array lengths of the nuclei in this collection as an array
     * 
     * @return
     */
    private int[] getArrayLengths() {

        int[] result = new int[this.getNuclei().size()];

        int i = 0;
        
        
        
        for (ICell cell : cells) {

            for (Nucleus n : cell.getNuclei()) {
                // Nucleus n = cell.getNucleus();
                result[i++] = n.getBorderLength();
            }
        }
        return result;
    }

    public double[] getMedianDistanceBetweenPoints() {

        int count = this.getNucleusCount();
        double[] result = new double[count];

        int i = 0;

        for (Nucleus n : getNuclei()) {
            result[i++] = n.getMedianDistanceBetweenPoints();
        }

        return result;
    }

    @Override
    public int getNucleusCount() {
        return this.getNuclei().size();
    }

    /**
     * Get the cells in this collection
     * 
     * @return
     */
      public Set<ICell> getCells() {
        return cells;
    }
      
      /**
       * Get the cells in this collection
       * 
       * @return
       */
      public Stream<ICell> streamCells() {
          return cells.stream();
      }

    /**
     * Get the cells within the given image file
     * 
     * @return
     */
    public Set<ICell> getCells(File imageFile) {
        Set<ICell> result = new HashSet<ICell>(cells.size());

        for (ICell cell : cells) {
            if (cell.getNuclei().get(0).getSourceFile().equals(imageFile)) {
                result.add(cell);
            }
        }
        return result;
    }

    @Override
    public boolean hasCells(File imageFile) {
        return getCells(imageFile).size() > 0;
    }

    @Override
    public boolean hasNuclei(File imageFile) {
        return getNuclei(imageFile).size() > 0;
    }

    /**
     * Get the nuclei in this collection
     * 
     * @return
     */
    public Set<Nucleus> getNuclei() {

        Set<Nucleus> result = new HashSet<Nucleus>(cells.size());
        for (ICell c : cells) {
            result.addAll(c.getNuclei());
        }

        return result;
    }

    /**
     * Get the nuclei within the specified image
     * 
     * @param image
     *            the file to search
     * @return the list of nuclei
     */
    public Set<Nucleus> getNuclei(File imageFile) {
        Set<Nucleus> result = new HashSet<Nucleus>(cells.size());
        for (Nucleus n : this.getNuclei()) {
            if (n.getSourceFile().equals(imageFile)) {
                result.add(n);
            }
        }
        return result;
    }

    /**
     * Create a SignalManager with responsibility for aggregate nuclear signal
     * methods.
     * 
     * @return
     */
    public SignalManager getSignalManager() {
        return signalManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public int getMedianArrayLength() {
        if (size() == 0) {
            return 0;
        }

        int[] p = this.getArrayLengths();
        return Stats.quartile(p, Stats.MEDIAN);
    }

    public int getMaxProfileLength() {

        return Arrays.stream(this.getArrayLengths()).max().orElse(0); // Stats.max(values);
    }

    /*
     * -------------------- Profile methods --------------------
     */

    /**
     * Create the profile collections to hold angles from nuclear profiles based
     * on the current nucleus profiles. The ProfileAggregate for each
     * ProfileType is recalculated. The resulting median profiles will have the
     * same length after this update
     * 
     * @param keepLength
     *            when recalculating the profile aggregate, should the previous
     *            length be kept
     * @return
     * @throws Exception
     */
    public void createProfileCollection() {

        /*
         * Build a set of profile aggregates Default is to make profile
         * aggregate from reference point
         * 
         */
        createProfileCollection(this.getMedianArrayLength());
    }

    public void createProfileCollection(int length) {

        /*
         * Build a set of profile aggregates Default is to make profile
         * aggregate from reference point
         * 
         */
        profileCollection.createProfileAggregate(this, length);
    }

    /**
     * Get a list of all the segments currently within the profile collection
     * 
     * @return
     */
    public List<String> getSegmentNames() throws Exception {

        List<String> result = new ArrayList<String>(0);
        IProfileCollection pc = this.getProfileCollection();
        List<IBorderSegment> segs = pc.getSegments(Tag.ORIENTATION_POINT);
        for (IBorderSegment segment : segs) {
            result.add(segment.getName());
        }
        return result;
    }

    /**
     * Get the differences to the median profile for each nucleus
     * 
     * @param pointType
     *            the point to fetch profiles from
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
            } catch (ProfileException | UnavailableProfileTypeException e) {
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
     * @param pointType
     *            the point to fetch profiles from
     * @return an array of normalised differences
     */
    public double[] getNormalisedDifferencesToMedianFromPoint(BorderTagObject pointType) {
        // List<Double> list = new ArrayList<Double>();
        int count = this.getNucleusCount();
        double[] result = new double[count];
        int i = 0;
        IProfile medianProfile;
        try {
            medianProfile = profileCollection.getProfile(ProfileType.ANGLE, pointType, Stats.MEDIAN);
        } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
            fine("Error getting median profile for collection", e);
            for (int j = 0; i < result.length; j++) {
                result[j] = 0;
            }
            return result;
        }

        for (Nucleus n : this.getNuclei()) {

            IProfile angleProfile;
            try {
                angleProfile = n.getProfile(ProfileType.ANGLE, pointType);
                double diff = angleProfile.absoluteSquareDifference(medianProfile);
                diff /= n.getStatistic(PlottableStatistic.PERIMETER, MeasurementScale.PIXELS); // normalise
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
                                                   // degrees, rather than
                                                   // square degrees
                result[i] = rootDiff;
            } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
                fine("Error getting nucleus profile", e);
                result[i] = 0;
            } finally {
                i++;
            }

        }
        return result;
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

    public double[] getRawValues(PlottableStatistic stat, String component,
            MeasurementScale scale) {

        return getRawValues(stat, component, scale, null);

    }

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

        if (statsCache.hasMedian(stat, component, scale, id)) {

            return statsCache.getMedian(stat, component, scale, id);

        } else {

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
     * Get a list of the given statistic values for each nucleus in the
     * collection
     * 
     * @param stat the statistic to use
     * @param scale the measurement scale
     * @return a list of values
     * @throws Exception
     */
    private double[] getCellStatistics(PlottableStatistic stat, MeasurementScale scale) {

        double[] result = null;

        if (statsCache.hasValues(stat, CellularComponent.WHOLE_CELL, scale, null)) {
            return statsCache.getValues(stat, CellularComponent.WHOLE_CELL, scale, null);
        } else {
            result = cells.parallelStream().mapToDouble(c -> c.getStatistic(stat, scale)).toArray();
            Arrays.sort(result);
            statsCache.setValues(stat, CellularComponent.WHOLE_CELL, scale, null, result);
        }
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

        } else {

            if (PlottableStatistic.VARIABILITY.equals(stat)) {
                result = this.getNormalisedDifferencesToMedianFromPoint(Tag.REFERENCE_POINT);
            } else {

                result = this.getNuclei().parallelStream().mapToDouble(n -> n.getStatistic(stat, scale)).toArray();
            }
            Arrays.sort(result);
            statsCache.setValues(stat, CellularComponent.NUCLEUS, scale, null, result);
        }

        return result;
    }

    /**
     * Calculate the length of the segment with the given name in each nucleus
     * of the collection
     * 
     * @param segName
     *            the segment name
     * @param scale
     *            the scale to use
     * @return a list of segment lengths
     * @throws Exception
     */
    private double[] getSegmentStatistics(PlottableStatistic stat, MeasurementScale scale, UUID id){
    	
    	double[] result = null;
    	if (statsCache.hasValues(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id)) {
    		return statsCache.getValues(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id);

    	} else {
    		AtomicInteger errorCount= new AtomicInteger(0);
    		result = getNuclei().parallelStream().mapToDouble(n -> {
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
    				double fractionOfPerimeter = (double) indexLength / (double) segment.getTotalLength();
    				perimeterLength = fractionOfPerimeter * n.getStatistic(PlottableStatistic.PERIMETER, scale);
    			}
    			return perimeterLength;

    		}).toArray();
    		Arrays.sort(result);
    		statsCache.setValues(stat, CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, id, result);
    		if(errorCount.get()>0)
              warn(String.format("%d nuclei had errors getting segments", errorCount.get()));

    	}
    	return result;
    }

    /*
     * 
     * METHODS IMPLEMENTING THE FILTERABLE INTERFACE
     * 
     */

    @Override
    public ICellCollection filter(Predicate<ICell> predicate) {

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

    public boolean updateSourceFolder(File newFolder) {
        File oldFile = this.getFolder();
        boolean ok = false;
        if (newFolder.exists()) {

            try {
                this.folder = newFolder;

                for (Nucleus n : this.getNuclei()) {
                    n.updateSourceFolder(newFolder);
                }
                ok = true;

            } catch (IllegalArgumentException e) {
                // one of the nuclei failed to update
                // reset all to previous
                warn("At least one nucleus did not update folder location properly");
                this.folder = oldFile;

                for (Nucleus n : this.getNuclei()) {
                    n.updateSourceFolder(oldFile); 
                }
                ok = false;
            }
        }
        return ok;
    }

    /**
     * Test if the collection contains a cell with the same id as the given cell
     * 
     * @param c
     * @return
     */
    public boolean contains(ICell c) {
        return contains(c.getId());
    }

    @Override
    public boolean contains(UUID id) {
        return cells.parallelStream().anyMatch(cell -> cell.getId().equals(id));
    }

    /**
     * Test if the collection contains the given cell (this must be the same
     * object, not just a cell with the same id)
     * 
     * @param c
     * @return
     */
    public boolean containsExact(ICell c) {
        return cells.parallelStream().anyMatch(cell -> cell == c);
    }

    /**
     * Fetch the signal group ids in this collection
     * 
     * @param id
     * @return
     */
    public Set<UUID> getSignalGroupIDs() {

        Set<UUID> ids = new HashSet<UUID>(signalGroups.keySet());
        ids.remove(IShellResult.RANDOM_SIGNAL_ID);
        return ids;
    }

    /**
     * Fetch the signal groups in this collection
     * 
     * @param id
     * @return
     */
    public Collection<ISignalGroup> getSignalGroups() {
        return this.signalGroups.values();
    }

    /**
     * Fetch the signal group with the given ID
     * 
     * @param id
     * @return
     */
    public Optional<ISignalGroup> getSignalGroup(UUID id) {
        return Optional.ofNullable(this.signalGroups.get(id));
    }

    public void addSignalGroup(UUID id, ISignalGroup group) {
        this.signalGroups.put(id, group);
    }

    public boolean hasSignalGroup(UUID id) {
        return this.signalGroups.containsKey(id);
    }

    /**
     * Remove the given group
     * 
     * @param id
     */
    public void removeSignalGroup(UUID id) {
        this.signalGroups.remove(id);
    }

    /**
     * Get the RuleSetCollection with the index finding rules for this nucleus
     * type
     * 
     * @return
     */
    public RuleSetCollection getRuleSetCollection() {
        return this.ruleSets;
    }

    /**
     * Get the number of nuclei shared with the given dataset
     * 
     * @param d2
     * @return
     */
    public synchronized int countShared(IAnalysisDataset d2) {
        return countShared(d2.getCollection());
    }

    /**
     * Get the number of nuclei shared with the given dataset
     * 
     * @param d2
     * @return
     */
    public synchronized int countShared(ICellCollection d2) {

        if (vennCache.hasCount(d2))
            return vennCache.getCount(d2);
        int shared = countSharedNuclei(d2);
        d2.setSharedCount(this, shared);
        vennCache.addCount(d2, shared);

        return shared;

    }

    @Override
    public void setSharedCount(ICellCollection d2, int i) {
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
        isRefolding = false;

        if (ruleSets == null || ruleSets.isEmpty()) {
            log("Creating default ruleset for collection");
            ruleSets = RuleSetCollection.createDefaultRuleSet(nucleusType);
        }

        statsCache = new StatsCache();
        vennCache = new VennCache();

        signalManager = new SignalManager(this);
        profileManager = new ProfileManager(this);

        // Make sure any profile aggregates match the length of saved segments
        this.profileCollection.createAndRestoreProfileAggregate(this);

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
