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
package com.bmskinner.nma.components.datasets;

import java.awt.Color;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.io.XmlSerializable;

/**
 * This describes an analysis dataset, which packages a collection of cells with
 * clusters, merge sources, and the options used for the detection of the cells.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IAnalysisDataset extends XmlSerializable {

	static final Logger LOGGER = Logger.getLogger(IAnalysisDataset.class.getName());

	/**
	 * Get the ID of the dataset
	 * 
	 * @return
	 */
	UUID getId();

	/**
	 * Make a copy of the cells in this dataset. Does not yet include child
	 * datasets, clusters or signal groups
	 * 
	 * @return
	 * @throws ComponentCreationException
	 */
	IAnalysisDataset copy() throws ComponentCreationException;

	/**
	 * Get the software version used to create the dataset
	 * 
	 * @return
	 */
	Version getVersionCreated();

	/**
	 * Get the software version the dataset was last saved in
	 * 
	 * @return
	 */
	Version getVersionLastSaved();

	/**
	 * Shortcut to ICellCollection::size for convenience
	 * 
	 * @return the number of cells in the dataset
	 */
	int size();

	/**
	 * Add the given cell collection as a child to this dataset. A new dataset is
	 * contructed to hold it.
	 * 
	 * @param collection the collection to add
	 * @return the newly created dataset
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 */
	IAnalysisDataset addChildCollection(@NonNull ICellCollection collection)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Add the given dataset as a child of this dataset
	 * 
	 * @param dataset
	 * @return the newly created dataset
	 */
	IAnalysisDataset addChildDataset(@NonNull IAnalysisDataset dataset);

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
	 * Recursive version of getChildUUIDs. Get the children of this dataset, and all
	 * their children
	 * 
	 * @return
	 */
	Set<UUID> getAllChildUUIDs();

	/**
	 * Get the specificed child
	 * 
	 * @param id the child UUID
	 * @return
	 */
	IAnalysisDataset getChildDataset(@NonNull UUID id);

	/**
	 * Get the AnalysisDataset with the given id that is a merge source to this
	 * dataset.
	 * 
	 * @param id the UUID of the dataset
	 * @return the dataset or null
	 */
	IAnalysisDataset getMergeSource(@NonNull UUID id);

	/**
	 * Recursively fetch all the merge sources for this dataset. Only includes the
	 * root sources (not intermediate merges)
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
	List<IAnalysisDataset> getMergeSources();

	/**
	 * Get the ids of all datasets considered merge sources to this dataset
	 * 
	 * @return
	 */
	Set<UUID> getMergeSourceIDs();

	/**
	 * Get the ids of all datasets considered merge sources to this dataset,
	 * recursively (that is, if the merge source is a merge, get the sources of that
	 * merge)
	 * 
	 * @return
	 */
	Set<UUID> getAllMergeSourceIDs();

	/**
	 * Test if a dataset with the given id is present as a merge source
	 * 
	 * @param id the UUID to test
	 * @return
	 */
	boolean hasMergeSource(@NonNull UUID id);

	/**
	 * Test if a dataset is present as a merge source
	 * 
	 * @param dataset the dataset to test
	 * @return
	 */
	boolean hasMergeSource(@NonNull IAnalysisDataset dataset);

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
	 * Recursive version of get child datasets Get all the direct children of this
	 * dataset, and all their children.
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
	 * Test if the dataset has analysis options set. This is not the case for (for
	 * example) merge sources
	 * 
	 * @return
	 */
	boolean hasAnalysisOptions();

	/**
	 * Set the analysis options for the dataset. Note that if this is a child
	 * dataset, the child and parent will lose sync
	 * 
	 * @param analysisOptions
	 */
	void setAnalysisOptions(@NonNull IAnalysisOptions analysisOptions);

	/**
	 * Add the given dataset as a cluster result. This is a form of child dataset
	 * 
	 * @param dataset
	 */
	void addClusterGroup(@NonNull IClusterGroup group);

	/**
	 * Check the list of cluster groups, and return the highest cluster group number
	 * present
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
	boolean hasCluster(@NonNull UUID id);

	/**
	 * Get the cluster groups in the dataset
	 * 
	 * @return
	 */
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
	boolean hasClusterGroup(@NonNull IClusterGroup group);

	/**
	 * Check that all cluster groups have child members present; if cluster groups
	 * do not have children, remove the group
	 */
	void refreshClusterGroups();

	/**
	 * Check if the dataset is root
	 * 
	 * @return
	 */
	boolean isRoot();

	/**
	 * Delete a child dataset. Has no effect if no child with the given id is
	 * present.
	 * 
	 * @param id the UUID of the child to delete
	 */
	void deleteChild(@NonNull UUID id);

	/**
	 * Delete the cluster with the given id. This will delete both the cluster group
	 * metadata, and the individual child datasets within the group.
	 * 
	 * @param group the cluster group to delete
	 */
	void deleteClusterGroup(@NonNull IClusterGroup group);

	/**
	 * Delete all cluster groups and associated child datasets.
	 */
	void deleteClusterGroups();

	/**
	 * Delete an associated dataset
	 * 
	 * @param id
	 */
	void deleteMergeSource(@NonNull UUID id);

	/**
	 * Check if the given dataset is a child dataset of this
	 * 
	 * @param child the dataset to test
	 * @return
	 */
	boolean hasDirectChild(@NonNull IAnalysisDataset child);

	/**
	 * Test if the given dataset is a child of this dataset or of one of its
	 * children
	 * 
	 * @param child
	 * @return
	 */
	boolean hasAnyChild(@NonNull IAnalysisDataset child);

	/**
	 * Check if the given dataset is a child dataset of this
	 * 
	 * @param child
	 * @return
	 */
	boolean hasDirectChild(@NonNull UUID child);

	/**
	 * Update the image scale for all cells in the dataset
	 * 
	 * @param scale
	 */
	void setScale(double scale);

	/**
	 * Set the dataset colour (used in comparisons between datasets)
	 * 
	 * @param colour the new colour
	 */
	void setDatasetColour(Color colour);

	/**
	 * Get the currently set dataset colour, or null if not set
	 * 
	 * @return colour or null
	 */
	Optional<Color> getDatasetColour();

	/**
	 * Test if the dataset colour is set or null
	 * 
	 * @return
	 */
	boolean hasDatasetColour();

	/**
	 * Update the source image paths in the dataset and its children to use the
	 * given directory. If the nuclei have signals in the same image as the nuclear
	 * stain, these will also be updated.
	 * 
	 * @param expectedImageDirectory
	 * @param dataset
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
	static boolean haveSameRulesets(@NonNull List<IAnalysisDataset> list) {

		RuleSetCollection col = list.get(0).getCollection().getRuleSetCollection();

		for (IAnalysisDataset d : list) {
			RuleSetCollection next = d.getCollection().getRuleSetCollection();
			if (!next.equals(col)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Test if the merge sources of a dataset have the same analysis options TODO:
	 * make recursive; what happens when two merged datsets are merged?
	 * 
	 * @param dataset
	 * @return the common options, or null if an options is different
	 */
	static boolean mergedSourceOptionsAreSame(@NonNull IAnalysisDataset dataset) {

		List<IAnalysisDataset> list = dataset.getMergeSources();

		boolean ok = true;

		for (IAnalysisDataset d1 : list) {

			/*
			 * If the dataset has merge sources, the options are null In this case,
			 * recursively go through the dataset's merge sources until the root datasets
			 * are found with analysis options
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

					if (!d1.hasAnalysisOptions() || !d2.hasAnalysisOptions()) {
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

}
