package components.generic;

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
/*
	-----------------------
	EQUATION
	-----------------------
	Line equations
*/

public class Equation{

	final double m;
	final double c;

	/**
	*	Constructor using gradient and intercept. 
	*
	* @param m the gradient of the line
	* @param c the y-intercept of the line
	* @return An Equation describing the line
	*/
	public Equation(final double m, final double c){
		if(Double.valueOf(m)==null || Double.valueOf(c)==null){
			throw new IllegalArgumentException("m or c is null");
		}
		this.m = m;
		this.c = c;
	}

	/**
	*	Constructor using two XYPoints. 
	*
	* @param a the first XYPoint
	* @param b the second XYPoint
	* @return An Equation describing the line between the points
	*/
	public Equation (XYPoint a, XYPoint b){

		if(a==null || b==null){
			throw new IllegalArgumentException("Point a or b is null");
		}
		// y=mx+c
		double deltaX = a.getX() - b.getX();
		double deltaY = a.getY() - b.getY();
			
		this.m = deltaY / deltaX;
			
		// y - y1 = m(x - x1)
		this.c = a.getY() -  ( m * a.getX() );
	}

	/**
	*	Returns the x value for a given y value
	*
	* @param y the y value on the line
	* @return The x value at the given y value
	*/
	public double getX(double y){
		// x = (y-c)/m
		return (y - this.c) / this.m;
	}

	/**
	*	Returns the y value for a given x value
	*
	* @param x the x value on the line
	* @return The y value at the given x value
	*/
	public double getY(double x){
		return (this.m * x) + this.c;
	}
	
	public double getM(){
		return this.m;
	}
	
	public double getC(){
		return this.c;
	}

	/**
	*	Returns a point a given distance away from a given point
	* on the line specified by this Equation.
	*
	* @param p	the reference point to measure from
	* @param distance	the distance along the line from the point p
	* @return The position <distance> away from <p>
	*/
	public XYPoint getPointOnLine(XYPoint p, double distance){

		double xA = p.getX();

		/*
			d^2 = dx^2 + m.dx^2 // dy is a function of dx
			d^2 = (m^2+1)*dx^2
			d^2 / (m^2+1) = dx^2
			root( d^2 / (m^2+1)) = dx
		*/

		double dx = Math.sqrt( Math.pow(distance,2) / (Math.pow(m,2)+1)  );

		double newX  = distance>0 ? xA + dx : xA - dx;
		double newY = this.getY(newX);
		return new XYPoint(newX, newY);	
	}

	/**
	* Finds the line equation perpendicular to this line,
	* at the given point. The point p must lie on the line;
	* if the y-value for XYPoint p's x does not lie on the line
	* to int precision, an empty Equation will be returned.
	*
	* @param p	the XYPoint to measure from
	* @return The Equation of the perpendicular line
	*/
	public Equation getPerpendicular(XYPoint p){

		if((int)p.getY()!=(int)this.getY(p.getX())){
			return new Equation(0,0);
		}
		double pM = 0-(1/m); // invert and flip sign

		// find new c
		// y = pM.x + c
		// y -(pM.x) = c
		double pC = p.getY() - (pM * p.getX());
		return new Equation(pM, pC);
	}

	/**
	* Translates the line to run through the given point,
	* keeping the gradient but moving the y intercept.
	*
	* @param p	the XYPoint to intercept
	* @return The Equation of the translated line
	*/ 
	public Equation translate(XYPoint p ){

		double oldY = this.getY(p.getX());
		double desiredY = p.getY();

		double dy = oldY - desiredY;
		double newC = this.c - dy;
		return new Equation(this.m, newC);

	}
	
	/**
	 * Find the intercept between this equation and another
	 * @param eq
	 * @return
	 */
	public XYPoint getIntercept(Equation eq){
		// (this.m * x) + this.c = (eq.m * x) + eq.c
		
		// (this.m * x) - (eq.m * x) + this.c = eq.c
		// (this.m * x) - (eq.m * x)  = eq.c - this.c
		// (this.m -eq.m) * x  = eq.c - this.c
		// x  = (eq.c - this.c) / (this.m -eq.m)
		
		double x = (eq.getC() - this.c) / (this.m - eq.getM());
		double y = this.getY(x);
		return new XYPoint(x, y);
	}
	
	public boolean intersects(Equation eq){
		if(m == eq.m){ // they are parallel
			return c==eq.c; 
		}
		return true;
	}
	
	
	/**
	 * Find the smallest distance from a given point to the line
	 * @param p
	 * @return
	 */
	public double getClosestDistanceToPoint(XYPoint p){
		
		// translate the equation to p
		Equation tr = this.translate(p);
		
		// get the orthogonal line, which will intersect the original equation
		Equation orth = tr.getPerpendicular(p);
		
		// find the point of intercept
		XYPoint intercept = this.getIntercept(orth);
//		IJ.log("Intercept: "+intercept.toString());
		
		// measure the distance between p and the intercept
		double distance = p.getLengthTo(intercept);
		return distance;
	}

	/**
	* Returns the equation as a string as y=mx+c
	*
	* @return The Equation of the line
	*/ 
	public String toString(){
		return "y="+m+".x+"+c;
	}

}