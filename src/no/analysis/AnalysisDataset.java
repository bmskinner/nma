package no.analysis;

import ij.IJ;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import no.collections.NucleusCollection;
import no.export.PopulationExporter;


/**
 * This holds a NucleusCollection, the analyses that have been run on it
 * and the relationships it holds with other NucleusCollections
 *
 */
public class AnalysisDataset implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private Map<UUID, AnalysisDataset> childCollections = new HashMap<UUID, AnalysisDataset>(); // hold the UUID of any child collections
	private NucleusCollection thisCollection;
	private File savePath; // the file to save the analysis to
	
	
	public AnalysisDataset(NucleusCollection collection){
		this.thisCollection = collection;
		this.savePath = new File(collection.getOutputFolder()+File.separator+collection.getType()+".nmd"); // nmd Nuclear Morphology Dataset
	}
	
	public AnalysisDataset(NucleusCollection collection, File saveFile){
		this.thisCollection = collection;
		this.savePath = saveFile;
	}
	
	
	public void addChildCollection(NucleusCollection collection){
		if(collection==null){
			throw new IllegalArgumentException("Nucleus collection is null");
		}
		UUID id = collection.getID();
		AnalysisDataset childDataset = new AnalysisDataset(collection, this.savePath);

		this.childCollections.put(id, childDataset);

	}
	
	public void removeChildCollection(UUID id){
		this.childCollections.remove(id);
	}
	
	public UUID getUUID(){
		return this.thisCollection.getID();
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
	
	public NucleusCollection getCollection(){
		return this.thisCollection;
	}
	
	public void save(){
		if(savePath==null){
			IJ.log("No save path defined");
		} else {
			PopulationExporter.saveAnalysisDataset(this);
		}
	}

}
