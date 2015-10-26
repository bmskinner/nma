package analysis;

import java.awt.image.BufferedImage;

import analysis.AnalysisOptions.CannyOptions;
import mmorpho.MorphoProcessor;
import mmorpho.StructureElement;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import io.ImageExporter;
import io.ImageImporter;
import utility.Constants;
import utility.Stats;

public class ImageFilterer {
	
	/**
	 * Run a Kuwahara filter to enhance edges in the image
	 * @param stack the image
	 * @param filterSize the radius of the kernel
	 */
	public static ImageProcessor runKuwaharaFiltering(ImageStack stack, int stackNumber, int filterSize){
		
		Kuwahara_Filter kw = new Kuwahara_Filter();
		ImagePlus img = ImageExporter.convert(stack);
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
		ImageProcessor ip = stack.getProcessor(stackNumber).duplicate();
		
		int[][] array = ip.getIntArray();
		
		// threshold
		for(int x = 0; x<ip.getWidth(); x++){
			for( int y=0; y<ip.getHeight(); y++){
				if(array[x][y] > threshold ){
					array[x][y] = threshold;
				}
			}
		}
		
		ip.setIntArray(array);
		return ip;
	}
	
	/**
	 * Close holes in the nuclear borders
	 * @param ip the image processor
	 * @param closingRadius the radius of the circle
	 */
	public static ImageProcessor morphologyClose(ImageProcessor ip, int closingRadius){
//		ImageProcessor result = ip.duplicate();
		ByteProcessor result = ip.convertToByteProcessor();
		try {
			
			int shift=1;
			int[] offset = {0,0};
			int elType = StructureElement.CIRCLE; //circle
//			logger.log("Closing objects with circle of radius "+closingRadius, Logger.DEBUG);
//			
			StructureElement se = new StructureElement(elType,  shift,  closingRadius, offset);
//			IJ.log("Made se");
			MorphoProcessor mp = new MorphoProcessor(se);
//			IJ.log("Made mp");
			mp.fclose(result);
//			if(logger!=null){
//				logger.log("Objects closed", Logger.DEBUG);
//			}
			
//			IJ.log("Closed");
		} catch (Exception e) {
//			if(logger!=null){
//				logger.error("Error in morphology closing", e);
//			}
			
		}
		return result;
		
	}
	
	/**
	 * Use Canny edge detection to produce an image with potential edges highlighted
	 * for the detector
	 * @param image the stack to process
	 * @return a stack with edges highlighted
	 */
	public static ImageStack runEdgeDetector(ImageStack image, int stackNumber, CannyOptions options){

		//		bi.show();
		ImageStack searchStack = null;
		try {
			// using canny detector
//			CannyOptions nucleusCannyOptions = analysisOptions.getCannyOptions("nucleus");

//			// calculation of auto threshold
			if(options.isCannyAutoThreshold()){
				autoDetectCannyThresholds(options, image, stackNumber);
			}

//			if(logger!=null){
//				logger.log("Creating edge detector", Logger.DEBUG);
//			}
			
			CannyEdgeDetector canny = new CannyEdgeDetector();
			canny.setSourceImage(image.getProcessor(stackNumber).getBufferedImage());
			canny.setLowThreshold( options.getLowThreshold() );
			canny.setHighThreshold( options.getHighThreshold());
			canny.setGaussianKernelRadius(options.getKernelRadius());
			canny.setGaussianKernelWidth(options.getKernelWidth());

			canny.process();
			BufferedImage edges = canny.getEdgesImage();
			ImagePlus searchImage = new ImagePlus(null, edges);

			ImageProcessor closed = ImageFilterer.morphologyClose( searchImage.getProcessor()  , options.getClosingObjectRadius()) ;
			// add morphological closing
			ByteProcessor bp = closed.convertToByteProcessor();

//			bp = ImageFilterer.morphologyClose( bp  , nucleusCannyOptions.getClosingObjectRadius()) ;
			ImagePlus bi= new ImagePlus(null, bp);
			searchStack = ImageImporter.convert(bi);

			bi.close();
			searchImage.close();

//			if(logger!=null){
//				logger.log("Edge detection complete", Logger.DEBUG);
//			}
			
		} catch (Exception e) {
//			if(logger!=null){
//				logger.error("Error in edge detection", e);
//			}
			
		}
		return searchStack;
	}
	
	/**
	 * Perform a Canny edge detection on the given image
	 * @param ip
	 * @param options
	 * @return
	 */
	public static ImageProcessor runEdgeDetector(ImageProcessor ip, CannyOptions options){ 
		ImageProcessor result = null;
		try {

//			// calculation of auto threshold
//			if(options.isCannyAutoThreshold()){
//				autoDetectCannyThresholds(options, image, stackNumber);
//			}

			
			CannyEdgeDetector canny = new CannyEdgeDetector();
			canny.setSourceImage(ip.duplicate().getBufferedImage());
			canny.setLowThreshold( options.getLowThreshold() );
			canny.setHighThreshold( options.getHighThreshold());
			canny.setGaussianKernelRadius(options.getKernelRadius());
			canny.setGaussianKernelWidth(options.getKernelWidth());

			canny.process();
			BufferedImage edges = canny.getEdgesImage();
			ImagePlus searchImage = new ImagePlus(null, edges);
			result = searchImage.getProcessor();
//
//			ImageProcessor closed = ImageFilterer.morphologyClose( searchImage.getProcessor()  , options.getClosingObjectRadius()) ;
//			// add morphological closing
//			ByteProcessor bp = closed.convertToByteProcessor();
//
////			bp = ImageFilterer.morphologyClose( bp  , nucleusCannyOptions.getClosingObjectRadius()) ;
//			ImagePlus bi= new ImagePlus(null, bp);
//			searchStack = ImageImporter.convert(bi);
//
//			bi.close();
//			searchImage.close();

//			if(logger!=null){
//				logger.log("Edge detection complete", Logger.DEBUG);
//			}
			
		} catch (Exception e) {
//			if(logger!=null){
//				logger.error("Error in edge detection", e);
//			}
			
		}
		return result;
	}
	
	/**
	 * Try to detect the optimal settings for the edge detector based on the 
	 * median image pixel intensity.
	 * @param nucleusCannyOptions the options
	 * @param image the image to analyse
	 */
	private static void autoDetectCannyThresholds(CannyOptions options, ImageStack image, int stackNumber){
		// calculation of auto threshold

		// find the median intensity of the image
		double medianPixel = getMedianIntensity(image, stackNumber);

		// if the median is >128, this is probably an inverted image.
		// invert it so the thresholds will work
		if(medianPixel>128){
//			if(logger!=null){
//				logger.log("Detected high median ("+medianPixel+"); inverting");
//			}
			
			image.getProcessor(Constants.COUNTERSTAIN).invert();
			medianPixel = getMedianIntensity(image, stackNumber);
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
	private static double getMedianIntensity(ImageStack image, int stackNumber){
		ImageProcessor median = image.getProcessor(stackNumber);
		double[] values = new double[ median.getWidth()*median.getHeight() ];
		try {
			int i=0;
			for(int w = 0; w<median.getWidth();w++){
				for(int h = 0; h<median.getHeight();h++){
					values[i] = (double) median.get(w, h);

					i++;
				}
			}
		} catch (Exception e) {
//			if(logger!=null){
//				logger.error("Error getting median image intensity", e);
//			}
			
		}
		return Stats.quartile(values, 50);
	}
}
