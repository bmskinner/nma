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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

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
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.components.profiles.DefaultProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileCollection;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.components.signals.DefaultSignalGroup;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.io.Io;

/**
 * Merge multiple datasets into a single dataset
 * 
 * @author bms41
 *
 */
public class DatasetMergeMethod extends MultipleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(DatasetMergeMethod.class.getName());

	/** Should warped signals be copied into new signal groups */
	private static final boolean COPY_WARPED = false;

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

		if (!datasetsAreValidToMerge())
			return null;

		LOGGER.fine("Finding new file name");

		// Set the names for the new collection
		File newDatasetFolder = saveFile.getParentFile();
		File newDatasetFile = saveFile;

		// ensure the new file name is valid
		newDatasetFile = checkName(newDatasetFile);

		String newDatasetName = newDatasetFile.getName().replace(Io.SAVE_FILE_EXTENSION, "");
		LOGGER.fine("Checked new file names");

		// make a new collection
		ICellCollection newCollection = new DefaultCellCollection(
				datasets.get(0).getCollection().getRuleSetCollection(),
				newDatasetName, UUID.randomUUID());

		IAnalysisDataset newDataset = performMerge(newCollection);

		spinWheels(MAX_PROGRESS, MILLISECONDS_TO_SLEEP);

		return newDataset;
	}

	/**
	 * Check that the given datasets can be merged
	 * 
	 * @return true if the datasets can be merged, false otherwise
	 */
	private boolean datasetsAreValidToMerge() {
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
		if (!nucleiHaveSameType()) {
			LOGGER.warning("Cannot merge collections of different nucleus type");
			return false;
		}
		return true;
	}

	/**
	 * Check if the nucleus classes of all datasets match. Cannot merge collections
	 * with different classes
	 * 
	 * @return true if all collection have the same nucleus type, false otherwise
	 */
	private boolean nucleiHaveSameType() {
		RuleSetCollection testClass = datasets.get(0).getCollection().getRuleSetCollection();
		for (IAnalysisDataset d : datasets) {
			if (!d.getCollection().getRuleSetCollection().equals(testClass))
				return false;
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
	private IAnalysisDataset performMerge(@NonNull ICellCollection newCollection)
			throws MissingOptionException, ProfileException, MissingLandmarkException,
			ComponentCreationException {

		for (IAnalysisDataset d : datasets) {

			for (ICell c : d.getCollection()) {
				if (!newCollection.contains(c))
					newCollection.add(c.duplicate());
			}

			// All the existing signal groups before merging
			for (UUID signalGroupID : d.getCollection().getSignalGroupIDs()) {
				newCollection.addSignalGroup(
						new DefaultSignalGroup(d.getCollection().getSignalGroup(signalGroupID)
								.orElseThrow(NullPointerException::new)));
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

		// TODO update nuclear signal options with new ids

		// create the dataset; has no analysis options at present
		IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection, saveFile);

		// Add the original datasets as merge sources
		for (IAnalysisDataset d : datasets) {

			// Make a new virtual collection for the sources
			newDataset.addMergeSource(d);
		}

		// TODO need to keep the signal folder settings preserved. Copy the analysis
		// options where
		// possible

		IAnalysisOptions mergedOptions = mergeOptions(newDataset);
		newDataset.setAnalysisOptions(mergedOptions);

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

		// Use an empty file since there are multiple folders in the merge
		HashOptions nucleus = OptionsFactory.makeNucleusDetectionOptions(new File("")).build();

		IAnalysisDataset d1 = datasets.get(0);
		IAnalysisOptions d1Options = d1.getAnalysisOptions()
				.orElseThrow(MissingOptionException::new);

		IAnalysisOptions mergedOptions = OptionsFactory
				.makeAnalysisOptions(d1Options.getRuleSetCollection());

		List<HashOptions> templates = new ArrayList<>();
		for (IAnalysisDataset d : datasets) {
			IAnalysisOptions dOptions = d.getAnalysisOptions()
					.orElseThrow(MissingOptionException::new);
			templates.add(
					dOptions.getNucleusDetectionOptions().orElseThrow(MissingOptionException::new));
		}
		mergeDetectionOptions(nucleus, templates);

		mergedOptions.setDetectionOptions(CellularComponent.NUCLEUS, nucleus);
		mergedOptions.setRuleSetCollection(d1Options.getRuleSetCollection());
		mergedOptions.setAngleWindowProportion(d1Options.getProfileWindowProportion());

		// Merge signal group options
		for (UUID signalGroupId : newDataset.getCollection().getSignalGroupIDs()) {
			HashOptions signal = OptionsFactory.makeNuclearSignalOptions((File) null)
					.withValue(HashOptions.SIGNAL_GROUP_ID, signalGroupId.toString()).build();
			mergedOptions.setNuclearSignalDetectionOptions(signal);
			// TODO: Merge the signal detection options
		}

		return mergedOptions;
	}

	/**
	 * Merge the detection options from the given templates, and put the merged
	 * result in the given target. If values cannot be merged sensibly (e.g.
	 * multiple different float values), then they will be set to: float and
	 * double:NaN; int: MAX_VALUE; String: NA_MERGE.
	 * 
	 * @param merged
	 * @param templates
	 * @throws MissingOptionException
	 */
	private void mergeDetectionOptions(HashOptions merged, List<HashOptions> templates)
			throws MissingOptionException {

		// Get a base template to compare the others to
		HashOptions t1 = templates.get(0);

		mergeFloatOptions(merged, t1);

		mergeDoubleOptions(merged, t1);

		mergeIntOptions(merged, t1);

		mergeStringOptions(merged, t1);

		mergeSubOptions(merged, t1);

	}

	private void mergeFloatOptions(HashOptions merged, HashOptions template)
			throws MissingOptionException {
		for (String s : template.getFloatKeys()) {
			float result = template.getFloat(s);
			boolean canAdd = true;
			for (IAnalysisDataset d : datasets) {
				IAnalysisOptions dOptions = d.getAnalysisOptions()
						.orElseThrow(MissingOptionException::new);
				HashOptions nOptions = dOptions.getNucleusDetectionOptions()
						.orElseThrow(MissingOptionException::new);
				canAdd &= nOptions.getFloat(s) == result;
			}
			if (canAdd)
				merged.setFloat(s, result);
			else
				merged.setFloat(s, Float.NaN);
		}
	}

	private void mergeDoubleOptions(HashOptions merged, HashOptions template)
			throws MissingOptionException {
		for (String s : template.getDoubleKeys()) {
			double result = template.getDouble(s);
			boolean canAdd = true;
			for (IAnalysisDataset d : datasets) {
				IAnalysisOptions dOptions = d.getAnalysisOptions()
						.orElseThrow(MissingOptionException::new);
				HashOptions nOptions = dOptions.getNucleusDetectionOptions()
						.orElseThrow(MissingOptionException::new);
				canAdd &= nOptions.getDouble(s) == result;
			}
			if (canAdd)
				merged.setDouble(s, result);
			else
				merged.setDouble(s, Double.NaN);
		}
	}

	private void mergeIntOptions(HashOptions merged, HashOptions template)
			throws MissingOptionException {
		for (String s : template.getIntegerKeys()) {
			int result = template.getInt(s);
			boolean canAdd = true;
			for (IAnalysisDataset d : datasets) {
				IAnalysisOptions dOptions = d.getAnalysisOptions()
						.orElseThrow(MissingOptionException::new);
				HashOptions nOptions = dOptions.getNucleusDetectionOptions()
						.orElseThrow(MissingOptionException::new);
				canAdd &= nOptions.getInt(s) == result;
			}
			if (canAdd)
				merged.setInt(s, result);
			else
				merged.setInt(s, Integer.MAX_VALUE);
		}
	}

	private void mergeStringOptions(HashOptions merged, HashOptions template)
			throws MissingOptionException {
		for (String s : template.getStringKeys()) {
			String result = template.getString(s);
			boolean canAdd = true;
			for (IAnalysisDataset d : datasets) {
				IAnalysisOptions dOptions = d.getAnalysisOptions()
						.orElseThrow(MissingOptionException::new);
				HashOptions nOptions = dOptions.getNucleusDetectionOptions()
						.orElseThrow(MissingOptionException::new);
				canAdd &= nOptions.getString(s).equals(result);
			}
			if (canAdd)
				merged.setString(s, result);
			else
				merged.setString(s, Labels.NA_MERGE);
		}
	}

	private void mergeSubOptions(HashOptions merged, HashOptions template)
			throws MissingOptionException {
		for (String s : template.getSubOptionKeys()) {
			HashOptions mergedOptions = template.getSubOptions(s);
			boolean canAdd = true;
			for (IAnalysisDataset d : datasets) {
				IAnalysisOptions dOptions = d.getAnalysisOptions()
						.orElseThrow(MissingOptionException::new);
				HashOptions nOptions = dOptions.getNucleusDetectionOptions()
						.orElseThrow(MissingOptionException::new);
				if (nOptions.hasSubOptions(s))
					canAdd &= nOptions.getSubOptions(s).equals(mergedOptions);
				else
					canAdd = false;
			}
			if (canAdd)
				merged.setSubOptions(s, mergedOptions);
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

		LOGGER.finer("Merging signal groups");

		// Decide which signal groups get which new ids
		// Key is old signal group. Entry is new id
		Map<DatasetSignalId, UUID> mergedSignalGroups = new HashMap<>();

		for (DatasetSignalId id1 : pairedSignalGroups.keySet()) {

			// If this id is not encountered, make a new one
			if (!mergedSignalGroups.keySet().contains(id1)) {
				LOGGER.finest("No merge group, creating");
				mergedSignalGroups.put(id1, UUID.randomUUID());
			}

			UUID newID = mergedSignalGroups.get(id1);

			// All the set share this new id
			Set<DatasetSignalId> id2Set = pairedSignalGroups.get(id1);
			for (DatasetSignalId id2 : id2Set) {
				LOGGER.finest(() -> String.format("Adding %s to %s", id2, newID));
				mergedSignalGroups.put(id2, newID);
			}
		}

		// Now, all the old ids have a link to a new id
		// Update the signal groups in the merged dataset

		// Add the old signal groups to the new collection

		LOGGER.finer("Updating signal group ids");
		for (Entry<DatasetSignalId, UUID> entry : mergedSignalGroups.entrySet()) {
			DatasetSignalId oldId = entry.getKey();
			UUID newID = entry.getValue();
			LOGGER.finer("New group id to merge into: " + newID);

			newCollection.getSignalManager().updateSignalGroupID(oldId.s, newID);
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
		String datasetName = fileName.replace(Io.SAVE_FILE_EXTENSION, "");

		File newFile = new File(name.getParentFile(), datasetName + Io.SAVE_FILE_EXTENSION);
		if (name.exists()) {
			datasetName += "_1";
			newFile = new File(name.getParentFile(), datasetName + Io.SAVE_FILE_EXTENSION);
			newFile = checkName(newFile);
		}
		return newFile;
	}

}
