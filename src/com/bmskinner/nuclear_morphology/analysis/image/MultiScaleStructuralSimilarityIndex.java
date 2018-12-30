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
package com.bmskinner.nuclear_morphology.analysis.image;

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
	 * All the values calculated by MS-SSIM
	 * @author ben
	 * @since 1.15.0
	 *
	 */
	public class MSSIMScore {
		public final double luminance, contrast, structure, msSsimIndex;
		
		public MSSIMScore(double l, double c, double s, double m) {
			luminance = l;
			contrast = c;
			structure = s;
			msSsimIndex = m;
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
		
		
		ImageProcessor image_1_p, image_2_p;
		int  pointer, filter_length, image_height, image_width, image_dimension, bits_per_pixel_1, bits_per_pixel_2, a, b, c;
		float filter_weights [];
		double luminance_exponent [] = { 1, 1, 1, 1, 1, 0.1333};
		double contrast_exponent [] = { 1, 0.0448, 0.2856, 0.3001, 0.2363, 0.1333};
		double structure_exponent []= { 1, 0.0448, 0.2856, 0.3001, 0.2363, 0.1333};
		double luminance_comparison =1;
		double contrast_comparison =1;
		double structure_comparison =1;
		double ms_ssim_index;
		double [] ssim_map;
		double ssim_index;

		bits_per_pixel_1=inputImageA.getBitDepth();
		bits_per_pixel_2=inputImageB.getBitDepth();

		// THIS DIALOG BOX SHOWS DIFFERENT OPTIONS TO CREATE THE WINDOW WE ARE GOING TO USE TO EVALUATE SSIM INDEX OVER THE ENTIRE IMAGES
		//	
		double sigma_gauss = DEFAULT_SIGMA_GAUSS;
		int filter_width = 11;
		double K1 = 0.01; 
		double K2 = 0.03;
		double lod [] = {0.0378, -0.0238, -0.1106, 0.3774, 0.8527, 0.3774, -0.1106, -0.0238, 0.0378};  
		//
		// LOD [] ARE LOW PASS FILTER VALUES. Impulse response of low-pass filter to use defaults to 9/7 biorthogonal wavelet filters. IT USES THE ROUSE/HEMAMI'S VALUES INSTEAD OF ZHOU WANG'S METHOD AND VALUES.
		// THERE IS A LITTLE DIFFERENCE (< 2%) WITH MR. WANG'S VALUES FOR THE SAME SET OF IMAGES.
		//
		double number_of_levels = 5;
		double downsampled=1;
		boolean gaussian_window = true;
//		String[] window_type = {"Gaussian","Same weight"};  // WE CAN WEIGHTS THE WINDOW WITH A GAUSSIAN WEIGHTING FUNCTION OR GIVING THE SAME WEIGHT TO ALL THE PIXELS IN THE WINDOW
//		String window_selection = window_type[0];
//		String[] kind_of_algorithm = {ZHOU_WANG,ROUSE_HEMAMI};  // WE CAN USE THE INDEX FOR THE SUPRA-THRESHOLD LEVEL (WANG)  OR THE RECOGNITION THRESHOLD (ROUSE/HEMAMI)
		String algorithm_selection = ROUSE_HEMAMI; // DEFAULT TO ROUSE/HENAMI
//		boolean out=false;
		boolean show_ssim_map= false;

//		String[] ssim_map_level_option = {"0", "1", "2", "3", "4", "5"};
//		String ssim_map_selection = ssim_map_level_option[0];
		int ssim_map_level=0;

		double C1 = (Math.pow(2, bits_per_pixel_1) - 1)*K1;
		C1= C1*C1;
		double C2 = (Math.pow(2, bits_per_pixel_1) - 1)*K2;
		C2=C2*C2;
		//
		// NOW, WE CREATE THE FILTER, GAUSSIAN OR MEDIA FILTER, ACCORDING TO THE VALUE OF boolean "gaussian_window"
		//
		filter_length = filter_width*filter_width;
		float window_weights [] = new float [filter_length];
		double [] array_gauss_window = new double [filter_length];

		if (gaussian_window) {	

			double value, distance = 0;
			int center = (filter_width/2);
			double total = 0;
			double sigma_sq=sigma_gauss*sigma_gauss;

			for (int y = 0; y < filter_width; y++){
				for (int x = 0; x < filter_width; x++){
					distance = Math.abs(x-center)*Math.abs(x-center)+Math.abs(y-center)*Math.abs(y-center);
					pointer = y*filter_width + x;
					array_gauss_window[pointer] = Math.exp(-0.5*distance/sigma_sq);
					total = total + array_gauss_window[pointer];
				}
			}
			for (pointer=0; pointer < filter_length; pointer++) {	
				array_gauss_window[pointer] = array_gauss_window[pointer] / total;
				window_weights [pointer] = (float) array_gauss_window[pointer];
			}
		}
		else { 								// NO WEIGHTS. ALL THE PIXELS IN THE EVALUATION WINDOW HAVE THE SAME WEIGHT
			for (pointer=0; pointer < filter_length; pointer++) {
				array_gauss_window[pointer]= 1.0/ filter_length;
				window_weights [pointer] = (float) array_gauss_window[pointer];
			}
		}
		//
		// END OF FILTER SELECTION							
		//
		//
		// THE VALUE OF THE LOW PASS FILTER
		//
		float [] lpf = new float [81]; 
		int lpf_width = 9;

		for (a=0; a<lpf_width;a++) {
			for (b=0; b<lpf_width;b++) {
				lpf [a*lpf_width+b] = (float) (lod[a]*lod[b]);
			}
		}
		float suma_lpf =0;
		int cont=0;
		for (cont=0; cont<81;cont++) {
			suma_lpf= suma_lpf + lpf[cont];
		}
		for (cont=0; cont<81;cont++) {
			lpf[cont]= lpf[cont]/suma_lpf;
		}
		//
		// MAIN ALGORITHM
		//

		image_width = inputImageA.getWidth();
		image_width = (int) (image_width/downsampled);		// YOU CAN DOWNSAMPLE THE IMAGE BEFORE YOU CALCULATE THE MS-SSIM
		inputImageA.setInterpolate(false);
		inputImageB.setInterpolate(false);
		image_1_p= inputImageA.resize (image_width);
		image_2_p= inputImageB.resize (image_width);

		// WE ARE GOING TO USE ARRAYS OF 6 LEVELS INSTEAD OF 5.
		// WE WANT TO FORCE THAT THE INDEX OVER THE LEVEL WERE THE SAME THAN THE INDEX OVER THE ARRAY. 
		// REMEMBER THAT IN JAVA THE FIRST INDEX OF AN ARRAY IS THE "0" POSITION. WE WILL NEVER USE THIS POSITION IN THE FOLLOWING THREE ARRAYS.
		//
		int level=1;
		double [] contrast = new double [6];  
		double [] structure = new double [6];
		double [] luminance = new double [6];

		for (level=1; level <=number_of_levels; level++) {	// THIS LOOP CALCULATES, FOR EACH ITERATION, THE VALUES OF L, C AND S

			if (level !=1) {
				image_1_p.convolve (lpf, lpf_width, lpf_width);
				image_2_p.convolve (lpf, lpf_width, lpf_width);
				image_1_p.setInterpolate(false);			// IT'S CRITICAL TO THIS VALUE. DON'T USE TRUE
				image_2_p.setInterpolate(false);
				image_1_p= image_1_p.resize (image_width/2);
				image_2_p= image_2_p.resize (image_width/2);
			}
			image_height = image_1_p.getHeight();
			image_width = image_1_p.getWidth();
			image_dimension = image_width*image_height;

			if (ssim_map_level == level) {
				ssim_map = new double [image_dimension];
				show_ssim_map=true;
			}	
			else {
				ssim_map = new double [1];
				show_ssim_map=false;
			}
			ImageProcessor mu1_ip = new FloatProcessor (image_width, image_height);
			ImageProcessor mu2_ip = new FloatProcessor (image_width, image_height);
			float [] array_mu1_ip = (float []) mu1_ip.getPixels();
			float [] array_mu2_ip = (float []) mu2_ip.getPixels();

			float [] array_mu1_ip_copy = new float [image_dimension];
			float [] array_mu2_ip_copy = new float [image_dimension];

			a=b=0;
			for (pointer =0; pointer<image_dimension; pointer++) {	

				if (bits_per_pixel_1 == 8) {
					a = (0xff & image_1_p.get (pointer));
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
				array_mu1_ip [pointer] = array_mu1_ip_copy [pointer] = a; // Float.intBitsToFloat(a);
				array_mu2_ip [pointer] = array_mu2_ip_copy [pointer] = b; //Float.intBitsToFloat(b);
			}
			mu1_ip.convolve (window_weights, filter_width, filter_width);
			mu2_ip.convolve (window_weights, filter_width, filter_width);

			double [] mu1_sq = new double [image_dimension];
			double [] mu2_sq = new double [image_dimension];
			double [] mu1_mu2 = new double [image_dimension];

			for (pointer =0; pointer<image_dimension; pointer++) {
				mu1_sq[pointer] = array_mu1_ip [pointer]*array_mu1_ip [pointer];
				mu2_sq[pointer] = array_mu2_ip[pointer]*array_mu2_ip[pointer];
				mu1_mu2 [pointer]= array_mu1_ip [pointer]*array_mu2_ip[pointer];
			}
			double [] sigma1 = new double [image_dimension];
			double [] sigma2 = new double [image_dimension];
			double [] sigma1_sq = new double [image_dimension];
			double [] sigma2_sq = new double [image_dimension];
			double [] sigma12 = new double [image_dimension];

			for (pointer =0; pointer<image_dimension; pointer++) {

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

			for (pointer =0; pointer<image_dimension; pointer++) {
				array_soporte_1[pointer] = (float) sigma1_sq[pointer];
				array_soporte_2[pointer] = (float) sigma2_sq[pointer];
				array_soporte_3[pointer] = (float) sigma12[pointer];
			}
			soporte_1_ip.convolve (window_weights, filter_width,  filter_width);
			soporte_2_ip.convolve (window_weights, filter_width,  filter_width); 
			soporte_3_ip.convolve (window_weights, filter_width,  filter_width);

			for (pointer =0; pointer<image_dimension; pointer++) {
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

				for (pointer =0; pointer<image_dimension; pointer++) {

					luminance_point = ( 2*mu1_mu2[pointer] + C1) / (mu1_sq[pointer]+mu2_sq[pointer] + C1);
					luminance[level] = luminance [level] + luminance_point;

					contrast_point = (2*sigma1[pointer]*sigma2[pointer] + C2) / (sigma1_sq[pointer] + sigma2_sq[pointer] + C2);
					contrast [level] = contrast [level]+contrast_point;

					structure_point = (sigma12[pointer] + C2/2) / (sigma1[pointer]*sigma2[pointer] + C2/2);
					structure [level] = structure [level]+structure_point;

					if (show_ssim_map) {
						ssim_map[pointer] = luminance_point*contrast_point*structure_point;
						suma = suma + ssim_map[pointer];
					}
				}	
			}	

			else {   // ROUSE/HEMAMI

				for (pointer =0; pointer<image_dimension; pointer++) {

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

					if (((sigma1[pointer] == 0) | (sigma2[pointer] == 0)) & (sigma1[pointer] != sigma2[pointer]))
						structure_point = 0;
					else
						if ((sigma1[pointer] == 0) & (sigma2[pointer] == 0))
							structure_point = 1;
						else
							structure_point = (sigma12[pointer]) / (sigma1[pointer]*sigma2[pointer]);

					structure [level] = structure [level]+structure_point;

					if (show_ssim_map) {
						ssim_map[pointer] = luminance_point*contrast_point*structure_point;
						suma = suma + ssim_map[pointer];
					}
				}	
			}	// END WANG - ROUSE/HEMAMI IF-ELSE

			contrast [level] = contrast [level] / image_dimension;
			structure [level] = structure [level] / image_dimension;
			if (level == number_of_levels) 
				luminance [level] = luminance [level] / image_dimension;
			else 
				luminance [level] =1;

		} 	// END-FOR OF OUTER LOOP OVER THE DIFFERENT VIEWING LEVELS
		//
		for (level=1; level <=number_of_levels; level++) {

			if (structure[level] < 0) structure[level] = -1*structure[level];
			luminance_comparison = Math.pow ( luminance [level], luminance_exponent[level])*luminance_comparison;
			contrast_comparison = Math.pow (contrast [level], contrast_exponent[level])*contrast_comparison;
			structure_comparison = Math.pow (structure [level], structure_exponent[level])*structure_comparison;
		}
		ms_ssim_index= luminance_comparison*contrast_comparison*structure_comparison;

		MSSIMScore result = new MSSIMScore(luminance_comparison, contrast_comparison, structure_comparison, ms_ssim_index);
		return result;
	}
}
