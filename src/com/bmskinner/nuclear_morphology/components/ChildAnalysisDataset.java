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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;

/**
 * This is the virtual child dataset, which retains only the pointer to its
 * parent, a list of the ICell IDs it contains, and stats / profile caches.
 * 
 * @author ben
 * @since 1.13.3
 */
public class ChildAnalysisDataset extends AbstractAnalysisDataset implements IAnalysisDataset {

    private static final long serialVersionUID = 1L;

    private IAnalysisDataset parent;

    /**
     * Construct from a parent dataset (of which this will be a child) and a
     * cell collection
     * 
     * @param parent
     * @param collection
     */
    public ChildAnalysisDataset(@NonNull IAnalysisDataset parent, @NonNull ICellCollection collection) {
        super(collection);
        this.parent = parent;
    }

    @Override
    public IAnalysisDataset duplicate() throws Exception {
    	ChildAnalysisDataset cd = new ChildAnalysisDataset(parent, new VirtualCellCollection(parent, parent.getCollection()));
    	for(ICell c: this.getCollection())
    		cd.getCollection().addCell(c);
    	
        // copy the signals
        for(UUID id : cellCollection.getSignalGroupIDs())
        	cd.getCollection().addSignalGroup(id, cellCollection.getSignalGroup(id).get().duplicate());


        // copy child datasets
        for(IAnalysisDataset child : this.getAllChildDatasets())
        	cd.addChildDataset(child.duplicate());
        
        // copy merge sources
        for(IAnalysisDataset mge : this.getMergeSources())
        	cd.addMergeSource(mge.duplicate());
        
        cd.setDatasetColour(datasetColour);

    	return cd;
    }

    @Override
    public void addChildCollection(@NonNull ICellCollection collection) {
        IAnalysisDataset childDataset = new ChildAnalysisDataset(this, collection);
        childDatasets.add(childDataset);
    }

    @Override
    public void addChildDataset(@NonNull IAnalysisDataset dataset) {
        childDatasets.add(dataset);

    }

    @Override
    public File getSavePath() {
        return parent.getSavePath();
    }

    @Override
    public void setSavePath(@NonNull File file) {}

    @Override
    public Set<UUID> getChildUUIDs() {
        Set<UUID> result = new HashSet<UUID>(childDatasets.size());
        for (IAnalysisDataset c : childDatasets) {
            result.add(c.getId());
        }

        return result;
    }

    @Override
    public Set<UUID> getAllChildUUIDs() {
        Set<UUID> result = new HashSet<UUID>();

        Set<UUID> idlist = getChildUUIDs();
        result.addAll(idlist);

        for (UUID id : idlist) {
            IAnalysisDataset d = getChildDataset(id);

            result.addAll(d.getAllChildUUIDs());
        }
        return result;
    }

    @Override
    public IAnalysisDataset getChildDataset(@NonNull UUID id) {
        if (this.hasChild(id)) {

            for (IAnalysisDataset c : childDatasets) {
                if (c.getId().equals(id)) {
                    return c;
                }
            }

        } else {
            for (IAnalysisDataset child : this.getAllChildDatasets()) {
                if (child.getId().equals(id)) {
                    return child;
                }
            }
        }
        return null;
    }

    @Override
    public IAnalysisDataset getMergeSource(@NonNull UUID id) {
        return null;
    }

    @Override
    public Set<IAnalysisDataset> getAllMergeSources() {
        return new HashSet<IAnalysisDataset>(0);
    }

    @Override
    public void addMergeSource(@NonNull IAnalysisDataset dataset) {
    }

    @Override
    public Set<IAnalysisDataset> getMergeSources() {
        return new HashSet<IAnalysisDataset>(0);
    }

    @Override
    public Set<UUID> getMergeSourceIDs() {
        return new HashSet<UUID>(0);
    }

    @Override
    public Set<UUID> getAllMergeSourceIDs() {
        return new HashSet<UUID>(0);
    }

    @Override
    public boolean hasMergeSource(UUID id) {
        return false;
    }

    @Override
    public boolean hasMergeSource(IAnalysisDataset dataset) {
        return false;
    }

    @Override
    public boolean hasMergeSources() {
        return false;
    }

    @Override
    public int getChildCount() {
        return childDatasets.size();
    }

    @Override
    public boolean hasChildren() {
        return !childDatasets.isEmpty();
    }

    @Override
    public Collection<IAnalysisDataset> getChildDatasets() {
        return childDatasets;
    }

    @Override
    public List<IAnalysisDataset> getAllChildDatasets() {
        List<IAnalysisDataset> result = new ArrayList<IAnalysisDataset>();
        if (!childDatasets.isEmpty()) {

            for (IAnalysisDataset d : childDatasets) {
                result.add(d);
                result.addAll(d.getAllChildDatasets());
            }
        }
        return result;
    }

    @Override
    public ICellCollection getCollection() {
        return cellCollection;
    }

    @Override
    public Optional<IAnalysisOptions> getAnalysisOptions() {
        return parent.getAnalysisOptions();
    }

    @Override
    public boolean hasAnalysisOptions() {
        return parent.hasAnalysisOptions();
    }

    @Override
    public void setAnalysisOptions(IAnalysisOptions analysisOptions) {
    }

    @Override
    public void refreshClusterGroups() {
        if (this.hasClusters()) {
            // Find the groups that need removing
            List<IClusterGroup> groupsToDelete = new ArrayList<IClusterGroup>();
            for (IClusterGroup g : this.getClusterGroups()) {
                boolean clusterRemains = false;

                for (UUID childID : g.getUUIDs()) {
                    if (this.hasChild(childID)) {
                        clusterRemains = true;
                    }
                }
                if (!clusterRemains) {
                    groupsToDelete.add(g);
                }
            }

            // Remove the groups
            for (IClusterGroup g : groupsToDelete) {
                this.deleteClusterGroup(g);
            }

        }

    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public void setRoot(boolean b) {
    }

    @Override
    public void deleteChild(@NonNull UUID id) {
        Iterator<IAnalysisDataset> it = childDatasets.iterator();

        while (it.hasNext()) {
            IAnalysisDataset child = it.next();

            if (child.getId().equals(id)) {
                for (IClusterGroup g : clusterGroups) {
                    if (g.hasDataset(id)) {
                        g.removeDataset(id);
                    }
                }
                it.remove();
                break;
            }
        }
    }

    @Override
    public void deleteClusterGroup(IClusterGroup group) {
        if (hasClusterGroup(group)) {

            for (UUID id : group.getUUIDs()) {
                if (hasChild(id)) {
                    this.deleteChild(id);
                }
            }
            this.clusterGroups.remove(group);
        }
    }

    @Override
    public void deleteMergeSource(@NonNull UUID id) {
    }

    @Override
    public boolean hasChild(UUID id) {

        for (IAnalysisDataset child : childDatasets) {
            if (child.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateSourceImageDirectory(@NonNull File expectedImageDirectory) {
        parent.updateSourceImageDirectory(expectedImageDirectory);

    }

    @Override
	public String toString() {
        return this.cellCollection.getName();
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChildAnalysisDataset other = (ChildAnalysisDataset) obj;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		return true;
	}
}
