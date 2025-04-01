package com.bmskinner.nma.gui.tabs.populations;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.TreeTableModel;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.gui.components.ColourSelecter;

public class DatasetTreeTable extends JXTreeTable {

	private static final long serialVersionUID = 1L;

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

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component c = super.prepareRenderer(renderer, row, column);

		int rendererWidth = c.getPreferredSize().width;
		TableColumn tableColumn = getColumnModel().getColumn(column);
		tableColumn.setMinWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));

		
		List<Integer> hashcodes = DatasetListManager.getInstance().getSelectedDatasets()
				.stream().map(IAnalysisDataset::hashCode)
				.collect(Collectors.toList());
		
		// add custom rendering here
		// Note that this method runs on the EDT
		if (column == 2) {
			Object c0 = getValueAt(row, 0);
			Color colour = Color.WHITE;

			if (c0 instanceof IAnalysisDataset d) {
				// Is the dataset currently selected?
				if(hashcodes.contains(d.hashCode())) {
					if (d.hasDatasetColour()) {
						colour = d.getDatasetColour().get();
					} else {
						int index = hashcodes.indexOf(d.hashCode());
						if (index > -1)
							colour = d.getDatasetColour().orElse(ColourSelecter.getColor(index));
					}
				}
			}
			c.setBackground(colour);
			c.setForeground(colour);
		}

		return c;
	}

	private void setDefaults() {
		setEnabled(true);
		setCellSelectionEnabled(false);
		setColumnSelectionAllowed(false);
		getTableHeader().setReorderingAllowed(false);
		setRowSelectionAllowed(true);
		setAutoCreateColumnsFromModel(false);

		getColumnModel().getColumn(0).setPreferredWidth(240);
//		getColumnModel().getColumn(0).setMinWidth(240);
//		getColumnModel().getColumn(0).setMaxWidth(240);
//		getColumnModel().getColumn(1).setPreferredWidth(100);
//		getColumnModel().getColumn(1).setMinWidth(100);
//		getColumnModel().getColumn(1).setMaxWidth(100);
		setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
	}

}
