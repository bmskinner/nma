package com.bmskinner.nma.visualisation.tables;

import java.awt.Color;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Collection;
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
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.visualisation.datasets.SignalTableCell;

public class NuclearSignalDetectionTableModel extends DatasetTableModel {

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

				HashOptions ns = op.get().getNuclearSignalOptions(signalGroup)
						.orElseThrow(IllegalArgumentException::new);
				Object signalThreshold = ns.getString(HashOptions.SIGNAL_DETECTION_MODE_KEY)
						.equals(SignalDetectionMode.FORWARD.name())
								? ns.getInt(HashOptions.THRESHOLD)
								: "Variable";

				Optional<File> folder = op.get().getNuclearSignalDetectionFolder(signalGroup);

				rowData[baseIndex + 0][c] = Labels.Signals.SIGNAL_COLOUR_LABEL;
				rowData[baseIndex + 1][c] = cell;
				rowData[baseIndex + 2][c] = ns.getInt(HashOptions.CHANNEL);
				rowData[baseIndex + 3][c] = folder.isPresent() ? folder.get().getAbsoluteFile()
						: Labels.NA_MERGE;
				rowData[baseIndex + 4][c] = signalThreshold;
				rowData[baseIndex + 5][c] = ns.getInt(HashOptions.MIN_SIZE_PIXELS);
				rowData[baseIndex + 6][c] = df
						.format(ns.getDouble(HashOptions.SIGNAL_MAX_FRACTION));
				rowData[baseIndex + 7][c] = df.format(ns.getDouble(HashOptions.MIN_CIRC));
				rowData[baseIndex + 8][c] = df.format(ns.getDouble(HashOptions.MAX_CIRC));
				rowData[baseIndex + 9][c] = ns.getString(HashOptions.SIGNAL_DETECTION_MODE_KEY);

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

	private void addMergedSignalData(List<Object> rowData, IAnalysisDataset dataset,
			int signalGroupCount,
			int rowsPerSignalGroup) {
		ICellCollection collection = dataset.getCollection();
		Collection<ISignalGroup> signalGroups = collection.getSignalManager().getSignalGroups();

		int j = 0;
		for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

			try {
				Optional<Color> c = collection.getSignalGroup(signalGroup).get().getGroupColour();
				Color colour = c.isPresent() ? c.get() : ColourSelecter.getColor(j);

				SignalTableCell cell = new SignalTableCell(signalGroup,
						collection.getSignalManager().getSignalGroupName(signalGroup), colour);

				rowData.add(EMPTY_STRING);// empty row for colour
				rowData.add(cell); // group name

				for (int i = 0; i < rowsPerSignalGroup - 2; i++) { // rest are
																	// NA
					rowData.add(Labels.NA + " - merge");
				}

			} finally {
				j++;
			}
		}

		// Add blank rows for any empty spaces in the table
		int remaining = signalGroupCount - signalGroups.size();
		for (int i = 0; i < remaining; i++) {
			for (int k = 0; k < rowsPerSignalGroup; k++) {
				rowData.add(EMPTY_STRING);
			}
		}
	}

	/**
	 * Fill a list with rows describing each signal group in a dataset
	 * 
	 * @param rowData            the list to add rows to
	 * @param dataset            the dataset with signals
	 * @param signalGroupCount   the total number of signal groups in the table
	 * @param rowsPerSignalGroup the number of rows each signal group requires
	 * @throws MissingOptionException
	 */
	private void addNonMergedSignalData(List<Object> rowData, IAnalysisDataset dataset,
			int signalGroupCount,
			int rowsPerSignalGroup) throws MissingOptionException {

		ICellCollection collection = dataset.getCollection();
		int signalGroupNumber = 0; // Store the number of signal groups
									// processed from this dataset

		int indexInTable = 0;
		for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

			signalGroupNumber++;
			Optional<Color> c = collection.getSignalGroup(signalGroup).get().getGroupColour();
			Color colour = c.isPresent() ? c.get() : ColourSelecter.getColor(indexInTable);
			indexInTable++;

			SignalTableCell cell = new SignalTableCell(signalGroup,
					collection.getSignalManager().getSignalGroupName(signalGroup), colour);

			Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
			if (!op.isPresent()) {
				for (int i = 0; i < rowsPerSignalGroup; i++) {
					rowData.add(EMPTY_STRING);
				}
				continue;
			}

			HashOptions ns = op.get().getNuclearSignalOptions(signalGroup)
					.orElseThrow(MissingOptionException::new);
			Object signalThreshold = ns.getString(HashOptions.SIGNAL_DETECTION_MODE_KEY)
					.equals(SignalDetectionMode.FORWARD.name()) ? ns.getInt(HashOptions.THRESHOLD)
							: "Variable";

			DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);

			rowData.add(Labels.Signals.SIGNAL_COLOUR_LABEL);
			rowData.add(cell);
			rowData.add(ns.getInt(HashOptions.CHANNEL));
			rowData.add(ns.getString(HashOptions.DETECTION_FOLDER));
			rowData.add(signalThreshold);
			rowData.add(ns.getInt(HashOptions.MIN_SIZE_PIXELS));
			rowData.add(df.format(ns.getDouble(HashOptions.SIGNAL_MAX_FRACTION)));
			rowData.add(df.format(ns.getDouble(HashOptions.MIN_CIRC)));
			rowData.add(df.format(ns.getDouble(HashOptions.MAX_CIRC)));
			rowData.add(ns.getString(HashOptions.SIGNAL_DETECTION_MODE_KEY));

		}

		/*
		 * If the number of signal groups in the dataset is less than the size of the
		 * table, the remainder should be filled with blank cells
		 */

		if (signalGroupNumber < signalGroupCount) {

			// There will be empty rows in the table. Fill the blanks
			for (int i = signalGroupNumber + 1; i <= signalGroupCount; i++) {
				for (int k = 0; k < rowsPerSignalGroup; k++) {
					rowData.add(EMPTY_STRING);
				}
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
	public Object getValueAt(int rowIndex, int columnIndex) {
		return rowData[rowIndex][columnIndex];
	}

}
