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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;

/**
 * This is the virtual child dataset, which retains only the pointer to its
 * parent, a list of the ICell IDs it contains, and stats / profile caches.
 * 
 * @author ben
 * @since 1.13.3
 */
public class ChildAnalysisDataset extends AbstractAnalysisDataset implements IAnalysisDataset {
	
	private static final Logger LOGGER = Logger.getLogger(ChildAnalysisDataset.class.getName());

	private static final long serialVersionUID = 1L;

	/**
	 * Construct from a parent dataset (of which this will be a child) and a
	 * cell collection
	 * 
	 * @param parent
	 * @param collection
	 */
	public ChildAnalysisDataset(@NonNull IAnalysisDataset parent, @NonNull ICellCollection collection) {
		super(collection);
		this.parentDataset = parent;
	}
	
	public ChildAnalysisDataset(@NonNull Element e) throws ComponentCreationException {
		super(e);
	}

	@Override
	public Element toXmlElement() {
		Element e = super.toXmlElement().setName("ChildAnalysisDataset");
		return e;
	}



	@Override
	public IAnalysisDataset duplicate() throws Exception {
		ChildAnalysisDataset cd = new ChildAnalysisDataset(parentDataset, 
				new VirtualCellCollection(parentDataset, parentDataset.getCollection()));
		for(ICell c: this.getCollection())
			cd.getCollection().addCell(c);

		// copy the signals
		for(ISignalGroup s : cellCollection.getSignalGroups()) {
			cd.getCollection().addSignalGroup(s.duplicate());
		}


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
		addChildDataset(new ChildAnalysisDataset(this, collection));
	}

	@Override
	public void addChildDataset(@NonNull IAnalysisDataset dataset) {
        // Ensure no duplicate dataset names
        // If the name is the same as this dataset, or one of the child datasets, 
        // apply a suffix
        if(getName().equals(dataset.getName()) || 
        		childDatasets.stream().map(IAnalysisDataset::getName).anyMatch(s->s.equals(dataset.getName()))) {
        	String newName = chooseSuffix(dataset.getName());
        	dataset.setName(newName);
        }
        childDatasets.add(dataset);

	}

	@Override
	public File getSavePath() {
		return parentDataset.getSavePath();
	}

	@Override
	public void setSavePath(@NonNull File file) {
		// We can't set a path for a child 
	}

	@Override
	public void setScale(double scale) {				
		if(scale<=0) // don't allow a scale to cause divide by zero errors
			return;
		LOGGER.fine(() -> "Setting scale for "+getName()+" to "+scale);
		getCollection().setScale(scale);

		Optional<IAnalysisOptions> op = getAnalysisOptions();
		if(op.isPresent()){
			Set<String> detectionOptions = op.get().getDetectionOptionTypes();
			for(String detectedComponent : detectionOptions) {
				Optional<HashOptions> subOptions = op.get().getDetectionOptions(detectedComponent);
				if(subOptions.isPresent())
					subOptions.get().setDouble(HashOptions.SCALE, scale);
			}
		}

		for(IAnalysisDataset child : getChildDatasets()) {
			child.setScale(scale);
		}
	}

	@Override
	public Set<UUID> getChildUUIDs() {
		Set<UUID> result = new HashSet<>(childDatasets.size());
		for (IAnalysisDataset c : childDatasets) {
			result.add(c.getId());
		}

		return result;
	}

	@Override
	public Set<UUID> getAllChildUUIDs() {
		Set<UUID> result = new HashSet<>();

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
		if (this.hasDirectChild(id)) {

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
		return new HashSet<>(0);
	}

	@Override
	public void addMergeSource(@NonNull IAnalysisDataset dataset) {
		// Not possible for a child
	}

	@Override
	public List<IAnalysisDataset> getMergeSources() {
		return new ArrayList<>(0);
	}

	@Override
	public Set<UUID> getMergeSourceIDs() {
		return new HashSet<>(0);
	}

	@Override
	public Set<UUID> getAllMergeSourceIDs() {
		return new HashSet<>(0);
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
		List<IAnalysisDataset> result = new ArrayList<>();
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
		return parentDataset.getAnalysisOptions();
	}

	@Override
	public boolean hasAnalysisOptions() {
		return parentDataset.hasAnalysisOptions();
	}

	@Override
	public void setAnalysisOptions(IAnalysisOptions analysisOptions) {
		// Not possible for a child
	}

	@Override
	public void refreshClusterGroups() {
		if (this.hasClusters()) {
			// Find the groups that need removing
			List<IClusterGroup> groupsToDelete = new ArrayList<>();
			for (IClusterGroup g : this.getClusterGroups()) {
				boolean clusterRemains = false;

				for (UUID childID : g.getUUIDs()) {
					if (this.hasDirectChild(childID)) {
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
		// Not possible for a child
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
    public void deleteClusterGroup(@NonNull final IClusterGroup group) {

        if (hasClusterGroup(group)) {
        	UUID[] groupIds = group.getUUIDs().toArray(new UUID[0]);
        	
        	for(UUID id : groupIds)
        		deleteChild(id);
            
            // Remove saved values associated with the cluster group
            // e.g. tSNE, PCA
            for(Nucleus n : getCollection().getNuclei()) {
            	for(Measurement s : n.getStatistics()) {
            		if(s.toString().endsWith(group.getId().toString()))
            			n.clearStatistic(s);
            	}
            }
            this.clusterGroups.remove(group);
        }
    }
    
    @Override
    public void deleteClusterGroups() {
    	LOGGER.fine("Deleting all cluster groups in "+getName());
    	// Use arrays to avoid concurrent modifications to cluster groups
    	Object[] ids = clusterGroups.parallelStream().map(IClusterGroup::getId).toArray();
    	for(Object id : ids) {
    		Optional<IClusterGroup> optg = clusterGroups.stream()
    				.filter(group->group.getId().equals(id)).findFirst();
    		if(optg.isPresent())
    			deleteClusterGroup(optg.get());
    	}
    }

	@Override
	public void deleteMergeSource(@NonNull UUID id) {
		// No action
	}

	@Override
	public boolean hasDirectChild(UUID id) {

		for (IAnalysisDataset child : childDatasets) {
			if (child.getId().equals(id)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void updateSourceImageDirectory(@NonNull File expectedImageDirectory) {
		parentDataset.updateSourceImageDirectory(expectedImageDirectory);

	}

	@Override
	public String toString() {
		return this.cellCollection.getName();
	}

    @Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    	// Ensure the save version is correct at time of save 
    	this.versionLastSaved = Version.currentVersion();
    	out.defaultWriteObject();
    }
}
