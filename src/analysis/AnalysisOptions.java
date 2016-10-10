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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import analysis.signals.NuclearSignalOptions;
import logging.Loggable;
import stats.NucleusStatistic;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;

public class AnalysisOptions implements Serializable, Loggable {

	private static final long serialVersionUID = 1L;
	private  int    nucleusThreshold;
	private  double minNucleusSize;
	private  double maxNucleusSize;
	private  double minNucleusCirc;
	private  double maxNucleusCirc;
	
	private Map<String, CannyOptions> edgeDetection = new HashMap<String, CannyOptions>(0);
	
	private Map<UUID, NuclearSignalOptions> signalDetection = new HashMap<UUID, NuclearSignalOptions>(0);
		
	private boolean normaliseContrast = false; 

	private double angleWindowProportion = 0.05;
	
	private double scale = 1; // hold the length of a pixel in metres

	private NucleusType nucleusType = null;


	/**
	 * Should a reanalysis be performed?
	 */
	private boolean performReanalysis = false;

	/**
	 * Should images for a reanalysis be aligned
	 * beyond the offsets provided?
	 */
	private boolean realignMode = false;

	private boolean refoldNucleus = false;

	private File folder = null;
	private File mappingFile = null;

	private String refoldMode = null;

	private int xoffset = 0;
	private int yoffset = 0;
	
	private boolean keepFailedCollections = false;// allow failed collection to be retained for manual analysis
	
	private int channel = 0;

	public AnalysisOptions(){
		
		this.addCannyOptions("nucleus");
		this.addCannyOptions("tail");
	}
	
	
	
	/**
	 * Duplicate the data in the template options
	 * @param template
	 */
	public AnalysisOptions(AnalysisOptions template){
		nucleusThreshold = template.getNucleusThreshold();
		minNucleusSize   = template.getMinNucleusSize();
		maxNucleusSize   = template.getMaxNucleusSize();
		minNucleusCirc   = template.getMinNucleusCirc();
		maxNucleusCirc   = template.getMaxNucleusCirc();
		
		edgeDetection    = new HashMap<String, CannyOptions>(0);
		for(String s : template.getCannyOptionTypes()){
			edgeDetection.put(s, template.getCannyOptions(s));
		}

        signalDetection = new HashMap<UUID, NuclearSignalOptions>(0);
        
        for(UUID s : template.getNuclearSignalGroups()){
			signalDetection.put(s, template.getNuclearSignalOptions(s));
		}
		
		normaliseContrast      = template.isNormaliseContrast(); 
		angleWindowProportion  = template.getAngleWindowProportion();
		scale                  = template.getScale(); 
		nucleusType            = template.getNucleusType();
		performReanalysis      = template.isReanalysis() ;
		realignMode            = template.realignImages();
		refoldNucleus          = template.refoldNucleus();
		folder                 = template.getFolder();
		mappingFile            = template.getMappingFile();
		refoldMode             = template.getRefoldMode();
		xoffset                = template.getXOffset();
		yoffset                = template.getYOffset();
		keepFailedCollections  = template.isKeepFailedCollections();
		channel                = template.getChannel();
	}


	/*
    -----------------------
    Getters
    -----------------------
	 */

	public File getFolder(){
		return this.folder;
	}

	public File getMappingFile(){
		return this.mappingFile;
	}

	public int getNucleusThreshold(){
		return this.nucleusThreshold;
	}

	public double getMinNucleusSize(){
		return this.minNucleusSize;
	}

	public double getMaxNucleusSize(){
		return this.maxNucleusSize;
	}

	public double getMinNucleusCirc(){
		return this.minNucleusCirc;
	}

	public double getMaxNucleusCirc(){
		return this.maxNucleusCirc;
	}

	public double getAngleWindowProportion(){
		return this.angleWindowProportion;
	}

	public NucleusType getNucleusType(){
		return this.nucleusType;
	}
	
	

	public String getRefoldMode(){
		return this.refoldMode;
	}

	public boolean isReanalysis(){
		return this.performReanalysis;
	}

	public boolean realignImages(){
		return this.realignMode;
	}

	public boolean refoldNucleus(){
		return this.refoldNucleus;
	}

	public int getXOffset(){
		return  this.xoffset;
	}

	public int getYOffset(){
		return  this.yoffset;
	}

	public double getScale() {
		return scale;
	}
	

	public int getChannel() {
		return channel;
	}



	public void setChannel(int channel) {
		this.channel = channel;
	}



	public void setScale(double scale) {
		this.scale = scale;
	}


	public void setNucleusThreshold(int nucleusThreshold) {
		this.nucleusThreshold = nucleusThreshold;
	}

	public void setMinNucleusSize(double minNucleusSize) {
		this.minNucleusSize = minNucleusSize;
	}


	public void setMaxNucleusSize(double maxNucleusSize) {
		this.maxNucleusSize = maxNucleusSize;
	}


	public void setMinNucleusCirc(double minNucleusCirc) {
		this.minNucleusCirc = minNucleusCirc;
	}


	public void setMaxNucleusCirc(double maxNucleusCirc) {
		this.maxNucleusCirc = maxNucleusCirc;
	}


	public void setAngleWindowProportion(double proportion) {
		this.angleWindowProportion = proportion;
	}


	public void setNucleusType(NucleusType nucleusType) {
		this.nucleusType = nucleusType;
	}


	public void setPerformReanalysis(boolean performReanalysis) {
		this.performReanalysis = performReanalysis;
	}


	public void setRealignMode(boolean realignMode) {
		this.realignMode = realignMode;
	}


	public void setRefoldNucleus(boolean refoldNucleus) {
		this.refoldNucleus = refoldNucleus;
	}


	public void setFolder(File folder) {
		this.folder = folder;
	}


	public void setMappingFile(File mappingFile) {
		this.mappingFile = mappingFile;
	}


	public void setRefoldMode(String refoldMode) {
		this.refoldMode = refoldMode;
	}


	public void setXoffset(int xoffset) {
		this.xoffset = xoffset;
	}


	public void setYoffset(int yoffset) {
		this.yoffset = yoffset;
	}

	public boolean isNormaliseContrast() {
		return normaliseContrast;
	}


	public void setNormaliseContrast(boolean normaliseContrast) {
		this.normaliseContrast = normaliseContrast;
	}
	
	/**
	 * Get the canny options associated with the
	 * given type, or null if not present
	 * @param type the name to check
	 * @return canny detection options
	 */
	public CannyOptions getCannyOptions(String type){
		return edgeDetection.get(type); 
	}
	
	public void addCannyOptions(String type){
		edgeDetection.put(type, new CannyOptions()); 
	}
	
	public Set<String> getCannyOptionTypes(){
		return edgeDetection.keySet();
	}
	
	/**
	 * Check if the given type name is already present
	 * @param type the name to check
	 * @return present or not
	 */
	public boolean hasCannyOptions(String type){
		if(this.edgeDetection.containsKey(type)){
			return true;
		} else {
			return false;
		}
	}
	
    public Set<UUID> getNuclearSignalGroups(){
		return signalDetection.keySet();
	}
	
	/**
	 * Get the nuclear signal options associated with the
     * given signal group id. If not present, the group is created
	 * @param type the name to check
	 * @return nuclear detection options
	 */
    public NuclearSignalOptions getNuclearSignalOptions(UUID signalGroup){
        if(this.signalDetection.containsKey(signalGroup)){
            return this.signalDetection.get(signalGroup);
        } else {
            this.addNuclearSignalOptions(signalGroup);
            return this.signalDetection.get(signalGroup);
        }

	}
	
    public void addNuclearSignalOptions(UUID id){
        signalDetection.put(id, new NuclearSignalOptions());
    }
    
    public void addNuclearSignalOptions(UUID id, NuclearSignalOptions options){
        signalDetection.put(id, options);
    }


	
	/**
	 * Check if the given type name is already present
	 * @param type the name to check
	 * @return present or not
	 */
    public boolean hasSignalDetectionOptions(UUID signalGroup){
        if(this.signalDetection.containsKey(signalGroup)){

			return true;
		} else {
			return false;
		}
	}

	public boolean isKeepFailedCollections() {
		return keepFailedCollections;
	}

	public void setKeepFailedCollections(boolean keepFailedCollections) {
		this.keepFailedCollections = keepFailedCollections;
	}

	public boolean isValid(Nucleus c){
		boolean result = true;
		
		if(c.getStatistic(NucleusStatistic.AREA) < getMinNucleusSize()){
			
			result = false;
		}
		
		if(c.getStatistic(NucleusStatistic.AREA) > getMaxNucleusSize()){
			
			result = false;
		}
		
		if(c.getStatistic(NucleusStatistic.CIRCULARITY) < getMinNucleusCirc()){
			
			result = false;
		}
		
		if(c.getStatistic(NucleusStatistic.CIRCULARITY) > getMaxNucleusCirc()){
			
			result = false;
		}
		
		return result;
	}


	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(angleWindowProportion);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		
		result = prime * result + channel;
		result = prime * result
				+ ((edgeDetection == null) ? 0 : edgeDetection.hashCode());
		result = prime * result + ((folder == null) ? 0 : folder.hashCode());
		
		temp = Double.doubleToLongBits(maxNucleusCirc);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maxNucleusSize);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minNucleusCirc);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minNucleusSize);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (normaliseContrast ? 1231 : 1237);
		result = prime * result + nucleusThreshold;
		result = prime * result
				+ ((nucleusType == null) ? 0 : nucleusType.hashCode());
		result = prime * result + (performReanalysis ? 1231 : 1237);
		result = prime * result + (realignMode ? 1231 : 1237);
		temp = Double.doubleToLongBits(scale);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((signalDetection == null) ? 0 : signalDetection.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
//		finest("Testing equality");
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AnalysisOptions other = (AnalysisOptions) obj;
		if (Double.doubleToLongBits(angleWindowProportion) != Double
				.doubleToLongBits(other.angleWindowProportion))
			return false;
		if (channel != other.channel)
			return false;
//		finest("Testing edge detection parameters");
		if (edgeDetection == null) {
			if (other.edgeDetection != null)
				return false;
		} else if (!edgeDetection.equals(other.edgeDetection))
			return false;
		
//		finest("Testing doubles");
		if (Double.doubleToLongBits(maxNucleusCirc) != Double
				.doubleToLongBits(other.maxNucleusCirc))
			return false;
		if (Double.doubleToLongBits(maxNucleusSize) != Double
				.doubleToLongBits(other.maxNucleusSize))
			return false;
		if (Double.doubleToLongBits(minNucleusCirc) != Double
				.doubleToLongBits(other.minNucleusCirc))
			return false;
		if (Double.doubleToLongBits(minNucleusSize) != Double
				.doubleToLongBits(other.minNucleusSize))
			return false;
		
//		finest("Testing contrast");
		if (normaliseContrast != other.normaliseContrast)
			return false;
		if (nucleusThreshold != other.nucleusThreshold)
			return false;
		if (nucleusType != other.nucleusType)
			return false;
		if (performReanalysis != other.performReanalysis)
			return false;
		if (realignMode != other.realignMode)
			return false;
		if (Double.doubleToLongBits(scale) != Double
				.doubleToLongBits(other.scale))
			return false;
//		finest("Testing signal detection");
		if (signalDetection == null) {
			if (other.signalDetection != null)
				return false;
		} else if (!signalDetection.equals(other.signalDetection))
			return false;
		return true;
	}





	public class CannyOptions implements Serializable {

		private static final long serialVersionUID = 1L;
		
		public static final double DEFAULT_CANNY_LOW_THRESHOLD 			= 0.5;
		public static final double DEFAULT_CANNY_HIGH_THRESHOLD 		= 1.5;
		
		public static final double DEFAULT_CANNY_TAIL_LOW_THRESHOLD 	= 0.1;
		public static final double DEFAULT_CANNY_TAIL_HIGH_THRESHOLD 	= 0.5;
		
		public static final double DEFAULT_CANNY_KERNEL_RADIUS 			= 3;
		public static final int    DEFAULT_CANNY_KERNEL_WIDTH 			= 16;
		public static final int    DEFAULT_CLOSING_OBJECT_RADIUS 		= 5;
		public static final int    DEFAULT_TAIL_CLOSING_OBJECT_RADIUS 	= 3;
		
		public static final int	   DEFAULT_KUWAHARA_KERNEL_RADIUS 		= 3;
		public static final boolean DEFAULT_USE_KUWAHARA 				= true;
		
		public static final boolean DEFAULT_FLATTEN_CHROMOCENTRES		= true;
		public static final int		DEFAULT_FLATTEN_THRESHOLD			= 100;

		// values for Canny edge deteection
		private boolean useCanny; 
		private boolean cannyAutoThreshold;
		
		private boolean flattenChromocentres; 	// should the white threshold be lowered to hide internal structures?
		private int flattenThreshold; // if the white threhold is lower, this is the value
		private boolean useKuwahara;	// perform a Kuwahara filtering to enhance edge detection?
		private int kuwaharaKernel;		// the radius of the Kuwahara kernel - must be an odd number
		
		private float lowThreshold;		// the canny low threshold
		private float highThreshold;	// the canny high threshold
		private float kernelRadius;		// the kernel radius
		private int   kernelWidth;		// the kernel width
		private int   closingObjectRadius; // the circle radius for morphological closing
		
		public CannyOptions(){
			
		}
		
		public boolean isUseCanny() {
			return useCanny;
		}


		public void setUseCanny(boolean useCanny) {
			this.useCanny = useCanny;
		}
		
		public boolean isUseFlattenImage() {
			return flattenChromocentres;
		}
		
		public void setFlattenImage(boolean flattenImage) {
			this.flattenChromocentres = flattenImage;
		}
		
		public int getFlattenThreshold() {
			return flattenThreshold;
		}


		public void setFlattenThreshold(int flattenThreshold) {
			this.flattenThreshold = flattenThreshold;
		}
		
		
		public boolean isUseKuwahara() {
			return useKuwahara;
		}
		
		public void setUseKuwahara(boolean b){
			this.useKuwahara = b;
		}
		
		public int getKuwaharaKernel(){
			return kuwaharaKernel;
		}
		
		public void setKuwaharaKernel(int radius){
			kuwaharaKernel = radius;
		}

		public int getClosingObjectRadius() {
			return closingObjectRadius;
		}


		public void setClosingObjectRadius(int closingObjectRadius) {
			this.closingObjectRadius = closingObjectRadius;
		}


		public boolean isCannyAutoThreshold() {
			return cannyAutoThreshold;
		}


		public void setCannyAutoThreshold(boolean cannyAutoThreshold) {
			this.cannyAutoThreshold = cannyAutoThreshold;
		}
		
		public float getLowThreshold() {
			return lowThreshold;
		}


		public void setLowThreshold(float lowThreshold) {
			this.lowThreshold = lowThreshold;
		}


		public float getHighThreshold() {
			return highThreshold;
		}


		public void setHighThreshold(float highThreshold) {
			this.highThreshold = highThreshold;
		}


		public float getKernelRadius() {
			return kernelRadius;
		}


		public void setKernelRadius(float kernelRadius) {
			this.kernelRadius = kernelRadius;
		}


		public int getKernelWidth() {
			return kernelWidth;
		}


		public void setKernelWidth(int kernelWidth) {
			this.kernelWidth = kernelWidth;
		}
		
		
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (cannyAutoThreshold ? 1231 : 1237);
			result = prime * result + closingObjectRadius;
			result = prime * result + (flattenChromocentres ? 1231 : 1237);
			result = prime * result + flattenThreshold;
			result = prime * result + Float.floatToIntBits(highThreshold);
			result = prime * result + Float.floatToIntBits(kernelRadius);
			result = prime * result + kernelWidth;
			result = prime * result + kuwaharaKernel;
			result = prime * result + Float.floatToIntBits(lowThreshold);
			result = prime * result + (useCanny ? 1231 : 1237);
			result = prime * result + (useKuwahara ? 1231 : 1237);
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
			CannyOptions other = (CannyOptions) obj;
//			if (!getOuterType().equals(other.getOuterType()))
//				return false;
			if (cannyAutoThreshold != other.cannyAutoThreshold)
				return false;
			if (closingObjectRadius != other.closingObjectRadius)
				return false;
			if (flattenChromocentres != other.flattenChromocentres)
				return false;
			if (flattenThreshold != other.flattenThreshold)
				return false;
			if (Float.floatToIntBits(highThreshold) != Float
					.floatToIntBits(other.highThreshold))
				return false;
			if (Float.floatToIntBits(kernelRadius) != Float
					.floatToIntBits(other.kernelRadius))
				return false;
			if (kernelWidth != other.kernelWidth)
				return false;
			if (kuwaharaKernel != other.kuwaharaKernel)
				return false;
			if (Float.floatToIntBits(lowThreshold) != Float
					.floatToIntBits(other.lowThreshold))
				return false;
			if (useCanny != other.useCanny)
				return false;
			if (useKuwahara != other.useKuwahara)
				return false;
			return true;
		}
		
		private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		    
			/*
			 * The chromocentre flattening parameter and Kuwahara
			 * kernel parameter are transient. When these are stored,
			 * check if they were filled, and override if needed.
			 */
			in.defaultReadObject();
		    
		}
	}
	
	
	
//	/**
//	 * Allow each signal group to have independent signal detection options
//	 *
//	 */
//	public class NuclearSignalOptions implements Serializable {
//		
//		public static final int    DEFAULT_SIGNAL_THRESHOLD		 	= 70;
//		public static final int    DEFAULT_MIN_SIGNAL_SIZE 			= 5;
//		public static final double DEFAULT_MAX_SIGNAL_FRACTION 		= 0.1;
//		public static final double DEFAULT_MIN_CIRC 				= 0.0;
//		public static final double DEFAULT_MAX_CIRC 				= 1.0;
//		
//		// modes for detecting signals
//		public static final int FORWARD 	= 0;
//		public static final int REVERSE 	= 1;
//		public static final int HISTOGRAM 	= 2;
//
//		private static final long serialVersionUID = 1L;
//		private int threshold		= DEFAULT_SIGNAL_THRESHOLD;
//		private double minCirc		= DEFAULT_MIN_CIRC;
//		private double maxCirc		= DEFAULT_MAX_CIRC;
//		
//		private double minSize 		= DEFAULT_MIN_SIGNAL_SIZE;
//		private double maxFraction	= DEFAULT_MAX_SIGNAL_FRACTION;
//		
//		private int detectionMode = NuclearSignalOptions.FORWARD;
//		
//		public NuclearSignalOptions(){
////			
//		}
//		
//		public int getSignalThreshold(){
//			return this.threshold;
//		}
//
//		public double getMinSize(){
//			return this.minSize;
//		}
//
//		public double getMaxFraction(){
//			return this.maxFraction;
//		}
//
//		public double getMinCirc(){
//			return this.minCirc;
//		}
//
//		public double getMaxCirc(){
//			return this.maxCirc;
//		}
//
//		public void setThreshold(int threshold) {
//			this.threshold = threshold;
//		}
//
//		public void setMinCirc(double minCirc) {
//			this.minCirc = minCirc;
//		}
//
//		public void setMaxCirc(double maxCirc) {
//			this.maxCirc = maxCirc;
//		}
//
//		public void setMinSize(double minSize) {
//			this.minSize = minSize;
//		}
//
//		public void setMaxFraction(double maxFraction) {
//			this.maxFraction = maxFraction;
//		}
//		
//		public int getMode(){
//			return this.detectionMode;
//		}
//		
//		public void setMode(int mode){
//			this.detectionMode = mode;
//		}
//
//		@Override
//		public int hashCode() {
//			final int prime = 31;
//			int result = 1;
//			result = prime * result + detectionMode;
//			long temp;
//			temp = Double.doubleToLongBits(maxCirc);
//			result = prime * result + (int) (temp ^ (temp >>> 32));
//			temp = Double.doubleToLongBits(maxFraction);
//			result = prime * result + (int) (temp ^ (temp >>> 32));
//			temp = Double.doubleToLongBits(minCirc);
//			result = prime * result + (int) (temp ^ (temp >>> 32));
//			temp = Double.doubleToLongBits(minSize);
//			result = prime * result + (int) (temp ^ (temp >>> 32));
//			result = prime * result + threshold;
//			return result;
//		}
//
//		@Override
//		public boolean equals(Object obj) {
//			if (this == obj)
//				return true;
//			if (obj == null)
//				return false;
//			if (getClass() != obj.getClass())
//				return false;
//			NuclearSignalOptions other = (NuclearSignalOptions) obj;
//
//			if (detectionMode != other.detectionMode)
//				return false;
//			if (Double.doubleToLongBits(maxCirc) != Double
//					.doubleToLongBits(other.maxCirc))
//				return false;
//			if (Double.doubleToLongBits(maxFraction) != Double
//					.doubleToLongBits(other.maxFraction))
//				return false;
//			if (Double.doubleToLongBits(minCirc) != Double
//					.doubleToLongBits(other.minCirc))
//				return false;
//			if (Double.doubleToLongBits(minSize) != Double
//					.doubleToLongBits(other.minSize))
//				return false;
//			if (threshold != other.threshold)
//				return false;
//			return true;
//		}
//
//	}
}
