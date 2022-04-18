package com.bmskinner.nuclear_morphology.visualisation.tables;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;

/**
 * Diaplay the results of Wilcoxon rank sum tests
 * 
 * @author ben
 *
 */
public class WilcoxonTableModel extends AbstractTableModel {

	protected static final String EMPTY_STRING = "";
	/** Default format for p-values */
	public static final String DEFAULT_PROBABILITY_FORMAT = "#0.0000";
	private DecimalFormat df = new DecimalFormat(DEFAULT_PROBABILITY_FORMAT);

	private String[] colNames;
	private String[][] rowData;

	public WilcoxonTableModel(@Nullable List<IAnalysisDataset> datasets, @Nullable List<WilcoxDatasetResult> results) {

		super();
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
		names.addAll(datasets.stream().map(IAnalysisDataset::getName).toList());
		colNames = names.toArray(new String[0]);
		int colCount = colNames.length;

		rowData = new String[rowCount][colCount];
		for (int r = 0; r < rowCount; r++) {
			for (int c = 0; c < colCount; c++) {
				if (c == 0) {
					rowData[r][c] = datasets.get(r).getName();
					continue;
				}

				// Diagonal
				if (c == r + 1) {
					rowData[r][c] = EMPTY_STRING;
					continue;
				}

				// Below diagonal r>c
				if (c < r + 1) {
					rowData[r][c] = String.valueOf(getPValue(datasets.get(r), datasets.get(c - 1), results));
					continue;
				}

				// Above diagonal r<c
				rowData[r][c] = String.valueOf(getUValue(datasets.get(r), datasets.get(c - 1), results));
			}
		}
	}

	private String getPValue(IAnalysisDataset d1, IAnalysisDataset d2, List<WilcoxDatasetResult> results) {
		long idVal = WilcoxDatasetResult.toId(d1, d2);
		double val = results.stream().filter(w -> w.id() == idVal).map(w -> w.r().p()).findFirst().orElse(0d);
		return df.format(val);
	}

	private String getUValue(IAnalysisDataset d1, IAnalysisDataset d2, List<WilcoxDatasetResult> results) {
		long idVal = WilcoxDatasetResult.toId(d1, d2);
		double val = results.stream().filter(w -> w.id() == idVal).map(w -> w.r().u()).findFirst().orElse(0d);
		return df.format(val);
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
