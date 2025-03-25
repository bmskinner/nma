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
package com.bmskinner.nma.gui.dialogs.collections;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.components.SelectableCellIcon;
import com.bmskinner.nma.gui.dialogs.MessagingDialog;
import com.bmskinner.nma.io.ImageImportWorker;


/**
 * Display collections of images from a dataset. Uses a SwingWorker to import
 * the images.
 * 
 * @author Ben Skinner
 * @since 1.13.7
 *
 */
public abstract class AbstractCellCollectionDialog extends MessagingDialog
		implements PropertyChangeListener {

	private static final Logger LOGGER = Logger
			.getLogger(AbstractCellCollectionDialog.class.getName());

	public static final int COLUMN_COUNT = 3;

	protected static final String LOADING_LBL = "Loading";

	protected IAnalysisDataset dataset;
	protected JTable table;
	protected JProgressBar progressBar;
	protected ImageImportWorker worker;
	protected CellCollectionModel model;

	/**
	 * Construct with a dataset to display
	 * 
	 * @param dataset
	 */
	protected AbstractCellCollectionDialog(IAnalysisDataset dataset) {
		super();
		this.dataset = dataset;

		createUI();
		createWorker();

		this.setModal(false);
		this.pack();
		this.setVisible(true);
	}

	/**
	 * Create the table to display cells in the underlying model
	 * 
	 */
	protected void createTable() {
		table = new JTable(model) {
			// Returning the Class of each column will allow different
			// renderers to be used based on Class
			@Override
			public Class<?> getColumnClass(int column) {
				return JLabel.class;
			}
		};

		for (int col = 0; col < COLUMN_COUNT; col++) {
			table.getColumnModel().getColumn(col)
					.setCellRenderer(new SelectableTableCellRenderer());
		}

		table.setRowHeight(180);
		table.setCellSelectionEnabled(true);
		table.setRowSelectionAllowed(false);
		table.setColumnSelectionAllowed(false);
		table.setTableHeader(null);

		ListSelectionModel cellSelectionModel = table.getSelectionModel();
		cellSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	/**
	 * Create the image import worker needed to import and annotate images.
	 */
	protected abstract void createWorker();

	/**
	 * Create the UI of the dialog
	 */
	protected abstract void createUI();

	protected abstract JPanel createHeader();

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		int value = 0;
		try {
			Object newValue = evt.getNewValue();

			if (newValue.getClass().isAssignableFrom(Integer.class)) {
				value = (int) newValue;

			}
			if (value >= 0 && value <= 100) {
				progressBar.setValue(value);
			}

			if (evt.getPropertyName().equals("Finished")) {
				LOGGER.finest("Worker signaled finished");
				progressBar.setVisible(false);

			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting value from property change", e);
		}

	}

	// @SuppressWarnings("serial")
	public class SelectableTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus,
				int row, int column) {

			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			SelectableCellIcon info = (SelectableCellIcon) value;
			setIcon(info);
			setHorizontalAlignment(SwingConstants.CENTER);
			setHorizontalTextPosition(SwingConstants.CENTER);
			setVerticalTextPosition(SwingConstants.BOTTOM);

			if (info.isSelected()) {
				setBackground(Color.GREEN);
			} else {
				setBackground(Color.WHITE);
			}

			return this;
		}
	}
}
