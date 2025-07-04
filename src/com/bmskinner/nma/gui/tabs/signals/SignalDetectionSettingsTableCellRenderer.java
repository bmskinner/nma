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
package com.bmskinner.nma.gui.tabs.signals;

import java.awt.Color;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JTable;

import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.renderers.ConsistentRowTableCellRenderer;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.datasets.SignalTableCell;

/**
 * Colour analysis parameter table cell background. If all the datasets selected
 * have the same value, colour them light green. Takes into consideration the
 * background colous from signal groups
 */
@SuppressWarnings("serial")
public class SignalDetectionSettingsTableCellRenderer extends ConsistentRowTableCellRenderer {

	private static final Logger LOGGER = Logger
			.getLogger(SignalDetectionSettingsTableCellRenderer.class.getName());

	@Override
	public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
				row, column);

		Color bg = Color.WHITE;

		String header = getFirstColumnText(row, table);

		// Highlight consistent rows
		if (isRowConsistentAcrossColumns(table, row))
			bg = ConsistentRowTableCellRenderer.CONSISTENT_CELL_COLOUR;

		Color fg = chooseForegroundColour(header, value);

		try {

			// Set colour of signal groups

			if (row < table.getModel().getRowCount() - 1) {

				// get the value in the first column of the row below
				String nextRowHeader = getFirstColumnText(row + 1, table);

				int signalGroupCount = Integer
						.parseInt(table.getModel().getValueAt(0, column).toString());

				if (nextRowHeader.equals(Labels.Signals.SIGNAL_GROUP_LABEL)) {

					if (signalGroupCount > 0) {
						// we want to colour this cell preemptively
						// get the signal table cell from the table
						Object val = table.getModel().getValueAt(row + 1, column);
						if (val != null && !val.toString().equals("")) {
							SignalTableCell cell = (SignalTableCell) table.getModel()
									.getValueAt(row + 1, column);

							bg = cell.color();
						}

					} else {
						bg = Color.WHITE; // don't allow consitentcy colours
											// in the signal group line
					}

				}
			}

		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error in signal detection table renderer", e);
		}

		l.setBackground(bg);
		l.setForeground(fg);
		return l;
	}

	private Color chooseForegroundColour(String header, Object value) {
		if (header.equals(Labels.Signals.SIGNAL_SOURCE_LABEL)) {
			if (value == null)
				return Color.RED;

			File signalFolder = new File(value.toString());
			if (!signalFolder.exists())
				return Color.RED;

		}
		return Color.BLACK;
	}

}
