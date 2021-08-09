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
package com.bmskinner.nuclear_morphology.analysis.image;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.detection.CannyEdgeDetector;
import com.bmskinner.nuclear_morphology.analysis.detection.Hough_Circles;
import com.bmskinner.nuclear_morphology.analysis.detection.Kuwahara_Filter;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions;
import com.bmskinner.nuclear_morphology.stats.Stats;

import ij.ImagePlus;
import ij.Prefs;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.strel.DiskStrel;

/**
 * Provides easy access to the filters used for nucleus detection, such as
 * background removal and edge detection.
 * 
 * @author ben
 * @since 1.11.0
 *
 */
public class ImageFilterer extends AbstractImageFilterer {
	
	private static final Logger LOGGER = Logger.getLogger(ImageFilterer.class.getName());
	
	public static final double DEFAULT_SCREEN_FRACTION = 0.8;

    public ImageFilterer(ImageProcessor ip) {
        super(ip);
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
     * @param ip the image to process
     * @param kernelRadius the radius of the kernel
     * @return the processed image
     */
    public static ImageProcessor kuwaharaFilter(ImageProcessor ip, int kernelRadius) {
    	LOGGER.finest("Running Kuwahara filter");
        Kuwahara_Filter kw = new Kuwahara_Filter();
        ImagePlus img = new ImagePlus("", ip);
        kw.setup("", img);
        ImageProcessor result = ip.duplicate();
        kw.filter(result, kernelRadius);
        return result;
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
     * The chromocentre can cause 'skipping' of the edge detection from the edge
     * to the interior of the nucleus. Make any pixel over threshold equal
     * threshold to remove internal structures
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
    public ImageFilterer colorThreshold(int minHue, int maxHue, int minSat, int maxSat, int minBri, int maxBri) {

        ColourThresholder ct = new ColourThresholder();

        ct.setHue(minHue, maxHue);
        ct.setBri(minBri, maxBri);
        ct.setSat(minSat, maxSat);

        ImageProcessor result = ct.threshold(ip);
        ip = result;
        return this;
    }

    /**
     * Bridges unconnected pixels, that is, sets 0-valued pixels to 1 if they
     * have two nonzero neighbors that are not connected. For example:
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
                    // IJ.log( "Bridge "+y+" "+x);
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
     * the screen width. If this would cause the height to become greater than the screen
     * height, the image will be resized such that the height is 80% of the screen height.
     * 
     * @return the filterer, for pipelining
     */
    public ImageFilterer fitToScreen() {
        if (ip == null)
            throw new IllegalArgumentException("Image processor is null");
        return fitToScreen(DEFAULT_SCREEN_FRACTION);
    }

    /**
     * Resize the image to fit on the screen. By default the width will be the given fraction of
     * the screen width. If this would cause the height to become greater than the screen
     * height, the image will be resized such that the height is that fraction of the screen height.
     * 
     * @param fraction the fraction of the screen width to take up (0-1)
     * @return the filterer, for pipelining
     */
    public ImageFilterer fitToScreen(double fraction) {
    	ip = fitToScreen(ip, fraction);
        return this;
    }
    
    /**
     * Resize the image to fit on the screen. By default the width will be the given fraction of
     * the screen width. If this would cause the height to become greater than the screen
     * height, the image will be resized such that the height is that fraction of the screen height.
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
        int newHeight = (int) ((double) newWidth / ratio);

        // Check height is OK. If not, recalculate sizes
        if (newHeight >= screenSize.getHeight()) {
            newHeight = (int) (screenSize.getHeight() * fraction);
            newWidth = (int) ((double) newHeight * ratio);
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
     * @param x the central x point
     * @param y the central y point
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
            // System.out.println("Skip, filled");
            return false;
        }

        /*
         * If there is a vertical or horizontal stripe of black pixels, they
         * should be bridged
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
            // System.out.println("No stripe");
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
     * @param ip the image processor. It must be convertible to a ByteProcessor
     * @param closingRadius the radius of the circle
     * @return a new ByteProcessor containing the closed image
     */
    public ImageFilterer close(int closingRadius) {
    	LOGGER.finest("Running gap closing");
    	LOGGER.finest("RankFilters threads: "+Prefs.getThreads());

    	// using the MorphoLibJ library
        ImageProcessor result = ip.convertToByteProcessor();
        
        Strel strel = DiskStrel.fromRadius(closingRadius);
        LOGGER.finest("Dilating");
        result = strel.dilation(result);

        fill(result);
        LOGGER.finest("Eroding");
        result = strel.erosion(result);
        ip = result;
        LOGGER.finest("Ran gap closing");
        return this;
    }

    /**
     * Dilate by the given amount using a circular stucture element
     * 
     * @param ip the image processor. It must be convertible to a ByteProcessor
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
     * Fill holes in the image. Based on the ImageJ Fill holes command: 
     * Binary fill by Gabriel Landini, G.Landini at bham.ac.uk 21/May/2008
     * 
     * @param ip the image to fill
     */
    private void fill(ImageProcessor ip) {
    	LOGGER.finest("Running fill");

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
        LOGGER.finest("Ran fill");
    }

    /**
     * Perform a Canny edge detection on the given image
     * 
     * @param options the canny options
     * @return this filterer with a new ByteProcessor containing the edge detected image
     */
    public ImageFilterer cannyEdgeDetection(@NonNull ICannyOptions options) {
    	LOGGER.finest("Running Canny edge detection");
        ByteProcessor result = null;

        // // calculation of auto threshold
        if (options.isCannyAutoThreshold()) {
            autoDetectCannyThresholds(options, ip);
        }

        CannyEdgeDetector canny = new CannyEdgeDetector(options);
        canny.setSourceImage(ip.duplicate().getBufferedImage());

        canny.process();
        BufferedImage edges = canny.getEdgesImage();

        // convert to an unsigned byte processor
        BufferedImage converted = new BufferedImage(edges.getWidth(), edges.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        converted.getGraphics().drawImage(edges, 0, 0, null);

        result = new ByteProcessor(converted);

        converted = null;
        ip = result;
        LOGGER.finest("Ran Canny edge detection");
        return this;
    }

    /**
     * Run circle detection using given Hough transform options
     * 
     * @param options the detection options
     * @return the points at the centres of the detected circles
     */
    public List<IPoint> houghCircleDetection(@NonNull IHoughDetectionOptions options) {

        LOGGER.finest("Running hough detection");

        Hough_Circles circ = new Hough_Circles();

        circ.threshold = options.getHoughThreshold();
        circ.maxCircles = options.getNumberOfCircles();
        circ.radiusMin = (int) options.getMinRadius();
        circ.radiusMax = (int) options.getMaxRadius();
        circ.radiusInc = 1;

        // Repeat the nucleus detection parameters

        circ.run(ip);

        List<IPoint> list = new ArrayList<>();
        Point[] points = circ.centerPoint;

        if (points != null) {
            for (Point p : points) {
                if (p != null) {
                    list.add(IPoint.makeNew(p.getX(), p.getY()));
                }
            }
        }
        LOGGER.finest("Ran hough detection");
        return list;
    }

    /**
     * Try to detect the optimal settings for the edge detector based on the
     * median image pixel intensity.
     * 
     * @param optons the canny options
     * @param image the image to analyse
     */
    private void autoDetectCannyThresholds(ICannyOptions options, ImageProcessor image) {
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

        if (options instanceof ICannyOptions) {
            ((ICannyOptions) options).setLowThreshold((float) lower);
            ((ICannyOptions) options).setHighThreshold((float) upper);
        }
    }

    /**
     * Find the median pixel intensity in the image. Used in auto-selection of
     * Canny thresholds.
     * 
     * @param image the image to process
     * @return the median pixel intensity
     */
    private double findMedianIntensity(ImageProcessor image) {
        int max = image.getPixelCount();
        double[] values = new double[max];
        for(int i=0; i<max; i++)
        	values[i]=image.get(i);
        return Stats.quartile(values, Stats.MEDIAN);
    }
    
    /**
     * Given a counterstain image, normalise the current image against it
     * to reveal regions of greater or lesser than expected intensity.
     * @param ip the image to be normalised
     * @param counterstain an image of equal dimensions
     * @return the normalised image of ip/counterstain
     */
    public static ImageProcessor normaliseToCounterStain(@NonNull ImageProcessor ip,
    		@NonNull ImageProcessor counterstain) {
    	if(ip.getWidth()!=counterstain.getWidth() || ip.getHeight()!=counterstain.getHeight()) {
    		throw new IllegalArgumentException("Image dimensions must match: input 1 "+
    				ip.getWidth()+" x " + ip.getHeight()+ "; input 2 "+ counterstain.getWidth()+" x "+
    				counterstain.getHeight());
    	}
    	
    	FloatProcessor result = new FloatProcessor(ip.getWidth(), ip.getHeight());
    	    	
    	float[][] input = ip.getFloatArray();
    	
    	for(int i=0; i<ip.getWidth(); i++) {
    		for(int j=0; j<ip.getHeight(); j++) {
    			
    			// Special case where both images are blank
    			if(ip.get(i, j)==0 && counterstain.get(i, j)==0) {
    				input[i][j] = 1f;
    				continue;
    			}
    			
    			float out = ((float)ip.get(i, j)) / ((float)counterstain.get(i, j));
    			out = Float.isInfinite(out) ? 0 : out;
    			out = Float.isNaN(out) ? 0 : out;
    			input[i][j] = out;
    		}
    	}
    	result.setFloatArray(input);
    	return result;
    }
    
    /**
     * Given a counterstain image, normalise the current image against it
     * to reveal regions of greater or lesser than expected intensity.
     * @param counterstain an image of equal dimensions
     * @return this filterer
     */
    public ImageFilterer normaliseToCounterStain(@NonNull ImageProcessor counterstain) {
    	ip = normaliseToCounterStain(ip, counterstain);
    	return this;
    }
}
