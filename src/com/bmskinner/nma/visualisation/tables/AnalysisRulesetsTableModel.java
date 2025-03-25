package com.bmskinner.nma.visualisation.tables;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.io.Io;

/**
 * Table model for rulesets table. This table should scale to arbitrary numbers
 * of landmarks in datasets and sensibly arrange datasets with differing numbers
 * of landmarks.
 * 
 * @author Ben Skinner
 * @since 2.2.0
 *
 */
public class AnalysisRulesetsTableModel extends DatasetTableModel {

	public static final boolean MERGES_RECOVERABLE = true;
	public static final boolean MERGES_NOT_RECOVERABLE = false;

	/**
	 * The number of rows that are static (i.e. not landmarks, orientation marks or
	 * measurements which can have different amounts between datasets)
	 */
	private static final int N_STATIC_ROWS = 2;

	private static final Logger LOGGER = Logger
			.getLogger(AnalysisRulesetsTableModel.class.getName());

	private static final long serialVersionUID = 8838655134490670910L;

	private String[] colNames;
	private Object[][] rowData;

	private int maxLandmarkCount;
	private int maxOrientationCount;
	private int maxMeasurementCount;

	public AnalysisRulesetsTableModel(@Nullable List<IAnalysisDataset> datasets) {

		if (datasets == null) {
			makeEmptyTable();
			return;
		}

		// Number of distinct columns - one per dataset
		colNames = makeColNames(datasets);
		int colCount = colNames.length + 1;

		// Distinct rows - one per landmark
		maxLandmarkCount = datasets.stream()
				.map(IAnalysisDataset::getAnalysisOptions)
				.map(Optional::get)
				.map(IAnalysisOptions::getRuleSetCollection)
				.map(RuleSetCollection::getLandmarks)
				.mapToInt(Set::size)
				.max().orElse(0);

		// Distinct rows - one per orientation mark
		maxOrientationCount = datasets.stream()
				.map(IAnalysisDataset::getAnalysisOptions)
				.map(Optional::get)
				.map(IAnalysisOptions::getRuleSetCollection)
				.map(RuleSetCollection::getOrientionMarks)
				.mapToInt(Set::size)
				.max().orElse(0);

		maxMeasurementCount = datasets.stream()
				.map(IAnalysisDataset::getAnalysisOptions)
				.map(Optional::get)
				.map(IAnalysisOptions::getRuleSetCollection)
				.map(RuleSetCollection::getMeasurableValues)
				.mapToInt(Set::size)
				.max().orElse(0);

		int rowCount = maxLandmarkCount + maxOrientationCount + maxMeasurementCount + N_STATIC_ROWS;

		rowData = new Object[rowCount][colCount];
		for (int r = 0; r < rowCount; r++) {
			for (int c = 0; c < colCount - 1; c++) {
//				// First column is row names
				if (c == 0) {
					if (r == 0)
						rowData[r][c] = Labels.AnalysisRulesets.RULESET_NAME;
					if (r == 1)
						rowData[r][c] = Labels.AnalysisRulesets.RULE_APPLICATON_TYPE;
					if (r >= N_STATIC_ROWS && r < maxLandmarkCount + N_STATIC_ROWS)
						rowData[r][c] = Labels.AnalysisRulesets.LANDMARK_NAME;
					if (r >= maxLandmarkCount + N_STATIC_ROWS
							&& r < maxLandmarkCount + maxOrientationCount + N_STATIC_ROWS)
						rowData[r][c] = Labels.AnalysisRulesets.ORIENTATION_NAME;
					if (r >= maxLandmarkCount + maxOrientationCount + N_STATIC_ROWS
							&& r < maxLandmarkCount + maxOrientationCount + maxMeasurementCount
									+ N_STATIC_ROWS)
						rowData[r][c] = Labels.AnalysisRulesets.MEASUREMENT_NAME;
					continue;
				}

				if (r < N_STATIC_ROWS)
					createRowData(r, c, datasets);
				if (r >= N_STATIC_ROWS && r < maxLandmarkCount + N_STATIC_ROWS)
					createLandmarkRowData(r, c, datasets);
				if (r >= maxLandmarkCount + N_STATIC_ROWS
						&& r < maxLandmarkCount + maxOrientationCount + N_STATIC_ROWS)
					createOrientationRowData(r, c, datasets);
				if (r >= maxLandmarkCount + maxOrientationCount + N_STATIC_ROWS
						&& r < maxLandmarkCount + maxOrientationCount + maxMeasurementCount
								+ N_STATIC_ROWS)
					createMeasurementRowData(r, c, datasets);
			}
		}
	}

	private void createLandmarkRowData(int r, int c, List<IAnalysisDataset> datasets) {
		// Skip datasets with missing options
		Optional<IAnalysisOptions> optional = datasets.get(c - 1)
				.getAnalysisOptions();
		if (optional.isEmpty()) {
			rowData[r][c] = Labels.NA;
			return;
		}

		IAnalysisOptions mainOptions = optional.get();
		RuleSetCollection rsc = mainOptions.getRuleSetCollection();
		List<Landmark> lms = List.copyOf(rsc.getLandmarks());

		int lmRow = r - N_STATIC_ROWS;

		// If there are no more landmarks to add, skip row
		if (lms.size() < maxLandmarkCount && lmRow >= lms.size()) {
			rowData[r][c] = EMPTY_STRING;
			return;
		}

		try {

			rowData[r][c] = lms.get(lmRow).toString() + Io.NEWLINE
					+ rsc.getRuleSets(lms.get(lmRow)).toString();

		} catch (Exception e) {
			LOGGER.fine("Error making analysis rulesets table row " + r + " of "
					+ rowData.length + ", col " + c + " of " + colNames.length + ": "
					+ e.getMessage());
			rowData[r][c] = "Error";
		}
	}

	private void createOrientationRowData(int r, int c, List<IAnalysisDataset> datasets) {
		// Skip datasets with missing options
		Optional<IAnalysisOptions> optional = datasets.get(c - 1)
				.getAnalysisOptions();
		if (optional.isEmpty()) {
			rowData[r][c] = Labels.NA;
			return;
		}

		IAnalysisOptions mainOptions = optional.get();
		RuleSetCollection rsc = mainOptions.getRuleSetCollection();

		List<OrientationMark> oms = List.copyOf(rsc.getOrientionMarks());

		int omRow = r - maxLandmarkCount - N_STATIC_ROWS;

		// If there are no more landmarks to add, skip row
		if (oms.size() < maxOrientationCount && omRow >= oms.size()) {
			rowData[r][c] = EMPTY_STRING;
			return;
		}

		try {
			rowData[r][c] = oms.get(omRow).toString() + ": " +
					rsc.getLandmark(oms.get(omRow)).get().getName();

		} catch (Exception e) {
			LOGGER.fine("Error making analysis rulesets table row " + r + " col " + c + " of "
					+ rowData.length + ": "
					+ e.getMessage());
			rowData[r][c] = "Error";
		}
	}

	private void createMeasurementRowData(int r, int c, List<IAnalysisDataset> datasets) {
		// Skip datasets with missing options
		Optional<IAnalysisOptions> optional = datasets.get(c - 1)
				.getAnalysisOptions();
		if (optional.isEmpty()) {
			rowData[r][c] = Labels.NA;
			return;
		}

		IAnalysisOptions mainOptions = optional.get();
		RuleSetCollection rsc = mainOptions.getRuleSetCollection();

		List<Measurement> vals = new ArrayList<>(rsc.getMeasurableValues());
		vals.sort((c1, c2) -> c1.toString().compareTo(c2.toString()));

		int mesRow = r - maxLandmarkCount - maxOrientationCount - N_STATIC_ROWS;

		// If there are no more to add, skip row
		if (vals.size() < maxMeasurementCount && mesRow >= vals.size()) {
			rowData[r][c] = EMPTY_STRING;
			return;
		}

		try {
			rowData[r][c] = vals.get(mesRow).toString();

		} catch (Exception e) {
			LOGGER.fine("Error making analysis rulesets table row " + r + " col " + c + " of "
					+ rowData.length + ": "
					+ e.getMessage());
			rowData[r][c] = "Error";
		}
	}

	private void createRowData(int r, int c, List<IAnalysisDataset> datasets) {
		// Skip datasets with missing options
		Optional<IAnalysisOptions> optional = datasets.get(c - 1)
				.getAnalysisOptions();
		if (optional.isEmpty()) {
			rowData[r][c] = Labels.NA;
			return;
		}

		IAnalysisOptions mainOptions = optional.get();
		RuleSetCollection rsc = mainOptions.getRuleSetCollection();
		try {

			rowData[r][c] = switch (r) {
			case 0 -> rsc.getName() + " (version " + rsc.getRulesetVersion() + ")";
			case 1 -> rsc.getApplicationType().toString();
			default -> EMPTY_STRING;
			};

		} catch (Exception e) {
			LOGGER.fine("Error making analysis rulesets table row " + r + " col " + c + " of "
					+ rowData.length + ": "
					+ e.getMessage());
			rowData[r][c] = "Error";
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
	public Object getValueAt(int rowIndex, int columnIndex) {
		return rowData[rowIndex][columnIndex];
	}

	@Override
	protected void makeEmptyTable() {
		colNames = new String[] { EMPTY_STRING };
		rowData = new String[1][colNames.length];
		for (int c = 0; c < colNames.length; c++) {
			rowData[0][c] = EMPTY_STRING;
		}
	}
}