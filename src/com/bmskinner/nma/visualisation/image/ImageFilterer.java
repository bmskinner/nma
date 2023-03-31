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
package com.bmskinner.nma.visualisation.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.image.CannyEdgeDetector;
import com.bmskinner.nma.analysis.image.ColourThresholder;
import com.bmskinner.nma.analysis.image.KuwaharaFilter;
import com.bmskinner.nma.components.ComponentOrienter;
import com.bmskinner.nma.components.Imageable;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.rules.PriorityAxis;
import com.bmskinner.nma.stats.Stats;

import ij.ImagePlus;
import ij.plugin.filter.EDM;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.TypeConverter;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.strel.DiskStrel;

/**
 * Contains methods for manipulating ImageProcessors, and provides conversion
 * between ImageStacks and ImageIcons for use in the UI.
 * 
 * @author ben
 *
 */
public class ImageFilterer {

	protected static final String DIMENSIONS_DO_NOT_MATCH_ERROR = "Dimensions do not match";

	private static final Logger LOGGER = Logger.getLogger(ImageFilterer.class.getName());

	private static final int RGB_WHITE = 16777215;
	private static final int RGB_BLACK = 0;
	protected static final int BYTE_MAX = 255;

	public static final double DEFAULT_SCREEN_FRACTION = 0.8;
	protected ImageProcessor ip = null;

	/**
	 * Construct with an image processor
	 * 
	 * @param ip the image processor
	 */
	public ImageFilterer(final ImageProcessor ip) {
		this.ip = ip;
	}

	/**
	 * Construct with an image processor from an image
	 * 
	 * @param img the image
	 */
	public ImageFilterer(final ImagePlus img) {
		this(img.getProcessor());
	}

	/**
	 * Duplicate the filterer - use the template processor and stack
	 * 
	 * @param f the template filterer
	 */
	public ImageFilterer(final ImageFilterer f) {
		this.ip = f.ip;
	}

	/**
	 * Create an annotator containing this image
	 * 
	 * @return
	 */
	public ImageAnnotator toAnnotator() {
		return new ImageAnnotator(ip);
	}

	/**
	 * Create a converter containing this image
	 * 
	 * @return
	 */
	public ImageConverter toConverter() {
		return new ImageConverter(ip);
	}

	/**
	 * Create a converter containing this image
	 * 
	 * @return
	 */
	public ImageFilterer toFilterer() {
		return new ImageFilterer(ip);
	}

	/**
	 * Get the current image processor
	 * 
	 * @return
	 */
	public ImageProcessor toProcessor() {
		if (ip == null)
			throw new NullPointerException("Filterer does not contain an image processor");
		return ip;
	}

	/**
	 * Test if the image is a 32-bit RGB processor
	 * 
	 * @return
	 */
	public boolean isColorProcessor() {
		return ip instanceof ColorProcessor;
	}

	/**
	 * Test if the image is an 8-bit processor
	 * 
	 * @return
	 */
	public boolean isByteProcessor() {
		return ip instanceof ByteProcessor;
	}

	/**
	 * Test if the image is a 16-bit unsigned processor
	 * 
	 * @return
	 */
	public boolean isShortProcessor() {
		return ip instanceof ShortProcessor;
	}

	/**
	 * Invert the processor
	 * 
	 * @return
	 */
	public ImageFilterer invert() {
		ip.invert();
		return this;
	}

	/**
	 * Convert the processor into a ByteProcessor. Has no effect if the processor is
	 * already a ByteProcessor
	 * 
	 * @return
	 */
	public ImageFilterer convertToByteProcessor() {
		if (!isByteProcessor()) {
			TypeConverter tc = new TypeConverter(ip, false);
			ip = tc.convertToByte();
		}
		return this;
	}

	/**
	 * Convert the processor into a ShortProcessor (16-bit unsigned). Has no effect
	 * if the processor is already a ShortProcessor
	 * 
	 * @return
	 */
	public ImageFilterer convertToShortProcessor() {
		if (!isShortProcessor()) {
			TypeConverter tc = new TypeConverter(ip, false);
			ip = tc.convertToShort();
		}
		return this;
	}

	/**
	 * Convert the processor into a ColorProcessor. Has no effect if the processor
	 * is already a ColorProcessor
	 * 
	 * @return
	 */
	public ImageFilterer convertToColorProcessor() {
		if (!isColorProcessor()) {
			TypeConverter tc = new TypeConverter(ip, false);
			ip = tc.convertToRGB();
		}
		return this;
	}

	/**
	 * Create an image icon from the current processor
	 * 
	 * @return
	 */
	public ImageIcon toImageIcon() {
		if (ip == null)
			throw new NullPointerException("Filterer does not contain an image processor");
		return new ImageIcon(ip.getBufferedImage());
	}

	/**
	 * Get the current image as a buffered image. If the filterer contains a stack,
	 * returns the first element of the stack
	 * 
	 * @return
	 */
	public BufferedImage toBufferedImage() {
		if (ip == null) // && st==null
			throw new NullPointerException("Filterer does not contain an image processor");
		return ip.getBufferedImage();
	}

	/**
	 * Create a white RGB colour processor
	 * 
	 * @param w the width
	 * @param h the height
	 * @return
	 */
	public static ImageProcessor createWhiteColorProcessor(int w, int h) {
		ImageProcessor ip = new ColorProcessor(w, h);
		for (int i = 0; i < ip.getPixelCount(); i++)
			ip.set(i, RGB_WHITE); // set all to white initially
		return ip;
	}

	/**
	 * Create an empty white byte processor
	 * 
	 * @param w the width
	 * @param h the height
	 * @return
	 */
	public static ImageProcessor createWhiteByteProcessor(int w, int h) {
		ImageProcessor ip = new ByteProcessor(w, h);
		for (int i = 0; i < ip.getPixelCount(); i++) {
			ip.set(i, 255); // set all to white initially
		}
		return ip;
	}

	/**
	 * Create a white RGB colour processor
	 * 
	 * @param w the width
	 * @param h the height
	 * @return
	 */
	public static ImageProcessor createBlackColorProcessor(int w, int h) {
		ImageProcessor ip = new ColorProcessor(w, h);
		for (int i = 0; i < ip.getPixelCount(); i++)
			ip.set(i, RGB_BLACK); // set all to white initially
		return ip;
	}

	/**
	 * Create an empty black byte processor
	 * 
	 * @param w the width
	 * @param h the height
	 * @return
	 */
	public static ImageProcessor createBlackByteProcessor(int w, int h) {
		ImageProcessor ip = new ByteProcessor(w, h);
		for (int i = 0; i < ip.getPixelCount(); i++)
			ip.set(i, 0);
		return ip;
	}

	/**
	 * Expand each image canvas as needed so all images have the same dimensions.
	 * The original image is centred in the new canvas
	 * 
	 * @param images
	 * @return
	 */
	public static List<ImageProcessor> fitToCommonCanvas(List<ImageProcessor> images) {
		int maxWidth = 0;
		int maxHeight = 0;
		for (ImageProcessor raw : images) {
			maxWidth = Math.max(maxWidth, raw.getWidth());
			maxHeight = Math.max(maxHeight, raw.getHeight());
		}

		List<ImageProcessor> result = new ArrayList<>();
		for (ImageProcessor raw : images) {
			// Beware of single pixel offsets
			int wDiff = maxWidth - raw.getWidth();
			int lbuffer = wDiff % 2 == 0 ? wDiff / 2 : wDiff / 2 + 1;
			int rbuffer = wDiff / 2;

			int hDiff = maxHeight - raw.getHeight();
			int tbuffer = hDiff % 2 == 0 ? hDiff / 2 : hDiff / 2 + 1;
			int bbuffer = hDiff / 2;

			result.add(ImageConverter.expandCanvas(raw, lbuffer,
					rbuffer, tbuffer, bbuffer, Color.BLACK));
		}
		return result;
	}

	/**
	 * Recolour the given 8-bit image to use the given colour, weighting the
	 * greyscale values by the HSB saturation level
	 * 
	 * @param ip     the image
	 * @param colour the maximum intensity colour for the image
	 * @return a colour processor with the recoloured values
	 */
	public static ImageProcessor recolorImage(ImageProcessor ip, Color colour) {

		/*
		 * The intensity of the signal is given by the 8-bit grey value. Intense = 0
		 * (black) and no signal = 255 (white)
		 * 
		 * Translate the desired colour to HSB values.
		 * 
		 * Keep the hue, and make a scale for s and b towards white
		 */

		float[] hsb = Color.RGBtoHSB(colour.getRed(), colour.getGreen(), colour.getBlue(), null);

		// Scale the brightness from 0-bri across the image
		ColorProcessor cp = new ColorProcessor(ip.getWidth(), ip.getHeight());

		for (int i = 0; i < ip.getPixelCount(); i++) {

			float h = hsb[0];
			float s = hsb[1];
//            float b = hsb[2];

			int pixel = ip.get(i);

			if (pixel == 255) { // skip fully white pixels
				int full = RGB_WHITE;
				cp.set(i, full);

			} else {

				// Calculate the fractional intensity of the pixel
				// Since we are scaling from 255-0, this is 1- the actual
				// fraction

				float invF = (pixel) / 255f;
				float f = 1f - invF;

				// Set the saturation to the fractional intensity of the
				// selected colour
				// A maximum of s and a minimum of 0

				s *= f;

				// Make the full pixel

				int full = Color.HSBtoRGB(h, s, 1);
				cp.set(i, full);

			}

		}

		return cp;
	}

	/**
	 * Blend two images together using the Blend images plugin method from Michael
	 * Schmid https://imagej.nih.gov/ij/plugins/blend-images.html
	 * 
	 * @param ip1     the first image
	 * @param weight1 how much image 1 should contribute (does not need to sum to 1
	 *                with weight2)
	 * @param ip2     the second image
	 * @param weight2 how much image 2 should contribute (does not need to sum to 1
	 *                with weight1)
	 * @return
	 */
	public static ImageProcessor blendImages(ImageProcessor ip1, float weight1, ImageProcessor ip2,
			float weight2) {
		ip1.invert();
		ip2.invert();
		ImageProcessor result = ip1.duplicate();
		FloatProcessor fp1 = null;
		FloatProcessor fp2 = null; // non-float images will be converted to these
		for (int i = 0; i < ip1.getNChannels(); i++) { // grayscale: once. RBG: once per color,
														// i.e., 3 times
			fp1 = ip1.toFloat(i, fp1); // convert image or color channel to float (unless float
										// already)
			fp2 = ip2.toFloat(i, fp2);
			blendFloat(fp1, weight1, fp2, weight2);
			result.setPixels(i, fp1); // convert back from float (unless ip is a FloatProcessor)
		}
		result.invert();
		return result;
	}

	/**
	 * Blend a FloatProcessor (i.e., a 32-bit image) with another one, i.e. set the
	 * pixel values of fp1 according to a weighted sum of the corresponding pixels
	 * of fp1 and fp2. This is done for pixels in the rectangle fp1.getRoi() only.
	 * Note that both FloatProcessors, fp1 and fp2 must have the same width and
	 * height.
	 * 
	 * From Michael Schmid's Blend images plugin:
	 * https://imagej.nih.gov/ij/plugins/blend-images.html
	 * 
	 * @param fp1     The FloatProcessor that will be modified.
	 * @param weight1 The weight of the pixels of fp1 in the sum.
	 * @param fp2     The FloatProcessor that will be read only.
	 * @param weight2 The weight of the pixels of fp2 in the sum.
	 */
	private static void blendFloat(FloatProcessor fp1, float weight1, FloatProcessor fp2,
			float weight2) {
		int width = fp1.getWidth();
		float[] pixels1 = (float[]) fp1.getPixels(); // array of the pixels of fp1
		float[] pixels2 = (float[]) fp2.getPixels();
		for (int y = 0; y < fp1.getHeight(); y++) // loop over all pixels inside the roi rectangle
			for (int x = 0; x < fp1.getWidth(); x++) {
				int i = x + y * width; // this is how the pixels are addressed
				pixels1[i] = weight1 * pixels1[i] + weight2 * pixels2[i]; // the weighted sum
			}
	}

	/**
	 * Crop the image to the region covered by the given component
	 * 
	 * @return
	 */
	public ImageFilterer crop(@NonNull ICell c) {
		ip = crop(ip, c);
		return this;
	}

	/**
	 * Crop the image to the region covered by the given cell.
	 * 
	 * @return
	 */
	public static ImageProcessor crop(ImageProcessor ip, ICell c) {
		if (ip == null)
			throw new IllegalArgumentException("Image processor is null");

		if (c.hasCytoplasm()) {
			return crop(ip, c.getCytoplasm());
		}
		return crop(ip, c.getPrimaryNucleus());
	}

	/**
	 * Crop the image to the region covered by the given component
	 * 
	 * @return
	 */
	public ImageFilterer crop(@NonNull CellularComponent c) {
		ip = crop(ip, c);
		return this;
	}

	/**
	 * Crop the image to the region covered by the given component
	 * 
	 * @param ip the image to crop
	 * @param c  the component to crop boundaries from
	 * @return
	 */
	public static ImageProcessor crop(ImageProcessor ip, CellularComponent c) {
		if (ip == null)
			throw new IllegalArgumentException("Image processor is null");
		// Choose a clip for the image (an enlargement of the original nucleus ROI
		int wideW = (int) c.getWidth() + Imageable.COMPONENT_BUFFER * 2;
		int wideH = (int) c.getHeight() + Imageable.COMPONENT_BUFFER * 2;
		int wideX = c.getXBase() - Imageable.COMPONENT_BUFFER;
		int wideY = c.getYBase() - Imageable.COMPONENT_BUFFER;

		wideX = wideX < 0 ? 0 : wideX;
		wideY = wideY < 0 ? 0 : wideY;

		ip.setRoi(wideX, wideY, wideW, wideH);
		return ip.crop();
	}

	/**
	 * Resize the given image to the maximum possible within the given constraints
	 * on width and height. Aspect ratio is preserved.
	 * 
	 * @param ip
	 * @param maxWidth
	 * @param maxHeight
	 * @return the resized image
	 */
	public static ImageProcessor resizeKeepingAspect(@NonNull ImageProcessor ip, int maxWidth,
			int maxHeight) {

		int originalWidth = ip.getWidth();
		int originalHeight = ip.getHeight();

		// keep the image aspect ratio
		double ratio = (double) originalWidth / (double) originalHeight;

		double finalWidth = maxHeight * ratio; // fix height
		finalWidth = finalWidth > maxWidth ? maxWidth : finalWidth; // but
																	// constrain
																	// width too

		return ip.duplicate().resize((int) finalWidth);
	}

	/**
	 * Resize the image to fit the given dimensions, preserving aspect ratio.
	 * 
	 * @param maxWidth  the maximum width of the new image
	 * @param maxHeight the maximum height of the new image
	 * @return a new image resized to fit the given dimensions
	 */
	public ImageFilterer resizeKeepingAspect(int maxWidth, int maxHeight) {
		ip = resizeKeepingAspect(ip, maxWidth, maxHeight);
		return this;
	}

	/**
	 * Enlarge the given processor as needed so that it can be rotated by the given
	 * angle without cropping, then rotates
	 * 
	 * @param ip
	 * @param degrees
	 * @return
	 */
	public static ImageProcessor rotateImage(final ImageProcessor ip, double degrees) {

		double rad = Math.toRadians(degrees);

		// Calculate the new width and height of the canvas
		// new width is h sin(a) + w cos(a) and vice versa for height
		double newWidth = Math.abs(Math.sin(rad) * ip.getHeight())
				+ Math.abs(Math.cos(rad) * ip.getWidth());
		double newHeight = Math.abs(Math.sin(rad) * ip.getWidth())
				+ Math.abs(Math.cos(rad) * ip.getHeight());

		int w = (int) Math.ceil(newWidth);
		int h = (int) Math.ceil(newHeight);

		// The new image may be narrower or shorter following rotation.
		// To avoid clipping, ensure the image never gets smaller in either
		// dimension.
		w = w < ip.getWidth() ? ip.getWidth() : w;
		h = h < ip.getHeight() ? ip.getHeight() : h;

		// paste old image to centre of enlarged canvas
		int xBase = (w - ip.getWidth()) >> 1;
		int yBase = (h - ip.getHeight()) >> 1;

		LOGGER.finer(String.format("New image %sx%s from %sx%s : Rot: %s", w, h, ip.getWidth(),
				ip.getHeight(), degrees));
		ColorProcessor newIp = new ColorProcessor(w, h);

		newIp.setColor(Color.WHITE); // fill current space with white
		newIp.fill();

		newIp.setBackgroundValue(16777215); // fill on rotate is RGB int white
		newIp.copyBits(ip, xBase, yBase, Blitter.COPY);
		newIp.rotate(degrees);
		return newIp;
	}

	/**
	 * Orient the given image using the rules in a nucleus.
	 * 
	 * @param ip
	 * @param n
	 * @return
	 */
	public static ImageProcessor orientImage(final ImageProcessor ip, final Nucleus n) {
		try {
			ImageProcessor newIp = ip.duplicate();
			double angle = ComponentOrienter.calcAngleToAlignVertically(n);
			newIp = rotateImage(ip, angle);
			boolean isFlip = ComponentOrienter.isFlipNeeded(n);
			if (isFlip) {
				if (PriorityAxis.Y.equals(n.getPriorityAxis())) {
					newIp.flipHorizontal();
				} else {
					newIp.flipVertical();
				}
			}
			return newIp;
		} catch (MissingLandmarkException | ComponentCreationException e) {
			LOGGER.warning("Unable to rotate image: " + e.getMessage());
		}
		return ip;
	}

	/**
	 * Rescale the image intensity to fill the 0-255 range
	 * 
	 * @param ip the image to adjust
	 * @return a new ColorProcessor with rescaled values
	 */
	public static ImageProcessor rescaleRGBImageIntensity(final ImageProcessor ip, int min,
			int max) {
		if (ip == null)
			throw new IllegalArgumentException("Image cannot be null");
		if (min < 0 || min > 255)
			throw new IllegalArgumentException("Min threshold must be within 0-255");
		if (max < 0 || max > 255)
			throw new IllegalArgumentException("Max threshold must be within 0-255");

		double rMax = min, gMax = min, bMax = min;
		double rMin = max, gMin = max, bMin = max;
		ImageProcessor result = new ColorProcessor(ip.getWidth(), ip.getHeight());

		for (int i = 0; i < ip.getPixelCount(); i++) {
			int pixel = ip.get(i);

			int[] rgb = intToRgb(pixel);

			rMax = Math.max(rgb[0], rMax);
			gMax = Math.max(rgb[1], gMax);
			bMax = Math.max(rgb[2], bMax);

			rMin = Math.min(rgb[0], rMin);
			gMin = Math.min(rgb[1], gMin);
			bMin = Math.min(rgb[2], bMin);
		}

		double rRange = rMax - rMin;
		double gRange = gMax - gMin;
		double bRange = bMax - bMin;

		// Adjust each pixel to the proportion in range min-max
		for (int i = 0; i < ip.getPixelCount(); i++) {
			int pixel = ip.get(i);
			int[] rgb = intToRgb(pixel);

			double rProp = (rgb[0] - rMin) / rRange;
			double gProp = (rgb[1] - gMin) / gRange;
			double bProp = (rgb[2] - bMin) / bRange;

			int rNew = (int) (max * rProp);
			int gNew = (int) (max * gProp);
			int bNew = (int) (max * bProp);

			int newPixel = rgbToInt(rNew, gNew, bNew);
			result.set(i, newPixel);
		}
		return result;

	}

	/**
	 * Rescale the image intensity to fill the 0-255 range
	 * 
	 * @param ip the image to adjust
	 * @return a new ColorProcessor with rescaled values
	 */
	public static ImageProcessor rescaleRGBImageIntensity(final ImageProcessor ip) {
		return rescaleRGBImageIntensity(ip, 0, 255);
	}

	/**
	 * Adjust the intensity of the given image so that the brightest pixel is at 255
	 * and the dimmest pixel is at 0
	 * 
	 * @param ip the image to adjust
	 * @return a new ByteProcessor with rescaled values
	 */
	public static ImageProcessor rescaleImageIntensity(@NonNull final ImageProcessor ip) {

		if (ip instanceof ColorProcessor)
			return rescaleRGBImageIntensity(ip);

		double maxIntensity = 0;
		double minIntensity = 255;
		ImageProcessor result = null;

		if (ip instanceof ByteProcessor) {
			maxIntensity = 0;
			minIntensity = 255;
			result = new ByteProcessor(ip.getWidth(), ip.getHeight());
		}

		if (ip instanceof FloatProcessor) {
			maxIntensity = 0;
			minIntensity = Float.MAX_VALUE;
			result = new ByteProcessor(ip.getWidth(), ip.getHeight());
		}

		if (ip instanceof ShortProcessor) {
			maxIntensity = 0;
			minIntensity = Short.MAX_VALUE;
			result = new ByteProcessor(ip.getWidth(), ip.getHeight());
		}

		if (result == null) {
			throw new IllegalArgumentException(
					"Unsupported image type: " + ip.getClass().getSimpleName());
		}

		// Find the range in the image

		for (int i = 0; i < ip.getPixelCount(); i++) {
			int pixel = ip.get(i);
			maxIntensity = pixel > maxIntensity ? pixel : maxIntensity;
			minIntensity = pixel < minIntensity ? pixel : minIntensity;
		}

		if (maxIntensity == 0) {
			return ip;
		}

		double range = maxIntensity - minIntensity < 0.01 ? 0d : maxIntensity - minIntensity;
		LOGGER.finer("Max intensity: " + maxIntensity);
		LOGGER.finer("Min intensity: " + minIntensity);
		LOGGER.finer("Rescaling image across image range " + range);

		// Adjust each pixel to the proportion in range 0-255
		for (int i = 0; i < ip.getPixelCount(); i++) {
			int pixel = ip.get(i);

			if (range == 0) {
				result.set(i, 128);
			} else {
				double proportion = (pixel - minIntensity) / range;
				int newPixel = (int) (255 * proportion);
				result.set(i, newPixel);
			}
		}
		return result;
	}

	/**
	 * Create a new 8-bit image processor with the average of all the non-null 8-bit
	 * images in the given list
	 * 
	 * @return
	 */
	public static ImageProcessor averageByteImages(@NonNull List<ImageProcessor> list) {

		if (list == null || list.isEmpty())
			throw new IllegalArgumentException("List null or empty");

		// Check images are same dimensions
		int w = list.get(0).getWidth();
		int h = list.get(0).getHeight();
		int nonNull = 0;

		// check sizes match
		for (ImageProcessor ip : list) {
			if (ip == null)
				continue;
			if (w != ip.getWidth() || h != ip.getHeight())
				throw new IllegalArgumentException(DIMENSIONS_DO_NOT_MATCH_ERROR);
			nonNull++;
		}
		// Create an empty white processor of the correct dimensions
		ImageProcessor mergeProcessor = ImageFilterer.createBlackByteProcessor(w, h);

		if (nonNull == 0)
			return mergeProcessor;

		// Average the pixels
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {

				int pixelTotal = 0;
				for (ImageProcessor ip : list) {
					if (ip == null) {
						continue;
					}
					pixelTotal += ip.get(x, y);
				}

				pixelTotal /= nonNull; // scale back down to 0-255

				// Ignore anything that is not signal -
				// the background is already black
				if (pixelTotal > 0)
					mergeProcessor.set(x, y, pixelTotal);
			}
		}
		return mergeProcessor;
	}

	/**
	 * Create a new 16-bit short processor with the sum of all the non-null 8-bit
	 * images in the given list.
	 * 
	 * @return
	 */
	public static ImageProcessor addByteImages(@NonNull List<ImageProcessor> list) {
		if (list == null || list.isEmpty())
			throw new IllegalArgumentException("List null or empty");

		// Check images are same dimensions
		int w = list.get(0).getWidth();
		int h = list.get(0).getHeight();

		// check sizes match
		for (ImageProcessor ip : list) {
			if (ip == null) {
				return new ShortProcessor(w, h);
			}

			if (w != ip.getWidth() || h != ip.getHeight())
				throw new IllegalArgumentException(DIMENSIONS_DO_NOT_MATCH_ERROR);

		}

		// Average the pixels. Track the highest value to avoid short overflows
		int maxPixelValue = 0;
		int[][] imageTotals = new int[w][h];

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int pixelTotal = 0;
				for (ImageProcessor ip : list) {
					if (ip == null)
						continue;
					pixelTotal += ip.get(x, y);
				}
				maxPixelValue = Math.max(maxPixelValue, pixelTotal);
				imageTotals[x][y] = pixelTotal;
			}
		}

		// Return the scaled processor
		return createScaledShortProcessor(imageTotals, maxPixelValue);
	}

	/**
	 * Given int pixel values, create a 16bit short processor, and scale the values
	 * if needed to avoid overflows
	 * 
	 * @param pixelValues the pixel values for the image
	 * @param maxValue    the maximum pixel value in the image
	 * @return
	 */
	private static ImageProcessor createScaledShortProcessor(int[][] pixelValues, int maxValue) {
		int w = pixelValues.length;
		int h = pixelValues[0].length;

		ImageProcessor result = new ShortProcessor(w, h);

		if (maxValue > Short.MAX_VALUE) {
			LOGGER.log(Level.FINE, "Rescaling pixels with max value {0} to fit short range",
					maxValue);
			for (int x = 0; x < w; x++) {
				for (int y = 0; y < h; y++) {
					pixelValues[x][y] = (int) ((((double) pixelValues[x][y]) / (double) maxValue)
							* Short.MAX_VALUE);
				}
			}
		}

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				result.set(x, y, pixelValues[x][y]);
			}
		}

		return result;
	}

	/**
	 * Calculate a measure of the colocalisation of values in the given images.
	 * ImageA is coloured red, imageB is coloured blue, and regions of
	 * colocalisation will be purple
	 * 
	 * @param imageA the first image
	 * @param imageB the second image
	 * @return an image showing colocalisation of pixels
	 */
	public static ImageProcessor cowarpalise(ImageProcessor imageA, ImageProcessor imageB) {

		if (imageA == null || imageB == null) {
			throw new IllegalArgumentException("Image(s) null");
		}

		// Check images are same dimensions
		if (imageA.getWidth() != imageB.getWidth() || imageA.getHeight() != imageB.getHeight()) {
			throw new IllegalArgumentException(DIMENSIONS_DO_NOT_MATCH_ERROR);
		}

		// Set the saturation scaled by intensity

		ImageProcessor cp = new ColorProcessor(imageA.getWidth(), imageA.getHeight());

		for (int i = 0; i < imageA.getPixelCount(); i++) {
			int r = imageA.get(i);
			int b = imageB.get(i);

			if (r == 255 && b == 255) {
				cp.set(i, rgbToInt(255, 255, 255));
				continue;
			}

			float diff = r - (float) b;
			float scaled = Math.abs(diff) / 255f; // fraction of 8bit space
			float ranged = 0.17f * scaled;

			// Scale to fit in hue range 240-360

			// Needs to be a fractional number that can be multiplied by 360
			// Therefore range is 0.66-1
			float h = diff < 0 ? 0.83f - ranged : 0.83f + ranged;
			// float h = 0.83f + diff; // start at purple, variation of up to
			// 128
			// float h = 300f + diff; // start at purple, variation of up to 128

			float s = diff < 0 ? 1 - (b / 255f) : 1 - (r / 255f);

			float v = 1f;

			int rgb = Color.HSBtoRGB(h, s, v);
			cp.set(i, rgb);

		}

		return cp;

	}

	/**
	 * Combine individual channel data to an int
	 * 
	 * @param r
	 * @param g
	 * @param b
	 * @return
	 */
	protected static int rgbToInt(int r, int g, int b) {
		int rgb = r;
		rgb = (rgb << 8) + g;
		rgb = (rgb << 8) + b;
		return rgb;
	}

	protected static int[] intToRgb(int i) {
		// pixel values for this image
		int[] rgb = new int[3];
		rgb[0] = (i >> 16) & 0xFF;
		rgb[1] = (i >> 8) & 0xFF;
		rgb[2] = i & 0xFF;
		return rgb;
	}

	/**
	 * Express the given pixel intensity as a fraction of 255
	 * 
	 * @param i the pixel value
	 * @return a value from 0-1
	 */
	protected static float getSaturationFromIntensity(int i) {
		return (255f - (255f - i)) / 255f;
	}

	/**
	 * Merge the given list of images by averaging the RGB values
	 * 
	 * @param list a list of RGB images
	 * @return a new colour processor with the averaged values
	 */
	public static ImageProcessor averageRGBImages(List<ImageProcessor> list) {

		if (list == null || list.isEmpty())
			throw new IllegalArgumentException("List null or empty");

		// Check images are same dimensions
		int w = list.get(0).getWidth();
		int h = list.get(0).getHeight();

		for (ImageProcessor ip : list) {

			if (ip == null)
				throw new IllegalArgumentException(
						"Cannot average: a null warp image was encountered");
			if (w != ip.getWidth() || h != ip.getHeight())
				throw new IllegalArgumentException(String.format(
						"Image dimensions %s by %s do not match expected dimensions %s by %s", w, h,
						ip.getWidth(), ip.getHeight()));
		}

		ImageProcessor cp = new ColorProcessor(w, h);

		// Average the colours at each pixel
		for (int i = 0; i < w * h; i++) {

			int r = 0, g = 0, b = 0; // total pixel values

			for (ImageProcessor ip : list) {
				int pixel = ip.get(i);

				// pixel values for this image
				int[] rgb = intToRgb(pixel);

				r += rgb[0];
				g += rgb[1];
				b += rgb[2];
			}

			r /= list.size();
			g /= list.size();
			b /= list.size();

			int rgb = rgbToInt(r, g, b);
			cp.set(i, rgb);

		}

		return cp;
	}

	/**
	 * Apply a binary thresholding to a greyscale processor. Has no effect if the
	 * processor is not greyscale.
	 * 
	 * @param threshold the threshold value.
	 * @return this filterer with the binary thresholded image.
	 * @see ImageProcessor#threshold(int)
	 */
	public ImageFilterer threshold(int threshold) {
		LOGGER.finest("Running thresholding");
		if (ip.isGrayscale())
			ip.threshold(threshold);
		LOGGER.finest("Ran thresholding");
		return this;
	}

	/**
	 * Run a Kuwahara filter to enhance edges in the image
	 * 
	 * @param kernelRadius the radius of the kernel
	 * @return a new ImageFilterer with the processed image
	 */
	public ImageFilterer kuwaharaFilter(int kernelRadius) {
		ip = kuwaharaFilter(ip, kernelRadius);
		return this;
	}

	/**
	 * Run a Kuwahara filter to enhance edges in the image
	 * 
	 * @param ip           the image to process
	 * @param kernelRadius the radius of the kernel
	 * @return the processed image
	 */
	public static ImageProcessor kuwaharaFilter(ImageProcessor ip, int kernelRadius) {
		return KuwaharaFilter.filter(ip, kernelRadius);
	}

	/**
	 * Make any pixel below the threshold equal to zero. Removes background.
	 * 
	 * @param threshold the minimum intensity to allow
	 * @return this filterer
	 */
	public ImageFilterer setBlackLevel(int threshold) {
		ImageProcessor result = ip.duplicate();
		for (int i = 0; i < result.getPixelCount(); i++) {
			if (result.get(i) < threshold)
				result.set(i, 0);
		}
		return new ImageFilterer(result);
	}

	/**
	 * Make any pixel above the threshold equal to the maximum intensity.
	 * 
	 * @param threshold the maximum intensity
	 * @return this filterer
	 */
	public ImageFilterer setWhiteLevel(int threshold) {
		ImageProcessor result = ip.duplicate();
		for (int i = 0; i < result.getPixelCount(); i++) {
			if (result.get(i) > threshold)
				result.set(i, BYTE_MAX);
		}
		return new ImageFilterer(result);
	}

	/**
	 * The chromocentre can cause 'skipping' of the edge detection from the edge to
	 * the interior of the nucleus. Make any pixel over threshold equal threshold to
	 * remove internal structures
	 * 
	 * @param threshold the maximum intensity to allow
	 * @return this filterer
	 */
	public ImageFilterer setMaximumPixelValue(int threshold) {
		LOGGER.finest("Setting max pixel value");
		ImageProcessor result = ip.duplicate();

		for (int i = 0; i < result.getPixelCount(); i++) {

			if (result.get(i) > threshold) {
				result.set(i, threshold);
			}
		}
		ip = result;
		LOGGER.finest("Set max pixel value");
		return this;
	}

	/**
	 * Make any pixel value below the threshold equal to the threshold.
	 * 
	 * @param threshold the minimum pixel value
	 * @return this filterer
	 */
	public ImageFilterer setMinimumPixelValue(int threshold) {

		ImageProcessor result = ip.duplicate();

		for (int i = 0; i < result.getPixelCount(); i++) {
			if (result.get(i) < threshold)
				result.set(i, threshold);
		}
		ip = result;
		return this;
	}

	/**
	 * Threshold based on HSV
	 * 
	 * @return this filterer
	 */
	public ImageFilterer colorThreshold(int minHue, int maxHue, int minSat, int maxSat,
			int minBri,
			int maxBri) {

		ColourThresholder ct = new ColourThresholder();

		ct.setHue(minHue, maxHue);
		ct.setBri(minBri, maxBri);
		ct.setSat(minSat, maxSat);

		ImageProcessor result = ct.threshold(ip);
		ip = result;
		return this;
	}

	/**
	 * Bridges unconnected pixels, that is, sets 0-valued pixels to 1 if they have
	 * two nonzero neighbors that are not connected. For example:
	 * 
	 * 1 0 0 1 1 0 1 0 1 becomes 1 1 1 0 0 1 0 1 1
	 * 
	 * @param bridgeSize the distance to search
	 * @return
	 */
	public ImageFilterer bridgePixelGaps(int bridgeSize) {

		if (bridgeSize % 2 == 0) {
			throw new IllegalArgumentException("Kernel size must be odd");
		}
		ByteProcessor result = ip.convertToByteProcessor();

		int[][] array = result.getIntArray();
		int[][] input = result.getIntArray();

		for (int x = 0; x < ip.getWidth(); x++) {
			for (int y = 0; y < ip.getHeight(); y++) {

				int[][] kernel = getKernel(input, x, y);
				if (bridgePixel(kernel)) {
					array[y][x] = 255;
				}
			}
		}

		result.setIntArray(array);
		ip = result;
		return this;
	}

	/**
	 * Resize the image to fit on the screen. By default the width will be 80% of
	 * the screen width. If this would cause the height to become greater than the
	 * screen height, the image will be resized such that the height is 80% of the
	 * screen height.
	 * 
	 * @return the filterer, for pipelining
	 */
	public ImageFilterer fitToScreen() {
		if (ip == null)
			throw new IllegalArgumentException("Image processor is null");
		return fitToScreen(DEFAULT_SCREEN_FRACTION);
	}

	/**
	 * Resize the image to fit on the screen. By default the width will be the given
	 * fraction of the screen width. If this would cause the height to become
	 * greater than the screen height, the image will be resized such that the
	 * height is that fraction of the screen height.
	 * 
	 * @param fraction the fraction of the screen width to take up (0-1)
	 * @return the filterer, for pipelining
	 */
	public ImageFilterer fitToScreen(double fraction) {
		ip = fitToScreen(ip, fraction);
		return this;
	}

	/**
	 * Resize the image to fit on the screen. By default the width will be the given
	 * fraction of the screen width. If this would cause the height to become
	 * greater than the screen height, the image will be resized such that the
	 * height is that fraction of the screen height.
	 * 
	 * @param fraction the fraction of the screen width to take up (0-1)
	 * @return the resized image, preserving aspect ratio
	 */
	public static ImageProcessor fitToScreen(ImageProcessor ip, double fraction) {
		if (ip == null) {
			throw new IllegalArgumentException("Image processor is null");
		}

		int originalWidth = ip.getWidth();
		int originalHeight = ip.getHeight();

		// keep the image aspect ratio
		double ratio = (double) originalWidth / (double) originalHeight;

		Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

		// set the new width
		int newWidth = (int) (screenSize.getWidth() * fraction);
		int newHeight = (int) (newWidth / ratio);

		// Check height is OK. If not, recalculate sizes
		if (newHeight >= screenSize.getHeight()) {
			newHeight = (int) (screenSize.getHeight() * fraction);
			newWidth = (int) (newHeight * ratio);
		}

		// Create the image
		return ip.duplicate().resize(newWidth, newHeight);
	}

	/**
	 * Resize the image by the given fraction, preserving aspect ratio
	 * 
	 * @param fraction the amount to rescale
	 * @return
	 */
	public ImageFilterer resize(double fraction) {

		if (ip == null)
			throw new IllegalArgumentException("Image processor is null");

		int originalWidth = ip.getWidth();

		double finalWidth = originalWidth * fraction; // fix height

		ImageProcessor result = ip.duplicate().resize((int) finalWidth);
		ip = result;
		return this;
	}

	/**
	 * Fetch a 3x3 image kernel from within an int image array
	 * 
	 * @param array the input image
	 * @param x     the central x point
	 * @param y     the central y point
	 * @return
	 */
	private int[][] getKernel(int[][] array, int x, int y) {

		/*
		 * Create the kernel array, and zero it
		 */
		int[][] result = new int[3][3];
		for (int w = 0; w < 3; w++) {

			for (int h = 0; h < 3; h++) {

				result[h][w] = 0;
			}
		}

		/*
		 * Fetch the pixel data
		 */

		for (int w = x - 1, xR = 0; w <= x + 1; w++, xR++) {
			if (w < 0 || w >= array.length) {
				continue; // ignore x values out of range
			}

			for (int h = y - 1, yR = 0; h <= y + 1; h++, yR++) {
				if (h < 0 || h >= array.length) {
					continue; // ignore y values out of range
				}

				result[yR][xR] = array[h][w];
			}

		}
		return result;
	}

	/**
	 * Should a pixel kernel be bridged? If two or more pixels in the array are
	 * filled, and not connected, return true
	 * 
	 * @param array the 3x3 array of pixels
	 * @return
	 */
	private boolean bridgePixel(int[][] array) {

		/*
		 * If the central pixel is filled, do nothing.
		 */
		if (array[1][1] == 255) {
			return false;
		}

		/*
		 * If there is a vertical or horizontal stripe of black pixels, they should be
		 * bridged
		 */

		int vStripe = 0;
		int hStripe = 0;
		for (int v = 0; v < 3; v++) {
			if (array[1][v] == 0) {
				vStripe++;
			}
			if (array[v][1] == 0) {
				hStripe++;
			}
		}

		if (vStripe < 3 && hStripe < 3) {
			return false;
		}

		/*
		 * Are two white pixels present?
		 */

		int count = 0;
		for (int x = 0; x < array.length; x++) {
			for (int y = 0; y < array.length; y++) {
				if (array[y][x] == 255) {
					count++;
				}

			}
			if (count >= 2) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Close holes in the nuclear borders using a circular structure element
	 * 
	 * @param ip            the image processor. It must be convertible to a
	 *                      ByteProcessor
	 * @param closingRadius the radius of the circle
	 * @return a new ByteProcessor containing the closed image
	 */
	public ImageFilterer close(int closingRadius) {
		ip = close(ip, closingRadius);
		return this;
	}

	/**
	 * Close holes in the given image using a circular structure element
	 * 
	 * @param ip            the image processor. It must be convertible to a
	 *                      ByteProcessor
	 * @param closingRadius the radius for enlargement
	 * @return a new ByteProcessor containing the closed image
	 */
	public static ImageProcessor close(ImageProcessor ip, int closingRadius) {
		// using the MorphoLibJ library
		ImageProcessor result = ip.convertToByteProcessor();

		Strel strel = DiskStrel.fromRadius(closingRadius);
		result = strel.dilation(result);

		fill(result);
		result = strel.erosion(result);
		return result;
	}

	/**
	 * Dilate by the given amount using a circular stucture element
	 * 
	 * @param ip     the image processor. It must be convertible to a ByteProcessor
	 * @param amount the radius of the circle
	 * @return this filterer with a new ByteProcessor containing the closed image
	 */
	public ImageFilterer dilate(int amount) {
		LOGGER.finest("Running dilation");
		// using the MorphoLibJ library
		ImageProcessor result = ip.convertToByteProcessor();

		Strel strel = DiskStrel.fromRadius(amount);

		result = Morphology.dilation(result, strel);
		ip = result;
		LOGGER.finest("Ran dilation");
		return this;
	}

	/**
	 * Fill holes in the image. Based on the ImageJ Fill holes command: Binary fill
	 * by Gabriel Landini, G.Landini at bham.ac.uk 21/May/2008
	 * 
	 * @param ip the image to fill
	 */
	private static void fill(ImageProcessor ip) {
		int foreground = 255;
		int background = 0;

		int width = ip.getWidth();
		int height = ip.getHeight();
		FloodFiller ff = new FloodFiller(ip);
		ip.setColor(127);
		for (int y = 0; y < height; y++) {
			if (ip.getPixel(0, y) == background)
				ff.fill(0, y);
			if (ip.getPixel(width - 1, y) == background)
				ff.fill(width - 1, y);
		}
		for (int x = 0; x < width; x++) {
			if (ip.getPixel(x, 0) == background)
				ff.fill(x, 0);
			if (ip.getPixel(x, height - 1) == background)
				ff.fill(x, height - 1);
		}
		byte[] pixels = (byte[]) ip.getPixels();
		int n = width * height;
		for (int i = 0; i < n; i++) {
			if (pixels[i] == 127)
				pixels[i] = (byte) background;
			else
				pixels[i] = (byte) foreground;
		}
	}

	/**
	 * Perform a Canny edge detection on the given image
	 * 
	 * @param options the canny options
	 * @return this filterer with a new ByteProcessor containing the edge detected
	 *         image
	 */
	public ImageFilterer cannyEdgeDetection(@NonNull HashOptions options) {
		LOGGER.finest("Running Canny edge detection");
		ByteProcessor result = null;

		// // calculation of auto threshold
		if (options.getBoolean(HashOptions.CANNY_IS_AUTO_THRESHOLD)) {
			autoDetectCannyThresholds(options, ip);
		}

		CannyEdgeDetector canny = new CannyEdgeDetector(options);
		canny.setSourceImage(ip.duplicate().getBufferedImage());

		canny.process();
		BufferedImage edges = canny.getEdgesImage();

		// convert to an unsigned byte processor
		BufferedImage converted = new BufferedImage(edges.getWidth(), edges.getHeight(),
				BufferedImage.TYPE_BYTE_GRAY);
		converted.getGraphics().drawImage(edges, 0, 0, null);

		result = new ByteProcessor(converted);
		ip = result;
		LOGGER.finest("Ran Canny edge detection");
		return this;
	}

	/**
	 * Try to detect the optimal settings for the edge detector based on the median
	 * image pixel intensity.
	 * 
	 * @param optons the canny options
	 * @param image  the image to analyse
	 */
	private void autoDetectCannyThresholds(HashOptions options, ImageProcessor image) {
		// calculation of auto threshold

		// find the median intensity of the image
		double medianPixel = findMedianIntensity(image);

		// if the median is >128, this is probably an inverted image.
		// invert it so the thresholds will work
		if (medianPixel > 128) {
			image.invert();
			medianPixel = findMedianIntensity(image);
		}

		// set the thresholds either side of the median
		double sigma = 0.33; // default value - TODO: enable change
		double lower = Math.max(0, (1.0 - (2.5 * sigma)) * medianPixel);
		lower = lower < 0.1 ? 0.1 : lower; // hard limit
		double upper = Math.min(255, (1.0 + (0.6 * sigma)) * medianPixel);
		upper = upper < 0.3 ? 0.3 : upper; // hard limit

		options.setFloat(HashOptions.CANNY_LOW_THRESHOLD_FLT, (float) lower);
		options.setFloat(HashOptions.CANNY_HIGH_THRESHOLD_FLT, (float) upper);
	}

	/**
	 * Find the median pixel intensity in the image. Used in auto-selection of Canny
	 * thresholds.
	 * 
	 * @param image the image to process
	 * @return the median pixel intensity
	 */
	private double findMedianIntensity(ImageProcessor image) {
		int max = image.getPixelCount();
		double[] values = new double[max];
		for (int i = 0; i < max; i++)
			values[i] = image.get(i);
		return Stats.quartile(values, Stats.MEDIAN);
	}

	/**
	 * Given a counterstain image, normalise the current image against it to reveal
	 * regions of greater or lesser than expected intensity.
	 * 
	 * @param ip           the image to be normalised
	 * @param counterstain an image of equal dimensions
	 * @return the normalised image of ip/counterstain
	 */
	public static ImageProcessor normaliseToCounterStain(@NonNull ImageProcessor ip,
			@NonNull ImageProcessor counterstain) {
		if (ip.getWidth() != counterstain.getWidth()
				|| ip.getHeight() != counterstain.getHeight()) {
			throw new IllegalArgumentException("Image dimensions must match: input 1 " +
					ip.getWidth() + " x " + ip.getHeight() + "; input 2 " + counterstain.getWidth()
					+ " x " +
					counterstain.getHeight());
		}

		FloatProcessor result = new FloatProcessor(ip.getWidth(), ip.getHeight());

		float[][] input = ip.getFloatArray();

		for (int i = 0; i < ip.getWidth(); i++) {
			for (int j = 0; j < ip.getHeight(); j++) {

				// divide by zero is bad; ensure if counterstain is zero
				// we zero the result
				float cs = counterstain.get(i, j);
				float im = ip.get(i, j);
				float out = cs == 0f ? 0 : im / cs;
				input[i][j] = out;
			}
		}
		result.setFloatArray(input);
		return result;
	}

	/**
	 * Given a counterstain image, normalise the current image against it to reveal
	 * regions of greater or lesser than expected intensity.
	 * 
	 * @param counterstain an image of equal dimensions
	 * @return this filterer
	 */
	public ImageFilterer normaliseToCounterStain(@NonNull ImageProcessor counterstain) {
		ip = normaliseToCounterStain(ip, counterstain);
		return this;
	}

	/**
	 * Perform watershed segmentation
	 * 
	 * @param source the input binary image
	 * @return the watershed image
	 */
	public static ImageProcessor watershed(@NonNull ImageProcessor source) {
		ImageProcessor result = source.duplicate();
		new EDM().toWatershed(result);
		return result;
	}

	/**
	 * Perform watershed segmentation
	 * 
	 * @return the filterer
	 */
	public ImageFilterer watershed() {
		ip = watershed(ip);
		return this;
	}

}
