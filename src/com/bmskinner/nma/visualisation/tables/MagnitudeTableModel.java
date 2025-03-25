package com.bmskinner.nma.visualisation.tables;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Display magnitude differences between datasets
 * 
 * @author Ben Skinner
 *
 */
public class MagnitudeTableModel extends AbstractTableModel {

	protected static final String EMPTY_STRING = "";

	public static final String DEFAULT_PROBABILITY_FORMAT = "#0.0000";
	private DecimalFormat df = new DecimalFormat(DEFAULT_PROBABILITY_FORMAT);

	private String[] colNames;
	private String[][] rowData;

	/**
	 * Create a magnitude table with datasets in the given order.
	 * 
	 * @param datasets
	 * @param results
	 */
	public MagnitudeTableModel(@Nullable List<IAnalysisDataset> datasets,
			@Nullable List<MagnitudeDatasetResult> results) {

		if (datasets == null || datasets.isEmpty() || results == null || results.isEmpty()) {
			colNames = new String[] { EMPTY_STRING };
			rowData = new String[1][colNames.length];
			for (int c = 0; c < colNames.length; c++) {
				rowData[0][c] = EMPTY_STRING;
			}
			return;
		}

		int rowCount = datasets.size();
		List<String> names = new ArrayList<>();
		names.add(EMPTY_STRING);
		names.addAll(datasets.stream().map(d -> d.getName() + " (denominator)").toList());
		colNames = names.toArray(new String[0]);
		int colCount = colNames.length;

		rowData = new String[rowCount][colCount];
		for (int r = 0; r < rowCount; r++) {
			for (int c = 0; c < colCount; c++) {
				if (c == 0) {
					rowData[r][c] = datasets.get(r).getName() + " (numerator)";
					continue;
				}

				// Diagonal
				if (c == r + 1) {
					rowData[r][c] = EMPTY_STRING;
					continue;
				}

				// Other
				final int rf = r;
				final int cf = c;
				double value = results.stream()
						.filter(m -> m.numerator().equals(datasets.get(rf))
								&& m.denominator().equals(datasets.get(cf - 1)))
						.map(m -> m.value()).findFirst().orElse(-1d);
				rowData[r][c] = df.format(value);
			}
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
