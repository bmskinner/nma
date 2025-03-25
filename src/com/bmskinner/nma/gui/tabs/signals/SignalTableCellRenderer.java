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
import java.util.logging.Logger;

import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.renderers.ConsistentRowTableCellRenderer;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.datasets.SignalTableCell;

/**
 * This allows a blank cell in a table to be coloured with a signal colour based
 * on the colour tag in the following cell, as stored within a SignalTableCell
 * 
 * @author Ben Skinner
 *
 */
@SuppressWarnings("serial")
public class SignalTableCellRenderer extends ConsistentRowTableCellRenderer {

	private static final Logger LOGGER = Logger.getLogger(SignalTableCellRenderer.class.getName());

	@Override
	public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table,
			java.lang.Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {

		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		Color colour = Color.WHITE;

		try {
			if (row < table.getModel().getRowCount() - 1) {

				int nextRow = row + 1;

				if (nextRow < table.getModel().getRowCount()) { // ignore if no data

					// get the value in the first column of the row below
					String nextRowHeader = table.getModel().getValueAt(nextRow, 0).toString();
					Object nextRowValue = table.getModel().getValueAt(nextRow, column);
					// Check if the signal block has a signal group by
					// looking at the next row
					if (nextRowHeader.equals(Labels.Signals.SIGNAL_GROUP_LABEL)
							&& nextRowValue != null
							&& !nextRowValue.toString().equals("")
							&& nextRowValue instanceof SignalTableCell cell) {
						colour = cell.color();
					}

				}
			}
		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error in signal renderer", e);
			colour = Color.WHITE;
		}

		setBackground(colour);

		return this;
	}
}
