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
package com.bmskinner.nuclear_morphology.analysis.signals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.VirtualDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.signals.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Method to detect nuclear signals in a dataset
 * @author bms41
 * @since 1.13.4
 *
 */
public class SignalDetectionMethod extends SingleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(SignalDetectionMethod.class.getName());

    protected final HashOptions options;
    protected final File folder;
    protected final int channel;
    protected final UUID signalGroupId;
    protected final String channelName;

    /**
     * For use when running on an existing dataset
     * 
     * @param d the dataset to add signals to
     * @param options the analysis options
     * @param group the signal group to add signals to
     * @throws UnavailableSignalGroupException if the group is not present in the dataset
     */

    public SignalDetectionMethod(@NonNull final IAnalysisDataset d, @NonNull final HashOptions options, @NonNull final UUID group)
            throws UnavailableSignalGroupException {
        super(d);
        
        if(!d.getAnalysisOptions().isPresent())
        	throw new IllegalArgumentException("No analysis options in dataset");
        
        if(!d.getCollection().hasSignalGroup(group))
        	throw new IllegalArgumentException("Signal group is not present in dataset");
        
        if(!options.hasString(HashOptions.SIGNAL_DETECTION_MODE_KEY))
        	throw new IllegalArgumentException("Signal options are not complete");

        this.options = options.duplicate();
        this.folder  = options.getFile(HashOptions.DETECTION_FOLDER).getAbsoluteFile();
        this.channel = options.getInt(HashOptions.CHANNEL);
        this.signalGroupId = group;
        this.channelName = d.getCollection().getSignalGroup(group).get().getGroupName();
    }

    @Override
    public IAnalysisResult call() throws Exception {
        run();
        postDetectionFilter();
        return new DefaultAnalysisResult(dataset);
    }

    protected void run() {

    	LOGGER.fine("Beginning signal detection in channel " + channel);

    	int originalMinThreshold = options.getInt(HashOptions.THRESHOLD);

    	SignalFinder finder = new SignalFinder(dataset.getAnalysisOptions().get(), options, dataset.getCollection());

    	dataset.getCollection().getCells().forEach(c-> detectInCell(c, finder, originalMinThreshold));
    }
    
    
    private void detectInCell(ICell c, SignalFinder finder, int originalMinThreshold){
        // reset the min threshold for each cell
        options.setInt(HashOptions.THRESHOLD, originalMinThreshold);
        
        for(Nucleus n : c.getNuclei())
        	detectInNucleus(n, finder);

        fireProgressEvent();
    }

    
    private void detectInNucleus(Nucleus n, SignalFinder finder){

        LOGGER.finer( "Looking for signals associated with nucleus " + n.getSourceFileName() + "-"
                + n.getNucleusNumber());

        // get the image in the folder with the same name as the
        // nucleus source image
        File imageFile = new File(folder, n.getSourceFileName());
        LOGGER.finer( "Source file: " + imageFile.getAbsolutePath());

        try {

            List<INuclearSignal> signals = finder.findInImage(imageFile, n);

            ISignalCollection signalCollection = n.getSignalCollection();
            signalCollection.addSignalGroup(signals, signalGroupId, imageFile, channel);

            // Measure the detected signals in the nucleus
            SignalAnalyser s = new SignalAnalyser();
            s.calculateSignalDistancesFromCoM(n);
            s.calculateFractionalSignalDistancesFromCoM(n);

            LOGGER.finer("Calculating signal angles");

            if (n.hasLandmark(Landmark.ORIENTATION_POINT)) {
            	n.calculateSignalAnglesFromPoint(n.getBorderPoint(Landmark.ORIENTATION_POINT));
            } else {
            	n.calculateSignalAnglesFromPoint(n.getBorderPoint(Landmark.REFERENCE_POINT));
            }

        } catch (ImageImportException | UnavailableBorderPointException | MissingLandmarkException e) {
            LOGGER.warning("Cannot open " + imageFile.getAbsolutePath());
            LOGGER.log(Loggable.STACK, "Cannot load image", e);
        }
    }
    
    private void postDetectionFilter() {

        List<ICellCollection> signalPopulations = dividePopulationBySignals(dataset.getCollection(), signalGroupId);

        List<IAnalysisDataset> list = new ArrayList<>();

        for (ICellCollection collection : signalPopulations) {
            LOGGER.finer( "Processing " + collection.getName());
            processSubPopulation(collection);
            LOGGER.finer( "Processed " + collection.getName());
            list.add(dataset.getChildDataset(collection.getId()));
        }

        LOGGER.fine("Finished processing sub-populations");
    }

    /**
     * Create child datasets for signal populations and perform basic analyses
     * 
     * @param collection
     */
    private void processSubPopulation(@NonNull ICellCollection collection) {

        try {
            LOGGER.finer( "Creating new analysis dataset for " + collection.getName());

            VirtualDataset subDataset = new VirtualDataset(dataset, collection.getName());
            subDataset.addAll(collection);

            dataset.addChildDataset(subDataset);
            dataset.getCollection().getProfileManager().copySegmentsAndLandmarksTo(subDataset);

        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error processing signal group", e);
        }
    }

    /**
     * Create two child populations for the given dataset: one with signals in
     * the given group, and one without signals
     * 
     * @param r the collection to split
     * @param signalGroup the signal group to split on
     * @return a list of new collections
     */
    private List<ICellCollection> dividePopulationBySignals(@NonNull ICellCollection r, @NonNull UUID signalGroup) {

        List<ICellCollection> signalPopulations = new ArrayList<>();
        LOGGER.fine("Dividing population by signals...");

        Optional<ISignalGroup> og = r.getSignalGroup(signalGroup);
        
        if(!og.isPresent())
        	return signalPopulations;
        ISignalGroup group = og.get();
        
        group.setVisible(true);

        List<ICell> list = r.getSignalManager().getCellsWithNuclearSignals(signalGroup, true);
		
		if (!list.isEmpty()) {
		    LOGGER.fine("Signal group " + group.getGroupName() + ": found nuclei with signals");
		    ICellCollection listCollection = new VirtualDataset(dataset,
		            group.getGroupName() + "_with_signals", UUID.randomUUID());
		    listCollection.addAll(list);
		    signalPopulations.add(listCollection);
		}
        return signalPopulations;
    }

}
