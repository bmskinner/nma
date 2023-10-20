package com.bmskinner.nma.visualisation.tables;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.utility.DatasetUtils;

public class AnalysisParametersTableModel extends DatasetTableModel {

	public static final boolean MERGES_RECOVERABLE = true;
	public static final boolean MERGES_NOT_RECOVERABLE = false;

	private static final Logger LOGGER = Logger
			.getLogger(AnalysisParametersTableModel.class.getName());

	private static final long serialVersionUID = 8838655134490670910L;

	private static final List<String> DEFAULT_ROW_NAMES = List.of(
			Labels.AnalysisParameters.IMAGE_PREPROCESSING,
			Labels.AnalysisParameters.NUCLEUS_DETECTION, Labels.AnalysisParameters.NUCLEUS_SIZE,
			Labels.AnalysisParameters.NUCLEUS_CIRCULARITY, Labels.AnalysisParameters.RUN_TIME,
			Labels.AnalysisParameters.COLLECTION_SOURCE, Labels.AnalysisParameters.RULESET_USED,
			Labels.AnalysisParameters.PROFILE_WINDOW, Labels.AnalysisParameters.PIXEL_SCALE,
			Labels.AnalysisParameters.SOFTWARE_VERSION);

	private String[] colNames;
	private Object[][] rowData;

	public AnalysisParametersTableModel(@Nullable List<IAnalysisDataset> datasets,
			boolean recoverable) {

		if (datasets == null) {
			makeEmptyTable();
			return;
		}

		colNames = makeColNames(datasets);
		int colCount = colNames.length + 1;

		List<String> rowNames = new ArrayList<>(DEFAULT_ROW_NAMES);
		if (DatasetUtils.hasMergeSource(datasets) && recoverable)
			rowNames.add(Labels.Merges.RECOVER_SOURCE);

		int rowCount = rowNames.size();

		rowData = new Object[rowCount][colCount];
		for (int r = 0; r < rowCount; r++) {
			for (int c = 0; c < colCount - 1; c++) {
				// First column is row names
				if (c == 0) {
					rowData[r][c] = rowNames.get(r);
					continue;
				}
				createRowData(r, c, datasets);
			}
		}
	}

	public void createRowData(int r, int c, List<IAnalysisDataset> datasets) {
		// Skip datasets with missing options
		Optional<IAnalysisOptions> optional = datasets.get(c - 1)
				.getAnalysisOptions();
		if (optional.isEmpty()) {
			rowData[r][c] = Labels.NA;
			return;
		}

		IAnalysisOptions mainOptions = optional.get();

		Optional<HashOptions> nuclOptional = mainOptions.getNucleusDetectionOptions();
		if (nuclOptional.isEmpty()) {
			rowData[r][c] = Labels.NA;
			return;
		}

		@NonNull
		HashOptions options = nuclOptional.get();

		try {

			rowData[r][c] = switch (r) {
			case 0 -> createImagePreprocessingString(options);
			case 1 -> createNucleusEdgeDetectionString(options);
			case 2 -> createNucleusSizeFilterString(options);
			case 3 -> createNucleusCircFilterString(options);
			case 4 -> createAnalysisRunTimeString(mainOptions);
			case 5 -> createSourceFolderString(datasets.get(c - 1), mainOptions);
			case 6 -> mainOptions.getRuleSetCollection().getName() + " (version "
					+ mainOptions.getRuleSetCollection().getRulesetVersion() + ")";
			case 7 -> String.valueOf(mainOptions.getProfileWindowProportion());
			case 8 -> createPixelScaleString(datasets.get(c - 1));
			case 9 -> datasets.get(c - 1).getVersionCreated().toString();
			case 10 -> datasets.get(c - 1); // only used in merge source table
			default -> EMPTY_STRING;
			};
		} catch (Exception e) {
			LOGGER.fine("Error making analysis options table row " + r + " col " + c + " of "
					+ rowData.length + ": "
					+ e.getMessage());
			rowData[r][c] = "Error";
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

	private String createImagePreprocessingString(@NonNull HashOptions options) {
		StringBuilder builder = new StringBuilder();
		if (options.getBoolean(HashOptions.IS_USE_KUWAHARA))
			builder.append("Kuwahara kernel: " + options.getInt(HashOptions.KUWAHARA_RADIUS_INT)
					+ Io.NEWLINE);
		if (options.getBoolean(HashOptions.IS_USE_FLATTENING))
			builder.append("Flattening threshold: "
					+ options.getInt(HashOptions.FLATTENING_THRESHOLD_INT));
		return builder.toString();
	}

	private String createNucleusEdgeDetectionString(@NonNull HashOptions options) {

		StringBuilder builder = new StringBuilder();

		boolean isCanny = options.getBoolean(HashOptions.IS_USE_CANNY);
		if (isCanny) {
			builder.append("Canny edge detection" + Io.NEWLINE);

			if (options.getBoolean(HashOptions.CANNY_IS_AUTO_THRESHOLD)) {
				builder.append("Auto-threshold" + Io.NEWLINE);
			} else {
				builder.append("Low threshold: "
						+ options.getFloat(HashOptions.CANNY_LOW_THRESHOLD_FLT) + Io.NEWLINE);
				builder.append(
						"High threshold: " + options.getFloat(HashOptions.CANNY_HIGH_THRESHOLD_FLT)
								+ Io.NEWLINE);
			}
			builder.append("Kernel radius: " + options.getFloat(HashOptions.CANNY_KERNEL_RADIUS_FLT)
					+ Io.NEWLINE);
			builder.append("Kernel width: " + options.getInt(HashOptions.CANNY_KERNEL_WIDTH_INT)
					+ Io.NEWLINE);
			builder.append(
					"Closing radius: " + options.getInt(HashOptions.GAP_CLOSING_RADIUS_INT)
							+ Io.NEWLINE);

			if (options.getBoolean(HashOptions.IS_RULESET_EDGE_FILTER))
				builder.append("Poor edge detection filter applied");

		} else {
			builder.append("Threshold: " + options.getInt(HashOptions.THRESHOLD));
			if (options.getBoolean(HashOptions.IS_USE_GAP_CLOSING))
				builder.append(Io.NEWLINE + "Closing radius: "
						+ options.getInt(HashOptions.GAP_CLOSING_RADIUS_INT));
		}

		if (options.getBoolean(HashOptions.IS_USE_WATERSHED)) {
			builder.append(Io.NEWLINE + "Watershed applied");
		}

		return builder.toString();
	}

	private String createNucleusSizeFilterString(@NonNull HashOptions options) {

		StringBuilder builder = new StringBuilder();

		builder.append("Min pixels: " + options.getInt(HashOptions.MIN_SIZE_PIXELS) + Io.NEWLINE
				+ "Max pixels: "
				+ options.getInt(HashOptions.MAX_SIZE_PIXELS));
		return builder.toString();
	}

	private String createNucleusCircFilterString(@NonNull HashOptions options) {

		StringBuilder builder = new StringBuilder();
		DecimalFormat formatter = new DecimalFormat("#.##");

		builder.append("Min: " + formatter.format(options.getDouble(HashOptions.MIN_CIRC))
				+ Io.NEWLINE + "Max: "
				+ formatter.format(options.getDouble(HashOptions.MAX_CIRC)));
		return builder.toString();
	}

	private String createAnalysisRunTimeString(@NonNull IAnalysisOptions options) {

		StringBuilder builder = new StringBuilder();

		long analysisTime = options.getAnalysisTime();
		if (analysisTime > 0) { // stored from 1.14.0
			Instant inst = Instant.ofEpochMilli(analysisTime);
			LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());

			String date = anTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
			String time = anTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
			builder.append(date + Io.NEWLINE + time);
		} else { // fall back to folder name method
			builder.append(Labels.NA);
		}
		return builder.toString();
	}

	private String createSourceFolderString(@NonNull IAnalysisDataset dataset,
			@NonNull IAnalysisOptions options) {
		if (dataset.hasMergeSources()) {
			return dataset.getAllMergeSources().stream()
					.map(d -> d.getAnalysisOptions().get())
					.map(o -> o.getNucleusDetectionFolder().get().getAbsolutePath())
					.collect(Collectors.joining(Io.NEWLINE)) + Io.NEWLINE;
		}
		// Note we add a newline so there is vertical room for word wrapping in the
		// table
		return options.getNucleusDetectionFolder().get().getAbsolutePath() + Io.NEWLINE;
	}

	private String createPixelScaleString(@NonNull IAnalysisDataset dataset) {
		String s = dataset.getCollection().getNuclei().stream()
				.map(Nucleus::getScale)
				.map(Object::toString)
				.distinct()
				.collect(Collectors.joining(", "));
		return s + " pixels per micron";
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

}
