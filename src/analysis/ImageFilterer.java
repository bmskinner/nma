/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis;

import java.awt.image.BufferedImage;

import analysis.AnalysisOptions.CannyOptions;
import mmorpho.MorphoProcessor;
import mmorpho.StructureElement;
import stats.Stats;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.plugin.filter.Binary;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;
import utility.Constants;

public class ImageFilterer {
	
	/**
	 * Run a Kuwahara filter to enhance edges in the image
	 * @param stack the image
	 * @param filterSize the radius of the kernel
	 */
	public static ImageProcessor runKuwaharaFiltering(ImageStack stack, int stackNumber, int filterSize){
		
		Kuwahara_Filter kw = new Kuwahara_Filter();
		ImagePlus img = ImageExporter.getInstance().convertToRGB(stack);
		kw.setup("", img);
		
		ImageProcessor result = stack.getProcessor(stackNumber).duplicate();
		
		kw.filter(result, filterSize);
		return result;
	}
	
	/**
	 * The chromocentre can cause 'skipping' of the edge detection
	 * from the edge to the interior of the nucleus. Make any pixel
	 * over threshold equal threshold to remove internal structures
	 * @param stack the stack to adjust
	 * @param stackNumber the plane in the stack (starts at 1)
	 * @param threshold the maximum intensity to allow
	 * @return a copy of the image processor, with flattening applied
	 */
	public static ImageProcessor squashChromocentres(ImageStack stack, int stackNumber, int threshold){	
		
		// fetch a copy of the int array
		ImageProcessor ip = stack.getProcessor(stackNumber);
		ImageProcessor result = squashChromocentres(ip, threshold);
		return result;
	}
	
	/**
	 * The chromocentre can cause 'skipping' of the edge detection
	 * from the edge to the interior of the nucleus. Make any pixel
	 * over threshold equal threshold to remove internal structures
	 * @param ip the image processor to flatten
	 * @param threshold the maximum intensity to allow
	 * @return a copy of the image processor, with flattening applied
	 */
	public static ImageProcessor squashChromocentres(ImageProcessor ip, int threshold){	
				
		ImageProcessor result = ip.duplicate();
		int[][] array = result.getIntArray();
		
		// threshold
		for(int x = 0; x<ip.getWidth(); x++){
			for( int y=0; y<ip.getHeight(); y++){
				if(array[x][y] > threshold ){
					array[x][y] = threshold;
				}
			}
		}
		
		result.setIntArray(array);
		return result;
	}
	
	/**
	 * Bridges unconnected pixels, that is, sets 0-valued pixels to 1 
	 * if they have two nonzero neighbors that are not connected. For example:

		1  0  0           1  1  0 
		1  0  1  becomes  1  1  1 
		0  0  1           0  1  1

	 * @param ip the image processor
	 * @param bridgeSize the distance to search
	 * @return
	 */
	public static ByteProcessor bridgePixelGaps(ImageProcessor ip, int bridgeSize){
		
		if(bridgeSize % 2 ==0 ){
			throw new IllegalArgumentException("Kernel size must be odd");
		}
		ByteProcessor result = ip.convertToByteProcessor();

		int[][] array = result.getIntArray();
		int[][] input = result.getIntArray();
		

		for(int x = 0; x<ip.getWidth(); x++){
			for( int y=0; y<ip.getHeight(); y++){

				int[][] kernel = getKernel(input, x, y);
				if(bridgePixel(kernel)){
//					IJ.log( "Bridge "+y+"  "+x);
					array[y][x] = 255;
				}
			}
		}
		
		result.setIntArray(array);
		return result;
	}
	
	/**
	 * Fetch a 3x3 image kernel from within an int image array
	 * @param array the input image
	 * @param x the central x point
	 * @param y the central y point
	 * @return
	 */
	public static int[][] getKernel(int[][] array, int x, int y){
		
		/*
		 * Create the kernel array, and zero it
		 */
		int[][] result = new int[3][3];
		for(int w =0; w<3; w++){
			
			for( int h=0; h<3; h++){
				
				result[h][w]  = 0;
			}
		}
		
		/*
		 * Fetch the pixel data
		 */
		
		for(int w = x-1, xR=0; w<=x+1; w++, xR++){
			if(w<0 || w>=array.length){
				continue; // ignore x values out of range
			}

			for( int h=y-1, yR=0; h<=y+1; h++, yR++){
				if(h<0|| h>=array.length){
					continue; // ignore y values out of range
				}
				
				result[yR][xR]  = array[h][w];
			}
			
		}
		return result;
	}
	
	/**
	 * Should a pixel kernel be bridged? If two or more pixels in the array are filled,
	 * and not connected, return true
	 * @param array the 3x3 array of pixels
	 * @return
	 */
	public static boolean bridgePixel(int[][] array){
		
		/*
		 * If the central pixel is filled, do nothing.
		 */
		if(array[1][1]==255){
//			System.out.println("Skip, filled");
			return false;
		}
		
		/*
		 * If there is a vertical or horizontal stripe
		 * of black pixels, they should be bridged
		 */
		
		int vStripe = 0;
		int hStripe = 0;
		for( int v=0; v<3; v++){
			if(array[1][v]==0){			
				vStripe++;
			}
			if(array[v][1]==0){			
				hStripe++;
			}
		}
		
		if(vStripe<3 && hStripe < 3){
//			System.out.println("No stripe");
			return false;
		}
				
		/*
		 * Are two white pixels present?
		 */
		
		int count = 0;
		for(int x = 0; x<array.length; x++){
			for( int y=0; y<array.length; y++){
				if(array[y][x] == 255 ){
					count++;
				}
				
			}
			if(count>=2){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Close holes in the nuclear borders
	 * @param ip the image processor. It must be convertible to a ByteProcessor
	 * @param closingRadius the radius of the circle
	 * @return a new ByteProcessor containing the closed image
	 */
	public static ByteProcessor morphologyClose(ImageProcessor ip, int closingRadius) throws Exception {

		ByteProcessor result = ip.convertToByteProcessor();

		int shift    = 1;
		int[] offset = {0,0}; // no offsets to the structure element
		int elType   = StructureElement.CIRCLE; //circle
		
		StructureElement se = new StructureElement(elType,  shift,  closingRadius, offset);
		MorphoProcessor  mp = new MorphoProcessor(se);

		/*
		 * Better way of closing.
		 * Dilate, fill, then erode
		 */
		mp.dilate(result);
		
		ImagePlus imp = new ImagePlus(null, result);

		IJ.run(imp, "Make Binary", "");
		IJ.run(imp, "Fill Holes", "");

		mp.erode(imp.getProcessor());
		
		/*
		 * The original way of closing - run the fclose
		 * This is just a dilate then erode, which can miss small holes
		 */
//		mp.fclose(result);

		return result;

	}
	
	/**
	 * Use Canny edge detection to produce an image with potential edges highlighted
	 * for the detector. Also performs morphology closing
	 * @param image the stack to process
	 * @return a stack with edges highlighted
	 * @throws Exception 
	 */
	public static ImageStack runEdgeDetector(ImageStack image, int stackNumber, CannyOptions options) throws Exception{

		ImageStack searchStack = null;
		// Run the edge detection
		
		ByteProcessor searchImage = runEdgeDetector(image.getProcessor(stackNumber), options);
		
		ByteProcessor closed = ImageFilterer.morphologyClose( searchImage  , options.getClosingObjectRadius()) ;
				
//		ByteProcessor bridged = ImageFilterer.bridgePixelGaps( closed  , 3) ;
		
//		ImagePlus imagePlus = new ImagePlus("", bridged);
//		imagePlus.show();
		
		
		searchStack = ImageStack.create(image.getWidth(), image.getHeight(), 0, 8);
		searchStack.addSlice("closed", closed, 0);
		
		
		searchImage=null;


		return searchStack;
	}
	
	/**
	 * Perform a Canny edge detection on the given image
	 * @param ip
	 * @param options
	 * @return
	 */
	public static ByteProcessor runEdgeDetector(ImageProcessor ip, CannyOptions options) throws Exception { 
		ByteProcessor result = null;

		
//		// calculation of auto threshold
		if(options.isCannyAutoThreshold()){
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

		return result;
	}
	
	/**
	 * Try to detect the optimal settings for the edge detector based on the 
	 * median image pixel intensity.
	 * @param nucleusCannyOptions the options
	 * @param image the image to analyse
	 * @throws Exception 
	 */
	private static void autoDetectCannyThresholds(CannyOptions options, ImageProcessor image) throws Exception{
		// calculation of auto threshold

		// find the median intensity of the image
		double medianPixel = getMedianIntensity(image);

		// if the median is >128, this is probably an inverted image.
		// invert it so the thresholds will work
		if(medianPixel>128){
			
			image.invert();
			medianPixel = getMedianIntensity(image);
		}

		// set the thresholds either side of the median
		double sigma = 0.33; // default value - TODO: enable change
		double lower = Math.max(0  , (1.0 - (2.5 * sigma)  ) * medianPixel  ) ;
		lower = lower < 0.1 ? 0.1 : lower; // hard limit
		double upper = Math.min(255, (1.0 + (0.6 * sigma)  ) * medianPixel  ) ;
		upper = upper < 0.3 ? 0.3 : upper; // hard limit
		options.setLowThreshold(  (float)  lower);
		options.setHighThreshold( (float)  upper);
//		logger.log("Auto thresholding: low: "+lower+"  high: "+upper, Logger.DEBUG);

	}
	
	/**
	 * Get the median pixel intensity in the image. Used in auto-selection
	 * of Canny thresholds.
	 * @param image the image to process
	 * @return the median pixel intensity
	 */
	private static double getMedianIntensity(ImageProcessor image) throws Exception {

		double[] values = new double[ image.getWidth()*image.getHeight() ];

		int i=0;
		for(int w = 0; w<image.getWidth();w++){
			for(int h = 0; h<image.getHeight();h++){
				values[i] = (double) image.get(w, h);

				i++;
			}
		}

		return Stats.quartile(values, Constants.MEDIAN);
	}
}
