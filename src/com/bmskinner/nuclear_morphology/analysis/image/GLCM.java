package com.bmskinner.nuclear_morphology.analysis.image;
//=================================================================================================
// GLCM_Texture_Too, v. 0.008
// Toby C. Cornish, tcornis3@jhmi.edu (or toby@tobycornish.com)
// 11/26/2007
// modified from GLCM_Texture (Gray Level Correlation Matrix Texture Analyzer) v0.4, Author: Julio E. Cabrera, 06/10/05
// 
// CHANGELOG:
// ------------------------------------------------------------------------------------------------
// 11/26/07 GLCM_TextureToo v0.008 - minor fixes; added references section -- tc
// 11/22/07 GLCM_TextureToo v0.007 - minor fixes to parameters -- tc
//                                 - moved mean and stdev calculations to common area -- tc
// 11/20/07 GLCM_TextureToo v0.006 - confirmed results against parker and Xite -- tc
//                                 - added preliminary shade, prominence, variance, homogeneity, inertia
//                                 - differentiated Haralick correlation from Walker correlation
// 11/19/07 GLCM_TextureToo v0.005 - changed method of calculating correlation -- tc
//                                 - should be closest in nomenclature to Walker, et al.  1995
// 11/19/07 GLCM_TextureToo v0.004 - corrected column name of idm -- tc
// 11/13/07 GLCM_TextureToo v0.003 - changed from roi.contains() to byte[] checking -> much faster -- tc
// 11/13/07 GLCM_TextureToo v0.002 - added progress bar --tc
// 11/11/07 GLCM_TextureToo v0.001 - inherited portions of the codebase from GLCM_Texture 0.4 -- tc
//                                 - fundamental rewrite of GLCM calculation
//                                 - added irregular ROI support
//                                 - added symmetrical/non-symmetrical GLCM calculations
//                                 - corrected directionality (phi) to be 0,45,90,135
//
//=================================================================================================
//
// References: 
//   R.M. Haralick, Texture feature for image classification, IEEE Trans. SMC 3 (1973) (1), pp. 610â??621.
//   Conners, R.W., Trivedi, M.M., and Harlow, C.A., Segmentation of a High-Resolution Urban Scene
//     Using Texture Operators, CVGIP(25), No. 3, March, 1984, pp. 273-310.
//   Walker, RF, Jackway, P and Longstaff, ID (1995) Improving Co-occurrence Matrix Feature Discrimination.'
//     In  DICTA '95, 3rd Conference on Digital Image Computing: Techniques and Application, 6 - 8 December,
//     1995, pages 643-648.
//   Parker, JR, Algorithms for Image Processing and Computer Vision, John Wiley & Sons, 1997.
//   Image processing lab, Department of Informatics, University of Oslo. Xite v1.35: glcmParameter.c, v1.30
//     2004/05/05 07:34:19 (2004)

import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.stats.GenericStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.StatisticDimension;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/**
 * This version of GLCM is based on Toby Cornish's
 * GLCM_Texture_Too, from {@link https://github.com/cornish/GLCM-TextureToo}.
 * @author ben
 * @since 1.16.1
 *
 */
public class GLCM {

	private static final Logger LOGGER = Logger.getLogger(GLCM.class.getName());

	public static final String USE_SYMMETRY_KEY   = "Use symmetry";

	/** Step size in pixels */
	public static final String STEP_SIZE_KEY      = "Step size";

	/** Direction of step - must be in [0, 45, 90, 135] */
	public static final String ANGLE_KEY          = "Angle";

	private final HashOptions options;

	/**
	 * The step angles possible in a GLCM analysis
	 * @author ben
	 *
	 */
	public enum GLCMStepAngle {
		NORTH(0),
		NORTHEAST(45), EAST(90), SOUTHEAST(135), ALL(-1);

		public final int angle;

		private GLCMStepAngle(int angle) {
			this.angle = angle;
		}

	}

	/**
	 * The parameters calculated from 
	 * the GCLM
	 * @author ben
	 *
	 */
	public enum GLCMParameter {
		/** Angular Second Moment */ ASM, 
		/** Inverse Difference Moment */ IDM, 
		CONSTRAST, ENERGY, ENTROPY,
		HOMOGENEITY, VARIANCE, SHADE, PROMINENCE, 
		INERTIA, CORRELATION, SUM;

		/**
		 * Convert to plottable stat for charting
		 * @return
		 */
		public PlottableStatistic toStat() {
			return new GenericStatistic(toString(), StatisticDimension.DIMENSIONLESS);
		}

		/**
		 * Convert all to plottable stats for charting
		 * @return
		 */
		public static PlottableStatistic[] toStats() {
			GLCMParameter[] values = values();
			PlottableStatistic[] result = new PlottableStatistic[values.length];
			for(int i=0; i<values.length; i++)
				result[i] = values[i].toStat();
			return result;
		}

		@Override
		public String toString() {
			// Capitalise first letter
			return super.toString().substring(0, 1).toUpperCase() + super.toString().substring(1).toLowerCase();
		}
	}

	

	/**
	 * Store results of the GLCM calculations from a
	 * single tile in an image. Does not store the
	 * entire matrix, just the output parameters
	 * @author ben
	 *
	 */
	public class GLCMTile {

		private Map<GLCMParameter, Double> values = new EnumMap<>(GLCMParameter.class);
		private String identifier = null;
		
		public GLCMTile() {
			// empty result
		}
		
		/**
		 * Create from a matrix
		 * @param matrix
		 */
		public GLCMTile(GLCMMatrix matrix) {
			set(GLCMParameter.ASM, matrix.asm());
			set(GLCMParameter.IDM, matrix.idm());
			set(GLCMParameter.CONSTRAST, matrix.contrast());
			set(GLCMParameter.ENERGY, matrix.energy());
			set(GLCMParameter.ENTROPY, matrix.entropy());
			set(GLCMParameter.HOMOGENEITY, matrix.homogeneity());
			set(GLCMParameter.INERTIA, matrix.inertia());
			set(GLCMParameter.VARIANCE, matrix.variance());
			set(GLCMParameter.SHADE, matrix.shade());
			set(GLCMParameter.PROMINENCE, matrix.prominence());
			set(GLCMParameter.CORRELATION, matrix.correlation());
			set(GLCMParameter.SUM, matrix.sum());
		}

		public void set(GLCMParameter key, double value) {
			values.put(key, value);
		}

		public double get(GLCMParameter key) {
			if(values.containsKey(key))
				return values.get(key);
			return 0;
		}

		/**
		 * Set an identifier for this result
		 * @param s
		 */
		public void setIdentifier(String s) {
			identifier = s;
		}

		/**
		 * Get the identifier for this result.
		 * Can be null.
		 * @return
		 */
		public String getIdentifier() {
			return identifier;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			if(identifier!=null)
				builder.append(identifier+Io.NEWLINE);
			for(Entry<GLCMParameter, Double> entry : values.entrySet()) {
				builder.append(entry.getKey()+": "+entry.getValue()+Io.NEWLINE);
			}
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
			result = prime * result + ((values == null) ? 0 : values.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GLCMTile other = (GLCMTile) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (identifier == null) {
				if (other.identifier != null)
					return false;
			} else if (!identifier.equals(other.identifier))
				return false;
			if (values == null) {
				if (other.values != null)
					return false;
			} else if (!values.equals(other.values))
				return false;
			return true;
		}

		private GLCM getOuterType() {
			return GLCM.this;
		}
		
		
	}

	/**
	 * Stores GLCM tiles covering an image
	 * @author ben
	 *
	 */
	public class GLCMTilePath {

		GLCMTile[][] values;

		/**
		 * Create from a template image, specifying the ROI diameter
		 * @param template the image to analyse
		 * @param roiDiameter the diameter of the circular roi
		 */
		public GLCMTilePath(ImageProcessor template, int roiDiameter) {
			values = new GLCMTile[template.getWidth()-roiDiameter][template.getHeight()-roiDiameter];
		}

		/**
		 * Add a GLCM result for when the roi is positioned at the given 
		 * coordinates
		 * @param result
		 * @param x
		 * @param y
		 */
		public void addGLCMTile(GLCMTile result, int x, int y) {
			values[x][y] = result;
		}

		/**
		 * Display the GLCM result an an image
		 * @param key the GLCM parameter to display
		 * @return
		 */
		public ImageProcessor toProcessor(GLCMParameter key) {
			ImageProcessor ip = new ShortProcessor(values.length, values[0].length);

			double max = 0;
			double min = Double.MAX_VALUE;
			for(int x=0; x<values.length; x++)
				for(int y=0; y<values[0].length;y++) {
					min = Math.min(min, values[x][y].get(key));
					max = Math.max(max, values[x][y].get(key));
				}

			max = max==0?1:max; // ensure never zero
			// Scale as a proportion of max value
			for(int x=0; x<values.length; x++) {
				for(int y=0; y<values[0].length;y++) {
					double v = values[x][y].get(key);
					double scaled = (v-min)/(max-min);
					ip.set(x, y, (int) (scaled*65536));
				}
			}
			return ip;
		}

		/**
		 * Add all the stored image processors as slices in a stack
		 * @return
		 */
		public ImageStack toStack() {

			ImageProcessor temp = toProcessor(GLCMParameter.SUM);
			ImageStack st = new ImageStack(temp.getWidth(), temp.getHeight());

			for(GLCMParameter key : GLCMParameter.values()) {
				LOGGER.finer("Adding "+key);
				st.addSlice(key.toString(), toProcessor(key));
			}
			return st;
		}

		public String toString(GLCMParameter key) {
			StringBuilder sb = new StringBuilder();
			for(int x=0; x<values.length; x++) {
				for(int y=0; y<values[0].length;y++) {
					sb.append(values[x][y].get(key)+"\t");
				}
				sb.append(Io.NEWLINE);
			}
			return sb.toString();
		}

	}

	
	/**
	 * Calculates GLCM probabilities
	 * across a matrix and parameters
	 * @author ben
	 *
	 */
	private class GLCMMatrix {
		
		public double[][] glcm;
		public double pixelCount;
		private GLCMStats stats;
		
		/**
		 * Internal store for overall stats on the GLCM
		 * matrix
		 * @author ben
		 *
		 */
		private class GLCMStats {

			public double meanx = 0;
			public double meany = 0;
			public double stdevx = 0;
			public double stdevy = 0;

			public GLCMStats() {
				double [] px = new double [256];
				double [] py = new double [256];

				// Px(i) and Py(j) are the marginal-probability matrix; sum rows (px) or columns (py) 
				// First, initialize the arrays to 0
				for (int i=0;  i<256; i++){
					px[i] = 0.0;
					py[i] = 0.0;
				}

				// sum the glcm rows to Px(i)
				for (int i=0;  i<256; i++) {
					for (int j=0; j<256; j++) {
						px[i] += glcm [i][j];
					} 
				}

				// sum the glcm rows to Py(j)
				for (int j=0;  j<256; j++) {
					for (int i=0; i<256; i++) {
						py[j] += glcm [i][j];
					} 
				}

				// calculate meanx and meany
				for (int i=0;  i<256; i++) {
					meanx += (i*px[i]);
					meany += (i*py[i]);
				}

				// calculate stdevx and stdevy
				for (int i=0;  i<256; i++) {
					stdevx += ((Math.pow((i-meanx),2))*px[i]);
					stdevy += ((Math.pow((i-meany),2))*py[i]);
				}
			}		
		}
		
		// Not accessible
		private GLCMMatrix() {
			// No access
		}
		
		/**
		 * Create with a specified dimension
		 * @param w
		 * @param h
		 */
		public GLCMMatrix(int w, int h) {
			glcm = new double[w][h];
			pixelCount = 0;
			stats = new GLCMStats();
		}
		
		private void updateStats() {
			stats = new GLCMStats();
		}
		
		/**
		 * Add values to this matrix
		 * @param g
		 */
		public GLCMMatrix plus(GLCMMatrix g) {
			
			if(g.glcm.length!=glcm.length && g.glcm[0].length!=glcm[0].length)
				return this;
			
			for (int i=0; i<256; i++)  {
				for (int j=0; j<256; j++) {
					glcm[i][j] += (g.glcm[i][j]);
				}
			}
			updateStats();
			return this;
		}
		
		/**
		 * Calculate the mean of the matrix
		 * for the given sample size
		 * @param n
		 * @return
		 */
		public GLCMMatrix average(double n) {
			for (int i=0; i<256; i++)  {
				for (int j=0; j<256; j++) {
					glcm[i][j] /= n;
				}
			}
			updateStats();
			return this;
		}
			
		
		private GLCMMatrix convertToProbabilities(){
			for (int i=0; i<256; i++)  {
				for (int j=0; j<256; j++) {
					glcm[i][j] = (glcm[i][j])/(pixelCount);
				}
			}
			updateStats();
			return this;
		}
		
		/**
		 * Calculate the angular second moment (ASM)
		 * @return
		 */
		public double asm() {
			double asm = 0.0;
			for (int i=0;  i<256; i++)  {
				for (int j=0; j<256; j++) {
					asm += (glcm[i][j]*glcm[i][j]);
				}
			}
			return asm;
		}
		
		/**
		 * Calculate the inverse difference moment (IDM) (Walker, et al. 1995). 
		 * This is calculated using the same formula as 
		 * Conners, et al., 1984 "Local Homogeneity"
		 * @param glcm
		 * @return
		 */
		public double idm() {
			double idm = 0.0;
			for (int i=0;  i<256; i++)  {
				for (int j=0; j<256; j++) {
					idm += ((1/(1+(Math.pow(i-j,2))))*glcm[i][j]);
				}
			}
			return idm;
		}

		/**
		 * Calculate the contrast (Haralick, et al. 1973).
		 * Similar to the inertia, except abs(i-j) is used
		 * @param glcm
		 * @return
		 */
		public double contrast() {
			double contrast = 0.0;
			for (int i=0;  i<256; i++)  {
				for (int j=0; j<256; j++) {
					contrast += Math.pow(Math.abs(i-j),2)*(glcm[i][j]);
				}
			}
			return contrast;
		}

		/**
		 * @param glcm
		 * @return
		 */
		public double energy() {
			double energy = 0.0;
			for (int i=0;  i<256; i++)  {
				for (int j=0; j<256; j++) {
					energy += Math.pow(glcm[i][j],2);
				}
			}
			return energy;
		}

		/**
		 * Calculate the entropy (Haralick et al., 1973; Walker, et al., 1995)
		 * @param glcm
		 * @return
		 */
		public double entropy() {
			double entropy = 0.0;
			for (int i=0;  i<256; i++)  {
				for (int j=0; j<256; j++) {
					if (glcm[i][j] != 0) {
						entropy = entropy-(glcm[i][j]*(Math.log(glcm[i][j])));
						//the next line is how Xite calculates it -- I am not sure why they use this, I do not think it is correct
						//(they also use log base 10, which I need to implement)
						//entropy = entropy-(glcm[i][j]*((Math.log(glcm[i][j]))/Math.log(2.0)) );
					}
				}
			}
			return entropy;
		}

		/**
		 * Calculate the homogeneity (Parker)
		 *  "Local Homogeneity" from Conners, et al., 1984 is calculated 
		 *  the same as IDM above.
		 *   Parker's implementation is below; absolute value
		 *   of i-j is taken rather than square
		 * @param glcm
		 * @return
		 */
		public double homogeneity() {
			double homogeneity = 0.0;
			for (int i=0;  i<256; i++) {
				for (int j=0; j<256; j++) {
					homogeneity += glcm[i][j]/(1.0+Math.abs(i-j));
				}
			}
			return homogeneity;
		}

		/**
		 * Calculate the inertia (Walker, et al., 1995; Connors, et al. 1984)
		 * @param glcm
		 * @return
		 */
		public double inertia() {
			double inertia = 0.0;
			for (int i=0;  i<256; i++)  {
				for (int j=0; j<256; j++) {
					if (glcm[i][j] != 0) {
						inertia += (Math.pow((i-j),2)*glcm[i][j]);
					}
				}
			}
			return inertia;
		}

		/**
		 * Calculate the sum of all glcm elements. If the matrix is
		 * of probabilities, this should return 1
		 * @param glcm
		 * @return
		 */
		public double sum() {
			double sum = 0.0;
			for (int i=0; i<256; i++)  {
				for (int j=0; j<256; j++) {
					sum = sum + glcm[i][j];
				}
			}
			return sum;
		}
		
		/**
		 * Calculate the variance ("variance" in Walker 1995; 
		 * "Sum of Squares: Variance" in Haralick 1973)
		 * @param glcm
		 * @param stats
		 * @return
		 */
		public double variance() {
			double variance = 0.0;
			double mean = 0.0;

			mean = (stats.meanx + stats.meany)/2;
			/*
		// this is based on xite, and is much greater than the actual mean -- it is here for reference only
		for (int i=0;  i<256; i++)  {
			for (int j=0; j<256; j++) {
				mean += glcm[i][j]*i*j;
			}
		}
			 */

			for (int i=0;  i<256; i++)  {
				for (int j=0; j<256; j++) {
					variance += (Math.pow((i-mean),2)* glcm[i][j]);
				}
			}
			return variance;
		}

		/**
		 * Calculate the shade (Walker, et al., 1995; Connors, et al. 1984)
		 * @param glcm
		 * @param stats
		 * @return
		 */
		public double shade() {
			double shade = 0.0;

			// calculate the shade parameter
			for (int i=0;  i<256; i++) {
				for (int j=0; j<256; j++) {
					shade += (Math.pow((i+j-stats.meanx-stats.meany),3)*glcm[i][j]);
				}
			}
			return shade;
		}

		/**
		 * Calculate the prominence (Walker, et al., 1995; Connors, et al. 1984)
		 * @param glcm
		 * @param stats
		 * @return
		 */
		public double prominence() {
			double prominence=0.0;
			for (int i=0;  i<256; i++) {
				for (int j=0; j<256; j++) {
					prominence += (Math.pow((i+j-stats.meanx-stats.meany),4)*glcm[i][j]);
				}
			}
			return prominence;
		}

		/**
		 * Calculate the correlation. Methods based on Haralick 1973 
		 * (and MatLab), Walker 1995 are included below. Haralick/Matlab 
		 * result reported for correlation currently; will 
		 * give Walker as an option in the future.
		 * @param glcm
		 * @param stats
		 * @return
		 */
		public double correlation() {
			double correlation=0.0;

			// calculate the correlation parameter
			for (int i=0;  i<256; i++) {
				for (int j=0; j<256; j++) {
					//Walker, et al. 1995 (matches Xite)
					//correlation += ((((i-meanx)*(j-meany))/Math.sqrt(stdevx*stdevy))*glcm[i][j]);
					//Haralick, et al. 1973 (continued below outside loop; matches original GLCM_Texture)
					//correlation += (i*j)*glcm[i][j];
					//matlab's rephrasing of Haralick 1973; produces the same result as Haralick 1973
					correlation += ((((i-stats.meanx)*(j-stats.meany))/( stats.stdevx*stats.stdevy))*glcm[i][j]);
				}
			}
			//Haralick, et al. 1973, original method continued.
			//correlation = (correlation -(meanx*meany))/(stdevx*stdevy);
			return correlation;
		}

	}
	
	/**
	 * Create with default options
	 */
	public GLCM() {
		this(defaultOptions());
	}

	/**
	 * Create with options
	 * @param options the options for the GLCM analysis
	 */
	public GLCM(@NonNull final HashOptions options) {
		this.options = options;

	}

	/**
	 * Create the default options for this analysis.
	 * @return
	 */
	public static HashOptions defaultOptions() {
		HashOptions o = new DefaultOptions();
		o.setInt(STEP_SIZE_KEY, 1);
		o.setString(ANGLE_KEY, GLCMStepAngle.ALL.toString());
		o.setBoolean(USE_SYMMETRY_KEY, true);
		return o;
	}


	/**
	 * Test if the given image is suitable for GLCM analysis
	 * @param ip the image to test
	 * @return true if the image can be analysed, false otherwise
	 */
	private boolean isValid(ImageProcessor ip) {
		if(ip==null) {
			LOGGER.fine("Image is null");
			return false;
		}

		if(ip.getBitDepth()!=8) {
			LOGGER.fine("Not 8 bit image");
			return false;
		}

		if(ip.getRoi()==null) {
			LOGGER.fine("No ROI in image");
			return false;
		}

		return true;
	}

	/**
	 * Given a square roi, calculate tiled GLCM values across
	 * the image, moving the roi by a fixed step size.
	 * @param ip the image to analyse
	 * @param tileWidth the width of the square roi in pixels 
	 * @return the calculated GLCM parameters for each tile
	 */
	public GLCMTilePath calculate(ImageProcessor ip, int tileWidth){
		LOGGER.fine("Calculating GLCM");
		int w = ip.getWidth();
		int h = ip.getHeight();

		GLCMTilePath result = new GLCMTilePath(ip, tileWidth);

		// Start from the left edge, up to 'tileWidth' from the right edge
		for(int x=0; x<w-tileWidth; x++) {
			LOGGER.fine("x: "+x+" of "+w);
			for(int y=0; y<h-tileWidth;y++) {
				Roi roi = new Roi(x, y, x+tileWidth, y+tileWidth);
				ip.setRoi(roi);
				result.addGLCMTile(calculate(ip), x, y);
			}
		}

		return result;
	}

	/**
	 * Calculate the GLCM across the entire component. Pixels
	 * outside the component roi are masked. Does not tile.
	 * @param component
	 * @return
	 */
	public GLCMTile calculate(CellularComponent component) {
		Roi roi = component.toRoi();
		roi.setLocation(Imageable.COMPONENT_BUFFER, Imageable.COMPONENT_BUFFER);
		try {
			ImageProcessor ip = component.getGreyscaleComponentImage();

			ip.setRoi(roi);
			GLCMTile r = calculate(ip);
			if(component instanceof Nucleus)
				r.setIdentifier( ((Nucleus)component).getNameAndNumber());
			else
				r.setIdentifier(component.getID().toString());
			return r;
		} catch (UnloadableImageException e) {
			LOGGER.log(Loggable.STACK, "Cannot open component image", e);
			return new GLCMTile();
		}
	}

	/**
	 * Calculate the GLCM results for the current tile ROI
	 * of the given image.
	 * @param ip the image to analyse, with a tile ROI already specified
	 * @return the GLCM result for the current tile
	 */
	public GLCMTile calculate(ImageProcessor ip) {

		if(!isValid(ip)) 
			return new GLCMTile();

		GLCMMatrix matrix = calculateMatrix(ip);
		return new GLCMTile(matrix);
	}

	/**
	 * Calculate the GLCM for the roi of the given image
	 * @param ip
	 * @return
	 */
	private GLCMMatrix calculateMatrix(ImageProcessor ip){

		LOGGER.finest("Calculating GLCM matrix");

		GLCMStepAngle phi = GLCMStepAngle.valueOf(options.getString(ANGLE_KEY));
		int d = options.getInt(STEP_SIZE_KEY);
		
		GLCMMatrix glcm;
		switch(phi) {
		case EAST:
			glcm = calculateMatrix(ip, 0, -d);
			break;
		case NORTH:
			glcm = calculateMatrix(ip, d, 0);
			break;
		case NORTHEAST:
			glcm = calculateMatrix(ip, d, -d);
			break;
		case SOUTHEAST:
			glcm = calculateMatrix(ip, -d, -d);
			break;
		case ALL:
		default:
			glcm = calculateMatrix(ip, 0, -d)
			.plus(calculateMatrix(ip, d, 0))
			.plus(calculateMatrix(ip, d, -d))
			.plus(calculateMatrix(ip, -d, -d))
			.average(4);
			break;
		
		}

		// convert the GLCM from absolute counts to probabilities
		return glcm.convertToProbabilities();
	}

	private GLCMMatrix calculateMatrix(ImageProcessor ip, int offsetX, int offsetY){

		// The matrix size is fixed to 8-bit grey levels.
		GLCMMatrix g = new GLCMMatrix(256, 256);

		// use the bounding rectangle ROI to roughly limit processing


		// get byte arrays for the image pixels and mask pixels
		int width = ip.getWidth();
		int height = ip.getHeight();
		byte [] pixels = (byte []) ip.getPixels();
		byte [] mask = ip.getMaskArray();

		// value = value at pixel of interest; dValue = value of pixel at offset    
		int value;
		int dValue;

		Rectangle roi = ip.getRoi();


		// loop through the pixels in the ROI bounding rectangle
		for (int y=roi.y; y<(roi.y + roi.height); y++) 	{
			for (int x=roi.x; x<(roi.x + roi.width); x++)	 {
				// check to see if the pixel is in the mask (if it exists)
				if ((mask == null) || ((0xff & mask[(((y-roi.y)*roi.width)+(x-roi.x))]) > 0) ) {
					// check to see if the offset pixel is in the roi
					int dx = x + offsetX;
					int dy = y + offsetY;
					if ( ((dx >= roi.x) && (dx < (roi.x+roi.width))) && ((dy >= roi.y) && (dy < (roi.y+roi.height))) ) {
						// check to see if the offset pixel is in the mask (if it exists) 
						if ((mask == null) || ((0xff & mask[(((dy-roi.y)*roi.width)+(dx-roi.x))]) > 0) ) {
							value = 0xff & pixels[(y*width)+x];
							dValue = 0xff & pixels[(dy*width) + dx];
							g.glcm [value][dValue]++;		  			
							g.pixelCount++;
						}
						// if symmetry is selected, invert the offsets and go through the process again
						if (options.getBoolean(USE_SYMMETRY_KEY)) {
							dx = x - offsetX;
							dy = y - offsetY;
							if ( ((dx >= roi.x) && (dx < (roi.x+roi.width))) && ((dy >= roi.y) && (dy < (roi.y+roi.height))) ) {
								// check to see if the offset pixel is in the mask (if it exists) 
								if ((mask == null) || ((0xff & mask[(((dy-roi.y)*roi.width)+(dx-roi.x))]) > 0) ) {
									value = 0xff & pixels[(y*width)+x];
									dValue = 0xff & pixels[(dy*width) + dx];
									g.glcm [dValue][value]++;		  			
									g.pixelCount++;
								}	
							}
						}
					}  
				}
			}
		}
		return g;
	}

}