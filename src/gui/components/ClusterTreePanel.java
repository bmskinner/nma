package gui.components;

import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JDialog;

import components.ClusterGroup;
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
public class ClusterTreePanel extends JDialog {

	private TreeViewer viewer;
	private AnalysisDataset dataset;
	private ClusterGroup group;
	

	public ClusterTreePanel(Logger programLogger, AnalysisDataset dataset, ClusterGroup group) {
		
		this.dataset = dataset;
		this.group = group;
		this.setLayout(new BorderLayout());
		this.setSize(600, 800);

		this.viewer = new TreeViewer();
		this.add(viewer, BorderLayout.CENTER);

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
			
			colourTreeNodesByCluster();
			
		} catch (IOException e) {
			programLogger.log(Level.SEVERE, "Error reading tree", e);
		} catch (ImportException e) {
			programLogger.log(Level.SEVERE, "Error in tree IO", e);
		}
				
		this.setModal(false);
		this.setVisible(true);
	}
	
	/**
	 * Update the taxon colours to match their cluster
	 */
	private void colourTreeNodesByCluster(){

		
		Map<Object,Paint> paintMap = new HashMap<Object, Paint>();
		paintMap.put("0", Color.RED);
		
		for(Taxon t : viewer.getTreePane().getTree().getTaxa()){
			
			viewer.getTreePane().setSelectedTaxon(t);
			viewer.getTreePane().annotateSelectedTaxa("Cluster", "0");
			
		}
		
		BranchDecorator b = new AttributeBranchDecorator("Cluster", paintMap);
//		Decorator decorator = new DiscreteColorDecorator("Cluster 0", viewer.getTreePane().getTree().getTaxa(), new Color[] {Color.RED});
		
		
		viewer.setBranchDecorator(b);
	}
	
}
