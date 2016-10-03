package gui.tabs.populations;

import java.util.Enumeration;
import java.util.List;

import org.jdesktop.swingx.treetable.DefaultTreeTableModel;

import analysis.AnalysisDataset;
import components.ClusterGroup;
import logging.Loggable;

public class PopulationTreeTableModel extends DefaultTreeTableModel implements Loggable{
		
	public PopulationTreeTableModel(){
		super();
	}

	
//	
	
//	/**
//	 * Get the index of the given cluster group in the model
//	 * @return the index
//	 */
//	public int getRowIndex(ClusterGroup g){
//		int index = 0;
//		
//		PopulationTreeTableNode n = this.getNode(g);
//		n.g
//		
//		for(int row = 0; row< this.getChildCount(this.getRoot()); row++){
//			
////			PopulationTreeTableNode p = this.getValueAt(row, COLUMN_NAME);
//			String populationName = this.getValueAt(row, COLUMN_NAME).toString();
//			
//			if(g.getName().equals(populationName)){
//				index = row;
//			}
//		}
//		return index;
//	}
	
	/**
	 * Move the given nodes one position up in the model. If the node
	 * is at the top of its sib list, this has no effect. The tree hierarchy
	 * is not changed.
	 * @param nodes
	 */
	public void moveNodesDown(List<PopulationTreeTableNode> nodes){

		finer("Selected "+nodes.size()+" nodes");

		for(PopulationTreeTableNode n : nodes){
			PopulationTreeTableNode parent = (PopulationTreeTableNode) n.getParent();

			//get the index of the child in the parent node
			int oldIndex = this.getIndexOfChild(parent, n);

			finest("Old index "+oldIndex);

			//					 if the index is last, do nothing
			if(oldIndex == parent.getChildCount()-1){
				return;
			}

			int sibIndex = oldIndex+1;
			finest("Sib index "+sibIndex);

			// Get the next node up
			PopulationTreeTableNode sib = (PopulationTreeTableNode) parent.getChildAt(sibIndex);
			this.removeNodeFromParent(n);
			this.removeNodeFromParent(sib);

			finest("Old node:  "+n.toString());
			finest("Sib node:  "+sib.toString());

			this.insertNodeInto(sib, parent, oldIndex);
			this.insertNodeInto(n,   parent, sibIndex);

		}
	}
	
	/**
	 * Move the given nodes one position down in the model. If the node
	 * is at the bottom of its sib list, this has no effect. The tree hierarchy
	 * is not changed.
	 * @param nodes
	 */
	public void moveNodesUp(List<PopulationTreeTableNode> nodes){

		finer("Selected "+nodes.size()+" nodes");

		for(PopulationTreeTableNode n : nodes){
			PopulationTreeTableNode parent = (PopulationTreeTableNode) n.getParent();

			//get the index of the child in the parent node
			int oldIndex = this.getIndexOfChild(parent, n);

			finest("Old index "+oldIndex);

			// if the index is first, do nothing
			if(oldIndex == 0){
				return;
			}

			int sibIndex = oldIndex-1;
			finest("Sib index "+sibIndex);

			// Get the next node up
			PopulationTreeTableNode sib = (PopulationTreeTableNode) parent.getChildAt(sibIndex);
			this.removeNodeFromParent(n);
			this.removeNodeFromParent(sib);

			finest("Old node:  "+n.toString());
			finest("Sib node:  "+sib.toString());

			this.insertNodeInto(n,   parent, sibIndex);
			this.insertNodeInto(sib, parent, oldIndex);

		}
	}
	
	/**
	 * Get the node in the tree corresponding to the given group,
	 * or null if no group is found
	 * @param g
	 * @return
	 */
	public PopulationTreeTableNode getNode(ClusterGroup g){
		
		if(g==null){
			throw new IllegalArgumentException("Cluster group cannot be null"); 
		}
		PopulationTreeTableNode result = null;
		
		PopulationTreeTableNode root = (PopulationTreeTableNode) this.getRoot();
		
		Enumeration<PopulationTreeTableNode> en = (Enumeration<PopulationTreeTableNode>) root.children();
		
		while(en.hasMoreElements()){
			PopulationTreeTableNode p = en.nextElement();
			if(p.hasClusterGroup()){
				if(p.getGroup()==g){
					return p;
				}
			}
		}
		return result;
	}
	
	/**
	 * Get the node in the tree corresponding to the given dataset,
	 * or null if no group is found
	 * @param g
	 * @return
	 */
	public PopulationTreeTableNode getNode(AnalysisDataset dataset){
		
		if(dataset==null){
			throw new IllegalArgumentException("Dataset cannot be null"); 
		}
		PopulationTreeTableNode result = null;

		PopulationTreeTableNode root = (PopulationTreeTableNode) this.getRoot();
		
		Enumeration<PopulationTreeTableNode> en = (Enumeration<PopulationTreeTableNode>) root.children();
		
		while(en.hasMoreElements()){
			PopulationTreeTableNode p = en.nextElement();
			if(p.hasDataset()){
				if(p.getDataset()==dataset){
					return p;
				}
			}
		}
		return result;
	}

}
