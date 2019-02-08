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
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.ChildAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

/**
 * Method to detect nuclear signals in a dataset
 * @author bms41
 * @since 1.13.4
 *
 */
public class SignalDetectionMethod extends SingleDatasetAnalysisMethod {

    protected final INuclearSignalOptions options;
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

    public SignalDetectionMethod(@NonNull final IAnalysisDataset d, @NonNull final INuclearSignalOptions options, @NonNull final UUID group)
            throws UnavailableSignalGroupException {
        super(d);
        
        if(!d.getAnalysisOptions().isPresent())
        	throw new IllegalArgumentException("No analysis options in dataset");
        
        if(!d.getCollection().hasSignalGroup(group))
        	throw new IllegalArgumentException("Signal group is not present in dataset");

        this.options = (INuclearSignalOptions) options.duplicate();
        this.folder  = options.getFolder().getAbsoluteFile();
        this.channel = options.getChannel();
        this.signalGroupId = group;
        this.channelName = d.getCollection().getSignalGroup(group).get().getGroupName();
    }

    @Override
    public IAnalysisResult call() throws Exception {

        run();
        postDetectionFilter();
        IAnalysisResult r = new DefaultAnalysisResult(dataset);

        return r;
    }

    protected void run() throws Exception {

        fine("Beginning signal detection in channel " + channel);

        try {

            int originalMinThreshold = options.getThreshold();

            SignalFinder finder = new SignalFinder(dataset.getAnalysisOptions().get(), options, dataset.getCollection());

            dataset.getCollection().getCells().forEach(c->{
                detectInCell(c, finder, originalMinThreshold);
            });

        } catch (Exception e) {
            stack("Error in signal detection", e);
        }
    }
    
    
    private void detectInCell(ICell c, SignalFinder finder, int originalMinThreshold){
        // reset the min threshold for each cell
        options.setThreshold(originalMinThreshold);
        
        for(Nucleus n : c.getNuclei())
        	detectInNucleus(n, finder);

        fireProgressEvent();
    }

    
    private void detectInNucleus(Nucleus n, SignalFinder finder){

        finer("Looking for signals associated with nucleus " + n.getSourceFileName() + "-"
                + n.getNucleusNumber());

        // get the image in the folder with the same name as the
        // nucleus source image
        File imageFile = new File(folder, n.getSourceFileName());
        finer("Source file: " + imageFile.getAbsolutePath());

        try {

            List<INuclearSignal> signals = finder.findInImage(imageFile, n);

            ISignalCollection signalCollection = n.getSignalCollection();
            signalCollection.addSignalGroup(signals, signalGroupId, imageFile, channel);

            // Measure the detected signals in the nucleus
            SignalAnalyser s = new SignalAnalyser();
            s.calculateSignalDistancesFromCoM(n);
            s.calculateFractionalSignalDistancesFromCoM(n);

            fine("Calculating signal angles");

            // If the nucleus is asymmetric, calculate angles
            if (!dataset.getCollection().getNucleusType().equals(NucleusType.ROUND)) {
                if (n.hasBorderTag(Tag.ORIENTATION_POINT)) {
                    finest("Calculating angle from orientation point");
                    n.calculateSignalAnglesFromPoint(n.getBorderPoint(Tag.ORIENTATION_POINT));
                } else {
                    finest("No orientation point in nucleus");
                }
            }

        } catch (ImageImportException | UnavailableBorderPointException | UnavailableBorderTagException e) {
            warn("Cannot open " + imageFile.getAbsolutePath());
            stack("Cannot load image", e);
        }
    }
    
    private void postDetectionFilter() {

        List<ICellCollection> signalPopulations = dividePopulationBySignals(dataset.getCollection(), signalGroupId);

        List<IAnalysisDataset> list = new ArrayList<>();

        for (ICellCollection collection : signalPopulations) {
            finer("Processing " + collection.getName());
            processSubPopulation(collection);
            finer("Processed " + collection.getName());
            list.add(dataset.getChildDataset(collection.getID()));
        }

        fine("Finished processing sub-populations");
    }

    /**
     * Create child datasets for signal populations and perform basic analyses
     * 
     * @param collection
     */
    private void processSubPopulation(@NonNull ICellCollection collection) {

        try {
            finer("Creating new analysis dataset for " + collection.getName());

            IAnalysisDataset subDataset = new ChildAnalysisDataset(dataset, collection);

            dataset.addChildDataset(subDataset);
            dataset.getCollection().getProfileManager().copyCollectionOffsets(collection);

        } catch (Exception e) {
            error("Error processing signal group", e);
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
        fine("Dividing population by signals...");

        Optional<ISignalGroup> og = r.getSignalGroup(signalGroup);
        
        if(!og.isPresent())
        	return signalPopulations;
        ISignalGroup group = og.get();
        
        group.setVisible(true);

		Set<ICell> list = r.getSignalManager().getCellsWithNuclearSignals(signalGroup, true);
		
		if (!list.isEmpty()) {
		    fine("Signal group " + group.getGroupName() + ": found nuclei with signals");
		    ICellCollection listCollection = new VirtualCellCollection(dataset,
		            group.getGroupName() + "_with_signals");

		    for (ICell c : list) {
		        listCollection.addCell(c);
		    }
		    signalPopulations.add(listCollection);

		    // Only add a group of cells without signals if at least one
		    // cell does have a signal

//		    Set<ICell> notList = r.getSignalManager().getCellsWithNuclearSignals(signalGroup, false);
//		    if (!notList.isEmpty()) {
//		        log("Signal group " + group.getGroupName()
//		                + ": found nuclei without signals");
//
//		        ICellCollection notListCollection = new VirtualCellCollection(dataset,
//		                group.getGroupName() + "_without_signals");
//
//		        for (ICell c : notList) {
//		            notListCollection.addCell(c);
//		        }
//
//		        signalPopulations.add(notListCollection);
//		    }

		}
        return signalPopulations;
    }

}
