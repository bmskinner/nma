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
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.ExportableTable;
import com.bmskinner.nma.gui.components.renderers.JTextAreaCellRenderer;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.options.AbstractOptions;
import com.bmskinner.nma.visualisation.options.TableOptions;
import com.bmskinner.nma.visualisation.options.TableOptionsBuilder;
import com.bmskinner.nma.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nma.visualisation.tables.AnalysisDatasetTableCreator;

/**
 * This panel shows any merge sources for a merged dataset, and the analysis
 * options used to create the merge
 * 
 * @author bms41
 * @since 1.9.0
 *
 */
@SuppressWarnings("serial")
public class MergesDetailPanel extends TableDetailPanel {

	private static final Logger LOGGER = Logger.getLogger(MergesDetailPanel.class.getName());

	private ExportableTable table;
	private JLabel headerLabel = new JLabel(Labels.NULL_DATASETS);

	private static final String RECOVER_BUTTON_TEXT = "Recover source";

	private static final String PANEL_TITLE_LBL = "Merges";
	private static final String PANEL_DESC_LBL = "Show the sources for merged datasets";

	public MergesDetailPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);

		try {
			createUI();
		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error creating merge panel", e);
		}
	}

	private void createUI() {

		this.setLayout(new BorderLayout());

		this.add(createHeaderPanel(), BorderLayout.NORTH);
		this.add(createTablePanel(), BorderLayout.CENTER);
	}

	private JPanel createTablePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		TableCellRenderer buttonRenderer = new JButtonRenderer();
		TableCellRenderer textRenderer = new JTextAreaCellRenderer();

		table = new ExportableTable() {
			@Override
			public TableCellRenderer getCellRenderer(int row, int column) {
				if ((this.getValueAt(row, 0).equals(Labels.Merges.RECOVER_SOURCE)) && column > 0) {
					return buttonRenderer;
				}
				return textRenderer;
			}
		};

		MouseListener mouseListener = new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				int col = table.columnAtPoint(e.getPoint());
				if (col == 0)
					return;

				if (table.getValueAt(row, 0).equals(Labels.Merges.RECOVER_SOURCE)
						&& table.getValueAt(row, col) != null) {
					IAnalysisDataset mergeSource = (IAnalysisDataset) table
							.getValueAt(row, col);
					LOGGER.fine(
							() -> String.format("Extracting merge source '%s'",
									mergeSource.getName()));

					UserActionController.getInstance().userActionEventReceived(
							new UserActionEvent(this, UserActionEvent.EXTRACT_MERGE_SOURCE,
									List.of(mergeSource)));
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// Not needed
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// Not needed
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// Not needed
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// Not needed
			}

		};
		table.addMouseListener(mouseListener);
		table.setModel(AbstractTableCreator.createBlankTable());

		table.setEnabled(false);
		table.setDefaultRenderer(Object.class, new JTextAreaCellRenderer());
		JScrollPane scrollPane = new JScrollPane(table);

		JPanel tablePanel = new JPanel(new BorderLayout());

		tablePanel.add(scrollPane, BorderLayout.CENTER);
		tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);

		panel.add(tablePanel);
		return panel;
	}

	@Override
	public synchronized void setLoading() {
		super.setLoading();
		table.setModel(AbstractTableCreator.createLoadingTable());
	}

	private JPanel createHeaderPanel() {
		JPanel panel = new JPanel();
		panel.add(headerLabel);
		return panel;

	}

	@Override
	protected synchronized void updateSingle() {

		headerLabel.setText(
				Labels.SINGLE_DATASET + " with " + activeDataset().getAllMergeSources().size()
						+ " merge sources");

		List<IAnalysisDataset> mergeSources = new ArrayList<>(activeDataset().getAllMergeSources());

		TableOptions options = new TableOptionsBuilder()
				.setDatasets(mergeSources)
				.setTarget(table)
				.setBoolean(AbstractOptions.IS_MERGE_SOURCE_OPTIONS_TABLE, true)
				.build();
		setTable(options);

	}

	@Override
	protected synchronized void updateMultiple() {
		updateNull();
		headerLabel.setText(Labels.MULTIPLE_DATASETS);
	}

	@Override
	protected synchronized void updateNull() {
		table.setModel(AbstractTableCreator.createBlankTable());
		headerLabel.setText(Labels.NULL_DATASETS);
	}

	@Override
	protected synchronized TableModel createPanelTableType(@NonNull TableOptions options) {
		return new AnalysisDatasetTableCreator(options).createMergeSourceAnalysisParametersTable();
	}

	/**
	 * Render a button in a cell. Note, this is non-functional - it just paints a
	 * button shape. Use a mouse listener on the table for functionality
	 * 
	 * @author bms41
	 * @since 1.16.0
	 *
	 */
	private class JButtonRenderer extends JButton implements TableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus,
				int row, int column) {
			String text = value == null ? ""
					: value instanceof IAnalysisDataset ? RECOVER_BUTTON_TEXT : "";
			setText(text);
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			return this;
		}
	}
}
