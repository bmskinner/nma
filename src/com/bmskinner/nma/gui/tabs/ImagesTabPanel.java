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
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.events.FilePathUpdatedListener;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.utility.FileUtils;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.image.ImageConverter;
import com.bmskinner.nma.visualisation.image.ImageFilterer;

import ij.process.ImageProcessor;

/**
 * Show the outlines of all cells in each image analysed
 * 
 * @author Ben Skinner
 * @since 1.13.5
 *
 */
@SuppressWarnings("serial")
public class ImagesTabPanel extends DetailPanel implements FilePathUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(ImagesTabPanel.class.getName());

	private JTree tree; // hold the image list
	private JPanel imagePanel;

	/** Label to hold the image icon displaying the selected image */
	private JLabel label;

	/** Hold the full path for the selected image */
	private final JLabel openImagePathLabel = new JLabel("");
	private JPanel contentPanel;

	private static final String IMAGES_LBL = "Images in dataset";
	private static final String PANEL_TITLE_LBL = "Images";
	private static final String PANEL_DESC_LBL = "Show image files annotated with detected cells";

	private static final String HEADER_LBL = "Double click a folder to update image paths";

	/** Store the last folder opened when changing paths */
	private File lastSelectedFolder = null;

	/**
	 * Create the panel.
	 */
	public ImagesTabPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);

		this.setLayout(new BorderLayout());
		UIController.getInstance().addFilePathUpdatedListener(this);

		createUI();
	}

	private void createUI() {

		final ImageTreeNode root = new ImageTreeNode(IMAGES_LBL);
		final TreeModel treeModel = new DefaultTreeModel(root);

		tree = new JTree(treeModel);
		tree.addTreeSelectionListener(new ImageTreeSelectionListener());
		tree.addMouseListener(new ImageTreeMouseListener());
		tree.setToggleClickCount(0); // disable double clicking to expand nodes
		tree.setEnabled(false);
		tree.setCellRenderer(new ImageNodeRenderer());
		ToolTipManager.sharedInstance().registerComponent(tree);

		contentPanel = new JPanel(new BorderLayout());
		imagePanel = new JPanel(new BorderLayout());

		label = new JLabel();
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setVerticalAlignment(SwingConstants.CENTER);
		label.setHorizontalTextPosition(SwingConstants.CENTER);
		label.setVerticalTextPosition(SwingConstants.CENTER);
		imagePanel.add(label, BorderLayout.CENTER);

		// Make a vertical panel for labels
		final JPanel headerPanel = new JPanel();
		final BoxLayout headerLayout = new BoxLayout(headerPanel, BoxLayout.Y_AXIS);
		headerPanel.setLayout(headerLayout);
		final JLabel headerLabel = new JLabel(HEADER_LBL);
		headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		openImagePathLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		headerPanel.add(headerLabel);
		headerPanel.add(openImagePathLabel);

		final JPanel headerContainer = new JPanel();
		headerContainer.add(headerPanel);
		contentPanel.add(headerContainer, BorderLayout.NORTH);
		contentPanel.add(imagePanel, BorderLayout.CENTER);

		imagePanel.addMouseListener(new DisplayedImageMouseAdapter());

		final JScrollPane scrollPane = new JScrollPane(tree);
		final Dimension size = new Dimension(300, 200);
		scrollPane.setMinimumSize(size);
		scrollPane.setPreferredSize(size);

		final JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		sp.setLeftComponent(scrollPane);
		sp.setRightComponent(contentPanel);

		this.add(sp, BorderLayout.CENTER);
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

		// Invalidate previous selection
		updateNull();
		label.setText("Loading...");
		final Runnable treeUpdateRunnable = () -> {

			final ImageTreeNode root = new ImageTreeNode(IMAGES_LBL);

			for (final IAnalysisDataset d : getDatasets()) {
				createNodes(root, d);
			}

			final TreeModel model = new DefaultTreeModel(root);

			synchronized (tree) {
				tree.setModel(model);
				tree.setCellRenderer(new ImageNodeRenderer());

				for (int i = 0; i < tree.getRowCount(); i++) {
					tree.expandRow(i);
				}
			}
			tree.setEnabled(true);
			openImagePathLabel.setText(Labels.EMPTY_STRING);
			label.setText(Labels.EMPTY_STRING);
			label.setIcon(null);


		};

		try {
			ThreadManager.getInstance().submitUIUpdate(treeUpdateRunnable);
		} catch (final RejectedExecutionException e) {
			// probably the dataset does not exist any more. Do not spam error messages
			// though.
		}

	}

	@Override
	protected void updateNull() {
		final ImageTreeNode root = new ImageTreeNode(IMAGES_LBL);
		final TreeModel model = new DefaultTreeModel(root);
		tree.setModel(model);
		tree.setEnabled(false);
		openImagePathLabel.setText(Labels.EMPTY_STRING);
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

		final List<File> files = new ArrayList<>(dataset.getCollection().getImageFiles());

		// Each folder of images should be a node. Find the unique folders
		final List<File> parents = files.stream().map(File::getParentFile).distinct().sorted().toList();

		final ImageTreeNode datasetRoot = new ImageTreeNode(
				"Dataset: %s (%s image files)".formatted(dataset.getName(), files.size()));

		// We want the image names sorted 'sensibly', which is not the same as
		// alphabetically.
		// An image s2 should be before s10
		// The only pattern to recognise for now is eg. "s12.tiff"
		final Pattern p = Pattern.compile("^.?(\\d+)\\.tiff?$");

		// Sort numerically where possible
		final Comparator<File> comp = (f1, f2) -> {
			final Matcher m1 = p.matcher(f1.getName());
			final Matcher m2 = p.matcher(f2.getName());

			if (m1.matches() && m2.matches()) {

				final String s1 = m1.group(1);
				final String s2 = m2.group(1);

				try {

					final int i1 = Integer.parseInt(s1);
					final int i2 = Integer.parseInt(s2);
					return i1 - i2;
				} catch (final NumberFormatException e) {
					LOGGER.log(Level.SEVERE, "Error parsing number", e);
					return f1.compareTo(f2);
				}
			}
			return f1.compareTo(f2);

		};

		final Comparator<File> defaultComp = File::compareTo;

		for (final File parent : parents) {

			final List<File> inParent = files.stream()
					.filter(f -> f.getParentFile() != null && f.getParentFile().equals(parent))
					.collect(Collectors.toList());

			try {
				inParent.sort(comp);
			} catch (final IllegalArgumentException e) { // not the expected format
				inParent.sort(defaultComp);
			}

			if (parent != null) {
				final ImageTreeNode parentNode = new ImageTreeNode(parent, "Folder: " + parent.getName(), dataset);

				for (final File f : inParent) {
					parentNode.add(new ImageTreeNode(f, dataset));
				}

				datasetRoot.add(parentNode);
			}

		}

		root.add(datasetRoot);

	}


	@Override
	public void filePathUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void filePathUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	/**
	 * Listener for selection changes to the tree
	 * 
	 */
	private class ImageTreeSelectionListener implements TreeSelectionListener {

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			final ImageTreeNode data = (ImageTreeNode) e.getPath().getLastPathComponent();

			final File f = data.getFile();
			if (f == null || f.isDirectory()) {
				label.setIcon(null);
				openImagePathLabel.setText(Labels.EMPTY_STRING);
				return;
			}

			final Runnable r = () -> {
				try {
					ImageProcessor ip;
					if (f.exists()) {
						ip = ImageImporter.importFileTo24bit(f);
					} else {
						// File not found; create a white colour processor to annotate
						// Check space needed by cells in this image

						double xMax = 0;
						double yMax = 0;
						for (final Nucleus n : data.dataset.getCollection().getNuclei(f)) {
							xMax = Math.max(xMax, n.getMaxX());
							yMax = Math.max(yMax, n.getMaxY());
						}
						// Add some buffer space
						xMax += CellularComponent.COMPONENT_BUFFER;
						yMax += CellularComponent.COMPONENT_BUFFER;

						ip = ImageFilterer.createWhiteColorProcessor((int) xMax, (int) yMax);
					}

					// If an 8bit image was read in, make it colour greyscale
					final ImageConverter cn = new ImageConverter(ip);
					if (cn.isByteProcessor()) {
						cn.convertToColorProcessor();
					}
					final ImageAnnotator an = cn.toAnnotator();

					// If the node has a dataset associated, draw the cells
					if (data.dataset != null) {
						data.dataset.getCollection().getCells(f).stream()
								.forEach(an::annotateCellBorders);
					}

					// Resize to slightly smaller than the image panel
					final ImageFilterer ic = new ImageFilterer(an.toProcessor());
					ic.resizeKeepingAspect(imagePanel.getWidth(), imagePanel.getHeight());
					label.setIcon(ic.toImageIcon());
					openImagePathLabel.setText(f.getAbsolutePath());
					openImagePathLabel.setForeground(f.exists() ? Color.BLACK : Color.RED);

				} catch (final Exception e1) {
					label.setIcon(null);
					LOGGER.log(Level.SEVERE,
							"Error fetching image %s: %s".formatted(f.getAbsolutePath(),
									e1.getMessage()),
							e);
				}
			};

			try {
				ThreadManager.getInstance().submitUIUpdate(r);
			} catch (final RejectedExecutionException ex) {
				// probably the dataset does not exist any more. Do not spam error messages
				// though.
			}


		}

	}

	/**
	 * Mouse listener to allow clicking on folders for updating image paths
	 * 
	 * @return
	 */
	private class ImageTreeMouseListener extends MouseAdapter implements MouseListener {
		@Override
		public void mouseClicked(MouseEvent e) {

			if (e.getClickCount() != 2)
				return;

			final int row = tree.getRowForLocation(e.getX(), e.getY());
			if (row == -1)
				return;

			final TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
			final ImageTreeNode node = (ImageTreeNode) selPath.getLastPathComponent();

			if (node == null)
				return;

			if (node.isLeaf())
				return; // folders only can be double clicked

			updateImageFolder(node);

			((DefaultTreeModel) tree.getModel()).nodeChanged(node);

			tree.repaint();
		}

		/**
		 * Update the folder for the given node
		 * 
		 * @param node the node to be updated
		 */
		private void updateImageFolder(ImageTreeNode node) {

			final File oldFolder = node.getFile();

			// Don't update nodes that have no file
			if (oldFolder == null)
				return;

			try {

				// Shortcut file search by finding any extant element of the path
				// If the drive does not exist returns the user home dir
				final File extantPath = FileUtils.extantComponent(oldFolder);

				// Store the last folder to be selected, to speed choosing other files
				final File folderToRequest = lastSelectedFolder != null ? lastSelectedFolder : extantPath;
				final File newFolder = getInputSupplier().requestFolder(folderToRequest);
				lastSelectedFolder = newFolder;
				LOGGER.finer(
						"Image tab last selected folder is now %s".formatted(lastSelectedFolder.getAbsolutePath()));

				// Update the folder for the node and it's children
				node.setFile(oldFolder, newFolder); // update node

			} catch (final RequestCancelledException e1) {
				// No action
			}
		}
	}

	/**
	 * Mouse adapter for the displayed annotated image to allow saving
	 */
	private class DisplayedImageMouseAdapter extends MouseAdapter implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {

			// Only allow when a single image is selected
			if (e.getButton() == MouseEvent.BUTTON3 && tree.getSelectionCount() == 1) {

				// Get the image file that is currently rendered
				final TreePath path = tree.getSelectionModel().getSelectionPath();
				final ImageTreeNode node = (ImageTreeNode) path.getLastPathComponent();

				// If the selected file does not exist, do not allow saving
				if (node.file == null || !node.file.exists())
					return;

				final File file = node.getFile();
				final String fileName = file != null ? file.getName() + "_annotated"
						: "Annotated";

				final JPopupMenu popup = new JPopupMenu();
				final JMenuItem save = new JMenuItem("Save image...");
				save.addActionListener(a -> saveImage(node.dataset, fileName));
				popup.add(save);
				popup.show(imagePanel, e.getX(), e.getY());
			}
		}

		private void saveImage(IAnalysisDataset dataset, String fileName) {

			try {
				final File f = new DefaultInputSupplier().requestFileSave(dataset.getSavePath(), fileName,
						"png");

				final BufferedImage img = new BufferedImage(label.getWidth(), label.getHeight(),
						BufferedImage.TYPE_INT_ARGB);
				final Graphics2D g2d = img.createGraphics();
				label.printAll(g2d);

				try {
					ImageIO.write(img, "png", f);
				} catch (final IOException e) {
					LOGGER.warning("Cannot save image: " + e.getMessage());
					LOGGER.log(Level.SEVERE, "Error saving annotated image", e);
				} finally {
					g2d.dispose();
				}

			} catch (final RequestCancelledException e1) {
				// User cancelled
			}
		}

	}

	/**
	 * A node in the file tree. May be a file or a directory
	 * 
	 */
	private class ImageTreeNode extends DefaultMutableTreeNode {

		private transient IAnalysisDataset dataset;
		private String displayName;
		private File file = null;

		public ImageTreeNode(String s) {
			super();
			displayName = s;
		}

		public ImageTreeNode(@Nullable File f, String displayName, IAnalysisDataset d) {
			super();
			this.displayName = displayName;
			file = f;
			dataset = d;
		}

		public ImageTreeNode(@Nullable File f, IAnalysisDataset d) {
			this(f, f.getName(), d);
		}

		/**
		 * Get the file in this node, if present. Otherwise return null.
		 * 
		 * @return
		 */
		public @Nullable File getFile() {
			return file;
		}

		/**
		 * Update the file for the given node. If the node is a parent, this will
		 * recurse through all child nodes.
		 * 
		 * @param f the file or directory to which the node should point
		 */
		public void setFile(@NonNull File oldFile, @NonNull File newFile) {

			// We only update folders, and not the root node
			if (!this.isLeaf() && file != null) {
				dataset.getCollection().getImageManager().updateImageDirectory(oldFile, newFile);
			}

			file = newFile;
			displayName = newFile.getName();

			// Update each file within the node to the new folder
			final Enumeration<TreeNode> childNodes = children();
			while (childNodes.hasMoreElements()) {
				final ImageTreeNode childNode = (ImageTreeNode) childNodes.nextElement();
				final File prevFile = childNode.getFile();
				if (prevFile == null) {
					continue;
				}

				childNode.setFile(prevFile, new File(newFile, prevFile.getName()));
			}
		}

		@Override
		public String toString() {
//			if (!this.isLeaf() && file != null)
//				return file.getAbsolutePath();
			return displayName;
		}
	}


	/**
	 * Allow the string value of a node to be displayed as a tooltip
	 * 
	 * @author Ben Skinner
	 * @since 1.13.8
	 *
	 */

	private static class ImageNodeRenderer extends DefaultTreeCellRenderer {

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
				boolean expanded,
				boolean leaf, int row, boolean hasFocus) {

			final Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
					hasFocus);

			// Tooltip should show the absolute path unless the node is not a file
			final ImageTreeNode n = (ImageTreeNode) value;
			setToolTipText(n.file == null ? n.displayName : n.file.getAbsolutePath());

			// Text colour should show missing directories
			final Color fg = !n.isLeaf() && n.file != null && !n.file.exists() ? Color.RED : Color.BLACK;
			c.setForeground(fg);

			return c;
		}
	}

}
