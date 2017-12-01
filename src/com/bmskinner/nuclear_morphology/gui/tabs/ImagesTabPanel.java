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


package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

import ij.process.ImageProcessor;

/**
 * Show the outlines of all cells in each image analysed
 * 
 * @author bms41
 * @since 1.13.5
 *
 */
@SuppressWarnings("serial")
public class ImagesTabPanel extends DetailPanel {

    private JTree  tree;      // hold the image list
    private JPanel imagePanel;

    private JLabel label;

    private static final String IMAGES_LBL = "Images in dataset";
    private static final String PANEL_TITLE_LBL = "Images";

    private class ImageNode {
        private String name;
        private File   f;

        public ImageNode(String name, File f) {
            this.name = name;
            this.f = f;
        }

        public String getName() {
            return name;
        }

        public File getFile() {
            return f;
        }

        public String toString() {
            return name;
        }
    }
    
    /**
     * Create the panel. 
     */
    public ImagesTabPanel() {
        super();

        this.setLayout(new BorderLayout());

        createUI();
    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    private void createUI() {

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new ImageNode(IMAGES_LBL, null));
        TreeModel treeModel = new DefaultTreeModel(root);

        tree = new JTree(treeModel);
        tree.addTreeSelectionListener(makeListener());
        tree.setEnabled(false);

        imagePanel = new JPanel(new BorderLayout());
        label = new JLabel();
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setVerticalAlignment(JLabel.CENTER);
        label.setHorizontalTextPosition(JLabel.CENTER);
        label.setVerticalTextPosition(JLabel.CENTER);
        imagePanel.add(label, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(tree);
        Dimension size = new Dimension(200, 200);
        scrollPane.setMinimumSize(size);
        scrollPane.setPreferredSize(size);

        this.add(imagePanel, BorderLayout.CENTER);
        this.add(scrollPane, BorderLayout.WEST);

    }

    /**
     * Trigger an update with a given dataset.
     * 
     */
    @Override
    protected void updateSingle() {
        updateMultiple();
    }

    @Override
    protected void updateMultiple() {

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new ImageNode(IMAGES_LBL, null));

        for(IAnalysisDataset d : getDatasets()){
            createNodes(root, d);
        }
        
        tree.setEnabled(true);

        TreeModel model = new DefaultTreeModel(root);

        tree.setModel(model);
        
        for(int i=0; i<tree.getRowCount(); i++){
            tree.expandRow(i);
        }
        
        label.setText(null);
    }

    @Override
    protected void updateNull() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new ImageNode(IMAGES_LBL, null));

        tree.setEnabled(false);

        TreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
        label.setText(Labels.NULL_DATASETS);
        label.setIcon(null);
    }

    /**
     * Create the nodes in the tree
     * 
     * @param root
     *            the root node
     * @param dataset
     *            the dataset to use
     */
    private void createNodes(DefaultMutableTreeNode root, IAnalysisDataset dataset) {
        
        DefaultMutableTreeNode datasetRoot = new DefaultMutableTreeNode(new ImageNode(dataset.getName(), null));
        List<File> files = new ArrayList<File>(dataset.getCollection().getImageFiles());
        Collections.sort(files);

        for (File f : files) {

            String name = f.getName();
            datasetRoot.add(new DefaultMutableTreeNode(new ImageNode(name, f)));
        }
        
        root.add(datasetRoot);

    }
    
    /**
     * Given an end node, get the dataset this came from
     * @param node
     * @return
     */
    private Optional<IAnalysisDataset> getDataset(DefaultMutableTreeNode node){
    	
    	DefaultMutableTreeNode n = (DefaultMutableTreeNode) node.getPath()[1];

    	if(n.getUserObject() instanceof ImageNode){
    		ImageNode im = (ImageNode) n.getUserObject();
    		for(IAnalysisDataset d : getDatasets()){
    			if(im.getName().equals(d.getName())){
    				return Optional.of(d);
    			}
    		}

    	}

    	
    	return null;
    }

    private TreeSelectionListener makeListener() {

    	TreeSelectionListener l = (TreeSelectionEvent e) -> {
    		DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();

    		ImageNode data = (ImageNode) node.getUserObject();

    		if (data.getFile() == null) {
    			label.setIcon(null);
    			return;
    		}

    		Runnable r = () -> {
    			try {

    				ImageProcessor ip = new ImageImporter(data.getFile()).importToColorProcessor();

    				ImageConverter cn = new ImageConverter(ip);
    				if (cn.isByteProcessor()) {
    					cn.convertToColorProcessor();
    				}
    				ImageAnnotator an = cn.toAnnotator();

    				Optional<IAnalysisDataset> dataset = getDataset(node);
    				dataset.ifPresent( d -> d.getCollection().getCells(data.getFile()).stream().forEach( c-> an.annotateCellBorders(c)) );

    				ImageFilterer ic = new ImageFilterer(an.toProcessor());
    				ic.resize(imagePanel.getWidth(), imagePanel.getHeight());
    				label.setIcon(ic.toImageIcon());

    			} catch (Exception e1) {
    				warn("Error fetching image");
    				stack("Error fetching image", e1);
    			}
    		};

    		ThreadManager.getInstance().submit(r);

    	};
    	return l;
    }

}
