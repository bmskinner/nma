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
package com.bmskinner.nuclear_morphology.visualisation.tables;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;
import com.bmskinner.nuclear_morphology.stats.Stats.WilcoxonRankSumResult;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;

/**
 * Creates the tables for display in the UI.
 * 
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

	/**
	 * Create a table model of analysis parameters from a nucleus collection. If
	 * null parameter is passed, will create an empty table
	 * 
	 * @return
	 */
	public TableModel createAnalysisParametersTable() {

		return new AnalysisParametersTableModel(options.getDatasets());

//		if (!options.hasDatasets()) {
//			return createBlankTable();
//		}
//
//		DefaultTableModel model = new DefaultTableModel();
//		List<Object> columnList = new ArrayList<>();
//		columnList.add(Labels.AnalysisParameters.IMAGE_PREPROCESSING);
//		columnList.add(Labels.AnalysisParameters.NUCLEUS_DETECTION);
//		columnList.add(Labels.AnalysisParameters.NUCLEUS_SIZE);
//		columnList.add(Labels.AnalysisParameters.NUCLEUS_CIRCULARITY);
//		columnList.add(Labels.AnalysisParameters.RUN_TIME);
//		columnList.add(Labels.AnalysisParameters.COLLECTION_SOURCE);
//		columnList.add(Labels.AnalysisParameters.NUCLEUS_TYPE);
//		columnList.add(Labels.AnalysisParameters.PROFILE_WINDOW);
//		columnList.add(Labels.AnalysisParameters.SOFTWARE_VERSION);
//
//		if (options.getBoolean(AbstractOptions.SHOW_RECOVER_MERGE_SOURCE_KEY))
//			columnList.add(Labels.Merges.RECOVER_SOURCE);
//		model.addColumn(EMPTY_STRING, columnList.toArray());
//
//		List<IAnalysisDataset> list = options.getDatasets();
//
//		for (IAnalysisDataset dataset : list) {
//
//			// only display if there are options available
//			// This may not be the case for a merged dataset or its children
//			if (dataset.hasAnalysisOptions()) {
//
//				// Do not provide an options as an argument here.
//				// This will cause the existing dataset options to be used
//				List<Object> collectionData = createAnalysisParametersColumn(dataset, null);
//
//				model.addColumn(dataset.getCollection().getName(), collectionData.toArray());
//
//			} else {
//				LOGGER.fine("No analysis options in dataset " + dataset.getName() + ", treating as merge");
//				List<Object> collectionData = createAnalysisParametersMergeColumn(dataset);
//				model.addColumn(dataset.getCollection().getName(), collectionData.toArray());
//			}
//		}
//
//		return model;
	}

	/**
	 * Create an analysis parameter column for a dataset with no top level options
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
//				if (o.isPresent())
//					dataList = createAnalysisParametersColumn(dataset, o.get());

			} else {
				return makeStringList(Labels.NA_MERGE, 9);
			}
		} else {
			// if this is a child of merged datasets, get the root.
			if (!dataset.isRoot()) {
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
	 *                null.
	 * @return
	 */
//	private List<Object> createAnalysisParametersColumn(@NonNull IAnalysisDataset dataset,
//			@Nullable IAnalysisOptions options) {
//
//		int numberOfRows = 9;
//		List<Object> dataList = new ArrayList<>();
//		Optional<IAnalysisOptions> optOptions = dataset.getAnalysisOptions();
//		if (options == null && !optOptions.isPresent())
//			return makeStringList("Error - no options present", numberOfRows);
//
//		// If no options were supplied, use the dataset's own options
//		options = options == null ? optOptions.get() : options;
//		String folder;
//
//		Optional<HashOptions> nO = options.getDetectionOptions(CellularComponent.NUCLEUS);
//
//		if (!nO.isPresent()) {
//			LOGGER.log(Loggable.STACK, "No nucleus options in dataset " + dataset.getName());
//			return makeStringList("Error", numberOfRows);
//		}
//
//		if (dataset.hasMergeSources()) {
//			folder = Labels.NA_MERGE;
//		} else {
//			File optionsFolder = new File(nO.get().getString(HashOptions.DETECTION_FOLDER));
//			folder = optionsFolder.getAbsolutePath();
//		}
//
//		dataList.add(createImagePreprocessingString(nO.get()));
//		dataList.add(createNucleusEdgeDetectionString(nO.get()));
//		dataList.add(createNucleusSizeFilterString(nO.get()));
//		dataList.add(createNucleusCircFilterString(nO.get()));
//		dataList.add(createAnalysisRunTimeString(options));
//		dataList.add(folder);
//		dataList.add(options.getRuleSetCollection().getName());
//		dataList.add(options.getProfileWindowProportion());
//		dataList.add(dataset.getVersionCreated().toString());
//
//		if (this.options.getBoolean(AbstractOptions.SHOW_RECOVER_MERGE_SOURCE_KEY))
//			dataList.add(dataset);
//		return dataList;
//	}

	private List<Object> makeStringList(String s, int rows) {
		List<Object> dataList = new ArrayList<>();
		for (int i = 0; i < rows; i++) {
			dataList.add(s);
		}
		return dataList;
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
		return new NucleusMeasurementsTableModel(options.getDatasets());
	}

	public TableModel createVennTable() {

		if (!options.hasDatasets()) {
			return createBlankTable();
		}

		if (options.isSingleDataset()) {
			return createBlankTable();
		}

		return new VennTableModel(options.getDatasets());
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

		return new VennDetailedTableModel(options.getDatasets());
	}

	/**
	 * Carry out pairwise wilcoxon rank-sum test on the given stat of the given
	 * datasets
	 * 
	 * @param options the table options
	 * @return a tablemodel for display
	 */
	public TableModel createWilcoxonStatisticTable(@Nullable String component) {

		if (!options.hasDatasets()) {
			return new WilcoxonTableModel(null, null);
		}

		try {

			if (CellularComponent.NUCLEUS.equals(component)) {
				List<WilcoxDatasetResult> results = calculateNuclearWilcoxonResults();
				return new WilcoxonTableModel(options.getDatasets(), results);
			}

			if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)) {
				List<WilcoxDatasetResult> results = calculateSegmentWilcoxonResults();
				return new WilcoxonTableModel(options.getDatasets(), results);
			}
		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error creating Wilcoxon table", e);
			return createBlankTable();
		}

		return new WilcoxonTableModel(null, null);
	}

	/**
	 * Run Wilcoxon rank sum tests on nuclear measurements
	 * 
	 * @return
	 */
	private List<WilcoxDatasetResult> calculateNuclearWilcoxonResults() {
		List<WilcoxDatasetResult> results = new ArrayList<>();

		// Bonferroni correction on number of datasets and number of measurement types
		int nComparisons = (options.datasetCount() * (options.datasetCount() - 1)) / 2;
		nComparisons *= Measurement.commonMeasurements(options.getDatasets()).size();

		Measurement stat = options.getMeasurement();

		for (IAnalysisDataset dataset : options.getDatasets()) {
			double[] d1Values = dataset.getCollection().getRawValues(stat, CellularComponent.NUCLEUS,
					MeasurementScale.PIXELS);
			for (IAnalysisDataset d2 : options.getDatasets()) {

				if (dataset.getId().equals(d2.getId()))
					continue;
				long idVal = WilcoxDatasetResult.toId(dataset, d2);
				if (results.stream().anyMatch(w -> w.id() == idVal)) {
					continue; // don't do reciprocal comparison
				}

				double[] d2Values = d2.getCollection().getRawValues(stat, CellularComponent.NUCLEUS,
						MeasurementScale.PIXELS);

				WilcoxonRankSumResult wilcox = Stats.runWilcoxonTest(d1Values, d2Values, nComparisons);
				results.add(new WilcoxDatasetResult(idVal, wilcox));
			}
		}
		return results;
	}

	/**
	 * Run Wilcoxon rank sum tests on segment measurements
	 * 
	 * @return
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 */
	private List<WilcoxDatasetResult> calculateSegmentWilcoxonResults()
			throws MissingLandmarkException, MissingProfileException, ProfileException {
		List<WilcoxDatasetResult> results = new ArrayList<>();
		int nComparisons = (options.datasetCount() * (options.datasetCount() - 1)) / 2;

		for (IAnalysisDataset dataset : options.getDatasets()) {

			IProfileSegment medianSeg1 = dataset.getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN).getSegments()
					.get(options.getSegPosition());

			double[] d1Values = dataset.getCollection().getRawValues(Measurement.LENGTH,
					CellularComponent.NUCLEAR_BORDER_SEGMENT, MeasurementScale.PIXELS, medianSeg1.getID());

			for (IAnalysisDataset d2 : options.getDatasets()) {

				if (dataset.getId().equals(d2.getId()))
					continue;

				long idVal = WilcoxDatasetResult.toId(dataset, d2);
				if (results.stream().anyMatch(w -> w.id() == idVal)) {
					continue; // don't do reciprocal comparison
				}

				IProfileSegment medianSeg2 = d2.getCollection().getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN).getSegments()
						.get(options.getSegPosition());

				double[] d2Values = d2.getCollection().getRawValues(Measurement.LENGTH,
						CellularComponent.NUCLEAR_BORDER_SEGMENT, MeasurementScale.PIXELS, medianSeg2.getID());

				WilcoxonRankSumResult wilcox = Stats.runWilcoxonTest(d1Values, d2Values, nComparisons);
				results.add(new WilcoxDatasetResult(idVal, wilcox));
			}
		}
		return results;
	}

	/**
	 * Generate a table of magnitude difference between datasets
	 * 
	 * @param options the table options
	 * @return a tablemodel for display
	 */
	public TableModel createMagnitudeStatisticTable(@Nullable String component) {

		if (!options.hasDatasets()) {
			return new MagnitudeTableModel(null, null);
		}

		try {

			if (CellularComponent.NUCLEUS.equals(component)) {
				List<MagnitudeDatasetResult> results = calculateNuclearMagnitudes();
				return new MagnitudeTableModel(options.getDatasets(), results);
			}

			if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)) {
				List<MagnitudeDatasetResult> results = calculateSegmentMagnitudes();
				return new MagnitudeTableModel(options.getDatasets(), results);
			}
		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error creating magnitude table", e);
			return createBlankTable();
		}

		return new MagnitudeTableModel(null, null);

	}

	/**
	 * Calculate magnitude differences between datasets
	 * 
	 * @return
	 */
	private List<MagnitudeDatasetResult> calculateNuclearMagnitudes() {
		List<MagnitudeDatasetResult> results = new ArrayList<>();

		for (IAnalysisDataset d1 : options.getDatasets()) {
			double v1 = d1.getCollection().getMedian(options.getMeasurement(), CellularComponent.NUCLEUS,
					options.getScale());

			for (IAnalysisDataset d2 : options.getDatasets()) {
				if (d1 == d2)
					continue;

				double v2 = d2.getCollection().getMedian(options.getMeasurement(), CellularComponent.NUCLEUS,
						options.getScale());

				results.add(new MagnitudeDatasetResult(d1, d2, v1 / v2));
			}
		}
		return results;
	}

	/**
	 * Calculate magnitude differences between datasets
	 * 
	 * @return
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 */
	private List<MagnitudeDatasetResult> calculateSegmentMagnitudes()
			throws MissingLandmarkException, MissingProfileException, ProfileException {
		List<MagnitudeDatasetResult> results = new ArrayList<>();

		for (IAnalysisDataset d1 : options.getDatasets()) {

			IProfileSegment medianSeg1 = d1.getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN).getSegments()
					.get(options.getSegPosition());

			double v1 = d1.getCollection().getMedian(Measurement.LENGTH, CellularComponent.NUCLEAR_BORDER_SEGMENT,
					MeasurementScale.PIXELS, medianSeg1.getID());

			for (IAnalysisDataset d2 : options.getDatasets()) {

				if (d1.getId().equals(d2.getId()))
					continue;

				IProfileSegment medianSeg2 = d2.getCollection().getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN).getSegments()
						.get(options.getSegPosition());

				double v2 = d2.getCollection().getMedian(Measurement.LENGTH, CellularComponent.NUCLEAR_BORDER_SEGMENT,
						MeasurementScale.PIXELS, medianSeg2.getID());

				results.add(new MagnitudeDatasetResult(d1, d2, v1 / v2));
			}
		}
		return results;
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
		if (options.getDatasets().stream().noneMatch(IAnalysisDataset::hasClusters))
			return createBlankTable();

		return new ClusterGroupTableModel(options.getDatasets());
	}

}
