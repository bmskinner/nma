package com.bmskinner.nuclear_morphology.visualisation.tables;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.IWarpedSignal;
import com.bmskinner.nuclear_morphology.gui.Labels;

public class SignalWarpingTableModel extends DatasetTableModel {
	private static final Logger LOGGER = Logger.getLogger(SignalWarpingTableModel.class.getName());
	private static final long serialVersionUID = 1L;

	private static final String[] COL_NAMES = new String[] { Labels.Signals.Warper.TABLE_HEADER_SOURCE_DATASET,
			Labels.Signals.Warper.TABLE_HEADER_SOURCE_SIGNALS, Labels.Signals.Warper.TABLE_HEADER_TARGET_SHAPE,
			Labels.Signals.Warper.TABLE_HEADER_SIGNALS_ONLY, Labels.Signals.Warper.TABLE_HEADER_BINARISED,
			Labels.Signals.Warper.TABLE_HEADER_NORMALISED, Labels.Signals.Warper.TABLE_HEADER_THRESHOLD,
			Labels.Signals.Warper.TABLE_HEADER_COLOUR_COLUMN };

	private Object[][] rowData;
	private IWarpedSignal[] ws;

	public SignalWarpingTableModel(@Nullable List<IAnalysisDataset> datasets) {
		if (datasets == null) {
			makeEmptyTable();
			return;
		}

		// Ensure we don't double count signals from child datasets, which share the
		// signal group with their parent
		int rowCount = 0;
		for (IAnalysisDataset d : datasets) {
			for (ISignalGroup s : d.getCollection().getSignalGroups()) {
				rowCount += s.getWarpedSignals().stream().filter(w -> w.source().equals(d.getId())).count();
			}
		}

		rowData = new Object[rowCount][COL_NAMES.length];
		ws = new IWarpedSignal[rowCount];

		int r = 0;
		for (IAnalysisDataset d : datasets) {
			for (ISignalGroup s : d.getCollection().getSignalGroups()) {
				for (IWarpedSignal w : s.getWarpedSignals()) {
					if (!w.source().equals(d.getId()))
						continue;
					ws[r] = w;
					rowData[r][0] = d.getName();
					rowData[r][1] = s.getGroupName();
					rowData[r][2] = ws[r].targetName();
					rowData[r][3] = ws[r].isCellsWithSignals();
					rowData[r][4] = ws[r].isBinarised();
					rowData[r][5] = ws[r].isNormalised();
					rowData[r][6] = ws[r].threshold();
					rowData[r][7] = ws[r].colour();

					r++;
				}
			}
		}
	}

	public IWarpedSignal getWarpedSignal(int row) {
		return ws[row];
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
