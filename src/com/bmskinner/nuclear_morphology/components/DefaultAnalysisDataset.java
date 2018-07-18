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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.Io.Importer;

/**
 * This is the replacement analysis dataset designed to use less memory from
 * versions 1.14.0 onwards. The first field in the object is the version,
 * allowing deserialisation to choose an appropriate path in the future.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultAnalysisDataset extends AbstractAnalysisDataset implements IAnalysisDataset {

    private static final long serialVersionUID = 1L;

    private boolean isRoot = false; // is this a root dataset

    /**
     * Other datasets associated with this dataset, that will need to be saved
     * out. Includes merge sources presently, with scope for expansion
     */
    private Set<IAnalysisDataset> otherDatasets = new HashSet<IAnalysisDataset>();

    /**
     * The ids of datasets merged to create this dataset. The IDs must be
     * present in otherCollections.
     */
    private Set<UUID> mergeSources = new HashSet<UUID>(0);

    private File savePath; // the file to save this dataset to

    private IAnalysisOptions analysisOptions;

    /**
     * Create a dataset from a cell collection. The save file is set as the
     * output folder of the collection
     * 
     * @param collection
     */
    public DefaultAnalysisDataset(ICellCollection collection) {
        this(collection, new File(collection.getOutputFolder(), collection.getName() + Importer.SAVE_FILE_EXTENSION));
    }

    /**
     * Create a dataset from a cell collection, with a defined save file
     * 
     * @param collection
     */
    public DefaultAnalysisDataset(ICellCollection collection, File saveFile) {
        super(collection);
        this.savePath = saveFile;
        this.isRoot = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#duplicate()
     */
    @Override
    public IAnalysisDataset duplicate() throws Exception {
        IAnalysisDataset result = new DefaultAnalysisDataset(cellCollection);

        List<ICell> l = new ArrayList<>();
        for (final ICell c : cellCollection.getCells()) {
            ICell n = new DefaultCell(c);
            l.add(n);
        }
        
        ICellCollection col = result.getCollection();
        for (final ICell c : l) {
            col.addCell(c);
        }

        // TODO: Add child collections, clusters etc

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * analysis.IAnalysisDataset#addChildCollection(components.CellCollection)
     */
    @Override
    public void addChildCollection(@NonNull ICellCollection collection) {

        IAnalysisDataset childDataset;

        if (collection instanceof VirtualCellCollection) {
            childDataset = new ChildAnalysisDataset(this, collection);
        } else {
            childDataset = new DefaultAnalysisDataset(collection, this.savePath);
            childDataset.setRoot(false);

            if(analysisOptions!=null)
                childDataset.setAnalysisOptions(analysisOptions);
        }

        this.childDatasets.add(childDataset);

    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#addChildDataset(analysis.AnalysisDataset)
     */
    @Override
    public void addChildDataset(@NonNull IAnalysisDataset dataset) {
        dataset.setRoot(false);
        this.childDatasets.add(dataset);
    }

    /**
     * Remove the child dataset with the given UUID
     * 
     * @param id
     *            the child ID to be deleted
     */
    private void removeChildCollection(UUID id) {

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

    /**
     * Add the given dataset as an associated dataset. This is not a child, and
     * must be added to an appropriate identifier list; this is handled by the
     * public functions calling this method
     * 
     * @param dataset
     *            the dataset to add
     */
    private void addAssociatedDataset(IAnalysisDataset dataset) {
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset is null");
        }

        this.otherDatasets.add(dataset);
    }

    /**
     * Get the associated dataset with the given id. Not public beacause each
     * associated dataset should have a further classification, and should be
     * retrieved through its own method
     * 
     * @param id
     *            the dataset to get
     * @return the dataset or null
     */
    private IAnalysisDataset getAssociatedDataset(UUID id) {

        for (IAnalysisDataset c : otherDatasets) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Remove the given dataset from the list of parents and any lists that
     * depend on parents
     * 
     * @param id
     *            the UUID to remove
     */
    private void removeAssociatedDataset(UUID id) {

        IAnalysisDataset d = getAssociatedDataset(id);

        if (d != null) {
            otherDatasets.remove(d);

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getSavePath()
     */
    @Override
    public File getSavePath() {
        return savePath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#setSavePath(java.io.File)
     */
    @Override
    public void setSavePath(@NonNull File file) {
        savePath = file;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getChildUUIDs()
     */
    @Override
    public Set<UUID> getChildUUIDs() {
        Set<UUID> result = new HashSet<UUID>(childDatasets.size());
        for (IAnalysisDataset c : childDatasets) {
            result.add(c.getId());
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getAllChildUUIDs()
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getChildDataset(java.util.UUID)
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getMergeSource(java.util.UUID)
     */
    @Override
    public IAnalysisDataset getMergeSource(@NonNull UUID id) {
    	
    	if (this.hasMergeSource(id))
    		return this.getAssociatedDataset(id);

    	for (IAnalysisDataset child : this.getAllMergeSources()) {
    		if (child.getId().equals(id)) {
    			return child;
    		}
    	}
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getAllMergeSources()
     */
    @Override
    public Set<IAnalysisDataset> getAllMergeSources() {

        Set<IAnalysisDataset> result = new HashSet<IAnalysisDataset>();

        for (UUID id : getMergeSourceIDs()) {

            IAnalysisDataset source = this.getAssociatedDataset(id);
            if (source.hasMergeSources()) {
                result.addAll(source.getAllMergeSources());
            } else {
                result.add(source);
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * For default analysis datasets, attempting to add a merge source will
     * create a new virtual collection of cells from the source dataset.
     * 
     * @see analysis.IAnalysisDataset#addMergeSource(analysis.AnalysisDataset)
     */
    @Override
    public void addMergeSource(@NonNull IAnalysisDataset dataset) {

        IAnalysisDataset mergeSource = new MergeSourceAnalysisDataset(this, dataset);
        this.mergeSources.add(mergeSource.getId());
        this.addAssociatedDataset(mergeSource);
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getMergeSources()
     */
    @Override
    public Set<IAnalysisDataset> getMergeSources() {
        Set<IAnalysisDataset> result = new HashSet<IAnalysisDataset>();

        for (UUID id : mergeSources) {
            result.add(this.getAssociatedDataset(id));
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getMergeSourceIDs()
     */
    @Override
    public Set<UUID> getMergeSourceIDs() {
        return this.mergeSources;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getAllMergeSourceIDs()
     */
    @Override
    public Set<UUID> getAllMergeSourceIDs() {

        Set<UUID> result = new HashSet<UUID>();

        for (UUID id : this.getMergeSourceIDs()) {
            result.addAll(getMergeSource(id).getAllMergeSourceIDs());
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#hasMergeSource(java.util.UUID)
     */
    @Override
    public boolean hasMergeSource(UUID id) {
        return mergeSources.contains(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#hasMergeSource(analysis.IAnalysisDataset)
     */
    @Override
    public boolean hasMergeSource(IAnalysisDataset dataset) {
        return this.hasMergeSource(dataset.getId());
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#hasMergeSources()
     */
    @Override
    public boolean hasMergeSources() {
        return !mergeSources.isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getChildCount()
     */
    @Override
    public int getChildCount() {
        return this.childDatasets.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#hasChildren()
     */
    @Override
    public boolean hasChildren() {
        return !childDatasets.isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getChildDatasets()
     */
    @Override
    public Collection<IAnalysisDataset> getChildDatasets() {
        return childDatasets;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getAllChildDatasets()
     */
    @Override
    public List<IAnalysisDataset> getAllChildDatasets() {

        List<IAnalysisDataset> result = new ArrayList<IAnalysisDataset>(childDatasets.size());

        if (!childDatasets.isEmpty()) {
            for (IAnalysisDataset d : childDatasets) {
                result.add(d);
                result.addAll(d.getAllChildDatasets());
            }

        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getCollection()
     */
    @Override
    public ICellCollection getCollection() {
        return this.cellCollection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getAnalysisOptions()
     */
    @Override
    public Optional<IAnalysisOptions> getAnalysisOptions() {
        return Optional.ofNullable(analysisOptions);
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#hasAnalysisOptions()
     */
    @Override
    public boolean hasAnalysisOptions() {
        return analysisOptions != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * analysis.IAnalysisDataset#setAnalysisOptions(analysis.AnalysisOptions)
     */
    @Override
    public void setAnalysisOptions(IAnalysisOptions analysisOptions) {
        this.analysisOptions = analysisOptions;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#refreshClusterGroups()
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#isRoot()
     */
    @Override
    public boolean isRoot() {
        return isRoot;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#setRoot(boolean)
     */
    @Override
    public void setRoot(boolean b) {
        isRoot = b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#deleteChild(java.util.UUID)
     */
    @Override
    public void deleteChild(@NonNull UUID id) {
        if (this.hasChild(id)) {
            this.removeChildCollection(id);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#deleteMergeSource(java.util.UUID)
     */
    @Override
    public void deleteMergeSource(@NonNull UUID id) {
        if (this.mergeSources.contains(id)) {
            this.removeAssociatedDataset(id);
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#updateSourceImageDirectory(java.io.File)
     */
    @Override
    public void updateSourceImageDirectory(@NonNull File expectedImageDirectory) {

        if (!expectedImageDirectory.exists())
            throw new IllegalArgumentException("Requested directory does not exist: " + expectedImageDirectory);

        // Is the name of the expectedImageDirectory the same as the dataset
        // image directory?
        if (!checkName(expectedImageDirectory, this))
            throw new IllegalArgumentException("Dataset name does not match new folder");

        // Does expectedImageDirectory contain image files?
        if (!hasImages(expectedImageDirectory))
            throw new IllegalArgumentException("Target folder contains no images");

        getCollection().setSourceFolder(expectedImageDirectory);

        for (IAnalysisDataset child : this.getAllChildDatasets()) {
            child.getCollection().setSourceFolder(expectedImageDirectory);

        }

        log("Updated image paths to new folder location");
    }

    /**
     * Check that the new image directory has the same name as the old image
     * directory. If the nmd has been copied to the wrong folder, don't update
     * nuclei
     * 
     * @param expectedImageDirectory
     * @param dataset
     * @return
     */
    private boolean checkName(@NonNull File expectedImageDirectory, @NonNull IAnalysisDataset dataset) { 	
       return (dataset.getCollection().getFolder().getName().equals(expectedImageDirectory.getName()));
    }

    /**
     * Check that the given directory contains >0 image files suitable for the
     * morphology analysis
     * 
     * @param expectedImageDirectory
     * @return
     */
    private boolean hasImages(@NonNull File expectedImageDirectory) {

        File[] listOfFiles = expectedImageDirectory.listFiles();

        if (listOfFiles == null)
            return false;

        int result = 0;
        for (File file : listOfFiles) {
            if (ImageImporter.fileIsImportable(file))
                result++;
        }
        return result > 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((analysisOptions == null) ? 0 : analysisOptions.hashCode());
        result = prime * result + ((childDatasets == null) ? 0 : childDatasets.hashCode());
        result = prime * result + ((clusterGroups == null) ? 0 : clusterGroups.hashCode());
        result = prime * result + ((datasetColour == null) ? 0 : datasetColour.hashCode());
        result = prime * result + (isRoot ? 1231 : 1237);
        result = prime * result + ((mergeSources == null) ? 0 : mergeSources.hashCode());
        result = prime * result + ((otherDatasets == null) ? 0 : otherDatasets.hashCode());
        result = prime * result + ((savePath == null) ? 0 : savePath.hashCode());
        result = prime * result + ((cellCollection == null) ? 0 : cellCollection.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultAnalysisDataset other = (DefaultAnalysisDataset) obj;
        if (analysisOptions == null) {
            if (other.analysisOptions != null)
                return false;
        } else if (!analysisOptions.equals(other.analysisOptions))
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
        if (isRoot != other.isRoot)
            return false;
        if (mergeSources == null) {
            if (other.mergeSources != null)
                return false;
        } else if (!mergeSources.equals(other.mergeSources))
            return false;
        if (otherDatasets == null) {
            if (other.otherDatasets != null)
                return false;
        } else if (!otherDatasets.equals(other.otherDatasets))
            return false;
        if (savePath == null) {
            if (other.savePath != null)
                return false;
        } else if (!savePath.equals(other.savePath))
            return false;
        if (cellCollection == null) {
            if (other.cellCollection != null)
                return false;
        } else if (!cellCollection.equals(other.cellCollection))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        in.defaultReadObject();

        if (cellCollection == null)
            warn("No cell collection could be read in dataset");

        IProfileCollection pc = cellCollection.getProfileCollection();
        
        try {
        if (pc == null) {
            warn("Missing profile collection");
        } else {
        	 int length = pc.length();
             // Update all children to have the same profile lengths and offsets

             if (!childDatasets.isEmpty()) {
                 for (IAnalysisDataset child : getAllChildDatasets()) {
                     child.getCollection().getProfileCollection().createProfileAggregate(child.getCollection(), length);
                 }
             }

             if (!otherDatasets.isEmpty()) {
                 for (IAnalysisDataset child : otherDatasets) {
                     child.getCollection().getProfileCollection().createAndRestoreProfileAggregate(child.getCollection());
                 }
             }
        }
        } catch(ProfileException e) {
        	 warn("Unable to update profile aggregates in child datasets");
        	 stack(e);
        }

    }

    private synchronized void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

    }
}
