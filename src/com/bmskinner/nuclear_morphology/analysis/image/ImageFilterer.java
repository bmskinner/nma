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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.detection.CannyEdgeDetector;
import com.bmskinner.nuclear_morphology.analysis.detection.Hough_Circles;
import com.bmskinner.nuclear_morphology.analysis.detection.Kuwahara_Filter;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions;
import com.bmskinner.nuclear_morphology.stats.Stats;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import mmorpho.MorphoProcessor;
import mmorpho.StructureElement;

/**
 * Provides easy access to the filters used for nucleus detection, such as
 * background removal and edge detection.
 * 
 * @author ben
 *
 */
public class ImageFilterer extends AbstractImageFilterer {

    public ImageFilterer(ImageProcessor ip) {
        super(ip);
    }

    public ImageFilterer(ImageStack st) {
        super(st);
    }

    /**
     * Apply a thresholding on a greyscale processor. Has no effect if the
     * processor is not greyscale
     * 
     * @param threshold
     * @return
     */
    public ImageFilterer threshold(int threshold) {
        if (ip.isGrayscale()) {
            ip.threshold(threshold);
        }
        return this;
    }

    /**
     * Run a Kuwahara filter to enhance edges in the image
     * 
     * @param stack
     *            the image
     * @param filterSize
     *            the radius of the kernel
     */
    public ImageFilterer runKuwaharaFiltering(int stackNumber, int filterSize) {

        ip = st.getProcessor(stackNumber).duplicate();

        return runKuwaharaFiltering(filterSize);
    }

    /**
     * Run a Kuwahara filter to enhance edges in the image
     * 
     * @param filterSize
     *            the radius of the kernel
     * @return a new ImageFilterer with the processed image
     */
    public ImageFilterer runKuwaharaFiltering(int filterSize) {

        Kuwahara_Filter kw = new Kuwahara_Filter();
        ImagePlus img = new ImagePlus("", ip);
        kw.setup("", img);

        ImageProcessor result = ip.duplicate();

        kw.filter(result, filterSize);
        ip = result;
        return this;
    }

    /**
     * The chromocentre can cause 'skipping' of the edge detection from the edge
     * to the interior of the nucleus. Make any pixel over threshold equal
     * threshold to remove internal structures
     * 
     * @param stack the stack to adjust
     * @param stackNumber the plane in the stack (starts at 1)
     * @param threshold the maximum intensity to allow
     * @return a copy of the image processor, with flattening applied
     */
    public ImageFilterer setMaximumPixelValue(int stackNumber, int threshold) {

        // fetch a copy of the int array
        ip = st.getProcessor(stackNumber);
        ImageProcessor result = setMaximumPixelValue(threshold).ip;
        ip = result;
        return this;
    }

    /**
     * The chromocentre can cause 'skipping' of the edge detection from the edge
     * to the interior of the nucleus. Make any pixel over threshold equal
     * threshold to remove internal structures
     * 
     * @param ip the image processor to flatten
     * @param threshold the maximum intensity to allow
     * @return a copy of the image processor, with flattening applied
     */
    public ImageFilterer setMaximumPixelValue(int threshold) {

        ImageProcessor result = ip.duplicate();

        for (int i = 0; i < result.getPixelCount(); i++) {

            if (result.get(i) > threshold) {
                result.set(i, threshold);
            }
        }
        ip = result;
        return this;
        // return new ImageFilterer(result);
    }

    /**
     * Make any pixel below the threshold equal the threshold.
     * 
     * @param ip
     *            the image processor to raise
     * @param threshold
     *            the maximum intensity to allow
     * @return a copy of the image processor, with raising applied
     */
    public ImageFilterer raise(int threshold) {

        ImageProcessor result = ip.duplicate();

        for (int i = 0; i < result.getPixelCount(); i++) {

            if (result.get(i) < threshold) {
                result.set(i, threshold);
            }
        }
        ip = result;
        return this;
        // return new ImageFilterer(result);
    }

    // /**
    // * Invert the processor
    // * @return
    // */
    // public ImageFilterer invert(){
    // ip.invert();
    // return this;
    // }

    /**
     * Threshold based on HSV
     * 
     * @return a copy of the image processor, with thresholding applied
     */
    public ImageFilterer colorThreshold(int minHue, int maxHue, int minSat, int maxSat, int minBri, int maxBri) {

        ColourThresholder ct = new ColourThresholder();

        ct.setHue(minHue, maxHue);
        ct.setBri(minBri, maxBri);
        ct.setSat(minSat, maxSat);

        ImageProcessor result = ct.threshold(ip);
        ip = result;
        return this;
        // return new ImageFilterer(result);
    }

    /**
     * Bridges unconnected pixels, that is, sets 0-valued pixels to 1 if they
     * have two nonzero neighbors that are not connected. For example:
     * 
     * 1 0 0 1 1 0 1 0 1 becomes 1 1 1 0 0 1 0 1 1
     * 
     * @param ip
     *            the image processor
     * @param bridgeSize
     *            the distance to search
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
        // return new ImageFilterer(result);
    }

    /**
     * Resize the image to fit on the screen with the given width.
     * 
     * @param fraction
     *            the fraction of the screen width to take up (0-1)
     * @return
     */
    public ImageFilterer fitToScreen(double fraction) {

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
        ImageProcessor result = ip.duplicate().resize(newWidth, newHeight);

        ip = result;
        return this;
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

//    /**
//     * Resize the image to fit the given dimensions, preserving aspect ratio
//     * 
//     * @param newWidth
//     *            the new width of the image
//     * @return
//     */
//    public ImageFilterer resize(int maxWidth, int maxHeight) {
//
//        if (ip == null) {
//            throw new IllegalArgumentException("Image processor is null");
//        }
//
//        int originalWidth = ip.getWidth();
//        int originalHeight = ip.getHeight();
//
//        // keep the image aspect ratio
//        double ratio = (double) originalWidth / (double) originalHeight;
//
//        double finalWidth = maxHeight * ratio; // fix height
//        finalWidth = finalWidth > maxWidth ? maxWidth : finalWidth; // but
//                                                                    // constrain
//                                                                    // width too
//
//        ImageProcessor result = ip.duplicate().resize((int) finalWidth);
//        ip = result;
//        return this;
//        // return new ImageFilterer(result);
//    }

    /**
     * Resize the image to fit on the screen. By default the width will be 80%,
     * unless this causes the height to become too great. In this case the
     * height will be set to 80%.
     * 
     * @return
     */
    public ImageFilterer fitToScreen() {

        if (ip == null) {
            throw new IllegalArgumentException("Image processor is null");
        }

        return fitToScreen(0.8);
    }

//    /**
//     * Crop the image to the region covered by the given component
//     * 
//     * @return
//     */
//    public ImageFilterer crop(@NonNull CellularComponent c) {
//
//        if (ip == null) {
//            throw new IllegalArgumentException("Image processor is null");
//        }
//        // Choose a clip for the image (an enlargement of the original nucleus
//        // ROI
//        int[] positions = c.getPosition();
//        int wideW = (int) (positions[CellularComponent.WIDTH] + 20);
//        int wideH = (int) (positions[CellularComponent.HEIGHT] + 20);
//        int wideX = (int) (positions[CellularComponent.X_BASE] - 10);
//        int wideY = (int) (positions[CellularComponent.Y_BASE] - 10);
//
//        wideX = wideX < 0 ? 0 : wideX;
//        wideY = wideY < 0 ? 0 : wideY;
//
//        ip.setRoi(wideX, wideY, wideW, wideH);
//        ImageProcessor result = ip.crop();
//        ip = result;
//        return this;
//        // return new ImageFilterer(result);
//    }

    /**
     * Fetch a 3x3 image kernel from within an int image array
     * 
     * @param array
     *            the input image
     * @param x
     *            the central x point
     * @param y
     *            the central y point
     * @return
     */
    public int[][] getKernel(int[][] array, int x, int y) {

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
     * @param array
     *            the 3x3 array of pixels
     * @return
     */
    public boolean bridgePixel(int[][] array) {

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
     * Close holes in the nuclear borders
     * 
     * @param ip
     *            the image processor. It must be convertible to a ByteProcessor
     * @param closingRadius
     *            the radius of the circle
     * @return a new ByteProcessor containing the closed image
     */
    public ImageFilterer morphologyClose(int closingRadius) {

        ByteProcessor result = ip.convertToByteProcessor();

        int shift = 1;
        int[] offset = { 0, 0 }; // no offsets to the structure element
        int elType = StructureElement.CIRCLE; // circle

        StructureElement se = new StructureElement(elType, shift, closingRadius, offset);
        MorphoProcessor mp = new MorphoProcessor(se);

        /*
         * Better way of closing. Dilate, fill, then erode
         */
        mp.dilate(result);

        // fill holes
        fill(result);

        mp.erode(result);
        ip = result;
        return this;
        // return new ImageFilterer(result);

    }

    /**
     * Dilate by the given amount
     * 
     * @param ip
     *            the image processor. It must be convertible to a ByteProcessor
     * @param amount
     *            the radius of the circle
     * @return a new ByteProcessor containing the closed image
     */
    public ImageFilterer dilate(int amount) {

        ByteProcessor result = ip.convertToByteProcessor();

        int shift = 1;
        int[] offset = { 0, 0 }; // no offsets to the structure element
        int elType = StructureElement.CIRCLE; // circle

        StructureElement se = new StructureElement(elType, shift, amount, offset);
        MorphoProcessor mp = new MorphoProcessor(se);

        /*
         * Better way of closing. Dilate, fill, then erode
         */
        mp.dilate(result);
        ip = result;
        return this;
        // return new ImageFilterer(result);

    }

    //
    /**
     * Based on the ImageJ Fill holes command: Binary fill by Gabriel Landini,
     * G.Landini at bham.ac.uk 21/May/2008
     * 
     * @param ip
     */
    private void fill(ImageProcessor ip) {

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
     * Use Canny edge detection to produce an image with potential edges
     * highlighted for the detector. Also performs morphology closing
     * 
     * @param image
     *            the stack to process
     * @return a stack with edges highlighted
     * @throws Exception
     */
    public ImageFilterer runEdgeDetector(int stackNumber, ICannyOptions options) {

        ImageStack searchStack = null;
        // Run the edge detection
        ip = st.getProcessor(stackNumber);
        // ByteProcessor searchImage = runEdgeDetector( options);
        // ip = runEdgeDetector( options).getProcessor();

        // ByteProcessor closed = (ByteProcessor) morphologyClose(
        // options.getClosingObjectRadius()).getProcessor() ;

        int closingRadius = options.getClosingObjectRadius();
        ImageProcessor closed = runEdgeDetector(options).morphologyClose(closingRadius).toProcessor();

        searchStack = ImageStack.create(st.getWidth(), st.getHeight(), 0, 8);
        searchStack.addSlice("closed", closed, 0);
        st = searchStack;
        return this;
        // return new ImageFilterer(searchStack);
    }

    /**
     * Perform a Canny edge detection on the given image
     * 
     * @param ip
     * @param options
     * @return
     */
    public ImageFilterer runEdgeDetector(ICannyOptions options) {
        ByteProcessor result = null;

        // // calculation of auto threshold
        if (options.isCannyAutoThreshold()) {
            autoDetectCannyThresholds(options, ip);
        }

        CannyEdgeDetector canny = new CannyEdgeDetector(options);
        canny.setSourceImage(ip.duplicate().getBufferedImage());

        canny.process();
        BufferedImage edges = canny.getEdgesImage();

        // convert to a TYPE_INT_GREY for use in a ByteProcessor
        BufferedImage converted = new BufferedImage(edges.getWidth(), edges.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        converted.getGraphics().drawImage(edges, 0, 0, null);

        result = new ByteProcessor(converted);

        converted = null;
        ip = result;
        return this;
        // return new ImageFilterer(result);
    }

    /**
     * Run circle detection using given Hough transform options
     * 
     * @param options
     *            the detection options
     * @return
     */
    public List<IPoint> runHoughCircleDetection(IHoughDetectionOptions options) {

        fine("Running hough detection");

        Hough_Circles circ = new Hough_Circles();

        circ.threshold = options.getHoughThreshold();
        circ.maxCircles = options.getNumberOfCircles();
        circ.radiusMin = (int) options.getMinRadius();
        circ.radiusMax = (int) options.getMaxRadius();
        circ.radiusInc = 1;

        // Repeat the nucleus detection parameters

        circ.run(ip);

        List<IPoint> list = new ArrayList<IPoint>();
        Point[] points = circ.centerPoint;

        if (points != null) {

            for (Point p : points) {
                if (p != null) {

                    list.add(IPoint.makeNew(p.getX(), p.getY()));
                }
            }
        }
        return list;
    }

    /**
     * Try to detect the optimal settings for the edge detector based on the
     * median image pixel intensity.
     * 
     * @param nucleusCannyOptions
     *            the options
     * @param image
     *            the image to analyse
     * @throws Exception
     */
    private void autoDetectCannyThresholds(ICannyOptions options, ImageProcessor image) {
        // calculation of auto threshold

        // find the median intensity of the image
        double medianPixel = getMedianIntensity(image);

        // if the median is >128, this is probably an inverted image.
        // invert it so the thresholds will work
        if (medianPixel > 128) {

            image.invert();
            medianPixel = getMedianIntensity(image);
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
     * Get the median pixel intensity in the image. Used in auto-selection of
     * Canny thresholds.
     * 
     * @param image
     *            the image to process
     * @return the median pixel intensity
     */
    private double getMedianIntensity(ImageProcessor image) {

        double[] values = new double[image.getWidth() * image.getHeight()];

        int i = 0;
        for (int w = 0; w < image.getWidth(); w++) {
            for (int h = 0; h < image.getHeight(); h++) {
                values[i] = (double) image.get(w, h);

                i++;
            }
        }
        DescriptiveStatistics ds = new DescriptiveStatistics();
        for(double d : values){
        	ds.addValue(d);
        }
        return ds.getPercentile(Stats.MEDIAN);
    }
}
