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
package gui;


import gui.DatasetEvent.DatasetMethod;
import gui.actions.SaveDatasetAction;
import gui.components.ColourSelecter;
import gui.tabs.DetailPanel;
import io.PopulationExporter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;

import utility.TreeOrderHashMap;
import analysis.AnalysisDataset;
import components.CellCollection;
import components.ClusterGroup;

public class PopulationsPanel extends DetailPanel implements SignalChangeListener {

	private static final long serialVersionUID = 1L;
	
	private static final int COLUMN_NAME = 0;
	private static final int COLUMN_CELL_COUNT = 1;
	private static final int COLUMN_COLOUR = 2;
	
		
	private final JPanel panelPopulations = new JPanel(); // holds list of active populations

	private JXTreeTable treeTable;
	private PopulationListPopupMenu populationPopup;
	
	private HashMap<String, UUID> populationNames = new HashMap<String, UUID>();
	private HashMap<UUID, AnalysisDataset> analysisDatasets = new HashMap<UUID, AnalysisDataset>();
	
	private TreeOrderHashMap treeOrderMap = new TreeOrderHashMap(); // order the root datasets
	
	public PopulationsPanel(Logger programLogger) {
		super(programLogger);
		this.setLayout(new BorderLayout());
		
		panelPopulations.setMinimumSize(new Dimension(100, 100));

		panelPopulations.setLayout(new BoxLayout(panelPopulations, BoxLayout.Y_AXIS));
		
		treeTable = createTreeTable();
							
		
		JScrollPane populationScrollPane = new JScrollPane(treeTable);		
		
		panelPopulations.add(populationScrollPane);
		this.add(panelPopulations, BorderLayout.CENTER);

	}
	
	public void update(List<AnalysisDataset> list){
		this.update();
	}
	
	/**
	 *  Find the populations in memory, and display them in the population chooser. 
	 *  Root populations are ordered according to position in the treeListOrder map.
	 */
	public void update(){
		
		int nameColWidth = treeTable.getColumnModel().getColumn(COLUMN_NAME).getWidth();
		int colourColWidth = treeTable.getColumnModel().getColumn(COLUMN_COLOUR).getWidth();
					
		List<String> columns = new ArrayList<String>();
		columns.add("Population");
		columns.add("Nuclei");
		columns.add("");

		DefaultTreeTableModel treeTableModel = new DefaultTreeTableModel();
		PopulationTreeTableNode  root = new PopulationTreeTableNode (java.util.UUID.randomUUID());
		treeTableModel.setRoot(root);
		treeTableModel.setColumnIdentifiers(columns);
				
		if(this.analysisDatasets.size()>0){ // if there are datasets to display
			programLogger.log(Level.FINEST, "Loaded: "+analysisDatasets.size()+" datasets");
			for(UUID id : treeOrderMap.getIDs()){
												
				AnalysisDataset rootDataset = analysisDatasets.get(id);
				root.add( addTreeTableChildNodes(    rootDataset    )     );
			}

		} else {
			programLogger.log(Level.FINEST, "No datasets loaded");
		}
		
		treeTable.setTreeTableModel(treeTableModel);

		if(this.analysisDatasets.size()>0){
			programLogger.log(Level.FINEST, "Expanding rows");
			for (int row = 0; row < treeTable.getRowCount(); row++) {
				treeTable.expandRow(row);
			}
		}
		
		programLogger.log(Level.FINEST, "Restoring column widths");
		treeTable.getColumnModel().getColumn(COLUMN_NAME).setWidth(nameColWidth);
		treeTable.getColumnModel().getColumn(COLUMN_COLOUR).setWidth(colourColWidth);
		programLogger.log(Level.FINEST, "Update complete");
	}
	
	/**
	 * Ensure that all child datasets are present within the  
	 * analysisDatasets Map
	 */
	public void refreshDatasets(){
		
		if(this.analysisDatasets.size()>0){
			for(UUID id : treeOrderMap.getIDs()){
												
				AnalysisDataset rootDataset = analysisDatasets.get(id);
				for(AnalysisDataset child : rootDataset.getAllChildDatasets()){
					if(! this.hasDataset(child.getUUID())){
						child.setName(checkName(child.getName(), child.getUUID()));
						this.analysisDatasets.put(child.getUUID(), child);
						this.populationNames.put(child.getName(), child.getUUID());
					}
				}
				
			}
		}
		
	}
	
	/**
	 * Update the cluster groups for each root dataset and its children.
	 * This will remove any cluster groups with no member datasets. 
	 */
	public void refreshClusters(){
		try {
		programLogger.log(Level.FINEST, "Refreshing clusters...");
		if(this.analysisDatasets.size()>0){
			for(UUID id : treeOrderMap.getIDs()){
												
				AnalysisDataset rootDataset = analysisDatasets.get(id);
				programLogger.log(Level.FINEST, "  Root dataset "+rootDataset.getName());
				rootDataset.refreshClusterGroups();
				for(AnalysisDataset child : rootDataset.getAllChildDatasets()){
					programLogger.log(Level.FINEST, "    Child dataset "+child.getName());
					child.refreshClusterGroups();
				}
				
			}
		}
		} catch (Exception e){
			programLogger.log(Level.SEVERE, "Error refreshing clusters", e);
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
		
		UUID id = dataset.getUUID();
		if(!this.hasDataset(id)){
			this.addDataset(dataset);
		}

		PopulationTreeTableNode category = new PopulationTreeTableNode(id);
		category.setValueAt(dataset.getName(), 0);
		category.setValueAt(dataset.getCollection().getNucleusCount(), 1);
				
		
//		Set<UUID> childIDList = dataset.getChildUUIDs();
		for(AnalysisDataset childDataset : dataset.getChildDatasets()){
			PopulationTreeTableNode childNode = addTreeTableChildNodes(childDataset);
			category.add(childNode);
		}
		
		category.sortNode(COLUMN_NAME, true, false);
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
		table.setRowSelectionAllowed(true);
		table.getColumnModel().getColumn(COLUMN_COLOUR).setCellRenderer(new PopulationTableCellRenderer());
		table.getColumnModel().getColumn(COLUMN_NAME).setPreferredWidth(120);
		table.getColumnModel().getColumn(COLUMN_COLOUR).setPreferredWidth(5);
		
		populationPopup = new PopulationListPopupMenu();
		populationPopup.disableAll();
		
		// this is the only listener for this menu
		// pass on signals to the main window
		populationPopup.addSignalChangeListener(this);
		
		table.setComponentPopupMenu(populationPopup);
		
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				
				JXTreeTable table = (JXTreeTable) e.getSource();
				
				int row		= table.rowAtPoint((e.getPoint()));
				int column 	= table.columnAtPoint(e.getPoint());
				String populationName = (String) table.getModel().getValueAt(row, 0);
				
				// double click
				if (e.getClickCount() == 2) {
					
					UUID id = PopulationsPanel.this.populationNames.get(populationName);
					AnalysisDataset dataset = PopulationsPanel.this.analysisDatasets.get(id);
					
					if (row >= 0 && column == 0) { // first (names) column						
						renameCollection(dataset);
					}
					
					if(row >= 0 && column == 2){ // third (colours) column
						
						Color oldColour = ColourSelecter.getSegmentColor( row );
						
						Color newColor = JColorChooser.showDialog(
								PopulationsPanel.this,
			                     "Choose dataset Color",
			                     oldColour);
						
						if(newColor != null){
							dataset.setDatasetColour(newColor);
							
							// Force the chart caches to clear
							List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
							list.add(dataset);
							fireDatasetEvent(DatasetMethod.RECALCULATE_CACHE, list);
						}

						fireSignalChangeEvent("UpdatePanels");
						
					}
				}
			}
		});
		

		TreeSelectionModel tableSelectionModel = table.getTreeSelectionModel();
		tableSelectionModel.addTreeSelectionListener(new TreeSelectionHandler());
		
		return table;
	}
	
	/**
	 * Get the number of datasets currently loaded
	 * @return
	 */
	public int getDatasetCount(){
		return this.analysisDatasets.size();
	}
	
	/**
	 * Get the datasets currently selected
	 * @return
	 */
	public List<AnalysisDataset> getSelectedDatasets(){

		List<AnalysisDataset> datasets = new ArrayList<AnalysisDataset>(0);

		TreeSelectionModel lsm = treeTable.getTreeSelectionModel();

		List<Integer> selectedIndexes = new ArrayList<Integer>(0);

		if (!lsm.isSelectionEmpty()) {
			// Find out which indexes are selected.
			int minIndex = lsm.getMinSelectionRow();
			int maxIndex = lsm.getMaxSelectionRow();
			for (int i = minIndex; i <= maxIndex; i++) {
				if (lsm.isRowSelected(i)) {

					String key = (String) treeTable.getModel().getValueAt(i, 0); // row i, column 0
					if(!key.equals("No populations")){

						// get uuid from populationNames, then population via uuid from analysisDatasets
						datasets.add(analysisDatasets.get(populationNames.get(key)));
						selectedIndexes.add(i);

					}

				}
			}
		}
		return datasets;
	}
	
	/**
	 * Get all the datasets currently loaded
	 * @return
	 */
	public List<AnalysisDataset> getAllDatasets(){
		List<AnalysisDataset> datasets = new ArrayList<AnalysisDataset>(0);
		for(AnalysisDataset d : analysisDatasets.values()){
			datasets.add(d);
		}
		return datasets;
	}
	
	/**
	 * Get root datasets currently loaded
	 * @return
	 */
	public List<AnalysisDataset> getRootDatasets(){
		List<AnalysisDataset> datasets = new ArrayList<AnalysisDataset>(0);
		for(AnalysisDataset d : analysisDatasets.values()){
			if(d.isRoot()){
				datasets.add(d);
			}
		}
		return datasets;
	}
	
	/**
	 * Get the names of loaded populations
	 * @return
	 */
	public List<String> getPopulationNames(){
		List<String> result = new ArrayList<String>(0);
		for(String s : populationNames.keySet()){
			result.add(s);
		}
		return result;
	}
	
	/**
	 * Given a population display name, get the UUID of the analysis dataset
	 * @param name
	 * @return
	 */
	public UUID getUuidFromName(String name){
		if(this.populationNames.containsKey(name)){
			return this.populationNames.get(name);
		} else {
			return null;
		}
	}
	
	/**
	 * Given an analysis dataset UUID, get the population display name
	 * @param uuid the id to fetch
	 * @return the name
	 */
	public String getNameFromUuid(UUID id){
		String result = null;
		if(this.populationNames.containsValue(id)){

			for(String s : populationNames.keySet()){
				
				if(populationNames.get(s).equals(id)){
					result = s;
				}
			}
		}
		return result;
	}
	
	/**
	 * Fetch the dataset with the given id, or null if not present
	 * @param id
	 * @return
	 */
	public AnalysisDataset getDataset(UUID id){
		AnalysisDataset result = null;
		if(this.analysisDatasets.containsKey(id)){
			result = this.analysisDatasets.get(id);
		}
		return result;
	}
	
	/**
	 * Given a population name, get the dataset
	 * @param name
	 * @return
	 */
	public AnalysisDataset getDataset(String name){
		UUID id = populationNames.get(name);
		AnalysisDataset result = getDataset(id);
		return result;
	}
	
	 
	
	/**
	 * Check if the given dataset is present in the list
	 * @param id
	 * @return
	 */
	public boolean hasDataset(UUID id){
		boolean result = false;
		if(this.analysisDatasets.containsKey(id)){
			result = true;
		}
		return result;
	}
	
	/**
	 * Add the given dataset to the main population list
	 * Check that the name is valid, and update if needed
	 * @param dataset the dataset to add
	 */
	public void addDataset(AnalysisDataset dataset){
		programLogger.log(Level.FINEST, "Checking dataset name is suitable");
		dataset.setName(checkName(dataset.getName(), dataset.getUUID()));
		programLogger.log(Level.FINEST, "Set name as "+dataset.getName());
		this.analysisDatasets.put(dataset.getUUID(), dataset);
		this.populationNames.put(dataset.getName(), dataset.getUUID());
		
		if(dataset.isRoot()){ // add to the list of datasets that can be ordered
			treeOrderMap.put(dataset.getUUID(), treeOrderMap.size()); // add to the end of the list
		}
		update();
	}
	
	/**
	 * Select the given dataset in the tree table
	 * @param dataset the dataset to select
	 */
	public void selectDataset(AnalysisDataset dataset){

		if(dataset!=null){
			TreeSelectionModel selectedRows = treeTable.getTreeSelectionModel();
			int index = getIndexOfDataset(dataset);

			TreePath path = treeTable.getPathForRow(index);
			if(path!=null){
				selectedRows.setSelectionPath(path);
			}
		}
		update();
	}
	
	public void selectDataset(UUID id){
		this.selectDataset(this.getDataset(id));
	}
	
	/**
	 * Get the index of the given dataset in the tree table.
	 * @return the index
	 */
	private int getIndexOfDataset(AnalysisDataset dataset){
		int index = 0;
		
		for(int row = 0; row< treeTable.getRowCount(); row++){
			String populationName = (String) treeTable.getModel().getValueAt(row, 0);
			
			if(dataset.getName().equals(populationName)){
				index = row;
			}
		}
		return index;
	}
	
	/**
	 * Check that the name of the dataset is not already in the list of datasets
	 * If the name is used, adjust and check again
	 * @param name the suggested name
	 * @return a valid name
	 */
	private String checkName(String name, UUID id){

		String result = name;
		programLogger.log(Level.FINEST, "Testing name: "+name);

		if(this.populationNames.containsKey(name)){
			
			// Check that the dataset with the same name is not the dataset in question
			if(!this.populationNames.get(name).equals(id)){

				programLogger.log(Level.FINEST, "Found existing dataset with different UUID: "+name);

				Pattern pattern = Pattern.compile("_(\\d+)$");
				Matcher matcher = pattern.matcher(name);

				int digit = 0;

				while (matcher.find()) {

					programLogger.log(Level.FINEST, "Matched regex: "+matcher.toString());
					programLogger.log(Level.FINEST, "Matched on "+matcher.group(1));

					digit = Integer.valueOf(matcher.group(1));
					programLogger.log(Level.FINEST, "Found "+name+": changing to "+digit);

					if(digit>0){
						digit++;
						programLogger.log(Level.FINEST, "Found "+name+": changing to "+digit);
						name = matcher.replaceFirst("_"+digit);
					}

				}

				if(digit == 0) {
					name = name+"_1";
					programLogger.log(Level.FINEST, "No matches - appending _1 to name");
				}
				programLogger.log(Level.FINEST, "Rechacking name");
				result = checkName(name, id);
			} else {
				programLogger.log(Level.FINEST, "No other datasets with name: "+name);
			}
		} else {
			programLogger.log(Level.FINEST, "No matches to "+name+": returning");
		}
		return result;
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
		
			if(this.populationNames.containsKey(newName)){
				programLogger.log(Level.SEVERE, "Name exists, aborting");
			} else {
				String oldName = collection.getName();
				collection.setName(newName);
				this.populationNames.put(newName, collection.getID());
				this.populationNames.remove(oldName);
				programLogger.log(Level.INFO, "Collection renamed: "+newName);
				
				
				File saveFile = dataset.getSavePath();
				if(saveFile.exists()){
					saveFile.delete();
				}
//				new SaveDatasetAction(dataset, "Saving dataset", "Error saving dataset", mw);
//				
				List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
				list.add(dataset);
				fireDatasetEvent(DatasetMethod.SAVE, list);
//				PopulationExporter.saveAnalysisDataset(dataset);
				update();
				
				selectDataset(dataset);
				fireSignalChangeEvent("UpdatePanels");
			}
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

			programLogger.log(Level.FINEST, "Deleting dataset: "+d.getName());
			UUID id = d.getUUID();

			Map<UUID, String> map = new HashMap<UUID, String>();


			// select all children of the collection
			for(UUID u : d.getAllChildUUIDs()){
				String name = this.getDataset(u).getName();

				programLogger.log(Level.FINEST, "  Removing child dataset: "+name);

				if(analysisDatasets.containsKey(u) || populationNames.containsValue(u) ){
					map.put(u, name);
				}
			}

			// remove selected children
			for(UUID u : map.keySet()){
				String name = map.get(u);
				analysisDatasets.remove(u);
				programLogger.log(Level.FINEST, "  Removed child "+name+" from analysisDatasets");

				populationNames.remove(name);
				programLogger.log(Level.FINEST, "  Removed child "+name+" from populationNames");

			}


			// remove the dataset from its parents
			programLogger.log(Level.FINEST, "Removing dataset from its parents");
			for(UUID parentID : analysisDatasets.keySet()){
				AnalysisDataset parent = analysisDatasets.get(parentID);

				programLogger.log(Level.FINEST, "Parent dataset "+parent.getName());

				if(parent.hasChild(id)){
					programLogger.log(Level.FINEST, "    Parent contains dataset; deleting");
					parent.deleteChild(id);
				}

			}

			// remove cluster groups
			programLogger.log(Level.FINEST, "Removing cluster groups");
			for(ClusterGroup g : d.getClusterGroups()){
				boolean clusterRemains = false;

				for(UUID childID : g.getUUIDs()){
					if(d.hasChild(childID)){
						clusterRemains = true;
					}
				}
				if(!clusterRemains){
					programLogger.log(Level.FINEST, "  Removing cluster group "+g.getName());
					d.deleteClusterGroup(g);
				}
			}


			programLogger.log(Level.FINEST, "Removing dataset from analysisDatasets");
			if(analysisDatasets.containsKey(id)){
				analysisDatasets.remove(id);
				programLogger.log(Level.FINEST, "Removed from analysisDatasets");
			}

			programLogger.log(Level.FINEST, "Removing dataset from populationNames");
			if(populationNames.containsValue(id)){
				populationNames.remove(d.getName());
				programLogger.log(Level.FINEST, "Removed from populationNames");
			}			

			if(d.isRoot()){
				programLogger.log(Level.FINEST, "Removing dataset from treeOrderMap");
				treeOrderMap.remove(id);
			}
			programLogger.log(Level.FINEST, "Deleted dataset "+d.getName());
		} catch (Exception e){
			programLogger.log(Level.SEVERE, "Error deleting dataset "+d.getName(), e);
		}
	}
	
	private void deleteDataset(){
		final List<AnalysisDataset> datasets = getSelectedDatasets();

		try {

			programLogger.log(Level.FINEST, "Deleting selected datasets");
			Thread thr = new Thread(){
				public void run(){

					if(!datasets.isEmpty()){
						// make a list of the unique UUIDs selected

						List<UUID> list = new ArrayList<UUID>();
						for(AnalysisDataset d : datasets){
							programLogger.log(Level.FINEST, "Selected dataset for deletion: "+d.getName());

							if(!list.contains(d.getUUID())){
								list.add(UUID.fromString((d.getUUID().toString())));
							}

							// add all the children of a dataset
							for(UUID childID : d.getAllChildUUIDs()){
								programLogger.log(Level.FINEST, "Adding child dataset to deletion list: "+childID.toString());
								if(!list.contains(childID)){
									list.add(UUID.fromString((childID.toString())));
								}
							}
						}

						// go through the list, removing all the ids to be deleted
						for(UUID id : list){
							if(analysisDatasets.containsKey(id)){
								AnalysisDataset d = getDataset(id);
								programLogger.log(Level.FINEST, "Preparing to delete dataset: "+d.getName());
								deleteDataset(d);
								programLogger.log(Level.FINEST, "Dataset removed: "+d.getName());
							} else {
								programLogger.log(Level.FINEST, "Dataset already removed");
							}

						}

						refreshClusters();
						programLogger.log(Level.FINEST, "Updating panels");
						update();
						programLogger.log(Level.FINEST, "Firing update panel event");
						fireSignalChangeEvent("UpdatePanelsNull");
						repaint();

					} else {
						programLogger.log(Level.FINEST, "No datasets selected");
					}
					programLogger.log(Level.FINEST, "Deletion complete");
					

				}
			};
			thr.start();
//			selectDataset((AnalysisDataset) null);
		} catch (Exception e){
			programLogger.log(Level.SEVERE, "Error deleting datasets", e);
		}
	}

	/**
	 * Establish the rows in the population tree that are currently selected.
	 * Set the possible menu options accordingly, and call the panel updates
	 */
	class TreeSelectionHandler implements TreeSelectionListener {
		public void valueChanged(TreeSelectionEvent e) {
			
//			List<AnalysisDataset> datasets = getSelectedRowsFromTreeTable();
			List<AnalysisDataset> datasets = new ArrayList<AnalysisDataset>(0);
			
			TreeSelectionModel lsm = (TreeSelectionModel)e.getSource();
			
			List<Integer> selectedIndexes = new ArrayList<Integer>(0);
			if (!lsm.isSelectionEmpty()) {
				// Find out which indexes are selected.
				int minIndex = lsm.getMinSelectionRow();
				int maxIndex = lsm.getMaxSelectionRow();
				for (int i = minIndex; i <= maxIndex; i++) {
					if (lsm.isRowSelected(i)) {

						String key = (String) treeTable.getModel().getValueAt(i, 0); // row i, column 0
						if(!key.equals("No populations")){
							
							// get uuid from populationNames, then population via uuid from analysisDatasets
							datasets.add(analysisDatasets.get(populationNames.get(key)));
							selectedIndexes.add(i);
						}

					}
				}
				String count = datasets.size() == 1 ? "population" : "populations"; // it matters to ME
				status(datasets.size()+" "+count+" selected");
				treeTable.getColumnModel().getColumn(2).setCellRenderer(new PopulationTableCellRenderer(selectedIndexes));

				if(datasets.isEmpty()){
					populationPopup.disableAll();
				} else {
					
					if(datasets.size()>1){ // multiple populations
						populationPopup.disableAll();
						populationPopup.enableMerge();
						populationPopup.enableDelete();
						
					} else { // single population
						AnalysisDataset d = datasets.get(0);
						setMenuForSingleDataset(d);
					}
					fireSignalChangeEvent("UpdatePanels");
				}
			}
		}
	}
	
	private void setMenuForSingleDataset(AnalysisDataset d){
		
		populationPopup.enableDelete();
		populationPopup.disableMerge();
		populationPopup.enableSplit();
		populationPopup.enableSave();
		populationPopup.enableExtract();
		populationPopup.enableCurate();
		populationPopup.enableExportStats();
//		populationPopup.enableAddTailStain();
		populationPopup.enableAddNuclearSignal();
		populationPopup.enableRunShellAnalysis();
		
		// check if we can move the dataset
		if(d.isRoot()){

			if(treeOrderMap.size()>1){
				populationPopup.enableApplySegmentation();

				// check if the selected dataset is at the top of the list
				if(treeOrderMap.get(0).equals(d.getUUID())){
					populationPopup.disableMenuUp();
//					populationPopup.disableMenuDown();
				} else {
					populationPopup.enableMenuUp();
//					populationPopup.enableMenuDown();
				}

				// check if the selected dataset is at the bottom of the list
				if(treeOrderMap.get(treeOrderMap.size()-1).equals(d.getUUID())){
					populationPopup.disableMenuDown();
//					populationPopup.disableMenuUp();
				} else {
//					populationPopup.enableMenuUp();
					populationPopup.enableMenuDown();
				}

			} else { // only one or zero datasets in the pogram 
				populationPopup.disableMenuUp();
				populationPopup.disableMenuDown();
			}

			// only root datasets can replace folder mappings
			populationPopup.enableReplaceFolder();

		} else { // not root
			
			if(treeOrderMap.size()>1){
				populationPopup.enableApplySegmentation();
			}
			
			populationPopup.disableReplaceFolder();
			populationPopup.disableMenuUp();
			populationPopup.disableMenuDown();
		}

	}
		
	/**
	 * Allows for cell background to be coloured based on poition in a list
	 *
	 */
	class PopulationTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;
		List<Integer> indexList = new ArrayList<Integer>(0);
		
		public PopulationTableCellRenderer(List<Integer> list){
			super();
			this.indexList = list;
		}
		
		public PopulationTableCellRenderer(){
			super();
		}

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        
	      //Cells are by default rendered as a JLabel.
	        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	        if (indexList.contains(row)) {
	        	
	        	// get the analysis dataset corresponding to this row
	        	String populationName = (String) table.getModel().getValueAt(row, 0);
	        	UUID id = PopulationsPanel.this.populationNames.get(populationName);
				AnalysisDataset dataset = PopulationsPanel.this.analysisDatasets.get(id);
	        	
				
	        	// if a preferred colour is specified, use it, otherwise go for defaults
	        	Color colour 	= dataset.getDatasetColour() == null 
	        					? ColourSelecter.getSegmentColor(indexList.indexOf(row))
	        					: dataset.getDatasetColour();

	        	l.setBackground(colour);
	        } else {
	        	l.setBackground(Color.WHITE); // only colour the selected rows
	        }

	      //Return the JLabel which renders the cell.
	      return l;
	    }
	}

		
	
	
	class PopulationTreeTableNode extends AbstractMutableTreeTableNode {
		
		Object[] columnData = new Object[3];
		UUID nodeID;

		PopulationTreeTableNode(UUID id) {
			super(id.toString());
			this.nodeID = id;
		}
		
		public UUID getID(){
			return this.nodeID;
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
		
		/**
		 * This method recursively (or not) sorts the nodes, ascending, or descending by the specified column.
		 * @param sortColumn Column to do the sorting by.
		 * @param sortAscending Boolean value of weather the sorting to be done ascending or not (descending).
		 * @param recursive Boolean value of weather or not the sorting should be recursively applied to children nodes.
		 * @author Alex Burdu Burdusel
		 */
		public void sortNode(int sortColumn, boolean sortAscending, boolean recursive) {
			boolean mLastSortAscending = sortAscending;
		    int mLastSortedColumn = sortColumn;
		    boolean mLastSortRecursive = recursive;

		    int childCount = this.getChildCount();
		    TreeMap<Object, PopulationTreeTableNode> nodeData = new TreeMap(String.CASE_INSENSITIVE_ORDER);

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
	
	

	@Override
	public void signalChangeReceived(SignalChangeEvent event) {

		if(event.sourceName().equals(PopulationListPopupMenu.SOURCE_COMPONENT)){
			
			// pass on signals from the menu
			fireSignalChangeEvent(event.type());
			programLogger.log(Level.FINEST, "Firing signal change event: "+event.type());
			
//			if(event.type().equals("SaveCollectionAction")){
//				programLogger.log(Level.FINEST, "Firing dataset save-as event");
//				List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
//				list.add(activeDataset());
//				fireDatasetEvent(DatasetMethod.SAVE_AS, list);
//			}
			
		}
		
		if(event.type().equals("SaveCollectionAction")){
			programLogger.log(Level.FINEST, "Firing dataset save-as event");
			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			list.add(activeDataset());
			fireDatasetEvent(DatasetMethod.SAVE_AS, list);
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
			programLogger.log(Level.FINEST, "Deleting dataset action received");
			deleteDataset();
		}
		
		
	}

	@Override
	protected void updateSingle() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void updateMultiple() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void updateNull() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
