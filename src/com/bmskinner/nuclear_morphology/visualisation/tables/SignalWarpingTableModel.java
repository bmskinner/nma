package com.bmskinner.nuclear_morphology.visualisation.tables;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.IWarpedSignal;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;

public class SignalWarpingTableModel extends DatasetTableModel {

	private static final long serialVersionUID = 1L;

	private static final String[] COL_NAMES = new String[] { Labels.Signals.Warper.TABLE_HEADER_SOURCE_DATASET,
			Labels.Signals.Warper.TABLE_HEADER_SOURCE_SIGNALS, Labels.Signals.Warper.TABLE_HEADER_SIGNALS_ONLY,
			Labels.Signals.Warper.TABLE_HEADER_BINARISED, Labels.Signals.Warper.TABLE_HEADER_NORMALISED,
			Labels.Signals.Warper.TABLE_HEADER_TARGET_SHAPE, Labels.Signals.Warper.TABLE_HEADER_THRESHOLD,
			Labels.Signals.Warper.TABLE_HEADER_COLOUR_COLUMN, Labels.Signals.Warper.TABLE_HEADER_DELETE_COLUMN };

	private Object[][] rowData;

	public SignalWarpingTableModel(@Nullable List<IAnalysisDataset> datasets) {
		if (datasets == null) {
			makeEmptyTable();
			return;
		}

		int rowCount = 0;
		for (IAnalysisDataset d : datasets) {
			for (ISignalGroup s : d.getCollection().getSignalGroups()) {
				rowCount += s.getWarpedSignals().size();
			}
		}

		rowData = new String[rowCount][COL_NAMES.length];

		int r = 0;
		for (IAnalysisDataset d : datasets) {
			for (ISignalGroup s : d.getCollection().getSignalGroups()) {
				for (IWarpedSignal w : s.getWarpedSignals()) {
					rowData[r][0] = d.getName();
					rowData[r][1] = s.getGroupColour();
					rowData[r][2] = w.isCellsWithSignals();
					rowData[r][3] = w.isBinarised();
					rowData[r][4] = w.isNormalised();
					rowData[r][5] = w.target();
					rowData[r][6] = w.threshold();
					rowData[r][7] = s.getGroupColour().orElse(ColourSelecter.getSignalColour(r));
					rowData[r][8] = EMPTY_STRING;
					r++;
				}
			}
		}

	}

	@Override
	public int getRowCount() {
		return rowData.length;
	}

	@Override
	public int getColumnCount() {
		return COL_NAMES.length;
	}

	@Override
	public String getColumnName(int column) {
		return COL_NAMES[column];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return rowData[rowIndex][columnIndex];
	}

	@Override
	protected void makeEmptyTable() {
		rowData = new String[1][COL_NAMES.length];
		for (int c = 0; c < COL_NAMES.length; c++) {
			rowData[0][c] = EMPTY_STRING;
		}
	}

}
