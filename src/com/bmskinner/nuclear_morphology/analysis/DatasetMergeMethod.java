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
package com.bmskinner.nuclear_morphology.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.io.Io.Importer;

public class DatasetMergeMethod extends MultipleDatasetAnalysisMethod {

    private File saveFile;

    private List<IAnalysisDataset> resultDatasets = new ArrayList<>();

    private Map<UUID, Set<UUID>> pairedSignalGroups = null;

    private static final int MAX_PROGRESS = 100;

    /**
     * Create the merger for the given datasets.
     * 
     * @param datasets the datasets to be merged
     * @param saveFile the file to specify as the new dataset save path. Note, this method
     * does not save out the file to the save path 
     */
    public DatasetMergeMethod(List<IAnalysisDataset> datasets, File saveFile) {
        super(datasets);
        this.saveFile = saveFile;
    }

    public DatasetMergeMethod(List<IAnalysisDataset> datasets, File saveFile, Map<UUID, Set<UUID>> pairedSignalGroups) {
        this(datasets, saveFile);
        this.pairedSignalGroups = pairedSignalGroups;
    }

    @Override
    public IAnalysisResult call() throws Exception {
        run();
        IAnalysisResult r = new DefaultAnalysisResult(resultDatasets);
        return r;
    }

    private void run() {
        try {
            merge();
        } catch (Exception e) {
            error("Error merging datasets", e);
        }
    }

    public List<IAnalysisDataset> getResults() {
        return resultDatasets;
    }

    /**
     * Check if the nucleus classes of all datasets match Cannot merge
     * collections with different classes
     * 
     * @return ok or not
     */
    private boolean checkNucleusClass() {
        boolean result = true;
        NucleusType testClass = datasets.get(0).getCollection().getNucleusType();
        for (IAnalysisDataset d : datasets) {

            if (!d.getCollection().getNucleusType().equals(testClass)) {
                result = false;
            }
        }
        return result;
    }

    private boolean merge() {

        if (datasets.size() <= 1) {
            warn("Must have multiple datasets to merge");
            return false;
        }

        // check we are not merging a parent and child (would just get parent)
        if (datasets.size() == 2) {
            if (datasets.get(0).hasChild(datasets.get(1)) || datasets.get(1).hasChild(datasets.get(0))) {
                warn("Merging parent and child would be silly.");
                return false;
            }
        }

        try {
            fine("Finding new names");

            // Set the names for the new collection
            File newDatasetFolder = saveFile.getParentFile();
            File newDatasetFile = saveFile;

            // ensure the new file name is valid
            newDatasetFile = checkName(newDatasetFile);

            String newDatasetName = newDatasetFile.getName().replace(Importer.SAVE_FILE_EXTENSION, "");
            fine("Checked new file names");

            // check all collections are of the same type
            if (!checkNucleusClass()) {
                warn("Error: cannot merge collections of different class");
                return false;
            }
            fine("Checked nucleus classes match");

            // // make a new collection based on the first dataset
            ICellCollection templateCollection = datasets.get(0).getCollection();

            ICellCollection newCollection = new DefaultCellCollection(newDatasetFolder, null, newDatasetName,
                    templateCollection.getNucleusType());

            IAnalysisDataset newDataset = performMerge(newCollection, datasets);

            resultDatasets.add(newDataset);

            spinWheels();

            return true;
        } catch (Exception e) {
            error("Error in merging", e);
            return false;
        }

    }

    /**
     * Merge the given datasets, copying each cell into the new collection.
     * 
     * @param newCollection
     * @param sources
     * @return
     * @throws Exception
     */
    private IAnalysisDataset performMerge(ICellCollection newCollection, List<IAnalysisDataset> sources)
            throws Exception {

        for (IAnalysisDataset d : datasets) {
            
            d.getCollection().streamCells()
                .filter(c->!newCollection.contains(c))
                .forEach(c->newCollection.addCell(new DefaultCell(c)));

            // All the existing signal groups before merging
            for (UUID signalGroupID : d.getCollection().getSignalGroupIDs()) {
                newCollection.addSignalGroup(signalGroupID,
                        new SignalGroup(d.getCollection().getSignalGroup(signalGroupID).get()));
            }

        }

        // Replace signal groups
        mergeSignalGroups(newCollection);
        
        //TODO update nuclear signal options with new ids

        // create the dataset; has no analysis options at present
        IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection);
        newDataset.setRoot(true);

        // Add the original datasets as merge sources
        for (IAnalysisDataset d : datasets) {

            // Make a new virtual collection for the sources
            newDataset.addMergeSource(d);
        }

        // a merged dataset should not have analysis options
        // of its own; it lets each merge source display options
        // appropriately
        //TODO need to keep the signal folder settings preserved. Copy the analysis options where
        // possible
        newDataset.setAnalysisOptions(null);

        return newDataset;
    }

    private void mergeSignalGroups(ICellCollection newCollection) {
        if (pairedSignalGroups == null) {
            finer("No signal groups to merge");
            return;
        }

        finer("Merging signal groups");

        // Decide which signal groups get which new ids
        // Key is old signal group. Entry is new id
        Map<UUID, UUID> mergedSignalGroups = new HashMap<UUID, UUID>();

        for (UUID id1 : pairedSignalGroups.keySet()) {

            // If this id is not encountered, make a new one
            if (!mergedSignalGroups.keySet().contains(id1)) {
                finest("No merge group, creating");
                mergedSignalGroups.put(id1, UUID.randomUUID());
            }

            UUID newID = mergedSignalGroups.get(id1);

            // All the set share this new id
            Set<UUID> id2Set = pairedSignalGroups.get(id1);
            for (UUID id2 : id2Set) {
                finest("Adding " + id2 + " to " + newID);
                mergedSignalGroups.put(id2, newID);
            }
        }

        // Now, all the old ids have a link to a new id
        // Update the signal groups in the merged dataset

        // Add the old signal groups to the new collection

        finer("Updating signal group ids");
        for (UUID oldID : mergedSignalGroups.keySet()) {

            finer("Old group id for signals  : " + oldID);

            UUID newID = mergedSignalGroups.get(oldID);
            finer("New group id to merge into: " + newID);

            newCollection.getSignalManager().updateSignalGroupID(oldID, newID);
        }

    }

    //
    /**
     * Check if the new dataset filename already exists. If so, append _1 to the
     * end and check again
     * 
     * @param name
     * @return
     */
    private File checkName(File name) {
        String fileName = name.getName();
        String datasetName = fileName.replace(Importer.SAVE_FILE_EXTENSION, "");

        File newFile = new File(name.getParentFile(), datasetName + Importer.SAVE_FILE_EXTENSION);
        if (name.exists()) {
            datasetName += "_1";
            newFile = new File(name.getParentFile(), datasetName + Importer.SAVE_FILE_EXTENSION);
            newFile = checkName(newFile);
        }
        return newFile;
    }

    // Ensure the progress bar does something for debugging
    private void spinWheels() {
        for (int i = 0; i < MAX_PROGRESS; i++) {
            fireProgressEvent();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                error("Thread interrupted", e);
            }
        }
    }

}
