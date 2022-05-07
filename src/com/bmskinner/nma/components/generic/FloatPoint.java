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
package com.bmskinner.nma.components.generic;

import java.awt.geom.Point2D;
import java.text.DecimalFormat;

import org.eclipse.jdt.annotation.NonNull;

/**
 * An extension to the Point2D.Float providing methods for calculating distances
 * between points implementing the {@link IPoint} interface.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class FloatPoint extends Point2D.Float implements IPoint {

	private static final long serialVersionUID = 1L;

	/**
	 * Construct from float values
	 * 
	 * @param x
	 * @param y
	 */
	public FloatPoint(float x, float y) {
		super(x, y);
	}

	/**
	 * Construct from double values. These will be converted to floats.
	 * 
	 * @param x
	 * @param y
	 */
	public FloatPoint(double x, double y) {
		super((float) x, (float) y);
	}

	/**
	 * Create from an existing point
	 * 
	 * @param p
	 */
	public FloatPoint(@NonNull FloatPoint p) {
		super(p.x, p.y);
	}

	/**
	 * Create from an existing point
	 * 
	 * @param p
	 */
	public FloatPoint(@NonNull IPoint p) {
		this(p.getX(), p.getY());
	}

	/**
	 * Create from an existing point
	 * 
	 * @param p
	 */
	public FloatPoint(@NonNull final Point2D a) {
		this(a.getX(), a.getY());
	}

	@Override
	public IPoint duplicate() {
		return new FloatPoint(x, y);
	}

	@Override
	public int getXAsInt() {
		return Math.round(x);
	}

	@Override
	public int getYAsInt() {
		return Math.round(y);
	}

	@Override
	public void setX(double x) {
		this.x = (float) x;
	}

	@Override
	public void setY(double y) {
		this.y = (float) y;
	}

	@Override
	public void set(@NonNull IPoint p) {
		this.x = (float) p.getX();
		this.y = (float) p.getY();
	}

	@Override
	public void set(@NonNull Point2D p) {
		this.x = (float) p.getX();
		this.y = (float) p.getY();
	}

	private double getLengthTo(final FloatPoint a) {
		// a2 = b2 + c2
		double dx = x - a.x;
		double dy = y - a.y;
		double dx2 = dx * dx;
		double dy2 = dy * dy;
		return Math.sqrt(dx2 + dy2);
	}

	@Override
	public double getLengthTo(@NonNull final IPoint a) {

		if (a instanceof FloatPoint p)
			return getLengthTo(p);

		// a2 = b2 + c2
		double dx = x - a.getX();
		double dy = y - a.getY();
		double dx2 = dx * dx;
		double dy2 = dy * dy;
		return Math.sqrt(dx2 + dy2);
	}

	@Override
	public boolean overlaps(@NonNull final IPoint a) {
		return (this.getXAsInt() == a.getXAsInt() && this.getYAsInt() == a.getYAsInt());
	}

	@Override
	public boolean isAbove(@NonNull IPoint p) {
		return y > p.getY();
	}

	@Override
	public boolean isBelow(@NonNull IPoint p) {
		return y < p.getY();
	}

	@Override
	public boolean isLeftOf(@NonNull IPoint p) {
		return x < p.getX();
	}

	@Override
	public boolean isRightOf(@NonNull IPoint p) {
		return x > p.getX();
	}

	@Override
	public void offset(double x, double y) {
		this.x += x;
		this.y += y;
	}

	public boolean overlapsPerfectly(@NonNull final FloatPoint a) {
		return (this.x == a.x && this.y == a.y);
	}

	@Override
	public boolean overlapsPerfectly(@NonNull final IPoint a) {
		if (a instanceof FloatPoint)
			return overlapsPerfectly((FloatPoint) a);
		return (this.getX() == a.getX() && this.getY() == a.getY());
	}

	@Override
	public String toString() {
		DecimalFormat df = new DecimalFormat("#0.####");
		return df.format(getX()) + " - " + df.format(getY());
	}

	@Override
	public Point2D toPoint2D() {
		return this;
	}

	@Override
	public double findSmallestAngle(@NonNull IPoint a, @NonNull IPoint c) {

		if (a instanceof FloatPoint && c instanceof FloatPoint)
			return findSmallestAngle((FloatPoint) a, (FloatPoint) c);

		/*
		 * Test of rotation and comparison to a horizontal axis From
		 * http://stackoverflow.com/questions/3486172/angle-between-3-points
		 * 
		 * The vectors are rotated so one is on the xaxis, at which point atan2 does the
		 * rest
		 */

		IPoint ab = new FloatPoint(x - a.getX(), y - a.getY());
		IPoint cb = new FloatPoint(x - c.getX(), y - c.getY());

		double dot = (ab.getX() * cb.getX() + ab.getY() * cb.getY()); // dot product
		double cross = (ab.getX() * cb.getY() - ab.getY() * cb.getX()); // cross product

		double alpha = Math.atan2(cross, dot);

		return Math.abs(alpha * 180 / Math.PI);
	}

	private double findSmallestAngle(@NonNull FloatPoint a, @NonNull FloatPoint c) {

		float abx = x - a.x;
		float aby = y - a.y;
		float cbx = x - c.x;
		float cby = y - c.y;

		double dot = (abx * cbx + aby * cby); // dot product
		double cross = (abx * cby - aby * cbx); // cross product
		double alpha = Math.atan2(cross, dot);
		return Math.abs(alpha * 180 / Math.PI);
	}

	@Override
	public double findAbsoluteAngle(@NonNull IPoint start, @NonNull IPoint end) {

		IPoint ab = new FloatPoint(x - start.getX(), y - start.getY());
		IPoint cb = new FloatPoint(x - end.getX(), y - end.getY());

		double dot = (ab.getX() * cb.getX() + ab.getY() * cb.getY()); // dot product
		double cross = (ab.getX() * cb.getY() - ab.getY() * cb.getX()); // cross product

		double alpha = Math.atan2(cross, dot);

		double angle = alpha * 180 / Math.PI;

		double neg = 0 - angle;

		return (neg + 360) % 360;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		// Note that since we override Point2D.Float
		// value testing is handled here
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

	@Override
	public IPoint minus(@NonNull IPoint p) {
		return new FloatPoint(x - p.getX(), y - p.getY());
	}

	@Override
	public IPoint plus(@NonNull IPoint p) {
		return new FloatPoint(x + p.getX(), y + p.getY());
	}

	@Override
	public IPoint minus(double value) {
		return new FloatPoint(x - value, y - value);
	}

	@Override
	public IPoint plus(double value) {
		return new FloatPoint(x + value, y + value);
	}

	@Override
	public IPoint multiply(double value) {
		return new FloatPoint(x * value, y * value);
	}

	@Override
	public IPoint divide(double value) {
		return new FloatPoint(x / value, y / value);
	}

}
