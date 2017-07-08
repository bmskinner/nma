/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.charting.datasets.tables;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.charting.datasets.SignalTableCell;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.PairwiseSignalDistanceCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions.SignalDetectionMode;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.main.GlobalOptions;
import com.bmskinner.nuclear_morphology.stats.Quartile;

public class NuclearSignalTableCreator extends AbstractTableCreator {

    /**
     * Create with a set of table options
     */
    public NuclearSignalTableCreator(final TableOptions o) {
        super(o);
    }

    /**
     * Create a table of signal stats for the given list of datasets. This table
     * covers analysis parameters for the signals
     * 
     * @param list
     *            the AnalysisDatasets to include
     * @return a table model
     */
    public TableModel createSignalDetectionParametersTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        List<IAnalysisDataset> list = options.getDatasets();
        DefaultTableModel model = new DefaultTableModel();

        List<Object> fieldNames = new ArrayList<Object>(0);

        // find the collection with the most channels
        // this defines the number of rows
        int maxChannels = list.stream().mapToInt(d -> d.getCollection().getSignalManager().getSignalGroupCount()).max()
                .orElse(0);

        if (maxChannels == 0) {
            return createBlankTable();
        }

        Object[] rowNameBlock = { "", Labels.SIGNAL_GROUP_LABEL, "Channel", "Source", "Threshold", "Min size",
                "Max fraction", "Min circ", "Max circ", "Detection mode" };

        // create the row names
        fieldNames.add(Labels.NUMBER_OF_SIGNAL_GROUPS);

        for (int i = 0; i < maxChannels; i++) {

            for (Object o : rowNameBlock) {
                fieldNames.add(o);
            }
        }

        int numberOfRowsPerSignalGroup = rowNameBlock.length;
        model.addColumn(EMPTY_STRING, fieldNames.toArray(new Object[0])); // separate
                                                                          // row
                                                                          // block
                                                                          // for
                                                                          // each
                                                                          // channel

        // make a new column for each collection
        for (IAnalysisDataset dataset : list) {

            List<Object> columnData = makeDetectionSettingsColumn(dataset, maxChannels, numberOfRowsPerSignalGroup);
            model.addColumn(dataset.getName(), columnData.toArray(new Object[0])); // separate
                                                                                   // row
                                                                                   // block
                                                                                   // for
                                                                                   // each
                                                                                   // channel
        }

        return model;
    }

    /**
     * Create a column of signal group detection information for the given
     * dataset. If the number of signal groups in the dataset is less than the
     * number of signal groups in the table total, then the empty spaces will be
     * added explicitly to the column
     * 
     * @param dataset
     *            the dataset
     * @param signalGroupCount
     *            the total number of signal groups in the table
     * @param rowsPerSignalGroup
     *            the number of rows a signal group takes up
     * @return a list of rows for a table.
     */
    private List<Object> makeDetectionSettingsColumn(IAnalysisDataset dataset, int signalGroupCount,
            int rowsPerSignalGroup) {

        List<Object> rowData = new ArrayList<Object>(0);

        int signalGroupsInDataset = dataset.getCollection().getSignalManager().getSignalGroupCount();

        rowData.add(signalGroupsInDataset);

        /*
         * If the dataset is a merge, then the analysis options will be null. We
         * could loop through the merge sources until finding the merge with the
         * signal options BUT there could be conflicts with signal groups when
         * merging.
         * 
         * For now, do not display a table if the dataset has merge sources
         */

        boolean isFromMerge = false;

        if (dataset.hasMergeSources()) {
            isFromMerge = true;
        }

        if (!dataset.isRoot() && !dataset.hasAnalysisOptions()) { // temp fix
                                                                  // for missing
                                                                  // options in
                                                                  // clusters
                                                                  // from a
                                                                  // merge
            isFromMerge = true;
        }

        if (isFromMerge) {

            addMergedSignalData(rowData, dataset, signalGroupCount, rowsPerSignalGroup);

        } else {

            addNonMergedSignalData(rowData, dataset, signalGroupCount, rowsPerSignalGroup);
        }
        return rowData;
    }

    /**
     * Fill a list with rows describing each signal group in a merged dataset
     * 
     * @param rowData
     *            the list to add rows to
     * @param dataset
     *            the dataset with signals
     * @param signalGroupCount
     *            the total number of signal groups in the table
     * @param rowsPerSignalGroup
     *            the number of rows each signal group requires
     */
    private void addMergedSignalData(List<Object> rowData, IAnalysisDataset dataset, int signalGroupCount,
            int rowsPerSignalGroup) {
        ICellCollection collection = dataset.getCollection();
        Collection<ISignalGroup> signalGroups = collection.getSignalManager().getSignalGroups();

        int j = 0;
        for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

            try {

                Color colour = collection.getSignalGroup(signalGroup).hasColour()
                        ? collection.getSignalGroup(signalGroup).getGroupColour() : ColourSelecter.getColor(j);

                SignalTableCell cell = new SignalTableCell(signalGroup,
                        collection.getSignalManager().getSignalGroupName(signalGroup), colour);

                rowData.add(EMPTY_STRING);// empty row for colour
                rowData.add(cell); // group name

                for (int i = 0; i < rowsPerSignalGroup - 2; i++) { // rest are
                                                                   // NA
                    rowData.add(Labels.NA + " - merge");
                }

            } catch (UnavailableSignalGroupException e) {
                fine("Signal group " + signalGroup + " is not present in collection", e);
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
     * @param rowData
     *            the list to add rows to
     * @param dataset
     *            the dataset with signals
     * @param signalGroupCount
     *            the total number of signal groups in the table
     * @param rowsPerSignalGroup
     *            the number of rows each signal group requires
     */
    private void addNonMergedSignalData(List<Object> rowData, IAnalysisDataset dataset, int signalGroupCount,
            int rowsPerSignalGroup) {

        ICellCollection collection = dataset.getCollection();
        int signalGroupNumber = 0; // Store the number of signal groups
                                   // processed from this dataset

        int j = 0;
        for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

            try {

                signalGroupNumber++;

                Color colour = collection.getSignalGroup(signalGroup).hasColour()
                        ? collection.getSignalGroup(signalGroup).getGroupColour() : ColourSelecter.getColor(j);

                SignalTableCell cell = new SignalTableCell(signalGroup,
                        collection.getSignalManager().getSignalGroupName(signalGroup), colour);

                INuclearSignalOptions ns = null;
                try {
                    ns = dataset.getAnalysisOptions().getNuclearSignalOptions(signalGroup);

                } catch (MissingOptionException e) {
                    for (int i = 0; i < rowsPerSignalGroup; i++) {
                        rowData.add(EMPTY_STRING);
                    }
                }

                if (ns == null) { // occurs when no signals are present? Should
                                  // never occur with the new SignalGroup system

                    for (int i = 0; i < rowsPerSignalGroup; i++) {
                        rowData.add(EMPTY_STRING);
                    }

                } else {
                    Object signalThreshold = ns.getDetectionMode().equals(SignalDetectionMode.FORWARD)
                            ? ns.getThreshold() : "Variable";

                    rowData.add(EMPTY_STRING);
                    rowData.add(cell);
                    rowData.add(ns.getChannel());
                    rowData.add(ns.getFolder());
                    rowData.add(signalThreshold);
                    rowData.add(ns.getMinSize());
                    rowData.add(DEFAULT_DECIMAL_FORMAT.format(ns.getMaxFraction()));
                    rowData.add(DEFAULT_DECIMAL_FORMAT.format(ns.getMinCirc()));
                    rowData.add(DEFAULT_DECIMAL_FORMAT.format(ns.getMaxCirc()));
                    rowData.add(ns.getDetectionMode().toString());
                }

            } catch (UnavailableSignalGroupException e) {
                stack("Signal group " + signalGroup + " is not present in collection", e);
            } finally {
                j++;
            }
        }

        /*
         * If the number of signal groups in the dataset is less than the size
         * of the table, the remainder should be filled with blank cells
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

    /**
     * Create a table of signal stats for the given list of datasets. This table
     * covers size, number of signals
     * 
     * @param list
     *            the AnalysisDatasets to include
     * @return a table model
     * @throws Exception
     */
    public TableModel createSignalStatsTable() {
        return createMultiDatasetSignalStatsTable();
    }

    /**
     * Create the signal statistics table for the given options
     * 
     * @param options
     * @return
     */
    private TableModel createMultiDatasetSignalStatsTable() {

        DefaultTableModel model = new DefaultTableModel();

        int signalGroupTotal = options.getDatasets().stream()
                .mapToInt(d -> d.getCollection().getSignalManager().getSignalGroupCount()).max().orElse(0);

        // int signalGroupCount = getSignalGroupCount(options.getDatasets());

        finest("Selected collections have " + signalGroupTotal + " signal groups");

        if (signalGroupTotal <= 0) {

            finest("No signal groups to show");
            model.addColumn(Labels.NO_SIGNAL_GROUPS);
            return model;
        }

        MeasurementScale scale = GlobalOptions.getInstance().getScale();

        // Make an instance of row names
        List<Object> rowNames = new ArrayList<Object>();
        rowNames.add(EMPTY_STRING);
        rowNames.add(Labels.SIGNAL_GROUP_LABEL);
        rowNames.add(Labels.SIGNALS_LABEL);
        rowNames.add(Labels.SIGNALS_PER_NUCLEUS);

        for (PlottableStatistic stat : PlottableStatistic.getSignalStats()) {
            rowNames.add(stat.label(scale));
        }

        // Make the full column of row names for each signal group
        List<Object> firstColumn = new ArrayList<Object>(0);
        firstColumn.add(Labels.NUMBER_OF_SIGNAL_GROUPS);
        for (int i = 0; i < signalGroupTotal; i++) {
            firstColumn.addAll(rowNames);
        }

        int numberOfRowsPerSignalGroup = rowNames.size();
        model.addColumn(EMPTY_STRING, firstColumn.toArray(new Object[0])); // separate
                                                                           // row
                                                                           // block
                                                                           // for
                                                                           // each
                                                                           // channel

        // make a new column for each collection
        for (IAnalysisDataset dataset : options.getDatasets()) {

            List<Object> rowData = addSignalDataColumn(dataset, numberOfRowsPerSignalGroup, signalGroupTotal);
            model.addColumn(dataset.getName(), rowData.toArray(new Object[0])); // separate
                                                                                // row
                                                                                // block
                                                                                // for
                                                                                // each
                                                                                // channel
        }

        return model;
    }

    /**
     * Add a signal column for a dataset
     * 
     * @param dataset
     *            the dataset to add
     * @param numberOfRowsPerSignalGroup
     *            the number of rows each signal group occupies
     * @param maxSignalGroup
     *            the highest signal group
     * @return a list of table row values
     */
    private List<Object> addSignalDataColumn(IAnalysisDataset dataset, int numberOfRowsPerSignalGroup,
            int maxSignalGroup) {
        ICellCollection collection = dataset.getCollection();
        MeasurementScale scale = GlobalOptions.getInstance().getScale();
        int signalGroupCount = collection.getSignalManager().getSignalGroupCount();

        List<Object> rowData = new ArrayList<Object>(0);
        rowData.add(signalGroupCount);

        for (UUID signalGroup : collection.getSignalManager().getSignalGroupIDs()) {

            List<Object> temp = new ArrayList<Object>(0);
            try {

                if (collection.getSignalManager().getSignalCount(signalGroup) == 0) { // Signal
                                                                                      // group
                                                                                      // has
                                                                                      // no
                                                                                      // signals
                    for (int j = 0; j < numberOfRowsPerSignalGroup; j++) { // Make
                                                                           // a
                                                                           // blank
                                                                           // block
                                                                           // of
                                                                           // cells
                        temp.add(EMPTY_STRING);
                    }
                    continue;
                }

                Color colour = collection.getSignalGroup(signalGroup).hasColour()
                        ? collection.getSignalGroup(signalGroup).getGroupColour() : Color.WHITE;

                SignalTableCell cell = new SignalTableCell(signalGroup,
                        collection.getSignalManager().getSignalGroupName(signalGroup), colour);

                temp.add(EMPTY_STRING);
                temp.add(cell);
                temp.add(collection.getSignalManager().getSignalCount(signalGroup));
                double signalPerNucleus = collection.getSignalManager().getSignalCountPerNucleus(signalGroup);
                temp.add(DEFAULT_DECIMAL_FORMAT.format(signalPerNucleus));

                for (PlottableStatistic stat : PlottableStatistic.getSignalStats()) {
                    double pixel = collection.getSignalManager().getMedianSignalStatistic(stat, scale, signalGroup);
                    temp.add(DEFAULT_DECIMAL_FORMAT.format(pixel));

                    // if(stat.isDimensionless()){
                    // temp.add(DEFAULT_DECIMAL_FORMAT.format(pixel) );
                    // } else {
                    // double micron =
                    // collection.getSignalManager().getMedianSignalStatistic(stat,
                    // MeasurementScale.MICRONS, signalGroup);
                    // temp.add(DEFAULT_DECIMAL_FORMAT.format(pixel) +" ("+
                    // DEFAULT_DECIMAL_FORMAT.format(micron)+ " "+
                    // stat.units(MeasurementScale.MICRONS)+")");
                    // }
                }

            } catch (UnavailableSignalGroupException e) {
                stack("Signal group " + signalGroup + " is not present in collection", e);
                temp = new ArrayList<Object>(0);
                for (int j = 0; j < numberOfRowsPerSignalGroup; j++) { // Make a
                                                                       // blank
                                                                       // block
                                                                       // of
                                                                       // cells
                    temp.add(EMPTY_STRING);
                }
            } finally {
                rowData.addAll(temp);
            }

        }

        if (signalGroupCount < maxSignalGroup) {

            // There will be empty rows in the table. Fill the blanks
            for (int i = signalGroupCount + 1; i <= maxSignalGroup; i++) {
                for (int j = 0; j < numberOfRowsPerSignalGroup; j++) {
                    rowData.add(EMPTY_STRING);
                }
            }

        }
        return rowData;
    }

    /**
     * Create a table with columns for dataset, signal group, and the p value of
     * a chi-square test for all shell analyses run
     * 
     * @param options
     * @return
     */
    public TableModel createShellChiSquareTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        DefaultTableModel model = new DefaultTableModel();

        DecimalFormat pFormat = new DecimalFormat("#0.000");

        Object[] columnNames = { Labels.DATASET, Labels.SIGNAL_GROUP_LABEL, Labels.AVERAGE_POSITION, Labels.PROBABILITY, };

        model.setColumnIdentifiers(columnNames);

        for (IAnalysisDataset d : options.getDatasets()) {

            for (UUID signalGroup : d.getCollection().getSignalManager().getSignalGroupIDs()) {

                try {

                    ISignalGroup group = d.getCollection().getSignalGroup(signalGroup);

                    if (group.hasShellResult()) {

                        String groupName = group.getGroupName();

                        IShellResult r = group.getShellResult();
                        
                        double mean = options.isNormalised() 
                        		? r.getNormalisedMeanShell(options.getCountType())
                        		: r.getRawMeanShell(options.getCountType());
                        		
                       double pval =  options.isNormalised() 
                       		? r.getNormalisedPValue(options.getCountType())
                       		: r.getRawPValue(options.getCountType());

                        Object[] rowData = {

                                d.getName(), 
                                groupName, 
                                pFormat.format(mean),
                                pFormat.format(pval) };

                        model.addRow(rowData);
                    }

                } catch (UnavailableSignalGroupException e) {
                    stack("Signal group " + signalGroup + " is not present in collection", e);
                }
            }

        }

        return model;
    }

    /**
     * Create a table showing the colocalisation level of all signals within a
     * single dataset
     * 
     * @param options
     * @return
     */
    public TableModel createSignalColocalisationTable() {

        if (!options.isSingleDataset()) {
            return createBlankTable();
        }

        DefaultTableModel model = new DefaultTableModel();

        // DecimalFormat pFormat = new DecimalFormat("#0.00");

        try {

            PairwiseSignalDistanceCollection ps = options.firstDataset().getCollection().getSignalManager()
                    .calculateSignalColocalisation(options.getScale());

            List<Object> firstColumnData = new ArrayList<Object>();

            Set<UUID> ids = new HashSet<UUID>();
            ids.addAll(ps.getIDs());

            for (UUID primaryID : ps.getIDs()) {
                String primaryName = options.firstDataset().getCollection().getSignalGroup(primaryID).getGroupName();
                firstColumnData.add(primaryName);
            }

            model.addColumn(Labels.SIGNAL_GROUP_LABEL, firstColumnData.toArray());

            for (UUID primaryID : ps.getIDs()) {

                List<Object> columnData = new ArrayList<Object>();

                String primaryName = options.firstDataset().getCollection().getSignalGroup(primaryID).getGroupName();

                for (UUID secondaryID : ps.getIDs()) {

                    if (primaryID.equals(secondaryID)) {
                        columnData.add(EMPTY_STRING);
                        continue;
                    }

                    String secondaryName = options.firstDataset().getCollection().getSignalGroup(secondaryID)
                            .getGroupName();

                    List<Double> values = ps.getValues(primaryID, secondaryID);

                    if (values == null) {
                        columnData.add(Labels.NA);
                        continue;
                    }

                    double median = new Quartile(values, Quartile.MEDIAN).doubleValue();
                    columnData.add(DEFAULT_DECIMAL_FORMAT.format(median));

                }

                model.addColumn(primaryName, columnData.toArray());

            }

        } catch (UnavailableSignalGroupException e) {
            stack("Cannot get signal group", e);
            return createBlankTable();
        }

        return model;
    }
}
