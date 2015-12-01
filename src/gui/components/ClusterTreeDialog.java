package gui.components;

import gui.DatasetEvent;
import gui.DatasetEventListener;
import gui.MainWindow;
import gui.DatasetEvent.DatasetMethod;
import gui.actions.MorphologyAnalysisAction;
import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
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
import jebl.evolution.io.NewickImporter;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.Tree;
import jebl.gui.trees.treeviewer.TreeViewer;
import jebl.gui.trees.treeviewer.decorators.AttributeBranchDecorator;
import jebl.gui.trees.treeviewer.decorators.BranchDecorator;
import jebl.gui.trees.treeviewer.painters.BasicLabelPainter;
import jebl.gui.trees.treeviewer.painters.BasicLabelPainter.PainterIntent;
import jebl.gui.trees.treeviewer.painters.Painter;
import jebl.gui.trees.treeviewer_dev.decorators.Decorator;
import jebl.gui.trees.treeviewer_dev.decorators.DiscreteColorDecorator;

@SuppressWarnings("serial")
public class ClusterTreeDialog extends JDialog implements ActionListener, ItemListener {

	private JPanel buttonPanel;
	private TreeViewer viewer;
	private AnalysisDataset dataset;
	private ClusterGroup group;
	private Logger programLogger;
	
	private JComboBox<AnalysisDataset> selectedClusterBox;
	private JComboBox<ClusterGroup> selectedClusterGroupBox;
	
	private List<Object> datasetListeners = new ArrayList<Object>();
	
	private List<CellCollection> clusterList = new ArrayList<CellCollection>(0);
	

	public ClusterTreeDialog(Logger programLogger, AnalysisDataset dataset, ClusterGroup group) {
		
		this.dataset = dataset;
		this.group = group;
		this.programLogger = programLogger;
		this.setLayout(new BorderLayout());

		this.viewer = new TreeViewer();
		this.add(viewer, BorderLayout.CENTER);
		
		this.buttonPanel = createButtonPanel();
		this.add(buttonPanel, BorderLayout.NORTH);

		programLogger.log(Level.FINE, "Reading tree");
		StringReader reader = new StringReader(group.getTree());

		boolean readUnquotedLabels = true;
		NewickImporter imp = new NewickImporter(reader, readUnquotedLabels);

		try {
			List<Tree> trees =  imp.importTrees();
			RootedTree topTree = (RootedTree) trees.get(0);
			
			int numTaxa = topTree.getTaxa().size(); 
			
			viewer.setTree( topTree );
			this.setTitle(dataset.getName() + " : " + group.getName() +" : "+numTaxa+ " taxa");
			colourTreeNodesByClusterGroup(group);
			
		} catch (IOException e) {
			programLogger.log(Level.SEVERE, "Error reading tree", e);
		} catch (ImportException e) {
			programLogger.log(Level.SEVERE, "Error in tree IO", e);
		}
				
		this.setModal(false);
		this.setMinimumSize(new Dimension(500, 500));
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
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
		
		selectedClusterBox = new JComboBox<AnalysisDataset>();
		selectedClusterBox.addItem(dataset);
		for(AnalysisDataset d: dataset.getChildDatasets()){
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
		
		return panel;
	}
	
	/**
	 * Update the taxon colours to match their cluster
	 * @param cluster the dataset of nuclei in the cluster
	 */
	private void colourTreeNodesByCluster(CellCollection cluster){

		if(cluster!=null){
			
			Map<Node, Color> clusterMemberships = new HashMap<Node, Color>();
			RootedTree tree = viewer.getTreePane().getTree();

			for(Node n : tree.getNodes()){

				if(tree.isExternal(n)){

					Taxon t = tree.getTaxon(n);

					String name = t.getName();

					for(Nucleus nucleus : cluster.getNuclei()){
						if(nucleus.getNameAndNumber().equals(name)){
							clusterMemberships.put(n, Color.BLACK);
						}
							
					}
				}

			}
			

			VariableNodePainter painter = new VariableNodePainter("cluster", tree, PainterIntent.TIP, clusterMemberships);
			viewer.getTreePane().setTaxonLabelPainter(painter);
		}
	}
	
	private void colourTreeNodesByClusterGroup(ClusterGroup group){

		if(group!=null){
			Map<Node, Color> clusterMemberships = new HashMap<Node, Color>();
			RootedTree tree = viewer.getTreePane().getTree();
			int clusterNumber = 0;
			for(UUID id : group.getUUIDs()){
				AnalysisDataset cluster = dataset.getChildDataset(id);
				Color colour = ColourSelecter.getSegmentColor(clusterNumber++);

				for(Node n : tree.getNodes()){

					if(tree.isExternal(n)){

						Taxon t = tree.getTaxon(n);

						String name = t.getName();

						for(Nucleus nucleus : cluster.getCollection().getNuclei()){
							if(nucleus.getNameAndNumber().equals(name)){
								clusterMemberships.put(n, colour);
							}
						}
					}

				}

			}
			VariableNodePainter painter = new VariableNodePainter("Cluster", tree, PainterIntent.TIP, clusterMemberships);
			viewer.getTreePane().setTaxonLabelPainter(painter);
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

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if(e.getActionCommand().equals("Extract")){
			
			CellCollection template = dataset.getCollection();
			
			CellCollection clusterCollection = new CellCollection(template.getFolder(), 
					template.getOutputFolderName(), 
					template.getName()+"_ManualCluster_"+clusterList.size(), 
					template.getDebugFile(), 
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
						if(c.getNucleus().getNameAndNumber().equals(name)){
							clusterCollection.addCell(new Cell (c));
						}
					}
				}

			}
			
			if(clusterCollection.hasCells()){
				colourTreeNodesByCluster(clusterCollection);
				clusterList.add(clusterCollection);
				programLogger.log(Level.INFO, "Extracted "+clusterCollection.size()+" cells");
			} else {
				programLogger.log(Level.WARNING, "No cells found. Check taxon labels are correct");
			}
			

		}
		
		if(e.getActionCommand().equals("Analyse")){
			
			List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
			
			for(CellCollection c : clusterList){
				if(c.hasCells()){

					dataset.addChildCollection(c);

					AnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
					clusterDataset.setRoot(false);
					list.add(clusterDataset);
				}
			}
			
			testClusterGroupable(list);

			
			if(!list.isEmpty()){
				fireDatasetEvent(DatasetMethod.COPY_MORPHOLOGY, list, dataset);
				programLogger.log(Level.FINEST, "Fired dataset copy event to listeners");
			} else {
				programLogger.log(Level.WARNING, "No datasets to analyse");
			}
			this.setVisible(false);
			this.dispose();
		}

	}
	
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

						int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
						programLogger.log(Level.INFO, "Creating cluster group "+clusterNumber);

						ClusteringOptions newOptions = new ClusteringOptions(ClusteringMethod.HIERARCHICAL);
						newOptions.setClusterNumber(list.size());
						newOptions.setHierarchicalMethod(group.getOptions().getHierarchicalMethod());
						newOptions.setIncludeModality(group.getOptions().isIncludeModality());
						newOptions.setModalityRegions(group.getOptions().getModalityRegions());
						newOptions.setUseSimilarityMatrix(group.getOptions().isUseSimilarityMatrix());
						ClusterGroup newGroup = new ClusterGroup("ClusterGroup_"+clusterNumber, newOptions, group.getTree());

						for(AnalysisDataset d : list){
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

	public synchronized void addDatasetEventListener( DatasetEventListener l ) {
		datasetListeners.add( l );
	}

	public synchronized void removeDatasetEventListener( DatasetEventListener l ) {
		datasetListeners.remove( l );
	}

	@Override
	public void itemStateChanged(ItemEvent e) {


		if(e.getSource().equals(selectedClusterBox)){
			selectedClusterGroupBox.removeItemListener(this);
			selectedClusterGroupBox.setSelectedIndex(-1);
			selectedClusterGroupBox.addItemListener(this);
			AnalysisDataset selected = (AnalysisDataset) selectedClusterBox.getSelectedItem();
			colourTreeNodesByCluster(selected.getCollection());
			
		}
		
		if(e.getSource().equals(selectedClusterGroupBox)){
			selectedClusterBox.removeItemListener(this);
			selectedClusterBox.setSelectedIndex(-1);
			selectedClusterBox.addItemListener(this);
			colourTreeNodesByClusterGroup((ClusterGroup) selectedClusterGroupBox.getSelectedItem());
			
		}
		
	}

}
