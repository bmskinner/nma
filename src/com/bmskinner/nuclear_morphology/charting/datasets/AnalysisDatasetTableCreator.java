/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.charting.datasets;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.DoubleStream;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.options.DefaultTableOptions.TableType;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.ClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.stats.ConfidenceInterval;
import com.bmskinner.nuclear_morphology.stats.DipTester;
import com.bmskinner.nuclear_morphology.stats.Quartile;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class AnalysisDatasetTableCreator extends AbstractTableCreator {

    private static final String NA_MERGE = "N/A - merge";
    private static final String NA       = "N/A";

    private static final Object[] ANALYSIS_PARAMETERS_ROWS = { "Profile window", "Nucleus detection method",
            "Nucleus threshold", "Kuwahara filter radius", "Chromocentre flattening threshold", "Canny auto threshold",
            "Canny low threshold", "Canny high threshold", "Canny kernel radius", "Canny kernel width",
            "Closing radius", "Nucleus min size", "Nucleus max size", "Nucleus min circ", "Nucleus max circ",
            "Consensus folded", "Refold mode", "Run date", "Run time", "Collection source", "Log file", "Type",
            "Created in" };

    /**
     * Create with a set of table options
     */
    public AnalysisDatasetTableCreator(final TableOptions o) {
        super(o);
    }

    public TableModel createMedianProfileStatisticTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        if (options.isSingleDataset()) {
            return createMedianProfileSegmentStatsTable(options.firstDataset(), options.getScale());
        }

        if (options.isMultipleDatasets()) {
            return createMultiDatasetMedianProfileSegmentStatsTable();
        }

        return createBlankTable();
    }

    public TableModel createMergeSourcesTable() {

        if (!options.hasDatasets()) {
            DefaultTableModel model = new DefaultTableModel();

            Vector<Object> names = new Vector<Object>();
            Vector<Object> nuclei = new Vector<Object>();

            names.add("No merge sources");
            nuclei.add(EMPTY_STRING);

            model.addColumn("Merge source", names);
            model.addColumn("Nuclei", nuclei);
            return model;
        }

        if (options.firstDataset().hasMergeSources()) {

            DefaultTableModel model = new DefaultTableModel();

            Vector<Object> names = new Vector<Object>();
            Vector<Object> nuclei = new Vector<Object>();

            for (IAnalysisDataset mergeSource : options.firstDataset().getMergeSources()) {
                // AnalysisDataset mergeSource =
                // options.firstDataset().getMergeSource(id);
                names.add(mergeSource.getName());
                nuclei.add(mergeSource.getCollection().size());
            }
            model.addColumn("Merge source", names);
            model.addColumn("Nuclei", nuclei);
            return model;
        }
        return createBlankTable();
    }

    /**
     * Create a table of segment stats for median profile of the given dataset.
     * 
     * @param dataset
     *            the AnalysisDataset to include
     * @return a table model
     * @throws Exception
     */
    private TableModel createMedianProfileSegmentStatsTable(IAnalysisDataset dataset, MeasurementScale scale) {

        DefaultTableModel model = new DefaultTableModel();

        if (dataset == null) {
            model.addColumn(Labels.NO_DATA_LOADED);

        } else {
            ICellCollection collection = dataset.getCollection();
            // check which reference point to use
            Tag point = Tag.REFERENCE_POINT;

            // get mapping from ordered segments to segment names
            List<IBorderSegment> segments;
            try {
                segments = collection.getProfileCollection()
                        .getSegmentedProfile(ProfileType.ANGLE, point, Quartile.MEDIAN).getOrderedSegments();
            } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                    | UnsegmentedProfileException e) {
                stack("Error getting median profile", e);
                return createBlankTable();
            }

            // create the row names
            Object[] fieldNames = { "Colour", "Length", "Start index", "End index", "Mean length (" + scale + ")",
                    "Mean length 95% CI (" + scale + ")", "Length std err. (" + scale + ")", "Length p(unimodal)" };

            model.addColumn(EMPTY_STRING, fieldNames);

            DecimalFormat pf = new DecimalFormat("#.###");

            for (IBorderSegment segment : segments) {

                List<Object> rowData = new ArrayList<Object>(0);

                rowData.add("");
                rowData.add(segment.length());
                rowData.add(segment.getStartIndex());
                rowData.add(segment.getEndIndex());

                double[] meanLengths = collection.getMedianStatistics(PlottableStatistic.LENGTH,
                        CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, segment.getID());

                double mean = DoubleStream.of(meanLengths).average().orElse(0);

                double sem = Stats.stderr(meanLengths);

                ConfidenceInterval ci = new ConfidenceInterval(meanLengths, 0.95);

                rowData.add(DEFAULT_DECIMAL_FORMAT.format(mean));
                rowData.add(DEFAULT_DECIMAL_FORMAT.format(ci.getLower().doubleValue()) + " - "
                        + DEFAULT_DECIMAL_FORMAT.format(ci.getUpper().doubleValue()));
                rowData.add(DEFAULT_DECIMAL_FORMAT.format(sem));

                double pval = DipTester.getDipTestPValue(meanLengths);
                rowData.add(pf.format(pval));

                model.addColumn(segment.getName(), rowData.toArray(new Object[0])); // separate
                                                                                    // column
                                                                                    // per
                                                                                    // segment
            }
        }
        return model;
    }

    /**
     * Create a table of segment stats for median profile of the given dataset.
     * 
     * @param dataset
     *            the AnalysisDataset to include
     * @return a table model
     * @throws Exception
     */
    private TableModel createMultiDatasetMedianProfileSegmentStatsTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        DefaultTableModel model = new DefaultTableModel();

        List<IAnalysisDataset> list = options.getDatasets();
        // If the datasets have different segment counts, show error message
        if (!IBorderSegment.segmentCountsMatch(list)) {
            model.addColumn(Labels.INCONSISTENT_SEGMENT_NUMBER);
            return model;
        }

        MeasurementScale scale = options.getScale();

        List<Object> fieldNames = new ArrayList<Object>(0);

        BorderTagObject point = Tag.REFERENCE_POINT;// .ORIENTATION_POINT;

        // assumes all datasets have the same number of segments
        List<IBorderSegment> segments;
        try {
            segments = list.get(0).getCollection().getProfileCollection().getSegments(point);
        } catch (UnavailableBorderTagException | ProfileException e1) {
            fine("Error getting segments from profile collection", e1);
            return createBlankTable();
        }

        // Add the dataset names column
        fieldNames.add(Labels.DATASET);
        for (IBorderSegment segment : segments) {
            fieldNames.add(segment.getName());
        }
        model.setColumnIdentifiers(fieldNames.toArray());
        ;

        // Add the segment colours column
        List<Object> colours = new ArrayList<Object>(0);
        colours.add(EMPTY_STRING);

        for (int i = 0; i < segments.size(); i++) {
            colours.add(EMPTY_STRING);
        }
        model.addRow(colours.toArray(new Object[0]));

        // Add the segment stats columns

        for (IAnalysisDataset dataset : list) {

            ICellCollection collection = dataset.getCollection();

            List<IBorderSegment> segs;
            try {
                segs = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, point, Quartile.MEDIAN)
                        .getOrderedSegments();
            } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                    | UnsegmentedProfileException e) {
                fine("Error getting median profile", e);
                return createBlankTable();
            }

            List<Object> rowData = new ArrayList<Object>(0);
            rowData.add(dataset.getName());

            for (IBorderSegment segment : segs) {

                double[] meanLengths = collection.getMedianStatistics(PlottableStatistic.LENGTH,
                        CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, segment.getID());
                double mean = DoubleStream.of(meanLengths).average().orElse(0);

                ConfidenceInterval ci = new ConfidenceInterval(meanLengths, 0.95);
                rowData.add(DEFAULT_DECIMAL_FORMAT.format(mean) + " ± "
                        + DEFAULT_DECIMAL_FORMAT.format(ci.getSize().doubleValue()));
            }
            model.addRow(rowData.toArray(new Object[0]));
        }

        return model;
    }

    /**
     * Create a table model of analysis parameters or stats from datasets. If
     * null parameter is passed, will create an empty table
     * 
     * @param collection
     * @return
     */
    public TableModel createAnalysisTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        TableOptions op = (TableOptions) options;

        if (op.getType().equals(TableType.ANALYSIS_PARAMETERS)) {
            return createAnalysisParametersTable();
        }

        if (op.getType().equals(TableType.ANALYSIS_STATS)) {
            return createStatsTable();
        }

        return createBlankTable();
    }

    /**
     * Create a table model of analysis parameters from a nucleus collection. If
     * null parameter is passed, will create an empty table
     * 
     * @param collection
     * @return
     */
    private TableModel createAnalysisParametersTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        DefaultTableModel model = new DefaultTableModel();

        model.addColumn(EMPTY_STRING, ANALYSIS_PARAMETERS_ROWS);

        List<IAnalysisDataset> list = options.getDatasets();

        for (IAnalysisDataset dataset : list) {

            // only display if there are options available
            // This may not be the case for a merged dataset or its children
            if (dataset.hasAnalysisOptions()) {

                // Do not provide an options as an argument here.
                // This will cause the existing dataset options to be used
                Object[] collectionData = createAnalysisParametersColumn(dataset, null);

                model.addColumn(dataset.getCollection().getName(), collectionData);

            } else {
                fine("No analysis options in dataset " + dataset.getName());
                Object[] collectionData = createAnalysisParametersMergeColumn(dataset);

                model.addColumn(dataset.getCollection().getName(), collectionData);
            }
        }

        return model;
    }

    /**
     * Create an analysis parameter column for a dataset with no top level
     * options
     * 
     * @param dataset
     * @return
     */
    private Object[] createAnalysisParametersMergeColumn(IAnalysisDataset dataset) {

        Object[] data = new Object[ANALYSIS_PARAMETERS_ROWS.length];

        if (dataset.hasMergeSources()) {

            fine("Dataset has merge sources");
            if (IAnalysisDataset.mergedSourceOptionsAreSame(dataset)) {

                fine("Merge source options are the same");

                // The options are the same in all merge sources
                // Show the first options from the first source

                List<IAnalysisDataset> l = new ArrayList<IAnalysisDataset>(dataset.getAllMergeSources());

                try {
                    IAnalysisOptions op = l.get(0).getAnalysisOptions();
                    data = createAnalysisParametersColumn(dataset, op);
                } catch (MissingOptionException e) {
                    fine("No option present for " + l.get(0).getName());
                }

                // Provide an options to be used

            } else {
                // Merge sources have different options
                Arrays.fill(data, NA_MERGE);
                fine("Merge source options are different");
            }

        } else {
            // there are no options to use; fill blank
            Arrays.fill(data, NA);
            fine("No merge sources, and no options");
        }
        return data;
    }

    /**
     * Get an array of formatted info from a dataset analysis options
     * 
     * @param dataset
     *            the dataset to format options for
     * @param options
     *            an options to use instead of the dataset's own options. Can be
     *            null.
     * @return
     */
    private Object[] createAnalysisParametersColumn(IAnalysisDataset dataset, IAnalysisOptions options) {
        try {
            options = options == null ? dataset.getAnalysisOptions() : options;
        } catch (MissingOptionException e1) {
            warn("Could not get analysis options");
            stack(e1.getMessage(), e1);
        }

        String refoldMode = "Fast";

        String date;
        String time;
        String folder;
        String logFile;

        if (dataset.hasMergeSources()) {
            date = NA_MERGE;
            time = NA_MERGE;
            folder = NA_MERGE;
            logFile = NA_MERGE;

        } else {
            if (dataset.getCollection().getOutputFolderName() == null) {
                date = NA;
                time = NA;
            } else {
                String[] times = dataset.getCollection().getOutputFolderName().split("_");
                date = times[0];
                time = times[1];
            }
            try {
                folder = options.getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder().getAbsolutePath();
            } catch (MissingOptionException e) {
                folder = "Missing data";
            }
            logFile = dataset.getDebugFile().getAbsolutePath();
        }

        ICannyOptions nucleusCannyOptions = null;
        IDetectionOptions nucleusOptions = null;
        try {
            nucleusOptions = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);

            nucleusCannyOptions = options.getDetectionOptions(IAnalysisOptions.NUCLEUS).getCannyOptions();
        } catch (MissingOptionException e) {
            warn("Missing options");
            stack(e.getMessage(), e);
        }

        String detectionMethod = nucleusCannyOptions.isUseCanny() ? "Canny edge detection" : "Thresholding";

        String nucleusThreshold = nucleusCannyOptions.isUseCanny() ? NA : String.valueOf(nucleusOptions.getThreshold());

        String kuwaharaRadius = nucleusCannyOptions.isUseKuwahara()
                ? String.valueOf(nucleusCannyOptions.getKuwaharaKernel()) : NA;

        String chromocentreThreshold = nucleusCannyOptions.isUseFlattenImage()
                ? String.valueOf(nucleusCannyOptions.getFlattenThreshold()) : NA;

        String cannyAutoThreshold = nucleusCannyOptions.isUseCanny()
                ? String.valueOf(nucleusCannyOptions.isCannyAutoThreshold()) : NA;
        String cannyLowThreshold = nucleusCannyOptions.isUseCanny() && !nucleusCannyOptions.isCannyAutoThreshold()
                ? String.valueOf(nucleusCannyOptions.getLowThreshold()) : "N/A";
        String cannyHighThreshold = nucleusCannyOptions.isUseCanny() && !nucleusCannyOptions.isCannyAutoThreshold()
                ? String.valueOf(nucleusCannyOptions.getHighThreshold()) : "N/A";
        String cannyKernelRadius = nucleusCannyOptions.isUseCanny()
                ? String.valueOf(nucleusCannyOptions.getKernelRadius()) : NA;
        String cannyKernelWidth = nucleusCannyOptions.isUseCanny()
                ? String.valueOf(nucleusCannyOptions.getKernelWidth()) : NA;
        String cannyClosingRadius = nucleusCannyOptions.isUseCanny()
                ? String.valueOf(nucleusCannyOptions.getClosingObjectRadius()) : NA;

        Object[] collectionData = { options.getProfileWindowProportion(), detectionMethod, nucleusThreshold,
                kuwaharaRadius, chromocentreThreshold, cannyAutoThreshold, cannyLowThreshold, cannyHighThreshold,
                cannyKernelRadius, cannyKernelWidth, cannyClosingRadius, nucleusOptions.getMinSize(),
                nucleusOptions.getMaxSize(), DEFAULT_DECIMAL_FORMAT.format(nucleusOptions.getMinCirc()),
                DEFAULT_DECIMAL_FORMAT.format(nucleusOptions.getMaxCirc()), options.refoldNucleus(), refoldMode, date,
                time, folder, logFile, options.getNucleusType().toString(), dataset.getVersion().toString() };
        return collectionData;
    }

    /**
     * Create a table model of basic stats from a nucleus collection. If null
     * parameter is passed, will create an empty table
     * 
     * @param collection
     * @return
     */
    private TableModel createStatsTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        final String NUCLEUS_LABEL = "Nuclei";
        final String[] VALUE_LABELS = { " median", " mean", " S.E.M.", " mean 95% CI", " p(unimodal)" };

        NucleusType type = IAnalysisDataset.getBroadestNucleusType(options.getDatasets()); // default,
                                                                                           // applies
                                                                                           // to
                                                                                           // everything

        DefaultTableModel model = new DefaultTableModel();

        List<IAnalysisDataset> list = options.getDatasets();

        List<Object> columnData = new ArrayList<Object>();
        columnData.add(NUCLEUS_LABEL);
        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(type)) {
            for (String value : VALUE_LABELS) {
                columnData.add(stat.toString() + value);
            }
        }

        model.addColumn(EMPTY_STRING, columnData.toArray());

        for (IAnalysisDataset dataset : list) {

            finest("Making column for " + dataset.getName());

            List<Object> datasetData = createDatasetStatsTableColumn(dataset, options.getScale());

            model.addColumn(dataset.getName(), datasetData.toArray());
            finest("Added column for " + dataset.getName());
        }

        return model;
    }

    private List<Object> createDatasetStatsTableColumn(IAnalysisDataset dataset, MeasurementScale scale) {

        // format the numbers and make into a tablemodel
        DecimalFormat pf = new DecimalFormat("#0.000");

        ICellCollection collection = dataset.getCollection();

        List<Object> datasetData = new ArrayList<Object>();

        datasetData.add(collection.size());
        NucleusType type = IAnalysisDataset.getBroadestNucleusType(options.getDatasets());

        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(type)) {
            // log("Getting stats for "+stat);
            double[] stats = collection.getMedianStatistics(stat, CellularComponent.NUCLEUS, scale);

            double mean = DoubleStream.of(stats).average().orElse(0);
            double sem = Stats.stderr(stats);
            double median = Quartile.quartile(stats, Quartile.MEDIAN);

            ConfidenceInterval ci = new ConfidenceInterval(stats, 0.95);
            String ciString = DEFAULT_DECIMAL_FORMAT.format(mean) + " ± "
                    + DEFAULT_DECIMAL_FORMAT.format(ci.getSize().doubleValue());
            double diptest = DipTester.getDipTestPValue(stats);

            datasetData.add(DEFAULT_DECIMAL_FORMAT.format(median));
            datasetData.add(DEFAULT_DECIMAL_FORMAT.format(mean));
            datasetData.add(DEFAULT_DECIMAL_FORMAT.format(sem));
            datasetData.add(ciString);
            datasetData.add(pf.format(diptest));
        }

        return datasetData;

    }

    public TableModel createVennTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        if (options.isSingleDataset()) {
            return createBlankTable();
        }

        finer("Creating venn table model");
        DefaultTableModel model = new DefaultTableModel();

        List<IAnalysisDataset> list = options.getDatasets();

        // set rows
        Object[] columnData = new Object[list.size()];
        int row = 0;
        for (IAnalysisDataset dataset : list) {
            columnData[row] = dataset.getName();
            row++;
        }
        model.addColumn(Labels.DATASET, columnData);

        // add columns
        // pre-cache data to ensure all values present when we get
        synchronized (this) {
            for (IAnalysisDataset dataset : list) {
                for (IAnalysisDataset dataset2 : list) {
                    if (!dataset2.getUUID().equals(dataset.getUUID())) {
                        dataset.getCollection().countShared(dataset2);
                    }
                }
            }
        }

        for (IAnalysisDataset dataset : list) {

            finest("Fetching comparisons for " + dataset.getName());

            Object[] popData = new Object[list.size()];

            int i = 0;
            for (IAnalysisDataset dataset2 : list) {

                String valueString = "";

                if (!dataset2.getUUID().equals(dataset.getUUID())) {

                    int shared = dataset.getCollection().countShared(dataset2);

                    int d2size = dataset2.getCollection().size();

                    double pct = ((double) shared / (double) d2size) * 100;
                    if (d2size == 0) {
                        pct = 0;
                    }

                    valueString = DEFAULT_DECIMAL_FORMAT.format(pct) + "%";
                }

                popData[i++] = valueString;
            }
            model.addColumn(dataset.getName(), popData);
        }
        finer("Created venn table model");
        return model;
    }

    /**
     * Create a pairwise Venn table showing all combinations
     * 
     * @param list
     * @return
     */
    public TableModel createPairwiseVennTable() {

        if (!options.hasDatasets()) {
            finest("No datasets, creating blank pairwise venn table");
            return createBlankTable();
        }

        if (options.isSingleDataset()) {
            finest("Single dataset, creating blank pairwise venn table");
            return createBlankTable();
        }
        finer("Creating venn pairwise table model");
        DefaultTableModel model = new DefaultTableModel();

        Object[] columnNames = new Object[] { "Dataset 1", "Unique %", "Unique", "Shared %", "Shared", "Shared %",
                "Unique", "Unique %", "Dataset 2" };
        model.setColumnIdentifiers(columnNames);

        List<IAnalysisDataset> list = options.getDatasets();
        // Track the pairwase comparisons performed to avoid duplicates
        Map<UUID, ArrayList<UUID>> existingMatches = new HashMap<UUID, ArrayList<UUID>>();

        // add columns
        for (IAnalysisDataset dataset1 : list) {

            ArrayList<UUID> set1List = new ArrayList<UUID>();
            existingMatches.put(dataset1.getUUID(), set1List);

            for (IAnalysisDataset dataset2 : list) {

                // Ignore self-self matches
                if (!dataset2.getUUID().equals(dataset1.getUUID())) {

                    set1List.add(dataset2.getUUID());

                    if (existingMatches.get(dataset2.getUUID()) != null) {
                        if (existingMatches.get(dataset2.getUUID()).contains(dataset1.getUUID())) {
                            continue;
                        }
                    }

                    Object[] popData = new Object[9];

                    popData[0] = dataset1;
                    popData[8] = dataset2;

                    // compare the number of shared nucleus ids
                    int shared = dataset1.getCollection().countShared(dataset2);

                    popData[4] = shared;

                    int unique1 = dataset1.getCollection().size() - shared;
                    int unique2 = dataset2.getCollection().size() - shared;
                    popData[2] = unique1;
                    popData[6] = unique2;

                    double uniquePct1 = ((double) unique1 / (double) dataset1.getCollection().size()) * 100;
                    double uniquePct2 = ((double) unique2 / (double) dataset2.getCollection().size()) * 100;

                    popData[1] = DEFAULT_DECIMAL_FORMAT.format(uniquePct1);
                    popData[7] = DEFAULT_DECIMAL_FORMAT.format(uniquePct2);

                    double sharedpct1 = ((double) shared / (double) dataset1.getCollection().size()) * 100;
                    double sharedpct2 = ((double) shared / (double) dataset2.getCollection().size()) * 100;

                    popData[3] = DEFAULT_DECIMAL_FORMAT.format(sharedpct1);
                    popData[5] = DEFAULT_DECIMAL_FORMAT.format(sharedpct2);

                    model.addRow(popData);
                }
            }
        }
        finer("Created venn pairwise table model");
        return model;
    }

    /**
     * Create an empty table to display.
     * 
     * @param list
     * @return
     */
    private DefaultTableModel makeEmptyWilcoxonTable(List<IAnalysisDataset> list) {
        DefaultTableModel model = new DefaultTableModel();

        if (list == null) {
            Object[] columnData = { EMPTY_STRING };
            model.addColumn("Population", columnData);
            model.addColumn("", columnData);
        } else {

            // set rows
            Object[] columnData = new Object[list.size()];
            int row = 0;
            for (IAnalysisDataset dataset : list) {
                columnData[row] = dataset.getName();
                row++;
            }
            model.addColumn(Labels.DATASET, columnData);
        }
        return model;
    }

    /**
     * Carry out pairwise wilcoxon rank-sum test on the given stat of the given
     * datasets
     * 
     * @param options
     *            the table options
     * @return a tablemodel for display
     */
    public TableModel createWilcoxonStatisticTable(String component) {

        if (!options.hasDatasets()) {
            return makeEmptyWilcoxonTable(null);
        }

        if (CellularComponent.NUCLEUS.equals(component)) {
            return createWilcoxonNuclearStatTable();
        }

        if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)) {
            return createWilcoxonSegmentStatTable();
        }

        return makeEmptyWilcoxonTable(null);

    }

    /**
     * Carry out pairwise wilcoxon rank-sum test on the given stat of the given
     * datasets
     * 
     * @param list
     *            the datasets to test
     * @param stat
     *            the statistic to measure
     * @return a tablemodel for display
     */
    private TableModel createWilcoxonNuclearStatTable() {

        if (!options.hasDatasets()) {
            return makeEmptyWilcoxonTable(null);
        }

        DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());

        PlottableStatistic stat = options.getStat();

        // add columns
        DecimalFormat df = new DecimalFormat("#0.0000");
        for (IAnalysisDataset dataset : options.getDatasets()) {

            double[] d1Values = dataset.getCollection().getMedianStatistics(stat, CellularComponent.NUCLEUS,
                    MeasurementScale.PIXELS);

            Object[] popData = new Object[options.datasetCount()];

            int i = 0;
            boolean isGetPVal = false;
            for (IAnalysisDataset dataset2 : options.getDatasets()) {

                if (dataset2.getUUID().equals(dataset.getUUID())) {
                    popData[i] = "";
                    isGetPVal = true;
                } else {

                    double[] d2Values = dataset2.getCollection().getMedianStatistics(stat, CellularComponent.NUCLEUS,
                            MeasurementScale.PIXELS);

                    double pValue = Stats.runWilcoxonTest(d1Values, d2Values, isGetPVal);

                    popData[i] = df.format(pValue);
                }
                i++;
            }
            model.addColumn(dataset.getName(), popData);
        }
        return model;
    }

    /**
     * Carry out pairwise wilcoxon rank-sum test on the given stat of the
     * segments
     * 
     * @param list
     *            the datasets to test
     * @param stat
     *            the statistic to measure
     * @param segName
     *            the segment to create the table for
     * @return a tablemodel for display
     */
    private TableModel createWilcoxonSegmentStatTable() {
        if (!options.hasDatasets()) {
            return makeEmptyWilcoxonTable(null);
        }

        DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());

        // add columns
        DecimalFormat df = new DecimalFormat("#0.0000");
        for (IAnalysisDataset dataset : options.getDatasets()) {

            Object[] popData = new Object[options.datasetCount()];

            IBorderSegment medianSeg1;
            try {
                medianSeg1 = dataset.getCollection().getProfileCollection()
                        .getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
                        .getSegmentAt(options.getSegPosition());
            } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                    | UnsegmentedProfileException e) {
                fine("Error getting median profile", e);
                return createBlankTable();
            }

            int i = 0;
            boolean getPValue = false;
            for (IAnalysisDataset dataset2 : options.getDatasets()) {

                if (dataset2.getUUID().equals(dataset.getUUID())) {
                    popData[i] = "";
                    getPValue = true;
                } else {

                    IBorderSegment medianSeg2;
                    try {
                        medianSeg2 = dataset2.getCollection().getProfileCollection()
                                .getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
                                .getSegmentAt(options.getSegPosition());
                    } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                            | UnsegmentedProfileException e) {
                        fine("Error getting median profile", e);
                        return createBlankTable();
                    }

                    popData[i] = df.format(Stats.runWilcoxonTest(dataset.getCollection().getMedianStatistics(
                            PlottableStatistic.LENGTH, CellularComponent.NUCLEAR_BORDER_SEGMENT,
                            MeasurementScale.PIXELS, medianSeg1.getID()),

                            dataset2.getCollection().getMedianStatistics(PlottableStatistic.LENGTH,
                                    CellularComponent.NUCLEAR_BORDER_SEGMENT, MeasurementScale.PIXELS,
                                    medianSeg2.getID()),

                            getPValue));
                }
                i++;
            }
            model.addColumn(dataset.getName(), popData);
        }
        return model;
    }

    /**
     * Generate a table of magnitude difference between datasets
     * 
     * @param options
     *            the table options
     * @return a tablemodel for display
     */
    public TableModel createMagnitudeStatisticTable(String component) {

        if (!options.hasDatasets()) {
            return makeEmptyWilcoxonTable(null);
        }

        if (CellularComponent.NUCLEUS.equals(component)) {
            return createMagnitudeNuclearStatTable();
        }

        if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)) {
            return createMagnitudeSegmentStatTable();
        }

        return makeEmptyWilcoxonTable(null);

    }

    /**
     * Generate a table of magnitude difference between datasets
     * 
     * @param list
     *            the datasets to test
     * @param stat
     *            the statistic to measure
     * @return a tablemodel for display
     */
    private TableModel createMagnitudeNuclearStatTable() {
        if (!options.hasDatasets()) {
            return makeEmptyWilcoxonTable(null);
        }

        DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());

        PlottableStatistic stat = options.getStat();

        // add columns
        DecimalFormat df = new DecimalFormat("#0.0000");
        for (IAnalysisDataset dataset : options.getDatasets()) {

            double value1;
            try {
                value1 = dataset.getCollection().getMedianStatistic(stat, CellularComponent.NUCLEUS,
                        MeasurementScale.PIXELS);
            } catch (Exception e) {
                fine("Error getting median statistic", e);
                return createBlankTable();
            }

            Object[] popData = new Object[options.datasetCount()];

            int i = 0;

            for (IAnalysisDataset dataset2 : options.getDatasets()) {

                if (dataset2.getUUID().equals(dataset.getUUID())) {

                    popData[i] = "";

                } else {

                    double value2;
                    try {
                        value2 = dataset2.getCollection().getMedianStatistic(stat, CellularComponent.NUCLEUS,
                                MeasurementScale.PIXELS);
                    } catch (Exception e) {
                        fine("Error getting median statistic", e);
                        return createBlankTable();
                    }

                    double magnitude = value2 / value1;
                    popData[i] = df.format(magnitude);
                }
                i++;
            }
            model.addColumn(dataset.getName(), popData);
        }
        return model;
    }

    /**
     * Generate a table of segmment magnitude differences between datasets
     * 
     * @param list
     *            the datasets to test
     * @param stat
     *            the statistic to measure
     * @param segName
     *            the segment to create the table for
     * @return a tablemodel for display
     */
    private TableModel createMagnitudeSegmentStatTable() {
        if (!options.hasDatasets()) {
            return makeEmptyWilcoxonTable(null);
        }

        DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());

        // add columns
        DecimalFormat df = new DecimalFormat("#0.0000");
        for (IAnalysisDataset dataset : options.getDatasets()) {

            IBorderSegment medianSeg1;
            try {
                medianSeg1 = dataset.getCollection().getProfileCollection()
                        .getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
                        .getSegmentAt(options.getSegPosition());
            } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                    | UnsegmentedProfileException e) {
                fine("Error getting median profile", e);
                return createBlankTable();
            }

            double value1 = new Quartile(
                    dataset.getCollection().getMedianStatistics(PlottableStatistic.LENGTH,
                            CellularComponent.NUCLEAR_BORDER_SEGMENT, MeasurementScale.PIXELS, medianSeg1.getID()),
                    Quartile.MEDIAN).doubleValue();

            Object[] popData = new Object[options.datasetCount()];

            int i = 0;

            for (IAnalysisDataset dataset2 : options.getDatasets()) {

                if (dataset2.getUUID().equals(dataset.getUUID())) {
                    popData[i] = "";

                } else {

                    IBorderSegment medianSeg2;
                    try {
                        medianSeg2 = dataset2.getCollection().getProfileCollection()
                                .getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
                                .getSegmentAt(options.getSegPosition());
                    } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException
                            | UnsegmentedProfileException e) {
                        fine("Error getting median profile", e);
                        return createBlankTable();
                    }

                    double value2 = new Quartile(dataset2.getCollection().getMedianStatistics(PlottableStatistic.LENGTH,
                            CellularComponent.NUCLEAR_BORDER_SEGMENT, MeasurementScale.PIXELS, medianSeg2.getID()),
                            Quartile.MEDIAN).doubleValue();

                    double magnitude = value2 / value1;
                    popData[i] = df.format(magnitude);
                }
                i++;
            }
            model.addColumn(dataset.getName(), popData);
        }
        return model;
    }

    /**
     * Get the options used for clustering as a table
     * 
     * @param list
     * @return
     */
    public TableModel createClusterOptionsTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        // Check that there are some cluster groups to render
        boolean hasClusters = false;
        for (IAnalysisDataset d : options.getDatasets()) {
            if (d.hasClusters()) {
                hasClusters = true;
            }
        }

        if (!hasClusters) {
            return createBlankTable();
        }

        // Make the table model

        DefaultTableModel model = new DefaultTableModel();

        List<Object> columnList = new ArrayList<Object>();
        columnList.add("Cluster group");
        columnList.add("Clusters found");
        columnList.add("Method");
        columnList.add("Iterations");
        columnList.add("Hierarchical method");
        columnList.add("Target cluster number");
        columnList.add("Include profile");
        columnList.add("Profile type");
        columnList.add("Include mesh");

        NucleusType type = IAnalysisDataset.getBroadestNucleusType(options.getDatasets());

        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(type)) {
            columnList.add("Include " + stat.toString());
        }

        columnList.add("Include segments");
        columnList.add("Tree");

        model.addColumn(EMPTY_STRING, columnList.toArray());

        List<IAnalysisDataset> list = options.getDatasets();

        // format the numbers and make into a tablemodel
        for (IAnalysisDataset dataset : list) {
            List<IClusterGroup> clusterGroups = dataset.getClusterGroups();

            for (IClusterGroup g : clusterGroups) {
                IClusteringOptions op = g.getOptions();

                Object iterationString = op.getType().equals(ClusteringMethod.EM) ? op.getIterations() : Labels.NA;

                Object hierarchicalMethodString = op.getType().equals(ClusteringMethod.HIERARCHICAL)
                        ? op.getHierarchicalMethod().toString() : Labels.NA;

                Object hierarchicalClusterString = op.getType().equals(ClusteringMethod.HIERARCHICAL)
                        ? op.getClusterNumber() : Labels.NA;

                String tree = g.hasTree() ? g.getTree() : Labels.NA;

                List<Object> dataList = new ArrayList<Object>();
                dataList.add(g.getName());
                dataList.add(g.size());
                dataList.add(op.getType().toString());
                dataList.add(iterationString);

                dataList.add(hierarchicalMethodString);
                dataList.add(hierarchicalClusterString);
                dataList.add(op.isIncludeProfile());

                String profileTypeString = op.isIncludeProfile() ? op.getProfileType().toString() : "N/A";
                dataList.add(profileTypeString);

                dataList.add(op.isIncludeMesh());

                for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(type)) {
                    try {
                        dataList.add(op.isIncludeStatistic(stat));
                    } catch (NullPointerException e) {
                        dataList.add(Labels.NA);
                    }
                }

                boolean seg = false;
                for (UUID id : op.getSegments()) {
                    if (op.isIncludeSegment(id)) {
                        seg = true;
                    }
                }
                dataList.add(seg);
                dataList.add(tree);

                //
                model.addColumn(dataset.getName(), dataList.toArray());
            }
        }

        return model;
    }

}
