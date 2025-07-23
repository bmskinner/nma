/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.gui.dialogs;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.virion.jam.controlpanels.BasicControlPalette;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.DefaultClusterGroup;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.components.DraggableTreeViewer;
import com.bmskinner.nma.gui.components.VariableNodePainter;
import com.bmskinner.nma.gui.components.panels.ClusterGroupSelectionPanel;
import com.bmskinner.nma.gui.components.panels.DatasetSelectionPanel;
import com.bmskinner.nma.gui.events.UIController;

import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.ImportException.DuplicateTaxaException;
import jebl.evolution.io.NewickImporter;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.TransformedRootedTree;
import jebl.evolution.trees.Tree;
import jebl.gui.trees.treeviewer.TreePaneSelector.SelectionMode;
import jebl.gui.trees.treeviewer.TreeViewer.TreeLayoutType;
import jebl.gui.trees.treeviewer.painters.BasicLabelPainter.PainterIntent;

/**
 * Display hierarchical clustering trees and apply colours based on clusters.
 * 
 * @author Ben Skinner
 *
 */
@SuppressWarnings("serial")
public class ClusterTreeDialog extends MessagingDialog {

	private static final Logger LOGGER = Logger.getLogger(ClusterTreeDialog.class.getName());

	private static final String ANALYSE_LBL = "Analyse new clusters";
	private static final String SHOW_MGE_SRC_LBL = "Show merge sources";
	private static final String EXTRACT_LBL = "Extract selected as cluster";
	private static final String COPY_NEWICK_LBL = "Copy Newick";

	private JPanel buttonPanel;
	private DraggableTreeViewer viewer;
	private final IAnalysisDataset dataset;
	private final IClusterGroup group;

	private DatasetSelectionPanel selectedClusterBox;
	private ClusterGroupSelectionPanel selectedClusterGroupBox;

	private final List<ICellCollection> clusterList = new ArrayList<>(0);

	public ClusterTreeDialog(final IAnalysisDataset dataset, final IClusterGroup group) {
		super();
		this.dataset = dataset;
		this.group = group;

		try {

			this.setLayout(new BorderLayout());
			this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			this.viewer = new DraggableTreeViewer(
					new BasicControlPalette(0, BasicControlPalette.DisplayMode.INITIALLY_CLOSED,
							true),
					SwingConstants.LEFT);

			viewer.addMouseListener(new MouseClusterSelectionAdapter());

			this.add(viewer, BorderLayout.CENTER);

			this.buttonPanel = createButtonPanel();

			this.add(buttonPanel, BorderLayout.NORTH);

			final RootedTree r = importTree();

			if (r == null) {
				LOGGER.warning("Unable to import tree");
				this.dispose();

			} else {

//				final HierarchyVisualizer hv = new HierarchyVisualizer(group.getTree());
//				this.setContentPane(hv);
			displayTree(r);
				this.setModal(false);
				this.setMinimumSize(new Dimension(500, 500));
				this.pack();
				this.setLocationRelativeTo(null);

				this.setVisible(true);
			}


		} catch (final Exception e) {

			LOGGER.log(Level.WARNING, "Error creating tree view");
			LOGGER.log(Level.SEVERE, "Error creating tree view", e);
			this.dispose();
		}
	}

	/**
	 * Turn the Newick string in the cluster group into a tree
	 * 
	 * @return
	 */
	private RootedTree importTree() {

		RootedTree topTree = null;
		LOGGER.fine("Reading tree");
		final StringReader reader = new StringReader(group.getTree());

		final boolean readUnquotedLabels = true;
		final NewickImporter imp = new NewickImporter(reader, readUnquotedLabels);

		try {
			final List<Tree> trees = imp.importTrees();
			topTree = (RootedTree) trees.get(0);

			// Add the cells to the external nodes as attributes
			// Also set the short names for the nodes
			for (final Node n : topTree.getNodes()) {

				if (topTree.isExternal(n)) { // choose the taxon nodes

					final Taxon t = topTree.getTaxon(n);

					final ICell c = dataset.getCollection().getCell(UUID.fromString(t.getName()));
					t.setAttribute("Cell", c);
					n.setAttribute("ShortName",
							c.getPrimaryNucleus().getSourceFolder().getName() + "/"
									+ c.getPrimaryNucleus().getNameAndNumber());
				}
			}

			LOGGER.fine("Imported tree");
		} catch (final IOException e) {
			LOGGER.log(Level.SEVERE, "Error reading tree", e);
		} catch (final DuplicateTaxaException e) {
			LOGGER.log(Level.SEVERE, "Duplicate taxon names", e);
		} catch (final ImportException e) {
			LOGGER.log(Level.SEVERE, "Error in tree IO", e);
		}
		return topTree;
	}

	/**
	 * Set the display options for the given tree
	 */
	private void displayTree(RootedTree tree) {

		final int numTaxa = tree.getTaxa().size();
		LOGGER.fine("Tree has %s taxa".formatted(numTaxa));

		viewer.setTree(tree);
		viewer.setSelectionMode(SelectionMode.CLADE);
		viewer.setTreeLayoutType(TreeLayoutType.RECTILINEAR);
		viewer.getTreePane().setBranchTransform(true, TransformedRootedTree.Transform.PROPORTIONAL);
		viewer.getTreePane().setBranchLineWeight(2f);

		colourTreeNodesByClusterGroup(group);

		this.setTitle(dataset.getName() + " : " + group.getName() + " : " + numTaxa + " taxa");
	}

	/**
	 * Fetch the cell from the active dataset that matches the given taxon from a
	 * tree. The match is based on the cell image path.
	 * 
	 * @param t
	 * @return
	 */
	private Optional<ICell> getCell(Taxon t) {
		return Optional.ofNullable((ICell) t.getAttribute("Cell"));
	}

	private JPanel createButtonPanel() {
		final JPanel panel = new JPanel(new FlowLayout());

		final JButton extractButton = new JButton(EXTRACT_LBL);
		extractButton.addActionListener(a -> {
			try {
				extractSelectedNodesToCluster();
			} catch (final Exception e) {
				LOGGER.warning("Error extracting cells");
				LOGGER.log(Level.SEVERE, "Error extracting cells", e);
			}
		});
		panel.add(extractButton);

		final JButton analyseButton = new JButton(ANALYSE_LBL);
		analyseButton.addActionListener(a -> analyseClusters());
		panel.add(analyseButton);

		if (dataset.hasMergeSources()) {
			final JButton mergeSourceButton = new JButton(SHOW_MGE_SRC_LBL);
			mergeSourceButton.addActionListener(a -> showMergeSources());
			panel.add(mergeSourceButton);
		}

		final List<IAnalysisDataset> l = dataset.getAllChildDatasets();
		l.add(0, dataset);
		selectedClusterBox = new DatasetSelectionPanel(l);
		selectedClusterBox.setSelectionNull();
		selectedClusterBox.addActionListener(e -> {
			selectedClusterGroupBox.setSelectionNull();
			colourTreeNodesByCluster(selectedClusterBox.getSelectedDataset().getCollection());
		});
		panel.add(selectedClusterBox);

		selectedClusterGroupBox = new ClusterGroupSelectionPanel(dataset.getClusterGroups());
		selectedClusterGroupBox.setSelectedGroup(group);
		selectedClusterGroupBox.addActionListener(e -> {
			selectedClusterBox.setSelectionNull();
			colourTreeNodesByClusterGroup(selectedClusterGroupBox.getSelectedItem());
		});
		panel.add(selectedClusterGroupBox);

		final JButton copyNewickButton = new JButton(COPY_NEWICK_LBL);
		copyNewickButton.addActionListener(a -> {
			final StringSelection stringSelection = new StringSelection(group.getTree());
			final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
			LOGGER.info("Copied Newick tree for cluster " + group.getName());
		});
		panel.add(copyNewickButton);
		return panel;
	}

	private void updateNodePainter() {
		final VariableNodePainter painter = new VariableNodePainter("Cluster",
				viewer.getTreePane().getTree(),
				PainterIntent.TIP);
		painter.setBorder(Color.BLACK, new BasicStroke(2f));
		viewer.getTreePane().setTaxonLabelPainter(painter);
	}

	/**
	 * Update the taxon colours to match their cluster
	 * 
	 * @param cluster the dataset of nuclei in the cluster
	 */
	private void colourTreeNodesByCluster(final ICellCollection cluster) {
		// Set everything to grey
		setNodeColour(dataset.getCollection(), Color.LIGHT_GRAY);

		if (cluster != null) {
			// Set the cluster colour
			setNodeColour(cluster, Color.BLACK);
		}
		updateNodePainter();
	}

	private void colourTreeNodesByClusterGroup(final IClusterGroup group) {

		if (group != null) {
			LOGGER.fine("Colouring nodes by cluster group: " + group.getName());

			int clusterNumber = 0;
			for (final UUID id : group.getUUIDs()) {

				// Find the appropriate dataset
				IAnalysisDataset cluster = null;

				if (dataset.hasDirectChild(id)) {

					cluster = dataset.getChildDataset(id);

				} else if (dataset.hasMergeSource(id)) {

					cluster = dataset.getMergeSource(id);
				} else if (dataset.getAllMergeSourceIDs().contains(id)) {
					cluster = dataset.getMergeSource(id);
				} else {
					// If the cluster was not found, stop
					LOGGER.warning("Child dataset not found, cancelling");
					return;
				}

				final Paint colour = ColourSelecter.getColor(clusterNumber++);
				setNodeColour(cluster.getCollection(), colour);

				LOGGER.fine("Node colours assigned");

			}
//            updateNodePainter();

		} else { // no cluster group, colour everything black
			setNodeColour(dataset.getCollection(), Color.BLACK);
		}
		updateNodePainter();
	}

	/**
	 * Set the label colour for the given cells
	 * 
	 * @param collection
	 * @param colour
	 */
	private void setNodeColour(final ICellCollection collection, final Paint colour) {

		final RootedTree tree = viewer.getTreePane().getTree();

		for (final Node n : tree.getNodes()) {

			if (tree.isExternal(n)) { // choose the taxon nodes
				final Taxon t = tree.getTaxon(n);
				if (collection.contains(((ICell) t.getAttribute("Cell")).getId())) {
					n.setAttribute("Color", colour);
				}
			}
		}
	}

	private String checkName(int offset) {

		int maxExisting = 0;
		final Pattern pattern = Pattern.compile(dataset.getName() + "_ManualCluster_(\\d+)$");

		for (final IAnalysisDataset d : dataset.getChildDatasets()) {

			final Matcher matcher = pattern.matcher(d.getName());

			int digit = 0;

			while (matcher.find()) {

				digit = Integer.valueOf(matcher.group(1));

				if (digit > maxExisting) {
					maxExisting = digit;
				}
			}
		}

		final int clusterNumber = maxExisting + offset;

		final String result = dataset.getName() + "_ManualCluster_" + clusterNumber;

		return result;
	}

	private void extractSelectedNodesToCluster() throws Exception {
		final ICellCollection template = dataset.getCollection();

		String newName = template.getName() + "_ManualCluster_" + clusterList.size();
		newName = checkName(clusterList.size());
		final ICellCollection clusterCollection = new VirtualDataset(dataset, newName);

		final Tree tree = viewer.getTreePane().getTree();

		final Set<Node> nodes = viewer.getTreePane().getSelectedNodes();
		for (final Node n : nodes) {

			if (tree.isExternal(n)) {

				final Taxon t = tree.getTaxon(n);
				final ICell c = (ICell) t.getAttribute("Cell");
				clusterCollection.add(c);
			}
		}

		if (clusterCollection.hasCells()) {
			colourTreeNodesByCluster(clusterCollection);
			clusterList.add(clusterCollection);
			LOGGER.info("Extracted " + clusterCollection.size() + " cells");
		} else {
			LOGGER.warning("No cells found. Check taxon labels are correct");
		}
	}

	private void analyseClusters() {
		final List<IAnalysisDataset> list = new ArrayList<>();

		for (final ICellCollection c : clusterList) {
			if (c.hasCells()) {
				try {
					final IAnalysisDataset clusterDataset = dataset.addChildCollection(c);
					list.add(clusterDataset);

				} catch (MissingDataException | SegmentUpdateException e) {
					LOGGER.warning("Error copying collection offsets");
					LOGGER.log(Level.SEVERE, "Error in offsetting", e);
				}

			}
		}

		testClusterGroupable(list);

		if (!list.isEmpty()) {
			LOGGER.finest("Firing population update request");
			UIController.getInstance().fireDatasetAdded(list);
		} else {
			LOGGER.info("No datasets to analyse");
		}
		this.setVisible(false);
		this.dispose();
	}

	private void showMergeSources() {
		// Disable the selection dropdown boxes
		selectedClusterGroupBox.setSelectionNull();
		selectedClusterBox.setSelectionNull();

		final List<IAnalysisDataset> list = new ArrayList<>(dataset.getAllMergeSources());

		final IClusterGroup mergeGroup = makeNewClusterGroup(list);

		for (final IAnalysisDataset d : list) {
			mergeGroup.addDataset(d);
		}

		colourTreeNodesByClusterGroup(mergeGroup);
	}

	/**
	 * Create a new cluster group based on the clustering options in the existing
	 * cluster group, and a new list of datasets
	 * 
	 * @param list the datasets to include in the cluster group
	 * @return
	 */
	private IClusterGroup makeNewClusterGroup(List<IAnalysisDataset> list) {
		final HashOptions newOptions = group.getOptions().get().duplicate();
		newOptions.setInt(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY, list.size());

		final int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
		final IClusterGroup newGroup = new DefaultClusterGroup(
				IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber,
				newOptions, group.getTree(), UUID.randomUUID());
		return newGroup;
	}

	/**
	 * Check that the list of datasets has only one copy of each cell
	 * 
	 * @param list
	 * @return
	 */
	private boolean cellsPresentOnlyOnce(List<IAnalysisDataset> list) {
		boolean ok = true;
		final Set<UUID> cellIDsFound = new HashSet<>();

		for (final IAnalysisDataset d : list) {

			for (final ICell c : d.getCollection().getCells()) {
				ok &= !cellIDsFound.contains(c.getId());
				cellIDsFound.add(c.getId());
			}
		}

		return ok;
	}

	/**
	 * Check that a cell is not present in more than one cluster
	 * 
	 * @param list
	 * @return true if all cells in the list are present in a cluster
	 */
	private boolean cellsAllPresent(List<IAnalysisDataset> clusters) {
		final boolean ok = true;

		final List<UUID> cellIDsFound = new ArrayList<UUID>();
		for (final IAnalysisDataset d : clusters) {
			d.getCollection().getCells().forEach(c -> cellIDsFound.add(c.getId()));
		}

		return dataset.getCollection().streamCells()
				.allMatch(c -> cellIDsFound.contains(c.getId()));
	}

	/*
	 * Offer to put the datasets into a cluster group if conditions are met
	 */
	private void testClusterGroupable(List<IAnalysisDataset> list) {

		if (!list.isEmpty()) {

			if (!cellsPresentOnlyOnce(list)) {
				LOGGER.info("Cannot make cluster group");
				LOGGER.info("Cells present in more than one cluster");
				LOGGER.info("Adding as standard manual clusters");
				return;
			}

			if (!cellsAllPresent(list)) {
				LOGGER.info("Cannot make cluster group");
				LOGGER.info("Not all cells are assigned clusters");
				LOGGER.info("Adding as standard manual clusters");
				return;
			}

			// Offer to make a cluster group
			try {
				final boolean join = new DefaultInputSupplier().requestApproval(
						"Join the new clusters into a cluster group?",
						"Create cluster group");

				if (join) {
					LOGGER.fine("Creating cluster group");
					// Make the group

					final IClusterGroup newGroup = makeNewClusterGroup(list);

					int i = 0;
					for (final IAnalysisDataset d : list) {
						d.setName(newGroup.getName() + "_Cluster_" + i);
						newGroup.addDataset(d);
						i++;
					}
					dataset.addClusterGroup(newGroup);

				} else {
					LOGGER.info("Adding as standard manual clusters");
				}
			} catch (final RequestCancelledException e) {
				LOGGER.info("Adding as standard manual clusters");
			}
		}
	}

	/**
	 * Select cluster nodes based on horizontal position
	 * 
	 * @author Ben Skinner
	 *
	 */
	protected class MouseClusterSelectionAdapter extends MouseAdapter {

		public MouseClusterSelectionAdapter() {
		}

		@Override
		public void mouseMoved(MouseEvent e) {

			final Point location = viewer.getMousePosition();
			final double lineLength = viewer.getTreePane().getBounds().getHeight();
			LOGGER.fine("Mouse at " + location + " length " + lineLength);

			final Line2D.Double line = new Line2D.Double(location.getX(), 0, location.getX(), lineLength);

			viewer.addLine(line);
			viewer.repaint();
		}

	}

}
