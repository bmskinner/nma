package com.bmskinner.nma.analysis.image;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.signals.ISignalCollection;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.io.ImageImporter;

import ij.gui.Roi;
import ij.process.ImageProcessor;

public class CellHistogramCalculationMethod extends SingleDatasetAnalysisMethod {

	private final HashOptions options;

	/**
	 * Create with a dataset to measure and an options object
	 * 
	 * @param dataset
	 * @param options
	 */
	public CellHistogramCalculationMethod(@NonNull IAnalysisDataset dataset,
			@NonNull HashOptions options) {
		super(dataset);
		this.options = options;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(dataset);
	}

	/**
	 * Calculate the histogram value across the entire nucleus image
	 * 
	 * @throws Exception
	 */
	private void run() throws Exception {

		// Determine progress bar length
		int totalProgress = 0;

		if (options.getBoolean(CellularComponent.NUCLEUS)) {
			totalProgress += dataset.getCollection().size();
		}
		for (final ISignalGroup sg : dataset.getCollection().getSignalGroups()) {
			if (options.getBoolean(CellularComponent.NUCLEAR_SIGNAL + sg.getId())) {
				totalProgress += dataset.getCollection().size();
			}
		}

		this.fireUpdateProgressTotalLength(totalProgress);

		// Look at nuclei if requested
		if (options.getBoolean(CellularComponent.NUCLEUS)) {

			final int nucleusChannel = dataset.getAnalysisOptions().get().getNucleusDetectionOptions().get()
					.getInt(HashOptions.CHANNEL);

			for (final File f : dataset.getCollection().getImageFiles()) {

				// Read the image for the desired channel
				final ImageProcessor nucleusImage = ImageImporter.importImage(f, nucleusChannel);

				for (final ICell c : dataset.getCollection().getCells(f)) {
					for (final Nucleus n : c.getNuclei()) {
						calculateHistogram(n, nucleusImage, CellularComponent.NUCLEUS);
					}
					fireProgressEvent();
				}
			}
		}

		// Look at signals - these may be in the same images or separate images
		for (final ISignalGroup sg : dataset.getCollection().getSignalGroups()) {

			// Only if requested in options
			if (!options.getBoolean(CellularComponent.NUCLEAR_SIGNAL + sg.getId())) {
				continue;
			}

			final HashOptions sgOptions = dataset.getAnalysisOptions().get().getNuclearSignalOptions(sg.getId()).get();
			final int sgChannel = sgOptions.getInt(HashOptions.CHANNEL);
			final File sgDir = dataset.getAnalysisOptions().get().getNuclearSignalDetectionFolder(sg.getId()).get();

			for (final ICell c : dataset.getCollection().getCells()) {
				for (final Nucleus n : c.getNuclei()) {

					final ISignalCollection sc = n.getSignalCollection();

					// If there is a signal in this nucleus, get the source file directly.
					// Otherwise infer the filename from the signal directory and the nucleus file
					// name
					final ImageProcessor sgImage = sc.hasSignal(sg.getId())
							? ImageImporter.importImage(sc.getSourceFile(sg.getId()),
									sgChannel)
							: ImageImporter.importImage(new File(sgDir, n.getSourceFileName()),
									sgChannel);
					calculateHistogram(n, sgImage, CellularComponent.NUCLEAR_SIGNAL + "_" + sg.getId());

					
				}
				fireProgressEvent();
			}


		}
	}

	/**
	 * Create a histogram for the pixels within the nucleus ROI for the given object
	 * channel
	 * 
	 * @param n          the nucleus to calculate within
	 * @param image      the image to measure
	 * @param objectName the name to call the resulting measurement
	 */
	private void calculateHistogram(Nucleus n, ImageProcessor image, String objectName) {
		final double[] histogram = new double[256];
		final Roi roi = n.toOriginalRoi();
		final int x = (int) n.getMinX();
		final int w = (int) n.getMaxX();
		final int y = (int) n.getMinY();
		final int h = (int) n.getMaxY();

		// Limit to bounding box
		for (int xx = x; xx <= w; xx++) {
			for (int yy = y; yy <= h; yy++) {
				if (roi.contains(xx, yy)) {
					histogram[image.get(xx, yy)]++;
				}
			}
		}
		n.setMeasurement(Measurement.makeImageHistogram(objectName), histogram);
	}
}
