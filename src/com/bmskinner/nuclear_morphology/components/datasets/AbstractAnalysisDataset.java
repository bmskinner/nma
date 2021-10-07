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
import java.io.IOException;
import java.io.PrintWriter;
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
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
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
public abstract class AbstractAnalysisDataset implements Serializable, IAnalysisDataset {

    private static final long serialVersionUID = 1L;

    /**The software version in which the dataset was created */
    protected final Version versionCreated;
    
    /**The software version in which the dataset was saved */
    protected Version versionLastSaved;
    
    /** Direct parent dataset to this dataset */
    protected IAnalysisDataset parentDataset = null;

    /** Direct child datasets to this dataset */
    protected List<IAnalysisDataset> childDatasets = new ArrayList<>();


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
    	
    	if(e.getChild("ChildDatasets")!=null) {
    		for(Element el : e.getChild("ChildDatasets").getChildren()) {
    			childDatasets.add(new VirtualDataset(el));
    		}
    	}
    }
    
	@Override
	public Element toXmlElement() {
		Element e = new Element("AnalysisDataset");
		e.addContent(new Element("VersionCreated").setText(versionCreated.toString()));
		e.addContent(new Element("VersionLastSaved").setText(Version.currentVersion().toString()));
		
		if(datasetColour!=null)
			e.addContent(new Element("Colour").setText(String.valueOf(datasetColour.getRGB())));
		
		if(!childDatasets.isEmpty()) {
			Element el = new Element("ChildDatasets");
			for(IAnalysisDataset c : childDatasets) {
				System.out.println(c.toString());
				el.addContent(c.toXmlElement());
			}
			e.addContent(el);
		}
		
		for(IClusterGroup c : clusterGroups) {
			e.addContent(c.toXmlElement());
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
    public void setAnalysisOptions(IAnalysisOptions analysisOptions) {
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
	public boolean hasDirectChild(IAnalysisDataset child) {
        return hasDirectChild(child.getId());
    }

    @Override
	public abstract Set<UUID> getChildUUIDs();

    @Override
	public boolean hasDirectChild(UUID child) {
        return getChildUUIDs().contains(child);
    }

    @Override
	public boolean hasAnyChild(IAnalysisDataset child) {
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
	public boolean hasCluster(UUID id) {
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
	public boolean hasClusterGroup(IClusterGroup group) {
        return clusterGroups.contains(group);
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
		} else if (!parentDataset.equals(other.parentDataset))
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
