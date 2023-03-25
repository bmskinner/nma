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
package com.bmskinner.nma.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.InterfaceUpdater;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.events.FilePathUpdatedListener;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.utility.FileUtils;
import com.bmskinner.nma.visualisation.image.AbstractImageFilterer;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.image.ImageConverter;
import com.bmskinner.nma.visualisation.image.ImageFilterer;

import ij.process.ImageProcessor;

/**
 * Show the outlines of all cells in each image analysed
 * 
 * @author bms41
 * @since 1.13.5
 *
 */
@SuppressWarnings("serial")
public class ImagesTabPanel extends DetailPanel implements FilePathUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(ImagesTabPanel.class.getName());

	private JTree tree; // hold the image list
	private JPanel imagePanel;
	private JLabel label;

	private static final String IMAGES_LBL = "Images in dataset";
	private static final String PANEL_TITLE_LBL = "Images";
	private static final String HEADER_LBL = "Double click a folder to update image paths";

	/** Store the last folder opened when changing paths */
	private File lastSelectedFolder = null;

	/**
	 * Create the panel.
	 */
	public ImagesTabPanel() {
		super();

		this.setLayout(new BorderLayout());
		UIController.getInstance().addFilePathUpdatedListener(this);

		createUI();
	}

	@Override
	public String getPanelTitle() {
		return PANEL_TITLE_LBL;
	}

	private void createUI() {

		ImageTreeNode root = new ImageTreeNode(IMAGES_LBL);
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
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setVerticalAlignment(SwingConstants.CENTER);
		label.setHorizontalTextPosition(SwingConstants.CENTER);
		label.setVerticalTextPosition(SwingConstants.CENTER);
		imagePanel.add(label, BorderLayout.CENTER);
		JPanel headerPanel = new JPanel();
		headerPanel.add(new JLabel(HEADER_LBL));
		imagePanel.add(headerPanel, BorderLayout.NORTH);

		imagePanel.addMouseListener(new ImageMouseAdapter());

		JScrollPane scrollPane = new JScrollPane(tree);
		Dimension size = new Dimension(200, 200);
		scrollPane.setMinimumSize(size);
		scrollPane.setPreferredSize(size);

		JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		sp.setLeftComponent(scrollPane);
		sp.setRightComponent(imagePanel);

		this.add(sp, BorderLayout.CENTER);
	}

	private class ImageMouseAdapter extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {

			// Only allow when a single image is selected
			if (e.getButton() == MouseEvent.BUTTON3 && tree.getSelectionCount() == 1) {

				TreePath path = tree.getSelectionModel().getSelectionPath();
				ImageTreeNode node = (ImageTreeNode) path.getLastPathComponent();

				if (!node.isFile)
					return;

				File file = node.getFile();
				String fileName = file != null ? file.getName() + "_annotated"
						: "Annotated";

				JPopupMenu popup = new JPopupMenu();
				JMenuItem save = new JMenuItem("Save image...");
				save.addActionListener(a -> saveImage(node.dataset, fileName));
				popup.add(save);
				popup.show(imagePanel, e.getX(), e.getY());
			}
		}

		private void saveImage(IAnalysisDataset dataset, String fileName) {

			try {
				File f = new DefaultInputSupplier().requestFileSave(dataset.getSavePath(), fileName,
						"png");

				BufferedImage img = new BufferedImage(label.getWidth(), label.getHeight(),
						BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = img.createGraphics();
				label.printAll(g2d);

				try {
					ImageIO.write(img, "png", f);
				} catch (IOException e) {
					LOGGER.warning("Cannot save image: " + e.getMessage());
					LOGGER.log(Loggable.STACK, "Error saving annotated image", e);
				} finally {
					g2d.dispose();
				}

			} catch (RequestCancelledException e1) {
				// User cancelled
			}
		}

	}


	/**
	 * Trigger an update with a given dataset.
	 * 
	 */
	@Override
	protected synchronized void updateSingle() {
		updateMultiple();
	}

	@Override
	protected synchronized void updateMultiple() {
		ImageTreeNode root = new ImageTreeNode(IMAGES_LBL);

		for (IAnalysisDataset d : getDatasets()) {
			createNodes(root, d);
		}

		tree.setEnabled(true);

		TreeModel model = new DefaultTreeModel(root);

		tree.setModel(model);

		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}

		label.setText(null);
	}

	@Override
	protected synchronized void updateNull() {
		ImageTreeNode root = new ImageTreeNode(IMAGES_LBL);
		TreeModel model = new DefaultTreeModel(root);
		tree.setModel(model);
		tree.setEnabled(false);
		label.setText(Labels.NULL_DATASETS);
		label.setIcon(null);
	}

	/**
	 * Create the nodes in the tree
	 * 
	 * @param root   the root node
	 * @param datase the dataset to use
	 */
	private void createNodes(DefaultMutableTreeNode root, IAnalysisDataset dataset) {

		List<File> files = new ArrayList<>(dataset.getCollection().getImageFiles());

		// Each folder of images should be a node. Find the unique folders
		List<File> parents = files.stream().map(File::getParentFile).distinct().sorted().toList();

		ImageTreeNode datasetRoot = new ImageTreeNode(
				dataset.getName() + " (" + files.size() + ")");

		// We want the image names sorted 'sensibly', which is not the same as
		// alphabetically.
		// An image s2 should be before s10
		// The only pattern to recognise for now is eg. "s12.tiff"
		Pattern p = Pattern.compile("^.?(\\d+)\\.tiff?$");

		// Sort numerically where possible
		Comparator<File> comp = (f1, f2) -> {
			Matcher m1 = p.matcher(f1.getName());
			Matcher m2 = p.matcher(f2.getName());

			if (m1.matches() && m2.matches()) {

				String s1 = m1.group(1);
				String s2 = m2.group(1);

				try {

					int i1 = Integer.parseInt(s1);
					int i2 = Integer.parseInt(s2);
					return i1 - i2;
				} catch (NumberFormatException e) {
					LOGGER.log(Loggable.STACK, "Error parsing number", e);
					return f1.compareTo(f2);
				}
			}
			return f1.compareTo(f2);

		};

		Comparator<File> defaultComp = (f1, f2) -> f1.compareTo(f2);

		for (File parent : parents) {
			List<File> inParent = files.stream().filter(f -> f.getParentFile().equals(parent))
					.collect(Collectors.toList());

			try {
				inParent.sort(comp);
			} catch (IllegalArgumentException e) { // not the expected format
				inParent.sort(defaultComp);
			}
			ImageTreeNode parentNode = new ImageTreeNode(parent, dataset);

			for (File f : inParent)
				parentNode.add(new ImageTreeNode(f, dataset));

			datasetRoot.add(parentNode);
		}

		root.add(datasetRoot);

	}

	/**
	 * Given an leaf node, get the dataset this came from
	 * 
	 * @param node
	 * @return
	 */
	private Optional<IAnalysisDataset> getDataset(ImageTreeNode node) {
		for (IAnalysisDataset d : getDatasets()) {
			if (node.toString()
					.equals(d.getName() + " (" + d.getCollection().getImageFiles().size() + ")"))
				return Optional.of(d);
		}
		if (node.isRoot())
			return Optional.empty();
		return getDataset((ImageTreeNode) node.getParent());

	}

	private TreeSelectionListener makeListener() {

		return (TreeSelectionEvent e) -> {
			ImageTreeNode data = (ImageTreeNode) e.getPath().getLastPathComponent();

			File f = data.getFile();
			if (f == null || f.isDirectory()) {
				label.setIcon(null);
				return;
			}

			InterfaceUpdater r = () -> {
				try {

					// TODO - check space needed by cells
					ImageProcessor ip = f.exists() ? ImageImporter.importFileTo24bit(f)
							: AbstractImageFilterer.createWhiteColorProcessor(1500, 1500);

					// If an 8bit image was read in, make it colour greyscale
					ImageConverter cn = new ImageConverter(ip);
					if (cn.isByteProcessor())
						cn.convertToColorProcessor();
					ImageAnnotator an = cn.toAnnotator();

					Optional<IAnalysisDataset> dataset = getDataset(data);
					dataset.ifPresent(
							d -> d.getCollection().getCells(f).stream()
									.forEach(an::annotateCellBorders));

					ImageFilterer ic = new ImageFilterer(an.toProcessor());
					ic.resizeKeepingAspect(imagePanel.getWidth(), imagePanel.getHeight());
					label.setIcon(ic.toImageIcon());

				} catch (Exception e1) {
					label.setIcon(null);
					LOGGER.log(Level.SEVERE,
							"Error fetching image %s: %s".formatted(f.getAbsolutePath(), e1.getMessage()),
							e);
				}
			};

			ThreadManager.getInstance().submit(r);
		};
	}

	/**
	 * Make a listener to allow image folder updating
	 * 
	 * @return
	 */
	private MouseListener makeDoubleClickListener() {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

				if (e.getClickCount() != 2)
					return;

				int row = tree.getRowForLocation(e.getX(), e.getY());
				if (row == -1)
					return;

				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				ImageTreeNode node = (ImageTreeNode) selPath.getLastPathComponent();

				if (node == null)
					return;

				if (node.isLeaf())
					return; // folders only can be double clicked

				updateImageFolder(node);

				tree.repaint();
			}

		};
	}

	/**
	 * Update the folder for the given node
	 * 
	 * @param node the node to be updated
	 */
	private void updateImageFolder(ImageTreeNode node) {

		File oldFolder = node.getFile();

		// Don't update nodes that have no file
		if (oldFolder == null)
			return;

		try {

			// Shortcut file search by finding any extant element of the path
			oldFolder = FileUtils.extantComponent(oldFolder);

			// Store the last folder to be selected, to speed choosing other files
			File folderToRequest = lastSelectedFolder != null ? lastSelectedFolder : oldFolder;
			File newFolder = getInputSupplier().requestFolder(folderToRequest);
			lastSelectedFolder = newFolder;
			LOGGER.finer("Image tab last selected folder is now "
					+ lastSelectedFolder.getAbsolutePath());

			// Update the folder for the node and it's children
			node.setFile(newFolder); // update node

		} catch (RequestCancelledException e1) {
			// No action
		}
	}

	private class ImageTreeNode extends DefaultMutableTreeNode {

		private transient IAnalysisDataset dataset;
		private String name;
		boolean isFile = false;

		public ImageTreeNode(String s) {
			super();
			name = s;
			isFile = false;
		}

		public ImageTreeNode(@Nullable File f, IAnalysisDataset d) {
			super();
			setFile(f);
			dataset = d;
		}

		public boolean isFile() {
			return isFile;
		}

		/**
		 * Get the file in this node, if present. Otherwise return null.
		 * 
		 * @return
		 */
		public @Nullable File getFile() {
			if (isFile)
				return new File(name);
			return null;
		}

		public void setFile(File f) {
			isFile = true;
			name = f.getAbsolutePath();

			// Update each file within the node to the new folder
			Enumeration<ImageTreeNode> children = convertChildren();
			while (children.hasMoreElements()) {
				ImageTreeNode imageData = children.nextElement();
				File imageFile = imageData.getFile();
				if (imageFile == null)
					continue;

				// Replace the source folder for all nuclei in the current image
				getDatasets().stream().flatMap(d -> d.getCollection().getCells(imageFile).stream())
						.flatMap(c -> c.getNuclei().stream()).forEach(n -> {
							n.setSourceFolder(f);

							// Update signals in the same file
							n.getSignalCollection().getAllSignals().stream().forEach(s -> {
								if (s.getSourceFile().equals(imageFile))
									s.setSourceFolder(f);
							});
						});
				imageData.setFile(new File(f, imageFile.getName()));
			}

		}

		/**
		 * A conversion method to avoid casting the super.children() method directly
		 * (fails in Java 12).
		 * 
		 * @return the converted enumeration
		 */
		public Enumeration<ImageTreeNode> convertChildren() {
			List<ImageTreeNode> list = new ArrayList<>();
			Enumeration<TreeNode> nodes = children();
			while (nodes.hasMoreElements())
				list.add((ImageTreeNode) nodes.nextElement());
			return Collections.enumeration(list);
		}

		@Override
		public String toString() {
			File f = new File(name);
			if (f.isDirectory())
				return f.getAbsolutePath();
			return f.getName();
		}
	}

	/**
	 * Allow the string value of a node to be displayed as a tooltip
	 * 
	 * @author ben
	 * @since 1.13.8
	 *
	 */
	private static class ImageNodeRenderer extends DefaultTreeCellRenderer {

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
				boolean expanded,
				boolean leaf, int row, boolean hasFocus) {

			Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
					hasFocus);
			setToolTipText(value.toString());

			Color fg = Color.BLACK;
			ImageTreeNode n = (ImageTreeNode) value;
			if (n.isFile()) {
				File f = n.getFile();
				if (f == null || !f.exists())
					fg = Color.RED;
			}
			c.setForeground(fg);
			return c;
		}
	}

	@Override
	public void filePathUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void filePathUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}
}
