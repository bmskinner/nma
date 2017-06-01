/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.components;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;

/**
 * This provides a virtual dataset view for merge sources.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class MergeSourceAnalysisDataset extends AbstractAnalysisDataset implements IAnalysisDataset {

    private static final long serialVersionUID = 1L;

    private IAnalysisDataset parent; // the 'parent to this dataset; the merged
                                     // dataset with the real cells

    private IMutableAnalysisOptions analysisOptions; // the analysis options for
                                                     // the merge source

    /**
     * Create a merge source for the given merged dataset, providing a source
     * template. A new virtual cell collection will be created from the merge
     * source dataset.
     * 
     * @param merged
     *            the dataset to which this dataset will belong
     * @param mergeSource
     *            the original dataset which was merged
     */
    public MergeSourceAnalysisDataset(IAnalysisDataset merged, IAnalysisDataset mergeSource) {
        super(new VirtualCellCollection(merged, mergeSource.getName(), mergeSource.getUUID(),
                mergeSource.getCollection())

        );

        this.parent = merged;
        try {
            this.analysisOptions = mergeSource.getAnalysisOptions();
        } catch (MissingOptionException e1) {
            stack(e1.getMessage(), e1);
        }
        this.datasetColour = mergeSource.getDatasetColour();

        this.getCollection().createProfileCollection();
        try {
            mergeSource.getCollection().getProfileManager().copyCollectionOffsets(this.getCollection());
        } catch (ProfileException e) {
            warn("Unable to create merge source dataset");
            fine("Error copying offsets", e);
        }

    }

    /**
     * Get the parent dataset (in this case, the merged dataset)
     * 
     * @return the parent dataset
     */
    public IAnalysisDataset getParent() {
        return parent;
    }

    @Override
    public IAnalysisDataset duplicate() throws Exception {
        throw new Exception("Not yet implemented");
    }

    @Override
    public Handler getLogHandler() throws Exception {
        return parent.getLogHandler();
    }

    @Override
    public void addChildCollection(ICellCollection collection) {
    }

    @Override
    public void addChildDataset(IAnalysisDataset dataset) {
    }

    @Override
    public File getSavePath() {
        return parent.getSavePath();
    }

    @Override
    public void setSavePath(File file) {
    }

    @Override
    public File getDebugFile() {
        return parent.getDebugFile();
    }

    @Override
    public void setDebugFile(File f) {
    }

    @Override
    public Set<UUID> getAllChildUUIDs() {
        return new HashSet<UUID>(0);
    }

    @Override
    public IAnalysisDataset getChildDataset(UUID id) {
        return null;
    }

    @Override
    public IAnalysisDataset getMergeSource(UUID id) {
        if (this.hasMergeSource(id)) {

            for (IAnalysisDataset c : childDatasets) {
                if (c.getUUID().equals(id)) {
                    return c;
                }
            }

        } else {
            for (IAnalysisDataset child : this.getAllMergeSources()) {
                if (child.getUUID().equals(id)) {
                    return child;
                }
            }
        }
        return null;
    }

    @Override
    public Set<IAnalysisDataset> getAllMergeSources() {
        Set<IAnalysisDataset> result = new HashSet<IAnalysisDataset>(childDatasets.size());

        if (!childDatasets.isEmpty()) {

            for (IAnalysisDataset d : childDatasets) {
                result.add(d);
                result.addAll(d.getAllChildDatasets());
            }
        }
        return result;
    }

    @Override
    public void addMergeSource(IAnalysisDataset dataset) {
        childDatasets.add(new MergeSourceAnalysisDataset(this, dataset));
    }

    @Override
    public Set<IAnalysisDataset> getMergeSources() {
        return childDatasets;
    }

    @Override
    public Set<UUID> getMergeSourceIDs() {
        Set<UUID> result = new HashSet<UUID>(childDatasets.size());
        for (IAnalysisDataset c : childDatasets) {
            result.add(c.getUUID());
        }

        return result;
    }

    @Override
    public Set<UUID> getAllMergeSourceIDs() {
        Set<UUID> result = new HashSet<UUID>();

        Set<UUID> idlist = getMergeSourceIDs();
        result.addAll(idlist);

        for (UUID id : idlist) {
            IAnalysisDataset d = getMergeSource(id);

            result.addAll(d.getAllMergeSourceIDs());
        }
        return result;
    }

    @Override
    public boolean hasMergeSource(UUID id) {
        for (IAnalysisDataset child : childDatasets) {
            if (child.getUUID().equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasMergeSource(IAnalysisDataset dataset) {
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
        return new ArrayList<IAnalysisDataset>(0);
    }

    @Override
    public List<IAnalysisDataset> getAllChildDatasets() {
        return new ArrayList<IAnalysisDataset>(0);
    }

    @Override
    public ICellCollection getCollection() {
        return cellCollection;
    }

    @Override
    public IMutableAnalysisOptions getAnalysisOptions() {
        return analysisOptions;
    }

    @Override
    public boolean hasAnalysisOptions() {
        return analysisOptions != null;
    }

    @Override
    public void setAnalysisOptions(IMutableAnalysisOptions analysisOptions) {
        this.analysisOptions = analysisOptions;

    }

    @Override
    public void refreshClusterGroups() {
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public void setRoot(boolean b) {
    }

    @Override
    public void deleteChild(UUID id) {
    }

    @Override
    public void deleteMergeSource(UUID id) {

        Iterator<IAnalysisDataset> it = childDatasets.iterator();

        while (it.hasNext()) {
            IAnalysisDataset child = it.next();
            if (child.getUUID().equals(id)) {
                it.remove();
            }
        }
    }

    @Override
    public void updateSourceImageDirectory(File expectedImageDirectory) {
    }

    @Override
    public Set<UUID> getChildUUIDs() {
        return new HashSet<UUID>(0);
    }

    @Override
    public void deleteClusterGroup(IClusterGroup group) {
    }

}
