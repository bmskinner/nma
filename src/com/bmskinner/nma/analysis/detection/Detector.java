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
/*
  -----------------------
  DETECTOR
  -----------------------
  Contains the variables for detecting
  nuclei and signals
 */
package com.bmskinner.nma.analysis.detection;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.visualisation.image.ImageFilterer;

import ij.ImagePlus;
import ij.Prefs;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.process.ByteProcessor;
import ij.process.ByteStatistics;
import ij.process.ColorStatistics;
import ij.process.FloatProcessor;
import ij.process.FloatStatistics;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.PolygonFiller;
import ij.process.ShortProcessor;
import ij.process.ShortStatistics;

/**
 * This abstract class is extended for detecting objects in images. This
 * provides methods for setting size, circularity and thresholding criteria. It
 * calls ImageJ's RoiManager to find the ROIs in the image.
 * 
 * @author bms41
 *
 */
public class Detector {

	public static final int MINIMUM_OBJECT_SIZE_PIXELS = 5;

	private static final Logger LOGGER = Logger.getLogger(Detector.class.getName());

	public static final int CLOSED_OBJECTS = 0; // Flags to allow detection of
	// open or closed objects
	public static final int OPEN_OBJECTS = 1;

	public static final String COM_X = "XM";
	public static final String COM_Y = "YM";

	public static final String RESULT_TABLE_PERIM = "Perim.";

	private static final String NO_DETECTION_PARAMS_ERR = "Detection parameters not set";
	private static final String SIZE_MISMATCH_ERR = "Minimum size >= maximum size";
	private static final String CIRC_MISMATCH_ERR = "Minimum circularity >= maximum circularity";

	private double minSize;
	private double maxSize;
	private double minCirc;
	private double maxCirc;

	private boolean includeHoles = true;
	private boolean excludeEdges = true;

	private boolean isWatershed = false;

	/**
	 * Detect rois in the image at the default threshold with no size or circularity
	 * parameters set
	 * 
	 * @param ip
	 * @return
	 */
	public Map<Roi, IPoint> getAllRois(ImageProcessor ip) {
		minCirc = 0;
		maxCirc = 1;
		minSize = MINIMUM_OBJECT_SIZE_PIXELS;
		maxSize = ip.getWidth() * ip.getHeight(); // object cannot be larger than the image
		return detectRois(ip);
	}

	/**
	 * Detect ROIs in the image that match the given options
	 * 
	 * @param ip      the image to search. Should be binarised
	 * @param options the detection options
	 * @return
	 */
	public Map<Roi, IPoint> getValidRois(ImageProcessor ip, HashOptions options) {
		if (options.hasDouble(HashOptions.MAX_SIZE_PIXELS))
			maxSize = options.getDouble(HashOptions.MAX_SIZE_PIXELS);
		else
			maxSize = ip.getWidth() * ip.getHeight();
		minSize = options.getInt(HashOptions.MIN_SIZE_PIXELS);
		minCirc = options.getDouble(HashOptions.MIN_CIRC);
		maxCirc = options.getDouble(HashOptions.MAX_CIRC);
		isWatershed = options.getBoolean(HashOptions.IS_USE_WATERSHED);
		return detectRois(ip);
	}

	/**
	 * Detect ROIs in the image that match the given options, and are within the
	 * given nucleus ROI
	 * 
	 * @param ip      the image to search
	 * @param options the detection options
	 * @param n       a nucleus to constrain the search within
	 * @return
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	public Map<Roi, IPoint> getValidRois(ImageProcessor ip, HashOptions options, Nucleus n)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {

		if (options.hasDouble(HashOptions.SIGNAL_MAX_FRACTION)) {
			maxSize = n.getMeasurement(Measurement.AREA)
					* options.getDouble(HashOptions.SIGNAL_MAX_FRACTION);
		} else {

			if (options.hasDouble(HashOptions.MAX_SIZE_PIXELS))
				maxSize = options.getDouble(HashOptions.MAX_SIZE_PIXELS);
			else
				maxSize = ip.getWidth() * ip.getHeight();
		}

		minSize = options.getInt(HashOptions.MIN_SIZE_PIXELS);
		minCirc = options.getDouble(HashOptions.MIN_CIRC);
		maxCirc = options.getDouble(HashOptions.MAX_CIRC);

		isWatershed = options.getBoolean(HashOptions.IS_USE_WATERSHED);

		Map<Roi, IPoint> rois = detectRois(ip);

		Map<Roi, IPoint> result = new HashMap<>();
		for (Entry<Roi, IPoint> entry : rois.entrySet()) {
			if (n.containsOriginalPoint(entry.getValue())) {
				result.put(entry.getKey(), entry.getValue());
			}
		}

		return result;
	}

	/**
	 * Set the minimum and maximum size ROIs to detect
	 * 
	 * @param min the minimum in pixels
	 * @param max the maximum in pixels
	 */
	public void setSize(double min, double max) {
		minSize = min;
		maxSize = max;
	}

	/**
	 * Set the minimum and maximum circularity ROIs to detect
	 * 
	 * @param min the minimum circularity
	 * @param max the maximum circularity
	 */
	public void setCirc(double min, double max) {
		minCirc = min;
		maxCirc = max;
	}

	public void setMinSize(double d) {
		this.minSize = d;
	}

	public void setMaxSize(double d) {
		this.maxSize = d;
	}

	public void setMinCirc(double d) {
		this.minCirc = d;
	}

	public void setMaxCirc(double d) {
		this.maxCirc = d;
	}

	/**
	 * Set whether the ROIs should include holes - i.e. should holes be flood filled
	 * before detection
	 * 
	 * @param b
	 */
	public void setIncludeHoles(boolean b) {
		includeHoles = b;
	}

	/**
	 * Set whether the ROIs should include holes - i.e. should holes be flood filled
	 * before detection
	 * 
	 * @param b
	 */
	public void setExcludeEdges(boolean b) {
		excludeEdges = b;
	}

	/**
	 * Detect and measure ROIs in this image
	 * 
	 * @param image
	 * @return a map of ROIs and their CoMs
	 */
	private Map<Roi, IPoint> detectRois(@NonNull ImageProcessor image) {
		if (Double.isNaN(this.minSize) || Double.isNaN(this.maxSize) || Double.isNaN(this.minCirc)
				|| Double.isNaN(this.maxCirc))
			throw new IllegalArgumentException(NO_DETECTION_PARAMS_ERR);

		if (this.minSize >= this.maxSize)
			throw new IllegalArgumentException(SIZE_MISMATCH_ERR);
		if (this.minCirc >= this.maxCirc)
			throw new IllegalArgumentException(CIRC_MISMATCH_ERR);
		if (!(image instanceof ByteProcessor || image instanceof ShortProcessor))
			throw new IllegalArgumentException("Processor must be byte or short");

		ImageProcessor searchProcessor = image.duplicate();

		// Watershed if needed
		if (isWatershed) {
			searchProcessor = ImageFilterer.watershed(searchProcessor);
		}

		Map<Roi, IPoint> result = new HashMap<>();

		// run the particle analyser
		// By default, add all particles to the ROI manager, and do not count
		// anything touching the edge
		int options = 0;

		if (excludeEdges)
			options = options | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
		if (includeHoles)
			options = options | ParticleAnalyzer.INCLUDE_HOLES;

		// Run particle analysis on the thresholded binary image
		ParticleAnalyzer pa = new ParticleAnalyzer(options);
		boolean success = pa.analyze(searchProcessor);
		if (!success) {
			LOGGER.log(Level.FINE, "Unable to perform particle analysis on object");
		}

		// Run measurements on the original image
		// to ensure CoM detection is accurate
		for (Roi r : pa.getRois())
			result.put(r, measure(r, image));

		return result;
	}

	/**
	 * Get the CoM for the region covered by the given roi. *
	 * 
	 * @param roi   the region to measure
	 * @param image the image to measure in
	 * @return
	 */
	private synchronized IPoint measure(@NonNull Roi roi, @NonNull ImageProcessor image) {

		ImagePlus imp = new ImagePlus(null, image);
		imp.setRoi(roi);
		ResultsTable rt = new ResultsTable();
		Analyzer analyser = new Analyzer(imp,
				Measurements.CENTER_OF_MASS, rt);
		analyser.measure();
		return new FloatPoint(rt.getValue(COM_X, 0), rt.getValue(COM_Y, 0));
	}

	/**
	 * This recapitulates the basic function of the ImageJ particle analyzer without
	 * using the static roi manager. It works better for multithreading.
	 * 
	 * @author bms41
	 * @since 1.13.8
	 *
	 */
	protected class ParticleAnalyzer implements Measurements {

		/** Do not measure particles touching edge of image. */
		private static final int EXCLUDE_EDGE_PARTICLES = 8;

		/** Flood fill to ignore interior holes. */
		private static final int INCLUDE_HOLES = 1024;

		/** Use 4-connected particle tracing. */
		private static final int FOUR_CONNECTED = 8192;

		private static final int BYTE = 0, SHORT = 1, FLOAT = 2, RGB = 3;

		private boolean excludeEdgeParticles, floodFill;

		private double level1, level2;

		private int options;
		private int measurements;

		private double fillColor;
		private int width;

		private Wand wand;
		private int imageType, imageType2;
		private int minX, maxX, minY, maxY;

		private PolygonFiller pf;

		private Rectangle r;

		private FloodFiller ff;

		private int roiType;
		private int wandMode = Wand.LEGACY_MODE;

		private boolean blackBackground = true;

		private final Set<Roi> rois = new HashSet<>();

		/**
		 * Constructs a ParticleAnalyzer.
		 * 
		 * @param options a flag word created by Oring SHOW_RESULTS,
		 *                EXCLUDE_EDGE_PARTICLES, etc.
		 */
		public ParticleAnalyzer(int options) {
			this.measurements = Measurements.FERET;

			if ((options & FOUR_CONNECTED) != 0) {
				wandMode = Wand.FOUR_CONNECTED;
				options |= INCLUDE_HOLES;
			}
			this.options = options;
			Prefs.blackBackground = true;
		}

		/**
		 * Get the detected Rois
		 * 
		 * @return
		 */
		public Set<Roi> getRois() {
			return rois;
		}

		/**
		 * Performs particle analysis on the specified ImagePlus and ImageProcessor.
		 * Returns false if there is an error.
		 */
		public boolean analyze(ImageProcessor ip) {
			rois.clear();

			excludeEdgeParticles = (options & EXCLUDE_EDGE_PARTICLES) != 0;
			floodFill = (options & INCLUDE_HOLES) == 0;

			ip.snapshot();

			if (!setThresholdLevels(ip))
				return false;

			width = ip.getWidth();

			byte[] pixels = null;
			if (ip instanceof ByteProcessor)
				pixels = (byte[]) ip.getPixels();
			if (r == null) {
				r = ip.getRoi();
			}
			minX = r.x;
			maxX = r.x + r.width;
			minY = r.y;
			maxY = r.y + r.height;

			int offset;
			double value;

			wand = new Wand(ip);
			pf = new PolygonFiller();
			if (floodFill) {
				ImageProcessor ipf = ip.duplicate();
				ipf.setValue(fillColor);
				ff = new FloodFiller(ipf);
			}
			roiType = Wand.allPoints() ? Roi.FREEROI : Roi.TRACED_ROI;

			boolean done = false;
			for (int y = r.y; y < (r.y + r.height); y++) {
				offset = y * width;
				for (int x = r.x; x < (r.x + r.width); x++) {
					if (pixels != null)
						value = pixels[offset + x] & 255;
					else if (imageType == SHORT)
						value = ip.getPixel(x, y);
					else
						value = ip.getPixelValue(x, y);
					if (value >= level1 && value <= level2 && !done) {
						analyzeParticle(x, y, ip);
						done = level1 == 0.0 && level2 == 255.0;
					}
				}
			}

			ip.resetRoi();
			ip.reset();

			return true;
		}

		/**
		 * Choose how to threshold the input image based on the image type.
		 * 
		 * @param ip
		 * @return
		 */
		private boolean setThresholdLevels(ImageProcessor ip) {
			double t1 = ip.getMinThreshold();
			double t2 = ip.getMaxThreshold();

			if (ip instanceof ShortProcessor)
				imageType = SHORT;
			else if (ip instanceof FloatProcessor)
				imageType = FLOAT;
			else
				imageType = BYTE;
			if (t1 == ImageProcessor.NO_THRESHOLD) {

				if (imageType != BYTE)
					return false;

				boolean threshold255 = false;
				if (blackBackground)
					threshold255 = !threshold255;
				if (threshold255) {
					level1 = 255;
					level2 = 255;
					fillColor = 64;
				} else {
					level1 = 0;
					level2 = 0;
					fillColor = 192;
				}
			} else {
				level1 = t1;
				level2 = t2;
				if (imageType == BYTE) {
					if (level1 > 0)
						fillColor = 0;
					else if (level2 < 255)
						fillColor = 255;
				} else if (imageType == SHORT) {
					if (level1 > 0)
						fillColor = 0;
					else if (level2 < 65535)
						fillColor = 65535;
				} else if (imageType == FLOAT)
					fillColor = -Float.MAX_VALUE;
				else
					return false;
			}
			imageType2 = imageType;

			return true;
		}

		private void analyzeParticle(int x, int y, ImageProcessor ip) {

			wand.autoOutline(x, y, level1, level2, wandMode);
			if (wand.npoints == 0) {
				return;
			}

			Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, roiType);
			Rectangle r = roi.getBounds();
			if (r.width > 1 && r.height > 1) {
				PolygonRoi proi = (PolygonRoi) roi;
				pf.setPolygon(proi.getXCoordinates(), proi.getYCoordinates(),
						proi.getNCoordinates());
				ip.setMask(pf.getMask(r.width, r.height));
				if (floodFill)
					ff.particleAnalyzerFill(x, y, level1, level2, ip.getMask(), r);
			}
			ip.setRoi(r);
			ip.setValue(fillColor);
			ImageStatistics stats = getStatistics(ip, measurements);
			boolean include = true;

			if (excludeEdgeParticles && (r.x == minX || r.y == minY || r.x + r.width == maxX
					|| r.y + r.height == maxY))
				include = false;

			ImageProcessor mask = ip.getMask();
			if (minCirc > 0.0 || maxCirc < 1.0) {
				double perimeter = roi.getLength();
				double circularity = perimeter == 0.0 ? 0.0
						: 4.0 * Math.PI * (stats.pixelCount / (perimeter * perimeter));
				if (circularity > 1.0)
					circularity = 1.0;

				if (circularity < minCirc || circularity > maxCirc)
					include = false;
			}

			if (stats.pixelCount >= minSize && stats.pixelCount <= maxSize && include) {
				stats.xstart = x;
				stats.ystart = y;
				rois.add((Roi) roi.clone());
			}

			ip.fill(mask);
		}

		private ImageStatistics getStatistics(ImageProcessor ip, int mOptions) {

			Calibration cal = new Calibration();
			switch (imageType2) {
			case BYTE:
				return new ByteStatistics(ip, mOptions, cal);
			case SHORT:
				return new ShortStatistics(ip, mOptions, cal);
			case FLOAT:
				return new FloatStatistics(ip, mOptions, cal);
			case RGB:
				return new ColorStatistics(ip, mOptions, cal);
			default:
				return null;
			}
		}

	}
}
