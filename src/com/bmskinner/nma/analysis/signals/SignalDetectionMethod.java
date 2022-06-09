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
package com.bmskinner.nma.analysis.signals;

import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.analysis.detection.FinderDisplayType;
import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.UnavailableBorderPointException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.signals.DefaultSignalGroup;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.components.signals.ISignalCollection;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.components.signals.UnavailableSignalGroupException;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.logging.Loggable;

/**
 * Method to detect nuclear signals in a dataset
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class SignalDetectionMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(SignalDetectionMethod.class.getName());

	protected final HashOptions options;
	protected final File folder;
	protected final int channel;

	/**
	 * For use when running on an existing dataset
	 * 
	 * @param d       the dataset to add signals to
	 * @param options the analysis options
	 * @param group   the signal group to add signals to
	 * @throws UnavailableSignalGroupException if the group is not present in the
	 *                                         dataset
	 */

	public SignalDetectionMethod(@NonNull final IAnalysisDataset d,
			@NonNull final HashOptions options, @NonNull File folder) {
		super(d);

		if (!d.getAnalysisOptions().isPresent())
			throw new IllegalArgumentException("No analysis options in dataset");

		if (!options.hasString(HashOptions.SIGNAL_DETECTION_MODE_KEY))
			throw new IllegalArgumentException("Signal options are not complete");

		this.options = options.duplicate();
		this.folder = folder;
		this.channel = options.getInt(HashOptions.CHANNEL);

		// Create a signal group in the dataset
		ISignalGroup group = new DefaultSignalGroup(
				options.getString(HashOptions.SIGNAL_GROUP_NAME),
				options.getUUID(HashOptions.SIGNAL_GROUP_ID));
		// Set the default colour for the signal group
		Color colour = ColourSelecter.getSignalColour(options.getInt(HashOptions.CHANNEL));
		group.setGroupColour(colour);

		dataset.getCollection().addSignalGroup(group);

		dataset.getAnalysisOptions().get().setNuclearSignalDetectionOptions(options);
		dataset.getAnalysisOptions().get().setNuclearSignalDetectionFolder(
				options.getUUID(HashOptions.SIGNAL_GROUP_ID), folder);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		Optional<IAnalysisDataset> signalChild = Optional.ofNullable(makeSignalChildDataset());
		if (signalChild.isEmpty())
			return new DefaultAnalysisResult(dataset);
		return new DefaultAnalysisResult(signalChild.get());
	}

	protected void run() {

		LOGGER.fine("Beginning signal detection in channel " + channel);

		int originalMinThreshold = options.getInt(HashOptions.THRESHOLD);

		SignalFinder finder = new SignalFinder(dataset.getAnalysisOptions().get(), options,
				dataset.getCollection(), FinderDisplayType.PIPELINE);

		dataset.getCollection().getCells()
				.forEach(c -> detectInCell(c, finder, originalMinThreshold));
	}

	private void detectInCell(ICell c, SignalFinder finder, int originalMinThreshold) {
		// reset the min threshold for each cell
		options.setInt(HashOptions.THRESHOLD, originalMinThreshold);

		for (Nucleus n : c.getNuclei())
			detectInNucleus(n, finder);

		fireProgressEvent();
	}

	private void detectInNucleus(Nucleus n, SignalFinder finder) {

		LOGGER.finer(
				"Looking for signals associated with nucleus " + n.getSourceFileName() + "-"
						+ n.getNucleusNumber());

		// get the image in the folder with the same name as the
		// nucleus source image
		File imageFile = new File(folder, n.getSourceFileName());
		LOGGER.finer("Source file: " + imageFile.getAbsolutePath());

		try {

			// Get all the signals in the image
			List<INuclearSignal> signals = finder.findInImage(imageFile);

			// Restrict to signals in the current nucleus of interest
			List<INuclearSignal> signalsInNucleus = signals.stream()
					.filter(s -> s.containsOriginalPoint(s.getOriginalCentreOfMass()))
					.collect(Collectors.toList());

			// No need to add a group to a nucleus if there were no signals
			if (!signalsInNucleus.isEmpty()) {

				ISignalCollection signalCollection = n.getSignalCollection();
				signalCollection.addSignalGroup(signalsInNucleus,
						options.getUUID(HashOptions.SIGNAL_GROUP_ID));

				// Measure the detected signals in the nucleus
				SignalMeasurer.calculateSignalDistancesFromCoM(n);
				SignalMeasurer.calculateFractionalSignalDistancesFromCoM(n);
				SignalMeasurer.calculateSignalAngles(n);
			}
		} catch (ImageImportException | UnavailableBorderPointException | MissingLandmarkException
				| ComponentCreationException e) {
			LOGGER.warning("Cannot open " + imageFile.getAbsolutePath());
			LOGGER.log(Loggable.STACK, "Cannot load image", e);
		}
	}

	/**
	 * Create child datasets for signal populations and perform basic analyses
	 * 
	 * @param collection
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 */
	private IAnalysisDataset makeSignalChildDataset()
			throws MissingProfileException, ProfileException, MissingLandmarkException {

		Optional<ISignalGroup> og = dataset.getCollection()
				.getSignalGroup(options.getUUID(HashOptions.SIGNAL_GROUP_ID));

		if (!og.isPresent())
			return null;

		ISignalGroup group = og.get();
		group.setVisible(true);

		List<ICell> collection = dataset.getCollection().getSignalManager()
				.getCellsWithNuclearSignals(options.getUUID(HashOptions.SIGNAL_GROUP_ID), true);

		if (collection.isEmpty())
			return null;

		VirtualDataset subDataset = new VirtualDataset(dataset,
				og.get().getGroupName() + "_with_signals", UUID.randomUUID());
		subDataset.addAll(collection);

		dataset.addChildDataset(subDataset);
		dataset.getCollection().getProfileManager().copySegmentsAndLandmarksTo(subDataset);
		return dataset.getChildDataset(subDataset.getId());
	}
}
