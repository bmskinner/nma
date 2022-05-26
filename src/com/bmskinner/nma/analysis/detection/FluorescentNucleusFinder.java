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
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.ComponentBuilderFactory;
import com.bmskinner.nma.components.ComponentBuilderFactory.NucleusBuilderFactory;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.DefaultCell;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.generic.IPoint;
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

	public FluorescentNucleusFinder(@NonNull final IAnalysisOptions op,
			@NonNull FinderDisplayType t) {
		super(op);
		displayType = t;

		if (op.getRuleSetCollection() == null)
			throw new IllegalArgumentException("No ruleset provided");

		nuclOptions = options.getDetectionOptions(CellularComponent.NUCLEUS)
				.orElseThrow(() -> new IllegalArgumentException("No nucleus options"));

		factory = ComponentBuilderFactory.createNucleusBuilderFactory(op.getRuleSetCollection(),
				options.getProfileWindowProportion(), nuclOptions.getDouble(HashOptions.SCALE));
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

		ImageImporter importer = new ImageImporter(imageFile);

		ImageProcessor ip = importer.importImage(nuclOptions.getInt(HashOptions.CHANNEL));

		ImageFilterer filt = new ImageFilterer(ip.duplicate());
		if (nuclOptions.getBoolean(HashOptions.IS_USE_KUWAHARA)) {
			filt.kuwaharaFilter(nuclOptions.getInt(HashOptions.KUWAHARA_RADIUS_INT));
		}

		if (nuclOptions.getBoolean(HashOptions.IS_USE_FLATTENING)) {
			filt.setMaximumPixelValue(nuclOptions.getInt(HashOptions.FLATTENING_THRESHOLD_INT));
		}

		if (nuclOptions.getBoolean(HashOptions.IS_USE_CANNY)) {
			filt.cannyEdgeDetection(nuclOptions);
			filt.close(nuclOptions.getInt(HashOptions.CANNY_CLOSING_RADIUS_INT));

		} else {
			filt.threshold(nuclOptions.getInt(HashOptions.THRESHOLD));
		}

		if (nuclOptions.getBoolean(HashOptions.IS_USE_WATERSHED)) {
			filt.watershed();
		}

		LOGGER.finer("Detecting ROIs in " + imageFile.getName());
		GenericDetector gd = new GenericDetector();

		gd.setSize(nuclOptions.getInt(HashOptions.MIN_SIZE_PIXELS),
				nuclOptions.getInt(HashOptions.MAX_SIZE_PIXELS));

		ImageProcessor img = filt.toProcessor();

		Map<Roi, IPoint> rois = gd.getRois(img.duplicate());
		LOGGER.finer(() -> "Image: " + imageFile.getName() + ": " + rois.size() + " rois");

		for (Entry<Roi, IPoint> entry : rois.entrySet()) {
			try {

				Nucleus n = factory.newBuilder().fromRoi(entry.getKey()).withFile(imageFile)
						.withChannel(nuclOptions.getInt(HashOptions.CHANNEL))
						.withCoM(entry.getValue()).build();

				list.add(n);
			} catch (ComponentCreationException e) {
				LOGGER.log(Loggable.STACK,
						"Unable to create nucleus from roi: " + e.getMessage() + "; skipping", e);
			}
		}
		LOGGER.finer(() -> "Detected nuclei in " + imageFile.getName());

		List<ICell> result = new ArrayList<>();
		for (Nucleus n : list) {
			if (isValid(nuclOptions, n)) {
				ICell c = new DefaultCell(n);
				DatasetValidator dv = new DatasetValidator();
				if (!dv.validate(c))
					LOGGER.fine("Error in cell " + n.getNameAndNumber() + ": " + dv.getSummary()
							+ dv.getErrors());
				result.add(new DefaultCell(n));
			}
		}
		return result;
	}

	private List<ICell> detectNucleusPreview(@NonNull final File imageFile)
			throws ImageImportException {

		List<Nucleus> list = new ArrayList<>();

		ImageImporter importer = new ImageImporter(imageFile);

		ImageProcessor ip = importer.importImage(nuclOptions.getInt(HashOptions.CHANNEL));

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

			filt.close(nuclOptions.getInt(HashOptions.CANNY_CLOSING_RADIUS_INT));
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
		}

		if (nuclOptions.getBoolean(HashOptions.IS_USE_WATERSHED)) {
			filt.watershed();
			if (hasDetectionListeners()) {
				ip = filt.toProcessor().duplicate();
				ip.invert();
				fireDetectionEvent(ip.duplicate(), "Watershed");
			}
		}

		GenericDetector gd = new GenericDetector();

		// Display passing and failing size nuclei
		ImageProcessor original = importer
				.importImageAndInvert(nuclOptions.getInt(HashOptions.CHANNEL)).convertToRGB();

		// do not use the minimum nucleus size - we want the roi outlined in red
		gd.setSize(MIN_PROFILABLE_OBJECT_SIZE, original.getWidth() * original.getHeight());

		ImageProcessor img = filt.toProcessor();

		Map<Roi, IPoint> rois = gd.getRois(img.duplicate());
		LOGGER.finer(() -> "Image: " + imageFile.getName() + ": " + rois.size() + " rois");

		for (Entry<Roi, IPoint> entry : rois.entrySet()) {
			try {
				list.add(factory.newBuilder().fromRoi(entry.getKey()).withFile(imageFile)
						.withChannel(nuclOptions.getInt(HashOptions.CHANNEL))
						.withCoM(entry.getValue()).build());
			} catch (ComponentCreationException e) {
				LOGGER.log(Loggable.STACK,
						"Unable to create nucleus from roi: " + e.getMessage() + "; skipping", e);
			}
		}
		LOGGER.finer(() -> "Detected nuclei in " + imageFile.getName());

		ImageAnnotator ann = new ImageAnnotator(original);

		for (Nucleus n : list) {
			Color colour = isValid(nuclOptions, n) ? Color.ORANGE : Color.RED;
			ann.drawBorder(n, colour);
		}
		fireDetectionEvent(ann.toProcessor().duplicate(), "Detected objects");

		for (Nucleus n : list) {
			ann.annotateStats(n, Color.ORANGE, Color.BLUE);
		}
		fireDetectionEvent(ann.toProcessor().duplicate(), "Annotated objects");

		List<ICell> result = new ArrayList<>();
		for (Nucleus n : list) {
			if (isValid(nuclOptions, n)) {
				result.add(new DefaultCell(n));
			}
		}
		return result;
	}

}
