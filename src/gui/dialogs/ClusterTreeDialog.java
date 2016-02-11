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
package gui.dialogs;

import gui.DatasetEvent;
import gui.DatasetEventListener;
import gui.ImageType;
import gui.InterfaceEvent;
import gui.InterfaceEvent.InterfaceMethod;
import gui.InterfaceEventListener;
import gui.LoadingIconDialog;
import gui.DatasetEvent.DatasetMethod;
import gui.components.ColourSelecter;
import gui.components.DraggableTreeViewer;
import gui.components.VariableNodePainter;
import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import components.Cell;
import components.CellCollection;
import components.ClusterGroup;
import components.nuclei.Nucleus;
import analysis.AnalysisDataset;
import analysis.ClusteringOptions;
import analysis.ClusteringOptions.ClusteringMethod;
import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.ImportException.DuplicateTaxaException;
import jebl.evolution.io.NewickImporter;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.TransformedRootedTree;
import jebl.evolution.trees.Tree;
import jebl.gui.trees.treeviewer.painters.BasicLabelPainter.PainterIntent;
import jebl.gui.trees.treeviewer.treelayouts.RectilinearTreeLayout;
import jebl.gui.trees.treeviewer.treelayouts.TreeLayout;
import jebl.gui.trees.treeviewer.TreePane;
import jebl.gui.trees.treeviewer.TreePaneSelector.SelectionMode;
import jebl.gui.trees.treeviewer.TreeViewer.TreeLayoutType;

@SuppressWarnings("serial")
public class ClusterTreeDialog extends LoadingIconDialog implements ActionListener, ItemListener {

	private JPanel buttonPanel;
	private DraggableTreeViewer viewer;
	private AnalysisDataset dataset;
	private ClusterGroup group;
	
	private int numberOfTimesColouringCalled = 0; // for debugging, ignore
	
	private JComboBox<AnalysisDataset> selectedClusterBox;
	private JComboBox<ClusterGroup> selectedClusterGroupBox;
	
	private List<Object> datasetListeners = new ArrayList<Object>();
	
	private List<CellCollection> clusterList = new ArrayList<CellCollection>(0);
	
	private boolean hasMergeSources; // cache this to speed comparisons
	
	public ClusterTreeDialog(final Logger programLogger, final AnalysisDataset dataset, final ClusterGroup group) {
		super(programLogger);
		this.dataset = dataset;
		this.group = group;
		this.hasMergeSources = dataset.hasMergeSources();
		
		try{

			programLogger.log(Level.FINEST, "Building tree view");
			this.setLayout(new BorderLayout());
			this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			this.viewer = new DraggableTreeViewer();
			programLogger.log(Level.FINEST, "Created draggable viewer");
			
			viewer.getTreePane().addMouseListener(new MouseClusterSelectionAdapter());
			programLogger.log(Level.FINEST, "Added listener");


			this.add(viewer, BorderLayout.CENTER);
			programLogger.log(Level.FINEST, "Added viewer");

			this.buttonPanel = createButtonPanel();
			programLogger.log(Level.FINEST, "Made button panel");
			this.add(buttonPanel, BorderLayout.NORTH);

			programLogger.log(Level.FINEST, "Importing tree");
			importTree();
			programLogger.log(Level.FINEST, "Imported tree");

			this.setModal(false);
			this.setMinimumSize(new Dimension(500, 500));
			this.pack();
			this.setLocationRelativeTo(null);
			programLogger.log(Level.FINEST, "Displaying dialog");
			this.setVisible(true);
		} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error creating tree view", e);
				this.dispose();
		}
	}
	
	/**
	 * Turn the Newick string in the cluster group into a tree,
	 * and display it  
	 */
	private void importTree(){
		programLogger.log(Level.FINE, "Reading tree");
		StringReader reader = new StringReader(group.getTree());

		boolean readUnquotedLabels = true;
		NewickImporter imp = new NewickImporter(reader, readUnquotedLabels);

		try {
			List<Tree> trees =  imp.importTrees();
			RootedTree topTree = (RootedTree) trees.get(0);
			
			/*
			 * TODO
			 * The tree (if created in 1.11.6 or above) will
			 * have the taxon labels as the Cell UUID.
			 * 
			 * If below 1.11.6, the label will be the path and nucleus name
			 * 
			 * Where UUIDs are used, the tree must be converted to a tree
			 * with image names
			 */
			
			
			
			int numTaxa = topTree.getTaxa().size(); 
			programLogger.log(Level.FINE, "Tree has "+numTaxa+" taxa");
			
			viewer.setTree( topTree );
			
			viewer.setSelectionMode(SelectionMode.CLADE);
			viewer.setTreeLayoutType(TreeLayoutType.RECTILINEAR);
			viewer.getTreePane().setBranchTransform(true,  TransformedRootedTree.Transform.PROPORTIONAL);
			

			this.setTitle(dataset.getName() + " : " + group.getName() +" : "+numTaxa+ " taxa");
			colourTreeNodesByClusterGroup(group);
			
		} catch (IOException e) {
			programLogger.log(Level.SEVERE, "Error reading tree", e);
		} catch (DuplicateTaxaException e){
			programLogger.log(Level.WARNING, "Unable to display tree: duplicate taxon names");
		} catch (ImportException e) {
			programLogger.log(Level.SEVERE, "Error in tree IO", e);
		}
	}
		
	private JPanel createButtonPanel(){
		JPanel panel = new JPanel(new FlowLayout());
		
		JButton extractButton = new JButton("Extract selected as cluster");
		extractButton.addActionListener(this);
		extractButton.setActionCommand("Extract");
		panel.add(extractButton);
		
		JButton analyseButton = new JButton("Analyse new clusters");
		analyseButton.addActionListener(this);
		analyseButton.setActionCommand("Analyse");
		panel.add(analyseButton);
		
		if(dataset.hasMergeSources()){
			JButton mergeSourceButton = new JButton("Show merge sources");
			mergeSourceButton.addActionListener(this);
			mergeSourceButton.setActionCommand("ShowMergeSources");
			panel.add(mergeSourceButton);
		}
		
		selectedClusterBox = new JComboBox<AnalysisDataset>();
		selectedClusterBox.addItem(dataset);
		for(AnalysisDataset d: dataset.getAllChildDatasets()){
			selectedClusterBox.addItem(d);
		}
		selectedClusterBox.setSelectedIndex(-1);
		selectedClusterBox.addItemListener(this);
		panel.add(selectedClusterBox);
		
		selectedClusterGroupBox = new JComboBox<ClusterGroup>();
		for(ClusterGroup g: dataset.getClusterGroups()){
			selectedClusterGroupBox.addItem(g);
		}
		selectedClusterGroupBox.setSelectedItem(group);
		selectedClusterGroupBox.addItemListener(this);
		panel.add(selectedClusterGroupBox);
		
		panel.add(this.getLoadingLabel());
		
		return panel;
	}
	
	/**
	 * Update the taxon colours to match their cluster
	 * @param cluster the dataset of nuclei in the cluster
	 */
	private void colourTreeNodesByCluster(final CellCollection cluster){

		if(cluster!=null){
			setStatusLoading();
			
			Map<Node, Color> clusterMemberships = new HashMap<Node, Color>();
			
			List<UUID> completedNuclei = new ArrayList<UUID>(); // store each nucleus assigned
			
			Map<Node, Color> map = assignNodeMemberships(completedNuclei, cluster, -1);
			 
			for(Node n : map.keySet()){
				clusterMemberships.put(n, map.get(n));
			}
			
			RootedTree tree = viewer.getTreePane().getTree();		

			VariableNodePainter painter = new VariableNodePainter("cluster", tree, PainterIntent.TIP, clusterMemberships);
			painter.setBorder(Color.BLACK, new BasicStroke(2f));
			viewer.getTreePane().setTaxonLabelPainter(painter);
			setStatusLoaded();

		}
	}
		
	private void colourTreeNodesByClusterGroup(final ClusterGroup group){

		numberOfTimesColouringCalled++;
		if(group!=null){
			programLogger.log(Level.FINER, "Colouring nodes by cluster group: "+group.getName()+" iteration "+numberOfTimesColouringCalled);	
			
			setStatusLoading();
			Thread thr = new Thread(){
				
				public void run(){
			
					Map<Node, Color> clusterMemberships = new HashMap<Node, Color>();
//					RootedTree tree = viewer.getTreePane().getTree();
					
					List<UUID> completedNuclei = new ArrayList<UUID>(); // store each nucleus assigned
					
					int clusterNumber = 0;
					for(UUID id : group.getUUIDs()){
						
						AnalysisDataset cluster = null;
												
						if(dataset.hasChild(id)){
							
							cluster = dataset.getChildDataset(id);
							
						} else if(dataset.hasMergeSource(id)){
							
							cluster = dataset.getMergeSource(id);
						} else {
							// If the cluster was not found, stop
							programLogger.log(Level.WARNING, "Child dataset not found, cancelling");
							return;
						}
						
						 Map<Node, Color> map = assignNodeMemberships(completedNuclei, cluster.getCollection(), clusterNumber);
						 
						 for(Node n : map.keySet()){
							 clusterMemberships.put(n, map.get(n));
						 }
						
						clusterNumber++;
						programLogger.log(Level.FINER, "Node colours assigned");	
		
					}
					RootedTree tree = viewer.getTreePane().getTree();
					VariableNodePainter painter = new VariableNodePainter("Cluster", tree, PainterIntent.TIP, clusterMemberships);
					painter.setBorder(Color.BLACK, new BasicStroke(2f));
					viewer.getTreePane().setTaxonLabelPainter(painter);
					setStatusLoaded();
				}
			};
			thr.start();
		}
	}
	
	private Map<Node, Color> assignNodeMemberships(List<UUID> completedNuclei, CellCollection cluster, int clusterNumber){
		
		RootedTree tree = viewer.getTreePane().getTree();
		
		Map<Node, Color> clusterMemberships = new HashMap<Node, Color>();
		
		// Cache the list of nuclei to be tested
		List<UUID> toAnalyse = new ArrayList<UUID>();
		for(Cell cell : cluster.getCells()){
			
			if( ! completedNuclei.contains(cell.getId())){
				toAnalyse.add(UUID.fromString(cell.getId().toString()));
			}
		}
		programLogger.log(Level.FINEST, "Built "+toAnalyse.size()+" cell list");	
								
		programLogger.log(Level.FINER, "Colouring dataset "+cluster.getName());				
		
		Color colour = clusterNumber == -1 ? Color.BLACK : ColourSelecter.getSegmentColor(clusterNumber);

		for(Node n : tree.getNodes()){

			if(tree.isExternal(n)){ // choose the taxon nodes

				Taxon t = tree.getTaxon(n);
				String name = t.getName();
				

				programLogger.log(Level.FINEST, "Testing "+name+" against "+toAnalyse.size()+" cells");	
				
				
				for (Iterator<UUID> iterator = toAnalyse.iterator(); iterator.hasNext();) {
					UUID nid = iterator.next();
					Nucleus nucleus = cluster.getCell(nid).getNucleus();
					if(taxonNamesMatch(name, nucleus)){
						programLogger.log(Level.FINEST, "Added colour for "+name);	
						clusterMemberships.put(n, colour);
						iterator.remove();
						completedNuclei.add(nid);
					}
				}						
			}

		}
		return clusterMemberships;
	}

	/**
	 * Check that a taxon name matches a nucleus name
	 * @param name
	 * @param nucleus
	 * @return
	 */
	private boolean taxonNamesMatch(String name, Nucleus nucleus){
		
		/*
		Testing name: P106.tiff-3
		Testing J:\Protocols\Scripts and macros\Testing_cluster_images\P100.tiff-P100.tiff-0
		Testing Testing_cluster_images-P100.tiff-0
		Testing P100.tiff-0
		Name not found
		*/
		String nucleusName = nucleus.getSourceFile()+"-"+nucleus.getNameAndNumber();
		
//		programLogger.log(Level.FINEST, "\tTesting name: "+name);
		
//		programLogger.log(Level.FINEST, "\t\tTesting "+nucleusName);

		// the ideal is full file path
		if(name.equals(nucleusName)){ // 'C:\bla\image.tiff-image.tiff-1'
			return true;
		} else {
//			return false;
			// otherwise look for the folder and name
			
			nucleusName = nucleus.getSourceFolder().getAbsolutePath()+"-"+nucleus.getNameAndNumber();
//			programLogger.log(Level.FINEST, "\t\tTesting "+nucleusName);
			if(name.equals(nucleusName)){
				return true;
			} else {
//				// Can't get just names from merge sources
				if(hasMergeSources){
//					programLogger.log(Level.FINEST, "Cannot test further in a merge source");
					return false;
				} else {
					// otherwise look for just the name from an old dataset
					nucleusName = nucleus.getNameAndNumber();
//					programLogger.log(Level.FINEST, "\t\tTesting "+nucleusName);
					if(name.equals(nucleusName)){
						return true;
					} else {
//						programLogger.log(Level.FINEST, "Name not found");s
						return false;
					}
				}
			}
		}
	}
	
	private String checkName(int offset){
		
		int maxExisting = 0;
		Pattern pattern = Pattern.compile(dataset.getName()+"_ManualCluster_(\\d+)$");
		
		for(AnalysisDataset d : dataset.getChildDatasets()){

			Matcher matcher = pattern.matcher(d.getName());

			int digit = 0;

			while (matcher.find()) {

				digit = Integer.valueOf(matcher.group(1));

				if(digit>maxExisting){
					maxExisting = digit;
				}

			}
			
		}
		
		int clusterNumber = maxExisting+offset;
		
		String result = dataset.getName()+"_ManualCluster_"+clusterNumber;
		
		return result;
	}
	
	private void extractSelectedNodesToCluster() throws Exception{
		CellCollection template = dataset.getCollection();
		
		CellCollection clusterCollection = new CellCollection(template.getFolder(), 
				template.getOutputFolderName(), 
				template.getName()+"_ManualCluster_"+clusterList.size(), 
				template.getNucleusType());
		
		String newName = template.getName()+"_ManualCluster_"+clusterList.size();
		newName = checkName(clusterList.size());
		
		clusterCollection.setName(newName);

		Tree tree = viewer.getTreePane().getTree();

		Set<Node> nodes = viewer.getTreePane().getSelectedNodes();
		for(Node n : nodes){

			if(tree.isExternal(n)){

				Taxon t = tree.getTaxon(n);
				
				String name = t.getName();
				
				for(Cell c : dataset.getCollection().getCells()){

					if(taxonNamesMatch(name, c.getNucleus())){
						clusterCollection.addCell(new Cell (c));
					}
				}
			}

		}
		
		if(clusterCollection.hasCells()){
			colourTreeNodesByCluster(clusterCollection);
			clusterList.add(clusterCollection);
			programLogger.log(Level.INFO, "Extracted "+clusterCollection.cellCount()+" cells");
		} else {
			programLogger.log(Level.WARNING, "No cells found. Check taxon labels are correct");
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if(e.getActionCommand().equals("Extract")){
			
			try {
				extractSelectedNodesToCluster();
			} catch (Exception e1) {
				programLogger.log(Level.SEVERE, "Error extracting cells", e);
			}
			

		}
		
		if(e.getActionCommand().equals("Analyse")){
			
			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			
			for(CellCollection c : clusterList){
				if(c.hasCells()){

					dataset.addChildCollection(c);
					try {
						dataset.getCollection().getProfileManager().copyCollectionOffsets(c);
					} catch (Exception e1) {
						programLogger.log(Level.SEVERE, "Error applying segments", e);
					}

					AnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
					clusterDataset.setRoot(false);
					list.add(clusterDataset);
				}
			}
			
			testClusterGroupable(list);

			
			if(!list.isEmpty()){
//				fireDatasetEvent(DatasetMethod.COPY_MORPHOLOGY, list, dataset);
				fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
				programLogger.log(Level.FINEST, "Fired dataset copy event to listeners");
			} else {
				programLogger.log(Level.WARNING, "No datasets to analyse");
			}
			this.setVisible(false);
			this.dispose();
		}
		
		if(e.getActionCommand().equals("ShowMergeSources")){
			
			// Disable the selection dropdown boxes
			setClusterGroupBoxNull();
			setClusterBoxNull();
			
			
			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			for(UUID id : dataset.getMergeSources()){
				list.add(dataset.getMergeSource(id));
			}
			
//			programLogger.log(Level.INFO, "Detected "+list.size()+" merge sources");
			
			ClusterGroup mergeGroup = makeNewClusterGroup(list);
		
			
			for(AnalysisDataset d : list){
				mergeGroup.addDataset(d);
//				programLogger.log(Level.INFO, "Added merge source with "+d.getCollection().size()+" nuclei");
			}
			
			colourTreeNodesByClusterGroup(mergeGroup);

		}
		

	}
	
	/**
	 * Create a new cluster group based on the clustering options
	 * in the existing cluster group, and a new list of datasets
	 * @param list the datasets to include in the cluster group
	 * @return
	 */
	private ClusterGroup makeNewClusterGroup(List<AnalysisDataset> list){
		ClusteringOptions newOptions = new ClusteringOptions(group.getOptions());
		newOptions.setClusterNumber(list.size());		
		
		int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
		ClusterGroup newGroup = new ClusterGroup("ClusterGroup_"+clusterNumber, newOptions, group.getTree());
		return newGroup;
	}
	
	/**
	 * Set the cluster group drop down menu to null.
	 * Removes and replaces the item listener to avoid 
	 * recursive firing
	 */
	private void setClusterGroupBoxNull(){
		selectedClusterGroupBox.removeItemListener(this);
		selectedClusterGroupBox.setSelectedIndex(-1);
		selectedClusterGroupBox.addItemListener(this);
	}
	
	/**
	 * Set the cluster drop down menu to null.
	 * Removes and replaces the item listener to avoid 
	 * recursive firing
	 */
	private void setClusterBoxNull(){
		selectedClusterBox.removeItemListener(this);
		selectedClusterBox.setSelectedIndex(-1);
		selectedClusterBox.addItemListener(this);
	}
	
	/**
	 * Check that the list of datasets has only one copy of each
	 * cell
	 * @param list
	 * @return
	 */
	private boolean checkCellPresentOnlyOnce(List<AnalysisDataset> list){
		boolean ok = true;
		// Check that a cell is not present in more than one cluster
		List<UUID> cellIDsFound = new ArrayList<UUID>();
		for(AnalysisDataset d : list){
			for(Cell c : d.getCollection().getCells()){
				if(cellIDsFound.contains(c.getId())){
					ok=false;
				}
				cellIDsFound.add(c.getId());
			}
		}
		return ok;
	}
	
	/**
	 * Check that a cell is not present in more than one cluster
	 * @param list
	 * @return
	 */
	private boolean checkAllCellsPresent(List<AnalysisDataset> list){
		boolean ok = true;
		
		List<UUID> cellIDsFound = new ArrayList<UUID>();
		for(AnalysisDataset d : list){
			for(Cell c : d.getCollection().getCells()){
				cellIDsFound.add(c.getId());
			}
		}
		
		for(Cell c : dataset.getCollection().getCells()){
			if(!cellIDsFound.contains(c.getId())){
				ok=false;
			}
		}
		
		return ok;
	}
	
	/*
	 * Offer to put the datasets into a cluster group if conditions are met
	 */
	private void testClusterGroupable(List<AnalysisDataset> list){
		
		if(!list.isEmpty()){
			
			if( checkCellPresentOnlyOnce(list)  ){

				if( checkAllCellsPresent(list) ) {
					// Offer to make a cluster group
					int option = JOptionPane.showOptionDialog(null, 
							"Join the new clusters into a cluster group?", 
							"Create cluster group", 
							JOptionPane.OK_CANCEL_OPTION, 
							JOptionPane.QUESTION_MESSAGE, null, null, null);

					if (option == JOptionPane.CANCEL_OPTION) {
						// Cancelled
						programLogger.log(Level.INFO, "Adding as standard manual clusters");
					} else if (option == JOptionPane.OK_OPTION)	{
						// Make the group

						ClusterGroup newGroup = makeNewClusterGroup(list);

						for(AnalysisDataset d : list){
							d.setName(newGroup.getName()+"_"+d.getName());
							newGroup.addDataset(d);
						}
						dataset.addClusterGroup(newGroup);

					}
				} else {
					programLogger.log(Level.INFO, "Cannot make cluster group");
					programLogger.log(Level.INFO, "Not all cells are assigned clusters");
					programLogger.log(Level.INFO, "Adding as standard manual clusters");
				}
			} else {
				programLogger.log(Level.INFO, "Cannot make cluster group");
				programLogger.log(Level.INFO, "Cells present in more than one cluster");
				programLogger.log(Level.INFO, "Adding as standard manual clusters");
			}
			
		}
	}
	
	protected synchronized void fireDatasetEvent(DatasetMethod method, List<AnalysisDataset> list, AnalysisDataset template) {

		DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list, template);
		Iterator<Object> iterator = datasetListeners.iterator();
		while( iterator.hasNext() ) {
			( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
		}
	}
	
	protected synchronized void fireInterfaceEvent(InterfaceMethod method) {

		InterfaceEvent event = new InterfaceEvent( this, method, this.getClass().getSimpleName());
		Iterator<Object> iterator = datasetListeners.iterator();
		while( iterator.hasNext() ) {
			( (InterfaceEventListener) iterator.next() ).interfaceEventReceived( event );
		}
	}
	
	public synchronized void addInterfaceEventListener( InterfaceEventListener l ) {
		datasetListeners.add( l );
	}

	public synchronized void removeInterfaceEventListener( InterfaceEventListener l ) {
		datasetListeners.remove( l );
	}

	public synchronized void addDatasetEventListener( DatasetEventListener l ) {
		datasetListeners.add( l );
	}

	public synchronized void removeDatasetEventListener( DatasetEventListener l ) {
		datasetListeners.remove( l );
	}

	@Override
	public void itemStateChanged(ItemEvent e) {


		if(e.getSource().equals(selectedClusterBox)){
			
			setClusterGroupBoxNull();
			AnalysisDataset selected = (AnalysisDataset) selectedClusterBox.getSelectedItem();
			colourTreeNodesByCluster(selected.getCollection());
			
		}
		
		if(e.getSource().equals(selectedClusterGroupBox)){
			
			setClusterBoxNull();
			colourTreeNodesByClusterGroup((ClusterGroup) selectedClusterGroupBox.getSelectedItem());
			
		}
		
	}
	
	protected class MouseClusterSelectionAdapter extends MouseAdapter{
		
		public MouseClusterSelectionAdapter(){
			
		}
		
//		public void mousePressed(MouseEvent e){
//			
//			TreePane pane = viewer.getTreePane();
//			Point location = pane.getMousePosition();
//			RootedTree tree = pane.getTree();
//			/*
//			 * How to get the rectangle left of the mouse x-position?
//			 * Then select all nodes to the left, and find the nodes with no children
//			 * selected. Use their direct children as the new clusters
//			 */
//			IJ.log("Click heard");
//			
////			 The region left of the line
//			Rectangle r = new Rectangle( (int) location.getX(), (int) pane.getBounds().getHeight());
//			viewer.setSelectionMode(SelectionMode.NODE);
//			pane.setDragRectangle(r);
//			
//			// Need to select the given nodes
//			
//			Set<Node> leftNodes = pane.getSelectedNodes();
//
////			Set<Node> leftNodes = viewer.getNodesAtPoint((Graphics2D) pane.getGraphics(), r);
////			Set<Node> leftNodes = getNodesAt((Graphics2D) pane.getGraphics(), r);
//			
//			// The child free nodes
//			Set<Node> childLess = new HashSet<Node>();
//			for(Node n : leftNodes){
//				for(Node check : leftNodes){
//					if( ! tree.getChildren(n).contains(check)){
//						// no children in the left space
//						childLess.add(n);
//					}
//				}
//			}
//			IJ.log("Found "+childLess.size()+" child free nodes left of point");
//			viewer.setSelectionMode(SelectionMode.CLADE);
//			for(Node n : childLess){
//				List<Node> clusterNodes = tree.getChildren(n);
//				
//				for(Node clusterNode : clusterNodes){
//					pane.setSelectedNode(clusterNode);
//					extractSelectedNodesToCluster();
//					
//				}
//			}
//			IJ.log("Clusters made");
//			
//			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
//			
//			for(CellCollection c : clusterList){
//				if(c.hasCells()){
//
//					dataset.addChildCollection(c);
//
//					AnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
//					clusterDataset.setRoot(false);
//					list.add(clusterDataset);
//				}
//			}
//			
//			ClusteringOptions newOptions = new ClusteringOptions(ClusteringMethod.HIERARCHICAL);
//			newOptions.setClusterNumber(list.size());
//			newOptions.setHierarchicalMethod(group.getOptions().getHierarchicalMethod());
//			newOptions.setIncludeModality(group.getOptions().isIncludeModality());
//			newOptions.setModalityRegions(group.getOptions().getModalityRegions());
//			newOptions.setUseSimilarityMatrix(group.getOptions().isUseSimilarityMatrix());
//			int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
//			ClusterGroup newGroup = new ClusterGroup("ClusterGroup_"+clusterNumber, newOptions, group.getTree());
//
//			for(AnalysisDataset d : list){
//				d.setName(newGroup.getName()+"_"+d.getName());
//				newGroup.addDataset(d);
//			}
//			
//			colourTreeNodesByClusterGroup(newGroup);
//			
//			
//		}
		
//		private Set<Node> getNodesAt(Graphics2D g2, Rectangle rect) {
////
//	        Set<Node> nodes = new HashSet<Node>();
////
//	        Tree tree = viewer.getTreePane().getTree();
//	        
//	        IJ.log("Selection rectangle: "+rect.x+", "+rect.y+"  :  "+rect.width+" x "+rect.height);
//	        AffineTransform transform = g2.getTransform();
//	        TreeLayout treeLayout = new RectilinearTreeLayout();
//	        treeLayout.setTree(tree);
//
//	        Node[] allNodes = tree.getNodes().toArray(new Node[0]);
//	        for(int i=allNodes.length-1; i >= 0; i--){
//	            if(rect.contains(transform.transform(treeLayout.getNodePoint(allNodes[i]),null))){
//	                nodes.add(allNodes[i]);
//	            }
//	        }
//
//	        return nodes;
//	    }
//		
		@Override
		public void mouseMoved(MouseEvent e){

			Point location = viewer.getMousePosition();
			double lineLength = viewer.getTreePane().getBounds().getHeight();

			Line2D.Double line = new Line2D.Double(location.getX(), 
					0, 
					location.getX(), 
					lineLength);
			
			viewer.addLine(line);
			viewer.repaint();
		}
		
	}

}
