/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.visualisation.tables;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.ICytoplasm;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.components.signals.IShellResult;
import com.bmskinner.nma.components.signals.ISignalCollection;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.visualisation.datasets.AbstractCellDatasetCreator;
import com.bmskinner.nma.visualisation.datasets.SignalTableCell;
import com.bmskinner.nma.visualisation.options.DisplayOptions;

/**
 * Generate the stats tables for a single cell
 * 
 * @author bms41
 *
 */
public class CellTableDatasetCreator extends AbstractCellDatasetCreator {

	private static final Logger LOGGER = Logger.getLogger(CellTableDatasetCreator.class.getName());

	public CellTableDatasetCreator(final DisplayOptions options, final ICell c) {
		super(options, c);
	}

	/**
	 * Create a table of stats for the given cell.
	 * 
	 * @return a table model
	 */
	public TableModel createCellInfoTable() {

		if (!options.hasDatasets())
			return AbstractTableCreator.createBlankTable();

		if (options.isMultipleDatasets())
			return AbstractTableCreator.createBlankTable();

		if (!options.hasCell())
			return AbstractTableCreator.createBlankTable();

		try {

			IAnalysisDataset d = options.firstDataset();
			DefaultTableModel model = new DefaultTableModel();

			List<Object> fieldNames = new ArrayList<>(0);
			List<Object> rowData = new ArrayList<>(0);

			// find the collection with the most channels
			// this defines the number of rows

			if (cell.hasCytoplasm()) {
				addCytoplasmDataToTable(fieldNames, rowData, cell, d);
			}

			fieldNames.add("Number of nuclei");
			rowData.add(cell.getMeasurement(Measurement.CELL_NUCLEUS_COUNT));

			int nucleusNumber = 0;
			for (Nucleus n : cell.getNuclei()) {
				nucleusNumber++;
				fieldNames.add("Nucleus " + nucleusNumber);
				rowData.add("");
				addNuclearDataToTable(fieldNames, rowData, n, d);

			}

			model.addColumn("", fieldNames.toArray(new Object[0]));
			model.addColumn("Info", rowData.toArray(new Object[0]));

			return model;
		} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
			LOGGER.log(Level.SEVERE, "Error creating table model", e);
			return AbstractTableCreator.createBlankTable();
		}
	}

	/**
	 * Create a table model showing the distances between all signals in a cell
	 * 
	 * @param options
	 * @return
	 */
	public TableModel createPairwiseSignalDistanceTable() {

		if (!options.isSingleDataset()) {
			return AbstractTableCreator.createBlankTable();
		}

		IAnalysisDataset d = options.firstDataset();
		DefaultTableModel model = new DefaultTableModel();

		List<Object> columnNames = new ArrayList<Object>(0);

		ISignalCollection sc = cell.getPrimaryNucleus().getSignalCollection();

		if (sc.numberOfSignals() == 0) {
			return AbstractTableCreator.createBlankTable();
		}
		DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
		// Make the first column, of names
		for (UUID id : sc.getSignalGroupIds()) {

			String signalName = d.getCollection().getSignalGroup(id).get().getGroupName();

			List<INuclearSignal> signalsRow = sc.getSignals(id);
			int sigNumber = 0;

			for (INuclearSignal row : signalsRow) {

				columnNames.add(signalName + "_Sig_" + sigNumber);
				sigNumber++;
			}

		}
		model.addColumn(Labels.Signals.SIGNAL_LABEL_SINGULAR, columnNames.toArray(new Object[0]));

		// Get the matrix to draw
		double[][] matrix = sc.calculateDistanceMatrix(options.getScale());

		// Make the subequent columns, one per signal
		int col = 0;

		for (UUID id : sc.getSignalGroupIds()) {

			String signalName = d.getCollection().getSignalGroup(id).get().getGroupName();

			List<INuclearSignal> signalsRow = sc.getSignals(id);
			int sigNumber = 0;
			for (INuclearSignal row : signalsRow) {
				String colName = signalName + "_Sig_" + sigNumber;
				List<Object> colData = new ArrayList<Object>(0);

				double[] colValues = matrix[col];
				for (double value : colValues) {
					colData.add(df.format(value));
				}

				model.addColumn(colName, colData.toArray(new Object[0]));
				col++;
				sigNumber++;
			}
		}

		return model;
	}

	/*
	 * 
	 * PRIVATE METHODS
	 * 
	 */

	private void addCytoplasmDataToTable(List<Object> fieldNames, List<Object> rowData, ICell c,
			IAnalysisDataset d)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		fieldNames.add("Cytoplasm");
		rowData.add("");

		ICytoplasm cyto = c.getCytoplasm();
		DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
		for (Measurement stat : Measurement.getComponentStats()) {
			fieldNames.add(stat.label(GlobalOptions.getInstance().getScale()));

			double value = cyto.getMeasurement(stat, GlobalOptions.getInstance().getScale());
			rowData.add(df.format(value));
		}

	}

	private void addNuclearDataToTable(List<Object> fieldNames, List<Object> rowData, Nucleus n,
			IAnalysisDataset d)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {

		fieldNames.add(Labels.Cells.SOURCE_FILE_LABEL);
		rowData.add(n.getSourceFile());

		fieldNames.add(Labels.Cells.SOURCE_FILE_NAME_LABEL);
		rowData.add(n.getSourceFileName());

		fieldNames.add(Labels.Cells.SOURCE_CHANNEL_LABEL);
		rowData.add(n.getChannel());

		fieldNames.add(Labels.Cells.ANGLE_WINDOW_PROP_LABEL);
		rowData.add(n.getWindowProportion());

		fieldNames.add(Labels.Cells.ANGLE_WINDOW_SIZE_LABEL);
		rowData.add(n.getWindowSize());

		fieldNames.add(Labels.Cells.SCALE_LABEL);
		rowData.add(n.getScale());

		addNuclearStatisticsToTable(fieldNames, rowData, n);

		DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);

		fieldNames.add("Original bounding width");
		rowData.add(df.format(n.getWidth()));

		fieldNames.add("Original bounding height");
		rowData.add(df.format(n.getHeight()));

		fieldNames.add("Nucleus CoM");
		rowData.add(n.getCentreOfMass().toString());

		fieldNames.add("Original CoM");
		rowData.add(n.getOriginalCentreOfMass().toString());

		fieldNames.add("Original nucleus position");
		rowData.add("x: " + n.getXBase() + " : y: " + n.getYBase());

		fieldNames.add("Current nucleus position");
		rowData.add("x: " + df.format(n.getMinX()) + " : y: " + df.format(n.getMinY()));

		for (OrientationMark tag : n.getOrientationMarks()) {
			fieldNames.add(tag);
			if (n.hasLandmark(tag)) {

				try {
					IPoint p = n.getBorderPoint(tag);
					int index = n.getIndexRelativeTo(OrientationMark.REFERENCE,
							n.getBorderIndex(tag));
					rowData.add(p.toString() + " at profile index " + index);
				} catch (MissingLandmarkException e) {
					LOGGER.fine("Tag not present: " + tag);
					rowData.add("Missing tag");
				}

			} else {
				rowData.add("N/A");
			}
		}

		try {
			ISegmentedProfile sp = n.getProfile(ProfileType.ANGLE.ANGLE, OrientationMark.REFERENCE);
			for (IProfileSegment s : sp.getOrderedSegments()) {
				fieldNames.add(s.getName());
				rowData.add(s.toString());
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Cannot get segmented cell profile", e);
			fieldNames.add("Segments");
			rowData.add("N/A");
		}

		addNuclearSignalsToTable(fieldNames, rowData, n, d);
	}

	/**
	 * Add the nuclear statistic information to a cell table
	 * 
	 * @param fieldNames
	 * @param rowData
	 * @param n
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	private void addNuclearStatisticsToTable(List<Object> fieldNames, List<Object> rowData,
			Nucleus n)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {

		DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
		for (Measurement stat : Measurement.getNucleusStats()) {

			if (!stat.equals(Measurement.VARIABILITY)) {

				fieldNames.add(stat.label(GlobalOptions.getInstance().getScale()));

				double value = n.getMeasurement(stat, GlobalOptions.getInstance().getScale());
				rowData.add(df.format(value));
			}

		}

	}

	/**
	 * Add the nuclear signal information to a cell table
	 * 
	 * @param fieldNames
	 * @param rowData
	 * @param n          the nucleus
	 * @param d          the source dataset for the nucleus
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	private void addNuclearSignalsToTable(List<Object> fieldNames, List<Object> rowData, Nucleus n,
			IAnalysisDataset d)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {

		int j = 0;

		for (UUID signalGroup : d.getCollection().getSignalGroupIDs()) {

			if (signalGroup.equals(IShellResult.RANDOM_SIGNAL_ID))
				continue;

			try {
				Optional<ISignalGroup> g = d.getCollection().getSignalGroup(signalGroup);
				if (!g.isPresent())
					continue;
				Optional<IAnalysisOptions> datasetOptionsOptional = d.getAnalysisOptions();
				if (!datasetOptionsOptional.isPresent())
					continue;
				HashOptions signalOptions = datasetOptionsOptional.get()
						.getNuclearSignalOptions(signalGroup)
						.orElseThrow(MissingOptionException::new);

				fieldNames.add("");
				rowData.add("");
				Optional<Color> c = g.get().getGroupColour();
				Color colour = c.isPresent() ? c.get() : ColourSelecter.getColor(j);

				SignalTableCell tableCell = new SignalTableCell(signalGroup, g.get().getGroupName(),
						colour);

				fieldNames.add("Signal group");
				rowData.add(tableCell);

				fieldNames.add("Source image");
				rowData.add(n.getSignalCollection().getSourceFile(signalGroup));

				fieldNames.add("Source channel");
				rowData.add(signalOptions == null ? Labels.NA
						: signalOptions.getInt(HashOptions.CHANNEL));

				fieldNames.add("Number of signals");
				rowData.add(n.getSignalCollection().numberOfSignals(signalGroup));

				for (INuclearSignal s : n.getSignalCollection().getSignals(signalGroup)) {
					addSignalStatisticsToTable(fieldNames, rowData, s);
				}
			} finally {
				j++;
			}

		}

	}

	/**
	 * Add the nuclear signal statistics to a cell table
	 * 
	 * @param fieldNames
	 * @param rowData
	 * @param s
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	private void addSignalStatisticsToTable(List<Object> fieldNames, List<Object> rowData,
			INuclearSignal s)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
		for (Measurement stat : Measurement.getSignalStats()) {

			fieldNames.add(stat.label(GlobalOptions.getInstance().getScale()));

			double value = s.getMeasurement(stat, GlobalOptions.getInstance().getScale());

			rowData.add(df.format(value));
		}

		fieldNames.add("Signal CoM");
		rowData.add(s.getCentreOfMass().toString());

		fieldNames.add("Original CoM");
		rowData.add(s.getOriginalCentreOfMass().toString());
	}

}
