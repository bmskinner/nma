/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.virion.jam.controlpanels.BasicControlPalette;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ClusterGroup;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.ClusteringOptions;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.components.DraggableTreeViewer;
import com.bmskinner.nuclear_morphology.gui.components.VariableNodePainter;
import com.bmskinner.nuclear_morphology.gui.components.panels.ClusterGroupSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.DatasetSelectionPanel;

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
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class ClusterTreeDialog extends LoadingIconDialog {
    
    private static final String ANALYSE_LBL = "Analyse new clusters";
    private static final String SHOW_MGE_SRC_LBL = "Show merge sources";
    private static final String EXTRACT_LBL = "Extract selected as cluster";

    private JPanel              buttonPanel;
    private DraggableTreeViewer viewer;
    private IAnalysisDataset    dataset;
    private IClusterGroup       group;

    private DatasetSelectionPanel selectedClusterBox;
    private ClusterGroupSelectionPanel selectedClusterGroupBox;

    private List<ICellCollection> clusterList = new ArrayList<>(0);

    public ClusterTreeDialog(final IAnalysisDataset dataset, final IClusterGroup group) {
        super();
        this.dataset = dataset;
        this.group = group;

        try {

            this.setLayout(new BorderLayout());
            this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            this.viewer = new DraggableTreeViewer(
                    new BasicControlPalette(0, BasicControlPalette.DisplayMode.INITIALLY_CLOSED, true),
                    SwingConstants.LEFT);

            viewer.getTreePane().addMouseListener(new MouseClusterSelectionAdapter());

            this.add(viewer, BorderLayout.CENTER);

            this.buttonPanel = createButtonPanel();

            this.add(buttonPanel, BorderLayout.NORTH);

            RootedTree r = importTree();

            if (r == null) {
                warn("Unable to import tree");
                this.dispose();

            } else {
                displayTree(r);
                this.setModal(false);
                this.setMinimumSize(new Dimension(500, 500));
                this.pack();
                this.setLocationRelativeTo(null);

                this.setVisible(true);
            }

        } catch (Exception e) {

            warn("Error creating tree view");
            stack("Error creating tree view", e);
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
        fine("Reading tree");
        StringReader reader = new StringReader(group.getTree());

        boolean readUnquotedLabels = true;
        NewickImporter imp = new NewickImporter(reader, readUnquotedLabels);

        try {
            List<Tree> trees = imp.importTrees();
            topTree = (RootedTree) trees.get(0);

            // Add the cells to the external nodes as attributes
            // Also set the short names for the nodes
            for (Node n : topTree.getNodes()) {

                if (topTree.isExternal(n)) { // choose the taxon nodes

                    Taxon t = topTree.getTaxon(n);
                    ICell c = getCell(t).get();
                    t.setAttribute("Cell", c);
                    n.setAttribute("ShortName",
                            c.getNucleus().getSourceFolder().getName() + "/" + c.getNucleus().getNameAndNumber());
                }
            }
        } catch (IOException e) {
            warn("Unable to display tree: Error reading data");
            log(Level.FINE, "Error reading tree", e);
        } catch (DuplicateTaxaException e) {
            warn("Unable to display tree: duplicate taxon names");
            log(Level.FINE, "Duplicate taxon names", e);
        } catch (ImportException e) {
            warn("Unable to display tree: error importing newick tree");
            log(Level.FINE, "Error in tree IO", e);
        }
        return topTree;
    }

    /**
     * Set the display options for the given tree
     */
    private void displayTree(RootedTree tree) {

        int numTaxa = tree.getTaxa().size();
        fine("Tree has " + numTaxa + " taxa");

        viewer.setTree(tree);

        viewer.setSelectionMode(SelectionMode.CLADE);
        viewer.setTreeLayoutType(TreeLayoutType.RECTILINEAR);
        viewer.getTreePane().setBranchTransform(true, TransformedRootedTree.Transform.PROPORTIONAL);
        viewer.getTreePane().setBranchLineWeight(2f);

        colourTreeNodesByClusterGroup(group);

        this.setTitle(dataset.getName() + " : " + group.getName() + " : " + numTaxa + " taxa");
    }

    /**
     * Fetch the cell from the active dataset that matches the given taxon from
     * a tree. The match is based on the cell image path.
     * 
     * @param t
     * @return
     */
    private Optional<ICell> getCell(Taxon t) {

        // Check if the taxon name is a UUID, as the tree format is changing for
        // 1.13.2
        // 4ca18dcd-7f5c-4443-89bc-c705435c30f7

        boolean isUUID = false;
        UUID id = null;
        if (t.getName().length() == 36) {

            try {
                id = UUID.fromString(t.getName());
                isUUID = true;
            } catch (IllegalArgumentException e) {
                // 36 char String was not a UUID
            }
        }

        if (isUUID && id!=null)
            return Optional.of(dataset.getCollection().getCell(id));
		return dataset.getCollection().streamCells()
		    .filter(c->hasMatchingNucleusName(t.getName(), c))
		    .findFirst();
    }
    
    private boolean hasMatchingNucleusName(String name, ICell c){
        return c.getNuclei().stream().anyMatch(n->taxonNamesMatch(name, n));
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        JButton extractButton = new JButton(EXTRACT_LBL);
        extractButton.addActionListener(a -> {
            try {
                extractSelectedNodesToCluster();
            } catch (Exception e) {
                warn("Error extracting cells");
                stack("Error extracting cells", e);
            }
        });
        panel.add(extractButton);

        JButton analyseButton = new JButton(ANALYSE_LBL);
        analyseButton.addActionListener(a -> analyseClusters());
        panel.add(analyseButton);

        if (dataset.hasMergeSources()) {
            JButton mergeSourceButton = new JButton(SHOW_MGE_SRC_LBL);
            mergeSourceButton.addActionListener(a -> showMergeSources());
            panel.add(mergeSourceButton);
        }

        List<IAnalysisDataset> l = dataset.getAllChildDatasets();
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
        selectedClusterGroupBox.addActionListener(e->{
            selectedClusterBox.setSelectionNull();
            colourTreeNodesByClusterGroup(selectedClusterGroupBox.getSelectedItem());
        });
        panel.add(selectedClusterGroupBox);

        panel.add(this.getLoadingLabel());

        return panel;
    }

    private void updateNodePainter() {
        VariableNodePainter painter = new VariableNodePainter("Cluster", viewer.getTreePane().getTree(),
                PainterIntent.TIP);
        painter.setBorder(Color.BLACK, new BasicStroke(2f));
        viewer.getTreePane().setTaxonLabelPainter(painter);
    }

    /**
     * Update the taxon colours to match their cluster
     * 
     * @param cluster
     *            the dataset of nuclei in the cluster
     */
    private void colourTreeNodesByCluster(final ICellCollection cluster) {

        setStatusLoading();
        // Set everything to grey
        setNodeColour(dataset.getCollection(), Color.LIGHT_GRAY);

        if (cluster != null) {

            // Set the cluster colour
            setNodeColour(cluster, Color.BLACK);

        }
        updateNodePainter();
        setStatusLoaded();

    }

    private void colourTreeNodesByClusterGroup(final IClusterGroup group) {

        if (group != null) {
            finer("Colouring nodes by cluster group: " + group.getName());

            setStatusLoading();

            int clusterNumber = 0;
            for (UUID id : group.getUUIDs()) {

                // Find the appropriate dataset
                IAnalysisDataset cluster = null;

                
                if (dataset.hasChild(id)) {

                    cluster = dataset.getChildDataset(id);

                } else if (dataset.hasMergeSource(id)) {

                    cluster = dataset.getMergeSource(id);
                } else if(dataset.getAllMergeSourceIDs().contains(id)){
                	cluster = dataset.getMergeSource(id);
                } else {
                    // If the cluster was not found, stop
                    warn("Child dataset not found, cancelling");
                    return;
                }

                Paint colour = ColourSelecter.getColor(clusterNumber++);
                setNodeColour(cluster.getCollection(), colour);

                finer("Node colours assigned");

            }
//            updateNodePainter();

        } else { // no cluster group, colour everything black
            setNodeColour(dataset.getCollection(), Color.BLACK);
        }
        updateNodePainter();
        setStatusLoaded();
    }

    /**
     * Set the label colour for the given cells
     * 
     * @param cells
     * @param colour
     */
    private void setNodeColour(final ICellCollection collection, final Paint colour) {

        RootedTree tree = viewer.getTreePane().getTree();

        for (Node n : tree.getNodes()) {

            if (tree.isExternal(n)) { // choose the taxon nodes

                Taxon t = tree.getTaxon(n);

                ICell c = (ICell) t.getAttribute("Cell");

                collection.streamCells().filter(cell->cell.equals(c))
                    .forEach(cell->n.setAttribute("Color", colour));
                


//                for (ICell cell : cells) {
//                    if (cell.equals(c)) {
//                        n.setAttribute("Color", colour);
//                    }
//                }
            }
        }
    }

    /**
     * Check that a taxon name matches a nucleus name
     * 
     * @param name
     * @param nucleus
     * @return
     */
    private boolean taxonNamesMatch(String name, Nucleus nucleus) {

        /*
         * Testing name: P106.tiff-3 Testing J:\Protocols\Scripts and
         * macros\Testing_cluster_images\P100.tiff-P100.tiff-0 Testing
         * Testing_cluster_images-P100.tiff-0 Testing P100.tiff-0 Name not found
         */
        String nucleusName = nucleus.getSourceFile() + "-" + nucleus.getNameAndNumber();

        // the ideal is full file path
        if (name.equals(nucleusName)) // 'C:\bla\image.tiff-image.tiff-1'
            return true;

		nucleusName = nucleus.getSourceFolder().getAbsolutePath() + "-" + nucleus.getNameAndNumber();

		if (name.equals(nucleusName)) 
		    return true;
		
		// Can't get just names from merge sources
		if (dataset.hasMergeSources())
		    return false;

		// otherwise look for just the name from an old dataset
		nucleusName = nucleus.getNameAndNumber();
		if (name.equals(nucleusName))
		    return true;
		return false;
    }

    private String checkName(int offset) {

        int maxExisting = 0;
        Pattern pattern = Pattern.compile(dataset.getName() + "_ManualCluster_(\\d+)$");

        for (IAnalysisDataset d : dataset.getChildDatasets()) {

            Matcher matcher = pattern.matcher(d.getName());

            int digit = 0;

            while (matcher.find()) {

                digit = Integer.valueOf(matcher.group(1));

                if (digit > maxExisting) {
                    maxExisting = digit;
                }

            }

        }

        int clusterNumber = maxExisting + offset;

        String result = dataset.getName() + "_ManualCluster_" + clusterNumber;

        return result;
    }

    private void extractSelectedNodesToCluster() throws Exception {
        ICellCollection template = dataset.getCollection();

        String newName = template.getName() + "_ManualCluster_" + clusterList.size();
        newName = checkName(clusterList.size());
        ICellCollection clusterCollection = new VirtualCellCollection(dataset, newName);

        Tree tree = viewer.getTreePane().getTree();

        Set<Node> nodes = viewer.getTreePane().getSelectedNodes();
        for (Node n : nodes) {

            if (tree.isExternal(n)) {

                Taxon t = tree.getTaxon(n);

                ICell c = (ICell) t.getAttribute("Cell");
                clusterCollection.addCell(c);

            }

        }

        if (clusterCollection.hasCells()) {
            colourTreeNodesByCluster(clusterCollection);
            clusterList.add(clusterCollection);
            log("Extracted " + clusterCollection.size() + " cells");
        } else {
            warn("No cells found. Check taxon labels are correct");
        }
    }

    private void analyseClusters() {
        List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();

        for (ICellCollection c : clusterList) {
            if (c.hasCells()) {

                dataset.addChildCollection(c);

                try {
                    dataset.getCollection().getProfileManager().copyCollectionOffsets(c);
                } catch (ProfileException e) {
                    warn("Error copying collection offsets");
                    fine("Error in offsetting", e);
                }

                IAnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());

                list.add(clusterDataset);
            }
        }

        testClusterGroupable(list);

        if (!list.isEmpty()) {
            finest("Firing population update request");
            fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
        } else {
            warn("No datasets to analyse");
        }
        this.setVisible(false);
        this.dispose();
    }

    private void showMergeSources() {
        // Disable the selection dropdown boxes
        selectedClusterGroupBox.setSelectionNull();
        selectedClusterBox.setSelectionNull();


        List<IAnalysisDataset> list = new ArrayList<>(dataset.getAllMergeSources());

        IClusterGroup mergeGroup = makeNewClusterGroup(list);

        for (IAnalysisDataset d : list) {
            mergeGroup.addDataset(d);
        }
        
        colourTreeNodesByClusterGroup(mergeGroup);
    }

    /**
     * Create a new cluster group based on the clustering options in the
     * existing cluster group, and a new list of datasets
     * 
     * @param list
     *            the datasets to include in the cluster group
     * @return
     */
    private ClusterGroup makeNewClusterGroup(List<IAnalysisDataset> list) {
        ClusteringOptions newOptions = new ClusteringOptions(group.getOptions().get());
        newOptions.setClusterNumber(list.size());

        int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
        ClusterGroup newGroup = new ClusterGroup(IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, newOptions,
                group.getTree());
        return newGroup;
    }

    /**
     * Check that the list of datasets has only one copy of each cell
     * 
     * @param list
     * @return
     */
    private boolean checkCellPresentOnlyOnce(List<IAnalysisDataset> list) {
        boolean ok = true;
        Set<UUID> cellIDsFound = new HashSet<>();
                
        for (IAnalysisDataset d : list) {
            
            for (ICell c : d.getCollection().getCells()) {
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
    private boolean checkAllCellsPresent(List<IAnalysisDataset> clusters) {
        boolean ok = true;

        List<UUID> cellIDsFound = new ArrayList<UUID>();
        for (IAnalysisDataset d : clusters) {
            d.getCollection().getCells().forEach(c->cellIDsFound.add(c.getId()));
        }
        
        return dataset.getCollection().streamCells()
                .allMatch(c-> cellIDsFound.contains(c.getId()));
    }

    /*
     * Offer to put the datasets into a cluster group if conditions are met
     */
    private void testClusterGroupable(List<IAnalysisDataset> list) {

        if (!list.isEmpty()) {

            if (checkCellPresentOnlyOnce(list)) {

                if (checkAllCellsPresent(list)) {
                    // Offer to make a cluster group
                    int option = JOptionPane.showOptionDialog(null, "Join the new clusters into a cluster group?",
                            "Create cluster group", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                            null, null);

                    if (option == JOptionPane.CANCEL_OPTION) {
                        // Cancelled
                        log("Adding as standard manual clusters");
                    } else if (option == JOptionPane.OK_OPTION) {
                        // Make the group

                        ClusterGroup newGroup = makeNewClusterGroup(list);

                        int i = 0;
                        for (IAnalysisDataset d : list) {
                            d.setName(newGroup.getName() + "_Cluster_" + i);
                            newGroup.addDataset(d);
                            i++;
                        }
                        dataset.addClusterGroup(newGroup);

                    }
                } else {
                    log("Cannot make cluster group");
                    log("Not all cells are assigned clusters");
                    log("Adding as standard manual clusters");
                }
            } else {
                log("Cannot make cluster group");
                log("Cells present in more than one cluster");
                log("Adding as standard manual clusters");
            }

        }
    }

    protected class MouseClusterSelectionAdapter extends MouseAdapter {

        public MouseClusterSelectionAdapter() {

        }

        // public void mousePressed(MouseEvent e){
        //
        // TreePane pane = viewer.getTreePane();
        // Point location = pane.getMousePosition();
        // RootedTree tree = pane.getTree();
        // /*
        // * How to get the rectangle left of the mouse x-position?
        // * Then select all nodes to the left, and find the nodes with no
        // children
        // * selected. Use their direct children as the new clusters
        // */
        // IJ.log("Click heard");
        //
        //// The region left of the line
        // Rectangle r = new Rectangle( (int) location.getX(), (int)
        // pane.getBounds().getHeight());
        // viewer.setSelectionMode(SelectionMode.NODE);
        // pane.setDragRectangle(r);
        //
        // // Need to select the given nodes
        //
        // Set<Node> leftNodes = pane.getSelectedNodes();
        //
        //// Set<Node> leftNodes = viewer.getNodesAtPoint((Graphics2D)
        // pane.getGraphics(), r);
        //// Set<Node> leftNodes = getNodesAt((Graphics2D) pane.getGraphics(),
        // r);
        //
        // // The child free nodes
        // Set<Node> childLess = new HashSet<Node>();
        // for(Node n : leftNodes){
        // for(Node check : leftNodes){
        // if( ! tree.getChildren(n).contains(check)){
        // // no children in the left space
        // childLess.add(n);
        // }
        // }
        // }
        // IJ.log("Found "+childLess.size()+" child free nodes left of point");
        // viewer.setSelectionMode(SelectionMode.CLADE);
        // for(Node n : childLess){
        // List<Node> clusterNodes = tree.getChildren(n);
        //
        // for(Node clusterNode : clusterNodes){
        // pane.setSelectedNode(clusterNode);
        // extractSelectedNodesToCluster();
        //
        // }
        // }
        // IJ.log("Clusters made");
        //
        // List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
        //
        // for(CellCollection c : clusterList){
        // if(c.hasCells()){
        //
        // dataset.addChildCollection(c);
        //
        // AnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
        // clusterDataset.setRoot(false);
        // list.add(clusterDataset);
        // }
        // }
        //
        // ClusteringOptions newOptions = new
        // ClusteringOptions(ClusteringMethod.HIERARCHICAL);
        // newOptions.setClusterNumber(list.size());
        // newOptions.setHierarchicalMethod(group.getOptions().getHierarchicalMethod());
        // newOptions.setIncludeModality(group.getOptions().isIncludeModality());
        // newOptions.setModalityRegions(group.getOptions().getModalityRegions());
        // newOptions.setUseSimilarityMatrix(group.getOptions().isUseSimilarityMatrix());
        // int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
        // ClusterGroup newGroup = new
        // ClusterGroup("ClusterGroup_"+clusterNumber, newOptions,
        // group.getTree());
        //
        // for(AnalysisDataset d : list){
        // d.setName(newGroup.getName()+"_"+d.getName());
        // newGroup.addDataset(d);
        // }
        //
        // colourTreeNodesByClusterGroup(newGroup);
        //
        //
        // }

        // private Set<Node> getNodesAt(Graphics2D g2, Rectangle rect) {
        ////
        // Set<Node> nodes = new HashSet<Node>();
        ////
        // Tree tree = viewer.getTreePane().getTree();
        //
        // IJ.log("Selection rectangle: "+rect.x+", "+rect.y+" : "+rect.width+"
        // x "+rect.height);
        // AffineTransform transform = g2.getTransform();
        // TreeLayout treeLayout = new RectilinearTreeLayout();
        // treeLayout.setTree(tree);
        //
        // Node[] allNodes = tree.getNodes().toArray(new Node[0]);
        // for(int i=allNodes.length-1; i >= 0; i--){
        // if(rect.contains(transform.transform(treeLayout.getNodePoint(allNodes[i]),null))){
        // nodes.add(allNodes[i]);
        // }
        // }
        //
        // return nodes;
        // }
        //
        @Override
        public void mouseMoved(MouseEvent e) {

            Point location = viewer.getMousePosition();
            double lineLength = viewer.getTreePane().getBounds().getHeight();

            Line2D.Double line = new Line2D.Double(location.getX(), 0, location.getX(), lineLength);

            viewer.addLine(line);
            viewer.repaint();
        }

    }

}
