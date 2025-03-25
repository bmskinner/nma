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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.DefaultAnalysisDataset;
import com.bmskinner.nma.components.datasets.DefaultCellCollection;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.signals.DefaultSignalGroup;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.logging.Loggable;

/**
 * Extract virtual merge source datasets into real root datasets.
 * 
 * @author Ben Skinner
 * @since 1.13.8
 *
 */
public class MergeSourceExtractionMethod extends MultipleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger
			.getLogger(MergeSourceExtractionMethod.class.getName());

	/**
	 * Create with virtual merge source datasets that are to be converted to root
	 * datasets
	 * 
	 * @param toExtract
	 */
	public MergeSourceExtractionMethod(@NonNull List<IAnalysisDataset> toExtract) {
		super(toExtract);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		List<IAnalysisDataset> extracted = extractSourceDatasets();
		return new DefaultAnalysisResult(extracted);
	}

	private List<IAnalysisDataset> extractSourceDatasets() throws ComponentCreationException {
		LOGGER.fine("Extracting merge sources");
		List<IAnalysisDataset> output = new ArrayList<>();

		DatasetValidator dv = new DatasetValidator();

		for (IAnalysisDataset virtualMergeSource : datasets) {

			try {
				IAnalysisDataset extracted = extractMergeSource(virtualMergeSource);

				LOGGER.fine("Checking new datasets from merge source " + extracted.getName());
				if (!dv.validate(extracted)) {
					LOGGER.warning("New dataset failed to validate; resegmentation is recommended");
					LOGGER.fine(dv.getErrors().stream().collect(Collectors.joining("\n")));
				}

				output.add(extracted);
			} catch (MissingDataException | SegmentUpdateException e) {
				LOGGER.warning("Missing analysis options or landmark; skipping "
						+ virtualMergeSource.getName());
				LOGGER.log(Loggable.STACK,
						"Missing analysis options in dataset " + virtualMergeSource.getName(), e);
			}

		}
		LOGGER.fine("Finished extracting merge sources");
		return output;
	}

	/**
	 * Extract the merge source for the given dataset into a real collection
	 * 
	 * @param template
	 * @return
	 * @throws ComponentCreationException
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 * @throws NoSuchElementException     if the template analysis options are not
	 *                                    present
	 */
	private IAnalysisDataset extractMergeSource(@NonNull IAnalysisDataset template)
			throws ComponentCreationException, MissingDataException, SegmentUpdateException {

		ICellCollection templateCollection = template.getCollection();

		ICellCollection newCollection = new DefaultCellCollection(
				templateCollection.getRuleSetCollection(), templateCollection.getName(),
				templateCollection.getId());

		for (ICell c : templateCollection) {
			newCollection.add(c.duplicate());
		}

		IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection,
				template.getSavePath());

		try {
			// Copy over the profile collections
			newDataset.getCollection().getProfileCollection().calculateProfiles();

			IAnalysisDataset parent = getRootParent(template);

			// Copy the merged dataset segmentation into the new dataset.
			// This will match cell segmentations by default, since the cells
			// have been copied from the merged dataset.
			parent.getCollection().getProfileManager()
					.copySegmentsAndLandmarksTo(newDataset.getCollection());

			// Copy over the signal collections where appropriate
			copySignalGroups(template, newDataset);

			// Child datasets are not present in merge sources

		} catch (MissingProfileException e) {
			LOGGER.log(Loggable.STACK, "Cannot copy profile offsets to recovered merge source", e);
		}

		Optional<IAnalysisOptions> op = template.getAnalysisOptions();
		if (op.isPresent())
			newDataset.setAnalysisOptions(op.get().duplicate());

		return newDataset;
	}

	/**
	 * Get the root parent of the dataset. IF the dataset is root, returns
	 * unchanged.
	 * 
	 * @param dataset the dataset to get the root parent of
	 * @return the root parent of the dataset
	 */
	private @NonNull IAnalysisDataset getRootParent(@NonNull IAnalysisDataset dataset) {
		if (dataset.isRoot())
			return dataset;

		if (dataset instanceof VirtualDataset d && d.hasParent()) {
			IAnalysisDataset parent = d.getParent().get();
			if (parent.isRoot())
				return parent;
			return getRootParent(parent);
		}
		return dataset;
	}

	/**
	 * Copy any signal groups in the template collection into the new dataset.
	 * 
	 * @param template   the virtual dataset to copy signal groups from
	 * @param newDataset the dataset to copy the signal groups to
	 * @throws MissingOptionException
	 * @throws NoSuchElementException if a template signal group is not present
	 */
	private void copySignalGroups(IAnalysisDataset template, IAnalysisDataset newDataset)
			throws MissingOptionException {

		ICellCollection newCollection = newDataset.getCollection();

		// Get the analysis options form the merged dataset
		IAnalysisOptions mergedOptions = ((VirtualDataset) template).getParent().get()
				.getAnalysisOptions().get();

		for (UUID signalGroupId : template.getCollection().getSignalGroupIDs()) {

			// Get the signal group options for the signal groups
			HashOptions ns = mergedOptions
					.getNuclearSignalOptions(signalGroupId)
					.orElseThrow(MissingOptionException::new);

			// Get the id saved in the merged dataset options
			UUID originalId = ns.getUUID(HashOptions.ORIGINAL_SIGNAL_PREFIX + template.getId());

			// We only want to make a signal group if a cell with the signal is present in
			// the merge source.
			boolean addSignalGroup = newCollection.stream()
					.flatMap(c -> c.getNuclei().stream())
					.anyMatch(n -> n.getSignalCollection().hasSignal(signalGroupId));

			for (Nucleus n : newDataset.getCollection().getNuclei()) {
				List<INuclearSignal> signals = n.getSignalCollection()
						.getSignals(signalGroupId);
				for (INuclearSignal s : signals) {
					n.getSignalCollection().addSignal(s.duplicate(), originalId);
				}
			}

			if (addSignalGroup) {
				ISignalGroup oldGroup = template.getCollection().getSignalGroup(signalGroupId)
						.orElseThrow(MissingOptionException::new);
				ISignalGroup newGroup = new DefaultSignalGroup(oldGroup.getGroupName(), originalId);
				if (oldGroup.hasColour())
					newGroup.setGroupColour(oldGroup.getGroupColour().get());
				if (oldGroup.hasShellResult())
					newGroup.setShellResult(oldGroup.getShellResult().get());
				if (oldGroup.hasWarpedSignals())
					oldGroup.getWarpedSignals().forEach(newGroup::addWarpedSignal);
				newDataset.getCollection().addSignalGroup(newGroup);
			}
		}
	}
}
