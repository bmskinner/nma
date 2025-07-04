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

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.Io.Exporter;

/**
 * An extension to a JTable that allows the table contents to be exported to
 * file, or copied to the clipboard.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class ExportableTable extends JTable {

	private boolean isGlobalEditable = true;

	/**
	 * Create a table with a provided model. Specify a global editing override.
	 * 
	 * @param model            the table model
	 * @param isGlobalEditable true if some or all cells are to be editable, false
	 *                         otherwise
	 */
	public ExportableTable(TableModel model, boolean isGlobalEditable) {
		super(model);
		setComponentPopupMenu(new TablePopupMenu());
		this.isGlobalEditable = isGlobalEditable;

		ColumnListener cl = new ColumnListener() {

			@Override
			public void columnMoved(int oldLocation, int newLocation) {
			}

			@Override
			public void columnResized(int column, int newWidth) {
				updateRowHeights();
			}

		};

		getColumnModel().addColumnModelListener(cl);
		getTableHeader().addMouseListener(cl);
	}

	/**
	 * Create an empty table
	 */
	public ExportableTable() {
		super();
		setComponentPopupMenu(new TablePopupMenu());
	}

	/**
	 * Create with a table model
	 * 
	 * @param model the table model
	 */
	public ExportableTable(TableModel model) {
		this(model, true);
		setComponentPopupMenu(new TablePopupMenu());
	}

	@Override
	public void validate() {
		updateRowHeights(); // make sure all rows will fit contents
		super.validate(); // redraw all components
		resizeAndRepaint();
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		if (isGlobalEditable)
			return super.isCellEditable(row, column);
		return false;
	}

	/**
	 * Ensure rows are high enough to fill all text. Get the number of text lines
	 * per cell and set row height to the maximum cell height in that row
	 */
	public void updateRowHeights() {
		for (int row = 0; row < getRowCount(); row++) {
			int h = 20;
			for (int col = 0; col < getColumnCount(); col++) {

				// Find the component at the given cell
				TableCellRenderer tcr = getCellRenderer(row, col);

				Component c = tcr.getTableCellRendererComponent(this,
						this.getValueAt(row, col), false, false, row, col);

				if (c instanceof JTextArea jta) {
					int lineCount = jta.getLineCount() + 1;
					Font f = c.getFont();
					FontMetrics fm = c.getFontMetrics(f);
					int fheight = fm.getHeight();
					int rowHeight = lineCount * fheight;
					h = Math.max(h, rowHeight);
				}
			}
			setRowHeight(row, h);
		}
	}

	/**
	 * Allow row heights to be resized when column widths change
	 * 
	 * @author bms41
	 * @since 1.16.0
	 *
	 */
	private abstract class ColumnListener extends MouseAdapter implements TableColumnModelListener {

		private int oldIndex = -1;
		private int newIndex = -1;
		private boolean dragging = false;

		private boolean resizing = false;
		private int resizingColumn = -1;
		private int oldWidth = -1;

		@Override
		public void mousePressed(MouseEvent e) {
			// capture start of resize
			if (e.getSource()instanceof JTableHeader header) {
				TableColumn tc = header.getResizingColumn();
				if (tc != null) {
					resizing = true;
					JTable table = header.getTable();
					resizingColumn = table.convertColumnIndexToView(tc.getModelIndex());
					oldWidth = tc.getPreferredWidth();
				} else {
					resizingColumn = -1;
					oldWidth = -1;
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// column moved
			if (dragging && oldIndex != newIndex) {
				columnMoved(oldIndex, newIndex);
			}
			dragging = false;
			oldIndex = -1;
			newIndex = -1;

			// column resized
			if (resizing) {
				if (e.getSource()instanceof JTableHeader header) {
					TableColumn tc = header.getColumnModel().getColumn(resizingColumn);
					if (tc != null) {
						int newWidth = tc.getPreferredWidth();
						if (newWidth != oldWidth) {
							columnResized(resizingColumn, newWidth);
						}
					}
				}
			}
			resizing = false;
			resizingColumn = -1;
			oldWidth = -1;
		}

		@Override
		public void columnAdded(TableColumnModelEvent e) {
		}

		@Override
		public void columnRemoved(TableColumnModelEvent e) {
		}

		@Override
		public void columnMoved(TableColumnModelEvent e) {
			// capture dragging
			dragging = true;
			if (oldIndex == -1) {
				oldIndex = e.getFromIndex();
			}

			newIndex = e.getToIndex();
		}

		@Override
		public void columnMarginChanged(ChangeEvent e) {
		}

		@Override
		public void columnSelectionChanged(ListSelectionEvent e) {
		}

		public abstract void columnMoved(int oldLocation, int newLocation);

		public abstract void columnResized(int column, int newWidth);
	}

	/**
	 * The popup menu for the table
	 * 
	 * @author bms41
	 *
	 */
	private class TablePopupMenu extends JPopupMenu {

		private static final String EXPORT_LBL = "Export to file";
		private static final String COPY_LBL = "Copy to clipboard";

		public TablePopupMenu() {
			super("Popup");
			createButtons();

		}

		private void createButtons() {
			JMenuItem exportItem = new JMenuItem(EXPORT_LBL);
			exportItem.addActionListener(e -> export());

			JMenuItem copyItem = new JMenuItem(COPY_LBL);
			copyItem.addActionListener(e -> copy());

			add(copyItem);
			add(exportItem);
		}

		private void export() {

			try {
				File saveFile = new DefaultInputSupplier().requestFileSave(null, "Table export",
						"txt");
				String string = makeExportString();
				Exporter.writeString(string, saveFile);

			} catch (RequestCancelledException e) {
				// User cancelled
			}
		}

		private void copy() {
			String string = makeExportString();
			StringSelection stringSelection = new StringSelection(string);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		}

		private String makeExportString() {
			StringBuilder builder = new StringBuilder();
			TableModel model = getModel();
			for (int col = 0; col < model.getColumnCount(); col++) {
				builder.append(model.getColumnName(col) + Io.TAB);
			}
			builder.append(Io.NEWLINE);
			for (int row = 0; row < model.getRowCount(); row++) {

				for (int col = 0; col < model.getColumnCount(); col++) {
					Object value = model.getValueAt(row, col);
					if (value != null) {
						value = value.toString().replaceAll(Io.NEWLINE, ", ");
						value = value.toString().replaceAll(", $", ""); // trailing comma
					}
					builder.append(value + Io.TAB);
				}
				builder.append(Io.NEWLINE);
			}
			return builder.toString();
		}

	}

}
