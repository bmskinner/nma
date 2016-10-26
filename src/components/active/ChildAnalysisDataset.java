package components.active;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;

import analysis.AnalysisOptions;
import analysis.IAnalysisDataset;
import components.ClusterGroup;
import components.ICellCollection;
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
	
	private Color datasetColour = null;
	
	private List<ClusterGroup> clusterGroups = new ArrayList<ClusterGroup>(0); // hold groups of cluster results
	
	 public ChildAnalysisDataset(IAnalysisDataset parent, ICellCollection collection){
		 this.parent = parent;
		 this.version = Version.currentVersion();
	 }

	@Override
	public IAnalysisDataset duplicate() throws Exception {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<UUID> getAllChildUUIDs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IAnalysisDataset getChildDataset(UUID id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IAnalysisDataset getMergeSource(UUID id) {
		return null;
	}

	@Override
	public List<IAnalysisDataset> getAllMergeSources() {
		return null;
	}

	@Override
	public void addMergeSource(IAnalysisDataset dataset) {}

	@Override
	public List<IAnalysisDataset> getMergeSources() {
		return null;
	}

	@Override
	public List<UUID> getMergeSourceIDs() {
		return null;
	}

	@Override
	public List<UUID> getAllMergeSourceIDs() {
		return null;
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
	public AnalysisOptions getAnalysisOptions() {
		return parent.getAnalysisOptions();
	}

	@Override
	public boolean hasAnalysisOptions() {
		return parent.hasAnalysisOptions();
	}

	@Override
	public void setAnalysisOptions(AnalysisOptions analysisOptions) {}

	@Override
	public void addClusterGroup(ClusterGroup group) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getMaxClusterGroupNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasCluster(UUID id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<ClusterGroup> getClusterGroups() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UUID> getClusterIDs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasClusters() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasClusterGroup(ClusterGroup group) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void refreshClusterGroups() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRoot() {
		return false;
	}

	@Override
	public void setRoot(boolean b) {}

	@Override
	public void deleteChild(UUID id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteClusterGroup(ClusterGroup group) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setDatasetColour(Color colour) {
		datasetColour = colour;
		
	}

	@Override
	public Color getDatasetColour() {
		return datasetColour;
	}

	@Override
	public boolean hasDatasetColour() {
		return datasetColour!=null;
	}

	@Override
	public void updateSourceImageDirectory(File expectedImageDirectory) {
		// TODO Auto-generated method stub
		
	}
	
	

}
