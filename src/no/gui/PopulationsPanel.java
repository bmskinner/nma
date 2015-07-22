package no.gui;

import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.BoxLayout;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import no.analysis.AnalysisDataset;
import no.collections.CellCollection;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;

import utility.TreeOrderHashMap;

public class PopulationsPanel extends JPanel implements SignalChangeListener {

	private static final long serialVersionUID = 1L;
	
	public static final String SOURCE_COMPONENT = "PopulationsPanel"; 
	
	private final JPanel panelPopulations = new JPanel(); // holds list of active populations

	private JXTreeTable treeTable;
	private PopulationListPopupMenu populationPopup;
	
	private HashMap<String, UUID> populationNames = new HashMap<String, UUID>();
	private HashMap<UUID, AnalysisDataset> analysisDatasets = new HashMap<UUID, AnalysisDataset>();
	
	private TreeOrderHashMap treeOrderMap = new TreeOrderHashMap(); // order the root datasets
	
	private List<Object> listeners = new ArrayList<Object>();

	public PopulationsPanel() {
		
		this.setLayout(new BorderLayout());
		
		panelPopulations.setMinimumSize(new Dimension(100, 100));

		panelPopulations.setLayout(new BoxLayout(panelPopulations, BoxLayout.Y_AXIS));
					
		// tree table approach
		List<String> columns = new ArrayList<String>();
		columns.add("Population");
		columns.add("Nuclei");
		columns.add("");

		DefaultTreeTableModel treeTableModel = new DefaultTreeTableModel();
		DefaultMutableTreeTableNode  root = new DefaultMutableTreeTableNode ("root node");
		treeTableModel.setRoot(root);
		treeTableModel.setColumnIdentifiers(columns);
		
		treeTable = new JXTreeTable(treeTableModel);
		treeTable.setEnabled(true);
		treeTable.setCellSelectionEnabled(false);
		treeTable.setColumnSelectionAllowed(false);
		treeTable.setRowSelectionAllowed(true);
		treeTable.getColumnModel().getColumn(2).setCellRenderer(new PopulationTableCellRenderer());
		treeTable.getColumnModel().getColumn(0).setPreferredWidth(120);
		treeTable.getColumnModel().getColumn(2).setPreferredWidth(5);
		
		populationPopup = new PopulationListPopupMenu();
		populationPopup.disableAll();
		
		// this is the only listener for this menu
		// pass on signals to the main window
		populationPopup.addSignalChangeListener(this);
		
		treeTable.setComponentPopupMenu(populationPopup);
		
		treeTable.addMouseListener(new MouseAdapter() {
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
						}

						fireSignalChangeEvent("UpdatePanels");
						
					}
				}
			}
		});
		

		TreeSelectionModel tableSelectionModel = treeTable.getTreeSelectionModel();
		tableSelectionModel.addTreeSelectionListener(new TreeSelectionHandler());
		
		JScrollPane populationScrollPane = new JScrollPane(treeTable);		
		
		panelPopulations.add(populationScrollPane);
		this.add(panelPopulations, BorderLayout.CENTER);

	}
	
	/**
	 *  Find the populations in memory, and display them in the population chooser. 
	 *  Root populations are ordered according to position in the treeListOrder map.
	 */
	public void update(){
					
		List<String> columns = new ArrayList<String>();
		columns.add("Population");
		columns.add("Nuclei");
		columns.add("");

		DefaultTreeTableModel treeTableModel = new DefaultTreeTableModel();
		PopulationTreeTableNode  root = new PopulationTreeTableNode (java.util.UUID.randomUUID());
		treeTableModel.setRoot(root);
		treeTableModel.setColumnIdentifiers(columns);
				
		if(this.analysisDatasets.size()>0){
			for(UUID id : treeOrderMap.getIDs()){
				
				AnalysisDataset rootDataset = analysisDatasets.get(id);
				root.add( addTreeTableChildNodes(rootDataset.getUUID()));
			}
		}
		
		treeTable.setTreeTableModel(treeTableModel);

		int row = 0;
		while (row < treeTable.getRowCount()) {
			treeTable.expandRow(row);
			row++;
		}
		
		treeTable.getColumnModel().getColumn(0).setPreferredWidth(120);
		treeTable.getColumnModel().getColumn(2).setPreferredWidth(5);
	}
	
	private PopulationTreeTableNode addTreeTableChildNodes(UUID id){
		AnalysisDataset dataset = PopulationsPanel.this.analysisDatasets.get(id);
		PopulationTreeTableNode category = new PopulationTreeTableNode(dataset.getUUID());
		category.setValueAt(dataset.getName(), 0);
		category.setValueAt(dataset.getCollection().getNucleusCount(), 1);
				
		Set<UUID> childIDList = dataset.getChildUUIDs();
		for(UUID childID : childIDList){
			PopulationTreeTableNode childNode = addTreeTableChildNodes(childID);
			category.add(childNode);
		}
		return category;
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
	 * @param d the dataset to add
	 */
	public void addDataset(AnalysisDataset dataset){
		dataset.setName(checkName(dataset.getName()));
		this.analysisDatasets.put(dataset.getUUID(), dataset);
		this.populationNames.put(dataset.getName(), dataset.getUUID());
		
		if(dataset.isRoot()){ // add to the list of datasets that can be ordered
			treeOrderMap.put(dataset.getUUID(), treeOrderMap.size()); // add to the end of the list
		}
	}
	
	/**
	 * Check that the name of the dataset is not already in the list of datasets
	 * If the name is used, adjust and check again
	 * @param name the suggested name
	 * @return a valid name
	 */
	private String checkName(String name){
		String result = name;
		if(this.populationNames.containsKey(name)){
			result = checkName(name+"_1");
		}
		return result;
	}
	
	/**
	 * Rename an existing dataset and update the population list.
	 * @param dataset the dataset to rename
	 */
	private void renameCollection(AnalysisDataset dataset){
		CellCollection collection = dataset.getCollection();
		String newName = JOptionPane.showInputDialog(this, "Rename collection", collection.getName());
		// validate
		if(!newName.isEmpty() && newName!=null){
		
			if(this.populationNames.containsKey(newName)){
				IJ.log("Name exists, aborting");
			} else {
				String oldName = collection.getName();
				collection.setName(newName);
				this.populationNames.put(newName, collection.getID());
				this.populationNames.remove(oldName);
				IJ.log("Collection renamed: "+newName);
				update();
				
				List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
				list.add(dataset);
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
			
		}
	}
	
	private void deleteDataset(){
		List<AnalysisDataset> datasets = getSelectedDatasets();

		if(!datasets.isEmpty()){

			// only delete single collections for now
			if(datasets.size()==1){

				//TODO: this still has problems with multiple datasets

				// get the ids as a list, so we don't iterate over datasets
				// when we could delete a child of the list in progress
				List<UUID> list = new ArrayList<UUID>(0);
				for(AnalysisDataset d : datasets){
					list.add(d.getUUID());
				}

				for(UUID id : list){
					// check dataset still exists
					if(this.hasDataset(id)){

						AnalysisDataset d = this.getDataset(id);


						// remove all children of the collection
						for(UUID u : d.getAllChildUUIDs()){
							String name = this.getDataset(u).getName();

							if(analysisDatasets.containsKey(u)){
								analysisDatasets.remove(u);
							}

							if(populationNames.containsValue(u)){
								populationNames.remove(name);
							}						

							d.deleteChild(u);

						}

						for(UUID parentID : analysisDatasets.keySet()){
							AnalysisDataset parent = analysisDatasets.get(parentID);
							if(parent.hasChild(id)){
								parent.deleteChild(id);
							}
						}
						populationNames.remove(d.getName());
						analysisDatasets.remove(id);

						if(d.isRoot()){
							treeOrderMap.remove(id);
						}
					}
				}
				this.update();

			}
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
//				lblStatusLine.setText(datasets.size()+" "+count+" selected");
				treeTable.getColumnModel().getColumn(2).setCellRenderer(new PopulationTableCellRenderer(selectedIndexes));

				if(datasets.isEmpty()){
					IJ.log("Error: list is empty");
					populationPopup.disableAll();
				} else {
					
					if(datasets.size()>1){ // multiple populations
						populationPopup.disableAll();
						populationPopup.enableMerge();
						populationPopup.enableDelete();
						
					} else { // single population
						AnalysisDataset d = datasets.get(0);
						populationPopup.enableDelete();
						populationPopup.disableMerge();
						populationPopup.enableSave();
						populationPopup.enableExtract();
						populationPopup.enableExportStats();
						populationPopup.enableAddTailStain();
						populationPopup.enableAddNuclearSignal();
						populationPopup.enableRunShellAnalysis();
						
						// check if we can move the dataset
						if(d.isRoot()){

							if(treeOrderMap.size()>1){
								populationPopup.enableApplySegmentation();

								// check if the selected dataset is at the top of the list
								if(treeOrderMap.get(0)==d.getUUID()){
									populationPopup.disableMenuUp();
//									populationPopup.disableMenuDown();
								} else {
									populationPopup.enableMenuUp();
//									populationPopup.enableMenuDown();
								}

								// check if the selected dataset is at the bottom of the list
								if(treeOrderMap.get(treeOrderMap.size()-1)==d.getUUID()){
									populationPopup.disableMenuDown();
//									populationPopup.disableMenuUp();
								} else {
//									populationPopup.enableMenuUp();
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

						if(!d.hasChildren()){ // cannot split population without children yet
							populationPopup.disableSplit();
						} else {
							populationPopup.enableSplit();
						}
					}
					fireSignalChangeEvent("UpdatePanels");
				}
			}
		}
	}
	
	public synchronized void addSignalChangeListener( SignalChangeListener l ) {
        listeners.add( l );
    }
    
    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
        listeners.remove( l );
    }
     
    private synchronized void fireSignalChangeEvent(String message) {
        SignalChangeEvent event = new SignalChangeEvent( this, message, SOURCE_COMPONENT );
        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
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
	}

	@Override
	public void signalChangeReceived(SignalChangeEvent event) {

		if(event.sourceName().equals(PopulationListPopupMenu.SOURCE_COMPONENT)){
			
			// pass on signals from the menu
			fireSignalChangeEvent(event.type());
			
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
			deleteDataset();
		}
		
		
	}

}
