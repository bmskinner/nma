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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.charting.datasets.SignalTableCell;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Normalisation;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.PairwiseSignalDistanceCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions.SignalDetectionMode;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class NuclearSignalTableCreator extends AbstractTableCreator {

    /**
     * Create with a set of table options
     */
    public NuclearSignalTableCreator(@NonNull final TableOptions o) {
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

        Object[] rowNameBlock = { "", Labels.Signals.SIGNAL_GROUP_LABEL, "Channel", "Source", "Threshold", "Min size",
                "Max fraction", "Min circ", "Max circ", "Detection mode" };

        // create the row names
        fieldNames.add(Labels.Signals.NUMBER_OF_SIGNAL_GROUPS);

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
                Optional<Color> c = collection.getSignalGroup(signalGroup).get().getGroupColour();
                Color colour = c.isPresent() ? c.get() : ColourSelecter.getColor(j);
                
                SignalTableCell cell = new SignalTableCell(signalGroup,
                        collection.getSignalManager().getSignalGroupName(signalGroup), colour);

                INuclearSignalOptions ns = null;
                Optional<? extends IAnalysisOptions> op = dataset.getAnalysisOptions();
                if(!op.isPresent()){
                    for (int i = 0; i < rowsPerSignalGroup; i++) {
                        rowData.add(EMPTY_STRING);
                    }
                } else {
                	ns = op.get().getNuclearSignalOptions(signalGroup);
                }

                if (ns == null) { // occurs when no signals are present? Should
                                  // never occur with the new SignalGroup system

                    for (int i = 0; i < rowsPerSignalGroup; i++) {
                        rowData.add(EMPTY_STRING);
                    }

                } else {
                    Object signalThreshold = ns.getDetectionMode().equals(SignalDetectionMode.FORWARD)
                            ? ns.getThreshold() : "Variable";
                            
                    DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);

                    rowData.add(EMPTY_STRING);
                    rowData.add(cell);
                    rowData.add(ns.getChannel());
                    rowData.add(ns.getFolder());
                    rowData.add(signalThreshold);
                    rowData.add(ns.getMinSize());
                    rowData.add(df.format(ns.getMaxFraction()));
                    rowData.add(df.format(ns.getMinCirc()));
                    rowData.add(df.format(ns.getMaxCirc()));
                    rowData.add(ns.getDetectionMode().toString());
                }

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
            model.addColumn(Labels.Signals.NO_SIGNAL_GROUPS);
            return model;
        }

        MeasurementScale scale = GlobalOptions.getInstance().getScale();

        // Make an instance of row names
        List<Object> rowNames = new ArrayList<Object>();
        rowNames.add(EMPTY_STRING);
        rowNames.add(Labels.Signals.SIGNAL_GROUP_LABEL);
        rowNames.add(Labels.Signals.SIGNALS_LABEL);
        rowNames.add(Labels.Signals.SIGNALS_PER_NUCLEUS);
        rowNames.add("ID");

        for (PlottableStatistic stat : PlottableStatistic.getSignalStats()) {
            rowNames.add(stat.label(scale));
        }

        // Make the full column of row names for each signal group
        List<Object> firstColumn = new ArrayList<Object>(0);
        firstColumn.add(Labels.Signals.NUMBER_OF_SIGNAL_GROUPS);
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
        DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
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
                Optional<Color> c = collection.getSignalGroup(signalGroup).get().getGroupColour();
                Color colour = c.isPresent() ? c.get() : Color.WHITE;


                SignalTableCell cell = new SignalTableCell(signalGroup,
                        collection.getSignalManager().getSignalGroupName(signalGroup), colour);

                temp.add(EMPTY_STRING);
                temp.add(cell);
                temp.add(collection.getSignalManager().getSignalCount(signalGroup));
                double signalPerNucleus = collection.getSignalManager().getSignalCountPerNucleus(signalGroup);
                temp.add(df.format(signalPerNucleus));
                temp.add(signalGroup.toString());

                for (PlottableStatistic stat : PlottableStatistic.getSignalStats()) {
                    double pixel = collection.getSignalManager().getMedianSignalStatistic(stat, scale, signalGroup);
                    temp.add(df.format(pixel));
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

        if (!options.hasDatasets())
            return createBlankTable();

        DefaultTableModel model = new DefaultTableModel();

        DecimalFormat pFormat = new DecimalFormat(DEFAULT_PROBABILITY_FORMAT);

        Object[] columnNames = { Labels.DATASET, 
        		Labels.Signals.SIGNAL_GROUP_LABEL, 
        		Labels.Signals.AVERAGE_POSITION, 
        		Labels.Stats.PROBABILITY };

        model.setColumnIdentifiers(columnNames);

        for (IAnalysisDataset d : options.getDatasets()) {
            
            Optional<ISignalGroup> randomGroup = d.getCollection().getSignalGroup(IShellResult.RANDOM_SIGNAL_ID);
            Optional<IShellResult> random = randomGroup.isPresent() ? 
                    d.getCollection().getSignalGroup(IShellResult.RANDOM_SIGNAL_ID).get().getShellResult() 
                    : Optional.empty();

            for (UUID signalGroup : d.getCollection().getSignalManager().getSignalGroupIDs()) {

                ISignalGroup group = d.getCollection().getSignalGroup(signalGroup).get();
				Optional<IShellResult> r = group.getShellResult();
				if (r.isPresent()) {

				    String groupName = group.getGroupName();
				    
				    double mean = r.get().getOverallShell(options.getAggregation(), options.getNormalisation());
				    double pval = 1;
				    if(random.isPresent())
	                    pval = r.get().getPValue(options.getAggregation(), options.getNormalisation(), random.get());
				    
				    Object[] rowData = {
				            d.getName(), 
				            groupName, 
				            pFormat.format(mean),
				            pFormat.format(pval) };
				    model.addRow(rowData);
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

        PairwiseSignalDistanceCollection ps = options.firstDataset().getCollection().getSignalManager()
		        .calculateSignalColocalisation(options.getScale());

		List<Object> firstColumnData = new ArrayList<Object>();

		Set<UUID> ids = new HashSet<UUID>();
		ids.addAll(ps.getIDs());

		for (UUID primaryID : ps.getIDs()) {
		    String primaryName = options.firstDataset().getCollection().getSignalGroup(primaryID).get().getGroupName();
		    firstColumnData.add(primaryName);
		}
		DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
		model.addColumn(Labels.Signals.SIGNAL_GROUP_LABEL, firstColumnData.toArray());

		for (UUID primaryID : ps.getIDs()) {

		    List<Object> columnData = new ArrayList<Object>();

		    String primaryName = options.firstDataset().getCollection().getSignalGroup(primaryID).get().getGroupName();

		    for (UUID secondaryID : ps.getIDs()) {

		        if (primaryID.equals(secondaryID)) {
		            columnData.add(EMPTY_STRING);
		            continue;
		        }

		        String secondaryName = options.firstDataset().getCollection().getSignalGroup(secondaryID).get()
		                .getGroupName();

		        List<Double> values = ps.getValues(primaryID, secondaryID);

		        if (values == null) {
		            columnData.add(Labels.NA);
		            continue;
		        }
		        
		        DescriptiveStatistics ds = new DescriptiveStatistics();
		        for(double d : values){
		        	ds.addValue(d);
		        }
		        double median = ds.getPercentile(Stats.MEDIAN);

		        columnData.add(df.format(median));

		    }

		    model.addColumn(primaryName, columnData.toArray());

		}

        return model;
    }
}
