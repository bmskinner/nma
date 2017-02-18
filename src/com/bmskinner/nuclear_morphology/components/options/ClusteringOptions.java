/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.components.options;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.IMutableClusteringOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

public class ClusteringOptions implements IMutableClusteringOptions {

	private static final long serialVersionUID = 1L;
	private ClusteringMethod type;
	private int clusterNumber;
	private HierarchicalClusterMethod hierarchicalMethod;
	private int iterations;
	private boolean autoClusterNumber;
	private boolean includeMesh;
	
	private Map<PlottableStatistic, Boolean> statMap = new HashMap<PlottableStatistic, Boolean>();
	private Map<UUID, Boolean>            segmentMap = new HashMap<UUID, Boolean>(); // which segments should be included
	
	private boolean includeProfile; // should the nuclear profiles be a part of the clustering?
	
	private ProfileType profileType;
	
	private transient boolean useSimilarityMatrix;
	
	/**
	 * Create a new set of options based on the given
	 * method.
	 * @param type
	 */
	public ClusteringOptions(ClusteringMethod type){
		this.type = type;
		for(PlottableStatistic stat : PlottableStatistic.getRoundNucleusStats()){
    		statMap.put(stat, false);
    	}
	}
	
	/**
	 * Copy the options from an existing object
	 * @param oldOptions
	 */
	public ClusteringOptions(IClusteringOptions oldOptions){
		this.type                = oldOptions.getType();
		this.hierarchicalMethod  = oldOptions.getHierarchicalMethod();
		this.useSimilarityMatrix = oldOptions.isUseSimilarityMatrix();
		this.includeProfile      = oldOptions.isIncludeProfile();
		
		for(PlottableStatistic stat : PlottableStatistic.getRoundNucleusStats()){
    		statMap.put(stat, oldOptions.isIncludeStatistic(stat));
    	}
		
		for(UUID i : oldOptions.getSegments()){
			segmentMap.put(i, oldOptions.isIncludeSegment(i));
		}
		
		this.profileType = oldOptions.getProfileType();
		this.includeMesh = oldOptions.isIncludeMesh();
		
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#isIncludeSegment(java.util.UUID)
	 */
	@Override
	public boolean isIncludeSegment(UUID i){
		if( this.segmentMap.containsKey(i)){
			return this.segmentMap.get(i);
		} else {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#useSegments()
	 */
	@Override
	public boolean useSegments(){
		if(this.segmentMap.isEmpty()){
			return false;
		} else {
			return true;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#getSegments()
	 */
	@Override
	public Set<UUID> getSegments(){
		return segmentMap.keySet();
	}
	
	public void setIncludeSegment(UUID id, boolean b){
		this.segmentMap.put(id, b);
	}
		
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#isIncludeStatistic(com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic)
	 */
	@Override
	public boolean isIncludeStatistic(PlottableStatistic stat){
		if( this.statMap.containsKey(stat)){
			return this.statMap.get(stat);
		} else {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#getSavedStatistics()
	 */
	@Override
	public Set<PlottableStatistic> getSavedStatistics(){
		return statMap.keySet();
	}
	
	public void setIncludeStatistic(PlottableStatistic stat, boolean b){
		this.statMap.put(stat, b);
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#isIncludeProfile()
	 */
	@Override
	public boolean isIncludeProfile(){
		return this.includeProfile;
	}
	
	public void setIncludeProfile(boolean b){
		this.includeProfile = b;
	}
	

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#isUseSimilarityMatrix()
	 */
	@Override
	public boolean isUseSimilarityMatrix() {
		return useSimilarityMatrix;
	}




	public void setUseSimilarityMatrix(boolean useSimilarityMatrix) {
		this.useSimilarityMatrix = useSimilarityMatrix;
	}




	/**
	 * Set the clustering method
	 * @param type
	 */
	public void setType(ClusteringMethod type) {
		this.type = type;
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#getType()
	 */
	@Override
	public ClusteringMethod getType() {
		return type;
	}
	
	/**
	 * Set the number of clusters to find automatically
	 * @param autoClusterNumber
	 */
	public void setAutoClusterNumber(boolean autoClusterNumber) {
		this.autoClusterNumber = autoClusterNumber;
	}
	
	/**
	 * Set the number of hierarchical clusters to return.
	 * Has no effect if clustering type is EM
	 * @param clusterNumber
	 */
	public void setClusterNumber(int clusterNumber) {
		this.clusterNumber = clusterNumber;
	}


	public void setHierarchicalMethod(HierarchicalClusterMethod hierarchicalMethod) {
		this.hierarchicalMethod = hierarchicalMethod;
	}

	/**
	 * Set the number of iterations to run an EM clusterer.
	 * Has no effect if type is hierarchical
	 * @param iterations
	 */
	public void setIterations(int iterations) {
		this.iterations = iterations;
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#getClusterNumber()
	 */
	@Override
	public int getClusterNumber() {
		return clusterNumber;
	}


	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#getHierarchicalMethod()
	 */
	@Override
	public HierarchicalClusterMethod getHierarchicalMethod() {
		return hierarchicalMethod;
	}


	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#getIterations()
	 */
	@Override
	public int getIterations() {
		return iterations;
	}
	
	

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#getProfileType()
	 */
	@Override
	public ProfileType getProfileType() {
		return profileType;
	}

	public void setProfileType(ProfileType profileType) {
		this.profileType = profileType;
	}
	
	

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#isIncludeMesh()
	 */
	@Override
	public boolean isIncludeMesh() {
		return includeMesh;
	}

	public void setIncludeMesh(boolean includeMesh) {
		this.includeMesh = includeMesh;
	}

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.options.IClusteringOptions#getOptions()
	 */
	@Override
	public String[] getOptions(){
	
		String[] options = null;
		
		if(this.type.equals(ClusteringMethod.EM)){
			options = new String[2];
			options[0] = "-I";                 // max. iterations
			options[1] = String.valueOf((Integer)iterations);
		}
		
		if(this.type.equals(ClusteringMethod.HIERARCHICAL)){
			options = new String[4];
			options[0] = "-N";                 // number of clusters
			options[1] = String.valueOf((Integer)clusterNumber);
			options[2] = "-L";                 // algorithm
			options[3] = hierarchicalMethod.code();
//			options[4] = "-P"; 				// print Newick Tree
//			options[5] = "";
		}
		
		return options;
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	    in.defaultReadObject();
	    if(statMap==null){
	    	statMap = new HashMap<PlottableStatistic, Boolean>();
	    	for(PlottableStatistic stat : PlottableStatistic.getRoundNucleusStats()){
	    		statMap.put(stat, false);
	    	}
	    }
	    
	    if(segmentMap==null){
	    	segmentMap = new HashMap<UUID, Boolean>();
	    }
	    
	    if(profileType==null){
	    	profileType = ProfileType.ANGLE;
	    }
	    
	    if(Boolean.valueOf(includeMesh)==null){ 
	    	includeMesh = false;
	    }
	}
	
	
	/**
	 * The available types of hierarchical clustering
	 * for the Weka clusterer
	 */
	public enum HierarchicalClusterMethod {
		WARD 			("Ward", "WARD"), 
		SINGLE			("Single", "SINGLE"), 
		COMPLETE		("Complete", "COMPLETE"), 
		AVERAGE			("Average", "AVERAGE"), 
		MEAN			("Mean", "MEAN"),
		CENTROID		("Centroid", "CENTROID"),
		ADJCOMPLETE		("Adjusted complete", "ADJCOMPLETE"),
		NEIGHBOR_JOINING("Neighbour joining", "NEIGHBOR_JOINING");
		
		private final String name;
		private final String code;
		
		HierarchicalClusterMethod(String name, String code){
			this.name = name;
			this.code = code;
		}
		
		public String toString(){
			return this.name;
		}
		
		public String code(){
			return this.code;
		}

	}
	
	/**
	 * The available types of clustering
	 * for the Weka clusterer
	 */
	public enum ClusteringMethod {
		EM ("Expectation maximisation", 0),
		HIERARCHICAL( "Hierarchical",1);


		private final String name;
		private final int code;

		ClusteringMethod(String name, int code){
			this.name = name;
			this.code = code;
		}

		public String toString(){
			return this.name;
		}

		public int code(){
			return this.code;
		}
	}

	@Override
	public IMutableClusteringOptions unlock() {
		return this;
	}

	@Override
	public IClusteringOptions lock() {
		return this;
	}
}
