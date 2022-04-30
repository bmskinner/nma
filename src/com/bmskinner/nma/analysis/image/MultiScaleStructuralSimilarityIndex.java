/**

MS-SSIM index by Zhou Wang and MS-SSIM* Index by David Rouse and Sheila Hemami


	The equivalent of Zhou Wang's MS-SSIM (SUPRA-THRESHOLD LEVEL PROBLEM) MatLab code as a Java plugin inside ImageJ.
	Also, this plugin performs the equivalent of David Rouse and Sheila Hemami's MSSIM* (RECOGNITION THRESHOLD PROBLEM).
	This plugin works with 8, 16 and 32 bits gray levels.

	Main references:

	Zhou Wang, A. C. Bovik, H. R. Sheikh, and E. P. Simoncelli, 
	"Image quality assessment: From error visibility to structural similarity", 
	IEEE Trans. Image Processing, vol. 13, pp. 600-612, Apr. 2004.

	David M. Rouse and Sheila S. Hemami, "Analyzing the Role of Visual Structure in the Recognition of Natural Image Content with Multi-Scale SSIM," 
	Proc. SPIE Vol. 6806, Human Vision and Electronic Imaging 2008.

	ImageJ by W. Rasband, U. S. National Institutes of Health, Bethesda, Maryland, USA, 
	http://rsb.info.nih.gov/ij/.  1997-2008. January 22th  2009.

	Java Code by Gabriel Prieto, Margarita Chevalier, Eduardo Guibelalde 22/01/2009.gprietor@med.ucm.es

	Permission to use, copy, or modify this software and its documentation for educational and research purposes only and without fee is hereby
	granted, provided that this copyright notice and the original authors' names appear on all copies and supporting documentation. This program
	shall not be used, rewritten, or adapted as the basis of a commercial software or hardware product without first obtaining permission of the
	authors. The authors make no representations about the suitability of this software for any purpose. It is provided "as is" without express
	or implied warranty.

	Please, refer to this version as:

	Gabriel Prieto, Margarita Chevalier, Eduardo Guibelalde. "MS_SSIM Index and MS_SSIM* Index as a Java plugin for ImageJ"
	Department of Radiology, Faculty of Medicine. Universidad Complutense. Madrid. SPAIN.
	http://www.ucm.es/info/fismed/MSSIM/MSSIM.htm

 */
package com.bmskinner.nma.analysis.image;

import java.text.DecimalFormat;

import org.eclipse.jdt.annotation.NonNull;

import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Calculate MS-SSIM* scores, using the algorithm from:
 * David M. Rouse and Sheila S. Hemami, "Analyzing the Role of Visual Structure 
 * in the Recognition of Natural Image Content with Multi-Scale SSIM," 
 * Proc. SPIE Vol. 6806, Human Vision and Electronic Imaging 2008.
 * This implementation was originally created as "MS_SSIM Index and MS_SSIM* Index as a Java plugin for ImageJ":
 * http://www.ucm.es/info/fismed/MSSIM/MSSIM.htm
 * @author Gabriel Prieto
 * @author Margarita Chevalier
 * @author Eduardo Guibelalde
 * @since 1.15.0
 *
 */
public class MultiScaleStructuralSimilarityIndex {
	
	private static final String ZHOU_WANG = "Zhou Wang";
	private static final String ROUSE_HEMAMI = "Rouse/Hemami";
	private static final int MIN_IMAGE_DIMENSION_PIXELS = 32;
	private static final double DEFAULT_SIGMA_GAUSS = 1.5;
	
	
	/**
	 * LOD [] ARE LOW PASS FILTER VALUES. Impulse response of low-pass filter to use defaults to 9/7 
	 * biorthogonal wavelet filters. IT USES THE ROUSE/HEMAMI'S VALUES INSTEAD OF ZHOU WANG'S METHOD AND VALUES.
	 * THERE IS A LITTLE DIFFERENCE (< 2%) WITH MR. WANG'S VALUES FOR THE SAME SET OF IMAGES.
	 */
	private static final double[] LOD = { 0.0378, -0.0238, -0.1106, 0.3774, 0.8527, 0.3774, -0.1106, -0.0238, 0.0378 };  
	
	/**
	 * All the values calculated by MS-SSIM
	 * @author ben
	 * @since 1.15.0
	 *
	 */
	public final class MSSIMScore {
		public final double luminance, contrast, structure, msSsimIndex;
		
		public MSSIMScore(double l, double c, double s, double m) {
			luminance = l;
			contrast = c;
			structure = s;
			msSsimIndex = m;
		}
		
		@Override
		public String toString() {
			DecimalFormat df = new DecimalFormat("0.000");
			return df.format(msSsimIndex);
		}
	}

	/**
	 * Calculate the MS-SSIM* index for the given images. Images must have the same dimensions,
	 * and must be either byte, float or short.
	 * @param inputImageA
	 * @param inputImageB
	 * @return the image MS-SSIM scores
	 */
	public MSSIMScore calculateMSSIM(@NonNull ImageProcessor inputImageA, @NonNull ImageProcessor inputImageB) {
		if(inputImageA.getWidth()!=inputImageB.getWidth())
			throw new IllegalArgumentException(String.format("Widths unqual: %s and %s", inputImageA.getWidth(), inputImageB.getWidth()));
		if(inputImageA.getHeight()!=inputImageB.getHeight())
			throw new IllegalArgumentException(String.format("Heights unqual: %s and %s", inputImageA.getHeight(), inputImageB.getHeight()));
		if(inputImageA.getHeight()<MIN_IMAGE_DIMENSION_PIXELS || inputImageA.getWidth()<MIN_IMAGE_DIMENSION_PIXELS)
			throw new IllegalArgumentException(String.format("Image is too small: %sx%s", inputImageA.getWidth(), inputImageA.getHeight()));
		if(inputImageA.getBitDepth()!=inputImageB.getBitDepth())
			throw new IllegalArgumentException(String.format("Bit depths do not match: %s and %s", inputImageA.getBitDepth(), inputImageB.getBitDepth()));
		if(inputImageA instanceof ColorProcessor)
			throw new IllegalArgumentException("Cannot handle colour images");
		
		double downsampled=1;

		int image_width = inputImageA.getWidth();
		image_width = (int) (image_width/downsampled);		// YOU CAN DOWNSAMPLE THE IMAGE BEFORE YOU CALCULATE THE MS-SSIM
		inputImageA.setInterpolate(false);
		inputImageB.setInterpolate(false);
		
		// Downsample both images
		ImageProcessor image_1_p = inputImageA.resize(image_width);
		ImageProcessor image_2_p = inputImageB.resize(image_width);

		MssimCalculation calc = new MssimCalculation(image_1_p, image_2_p);
		calc.calculate();
		return calc.getResult();
	}
	
	private class MssimCalculation {
		
		public static final double NUMBER_OF_LEVELS = 5;
		public static final int LPF_WIDTH = 9;
		public static final int FILTER_WIDTH = 11;
		
		public double luminance_exponent [] = { 1, 1, 1, 1, 1, 0.1333};
		public double contrast_exponent [] = { 1, 0.0448, 0.2856, 0.3001, 0.2363, 0.1333};
		public double structure_exponent []= { 1, 0.0448, 0.2856, 0.3001, 0.2363, 0.1333};
		public double luminance_comparison =1;
		public double contrast_comparison =1;
		public double structure_comparison =1;
		
		private double[] contrast = new double [6];  
		private double[] structure = new double [6];
		private double[] luminance = new double [6];
		
		private final float[] window_weights;
		private final double[] array_gauss_window;
		
		private double[] ssim_map;
		
		private String algorithm_selection = ROUSE_HEMAMI; // DEFAULT TO ROUSE/HENAMI
		private boolean showSsimMap = false;
		
		private static final boolean IS_GAUSSIAN_WINDOW = true;
		
		private ImageProcessor image_1_p, image_2_p;
		
		private int ssim_map_level=0;
		
		private int bits_per_pixel_1;
		
		private final float[] lpf;
		
		double K1 = 0.01; 
		double K2 = 0.03;
		double C1;
		double C2;
		
		public MssimCalculation(ImageProcessor imp1, ImageProcessor imp2) {
			image_1_p = imp1;
			image_2_p = imp2;
			
			bits_per_pixel_1=imp1.getBitDepth();
			C1 = (Math.pow(2, bits_per_pixel_1) - 1)*K1;
			C1 = C1*C1;
			C2 = (Math.pow(2, bits_per_pixel_1) - 1)*K2;
			C2 = C2*C2;

			int filterLength =  FILTER_WIDTH*FILTER_WIDTH;
			window_weights = new float [filterLength];
			array_gauss_window = new double [filterLength];
			createWeights(FILTER_WIDTH);
			
//			Create the low pass filters
			lpf = createLowPassFilter(LPF_WIDTH);
		}
		
		/**
		 * NOW, WE CREATE THE FILTER, GAUSSIAN OR MEDIA FILTER, ACCORDING TO THE VALUE OF boolean "gaussian_window"
		 * @param filterWidth
		 */
		private void createWeights(int filterWidth) {
			int filterLength =  filterWidth*filterWidth;
			double sigma_gauss = DEFAULT_SIGMA_GAUSS;
			if (IS_GAUSSIAN_WINDOW) {

				double distance = 0;
				int center = (filterWidth/2);
				double total = 0;
				double sigma_sq = sigma_gauss*sigma_gauss;

				for (int y = 0; y < filterWidth; y++){
					for (int x = 0; x < filterWidth; x++){
						distance = Math.abs(x-center)*Math.abs(x-center)+Math.abs(y-center)*Math.abs(y-center);
						int pointer = y*filterWidth + x;
						array_gauss_window[pointer] = Math.exp(-0.5*distance/sigma_sq);
						total = total + array_gauss_window[pointer];
					}
				}
				for (int pointer=0; pointer < filterLength; pointer++) {	
					array_gauss_window[pointer] = array_gauss_window[pointer] / total;
					window_weights [pointer] = (float) array_gauss_window[pointer];
				}
			}
			else { // NO WEIGHTS. ALL THE PIXELS IN THE EVALUATION WINDOW HAVE THE SAME WEIGHT
				for (int i=0; i < filterLength; i++) {
					array_gauss_window[i] = 1.0/ filterLength;
					window_weights [i] = (float) array_gauss_window[i];
				}
			}
		}
		
		private void calculate() {
			for (int level=1; level <=NUMBER_OF_LEVELS; level++) {	// THIS LOOP 
				calculateLevel(level);
			} 

			for (int level=1; level <=NUMBER_OF_LEVELS; level++) {
				if (structure[level] < 0)
					structure[level] = -1*structure[level];
				luminance_comparison = Math.pow ( luminance [level], luminance_exponent[level])*luminance_comparison;
				contrast_comparison = Math.pow (contrast [level], contrast_exponent[level])*contrast_comparison;
				structure_comparison = Math.pow (structure [level], structure_exponent[level])*structure_comparison;
			}
		}
		
		/**
		 * CALCULATE, FOR EACH ITERATION, THE VALUES OF L, C AND S
		 * @param level
		 */
		private void calculateLevel(int level) {
			
			int image_width  = image_1_p.getWidth();
			int image_height = image_1_p.getHeight();
			
			if (level!=1) {
				image_1_p.convolve (lpf, LPF_WIDTH, LPF_WIDTH);
				image_2_p.convolve (lpf, LPF_WIDTH, LPF_WIDTH);
				image_1_p.setInterpolate(false);			// IT'S CRITICAL TO THIS VALUE. DON'T USE TRUE
				image_2_p.setInterpolate(false);
				image_1_p = image_1_p.resize(image_width/2);
				image_2_p = image_2_p.resize(image_width/2);
			}
			
			// If level is above 1, these will have changed
			image_width  = image_1_p.getWidth();
			image_height = image_1_p.getHeight();

			int image_dimension = image_width*image_height;

			if (ssim_map_level == level) {
				ssim_map = new double [image_dimension];
				showSsimMap=true;
			}	
			else {
				ssim_map = new double [1];
				showSsimMap=false;
			}
			ImageProcessor mu1_ip = new FloatProcessor (image_width, image_height);
			ImageProcessor mu2_ip = new FloatProcessor (image_width, image_height);
			float [] array_mu1_ip = (float []) mu1_ip.getPixels();
			float [] array_mu2_ip = (float []) mu2_ip.getPixels();

			float [] array_mu1_ip_copy = new float [image_dimension];
			float [] array_mu2_ip_copy = new float [image_dimension];

			int a=0;
			int b=0;
			for (int pointer=0; pointer<image_dimension; pointer++) {	

				if (bits_per_pixel_1 == 8) {
					a = (0xff & image_1_p.get(pointer));
					b = (0xff & image_2_p.get(pointer));
				}
				if (bits_per_pixel_1 == 16) {
					a = (0xffff & image_1_p.get(pointer));
					b = (0xffff & image_2_p.get(pointer));	
				}
				if (bits_per_pixel_1 == 32) {
					a = (image_1_p.get(pointer));
					b = (image_2_p.get(pointer));
				}
				array_mu1_ip [pointer] = array_mu1_ip_copy [pointer] = a;
				array_mu2_ip [pointer] = array_mu2_ip_copy [pointer] = b;
			}
			mu1_ip.convolve (window_weights, FILTER_WIDTH, FILTER_WIDTH);
			mu2_ip.convolve (window_weights, FILTER_WIDTH, FILTER_WIDTH);

			double [] mu1_sq = new double [image_dimension];
			double [] mu2_sq = new double [image_dimension];
			double [] mu1_mu2 = new double [image_dimension];

			for (int pointer =0; pointer<image_dimension; pointer++) {
				mu1_sq[pointer] = array_mu1_ip [pointer]*array_mu1_ip [pointer];
				mu2_sq[pointer] = array_mu2_ip[pointer]*array_mu2_ip[pointer];
				mu1_mu2 [pointer]= array_mu1_ip [pointer]*array_mu2_ip[pointer];
			}
			double [] sigma1 = new double [image_dimension];
			double [] sigma2 = new double [image_dimension];
			double [] sigma1_sq = new double [image_dimension];
			double [] sigma2_sq = new double [image_dimension];
			double [] sigma12 = new double [image_dimension];

			for (int pointer =0; pointer<image_dimension; pointer++) {

				sigma1_sq[pointer] =array_mu1_ip_copy [pointer]*array_mu1_ip_copy [pointer];
				sigma2_sq[pointer] =array_mu2_ip_copy [pointer]*array_mu2_ip_copy [pointer];
				sigma12 [pointer] =array_mu1_ip_copy [pointer]*array_mu2_ip_copy [pointer];
			}
			//	
			//THERE IS A METHOD IN IMAGEJ THAT CONVOLVES ANY ARRAY, BUT IT ONLY WORKS WITH IMAGE PROCESSORS. THIS IS THE REASON BECAUSE I CREATE THE FOLLOWING PROCESSORS
			//
			ImageProcessor soporte_1_ip = new FloatProcessor (image_width, image_height);
			ImageProcessor soporte_2_ip = new FloatProcessor (image_width, image_height);
			ImageProcessor soporte_3_ip = new FloatProcessor (image_width, image_height);
			float [] array_soporte_1 =  (float []) soporte_1_ip.getPixels();
			float [] array_soporte_2 =  (float []) soporte_2_ip.getPixels();
			float [] array_soporte_3 =  (float []) soporte_3_ip.getPixels();

			for (int pointer =0; pointer<image_dimension; pointer++) {
				array_soporte_1[pointer] = (float) sigma1_sq[pointer];
				array_soporte_2[pointer] = (float) sigma2_sq[pointer];
				array_soporte_3[pointer] = (float) sigma12[pointer];
			}
			soporte_1_ip.convolve (window_weights, FILTER_WIDTH,  FILTER_WIDTH);
			soporte_2_ip.convolve (window_weights, FILTER_WIDTH,  FILTER_WIDTH); 
			soporte_3_ip.convolve (window_weights, FILTER_WIDTH,  FILTER_WIDTH);

			for (int pointer =0; pointer<image_dimension; pointer++) {
				sigma1_sq[pointer] =  array_soporte_1[pointer] - mu1_sq[pointer];
				sigma2_sq[pointer] =  array_soporte_2[pointer ]- mu2_sq[pointer];
				sigma12[pointer] =  array_soporte_3[pointer] - mu1_mu2[pointer];
				//
				// THE FOLLOWING SENTENCES ARE VERY AD-HOC. SOMETIMES, FOR INTERNAL REASONS OF PRECISION OF CALCULATIONS AROUND THE BORDERS, SIGMA_SQ
				// CAN BE NEGATIVE. THE VALUE CAN BE AROUND 0.001 IN SOME POINTS (A FEW). THE PROBLEM IS THAT, FOR SIMPICITY I CALCULATE SIGMA1 AS SQUARE ROOT OF SIGMA1_SQ
				// OF COURSE, IF THE ALGORITHM FINDS NEGATIVE VALUES, YOU GET THE MESSAGE  "IS NOT A NUMBER" IN RUN TIME.
				// 
				if (sigma1_sq[pointer]<0) {
					sigma1_sq[pointer]=0;
				}
				if (sigma2_sq[pointer]<0) {
					sigma2_sq[pointer]=0;
				}
				sigma1 [pointer] = Math.sqrt (sigma1_sq[pointer]);
				sigma2 [pointer] = Math.sqrt (sigma2_sq[pointer]);
			}
			//
			// WE HAVE GOT ALL THE VALUES TO CALCULATE LUMINANCE, CONTRAST AND STRUCTURE
			//
			double luminance_point=1;
			double contrast_point=0;
			double structure_point = 0;
			double suma=0;
			luminance [level] = 0;
			contrast [level] = 0;
			structure [level] = 0;

			if (algorithm_selection.equals(ZHOU_WANG)) {

				for (int pointer =0; pointer<image_dimension; pointer++) {

					luminance_point = ( 2*mu1_mu2[pointer] + C1) / (mu1_sq[pointer]+mu2_sq[pointer] + C1);
					luminance[level] = luminance [level] + luminance_point;

					contrast_point = (2*sigma1[pointer]*sigma2[pointer] + C2) / (sigma1_sq[pointer] + sigma2_sq[pointer] + C2);
					contrast [level] = contrast [level]+contrast_point;

					structure_point = (sigma12[pointer] + C2/2) / (sigma1[pointer]*sigma2[pointer] + C2/2);
					structure [level] = structure [level]+structure_point;

					if (showSsimMap) {
						ssim_map[pointer] = luminance_point*contrast_point*structure_point;
						suma = suma + ssim_map[pointer];
					}
				}	
			}	

			else {   // ROUSE/HEMAMI

				for (int pointer =0; pointer<image_dimension; pointer++) {

					if ( (mu1_sq[pointer]+mu2_sq[pointer]) == 0)
						luminance_point = 1;
					else
						luminance_point = ( 2*mu1_mu2[pointer]) / (mu1_sq[pointer]+mu2_sq[pointer]);

					luminance[level] = luminance [level] + luminance_point;

					if ( (sigma1_sq[pointer] + sigma2_sq[pointer]) == 0) 
						contrast_point =1;
					else
						contrast_point = (2*sigma1[pointer]*sigma2[pointer]) / (sigma1_sq[pointer] + sigma2_sq[pointer]);

					contrast [level] = contrast [level]+contrast_point;

					if (((sigma1[pointer] == 0) || (sigma2[pointer] == 0)) && (sigma1[pointer] != sigma2[pointer]))
						structure_point = 0;
					else
						if ((sigma1[pointer] == 0) && (sigma2[pointer] == 0))
							structure_point = 1;
						else
							structure_point = (sigma12[pointer]) / (sigma1[pointer]*sigma2[pointer]);

					structure [level] = structure [level]+structure_point;

					if (showSsimMap) {
						ssim_map[pointer] = luminance_point*contrast_point*structure_point;
						suma = suma + ssim_map[pointer];
					}
				}	
			}	// END WANG - ROUSE/HEMAMI IF-ELSE

			contrast [level] = contrast [level] / image_dimension;
			structure [level] = structure [level] / image_dimension;
			if (level == NUMBER_OF_LEVELS) 
				luminance [level] = luminance [level] / image_dimension;
			else 
				luminance [level] =1;
		}
		
		public MSSIMScore getResult() {
			double index = luminance_comparison*contrast_comparison*structure_comparison;
			return new MSSIMScore(luminance_comparison, contrast_comparison, structure_comparison, index);
		}
	}
	
	/**
	 * Create the low pass filter
	 * @param lpfWidth the filter width
	 * @return
	 */
	private float[] createLowPassFilter(int lpfWidth) {
		int lpfLength = lpfWidth*lpfWidth;
		float [] lpf = new float [lpfLength]; 

		for (int a = 0; a<lpfWidth; a++) {
			for (int b=0; b<lpfWidth; b++)
				lpf [a*lpfWidth+b] = (float) (LOD[a]*LOD[b]);
		}
		float sumA = 0;

		for (int cont=0; cont<lpfLength;cont++) 
			sumA += lpf[cont];
		if(sumA==0)
			return lpf; // Probably not a sensible abort point, but...
		
		for (int cont=0; cont<lpfLength;cont++)
			lpf[cont] /= sumA;
		return lpf;
	}
}
