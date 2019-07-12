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
package com.bmskinner.nuclear_morphology.components;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.Io.Importer;

/**
 * This holds a CellCollection, the analyses that have been run on it and the
 * relationships it holds with other CellCollections. It also provides colour
 * and UI options
 * 
 * @since 1.7.0
 * @deprecated
 *
 */
@Deprecated
public class AnalysisDataset implements IAnalysisDataset {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final long          serialVersionUID = 1L;
    private Map<UUID, AnalysisDataset> childCollections = new HashMap<>();

    // Other datasets associated with this dataset, that will need to be saved
    // out.
    // Includes merge sources. Scope for expansion
    private Map<UUID, AnalysisDataset> otherCollections = new HashMap<>();

    private CellCollection thisCollection;
    private File           savePath;      // the file to save the analysis to

    private IAnalysisOptions analysisOptions;

    private Paint datasetColour = null; // use for colouring the dataset in
                                        // comparison with other datasets

    private List<IClusterGroup> clusterGroups = new ArrayList<>(0); 

    // The ids of datasets merged to create this dataset. The IDs must be
    // present in
    // otherCollections
    private List<UUID> mergeSources = new ArrayList<>(0);

    private File debugFile;

    private final Version version;

    private boolean isRoot = false; // is this a root dataset

    /**
     * Create a dataset from a cell collection. The save file is set as the
     * output folder of the collection
     * 
     * @param collection
     */
    public AnalysisDataset(ICellCollection collection) {
        this(collection, new File(collection.getOutputFolder(), collection.getName() + Importer.SAVE_FILE_EXTENSION));
    }

    /**
     * Create a dataset from a cell collection, with a defined save file
     * 
     * @param collection
     */
    public AnalysisDataset(ICellCollection collection, File saveFile) {
        this.thisCollection = (CellCollection) collection;
        this.savePath = saveFile;

        this.debugFile = Importer.replaceFileExtension(saveFile, Importer.SAVE_FILE_EXTENSION,
                Importer.LOG_FILE_EXTENSION);
        this.isRoot = false;
        this.version = new Version(Version.VERSION_MAJOR, Version.VERSION_MINOR, Version.VERSION_REVISION);
    }

    @Override
    public IAnalysisDataset duplicate() throws Exception {
        IAnalysisDataset result = new AnalysisDataset(this.getCollection());
        for (ICell c : thisCollection.getCells()) {

            result.getCollection().addCell(new DefaultCell(c));
        }
        return result;
    }

    @Override
    public Version getVersion() {
        return this.version;
    }

    @Override
    public void addChildCollection(ICellCollection collection) {
        if (collection == null) {
            throw new IllegalArgumentException("Nucleus collection is null");
        }
        UUID id = collection.getID();
        IAnalysisDataset childDataset = new AnalysisDataset(collection, this.savePath);
        childDataset.setRoot(false);
        if(analysisOptions!=null)
        	childDataset.setAnalysisOptions(analysisOptions);
        this.childCollections.put(id, (AnalysisDataset) childDataset);

    }

    @Override
    public void addChildDataset(IAnalysisDataset dataset) {
        if (dataset == null) {
            throw new IllegalArgumentException("Nucleus collection is null");
        }
        dataset.setRoot(false);
        UUID id = dataset.getId();
        this.childCollections.put(id, (AnalysisDataset) dataset);
    }

    /**
     * Remove the child dataset with the given UUID
     * 
     * @param id
     */
    private void removeChildCollection(UUID id) {
        this.childCollections.remove(id);
        for (IClusterGroup g : clusterGroups) {
            if (g.hasDataset(id)) {
                g.removeDataset(id);
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
        UUID id = dataset.getId();
        this.otherCollections.put(id, (AnalysisDataset) dataset);
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
        return this.otherCollections.get(id);
    }

    /**
     * Remove the given dataset from the list of parents and any lists that
     * depend on parents
     * 
     * @param id
     *            the UUID to remove
     */
    private void removeAssociatedDataset(UUID id) {
        this.otherCollections.remove(id);

    }

    @Override
    public UUID getId() {
        return this.thisCollection.getID();
    }

    @Override
    public String getName() {
        return this.thisCollection.getName();
    }

    @Override
    public void setName(String s) {
        this.thisCollection.setName(s);
    }

    @Override
    public File getSavePath() {
        return this.savePath;
    }

    @Override
    public void setSavePath(File file) {
        this.savePath = file;
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
				Optional<IDetectionOptions> subOptions = op.get().getDetectionOptions(detectedComponent);
				if(subOptions.isPresent())
					subOptions.get().setScale(scale);
			}
		}
		
		for(IAnalysisDataset child : getChildDatasets()) {
			child.setScale(scale);
		}
    }

    @Override
    public Set<UUID> getChildUUIDs() {
        return this.childCollections.keySet();
    }

    @Override
    public Set<UUID> getAllChildUUIDs() {
        Set<UUID> idlist = this.getChildUUIDs();
        Set<UUID> result = new HashSet<>();
        result.addAll(idlist);

        for (UUID id : idlist) {
            IAnalysisDataset d = getChildDataset(id);
            result.addAll(d.getAllChildUUIDs());
        }
        return result;
    }

    @Override
    public IAnalysisDataset getChildDataset(UUID id) {
        if (this.hasChild(id))
            return this.childCollections.get(id);
		for (IAnalysisDataset child : this.getAllChildDatasets()) {
		    if (child.getId().equals(id)) {
		        return child;
		    }
		}
        return null;
    }

    @Override
    public IAnalysisDataset getMergeSource(UUID id) {
        if (this.mergeSources.contains(id)) {
            return this.getAssociatedDataset(id);
        }
		return null;
    }

    @Override
    public Set<IAnalysisDataset> getAllMergeSources() {

        Set<IAnalysisDataset> result = new HashSet<>();

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

    @Override
    public void addMergeSource(IAnalysisDataset dataset) {
        this.mergeSources.add(dataset.getId());
        this.addAssociatedDataset(dataset);
    }

    @Override
    public Set<IAnalysisDataset> getMergeSources() {
        Set<IAnalysisDataset> result = new HashSet<>();

        for (UUID id : mergeSources) {
            result.add(this.getAssociatedDataset(id));
        }
        return result;
    }

    @Override
    public Set<UUID> getMergeSourceIDs() {
        Set<UUID> result = new HashSet<>();
        for (UUID id : mergeSources) {
            result.add(id);
        }
        return result;
    }

    @Override
    public Set<UUID> getAllMergeSourceIDs() {

        Set<UUID> result = new HashSet<>();

        for (UUID id : this.getMergeSourceIDs()) {
            result.addAll(getMergeSource(id).getAllMergeSourceIDs());
        }

        return result;
    }

    @Override
    public boolean hasMergeSource(UUID id) {
    	return mergeSources.contains(id);
    }

    @Override
    public boolean hasMergeSource(IAnalysisDataset dataset) {
        return this.hasMergeSource(dataset.getId());
    }

    @Override
    public boolean hasMergeSources() {
        return !mergeSources.isEmpty();
    }

    @Override
    public int getChildCount() {
        return this.childCollections.size();
    }

    @Override
    public boolean hasChildren() {
    	return !childCollections.isEmpty();
    }

    @Override
    public Collection<IAnalysisDataset> getChildDatasets() {

        List<IAnalysisDataset> result = new ArrayList<>(childCollections.size());

        for (AnalysisDataset d : childCollections.values()) {
            result.add(d);
        }
        return result;

    }

    @Override
    public List<IAnalysisDataset> getAllChildDatasets() {

        List<IAnalysisDataset> result = new ArrayList<>(0);
        for (IAnalysisDataset d : this.getChildDatasets()) { // the direct
                                                             // descendents of
                                                             // this dataset
            result.add(d);

            if (this.hasChildren()) {
                result.addAll(d.getAllChildDatasets());
            }
        }
        return result;
    }

    @Override
    public CellCollection getCollection() {
        return this.thisCollection;
    }

    @Override
    public Optional<IAnalysisOptions> getAnalysisOptions() {
        return Optional.ofNullable(analysisOptions);
    }

    @Override
    public boolean hasAnalysisOptions() {
    	return analysisOptions!=null;
    }

    @Override
    public void setAnalysisOptions(IAnalysisOptions analysisOptions) {
        this.analysisOptions = analysisOptions;
    }

    @Override
    public void addClusterGroup(IClusterGroup group) {
        this.clusterGroups.add(group);
    }

    @Override
    public int getMaxClusterGroupNumber() {
        int number = 0;

        if (this.hasClusters()) {

            for (IClusterGroup g : this.getClusterGroups()) {

                String name = g.getName();

                Pattern p = Pattern.compile("^" + IClusterGroup.CLUSTER_GROUP_PREFIX + "_(\\d+)$");

                Matcher m = p.matcher(name);
                if (m.find()) {
                    String s = m.group(1);

                    int n = Integer.parseInt(s);
                    if (n > number) {
                        number = n;
                    }
                }
            }
        }
        return number;
    }

    @Override
    public boolean hasCluster(UUID id) {

        boolean result = false;
        for (IClusterGroup g : this.clusterGroups) {
            if (g.hasDataset(id)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getClusterGroups()
     */
    @Override
    public List<IClusterGroup> getClusterGroups() {
        return this.clusterGroups;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#getClusterIDs()
     */
    @Override
    public List<UUID> getClusterIDs() {
        List<UUID> result = new ArrayList<>();
        for (IClusterGroup g : this.clusterGroups) {
            result.addAll(g.getUUIDs());
        }
        return result;
    }

    @Override
    public boolean hasClusters() {
       return clusterGroups != null && !clusterGroups.isEmpty();
    }

    @Override
    public boolean hasClusterGroup(IClusterGroup group) {
        return clusterGroups.contains(group);
    }

    @Override
    public void refreshClusterGroups() {

        if (this.hasClusters()) {
            // Find the groups that need removing
            List<IClusterGroup> groupsToDelete = new ArrayList<>();
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

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#isRoot()
     */
    @Override
    public boolean isRoot() {
        return this.isRoot;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#setRoot(boolean)
     */
    @Override
    public void setRoot(boolean b) {
        this.isRoot = b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#deleteChild(java.util.UUID)
     */
    @Override
    public void deleteChild(UUID id) {
        if (this.hasChild(id)) {
            this.removeChildCollection(id);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * analysis.IAnalysisDataset#deleteClusterGroup(components.ClusterGroup)
     */
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
     * @see analysis.IAnalysisDataset#deleteMergeSource(java.util.UUID)
     */
    @Override
    public void deleteMergeSource(UUID id) {
        if (this.mergeSources.contains(id)) {
            this.removeAssociatedDataset(id);
            this.otherCollections.remove(id);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.IAnalysisDataset#hasChild(analysis.IAnalysisDataset)
     */
    @Override
    public boolean hasChild(IAnalysisDataset child) {
        return this.childCollections.containsKey(child.getId());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * analysis.IAnalysisDataset#hasRecursiveChild(analysis.IAnalysisDataset)
     */
    @Override
    public boolean hasRecursiveChild(IAnalysisDataset child) {
        for (IAnalysisDataset d : this.getAllChildDatasets()) {
            if (d.hasChild(child)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasChild(UUID child) {
    	return childCollections.containsKey(child);
    }

    @Override
    public void setDatasetColour(Color colour) {
        this.datasetColour = colour;
    }

    @Override
    public Optional<Color> getDatasetColour() {
        return Optional.ofNullable( (Color)this.datasetColour);
    }

    @Override
    public boolean hasDatasetColour() {
    	return getDatasetColour().isPresent();
    }

    @Override
    public String toString() {
        return this.getName();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    @Override
    public void updateSourceImageDirectory(File expectedImageDirectory) {

        LOGGER.fine("Searching " + expectedImageDirectory.getAbsolutePath());

        if (!expectedImageDirectory.exists()) {
            throw new IllegalArgumentException("Requested directory does not exist: " + expectedImageDirectory);
        }

        // Is the name of the expectedImageDirectory the same as the dataset
        // image directory?
        if (!checkName(expectedImageDirectory, this)) {
            throw new IllegalArgumentException("Dataset name does not match new folder; unable to update paths");
        }
        LOGGER.fine("Dataset name matches new folder");

        // Does expectedImageDirectory contain image files?
        if (!checkHasImages(expectedImageDirectory)) {
            throw new IllegalArgumentException("Target folder contains no images; unable to update paths");
        }

        LOGGER.fine("Target folder contains at least one image");

        LOGGER.fine("Updating dataset image paths");
        this.getCollection().setSourceFolder(expectedImageDirectory);

        LOGGER.fine("Updating child dataset image paths");
        for (IAnalysisDataset child : this.getAllChildDatasets()) {
             child.getCollection().setSourceFolder(expectedImageDirectory);
        }

        LOGGER.info("Updated image paths to new folder location");
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
    private boolean checkName(File expectedImageDirectory, IAnalysisDataset dataset) {
         return dataset.getCollection().getFolder().getName().equals(expectedImageDirectory.getName());
    }

    /**
     * Check that the given directory contains >0 image files suitable for the
     * morphology analysis
     * 
     * @param expectedImageDirectory
     * @return
     */
    private boolean checkHasImages(File expectedImageDirectory) {

        if (expectedImageDirectory == null) {
            throw new IllegalArgumentException("Folder cannot be null");
        }

        File[] listOfFiles = expectedImageDirectory.listFiles();

        int result = 0;

        if (listOfFiles == null) {
            return false;
        }

        for (File file : listOfFiles) {

            boolean ok = ImageImporter.fileIsImportable(file);

            if (ok) {
                result++;
            }
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
        int result = 1;
        result = prime * result + ((analysisOptions == null) ? 0 : analysisOptions.hashCode());
        result = prime * result + ((childCollections == null) ? 0 : childCollections.hashCode());
        result = prime * result + ((clusterGroups == null) ? 0 : clusterGroups.hashCode());
        result = prime * result + ((datasetColour == null) ? 0 : datasetColour.hashCode());
        result = prime * result + ((debugFile == null) ? 0 : debugFile.hashCode());
        result = prime * result + (isRoot ? 1231 : 1237);
        result = prime * result + ((mergeSources == null) ? 0 : mergeSources.hashCode());
        result = prime * result + ((otherCollections == null) ? 0 : otherCollections.hashCode());
        result = prime * result + ((savePath == null) ? 0 : savePath.hashCode());
        result = prime * result + ((thisCollection == null) ? 0 : thisCollection.hashCode());
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
        AnalysisDataset other = (AnalysisDataset) obj;
        if (analysisOptions == null) {
            if (other.analysisOptions != null)
                return false;
        } else if (!analysisOptions.equals(other.analysisOptions))
            return false;
        if (childCollections == null) {
            if (other.childCollections != null)
                return false;
        } else if (!childCollections.equals(other.childCollections))
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
        if (debugFile == null) {
            if (other.debugFile != null)
                return false;
        } else if (!debugFile.equals(other.debugFile))
            return false;
        if (isRoot != other.isRoot)
            return false;
        if (mergeSources == null) {
            if (other.mergeSources != null)
                return false;
        } else if (!mergeSources.equals(other.mergeSources))
            return false;
        if (otherCollections == null) {
            if (other.otherCollections != null)
                return false;
        } else if (!otherCollections.equals(other.otherCollections))
            return false;
        if (savePath == null) {
            if (other.savePath != null)
                return false;
        } else if (!savePath.equals(other.savePath))
            return false;
        if (thisCollection == null) {
            if (other.thisCollection != null)
                return false;
        } else if (!thisCollection.equals(other.thisCollection))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

}
