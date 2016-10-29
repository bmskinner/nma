package components.active;

import java.awt.Paint;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import analysis.IAnalysisDataset;
import analysis.IAnalysisOptions;
import components.ICellCollection;
import components.IClusterGroup;
import utility.Constants;
import utility.Version;

/**
 * This is the virtual child dataset, which retains only the pointer
 * to its parent, a list of the ICell IDs it contains, and stats / profile
 * caches.
 * @author ben
 *
 */
public class ChildAnalysisDataset implements IAnalysisDataset {

	private static final long serialVersionUID = 1L;

	private final Version version;
	
	private IAnalysisDataset parent;
	
	private Set<IAnalysisDataset> childDatasets  = new HashSet<IAnalysisDataset>(); // direct child collections
	
	private ICellCollection cellCollection; // VirtualCellCollection
	
	private Paint datasetColour = null;
	
	private List<IClusterGroup> clusterGroups = new ArrayList<IClusterGroup>(0); // hold groups of cluster results
	
	 public ChildAnalysisDataset(IAnalysisDataset parent, ICellCollection collection){
		 this.parent = parent;
		 this.version = Version.currentVersion();
		 this.cellCollection = collection;
	 }

	@Override
	public IAnalysisDataset duplicate() throws Exception {
		
		throw new Exception("Not yet implemented");
	}

	@Override
	public Handler getLogHandler() throws Exception {
		return parent.getLogHandler();
	}

	@Override
	public Version getVersion() {
		return this.version;
	}

	@Override
	public void addChildCollection(ICellCollection collection) {
		if(collection==null){
			throw new IllegalArgumentException("Nucleus collection is null");
		}

		IAnalysisDataset childDataset = new ChildAnalysisDataset(this, collection);
		this.childDatasets.add( childDataset );
		
	}

	@Override
	public void addChildDataset(IAnalysisDataset dataset) {
		childDatasets.add(dataset);
		
	}

	@Override
	public UUID getUUID() {
		return cellCollection.getID();
	}

	@Override
	public String getName() {
		return cellCollection.getName();
	}

	@Override
	public void setName(String s) {
		cellCollection.setName(s);
		
	}

	@Override
	public File getSavePath() {
		return parent.getSavePath();
	}

	@Override
	public void setSavePath(File file) {}

	@Override
	public File getDebugFile() {
		return parent.getDebugFile();
	}

	@Override
	public void setDebugFile(File f) {}

	@Override
	public Set<UUID> getChildUUIDs() {
		Set<UUID> result = new HashSet<UUID>(childDatasets.size());
		for(IAnalysisDataset c : childDatasets){
			result.add(c.getUUID());
		}
		
		return result;
	}

	@Override
	public Set<UUID> getAllChildUUIDs() {
		Set<UUID> result = new HashSet<UUID>();
		
		Set<UUID> idlist = getChildUUIDs();
		result.addAll(idlist);
		
		for(UUID id : idlist){
			IAnalysisDataset d = getChildDataset(id);
			
			result.addAll(d.getAllChildUUIDs());
		}
		return result;
	}

	@Override
	public IAnalysisDataset getChildDataset(UUID id) {
		if(this.hasChild(id)){
			
			for(IAnalysisDataset c : childDatasets){
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

	@Override
	public IAnalysisDataset getMergeSource(UUID id) {
		return null;
	}

	@Override
	public List<IAnalysisDataset> getAllMergeSources() {
		return new ArrayList<IAnalysisDataset>(0);
	}

	@Override
	public void addMergeSource(IAnalysisDataset dataset) {}

	@Override
	public List<IAnalysisDataset> getMergeSources() {
		return new ArrayList<IAnalysisDataset>(0);
	}

	@Override
	public List<UUID> getMergeSourceIDs() {
		return new ArrayList<UUID>(0);
	}

	@Override
	public List<UUID> getAllMergeSourceIDs() {
		return new ArrayList<UUID>(0);
	}

	@Override
	public boolean hasMergeSource(UUID id) {
		return false;
	}

	@Override
	public boolean hasMergeSource(IAnalysisDataset dataset) {
		return false;
	}

	@Override
	public boolean hasMergeSources() {
		return false;
	}

	@Override
	public int getChildCount() {
		return childDatasets.size();
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
		List<IAnalysisDataset> result = new ArrayList<IAnalysisDataset>();
		result.addAll(childDatasets);
		for(IAnalysisDataset c : childDatasets){
			result.addAll(c.getChildDatasets());
		}
		
		return result;
	}

	@Override
	public ICellCollection getCollection() {
		return cellCollection;
	}

	@Override
	public IAnalysisOptions getAnalysisOptions() {
		return parent.getAnalysisOptions();
	}

	@Override
	public boolean hasAnalysisOptions() {
		return parent.hasAnalysisOptions();
	}

	@Override
	public void setAnalysisOptions(IAnalysisOptions analysisOptions) {}

	@Override
	public void addClusterGroup(IClusterGroup group) {
		this.clusterGroups.add(group);
	}

	@Override
	public int getMaxClusterGroupNumber() {
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

	@Override
	public boolean hasCluster(UUID id) {
		boolean result = false;
		for(IClusterGroup g : this.clusterGroups){
			if(g.hasDataset(id)){
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public List<IClusterGroup> getClusterGroups() {
		return  this.clusterGroups;
	}

	@Override
	public List<UUID> getClusterIDs() {
		List<UUID> result = new ArrayList<UUID>();
		for(IClusterGroup g : this.clusterGroups){
			result.addAll(g.getUUIDs());
		}
		return result;
	}

	@Override
	public boolean hasClusters() {
		return clusterGroups != null && clusterGroups.size()>0;
	}

	@Override
	public boolean hasClusterGroup(IClusterGroup group) {
		return clusterGroups.contains(group);
	}

	@Override
	public void refreshClusterGroups() {
		if(this.hasClusters()){
			// Find the groups that need removing
			List<IClusterGroup> groupsToDelete = new ArrayList<IClusterGroup>();
			for(IClusterGroup g : this.getClusterGroups()){
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
			for(IClusterGroup g : groupsToDelete){
				this.deleteClusterGroup(g);
			}


		}

	}

	@Override
	public boolean isRoot() {
		return false;
	}

	@Override
	public void setRoot(boolean b) {}

	@Override
	public void deleteChild(UUID id) {
		if(this.hasChild(id)){

			this.childDatasets.remove(id);
			for(IClusterGroup g : clusterGroups){
				if(g.hasDataset(id)){
					g.removeDataset(id);
				}
			}
		}
		
	}

	@Override
	public void deleteClusterGroup(IClusterGroup group) {
		if(hasClusterGroup(group)){

			for(UUID id : group.getUUIDs()){
				if(hasChild(id)){
					this.deleteChild(id);
				}
			}
			this.clusterGroups.remove(group);
		}
	}

	@Override
	public void deleteMergeSource(UUID id) {}

	@Override
	public boolean hasChild(IAnalysisDataset child) {
		return childDatasets.contains(child);
	}

	@Override
	public boolean hasRecursiveChild(IAnalysisDataset child) {
		
		for(IAnalysisDataset c : childDatasets){
			if(c.hasRecursiveChild(child)){
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasChild(UUID child) {
		return childDatasets.contains(child);
	}

	@Override
	public void setDatasetColour(Paint colour) {
		datasetColour = colour;
		
	}

	@Override
	public Paint getDatasetColour() {
		return datasetColour;
	}

	@Override
	public boolean hasDatasetColour() {
		return datasetColour!=null;
	}

	@Override
	public void updateSourceImageDirectory(File expectedImageDirectory) {
		parent.updateSourceImageDirectory(expectedImageDirectory);
		
	}
	
	public String toString(){
		return this.cellCollection.getName();
	}

}
