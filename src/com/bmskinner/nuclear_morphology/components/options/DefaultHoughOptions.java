package com.bmskinner.nuclear_morphology.components.options;

/**
 * The default implementation of the IHoughDetectionOptions
 * interface.
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultHoughOptions 
	extends AbstractHashOptions
	implements IHoughDetectionOptions {

	public static final double DEFAULT_MIN_RADIUS = 5;
	public static final double DEFAULT_MAX_RADIUS = 50;
	public static final int    DEFAULT_NUM_CIRCLES = 6;
	
	public DefaultHoughOptions(){
		this.setMinRadius(DEFAULT_MIN_RADIUS);
		this.setMaxRadius(DEFAULT_MAX_RADIUS);
		this.setNumberOfCircles(DEFAULT_NUM_CIRCLES);
	}
	
	public DefaultHoughOptions(IHoughDetectionOptions template){
		this.setMinRadius(template.getMinRadius());
		this.setMaxRadius(template.getMaxRadius());
		this.setNumberOfCircles(template.getNumberOfCircles());
	}
	
	@Override
	public double getMinRadius() {
		return dblMap.get(MIN_RADIUS);
	}
	
	public void setMinRadius(double d){
		setDouble(MIN_RADIUS, d);
	}

	@Override
	public double getMaxRadius() {
		return dblMap.get(MAX_RADIUS);
	}
	
	public void setMaxRadius(double d){
		setDouble(MAX_RADIUS, d);
	}

	@Override
	public int getNumberOfCircles() {
		return intMap.get(NUM_CIRCLES);
	}
	
	public void setNumberOfCircles(int i){
		setInt(NUM_CIRCLES, i);
	}

}
