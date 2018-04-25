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


package com.bmskinner.nuclear_morphology.io;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ClusterGroup;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.DefaultCellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.IMutableCell;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.nuclear.DefaultNuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.sperm.DefaultPigSpermNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.sperm.DefaultRodentSpermNucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SegmentStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

/**
 * This class will take old format datasets and convert them to use the newer
 * objects.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DatasetConverter implements Loggable, Importer {

    private IAnalysisDataset oldDataset;

    /**
     * Migration table for stats classes
     */
    protected static final Map<NucleusStatistic, PlottableStatistic> NUCLEUS_STATS_MAP = new HashMap<>();
    protected static final Map<SignalStatistic, PlottableStatistic>  SIGNAL_STATS_MAP  = new HashMap<>();
    protected static final Map<SegmentStatistic, PlottableStatistic> SEGMENT_STATS_MAP = new HashMap<>();
    static {
        NUCLEUS_STATS_MAP.put(NucleusStatistic.AREA, PlottableStatistic.AREA);
        NUCLEUS_STATS_MAP.put(NucleusStatistic.ASPECT, PlottableStatistic.ASPECT);
        NUCLEUS_STATS_MAP.put(NucleusStatistic.BODY_WIDTH, PlottableStatistic.BODY_WIDTH);
        NUCLEUS_STATS_MAP.put(NucleusStatistic.BOUNDING_HEIGHT, PlottableStatistic.BOUNDING_HEIGHT);
        NUCLEUS_STATS_MAP.put(NucleusStatistic.BOUNDING_WIDTH, PlottableStatistic.BOUNDING_WIDTH);
        NUCLEUS_STATS_MAP.put(NucleusStatistic.CIRCULARITY, PlottableStatistic.CIRCULARITY);
        NUCLEUS_STATS_MAP.put(NucleusStatistic.HOOK_LENGTH, PlottableStatistic.HOOK_LENGTH);
        NUCLEUS_STATS_MAP.put(NucleusStatistic.MAX_FERET, PlottableStatistic.MAX_FERET);
        NUCLEUS_STATS_MAP.put(NucleusStatistic.MIN_DIAMETER, PlottableStatistic.MIN_DIAMETER);
        NUCLEUS_STATS_MAP.put(NucleusStatistic.OP_RP_ANGLE, PlottableStatistic.OP_RP_ANGLE);
        NUCLEUS_STATS_MAP.put(NucleusStatistic.PERIMETER, PlottableStatistic.PERIMETER);
        NUCLEUS_STATS_MAP.put(NucleusStatistic.VARIABILITY, PlottableStatistic.VARIABILITY);

        SIGNAL_STATS_MAP.put(SignalStatistic.ANGLE, PlottableStatistic.ANGLE);
        SIGNAL_STATS_MAP.put(SignalStatistic.AREA, PlottableStatistic.AREA);
        SIGNAL_STATS_MAP.put(SignalStatistic.DISTANCE_FROM_COM, PlottableStatistic.DISTANCE_FROM_COM);
        SIGNAL_STATS_MAP.put(SignalStatistic.FRACT_DISTANCE_FROM_COM, PlottableStatistic.FRACT_DISTANCE_FROM_COM);
        SIGNAL_STATS_MAP.put(SignalStatistic.MAX_FERET, PlottableStatistic.MAX_FERET);
        SIGNAL_STATS_MAP.put(SignalStatistic.PERIMETER, PlottableStatistic.PERIMETER);
        SIGNAL_STATS_MAP.put(SignalStatistic.RADIUS, PlottableStatistic.RADIUS);

        SEGMENT_STATS_MAP.put(SegmentStatistic.DISPLACEMENT, PlottableStatistic.DISPLACEMENT);
        SEGMENT_STATS_MAP.put(SegmentStatistic.LENGTH, PlottableStatistic.LENGTH);

    }

    /**
     * Construct using the old dataset version
     * 
     * @param old
     *            the old format dataset
     */
    public DatasetConverter(IAnalysisDataset old) {
        this.oldDataset = old;
    }

    /**
     * Run the converter and make a new DefaultAnalysisDataset from the root,
     * and ChildAnalysisDatasets from children.
     * 
     * @return
     */
    public IAnalysisDataset convert() throws DatasetConversionException {

        try {
            log("Old dataset version : " + oldDataset.getVersion());
            log("Shiny target version: " + Version.currentVersion());

            backupOldDataset();

            log("Beginning conversion...");

            ICellCollection newCollection = makeNewRootCollection();

            IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection, oldDataset.getSavePath());

            if (oldDataset.getAnalysisOptions().isPresent()) {
            	IAnalysisOptions oldOptions = oldDataset.getAnalysisOptions().get();
            	IAnalysisOptions newOptions = OptionsFactory.makeAnalysisOptions();

                IDetectionOptions oldNucleusOptions = oldOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).get();

                IDetectionOptions nucleusOptions = OptionsFactory.makeNucleusDetectionOptions(oldNucleusOptions);
                newOptions.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleusOptions);

                for (UUID id : oldOptions.getNuclearSignalGroups()) {
                    INuclearSignalOptions oldSignalOptions = oldOptions.getNuclearSignalOptions(id);
                    if(oldDataset.getCollection().hasSignalGroup(id)){
                    	File folder = oldDataset.getCollection().getSignalGroup(id).get().getFolder();
                    	int channel = oldDataset.getCollection().getSignalGroup(id).get().getChannel();

                    	INuclearSignalOptions newSignalOptions = OptionsFactory
                    			.makeNuclearSignalOptions(oldSignalOptions);
                    	newSignalOptions.setFolder(folder);
                    	newSignalOptions.setChannel(channel);

                    	newOptions.setDetectionOptions(id.toString(), newSignalOptions);
                    }
                }

                newDataset.setAnalysisOptions(newOptions);
            } else {
                newDataset.setAnalysisOptions(null);
            }

            newDataset.setDatasetColour(oldDataset.getDatasetColour().orElse(null));

            // arrange root cluster groups
            for (IClusterGroup oldGroup : oldDataset.getClusterGroups()) {

                IClusterGroup newGroup = new ClusterGroup(oldGroup);

                newDataset.addClusterGroup(newGroup);

            }

            // add the child datasets
            makeVirtualCollections(oldDataset, newDataset);

            // Add merge sources
            makeMergeSources(oldDataset, newDataset);

            oldDataset = null;
            return newDataset;

        } catch (Exception e) {
            stack("Error converting dataset", e);
            throw new DatasetConversionException(e.getCause());
        }
    }

    private void makeMergeSources(IAnalysisDataset template, IAnalysisDataset dest) throws DatasetConversionException {

        if (template.hasMergeSources()) {

            for (IAnalysisDataset d : template.getMergeSources()) {

                dest.addMergeSource(d);

            }

            log("Added merge sources");
        }

    }

    /**
     * Recursively create cell collections for all child datasets
     * 
     * @param template
     * @param dest
     */
    private void makeVirtualCollections(IAnalysisDataset template, IAnalysisDataset dest)
            throws DatasetConversionException {

        for (IAnalysisDataset child : template.getChildDatasets()) {

            // log("\tConverting: "+child.getName());

            ICellCollection oldCollection = child.getCollection();
            // make a virtual collection for the cells
            ICellCollection newCollection = new VirtualCellCollection(dest, child.getName(), child.getUUID());

            
            child.getCollection().getCells().forEach(c->newCollection.addCell(c));
//            for (ICell c : child.getCollection().getCells()) {
//                newCollection.addCell(c);
//            }

            newCollection.createProfileCollection();

            // Copy segmentation patterns over
            try {
                oldCollection.getProfileManager().copyCollectionOffsets(newCollection);
            } catch (ProfileException e) {
                warn("Unable to copy profile offsets");
                stack("Unable to copy collection offsets", e);
            }

            dest.addChildCollection(newCollection);

            IAnalysisDataset destChild = dest.getChildDataset(newCollection.getID());

            // log("\tMaking clusters: "+child.getName());
            for (IClusterGroup oldGroup : child.getClusterGroups()) {

                IClusterGroup newGroup = new ClusterGroup(oldGroup);

                destChild.addClusterGroup(newGroup);

            }

            // Recurse until complete
            makeVirtualCollections(child, dest.getChildDataset(newCollection.getID()));

        }
    }

    /**
     * Copy the cells and signal groups from the old collection
     * 
     * @return
     * @throws DatasetConversionException
     */
    private ICellCollection makeNewRootCollection() throws DatasetConversionException {
        fine("Converting root: " + oldDataset.getName());
        ICellCollection oldCollection = oldDataset.getCollection();

        ICellCollection newCollection = new DefaultCellCollection(oldCollection.getFolder(),
                oldCollection.getOutputFolderName(), oldCollection.getName(), oldCollection.getNucleusType());

                
        for (ICell c : oldCollection.getCells()) {
            newCollection.addCell(createNewCell(c));
        }

        try {

            // Make new profile aggregates
            newCollection.createProfileCollection();

            // Copy segmentation patterns over
            oldCollection.getProfileManager().copyCollectionOffsets(newCollection);

            // copyCollectionOffsets(oldCollection, newCollection);
        } catch (ProfileException e) {
            stack("Error updating profiles across datasets", e);
            throw new DatasetConversionException("Profiling error in root dataset");
        }

        for (UUID id : oldCollection.getSignalGroupIDs()) {
            newCollection.addSignalGroup(id, oldCollection.getSignalGroup(id).get());
        }

        return newCollection;

    }

    // /**
    // * Copy profile offsets from the this collection, to the
    // * destination and build the median profiles for all profile types.
    // * Also copy the segments from the regular angle profile onto
    // * all other profile types
    // * @param destination the collection to update
    // * @throws Exception
    // */
    // public void copyCollectionOffsets(final ICellCollection source, final
    // ICellCollection destination) throws ProfileException {
    //
    // if(source instanceof CellCollection){
    //
    // CellCollection sourceCollection = (CellCollection) source;
    //
    // /*
    // * Get the corresponding profile collection from the tempalte
    // */
    // ProfileCollection sourcePC = (ProfileCollection)
    // sourceCollection.getProfileCollection(type);
    //
    // List<IBorderSegment> segments;
    //
    // segments = sourcePC.getSegments(Tag.REFERENCE_POINT);
    //
    // fine("Got existing list of "+segments.size()+" segments");
    //
    // // use the same array length as the source collection to avoid segment
    // slippage
    // int profileLength = sourcePC.length();
    //
    //
    // /*
    // * Get the empty profile collection from the new ICellCollection
    // * This has a ProfileCollection containing a map of aggregates for each
    // profile type
    // */
    // IProfileCollection destPC = destination.getProfileCollection();
    //
    //
    //
    // /*
    // * Create an aggregate from the nuclei in the collection.
    // * This will have the length of the source collection.
    // */
    // destPC.createProfileAggregate(destination, profileLength);
    // fine("Created new profile aggregates with length "+profileLength);
    //
    // /*
    // * Copy the offset keys from the source collection
    // */
    // try {
    // for(Tag key : sourcePC.getBorderTags()){
    //
    // destPC.addIndex(key, sourcePC.getIndex(key));
    //
    // }
    //
    // destPC.addSegments(Tag.REFERENCE_POINT, segments);
    //
    // } catch (UnavailableBorderTagException | IllegalArgumentException e) {
    // warn("Cannot add segments to RP: "+e.getMessage());
    // fine("Cannot add segments to RP", e);
    // }
    // fine("Copied tags to new collection");
    //
    //
    // } else {
    // source.getProfileManager().copyCollectionOffsets(destination);
    // }
    //
    //
    //
    // }

    private ICell createNewCell(ICell oldCell) throws DatasetConversionException {
        IMutableCell newCell = new DefaultCell(oldCell.getId());

        // make a new nucleus
        Nucleus newNucleus = createNewNucleus(oldCell.getNucleus());

        newCell.setNucleus(newNucleus);

        return newCell;

    }

    private Nucleus createNewNucleus(Nucleus n) throws DatasetConversionException {

        NucleusType type = oldDataset.getCollection().getNucleusType();

        fine("\tCreating nucleus: " + n.getNameAndNumber() + "\t" + type);

        Nucleus newNucleus;

        switch (type) {
        case PIG_SPERM:
            newNucleus = makePigNucleus(n);
            break;
        case RODENT_SPERM:
            newNucleus = makeRodentNucleus(n);
            break;
        case ROUND:
            newNucleus = makeRoundNucleus(n);
            break;
        default:
            newNucleus = makeRoundNucleus(n);
            break;

        }

        return newNucleus;

    }

    private Nucleus makeRoundNucleus(Nucleus n) throws DatasetConversionException {

        // Easy stuff
        File f = n.getSourceFile(); // the source file
        int channel = n.getChannel();// the detection channel
        int number = n.getNucleusNumber(); // copy over
        IPoint com = n.getOriginalCentreOfMass();

        // Position converted down internally
        int[] position = n.getPosition();

        // Get the roi for the old nucleus
        float[] xpoints = new float[n.getBorderLength()], ypoints = new float[n.getBorderLength()];
        try {
            for (int i = 0; i < xpoints.length; i++) {
                xpoints[i] = (float) n.getOriginalBorderPoint(i).getX();
                ypoints[i] = (float) n.getOriginalBorderPoint(i).getY();
            }

        } catch (UnavailableBorderPointException e) {
            stack("Unable to get border point", e);
            throw new DatasetConversionException("Unable to get border point");
        }

        PolygonRoi roi = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.TRACED_ROI);

        // Use the default constructor
        Nucleus newNucleus = new DefaultNucleus(roi, com, f, channel, position, number);

        newNucleus = copyGenericData(n, newNucleus);
        newNucleus.moveCentreOfMass(n.getCentreOfMass());
        return newNucleus;

    }

    private Nucleus makeRodentNucleus(Nucleus n) throws DatasetConversionException {

        // Easy stuff
        File f = n.getSourceFile(); // the source file
        int channel = n.getChannel();// the detection channel
        int number = n.getNucleusNumber(); // copy over
        IPoint com = n.getOriginalCentreOfMass();
        int[] position = n.getPosition();

        // Get the roi for the old nucleus
        float[] xpoints = new float[n.getBorderLength()], ypoints = new float[n.getBorderLength()];
        try {
            for (int i = 0; i < xpoints.length; i++) {
                xpoints[i] = (float) n.getOriginalBorderPoint(i).getX();
                ypoints[i] = (float) n.getOriginalBorderPoint(i).getY();
            }
        } catch (UnavailableBorderPointException e) {
            stack("Unable to get border point", e);
            throw new DatasetConversionException("Unable to get border point");
        }
        PolygonRoi roi = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.TRACED_ROI);

        if (!roi.contains(n.getOriginalCentreOfMass().getXAsInt(), n.getOriginalCentreOfMass().getYAsInt())) {
            warn("Updating roi location");
            // Set the position of the top left corner of the ROI
            roi.setLocation(n.getPosition()[CellularComponent.X_BASE], n.getPosition()[CellularComponent.Y_BASE]);
        }

        // log(n.getNameAndNumber()+": Created roi at "+roi.getBounds());
        // Use the default constructor

        try {

            // log(n.getNameAndNumber()+": Creating nucleus at original position
            // "+xpoints[0]+", "+ypoints[0]);
            Nucleus newNucleus = new DefaultRodentSpermNucleus(roi, com, f, channel, position, number);

            newNucleus = copyGenericData(n, newNucleus);
            newNucleus.moveCentreOfMass(n.getCentreOfMass());
            return newNucleus;
        } catch (IllegalArgumentException e) {
            stack("Error making nucleus", e);
            throw new DatasetConversionException("Cannot create nucleus from input data", e);
        }
    }

    private Nucleus makePigNucleus(Nucleus n) throws DatasetConversionException {

        // Easy stuff
        File f = n.getSourceFile(); // the source file
        int channel = n.getChannel();// the detection channel
        int number = n.getNucleusNumber(); // copy over
        IPoint com = n.getOriginalCentreOfMass();

        // Position converted down internally
        int[] position = n.getPosition();

        // Get the roi for the old nucleus
        float[] xpoints = new float[n.getBorderLength()], ypoints = new float[n.getBorderLength()];
        try {
            for (int i = 0; i < xpoints.length; i++) {
                xpoints[i] = (float) n.getOriginalBorderPoint(i).getX();
                ypoints[i] = (float) n.getOriginalBorderPoint(i).getY();
            }
        } catch (UnavailableBorderPointException e) {
            stack("Unable to get border point", e);
            throw new DatasetConversionException("Unable to get border point");
        }

        PolygonRoi roi = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.TRACED_ROI);

        // Use the default constructor
        Nucleus newNucleus = new DefaultPigSpermNucleus(roi, com, f, channel, position, number);

        newNucleus = copyGenericData(n, newNucleus);

        newNucleus.moveCentreOfMass(n.getCentreOfMass());

        return newNucleus;

    }

    private Nucleus copyGenericData(Nucleus template, Nucleus newNucleus) throws DatasetConversionException {

        // The nucleus ID is created with the nucleus and is not accessible
        // Use reflection to get access and set the new id to the same as the
        // template

        try {
            // fine("Created nucleus id is "+newNucleus.getID());
            Class<DefaultCellularComponent> superClass = DefaultCellularComponent.class;
            Field field = superClass.getDeclaredField("id");
            field.setAccessible(true);
            field.set(newNucleus, template.getID());
            field.setAccessible(false);

        } catch (NoSuchFieldException e) {
            stack("No field", e);
            throw new DatasetConversionException("Cannot set ID", e);
        } catch (SecurityException e) {
            stack("Security error", e);
            throw new DatasetConversionException("Cannot set ID", e);
        } catch (IllegalArgumentException e) {
            stack("Illegal argument", e);
            throw new DatasetConversionException("Cannot set ID", e);
        } catch (IllegalAccessException e) {
            stack("Illegal access", e);
            throw new DatasetConversionException("Cannot set ID", e);
        } catch (Exception e) {
            stack("Unexpected exception", e);
            throw new DatasetConversionException("Cannot set ID", e);
        }

        // fine("New nucleus id is "+newNucleus.getID());

        finer("\tCreated nucleus object");

        finer("Converting stats");
        convertPlottableStatistics(newNucleus, template);

        newNucleus.setScale(template.getScale());

        // Create the profiles within the nucleus
        finer("\tInitialising");
        try {
            newNucleus.initialise(template.getWindowProportion(ProfileType.ANGLE));

            fine("\tCopying tags");

            // The keyset of the map will not have a defined order, so do the RP
            // first now
            // and skip it in the loop below
            template.getBorderPoint(Tag.REFERENCE_POINT);

            // get the proporitonal index of the old tag
            double propIndex = (double) template.getBorderIndex(Tag.REFERENCE_POINT)
                    / (double) template.getBorderLength();

            int newIndex = (int) ((double) newNucleus.getBorderLength() * propIndex);

            fine("\tChanging tag " + Tag.REFERENCE_POINT + " to index " + newIndex + " : " + propIndex);
            newNucleus.setBorderTag(Tag.REFERENCE_POINT, newIndex);
        } catch (UnavailableBorderTagException | IndexOutOfBoundsException | ComponentCreationException e) {
            stack("Cannot initialise or cannot set border tag to requested index", e);
            throw new DatasetConversionException("Cannot create profilable object", e);
        }

        // Copy the other existing border tags
        for (Tag t : template.getBorderTags().keySet()) {

            if (t.equals(Tag.INTERSECTION_POINT) || t.equals(Tag.REFERENCE_POINT)) {
                continue;
            }

            try {

                template.getBorderPoint(t);

                // get the proporitonal index of the old tag

                double propIndex = (double) template.getBorderIndex(t) / (double) template.getBorderLength();

                int newIndex = (int) ((double) newNucleus.getBorderLength() * propIndex);

                fine("\tChanging tag " + t + " to index " + newIndex + " : " + propIndex);
                newNucleus.setBorderTag(t, newIndex);
            } catch (UnavailableBorderTagException | IndexOutOfBoundsException e) {
                stack("Cannot set border tag to requested index", e);
            }
            finer("\tSetting tag " + t);
        }

        // Copy segments from RP
        convertNuclearSegments(template, newNucleus);

        convertNuclearSignals(template, newNucleus);

        fine("Created nucleus " + newNucleus.getNameAndNumber() + "\n");

        return newNucleus;
    }

    /**
     * Update all stats to the latest versions
     * 
     * @param newNucleus
     * @param template
     */
    private void convertPlottableStatistics(Nucleus newNucleus, Nucleus template) {

        for (PlottableStatistic stat : template.getStatistics()) {
            try {
                PlottableStatistic newStat = stat;

                if (stat instanceof NucleusStatistic) {
                    newStat = NUCLEUS_STATS_MAP.get(stat);
                }

                if (stat instanceof SignalStatistic) {
                    newStat = SIGNAL_STATS_MAP.get(stat);
                }

                if (stat instanceof SegmentStatistic) {
                    newStat = SEGMENT_STATS_MAP.get(stat);
                }

                newNucleus.setStatistic(newStat, template.getStatistic(stat, MeasurementScale.PIXELS));
            } catch (Exception e) {
                fine("Error setting statistic: " + stat, e);
                newNucleus.setStatistic(stat, 0);
            }
        }
    }

    private void convertNuclearSegments(Nucleus template, Nucleus newNucleus) throws DatasetConversionException {
        // Copy segments from RP
        for (ProfileType type : ProfileType.values()) {

            fine("\nCopying profile type " + type);

            if (template.hasProfile(type)) {
                try {
                    ISegmentedProfile profile = template.getProfile(type, Tag.REFERENCE_POINT);
                    ISegmentedProfile target = newNucleus.getProfile(type, Tag.REFERENCE_POINT);

                    ISegmentedProfile newProfile;

                    if (profile.size() != target.size()) {
                        // log("Interpolating profile");
                        // fine("\tNew nucleus profile length of
                        // "+target.size()+" : original nucleus was
                        // "+profile.size());
                        newProfile = profile.interpolate(target.size());
                        // fine("\tInterpolated profile has length
                        // "+target.size()+" with segment total length
                        // "+target.getSegments().get(0).getTotalLength());
                    } else {
                        // log("Not interpolating profile");
                        newProfile = ISegmentedProfile.makeNew(profile);
                    }

                    if (newProfile.getSegmentCount() != profile.getSegmentCount()) {
                        warn("Segment count mismatch: new has " + newProfile.getSegmentCount() + ", target has "
                                + profile.getSegmentCount());
                        throw new DatasetConversionException(
                                "Error copying segments for nucleus " + template.getNameAndNumber());
                    }

                    fine("\tSetting the profile " + type + " in the new nucleus");
                    newNucleus.setProfile(type, Tag.REFERENCE_POINT, newProfile);

                } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e1) {
                    stack("Error getting profile from template or target nucleus", e1);
                    throw new DatasetConversionException("Cannot convert nucleus", e1);
                }
            }

            fine("Complete profile type " + type);
        }
    }

    private void convertNuclearSignals(Nucleus template, Nucleus newNucleus) {

        // Copy signals

        fine("Copying signals for " + template.getNameAndNumber());

        for (UUID signalGroup : template.getSignalCollection().getSignalGroupIds()) {

            for (INuclearSignal s : template.getSignalCollection().getSignals(signalGroup)) {

                try {
                    INuclearSignal newSignal = convertSignal(s);
                    newNucleus.getSignalCollection().addSignal(newSignal, signalGroup);
                } catch (UnavailableBorderPointException e) {
                    warn("Could not convert signal " + s.toString());
                    stack("Unable to get border point", e);
                }

            }

        }
    }

    private INuclearSignal convertSignal(INuclearSignal oldSignal) throws UnavailableBorderPointException {
        // Get the roi for the old signal
        float[] xpoints = new float[oldSignal.getBorderLength()], ypoints = new float[oldSignal.getBorderLength()];

        for (int i = 0; i < xpoints.length; i++) {
            xpoints[i] = (float) oldSignal.getOriginalBorderPoint(i).getX();
            ypoints[i] = (float) oldSignal.getOriginalBorderPoint(i).getY();
        }

        // Create an roi from the old signal

        PolygonRoi roi = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.TRACED_ROI);

        // Move the roi over the original centre of mass in case it is not
        // already there
        roi.setLocation(oldSignal.getPosition()[CellularComponent.X_BASE],
                oldSignal.getPosition()[CellularComponent.Y_BASE]);

        INuclearSignal newSignal = new DefaultNuclearSignal(roi, oldSignal.getOriginalCentreOfMass(),
                oldSignal.getSourceFile(), oldSignal.getChannel(), oldSignal.getPosition());

        for (PlottableStatistic st : oldSignal.getStatistics()) {

            PlottableStatistic newStat = st;
            if (st instanceof SignalStatistic) {
                newStat = SIGNAL_STATS_MAP.get(st);
            }
            newSignal.setStatistic(newStat, oldSignal.getStatistic(st));
            ;
        }
        return newSignal;
    }

    /**
     * Save a copy of the old dataset by renaming the nmd file to a backup file.
     * 
     * @throws DatasetConversionException
     */
    private void backupOldDataset() throws DatasetConversionException {

        File saveFile = oldDataset.getSavePath();

        if (saveFile.exists()) {

            File newFile = Importer.replaceFileExtension(saveFile, SAVE_FILE_EXTENSION, BAK_FILE_EXTENSION);

            if (newFile.exists()) {

                warn("Overwriting existing backup file");

            }

            try {

                Files.copy(saveFile.toPath(), newFile.toPath(), REPLACE_EXISTING);

                log("Backup file created OK");
            } catch (IOException e) {
                stack("Error copying file", e);
                throw new DatasetConversionException("Unable to make backup file");
            }

        }

    }

    public class DatasetConversionException extends Exception {
        private static final long serialVersionUID = 1L;

        public DatasetConversionException() {
            super();
        }

        public DatasetConversionException(String message) {
            super(message);
        }

        public DatasetConversionException(String message, Throwable cause) {
            super(message, cause);
        }

        public DatasetConversionException(Throwable cause) {
            super(cause);
        }
    }

}
