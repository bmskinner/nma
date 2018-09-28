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
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This is the most primitive information an analysis dataset requires. This
 * does not implement the IAnalysisDataset interface itself - it is the
 * responsibility of extending classes to add the remaining fields and methods.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public abstract class AbstractAnalysisDataset implements Serializable, Loggable {

    private static final long serialVersionUID = 1L;

    protected final Version version;

    // direct child collections
    protected Set<IAnalysisDataset> childDatasets = new HashSet<>();

    protected ICellCollection cellCollection;

    protected Paint datasetColour = null;

    // groups of cluster results
    protected List<IClusterGroup> clusterGroups = new ArrayList<>();

    /**
     * Create a dataset from a cell collection
     * 
     * @param collection
     */
    protected AbstractAnalysisDataset(ICellCollection collection) {
        this.cellCollection = collection;
        this.version = Version.currentVersion();
    }
    
    public Version getVersion() {
        return this.version;
    }

    public UUID getId() {
        return cellCollection.getID();
    }

    public String getName() {
        return cellCollection.getName();
    }

    public void setName(String s) {
        cellCollection.setName(s);
    }

    public void setDatasetColour(Paint colour) {
        datasetColour = colour;

    }

    public Optional<Paint> getDatasetColour() {
        return Optional.ofNullable(datasetColour);
    }

    public boolean hasDatasetColour() {
        return datasetColour != null;
    }

    public boolean hasChild(IAnalysisDataset child) {
        return childDatasets.contains(child);
    }

    public abstract Set<UUID> getChildUUIDs();

    public boolean hasChild(UUID child) {
        return this.getChildUUIDs().contains(child);
    }

    public boolean hasRecursiveChild(IAnalysisDataset child) {
        if (hasChild(child)) {
            return true;
        }
        for (IAnalysisDataset c : childDatasets) {
            if (c.hasRecursiveChild(child)) {
                return true;
            }
        }
        return false;
    }

    public abstract void deleteClusterGroup(@NonNull IClusterGroup group);

    public void addClusterGroup(IClusterGroup group) {
        this.clusterGroups.add(group);
    }

    public int getMaxClusterGroupNumber() {
        int number = 0;

        if (this.hasClusters()) {

            for (IClusterGroup g : this.getClusterGroups()) {

                String name = g.getName();

                Pattern p = Pattern.compile("^" + IClusterGroup.CLUSTER_GROUP_PREFIX + "_(\\d+)$");

                Matcher m = p.matcher(name);
                if (m.find()) {
                    String s = m.group(1);

                    int n = Integer.valueOf(s);
                    if (n > number) {
                        number = n;
                    }
                }
            }
        }
        return number;
    }

    public boolean hasCluster(UUID id) {
        for (IClusterGroup g : this.clusterGroups) {
            if (g.hasDataset(id)) 
            	return true;
        }
        return false;
    }

    public List<IClusterGroup> getClusterGroups() {
        return this.clusterGroups;
    }

    public List<UUID> getClusterIDs() {
        List<UUID> result = new ArrayList<UUID>();
        for (IClusterGroup g : this.clusterGroups) {
            result.addAll(g.getUUIDs());
        }
        return result;
    }

    public boolean hasClusters() {
    	return this.clusterGroups != null && this.clusterGroups.size() > 0;
    }

    public boolean hasClusterGroup(IClusterGroup group) {
        return clusterGroups.contains(group);
    }
    
    @Override
    public String toString(){
        return getName();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    	// The first thing to be deserialised in this dataset will be the Version.
    	// If not supported, an UnsupportedVersionException will be thrown, and
    	// passed upwards here for the import method to handle.
        in.defaultReadObject();
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cellCollection == null) ? 0 : cellCollection.hashCode());
		result = prime * result + ((childDatasets == null) ? 0 : childDatasets.hashCode());
		result = prime * result + ((clusterGroups == null) ? 0 : clusterGroups.hashCode());
		result = prime * result + ((datasetColour == null) ? 0 : datasetColour.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractAnalysisDataset other = (AbstractAnalysisDataset) obj;
		if (cellCollection == null) {
			if (other.cellCollection != null)
				return false;
		} else if (!cellCollection.equals(other.cellCollection))
			return false;
		if (childDatasets == null) {
			if (other.childDatasets != null)
				return false;
		} else if (!childDatasets.equals(other.childDatasets))
			return false;
		if (clusterGroups == null) {
			if (other.clusterGroups != null)
				return false;
		} else if (!clusterGroups.equals(other.clusterGroups))
			return false;
		if (datasetColour == null) {
			if (other.datasetColour != null)
				return false;
		} else if (!datasetColour.equals(other.datasetColour))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}
    
    

}
