package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.TreeTableModel;

import com.bmskinner.nuclear_morphology.gui.tabs.populations.DatasetsPanel.TreeSelectionHandler;

public class DatasetTreeTable extends JXTreeTable {

	private TreeSelectionHandler treeListener;

	public DatasetTreeTable() {
		super();
		setDefaults();
	}

	public DatasetTreeTable(TreeTableModel model) {
		super(model);
		setDefaults();
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	private void setDefaults() {
		setEnabled(true);
		setCellSelectionEnabled(false);
		setColumnSelectionAllowed(false);
		getTableHeader().setReorderingAllowed(false);
		setRowSelectionAllowed(true);
		setAutoCreateColumnsFromModel(false);
//		getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR)
//				.setCellRenderer(new PopulationTableCellRenderer());
//		getColumnModel().getColumn(PopulationTreeTable.COLUMN_NAME).setPreferredWidth(DEFAULT_NAME_COLUMN_WIDTH);
//		getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR).setPreferredWidth(DEFAULT_COLOUR_COLUMN_WIDTH);
	}

}
