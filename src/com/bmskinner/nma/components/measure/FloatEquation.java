/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.components.measure;

import java.awt.geom.Point2D;
import java.util.List;

import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;

public class FloatEquation implements LineEquation {

	final float m, c;
	final boolean isVert;

	float xf; // fixed value vertical

	/**
	 * Constructor using gradient and intercept.
	 *
	 * @param m the gradient of the line
	 * @param c the y-intercept of the line
	 */
	public FloatEquation(final float m, final float c) {

		if (Float.valueOf(m) == null || Float.valueOf(c) == null) {
			throw new IllegalArgumentException("m or c is null");
		}
		if (Float.isInfinite(m) || Float.isInfinite(c)) {
			throw new IllegalArgumentException("m or c is infinite");
		}

		this.m = m;
		this.c = c;
		this.isVert = false;
	}

	/**
	 * Constructor using two points.
	 *
	 * @param a the first point
	 * @param b the second point
	 */
	public FloatEquation(IPoint a, IPoint b) {
		this(a.toPoint2D(), b.toPoint2D());
	}

	public FloatEquation(Point2D a, Point2D b) {

		if (a == null || b == null)
			throw new IllegalArgumentException("Point a or b is null");

		if (a.getX() == b.getX() && a.getY() == b.getY())
			throw new IllegalArgumentException("Point a and b are identical: " + a.toString());

		float aX = (float) a.getX();
		float bX = (float) b.getX();

		// y=mx+c
		float deltaX = aX - bX;
		float deltaY = (float) (a.getY() - b.getY());

		isVert = deltaX == 0;
		xf = aX;

		this.m = deltaY / deltaX;

		// y - y1 = m(x - x1)
		this.c = (float) (a.getY() - (m * aX));
	}

	@Override
	public double getX(double y) {
		// x = (y-c)/m
		return isVert ? xf : (y - c) / m;
	}

	@Override
	public double getY(double x) {
		return (this.m * x) + this.c;
	}

	@Override
	public double getM() {
		return this.m;
	}

	@Override
	public double getC() {
		return this.c;
	}

	@Override
	public boolean isOnLine(IPoint p) {
		return isVert ? Math.abs(p.getX() - xf) < 0.0000001
				: Math.abs(p.getY() - ((m * p.getX()) + c)) < .0000001;
	}

	@Override
	public IPoint getPointOnLine(IPoint p, double distance) {
		if (isVert)
			return new FloatPoint(p.getX(), p.getY() + distance);
		double xA = p.getX();

		/*
		 * d^2 = dx^2 + m.dx^2 // dy is a function of dx d^2 = (m^2+1)*dx^2 d^2 /
		 * (m^2+1) = dx^2 root( d^2 / (m^2+1)) = dx
		 */

		double dx = Math.sqrt(Math.pow(distance, 2) / (Math.pow(m, 2) + 1));

		double newX = distance > 0 ? xA + dx : xA - dx;
		double newY = this.getY(newX);
		return new FloatPoint(newX, newY);
	}

	@Override
	public LineEquation getPerpendicular(IPoint p) {

		if (isVert)
			return new FloatEquation(0, (float) p.getY());

		if ((int) p.getY() != (int) this.getY(p.getX())) {
			return new DoubleEquation(0, 0);
		}
		double pM = 0 - (1 / m); // invert and flip sign

		// find new c
		// y = pM.x + c
		// y -(pM.x) = c
		double pC = p.getY() - (pM * p.getX());
		return new DoubleEquation(pM, pC);
	}

	@Override
	public LineEquation translate(IPoint p) {
		if (isVert)
			return this;

		double oldY = this.getY(p.getX());
		double desiredY = p.getY();

		double dy = oldY - desiredY;
		double newC = this.c - dy;
		return new DoubleEquation(this.m, newC);

	}

	@Override
	public IPoint getIntercept(LineEquation eq) {
		// (this.m * x) + this.c = (eq.m * x) + eq.c

		// (this.m * x) - (eq.m * x) + this.c = eq.c
		// (this.m * x) - (eq.m * x) = eq.c - this.c
		// (this.m -eq.m) * x = eq.c - this.c
		// x = (eq.c - this.c) / (this.m -eq.m)

		double x = (eq.getC() - this.c) / (this.m - eq.getM());
		double y = this.getY(x);
		return new FloatPoint(x, y);
	}

	@Override
	public boolean intersects(DoubleEquation eq) {
		if (Math.abs(m - eq.m) < 0.000001) { // they are parallel
			return Math.abs(c - eq.c) < 0.000001;
		}
		return true;
	}

	/**
	 * Get the point that lies proportion of the way between points start and end
	 * 
	 * @param start
	 * @param end
	 * @param proportion
	 * @return
	 */
	public static IPoint getProportionalDistance(IPoint start, IPoint end, double proportion) {

		LineEquation eq = new DoubleEquation(start, end);
		double totalLength = start.getLengthTo(end);
		double propLength = totalLength * proportion;

		IPoint p = eq.getPointOnLine(start, propLength);

		// check direction of the line
		if (p.getLengthTo(end) > totalLength) {
			p = eq.getPointOnLine(start, -propLength);
		}

		return p;
	}

	@Override
	public double getClosestDistanceToPoint(IPoint p) {

		// translate the equation to p
		LineEquation tr = this.translate(p);

		// get the orthogonal line, which will intersect the original equation
		LineEquation orth = tr.getPerpendicular(p);

		// find the point of intercept
		IPoint intercept = this.getIntercept(orth);
		// IJ.log("Intercept: "+intercept.toString());

		// measure the distance between p and the intercept
		double distance = p.getLengthTo(intercept);
		return distance;
	}

	/**
	 * Find the best fit to the points using the least square method
	 * 
	 * @param points
	 * @return
	 */
	public static LineEquation calculateBestFitLine(List<IPoint> points) {

		// Find the means of x and y
		double xMean = 0;
		double yMean = 0;

		for (IPoint p : points) {
			xMean += p.getX();
			yMean += p.getY();
		}

		xMean /= points.size();
		yMean /= points.size();

		/*
		 * Find the slope of the line
		 * 
		 * m = sumof( (x - xMean) (y-yMean) ) -------------------------------- sumof( (x
		 * - xMean)^2 )
		 * 
		 */

		double sumDiffs = 0;
		double sumSquare = 0;

		for (IPoint p : points) {

			double x = p.getX() - xMean;
			double y = p.getY() - yMean;
			double x2 = x * x;
			sumDiffs += x * y;
			sumSquare += x2;
		}

		double m = sumDiffs / sumSquare;

		// Calculate the intercept: b = yMean - m*xMean
		double c = yMean - (m * xMean);

		// Return the equation
		LineEquation eq = new DoubleEquation(m, c);
		return eq;
	}

	/**
	 * Returns the equation as a string as y=mx+c
	 *
	 * @return The Equation of the line
	 */
	@Override
	public String toString() {
		return String.format("y = %f.x+%f", m, c);
	}

}
