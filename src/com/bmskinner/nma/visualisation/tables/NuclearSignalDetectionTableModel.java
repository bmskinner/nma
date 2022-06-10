package com.bmskinner.nma.visualisation.tables;

import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.signals.SignalDetectionMode;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.visualisation.datasets.SignalTableCell;

public class NuclearSignalDetectionTableModel extends DatasetTableModel {

	private static final String VALUE_MISSING_LBL = "Value missing";

	private static final long serialVersionUID = 1l;

	private static final Logger LOGGER = Logger
			.getLogger(NuclearSignalDetectionTableModel.class.getName());

	private static final List<String> ROW_NAMES = List.of(EMPTY_STRING,
			Labels.Signals.SIGNAL_GROUP_LABEL,
			Labels.Signals.SIGNAL_CHANNEL_LABEL, Labels.Signals.SIGNAL_SOURCE_LABEL,
			Labels.Signals.THRESHOLD_LBL,
			Labels.Signals.MIN_SIZE_LBL, Labels.Signals.MAX_SIZE_LBL, Labels.Signals.MIN_CIRC_LBL,
			Labels.Signals.MAX_CIRC_LBL, Labels.Signals.DETECTION_MODE_LBL);

	private String[] colNames;
	private Object[][] rowData;

	public NuclearSignalDetectionTableModel(@Nullable List<IAnalysisDataset> datasets) {

		if (datasets == null) {
			makeEmptyTable();
			return;
		}

		// find the collection with the most channels
		// this defines the number of rows
		int maxChannels = datasets.stream()
				.mapToInt(d -> d.getCollection().getSignalManager().getSignalGroupCount())
				.max().orElse(0);

		if (maxChannels == 0) {
			makeEmptyTable();
			return;
		}

		colNames = makeColNames(datasets);
		int colCount = colNames.length;
		int rowCount = (ROW_NAMES.size() * maxChannels) + 1;

		rowData = new Object[rowCount][colCount];
		rowData[0][0] = Labels.Signals.NUMBER_OF_SIGNAL_GROUPS;

		for (int c = 0; c < colCount; c++) {
			if (c == 0) {
				for (int r = 1; r < rowCount; r++)
					rowData[r][0] = ROW_NAMES.get((r - 1) % ROW_NAMES.size());
				continue;
			}

			IAnalysisDataset d = datasets.get(c - 1);

			ICellCollection collection = d.getCollection();
			int signalGroupsInDataset = collection.getSignalManager().getSignalGroupCount();

			rowData[0][c] = String.valueOf(signalGroupsInDataset);

			int signalGroupNumber = 0; // number of signal groups from this dataset

			for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

				LOGGER.fine("Making for signal group " + signalGroup);

				int baseIndex = signalGroupNumber * ROW_NAMES.size() + 1;

				Color colour = collection.getSignalGroup(signalGroup).get().hasColour()
						? collection.getSignalGroup(signalGroup).get().getGroupColour().get()
						: ColourSelecter.getColor(signalGroupNumber);

				SignalTableCell cell = new SignalTableCell(signalGroup,
						collection.getSignalManager().getSignalGroupName(signalGroup), colour);

				Optional<IAnalysisOptions> op = d.getAnalysisOptions();
				if (!op.isPresent()) {
					for (int i = 0; i < ROW_NAMES.size(); i++) {
						rowData[i + baseIndex][c] = EMPTY_STRING;
					}
					continue;
				}

				for (String s : op.get().getDetectionOptionTypes()) {
					LOGGER.fine("Dataset has " + s);
				}

				HashOptions ns = op.get().getNuclearSignalOptions(signalGroup)
						.orElseThrow(IllegalArgumentException::new);

				rowData[baseIndex + 0][c] = Labels.Signals.SIGNAL_COLOUR_LABEL;
				rowData[baseIndex + 1][c] = cell;
				rowData[baseIndex + 2][c] = makeChannelLabel(d, ns);
				rowData[baseIndex + 3][c] = makeSignalFolderLabel(d, signalGroup, op, ns);
				rowData[baseIndex + 4][c] = makeSignalThresholdLabel(d, ns, signalGroup);
				rowData[baseIndex + 5][c] = makeMinSizeLabel(d, ns);
				rowData[baseIndex + 6][c] = makeMaxFractionLabel(d, ns);
				rowData[baseIndex + 7][c] = makeMinCircLabel(d, ns);
				rowData[baseIndex + 8][c] = makeMaxCircLabel(d, ns);
				rowData[baseIndex + 9][c] = makeDetctionModeLabel(d, ns);

				signalGroupNumber++;
			}

			/*
			 * If the number of signal groups in the dataset is less than the size of the
			 * table, the remainder should be filled with blank cells
			 */

			if (signalGroupNumber < signalGroupsInDataset) {

				// There will be empty rows in the table. Fill the blanks
				for (int i = signalGroupNumber * ROW_NAMES.size()
						+ 1; i <= signalGroupsInDataset; i++) {
					rowData[i][c] = EMPTY_STRING;

				}
			}

		}

	}

	private Object makeChannelLabel(IAnalysisDataset d, HashOptions ns) {
		if (ns.hasInt(HashOptions.CHANNEL))
			return ns.getInt(HashOptions.CHANNEL);
		if (d.hasMergeSources())
			return Labels.NA_MERGE;
		return VALUE_MISSING_LBL;
	}

	private Object makeMinSizeLabel(IAnalysisDataset d, HashOptions ns) {
		if (ns.hasInt(HashOptions.MIN_SIZE_PIXELS))
			return ns.getInt(HashOptions.MIN_SIZE_PIXELS);
		if (d.hasMergeSources())
			return Labels.NA_MERGE;
		return VALUE_MISSING_LBL;
	}

	private Object makeMaxFractionLabel(IAnalysisDataset d, HashOptions ns) {
		if (ns.hasDouble(HashOptions.SIGNAL_MAX_FRACTION))
			return df
					.format(ns.getDouble(HashOptions.SIGNAL_MAX_FRACTION));
		if (d.hasMergeSources())
			return Labels.NA_MERGE;
		return VALUE_MISSING_LBL;
	}

	private Object makeMinCircLabel(IAnalysisDataset d, HashOptions ns) {
		if (ns.hasDouble(HashOptions.MIN_CIRC))
			return df
					.format(ns.getDouble(HashOptions.MIN_CIRC));
		if (d.hasMergeSources())
			return Labels.NA_MERGE;
		return VALUE_MISSING_LBL;
	}

	private Object makeMaxCircLabel(IAnalysisDataset d, HashOptions ns) {
		if (ns.hasDouble(HashOptions.MAX_CIRC))
			return df
					.format(ns.getDouble(HashOptions.MAX_CIRC));
		if (d.hasMergeSources())
			return Labels.NA_MERGE;
		return VALUE_MISSING_LBL;
	}

	private Object makeDetctionModeLabel(IAnalysisDataset d, HashOptions ns) {
		if (d.hasMergeSources())
			return Labels.NA_MERGE;
		StringBuilder builder = new StringBuilder();

		if (ns.hasString(HashOptions.SIGNAL_DETECTION_MODE_KEY))
			builder.append(ns.getString(HashOptions.SIGNAL_DETECTION_MODE_KEY));
		else
			builder.append(VALUE_MISSING_LBL);

		if (ns.getBoolean(HashOptions.IS_USE_WATERSHED))
			builder.append(" + Watershed");

		return builder.toString();
	}

	private Object makeSignalFolderLabel(IAnalysisDataset d, UUID signalGroup,
			Optional<IAnalysisOptions> op,
			HashOptions ns) {
		Optional<File> folder = op.get().getNuclearSignalDetectionFolder(signalGroup);
		return folder.isPresent() ? folder.get().getAbsoluteFile()
				: d.hasMergeSources() ? Labels.NA_MERGE : VALUE_MISSING_LBL;
	}

	private Object makeSignalThresholdLabel(IAnalysisDataset d, HashOptions ns, UUID signalGroup) {
		if (ns.hasString(HashOptions.SIGNAL_DETECTION_MODE_KEY)
				&& ns.hasInt(HashOptions.THRESHOLD)) {

			String mode = ns.getString(HashOptions.SIGNAL_DETECTION_MODE_KEY);
			return SignalDetectionMode.FORWARD.name().equals(mode)
					? ns.getInt(HashOptions.THRESHOLD)
					: d.hasMergeSources() ? Labels.NA_MERGE : "Variable";
		}
		if (d.hasMergeSources()) {
			return Labels.NA_MERGE + " and sources have multiple values";

			// Note - this does not work because the merged signal group has a different id
			// to the sources
//			return "Merge:" + Io.NEWLINE + d.getMergeSources().stream()
//					.filter(c -> c.getAnalysisOptions().get()
//							.hasNuclearSignalDetectionOptions(signalGroup))
//					.map(c -> c.getAnalysisOptions().get().getNuclearSignalOptions(signalGroup))
//					.map(o -> String.valueOf(o.get().getInt(HashOptions.THRESHOLD)))
//					.collect(Collectors.joining(Io.NEWLINE));
		}

		return VALUE_MISSING_LBL;
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
	public Object getValueAt(int rowIndex, int columnIndex) {
		return rowData[rowIndex][columnIndex];
	}

}
