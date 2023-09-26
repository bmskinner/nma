package com.bmskinner.nma.analysis.image;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
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

		int channel = options.get(HashOptions.CHANNEL);

		for (File f : dataset.getCollection().getImageFiles()) {

			// Read the image for the nucleus channel
			ImageProcessor image = ImageImporter.importImage(f, channel);

			for (ICell c : dataset.getCollection().getCells(f)) {
				for (Nucleus n : c.getNuclei()) {

					int[] histogram = new int[256];
					Roi roi = n.toOriginalRoi();
					int x = (int) n.getMinX();
					int w = (int) n.getMaxX();
					int y = (int) n.getMinY();
					int h = (int) n.getMaxY();

					// Limit to bounding box
					for (int xx = x; xx <= w; xx++) {
						for (int yy = y; yy <= h; yy++) {
							if (roi.contains(xx, yy)) {
								histogram[image.get(xx, yy)]++;
							}
						}
					}

					// Add the measurments to the nucleus
					for (int i = 0; i < histogram.length; i++) {
						Measurement m = Measurement.makePixelHistogram(channel, i);
						n.setMeasurement(m, histogram[i]);
					}

				}
				fireProgressEvent();
			}
		}
	}
}
