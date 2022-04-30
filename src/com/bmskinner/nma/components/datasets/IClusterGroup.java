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

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.io.XmlSerializable;

/**
 * The cluster group saves a list of child datasets with a group name and a
 * Newick tree of the clustered nuclei within the datasets.
 * 
 * @author ben
 *
 */
public interface IClusterGroup extends Serializable, XmlSerializable {

    String CLUSTER_GROUP_PREFIX = "Group";
    
    /**
     * Get the ID of the group
     * @return
     */
    UUID getId();

    /**
     * Get the public name of the cluster groups
     * 
     * @return
     */
    String getName();
    
    /**
     * Make a copy of this group
     * @return
     */
    IClusterGroup duplicate();
    
    
    /**
     * Set the name of the cluster group
     */
    void setName(String s);

    /**
     * Get the number of datasets in the group
     * 
     * @return
     */
    int size();

    /**
     * Get the Newick tree for the cluster if set, or null
     * 
     * @return
     */
    String getTree();

    /**
     * Get the IDs of the datasets in the group
     * 
     * @return
     */
    List<UUID> getUUIDs();

    /**
     * Add a dataset as a cluster in the group
     * 
     * @param dataset
     */
    void addDataset(IAnalysisDataset dataset);

    /**
     * Add a cell collection as a cluster in the group
     * 
     * @param collection
     */
    void addDataset(ICellCollection collection);

    /**
     * Remove the selected dataset from the cluster group
     * 
     * @param dataset
     */
    void removeDataset(IAnalysisDataset dataset);

    /**
     * Remove the selected dataset id from the cluster group
     * 
     * @param dataset
     */
    void removeDataset(UUID id);

    /**
     * Get the options used to make this cluster group
     * 
     * @return
     */
    Optional<HashOptions> getOptions();

    /**
     * Test if this group contains the given dataset id
     * 
     * @param id
     * @return
     */
    boolean hasDataset(UUID id);

    /**
     * Check if there is a tree in this cluster group
     * 
     * @return
     */
    boolean hasTree();


}
