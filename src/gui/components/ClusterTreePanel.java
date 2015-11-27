package gui.components;

import gui.DatasetEvent;
import gui.DatasetEventListener;
import gui.MainWindow;
import gui.DatasetEvent.DatasetMethod;
import gui.actions.MorphologyAnalysisAction;
import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import components.Cell;
import components.CellCollection;
import components.ClusterGroup;
import components.nuclei.Nucleus;
import analysis.AnalysisDataset;
import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NewickImporter;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.Tree;
import jebl.gui.trees.treeviewer.TreeViewer;
import jebl.gui.trees.treeviewer.decorators.AttributeBranchDecorator;
import jebl.gui.trees.treeviewer.decorators.BranchDecorator;
import jebl.gui.trees.treeviewer_dev.decorators.Decorator;
import jebl.gui.trees.treeviewer_dev.decorators.DiscreteColorDecorator;

@SuppressWarnings("serial")
public class ClusterTreePanel extends JDialog implements ActionListener {

	private JPanel buttonPanel;
	private TreeViewer viewer;
	private AnalysisDataset dataset;
	private ClusterGroup group;
	private Logger programLogger;
	
	private List<Object> datasetListeners = new ArrayList<Object>();
	
	private List<CellCollection> clusterList = new ArrayList<CellCollection>(0);
	

	public ClusterTreePanel(Logger programLogger, AnalysisDataset dataset, ClusterGroup group) {
		
		this.dataset = dataset;
		this.group = group;
		this.programLogger = programLogger;
		this.setLayout(new BorderLayout());
		this.setSize(600, 800);

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
			
//			colourTreeNodesByCluster();
			
		} catch (IOException e) {
			programLogger.log(Level.SEVERE, "Error reading tree", e);
		} catch (ImportException e) {
			programLogger.log(Level.SEVERE, "Error in tree IO", e);
		}
				
		this.setModal(false);
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
		
		return panel;
	}
	
	/**
	 * Update the taxon colours to match their cluster
	 */
//	private void colourTreeNodesByCluster(){
//
//		
//		Map<Object,Paint> paintMap = new HashMap<Object, Paint>();
//		paintMap.put("0", Color.RED);
//		
//		for(Taxon t : viewer.getTreePane().getTree().getTaxa()){
//			
//			viewer.getTreePane().setSelectedTaxon(t);
//			viewer.getTreePane().annotateSelectedTaxa("Cluster", "0");
//			
//		}
//		
//		BranchDecorator b = new AttributeBranchDecorator("Cluster", paintMap);
////		Decorator decorator = new DiscreteColorDecorator("Cluster 0", viewer.getTreePane().getTree().getTaxa(), new Color[] {Color.RED});
//		
//		
//		viewer.setBranchDecorator(b);
//	}
	
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
			
			if(!list.isEmpty()){
				fireDatasetEvent(DatasetMethod.COPY_MORPHOLOGY, list, dataset);
			} else {
				programLogger.log(Level.WARNING, "No datasets to analyse");
			}
			this.setVisible(false);
			this.dispose();
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

}
