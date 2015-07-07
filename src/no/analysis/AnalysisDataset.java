package no.analysis;

import ij.IJ;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import no.collections.CellCollection;
import no.components.AnalysisOptions;
import no.components.ShellResult;
import no.export.PopulationExporter;
import no.gui.MainWindow;


/**
 * This holds a NucleusCollection, the analyses that have been run on it
 * and the relationships it holds with other NucleusCollections
 *
 */
public class AnalysisDataset implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private Map<UUID, AnalysisDataset> childCollections = new HashMap<UUID, AnalysisDataset>(); // hold the UUID of any child collections
	private CellCollection thisCollection;
	private File savePath; // the file to save the analysis to
	
	private AnalysisOptions analysisOptions;
	private Map<Integer, ShellResult> shellResults = new HashMap<Integer, ShellResult>(0); // store shell analysis for each channel
	
	private List<UUID> clusterResults = new ArrayList<UUID>(0);
	private String newickTree;
	
	private File debugFile;
	
	private String version;
	
	private boolean isRoot;
	
	
	public AnalysisDataset(CellCollection collection){
		this.thisCollection = collection;
		this.savePath = new File(collection.getOutputFolder()+File.separator+collection.getType()+".nmd"); // nmd Nuclear Morphology Dataset
		this.isRoot = false;
		this.version = MainWindow.VERSION_MAJOR+"."+MainWindow.VERSION_REVISION+"."+MainWindow.VERSION_BUGFIX;
	}
	
	public AnalysisDataset(CellCollection collection, File saveFile){
		this.thisCollection = collection;
		this.savePath = saveFile;
		this.isRoot = false;
		this.version = MainWindow.VERSION_MAJOR+"."+MainWindow.VERSION_REVISION+"."+MainWindow.VERSION_BUGFIX;
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

	}
	
	public void removeChildCollection(UUID id){
		this.childCollections.remove(id);
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
	
	public Set<UUID> getChildUUIDs(){
		return this.childCollections.keySet();
	}
	
	// recursive version of get UUIDs
	public Set<UUID> getAllChildUUIDs(){
		Set<UUID> idlist = this.getChildUUIDs();
		for(UUID id : idlist){
			AnalysisDataset d = getChildDataset(id);
			idlist.addAll(d.getAllChildUUIDs());
		}
		return idlist;
	}
	
	public AnalysisDataset getChildDataset(UUID id){
		return this.childCollections.get(id);
	}
	
	public int getChildCount(){
		return this.childCollections.size();
	}
	
	public boolean hasChildren(){
		if(this.childCollections.size()>0){
			return true;
		} else {
			return false;
		}
	}
	
	public Collection<AnalysisDataset> getChildDatasets(){
		return this.childCollections.values();
	}
	
	// recursive version of get child datasets
	public List<AnalysisDataset> getAllChildDatasets(){
//		IJ.log("Traversing "+this.getName());
		List<AnalysisDataset> result = new ArrayList<AnalysisDataset>(0);
		for(AnalysisDataset d : this.getChildDatasets()){ // the direct descendents of this dataset
			result.add(d);
			
//			IJ.log("Fetching child "+d.getName()+" of "+this.getName());
			if(this.hasChildren()){
				result.addAll(d.getAllChildDatasets());
			}
		}
		return result;
	}
	
	public CellCollection getCollection(){
		return this.thisCollection;
	}

	public void save(){
		if(savePath==null){
			IJ.log("No save path defined");
		} else {
			PopulationExporter.saveAnalysisDataset(this);
		}
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
		for(Integer channel : this.thisCollection.getSignalChannels()){
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

	public boolean hasChild(AnalysisDataset child){
		if(this.childCollections.containsKey(child.getUUID())){
			return true;
		} else {
			return false;
		}
	}

	public boolean hasChild(UUID child){
		if(this.childCollections.containsKey(child)){
			return true;
		} else {
			return false;
		}
	}

}
