package com.bmskinner.nuclear_morphology.visualisation.tables;

import java.io.File;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.io.Io;

public class AnalysisParametersTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8838655134490670910L;

	protected static final String EMPTY_STRING = "";

	private static final List<String> DEFAULT_ROW_NAMES = List.of(Labels.AnalysisParameters.IMAGE_PREPROCESSING,
			Labels.AnalysisParameters.NUCLEUS_DETECTION, Labels.AnalysisParameters.NUCLEUS_SIZE,
			Labels.AnalysisParameters.NUCLEUS_CIRCULARITY, Labels.AnalysisParameters.RUN_TIME,
			Labels.AnalysisParameters.COLLECTION_SOURCE, Labels.AnalysisParameters.RULESET_USED,
			Labels.AnalysisParameters.PROFILE_WINDOW, Labels.AnalysisParameters.SOFTWARE_VERSION);

	private String[] colNames;
	private String[][] rowData;

	@SuppressWarnings("null")
	public AnalysisParametersTableModel(@Nullable List<IAnalysisDataset> datasets) {

		if (datasets == null) {
			makeEmptyTable();
			return;
		}

		// only display if there are options available
		// This may not be the case for a merged dataset or its children
		List<IAnalysisDataset> usableDatasets = datasets.stream().filter(IAnalysisDataset::hasAnalysisOptions).toList();

		if (usableDatasets.isEmpty()) {
			makeEmptyTable();
			return;
		}

		colNames = makeColNames(usableDatasets);
		int colCount = colNames.length + 1;

		List<String> rowNames = new ArrayList<>(DEFAULT_ROW_NAMES);
		if (usableDatasets.stream().anyMatch(d -> d.hasMergeSources()))
			rowNames.add(Labels.Merges.RECOVER_SOURCE);
		int rowCount = rowNames.size();

		rowData = new String[rowCount][colCount];
		for (int r = 0; r < rowCount; r++) {
			for (int c = 0; c < colCount - 1; c++) {
				if (c == 0) {
					rowData[r][c] = rowNames.get(r);
					continue;
				}

				rowData[r][c] = switch (r) {
				case 0 -> createImagePreprocessingString(usableDatasets.get(c - 1));
				case 1 -> createNucleusEdgeDetectionString(usableDatasets.get(c - 1));
				case 2 -> createNucleusSizeFilterString(usableDatasets.get(c - 1));
				case 3 -> createNucleusCircFilterString(usableDatasets.get(c - 1));
				case 4 -> createAnalysisRunTimeString(usableDatasets.get(c - 1));
				case 5 -> createSourceFolderString(usableDatasets.get(c - 1));
				case 6 -> usableDatasets.get(c - 1).getAnalysisOptions().get().getRuleSetCollection().getName();
				case 7 -> String
						.valueOf(usableDatasets.get(c - 1).getAnalysisOptions().get().getProfileWindowProportion());
				case 8 -> usableDatasets.get(c - 1).getVersionCreated().toString();
				default -> EMPTY_STRING;
				};

			}
		}
	}

	private void makeEmptyTable() {
		colNames = new String[] { EMPTY_STRING };
		rowData = new String[1][colNames.length];
		for (int c = 0; c < colNames.length; c++) {
			rowData[0][c] = EMPTY_STRING;
		}
	}

	private String[] makeColNames(@NonNull List<IAnalysisDataset> datasets) {
		List<String> names = new ArrayList<>();
		names.add(EMPTY_STRING);
		names.addAll(datasets.stream().map(IAnalysisDataset::getName).toList());
		return names.toArray(new String[0]);
	}

	private String createImagePreprocessingString(@NonNull IAnalysisDataset dataset) {

		HashOptions options = dataset.getAnalysisOptions().get().getNucleusDetectionOptions().get();

		StringBuilder builder = new StringBuilder();
		if (options == null) {
			builder.append(Labels.NA);
			return builder.toString();
		}

		if (options.getBoolean(HashOptions.IS_USE_KUWAHARA))
			builder.append("Kuwahara kernel: " + options.getInt(HashOptions.KUWAHARA_RADIUS_INT) + Io.NEWLINE);
		if (options.getBoolean(HashOptions.IS_USE_FLATTENING))
			builder.append("Flattening threshold: " + options.getInt(HashOptions.FLATTENING_THRESHOLD_INT));
		return builder.toString();
	}

	private String createNucleusEdgeDetectionString(@NonNull IAnalysisDataset dataset) {

		HashOptions options = dataset.getAnalysisOptions().get().getNucleusDetectionOptions().get();

		StringBuilder builder = new StringBuilder();
		if (options == null)
			return builder.toString();

		boolean isCanny = options.getBoolean(HashOptions.IS_USE_CANNY);
		if (isCanny) {
			builder.append("Canny edge detection" + Io.NEWLINE);

			if (options.getBoolean(HashOptions.CANNY_IS_AUTO_THRESHOLD)) {
				builder.append("Auto-threshold" + Io.NEWLINE);
			} else {
				builder.append("Low threshold: " + options.getFloat(HashOptions.CANNY_LOW_THRESHOLD_FLT) + Io.NEWLINE);
				builder.append(
						"High threshold: " + options.getFloat(HashOptions.CANNY_HIGH_THRESHOLD_FLT) + Io.NEWLINE);
			}
			builder.append("Kernel radius: " + options.getFloat(HashOptions.CANNY_KERNEL_RADIUS_FLT) + Io.NEWLINE);
			builder.append("Kernel width: " + options.getInt(HashOptions.CANNY_KERNEL_WIDTH_INT) + Io.NEWLINE);
			builder.append("Closing radius: " + options.getInt(HashOptions.CANNY_CLOSING_RADIUS_INT));
		} else {
			builder.append("Threshold: " + options.getInt(HashOptions.THRESHOLD));
		}
		return builder.toString();
	}

	private String createNucleusSizeFilterString(@NonNull IAnalysisDataset dataset) {

		HashOptions options = dataset.getAnalysisOptions().get().getNucleusDetectionOptions().get();

		StringBuilder builder = new StringBuilder();
		if (options == null) {
			builder.append(Labels.NA);
			return builder.toString();
		}
		builder.append("Min pixels: " + options.getInt(HashOptions.MIN_SIZE_PIXELS) + Io.NEWLINE + "Max pixels: "
				+ options.getInt(HashOptions.MAX_SIZE_PIXELS));
		return builder.toString();
	}

	private String createNucleusCircFilterString(@NonNull IAnalysisDataset dataset) {

		HashOptions options = dataset.getAnalysisOptions().get().getNucleusDetectionOptions().get();

		StringBuilder builder = new StringBuilder();
		DecimalFormat formatter = new DecimalFormat("#.##");
		if (options == null) {
			builder.append(Labels.NA);
			return builder.toString();
		}
		builder.append("Min: " + formatter.format(options.getDouble(HashOptions.MIN_CIRC)) + Io.NEWLINE + "Max: "
				+ formatter.format(options.getDouble(HashOptions.MAX_CIRC)));
		return builder.toString();
	}

	private String createAnalysisRunTimeString(@NonNull IAnalysisDataset dataset) {

		IAnalysisOptions options = dataset.getAnalysisOptions().get();

		StringBuilder builder = new StringBuilder();
		if (options == null) {
			builder.append(Labels.NA);
			return builder.toString();
		}
		long analysisTime = options.getAnalysisTime();
		if (analysisTime > 0) { // stored from 1.14.0
			Instant inst = Instant.ofEpochMilli(analysisTime);
			LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());

			String date = anTime.format(DateTimeFormatter.ofPattern("dd MMMM YYYY"));
			String time = anTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
			builder.append(date + Io.NEWLINE + time);
		} else { // fall back to folder name method
			builder.append(Labels.NA);
		}
		return builder.toString();
	}

	private String createSourceFolderString(@NonNull IAnalysisDataset dataset) {
		HashOptions options = dataset.getAnalysisOptions().get().getNucleusDetectionOptions().get();
		return new File(options.getString(HashOptions.DETECTION_FOLDER)).getAbsolutePath();
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
