/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package analysis;

import java.awt.Paint;
import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;

import logging.Loggable;
import utility.Version;
import components.ICellCollection;
import components.IClusterGroup;

/**
 * This describes an analysis dataset, which packages a collection
 * of cells with clusters, merge sources, and the options used for
 * the detection of the cells.
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IAnalysisDataset extends Serializable, Loggable  {

	/**
	 * Make a copy of the cells in this dataset. Does not yet include
	 * child datasets, clusters or signal groups
	 * @return
	 * @throws Exception 
	 */
	IAnalysisDataset duplicate() throws Exception;

	/**
	 * Get the file handler for this dataset. Create a handler
	 * if needed.
	 * @return
	 */
	Handler getLogHandler() throws Exception;

	/**
	 * Get the software version used to create the dataset
	 * @return
	 */
	Version getVersion();

	/**
	 * Add the given cell collection as a child to this dataset. A
	 * new dataset is contructed to hold it.
	 * @param collection the collection to add
	 */
	void addChildCollection(ICellCollection collection);

	/**
	 * Add the given dataset as a child of this dataset
	 * @param dataset
	 */
	void addChildDataset(IAnalysisDataset dataset);

	UUID getUUID();

	/**
	 * Get the name of the dataset. Passes through to
	 * CellCollection
	 * @return
	 * @see CellCollection
	 */
	String getName();

	/**
	 * Set the name of the dataset. Passes through
	 * to the CellCollection
	 * @param s
	 * @see CellCollection
	 */
	void setName(String s);

	/**
	 * Get the save file location
	 * @return
	 */
	File getSavePath();

	/**
	 * Set the path to save the dataset
	 * @param file
	 */
	void setSavePath(File file);

	/**
	 * Get the log file for the dataset.
	 * @return
	 * @see CellCollection
	 */
	File getDebugFile();

	/**
	 * Allow the collection to update the debug file location
	 * @param f the new file
	 */
	void setDebugFile(File f);

	/**
	 * Get all the direct children of this dataset
	 * @return
	 */
	Set<UUID> getChildUUIDs();

	/**
	 * Recursive version of getChildUUIDs.
	 * Get the children of this dataset, and all
	 * their children
	 * @return
	 */
	Set<UUID> getAllChildUUIDs();

	/**
	 * Get the specificed child
	 * @param id the child UUID
	 * @return
	 */
	IAnalysisDataset getChildDataset(UUID id);

	/**
	 * Get the AnalysisDataset with the given id
	 * that is a merge source to this dataset. 
	 * @param id the UUID of the dataset
	 * @return the dataset or null
	 */
	IAnalysisDataset getMergeSource(UUID id);

	/**
	 * Recursively fetch all the merge sources for this dataset.
	 * Only includes the root sources (not intermediate merges)
	 * @return
	 */
	Set<IAnalysisDataset> getAllMergeSources();

	/**
	 * Add the given dataset as a merge source
	 * @param dataset
	 */
	void addMergeSource(IAnalysisDataset dataset);

	/**
	 * Get all datasets considered direct merge sources to this
	 * dataset
	 * @return
	 */
	Set<IAnalysisDataset> getMergeSources();

	/**
	 * Get the ids of all datasets considered merge sources to this
	 * dataset
	 * @return
	 */
	Set<UUID> getMergeSourceIDs();

	/**
	 * Get the ids of all datasets considered merge sources to this
	 * dataset, recursively (that is, if the merge source is a merge, get
	 * the sources of that merge)
	 * @return
	 */
	Set<UUID> getAllMergeSourceIDs();

	/**
	 * Test if a dataset with the given id is present
	 * as a merge source
	 * @param id the UUID to test
	 * @return
	 */
	boolean hasMergeSource(UUID id);

	/**
	 * Test if a dataset is present
	 * as a merge source
	 * @param dataset the dataset to test
	 * @return
	 */
	boolean hasMergeSource(IAnalysisDataset dataset);

	/**
	 * Test if the dataset has merge sources
	 * @return
	 */
	boolean hasMergeSources();

	/**
	 * Get the number of direct children of this dataset
	 * @return
	 */
	int getChildCount();

	/**
	 * Check if the dataset has children
	 * @return
	 */
	boolean hasChildren();

	/**
	 * Get all the direct children of this dataset
	 * @return
	 */
	Collection<IAnalysisDataset> getChildDatasets();

	/**
	 * Recursive version of get child datasets
	 * Get all the direct children of this dataset, 
	 * and all their children.
	 * @return
	 */
	List<IAnalysisDataset> getAllChildDatasets();

	/**
	 * Get the collection in this dataset
	 * @return
	 */
	ICellCollection getCollection();

	/**
	 * Get the analysis options from this dataset
	 * @return
	 */
	IAnalysisOptions getAnalysisOptions();

	/**
	 * Test if the dataset has analysis options set.
	 * This is not the case for (for example) merge sources
	 * @return
	 */
	boolean hasAnalysisOptions();

	/**
	 * Set the analysis options for the dataset
	 * @param analysisOptions
	 */
	void setAnalysisOptions(IAnalysisOptions analysisOptions);

	/**
	 * Add the given dataset as a cluster result.
	 * This is a form of child dataset
	 * @param dataset
	 */
	void addClusterGroup(IClusterGroup group);

	/**
	 * Check the list of cluster groups, and return the highest
	 * cluster group number present
	 * @return
	 */
	int getMaxClusterGroupNumber();

	/**
	 * Check if the dataset id is in a cluster
	 * @param id
	 * @return
	 */
	boolean hasCluster(UUID id);

	List<IClusterGroup> getClusterGroups();

	/**
	 * Get the UUIDs of all datasets in clusters
	 * @return
	 */
	List<UUID> getClusterIDs();

	/**
	 * Check if the dataset has clusters
	 * @return
	 */
	boolean hasClusters();

	/**
	 * Test if the given group is present in this dataset
	 * @param group
	 * @return
	 */
	boolean hasClusterGroup(IClusterGroup group);

	/**
	 * Check that all cluster groups have child members present;
	 * if cluster groups do not have children, remove the group
	 */
	void refreshClusterGroups();

	/**
	 * Check if the dataset is root
	 * @return
	 */
	boolean isRoot();

	/**
	 * Set the dataset root status
	 * @param b is the dataset root
	 */
	void setRoot(boolean b);

	/**
	 * Delete the child AnalysisDataset specified
	 * @param id the UUID of the child to delete
	 */
	void deleteChild(UUID id);

	/**
	 * Delete the cluster with the given id
	 * @param id
	 */
	void deleteClusterGroup(IClusterGroup group);

	/**
	 * Delete an associated dataset
	 * @param id
	 */
	void deleteMergeSource(UUID id);

	/**
	 * Check if the given dataset is a child dataset of this
	 * @param child the dataset to test
	 * @return
	 */
	boolean hasChild(IAnalysisDataset child);

	/**
	 * Test if the given dataset is a child of this dataset or
	 * of one of its children
	 * @param child
	 * @return
	 */
	boolean hasRecursiveChild(IAnalysisDataset child);

	/**
	 * Check if the given dataset is a child dataset of this
	 * @param child
	 * @return
	 */
	boolean hasChild(UUID child);

	/**
	 * Set the dataset colour (used in comparisons between datasets)
	 * @param colour the new colour
	 */
	void setDatasetColour(Paint colour);

	/**
	 * Get the currently set dataset colour, or null if not set
	 * @return colour or null
	 */
	Paint getDatasetColour();

	/**
	 * Test if the dataset colour is set or null
	 * @return
	 */
	boolean hasDatasetColour();

	/**
	 * Get the swatch, or null if the swatch is not set. 
	 * Transient, not saved to nmd
	 * @return
	 */
	//	public ColourSwatch getSwatch() {
	//		return swatch;
	//	}

	String toString();

	/**
	 * Update the source image paths in the dataset and its children
	 * to use the given directory 
	 * @param expectedImageDirectory
	 * @param dataset
	 * @throws Exception
	 */
	void updateSourceImageDirectory(File expectedImageDirectory);

	/**
	 * Test if all the datasets in the list have a consensus nucleus
	 * @param list
	 * @return
	 */
	static boolean haveConsensusNuclei(List<IAnalysisDataset> list){
		for(IAnalysisDataset d : list){
			if( ! d.getCollection().hasConsensusNucleus()){
				return false;
			}
		}
		return true;
	}

}