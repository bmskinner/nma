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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.AnalysisDataset;
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
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
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
    private boolean wasConverted = false;

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
     * @param old the old format dataset
     */
    public DatasetConverter(IAnalysisDataset old) {
        this.oldDataset = old;
    }
    
    public boolean shouldSave() {
    	return wasConverted;
    }

    /**
     * Run the converter
     * 
     * @return
     */
    public IAnalysisDataset convert() throws DatasetConversionException {
    	IAnalysisDataset result = oldDataset;
    	
    	// The oldest format did not implement a version field 
    	if(result instanceof AnalysisDataset)
    		result = convertAnalysisDatasetToCurrent(result);
    	
    	// Try to get the old dataset version
    	// After converting an AnalysisDataset, the result version will be current
    	// version, but if the source was older than 1.14.0 we need to adjust the spline fitting
    	Version oldVersion = oldDataset.getVersion();
    	if(oldVersion!=null && oldVersion.isOlderThan(Version.v_1_13_3))
    		result = convert1_13_8To1_14_0(result);
    	
    	// Now the result will in a format with a checkable version
    	if(result.getVersion().isOlderThan(Version.v_1_13_2))
    		result = convert1_13_1To1_13_2(result);
    	if(result.getVersion().isOlderThan(Version.v_1_14_0))
    		result = convert1_13_8To1_14_0(result);
    	return result;
    }
    
    private IAnalysisDataset convert1_13_1To1_13_2(IAnalysisDataset template) throws DatasetConversionException {
    	// Correct signal border locations from older versions for all
    	// imported datasets
        fine("Updating signal locations for pre-1.13.2 dataset");
        updateSignalPositions(template);
        for (IAnalysisDataset child : template.getAllChildDatasets()) {
        	updateSignalPositions(child);
        }

        if (template.hasMergeSources()) {
        	for (IAnalysisDataset source : template.getAllMergeSources()) {
        		updateSignalPositions(source);
        	}
        }
        return template;
    
    }
    
    /**
     * In older versions of the program, signal border positions were stored
     * differently to the CoM. This needs correcting, as it causes errors in
     * rotating signals. The CoM is relative to the nucleus, but the border list
     * is relative to the image. Adjust the border to bring it back in line with
     * the CoM.
     * 
     * @param dataset
     */
    private void updateSignalPositions(IAnalysisDataset dataset) {
        dataset.getCollection().getNuclei().parallelStream().forEach(n -> {

            if (n.getSignalCollection().hasSignal()) {

                for (UUID id : n.getSignalCollection().getSignalGroupIds()) {

                    n.getSignalCollection().getSignals(id).parallelStream().forEach(s -> {

                        if (!s.containsPoint(s.getCentreOfMass())) {

                            for (int i = 0; i < s.getBorderLength(); i++) {
                                try {
                                    s.getBorderPoint(i).offset(-n.getPosition()[0], -n.getPosition()[1]);
                                } catch (UnavailableBorderPointException e) {
                                    stack("Could not offset border point", e);
                                }
                            }
                        }

                    });
                }

            }

        });
    }
    
    
    /**
     * Create a new dataset based on the values in the old dataset. The old
     * classes cannot be used any more, so the resulting dataset will have
     * the current version.
     * @param template
     * @return
     * @throws DatasetConversionException
     */
    private IAnalysisDataset convertAnalysisDatasetToCurrent(IAnalysisDataset template) throws DatasetConversionException {
    	
    	 try {
             log("Old dataset version : " + template.getVersion());
             log("Shiny target version: " + Version.currentVersion());

             backupOldDataset();

             log("Beginning conversion...");

             ICellCollection newCollection = makeNewRootCollection();

             IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection, template.getSavePath());

             if (oldDataset.getAnalysisOptions().isPresent()) {
             	IAnalysisOptions oldOptions = template.getAnalysisOptions().get();
             	IAnalysisOptions newOptions = OptionsFactory.makeAnalysisOptions();

                 IDetectionOptions oldNucleusOptions = oldOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).get();

                 IDetectionOptions nucleusOptions = OptionsFactory.makeNucleusDetectionOptions(oldNucleusOptions);
                 newOptions.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleusOptions);

                 for (UUID id : oldOptions.getNuclearSignalGroups()) {
                     INuclearSignalOptions oldSignalOptions = oldOptions.getNuclearSignalOptions(id);
                     if(template.getCollection().hasSignalGroup(id)){
                     	File folder = oldSignalOptions.getFolder();
                     	int channel = oldSignalOptions.getChannel();

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

             newDataset.setDatasetColour(template.getDatasetColour().orElse(null));

             // arrange root cluster groups
             for (IClusterGroup oldGroup : template.getClusterGroups()) {

                 IClusterGroup newGroup = new ClusterGroup(oldGroup);

                 newDataset.addClusterGroup(newGroup);

             }

             // add the child datasets
             makeVirtualCollections(template, newDataset);

             // Add merge sources
             makeMergeSources(template, newDataset);
             wasConverted = true;
             return newDataset;

         } catch (Exception e) {
             stack("Error converting dataset", e);
             throw new DatasetConversionException(e.getCause());
         }
    	
    }
    
    private IAnalysisDataset convert1_13_8To1_14_0(IAnalysisDataset template)  throws DatasetConversionException {
    	try {
    		
    		for(Nucleus n : template.getCollection().getNuclei()) {
    			n.refreshBorderList(false); // don't use spline fitting, to mimic the original border creation
    			n.calculateProfiles();
    			// At this point, some of the cells may have RPs that are not exactly at segment boundaries
    			// Correct this - find the first index of seg0. Update it to the RP
    			ISegmentedProfile rpProfile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
    			IBorderSegment seg = rpProfile.getSegmentAt(0);    				
    			if(seg.getStartIndex()!=0) {
    				fine("Found mismatched index");
    				seg.update(0, seg.getEndIndex());
    				n.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, rpProfile);
    			}
    		}

    		// Update the collection profiles
    		ICellCollection c = template.getCollection();
    		Method m = c.getProfileCollection().getClass().getDeclaredMethod("createAndRestoreProfileAggregate", ICellCollection.class);
    		m.setAccessible(false);
    		m.invoke(c.getProfileCollection(), c);
    		
    		for(IAnalysisDataset d : template.getAllChildDatasets()) {
    			ICellCollection child = d.getCollection();
        		Method v = child.getProfileCollection().getClass().getDeclaredMethod("createAndRestoreProfileAggregate", ICellCollection.class);
        		v.setAccessible(false);
        		v.invoke(child.getProfileCollection(), child);
    		}
    		
    	} catch (ProfileException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | UnavailableProfileTypeException | UnavailableBorderTagException | SegmentUpdateException e) {
    		stack("Error converting dataset", e);
            throw new DatasetConversionException(e.getCause());
    	}

    	return template;

    }

    private void makeMergeSources(@NonNull IAnalysisDataset template, @NonNull IAnalysisDataset dest) throws DatasetConversionException {
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
    private void makeVirtualCollections(@NonNull IAnalysisDataset template, @NonNull IAnalysisDataset dest)
            throws DatasetConversionException {

        for (IAnalysisDataset child : template.getChildDatasets()) {

            ICellCollection oldCollection = child.getCollection();
            // make a virtual collection for the cells
            ICellCollection newCollection = new VirtualCellCollection(dest, child.getName(), child.getId());

            
            child.getCollection().getCells().forEach(c->newCollection.addCell(c));            

            // Copy segmentation patterns over
            try {
            	newCollection.createProfileCollection();
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

        } catch (ProfileException e) {
            stack("Error updating profiles across datasets", e);
            throw new DatasetConversionException("Profiling error in root dataset");
        }

        for (UUID id : oldCollection.getSignalGroupIDs()) {
            newCollection.addSignalGroup(id, oldCollection.getSignalGroup(id).get());
        }

        return newCollection;

    }

    private ICell createNewCell(@NonNull ICell oldCell) throws DatasetConversionException {
        ICell newCell = new DefaultCell(oldCell.getId());
        Nucleus newNucleus = createNewNucleus(oldCell.getNucleus());
        newCell.setNucleus(newNucleus);
        return newCell;
    }

    private Nucleus createNewNucleus(@NonNull Nucleus n) throws DatasetConversionException {

        NucleusType type = oldDataset.getCollection().getNucleusType();

        switch (type) {
	        case PIG_SPERM:    return makePigNucleus(n);
	        case RODENT_SPERM: return makeRodentNucleus(n);
	        case ROUND:        return makeRoundNucleus(n);
	        default:           return makeRoundNucleus(n);
        }
    }

    private Nucleus makeRoundNucleus(@NonNull Nucleus n) throws DatasetConversionException {

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

    private Nucleus makeRodentNucleus(@NonNull Nucleus n) throws DatasetConversionException {

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

    private Nucleus makePigNucleus(@NonNull Nucleus n) throws DatasetConversionException {

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

    private Nucleus copyGenericData(@NonNull Nucleus template, @NonNull Nucleus newNucleus) throws DatasetConversionException {

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
    private void convertPlottableStatistics(@NonNull Nucleus newNucleus, @NonNull Nucleus template) {

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

    private void convertNuclearSegments(@NonNull Nucleus template, @NonNull Nucleus newNucleus) throws DatasetConversionException {
        // Copy segments from RP
        for (ProfileType type : ProfileType.values()) {

            fine("\nCopying profile type " + type);

            if (template.hasProfile(type)) {
                try {
                    ISegmentedProfile profile = template.getProfile(type, Tag.REFERENCE_POINT);
                    ISegmentedProfile target  = newNucleus.getProfile(type, Tag.REFERENCE_POINT);

                    ISegmentedProfile newProfile;

                    if (profile.size() != target.size()) {
                        newProfile = profile.interpolate(target.size());
                    } else {
                        newProfile = profile.copy();//ISegmentedProfile.makeNew(profile);
                    }

                    if (newProfile.getSegmentCount() != profile.getSegmentCount()) {
                        warn("Segment count mismatch: new has " + newProfile.getSegmentCount() + ", target has "
                                + profile.getSegmentCount());
                        throw new DatasetConversionException( "Error copying segments for nucleus " + template.getNameAndNumber());
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

    private void convertNuclearSignals(@NonNull Nucleus template, @NonNull Nucleus newNucleus) {

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

    private INuclearSignal convertSignal(@NonNull INuclearSignal oldSignal) throws UnavailableBorderPointException {
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
        roi.setLocation(oldSignal.getOriginalBase().getX(), oldSignal.getOriginalBase().getY());

        INuclearSignal newSignal = new DefaultNuclearSignal(roi, oldSignal.getOriginalCentreOfMass(),
                oldSignal.getSourceFile(), oldSignal.getChannel(), oldSignal.getPosition());

        for (PlottableStatistic st : oldSignal.getStatistics()) {

            PlottableStatistic newStat = st;
            if (st instanceof SignalStatistic)
                newStat = SIGNAL_STATS_MAP.get(st);

            newSignal.setStatistic(newStat, oldSignal.getStatistic(st));
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

            File newFile = new File(saveFile.getAbsolutePath());
            if(saveFile.getName().endsWith(SAVE_FILE_EXTENSION))
                newFile = Importer.replaceFileExtension(saveFile, SAVE_FILE_EXTENSION, BAK_FILE_EXTENSION);

            if (newFile.exists()) 
                warn("Overwriting existing backup file");

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
