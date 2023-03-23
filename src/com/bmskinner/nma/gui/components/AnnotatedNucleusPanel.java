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
package com.bmskinner.nma.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.image.ImageFilterer;

import ij.process.ImageProcessor;

/**
 * Display the original image for a cell, with the nucleus outlines drawn on it.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class AnnotatedNucleusPanel extends JPanel {

	private static final Logger LOGGER = Logger.getLogger(AnnotatedNucleusPanel.class.getName());

	private ICell cell;
	private JLabel imageLabel = new JLabel();
	private boolean fixedDim = false;

	public AnnotatedNucleusPanel(boolean fixedDimension) {
		this.fixedDim = fixedDimension;
		this.setLayout(new BorderLayout());
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.add(imageLabel, BorderLayout.CENTER);
		imageLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		imageLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
		imageLabel.setVerticalAlignment(SwingConstants.CENTER);
		imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
	}

	/**
	 * Display the given cell annotated on its source image
	 * 
	 * @param c the cell to draw
	 * @throws Exception
	 */
	public void showCell(ICell c) throws Exception {
		this.cell = c;
		importCellImage();

	}

	/**
	 * Display the given cell cropped
	 * 
	 * @param c           the cell to display
	 * @param isAnnotated if true, draw the cell border or nuclear segments
	 * @param isRGB       if true, import the RGB image of the cell, otherwise
	 *                    import the nucleus image channel as 24bit greyscale
	 * @throws Exception
	 */
	public void showOnlyCell(ICell c, boolean isAnnotated, boolean isRGB) throws Exception {
		this.cell = c;
		ImageProcessor ip;

		CellularComponent component = c.hasCytoplasm() ? c.getCytoplasm() : c.getPrimaryNucleus();

		if (isRGB) {
			ip = ImageImporter.importCroppedImageTo24bitRGB(component);
		} else {
			ip = ImageImporter.importCroppedImageTo24bitGreyscale(component);
		}

//	try
//
//	{
//		if (c.hasCytoplasm()) {
//			ip = ImageImporter.importCroppedImageTo24bitGreyscale(c.getCytoplasm());
//		} else {
//			ip = ImageImporter.importCroppedImageTo8bit(c.getPrimaryNucleus());
//		}
//
//	}catch(
//	UnloadableImageException e)
//	{
//		LOGGER.log(Loggable.STACK, "Cannot load image for component", e);
//		return;
//	}

		if (isAnnotated)

		{

			ImageAnnotator an = new ImageAnnotator(ip);
			if (c.hasCytoplasm()) {

				an = an.annotateBorder(c.getCytoplasm(), c.getCytoplasm(), Color.CYAN);
				for (Nucleus n : c.getNuclei()) {
					an.annotateBorder(n, c.getCytoplasm(), Color.ORANGE);
				}

			} else {

				for (Nucleus n : c.getNuclei()) {
					an = an.annotateSegments(n, n);
				}
			}
			ip = an.toProcessor();
		}

		drawIcon(ip);
	}

	private void importCellImage() throws Exception {

		boolean useRGB = false;

		if (cell.hasCytoplasm()) {
			useRGB = true;
		}

		ImageProcessor openProcessor = useRGB ? ImageImporter.importCroppedImageTo24bitGreyscale(cell.getCytoplasm())
				: ImageImporter.importCroppedImageTo24bitGreyscale(cell.getPrimaryNucleus());

		ImageAnnotator an = new ImageAnnotator(openProcessor);

		if (cell.hasCytoplasm()) {
			an.drawBorder(cell.getCytoplasm(), Color.CYAN);
		}

		for (Nucleus n : cell.getNuclei()) {
			an.drawBorder(n, Color.ORANGE);
		}

		if (fixedDim)
			an.resizeKeepingAspect(200, 200);

		drawIcon(an.toProcessor());
	}

	/**
	 * Draw the given processor to the image icon
	 * 
	 * @param ip
	 */
	private void drawIcon(ImageProcessor ip) {
		ImageIcon icon = null;
		if (imageLabel.getIcon() != null) {
			icon = (ImageIcon) imageLabel.getIcon();
			icon.getImage().flush();
		}

		if (fixedDim)
			icon = new ImageIcon(ip.getBufferedImage());
		else
			icon = new ImageIcon(ImageFilterer.fitToScreen(ip, 0.8).getBufferedImage());

		imageLabel.setIcon(icon);
		imageLabel.revalidate();
		imageLabel.repaint();

		this.repaint();
	}
}
