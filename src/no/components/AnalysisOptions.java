package no.components;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AnalysisOptions implements Serializable {

	private static final long serialVersionUID = 1L;
	private  int    nucleusThreshold;
	private  double minNucleusSize;
	private  double maxNucleusSize;
	private  double minNucleusCirc;
	private  double maxNucleusCirc;
	
	private Map<String, CannyOptions> edgeDetection = new HashMap<String, CannyOptions>(0);
	
	private Map<String, NuclearSignalOptions> signalDetection = new HashMap<String, NuclearSignalOptions>(0);
		
	private boolean normaliseContrast; 

	private int angleProfileWindowSize;
	
	private double scale; // hold the length of a pixel in metres

	private Class<?> nucleusClass;


	/**
	 * Should a reanalysis be performed?
	 */
	private boolean performReanalysis;

	/**
	 * Should images for a reanalysis be aligned
	 * beyond the offsets provided?
	 */
	private boolean realignMode;

	private boolean refoldNucleus;

	private File folder;
	private File mappingFile;

	private String refoldMode;

	private int xoffset;
	private int yoffset;

	public AnalysisOptions(){
		
		this.addCannyOptions("nucleus");
		this.addCannyOptions("tail");
		this.addNuclearSignalOptions("default");
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

	public int getAngleProfileWindowSize(){
		return this.angleProfileWindowSize;
	}

	public Class<?> getNucleusClass(){
		return this.nucleusClass;
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


	public void setAngleProfileWindowSize(int angleProfileWindowSize) {
		this.angleProfileWindowSize = angleProfileWindowSize;
	}


	public void setNucleusClass(Class<?> nucleusClass) {
		this.nucleusClass = nucleusClass;
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
	
	/**
	 * Get the nuclear signal options associated with the
	 * given type, or null if not present
	 * @param type the name to check
	 * @return nuclear detection options
	 */
	public NuclearSignalOptions getNuclearSignalOptions(String type){
		return this.signalDetection.get(type);
	}
	
	public void addNuclearSignalOptions(String type){
		signalDetection.put(type, new NuclearSignalOptions());
	}
	
	/**
	 * Check if the given type name is already present
	 * @param type the name to check
	 * @return present or not
	 */
	public boolean hasSignalDetectionOptions(String type){
		if(this.signalDetection.containsKey(type)){
			return true;
		} else {
			return false;
		}
	}

	
	public class CannyOptions implements Serializable {

		private static final long serialVersionUID = 1L;
		
		public static final double DEFAULT_CANNY_LOW_THRESHOLD = 0.5;
		public static final double DEFAULT_CANNY_HIGH_THRESHOLD = 1.5;
		
		public static final double DEFAULT_CANNY_TAIL_LOW_THRESHOLD = 0.1;
		public static final double DEFAULT_CANNY_TAIL_HIGH_THRESHOLD = 0.5;
		
		public static final double DEFAULT_CANNY_KERNEL_RADIUS = 3;
		public static final int    DEFAULT_CANNY_KERNEL_WIDTH = 16;
		public static final int    DEFAULT_CLOSING_OBJECT_RADIUS = 5;
		public static final int    DEFAULT_TAIL_CLOSING_OBJECT_RADIUS = 3;

		// values for Canny edge deteection
		private boolean useCanny; 
		private boolean cannyAutoThreshold;
//		private boolean flattenImage; 	// should the white threshold be lowered to hide internal structures?
//		private float flattenThreshold; // if the white threhold is lower, this is the value
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
		
//		public boolean isFlattenImage() {
//			return flattenImage;
//		}
//		
//		public void setFlattenImage(boolean flattenImage) {
//			this.flattenImage = flattenImage;
//		}
//		
//		public float getFlattenThreshold() {
//			return flattenThreshold;
//		}
//
//
//		public void setFlattenThreshold(float flattenThreshold) {
//			this.flattenThreshold = flattenThreshold;
//		}


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
	}
	
	
	
	/**
	 * Allow each signal group to have independent signal detection options
	 *
	 */
	public class NuclearSignalOptions implements Serializable {
		
		public static final int    DEFAULT_SIGNAL_THRESHOLD		 	= 70;
		public static final int    DEFAULT_MIN_SIGNAL_SIZE 			= 5;
		public static final double DEFAULT_MAX_SIGNAL_FRACTION 		= 0.1;
		public static final double DEFAULT_MIN_CIRC 				= 0.0;
		public static final double DEFAULT_MAX_CIRC 				= 1.0;
		
		// modes for detecting signals
		public static final int FORWARD 	= 0;
		public static final int REVERSE 	= 1;
		public static final int HISTOGRAM 	= 2;

		private static final long serialVersionUID = 1L;
		private int threshold		= DEFAULT_SIGNAL_THRESHOLD;
		private double minCirc		= DEFAULT_MIN_CIRC;
		private double maxCirc		= DEFAULT_MAX_CIRC;
		
		private double minSize 		= DEFAULT_MIN_SIGNAL_SIZE;
		private double maxFraction	= DEFAULT_MAX_SIGNAL_FRACTION;
		
		private int detectionMode = NuclearSignalOptions.FORWARD;
		
		public NuclearSignalOptions(){
//			
		}
		
		public int getSignalThreshold(){
			return this.threshold;
		}

		public double getMinSize(){
			return this.minSize;
		}

		public double getMaxFraction(){
			return this.maxFraction;
		}

		public double getMinCirc(){
			return this.minCirc;
		}

		public double getMaxCirc(){
			return this.maxCirc;
		}

		public void setThreshold(int threshold) {
			this.threshold = threshold;
		}

		public void setMinCirc(double minCirc) {
			this.minCirc = minCirc;
		}

		public void setMaxCirc(double maxCirc) {
			this.maxCirc = maxCirc;
		}

		public void setMinSize(double minSize) {
			this.minSize = minSize;
		}

		public void setMaxFraction(double maxFraction) {
			this.maxFraction = maxFraction;
		}
		
		public int getMode(){
			return this.detectionMode;
		}
		
		public void setMode(int mode){
			this.detectionMode = mode;
		}

	}
}
