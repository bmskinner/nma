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


package com.bmskinner.nuclear_morphology.components;

import java.awt.Paint;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This describes an analysis dataset, which packages a collection of cells with
 * clusters, merge sources, and the options used for the detection of the cells.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IAnalysisDataset extends Serializable, Loggable {

    /**
     * Get the ID of the dataset
     * @return
     */
    UUID getId();
	
    /**
     * Make a copy of the cells in this dataset. Does not yet include child
     * datasets, clusters or signal groups
     * 
     * @return
     * @throws Exception
     */
    IAnalysisDataset duplicate() throws Exception;

    /**
     * Get the software version used to create the dataset
     * 
     * @return
     */
    Version getVersion();

    /**
     * Add the given cell collection as a child to this dataset. A new dataset
     * is contructed to hold it.
     * 
     * @param collection
     *            the collection to add
     */
    void addChildCollection(@NonNull ICellCollection collection);

    /**
     * Add the given dataset as a child of this dataset
     * 
     * @param dataset
     */
    void addChildDataset(@NonNull IAnalysisDataset dataset);

    /**
     * Get the name of the dataset. Passes through to CellCollection
     * 
     * @return
     * @see CellCollection
     */
    String getName();

    /**
     * Set the name of the dataset. Passes through to the CellCollection
     * 
     * @param s
     * @see CellCollection
     */
    void setName(@NonNull String s);

    /**
     * Get the save file location
     * 
     * @return
     */
    File getSavePath();

    /**
     * Set the path to save the dataset
     * 
     * @param file
     */
    void setSavePath(@NonNull File file);


    /**
     * Get all the direct children of this dataset
     * 
     * @return
     */
    Set<UUID> getChildUUIDs();

    /**
     * Recursive version of getChildUUIDs. Get the children of this dataset, and
     * all their children
     * 
     * @return
     */
    Set<UUID> getAllChildUUIDs();

    /**
     * Get the specificed child
     * 
     * @param id
     *            the child UUID
     * @return
     */
    IAnalysisDataset getChildDataset(@NonNull UUID id);

    /**
     * Get the AnalysisDataset with the given id that is a merge source to this
     * dataset.
     * 
     * @param id
     *            the UUID of the dataset
     * @return the dataset or null
     */
    IAnalysisDataset getMergeSource(@NonNull UUID id);

    /**
     * Recursively fetch all the merge sources for this dataset. Only includes
     * the root sources (not intermediate merges)
     * 
     * @return
     */
    Set<IAnalysisDataset> getAllMergeSources();

    /**
     * Add the given dataset as a merge source
     * 
     * @param dataset
     */
    void addMergeSource(@NonNull IAnalysisDataset dataset);

    /**
     * Get all datasets considered direct merge sources to this dataset
     * 
     * @return
     */
    Set<IAnalysisDataset> getMergeSources();

    /**
     * Get the ids of all datasets considered merge sources to this dataset
     * 
     * @return
     */
    Set<UUID> getMergeSourceIDs();

    /**
     * Get the ids of all datasets considered merge sources to this dataset,
     * recursively (that is, if the merge source is a merge, get the sources of
     * that merge)
     * 
     * @return
     */
    Set<UUID> getAllMergeSourceIDs();

    /**
     * Test if a dataset with the given id is present as a merge source
     * 
     * @param id
     *            the UUID to test
     * @return
     */
    boolean hasMergeSource(UUID id);

    /**
     * Test if a dataset is present as a merge source
     * 
     * @param dataset
     *            the dataset to test
     * @return
     */
    boolean hasMergeSource(IAnalysisDataset dataset);

    /**
     * Test if the dataset has merge sources
     * 
     * @return
     */
    boolean hasMergeSources();

    /**
     * Get the number of direct children of this dataset
     * 
     * @return
     */
    int getChildCount();

    /**
     * Check if the dataset has children
     * 
     * @return
     */
    boolean hasChildren();

    /**
     * Get all the direct children of this dataset
     * 
     * @return
     */
    Collection<IAnalysisDataset> getChildDatasets();

    /**
     * Recursive version of get child datasets Get all the direct children of
     * this dataset, and all their children.
     * 
     * @return
     */
    List<IAnalysisDataset> getAllChildDatasets();

    /**
     * Get the collection in this dataset
     * 
     * @return
     */
    ICellCollection getCollection();

    /**
     * Get the analysis options from this dataset
     * 
     * @return
     */
    Optional<IAnalysisOptions> getAnalysisOptions();

    /**
     * Test if the dataset has analysis options set. This is not the case for
     * (for example) merge sources
     * 
     * @return
     */
    boolean hasAnalysisOptions();

    /**
     * Set the analysis options for the dataset
     * 
     * @param analysisOptions
     */
    void setAnalysisOptions(IAnalysisOptions analysisOptions);

    /**
     * Add the given dataset as a cluster result. This is a form of child
     * dataset
     * 
     * @param dataset
     */
    void addClusterGroup(IClusterGroup group);

    /**
     * Check the list of cluster groups, and return the highest cluster group
     * number present
     * 
     * @return
     */
    int getMaxClusterGroupNumber();

    /**
     * Check if the dataset id is in a cluster
     * 
     * @param id
     * @return
     */
    boolean hasCluster(UUID id);

    List<IClusterGroup> getClusterGroups();

    /**
     * Get the UUIDs of all datasets in clusters
     * 
     * @return
     */
    List<UUID> getClusterIDs();

    /**
     * Check if the dataset has clusters
     * 
     * @return
     */
    boolean hasClusters();

    /**
     * Test if the given group is present in this dataset
     * 
     * @param group
     * @return
     */
    boolean hasClusterGroup(IClusterGroup group);

    /**
     * Check that all cluster groups have child members present; if cluster
     * groups do not have children, remove the group
     */
    void refreshClusterGroups();

    /**
     * Check if the dataset is root
     * 
     * @return
     */
    boolean isRoot();

    /**
     * Set the dataset root status
     * 
     * @param b
     *            is the dataset root
     */
    void setRoot(boolean b);

    /**
     * Delete the child AnalysisDataset specified
     * 
     * @param id
     *            the UUID of the child to delete
     */
    void deleteChild(@NonNull UUID id);

    /**
     * Delete the cluster with the given id
     * 
     * @param id
     */
    void deleteClusterGroup(IClusterGroup group);

    /**
     * Delete an associated dataset
     * 
     * @param id
     */
    void deleteMergeSource(@NonNull UUID id);

    /**
     * Check if the given dataset is a child dataset of this
     * 
     * @param child
     *            the dataset to test
     * @return
     */
    boolean hasChild(IAnalysisDataset child);

    /**
     * Test if the given dataset is a child of this dataset or of one of its
     * children
     * 
     * @param child
     * @return
     */
    boolean hasRecursiveChild(IAnalysisDataset child);

    /**
     * Check if the given dataset is a child dataset of this
     * 
     * @param child
     * @return
     */
    boolean hasChild(UUID child);

    /**
     * Set the dataset colour (used in comparisons between datasets)
     * 
     * @param colour
     *            the new colour
     */
    void setDatasetColour(Paint colour);

    /**
     * Get the currently set dataset colour, or null if not set
     * 
     * @return colour or null
     */
    Optional<Paint> getDatasetColour();

    /**
     * Test if the dataset colour is set or null
     * 
     * @return
     */
    boolean hasDatasetColour();


    /**
     * Update the source image paths in the dataset and its children to use the
     * given directory
     * 
     * @param expectedImageDirectory
     * @param dataset
     * @throws Exception
     */
    void updateSourceImageDirectory(@NonNull File expectedImageDirectory);

    /**
     * Test if all the datasets in the list have a consensus nucleus
     * 
     * @param list
     * @return
     */
    static boolean haveConsensusNuclei(@NonNull List<IAnalysisDataset> list) {
        for (IAnalysisDataset d : list) {
            if (!d.getCollection().hasConsensus()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if all the datasets have the same type of nucleus
     * 
     * @param list
     * @return
     */
    static boolean areSameNucleusType(@NonNull List<IAnalysisDataset> list) {

        NucleusType type = list.get(0).getCollection().getNucleusType();

        for (IAnalysisDataset d : list) {
            NucleusType next = d.getCollection().getNucleusType();
            if (!next.equals(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if the merge sources of a dataset have the same analysis options
     * TODO: make recursive; what happens when two merged datsets are merged?
     * 
     * @param dataset
     * @return the common options, or null if an options is different
     */
    static boolean mergedSourceOptionsAreSame(@NonNull IAnalysisDataset dataset) {

        Set<IAnalysisDataset> list = dataset.getMergeSources();

        boolean ok = true;

        for (IAnalysisDataset d1 : list) {

            /*
             * If the dataset has merge sources, the options are null In this
             * case, recursively go through the dataset's merge sources until
             * the root datasets are found with analysis options
             */
            if (d1.hasMergeSources()) {
                ok = mergedSourceOptionsAreSame(d1);

            } else {

                for (IAnalysisDataset d2 : list) {
                    if (d1 == d2) {
                        continue; // ignore self self comparisons
                    }

                    // ignore d2 with a merge source - it will be covered in the
                    // d1 loop
                    if (d2.hasMergeSources()) {
                        continue;
                    }

                    if(!d1.hasAnalysisOptions() || !d2.hasAnalysisOptions()){
                    	ok = false;
                    	continue;
                    }

                    IAnalysisOptions a1 = d1.getAnalysisOptions().get();
                    IAnalysisOptions a2 = d2.getAnalysisOptions().get();

                    if (!a1.equals(a2)) {
                    	ok = false;
                    }
                }

            }

        }
        return ok;
    }

    /**
     * Get the nucleus type that is applicable to all datasets in the list
     * 
     * @param list
     * @return
     */
    static NucleusType getBroadestNucleusType(@NonNull List<IAnalysisDataset> list) {

        NucleusType type = list.get(0).getCollection().getNucleusType();
        if (areSameNucleusType(list)) {
            return type;
        }

        return NucleusType.ROUND;
    }
    
    static File commonPathOfFiles(@NonNull Collection<File> files) {
    	String[][] folders = new String[files.size()][];

        int k = 0;

        // Split out the path elements to an array
        for (File f : files) {

            Path p = f.toPath();
            
            if(p!=null){

                Iterator<Path> it = p.iterator();
                List<String> s = new ArrayList<>();
                s.add(p.getRoot().toString());
                while (it.hasNext()) {
                    Path n = it.next();
                    s.add(n.toString());

                }
                folders[k++] = s.toArray(new String[0]);
            }

        }

        boolean breakLoop = false;
        List<String> common = new ArrayList<String>();
        for (int col = 0; col < folders[0].length; col++) {

            if (breakLoop) {
                break;
            }
            // Get first row
            String s = folders[0][col];

            for (int row = 1; row < files.size(); row++) {
                if (!s.equals(folders[row][col])) {
                    breakLoop = true;
                    break;
                }
            }
            if (breakLoop == false)
                common.add(s);

        }

        String commonPath = "";
        for (int i = 0; i < common.size(); i++) {

            commonPath += common.get(i);
            if (i > 0 && i < common.size() - 1) { // don't add separator after
                                                  // root or at the end
                commonPath += File.separator;
            }
        }

        return new File(commonPath);
    }

    /**
     * Get the most recent common ancestor of the dataset save file paths
     * 
     * @param datasets the list of datasets.
     * @return a file for the common directory. Check that the path exists and
     *         is a directory before using this.
     */
    static File commonPathOfFiles(@NonNull List<IAnalysisDataset> datasets) {

        List<File> files = new ArrayList<>(datasets.size());
        for (IAnalysisDataset d : datasets) {
            files.add(d.getSavePath());
        }
        return commonPathOfFiles(files);
        
    }

}
