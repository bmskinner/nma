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
package com.bmskinner.nma.analysis;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.signals.PairedSignalGroups;
import com.bmskinner.nma.analysis.signals.PairedSignalGroups.DatasetSignalId;
import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DefaultAnalysisDataset;
import com.bmskinner.nma.components.datasets.DefaultCellCollection;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.profiles.DefaultProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileCollection;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.components.signals.DefaultSignalGroup;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.io.Io;

/**
 * Merge multiple datasets into a single dataset
 * 
 * @author bms41
 *
 */
public class DatasetMergeMethod extends MultipleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(DatasetMergeMethod.class.getName());

	private File saveFile;

	/** Describe which signal groups will be merged */
	private PairedSignalGroups pairedSignalGroups = null;

	private static final int MAX_PROGRESS = 100;
	private static final int MILLISECONDS_TO_SLEEP = 10;

	/**
	 * Create the merger for the given datasets.
	 * 
	 * @param datasets the datasets to be merged
	 * @param saveFile the file to specify as the new dataset save path. Note, this
	 *                 method does not save out the file to the save path
	 */
	public DatasetMergeMethod(@NonNull List<IAnalysisDataset> datasets, File saveFile) {
		this(datasets, saveFile, null);
	}

	/**
	 * Create the merger for the given datasets.
	 * 
	 * @param datasets           the datasets to be merged
	 * @param saveFile           the file to specify as the new dataset save path.
	 *                           Note, this method does not save out the file to the
	 *                           save path
	 * @param pairedSignalGroups the signal groups which are to be merged
	 */
	public DatasetMergeMethod(@NonNull List<IAnalysisDataset> datasets, File saveFile,
			PairedSignalGroups pairedSignalGroups) {
		super(datasets);
		this.saveFile = saveFile;
		this.pairedSignalGroups = pairedSignalGroups;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		IAnalysisDataset merged = run();
		return new DefaultAnalysisResult(merged);
	}

	private IAnalysisDataset run() throws Exception {

		if (!datasetsCanBeMerged())
			return null;

		// Set the names for the new collection
		// ensure the new file name is valid
		saveFile = checkName(saveFile).getAbsoluteFile();
		String newDatasetName = saveFile.getName().replace(Io.NMD_FILE_EXTENSION, "");

		IAnalysisDataset newDataset = performMerge(newDatasetName);

		spinWheels(MAX_PROGRESS, MILLISECONDS_TO_SLEEP);

		return newDataset;
	}

	/**
	 * Check that the given datasets can be merged
	 * 
	 * @return true if the datasets can be merged, false otherwise
	 */
	private boolean datasetsCanBeMerged() {
		if (datasets.size() <= 1) {
			LOGGER.warning("Must have multiple datasets to merge");
			return false;
		}

		// check we are not merging a parent and child (would just get parent)
		if (datasets.size() == 2 && (datasets.get(0).hasDirectChild(datasets.get(1))
				|| datasets.get(1).hasDirectChild(datasets.get(0)))) {
			LOGGER.warning("Merging parent and child would be silly.");
			return false;

		}

		// check all collections are of the same type
		RuleSetCollection testClass = datasets.get(0).getCollection().getRuleSetCollection();
		for (IAnalysisDataset d : datasets) {
			if (!d.getCollection().getRuleSetCollection().equals(testClass)) {
				LOGGER.warning("Cannot merge collections with different rulesets");
				return false;
			}
		}

		return true;
	}

	/**
	 * Merge the given datasets, copying each cell into the new collection and
	 * removing existing segmentation patterns.
	 * 
	 * @param newCollection the new collection to copy cells into
	 * @return the merged dataset
	 * @throws MissingProfileException
	 * @throws MissingOptionException
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 */
	private IAnalysisDataset performMerge(@NonNull String newDatasetName)
			throws MissingOptionException, ProfileException, MissingLandmarkException,
			ComponentCreationException {

		// make a new collection
		ICellCollection newCollection = new DefaultCellCollection(
				datasets.get(0).getCollection().getRuleSetCollection(),
				newDatasetName, UUID.randomUUID());

		// Add cells from each source dataset
		for (IAnalysisDataset d : datasets) {
			for (ICell c : d.getCollection()) {
				if (!newCollection.contains(c))
					newCollection.add(c.duplicate());
			}
		}

		for (Nucleus n : newCollection.getNuclei()) {
			// Ensure that all nuclei have any existing segments removed
			// and replaced with the default segment starting at the RP
			n.setSegments(List.of(new DefaultProfileSegment(0, 0, n.getBorderLength(),
					IProfileCollection.DEFAULT_SEGMENT_ID)));

		}

		// Replace signal groups
		mergeSignalGroups(newCollection);

		// create the dataset; has no analysis options at present
		IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection, saveFile);

		// Add the original datasets as merge sources
		for (IAnalysisDataset d : datasets) {
			newDataset.addMergeSource(d);
		}

		IAnalysisOptions mergedOptions = mergeOptions(newDataset);
		newDataset.setAnalysisOptions(mergedOptions);

		mergeSignalOptions(newDataset);

		return newDataset;
	}

	/**
	 * Merge any identical options amongst the datasets
	 * 
	 * @return the merged options
	 * @throws MissingOptionException
	 */
	private IAnalysisOptions mergeOptions(IAnalysisDataset newDataset)
			throws MissingOptionException {

		IAnalysisDataset d1 = datasets.get(0);
		IAnalysisOptions d1Options = d1.getAnalysisOptions()
				.orElseThrow(MissingOptionException::new);

		IAnalysisOptions mergedOptions = OptionsFactory
				.makeAnalysisOptions(d1Options.getRuleSetCollection());

		// Start with a blank slate for nucleus options
		HashOptions nOptions = new OptionsBuilder().build();

		// Get a base template to compare the others to
		HashOptions t1 = datasets.get(0).getAnalysisOptions()
				.orElseThrow(MissingOptionException::new).getNucleusDetectionOptions()
				.orElseThrow(MissingOptionException::new);
		mergeOptions(nOptions, t1);
		mergedOptions.setDetectionOptions(CellularComponent.NUCLEUS, nOptions);

		mergedOptions.setAngleWindowProportion(d1Options.getProfileWindowProportion());

		return mergedOptions;
	}

	private void mergeSignalOptions(IAnalysisDataset newDataset) throws MissingOptionException {

		if (pairedSignalGroups == null)
			return;

		// For each set of mergeable signals, make a new signal group
		for (UUID newSignalId : pairedSignalGroups.getMergedSignalGroups()) {

			HashOptions mergedOptions = new OptionsBuilder().build();

			// Get the first signal options
			List<DatasetSignalId> ids = pairedSignalGroups.get(newSignalId);
			HashOptions template = ids.get(0).datasetId().getAnalysisOptions()
					.orElseThrow(MissingOptionException::new)
					.getNuclearSignalOptions(ids.get(0).signalId().getId())
					.orElseThrow(MissingOptionException::new);

			// Add the original signal ids to the options so we can extradct later
			for (DatasetSignalId d : pairedSignalGroups.get(newSignalId)) {
				mergedOptions.setUUID(HashOptions.ORIGINAL_SIGNAL_PREFIX + d.datasetId().getId(),
						d.signalId().getId());
			}

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

			// Ensure the new options have the correct id
			mergedOptions.set(HashOptions.SIGNAL_GROUP_ID, newSignalId.toString());

			// Add the final options to the dataset
			newDataset.getAnalysisOptions().orElseThrow(MissingOptionException::new)
					.setNuclearSignalDetectionOptions(mergedOptions);
		}

	}

	private void mergeOptions(HashOptions mergedOptions, HashOptions template)
			throws MissingOptionException {

		// Check every key in the options. If any are the same across all datasets
		for (String s : template.getKeys()) {
			Object result = template.getValue(s);
			boolean canAdd = true;
			for (IAnalysisDataset d : datasets) {
				IAnalysisOptions dOptions = d.getAnalysisOptions()
						.orElseThrow(MissingOptionException::new);
				HashOptions nOptions = dOptions.getNucleusDetectionOptions()
						.orElseThrow(MissingOptionException::new);
				canAdd &= Objects.equals(result, nOptions.getValue(s));
			}

			if (canAdd)
				mergedOptions.set(s, result);
		}
	}

	/**
	 * Merge any signal groups in the new collection, as described by the paired
	 * signal groups map
	 * 
	 * @param newCollection
	 */
	private void mergeSignalGroups(ICellCollection newCollection) {
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
			newCollection.addSignalGroup(newGroup);
			for (Nucleus n : newCollection.getNuclei()) {
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

	/**
	 * Check if the new dataset filename already exists. If so, append _1 to the end
	 * and check again
	 * 
	 * @param name
	 * @return
	 */
	private File checkName(File name) {
		String fileName = name.getName();
		String datasetName = fileName.replace(Io.NMD_FILE_EXTENSION, "");

		File newFile = new File(name.getParentFile(), datasetName + Io.NMD_FILE_EXTENSION);
		if (name.exists()) {
			datasetName += "_1";
			newFile = new File(name.getParentFile(), datasetName + Io.NMD_FILE_EXTENSION);
			newFile = checkName(newFile);
		}
		return newFile;
	}

}
