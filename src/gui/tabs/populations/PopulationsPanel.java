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


import gui.DatasetListManager;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import gui.components.ColourSelecter;
import gui.tabs.DetailPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeSelectionModel;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jfree.chart.JFreeChart;

import charting.options.ChartOptions;
import charting.options.TableOptions;
import utility.TreeOrderHashMap;
import analysis.AnalysisDataset;
import components.CellCollection;
import components.ClusterGroup;

@SuppressWarnings("serial")
public class PopulationsPanel extends DetailPanel implements SignalChangeListener {
	
	public static final int COLUMN_NAME       = 0;
	public static final int COLUMN_CELL_COUNT = 1;
	public static final int COLUMN_COLOUR     = 2;

	final private JXTreeTable treeTable;
	private PopulationListPopupMenu populationPopup;
		
	private TreeOrderHashMap treeOrderMap = new TreeOrderHashMap(); // order the root datasets
	
	final private TreeSelectionHandler treeListener = new TreeSelectionHandler();
	
	public PopulationsPanel() {
		super();
		this.setLayout(new BorderLayout());
		
		this.setMinimumSize(new Dimension(100, 100));
		
		populationPopup = new PopulationListPopupMenu();
		populationPopup.disableAll();
		populationPopup.addSignalChangeListener(this);
		
		treeTable = createTreeTable();
		
				
		JScrollPane populationScrollPane = new JScrollPane(treeTable);		
		
		this.add(populationScrollPane, BorderLayout.CENTER);

	}
	
	
	@Override
	public void update(final List<AnalysisDataset> list){
		this.update();
		finest("Preparing to select datasets");
		selectDatasets(list);
		treeTable.repaint();
	}
		
	/**
	 * Find the datasets which are collapsed in the tree
	 * @return
	 */
	private List<Object> getCollapsedRows(){
		List<Object> collapsedRows = new ArrayList<Object>();		
		for (int row = 0; row < treeTable.getRowCount(); row++) {
			if(!treeTable.isExpanded(row)){
				
				Object columnOneObject = treeTable.getModel().getValueAt(row, PopulationsPanel.COLUMN_NAME);
				collapsedRows.add(columnOneObject);
			}
		}
		finest("Got all collapsed rows");
		return collapsedRows;
	}
	
	
	/**
	 * Set the dataset rows with the given IDs to be collapsed
	 * @param collapsedRows
	 */
	private void setCollapsedRows(List<Object> collapsedRows){
		if(DatasetListManager.getInstance().hasDatasets()){

			finest("Expanding rows");
			for (int row = 0; row < treeTable.getRowCount(); row++) {
				
				Object columnOneObject = treeTable.getModel().getValueAt(row, PopulationsPanel.COLUMN_NAME);
				
				if(collapsedRows.contains(columnOneObject)){
					treeTable.collapseRow(row);	
				} else {
					treeTable.expandRow(row);
				}
			}
		}
	}
	
	/**
	 *  Find the populations in memory, and display them in the population chooser. 
	 *  Root populations are ordered according to position in the treeListOrder map.
	 */
	private void update(){
		
		int nameColWidth   = treeTable.getColumnModel().getColumn(COLUMN_NAME).getWidth();
		int colourColWidth = treeTable.getColumnModel().getColumn(COLUMN_COLOUR).getWidth();

		/*
		 * Determine the ids of collapsed datasets, and store them
		 */
		finest("Storing collapsed rows");
		List<Object> collapsedRows = getCollapsedRows();		
		
		finest("Creating columns");
		List<String> columns = new ArrayList<String>();
		columns.add("Population");
		columns.add("Nuclei");
		columns.add("");

		finest("Creating tree table model");
		DefaultTreeTableModel treeTableModel = new DefaultTreeTableModel();
		PopulationTreeTableNode  root = new PopulationTreeTableNode();
		treeTableModel.setRoot(root);
		treeTableModel.setColumnIdentifiers(columns);
		finer("Created tree table model");
		
		try {

			if(DatasetListManager.getInstance().hasDatasets()){
				
				finer("List manager has "+DatasetListManager.getInstance().count()+" datasets");

				for(AnalysisDataset rootDataset : DatasetListManager.getInstance().getRootDatasets()){
					finer("Adding "+rootDataset.getName()+" as node");
					root.add( addTreeTableChildNodes(    rootDataset    )     );
				}
				
				finer("Added datasets to nodes");

			} else {
				finer("No datasets loaded");
			}
		} catch(Exception e){
			error("Error adding nodes to table model", e);
		}

		treeTable.setTreeTableModel(treeTableModel);

		finer("Set the tree table model");
		
		
		
		/*
		 * Collapse the same ids as saved earlier
		 */
		setCollapsedRows(collapsedRows);
		
		finer("Restoring column widths");
		treeTable.getColumnModel().getColumn(COLUMN_NAME).setWidth(nameColWidth);
		treeTable.getColumnModel().getColumn(COLUMN_COLOUR).setWidth(colourColWidth);
//		treeTable.repaint();
		finer("Update complete");
	}
	
	/**
	 * Ensure that all child datasets are present within the  
	 * analysisDatasets Map
	 */
//	public void refreshDatasets(){
//		
//		if(DatasetListManager.getInstance().hasDatasets()){
////		if(this.analysisDatasets.size()>0){
//			for(UUID id : treeOrderMap.getIDs()){
////				AnalysisDataset rootDataset = DatasetListManager.getInstance().getDataset(id);					
////				AnalysisDataset rootDataset = analysisDatasets.get(id);
////				for(AnalysisDataset child : rootDataset.getAllChildDatasets()){
////					if( ! this.hasDataset(child.getUUID())){
//////						child.setName(checkName(child.getName(), child.getUUID()));
////						this.analysisDatasets.put(child.getUUID(), child);
//////						this.populationNames.put(child.getName(), child.getUUID());
////					}
////				}
//				
//			}
//		}
//		update();
//	}
	
	/**
	 * Update the cluster groups for each root dataset and its children.
	 * This will remove any cluster groups with no member datasets. 
	 */
	public void refreshClusters(){
		try {
		finest("Refreshing clusters...");
		if(DatasetListManager.getInstance().hasDatasets()){

			for(UUID id : treeOrderMap.getIDs()){
												
				AnalysisDataset rootDataset = DatasetListManager.getInstance().getDataset(id);
				finest("  Root dataset "+rootDataset.getName());
				rootDataset.refreshClusterGroups();
				for(AnalysisDataset child : rootDataset.getAllChildDatasets()){
					finest("    Child dataset "+child.getName());
					child.refreshClusterGroups();
				}
				
			}
		}
		} catch (Exception e){
			log(Level.SEVERE, "Error refreshing clusters", e);
		}
	}
	
	/**
	 * Create a node in the tree table, recursively adding all
	 * the children of the given dataset id. If the child of a
	 * dataset is not already in the names list, add it
	 * @param dataset the dataset to add
	 * @return
	 */
	private PopulationTreeTableNode addTreeTableChildNodes(AnalysisDataset dataset){
		
		if(dataset==null){
			throw new IllegalArgumentException("Dataset is null when generating population table nodes");
		}
		
		UUID id = dataset.getUUID();
		if( ! DatasetListManager.getInstance().hasDataset(id)){
			finer("Adding missing dataset to list manager");
			this.addDataset(dataset);
		}

		PopulationTreeTableNode category = new PopulationTreeTableNode(dataset);
				
		// Add cluster groups separately
		Set<UUID> clusterIDs = new HashSet<UUID>(); // track the child datasets in clusters, so they are not added twice
		
		for(ClusterGroup group : dataset.getClusterGroups()){
			fine("Making node for cluster group "+group.getName());
			PopulationTreeTableNode clusterGroupNode = new PopulationTreeTableNode(group);
			category.add(clusterGroupNode);
			
			for(UUID clusterID : group.getUUIDs()){
				AnalysisDataset clusterDataset = DatasetListManager.getInstance().getDataset(clusterID);
				PopulationTreeTableNode childNode = addTreeTableChildNodes(clusterDataset);
				clusterGroupNode.add(childNode);
				clusterIDs.add(clusterID);
			}
		
		}
		
		// Add remaining child datasets not in clusters
		
		for(AnalysisDataset childDataset : dataset.getChildDatasets()){
			if( ! clusterIDs.contains(childDataset.getUUID())){
				PopulationTreeTableNode childNode = addTreeTableChildNodes(childDataset);
				category.add(childNode);
			}
		}
		finer("Added all child nodes for dataset "+dataset.toString());
		
//		category.sortNode(COLUMN_NAME, true, false);
		return category;
	}
	
	
	private JXTreeTable createTreeTable(){
		
		// tree table approach
		List<String> columns = new ArrayList<String>();
		columns.add("Population");
		columns.add("Nuclei");
		columns.add("");
		
		DefaultTreeTableModel treeTableModel = new DefaultTreeTableModel();
		DefaultMutableTreeTableNode  root = new DefaultMutableTreeTableNode ("root node");
		treeTableModel.setRoot(root);
		treeTableModel.setColumnIdentifiers(columns);
		
		JXTreeTable table = new JXTreeTable(treeTableModel);
		table.setEnabled(true);
		table.setCellSelectionEnabled(false);
		table.setColumnSelectionAllowed(false);
		table.getTableHeader().setReorderingAllowed(false);
		table.setRowSelectionAllowed(true);
		table.setAutoCreateColumnsFromModel(false);
		table.getColumnModel().getColumn(COLUMN_COLOUR).setCellRenderer(new PopulationTableCellRenderer());
		table.getColumnModel().getColumn(COLUMN_NAME).setPreferredWidth(120);
		table.getColumnModel().getColumn(COLUMN_COLOUR).setPreferredWidth(5);
				
		table.setComponentPopupMenu(populationPopup);
		
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				JXTreeTable table = (JXTreeTable) e.getSource();
				
				int row		= table.rowAtPoint((e.getPoint()));
				int column 	= table.columnAtPoint(e.getPoint());
				String populationName = table.getModel().getValueAt(row, 0).toString();
				
				if( ! populationName.startsWith("ClusterGroup")){ // Only allow datasets to change

					// double click
					if (e.getClickCount() == 2) {

						AnalysisDataset dataset = (AnalysisDataset) treeTable.getModel().getValueAt(row, COLUMN_NAME); // row i, column 0

						if (row >= 0 && column == 0) { // first (names) column						
							renameCollection(dataset);
						}

						if(row >= 0 && column == 2){ // third (colours) column
							changeDatasetColour(dataset, row);

						}
					}
				}
			}
		});
		
		TreeSelectionModel tableSelectionModel = table.getTreeSelectionModel();
		tableSelectionModel.addTreeSelectionListener(treeListener);
		
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
			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			list.add(dataset);
			fireDatasetEvent(DatasetMethod.REFRESH_CACHE, list);
		}
		fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
	}
	
	
	/**
	 * Get the datasets currently selected
	 * @return
	 */
	public synchronized List<AnalysisDataset> getSelectedDatasets(){

		List<AnalysisDataset> datasets = new ArrayList<AnalysisDataset>(0);

		TreeSelectionModel lsm = treeTable.getTreeSelectionModel();

		List<Integer> selectedIndexes = new ArrayList<Integer>(0);

		if (!lsm.isSelectionEmpty()) {
			// Find out which indexes are selected.
			int minIndex = lsm.getMinSelectionRow();
			int maxIndex = lsm.getMaxSelectionRow();
			for (int i = minIndex; i <= maxIndex; i++) {
				if (lsm.isRowSelected(i)) {

					
					Object columnOneObject = treeTable.getModel().getValueAt(i, COLUMN_NAME);
					
					if(columnOneObject instanceof ClusterGroup){
						continue;
					}

					if(columnOneObject instanceof AnalysisDataset){
						AnalysisDataset d = (AnalysisDataset) treeTable.getModel().getValueAt(i, COLUMN_NAME); // row i, column 0
						datasets.add(d);
						selectedIndexes.add(i);
					}

				}
			}
		}
		return datasets;
	}
	
	
	private List<Integer> getSelectedDatasetIndexes(){
		List<Integer> result = new ArrayList<Integer>();
		List<AnalysisDataset> datasets = getSelectedDatasets();
		for(AnalysisDataset d : datasets){
			result.add( getIndexOfDataset(d));
		}
		return result;
		
	}
	
	/**
	 * Add the given dataset to the main population list
	 * Check that the name is valid, and update if needed
	 * @param dataset the dataset to add
	 */
	public void addDataset(AnalysisDataset dataset){
		
		if(dataset.isRoot()){ // add to the list of datasets that can be ordered
			treeOrderMap.put(dataset.getUUID(), treeOrderMap.size()); // add to the end of the list
			fine("Adding root dataset "+dataset.getName()+" to list manager");
			DatasetListManager.getInstance().addDataset(dataset);
		}
		
	}
	
	/**
	 * Select the given dataset in the tree table
	 * @param dataset the dataset to select
	 */
	public void selectDataset(AnalysisDataset dataset){
		if(dataset!=null){
			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			list.add(dataset);
			selectDatasets(list);
		}
	}
	
	/**
	 * Select the given datasets in the tree table
	 * @param dataset the dataset to select
	 */
	public void selectDatasets(List<AnalysisDataset> list){
		finer("Selecting list of "+list.size()+" datasets in populations panel");
		for(AnalysisDataset dataset : list){
			int index = getIndexOfDataset(dataset);

			ListSelectionModel selectionModel = 
					treeTable.getSelectionModel();
			
			TreeSelectionModel treeSelectionModel = treeTable.getTreeSelectionModel();
			
			finest("Removing tree selection listener");
			treeSelectionModel.removeTreeSelectionListener(treeListener); // if we don't remove the listener, the clearing will trigger an update
			finest("Clearing tree selection");
			selectionModel.clearSelection(); // if the new selection is the same as the old, the charts will not recache
			finest("Restoring tree selection listener");
			treeSelectionModel.addTreeSelectionListener(treeListener);
			
			finest("Adding index at "+index);
			selectionModel.addSelectionInterval(index, index);
			

		}
		
		// Update the table colours
		List<Integer> selectedIndexes = getSelectedDatasetIndexes();
		PopulationTableCellRenderer rend = (PopulationTableCellRenderer) treeTable.getColumnModel().getColumn(COLUMN_COLOUR).getCellRenderer();
		rend.update(selectedIndexes);
	}
	
	public void repaintTreeTable(){
		treeTable.repaint();
	}
	
	public void selectDataset(UUID id){
		this.selectDataset(DatasetListManager.getInstance().getDataset(id));
	}
	
	/**
	 * Get the index of the given dataset in the tree table.
	 * @return the index
	 */
	private int getIndexOfDataset(AnalysisDataset dataset){
		int index = 0;
		
		for(int row = 0; row< treeTable.getRowCount(); row++){
			
			String populationName = treeTable.getModel().getValueAt(row, COLUMN_NAME).toString();
			
			if(dataset.getName().equals(populationName)){
				index = row;
			}
		}
		return index;
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
		if(!newName.isEmpty() && newName!=null){
		
//			if(this.populationNames.containsKey(newName)){
//				log(Level.SEVERE, "Name exists, aborting");
//			} else {
//				String oldName = collection.getName();
				collection.setName(newName);
//				this.populationNames.put(newName, collection.getID());
//				this.populationNames.remove(oldName);
				log(Level.INFO, "Collection renamed: "+newName);
				
				
				File saveFile = dataset.getSavePath();
				if(saveFile.exists()){
					saveFile.delete();
				}

				List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
				list.add(dataset);
				fireDatasetEvent(DatasetMethod.SAVE, list);

				update(list);
//			}
		}
	}
	
	
	/**
	 * Move the selected dataset down in the list
	 */
	private void moveDatasetDown() {
		
		List<AnalysisDataset> datasets = getSelectedDatasets();

		if(datasets.size()==1){

			AnalysisDataset dataToMove = datasets.get(0);
			
			int oldValue = treeOrderMap.get(dataToMove.getUUID());
			int newValue = oldValue;
			if(oldValue<treeOrderMap.size()-1){ // do not change if already at the bottom
				
				newValue = oldValue+1;
				UUID replacedID = treeOrderMap.get(newValue); // find the dataset currently holding the spot
				treeOrderMap.put(dataToMove.getUUID(), newValue ); // move the dataset up
				treeOrderMap.put(replacedID, oldValue); // move the dataset in place down
				update();
				
			}
			selectDataset(dataToMove);
		}
	}
	
	
	/**
	 * Move the selected dataset down in the list
	 */
	private void moveDatasetUp() {
		
		List<AnalysisDataset> datasets = getSelectedDatasets();

		if(datasets.size()==1){

			AnalysisDataset dataToMove = datasets.get(0);

			int oldValue = treeOrderMap.get(dataToMove.getUUID());
			int newValue = oldValue;
			if(oldValue>0){ // do not change if already at the top
				newValue = oldValue-1;

				UUID replacedID = treeOrderMap.get(newValue); // find the dataset currently holding the spot
				treeOrderMap.put(dataToMove.getUUID(), newValue ); // move the dataset up
				treeOrderMap.put(replacedID, oldValue); // move the dataset in place down
				update();
			}
			selectDataset(dataToMove);
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
				treeOrderMap.remove(id);
				DatasetListManager.getInstance().removeDataset(d);
			} else {
				finer("Dataset is not root");
			}

//			if(treeOrderMap.contains(id)){
//				finest("Removing dataset from treeOrderMap");
//				treeOrderMap.remove(id);
//				DatasetListManager.getInstance().removeDataset(d);
//			} else {
//				finest("Dataset is not root");
//			}
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
		final List<AnalysisDataset> datasets = getSelectedDatasets();

		if(!datasets.isEmpty()){
			// make a list of the unique UUIDs selected
			finest("There are "+datasets.size()+" datasets selected");

			Set<UUID> list = new HashSet<UUID>();
			for(AnalysisDataset d : datasets){
				finest("Selected dataset for deletion: "+d.getName());

				list.add(d.getUUID());
				
				if(d.hasChildren()){
					finest("Children found in: "+d.getName());
					// add all the children of a dataset
					for(UUID childID : d.getAllChildUUIDs()){
						finest("Adding child dataset to deletion list: "+childID.toString());
						list.add(childID);
					}
				} else {
					finest("No children in: "+d.getName());
				}
			}


			deleteDatasetsInList(list);
			refreshClusters();
			finest("Updating tree panel");
			update();
			finest("Firing update panel event");
			fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);

		} else {
			finest("No datasets selected");
		}
		finest("Deletion complete");

	}

	/**
	 * Establish the rows in the population tree that are currently selected.
	 * Set the possible menu options accordingly, and call the panel updates
	 */
	class TreeSelectionHandler implements TreeSelectionListener {
		public void valueChanged(TreeSelectionEvent e) {

			try {

				List<AnalysisDataset> datasets = new ArrayList<AnalysisDataset>(0);

				TreeSelectionModel lsm = (TreeSelectionModel)e.getSource();

				List<Integer> selectedIndexes = new ArrayList<Integer>(0);
				if (!lsm.isSelectionEmpty()) {
					// Find out which indexes are selected.
					int minIndex = lsm.getMinSelectionRow();
					int maxIndex = lsm.getMaxSelectionRow();
					for (int i = minIndex; i <= maxIndex; i++) {
						if (lsm.isRowSelected(i)) {
							
							Object columnOneObject = treeTable.getModel().getValueAt(i, COLUMN_NAME);
							
							if(columnOneObject instanceof ClusterGroup){
								continue;
							}

							if(columnOneObject instanceof AnalysisDataset){
								AnalysisDataset d = (AnalysisDataset) treeTable.getModel().getValueAt(i, COLUMN_NAME); // row i, column 0
								datasets.add(d);
								selectedIndexes.add(i);
							}
							
						}
					}

					treeTable.getColumnModel().getColumn(COLUMN_COLOUR).setCellRenderer(new PopulationTableCellRenderer(selectedIndexes));

					if(datasets.isEmpty()){
						populationPopup.disableAll();
					} else {

						if(datasets.size()>1){ // multiple populations
							populationPopup.disableAll();
							populationPopup.enableMerge();
							populationPopup.enableDelete();
							populationPopup.enableBoolean();

						} else { // single population
							AnalysisDataset d = datasets.get(0);
							setMenuForSingleDataset(d);
						}
						finest("Firing update panel event due to tree selection");
						fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
					}
				}
			} catch(Exception ex){
				error("Error in tree selection handler", ex);
			}
		}
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
		
		// check if we can move the dataset
		if(d.isRoot()){

			if(treeOrderMap.size()>1){

				// check if the selected dataset is at the top of the list
				if(treeOrderMap.get(0).equals(d.getUUID())){
					populationPopup.disableMenuUp();
				} else {
					populationPopup.enableMenuUp();
				}

				// check if the selected dataset is at the bottom of the list
				if(treeOrderMap.get(treeOrderMap.size()-1).equals(d.getUUID())){
					populationPopup.disableMenuDown();
				} else {
					populationPopup.enableMenuDown();
				}

			} else { // only one or zero datasets in the pogram 
				populationPopup.disableMenuUp();
				populationPopup.disableMenuDown();
			}

			// only root datasets can replace folder mappings
			populationPopup.enableReplaceFolder();

		} else { // not root
						
			populationPopup.disableReplaceFolder();
			populationPopup.disableMenuUp();
			populationPopup.disableMenuDown();
		}

	}
		

	@Override
	public void signalChangeReceived(SignalChangeEvent event) {

		// Pass on events from the popup menu
		if(event.sourceName().equals(PopulationListPopupMenu.SOURCE_COMPONENT)){
				fireSignalChangeEvent(event.type());
				finest("Firing signal change event: "+event.type());
			}
					
		// catch any signals that affect the datasets directly
		if(event.type().equals("MoveDatasetDownAction")){
			moveDatasetDown();
		}
		
		if(event.type().equals("MoveDatasetUpAction")){
			moveDatasetUp();
		}
		
		if(event.type().equals("RenameDatasetUpAction")){
			moveDatasetUp();
		}
		
		if(event.type().equals("DeleteCollectionAction")){
			finest("Deleting dataset action received");
			deleteSelectedDatasets();
		}
		
		
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}

}
