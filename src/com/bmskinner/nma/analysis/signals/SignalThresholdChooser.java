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

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.BooleanProfile;
import com.bmskinner.nma.components.profiles.DefaultProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;

import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

/**
 * The detector for nuclear signals.
 * 
 * @author bms41
 *
 */
public class SignalThresholdChooser {

	private static final Logger LOGGER = Logger.getLogger(SignalThresholdChooser.class.getName());

	private HashOptions options;
	private int minThreshold;

	/**
	 * Create a detector with the desired options
	 * 
	 * @param options the size and circularity parameters
	 * @param channel the RGB channel
	 */
	public SignalThresholdChooser(@NonNull HashOptions options) {
		if (!options.hasInt(HashOptions.CHANNEL))
			throw new IllegalArgumentException("Channel not present in detection options");
		if (!options.hasInt(HashOptions.THRESHOLD))
			throw new IllegalArgumentException("Threshold not present in detection options");

		this.options = options;
		this.minThreshold = options.getInt(HashOptions.THRESHOLD);

		if (minThreshold < 0)
			throw new IllegalArgumentException("Min threshold must be greater or equal to 0");
	}

	/**
	 * find the appropriate signal detection threshold for this image based on the
	 * analysis options
	 * 
	 * @param sourceFile the file the image came from
	 * @param stack      the imagestack
	 * @param n          the nucleus
	 * @throws ImageImportException
	 * @throws Exception
	 */
	public int chooseThreshold(@NonNull ImageProcessor ip, @NonNull Nucleus n) {
		if (options.getString(HashOptions.SIGNAL_DETECTION_MODE_KEY)
				.equals(SignalDetectionMode.FORWARD.name())) {
			LOGGER.finer("Running forward detection");
			return chooseForwardThresholdSignal(ip, n);
		}

		if (options.getString(HashOptions.SIGNAL_DETECTION_MODE_KEY)
				.equals(SignalDetectionMode.REVERSE.name())) {
			LOGGER.finer("Running reverse detection");
			return chooseReverseThresholdSignal(ip, n);
		}

		if (options.getString(HashOptions.SIGNAL_DETECTION_MODE_KEY)
				.equals(SignalDetectionMode.ADAPTIVE.name())) {
			LOGGER.finer("Running adaptive detection");
			return chooseHistogramThresholdSignal(ip, n);
		}
		throw new IllegalArgumentException("No detection mode found");
	}

	private int chooseForwardThresholdSignal(@NonNull ImageProcessor ip,
			@NonNull Nucleus n) {
		return minThreshold;
	}

	/**
	 * Find the brightest pixels in the nuclear roi. If < maxSignalFraction, get
	 * dimmer pixels and remeasure. Continue until signal size is met. Works best
	 * with maxSignalFraction of ~0.1 for a chromosome paint TODO: assumes there is
	 * only one signal. Check that the detector picks up an object of
	 * MIN_SIGNAL_SIZE before setting the threshold.
	 * 
	 * @param sourceFile the file the image came from
	 * @param n          the nucleus
	 * @throws ImageImportException
	 * @throws Exception
	 */
	private int chooseReverseThresholdSignal(@NonNull ImageProcessor ip,
			@NonNull Nucleus n) {
		FloatPolygon polygon = n.toOriginalPolygon();

		// map brightness to count
		Map<Integer, Integer> counts = new HashMap<>();
		for (int i = 0; i < 256; i++) {
			counts.put(i, 0);
		}

		// get the region bounded by the nuclear roi
		// for max intensity (255) , downwards, count pixels with that intensity
		// if count / area < fraction, continue

		// sort the pixels in the roi to bins
		for (int width = 0; width < ip.getWidth(); width++) {
			for (int height = 0; height < ip.getHeight(); height++) {

				if (polygon.contains(width, height)) {
					int brightness = ip.getPixel(width, height);
					int oldCount = counts.get(brightness);
					counts.put(brightness, oldCount + 1);
				}

			}
		}

		// find the threshold from the bins
		int area = (int) (n.getMeasurement(Measurement.AREA)
				* options.getDouble(HashOptions.SIGNAL_MAX_FRACTION));
		int total = 0;
		int threshold = 0; // the value to threshold at

		for (int brightness = 255; brightness > 0; brightness--) {

			total += counts.get(brightness);

			if (total > area) {
				threshold = brightness + 1;
				break;
			}
		}

		minThreshold = threshold;

		// now we have the reverse threshold value, do the thresholding
		// and find signal rois
		return chooseForwardThresholdSignal(ip, n);
	}

	/**
	 * This method uses the histogram of pixel intensities in the signal channel
	 * within the bounding box of the nucleus. The histogram shows a drop at the
	 * point where background transitions to real signal. We detect this drop, and
	 * set it as the appropriate forward threshold for the nucleus.
	 * 
	 * @throws ImageImportException
	 * 
	 * @throws Exception
	 */
	private int chooseHistogramThresholdSignal(@NonNull final ImageProcessor ip,
			@NonNull final Nucleus n) {

		Rectangle boundingBox = new Rectangle(n.getXBase(),
				n.getYBase(), (int) n.getWidth(), (int) n.getHeight());

		ip.setRoi(boundingBox);
		ImageStatistics statistics = ImageStatistics.getStatistics(ip, Measurements.AREA,
				new Calibration());
		long[] histogram = statistics.getHistogram();

		float[] d = new float[histogram.length];

		for (int i = 0; i < histogram.length; i++) {
			d[i] = histogram[i];

		}

		/*
		 * trim the histogram to the minimum signal intensity. No point looking lower,
		 * and the black pixels increase the total range making it harder to carry out
		 * the range based minima detection below
		 */
		LOGGER.finest("Initial histo threshold: " + minThreshold);

		IProfile histogramProfile = new DefaultProfile(d);
		IProfile trimmedHisto = histogramProfile.getSubregion(minThreshold, 255);

		// smooth the arrays, get the deltas, and double smooth them
		IProfile trimDS = trimmedHisto.smooth(3).calculateDeltas(3).smooth(3).smooth(3);

		/*
		 * find minima and maxima above or below zero, with a total displacement more
		 * than 0.1 of the range of values in the delta profile
		 */
		BooleanProfile minimaD = getLocalMinimaWithRangeThreshold(trimDS, 3, 0, 0.1);

		/*
		 * Set the threshold for this nucleus to the drop-off This is the highest local
		 * minimum detected in the delta profile (if no minima were detected, we use the
		 * original signal threshold).
		 */
		int maxIndex = minThreshold;
		for (int i = 0; i < minimaD.size(); i++) {
			if (minimaD.get(i)) {
				maxIndex = i + minThreshold;
			}
		}

		/*
		 * Add a bit more to the new threshold. This is because the minimum of the delta
		 * profile is in middle of the background drop off; we actually want to ignore
		 * the remainder of this background and just keep the signal. Arbitrary at
		 * present. TODO: Find the best point.
		 */
		maxIndex += 10;

		minThreshold = maxIndex;
		return chooseForwardThresholdSignal(ip, n);
	}

	private BooleanProfile getLocalMinimaWithRangeThreshold(IProfile p, int window,
			double threshold, double range) {

		BooleanProfile minima = p.getLocalMinima(window, threshold);

		boolean[] values = new boolean[p.size()];

		double fractionThreshold = (p.getMax() - p.getMin()) * range;

		for (int i : p) {
			values[i] = minima.get(i)
					&& (p.get(i) > fractionThreshold || p.get(i) < -fractionThreshold);
		}
		return new BooleanProfile(values);
	}
}
