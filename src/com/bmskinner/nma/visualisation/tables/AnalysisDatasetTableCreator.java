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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.measure.MissingMeasurementException;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;
import com.bmskinner.nma.stats.Stats.WilcoxonRankSumResult;
import com.bmskinner.nma.visualisation.options.AbstractOptions;
import com.bmskinner.nma.visualisation.options.TableOptions;

/**
 * Creates the tables for display in the UI.
 * 
 * @author Ben Skinner
 *
 */
public class AnalysisDatasetTableCreator extends AbstractTableCreator {

	private static final Logger LOGGER = Logger
			.getLogger(AnalysisDatasetTableCreator.class.getName());

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

		// If we are showing merge sources, ensure there is something to show
		if (options.getBoolean(AbstractOptions.IS_MERGE_SOURCE_OPTIONS_TABLE)
				&& !hasMergeSource(options.getDatasets()))
			return createBlankTable();

		return new AnalysisParametersTableModel(options.getDatasets(),
				AnalysisParametersTableModel.MERGES_NOT_RECOVERABLE);

	}

	/**
	 * Create a table model of rulesets from datasets. If null parameter is passed,
	 * will create an empty table
	 * 
	 * @return
	 */
	public TableModel createAnalysisRulesetsTable() {

		// If we are showing merge sources, ensure there is something to show
		if (options.getBoolean(AbstractOptions.IS_MERGE_SOURCE_OPTIONS_TABLE)
				&& !hasMergeSource(options.getDatasets()))
			return createBlankTable();

		return new AnalysisRulesetsTableModel(options.getDatasets());

	}

	/**
	 * Create a table model of analysis parameters from a nucleus collection. If
	 * null parameter is passed, will create an empty table
	 * 
	 * @return
	 */
	public TableModel createMergeSourceAnalysisParametersTable() {

		// If we are showing merge sources, ensure there is something to show
		if (options.getBoolean(AbstractOptions.IS_MERGE_SOURCE_OPTIONS_TABLE)
				&& !hasMergeSource(options.getDatasets()))
			return createBlankTable();

		return new AnalysisParametersTableModel(options.getDatasets(),
				AnalysisParametersTableModel.MERGES_RECOVERABLE);

	}

	/**
	 * Test if any of the given datasets in the input list are merge sources. Checks
	 * against all open datasets.
	 * 
	 * @return
	 */
	private boolean hasMergeSource(@NonNull List<IAnalysisDataset> datasets) {

		Set<IAnalysisDataset> all = DatasetListManager.getInstance().getAllDatasets();

		for (IAnalysisDataset d : datasets) {
			for (IAnalysisDataset p : all)
				if (p.hasMergeSource(d))
					return true;
		}
		return false;
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
	 * @throws MissingDataException
	 * @throws ProfileException
	 * @throws MissingMeasurementException
	 * @throws SegmentUpdateException
	 */
	private List<WilcoxDatasetResult> calculateNuclearWilcoxonResults()
			throws MissingDataException, SegmentUpdateException {
		List<WilcoxDatasetResult> results = new ArrayList<>();

		// Bonferroni correction on number of datasets and number of measurement types
		int nComparisons = (options.datasetCount() * (options.datasetCount() - 1)) / 2;
		nComparisons *= Measurement.commonMeasurements(options.getDatasets()).size();

		Measurement stat = options.getMeasurement();

		for (IAnalysisDataset dataset : options.getDatasets()) {
			double[] d1Values = dataset.getCollection().getRawValues(stat,
					CellularComponent.NUCLEUS,
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

				WilcoxonRankSumResult wilcox = Stats.runWilcoxonTest(d1Values, d2Values,
						nComparisons);
				results.add(new WilcoxDatasetResult(idVal, wilcox));
			}
		}
		return results;
	}

	/**
	 * Run Wilcoxon rank sum tests on segment measurements
	 * 
	 * @return
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	private List<WilcoxDatasetResult> calculateSegmentWilcoxonResults()
			throws MissingDataException,
			SegmentUpdateException {
		List<WilcoxDatasetResult> results = new ArrayList<>();
		int nComparisons = (options.datasetCount() * (options.datasetCount() - 1)) / 2;

		for (IAnalysisDataset dataset : options.getDatasets()) {

			IProfileSegment medianSeg1 = dataset.getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
					.getSegments()
					.get(options.getSegPosition());

			double[] d1Values = dataset.getCollection().getRawValues(Measurement.LENGTH,
					CellularComponent.NUCLEAR_BORDER_SEGMENT, MeasurementScale.PIXELS,
					medianSeg1.getID());

			for (IAnalysisDataset d2 : options.getDatasets()) {

				if (dataset.getId().equals(d2.getId()))
					continue;

				long idVal = WilcoxDatasetResult.toId(dataset, d2);
				if (results.stream().anyMatch(w -> w.id() == idVal)) {
					continue; // don't do reciprocal comparison
				}

				IProfileSegment medianSeg2 = d2.getCollection().getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
								Stats.MEDIAN)
						.getSegments()
						.get(options.getSegPosition());

				double[] d2Values = d2.getCollection().getRawValues(Measurement.LENGTH,
						CellularComponent.NUCLEAR_BORDER_SEGMENT, MeasurementScale.PIXELS,
						medianSeg2.getID());

				WilcoxonRankSumResult wilcox = Stats.runWilcoxonTest(d1Values, d2Values,
						nComparisons);
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
	 * @throws MissingDataException
	 * @throws ProfileException
	 * @throws MissingMeasurementException
	 * @throws SegmentUpdateException
	 */
	private List<MagnitudeDatasetResult> calculateNuclearMagnitudes()
			throws MissingDataException,
			SegmentUpdateException {
		List<MagnitudeDatasetResult> results = new ArrayList<>();

		for (IAnalysisDataset d1 : options.getDatasets()) {
			double v1 = d1.getCollection().getMedian(options.getMeasurement(),
					CellularComponent.NUCLEUS,
					options.getScale());

			for (IAnalysisDataset d2 : options.getDatasets()) {
				if (d1 == d2)
					continue;

				double v2 = d2.getCollection().getMedian(options.getMeasurement(),
						CellularComponent.NUCLEUS,
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
	 * @throws MissingDataException
	 * @throws MissingMeasurementException
	 * @throws SegmentUpdateException
	 */
	private List<MagnitudeDatasetResult> calculateSegmentMagnitudes()
			throws MissingDataException,
			SegmentUpdateException {
		List<MagnitudeDatasetResult> results = new ArrayList<>();

		for (IAnalysisDataset d1 : options.getDatasets()) {

			IProfileSegment medianSeg1 = d1.getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
					.getSegments()
					.get(options.getSegPosition());

			double v1 = d1.getCollection().getMedian(Measurement.LENGTH,
					CellularComponent.NUCLEAR_BORDER_SEGMENT,
					MeasurementScale.PIXELS, medianSeg1.getID());

			for (IAnalysisDataset d2 : options.getDatasets()) {

				if (d1.getId().equals(d2.getId()))
					continue;

				IProfileSegment medianSeg2 = d2.getCollection().getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
								Stats.MEDIAN)
						.getSegments()
						.get(options.getSegPosition());

				double v2 = d2.getCollection().getMedian(Measurement.LENGTH,
						CellularComponent.NUCLEAR_BORDER_SEGMENT,
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
