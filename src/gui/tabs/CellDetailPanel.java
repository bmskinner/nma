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
package gui.tabs;

import gui.DatasetEvent.DatasetMethod;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.tabs.CellDetailPanel.CellsListPanel.NodeData;
import gui.tabs.cells.AbstractCellDetailPanel;
import gui.tabs.cells.CellBorderTagPanel;
import gui.tabs.cells.CellOutlinePanel;
import gui.tabs.cells.CellProfilePanel;
import gui.tabs.cells.CellStatsPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import charting.datasets.SignalTableCell;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import components.Cell;
import components.CellularComponent;
import components.nuclear.NuclearSignal;

@SuppressWarnings("serial")
public class CellDetailPanel extends AbstractCellDetailPanel implements SignalChangeListener, TreeSelectionListener {
		
	private JTabbedPane tabPane; 
	
	protected CellsListPanel	 cellsListPanel;		// the list of cells in the active dataset
	protected CellProfilePanel	 segmentProfilePanel = new CellProfilePanel(); 		// the nucleus angle profile
	protected CellBorderTagPanel cellBorderTagPanel  = new CellBorderTagPanel();
	protected CellOutlinePanel 	 outlinePanel        = new CellOutlinePanel(); 		// the outline of the cell and detected objects
	protected CellStatsPanel 	 cellStatsPanel      = new CellStatsPanel();		// the stats table
	protected ComponentListPanel signalListPanel;	// choose which background image to display

	public CellDetailPanel() {

		super();

		try{
			
			this.setLayout(new BorderLayout());
			
			this.add(createCellandSignalListPanels(), BorderLayout.WEST);
			
			
			this.addSubPanel(cellStatsPanel);
			this.addSubPanel(segmentProfilePanel);
			this.addSubPanel(cellBorderTagPanel);
			this.addSubPanel(outlinePanel);
			
			cellStatsPanel.setParent(this);
			segmentProfilePanel.setParent(this);
			cellBorderTagPanel.setParent(this);
			outlinePanel.setParent(this);
			
			tabPane = new JTabbedPane(JTabbedPane.LEFT);
			this.add(tabPane, BorderLayout.CENTER);
			
			tabPane.add("Info", cellStatsPanel);
			
			tabPane.add("Segments", segmentProfilePanel);
			
			tabPane.add("Tags", cellBorderTagPanel);
			
			tabPane.add("Outline", outlinePanel);

			

			this.validate();
		} catch(Exception e){
			error("Error creating cell detail panel", e);
		}

	}
	
	private JPanel createCellandSignalListPanels(){
		JPanel panel = new JPanel(new GridBagLayout());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridheight = 2;
		constraints.gridwidth = 1;
		constraints.weightx = 0.5;
		constraints.weighty = 0.6;
		constraints.anchor = GridBagConstraints.CENTER;

		cellsListPanel = new CellsListPanel();
		cellsListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		panel.add(cellsListPanel, constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.gridheight = 2;
		constraints.gridwidth = 1;
		constraints.weightx = 0.5;
		constraints.weighty = 0.4;
		signalListPanel = new ComponentListPanel();
		signalListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		panel.add(signalListPanel, constraints);
		

		
		return panel;
	}

	
	@Override
	public void setActiveComponent(CellularComponent c){
		super.setActiveComponent(c);
		outlinePanel.setActiveComponent(activeComponent);
		cellStatsPanel.setActiveComponent(activeComponent);
		segmentProfilePanel.setActiveComponent(activeComponent);
	}
				
	@Override
	protected void updateSingle() {
		cellsListPanel.updateDataset( activeDataset()  );
		outlinePanel.update(getDatasets());
		cellStatsPanel.update(getDatasets());
		segmentProfilePanel.update(getDatasets());
		cellBorderTagPanel.update(getDatasets());
		
		finest("Updated cell list panel");
		updateCell(activeCell);
		finest("Updated active cell panel");
	}
	
	@Override
	protected void updateMultiple() {
		updateNull();
	}
	
	@Override
	protected void updateNull() {
		cellsListPanel.updateDataset(null);
		finest("Updated cell list panel");
		updateCell(null);
	}
		
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}
	
	/**
	 * Display data for the given cell
	 * @param cell
	 */
	private void updateCell(Cell cell){
		
		signalListPanel.update(cell);
		cellStatsPanel.update(cell);
		segmentProfilePanel.update(cell);
		cellBorderTagPanel.update(cell);
		outlinePanel.update(cell);
		
		
	}
	

	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		if(event.type().equals("SignalColourUpdate")){
			updateCell(activeCell);
		}

	}
	
	/**
	 * Hold the list of cells within the current active dataset
	 *
	 */
	protected class CellsListPanel extends JPanel {
		
		private JTree tree;
		
		protected CellsListPanel(){
			this.setLayout(new BorderLayout());
			
			DefaultMutableTreeNode root =
					new DefaultMutableTreeNode(new NodeData("Cells", null));
			TreeModel model = new DefaultTreeModel(root);
			tree = new JTree(model);
			tree.addTreeSelectionListener(CellDetailPanel.this);
			tree.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					
					JTree tree = (JTree) e.getSource();
					
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
					NodeData data = (NodeData) node.getUserObject();

					UUID cellID = data.getID();

					// double click - remove cell
					
					if (e.getClickCount() == 2) {
						
						Object[] options = { "Don't delete cell" , "Delete cell", };
						int result = JOptionPane.showOptionDialog(null, "Delete this cell?", "Confirm delete",

						        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,

						        null, options, options[0]);

						if(result==1){ // button at index 1

							
							// delete the cell
							Cell cell = activeDataset().getCollection().getCell(cellID);
							try {
								activeDataset().getCollection().removeCell(cell);
							} catch (Exception e2) {
								error("Error removing cell from collection", e2);
							}
							node.removeFromParent();
							DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
							model.reload();
							
							List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
							list.add(activeDataset());
							
							try {
								fireDatasetEvent(DatasetMethod.REFRESH_CACHE, list);
								fireDatasetEvent(DatasetMethod.REFRESH_MORPHOLOGY, list);

							} catch (Exception e1) {
								error("Error deleting cell", e1);
							}
							
							try {
								CellDetailPanel.this.updateSingle();
							} catch (Exception e1) {
								error("Error updating cell", e1);
							}
							fireSignalChangeEvent("UpdatePanels");
							fireSignalChangeEvent("UpdatePopulationPanel");
							fireDatasetEvent(DatasetMethod.SELECT_ONE_DATASET, list);

						}
						
					}
				}
			});
			
			tree.setEnabled(false);
			JScrollPane scrollPane = new JScrollPane(tree);
			Dimension size = new Dimension(120, 200);
			scrollPane.setMinimumSize(size);
			scrollPane.setPreferredSize(size);
			
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		
		/**
		 * Trigger an update with a given dataset
		 * @param dataset
		 */
		protected void updateDataset(AnalysisDataset dataset){
			DefaultMutableTreeNode root =
					new DefaultMutableTreeNode(new NodeData("Cells", null));
			
			if(dataset!=null){
				createNodes(root, dataset);
				tree.setEnabled(true);
			} else {
				TreeModel model = new DefaultTreeModel(root);
				tree.setModel(model);
				tree.setEnabled(false);
			}
			TreeModel model = new DefaultTreeModel(root);
			tree.setModel(model);
		}
		
		/**
		 * Create the nodes in the tree
		 * @param root the root node
		 * @param dataset the dataset to use
		 */
		private void createNodes(DefaultMutableTreeNode root, AnalysisDataset dataset){
		    
		    for(Cell cell : dataset.getCollection().getCells()){	
//		    	String name = cell.getCellId().toString();
		    	String name = cell.getNucleus().getNameAndNumber();
		    	UUID id = cell.getId();
//		    	root.add(new DefaultMutableTreeNode(name));
		    	root.add(new DefaultMutableTreeNode( new NodeData(name, id)));
		    }
		    sort(root);

		}
		
		private DefaultMutableTreeNode sort(DefaultMutableTreeNode node){
			for(int i = 0; i < node.getChildCount() - 1; i++) {
		        DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
		        String nt = child.getUserObject().toString();
		        
		        for(int j = i + 1; j <= node.getChildCount() - 1; j++) {
		            DefaultMutableTreeNode prevNode = (DefaultMutableTreeNode) node.getChildAt(j);
		            String np = prevNode.getUserObject().toString();

		            if(nt.compareToIgnoreCase(np) > 0) {
		                node.insert(child, j);
		                node.insert(prevNode, i);
		            }
		        }
		        if(child.getChildCount() > 0) {
		            sort(child);
		        }
			}
			return node;
			
		}
		
		public class NodeData {
			private String name;
			private UUID id;
			private String imageName;
			private int nucleusNumber;
			
			
			public NodeData(String name, UUID id) {
				this.name = name;
				this.id = id;
				if(!name.equals("Cells")){
					String[] array = name.split("\\.\\w+-"); // remove file extension and dash, leaving filename and nucleus number
					this.imageName = array[0];
					this.nucleusNumber = Integer.valueOf(array[1]);
				}
				
			}
			public String getName() {
				return name;
			}
			public UUID getID() {
				return id;
			}
			public String toString() {
//				return name;
				if(name.equals("Cells")){
					return name;
				}
				NumberFormat df = DecimalFormat.getInstance();
				df.setMaximumFractionDigits(0);
				df.setMinimumIntegerDigits(2);
				return imageName+"-"+df.format(nucleusNumber);
			}
		}
	}
	
		
	protected class ComponentListPanel extends JPanel implements ListSelectionListener {
						
		private JList<Object> signalList;
		private JScrollPane   scrollPane;
		private Cell componentCell = null;
		private String componentString = null; // store the active component name, and use it if available when cells change
		
		protected ComponentListPanel(){
			
			this.setLayout(new BorderLayout());
			
			scrollPane = new JScrollPane();
	
			signalList = new JList<Object>();
			ListModel<Object> model = createListModel(null);

			signalList.setModel(model);
			signalList.addListSelectionListener(this);
			signalList.setEnabled(false);
										
			scrollPane.setViewportView(signalList);
			Dimension size = new Dimension(120, 200);
			scrollPane.setMinimumSize(size);
			scrollPane.setPreferredSize(size);
			
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		private ListModel<Object> createListModel(Cell cell){
			DefaultListModel<Object> model = new DefaultListModel<Object>();
			
			if(cell!=null){
				SignalTableCell nucleusCell = new SignalTableCell(activeCell.getNucleus().getID(), "Nucleus");
				model.addElement(nucleusCell);
				for(UUID i : activeCell.getNucleus().getSignalCollection().getSignalGroupIDs()){
					if(activeCell.getNucleus().getSignalCollection().hasSignal(i)){

						SignalTableCell signalCell = new SignalTableCell(i, activeDataset().getCollection().getSignalGroup(i).getGroupName());
						model.addElement(signalCell);
					}
				}
			}
			return model;
		}
		
			
		protected void update(Cell cell){

			// Only update the component list if the cell has changed
			if(cell!=componentCell){
				componentCell = cell;
				finest("Updating component list for cell");
				ListModel<Object> model = createListModel(cell);
				signalList.setModel(model);

				if(cell!=null){
					finest("Cell is not null");
					signalList.removeListSelectionListener(this);
					
					// Check if the new cell has the same component as the last
					int index = 0;
					for(int i=0; i<model.getSize();i++){
						SignalTableCell tableCell   =  (SignalTableCell) signalList.getModel().getElementAt(i);
						if(tableCell.toString().equals(componentString)){
							index=i;
						}
					}
					signalList.setSelectedIndex(index);
					componentString = ((SignalTableCell) signalList.getModel().getElementAt(index)).toString(); // set the new component string
					
					setActiveComponent(getActiveComponent());
					
					signalList.addListSelectionListener(this);
					signalList.setEnabled(true);
				}
			}
		}
		
		private CellularComponent getActiveComponent(){
			int row = signalList.getSelectedIndex();
			CellularComponent c = null;
			if(row>=0){ // -1 if nothing selected
				SignalTableCell cell   =  (SignalTableCell) signalList.getModel().getElementAt(row);
				String signalGroupName = cell.toString();

				

				if(signalGroupName.equals("Nucleus")){

					c = activeCell.getNucleus();

				} else {

					UUID signalGroup = cell.getID();

					for(NuclearSignal n : activeCell.getNucleus().getSignalCollection().getSignals(signalGroup)){
						c = n;
					}

				}
				finest("Component selected is "+signalGroupName);
			}
			return c;
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {

			finest("Component selection changed");
			CellularComponent c = getActiveComponent();
			setActiveComponent(c);
			updateCell(activeCell);

			
		}
	}


	@Override
	public void valueChanged(TreeSelectionEvent arg0) {
		finest("Cell list selection changed");
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) arg0.getPath().getLastPathComponent();
		NodeData data = (NodeData) node.getUserObject();

		UUID cellID = data.getID();

		if(getDatasets().size()==1){	
			try{
				
				activeCell = activeDataset().getCollection().getCell(cellID);
				finest("Updating selected cell to "+activeCell.getNucleus().getNameAndNumber());
				updateCell(activeCell);
				
			} catch (Exception e1){
				warn("Error fetching cell");
				log(Level.FINE, "Error fetching cell", e1);
			}
		}
		
	}
	

}
