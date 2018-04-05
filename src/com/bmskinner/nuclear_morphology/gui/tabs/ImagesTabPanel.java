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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageConverter;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
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
    private static final String HEADER_LBL = "Double click a folder to update image paths";

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
        tree.addMouseListener(makeDoubleClickListener());
        tree.setToggleClickCount(0); // disable double clicking to expand nodes
        tree.setEnabled(false);
        tree.setCellRenderer(new ImageNodeRenderer());
        ToolTipManager.sharedInstance().registerComponent(tree);

        imagePanel = new JPanel(new BorderLayout());
        label = new JLabel();
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setVerticalAlignment(JLabel.CENTER);
        label.setHorizontalTextPosition(JLabel.CENTER);
        label.setVerticalTextPosition(JLabel.CENTER);
        imagePanel.add(label, BorderLayout.CENTER);
        JPanel headerPanel = new JPanel();
        headerPanel.add(new JLabel(HEADER_LBL));
        imagePanel.add(headerPanel, BorderLayout.NORTH);
        

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
     * @param root the root node
     * @param datase the dataset to use
     */
    private void createNodes(DefaultMutableTreeNode root, IAnalysisDataset dataset) {
        
    	List<File> files = new ArrayList<>(dataset.getCollection().getImageFiles());
    	
    	// Each folder of images should be a node. Find the unique folders
    	List<File> parents = files.stream()
    			.map(f->f.getParentFile())
    			.distinct()
    			.collect(Collectors.toList());
    	
    	ImageNode r = new ImageNode(dataset.getName()+" ("+files.size()+")", null);
        DefaultMutableTreeNode datasetRoot = new DefaultMutableTreeNode(r);
                
        // The only pattern to recognise for now is eg. "s12.tiff"
        Pattern p = Pattern.compile("^.?(\\d+)\\.tiff?$");
        
        // Sort numerically where possible
        Comparator<File> comp = (f1, f2) -> {
            Matcher m1 = p.matcher(f1.getName());
            Matcher m2 = p.matcher(f2.getName());

            if(m1.matches() && m2.matches()){
                
                String s1 = m1.group(1);
                String s2 = m2.group(1);

                try {
                    
                    int i1 = Integer.parseInt(s1);
                    int i2 = Integer.parseInt(s2);
                    return i1 - i2;
                } catch(NumberFormatException e) {
                    stack("Error parsing number", e);
                    return f1.compareTo(f2);
                }
                
            } else {
                return f1.compareTo(f2);
            }
        };
        
        Comparator<File> defaultComp = (f1, f2) -> {
        	return f1.compareTo(f2);
        };
        
        
        for (File parent : parents) {
        	List<File> inParent = files.stream().filter(f->f.getParentFile().equals(parent))
        	.collect(Collectors.toList());
        	
        	try{
        		inParent.sort(comp);
        	} catch(IllegalArgumentException e){
        		inParent.sort(defaultComp);
        	}
        	DefaultMutableTreeNode parentNode = new DefaultMutableTreeNode(new ImageNode(parent.getAbsolutePath(), parent));
        	
        	for (File f : inParent) {

                String name = f.getName();
                parentNode.add(new DefaultMutableTreeNode(new ImageNode(name, f)));
            }
            datasetRoot.add(parentNode);
        }
        
        
        root.add(datasetRoot);

    }
    
    /**
     * Given an leaf node, get the dataset this came from
     * @param node
     * @return
     */
    private Optional<IAnalysisDataset> getDataset(DefaultMutableTreeNode node){
    	
    	DefaultMutableTreeNode n = (DefaultMutableTreeNode) node.getPath()[1];

    	if(n.getUserObject() instanceof ImageNode){
    		ImageNode im = (ImageNode) n.getUserObject();
    		for(IAnalysisDataset d : getDatasets()){
    			if(im.getName().equals(d.getName()+" ("+d.getCollection().getImageFiles().size()+")")){
    				return Optional.of(d);
    			}
    		}

    	}
    	return Optional.empty();
    }

    private TreeSelectionListener makeListener() {

    	TreeSelectionListener l = (TreeSelectionEvent e) -> {
    		DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();

    		ImageNode data = (ImageNode) node.getUserObject();
    		

    		if (data.getFile() == null || data.getFile().isDirectory()) {
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

    /**
     * Make a listener to allow image folder updating
     * @return
     */
    private MouseListener makeDoubleClickListener(){
    	MouseListener l = new MouseAdapter(){
    		@Override
    		public void mouseClicked(MouseEvent e) {

    			if(e.getClickCount()!=2){
    				return;
    			}
    			int row = tree.getRowForLocation(e.getX(), e.getY());
    	        if(row==-1){
    	        	return;
    	        }

    	        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
    	        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
    	        if(node==null){
    	        	return;
    	        }

    	        ImageNode data = (ImageNode) node.getUserObject();

    	        if(data==null){
    	        	return;
    	        }
    	        
    	        File oldFolder = data.getFile();
    	        
    	        if(oldFolder==null || oldFolder.getName().endsWith(".tiff"))
    	        	return;

    	        File newFolder = FileSelector.chooseFolder(oldFolder);
    	        if(newFolder==null || !newFolder.exists())
    	        	return;

    	        Enumeration<DefaultMutableTreeNode> children = node.children();
    	        while(children.hasMoreElements()){
    	        	DefaultMutableTreeNode imageNode = children.nextElement(); 	        
    	        	ImageNode imageData = (ImageNode) imageNode.getUserObject();
    	        	File imageFile = imageData.getFile();
    	        	for(IAnalysisDataset d : getDatasets()){
    	        		Set<ICell> cells = d.getCollection().getCells(imageFile);
    	        		cells.parallelStream().forEach(c->{
    	        			c.getNuclei().stream().forEach(n->{
    	        				n.setSourceFolder(newFolder);
    	        			});
    	        		});
    	        	}
    	        }
    	        
    	        getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
    	        
    			
    		}

    	};
    	return l;
    }
    
    /**
     * Allow the string value of a node to be displayed as a tooltip
     * @author ben
     * @since 1.13.8
     *
     */
    private static class ImageNodeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            setToolTipText(value.toString());
            return super.getTreeCellRendererComponent(tree, value, sel,
                    expanded, leaf, row, hasFocus);
        }
    }
}
