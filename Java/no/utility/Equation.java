/*
	-----------------------
	EQUATION
	-----------------------
	Line equations
*/  
package no.utility;

import java.util.*;
import no.components.XYPoint;


public class Equation{

	double m;
	double c;

	public Equation(double m, double c){
		this.m = m;
		this.c = c;
	}

	public Equation (XYPoint a, XYPoint b){

		// y=mx+c
		double deltaX = a.getX() - b.getX();
		double deltaY = a.getY() - b.getY();
			
		this.m = deltaY / deltaX;
			
		// y - y1 = m(x - x1)
		this.c = a.getY() -  ( m * a.getX() );
	}

	public double getX(double y){
		// x = (y-c)/m
		return (y - this.c) / this.m;
	}

	public double getY(double x){
		return (this.m * x) + this.c;
	}

	/*
		Given a position on the line, return the position 
		a given distance away.
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

	public Equation getPerpendicular(XYPoint p){
		double pM = 0-(1/m); // invert and flip sign

		// find new c

		// y = pM.x + c
		// y -(pM.x) = c
		double pC = p.getY() - (pM * p.getX());

		return new Equation(pM, pC);
	}

	// translate the line to run through the given point,
	// keeping the gradient but moving the y intercept
	public Equation translate(XYPoint p ){

		double oldY = this.getY(p.getX());
		double desiredY = p.getY();

		double dy = oldY - desiredY;
		double newC = this.c - dy;
		return new Equation(this.m, newC);

	}

}