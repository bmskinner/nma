package com.bmskinner.nuclear_morphology.components.options;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

/**
 * A hash based options for nucleus detection settings
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultNucleusHashDetectionOptions 
	extends AbstractHashDetectionOptions {

	public static final int    DEFAULT_MIN_NUCLEUS_SIZE = 2000;
	public static final int    DEFAULT_MAX_NUCLEUS_SIZE = 10000;
	public static final double DEFAULT_MIN_NUCLEUS_CIRC = 0.2;
	public static final double DEFAULT_MAX_NUCLEUS_CIRC = 0.8;
	public static final int    DEFAULT_NUCLEUS_THRESHOLD = 36;
	private static final double    DEFAULT_SCALE = 1.0;
	private static final int    DEFAULT_CHANNEL      = 2;
	private static final boolean DEFAULT_NORMALISE_CONTRAST = false;
	private static final boolean DEFAULT_IS_RGB = false;
	private static final long serialVersionUID = 1L;
	
	/**
	 * Construct for a folder with default values
	 * @param folder the image folder
	 */
	public DefaultNucleusHashDetectionOptions(File folder){
		super(folder);
		
		this.setSize(DEFAULT_MIN_NUCLEUS_SIZE, DEFAULT_MAX_NUCLEUS_SIZE);
		this.setCircularity(DEFAULT_MIN_NUCLEUS_CIRC, DEFAULT_MAX_NUCLEUS_CIRC);
		this.setThreshold(DEFAULT_NUCLEUS_THRESHOLD);
		this.setScale(DEFAULT_SCALE);
		this.setChannel(DEFAULT_CHANNEL);
		this.setNormaliseContrast(DEFAULT_NORMALISE_CONTRAST);
		this.setRGB(DEFAULT_IS_RGB);
		
		this.setCannyOptions( OptionsFactory.makeCannyOptions());
		this.setHoughOptions( OptionsFactory.makeHoughOptions());
		this.setSubOptions(  IDetectionSubOptions.BACKGROUND_OPTIONS, OptionsFactory.makePreprocessingOptions());
	}
	
	public DefaultNucleusHashDetectionOptions(IDetectionOptions template){
		super(template);
		
	}
	
	public IMutableDetectionOptions unlock(){
		return this;
	}
	
	public IDetectionOptions lock(){
		return this;
	}
	
	@Override
	public DefaultNucleusHashDetectionOptions setSize(double min, double max){
		super.setSize(min, max);
		return this;
	}
	
	@Override
	public DefaultNucleusHashDetectionOptions setCircularity(double min, double max){
		super.setCircularity(min, max);
		return this;
	}
	

	@Override
	public boolean isValid(CellularComponent c) {
		if(c instanceof Nucleus){
			return super.isValid(c);
		} else {
			return false;
		}
	}

	@Override
	public IMutableDetectionOptions duplicate() {
		return new DefaultNucleusHashDetectionOptions(this);
	}
}
