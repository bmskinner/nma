/*
	-----------------------
	EQUATION
	-----------------------
	Line equations
*/  
package utility;

import no.components.XYPoint;


public class Equation{

	double m;
	double c;

	/**
	*	Constructor using gradient and intercept. 
	*
	* @param m the gradient of the line
	* @param c the y-intercept of the line
	* @return An Equation describing the line
	*/
	public Equation(double m, double c){
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
	* Returns the equation as a string as y=mx+c
	*
	* @return The Equation of the line
	*/ 
	public String print(){
		return "y="+m+".x+"+c;
	}

}