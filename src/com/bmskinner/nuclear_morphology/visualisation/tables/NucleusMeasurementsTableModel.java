package com.bmskinner.nuclear_morphology.visualisation.tables;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.stats.ConfidenceInterval;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class NucleusMeasurementsTableModel extends DatasetTableModel {

	private static final long serialVersionUID = 6546268613206621371L;

	/** Default format for numbers */

	private static final List<String> DEFAULT_ROW_NAMES = List.of(" median", " mean", " S.E.M.", " C.o.V.", " 95% CI");

	private String[] colNames;
	private String[][] rowData;

	@SuppressWarnings("null")
	public NucleusMeasurementsTableModel(@Nullable List<IAnalysisDataset> datasets) {

		if (datasets == null) {
			makeEmptyTable();
			return;
		}

		colNames = makeColNames(datasets);
		int colCount = colNames.length;

		List<String> rowNames = new ArrayList<>();

		List<Measurement> stats = Measurement.getNucleusStats();
		for (Measurement stat : stats) {
			for (String value : DEFAULT_ROW_NAMES) {
				String unitLabel = stat.isDimensionless() ? ""
						: " (" + Measurement.units(GlobalOptions.getInstance().getScale(), stat.getDimension()) + ")";
				rowNames.add(stat + value + unitLabel);
			}
		}
		int rowCount = rowNames.size();

		rowData = new String[rowCount][colCount];
		for (int r = 0; r < rowCount; r += DEFAULT_ROW_NAMES.size()) {
			Measurement m = stats.get(r % DEFAULT_ROW_NAMES.size());
			for (int c = 0; c < colCount; c++) {
				if (c == 0) {
					for (int k = 0; k < rowNames.size(); k++)
						rowData[k][c] = rowNames.get(k);
					continue;
				}

				// Add the calculated values in each dataset as a batch

				double[] vals = datasets.get(c - 1).getCollection().getRawValues(m, CellularComponent.NUCLEUS,
						GlobalOptions.getInstance().getScale());
				double mean = DoubleStream.of(vals).average().orElse(0);
				double sem = Stats.stderr(vals);
				double cv = Stats.stdev(vals) / mean;

				double median = Stats.quartile(vals, Stats.MEDIAN);

				ConfidenceInterval ci = new ConfidenceInterval(vals, 0.95);
				String ciString = df.format(mean) + " ± " + df.format(ci.getSize().doubleValue());

				rowData[r][c] = df.format(median);
				rowData[r + 1][c] = df.format(mean);
				rowData[r + 2][c] = df.format(sem);
				rowData[r + 3][c] = df.format(cv);
				rowData[r + 4][c] = ciString;

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
