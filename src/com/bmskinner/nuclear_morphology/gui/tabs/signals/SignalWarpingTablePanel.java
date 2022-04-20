package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.TableDetailPanel;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;
import com.bmskinner.nuclear_morphology.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.tables.SignalWarpingTableModel;

public class SignalWarpingTablePanel extends TableDetailPanel {

	private ExportableTable table;

	public SignalWarpingTablePanel() {

		setLayout(new BorderLayout());

		table = new ExportableTable(AbstractTableCreator.createBlankTable());
		table.setEnabled(false);
		JScrollPane scrollPane = new JScrollPane(table);
		add(scrollPane, BorderLayout.CENTER);
	}

	@Override
	protected TableModel createPanelTableType(@NonNull TableOptions options) {
		return new SignalWarpingTableModel(options.getDatasets());
	}

}
