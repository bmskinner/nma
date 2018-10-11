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
package com.bmskinner.nuclear_morphology.charting.datasets.tables;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.image.ColourMeasurometer;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.datasets.AbstractCellDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.SignalTableCell;
import com.bmskinner.nuclear_morphology.charting.options.DisplayOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

/**
 * Generate the stats tables for a single cell
 * 
 * @author bms41
 *
 */
public class CellTableDatasetCreator extends AbstractCellDatasetCreator {

    public CellTableDatasetCreator(final DisplayOptions options, final ICell c) {
        super(options, c);
    }

    /**
     * Create a table of stats for the given cell.
     * 
     * @param cell
     *            the cell
     * @return a table model
     * @throws ChartDatasetCreationException
     * @throws Exception
     */
    public TableModel createCellInfoTable() {

        if (!options.hasDatasets()) {
            return AbstractTableCreator.createBlankTable();
        }

        if (options.isMultipleDatasets()) {
            return AbstractTableCreator.createBlankTable();
        }

        IAnalysisDataset d = options.firstDataset();
        DefaultTableModel model = new DefaultTableModel();

        List<Object> fieldNames = new ArrayList<Object>(0);
        List<Object> rowData = new ArrayList<Object>(0);

        // find the collection with the most channels
        // this defines the number of rows

        if (cell.hasCytoplasm()) {
            addCytoplasmDataToTable(fieldNames, rowData, cell, d);
        }

        fieldNames.add("Number of nuclei (lobes)");
        rowData.add(cell.getStatistic(PlottableStatistic.CELL_NUCLEUS_COUNT) + " ("
                + cell.getStatistic(PlottableStatistic.LOBE_COUNT) + ")");

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
    }

    public TableModel createCellSegmentsTable() {
        if (!options.hasDatasets()) {
            return AbstractTableCreator.createBlankTable();
        }

        if (options.isMultipleDatasets()) {
            return AbstractTableCreator.createBlankTable();
        }

        IAnalysisDataset d = options.firstDataset();
        DefaultTableModel model = new DefaultTableModel();

        List<Object> fieldNames = new ArrayList<Object>(0);
        List<Object> rowData = new ArrayList<Object>(0);

        try {
            ISegmentedProfile p = cell.getNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

            for (IBorderSegment s : p.getSegments()) {
                fieldNames.add(s.getName());
                rowData.add(s.getStartIndex() + "-" + s.getEndIndex());

            }

            model.addColumn("Segment", fieldNames.toArray(new Object[0]));
            model.addColumn("Range (of " + p.size() + ")", rowData.toArray(new Object[0]));

        } catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
            return AbstractTableCreator.createBlankTable();
        }

        return model;

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

        ISignalCollection sc = cell.getNucleus().getSignalCollection();

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

    private void addCytoplasmDataToTable(List<Object> fieldNames, List<Object> rowData, ICell c, IAnalysisDataset d) {
        fieldNames.add("Cytoplasm");
        rowData.add("");

        try {

            ICytoplasm cyto = c.getCytoplasm();
            DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
            for (PlottableStatistic stat : PlottableStatistic.getComponentStats()) {
                fieldNames.add(stat.label(GlobalOptions.getInstance().getScale()));

                double value = cyto.getStatistic(stat, GlobalOptions.getInstance().getScale());
                rowData.add(df.format(value));
            }

            // ColourMeasurometer cm = new ColourMeasurometer();
            Color colour = ColourMeasurometer.calculateAverageRGB(c, CellularComponent.CYTOPLASM);

            fieldNames.add("Average RGB");
            rowData.add(colour.getRed() + ", " + colour.getGreen() + ", " + colour.getBlue());

        } catch (UnloadableImageException e) {
            warn("Cannot get colour of cytoplasm");
            stack(e);
        }

    }

    private void addNuclearDataToTable(List<Object> fieldNames, List<Object> rowData, Nucleus n, IAnalysisDataset d) {

        fieldNames.add(Labels.Cells.SOURCE_FILE_LABEL);
        rowData.add(n.getSourceFile());

        fieldNames.add(Labels.Cells.SOURCE_FILE_NAME_LABEL);
        rowData.add(n.getSourceFileName());

        fieldNames.add(Labels.Cells.SOURCE_CHANNEL_LABEL);
        rowData.add(n.getChannel());

        fieldNames.add(Labels.Cells.ANGLE_WINDOW_PROP_LABEL);
        rowData.add(n.getWindowProportion(ProfileType.ANGLE));

        fieldNames.add(Labels.Cells.ANGLE_WINDOW_SIZE_LABEL);
        rowData.add(n.getWindowSize(ProfileType.ANGLE));

        fieldNames.add(Labels.Cells.SCALE_LABEL);
        rowData.add(n.getScale());

        addNuclearStatisticsToTable(fieldNames, rowData, n);

        fieldNames.add("Original bounding width");
        rowData.add(n.getBounds().getWidth());

        fieldNames.add("Original bounding height");
        rowData.add(n.getBounds().getHeight());

        fieldNames.add("Nucleus CoM");
        rowData.add(n.getCentreOfMass().toString());

        fieldNames.add("Original CoM");
        rowData.add(n.getOriginalCentreOfMass().toString());

        fieldNames.add("Original nucleus position");
        rowData.add("x: " + n.getPosition()[0] + " : y: " + n.getPosition()[1]);

        fieldNames.add("Current nucleus position");
        rowData.add("x: " + n.getMinX() + " : y: " + n.getMinY());

        NucleusType type = NucleusType.getNucleusType(n);

        if (type != null) {

            for (Tag tag : n.getBorderTags().keySet()) {
                fieldNames.add(tag);
                if (n.hasBorderTag(tag)) {

                    try {
                        IBorderPoint p = n.getBorderPoint(tag);
                        int index = n.getOffsetBorderIndex(Tag.REFERENCE_POINT, n.getBorderIndex(tag));
                        rowData.add(p.toString() + " at profile index " + index);
                    } catch (UnavailableBorderTagException e) {
                        fine("Tag not present: " + tag);
                        rowData.add("Missing tag");
                    }

                } else {
                    rowData.add("N/A");
                }
            }
        }

        addNuclearSignalsToTable(fieldNames, rowData, n, d);
    }

    /**
     * Add the nuclear statistic information to a cell table
     * 
     * @param fieldNames
     * @param rowData
     * @param n
     */
    private void addNuclearStatisticsToTable(List<Object> fieldNames, List<Object> rowData, Nucleus n) {

        NucleusType type = options.firstDataset().getCollection().getNucleusType();
        DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(type)) {

            if (!stat.equals(PlottableStatistic.VARIABILITY)) {

                fieldNames.add(stat.label(GlobalOptions.getInstance().getScale()));

                double value = n.getStatistic(stat, GlobalOptions.getInstance().getScale());
                rowData.add(df.format(value));
            }

        }

    }

    /**
     * Add the nuclear signal information to a cell table
     * 
     * @param fieldNames
     * @param rowData
     * @param n
     *            the nucleus
     * @param d
     *            the source dataset for the nucleus
     */
    private void addNuclearSignalsToTable(List<Object> fieldNames, List<Object> rowData, Nucleus n,
            IAnalysisDataset d) {

        int j = 0;

        for (UUID signalGroup : d.getCollection().getSignalGroupIDs()) {

            if (signalGroup.equals(IShellResult.RANDOM_SIGNAL_ID))
                continue;

            try {
                Optional<ISignalGroup> g = d.getCollection().getSignalGroup(signalGroup);
                if(!g.isPresent())
                	continue;
                Optional<IAnalysisOptions> datasetOptionsOptional = d.getAnalysisOptions();
                if(!datasetOptionsOptional.isPresent())
                	continue;
                INuclearSignalOptions signalOptions = datasetOptionsOptional.get().getNuclearSignalOptions(signalGroup);

                fieldNames.add("");
                rowData.add("");
                Optional<Color> c = g.get().getGroupColour();
                Color colour = c.isPresent() ? c.get() : ColourSelecter.getColor(j);

                SignalTableCell tableCell = new SignalTableCell(signalGroup, g.get().getGroupName(), colour);

                fieldNames.add("Signal group");
                rowData.add(tableCell);

                fieldNames.add("Source image");
                rowData.add(n.getSignalCollection().getSourceFile(signalGroup));

                fieldNames.add("Source channel");
                rowData.add(signalOptions.getChannel());

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
     */
    private void addSignalStatisticsToTable(List<Object> fieldNames, List<Object> rowData, INuclearSignal s) {
    	DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
        for (PlottableStatistic stat : PlottableStatistic.getSignalStats()) {

            fieldNames.add(stat.label(GlobalOptions.getInstance().getScale()));

            double value = s.getStatistic(stat, GlobalOptions.getInstance().getScale());

            rowData.add(df.format(value));
        }

        fieldNames.add("Signal CoM");
        rowData.add(s.getCentreOfMass().toString());

        fieldNames.add("Original CoM");
        rowData.add(s.getOriginalCentreOfMass().toString());

        // fieldNames.add("First border point");
        // rowData.add(s.getBorderPoint(0).toString());

    }

}
