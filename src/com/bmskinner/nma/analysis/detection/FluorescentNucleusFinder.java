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
package com.bmskinner.nma.analysis.detection;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.nucleus.PoorEdgeDetectionProfilePredicate;
import com.bmskinner.nma.components.ComponentBuilderFactory;
import com.bmskinner.nma.components.ComponentBuilderFactory.NucleusBuilderFactory;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.DefaultCell;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.image.ImageFilterer;

import ij.gui.Roi;
import ij.process.ImageProcessor;

public class FluorescentNucleusFinder extends CellFinder {

	private static final Logger LOGGER = Logger.getLogger(FluorescentNucleusFinder.class.getName());

	private final NucleusBuilderFactory factory;
	private final @NonNull HashOptions nuclOptions;
	private final FinderDisplayType displayType;

	private Predicate<ICell> validCellPredicate = null;

	public FluorescentNucleusFinder(@NonNull final IAnalysisOptions op,
			@NonNull FinderDisplayType t) {
		super(op);
		displayType = t;

		if (op.getRuleSetCollection() == null)
			throw new IllegalArgumentException("No ruleset provided");

		nuclOptions = options.getNucleusDetectionOptions()
				.orElseThrow(() -> new IllegalArgumentException("No nucleus options"));

		validCellPredicate = createCellPredicate();

		factory = ComponentBuilderFactory.createNucleusBuilderFactory(op.getRuleSetCollection(),
				options.getProfileWindowProportion(), nuclOptions.getDouble(HashOptions.SCALE));
	}

	/**
	 * Create the predicate for testing valid cells
	 * 
	 * @return
	 */
	private Predicate<ICell> createCellPredicate() {

		LOGGER.finer("Creating cell predicate");

//		try {
		final Predicate<ICell> edgeFilter = new PoorEdgeDetectionProfilePredicate(
				options.getRuleSetCollection().getOtherOptions());

		return (t) -> {

			boolean result = true;

			for (Nucleus n : t.getNuclei()) {
				result &= n.getMeasurement(Measurement.AREA) > nuclOptions
						.getInt(HashOptions.MIN_SIZE_PIXELS);

				result &= n.getMeasurement(Measurement.AREA) < nuclOptions
						.getInt(HashOptions.MAX_SIZE_PIXELS);

				result &= n.getMeasurement(Measurement.CIRCULARITY) > nuclOptions
						.getDouble(HashOptions.MIN_CIRC);

				result &= n.getMeasurement(Measurement.CIRCULARITY) < nuclOptions
						.getDouble(HashOptions.MAX_CIRC);
			}

			// Only use the predicate if the box was ticked and the options are present
			if (nuclOptions.has(HashOptions.IS_RULESET_EDGE_FILTER)
					&& nuclOptions.getBoolean(HashOptions.IS_RULESET_EDGE_FILTER)) {

				result &= edgeFilter.test(t);
			}

			return result;
		};

//		} catch (MissingOptionException e) {
//			LOGGER.log(Level.SEVERE,
//					"Unable to create cell predicate: %s".formatted(e.getMessage()), e);
//			return (t) -> {
//				return false;
//			};
//		}
	}

	@Override
	public List<ICell> findInImage(@NonNull final File imageFile) throws ImageImportException {
		List<ICell> list = new ArrayList<>();

		try {
			if (FinderDisplayType.PREVIEW.equals(displayType)) {
				// Get all objects and annotate if passing filters
				list = detectNucleusPreview(imageFile);
			}

			if (FinderDisplayType.PIPELINE.equals(displayType)) {
				// Get only objects matching filters
				list = detectNucleusPipeline(imageFile);
			}
		} finally {
			fireProgressEvent();
		}
		return list;
	}

	private List<ICell> detectNucleusPipeline(@NonNull final File imageFile)
			throws ImageImportException {
		List<Nucleus> list = new ArrayList<>();

		ImageProcessor ip = ImageImporter.importImage(imageFile,
				nuclOptions.getInt(HashOptions.CHANNEL));

		ImageFilterer filt = new ImageFilterer(ip.duplicate());
		if (nuclOptions.getBoolean(HashOptions.IS_USE_KUWAHARA)) {
			filt.kuwaharaFilter(nuclOptions.getInt(HashOptions.KUWAHARA_RADIUS_INT));
		}

		if (nuclOptions.getBoolean(HashOptions.IS_USE_FLATTENING)) {
			filt.setMaximumPixelValue(nuclOptions.getInt(HashOptions.FLATTENING_THRESHOLD_INT));
		}

		if (nuclOptions.getBoolean(HashOptions.IS_USE_CANNY)) {
			filt.cannyEdgeDetection(nuclOptions);
			filt.close(nuclOptions.getInt(HashOptions.GAP_CLOSING_RADIUS_INT));

		} else {
			filt.threshold(nuclOptions.getInt(HashOptions.THRESHOLD));
			if (nuclOptions.getBoolean(HashOptions.IS_USE_GAP_CLOSING)) {
				filt.close(nuclOptions.getInt(HashOptions.GAP_CLOSING_RADIUS_INT));
			}
		}

		if (nuclOptions.getBoolean(HashOptions.IS_USE_WATERSHED)) {
			filt.watershed();
		}

		LOGGER.finer("Detecting ROIs in " + imageFile.getName());
		Detector gd = new Detector();

		Map<Roi, IPoint> rois = gd.getValidRois(filt.toProcessor(), nuclOptions);
		LOGGER.finer(() -> "Image: " + imageFile.getName() + ": " + rois.size() + " rois");

		for (Entry<Roi, IPoint> entry : rois.entrySet()) {
			try {

				Nucleus n = factory.newBuilder()
						.fromRoi(entry.getKey())
						.withFile(imageFile)
						.withChannel(nuclOptions.getInt(HashOptions.CHANNEL))
						.withCoM(entry.getValue())
						.build();

				list.add(n);
			} catch (ComponentCreationException e) {
				LOGGER.log(Loggable.STACK, "Unable to create nucleus from roi: %s; skipping"
						.formatted(e.getMessage()), e);
			}
		}
		LOGGER.finer(() -> "Detected %d nuclei in %s".formatted(list.size(), imageFile.getName()));

		List<ICell> result = new ArrayList<>();
		for (Nucleus n : list) {
			ICell c = new DefaultCell(n);
			if (isValid(c)) {
				DatasetValidator dv = new DatasetValidator();
				if (!dv.validate(c))
					LOGGER.fine("Error in cell " + n.getNameAndNumber() + ": " + dv.getSummary()
							+ dv.getErrors());
				result.add(c);
			}
		}
		return result;
	}

	private List<ICell> detectNucleusPreview(@NonNull final File imageFile)
			throws ImageImportException {

		List<Nucleus> list = new ArrayList<>();

		ImageProcessor ip = ImageImporter.importImage(imageFile,
				nuclOptions.getInt(HashOptions.CHANNEL));

		ImageFilterer filt = new ImageFilterer(ip.duplicate());
		if (nuclOptions.getBoolean(HashOptions.IS_USE_KUWAHARA)) {
			filt.kuwaharaFilter(nuclOptions.getInt(HashOptions.KUWAHARA_RADIUS_INT));
			if (hasDetectionListeners()) {
				ip = filt.toProcessor().duplicate();
				ip.invert();
				fireDetectionEvent(ip.duplicate(), "Kuwahara filter");
			}
		}

		if (nuclOptions.getBoolean(HashOptions.IS_USE_FLATTENING)) {
			filt.setMaximumPixelValue(nuclOptions.getInt(HashOptions.FLATTENING_THRESHOLD_INT));
			if (hasDetectionListeners()) {
				ip = filt.toProcessor().duplicate();
				ip.invert();
				fireDetectionEvent(ip.duplicate(), "Chromocentre flattening");
			}
		}

		if (nuclOptions.getBoolean(HashOptions.IS_USE_CANNY)) {
			filt.cannyEdgeDetection(nuclOptions);
			if (hasDetectionListeners()) {
				ip = filt.toProcessor().duplicate();
				ip.invert();
				fireDetectionEvent(ip.duplicate(), "Edge detection");
			}

			filt.close(nuclOptions.getInt(HashOptions.GAP_CLOSING_RADIUS_INT));
			if (hasDetectionListeners()) {
				ip = filt.toProcessor().duplicate();
				ip.invert();
				fireDetectionEvent(ip.duplicate(), "Gap closing");
			}

		} else {
			filt.threshold(nuclOptions.getInt(HashOptions.THRESHOLD));

			if (hasDetectionListeners()) {
				ip = filt.toProcessor().duplicate();
				ip.invert();
				fireDetectionEvent(ip.duplicate(), "Thresholded");
			}

			if (nuclOptions.getBoolean(HashOptions.IS_USE_GAP_CLOSING)) {

				filt.close(nuclOptions.getInt(HashOptions.GAP_CLOSING_RADIUS_INT));
				if (hasDetectionListeners()) {
					ip = filt.toProcessor().duplicate();
					ip.invert();
					fireDetectionEvent(ip.duplicate(), "Gap closing");
				}
			}
		}

		if (nuclOptions.getBoolean(HashOptions.IS_USE_WATERSHED)) {
			filt.watershed();
			if (hasDetectionListeners()) {
				ip = filt.toProcessor().duplicate();
				ip.invert();
				fireDetectionEvent(ip.duplicate(), "Watershed");
			}
		}

		Detector gd = new Detector();

		// Display passing and failing size nuclei
		ImageProcessor original = ImageImporter
				.importImageAndInvert(imageFile, nuclOptions.getInt(HashOptions.CHANNEL))
				.convertToRGB();

		ImageProcessor img = filt.toProcessor();

		Map<Roi, IPoint> rois = gd.getAllRois(img.duplicate());
		LOGGER.finer(() -> "Image: %s has %d rois ".formatted(imageFile.getName(), rois.size()));

		for (Entry<Roi, IPoint> entry : rois.entrySet()) {
			try {
				list.add(factory.newBuilder()
						.fromRoi(entry.getKey())
						.withFile(imageFile)
						.withChannel(nuclOptions.getInt(HashOptions.CHANNEL))
						.withCoM(entry.getValue())
						.build());
			} catch (ComponentCreationException e) {
				LOGGER.log(Level.FINE, "Unable to create nucleus from roi: %s; skipping"
						.formatted(e.getMessage()), e);
			}
		}
		LOGGER.finer(() -> "Detected nuclei in " + imageFile.getName());

		List<ICell> result = new ArrayList<>();
		List<ICell> invalid = new ArrayList<>();

		for (Nucleus n : list) {
			ICell c = new DefaultCell(n);
			if (isValid(c)) {
				result.add(c);
			} else {
				invalid.add(c);
			}
		}

		// Draw the outlines of passing and invalid nuclei
		ImageAnnotator ann = new ImageAnnotator(original);

		for (ICell c : result) {
			c.getNuclei().forEach(n -> ann.drawBorder(n, Color.ORANGE));
		}

		for (ICell c : invalid) {
			c.getNuclei().forEach(n -> ann.drawBorder(n, Color.RED));
		}

		fireDetectionEvent(ann.toProcessor().duplicate(), "Detected objects");

		// Draw the annotated objects
		for (Nucleus n : list) {
			ann.annotateStats(n, Color.ORANGE, Color.BLUE);
		}
		fireDetectionEvent(ann.toProcessor().duplicate(), "Annotated objects");

		// Return all the cells, whether valid or not for display only
		if (hasDetectionListeners()) {
			fireDetectedObjectEvent(result, invalid, "Detected objects");
		}

		return result;
	}

	@Override
	public boolean isValid(@NonNull ICell c) {
		return validCellPredicate.test(c);
	}

}
