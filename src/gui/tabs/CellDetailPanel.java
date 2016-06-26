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
import gui.InterfaceEvent.InterfaceMethod;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.components.DraggableOverlayChartPanel;
import gui.components.panels.ProfileTypeOptionsPanel;
import gui.tabs.CellDetailPanel.CellsListPanel.NodeData;
import gui.tabs.cells.CellOutlinePanel;
import gui.tabs.cells.CellProfilePanel;
import gui.tabs.cells.CellStatsPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
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
import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.MorphologyChartFactory;
import charting.datasets.SignalTableCell;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import components.Cell;
import components.CellularComponent;
import components.generic.BorderTag;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclear.NuclearSignal;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class CellDetailPanel extends DetailPanel implements SignalChangeListener, TreeSelectionListener {
	
	private Cell activeCell = null;
	
	private CellularComponent activeComponent = null; 
	
	private JTabbedPane tabPane; 
	
	protected CellsListPanel	cellsListPanel;		// the list of cells in the active dataset
	protected CellProfilePanel	 	profilePanel = new CellProfilePanel(); 		// the nucleus angle profile
	protected CellOutlinePanel 	outlinePanel     = new CellOutlinePanel(); 		// the outline of the cell and detected objects
	protected CellStatsPanel 	cellStatsPanel   = new CellStatsPanel();		// the stats table
	protected SignalListPanel 	signalListPanel;	// choose which background image to display

	public CellDetailPanel() {

		super();

		try{
			
			this.setLayout(new BorderLayout());
			
			this.add(createCellandSignalListPanels(), BorderLayout.WEST);
			
			
			this.addSubPanel(cellStatsPanel);
			this.addSubPanel(profilePanel);
			this.addSubPanel(outlinePanel);
			
			tabPane = new JTabbedPane(JTabbedPane.LEFT);
			this.add(tabPane, BorderLayout.CENTER);
			
			tabPane.add("Info", cellStatsPanel);
			
			tabPane.add("Segments", profilePanel);
			
			tabPane.add("Outline", outlinePanel);
			
			
			
			
//			this.setLayout(new GridBagLayout());
//			
//			
//
//			GridBagConstraints constraints = new GridBagConstraints();
//			constraints.fill = GridBagConstraints.BOTH;
//			constraints.gridx = 0;
//			constraints.gridy = 0;
//			constraints.gridheight = 2;
//			constraints.gridwidth = 1;
//			constraints.weightx = 0.5;
//			constraints.weighty = 0.6;
//			constraints.anchor = GridBagConstraints.CENTER;
//
//			cellsListPanel = new CellsListPanel();
//			cellsListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
//			this.add(cellsListPanel, constraints);
//			
//			constraints.gridx = 0;
//			constraints.gridy = 2;
//			constraints.gridheight = 2;
//			constraints.gridwidth = 1;
//			constraints.weightx = 0.5;
//			constraints.weighty = 0.4;
//			signalListPanel = new SignalListPanel();
//			signalListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
//			this.add(signalListPanel, constraints);
//
//			// make the chart for each nucleus
//			JPanel centrePanel = createCentrePanel();
//
//			constraints.gridx = 1;
//			constraints.gridy = 0;
//			constraints.gridwidth = 2;
//			constraints.gridheight = 4;
//			constraints.weightx = 0.9;
//			constraints.weighty = 1;
//			this.add(centrePanel, constraints);
//
//
////			outlinePanel = new OutlinePanel();
//			outlinePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
//			constraints.gridx = 3;
//			constraints.gridy = 0;
//			constraints.weightx = 0.7;
//			this.add(outlinePanel, constraints);
			outlinePanel.setParent(this);
////			this.addSubPanel(outlinePanel);
//
//
//			this.validate();
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
		signalListPanel = new SignalListPanel();
		signalListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		panel.add(signalListPanel, constraints);
		
//		cellsListPanel = new CellsListPanel();
//		panel.add(cellsListPanel, BorderLayout.NORTH);
		
//		signalListPanel = new SignalListPanel();
//		panel.add(signalListPanel, BorderLayout.SOUTH);
		
		return panel;
	}

	

	
			
	public CellularComponent getActiveComponent(){
		return this.activeComponent;
	}
	
	public Cell getActiveCell(){
		return this.activeCell;
	}
	
	@Override
	protected void updateSingle() {
		cellsListPanel.updateDataset( activeDataset()  );
		outlinePanel.update(getDatasets());
		cellStatsPanel.update(getDatasets());
		profilePanel.update(getDatasets());
		
		
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
		profilePanel.update(cell);
		outlinePanel.update(cell);
		
		
	}
	
	
//	private void updateSegmentIndex(boolean start, int index, NucleusBorderSegment seg, Nucleus n, SegmentedProfile profile) throws Exception{
//		
//		int startPos = start ? seg.getStartIndex() : seg.getEndIndex();
//		int newStart = start ? index : seg.getStartIndex();
//		int newEnd = start ? seg.getEndIndex() : index;
//		
//		int rawOldIndex =  n.getOffsetBorderIndex(BorderTag.REFERENCE_POINT, startPos);
//
//						
//		if(profile.update(seg, newStart, newEnd)){
//			n.setProfile(ProfileType.ANGLE, BorderTag.REFERENCE_POINT, profile);
//			
//			/* Check the border tags - if they overlap the old index
//			 * replace them. 
//			 */
//			int rawIndex = n.getOffsetBorderIndex(BorderTag.REFERENCE_POINT, index);
//			log(Level.FINEST, "Testing border tags");
//			log(Level.FINEST, "Updating to index "+index+" from reference point");
//			log(Level.FINEST, "Raw old border point is index "+rawOldIndex);
//			log(Level.FINEST, "Raw new border point is index "+rawIndex);
//			
//			if(n.hasBorderTag(rawOldIndex)){						
//				BorderTag tagToUpdate = n.getBorderTag(rawOldIndex);
//				log(Level.FINE, "Updating tag "+tagToUpdate);
//				n.setBorderTag(tagToUpdate, rawIndex);	
//				
//				// Update intersection point if needed
//				if(tagToUpdate.equals(BorderTag.ORIENTATION_POINT)){
//					n.setBorderTag(BorderTag.INTERSECTION_POINT, n.getBorderIndex(n.findOppositeBorder(n.getBorderTag(BorderTag.ORIENTATION_POINT))));
//				}
//				
//			} else {
//				log(Level.FINEST, "No border tag at index "+rawOldIndex+" from reference point");
////				log(Level.FINEST, n.dumpInfo(Nucleus.ALL_POINTS));
//			}
//
//			updateCell(activeCell);
//			fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
//		} else {
//			log(Level.INFO, "Updating "+seg.getStartIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
//		}
//	}
			

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
		
		private static final long serialVersionUID = 1L;
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
	
	
//	/**
//	 * Show the profile for the nuclei in the given cell
//	 *
//	 */
//	protected class ProfilePanel extends JPanel implements SignalChangeListener, ActionListener {
//		
//		private DraggableOverlayChartPanel profileChartPanel; // holds the chart with the cell
//		private ProfileTypeOptionsPanel profileOptions  = new ProfileTypeOptionsPanel();
//				
//		protected ProfilePanel(){
//
//			this.setLayout(new BorderLayout());
//			JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
//			
//			profileChartPanel = new DraggableOverlayChartPanel(chart, null, false); 
//			profileChartPanel.addSignalChangeListener(this);
//			this.add(profileChartPanel, BorderLayout.CENTER);
//			
//			JPanel header = new JPanel(new FlowLayout());
//			header.add(profileOptions);
//			profileOptions.addActionListener(  e -> update(activeCell)   );
//						
//			this.add(header, BorderLayout.NORTH);
//			
//		}
//		
//		protected void update(Cell cell){
//
//			try{
//				
//				ProfileType type = profileOptions.getSelected();
//
//				if(cell==null){
//					JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
//					profileChartPanel.setChart(chart);
//					profileOptions.setEnabled(false);
//
//				} else {
//
//					profileOptions.setEnabled(true);
//					Nucleus nucleus = cell.getNucleus();
//					
//					ChartOptions options = new ChartOptionsBuilder()
//							.setSwatch(activeDataset().getSwatch())
//							.setProfileType(type)
//							.build();
//
////					JFreeChart chart = MorphologyChartFactory.makeIndividualNucleusProfileChart(nucleus, options);
//
////					profileChartPanel.setChart(chart, nucleus.getProfile(ProfileType.ANGLE, BorderTag.REFERENCE_POINT), false);
//					
//				}
//
//			} catch(Exception e){
//				error("Error updating cell panel", e);
//				JFreeChart chart = MorphologyChartFactory.getInstance().makeEmptyChart();
//				profileChartPanel.setChart(chart);
//				profileOptions.setEnabled(false);
//			}
//
//		}
//
//		@Override
//		public void signalChangeReceived(SignalChangeEvent event) {
//			if(event.type().contains("UpdateSegment")){
//
//				try{
////					
//					String[] array = event.type().split("\\|");
//					int selectedSegMidpoint = Integer.valueOf(array[1]);
//					String index = array[2];
//					int indexValue = Integer.valueOf(index);
//
//					Nucleus n = activeCell.getNucleus();
//					SegmentedProfile profile = n.getProfile(ProfileType.ANGLE, BorderTag.REFERENCE_POINT);
//					
//					/*
//					 * The numbering of segments is adjusted for profile charts, so we can't rely on 
//					 * the segment name stored in the profile.
//					 * 
//					 * Get the name via the midpoint index of the segment that was selected. 
//					 */
//					NucleusBorderSegment seg = profile.getSegmentContaining(selectedSegMidpoint);
////					NucleusBorderSegment seg = profile.getSegment(segName);
//
//					updateSegmentIndex(true, indexValue, seg, n, profile);
//					
//					n.updateVerticallyRotatedNucleus();
//					fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());
//				} catch(Exception e){
//					error("Error updating segment", e);
//				}
//
//			}
//			
//			
//		}
//
//		@Override
//		public void actionPerformed(ActionEvent e) {
//			update(activeCell);			
//		}
//
//	}
	
	protected class SignalListPanel extends JPanel implements ListSelectionListener {
		
		private static final long serialVersionUID = 1L;
				
		private JList<Object> signalList;
		private JScrollPane scrollPane;
		
		protected SignalListPanel(){
			
			this.setLayout(new BorderLayout());
			
			scrollPane = new JScrollPane();
						
			try {
				
				
				signalList = new JList<Object>();
				DefaultListModel<Object> model = new DefaultListModel<Object>();

				signalList.setModel(model);
				signalList.addListSelectionListener(this);
				signalList.setEnabled(false);
				
			} catch (Exception e) {
				error("Error in segment stats", e);
			}
						
			scrollPane.setViewportView(signalList);
			Dimension size = new Dimension(120, 200);
			scrollPane.setMinimumSize(size);
			scrollPane.setPreferredSize(size);
			
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
			
		protected void update(Cell cell){

			if(cell!=null){
				
				activeComponent = activeCell.getNucleus();
				
				try {
					DefaultListModel<Object> model = new DefaultListModel<Object>();
					SignalTableCell nucleusCell = new SignalTableCell(activeCell.getNucleus().getID(), "Nucleus");
					model.addElement(nucleusCell);
					for(UUID i : activeCell.getNucleus().getSignalCollection().getSignalGroupIDs()){
						if(activeCell.getNucleus().getSignalCollection().hasSignal(i)){
							
							SignalTableCell signalCell = new SignalTableCell(i, activeDataset().getCollection().getSignalGroup(i).getGroupName());
							model.addElement(signalCell);
						}
					}
					signalList.setModel(model);
					signalList.setSelectedIndex(0);
					signalList.setEnabled(true);

				} catch (Exception e) {
					error("Error updating signal list", e);
				}
			} else {
				try {
					DefaultListModel<Object> model = new DefaultListModel<Object>();

					signalList.setModel(model);
					signalList.setEnabled(false);
				} catch (Exception e) {
					error("Error updating signal list", e);
				}
			}
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {

//			if selected value is a valid file reference, load the file and update the
			// nucleus annotation
			int row = signalList.getSelectedIndex();
			
			if(row>=0){ // -1 if nothing selected
				SignalTableCell cell   =  (SignalTableCell) signalList.getModel().getElementAt(row);
				String signalGroupName = cell.toString();

				if(signalGroupName.equals("Nucleus")){
					
					activeComponent = activeCell.getNucleus();

				} else {

					UUID signalGroup = cell.getID();

					for(NuclearSignal n : activeCell.getNucleus().getSignalCollection().getSignals(signalGroup)){
						activeComponent = n;
					}

				}
				outlinePanel.update(activeCell);
			}
			
		}
	}


	@Override
	public void valueChanged(TreeSelectionEvent arg0) {

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) arg0.getPath().getLastPathComponent();
		NodeData data = (NodeData) node.getUserObject();

		UUID cellID = data.getID();

		if(getDatasets().size()==1){	
			try{
				
				activeCell = activeDataset().getCollection().getCell(cellID);
				updateCell(activeCell);
				
			} catch (Exception e1){
				warn("Error fetching cell");
				log(Level.FINE, "Error fetching cell", e1);
			}
		}
		
	}
	

}
