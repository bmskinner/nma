package com.bmskinner.nma.visualisation.tables;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Calculate and display the number of shared cells between datasets
 * 
 * @author Ben Skinner
 *
 */
public class VennTableModel extends DatasetTableModel {

	private static final long serialVersionUID = -3102122971226626102L;
	private String[] colNames;
	private String[][] rowData;

	public VennTableModel(@Nullable List<IAnalysisDataset> datasets) {

		if (datasets == null) {
			makeEmptyTable();
			return;
		}

		colNames = makeColNames(datasets);
		int colCount = colNames.length;

		int rowCount = datasets.size();

		rowData = new String[rowCount][colCount];
		for (int r = 0; r < rowCount; r++) {
			for (int c = 0; c < colCount; c++) {
				if (c == 0) {
					rowData[r][c] = datasets.get(r).getName();
					continue;
				}

				if (r == c - 1) {
					rowData[r][c] = EMPTY_STRING;
					continue;
				}

				int shared = datasets.get(r).getCollection().countShared(datasets.get(c - 1).getCollection());

				int d2size = datasets.get(c - 1).getCollection().size();

				double pct = d2size == 0 ? 0 : (shared / (double) d2size) * 100;

				rowData[r][c] = df.format(pct) + "%";
			}
		}
	}

	@Override
	protected void makeEmptyTable() {
		colNames = new String[] { EMPTY_STRING };
		rowData = new String[1][colNames.length];
		for (int c = 0; c < colNames.length; c++) {
			rowData[0][c] = EMPTY_STRING;
		}
	}

	@Override
	public int getRowCount() {
		return rowData.length;
	}

	@Override
	public int getColumnCount() {
		return colNames.length;
	}

	@Override
	public String getColumnName(int column) {
		return colNames[column];
	}

	@Override
	public String getValueAt(int rowIndex, int columnIndex) {
		return rowData[rowIndex][columnIndex];
	}
}
