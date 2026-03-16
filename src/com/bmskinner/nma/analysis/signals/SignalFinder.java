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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.detection.AbstractFinder;
import com.bmskinner.nma.analysis.detection.Detector;
import com.bmskinner.nma.analysis.detection.FinderDisplayType;
import com.bmskinner.nma.components.ComponentBuilderFactory;
import com.bmskinner.nma.components.ComponentBuilderFactory.SignalBuilderFactory;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MissingMeasurementException;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.signals.INuclearSignal;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.image.ImageConverter;
import com.bmskinner.nma.visualisation.image.ImageFilterer;

import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;

/**
 * Implementation of the Finder interface for detecting nuclear signals. It
 * generates the step-by-step images for display in the image prober UI, calling
 * a SignalDetector to find the actual signals
 * 
 * @author Ben Skinner
 * @since 1.13.5
 *
 */
public class SignalFinder extends AbstractFinder<INuclearSignal> {

	private static final Logger LOGGER = Logger.getLogger(SignalFinder.class.getName());

	private final HashOptions signalOptions;
	private final ICellCollection collection;
	private final SignalBuilderFactory factory = ComponentBuilderFactory
			.createSignalBuilderFactory();
	private final FinderDisplayType displayType;
	private final SignalThresholdChooser thresholdChooser;

	/**
	 * Create a signal detector for a dataset using the given options
	 * 
	 * @param analysisOptions the dataset analysis options
	 * @param signalOptions   the signal group analysis options
	 * @param collection      the cell collection to detect within
	 */
	public SignalFinder(@NonNull IAnalysisOptions analysisOptions,
			@NonNull HashOptions signalOptions,
			@NonNull ICellCollection collection, @NonNull FinderDisplayType t) {
		super(analysisOptions);
		this.signalOptions = signalOptions;
		this.collection = collection;
		this.displayType = t;
		thresholdChooser = new SignalThresholdChooser();
	}

	@Override
	public List<INuclearSignal> findInFolder(@NonNull File folder) throws ImageImportException {

		final List<INuclearSignal> list = new ArrayList<>();

		if (folder.listFiles() == null)
			return list;

		for (final File f : folder.listFiles()) {
			if (ImageImporter.isFileImportable(f)) {
				try {
					list.addAll(findInFile(f));
				} catch (final ImageImportException e) {
					LOGGER.log(Level.SEVERE, "Error searching image", e);
				}
			}
		}

		return list;
	}

	@Override
	public List<INuclearSignal> findInFile(@NonNull File imageFile) throws ImageImportException {
		List<INuclearSignal> list = new ArrayList<>();
		try {
			if (FinderDisplayType.PREVIEW.equals(displayType)) {
				// Get all objects and annotate if passing filters
				list = detectPreview(imageFile);
			}

			if (FinderDisplayType.PIPELINE.equals(displayType)) {
				// Get only objects matching filters
				list = detectPipeline(imageFile);
			}
		} finally {
			fireProgressEvent();
		}
		return list;
	}

	private List<INuclearSignal> detectPreview(@NonNull File imageFile)
			throws ImageImportException {

		final List<INuclearSignal> list = new ArrayList<>();

		// Import the image processor
		// Note we are checking stack size to avoid exceptions in the preview windows
		// when the image does n
		final ImageStack stack = ImageImporter.importToStack(imageFile);
		final int stackNumber = ImageImporter.rgbToStack(signalOptions.getInt(HashOptions.CHANNEL));
		// Ignore incorrect channel selections
		if (stack.getSize() < stackNumber) {
			LOGGER.finer("Channel not present in image");
			return list;
		}
		final ImageProcessor greyProcessor = stack.getProcessor(stackNumber);

		// Convert to an RGB processor for annotation
		final ImageProcessor ip = new ImageConverter(greyProcessor).convertToRGBGreyscale().invert()
				.toProcessor();

		final ImageProcessor ap = ip.duplicate();

		final ImageAnnotator in = new ImageAnnotator(ip);
		final ImageAnnotator an = new ImageAnnotator(ap);

		// The given image file may not be the same image that the nucleus was
		// detected in.
		// Take the image name only, and add onto the DAPI folder name.
		// This requires that the signal file name is identical to the dapi file
		// name

		final String imageName = imageFile.getName();
		final File dapiFolder = options.getNucleusDetectionFolder().get();

		final File dapiFile = new File(dapiFolder, imageName);

		final Set<Nucleus> nuclei = collection.getNuclei(dapiFile);

		int i = 0;
		for (final Nucleus n : nuclei) {
			try {

				final List<INuclearSignal> temp = new ArrayList<>();
				final int threshold = thresholdChooser.chooseThreshold(greyProcessor, n, signalOptions);

				ImageProcessor gp = greyProcessor.duplicate();
				gp.threshold(threshold);
				if (hasDetectionListeners() && i == 0) {
					final ImageProcessor gpp = gp.duplicate();
					gpp.invert();
					fireDetectionEvent(gpp, "Thresholded");
				}

				if (signalOptions.getBoolean(HashOptions.IS_USE_GAP_CLOSING)) {
					gp = ImageFilterer.close(gp,
							signalOptions.getInt(HashOptions.GAP_CLOSING_RADIUS_INT));

					if (hasDetectionListeners() && i == 0) {
						final ImageProcessor gpp = gp.duplicate();
						gpp.invert();
						fireDetectionEvent(gpp, "Gap closed");
					}
				}

				if (signalOptions.getBoolean(HashOptions.IS_USE_WATERSHED)) {
					gp = ImageFilterer.watershed(gp);
					if (hasDetectionListeners()) {
						final ImageProcessor gpp = gp.duplicate();
						gpp.invert();
						fireDetectionEvent(gpp, "Watershed");
					}
				}

				final Detector d = new Detector();
				final Map<Roi, IPoint> rois = d.getAllRois(gp);

				for (final Entry<Roi, IPoint> entry : rois.entrySet()) {

					if (n.containsOriginalPoint(entry.getValue())) {

						final INuclearSignal s = factory.newBuilder()
								.fromRoi(entry.getKey())
								.withFile(imageFile)
								.withChannel(signalOptions.getInt(HashOptions.CHANNEL))
								.withCoM(entry.getValue())
								.withScale(n.getScale())
								.build();

						temp.add(s);
					}
				}

				if (hasDetectionListeners()) {
					LOGGER.finer("Drawing signals for " + n.getNameAndNumber());
					drawSignals(n, temp, in, false);
					drawSignals(n, temp, an, true);
				}

				for (final INuclearSignal s : temp) {
					if (isValid(s, n)) {
						list.add(s);
					}
				}
				i++;

			} catch (final Exception e) {
				LOGGER.log(Level.SEVERE, "Error in signal detector", e);
			}
		}

		if (hasDetectionListeners()) {
			// annotate detected signals onto the imagefile
			fireDetectionEvent(ip.duplicate(), "Detected objects");

			// annotate detected signals onto the imagefile
			fireDetectionEvent(ap.duplicate(), "Annotated objects");
		}
		return list;
	}

	private List<INuclearSignal> detectPipeline(@NonNull File imageFile)
			throws ImageImportException {

		final List<INuclearSignal> list = new ArrayList<>();

		// Import the image processor
		final ImageProcessor greyProcessor = ImageImporter
				.importImage(imageFile, signalOptions.getInt(HashOptions.CHANNEL));

		// The given image file may not be the same image that the nucleus was
		// detected in.
		// Take the image name only, and add onto the DAPI folder name.
		// This requires that the signal file name is identical to the dapi file
		// name

		final String imageName = imageFile.getName();
		final File dapiFolder = options.getNucleusDetectionFolder().get();

		final File dapiFile = new File(dapiFolder, imageName);

		// Get the nuclei corresponding to the DAPI image
		final Set<Nucleus> nuclei = collection.getNuclei(dapiFile);

		for (final Nucleus n : nuclei) {
			try {

				// Since thresholding can be unique for each nucleus, we duplicate the processor
				final int threshold = thresholdChooser.chooseThreshold(greyProcessor, n, signalOptions);

				ImageProcessor ip = greyProcessor.duplicate();
				ip.threshold(threshold);

				if (signalOptions.getBoolean(HashOptions.IS_USE_GAP_CLOSING)) {
					ip = ImageFilterer.close(ip,
							signalOptions.getInt(HashOptions.GAP_CLOSING_RADIUS_INT));
				}

				final Detector d = new Detector();
				final Map<Roi, IPoint> rois = d.getValidRois(ip, signalOptions, n);

				for (final Entry<Roi, IPoint> entry : rois.entrySet()) {

					final INuclearSignal s = factory.newBuilder()
							.fromRoi(entry.getKey())
							.withFile(imageFile)
							.withChannel(signalOptions.getInt(HashOptions.CHANNEL))
							.withCoM(entry.getValue())
							.withScale(n.getScale())
							.build();
					list.add(s);

				}

			} catch (final Exception e) {
				LOGGER.log(Level.SEVERE, "Error in nucleus signal detector: %s".formatted(e.getMessage()), e);
			}
		}
		return list;
	}

	/**
	 * Draw the signals for the given nucleus on an annotator
	 * 
	 * @param n             the nucleus to annotate
	 * @param list          the signals in the nucleus to be annotated
	 * @param an            the annotator
	 * @param annotateStats should the stats be drawn on the image
	 */
	protected void drawSignals(@NonNull Nucleus n, @NonNull List<INuclearSignal> list,
			@NonNull ImageAnnotator an,
			boolean annotateStats) {

		an.drawBorder(n, Color.BLUE);
		for (final INuclearSignal s : list) {
			final Color color = isValid(s, n) ? Color.ORANGE : Color.RED;
			an.drawBorder(s, color);
			if (annotateStats) {
				an.annotateSignalStats(n, s, Color.YELLOW, Color.BLUE);
			}
		}
	}

	/**
	 * Test if the given signal passes the options criteria
	 * 
	 * @param s the signal
	 * @param n the nucleus the signal belongs to
	 * @return
	 * @throws MissingMeasurementException
	 */
	private boolean isValid(@NonNull INuclearSignal s, @NonNull Nucleus n) {

		try {
			return (n.containsOriginalPoint(s.getOriginalCentreOfMass())
					&& s.getMeasurement(Measurement.AREA) >= signalOptions
							.getInt(HashOptions.MIN_SIZE_PIXELS)
					&& s.getMeasurement(Measurement.AREA) <= (signalOptions
							.getDouble(HashOptions.SIGNAL_MAX_FRACTION)
							* n.getMeasurement(Measurement.AREA)));
		} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
			LOGGER.log(Level.SEVERE,
					"Missing measurement in signal validation: %s".formatted(e.getMessage()), e);
			return false;
		}
	}

	@Override
	public boolean isValid(@NonNull INuclearSignal entity) {
		// A signal should always have a nucleus - we use the method above. This method
		// is just a stub.
		return false;
	}

}
