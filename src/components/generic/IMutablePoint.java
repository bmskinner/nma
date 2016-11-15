package components.generic;

import components.active.generic.FloatPoint;

/**
 * This extension to the IPoint interface adds setters and
 * the ability to alter the point data. Used to keep the IPoint
 * immutable by default.
 * @author ben
 * @since 1.13.3
 *
 */
public interface IMutablePoint extends IPoint {
	
	
	/**
	 * Create a new point of the default type
	 * @param x the x position
	 * @param y the y position
	 * @return a point at the specified position
	 */
	static IMutablePoint makeNew(float x, float y){
		return new FloatPoint(x,y);
	}
	
	/**
	 * Create a new point of the default type
	 * @param x the x position
	 * @param y the y position
	 * @return a point at the specified position
	 */
	static IMutablePoint makeNew(double x, double y){
		return makeNew( (float) x, (float) y);
	}
	
	/**
	 * Create a new point of the default type
	 * based on the given point
	 * @param x the x position
	 * @param y the y position
	 * @return a point at the specified position
	 */
	static IMutablePoint makeNew(IPoint a){
		return makeNew( a.getX(), a.getY());
	}
	
	/**
	 * Set the x-value
	 *
	 * @param x the new x-value
	 */
	void setX(double x);

	/**
	 * Set the y-value
	 *
	 * @param y the new x-value
	 */
	void setY(double y);

	/**
	 * Set the point to the position in the given point
	 * @param p the position to move this point to
	 */
	void set(IPoint p);
	
	/**
	 * Offset the point by the given amounts
	 * @param x the x offset
	 * @param y the y offset
	 */
	void offset(double x, double y);

}
