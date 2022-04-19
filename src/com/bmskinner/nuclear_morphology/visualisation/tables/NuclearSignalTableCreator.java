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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.PairwiseSignalDistanceCollection;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.stats.ShellDistributionTester;
import com.bmskinner.nuclear_morphology.stats.Stats;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;

public class NuclearSignalTableCreator extends AbstractTableCreator {

	private static final Logger LOGGER = Logger.getLogger(NuclearSignalTableCreator.class.getName());

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
	 * @param list the AnalysisDatasets to include
	 * @return a table model
	 */
	public TableModel createSignalDetectionParametersTable() {

		if (!options.hasDatasets())
			return createBlankTable();

		return new NuclearSignalDetectionTableModel(options.getDatasets());
	}

	/**
	 * Create a table of signal stats for the given list of datasets. This table
	 * covers size, number of signals
	 * 
	 * @param list the AnalysisDatasets to include
	 * @return a table model
	 */
	public TableModel createSignalStatsTable() {
		return new NuclearSignalMeasurementsTableModel(options.getDatasets());
	}

	/**
	 * Create a table with columns for dataset, signal group, and the p value of a
	 * chi-square test for all shell analyses run
	 * 
	 * @param options
	 * @return
	 */
	public TableModel createShellChiSquareTable() {

		if (!options.hasDatasets())
			return createBlankTable();

		DefaultTableModel model = new DefaultTableModel();

		DecimalFormat lowFormat = new DecimalFormat("0.00E00");
		DecimalFormat pFormat = new DecimalFormat(DEFAULT_PROBABILITY_FORMAT);

		Object[] columnNames = { Labels.DATASET, Labels.Signals.SIGNAL_GROUP_LABEL, Labels.Signals.AVERAGE_POSITION,
				Labels.Stats.PROBABILITY };

		model.setColumnIdentifiers(columnNames);
		int nComparisons = 0;

		Map<String, Object[]> valuesToAdd = new HashMap<>();

		for (IAnalysisDataset d : options.getDatasets()) {

			Optional<ISignalGroup> randomGroup = d.getCollection().getSignalGroup(IShellResult.RANDOM_SIGNAL_ID);
			Optional<IShellResult> random = randomGroup.isPresent()
					? d.getCollection().getSignalGroup(IShellResult.RANDOM_SIGNAL_ID).get().getShellResult()
					: Optional.empty();

			for (UUID signalGroup : d.getCollection().getSignalManager().getSignalGroupIDs()) {

				ISignalGroup group = d.getCollection().getSignalGroup(signalGroup).get();
				Optional<IShellResult> r = group.getShellResult();
				if (r.isPresent()) {

					String groupName = group.getGroupName();

					double mean = r.get().getOverallShell(options.getAggregation(), options.getNormalisation());
					double pval = 1;
					if (random.isPresent()) {
						ShellDistributionTester tester = new ShellDistributionTester(r.get(), random.get());
						pval = tester.test(options.getAggregation(), options.getNormalisation()).getPValue();
					}

					String key = d.getId().toString() + groupName;
					Object[] rowData = { d.getName(), groupName, pFormat.format(mean), pval };
					valuesToAdd.put(key, rowData);
					nComparisons++;
				}
			}
		}

		for (String key : valuesToAdd.keySet()) {
			Object[] values = valuesToAdd.get(key);
			double d = (double) values[3];
			d *= nComparisons; // Bonferroni correction
			d = Math.min(d, 1);
			values[3] = d < 0.001 ? lowFormat.format(d) : pFormat.format(d); // Choose the most readable format
			model.addRow(values);
		}
		return model;
	}

	/**
	 * Create a table with columns for dataset, signal group, and the p value of a
	 * chi-square test for all selected pairwise dataset and signal group
	 * combinations
	 * 
	 * @param options
	 * @return
	 */
	public TableModel createPairwiseShellChiSquareTable() {

		if (!options.hasDatasets())
			return createBlankTable();

		DefaultTableModel model = new DefaultTableModel();

		DecimalFormat lowFormat = new DecimalFormat("0.00E00");
		DecimalFormat pFormat = new DecimalFormat(DEFAULT_PROBABILITY_FORMAT);

		Object[] columnNames = { Labels.DATASET, Labels.Signals.SIGNAL_GROUP_LABEL, Labels.DATASET,
				Labels.Signals.SIGNAL_GROUP_LABEL, Labels.Stats.PROBABILITY };

		model.setColumnIdentifiers(columnNames);

		int nComparisons = 0;

		Map<String, Object[]> valuesAdded = new HashMap<>();

		for (IAnalysisDataset d1 : options.getDatasets()) {

			for (UUID signalGroup1 : d1.getCollection().getSignalManager().getSignalGroupIDs()) {

				ISignalGroup group1 = d1.getCollection().getSignalGroup(signalGroup1).get();
				Optional<IShellResult> r1 = group1.getShellResult();
				if (!r1.isPresent())
					continue;

				String groupName1 = group1.getGroupName();

				for (IAnalysisDataset d2 : options.getDatasets()) {

					for (UUID signalGroup2 : d2.getCollection().getSignalManager().getSignalGroupIDs()) {
						if (d1 == d2 && signalGroup1 == signalGroup2)
							continue;

						ISignalGroup group2 = d2.getCollection().getSignalGroup(signalGroup2).get();
						Optional<IShellResult> r2 = group2.getShellResult();
						if (!r2.isPresent())
							continue;

						String groupName2 = group2.getGroupName();

						ShellDistributionTester tester = new ShellDistributionTester(r1.get(), r2.get());
						double pval = tester.test(options.getAggregation(), options.getNormalisation()).getPValue();

						String k1 = d1.getId().toString() + signalGroup1.toString() + d2.getId().toString()
								+ signalGroup2.toString();
						String k2 = d2.getId().toString() + signalGroup2.toString() + d1.getId().toString()
								+ signalGroup1.toString();

						Object[] rowData = { d1.getName(), groupName1, d2.getName(), groupName2, pval };

						if (valuesAdded.containsKey(k2)) {
							double prevPValue = (double) valuesAdded.get(k2)[4];
							if (prevPValue < pval)
								valuesAdded.put(k2, rowData);
						} else {
							valuesAdded.put(k1, rowData);
							nComparisons++;
						}
					}
				}

			}
		}

		for (String key : valuesAdded.keySet()) {
			Object[] values = valuesAdded.get(key);
			double d = (double) values[4];
			d *= nComparisons; // Bonferroni correction
			d = Math.min(d, 1);
			values[4] = d < 0.001 ? lowFormat.format(d) : pFormat.format(d);
			model.addRow(values);
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

				List<Double> values = ps.getValues(primaryID, secondaryID);

				if (values == null) {
					columnData.add(Labels.NA);
					continue;
				}

				DescriptiveStatistics ds = new DescriptiveStatistics();
				for (double d : values) {
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
