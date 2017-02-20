package com.bmskinner.nuclear_morphology.components.options;

import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions.IMutableHoughDetectionOptions;

/**
 * The default implementation of the IHoughDetectionOptions
 * interface.
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultHoughOptions 
	extends AbstractHashOptions
	implements IMutableHoughDetectionOptions {

	private static final long serialVersionUID = 1L;
	public static final int DEFAULT_MIN_RADIUS = 5;
	public static final int DEFAULT_MAX_RADIUS = 30;
	public static final int DEFAULT_NUM_CIRCLES = 0;
	public static final int DEFAULT_HOUGH_THRESHOLD = 20;
	
	/**
	 * Construct with default options
	 */
	public DefaultHoughOptions(){
		this.setMinRadius(DEFAULT_MIN_RADIUS);
		this.setMaxRadius(DEFAULT_MAX_RADIUS);
		this.setNumberOfCircles(DEFAULT_NUM_CIRCLES);
		this.setHoughThreshold(DEFAULT_HOUGH_THRESHOLD);
	}
	
	/**
	 * Construct using a template
	 * @param template
	 */
	public DefaultHoughOptions(IHoughDetectionOptions template){
		this.setMinRadius(template.getMinRadius());
		this.setMaxRadius(template.getMaxRadius());
		this.setNumberOfCircles(template.getNumberOfCircles());
		this.setHoughThreshold(template.getHoughThreshold());
	}
	
	@Override
	public int getMinRadius() {
		return getInt(MIN_RADIUS);
	}
	
	public void setMinRadius(int i){
		setInt(MIN_RADIUS, i);
	}

	@Override
	public int getMaxRadius() {
		return getInt(MAX_RADIUS);
	}
	
	public void setMaxRadius(int i ){
		setInt(MAX_RADIUS, i);
	}

	@Override
	public int getNumberOfCircles() {
		return getInt(NUM_CIRCLES);
	}
	
	public void setNumberOfCircles(int i){
		setInt(NUM_CIRCLES, i);
	}

	@Override
	public IHoughDetectionOptions lock() {
		return this;
	}

	@Override
	public int getHoughThreshold() {
		return getInt(HOUGH_THRESHOLD);

	}

	@Override
	public void setHoughThreshold(int i) {
		setInt(HOUGH_THRESHOLD, i);
		
	}

	@Override
	public IMutableHoughDetectionOptions unlock() {
		return this;
	}
	
	public String toString(){
		
		return "Hough options:"+IDetectionOptions.NEWLINE + super.toString();		
	}

}
