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
package gui.tabs.populations;

import gui.DatasetEvent;
import gui.DatasetListManager;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.InterfaceEvent.InterfaceMethod;
import gui.components.ColourSelecter;
import gui.tabs.DetailPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import analysis.AnalysisDataset;
import components.CellCollection;
import components.ClusterGroup;

@SuppressWarnings("serial")
public class PopulationsPanel extends DetailPanel implements SignalChangeListener {
		

	final private PopulationTreeTable treeTable;
	
	private PopulationListPopupMenu populationPopup;
		
	/**
	 * This tracks the ordering of the datasets within the panel
	 */
//	private TreeOrderHashMap treeOrderMap = new TreeOrderHashMap(); // order the root datasets
	
	/**
	 * This tracks which datasets are currently selected, and the order in which they
	 * were selected.  
	 */
	private final Set<AnalysisDataset> datasetSelectionOrder = new LinkedHashSet<AnalysisDataset>();
	
	final private TreeSelectionHandler treeListener = new TreeSelectionHandler();
	
	
	 private boolean ctrlPressed = false;
	    
	 public boolean isCtrlPressed() {
		 synchronized (PopulationsPanel.class) {
			 return ctrlPressed;
		 }
	 }
	
	public PopulationsPanel() {
		super();
		this.setLayout(new BorderLayout());
		
		this.setMinimumSize(new Dimension(100, 100));
		
		populationPopup = new PopulationListPopupMenu();
		populationPopup.setEnabled(false);
		populationPopup.addSignalChangeListener(this);
		
		treeTable = createTreeTable();
		
				
		JScrollPane populationScrollPane = new JScrollPane(treeTable);		
		
		// Track when the Ctrl key is down
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

            @Override
            public boolean dispatchKeyEvent(KeyEvent ke) {
            	synchronized (PopulationsPanel.class) {
                switch (ke.getID()) {
                    case KeyEvent.KEY_PRESSED:
                        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
                        	ctrlPressed = true;
                        }
                        break;

                    case KeyEvent.KEY_RELEASED:
                        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
                        	ctrlPressed = false;
                        }
                        break;
                    }
                    return false;
              }
            }
            
        });
		
		this.add(populationScrollPane, BorderLayout.CENTER);

	}
	
	
	@Override
	public void update(final List<AnalysisDataset> list){
		this.update();
		finest("Preparing to select datasets");
		treeTable.selectDatasets(list);
//		selectDatasets(list);
		treeTable.repaint();
	}
	
	public void update(final AnalysisDataset dataset){
		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		list.add(dataset);
		update(list);
	}
		

	/**
	 *  Find the populations in memory, and display them in the population chooser. 
	 *  Root populations are ordered according to position in the treeListOrder map.
	 */
	private void update(){
		
		int nameColWidth   = treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_NAME).getWidth();
		int colourColWidth = treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR).getWidth();

		/*
		 * Determine the ids of collapsed datasets, and store them
		 */
		finest("Storing collapsed rows");
		List<Object> collapsedRows = treeTable.getCollapsedRows();		
		
		
		PopulationTreeTableModel oldModel = (PopulationTreeTableModel) treeTable.getTreeTableModel();
		
		// Need to modify the model, not replace it to keep ordering
//		PopulationTreeTableModel newModel = createTableModel();
		PopulationTreeTableModel newModel = new PopulationTreeTableModel();
		treeTable.setTreeTableModel(newModel);

		finer("Set the tree table model");
		
		
		
		/*
		 * Collapse the same ids as saved earlier
		 */
		treeTable.setCollapsedRows(collapsedRows);
		
		finer("Restoring column widths");
		treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_NAME).setWidth(nameColWidth);
		treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR).setWidth(colourColWidth);

		finer("Update complete");
	}

	
	private PopulationTreeTable createTreeTable(){

		PopulationTreeTableModel treeTableModel = new PopulationTreeTableModel();

		PopulationTreeTable table = new PopulationTreeTable(treeTableModel);	
		
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

				PopulationTreeTable table = (PopulationTreeTable) e.getSource();
				
				int row		= table.rowAtPoint((e.getPoint()));
				int column 	= table.columnAtPoint(e.getPoint());
				
				Object o = table.getModel().getValueAt(row, PopulationTreeTable.COLUMN_NAME);
				
				// double click
				if (e.getClickCount() == 2) {

					if(o instanceof ClusterGroup){
						clusterGroupClicked((ClusterGroup) o);
					}
					
					if(o instanceof AnalysisDataset){
						datasetClicked((AnalysisDataset) o, row, column);
					}					
				}
				
				// right click  - show the popup, but change delete to close for root datasets
				if(e.getButton() == MouseEvent.BUTTON3){
										
					if(o instanceof AnalysisDataset){
						if(((AnalysisDataset) o).isRoot()){
							populationPopup.setDeleteString("Close");
						} else {
							populationPopup.setDeleteString("Delete");
						}
					} else {
						populationPopup.setDeleteString("Delete");
					}				
					
					populationPopup.show(table, e.getX(), e.getY());
				}
			}
			
			private void clusterGroupClicked(ClusterGroup g){

			}
			
			private void datasetClicked(AnalysisDataset d, int row, int column){

				switch(column){
				
					case PopulationTreeTable.COLUMN_NAME:{
						renameCollection(d);
						break;
					}
					
					case PopulationTreeTable.COLUMN_COLOUR:{
						changeDatasetColour(d, row);
						break;
					}
					
					default:
						break;
					
				}				
			}
		});
		
		TreeSelectionModel tableSelectionModel = table.getTreeSelectionModel();
		tableSelectionModel.addTreeSelectionListener(treeListener);
		table.setTreeSelectionListener(treeListener);
		
		return table;
	}
	
	/**
	 * Make a JColorChooser for the given dataset, and set the color.
	 * @param dataset
	 * @param row
	 */
	private void changeDatasetColour(AnalysisDataset dataset, int row){
		Color oldColour = ColourSelecter.getColor( row );
		
		Color newColor = JColorChooser.showDialog(
				PopulationsPanel.this,
                 "Choose dataset Color",
                 oldColour);
		
		if(newColor != null){
			dataset.setDatasetColour(newColor);
			
			// Force the chart caches to clear, but don't trigger a panel update
			finest("Firing clearing chart cache signals from population colour change");
			fireDatasetEvent(DatasetEvent.REFRESH_CACHE, dataset);
		}
		fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
	}
	
	
	/**
	 * Get the datasets currently selected
	 * @return
	 */
	public synchronized List<AnalysisDataset> getSelectedDatasets(){

		return new ArrayList<AnalysisDataset>(datasetSelectionOrder);
	}


	
	
	/**
	 * Add the given dataset to the main population list
	 * Check that the name is valid, and update if needed
	 * @param dataset the dataset to add
	 */
	public void addDataset(AnalysisDataset dataset){
				
		if(dataset.isRoot()){ // add to the list of datasets that can be ordered
//			treeOrderMap.put(dataset.getUUID(), treeOrderMap.size()); // add to the end of the list
			fine("Adding root dataset "+dataset.getName()+" to list manager");
			DatasetListManager.getInstance().addDataset(dataset);
			
			
			
		}
//		PopulationTreeTableModel model = (PopulationTreeTableModel) treeTable.getTreeTableModel();
//		model.addDataset(dataset);
	}
	
	
	/**
	 * Select the given dataset in the tree table
	 * @param dataset the dataset to select
	 */
	public void selectDataset(AnalysisDataset dataset){
		if(dataset!=null){
			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			list.add(dataset);
			treeTable.selectDatasets(list);
//			selectDatasets(list);
		}
	}
	
	
	/**
	 * Select the given datasets in the tree table
	 * @param dataset the dataset to select
	 */
	public void selectDatasets(List<AnalysisDataset> list){
		treeTable.selectDatasets(list);
	}
	
	public void repaintTreeTable(){
		treeTable.repaint();
	}
	
	public void selectDataset(UUID id){
		this.selectDataset(DatasetListManager.getInstance().getDataset(id));
	}
				
	/**
	 * Rename an existing dataset and update the population list.
	 * @param dataset the dataset to rename
	 */

	private void renameCollection(AnalysisDataset dataset){
		CellCollection collection = dataset.getCollection();
		String newName = (String) JOptionPane.showInputDialog(this, 
				"Choose a new name", 
				"Rename collection", 
				JOptionPane.INFORMATION_MESSAGE, 
				null, 
				null,
				collection.getName());
		
		// validate
		if( newName==null || newName.isEmpty()){
			fine("New name null or empty");
			return;
		}
		

		// Get the existing names and check duplicates
		List<String> currentNames = treeTable.getDatasetNames();

		if(currentNames.contains(newName)){
			fine("Checking duplicate name is OK");
			int result = JOptionPane.showConfirmDialog(this, 
					"Chosen name exists. Use anyway?");

			if(result!= JOptionPane.OK_OPTION){
				log("User cancelled name change");
				return;
			}
		}
		
		collection.setName(newName);

		log("Collection renamed: "+newName);


		File saveFile = dataset.getSavePath();
		if(saveFile.exists()){
			saveFile.delete();
		}

		fireDatasetEvent(DatasetEvent.SAVE, dataset);

		update(dataset);
		
	}
	
	
	/**
	 * Move the selected dataset  in the list
	 * @param isDown move the dataset down (true) or up (false)
	 */
	private void moveDataset(boolean isDown) {
		finer("Move dataset heard");
		List<AnalysisDataset> datasets = getSelectedDatasets();
		List<PopulationTreeTableNode> nodes = treeTable.getSelectedNodes();
		
		if(nodes.isEmpty() || nodes.size()>1){
			return;
		}

		// May be a dataset or cluster group selected
		AnalysisDataset datasetToMove = datasets.isEmpty() ? null : datasets.get(0);

		// Get the node containing the dataset
		PopulationTreeTableModel model = (PopulationTreeTableModel) treeTable.getTreeTableModel();
		
		if(isDown){
			model.moveNodesDown(nodes);
		} else {
			model.moveNodesUp(nodes);
		}

		if(datasets!=null){
			selectDataset(datasetToMove);
		}

	}
	
		
	private void deleteDataset(AnalysisDataset d){
		
		try{

			finer("Deleting dataset: "+d.getName());
			UUID id = d.getUUID();


			// remove the dataset from its parents
			finer("Removing dataset from its parents");
			for(AnalysisDataset parent : DatasetListManager.getInstance().getAllDatasets()){ //analysisDatasets.keySet()){
//				AnalysisDataset parent = analysisDatasets.get(parentID);

				finest("Parent dataset "+parent.getName());

				if(parent.hasChild(id)){
					finest("    Parent contains dataset; deleting");
					parent.deleteChild(id);
				}

			}
			
			finer("Checking if dataset is root");
			
			if(d.isRoot()){
				finer("Removing dataset from treeOrderMap and list manager");
//				treeOrderMap.remove(id);
				DatasetListManager.getInstance().removeDataset(d);
			} else {
				finer("Dataset is not root");
			}
			finest("Clearing dataset from memory");
			
			d=null; // clear from memory
			finest("Deleted dataset");
		} catch (Exception e){
			log(Level.SEVERE, "Error deleting dataset "+d.getName(), e);
		}
	}
	
	/**
	 * Recursively delete datasets. Remove all datasets with no children
	 * from the list, then call this method again on all the remaining ids
	 * @param ids
	 */
	private void deleteDatasetsInList(Set<UUID> ids){
		
		if(ids.isEmpty()){
			return;
		}
		
		Set<UUID> keepIds = new HashSet<UUID>();
		finest("Candidate delete list has "+ids.size()+" datasets");
		Iterator<UUID> it = ids.iterator();
		while(it.hasNext()){
			UUID id = it.next();
			AnalysisDataset d = DatasetListManager.getInstance().getDataset(id);
			
			if( ! d.hasChildren()){
				finest("Preparing to delete dataset: "+d.getName());
				deleteDataset(d);
			} else {
				finest("Dataset "+d.getName()+" still has children");
				keepIds.add(id);
			}
		}
		
		deleteDatasetsInList(keepIds);
	}
	
	private void deleteSelectedDatasets(){
		final List<AnalysisDataset>         datasets = getSelectedDatasets();
		final List<PopulationTreeTableNode> nodes    = treeTable.getSelectedNodes();
		
		// Check if cluster groups need removing
		if(nodes.size()>datasets.size()){
			// cluster groups are also selected, add to list
			for(PopulationTreeTableNode n : treeTable.getSelectedNodes()){

				if(n.hasClusterGroup()){
					ClusterGroup g = n.getGroup();
					for(UUID childID : g.getUUIDs()){
						AnalysisDataset child = DatasetListManager.getInstance().getDataset(childID);
						datasets.add(child);
					}
					
				}
			}
		}
		
		if(datasets.isEmpty()){
			finest("No datasets selected");
			return;
		}
		
		// Now extract the unique UUIDs of all datasets to be deleted (including children)
		finest("There are "+datasets.size()+" datasets selected");

		Set<UUID> list = unique(datasets);

		deleteDatasetsInList(list);
		DatasetListManager.getInstance().refreshClusters(); // remove unneeded cluster groups from datasets
		
		finest("Updating cluster groups in tree panel");
		
		// remove any empty cluster groups
		PopulationTreeTableModel model = (PopulationTreeTableModel) treeTable.getTreeTableModel();

//		PopulationTreeTableNode root = (PopulationTreeTableNode)model.getRoot();
//		for(PopulationTreeTableNode n : getSelectedNodes()){
//				
//			if(n.hasDataset() && datasets.contains(n.getDataset())){
//				n.removeFromParent();
////				int index = root.getIndex(n);
////				root.remove(index);
//			}
//			
//			
//			if(n.hasClusterGroup()){
//				ClusterGroup g = n.getGroup();
//				boolean canRemove = true;
//				for(AnalysisDataset parent : DatasetListManager.getInstance().getAllDatasets()){ 
//
//					finest("Parent dataset "+parent.getName());
//
//					for(UUID child : g.getUUIDs()){
//						if(parent.hasChild(child)){
//							canRemove = false; // dataset in group still exists
//
//						}
//					}
//
//				}
//				if(canRemove){
////					int index = root.getIndex(n);
////					root.remove(index);
//					n.removeFromParent();
//				}
//			}
//		}
//		model.setRoot(root);


		//			treeTable.updateUI();
					update();
		finest("Firing update panel event");
		fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);


		finest("Deletion complete");

	}
	
	private Set<UUID> unique(List<AnalysisDataset> list){
		Set<UUID> set = new HashSet<UUID>();
		for(AnalysisDataset d : list){
			finest("Selected dataset for deletion: "+d.getName());

			set.add(d.getUUID());

			if(d.hasChildren()){
				finest("Children found in: "+d.getName());
				// add all the children of a dataset
				for(UUID childID : d.getAllChildUUIDs()){
					finest("Adding child dataset to deletion list: "+childID.toString());
					set.add(childID);
				}
			} else {
				finest("No children in: "+d.getName());
			}
		}
		return set;
	}

	/**
	 * Establish the rows in the population tree that are currently selected.
	 * Set the possible menu options accordingly, and call the panel updates
	 */
	public class TreeSelectionHandler implements TreeSelectionListener {
		public void valueChanged(TreeSelectionEvent e) {
			
			try {
				
//				log("Ctrl down: "+isCtrlPressed());
				
				if( ! isCtrlPressed()){

					datasetSelectionOrder.clear();
				}
								
				// Track the datasets currently selected
				List<AnalysisDataset> datasets = new ArrayList<AnalysisDataset>(0);

				TreeSelectionModel lsm = (TreeSelectionModel)e.getSource();
				int totalSelectionCount = lsm.getSelectionCount();
				Map<Integer, Integer> selectedIndexes = new HashMap<Integer, Integer>(0);
				
				if (!lsm.isSelectionEmpty()) {
					// Find out which indexes are selected.
					int minIndex = lsm.getMinSelectionRow();
					int maxIndex = lsm.getMaxSelectionRow();
					for (int i = minIndex; i <= maxIndex; i++) {
						if (lsm.isRowSelected(i)) {
							
							if(treeTable.isDataset(i)){
								
								AnalysisDataset d = treeTable.getDatasetAtRow(i);
								datasets.add(d);
								
								datasetSelectionOrder.add(d);
								
								int selectionIndex = 0;
								for(AnalysisDataset an : datasetSelectionOrder){
									
									if(an==d){
										selectedIndexes.put(i, selectionIndex);
										break;
									}
									selectionIndex++;
								}
								
							}
														
						}
					}
					
					// Ctrl deselect happened - a dataset has been deselected and remains in the
					// datasetSelectionOrder map
					if(datasetSelectionOrder.size() > datasets.size()){
						// Go through tree table and check for deselected dataset
						Iterator<AnalysisDataset> it= datasetSelectionOrder.iterator();
						
						while(it.hasNext()){
							AnalysisDataset d = it.next();
							if(! datasets.contains(d)){
								it.remove();
							}	
						}
						
						// Adjust the indexes of the remaining datasets
						fixDiscontinuousPositions(selectedIndexes);
						
					}
					
					
					PopulationTableCellRenderer rend = new PopulationTableCellRenderer(selectedIndexes);
					treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR).setCellRenderer(rend);

					if(datasets.isEmpty() && totalSelectionCount==0){
						populationPopup.setEnabled(false);
					} else {

						if(totalSelectionCount>1){ // multiple of datasets or clusters
							// single dataset
								populationPopup.setEnabled(false);
								populationPopup.enableMerge();
								populationPopup.enableDelete();
								populationPopup.enableBoolean();
							

						} else { // single population
							
							if(datasets.size()==1){ // single datasets
								AnalysisDataset d = datasets.get(0);
								setMenuForSingleDataset(d);
							} else {
								// single clustergoup
								populationPopup.enableMenuUp();
								populationPopup.enableMenuDown();
							}
						}
						finer("Firing update panel event due to tree selection");
						fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
					}
				}
			} catch(Exception ex){
				error("Error in tree selection handler", ex);
			}
		}
		
		private Map<Integer, Integer> fixDiscontinuousPositions(Map<Integer, Integer> selectedIndexes){
			// Find a discontinuity in the indexes - one value is missing
			List<Integer> values = new ArrayList<Integer>(selectedIndexes.values());
			Collections.sort(values);
			
			int prev = -1;
			for(int i : values){
				if ( i - prev > 1){
					// a value was skipped
					for(int k : selectedIndexes.keySet()){
						int j = selectedIndexes.get(k);
						if(j ==i){ // this is the entry that is too high
							selectedIndexes.put(k, j-1); // Move index down by 1
						}
					}
					fixDiscontinuousPositions(selectedIndexes); // there will now be a new discontinuity. Fix until end of list
				}
				prev = i;
			}
			return selectedIndexes;
		}
		
//		private boolean rowIsDataset(int i){
//			Object columnOneObject = treeTable.getModel().getValueAt(i, PopulationTreeTable.COLUMN_NAME);
//			if(columnOneObject instanceof AnalysisDataset){
//				return true;
//			}
//			return false;
//		}
//		
//		private AnalysisDataset getDatasetAtRow(int i){
//			Object columnOneObject = treeTable.getModel().getValueAt(i, PopulationTreeTable.COLUMN_NAME);
//
//			if(columnOneObject instanceof AnalysisDataset){
//				return (AnalysisDataset) treeTable.getModel().getValueAt(i, PopulationTreeTable.COLUMN_NAME); // row i, column 0
//			}
//			return null;
//		}
		
	}
		
	private void setMenuForSingleDataset(AnalysisDataset d){
		
		populationPopup.enableDelete();
		populationPopup.disableMerge();
		populationPopup.enableBoolean();
		populationPopup.enableSave();
		populationPopup.enableCurate();
		populationPopup.enableRelocateCells();
		populationPopup.enableSaveCells();
		populationPopup.enableAddNuclearSignal();
		
		populationPopup.enableMenuUp();
		populationPopup.enableMenuDown();
		// check if we can move the dataset
//		if(d.isRoot()){
//
//			if(DatasetListManager.getInstance().count()>1){
//
//				// check if the selected dataset is at the top of the list
//				if(DatasetListManager.getInstance().getRootDatasets().get(0).getUUID().equals(d.getUUID())){
//					populationPopup.disableMenuUp();
//				} else {
//					populationPopup.enableMenuUp();
//				}
//
//				// check if the selected dataset is at the bottom of the list
//				if(DatasetListManager.getInstance().getRootDatasets().get(DatasetListManager.getInstance().getRootDatasets().size()-1).getUUID().equals(d.getUUID())){
//					populationPopup.disableMenuDown();
//				} else {
//					populationPopup.enableMenuDown();
//				}
//
//			} else { // only one or zero datasets in the pogram 
//				populationPopup.disableMenuUp();
//				populationPopup.disableMenuDown();
//			}
//
//			// only root datasets can replace folder mappings
//			populationPopup.enableReplaceFolder();
//
//		} else { // not root
//						
//			populationPopup.disableReplaceFolder();
//			populationPopup.disableMenuUp();
//			populationPopup.disableMenuDown();
//		}

	}
		

	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
							
		switch(event.type()){
			
			// catch any signals that affect the datasets directly
		
			case "MoveDatasetDownAction":{
				moveDataset(true);
				break;
			}
			
			case "MoveDatasetUpAction":{
				moveDataset(false);
				break;
			}
			
			case "DeleteCollectionAction":{
				deleteSelectedDatasets();
				break;
			}
			
			default: {
				// Pass on events from the popup menu
				if(event.sourceName().equals(PopulationListPopupMenu.SOURCE_COMPONENT)){
					fireSignalChangeEvent(event.type());
					finest("Firing signal change event: "+event.type());
				}
				break;
			}
			
		}
	
	}
	
}
