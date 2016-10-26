/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

package components.active;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logging.DebugFileFormatter;
import logging.DebugFileHandler;
import utility.Constants;
import utility.Version;
import components.Cell;
import components.ClusterGroup;
import components.ICell;
import components.ICellCollection;
import analysis.AnalysisOptions;
import analysis.IAnalysisDataset;
import analysis.nucleus.NucleusDetectionWorker;

/**
 * This is the replacement analysis dataset designed to use less memory from
 * versions 1.14.0 onwards. The first field in the object is the version, allowing
 * deserialisation to choose an appropriate path in the future.
 * @author bms41
 *
 */
public class DefaultAnalysisDataset implements IAnalysisDataset {
	
	private static final long serialVersionUID = 1L;
	
	private final Version version;
	
	private boolean isRoot = false;	// is this a root dataset
	
	private Set<IAnalysisDataset> childDatasets  = new HashSet<IAnalysisDataset>(); // hold direct child collections
	
	/**
	 * Other datasets associated with this dataset, that will need to be saved out.
	 * Includes merge sources presently, with scope for expansion
	 */
	private Set<IAnalysisDataset> otherDatasets = new HashSet<IAnalysisDataset>();
	

	/**
	 * The ids of datasets merged to create this dataset. 
	 * The IDs must be present in otherCollections.
	 */
	private List<UUID> mergeSources	  = new ArrayList<UUID>(0);
	
	private ICellCollection thisCollection;
	
	private File savePath; // the file to save this dataset to
	
	private AnalysisOptions analysisOptions; // the setup for this analysis
	
	private Color datasetColour = null; // use for colouring the dataset in comparison with other datasets
	
	private List<ClusterGroup> clusterGroups = new ArrayList<ClusterGroup>(0); // hold groups of cluster results
	

	/*
	 * TRANSIENT FIELDS
	 */
	
	private transient File debugFile;
	
		

		
	/**
	 * Create a dataset from a cell collection. The save file is
	 * set as the output folder of the collection
	 * @param collection
	 */
	public DefaultAnalysisDataset(ICellCollection collection){
		this(collection, new File(collection.getOutputFolder()
				+File.separator
				+collection.getName()
				+Constants.SAVE_FILE_EXTENSION));
	}
	
	/**
	 * Create a dataset from a cell collection, with a defined
	 * save file
	 * @param collection
	 */
	public DefaultAnalysisDataset(ICellCollection collection, File saveFile){
		this.thisCollection = collection;
		this.savePath       = saveFile;
		
		this.debugFile      = new File(saveFile.getParent()
				                        + File.separator
				                        + saveFile.getName().replace(Constants.SAVE_FILE_EXTENSION, Constants.LOG_FILE_EXTENSION));
		this.isRoot         = false;
		this.version        = Version.currentVersion();
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#duplicate()
	 */
	@Override
	public IAnalysisDataset duplicate() throws Exception {
		IAnalysisDataset result = new DefaultAnalysisDataset(thisCollection);
		
		for(ICell c : thisCollection.getCells()){
	
			result.getCollection().addCell(new Cell(c));
		}
		
//		TODO: Add child collections, clusters etc
		
		return result;
	}
		
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getLogHandler()
	 */
	@Override
	public Handler getLogHandler() throws Exception {

		if(debugFile == null || !debugFile.exists()){
			return null;
		}
		
		Handler fileHandler = new DebugFileHandler(this.getDebugFile());
		fileHandler.setFormatter(new DebugFileFormatter());

		return fileHandler;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getVersion()
	 */
	@Override
	public Version getVersion(){
		return this.version;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#addChildCollection(components.CellCollection)
	 */
	@Override
	public void addChildCollection(ICellCollection collection){
		if(collection==null){
			throw new IllegalArgumentException("Nucleus collection is null");
		}

		IAnalysisDataset childDataset = new DefaultAnalysisDataset(collection, this.savePath);
		childDataset.setRoot(false);
		childDataset.setAnalysisOptions(this.getAnalysisOptions());
		this.childDatasets.add( childDataset);

	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#addChildDataset(analysis.AnalysisDataset)
	 */
	@Override
	public void addChildDataset(IAnalysisDataset dataset){
		if(dataset==null){
			throw new IllegalArgumentException("Nucleus collection is null");
		}
		dataset.setRoot(false);
		this.childDatasets.add(dataset);
	}
	
	/**
	 * Remove the child dataset with the given UUID
	 * @param id
	 */
	private void removeChildCollection(UUID id){
		
		IAnalysisDataset child = null;
		
		for(IAnalysisDataset c : childDatasets){
			if(c.getUUID().equals(id)){
				child = c;
				break;
			}
		}
		
		if(child != null){
			this.childDatasets.remove(id);
			for(ClusterGroup g : clusterGroups){
				if(g.hasDataset(id)){
					g.removeDataset(id);
				}
			}
		}

	}
	
	
	/**
	 * Add the given dataset as an associated dataset.
	 * This is not a child, and must be added to an 
	 * appropriate identifier list; this is handled by
	 * the public functions calling this method
	 * @param dataset the dataset to add
	 */
	private void addAssociatedDataset(IAnalysisDataset dataset){
		if(dataset==null){
			throw new IllegalArgumentException("Dataset is null");
		}

		this.otherDatasets.add(dataset);
	}
	
	/**
	 * Get the associated dataset with the given id. Not public
	 * beacause each associated dataset should have a further
	 * classification, and should be retrieved through its own
	 * method
	 * @param id the dataset to get
	 * @return the dataset or null
	 */
	private IAnalysisDataset getAssociatedDataset(UUID id){
		
		for(IAnalysisDataset c : otherDatasets){
			if(c.getUUID().equals(id)){
				return c;
			}
		}
		return null;
	}
	
	/**
	 * Remove the given dataset from the list of parents
	 * and any lists that depend on parents
	 * @param id the UUID to remove
	 */
	private void removeAssociatedDataset(UUID id){
		
		IAnalysisDataset d = getAssociatedDataset(id);
		
		if( d != null){
			otherDatasets.remove(d);

		}
	}
	
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getUUID()
	 */
	@Override
	public UUID getUUID(){
		return thisCollection.getID();
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getName()
	 */
	@Override
	public String getName(){
		return thisCollection.getName();
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#setName(java.lang.String)
	 */
	@Override
	public void setName(String s){
		thisCollection.setName(s);
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getSavePath()
	 */
	@Override
	public File getSavePath(){
		return savePath;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#setSavePath(java.io.File)
	 */
	@Override
	public void setSavePath(File file){
		savePath = file;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getDebugFile()
	 */
	@Override
	public File getDebugFile(){
		return debugFile;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#setDebugFile(java.io.File)
	 */
	@Override
	public void setDebugFile(File f){
		try {
			if(!f.exists()){
				f.createNewFile();
			}
			if(f.canWrite()){
				this.debugFile = f;
			}
		} catch (IOException e) {
			warn("Unable to update debug file location");
			fine("IO error setting file location", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getChildUUIDs()
	 */
	@Override
	public Set<UUID> getChildUUIDs(){
		Set<UUID> result = new HashSet<UUID>(childDatasets.size());
		for(IAnalysisDataset c : childDatasets){
			result.add(c.getUUID());
		}
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getAllChildUUIDs()
	 */
	@Override
	public Set<UUID> getAllChildUUIDs(){
		
		Set<UUID> result = new HashSet<UUID>();
		
		Set<UUID> idlist = getChildUUIDs();
		result.addAll(idlist);
		
		for(UUID id : idlist){
			IAnalysisDataset d = getChildDataset(id);
			
			result.addAll(d.getAllChildUUIDs());
		}
		return result;
	}
	
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getChildDataset(java.util.UUID)
	 */
	@Override
	public IAnalysisDataset getChildDataset(UUID id){
		if(this.hasChild(id)){
			
			for(IAnalysisDataset c : otherDatasets){
				if(c.getUUID().equals(id)){
					return c;
				}
			}
			
		} else {
			for(IAnalysisDataset child : this.getAllChildDatasets()){
				if(child.getUUID().equals(id)){
					return child;
				}
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getMergeSource(java.util.UUID)
	 */
	@Override
	public IAnalysisDataset getMergeSource(UUID id){
		if(this.mergeSources.contains(id)){
		return this.getAssociatedDataset(id);
		} else {
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getAllMergeSources()
	 */
	@Override
	public List<IAnalysisDataset> getAllMergeSources(){
		
		List<IAnalysisDataset>  result = new ArrayList<IAnalysisDataset>();
		
		for(UUID id : getMergeSourceIDs()){
			
			IAnalysisDataset source = this.getAssociatedDataset(id);
			if(source.hasMergeSources()){
				result.addAll(source.getAllMergeSources());
			} else {
				result.add(source);
			}
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#addMergeSource(analysis.AnalysisDataset)
	 */
	@Override
	public void addMergeSource(IAnalysisDataset dataset){
		this.mergeSources.add(dataset.getUUID());
		this.addAssociatedDataset(dataset);
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getMergeSources()
	 */
	@Override
	public List<IAnalysisDataset> getMergeSources(){
		List<IAnalysisDataset>  result = new ArrayList<IAnalysisDataset>();
		
		for(UUID id : mergeSources){
			result.add(this.getAssociatedDataset(id));	
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getMergeSourceIDs()
	 */
	@Override
	public List<UUID> getMergeSourceIDs(){
		return this.mergeSources;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getAllMergeSourceIDs()
	 */
	@Override
	public List<UUID> getAllMergeSourceIDs(){
		
		List<UUID> result = new ArrayList<UUID>();
		
		for(UUID id : this.getMergeSourceIDs()){
			result.addAll(getMergeSource(id).getAllMergeSourceIDs());
		}

		return result;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hasMergeSource(java.util.UUID)
	 */
	@Override
	public boolean hasMergeSource(UUID id){
		return mergeSources.contains(id);
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hasMergeSource(analysis.IAnalysisDataset)
	 */
	@Override
	public boolean hasMergeSource(IAnalysisDataset dataset){
		return this.hasMergeSource(dataset.getUUID());
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hasMergeSources()
	 */
	@Override
	public boolean hasMergeSources(){
		return !mergeSources.isEmpty();
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getChildCount()
	 */
	@Override
	public int getChildCount(){
		return this.childDatasets.size();
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hasChildren()
	 */
	@Override
	public boolean hasChildren(){
		return !childDatasets.isEmpty();
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getChildDatasets()
	 */
	@Override
	public Collection<IAnalysisDataset> getChildDatasets(){
		return childDatasets;			
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getAllChildDatasets()
	 */
	@Override
	public List<IAnalysisDataset> getAllChildDatasets(){

		List<IAnalysisDataset> result = new ArrayList<IAnalysisDataset>(childDatasets.size());
		for(IAnalysisDataset d : this.getChildDatasets()){ // the direct descendents of this dataset
			result.add(d);
			
			if(d.hasChildren()){
				result.addAll(d.getAllChildDatasets());
			}
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getCollection()
	 */
	@Override
	public ICellCollection getCollection(){
		return this.thisCollection;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getAnalysisOptions()
	 */
	@Override
	public AnalysisOptions getAnalysisOptions() {
		return analysisOptions;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hasAnalysisOptions()
	 */
	@Override
	public boolean hasAnalysisOptions(){
		return analysisOptions!=null;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#setAnalysisOptions(analysis.AnalysisOptions)
	 */
	@Override
	public void setAnalysisOptions(AnalysisOptions analysisOptions) {
		this.analysisOptions = analysisOptions;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#addClusterGroup(components.ClusterGroup)
	 */
	@Override
	public void addClusterGroup(ClusterGroup group){
		this.clusterGroups.add(group);
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getMaxClusterGroupNumber()
	 */
	@Override
	public int getMaxClusterGroupNumber(){
		int number = 0;
		
		if(this.hasClusters()){

			for (ClusterGroup g :  this.getClusterGroups()){

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


	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hasCluster(java.util.UUID)
	 */
	@Override
	public boolean hasCluster(UUID id){
		
		boolean result = false;
		for(ClusterGroup g : this.clusterGroups){
			if(g.hasDataset(id)){
				result = true;
				break;
			}
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getClusterGroups()
	 */
	@Override
	public List<ClusterGroup> getClusterGroups(){
		return  this.clusterGroups;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getClusterIDs()
	 */
	@Override
	public List<UUID> getClusterIDs(){
		List<UUID> result = new ArrayList<UUID>();
		for(ClusterGroup g : this.clusterGroups){
			result.addAll(g.getUUIDs());
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hasClusters()
	 */
	@Override
	public boolean hasClusters(){
		if(this.clusterGroups != null && this.clusterGroups.size()>0){
			return true;
		} else {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hasClusterGroup(components.ClusterGroup)
	 */
	@Override
	public boolean hasClusterGroup(ClusterGroup group){
		return clusterGroups.contains(group);
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#refreshClusterGroups()
	 */
	@Override
	public void refreshClusterGroups(){

		if(this.hasClusters()){
			// Find the groups that need removing
			List<ClusterGroup> groupsToDelete = new ArrayList<ClusterGroup>();
			for(ClusterGroup g : this.getClusterGroups()){
				boolean clusterRemains = false;

				for(UUID childID : g.getUUIDs()){
					if(this.hasChild(childID)){
						clusterRemains = true;
					}
				}
				if(!clusterRemains){
					groupsToDelete.add(g);
				}
			}
			
			// Remove the groups
			for(ClusterGroup g : groupsToDelete){
				this.deleteClusterGroup(g);
			}
			
			
		}
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#isRoot()
	 */
	@Override
	public boolean isRoot(){
		return isRoot;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#setRoot(boolean)
	 */
	@Override
	public void setRoot(boolean b){
		isRoot = b;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#deleteChild(java.util.UUID)
	 */
	@Override
	public void deleteChild(UUID id){
		if(this.hasChild(id)){
			this.removeChildCollection(id);
		}
	}
		
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#deleteClusterGroup(components.ClusterGroup)
	 */
	@Override
	public void deleteClusterGroup(ClusterGroup group){
		
		if(hasClusterGroup(group)){

			for(UUID id : group.getUUIDs()){
				if(hasChild(id)){
					this.deleteChild(id);
				}
			}
			this.clusterGroups.remove(group);
		}
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#deleteMergeSource(java.util.UUID)
	 */
	@Override
	public void deleteMergeSource(UUID id){
		if(this.mergeSources.contains(id)){
			this.removeAssociatedDataset(id);
		}
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hasChild(analysis.IAnalysisDataset)
	 */
	@Override
	public boolean hasChild(IAnalysisDataset child){
		return childDatasets.contains(child);
	}
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hasRecursiveChild(analysis.IAnalysisDataset)
	 */
	@Override
	public boolean hasRecursiveChild(IAnalysisDataset child){
		for(IAnalysisDataset d : this.getAllChildDatasets()){
			if(d.hasChild(child)){
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hasChild(java.util.UUID)
	 */
	@Override
	public boolean hasChild(UUID child){
		return this.getChildUUIDs().contains(child);
	}
			
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#setDatasetColour(java.awt.Color)
	 */
	@Override
	public void setDatasetColour(Color colour){
		this.datasetColour = colour;
	}
	
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#getDatasetColour()
	 */
	@Override
	public Color getDatasetColour(){
		return this.datasetColour;
	}
	
	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hasDatasetColour()
	 */
	@Override
	public boolean hasDatasetColour(){
		return this.getDatasetColour()!=null;
	}

	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#toString()
	 */
//	public ColourSwatch getSwatch() {
//		return swatch;
//	}
	
	@Override
	public String toString(){
		return this.getName();
	}


	
	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#updateSourceImageDirectory(java.io.File)
	 */
	@Override
	public void updateSourceImageDirectory(File expectedImageDirectory) {
		
		fine("Searching "+expectedImageDirectory.getAbsolutePath());
		
		if( ! expectedImageDirectory.exists()){
			throw new IllegalArgumentException("Requested directory does not exist: "+expectedImageDirectory);
		}

		// Is the name of the expectedImageDirectory the same as the dataset image directory?
		if( ! checkName(expectedImageDirectory, this)){
			throw new IllegalArgumentException("Dataset name does not match new folder; unable to update paths");
		}
		fine("Dataset name matches new folder");
			
		// Does expectedImageDirectory contain image files?
		if( ! checkHasImages(expectedImageDirectory)){
			throw new IllegalArgumentException("Target folder contains no images; unable to update paths");
		}
		
		fine("Target folder contains at least one image");

		fine("Updating dataset image paths");
		boolean ok = this.getCollection().updateSourceFolder(expectedImageDirectory);
		if(!ok){
			warn("Error updating dataset image paths; update cancelled");
		}

		fine("Updating child dataset image paths");
		for(IAnalysisDataset child : this.getAllChildDatasets()){
			ok = child.getCollection().updateSourceFolder(expectedImageDirectory);
			if(!ok){
				warn("Error updating child dataset image paths; update cancelled");
			}
		}

		log("Updated image paths to new folder location");
	}
	
	/**
	 * Check that the new image directory has the same name as the old image directory.
	 * If the nmd has been copied to the wrong folder, don't update nuclei
	 * @param expectedImageDirectory
	 * @param dataset
	 * @return
	 */
	private boolean checkName(File expectedImageDirectory, IAnalysisDataset dataset){
		if(dataset.getCollection().getFolder().getName().equals(expectedImageDirectory.getName())){
			return true;
		} else {
			return false;
		}
		
	}
	
	/**
	 * Check that the given directory contains >0 image files
	 * suitable for the morphology analysis
	 * @param expectedImageDirectory
	 * @return
	 */
	private boolean checkHasImages(File expectedImageDirectory){

		File[] listOfFiles = expectedImageDirectory.listFiles();

		int result = 0;

		for (File file : listOfFiles) {

			boolean ok = NucleusDetectionWorker.checkFile(file);

			if(ok){
				result++;
			}
		} 
		
		if(result>0){
			return true;
		} else {
			return false;
		}
	}
	

	

	/* (non-Javadoc)
	 * @see analysis.IAnalysisDataset#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((analysisOptions == null) ? 0 : analysisOptions.hashCode());
		result = prime
				* result
				+ ((childDatasets == null) ? 0 : childDatasets.hashCode());
		result = prime * result
				+ ((clusterGroups == null) ? 0 : clusterGroups.hashCode());
		result = prime * result
				+ ((datasetColour == null) ? 0 : datasetColour.hashCode());
		result = prime * result
				+ ((debugFile == null) ? 0 : debugFile.hashCode());
		result = prime * result + (isRoot ? 1231 : 1237);
		result = prime * result
				+ ((mergeSources == null) ? 0 : mergeSources.hashCode());
		result = prime
				* result
				+ ((otherDatasets == null) ? 0 : otherDatasets.hashCode());
		result = prime * result
				+ ((savePath == null) ? 0 : savePath.hashCode());
		result = prime * result
				+ ((thisCollection == null) ? 0 : thisCollection.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	    in.defaultReadObject();
}
}
