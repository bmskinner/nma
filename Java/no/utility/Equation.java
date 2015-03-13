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
			the equation is (xA - xB)^2 + (yA - yB)^2 = distance^2
			y=mx+c
			(xA - xB)^2 + ((m*xA+c) - (m*xB+c))^2 = distance^2
			xA^2 - 2*xA*xB + xb^2  + ((m*xA+c) - (m*xB+c))^2 = distance^2
			xA^2 - 2*xA*xB + xb^2  + ((m*xA) - (m*xB))^2 = distance^2
			xA^2 - 2*xA*xB + xb^2  + ( (m*xA)^2 - 2*(m*xA)*(m*xB) + (m*xB)^2   ) = distance^2
			xA^2 - 2*xA*xB + xb^2  + ( (m^2*xA^2) - 2*(m*xA)*(m*xB) + (m^2*xB^2)   ) = distance^2
			( (m^2+1*xA^2) - (2m^2+2)*(xA)*(xB) + (m^2+1*xB^2)   ) = distance^2
			( (m^2+1*xA^2) - (2m^2+2)*(xA)*(xB) + (m^2+1*xB^2)   ) - distance^2 = 0

			Quadratic: ax^2 + bx + c
			a = (m^2+1*xB^2)  
			b = -(2m^2+2)*(xA)*(xB)
			c = ( (m^2+1*xA^2) - distance^2

		*/
	
		// get the parameters of the quadratic equation
		double eqa = (Math.pow(m,2)+1);
		double eqb = -(Math.pow(2*m,2)+2)*xA;
		double eqc = ((Math.pow(m,2)+1) *Math.pow(xA,2)) - Math.pow(distance,2)  ;

		// solve the quadratic
		double xB1 = (0-eqb)+(Math.sqrt( ((Math.pow(eqb,2)) - (4*eqa*eqc))/2*eqa ));
		double xB2 = (0-eqb)-(Math.sqrt( ((Math.pow(eqb,2)) - (4*eqa*eqc))/2*eqa ));

		double xB = 0;
		if(distance>0){
		 	xB = Math.max(xB1, xB2);
		 } else {
		 	xB = Math.min(xB1, xB2);
		 }

		double yB = this.getY(xB);
		XYPoint result = new XYPoint(xB, yB);
		return result;
	
  	}

}