package com.bmskinner.nuclear_morphology.components.options;

import java.io.File;

/**
 * A hash based replacement for the nuclear signal detection options
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultNuclearSignalHashOptions 
	extends AbstractHashDetectionOptions 
	implements IMutableNuclearSignalOptions {
	 
	private static final long serialVersionUID = 1L;
	
	private SignalDetectionMode mode;
	
	public DefaultNuclearSignalHashOptions(File folder){
		super(folder);
		setDouble(MAX_FRACTION,  DEFAULT_MAX_SIGNAL_FRACTION);
		mode        = DEFAULT_METHOD;
		
		setMinSize(DEFAULT_MIN_SIGNAL_SIZE);
		setMaxSize(DEFAULT_MAX_SIGNAL_SIZE);
		setMinCirc(INuclearSignalOptions.DEFAULT_MIN_CIRC);
		setMaxCirc(INuclearSignalOptions.DEFAULT_MAX_CIRC);
		setChannel(DEFAULT_CHANNEL);
		setThreshold(DEFAULT_SIGNAL_THRESHOLD);
		setScale(DEFAULT_SCALE);
		setBoolean(IDetectionOptions.IS_RGB, DEFAULT_IS_RGB);
		setBoolean(IDetectionOptions.IS_NORMALISE_CONTRAST, DEFAULT_IS_NORMALISE);
		
	}
	
	/**
	 * Construct from an existing nuclear options
	 * @param template
	 */
	public DefaultNuclearSignalHashOptions(INuclearSignalOptions template){
		super(template);
		this.setMaxFraction(template.getMaxFraction());
		this.mode        = template.getDetectionMode();
		
	}
	
	public DefaultNuclearSignalHashOptions setSize(double min, double max){
		super.setSize(min, max);
		return this;
	}
	
	public DefaultNuclearSignalHashOptions setCircularity(double min, double max){
		super.setCircularity(min, max);
		return this;
	}
	

	@Override
	public double getMaxFraction() {
		return getDouble(MAX_FRACTION);
	}

	@Override
	public SignalDetectionMode getDetectionMode() {
		return mode;
	}

	@Override
	public void setMaxFraction(double maxFraction) {
		setDouble(MAX_FRACTION, maxFraction);
		
	}

	@Override
	public void setDetectionMode(SignalDetectionMode detectionMode) {
		mode = detectionMode;
		
	}

	@Override
	public IMutableDetectionOptions duplicate() {
		return new DefaultNuclearSignalHashOptions(this) ;
	}
	
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = super.hashCode();
		
		long temp = Double.doubleToLongBits(this.getMaxFraction());
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + mode.hashCode();
		return result;
	}
	
	@Override
	public boolean equals(Object o){
		
		if(! super.equals(o))
			return false;
		
		if( ! (o instanceof INuclearSignalOptions))
			return false;
		
		INuclearSignalOptions other = (INuclearSignalOptions) o;
		
		if(Double.doubleToLongBits(getMaxFraction()) != 
				Double.doubleToLongBits(other.getMaxFraction()))
			return false;
		
		if(mode != other.getDetectionMode())
			return false;
		
		return true;
		
	}





}
