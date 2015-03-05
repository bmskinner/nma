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
}