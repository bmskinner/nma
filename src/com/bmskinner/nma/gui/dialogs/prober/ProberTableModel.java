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

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;

import com.bmskinner.nma.analysis.detection.Finder.DetectionEvent;
import com.bmskinner.nma.analysis.detection.Finder.DetectionEventListener;
import com.bmskinner.nma.gui.dialogs.prober.GenericImageProberPanel.ProberTableCell;
import com.bmskinner.nma.visualisation.image.ImageFilterer;
import com.bmskinner.nma.visualisation.image.ImageConverter;

import ij.process.ImageProcessor;

public class ProberTableModel extends DefaultTableModel implements DetectionEventListener {

	private static final Logger LOGGER = Logger.getLogger(ProberTableModel.class.getName());

	private static final long serialVersionUID = 1L;

	private final int maxDimension;

	public static final int DEFAULT_MAX_DIMENSION = 250;

	/**
	 * Default constructor with two columns. Images will be drawn at the size best
	 * fitting {@link DEFAULT_MAX_DIMENSION}
	 */
	public ProberTableModel() {
		this(DEFAULT_MAX_DIMENSION);
	}

	/**
	 * Default constructor with two columns
	 * 
	 * @param maxDimension the maximum width or height an image in the table will be
	 *                     drawn, preserving aspect ratio
	 */
	public ProberTableModel(int maxDimension) {
		super();
		this.maxDimension = maxDimension;
		this.setColumnCount(2);
	}

	public int getMaxDimension() {
		return maxDimension;
	}

	@Override
	public void detectionEventReceived(DetectionEvent e) {
		ProberTableCell cell = makeIconCell(e.getProcessor(), e.getMessage(), true);
		ProberTableCell blank = makeIconCell(
				ImageConverter.createBlankImage(maxDimension, maxDimension), "", true);

		if (getRowCount() == 0) {
			addRow(new ProberTableCell[] { cell, blank });
			return;
		}

		ProberTableCell existing = (ProberTableCell) getValueAt(getRowCount() - 1, 1);

		if (existing.toString().equals("")) { // Blank processor
			this.setValueAt(cell, getRowCount() - 1, 1);
		} else {
			addRow(new ProberTableCell[] { cell, blank });
		}

	}

	/**
	 * Create a table cell from the given image, specifying the image type and
	 * enabled
	 * 
	 * @param ip      the image processor
	 * @param label   the label for the cell
	 * @param enabled is the cell enabled
	 * @return a table cell for the image prober
	 */
	protected ProberTableCell makeIconCell(ImageProcessor ip, String label, boolean enabled) {

		ImageFilterer filt = new ImageFilterer(ip);
		ImageIcon ic = filt.toImageIcon();
		ProberTableCell iconCell = new ProberTableCell(ic, label, enabled);
		filt.resizeKeepingAspect(maxDimension, maxDimension);
		ImageIcon small = filt.toImageIcon();

		iconCell.setSmallIcon(small);
		return iconCell;
	}

}
