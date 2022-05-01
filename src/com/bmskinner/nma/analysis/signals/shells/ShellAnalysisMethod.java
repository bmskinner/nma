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
package com.bmskinner.nma.analysis.signals.shells;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.signals.DefaultShellResult;
import com.bmskinner.nma.components.signals.DefaultSignalGroup;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.components.signals.IShellResult;
import com.bmskinner.nma.components.signals.IShellResult.CountType;
import com.bmskinner.nma.components.signals.IShellResult.ShrinkType;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.logging.Loggable;

import ij.ImageStack;

/**
 * Detect signal proportions within concentric shells of a nucleus
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class ShellAnalysisMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(ShellAnalysisMethod.class.getName());

	/** A shell must have a minimum size */
	public static final int MINIMUM_AREA_PER_SHELL = 50;

	/** A shell must have a minimum circularity */
	public static final double MINIMUM_CIRCULARITY = 0.07;

	/** The analysis options */
	private final HashOptions options;

	/** The cells to be analysed */
	private ICellCollection collection;

	private final Map<UUID, DefaultShellResult> counters = new HashMap<>();

	public ShellAnalysisMethod(@NonNull final IAnalysisDataset dataset,
			@NonNull final HashOptions o) {
		super(dataset);
		options = o;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		fireUpdateProgressTotalLength(dataset.getCollection().getNucleusCount() - 1);
		run();
		return new DefaultAnalysisResult(dataset);
	}

	private void run() throws MissingComponentException, ImageImportException {
		collection = dataset.getCollection();
		if (!collection.getSignalManager().hasSignals())
			return;

		LOGGER.fine(
				() -> String.format("Performing %s shell analysis with %s shells on dataset %s...",
						options.getString(HashOptions.SHELL_EROSION_METHOD_KEY),
						options.getInt(HashOptions.SHELL_COUNT_INT), collection.getName()));

		// If this is a child, and the parent already has the data, just copy it
		if (collection.isVirtual()) {
			IAnalysisDataset parent = ((VirtualDataset) collection).getParent().get();
			for (UUID signalGroupId : collection.getSignalManager().getSignalGroupIDs()) {
				if (parent.getCollection().getSignalGroup(signalGroupId).get().hasShellResult())
					copyShellResults(signalGroupId, parent.getCollection(), collection);
			}

			Optional<ISignalGroup> randomGroup = parent.getCollection()
					.getSignalGroup(IShellResult.RANDOM_SIGNAL_ID);
			if (!randomGroup.isPresent()) {
				LOGGER.warning("Parent dataset does not have shell results to copy");
				return;
			}
			if (randomGroup.get().hasShellResult())
				copyShellResults(IShellResult.RANDOM_SIGNAL_ID, parent.getCollection(), collection);
			return;
		}

		createShellCounters();

		// make the shells and measure the values
		for (ICell c : collection) {
			new CellAnalysis(c).analyse();
		}

		fireIndeterminateState();
		createResults();

		LOGGER.info("Shell analysis complete");

	}

	/**
	 * Create the shell results for each signal group
	 * 
	 * @throws MissingComponentException
	 */
	private void createShellCounters() throws MissingComponentException {
		for (UUID signalGroupId : collection.getSignalManager().getSignalGroupIDs()) {
			if (IShellResult.RANDOM_SIGNAL_ID.equals(signalGroupId))
				continue;
			counters.put(signalGroupId, new DefaultShellResult(
					options.getInt(HashOptions.SHELL_COUNT_INT),
					ShrinkType.valueOf(options.getString(HashOptions.SHELL_EROSION_METHOD_KEY))));

			// Assign the options to each signal group
			Optional<IAnalysisOptions> datasetOptions = dataset.getAnalysisOptions();
			if (!datasetOptions.isPresent()) {
				LOGGER.warning("No analysis options in dataset; unable to set shell options");
				return;
			}

			// Store the shell options in the signal options
			datasetOptions.get().getNuclearSignalOptions(signalGroupId)
					.orElseThrow(MissingComponentException::new).set(options);
		}

		// Ensure a random distribution exists
		if (!collection.getSignalManager().getSignalGroupIDs()
				.contains(IShellResult.RANDOM_SIGNAL_ID))
			counters.put(IShellResult.RANDOM_SIGNAL_ID, new DefaultShellResult(
					options.getInt(HashOptions.SHELL_COUNT_INT),
					ShrinkType.valueOf(options.getString(HashOptions.SHELL_EROSION_METHOD_KEY))));
	}

	private synchronized void createResults() {

		boolean addRandom = false;

		for (Entry<UUID, DefaultShellResult> entry : counters.entrySet()) {
			addRandom |= collection.getSignalManager().hasSignals(entry.getKey());
			if (collection.getSignalManager().hasSignals(entry.getKey())) {
				collection.getSignalGroup(entry.getKey()).get().setShellResult(entry.getValue());
				copyShellResultsToChildDatasets(entry.getKey());
			}
		}

		// Only add a random signal if there was at least one other signal group present
		if (addRandom)
			addRandomSignal();
	}

	/**
	 * Copy shell values from the source to the destination collection. If is
	 * assumed that the destination is a child of the source
	 * 
	 * @param group
	 * @param src
	 * @param dest
	 */
	private void copyShellResults(@NonNull UUID group, @NonNull ICellCollection src,
			@NonNull ICellCollection dest) {
		IShellResult channelCounter = src.getSignalGroup(group).get().getShellResult().get();
		if (dest.getSignalManager().hasSignals(group)
				|| IShellResult.RANDOM_SIGNAL_ID.equals(group)) {
			DefaultShellResult childCounter = new DefaultShellResult(
					options.getInt(HashOptions.SHELL_COUNT_INT),
					ShrinkType.valueOf(options.getString(HashOptions.SHELL_EROSION_METHOD_KEY)));
			for (ICell c : dest.getCells()) {
				for (Nucleus n : c.getNuclei()) {

					long[] counterstain = channelCounter.getPixelValues(CountType.COUNTERSTAIN, c,
							n, null);
					long[] signals = channelCounter.getPixelValues(CountType.SIGNAL, c, n, null);

					if (counterstain == null) {
						LOGGER.fine("No counterstain for " + n.getNameAndNumber());
						continue;
					}
					if (signals == null)
						continue;

					childCounter.addShellData(CountType.COUNTERSTAIN, c, n, counterstain);
					childCounter.addShellData(CountType.SIGNAL, c, n, signals);

					for (INuclearSignal s : n.getSignalCollection().getSignals(group)) {
						long[] signalValue = channelCounter.getPixelValues(CountType.SIGNAL, c, n,
								s);
						childCounter.addShellData(CountType.SIGNAL, c, n, s, signalValue);
					}

					// We can't select signals by group in the random set
					if (IShellResult.RANDOM_SIGNAL_ID.equals(group)) {
						n.getSignalCollection().getSignals().stream().flatMap(l -> l.stream())
								.forEach(s -> {
									long[] signalValue = channelCounter
											.getPixelValues(CountType.SIGNAL, c, n, s);
									childCounter.addShellData(CountType.SIGNAL, c, n, s,
											signalValue);
								});
					}
				}
			}

			dest.getSignalGroup(group).get().setShellResult(childCounter);
		}
	}

	/**
	 * Duplicate cell values to new shell results for child collections
	 * 
	 * @param group
	 */
	private void copyShellResultsToChildDatasets(UUID group) {
		for (IAnalysisDataset childDataset : dataset.getAllChildDatasets()) {
			ICellCollection child = childDataset.getCollection();
			copyShellResults(group, collection, child);
		}
	}

	private synchronized void addRandomSignal() {
		ISignalGroup random = new DefaultSignalGroup("Random distribution",
				IShellResult.RANDOM_SIGNAL_ID);
		random.setGroupColour(Color.LIGHT_GRAY);
		DefaultShellResult channelCounter = counters.get(IShellResult.RANDOM_SIGNAL_ID);
		random.setShellResult(channelCounter);
		// If a random group already exists, clear it
		collection.removeSignalGroup(IShellResult.RANDOM_SIGNAL_ID);
		collection.addSignalGroup(random);
		copyShellResultsToChildDatasets(IShellResult.RANDOM_SIGNAL_ID);
	}

	/**
	 * Abstracts the business of creating shell detectors.
	 * 
	 * @author bms41
	 * @since 1.13.8
	 *
	 */
	private class CellAnalysis {
		private ShellDetector shellDetector;
		final ICell c;

		public CellAnalysis(@NonNull ICell c) {
			this.c = c;
		}

		public synchronized void analyse() throws ImageImportException {

			for (Nucleus n : c.getNuclei()) {
				analyseNucleus(n);
				fireProgressEvent();
			}
		}

		private synchronized void analyseNucleus(@NonNull final Nucleus n)
				throws ImageImportException {

			try {
				shellDetector = new ShellDetector(n,
						options.getInt(HashOptions.SHELL_COUNT_INT),
						ShrinkType
								.valueOf(options.getString(HashOptions.SHELL_EROSION_METHOD_KEY)));
			} catch (ShellAnalysisException e1) {
				LOGGER.warning("Unable to make shells for " + n.getNameAndNumber());
				LOGGER.log(Loggable.STACK, "Error in shell detector", e1);
				return;
			}

			for (UUID signalGroup : n.getSignalCollection().getSignalGroupIds()) {

				if (signalGroup.equals(IShellResult.RANDOM_SIGNAL_ID))
					continue;
				analyseSignalGroup(n, signalGroup);
			}
		}

		private synchronized void analyseSignalGroup(@NonNull final Nucleus n,
				@NonNull final UUID signalGroup) throws ImageImportException {
			if (!collection.getSignalManager().hasSignals(signalGroup))
				return;

			List<INuclearSignal> signals = n.getSignalCollection().getSignals(signalGroup);
			if (signals.isEmpty())
				return;

			File sourceFile = n.getSignalCollection().getSourceFile(signalGroup);

			if (sourceFile == null)
				return;

			DefaultShellResult counter = counters.get(signalGroup);

			ImageStack signalStack = new ImageImporter(sourceFile).importToStack();
			int signalChannel = n.getSignalCollection().getSourceChannel(signalGroup);

			long[] totalSignalIntensity = shellDetector.findPixelIntensities(signalStack,
					signalChannel);
			long[] totalCounterIntensity = shellDetector.findPixelIntensities(n);

			counter.addShellData(CountType.COUNTERSTAIN, c, n, totalCounterIntensity); // the
																						// counterstain
																						// within
																						// the
																						// nucleus
			counter.addShellData(CountType.SIGNAL, c, n, totalSignalIntensity); // the pixels within
																				// the whole nucleus

			long[] random = new RandomDistribution(n, shellDetector,
					RandomDistribution.DEFAULT_ITERATIONS).getCounts();

			counters.get(IShellResult.RANDOM_SIGNAL_ID).addShellData(CountType.SIGNAL, c, n,
					random); // random pixels in the nucleus
			counters.get(IShellResult.RANDOM_SIGNAL_ID).addShellData(CountType.COUNTERSTAIN, c, n,
					totalCounterIntensity); // counterstain for random signal

			for (INuclearSignal s : signals) {
				long[] countsInSignals = shellDetector.findPixelIntensities(s);
				counter.addShellData(CountType.SIGNAL, c, n, s, countsInSignals); // the pixels
																					// within the
																					// signal
				counters.get(IShellResult.RANDOM_SIGNAL_ID).addShellData(CountType.SIGNAL, c, n, s,
						random); // same random pixels in the nucleus keyed to the signal
			}
		}
	}

	private class RandomDistribution {

		private static final long DEFAULT_SEED = 1234;

		private long[] counts;
		private ShellDetector shellDetector;
		private Random rng;

		public static final int DEFAULT_ITERATIONS = 10000;

		public RandomDistribution(@NonNull CellularComponent template, ShellDetector detector,
				int iterations) {
			rng = new Random(DEFAULT_SEED);
			if (iterations <= 0)
				throw new IllegalArgumentException("Must have at least one iteration");
			shellDetector = detector;
			counts = new long[options.getInt(HashOptions.SHELL_COUNT_INT)];
			for (int i = 0; i < options.getInt(HashOptions.SHELL_COUNT_INT); i++) {
				counts[i] = 0;
			}

			List<IPoint> list = new ArrayList<>();
			for (int i = 0; i < iterations; i++) {
				IPoint p = createRandomPoint(template);
				while (!template.containsOriginalPoint(p))
					p = createRandomPoint(template);
				list.add(p);
			}

			int unMappedPoints = 0;
			for (IPoint p : list) {
				int shell = shellDetector.findShell(p);
				if (shell >= 0) // -1 for point not found
					counts[shell]++;
				else {
					unMappedPoints++;
				}
			}
			LOGGER.finest(String.format("Random signal: %s of %s points were not in a shell",
					unMappedPoints, iterations));

		}

		public long[] getCounts() {
			return counts;
		}

		/**
		 * Create a random point that lies within the template
		 * 
		 * @param template
		 * @return
		 */
		private IPoint createRandomPoint(@NonNull CellularComponent template) {

			IPoint base = template.getOriginalBase();
			double w = template.getWidth();
			double h = template.getHeight();

			// Make a pseudo-random position in the rectangle
			double rx = rng.nextDouble();
			double ry = rng.nextDouble();

			double xrange = w * rx;
			double yrange = h * ry;

			return new FloatPoint(base.getX() + xrange, base.getY() + yrange);
		}
	}

	public class ShellAnalysisException extends Exception {
		private static final long serialVersionUID = 1L;

		public ShellAnalysisException() {
			super();
		}

		public ShellAnalysisException(String message) {
			super(message);
		}

		public ShellAnalysisException(String message, Throwable cause) {
			super(message, cause);
		}

		public ShellAnalysisException(Throwable cause) {
			super(cause);
		}
	}

}
