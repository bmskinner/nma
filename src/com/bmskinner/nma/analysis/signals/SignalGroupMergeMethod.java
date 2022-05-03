/*******************************************************************************
 * Copyright (C) 2019 Ben Skinner
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
package com.bmskinner.nma.analysis.signals;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.analysis.signals.PairedSignalGroups.DatasetSignalId;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.components.signals.DefaultSignalGroup;
import com.bmskinner.nma.components.signals.INuclearSignal;

/**
 * Method to merge signals groups within a single dataaset
 * 
 * @author bs19022
 * @since 1.16.1
 *
 */
public class SignalGroupMergeMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getGlobal();

	private static final int MAX_PROGRESS = 100;
	private static final int MILLISECONDS_TO_SLEEP = 10;

	private PairedSignalGroups pairedSignalGroups;

	/**
	 * Create the method
	 * 
	 * @param dataset            the dataset with signals to be merged
	 * @param pairedSignalGroups the signal groups to be merged
	 */
	public SignalGroupMergeMethod(@NonNull IAnalysisDataset dataset,
			@NonNull PairedSignalGroups pairedSignalGroups) {
		super(dataset);

		if (dataset.getCollection().getSignalManager().getSignalGroupCount() < 2)
			throw new IllegalArgumentException("Must have two or more signal groups to merge");
		this.pairedSignalGroups = pairedSignalGroups;

	}

	@Override
	public IAnalysisResult call() throws Exception {
		mergeSignalGroups();
		mergeSignalOptions();
		spinWheels(MAX_PROGRESS, MILLISECONDS_TO_SLEEP);
		return new DefaultAnalysisResult(dataset);
	}

	private void mergeSignalGroups() {
		if (pairedSignalGroups == null || pairedSignalGroups.isEmpty()) {
			LOGGER.finer("No signal groups to merge");
			return;
		}

		// For each set of mergeable signals, make a new signal group
		for (UUID newSignalId : pairedSignalGroups.getMergedSignalGroups()) {

			List<DatasetSignalId> ids = pairedSignalGroups.get(newSignalId);

			// Create merged group name
			String newName = ids.stream().map(i -> i.signalId().getGroupName())
					.collect(Collectors.joining("_")) + "_merged";

			DefaultSignalGroup newGroup = new DefaultSignalGroup(newName, newSignalId);

			// Duplicate the signals into the new signal group
			dataset.getCollection().addSignalGroup(newGroup);
			for (Nucleus n : dataset.getCollection().getNuclei()) {
				for (DatasetSignalId id : ids) {
					List<INuclearSignal> signals = n.getSignalCollection()
							.getSignals(id.signalId().getId());
					for (INuclearSignal s : signals) {
						n.getSignalCollection().addSignal(s.duplicate(), newSignalId);
					}
				}
			}

		}

	}

	private void mergeSignalOptions() throws MissingOptionException {
		// For each set of mergeable signals, make a new signal group
		for (UUID newSignalId : pairedSignalGroups.getMergedSignalGroups()) {

			HashOptions mergedOptions = new OptionsBuilder()
					.withValue(HashOptions.SIGNAL_GROUP_ID, newSignalId.toString()).build();

			// Get the first signal options
			List<DatasetSignalId> ids = pairedSignalGroups.get(newSignalId);
			HashOptions template = ids.get(0).datasetId().getAnalysisOptions()
					.orElseThrow(MissingOptionException::new)
					.getNuclearSignalOptions(ids.get(0).signalId().getId())
					.orElseThrow(MissingOptionException::new);

			// Check every key in the options. If any are the same across all signaal
			// groups, keep them
			for (String s : template.getKeys()) {
				Object result = template.getValue(s);
				boolean canAdd = true;
				for (DatasetSignalId d : pairedSignalGroups.get(newSignalId)) {
					IAnalysisOptions dOptions = d.datasetId().getAnalysisOptions()
							.orElseThrow(MissingOptionException::new);
					HashOptions nOptions = dOptions.getNuclearSignalOptions(d.signalId().getId())
							.orElseThrow(MissingOptionException::new);
					canAdd &= Objects.equals(result, nOptions.getValue(s));
				}

				if (canAdd)
					mergedOptions.set(s, result);
			}

			// Add the final options to the dataset
			dataset.getAnalysisOptions().orElseThrow(MissingOptionException::new)
					.setNuclearSignalDetectionOptions(mergedOptions);
		}

	}

}
