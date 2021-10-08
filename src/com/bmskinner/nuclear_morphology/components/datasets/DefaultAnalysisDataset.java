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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.DefaultAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileCollection;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

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
	
	private static final Logger LOGGER = Logger.getLogger(DefaultAnalysisDataset.class.getName());

    private static final long serialVersionUID = 1L;

    private boolean isRoot = false; // is this a root dataset
    

    /** The cell collection for this dataset */
    protected ICellCollection cellCollection;

    /**
     * Other datasets associated with this dataset, that will need to be saved
     * out. Includes merge sources presently, with scope for expansion
     */
    private Set<IAnalysisDataset> otherDatasets = new HashSet<>();

    /**
     * The ids of datasets merged to create this dataset. The IDs must be
     * present in otherCollections.
     */
    private Set<UUID> mergeSources = new HashSet<>();

    private File savePath; // the file to save this dataset to

    /**
     * Create a dataset from a cell collection, with a defined save file
     * 
     * @param collection
     */
    public DefaultAnalysisDataset(@NonNull ICellCollection collection, @NonNull File saveFile) {
        super();
        this.cellCollection = collection;
        this.savePath = saveFile;
        this.isRoot = false;
    }
    
    public DefaultAnalysisDataset(@NonNull Element e) throws ComponentCreationException {
    	super(e);
    	isRoot = Boolean.valueOf(e.getChildText("IsRoot"));
    	
    	for(Element el : e.getChild("OtherDatasets").getChildren()) {
    		otherDatasets.add(new VirtualDataset(el));
    	}
    	
    	for(Element el : e.getChildren("MergeSource")) {
    		mergeSources.add(UUID.fromString(el.getText()));
    	}
    	
    	savePath = new File(e.getChildText("SaveFile")).getAbsoluteFile();
    	
    	analysisOptions = new DefaultAnalysisOptions(e.getChild("AnalysisOptions"));
    	
    	cellCollection = new DefaultCellCollection(e.getChild("CellCollection"));		
    }
    
    /**
     * Constructor used for duplicating datasets
     * @param d the template dataset
     * @throws Exception 
     */
    private DefaultAnalysisDataset(DefaultAnalysisDataset d) {
    	super(d);
    	isRoot = d.isRoot;
    	cellCollection = d.cellCollection.duplicate();
    	
    	for(IAnalysisDataset g : d.otherDatasets)
    		otherDatasets.add(g.copy());
    	
    	mergeSources.addAll(d.mergeSources);
    	
    	savePath = new File(d.savePath.getAbsolutePath());

    }

    @Override
	public Element toXmlElement() {
		Element e = super.toXmlElement();
		
		e.addContent(new Element("IsRoot").setText(String.valueOf(isRoot)));
		
		e.addContent(cellCollection.toXmlElement());
		
		Element other = new Element("OtherDatasets");
		for(IAnalysisDataset c : otherDatasets) {
			other.addContent(c.toXmlElement());
		}
		e.addContent(other);
		
		for(UUID i : mergeSources)
			e.addContent(new Element("MergeSource").setText(i.toString()));
		
		e.addContent(new Element("SaveFile").setText(savePath.getPath()));
		
		e.addContent(analysisOptions.toXmlElement());
		
		return e;
	}


    @Override
	public IAnalysisDataset copy() {
    	return new DefaultAnalysisDataset(this);
    }
    
    @Override
	public UUID getId() {
        return cellCollection.getId();
    }

    @Override
	public String getName() {
        return cellCollection.getName();
    }

    @Override
	public void setName(@NonNull String s) {
        cellCollection.setName(s);
    }

    @Override
    public void addChildCollection(@NonNull final ICellCollection collection) {
    	VirtualDataset v = new VirtualDataset(this, collection.getName(), collection.getId());
    	v.addAll(collection.getCells());
    	addChildDataset(v);
    }

    @Override
    public void addChildDataset(@NonNull final IAnalysisDataset dataset) {

        if(dataset instanceof VirtualDataset) {
        	// Ensure no duplicate dataset names
        	// If the name is the same as this dataset, or one of the child datasets, 
        	// apply a suffix
        	if(getName().equals(dataset.getName()) || 
        			childDatasets.stream().map(IAnalysisDataset::getName).anyMatch(s->s.equals(dataset.getName()))) {
        		String newName = chooseSuffix(dataset.getName());
        		dataset.setName(newName);
        	}
        }
    	childDatasets.add(dataset);
    }
    
    /**
     * Remove the child dataset with the given UUID
     * 
     * @param id the child ID to be deleted
     */
    private void removeChildCollection(UUID id) {
    	    	
    	childDatasets.removeIf(c->c.getId().equals(id));
    	
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
    private void addAssociatedDataset(@NonNull final IAnalysisDataset dataset) {
        otherDatasets.add(dataset);
    }

    /**
     * Get the associated dataset with the given id. Not public because each
     * associated dataset should have a further classification, and should be
     * retrieved through its own method
     * 
     * @param id the dataset to get
     * @return the dataset or null
     */
    private IAnalysisDataset getAssociatedDataset(@NonNull final UUID id) {
    	return otherDatasets.stream().filter(d->d.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Remove the given dataset from the list of parents and any lists that
     * depend on parents
     * 
     * @param id the UUID to remove
     */
    private void removeAssociatedDataset(@NonNull final UUID id) {
        IAnalysisDataset d = getAssociatedDataset(id);
        otherDatasets.remove(d);
    }

    @Override
    public File getSavePath() {
        return savePath;
    }


    @Override
    public void setSavePath(@NonNull final File file) {
        savePath = file;
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
    	return childDatasets.stream().map(IAnalysisDataset::getId).collect(Collectors.toSet());
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
    public IAnalysisDataset getChildDataset(@NonNull final UUID id) {
        if (this.hasDirectChild(id)) {
            for (IAnalysisDataset c : childDatasets) {
                if (c.getId().equals(id))
                    return c;
            }

        } else {
            for (IAnalysisDataset child : this.getAllChildDatasets()) {
                if (child.getId().equals(id))
                    return child;
            }
        }
        return null;
    }

    @Override
    public IAnalysisDataset getMergeSource(@NonNull final UUID id) {
    	
    	if (this.hasMergeSource(id))
    		return this.getAssociatedDataset(id);

    	for (IAnalysisDataset child : this.getAllMergeSources()) {
    		if (child.getId().equals(id))
    			return child;
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
    	VirtualDataset mergeSource = new VirtualDataset(this, dataset.getName(), dataset.getId());
        mergeSource.addAll(dataset.getCollection().getCells());
        mergeSource.setAnalysisOptions(dataset.getAnalysisOptions().get());
        this.mergeSources.add(mergeSource.getId());
        this.addAssociatedDataset(mergeSource);
    }

    @Override
    public List<IAnalysisDataset> getMergeSources() {
    	List<IAnalysisDataset> result = new ArrayList<>();
        for (UUID id : mergeSources) 
            result.add(this.getAssociatedDataset(id));
        return result;
    }

    @Override
    public Set<UUID> getMergeSourceIDs() {
        return this.mergeSources;
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
    public boolean hasMergeSource(@NonNull final UUID id) {
        return mergeSources.contains(id);
    }


    @Override
    public boolean hasMergeSource(@NonNull IAnalysisDataset dataset) {
        return this.hasMergeSource(dataset.getId());
    }

    @Override
    public boolean hasMergeSources() {
        return !mergeSources.isEmpty();
    }

    @Override
    public int getChildCount() {
        return this.childDatasets.size();
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
        return this.cellCollection;
    }

    @Override
    public void refreshClusterGroups() {

        if (this.hasClusters()) {
            // Find the groups that need removing
            List<IClusterGroup> groupsToDelete = new ArrayList<>();
            for (IClusterGroup g : this.getClusterGroups()) {
                boolean clusterRemains = false;

                for (UUID childID : g.getUUIDs()) {
                    if (this.hasDirectChild(childID))
                        clusterRemains = true;
                }
                if (!clusterRemains) 
                    groupsToDelete.add(g);
            }

            // Remove the groups
            for (IClusterGroup g : groupsToDelete)
                this.deleteClusterGroup(g);
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
    		IClusterGroup g = clusterGroups.stream().filter(group->group.getId().equals(id)).findFirst().get();
    		deleteClusterGroup(g);
    	}
    }

    @Override
    public boolean isRoot() {
        return isRoot;
    }

    @Override
    public void setRoot(boolean b) {
        isRoot = b;
    }

    @Override
    public void deleteChild(@NonNull UUID id) {
        if (this.hasDirectChild(id)) {
            this.removeChildCollection(id);
        }
    }

    @Override
    public void deleteMergeSource(@NonNull final UUID id) {
        if (this.mergeSources.contains(id)) {
            this.removeAssociatedDataset(id);
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public void updateSourceImageDirectory(@NonNull final File expectedImageDirectory) {

    	if (!expectedImageDirectory.exists()) {
    		LOGGER.warning(String.format("Requested directory '%s' does not exist",  expectedImageDirectory));
    		return;
    	}

    	// Is the name of the expectedImageDirectory the same as the dataset
    	// image directory?
    	// Update the analysis options
        if(analysisOptions==null) {
        	LOGGER.warning("No analysis options to contain image folder");
        	return;
        }
        
    	Optional<HashOptions> nucleusOptions = analysisOptions
    			.getDetectionOptions(CellularComponent.NUCLEUS);
    	
    	if(!nucleusOptions.isPresent()) {
    		LOGGER.warning("No nucleus detection options to contain image folder");
    		return;
    	}
    	
    	// Check that the folders have the same name - if the files have
    	// just been copied between computers, this should be true
    	String filePath = nucleusOptions.get().getString(HashOptions.DETECTION_FOLDER);
    	String expectedName = new File(filePath).getName();

    	if (!expectedImageDirectory.getName().equals(expectedName)) {
    		LOGGER.warning(String.format("Caution: Existing dataset folder '%s' does not match new folder name '%s'",
    				expectedName, expectedImageDirectory.getName()));
    	}

    	// Does expectedImageDirectory contain image files?
    	if (!hasImages(expectedImageDirectory)) {
    		LOGGER.warning("Target folder contains no images");
    		return;
    	}

    	 // Update the old storage location
        getCollection().setSourceFolder(expectedImageDirectory);
        
        // Update the analysis options
        nucleusOptions.get().setString(HashOptions.DETECTION_FOLDER, expectedImageDirectory.getAbsolutePath());

        
        //TODO add unit tests that this completes correctly
        
        for (IAnalysisDataset child : this.getAllChildDatasets()) {
            child.getCollection().setSourceFolder(expectedImageDirectory);
        }

        LOGGER.info("Updated image paths to new folder location");
    }

    /**
     * Check that the given directory contains >0 image files suitable for the
     * morphology analysis
     * 
     * @param expectedImageDirectory
     * @return
     */
    private boolean hasImages(@NonNull final File expectedImageDirectory) {

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
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((analysisOptions == null) ? 0 : analysisOptions.hashCode());
        result = prime * result + (isRoot ? 1231 : 1237);
        result = prime * result + ((mergeSources == null) ? 0 : mergeSources.hashCode());
        result = prime * result + ((otherDatasets == null) ? 0 : otherDatasets.hashCode());
        result = prime * result + ((savePath == null) ? 0 : savePath.hashCode());
        result = prime * result + ((cellCollection == null) ? 0 : cellCollection.hashCode());
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
        return true;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        in.defaultReadObject();

        IProfileCollection pc = cellCollection.getProfileCollection();
        
        try {
        if (pc == null) {
            LOGGER.warning("Missing profile collection");
        } else {
        	 int length = pc.length();
             // Update all children to have the same profile lengths and offsets
             if (!childDatasets.isEmpty()) {
                 for (IAnalysisDataset child : getAllChildDatasets())
                     child.getCollection().getProfileCollection().createProfileAggregate(child.getCollection(), length);
             }

             // Allow merge sources to retain their original lengths
             if (!otherDatasets.isEmpty()) {
                 for (IAnalysisDataset child : otherDatasets)
                     child.getCollection().getProfileCollection().createAndRestoreProfileAggregate(child.getCollection());
             }
        }
        } catch(ProfileException e) {
        	 LOGGER.warning("Unable to update profile aggregates in child datasets");
        	 LOGGER.log(Loggable.STACK, e.getMessage(), e);
        }
    }
    
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    	// Ensure the save version is correct at time of save 
    	this.versionLastSaved = Version.currentVersion();
    	out.defaultWriteObject();
    }

}
