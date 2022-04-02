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
package com.bmskinner.nuclear_morphology.charting.datasets;

import java.io.File;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.classification.PrincipalComponentAnalysis;
import com.bmskinner.nuclear_morphology.analysis.classification.TsneMethod;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.AbstractOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.options.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.ConfidenceInterval;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Creates the tables for display in the UI.
 * @author bms41
 *
 */
public class AnalysisDatasetTableCreator extends AbstractTableCreator {
	
	private static final Logger LOGGER = Logger.getLogger(AnalysisDatasetTableCreator.class.getName());

    /**
     * Create with a set of table options
     */
    public AnalysisDatasetTableCreator(@NonNull final TableOptions o) {
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

    /**
     * Create a table of segment stats for median profile of the given dataset.
     * 
     * @param dataset
     *            the AnalysisDataset to include
     * @return a table model
     * @throws Exception
     */
    private TableModel createMedianProfileSegmentStatsTable(@Nullable IAnalysisDataset dataset, MeasurementScale scale) {

        DefaultTableModel model = new DefaultTableModel();

        if (dataset == null) {
            model.addColumn(Labels.NO_DATA_LOADED);

        } else {
            ICellCollection collection = dataset.getCollection();
            // check which reference point to use
            Landmark point = Landmark.REFERENCE_POINT;

            // get mapping from ordered segments to segment names
            List<IProfileSegment> segments;
            try {
                segments = collection.getProfileCollection()
                        .getSegmentedProfile(ProfileType.ANGLE, point, Stats.MEDIAN).getOrderedSegments();
            } catch (MissingLandmarkException | ProfileException | MissingProfileException
                     e) {
                LOGGER.log(Loggable.STACK, "Error getting median profile", e);
                return createBlankTable();
            }

            // create the row names
            Object[] fieldNames = { "Colour", "Length", "Start index", "End index", "Mean length (" + scale + ")",
                    "Mean length 95% CI (" + scale + ")", "Length std err. (" + scale + ")", "Length p(unimodal)" };

            model.addColumn(EMPTY_STRING, fieldNames);
            
            DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
            DecimalFormat pf = new DecimalFormat(DEFAULT_PROBABILITY_FORMAT);

            for (IProfileSegment segment : segments) {

                List<Object> rowData = new ArrayList<>();

                rowData.add("");
                rowData.add(segment.length());
                rowData.add(segment.getStartIndex());
                rowData.add(segment.getEndIndex());

                double[] meanLengths = collection.getRawValues(Measurement.LENGTH,
                        CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, segment.getID());

                double mean = DoubleStream.of(meanLengths).average().orElse(0);

                double sem = Stats.stderr(meanLengths);

                ConfidenceInterval ci = new ConfidenceInterval(meanLengths, 0.95);

                rowData.add(df.format(mean));
                rowData.add(df.format(ci.getLower().doubleValue()) + " - "
                        + df.format(ci.getUpper().doubleValue()));
                rowData.add(df.format(sem));

                model.addColumn(segment.getName(), rowData.toArray(new Object[0]));
            }
        }
        return model;
    }

    /**
     * Create a table of segment stats for median profile of the given dataset.
     * 
     * @param dataset the AnalysisDataset to include
     * @return a table model
     */
    private TableModel createMultiDatasetMedianProfileSegmentStatsTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        DefaultTableModel model = new DefaultTableModel();

        // If the datasets have different segment counts, show error message
        if (!IProfileSegment.segmentCountsMatch(options.getDatasets())) {
            model.addColumn(Labels.INCONSISTENT_SEGMENT_NUMBER);
            return model;
        }

        MeasurementScale scale = options.getScale();

        List<Object> fieldNames = new ArrayList<>();

        // assumes all datasets have the same number of segments
        List<IProfileSegment> segments;
        try {
            segments = options.firstDataset().getCollection().getProfileCollection().getSegments(Landmark.REFERENCE_POINT);
        } catch (MissingLandmarkException | ProfileException e1) {
            LOGGER.log(Loggable.STACK, "Error getting segments from profile collection", e1);
            return createBlankTable();
        }

        // Add the dataset names column
        fieldNames.add(Labels.DATASET);
        
        List<Object> colours = new ArrayList<>();
        colours.add(EMPTY_STRING);
        
        for (IProfileSegment segment : segments) {
            fieldNames.add(segment.getName());
            colours.add(EMPTY_STRING); // Add the segment colours columns
        }
        
        model.setColumnIdentifiers(fieldNames.toArray());
        model.addRow(colours.toArray(new Object[0]));

        // Add the segment stats columns
        DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);

        for (IAnalysisDataset dataset : options.getDatasets()) {

            ICellCollection collection = dataset.getCollection();

            try {
            	List<IProfileSegment> segs = collection.getProfileCollection()
            			.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN)
                        .getOrderedSegments();
            	
            	List<Object> rowData = new ArrayList<>();
                rowData.add(dataset.getName());
                
                for (IProfileSegment segment : segs) {

                    double[] meanLengths = collection.getRawValues(Measurement.LENGTH,
                            CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, segment.getID());
                    double mean = DoubleStream.of(meanLengths).average().orElse(0);

                    ConfidenceInterval ci = new ConfidenceInterval(meanLengths, 0.95);
                    rowData.add(df.format(mean) + " ± "
                            + df.format(ci.getSize().doubleValue()));
                }
                model.addRow(rowData.toArray(new Object[0]));
            } catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
                LOGGER.log(Loggable.STACK, "Error getting median profile", e);
                return createBlankTable();
            }
        }

        return model;
    }
 
    /**
     * Create a table model of analysis parameters from a nucleus collection. If
     * null parameter is passed, will create an empty table
     * @return
     */
    public TableModel createAnalysisParametersTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        DefaultTableModel model = new DefaultTableModel();
        List<Object> columnList = new ArrayList<>();
        columnList.add(Labels.AnalysisParameters.IMAGE_PREPROCESSING);
        columnList.add(Labels.AnalysisParameters.NUCLEUS_DETECTION);
        columnList.add(Labels.AnalysisParameters.NUCLEUS_SIZE);
        columnList.add(Labels.AnalysisParameters.NUCLEUS_CIRCULARITY);
        columnList.add(Labels.AnalysisParameters.RUN_TIME);
        columnList.add(Labels.AnalysisParameters.COLLECTION_SOURCE);
        columnList.add(Labels.AnalysisParameters.NUCLEUS_TYPE);
        columnList.add(Labels.AnalysisParameters.PROFILE_WINDOW);
        columnList.add(Labels.AnalysisParameters.SOFTWARE_VERSION);
        
        if(options.getBoolean(AbstractOptions.SHOW_RECOVER_MERGE_SOURCE_KEY))
        	columnList.add(Labels.Merges.RECOVER_SOURCE);
        model.addColumn(EMPTY_STRING, columnList.toArray());

        List<IAnalysisDataset> list = options.getDatasets();

        for (IAnalysisDataset dataset : list) {

            // only display if there are options available
            // This may not be the case for a merged dataset or its children
            if (dataset.hasAnalysisOptions()) {

                // Do not provide an options as an argument here.
                // This will cause the existing dataset options to be used
            	List<Object> collectionData = createAnalysisParametersColumn(dataset, null);

                model.addColumn(dataset.getCollection().getName(), collectionData.toArray());

            } else {
                LOGGER.fine("No analysis options in dataset " + dataset.getName()+", treating as merge");
                List<Object> collectionData = createAnalysisParametersMergeColumn(dataset);
                model.addColumn(dataset.getCollection().getName(), collectionData.toArray());
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
    private List<Object> createAnalysisParametersMergeColumn(@NonNull IAnalysisDataset dataset) {
    	List<Object> dataList = new ArrayList<>();

        if (dataset.hasMergeSources()) {
            if (IAnalysisDataset.mergedSourceOptionsAreSame(dataset)) {

                // The options are the same in all merge sources
                // Show the first options from the first source
                List<IAnalysisDataset> l = new ArrayList<>(dataset.getAllMergeSources());

                Optional<IAnalysisOptions> o = l.get(0).getAnalysisOptions();
                if(o.isPresent())
                	dataList = createAnalysisParametersColumn(dataset, o.get());
                	
            } else {
            	return makeStringList(Labels.NA_MERGE, 9);
            }
        } else {
        	// if this is a child of merged datasets, get the root.
        	if(!dataset.isRoot()){
        		return createAnalysisParametersMergeColumn(DatasetListManager.getInstance().getRootParent(dataset));

        	} else {
        		return makeStringList(Labels.NA, 9);
        	}
        }
        return dataList;
    }
    
    /**
     * Get an array of formatted info from a dataset analysis options
     * 
     * @param dataset the dataset to format options for
     * @param options an options to use instead of the dataset's own options. Can be
     *            null.
     * @return
     */
    private List<Object> createAnalysisParametersColumn(@NonNull IAnalysisDataset dataset, @Nullable IAnalysisOptions options) {
        
    	int numberOfRows = 9;
    	List<Object> dataList = new ArrayList<>();
    	Optional<IAnalysisOptions> optOptions = dataset.getAnalysisOptions();
    	if(options == null && !optOptions.isPresent())
    		return makeStringList("Error - no options present", numberOfRows);
    	
    	
    	// If no options were supplied, use the dataset's own options 
    	options = options == null ? optOptions.get() : options;
        String folder;

        Optional<HashOptions> nO = options.getDetectionOptions(CellularComponent.NUCLEUS);
        
        if(!nO.isPresent()) {
        	LOGGER.log(Loggable.STACK, "No nucleus options in dataset "+dataset.getName());
        	return makeStringList("Error", numberOfRows);
        }
        
        if (dataset.hasMergeSources()) {
            folder = Labels.NA_MERGE;
        } else {
        	File optionsFolder = new File(nO.get().getString(HashOptions.DETECTION_FOLDER));
        	folder = optionsFolder.getAbsolutePath();
        }

        dataList.add(createImagePreprocessingString(nO.get()));
        dataList.add(createNucleusEdgeDetectionString(nO.get()));
        dataList.add(createNucleusSizeFilterString(nO.get()));
        dataList.add(createNucleusCircFilterString(nO.get()));
        dataList.add(createAnalysisRunTimeString(options));
        dataList.add(folder);
        dataList.add(options.getRuleSetCollection().getName());
        dataList.add(options.getProfileWindowProportion());
        dataList.add(dataset.getVersionCreated().toString());
        
        if(this.options.getBoolean(AbstractOptions.SHOW_RECOVER_MERGE_SOURCE_KEY))
        	dataList.add(dataset);
        return dataList;
    }
    
    private String createImagePreprocessingString(@Nullable HashOptions options) {
    	StringBuilder builder = new StringBuilder();
    	if(options==null) {
    		builder.append(Labels.NA);
    		return builder.toString();
    	}
		
		if(options.getBoolean(HashOptions.IS_USE_KUWAHARA))
			builder.append("Kuwahara kernel: "+options.getInt(HashOptions.KUWAHARA_RADIUS_INT)+Io.NEWLINE);
		if(options.getBoolean(HashOptions.IS_USE_FLATTENING))
			builder.append("Flattening threshold: "+options.getInt(HashOptions.FLATTENING_THRESHOLD_INT));	
		return builder.toString();
    }

    private String createNucleusEdgeDetectionString(@Nullable HashOptions nucleusOptions) {
    	StringBuilder builder = new StringBuilder();
    	if(nucleusOptions==null)
    		return builder.toString();

		boolean isCanny = nucleusOptions.getBoolean(HashOptions.IS_USE_CANNY);
		if(isCanny) {
			builder.append("Canny edge detection"+Io.NEWLINE);
			
			if(nucleusOptions.getBoolean(HashOptions.CANNY_IS_AUTO_THRESHOLD)) {
				builder.append("Auto-threshold"+Io.NEWLINE);
			} else {
				builder.append("Low threshold: "+nucleusOptions.getFloat(HashOptions.CANNY_LOW_THRESHOLD_FLT)+Io.NEWLINE);
				builder.append("High threshold: "+nucleusOptions.getFloat(HashOptions.CANNY_HIGH_THRESHOLD_FLT)+Io.NEWLINE);
			}
			builder.append("Kernel radius: "+nucleusOptions.getFloat(HashOptions.CANNY_KERNEL_RADIUS_FLT)+Io.NEWLINE);
			builder.append("Kernel width: "+nucleusOptions.getInt(HashOptions.CANNY_KERNEL_WIDTH_INT)+Io.NEWLINE);
			builder.append("Closing radius: "+nucleusOptions.getInt(HashOptions.CANNY_CLOSING_RADIUS_INT));
		} else {
			builder.append("Threshold: "+nucleusOptions.getInt(HashOptions.THRESHOLD));
		}		
		return builder.toString();
    }
    
    private String createNucleusSizeFilterString(@Nullable HashOptions nucleusOptions) {
    	StringBuilder builder = new StringBuilder();
    	if(nucleusOptions==null) {
    		builder.append(Labels.NA);
    		return builder.toString();
    	}
    	builder.append("Min pixels: "+nucleusOptions.getInt(HashOptions.MIN_SIZE_PIXELS)
    	+Io.NEWLINE+"Max pixels: "+nucleusOptions.getInt(HashOptions.MAX_SIZE_PIXELS));
		return builder.toString();
    }
    
    private String createNucleusCircFilterString(@Nullable HashOptions nucleusOptions) {
    	StringBuilder builder = new StringBuilder();
    	DecimalFormat formatter = new DecimalFormat("#.##");
    	if(nucleusOptions==null) {
    		builder.append(Labels.NA);
    		return builder.toString();
    	}
    	builder.append("Min: "+formatter.format(nucleusOptions.getDouble(HashOptions.MIN_CIRC))
    	+Io.NEWLINE+"Max: "+formatter.format(nucleusOptions.getDouble(HashOptions.MAX_CIRC)));
    	return builder.toString();
    }

    private String createAnalysisRunTimeString(@Nullable IAnalysisOptions analysisOptions) {
    	StringBuilder builder = new StringBuilder();
    	if(analysisOptions==null) {
    		builder.append(Labels.NA);
    		return builder.toString();
    	}
    	long analysisTime = analysisOptions.getAnalysisTime();
    	if(analysisTime>0) { // stored from 1.14.0
    		Instant inst = Instant.ofEpochMilli(analysisTime);
    		LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());
    		
    		
    		String date = anTime.format(DateTimeFormatter.ofPattern("dd MMMM YYYY"));
    		String time = anTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    		builder.append(date+Io.NEWLINE+time);
    	} else { // fall back to folder name method
    		builder.append(Labels.NA);
    	}
		return builder.toString();
    }
    
    
    private List<Object> makeStringList(String s, int rows){
    	List<Object> dataList = new ArrayList<>();
    	for(int i=0; i<rows; i++) {
    		dataList.add(s);
    	}
    	return dataList;
//    	Object[] err = new Object[rows];
//        Arrays.fill(err, 0, rows-1, "Error");
//        return err;
    }

    /**
     * Create a table model of basic stats from a nucleus collection. If null
     * parameter is passed, will create an empty table
     * 
     * @param collection
     * @return
     */
    public TableModel createNucleusStatsTable() {

        if (!options.hasDatasets()) {
            return createBlankTable();
        }

        final String NUCLEUS_LABEL = "Nuclei";
        final String[] valueLabels = { " median", " mean", " S.E.M.", 
        		" coefficient of variation", " mean 95% CI", " p(unimodal)" };

        DefaultTableModel model = new DefaultTableModel();

        List<IAnalysisDataset> list = options.getDatasets();

        List<Object> columnData = new ArrayList<>();
        columnData.add(NUCLEUS_LABEL);
        for (Measurement stat : Measurement.getNucleusStats()) {
            for (String value : valueLabels) {
                columnData.add(stat.toString() + value);
            }
        }

        model.addColumn(EMPTY_STRING, columnData.toArray());

        for (IAnalysisDataset dataset : list) {
            List<Object> datasetData = createDatasetStatsTableColumn(dataset, options.getScale());
            model.addColumn(dataset.getName(), datasetData.toArray());
        }

        return model;
    }

    private List<Object> createDatasetStatsTableColumn(@NonNull IAnalysisDataset dataset, MeasurementScale scale) {

        // format the numbers and make into a tablemodel
        DecimalFormat pf = new DecimalFormat(DEFAULT_PROBABILITY_FORMAT);

        ICellCollection collection = dataset.getCollection();

        List<Object> datasetData = new ArrayList<>();

        datasetData.add(collection.size());

        DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
        for (Measurement stat : Measurement.getNucleusStats()) {
            double[] stats = collection.getRawValues(stat, CellularComponent.NUCLEUS, scale);

            double mean = DoubleStream.of(stats).average().orElse(0);
            double sem = Stats.stderr(stats);
            double cv = Stats.stdev(stats)/mean;
            
            double median = Stats.quartile(stats, Stats.MEDIAN);

            ConfidenceInterval ci = new ConfidenceInterval(stats, 0.95);
            String ciString = df.format(mean) + " ± "
                    + df.format(ci.getSize().doubleValue());

            datasetData.add(df.format(median));
            datasetData.add(df.format(mean));
            datasetData.add(df.format(sem));
            datasetData.add(df.format(cv));
            datasetData.add(ciString);
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

        DefaultTableModel model = new DefaultTableModel();

        List<IAnalysisDataset> list = options.getDatasets();

        // set rows
        Object[] columnData = new Object[list.size()];
        int row = 0;
        for (IAnalysisDataset dataset : list) {
            columnData[row++] = dataset.getName();
        }
        model.addColumn(Labels.DATASET, columnData);
        
        DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);

        for (IAnalysisDataset dataset : list) {

            Object[] popData = new Object[list.size()];

            int i = 0;
            for (IAnalysisDataset dataset2 : list) {

                String valueString = "";

                if (!dataset2.getId().equals(dataset.getId())) {

                    int shared = dataset.getCollection().countShared(dataset2);

                    int d2size = dataset2.getCollection().size();

                    double pct = ((double) shared / (double) d2size) * 100;
                    if (d2size == 0) {
                        pct = 0;
                    }

                    valueString = df.format(pct) + "%";
                }

                popData[i++] = valueString;
            }
            model.addColumn(dataset.getName(), popData);
        }
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
            return createBlankTable();
        }

        if (options.isSingleDataset()) {
            return createBlankTable();
        }
        
        DefaultTableModel model = new DefaultTableModel();

        Object[] columnNames = new Object[] { "Dataset 1", "Unique %", "Unique", "Shared %", "Shared", "Shared %",
                "Unique", "Unique %", "Dataset 2" };
        model.setColumnIdentifiers(columnNames);

        List<IAnalysisDataset> list = options.getDatasets();
        // Track the pairwase comparisons performed to avoid duplicates
        Map<UUID, List<UUID>> existingMatches = new HashMap<>();

        // add columns
        DecimalFormat df = new DecimalFormat(DEFAULT_DECIMAL_FORMAT);
        for (IAnalysisDataset dataset1 : list) {

            List<UUID> set1List = new ArrayList<>();
            existingMatches.put(dataset1.getId(), set1List);

            for (IAnalysisDataset dataset2 : list) {

                // Ignore self-self matches
                if (!dataset2.getId().equals(dataset1.getId())) {

                    set1List.add(dataset2.getId());

                    if (existingMatches.get(dataset2.getId()) != null && existingMatches.get(dataset2.getId()).contains(dataset1.getId())) {
                    	continue;
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

                    popData[1] = df.format(uniquePct1);
                    popData[7] = df.format(uniquePct2);

                    double sharedpct1 = ((double) shared / (double) dataset1.getCollection().size()) * 100;
                    double sharedpct2 = ((double) shared / (double) dataset2.getCollection().size()) * 100;

                    popData[3] = df.format(sharedpct1);
                    popData[5] = df.format(sharedpct2);

                    model.addRow(popData);
                }
            }
        }
        LOGGER.finer( "Created venn pairwise table model");
        return model;
    }

    /**
     * Create an empty table to display.
     * 
     * @param list
     * @return
     */
    private DefaultTableModel makeEmptyWilcoxonTable(@Nullable List<IAnalysisDataset> list) {
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
    public TableModel createWilcoxonStatisticTable(@Nullable String component) {

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

        Measurement stat = options.getStat();
        
        MeasurementScale scale = GlobalOptions.getInstance().getScale();

        int nComparisons = (options.datasetCount()*(options.datasetCount()-1))/2;
        nComparisons *= Measurement.getNucleusStats().size(); // correct for each set of analyses also
        // add columns
        DecimalFormat df = new DecimalFormat(DEFAULT_PROBABILITY_FORMAT);
        for (IAnalysisDataset dataset : options.getDatasets()) {

            double[] d1Values = dataset.getCollection().getRawValues(stat, CellularComponent.NUCLEUS,
            		scale);

            Object[] popData = new Object[options.datasetCount()];

            int i = 0;
            boolean isGetPVal = false;
            for (IAnalysisDataset dataset2 : options.getDatasets()) {

                if (dataset2.getId().equals(dataset.getId())) {
                    popData[i] = "";
                    isGetPVal = true;
                } else {

                    double[] d2Values = dataset2.getCollection().getRawValues(stat, CellularComponent.NUCLEUS,
                    		scale);

                    double pValue = Stats.runWilcoxonTest(d1Values, d2Values, isGetPVal, nComparisons );

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
        DecimalFormat df = new DecimalFormat(DEFAULT_PROBABILITY_FORMAT);
        int nComparisons = (options.datasetCount()*(options.datasetCount()-1))/2;
        for (IAnalysisDataset dataset : options.getDatasets()) {

            Object[] popData = new Object[options.datasetCount()];

            IProfileSegment medianSeg1;
            try {
                medianSeg1 = dataset.getCollection().getProfileCollection()
                        .getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN)
                        .getSegments().get(options.getSegPosition());
//                        .getSegmentAt(options.getSegPosition());
            } catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
                LOGGER.log(Loggable.STACK, "Error getting median profile", e);
                return createBlankTable();
            }

            int i = 0;
            boolean getPValue = false;
            
            for (IAnalysisDataset dataset2 : options.getDatasets()) {

                if (dataset2.getId().equals(dataset.getId())) {
                    popData[i] = "";
                    getPValue = true;
                } else {

                    IProfileSegment medianSeg2;
                    try {
                        medianSeg2 = dataset2.getCollection().getProfileCollection()
                                .getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN)
                                .getSegments().get(options.getSegPosition());
//                                .getSegmentAt(options.getSegPosition());
                        
                    } catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
                        LOGGER.log(Loggable.STACK, "Error getting median profile", e);
                        return createBlankTable();
                    }

                    popData[i] = df.format(Stats.runWilcoxonTest(dataset.getCollection().getRawValues(
                            Measurement.LENGTH, CellularComponent.NUCLEAR_BORDER_SEGMENT,
                            MeasurementScale.PIXELS, medianSeg1.getID()),

                            dataset2.getCollection().getRawValues(Measurement.LENGTH,
                                    CellularComponent.NUCLEAR_BORDER_SEGMENT, MeasurementScale.PIXELS,
                                    medianSeg2.getID()),

                            getPValue, nComparisons));
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
    public TableModel createMagnitudeStatisticTable(@Nullable String component) {

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

        Measurement stat = options.getStat();
        
        MeasurementScale scale = GlobalOptions.getInstance().getScale();

        // add columns
        DecimalFormat df = new DecimalFormat("#0.0000");
        for (IAnalysisDataset dataset : options.getDatasets()) {

            double value1;
            try {
                value1 = dataset.getCollection().getMedian(stat, CellularComponent.NUCLEUS,
                		scale);
            } catch (Exception e) {
                LOGGER.log(Loggable.STACK, "Error getting median statistic", e);
                return createBlankTable();
            }

            Object[] popData = new Object[options.datasetCount()];

            int i = 0;

            for (IAnalysisDataset dataset2 : options.getDatasets()) {

                if (dataset2.getId().equals(dataset.getId())) {

                    popData[i] = "";

                } else {

                    double value2;
                    try {
                        value2 = dataset2.getCollection().getMedian(stat, CellularComponent.NUCLEUS,
                        		scale);
                    } catch (Exception e) {
                        LOGGER.log(Loggable.STACK, "Error getting median statistic", e);
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
        if (!options.hasDatasets())
            return makeEmptyWilcoxonTable(null);

        DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());
        MeasurementScale scale = GlobalOptions.getInstance().getScale();
        // add columns
        DecimalFormat df = new DecimalFormat("#0.0000");
        for (IAnalysisDataset dataset : options.getDatasets()) {

            IProfileSegment medianSeg1;
            try {
                medianSeg1 = dataset.getCollection().getProfileCollection()
                        .getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN)
                        .getSegments().get(options.getSegPosition());
//                        .getSegmentAt(options.getSegPosition());
            } catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
                LOGGER.log(Loggable.STACK, "Error getting median profile", e);
                return createBlankTable();
            }
            
            DescriptiveStatistics ds = new DescriptiveStatistics(
                    dataset.getCollection().getRawValues(Measurement.LENGTH,
                            CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, medianSeg1.getID()));
                    double value1 = ds.getPercentile(Stats.MEDIAN);
            
            

            Object[] popData = new Object[options.datasetCount()];

            int i = 0;

            for (IAnalysisDataset dataset2 : options.getDatasets()) {

                if (dataset2.getId().equals(dataset.getId())) {
                    popData[i] = "";

                } else {

                    IProfileSegment medianSeg2;
                    try {
                        medianSeg2 = dataset2.getCollection().getProfileCollection()
                                .getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN)
                                .getSegments().get(options.getSegPosition());
//                                .getSegmentAt(options.getSegPosition());
                    } catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
                        LOGGER.log(Loggable.STACK, "Error getting median profile", e);
                        return createBlankTable();
                    }

                    DescriptiveStatistics ss = new DescriptiveStatistics(
                    dataset2.getCollection().getRawValues(Measurement.LENGTH,
                            CellularComponent.NUCLEAR_BORDER_SEGMENT, scale, medianSeg2.getID()));
                    double value2 = ss.getPercentile(Stats.MEDIAN);
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
     * Get the options used for creating cluster groups
     * 
     * @return
     */
    public TableModel createClusterOptionsTable() {

        if (!options.hasDatasets())
            return createBlankTable();

        // Check that there are some cluster groups to render        
        if(options.getDatasets().stream().noneMatch(IAnalysisDataset::hasClusters))
        	return createBlankTable();

        // Make the table model
        DefaultTableModel model = new DefaultTableModel();

        List<Object> columnList = new ArrayList<>();
        columnList.add(Labels.Clusters.CLUSTER_GROUP);
        columnList.add(Labels.Clusters.CLUSTER_FOUND);
        columnList.add(Labels.Clusters.CLUSTER_PARAMS);
        columnList.add(Labels.Clusters.CLUSTER_DIM_RED);
        columnList.add(Labels.Clusters.CLUSTER_DIM_PLOT);
        columnList.add(Labels.Clusters.CLUSTER_METHOD);
        columnList.add(Labels.Clusters.TREE);

        model.addColumn(EMPTY_STRING, columnList.toArray());

        List<IAnalysisDataset> list = options.getDatasets();

        // format the numbers and make into a tablemodel
        for (IAnalysisDataset dataset : list) {
            List<IClusterGroup> clusterGroups = dataset.getClusterGroups();

            for (IClusterGroup g : clusterGroups) {  	
            	List<Object> dataList = new ArrayList<>();
            	dataList.add(g);
                dataList.add(String.valueOf(g.size()));
            	dataList.add(createClusterParameterString(g));
            	dataList.add(createDimensionalReductionString(g));
            	dataList.add(createDimensionalPlotString(g));
            	dataList.add(createClusterMethodString(g));
            	dataList.add(g.hasTree() ? Labels.Clusters.CLUSTER_SHOW_TREE : Labels.NA);   
                model.addColumn(dataset.getName(), dataList.toArray());
            }
        }
        return model;
    }
    
    private String createDimensionalPlotString(IClusterGroup group) {
    	StringBuilder builder = new StringBuilder();
    	Optional<HashOptions> opn = group.getOptions();
    	
    	if(!opn.isPresent()) {
    		builder.append(Labels.NA);
    		return builder.toString();
    	}

    	HashOptions op = opn.get();
        if(op.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY) || op.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY)) {
        	builder.append(Labels.Clusters.VIEW_PLOT);
        }

        String s = builder.toString();
        if(s.equals(EMPTY_STRING))
        	return Labels.NA;
        return s;
    }
    
    private String createClusterParameterString(IClusterGroup group) {
    	StringBuilder builder = new StringBuilder();
    	Optional<HashOptions> opn = group.getOptions();
    	
    	if(!opn.isPresent()) {
    		builder.append(Labels.NA);
    		return builder.toString();
    	}

        HashOptions op = opn.get();

        for(ProfileType t : ProfileType.displayValues())
        	if(op.getBoolean(t.toString()))
        		builder.append(t+Io.NEWLINE);
 
        for (Measurement stat : Measurement.getNucleusStats())
        	if(op.getBoolean(stat.toString()))
        		builder.append(stat.toString()+Io.NEWLINE);

        for(String s : op.getStringKeys()) {
        	try {
        		UUID id = UUID.fromString(s);
        		if(op.getBoolean(id.toString()))
            		builder.append("Segment_"+id.toString()+Io.NEWLINE);
        	} catch(IllegalArgumentException e) {
        		//not a UUID, skip
        	}
        }
        	
        
        String s = builder.toString();
        if(s.equals(EMPTY_STRING))
        	return Labels.NA;
        return s;
    }

    
    private String createDimensionalReductionString(IClusterGroup group) {
    	StringBuilder builder = new StringBuilder();
    	Optional<HashOptions> opn = group.getOptions();
    	
    	if(!opn.isPresent()) {
    		builder.append(Labels.NA);
    		return builder.toString();
    	}
    	
        HashOptions op = opn.get();
        if(op.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY)) {
        	builder.append(Labels.Clusters.TSNE+Io.NEWLINE);
        	builder.append(Labels.Clusters.TSNE_PERPLEXITY+": "+op.getDouble(TsneMethod.PERPLEXITY_KEY)+Io.NEWLINE);
        	builder.append(Labels.Clusters.TSNE_MAX_ITER+": "+op.getInt(TsneMethod.MAX_ITERATIONS_KEY));
        }
        
        if(op.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY)) {
        	builder.append(Labels.Clusters.PCA+Io.NEWLINE);
        	builder.append(Labels.Clusters.PCA_VARIANCE+": "+op.getDouble(PrincipalComponentAnalysis.PROPORTION_VARIANCE_KEY)+Io.NEWLINE);
        	builder.append(Labels.Clusters.PCA_NUM_PCS+": "+op.getInt(HashOptions.CLUSTER_NUM_PCS_KEY)+Io.NEWLINE);
        }

        String s = builder.toString();
        if(s.equals(EMPTY_STRING))
        	return Labels.NA;
        return s;
    }
    
    private String createClusterMethodString(IClusterGroup group) {
    	StringBuilder builder = new StringBuilder();
    	Optional<HashOptions> opn = group.getOptions();
    	
    	if(!opn.isPresent()) {
    		builder.append(Labels.NA);
    		return builder.toString();
    	}
    	
    	HashOptions op = opn.get();
    	
    	ClusteringMethod method = ClusteringMethod.valueOf(op.getString(HashOptions.CLUSTER_METHOD_KEY));
    	builder.append(method+Io.NEWLINE);
    	if(method.equals(ClusteringMethod.EM)) {
    		builder.append(op.getInt(HashOptions.CLUSTER_EM_ITERATIONS_KEY)+" iterations"+Io.NEWLINE);
    	}
    	
    	if(method.equals(ClusteringMethod.HIERARCHICAL)) {
    		builder.append("Distance: "+op.getString(HashOptions.CLUSTER_HIERARCHICAL_METHOD_KEY)+Io.NEWLINE);
    	}
        return builder.toString();
    }
}
