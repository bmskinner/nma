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
package com.bmskinner.nuclear_morphology.components.datasets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;

/**
 * This holds the ids of datasets created by clustering, plus the clustering
 * options that were used to generate the clusters
 *
 */
public class DefaultClusterGroup implements IClusterGroup {

    private static final long  serialVersionUID = 1L;
    
    /** Dataset IDs in the cluster */
    private List<UUID> ids = new ArrayList<>();
    
    /** Options used to generate the cluster */
    private HashOptions options = null;
    private String name;
    private String newickTree = null;
    private UUID id;

    /**
     * Create a new cluster group
     * 
     * @param name the group name (informal)
     * @param options the options used to create the cluster
     */
    public DefaultClusterGroup(@NonNull String name, @NonNull HashOptions options) {
        this.name = name;
        this.options = options;
        this.id = UUID.randomUUID();
    }
    
    public DefaultClusterGroup(@NonNull Element e) { 
    	id = UUID.fromString(e.getAttributeValue("id"));
    	name = e.getAttributeValue("name");
    	
    	if(e.getChild("NewickTree")!=null)
    		newickTree = e.getChildText("NewickTree");
    	
    	options = new DefaultOptions(e.getChild("Options"));
    	
    	for(Element el : e.getChildren("DatasetId"))
    		ids.add(UUID.fromString(el.getText()));
    	
    }

    @Override
	public Element toXmlElement() {
    	Element e = new Element("ClusterGroup").setAttribute("id", id.toString()).setAttribute("name", name);
    	
    	e.addContent(options.toXmlElement());
    	
    	if(newickTree!=null)
    		e.addContent(new Element("NewickTree").setText(newickTree));
    	
    	for(UUID i : ids)
    		e.addContent(new Element("DatasetId").setText(i.toString()));
    	
    	return e;
	}



	/**
     * Create a new cluster group with a tree
     * 
     * @param name the group name (informal)
     * @param options the options used to create the cluster
     * @param tree the Newick tree for the cluster as a String
     */
    public DefaultClusterGroup(@NonNull String name, @NonNull HashOptions options, @NonNull String tree) {
        this(name, options);
        this.newickTree = tree;
    }

    /**
     * Create a cluster group from a template
     * @param template
     */
    public DefaultClusterGroup(@NonNull IClusterGroup template) {
    	if(template.getOptions().isPresent()){
    		options = template.getOptions().get().duplicate();
    	} else {
    		options = null;
    	}

        this.name = template.getName();
        this.newickTree = template.getTree();
        this.ids = template.getUUIDs();
        this.id = template.getId();
    }
    
    @Override
    public UUID getId() {
    	return id;
    }

    @Override
    public String getName() {
        return this.name;
    }
    
    @Override
    public void setName(String s) {
        name = s;
    }

    @Override
    public int size() {
        return this.ids.size();
    }

    @Override
    public String getTree() {
        return this.newickTree;
    }

    @Override
    public List<UUID> getUUIDs() {
        return this.ids;
    }

    @Override
    public void addDataset(final IAnalysisDataset dataset) {
        this.ids.add(dataset.getId());
    }

    @Override
    public void addDataset(final ICellCollection collection) {
        this.ids.add(collection.getId());
    }

    @Override
    public void removeDataset(final IAnalysisDataset dataset) {
        removeDataset(dataset.getId());
    }

    @Override
    public void removeDataset(final UUID id) {
        this.ids.remove(id);
    }

    @Override
    public Optional<HashOptions> getOptions() {
        return Optional.ofNullable(options);
    }

    @Override
    public boolean hasDataset(final UUID id) {
        return ids.contains(id);
    }

    @Override
    public boolean hasTree() {
        return newickTree != null;
    }

    @Override
    public String toString() {
        return this.name;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if(id==null)
        	id = UUID.randomUUID();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

}
