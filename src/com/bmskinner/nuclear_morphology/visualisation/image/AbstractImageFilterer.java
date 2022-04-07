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
package com.bmskinner.nuclear_morphology.visualisation.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.ComponentOrienter;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.rules.PriorityAxis;

import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.TypeConverter;

/**
 * Contains methods for manipulating ImageProcessors, and provides conversion
 * between ImageStacks and ImageIcons for use in the UI.
 * 
 * @author ben
 *
 */
public abstract class AbstractImageFilterer {
	
	protected static final String DIMENSIONS_DO_NOT_MATCH_ERROR = "Dimensions do not match";

	private static final Logger LOGGER = Logger.getLogger(AbstractImageFilterer.class.getName());

    private static final int RGB_WHITE = 16777215;
    private static final int RGB_BLACK = 0;
    protected static final int BYTE_MAX  = 255;
    protected ImageProcessor ip        = null;

    /**
     * Construct with an image processor
     * 
     * @param ip the image processor
     */
    protected AbstractImageFilterer(final ImageProcessor ip) {
        this.ip = ip;
    }

    /**
     * Construct with an image processor from an image
     * 
     * @param img the image
     */
    protected AbstractImageFilterer(final ImagePlus img) {
        this(img.getProcessor());
    }

    /**
     * Duplicate the filterer - use the template processor and stack
     * 
     * @param f the template filterer
     */
    protected AbstractImageFilterer(final AbstractImageFilterer f) {
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
    public AbstractImageFilterer invert() {
        ip.invert();
        return this;
    }

    /**
     * Convert the processor into a ByteProcessor. Has no effect if the
     * processor is already a ByteProcessor
     * 
     * @return
     */
    public AbstractImageFilterer convertToByteProcessor() {
        if (!isByteProcessor()) {
            TypeConverter tc = new TypeConverter(ip, false);
            ip = tc.convertToByte();
        }
        return this;
    }

    /**
     * Convert the processor into a ShortProcessor (16-bit unsigned). Has no
     * effect if the processor is already a ShortProcessor
     * 
     * @return
     */
    public AbstractImageFilterer convertToShortProcessor() {
        if (!isShortProcessor()) {
            TypeConverter tc = new TypeConverter(ip, false);
            ip = tc.convertToShort();
        }
        return this;
    }

    /**
     * Convert the processor into a ColorProcessor. Has no effect if the
     * processor is already a ColorProcessor
     * 
     * @return
     */
    public AbstractImageFilterer convertToColorProcessor() {
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
     * Get the current image as a buffered image. If the filterer contains a
     * stack, returns the first element of the stack
     * @return
     */
    public BufferedImage toBufferedImage() {
    	if (ip==null) // && st==null
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
     * @param images
     * @return
     */
    public static List<ImageProcessor> fitToCommonCanvas(List<ImageProcessor> images){
    	int maxWidth = 0;
    	int maxHeight = 0;
    	for (ImageProcessor raw : images) {     
    		maxWidth = Math.max(maxWidth, raw.getWidth());
    		maxHeight = Math.max(maxHeight, raw.getHeight());
    	}
    	
    	List<ImageProcessor> result = new ArrayList<>();
    	for (ImageProcessor raw : images) {     
    		// Beware of single pixel offsets
    		int wDiff = maxWidth-raw.getWidth();
    		int lbuffer = wDiff%2==0 ? wDiff/2 : wDiff/2+1;
    		int rbuffer = wDiff/2;
    		
    		int hDiff = maxHeight-raw.getHeight();
    		int tbuffer = hDiff%2==0 ? hDiff/2 : hDiff/2+1;
    		int bbuffer = hDiff/2;
    		
    		result.add(ImageConverter.expandCanvas(raw, lbuffer, 
    				rbuffer, tbuffer, bbuffer, Color.BLACK));
    	}
    	return result;
    }

    /**
     * Recolour the given 8-bit image to use the given colour, weighting the
     * greyscale values by the HSB saturation level
     * 
     * @param ip the image
     * @param colour the maximum intensity colour for the image 
     * @return a colour processor with the recoloured values
     */
    public static ImageProcessor recolorImage(ImageProcessor ip, Color colour) {

        /*
         * The intensity of the signal is given by the 8-bit grey value. Intense
         * = 0 (black) and no signal = 255 (white)
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
     * Blend two images together using the Blend images plugin method
     * from Michael Schmid
     * https://imagej.nih.gov/ij/plugins/blend-images.html
     * @param ip1 the first image
     * @param ip2 the second image
     * @return
     */
    public static ImageProcessor blendImages(ImageProcessor ip1, float weight1, ImageProcessor ip2, float weight2) {
    	ip1.invert();
    	ip2.invert();
    	ImageProcessor result = ip1.duplicate();
    	FloatProcessor fp1 = null, fp2 = null;  // non-float images will be converted to these
        for (int i=0; i<ip1.getNChannels(); i++) { //grayscale: once. RBG: once per color, i.e., 3 times
            fp1 = ip1.toFloat(i, fp1);           // convert image or color channel to float (unless float already)
            fp2 = ip2.toFloat(i, fp2);
            blendFloat(fp1, weight1, fp2, weight2);
            result.setPixels(i, fp1);               // convert back from float (unless ip is a FloatProcessor)
        }
        result.invert();
        return result;
    }
    
    /**
     * Blend a FloatProcessor (i.e., a 32-bit image) with another one, i.e.
     * set the pixel values of fp1 according to a weighted sum of the corresponding
     * pixels of fp1 and fp2. This is done for pixels in the rectangle fp1.getRoi()
     * only.
     * Note that both FloatProcessors, fp1 and fp2 must have the same width and height.
     * 
     * From Michael Schmid's Blend images plugin:
     * https://imagej.nih.gov/ij/plugins/blend-images.html
     * @param fp1 The FloatProcessor that will be modified.
     * @param weight1 The weight of the pixels of fp1 in the sum.
     * @param fp2  The FloatProcessor that will be read only.
     * @param weight2 The weight of the pixels of fp2 in the sum.
     */
    private static void blendFloat (FloatProcessor fp1, float weight1, FloatProcessor fp2, float weight2) {
        int width = fp1.getWidth();
        float[] pixels1 = (float[])fp1.getPixels();     // array of the pixels of fp1
        float[] pixels2 = (float[])fp2.getPixels();
        for (int y=0; y<fp1.getHeight(); y++)          // loop over all pixels inside the roi rectangle
            for (int x=0; x<fp1.getWidth(); x++) {
                int i = x + y*width;                    // this is how the pixels are addressed
                pixels1[i] = weight1*pixels1[i] + weight2*pixels2[i]; //the weighted sum
            }
    }
    
    /**
     * Crop the image to the region covered by the given component
     * 
     * @return
     */
    public AbstractImageFilterer crop(@NonNull ICell c) {
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
        
        if(c.hasCytoplasm()) {
        	return crop(ip, c.getCytoplasm());
        }
        return crop(ip, c.getPrimaryNucleus());
    }
    
    /**
     * Crop the image to the region covered by the given component
     * 
     * @return
     */
    public AbstractImageFilterer crop(@NonNull CellularComponent c) {
        ip = crop(ip, c);
        return this;
    }
    
    /**
     * Crop the image to the region covered by the given component
     * 
     * @param ip the image to crop
     * @param c the component to crop boundaries from 
     * @return
     */
    public static ImageProcessor crop(ImageProcessor ip, CellularComponent c) {
    	if (ip == null)
            throw new IllegalArgumentException("Image processor is null");
        // Choose a clip for the image (an enlargement of the original nucleus ROI
        int wideW = (int)c.getWidth() + CellularComponent.COMPONENT_BUFFER*2;
        int wideH = (int)c.getHeight() + CellularComponent.COMPONENT_BUFFER*2;
        int wideX = c.getXBase() - CellularComponent.COMPONENT_BUFFER;
        int wideY = c.getYBase() - CellularComponent.COMPONENT_BUFFER;

        wideX = wideX < 0 ? 0 : wideX;
        wideY = wideY < 0 ? 0 : wideY;

        ip.setRoi(wideX, wideY, wideW, wideH);
        return ip.crop();
    }
    
    /**
     * Resize the given image to the maximum possible within the given
     * constraints on width and height. Aspect ratio is preserved. 
     * @param ip
     * @param maxWidth
     * @param maxHeight
     * @return the resized image
     */
    public static ImageProcessor resizeKeepingAspect(@NonNull ImageProcessor ip, int maxWidth, int maxHeight) {

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
     * @param maxWidth the maximum width of the new image
     * @param maxHeight the maximum height of the new image
     * @return a new image resized to fit the given dimensions
     */
    public AbstractImageFilterer resizeKeepingAspect(int maxWidth, int maxHeight) {
        ip = resizeKeepingAspect(ip, maxWidth, maxHeight);
        return this;
    }
    
    /**
     * Enlarge the given processor as needed so that it can be rotated by the given angle
     * without cropping, then rotates
     * @param ip
     * @param degrees
     * @return
     */
    public static ImageProcessor rotateImage(final ImageProcessor ip, double degrees) {

        double rad = Math.toRadians(degrees);

        // Calculate the new width and height of the canvas
        // new width is h sin(a) + w cos(a) and vice versa for height
        double newWidth = Math.abs(Math.sin(rad) * ip.getHeight()) + Math.abs(Math.cos(rad) * ip.getWidth());
        double newHeight = Math.abs(Math.sin(rad) * ip.getWidth()) + Math.abs(Math.cos(rad) * ip.getHeight());

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

        LOGGER.finer( String.format("New image %sx%s from %sx%s : Rot: %s", w, h, ip.getWidth(), ip.getHeight(), degrees));
        ImageProcessor newIp = new ColorProcessor(w, h);

        newIp.setColor(Color.WHITE); // fill current space with white
        newIp.fill();

        newIp.setBackgroundValue(16777215); // fill on rotate is RGB int white
        newIp.copyBits(ip, xBase, yBase, Blitter.COPY);
        newIp.rotate(degrees);
        return newIp;
    }
    
    /**
     * Orient the given image using the rules in a nucleus.
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
    		if(isFlip) {
    			if(PriorityAxis.Y.equals(n.getPriorityAxis())) {
    				newIp.flipHorizontal();
    			} else {
    				newIp.flipVertical();
    			}
    		}
    		return newIp;
    	} catch (MissingLandmarkException | ComponentCreationException e) {
    		LOGGER.warning("Unable to rotate image: "+e.getMessage());
    	}
    	return ip;
    }
    
    
    /**
     * Rescale the image intensity to fill the 0-255 range
     * @param ip the image to adjust
     * @return a new ColorProcessor with rescaled values
     */
    public static ImageProcessor rescaleRGBImageIntensity(final ImageProcessor ip, int min, int max) {
    	if (ip == null)
            throw new IllegalArgumentException("Image cannot be null");
    	if(min<0||min>255)
    		throw new IllegalArgumentException("Min threshold must be within 0-255");
    	if(max<0||max>255)
    		throw new IllegalArgumentException("Max threshold must be within 0-255");
    	
    	double rMax = min, gMax=min, bMax=min;
    	double rMin = max, gMin=max, bMin=max;
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
     * @param ip the image to adjust
     * @return a new ColorProcessor with rescaled values
     */
    public static ImageProcessor rescaleRGBImageIntensity(final ImageProcessor ip) {
    	return rescaleRGBImageIntensity(ip, 0, 255);
    }

    /**
     * Adjust the intensity of the given image so that the brightest pixel is at
     * 255 and the dimmest pixel is at 0
     * 
     * @param ip the image to adjust
     * @return a new ByteProcessor with rescaled values
     */
    public static ImageProcessor rescaleImageIntensity(@NonNull final ImageProcessor ip) {

        if (ip == null)
            throw new IllegalArgumentException("Image cannot be null");
        
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
            throw new IllegalArgumentException("Unsupported image type: " + ip.getClass().getSimpleName());
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
        LOGGER.finer("Max intensity: "+maxIntensity);
        LOGGER.finer("Min intensity: "+minIntensity);
        LOGGER.finer("Rescaling image across image range "+ range);
        
        // Adjust each pixel to the proportion in range 0-255
        for (int i = 0; i < ip.getPixelCount(); i++) {
            int pixel = ip.get(i);
            
            if(range==0) {
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
     * Create a new 8-bit image processor with the average of all the non-null
     * 8-bit images in the given list
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
     * Create a new 16-bit short processor with the sum of all the non-null
     * 8-bit images in the given list.
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
     * Given int pixel values, create a 16bit short processor, and scale the values if needed to
     * avoid overflows
     * @param pixelValues the pixel values for the image
     * @param maxValue the maximum pixel value in the image
     * @return
     */
    private static ImageProcessor createScaledShortProcessor(int[][] pixelValues, int maxValue) {
    	int w = pixelValues.length;
    	int h = pixelValues[0].length;
    	
    	ImageProcessor result = new ShortProcessor(w, h);
    	
    	if(maxValue > Short.MAX_VALUE) {
    		LOGGER.fine(String.format("Rescaling pixels with max value %s to fit short range", maxValue));
    		for (int x = 0; x < w; x++) {
    			for (int y = 0; y < h; y++) {
    				pixelValues[x][y] = (int) ((((double)pixelValues[x][y])/(double)maxValue) * (double)Short.MAX_VALUE);
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

            float diff = r - (float)b;
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
    
    protected static int[] intToRgb(int i){
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
                throw new IllegalArgumentException("Cannot average: a null warp image was encountered");
            if (w != ip.getWidth() || h != ip.getHeight())
                throw new IllegalArgumentException(String.format("Image dimensions %s by %s do not match expected dimensions %s by %s", w, h, ip.getWidth(), ip.getHeight()));
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

}
