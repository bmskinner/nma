package analysis.signals;

import java.io.Serializable;

public class NuclearSignalOptions implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public static final int    DEFAULT_SIGNAL_THRESHOLD		 	= 70;
	public static final int    DEFAULT_MIN_SIGNAL_SIZE 			= 5;
	public static final double DEFAULT_MAX_SIGNAL_FRACTION 		= 0.1;
	public static final double DEFAULT_MIN_CIRC 				= 0.0;
	public static final double DEFAULT_MAX_CIRC 				= 1.0;
	
	// modes for detecting signals
	public static final int FORWARD 	= 0;
	public static final int REVERSE 	= 1;
	public static final int HISTOGRAM 	= 2;

	public int threshold;
	public double minCirc, maxCirc, minSize, maxFraction;
	public int detectionMode;

	public NuclearSignalOptions(){
		this(   DEFAULT_SIGNAL_THRESHOLD, 
				DEFAULT_MIN_CIRC,
				DEFAULT_MAX_CIRC,
				DEFAULT_MIN_SIGNAL_SIZE,
				DEFAULT_MAX_SIGNAL_FRACTION,
				FORWARD);
		
	}
	
	public NuclearSignalOptions(int threshold, double minCirc, double maxCirc,
			double minSize, double maxFraction, int detectionMode) {
		this.threshold = threshold;
		this.minCirc = minCirc;
		this.maxCirc = maxCirc;
		this.minSize = minSize;
		this.maxFraction = maxFraction;
		this.detectionMode = detectionMode;
	}

	public int getThreshold() {
		return threshold;
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

	public double getMinCirc() {
		return minCirc;
	}

	public void setMinCirc(double minCirc) {
		this.minCirc = minCirc;
	}

	public double getMaxCirc() {
		return maxCirc;
	}

	public void setMaxCirc(double maxCirc) {
		this.maxCirc = maxCirc;
	}

	public double getMinSize() {
		return minSize;
	}

	public void setMinSize(double minSize) {
		this.minSize = minSize;
	}

	public double getMaxFraction() {
		return maxFraction;
	}

	public void setMaxFraction(double maxFraction) {
		this.maxFraction = maxFraction;
	}

	public int getDetectionMode() {
		return detectionMode;
	}

	public void setDetectionMode(int detectionMode) {
		this.detectionMode = detectionMode;
	}
	
	
}