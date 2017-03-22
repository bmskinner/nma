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
package com.bmskinner.nuclear_morphology.components.generic;

import java.awt.geom.Point2D;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;

/**
 * Describes line equations of the form y=m*x + c
 * @author bms41
 *
 */
public class DoubleEquation implements LineEquation {

	final double m, c;

	/**
	*	Constructor using gradient and intercept. 
	*
	* @param m the gradient of the line
	* @param c the y-intercept of the line
	* @return An Equation describing the line
	*/
	public DoubleEquation(final double m, final double c){
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
	public DoubleEquation (IPoint a, IPoint b){

		this(a.toPoint2D(), b.toPoint2D());
	}
	
	/**
	*	Constructor using two Points. 
	*
	* @param a the first XYPoint
	* @param b the second XYPoint
	* @return An Equation describing the line between the points
	*/
	public DoubleEquation (Point2D a, Point2D b){

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

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.generic.Equation#getX(double)
	 */
	@Override
	public double getX(double y){
		// x = (y-c)/m
		return (y - this.c) / this.m;
	}

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.generic.Equation#getY(double)
	 */
	@Override
	public double getY(double x){
		return (this.m * x) + this.c;
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.generic.Equation#getM()
	 */
	@Override
	public double getM(){
		return this.m;
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.generic.Equation#getC()
	 */
	@Override
	public double getC(){
		return this.c;
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.generic.Equation#isOnLine(com.bmskinner.nuclear_morphology.components.generic.IPoint)
	 */
	@Override
	public boolean isOnLine(IPoint p){
		return p.getY() == (   (m * p.getX())    +c);
	}

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.generic.Equation#getPointOnLine(com.bmskinner.nuclear_morphology.components.generic.IPoint, double)
	 */
	@Override
	public IPoint getPointOnLine(IPoint p, double distance){

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
		return IPoint.makeNew(newX, newY);	
	}

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.generic.Equation#getPerpendicular(com.bmskinner.nuclear_morphology.components.generic.IPoint)
	 */
	@Override
	public LineEquation getPerpendicular(IPoint p){

		if((int)p.getY()!=(int)this.getY(p.getX())){
			return new DoubleEquation(0,0);
		}
		double pM = 0-(1/m); // invert and flip sign

		// find new c
		// y = pM.x + c
		// y -(pM.x) = c
		double pC = p.getY() - (pM * p.getX());
		return new DoubleEquation(pM, pC);
	}

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.generic.Equation#translate(com.bmskinner.nuclear_morphology.components.generic.IPoint)
	 */ 
	@Override
	public LineEquation translate(IPoint p ){

		double oldY = this.getY(p.getX());
		double desiredY = p.getY();

		double dy = oldY - desiredY;
		double newC = this.c - dy;
		return new DoubleEquation(this.m, newC);

	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.generic.Equation#getIntercept(com.bmskinner.nuclear_morphology.components.generic.Equation)
	 */
	@Override
	public IPoint getIntercept(LineEquation eq){
		// (this.m * x) + this.c = (eq.m * x) + eq.c
		
		// (this.m * x) - (eq.m * x) + this.c = eq.c
		// (this.m * x) - (eq.m * x)  = eq.c - this.c
		// (this.m -eq.m) * x  = eq.c - this.c
		// x  = (eq.c - this.c) / (this.m -eq.m)
		
		double x = (eq.getC() - this.c) / (this.m - eq.getM());
		double y = this.getY(x);
		return IPoint.makeNew(x, y);
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.generic.Equation#intersects(com.bmskinner.nuclear_morphology.components.generic.DoubleEquation)
	 */
	@Override
	public boolean intersects(DoubleEquation eq){
		
		if(Math.abs(m - eq.m) < 0.000001){// they are parallel within the bounds of FP calculations
			return c==eq.c; 
		}
		return true;
	}
	
	/**
	 * Get the point that lies proportion of the way between points start and end
	 * @param start
	 * @param end
	 * @param proportion
	 * @return
	 */
	public static IPoint getProportionalDistance(IPoint start, IPoint end, double proportion){
				
		LineEquation eq = new DoubleEquation(start, end);
		double totalLength = start.getLengthTo(end);
		double propLength  = totalLength * proportion;
		
		IPoint p = eq.getPointOnLine(start, propLength);
		
		// check direction of the line
		if(p.getLengthTo(end) > totalLength){
			p = eq.getPointOnLine(start, -propLength);
		}
		
		return p;
	}
	
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.components.generic.Equation#getClosestDistanceToPoint(com.bmskinner.nuclear_morphology.components.generic.IPoint)
	 */
	@Override
	public double getClosestDistanceToPoint(IPoint p){
		
		// translate the equation to p
		LineEquation tr = this.translate(p);
		
		// get the orthogonal line, which will intersect the original equation
		LineEquation orth = tr.getPerpendicular(p);
		
		// find the point of intercept
		IPoint intercept = this.getIntercept(orth);
//		IJ.log("Intercept: "+intercept.toString());
		
		// measure the distance between p and the intercept
		double distance = p.getLengthTo(intercept);
		return distance;
	}
	

	/**
	 * Find the best fit to the points using the least square
	 * method
	 * @param points
	 * @return
	 */
	public static LineEquation calculateBestFitLine(List<IBorderPoint> points){
		
		// Find the means of x and y
		double xMean = 0;
		double yMean = 0;
		
		for(IBorderPoint p : points){
			xMean += p.getX();
			yMean += p.getY();
		}
		
		xMean /= points.size();
		yMean /= points.size();
		
		/*
		 *  Find the slope of the line
		
		m = sumof(  (x - xMean) (y-yMean)  )
			--------------------------------
			sumof(  (x - xMean)^2 )
		
		 */
		
		double sumDiffs = 0;
		double sumSquare = 0;
		
		for(IBorderPoint p : points){
			
			double x = p.getX() - xMean;
			double y = p.getY() - yMean;
			double x2 = x*x;
			sumDiffs  += x*y;
			sumSquare += x2;
		}
		
		double m = sumDiffs / sumSquare;
		
		// Calculate the intercept: b = yMean - m*xMean
		double c = yMean - (  m * xMean);
		
		// Return the equation
		LineEquation eq = new DoubleEquation(m, c);
		return eq;
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