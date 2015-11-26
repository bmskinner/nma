package gui.components;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JDialog;

import analysis.AnalysisDataset;
import gui.tabs.DetailPanel;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NewickImporter;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.Tree;
import jebl.gui.trees.treeviewer.TreeViewer;

@SuppressWarnings("serial")
public class ClusterTreePanel extends JDialog {

	private TreeViewer viewer;

	public ClusterTreePanel(Logger programLogger, String newickTree, String datasetName) {
		
		// TODO Auto-generated constructor stub
		this.setLayout(new BorderLayout());
		this.setTitle(datasetName);
		this.setSize(400, 300);

		this.viewer = new TreeViewer();
		this.add(viewer, BorderLayout.CENTER);

		programLogger.log(Level.FINE, "Reading tree");
		StringReader reader = new StringReader(newickTree);

		boolean readUnquotedLabels = true;
		NewickImporter imp = new NewickImporter(reader, readUnquotedLabels);

		try {
			List<Tree> trees =  imp.importTrees();
			RootedTree topTree = (RootedTree) trees.get(0);

			programLogger.log(Level.FINEST, topTree.toString());

			viewer.setTree( topTree );


		} catch (IOException e) {
			programLogger.log(Level.SEVERE, "Error reading tree", e);
		} catch (ImportException e) {
			programLogger.log(Level.SEVERE, "Error in tree IO", e);
		}
		
		this.setModal(false);
		this.setVisible(true);
		

	}

	
}
