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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;

/**
 * This holds the ids of datasets created by clustering, plus the clustering
 * options that were used to generate the clusters
 *
 */
public class ClusterGroup implements IClusterGroup {

    private static final long  serialVersionUID = 1L;
    private List<UUID>         ids              = new ArrayList<UUID>(0); // hold
                                                                          // the
                                                                          // ids
                                                                          // of
                                                                          // datasets
                                                                          // in
                                                                          // a
                                                                          // cluster
    private IClusteringOptions options = null;                              // store
                                                                          // the
                                                                          // options
                                                                          // that
                                                                          // were
                                                                          // used
                                                                          // to
                                                                          // generate
                                                                          // the
                                                                          // cluster
    private String             name;
    private String             newickTree       = null;

    /**
     * Create a new cluster group
     * 
     * @param name
     *            the group name (informal)
     * @param options
     *            the options used to create the cluster
     */
    public ClusterGroup(@NonNull String name, @NonNull IClusteringOptions options) {
        this.name = name;
        this.options = options;
    }

    /**
     * Create a new cluster group with a tree
     * 
     * @param name
     *            the group name (informal)
     * @param options
     *            the options used to create the cluster
     * @param tree
     *            the Newick tree for the cluster as a String
     */
    public ClusterGroup(@NonNull String name, @NonNull IClusteringOptions options, @NonNull String tree) {
        this(name, options);
        this.newickTree = tree;
    }

    public ClusterGroup(@NonNull IClusterGroup template) {
    	if(template.getOptions().isPresent()){
    		options = OptionsFactory.makeClusteringOptions(template.getOptions().get());
    	} else {
    		options = null;
    	}

        this.name = template.getName();
        this.newickTree = template.getTree();
        this.ids = template.getUUIDs();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.IClusterGroup#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public void setName(String s) {
        name = s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.IClusterGroup#size()
     */
    @Override
    public int size() {
        return this.ids.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.IClusterGroup#getTree()
     */
    @Override
    public String getTree() {
        return this.newickTree;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.IClusterGroup#getUUIDs()
     */
    @Override
    public List<UUID> getUUIDs() {
        return this.ids;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.IClusterGroup#addDataset(analysis.IAnalysisDataset)
     */
    @Override
    public void addDataset(IAnalysisDataset dataset) {
        this.ids.add(dataset.getId());
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.IClusterGroup#addDataset(components.ICellCollection)
     */
    @Override
    public void addDataset(ICellCollection collection) {
        this.ids.add(collection.getID());
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.IClusterGroup#removeDataset(analysis.IAnalysisDataset)
     */
    @Override
    public void removeDataset(IAnalysisDataset dataset) {
        removeDataset(dataset.getId());
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.IClusterGroup#removeDataset(java.util.UUID)
     */
    @Override
    public void removeDataset(UUID id) {
        this.ids.remove(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.IClusterGroup#getOptions()
     */
    @Override
    public Optional<IClusteringOptions> getOptions() {
        return Optional.ofNullable(options);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.IClusterGroup#hasDataset(java.util.UUID)
     */
    @Override
    public boolean hasDataset(UUID id) {
        return ids.contains(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.IClusterGroup#hasTree()
     */
    @Override
    public boolean hasTree() {
        return newickTree != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.IClusterGroup#toString()
     */
    @Override
    public String toString() {
        return this.name;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

}
