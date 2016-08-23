package gui.tabs.populations;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;

import analysis.AnalysisDataset;
import components.ClusterGroup;

public class PopulationTreeTableNode extends AbstractMutableTreeTableNode {
	
	Object[] columnData = new Object[3];

	AnalysisDataset dataset = null; // the dataset in the node
	ClusterGroup group      = null;

	public PopulationTreeTableNode(AnalysisDataset dataset) {
		super(dataset.getUUID().toString());
		this.dataset = dataset;
		columnData[0] = dataset;
		columnData[1] = dataset.getCollection().getNucleusCount();
	}
	
	public PopulationTreeTableNode() {
		super();
	}
	
	public PopulationTreeTableNode(ClusterGroup group) {
		super(UUID.randomUUID().toString());
		this.group = group; 
		columnData[0] = group;
		columnData[1] = "";
		
	}
	
	public boolean hasDataset(){
		return dataset!=null;
	}
	
	public boolean hasClusterGroup(){
		return group!=null;
	}
			
	public AnalysisDataset getDataset() {
		return dataset;
	}

	public ClusterGroup getGroup() {
		return group;
	}

	public int getColumnCount() {
	    return 3;
	}

	public Object getValueAt(int column){
		return columnData[column];
	}
	
	public void setValueAt(Object aValue, int column){
		columnData[column] = aValue;
	}
	
	public String toString(){
		if(this.hasDataset()){
			return dataset.getName();
		}
		if(this.hasClusterGroup()){
			return group.toString();
		}
		return "No name found";
	}
	
	/**
	 * This method recursively (or not) sorts the nodes, ascending, or descending by the specified column.
	 * @param sortColumn Column to do the sorting by.
	 * @param sortAscending Boolean value of weather the sorting to be done ascending or not (descending).
	 * @param recursive Boolean value of weather or not the sorting should be recursively applied to children nodes.
	 * @author Alex Burdu Burdusel
	 */
	public void sortNode(int sortColumn, boolean sortAscending, boolean recursive) {

	    int childCount = this.getChildCount();
	    	    
	    TreeMap<Object, PopulationTreeTableNode> nodeData = new TreeMap( String.CASE_INSENSITIVE_ORDER);

	    for (int i = 0; i < childCount; i++) {
	    	PopulationTreeTableNode child = (PopulationTreeTableNode) this.getChildAt(i);
	        if (child.getChildCount() > 0 & recursive) {
	            child.sortNode(sortColumn, sortAscending, recursive);
	        }
	        Object key = child.getValueAt(sortColumn);
	        nodeData.put(key, child);
	    }

	    Iterator<Map.Entry<Object, PopulationTreeTableNode>> nodesIterator;
	    if (sortAscending) {
	        nodesIterator = nodeData.entrySet().iterator();
	    } else {
	        nodesIterator = nodeData.descendingMap().entrySet().iterator();
	    }

	    while (nodesIterator.hasNext()) {
	        Map.Entry<Object, PopulationTreeTableNode> nodeEntry = nodesIterator.next();
	        this.add(nodeEntry.getValue());
	    }
	}
}
