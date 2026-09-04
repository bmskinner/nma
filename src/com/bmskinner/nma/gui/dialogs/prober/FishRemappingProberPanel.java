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
package com.bmskinner.nma.gui.dialogs.prober;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.detection.Finder;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.components.ColourSelecter;

/**
 * The image panel for FISH remapping. Stores the cells selected for remapping
 * and colours them appropriately.
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class FishRemappingProberPanel extends GenericImageProberPanel {

	private static final Logger LOGGER = Logger.getLogger(FishRemappingProberPanel.class.getName());

	private static final int ORIGINAL_IMG_COL = 0;
	private static final int ORIGINAL_IMG_ROW = 0;
	private static final double IMAGE_SCREEN_WIDTH_PROP = 0.4;
	private static final double PANEL_SCREEN_WIDTH_PROP = 0.9;
	private static final int CELL_LABEL_HEIGHT_PIXELS = 30;

	private static final String HEADER_LBL = "Unselected nuclei are blue. Use left and right mouse buttons to select nuclei.";

	private final IAnalysisDataset dataset;

	/**
	 * Nuclei selected with the left button
	 */
	private final List<UUID> selectedCellsLeft = new ArrayList<>(96);

	/**
	 * Nuclei selected with the right button
	 */
	private final List<UUID> selectedCellsRight = new ArrayList<>(96);

	private Set<ICell> openCells = new HashSet<>();

	public FishRemappingProberPanel(@NonNull IAnalysisDataset dataset, @NonNull Finder<?> finder,
			Window parent)
			throws MissingOptionException {

		super(dataset.getAnalysisOptions().get().getNucleusDetectionFolder()
				.orElseThrow(MissingOptionException::new), finder, parent);

		this.setHeaderLabelText(HEADER_LBL);
		this.dataset = dataset;

		// // Make sure the table is large enought for the images
		final Dimension minPanelSize = getPreferredSize();
		minPanelSize.width = (int) (java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth()
				* (PANEL_SCREEN_WIDTH_PROP));
		minPanelSize.height = (int) ((Toolkit.getDefaultToolkit().getScreenSize().getHeight()
				* PANEL_SCREEN_WIDTH_PROP));
		setPreferredSize(minPanelSize);
	}

	@Override
	protected JTable createTable(TableModel model) {
		// Model will not be used - we substitute a new model with more usable image
		// size
		// This is because the image prober is designed to show lots of small images,
		// and this fish remapper is cobbled on top
		final ProberTableModel m = new ProberTableModel(
				(int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth()
						* IMAGE_SCREEN_WIDTH_PROP));
		final JTable table = super.createTable(m);
		finder.addDetectionEventListener(m);
		table.setRowHeight(m.getMaxDimension());

		for (final MouseListener l : table.getMouseListeners()) {
			table.removeMouseListener(l);
		}

		// Add listener for nucleus click
		table.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				LOGGER.finer("Clicked at %s".formatted(e.getPoint()));
				if (e.getClickCount() == 1) {

					final Point pnt = e.getPoint();
					final int row = table.rowAtPoint(pnt);
					final int col = table.columnAtPoint(pnt);
					if (row == ORIGINAL_IMG_ROW && col == ORIGINAL_IMG_COL) {

						final Runnable r = () -> {
							smallImageClicked(e, pnt);
						};

						ThreadManager.getInstance().submit(r);

					} else { // Show a large image for the FISH image when
								// clicked

						final TableModel model = table.getModel();

						final ProberTableCell selectedData = (ProberTableCell) model.getValueAt(row, col);

						if (selectedData.getLargeIcon() != null) {
							new LargeImageDialog(selectedData, getWindow());
						}
					}

				}
			}

		});
		return table;
	}

	/**
	 * Import the given file as an image, detect nuclei and display the image with
	 * annotated nuclear outlines
	 * 
	 * @param imageFile
	 */
	@Override
	protected void importAndDisplayImage(File imageFile) {

		// Get the cells open in this image
		super.importAndDisplayImage(imageFile);

		if (dataset.getCollection().hasCells(imageFile)) {
			openCells = dataset.getCollection().getCells(imageFile);

			final ProberTableModel model = (ProberTableModel) table.getModel();
			final ProberTableCell infoCell = (ProberTableCell) model.getValueAt(ORIGINAL_IMG_COL,
					ORIGINAL_IMG_ROW);

			// Get the full size original image
			final Image largeImage = infoCell.getLargeIcon().getImage();

			// Draw the cells on the full size image
			for (final ICell c : openCells) {
				drawNucleus(c, largeImage);
			}

			// Rescale and redraw the small image icon from the full size image
			infoCell.setSmallIcon(
					new ImageIcon(scaleImage(infoCell.getLargeIcon(), model.getMaxDimension())));
			table.repaint();
		}

	}

	private void smallImageClicked(MouseEvent e, Point pnt) {

		final IPoint p = getPointInOriginalImage(pnt);
		if (p == null) {
			LOGGER.warning("Cannot convert to point in original image");
			return;
		}

		LOGGER.finer("Clicked at position %s in original image".formatted(p.toString()));

		// See if the clicked position is in a nucleus

		final int row = table.rowAtPoint(pnt);
		final int col = table.columnAtPoint(pnt);
		// Get the rectangle covering the cell of the table

		final Rectangle cellRectangle = table.getCellRect(row, col, false);

		// Get the icon cell at the clicked row and column
		final ProberTableModel model = (ProberTableModel) table.getModel();
		final ProberTableCell selectedData = (ProberTableCell) model.getValueAt(row, col);

		for (final ICell c : openCells) {

			for (final Nucleus n : c.getNuclei()) {
				if (n.containsOriginalPoint(p)) {
					LOGGER.finer("Match to nucleus %s".formatted(n.getNameAndNumber()));

					updateSelectedNuclei(e, c);
					drawNucleus(c, selectedData.getLargeIcon().getImage());
					// Update the small icon
					selectedData.setSmallIcon(new ImageIcon(
							scaleImage(selectedData.getLargeIcon(), model.getMaxDimension())));
					table.repaint(cellRectangle); // repaint the affected cell
													// only
					return; // don't keep searching

				}
			}

		}
	}

	/**
	 * Convert the coordinates clicked in the small icon to coordinates in the
	 * original image.
	 * 
	 * @param e
	 * @param pnt
	 * @return
	 */
	private @Nullable IPoint getPointInOriginalImage(Point pnt) {
		// Get the data model for this table
		final TableModel model = table.getModel();

		final int row = table.rowAtPoint(pnt);
		final int col = table.columnAtPoint(pnt);

		// The coordinates are relative to the cell of the table.
		// The height of the image is less than the table height, so
		// subtract the y difference
		double x = pnt.getX();
		double y = pnt.getY();

		LOGGER.finer("Clicked " + x + " : " + y);

		/*
		 * The coordinates within the cell must be converted to coordinates within the
		 * small image in the IconCell.
		 * 
		 * The x coordinates are not always correct. The IconCell is aligned
		 * horizontally, so the difference in width between the IconCell and the table
		 * cell can be used as an offset
		 * 
		 * The image prober has vertical alignment to the top of the cell, so y
		 * coordinates should also be correct.
		 * 
		 * TODO: When the row height changes from 200, there may be issues getting the
		 * correct position. The row height is assumed to be fixed. We will need to
		 * adjust the y offset based on the row height and the icon height as we do with
		 * the widths. Despite what is written in the paragraph above, the icon is
		 * vertically centre aligned, hence we can't assume y position.
		 * 
		 */

		// Get the rectangle covering the cell of the table
		final Rectangle cellRectangle = table.getCellRect(row, col, false);

		// Get the icon cell at the clicked row and column
		final ProberTableCell selectedData = (ProberTableCell) model.getValueAt(row, col);

		// Get the width of the icon in the icon cell
		final int iconWidth = selectedData.getSmallIcon().getIconWidth();
		final int iconHeight = selectedData.getSmallIcon().getIconHeight();

		// // Get the width of the column of interest
		final int columnWidth = cellRectangle.width;
		final int rowHeight = cellRectangle.height;

		LOGGER.finer("Column width is " + columnWidth);
		LOGGER.finer("IconCell width is " + iconWidth);

		// Split the difference
		final int xOffset = (columnWidth - iconWidth) >> 1;
		final int yOffset = (rowHeight - iconHeight - CELL_LABEL_HEIGHT_PIXELS) >> 1;

		x -= xOffset;
		y -= yOffset;

		LOGGER.finer("Clicked in small image " + x + " : " + y);

		if (x < 0 || x > iconWidth)
			return null; // out of bounds of icon

		if (y > selectedData.getSmallIcon().getIconHeight())
			return null; // out of image bounds in cell

		// Translate coordinates back to large image
		final double factor = selectedData.getFactor();

		final double largeX = x * factor;
		final double largeY = y * factor;

		final IPoint p = new FloatPoint(largeX, largeY);
		return p;
	}

	/**
	 * Draw the cell on the given image
	 * 
	 * @param c
	 * @param image
	 */
	private void drawNucleus(ICell c, Image image) {

		final Graphics2D g2 = (Graphics2D) image.getGraphics();

		final Color oldColor = g2.getColor();
		g2.setColor(getCellColour(c));

		for (final Nucleus n : c.getNuclei()) {
			final Shape p = n.toOriginalShape();
			g2.fill(p);
		}

		g2.setColor(oldColor);

	}

	/**
	 * Choose the colour to fill nuclei based on whether they are selected.
	 * 
	 * @param c the cell
	 * @return the nucleus colour
	 */
	private Color getCellColour(ICell c) {
		Color color = Color.BLUE;
		if (selectedCellsLeft.contains(c.getId())) {
			color = ColourSelecter.getRemappingColour(0);
		}
		if (selectedCellsRight.contains(c.getId())) {
			color = ColourSelecter.getRemappingColour(1);
		}

		LOGGER.finer("Selected %s for %s".formatted(color, c.getPrimaryNucleus().getNameAndNumber()));

		return color;
	}

	/**
	 * Create a copy of the given processor, and scale it fit the maximum dimensions
	 * specified by setSmallIconSize(). The aspect ratio is preserved.
	 * 
	 * @param ip
	 * @return
	 */
	protected Image scaleImage(ImageIcon ic, int width) {

		final double aspect = (double) ic.getIconWidth() / (double) ic.getIconHeight();

		final Dimension smallDimension = new Dimension(width,
				table.getRowHeight() - CELL_LABEL_HEIGHT_PIXELS);

		double finalWidth = smallDimension.getHeight() * aspect; // fix height
		finalWidth = finalWidth > smallDimension.getWidth() ? smallDimension.getWidth()
				: finalWidth; // but
								// constrain
								// width
								// too

		return ic.getImage().getScaledInstance((int) finalWidth, -1, Image.SCALE_SMOOTH);
	}

	/**
	 * Get a list of CellCollections, containing the selected nuclei. If no nuclei
	 * were selected, the list is empty
	 * 
	 * @return
	 */
	public List<ICellCollection> getSubCollections() {
		final List<ICellCollection> result = new ArrayList<>();

		if (!selectedCellsLeft.isEmpty()) {
			final ICellCollection subCollectionLeft = new VirtualDataset(dataset, "SubCollectionLeft");
			for (final UUID id : selectedCellsLeft) {
				final ICell cell = dataset.getCollection().getCell(id);
				subCollectionLeft.add(cell);
			}
			result.add(subCollectionLeft);
		}

		if (!selectedCellsRight.isEmpty()) {
			final ICellCollection subCollectionRight = new VirtualDataset(dataset, "SubCollectionRight");
			for (final UUID id : selectedCellsRight) {
				final ICell cell = dataset.getCollection().getCell(id);
				subCollectionRight.add(cell);
			}
			result.add(subCollectionRight);
		}

		return result;
	}

	/**
	 * Update the selected nuclei based on whether the cell was clicked with left or
	 * right mouse button
	 * 
	 * @param e the mouse event
	 * @param c the cell that was selected
	 */
	private synchronized void updateSelectedNuclei(MouseEvent e, ICell c) {

		// if present in list, remove it, otherwise add it
		if (selectedCellsLeft.contains(c.getId()) || selectedCellsRight.contains(c.getId())) {
			selectedCellsLeft.remove(c.getId());
			selectedCellsRight.remove(c.getId());

		} else {

			if (e.getButton() == MouseEvent.BUTTON3) {
				selectedCellsRight.add(c.getId());
				selectedCellsLeft.remove(c.getId());
			}

			if (e.getButton() == MouseEvent.BUTTON1) {
				selectedCellsLeft.add(c.getId());
				selectedCellsRight.remove(c.getId());
			}

		}

	}
}
