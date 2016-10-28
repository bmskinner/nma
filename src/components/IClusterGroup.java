package components;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import analysis.ClusteringOptions;
import analysis.IAnalysisDataset;
import logging.Loggable;

public interface IClusterGroup extends Serializable, Loggable {

	/**
	 * Get the public name of the cluster groups
	 * @return
	 */
	String getName();

	/**
	 * Get the number of datasets in the group
	 * @return
	 */
	int size();

	/**
	 * Get the Newick tree for the cluster if set, or null
	 * @return
	 */
	String getTree();

	/**
	 * Get the IDs of the datasets in the group
	 * @return
	 */
	List<UUID> getUUIDs();

	/**
	 * Add a dataset as a cluster in the group
	 * @param dataset
	 */
	void addDataset(IAnalysisDataset dataset);

	/**
	 * Add a cell collection as a cluster in the group
	 * @param collection
	 */
	void addDataset(ICellCollection collection);

	/**
	 * Remove the selected dataset from the cluster group
	 * @param dataset
	 */
	void removeDataset(IAnalysisDataset dataset);

	/**
	 * Remove the selected dataset id from the cluster group
	 * @param dataset
	 */
	void removeDataset(UUID id);

	/**
	 * Get the options used to make this cluster group
	 * @return
	 */
	ClusteringOptions getOptions();

	/** 
	 * Test if this group contains the given dataset id
	 * @param id
	 * @return
	 */
	boolean hasDataset(UUID id);

	/**
	 * Check if there is a tree in this cluster group
	 * @return
	 */
	boolean hasTree();

	String toString();

}