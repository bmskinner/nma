package com.bmskinner.nma.visualisation.tables;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.visualisation.datasets.SignalTableCell;

public class NuclearSignalMeasurementsTableModel extends DatasetTableModel {

	private static final long serialVersionUID = 1l;

	private static final Logger LOGGER = Logger
			.getLogger(NuclearSignalMeasurementsTableModel.class.getName());

	private static final List<String> ROW_NAMES = List.of(EMPTY_STRING,
			Labels.Signals.SIGNAL_GROUP_LABEL,
			Labels.Signals.SIGNAL_ID_LABEL,
			Labels.Signals.SIGNALS_LABEL, Labels.Signals.SIGNALS_PER_NUCLEUS);

	private String[] colNames;
	private Object[][] rowData;

	public NuclearSignalMeasurementsTableModel(@Nullable List<IAnalysisDataset> datasets) {

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

		MeasurementScale scale = GlobalOptions.getInstance().getScale();

		// Make an instance of row names
		List<Object> rowNames = new ArrayList<>();
		rowNames.addAll(ROW_NAMES);
		for (Measurement stat : Measurement.getSignalStats()) {
			rowNames.add(stat.label(scale));
		}

		int rowCount = (rowNames.size() * maxChannels) + 1;

		rowData = new Object[rowCount][colCount];
		rowData[0][0] = Labels.Signals.NUMBER_OF_SIGNAL_GROUPS;

		for (int c = 0; c < colCount; c++) {
			// Make the first column of row names
			if (c == 0) {
				for (int r = 1; r < rowCount; r++)
					rowData[r][0] = rowNames.get((r - 1) % rowNames.size());
				continue;
			}

			ICellCollection collection = datasets.get(c - 1).getCollection();
			int signalGroupsInDataset = collection.getSignalManager().getSignalGroupCount();
			rowData[0][c] = signalGroupsInDataset;

			int signalGroupNumber = 0; // number of signal groups from this dataset

			for (UUID id : collection.getSignalManager().getSignalGroupIDs()) {

				int baseIndex = signalGroupNumber * rowNames.size() + 1;

				Color colour = collection.getSignalGroup(id).get().hasColour()
						? collection.getSignalGroup(id).get().getGroupColour().get()
						: ColourSelecter.getColor(signalGroupNumber);

				SignalTableCell cell = new SignalTableCell(id,
						collection.getSignalManager().getSignalGroupName(id),
						colour);

				rowData[baseIndex + 0][c] = Labels.Signals.SIGNAL_COLOUR_LABEL;
				rowData[baseIndex + 1][c] = cell;
				rowData[baseIndex + 2][c] = id;
				rowData[baseIndex + 3][c] = String
						.valueOf(collection.getSignalManager().getSignalCount(id));
				rowData[baseIndex + 4][c] = df
						.format(collection.getSignalManager().getSignalCountPerNucleus(id));

				List<Measurement> measurements = Measurement.getSignalStats();
				for (int m = 0; m < measurements.size(); m++) {
					rowData[baseIndex + 5 + m][c] = df.format(
							collection.getSignalManager()
									.getMedianSignalStatistic(measurements.get(m), scale, id));
				}

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
