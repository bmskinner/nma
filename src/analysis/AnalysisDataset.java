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
package analysis;

import gui.components.ColourSelecter;
import gui.components.ColourSelecter.ColourSwatch;
import ij.IJ;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import analysis.nucleus.NucleusDetector;
import logging.DebugFileFormatter;
import logging.DebugFileHandler;
import utility.Constants;
import utility.Version;
import components.Cell;
import components.CellCollection;
import components.ClusterGroup;
import components.nuclear.ShellResult;


/**
 * This holds a CellCollection, the analyses that have been run on it
 * and the relationships it holds with other CellCollections. It also provides
 * colour and UI options
 *
 */
public class AnalysisDataset implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private Map<UUID, AnalysisDataset> childCollections  = new HashMap<UUID, AnalysisDataset>(); // hold the UUID of any child collections
	
	// Other datasets associated with this dataset, that will need to be saved out.
	// Includes merge sources. Scope for expansion
	private Map<UUID, AnalysisDataset> otherCollections = new HashMap<UUID, AnalysisDataset>();
	
	private CellCollection thisCollection;
	private File savePath; // the file to save the analysis to
	
	private AnalysisOptions analysisOptions;
	
	private Map<UUID, ShellResult> shellResults 	   = new HashMap<UUID, ShellResult>(0); // store shell analysis for each channel
	private Map<UUID, String>      signalGroupsAdded   = new HashMap<UUID, String>(0);	// store the names of the groups added
	private Map<UUID, Boolean>     signalGroupsVisible = new HashMap<UUID, Boolean>(0); // is the given signal group shown in plots
	private Map<UUID, Color> 	   signalGroupColours  = new HashMap<UUID, Color>(0); // allow saving of colour choices
	
	private Color datasetColour = null; // use for colouring the dataset in comparison with other datasets
	
	private List<ClusterGroup> clusterGroups = new ArrayList<ClusterGroup>(0); // hold groups of cluster results
	
	//The ids of datasets merged to create this dataset. The IDs must be present in
	// otherCollections
	private List<UUID> mergeSources	  = new ArrayList<UUID>(0);
	
	private File debugFile;

	private final Version version;
	
	private transient ColourSwatch swatch = ColourSwatch.REGULAR_SWATCH;
		
	private boolean isRoot = false;	// is this a root dataset
		
	/**
	 * Create a dataset from a cell collection. The save file is
	 * set as the output folder of the collection
	 * @param collection
	 */
	public AnalysisDataset(CellCollection collection){
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
	public AnalysisDataset(CellCollection collection, File saveFile){
		this.thisCollection = collection;
		this.savePath       = saveFile;
		
		this.debugFile      = new File(saveFile.getParent()
				                        + File.separator
				                        + saveFile.getName().replace(Constants.SAVE_FILE_EXTENSION, Constants.LOG_FILE_EXTENSION));
		this.isRoot         = false;
		this.version        = new Version(Constants.VERSION_MAJOR,Constants.VERSION_MINOR,Constants.VERSION_REVISION);
	}
	
	
	/**
	 * Make a copy of the cells in this dataset. Does not yet include
	 * child datasets, clusters or signal groups
	 * @return
	 * @throws Exception 
	 */
	public AnalysisDataset duplicate() throws Exception{
		AnalysisDataset result = new AnalysisDataset(this.getCollection());
		Iterator<Cell> it = this.getCollection().getCellIterator();
		
		while(it.hasNext()){
			Cell c = it.next();

			result.getCollection().addCell(new Cell(c));
		}
		
//		TODO: Add child collections, clusters etc
		
		return result;
	}
		
	/**
	 * Get the file handler for this dataset. Create a handler
	 * if needed.
	 * @return
	 */
	public Handler getLogHandler() throws Exception {

		Handler fileHandler = new DebugFileHandler(this.getDebugFile());
		fileHandler.setFormatter(new DebugFileFormatter());

		return fileHandler;
	}
	
	/**
	 * Get the software version used to create the dataset
	 * @return
	 */
	public Version getVersion(){
		return this.version;
	}
	
	/**
	 * Add the given cell collection as a child to this dataset. A
	 * new dataset is contructed to hold it.
	 * @param collection the collection to add
	 */
	public void addChildCollection(CellCollection collection){
		if(collection==null){
			throw new IllegalArgumentException("Nucleus collection is null");
		}
		UUID id = collection.getID();
		AnalysisDataset childDataset = new AnalysisDataset(collection, this.savePath);
		childDataset.setRoot(false);
		childDataset.setAnalysisOptions(this.getAnalysisOptions());
		this.childCollections.put(id, childDataset);

	}
	
	/**
	 * Add the given dataset as a child of this dataset
	 * @param dataset
	 */
	public void addChildDataset(AnalysisDataset dataset){
		if(dataset==null){
			throw new IllegalArgumentException("Nucleus collection is null");
		}
		dataset.setRoot(false);
		UUID id = dataset.getUUID();
		this.childCollections.put(id, dataset);
	}
	
	/**
	 * Remove the child dataset with the given UUID
	 * @param id
	 */
	private void removeChildCollection(UUID id){
		this.childCollections.remove(id);
	}
	
	
	/**
	 * Add the given dataset as an associated dataset.
	 * This is not a child, and must be added to an 
	 * appropriate identifier list; this is handled by
	 * the public functions calling this method
	 * @param dataset the dataset to add
	 */
	private void addAssociatedDataset(AnalysisDataset dataset){
		if(dataset==null){
			throw new IllegalArgumentException("Dataset is null");
		}
		UUID id = dataset.getUUID();
		this.otherCollections.put(id, dataset);
	}
	
	/**
	 * Get the associated dataset with the given id. Not public
	 * beacause each associated dataset should have a further
	 * classification, and should be retrieved through its own
	 * method
	 * @param id the dataset to get
	 * @return the dataset or null
	 */
	private AnalysisDataset getAssociatedDataset(UUID id){
		return this.otherCollections.get(id);
	}
	
	/**
	 * Remove the given dataset from the list of parents
	 * and any lists that depend on parents
	 * @param id the UUID to remove
	 */
	private void removeAssociatedDataset(UUID id){
		this.otherCollections.remove(id);

	}
	
	
	public UUID getUUID(){
		return this.thisCollection.getID();
	}
	
	/**
	 * Get the name of the dataset. Passes through to
	 * CellCollection
	 * @return
	 * @see CellCollection
	 */
	public String getName(){
		return this.thisCollection.getName();
	}
	
	/**
	 * Set the name of the dataset. Passes through
	 * to the CellCollection
	 * @param s
	 * @see CellCollection
	 */
	public void setName(String s){
		this.thisCollection.setName(s);
	}
	
	/**
	 * Get the save file location
	 * @return
	 */
	public File getSavePath(){
		return this.savePath;
	}
	
	/**
	 * Set the path to save the dataset
	 * @param file
	 */
	public void setSavePath(File file){
		this.savePath = file;
	}
	
	/**
	 * Get the debug file for the dataset. Passes
	 * through to cell collection for now
	 * @return
	 * @see CellCollection
	 */
	public File getDebugFile(){
		return this.debugFile;
	}
	
	/**
	 * Allow the collection to update the debug file location
	 * @param f the new file
	 */
	public void setDebugFile(File f){
		try {
			if(!f.exists()){
				f.createNewFile();
			}
			if(f.canWrite()){
				this.debugFile = f;
			}
		} catch (IOException e) {
			IJ.log("Unable to update debug file location");
		}
	}
	
	/**
	 * Get all the direct children of this dataset
	 * @return
	 */
	public Set<UUID> getChildUUIDs(){
		return this.childCollections.keySet();
	}
	
	/**
	 * Recursive version of getChildUUIDs.
	 * Get the children of this dataset, and all
	 * their children
	 * @return
	 */
	public Set<UUID> getAllChildUUIDs(){
		Set<UUID> idlist = this.getChildUUIDs();
		Set<UUID> result = new HashSet<UUID>();
		result.addAll(idlist);
		
		for(UUID id : idlist){
			AnalysisDataset d = getChildDataset(id);
//			Set<UUID> childIdList = d.getAllChildUUIDs();
//			for(UUID childId : childIdList){
//				result.add(id);
//			}
			
			result.addAll(d.getAllChildUUIDs());
		}
		return result;
	}
	
	/**
	 * Get the specificed child
	 * @param id the child UUID
	 * @return
	 */
	public AnalysisDataset getChildDataset(UUID id){
		if(this.hasChild(id)){
			return this.childCollections.get(id);
		} else {
			for(AnalysisDataset child : this.getAllChildDatasets()){
				if(child.getUUID().equals(id)){
					return child;
				}
			}
		}
		return null;
	}
	
	/**
	 * Get the AnalysisDataset with the given id
	 * that is a merge source to this dataset. 
	 * @param id the UUID of the dataset
	 * @return the dataset or null
	 */
	public AnalysisDataset getMergeSource(UUID id){
		if(this.mergeSources.contains(id)){
		return this.getAssociatedDataset(id);
		} else {
			return null;
		}
	}
	
	/**
	 * Recursively fetch all the merge sources for this dataset.
	 * Only includes the root sources (not intermediate merges)
	 * @return
	 */
	public List<AnalysisDataset> getAllMergeSources(){
		
		List<AnalysisDataset>  result = new ArrayList<AnalysisDataset>();
		
		for(UUID id : getMergeSourceIDs()){
			
			AnalysisDataset source = this.getAssociatedDataset(id);
			if(source.hasMergeSources()){
				result.addAll(source.getAllMergeSources());
			} else {
				result.add(source);
			}
		}
		return result;
	}
	
	/**
	 * Add the given dataset as a merge source
	 * @param dataset
	 */
	public void addMergeSource(AnalysisDataset dataset){
		this.mergeSources.add(dataset.getUUID());
		this.addAssociatedDataset(dataset);
	}
	
	/**
	 * Get all datasets considered direct merge sources to this
	 * dataset
	 * @return
	 */
	public List<AnalysisDataset> getMergeSources(){
		List<AnalysisDataset>  result = new ArrayList<AnalysisDataset>();
		
		for(UUID id : mergeSources){
			result.add(this.getAssociatedDataset(id));	
		}
		return result;
	}
	
	/**
	 * Get the ids of all datasets considered merge sources to this
	 * dataset
	 * @return
	 */
	public List<UUID> getMergeSourceIDs(){
		return this.mergeSources;
	}
	
	/**
	 * Get the ids of all datasets considered merge sources to this
	 * dataset, recursively (that is, if the merge source is a merge, get
	 * the sources of that merge)
	 * @return
	 */
	public List<UUID> getAllMergeSourceIDs(){
		
		List<UUID> result = new ArrayList<UUID>();
		
		for(UUID id : this.getMergeSourceIDs()){
			result.addAll(getMergeSource(id).getAllMergeSourceIDs());
		}

		return result;
	}
	
	/**
	 * Test if a dataset with the given id is present
	 * as a merge source
	 * @param id the UUID to test
	 * @return
	 */
	public boolean hasMergeSource(UUID id){
		if(this.mergeSources.contains(id)){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Test if a dataset is present
	 * as a merge source
	 * @param dataset the dataset to test
	 * @return
	 */
	public boolean hasMergeSource(AnalysisDataset dataset){
		return this.hasMergeSource(dataset.getUUID());
	}
	
	/**
	 * Test if the dataset has merge sources
	 * @return
	 */
	public boolean hasMergeSources(){
		if(this.mergeSources.isEmpty()){
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Get the number of direct children of this dataset
	 * @return
	 */
	public int getChildCount(){
		return this.childCollections.size();
	}
	
	/**
	 * Check if the dataset has children
	 * @return
	 */
	public boolean hasChildren(){
		if(this.childCollections.size()>0){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get all the direct children of this dataset
	 * @return
	 */
	public Collection<AnalysisDataset> getChildDatasets(){
		return this.childCollections.values();
	}
	
	/**
	 * Recursive version of get child datasets
	 * Get all the direct children of this dataset, 
	 * and all their children.
	 * @return
	 */
	public List<AnalysisDataset> getAllChildDatasets(){

		List<AnalysisDataset> result = new ArrayList<AnalysisDataset>(0);
		for(AnalysisDataset d : this.getChildDatasets()){ // the direct descendents of this dataset
			result.add(d);
			
			if(this.hasChildren()){
				result.addAll(d.getAllChildDatasets());
			}
		}
		return result;
	}
	
	/**
	 * Get the collection in this dataset
	 * @return
	 */
	public CellCollection getCollection(){
		return this.thisCollection;
	}

	public void addShellResult(UUID signalGroup, ShellResult result){
		this.shellResults.put(signalGroup, result);
	}

	public ShellResult getShellResult(UUID group){
		return this.shellResults.get(group);
	}

	/**
	 * Test if the collection has a ShellResult in any channel
	 * 
	 */
	public boolean hasShellResult(){
		for(UUID channel : thisCollection.getSignalManager().getSignalGroups()){
			if(this.shellResults.containsKey(channel)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the analysis options from this dataset
	 * @return
	 */
	public AnalysisOptions getAnalysisOptions() {
		return analysisOptions;
	}
	
	/**
	 * Test if the dataset has analysis options set.
	 * This is not the case for (for example) merge sources
	 * @return
	 */
	public boolean hasAnalysisOptions(){
		if(this.analysisOptions==null){
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Set the analysis options for the dataset
	 * @param analysisOptions
	 */
	public void setAnalysisOptions(AnalysisOptions analysisOptions) {
		this.analysisOptions = analysisOptions;
	}
	
	/**
	 * Add the given dataset as a cluster result.
	 * This is a form of child dataset
	 * @param dataset
	 */
	public void addClusterGroup(ClusterGroup group){
		this.clusterGroups.add(group);
	}
	
	/**
	 * Check the list of cluster groups, and return the highest
	 * cluster group number present
	 * @return
	 */
	public int getMaxClusterGroupNumber(){
		int number = 0;
		
		if(this.hasClusters()){

			for (ClusterGroup g :  this.getClusterGroups()){

				String name = g.getName();
//				"ClusterGroup_"+clusterNumber
				Pattern p = Pattern.compile("^ClusterGroup_(\\d+)$");
//				IJ.log("Matching "+p.pattern()+" againt "+name);
				Matcher m = p.matcher(name);
				if(m.find()){
					String s = m.group(1);
//					IJ.log("  Match found: "+s);
					int n = Integer.valueOf(s);
					if(n>number){
						number=n;
					}
				}
			}
		}
		return number;
	}


	
	/**
	 * Check if the dataset id is in a cluster
	 * @param id
	 * @return
	 */
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
	
	public List<ClusterGroup> getClusterGroups(){
		return  this.clusterGroups;
	}
	
	/**
	 * Get the UUIDs of all datasets in clusters
	 * @return
	 */
	public List<UUID> getClusterIDs(){
		List<UUID> result = new ArrayList<UUID>();
		for(ClusterGroup g : this.clusterGroups){
			result.addAll(g.getUUIDs());
		}
		return result;
	}
	
	/**
	 * Check if the dataset has clusters
	 * @return
	 */
	public boolean hasClusters(){
		if(this.clusterGroups != null && this.clusterGroups.size()>0){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Test if the given group is present in this dataset
	 * @param group
	 * @return
	 */
	public boolean hasClusterGroup(ClusterGroup group){
		if(this.clusterGroups.contains(group)){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Check that all cluster groups have child members present;
	 * if cluster groups do not have children, remove the group
	 */
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

	/**
	 * Check if the dataset is root
	 * @return
	 */
	public boolean isRoot(){
		return this.isRoot;
	}

	/**
	 * Set the dataset root status
	 * @param b is the dataset root
	 */
	public void setRoot(boolean b){
		this.isRoot = b;
	}

	/**
	 * Delete the child AnalysisDataset specified
	 * @param id the UUID of the child to delete
	 */
	public void deleteChild(UUID id){
		if(this.hasChild(id)){
			this.removeChildCollection(id);
		}
	}
		
	/**
	 * Delete the cluster with the given id
	 * @param id
	 */
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
	
	/**
	 * Delete an associated dataset
	 * @param id
	 */
	public void deleteMergeSource(UUID id){
		if(this.mergeSources.contains(id)){
			this.removeAssociatedDataset(id);
			this.otherCollections.remove(id);
		}
	}

	/**
	 * Check if the given dataset is a child dataset of this
	 * @param child the dataset to test
	 * @return
	 */
	public boolean hasChild(AnalysisDataset child){
		if(this.childCollections.containsKey(child.getUUID())){
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Check if the given dataset is a child dataset of this
	 * @param child
	 * @return
	 */
	public boolean hasChild(UUID child){
		if(this.childCollections.containsKey(child)){
			return true;
		} else {
			return false;
		}
	}
			
	/**
	 * Set the given signal group to be visible in plots
	 * @param signalGroup the group
	 * @param b visible or not
	 */
	public void setSignalGroupVisible(UUID signalGroup, boolean b){
		this.signalGroupsVisible.put(signalGroup, b);
	}
	
	/**
	 * Check if the given signal group is visible in plots
	 * @param signalGroup the group
	 * @return
	 */
	public boolean isSignalGroupVisible(UUID signalGroup){
		if(this.signalGroupsVisible.containsKey(signalGroup)){
			return this.signalGroupsVisible.get(signalGroup);
		} else {
			return true; // default true - only store the false toggle as needed
		}
	}
	
	/**
	 * Get the set colour for the signal group, or the default colour if
	 * none is set
	 * @param signalGroup the group
	 * @return a colour
	 */
	public Color getSignalGroupColour(UUID signalGroup){
		if(this.signalGroupColours.containsKey(signalGroup)){
			return this.signalGroupColours.get(signalGroup);
		} else {
//			The default is the colour selection model for the entire program
			return Color.RED;
//			return ColourSelecter.getSignalColour(  signalGroup-1); 
		}
	}
	
	/**
	 * Set the given signal group colour for plots
	 * @param signalGroup the group
	 * @param colour the colour
	 */
	public void setSignalGroupColour(UUID signalGroup, Color colour){
		this.signalGroupColours.put(signalGroup, colour);
	}
	
	
	/**
	 * Get the name of the signal group
	 * @param signalGroup the group the fetch
	 * @return
	 */
	public String getSignalGroupName(UUID signalGroup){
		return this.signalGroupsAdded.get(signalGroup);
	}
	
	/**
	 * Set the given signal group name
	 * @param signalGroup
	 * @param name
	 */
	public void setSignalGroupName(UUID signalGroup, String name){
		this.signalGroupsAdded.put(signalGroup, name);
	}
	
	  /**
	   * Return the highest signal group present, or 0 if no signal groups
	   * are present
	 * @return the highest signal group
	 */
//	  public int getHighestSignalGroup(){
//		  int maxGroup = 0;
//		  for(UUID n : signalGroupsAdded.keySet()){
//			  maxGroup = n > maxGroup ? n : maxGroup; 
//		  }
//		  return maxGroup;
//	  }
	
	/**
	 * Set the dataset colour (used in comparisons between datasets)
	 * @param colour the new colour
	 */
	public void setDatasetColour(Color colour){
		this.datasetColour = colour;
	}
	
	
	/**
	 * Get the currently set dataset colour, or null if not set
	 * @return colour or null
	 */
	public Color getDatasetColour(){
		return this.datasetColour;
	}
	
	
	/**
	 * Test if the dataset colour is set or null
	 * @return
	 */
	public boolean hasDatasetColour(){
		if(this.getDatasetColour()==null){
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Get the swatch, or null if the swatch is not set. 
	 * Transient, not saved to nmd
	 * @return
	 */
	public ColourSwatch getSwatch() {
		return swatch;
	}
	
	public String toString(){
		return this.getName();
	}

	/**
	 * Set the swatch
	 * @param swatch
	 */
	public void setSwatch(ColourSwatch swatch) {
		this.swatch = swatch;
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		    in.defaultReadObject();
		    this.swatch = ColourSwatch.REGULAR_SWATCH;
	}
	
	/**
	 * Update the source image paths in the dataset and its children
	 * to use the given directory 
	 * @param expectedImageDirectory
	 * @param dataset
	 * @throws Exception
	 */
	public void updateSourceImageDirectory(File expectedImageDirectory) {
		
		Logger logger = Logger.getLogger("ProgramLogger");
		logger.log(Level.FINE, "Searching "+expectedImageDirectory.getAbsolutePath());
		
		if( ! expectedImageDirectory.exists()){
			throw new IllegalArgumentException("Requested directory does not exist: "+expectedImageDirectory);
		}

		// Is the name of the expectedImageDirectory the same as the dataset image directory?
		if( ! checkName(expectedImageDirectory, this)){
			throw new IllegalArgumentException("Dataset name does not match new folder; unable to update paths");
		}
		logger.log(Level.FINE, "Dataset name matches new folder");
			
		// Does expectedImageDirectory contain image files?
		if( ! checkHasImages(expectedImageDirectory)){
			throw new IllegalArgumentException("Target folder contains no images; unable to update paths");
		}
		
		logger.log(Level.FINE, "Target folder contains at least one image");

		logger.log(Level.FINE, "Updating dataset image paths");
		boolean ok = this.getCollection().updateSourceFolder(expectedImageDirectory);
		if(!ok){
			logger.log(Level.WARNING, "Error updating dataset image paths; update cancelled");
		}

		logger.log(Level.FINE, "Updating child dataset image paths");
		for(AnalysisDataset child : this.getAllChildDatasets()){
			ok = child.getCollection().updateSourceFolder(expectedImageDirectory);
			if(!ok){
				logger.log(Level.SEVERE, "Error updating child dataset image paths; update cancelled");
			}
		}

		logger.log(Level.INFO, "Updated image paths to new folder location");
	}
	
	/**
	 * Check that the new image directory has the same name as the old image directory.
	 * If the nmd has been copied to the wrong folder, don't update nuclei
	 * @param expectedImageDirectory
	 * @param dataset
	 * @return
	 */
	private boolean checkName(File expectedImageDirectory, AnalysisDataset dataset){
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

			boolean ok = NucleusDetector.checkFile(file);

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
	

}
