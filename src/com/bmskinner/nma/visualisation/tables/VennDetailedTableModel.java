package com.bmskinner.nma.visualisation.tables;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

public class VennDetailedTableModel extends DatasetTableModel {

	private static final Logger LOGGER = Logger.getLogger(VennDetailedTableModel.class.getName());

	private static final long serialVersionUID = -3102122971226626102L;

	private static final String[] COL_NAMES = new String[] { "Dataset 1", "Unique %", "Unique cells", "Shared %",
			"Shared cells", "Shared %", "Unique cells", "Unique %", "Dataset 2" };

	private String[][] rowData;

	private record DatasetTuple(IAnalysisDataset a, IAnalysisDataset b) {
	}

	public VennDetailedTableModel(@Nullable List<IAnalysisDataset> datasets) {

		if (datasets == null) {
			makeEmptyTable();
			return;
		}

		// Track the pairwise comparisons performed to avoid duplicates
		List<DatasetTuple> matches = new ArrayList<>();
		for (IAnalysisDataset dataset1 : datasets) {
			for (IAnalysisDataset dataset2 : datasets) {
				if (dataset1 == dataset2)
					continue;
				DatasetTuple t = new DatasetTuple(dataset1, dataset2);
				DatasetTuple s = new DatasetTuple(dataset2, dataset1);
				if (!(matches.contains(t) || matches.contains(s))) {
					matches.add(t);
				}
			}
		}

		rowData = new String[matches.size()][COL_NAMES.length];

		for (int r = 0; r < rowData.length; r++) {
			IAnalysisDataset dataset1 = matches.get(r).a;
			IAnalysisDataset dataset2 = matches.get(r).b;

			rowData[r][0] = dataset1.getName();
			rowData[r][8] = dataset2.getName();

			// compare the number of shared nucleus ids
			int shared = dataset1.getCollection().countShared(dataset2);

			rowData[r][4] = String.valueOf(shared);

			int unique1 = dataset1.getCollection().size() - shared;
			int unique2 = dataset2.getCollection().size() - shared;
			rowData[r][2] = String.valueOf(unique1);
			rowData[r][6] = String.valueOf(unique2);

			double uniquePct1 = ((double) unique1 / (double) dataset1.getCollection().size()) * 100;
			double uniquePct2 = ((double) unique2 / (double) dataset2.getCollection().size()) * 100;

			rowData[r][1] = df.format(uniquePct1) + "%";
			rowData[r][7] = df.format(uniquePct2) + "%";

			double sharedpct1 = ((double) shared / (double) dataset1.getCollection().size()) * 100;
			double sharedpct2 = ((double) shared / (double) dataset2.getCollection().size()) * 100;

			rowData[r][3] = df.format(sharedpct1) + "%";
			rowData[r][5] = df.format(sharedpct2) + "%";

		}
	}

	@Override
	protected void makeEmptyTable() {
		rowData = new String[1][COL_NAMES.length];
		for (int c = 0; c < COL_NAMES.length; c++) {
			rowData[0][c] = EMPTY_STRING;
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
	public String getValueAt(int rowIndex, int columnIndex) {
		return rowData[rowIndex][columnIndex];
	}

}
