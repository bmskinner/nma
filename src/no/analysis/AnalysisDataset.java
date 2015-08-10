package no.analysis;

import java.awt.Color;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import utility.Constants;
import no.collections.CellCollection;
import no.components.AnalysisOptions;
import no.components.ShellResult;
import no.gui.ColourSelecter;


/**
 * This holds a NucleusCollection, the analyses that have been run on it
 * and the relationships it holds with other NucleusCollections
 *
 */
public class AnalysisDataset implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private Map<UUID, AnalysisDataset> childCollections  = new HashMap<UUID, AnalysisDataset>(); // hold the UUID of any child collections
	private Map<UUID, AnalysisDataset> parentCollections = new HashMap<UUID, AnalysisDataset>(); // parents of this dataset
	
	private CellCollection thisCollection;
	private File savePath; // the file to save the analysis to
	
	private AnalysisOptions analysisOptions;
	private Map<Integer, ShellResult> shellResults 		= new HashMap<Integer, ShellResult>(0); // store shell analysis for each channel
	
	private Map<Integer, String>	signalGroupsAdded		= new HashMap<Integer, String>(0);	// store the names of the groups added
	private Map<Integer, Boolean> 	signalGroupsVisible 	= new HashMap<Integer, Boolean>(0); // is the given signal group shown in plots
	private Map<Integer, Color> 	signalGroupColours 		= new HashMap<Integer, Color>(0); // allow saving of colour choices
	private Color datasetColour = null; // use for colouring the dataset in comparison with other datasets
	
	private List<UUID> clusterResults = new ArrayList<UUID>(0);
	private List<UUID> mergeParents	  = new ArrayList<UUID>(0); // hold the ids of datasets merged to create this dataset
	private String newickTree;
	
	private File debugFile;
	
	private String version;
	
	private boolean isRoot;
	
	
	public AnalysisDataset(CellCollection collection){
		this.thisCollection = collection;
		this.savePath = new File(collection.getOutputFolder()+File.separator+collection.getType()+".nmd"); // nmd Nuclear Morphology Dataset
		this.isRoot = false;
		this.version = Constants.VERSION_MAJOR+"."+Constants.VERSION_REVISION+"."+Constants.VERSION_BUGFIX;
	}
	
	public AnalysisDataset(CellCollection collection, File saveFile){
		this.thisCollection = collection;
		this.savePath = saveFile;
		this.isRoot = false;
		this.version = Constants.VERSION_MAJOR+"."+Constants.VERSION_REVISION+"."+Constants.VERSION_BUGFIX;
	}
	
	public String getVersion(){
		return this.version;
	}
	
	public void addChildCollection(CellCollection collection){
		if(collection==null){
			throw new IllegalArgumentException("Nucleus collection is null");
		}
		UUID id = collection.getID();
		AnalysisDataset childDataset = new AnalysisDataset(collection, this.savePath);
		childDataset.setAnalysisOptions(this.getAnalysisOptions());
		this.childCollections.put(id, childDataset);

	}
	
	public void addChildDataset(AnalysisDataset dataset){
		if(dataset==null){
			throw new IllegalArgumentException("Nucleus collection is null");
		}
		UUID id = dataset.getUUID();
		this.childCollections.put(id, dataset);
		dataset.addParentDataset(this);
	}
	
	public void removeChildCollection(UUID id){
		this.childCollections.remove(id);
		if(this.clusterResults.contains(id)){
			this.clusterResults.remove(id);
		}
	}
	
	
	/**
	 * Add the given dataset as a parent
	 * @param dataset
	 */
	public void addParentDataset(AnalysisDataset dataset){
		if(dataset==null){
			throw new IllegalArgumentException("Nucleus collection is null");
		}
		UUID id = dataset.getUUID();
		this.parentCollections.put(id, dataset);
		
		// if this dataset has a parent, it cannot be root
		// TODO: multiple dataset sharing between roots
		if(this.isRoot){
			this.setRoot(false);
		}

	}
	
	/**
	 * Remove the given dataset from the list of parents
	 * and any lists that depend on parents
	 * @param id the UUID to remove
	 */
	public void removeParentDataset(UUID id){
		this.parentCollections.remove(id);
		if(this.mergeParents.contains(id)){
			this.mergeParents.remove(id);
		}
	}
	
	
	public UUID getUUID(){
		return this.thisCollection.getID();
	}
	
	public String getName(){
		return this.thisCollection.getName();
	}
	
	public void setName(String s){
		this.thisCollection.setName(s);
	}
	
	public File getSavePath(){
		return this.savePath;
	}
	
	public void setSavePath(File file){
		this.savePath = file;
	}
	
	public File getDebugFile(){
		return this.thisCollection.getDebugFile();
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
		for(UUID id : idlist){
			AnalysisDataset d = getChildDataset(id);
			idlist.addAll(d.getAllChildUUIDs());
		}
		return idlist;
	}
	
	/**
	 * Get the specificed child
	 * @param id the child UUID
	 * @return
	 */
	public AnalysisDataset getChildDataset(UUID id){
		return this.childCollections.get(id);
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

	public void addShellResult(int channel, ShellResult result){
		this.shellResults.put(channel, result);
	}

	public ShellResult getShellResult(int channel){
		return this.shellResults.get(channel);
	}

	/**
	 * Test if the collection has a ShellResult in any channel
	 * 
	 */
	public boolean hasShellResult(){
		for(Integer channel : this.thisCollection.getSignalGroups()){
			if(this.shellResults.containsKey(channel)){
				return true;
			}
		}
		return false;
	}

	public AnalysisOptions getAnalysisOptions() {
		return analysisOptions;
	}

	public void setAnalysisOptions(AnalysisOptions analysisOptions) {
		this.analysisOptions = analysisOptions;
	}

	public void addCluster(AnalysisDataset dataset){
		this.addChildDataset(dataset);
		this.clusterResults.add(dataset.getUUID());
	}

	public void addCluster(CellCollection collection){
		this.addChildCollection(collection);
		this.clusterResults.add(collection.getID());
	}

	public List<UUID> getClusterIDs(){
		return this.clusterResults;
	}

	public boolean hasClusters(){
		if(this.clusterResults.size()>0){
			return true;
		} else {
			return false;
		}
	}

	public void setClusterTree(String s){
		this.newickTree = s;
	}

	public String getClusterTree(){
		return this.newickTree;
	}

	public boolean isRoot(){
		return  this.isRoot;
	}

	public void setRoot(boolean b){
		this.isRoot = b;
	}

	/**
	 * Delete the child AnalysisDataset specified
	 * @param id the UUID of the child to delete
	 */
	public void deleteChild(UUID id){
		if(this.hasChild(id)){
			this.childCollections.remove(id);
//			IJ.log("    Removed child id");
			
			if(this.clusterResults.contains(id)){
				this.clusterResults.remove(id);
//				IJ.log("    Removed cluster id");
			}
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
	 * Check if this dataset is the child of the given dataset
	 * @param parent the parent dataset
	 * @return
	 */
	public boolean isChild(AnalysisDataset parent){
		if(this.parentCollections.containsKey(parent.getUUID())){
			return true;
		} else{
			return false;
		}
	}
	
	/**
	 * Check if this dataset is the child of the given dataset
	 * @param parent the parent dataset
	 * @return
	 */
	public boolean isChild(UUID parent){
		if(this.parentCollections.containsKey(parent)){
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
	public void setSignalGroupVisible(int signalGroup, boolean b){
		this.signalGroupsVisible.put(signalGroup, b);
	}
	
	/**
	 * Check if the given signal group is visible in plots
	 * @param signalGroup the group
	 * @return
	 */
	public boolean isSignalGroupVisible(int signalGroup){
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
	public Color getSignalGroupColour(int signalGroup){
		if(this.signalGroupColours.containsKey(signalGroup)){
			return this.signalGroupColours.get(signalGroup);
		} else {
//			The default is the colour selection model for the entire program
			return ColourSelecter.getSignalColour(  signalGroup-1); 
		}
	}
	
	/**
	 * Set the given signal group colour for plots
	 * @param signalGroup the group
	 * @param colour the colour
	 */
	public void setSignalGroupColour(int signalGroup, Color colour){
		this.signalGroupColours.put(signalGroup, colour);
	}
	
	
	/**
	 * Get the name of the signal group
	 * @param signalGroup the group the fetch
	 * @return
	 */
	public String getSignalGroupName(int signalGroup){
		return this.signalGroupsAdded.get(signalGroup);
	}
	
	/**
	 * Set the given signal group name
	 * @param signalGroup
	 * @param name
	 */
	public void setSignalGroupName(int signalGroup, String name){
		this.signalGroupsAdded.put(signalGroup, name);
	}
	
	  /**
	   * Return the highest signal group present, or 0 if no signal groups
	   * are present
	 * @return the highest signal group
	 */
	  public int getHighestSignalGroup(){
		  int maxGroup = 0;
		  for(Integer n : signalGroupsAdded.keySet()){
			  maxGroup = n > maxGroup ? n : maxGroup; 
		  }
		  return maxGroup;
	  }
	
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
	

}
