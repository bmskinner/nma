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

import java.awt.Paint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.utility.Constants;
import com.bmskinner.nuclear_morphology.utility.Version;

/**
 * This is the most primitive information an analysis dataset requires.
 * This does not implement the IAnalysisDataset interface itself - 
 * it is the responsibility of extending classes to add the remaining
 * fields and methods.
 * @author bms41
 * @since 1.13.3
 *
 */
public abstract class AbstractAnalysisDataset implements Serializable {
	
private static final long serialVersionUID = 1L;
	
	protected final Version version;
	
	protected Set<IAnalysisDataset> childDatasets  = new HashSet<IAnalysisDataset>(1); // direct child collections
	
	protected ICellCollection cellCollection;
	
	protected Paint datasetColour = null;
	
	protected List<IClusterGroup> clusterGroups = new ArrayList<IClusterGroup>(0); // hold groups of cluster results
		
	/**
	 * Create a dataset from a cell collection
	 * @param collection
	 */
	protected AbstractAnalysisDataset(ICellCollection collection){
		this.cellCollection = collection;
		this.version        = Version.currentVersion();
	}
	
	
	public Version getVersion() {
		return this.version;
	}
	
	public UUID getUUID() {
		return cellCollection.getID();
	}
	

	public String getName(){
		return cellCollection.getName();
	}
	
	public void setName(String s){
		cellCollection.setName(s);
	}
	
	public void setDatasetColour(Paint colour) {
		datasetColour = colour;
		
	}

	public Paint getDatasetColour() {
		return datasetColour;
	}

	public boolean hasDatasetColour() {
		return datasetColour!=null;
	}
	
	public boolean hasChild(IAnalysisDataset child) {
		return childDatasets.contains(child);
	}
	

	public abstract Set<UUID> getChildUUIDs();
	
	public boolean hasChild(UUID child){
		return this.getChildUUIDs().contains(child);
	}

	public boolean hasRecursiveChild(IAnalysisDataset child) {
		if(hasChild(child)){
			return true;
		}
		for(IAnalysisDataset c : childDatasets){
			if(c.hasRecursiveChild(child)){
				return true;
			}
		}
		return false;
	}
	
	public abstract void deleteClusterGroup(IClusterGroup group);
	

	public void addClusterGroup(IClusterGroup group){
		this.clusterGroups.add(group);
	}
	

	public int getMaxClusterGroupNumber(){
		int number = 0;
		
		if(this.hasClusters()){

			for (IClusterGroup g :  this.getClusterGroups()){

				String name = g.getName();

				Pattern p = Pattern.compile("^"+Constants.CLUSTER_GROUP_PREFIX+"_(\\d+)$");

				Matcher m = p.matcher(name);
				if(m.find()){
					String s = m.group(1);

					int n = Integer.valueOf(s);
					if(n>number){
						number=n;
					}
				}
			}
		}
		return number;
	}


	
	public boolean hasCluster(UUID id){
		
		boolean result = false;
		for(IClusterGroup g : this.clusterGroups){
			if(g.hasDataset(id)){
				result = true;
				break;
			}
		}
		return result;
	}
	

	public List<IClusterGroup> getClusterGroups(){
		return  this.clusterGroups;
	}
	

	public List<UUID> getClusterIDs(){
		List<UUID> result = new ArrayList<UUID>();
		for(IClusterGroup g : this.clusterGroups){
			result.addAll(g.getUUIDs());
		}
		return result;
	}
	

	public boolean hasClusters(){
		if(this.clusterGroups != null && this.clusterGroups.size()>0){
			return true;
		} else {
			return false;
		}
	}
	

	public boolean hasClusterGroup(IClusterGroup group){
		return clusterGroups.contains(group);
	}
	
	

}
