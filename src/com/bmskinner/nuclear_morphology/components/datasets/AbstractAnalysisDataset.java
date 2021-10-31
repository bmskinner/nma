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

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.options.DefaultAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;

/**
 * This is the most primitive information an analysis dataset requires. This
 * does not implement the IAnalysisDataset interface itself - it is the
 * responsibility of extending classes to add the remaining fields and methods.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public abstract class AbstractAnalysisDataset implements IAnalysisDataset {

    /**The software version in which the dataset was created */
    protected final Version versionCreated;
    
    /**The software version in which the dataset was saved */
    protected Version versionLastSaved;
    
    /** Direct parent dataset to this dataset */
    protected IAnalysisDataset parentDataset = null;

    /** Direct child datasets to this dataset */
    protected List<IAnalysisDataset> childDatasets = new ArrayList<>();
    
    /**
     * Other datasets associated with this dataset, that will need to be saved
     * out, but should not be included in lists fo child datasets. 
     * Includes merge sources presently, with scope for expansion.
     */
    protected List<IAnalysisDataset> otherDatasets = new ArrayList<>();

    /**
     * The ids of datasets merged to create this dataset. The IDs must be
     * present in otherCollections.
     */
    protected Set<UUID> mergeSources = new HashSet<>();

    /** The colour to draw this dataset in charts */
    protected Color datasetColour = null;

    /** Clusters identified in this dataset */
    protected List<IClusterGroup> clusterGroups = new ArrayList<>();
    
    /** Options used to construct this dataset */
    protected IAnalysisOptions analysisOptions = null;
    
    /**
     * Create a new dataset
     */
    protected AbstractAnalysisDataset() {
        this.versionCreated = Version.currentVersion();
        this.versionLastSaved = Version.currentVersion();
    }
    
    protected AbstractAnalysisDataset(@NonNull Element e) throws ComponentCreationException {
    	versionCreated = Version.fromString(e.getChildText("VersionCreated"));
    	versionLastSaved = Version.fromString(e.getChildText("VersionLastSaved"));

    	if(e.getChild("Colour")!=null)
    		datasetColour = Color.decode(e.getChildText("Colour"));

    	for(Element el : e.getChildren("ClusterGroup")) {
    		clusterGroups.add(new DefaultClusterGroup(el));
    	}
    	
    	if(e.getChild("AnalysisOptions")!=null)
    		analysisOptions = new DefaultAnalysisOptions(e.getChild("AnalysisOptions"));
    	
    	if(e.getChild("ChildDatasets")!=null) {
    		for(Element el : e.getChild("ChildDatasets").getChildren()) {
    			VirtualDataset v = new VirtualDataset(el);
    			v.parentDataset = this;
    			childDatasets.add(v);
    		}
    	}
    	
    	if(e.getChild("OtherDatasets")!=null) {
    		for(Element el : e.getChild("OtherDatasets").getChildren()) {
    			otherDatasets.add(new VirtualDataset(el));
    		}
    	}

    	for(Element el : e.getChildren("MergeSource")) {
    		mergeSources.add(UUID.fromString(el.getText()));
    	}
    }
    
    /**
     * Constructor used when copying datasets
     * @param d
     */
    protected AbstractAnalysisDataset(AbstractAnalysisDataset d) {
    	versionCreated = d.versionCreated;
    	versionLastSaved = d.versionLastSaved;
    	
    	if(d.datasetColour!=null)
    		datasetColour = d.datasetColour;
    	    	
    	for(IClusterGroup g : d.clusterGroups)
    		clusterGroups.add(g.duplicate());
    	
    	for(IAnalysisDataset g : d.childDatasets)
    		childDatasets.add(g.copy());
    	
    	for(IAnalysisDataset g : d.otherDatasets)
    		otherDatasets.add(g.copy());
    	
    	mergeSources.addAll(d.mergeSources);
    	
		if(d.analysisOptions!=null)
			analysisOptions = d.analysisOptions.duplicate();
		
		
    }
    
	@Override
	public Element toXmlElement() {		
		Element e = new Element("AnalysisDataset");
		e.addContent(new Element("VersionCreated").setText(versionCreated.toString()));
		e.addContent(new Element("VersionLastSaved").setText(Version.currentVersion().toString()));
		
		if(datasetColour!=null)
			e.addContent(new Element("Colour").setText(String.valueOf(datasetColour.getRGB())));
				
		if(!mergeSources.isEmpty()) {
			for(UUID i : mergeSources)
				e.addContent(new Element("MergeSource").setText(i.toString()));
		}
		
		if(parentDataset!=null)
			e.addContent(new Element("Parent").setText(parentDataset.getId().toString()));
		
		for(IClusterGroup c : clusterGroups) {
			e.addContent(c.toXmlElement());
		}
				
		if(!childDatasets.isEmpty()) {
			Element el = new Element("ChildDatasets");
			for(IAnalysisDataset c : childDatasets) {
				el.addContent(c.toXmlElement());
			}
			e.addContent(el);
		}
		
		if(!otherDatasets.isEmpty()) {
			Element el = new Element("OtherDatasets");
			for(IAnalysisDataset c : childDatasets) {
				el.addContent(c.toXmlElement());
			}
			e.addContent(el);
		}

		return e;
	}
    
    @Override
	public Version getVersionCreated() {
        return this.versionCreated;
    }
    
    @Override
	public Version getVersionLastSaved() {
        return this.versionLastSaved;
    }


        
    @Override
	public void setDatasetColour(Color colour) {
        datasetColour = colour;

    }

    @Override
	public Optional<Color> getDatasetColour() {
        return Optional.ofNullable(datasetColour);
    }

    @Override
	public boolean hasDatasetColour() {
        return datasetColour != null;
    }
    

    @Override
    public Optional<IAnalysisOptions> getAnalysisOptions() {
        return Optional.ofNullable(analysisOptions);
    }
    

    @Override
    public void setAnalysisOptions(@NonNull IAnalysisOptions analysisOptions) {
        this.analysisOptions = analysisOptions;
    }

    @Override
    public boolean hasAnalysisOptions() {
        return analysisOptions != null;
    }
    
    public boolean hasParent() {
    	return parentDataset != null;
    }
    public Optional<IAnalysisDataset> getParent(){
    	return Optional.ofNullable(parentDataset);
    }

    @Override
	public boolean hasDirectChild(@NonNull IAnalysisDataset child) {
        return hasDirectChild(child.getId());
    }

    @Override
	public abstract Set<UUID> getChildUUIDs();

    @Override
	public boolean hasDirectChild(@NonNull UUID child) {
        return getChildUUIDs().contains(child);
    }

    @Override
	public boolean hasAnyChild(@NonNull IAnalysisDataset child) {
        if (hasDirectChild(child)) {
            return true;
        }
        for (IAnalysisDataset c : childDatasets) {
            if (c.hasAnyChild(child)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Given a potential name, avoid conflicts with existing
     * names of this or child datasets by appending a digit
     * @param baseName the name to test
     * @return the name unaltered, or with a non-conflicting suffix.
     */
    protected String chooseSuffix(String baseName) {
    	int appender = 1;
    	boolean isValidName = false;
    	String testName = baseName;
    	
    	while(!isValidName) {
    		testName = baseName+"_"+appender;
    		isValidName = true;
    		if(testName.equals(getName()))
    			isValidName = false;
    		for(IAnalysisDataset d : childDatasets ) {
            	if(d.getName().equals(testName))
            		isValidName = false;
            }
    		appender++;
    	}
    	return testName;
    }
    
    @Override
	public abstract void deleteClusterGroup(@NonNull IClusterGroup group);

    @Override
	public void addClusterGroup(@NonNull IClusterGroup group) {
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

                    int n = Integer.valueOf(s);
                    if (n > number) {
                        number = n;
                    }
                }
            }
        }
        return number;
    }

    @Override
	public boolean hasCluster(@NonNull UUID id) {
        for (IClusterGroup g : this.clusterGroups) {
            if (g.hasDataset(id)) 
            	return true;
        }
        return false;
    }

    @Override
	public List<IClusterGroup> getClusterGroups() {
        return this.clusterGroups;
    }

    @Override
	public List<UUID> getClusterIDs() {
        List<UUID> result = new ArrayList<UUID>();
        for (IClusterGroup g : this.clusterGroups) {
            result.addAll(g.getUUIDs());
        }
        return result;
    }

    @Override
	public boolean hasClusters() {
    	return this.clusterGroups != null && this.clusterGroups.size() > 0;
    }

    @Override
	public boolean hasClusterGroup(@NonNull IClusterGroup group) {
        return clusterGroups.contains(group);
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
     * Remove the given dataset from the associated list
     * 
     * @param id the UUID to remove
     */
    private void removeAssociatedDataset(@NonNull final UUID id) {
        IAnalysisDataset d = getAssociatedDataset(id);
        otherDatasets.remove(d);
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

    @Override
    public void addMergeSource(@NonNull IAnalysisDataset dataset) {
    	VirtualDataset mergeSource = new VirtualDataset(this, dataset.getName(), dataset.getId());
        mergeSource.addAll(dataset.getCollection().getCells());
        
        // May not be present
        if(dataset.hasAnalysisOptions())
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
    public void deleteMergeSource(@NonNull final UUID id) {
        if (this.mergeSources.contains(id)) {
            this.removeAssociatedDataset(id);
        }
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
    public String toString(){
        return getName();
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((childDatasets == null) ? 0 : childDatasets.hashCode());
		result = prime * result + ((clusterGroups == null) ? 0 : clusterGroups.hashCode());
		result = prime * result + ((datasetColour == null) ? 0 : datasetColour.hashCode());
		result = prime * result + ((versionCreated == null) ? 0 : versionCreated.hashCode());
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
		if (parentDataset == null) {
			if (other.parentDataset != null)
				return false;
			// Note - we can't compare datasets directly because the equals is recursive through
			// children
		} else if (!parentDataset.getId().equals(other.parentDataset.getId()))
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
		if (versionCreated == null) {
			if (other.versionCreated != null)
				return false;
		} else if (!versionCreated.equals(other.versionCreated))
			return false;
		if (versionLastSaved == null) {
			if (other.versionLastSaved != null)
				return false;
		} else if (!versionLastSaved.equals(other.versionLastSaved))
			return false;
		return true;
	}
    
    

}
