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

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This provides a virtual dataset view for merge sources.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class MergeSourceAnalysisDataset extends AbstractAnalysisDataset implements IAnalysisDataset {
	
	private static final Logger LOGGER = Logger.getLogger(MergeSourceAnalysisDataset.class.getName());

    private static final long serialVersionUID = 1L;

    private IAnalysisOptions analysisOptions; // the analysis options for
                                              // the merge source

    /**
     * Create a merge source for the given merged dataset, providing a source
     * template. A new virtual cell collection will be created from the merge
     * source dataset.
     * 
     * @param merged the dataset to which this dataset will belong
     * @param mergeSource the original dataset which was merged
     */
    public MergeSourceAnalysisDataset(IAnalysisDataset merged, IAnalysisDataset mergeSource) {
        super(new VirtualCellCollection(merged, mergeSource.getName(), mergeSource.getId(),
                mergeSource.getCollection())
        );

        this.parentDataset = merged;
        
        Optional<IAnalysisOptions> optionalOptions = mergeSource.getAnalysisOptions();
        if(optionalOptions.isPresent())
        	analysisOptions = optionalOptions.get().duplicate();

        this.datasetColour = mergeSource.getDatasetColour().orElse(null);

        try {
        	getCollection().createProfileCollection();
            mergeSource.getCollection().getProfileManager().copyCollectionOffsets(getCollection());
        } catch (ProfileException e) {
            LOGGER.warning("Unable to create merge source dataset");
            LOGGER.log(Loggable.STACK, "Error copying offsets", e);
        }
        
        // Ensure merge sources from the source datasets are retained
        if(mergeSource.hasMergeSources()){
        	for(IAnalysisDataset d : mergeSource.getMergeSources()){
        		this.addMergeSource(d);
        	}
        }

    }

    @Override
    public IAnalysisDataset duplicate() throws Exception {
        throw new Exception("Not yet implemented");
    }

    @Override
    public void addChildCollection(@NonNull ICellCollection collection) {
    	// cannot be changed here
    }

    @Override
    public void addChildDataset(@NonNull IAnalysisDataset dataset) {
    	// cannot be changed here
    }

    @Override
    public File getSavePath() {
        return parentDataset.getSavePath();
    }

    @Override
    public void setSavePath(@NonNull File file) {
    	// cannot be changed here
    }
    
    @Override
    public void setScale(double scale) {				
		if(scale<=0) // don't allow a scale to cause divide by zero errors
			return;
		LOGGER.fine("Setting scale for "+getName()+" to "+scale);
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
    public Set<UUID> getAllChildUUIDs() {
        return new HashSet<>(0);
    }

    @Override
    public IAnalysisDataset getChildDataset(@NonNull UUID id) {
        return null;
    }

    @Override
    public IAnalysisDataset getMergeSource(@NonNull UUID id) {
        if (this.hasMergeSource(id)) {

            for (IAnalysisDataset c : childDatasets) {
                if (c.getId().equals(id)) {
                    return c;
                }
            }

        } else {
            for (IAnalysisDataset child : this.getAllMergeSources()) {
                if (child.getId().equals(id)) {
                    return child;
                }
            }
        }
        return null;
    }

    @Override
    public Set<IAnalysisDataset> getAllMergeSources() {
        Set<IAnalysisDataset> result = new HashSet<>(childDatasets.size());

        if (!childDatasets.isEmpty()) {
            for (IAnalysisDataset d : childDatasets) {
                result.add(d);
                result.addAll(d.getAllChildDatasets());
            }
        }
        return result;
    }

    @Override
    public void addMergeSource(@NonNull IAnalysisDataset dataset) {
        childDatasets.add(new MergeSourceAnalysisDataset(this, dataset));
    }

    @Override
    public Set<IAnalysisDataset> getMergeSources() {
        return childDatasets;
    }

    @Override
    public Set<UUID> getMergeSourceIDs() {
        Set<UUID> result = new HashSet<>();
        for (IAnalysisDataset c : childDatasets)
            result.add(c.getId());
        return result;
    }

    @Override
    public Set<UUID> getAllMergeSourceIDs() {
        Set<UUID> result = new HashSet<>();

        Set<UUID> idlist = getMergeSourceIDs();
        result.addAll(idlist);

        for (UUID id : idlist) {
            IAnalysisDataset d = getMergeSource(id);

            result.addAll(d.getAllMergeSourceIDs());
        }
        return result;
    }

    @Override
    public boolean hasMergeSource(@NonNull UUID id) {
        for (IAnalysisDataset child : childDatasets) {
            if (child.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasMergeSource(@NonNull IAnalysisDataset dataset) {
        return childDatasets.contains(dataset);
    }

    @Override
    public boolean hasMergeSources() {
        return !childDatasets.isEmpty();
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public Collection<IAnalysisDataset> getChildDatasets() {
        return new ArrayList<>(0);
    }

    @Override
    public List<IAnalysisDataset> getAllChildDatasets() {
        return new ArrayList<>(0);
    }

    @Override
    public ICellCollection getCollection() {
        return cellCollection;
    }

    @Override
    public Optional<IAnalysisOptions> getAnalysisOptions() {
        return Optional.ofNullable(analysisOptions);
    }

    @Override
    public boolean hasAnalysisOptions() {
        return analysisOptions != null;
    }

    @Override
    public void setAnalysisOptions(IAnalysisOptions analysisOptions) {
        this.analysisOptions = analysisOptions;

    }

    @Override
    public void refreshClusterGroups() {
    	// impossible
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public void setRoot(boolean b) {
    	// impossible
    }

    @Override
    public void deleteChild(@NonNull UUID id) {
    	// do not allow
    }

    @Override
    public void deleteMergeSource(@NonNull UUID id) {

        Iterator<IAnalysisDataset> it = childDatasets.iterator();

        while (it.hasNext()) {
            IAnalysisDataset child = it.next();
            if (child.getId().equals(id)) {
                it.remove();
            }
        }
    }

    @Override
    public void updateSourceImageDirectory(@NonNull File expectedImageDirectory) {
    	// No action needed
    }

    @Override
    public Set<UUID> getChildUUIDs() {
        return new HashSet<>(0);
    }

    @Override
    public void deleteClusterGroup(@NonNull IClusterGroup group) {
    	// No groups present
    }
    
    @Override
    public void deleteClusterGroups() {
    	// No groups present
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((analysisOptions == null) ? 0 : analysisOptions.hashCode());
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
		MergeSourceAnalysisDataset other = (MergeSourceAnalysisDataset) obj;
		if (analysisOptions == null) {
			if (other.analysisOptions != null)
				return false;
		} else if (!analysisOptions.equals(other.analysisOptions))
			return false;
		return true;
	}
    
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    	// Ensure the save version is correct at time of save 
    	this.versionLastSaved = Version.currentVersion();
    	out.defaultWriteObject();
    }

}
