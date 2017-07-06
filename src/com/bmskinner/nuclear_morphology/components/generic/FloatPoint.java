/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.generic;

import ij.IJ;
import ij.Prefs;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;

import java.awt.geom.Point2D;

/**
 * An extension to the Point2D.Float providing methods for calculating distances
 * between points implementing the {@link IPoint} interface.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class FloatPoint extends Point2D.Float implements IMutablePoint {

    private static final long serialVersionUID = 1L;

    public FloatPoint(float x, float y) {
        super(x, y);
    }

    public FloatPoint(double x, double y) {
        super((float) x, (float) y);
    }

    public FloatPoint(IPoint p) {
        super((float) p.getX(), (float) p.getY());
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#getXAsInt()
     */
    @Override
    public int getXAsInt() {
        return (int) Math.round(x);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#getYAsInt()
     */
    @Override
    public int getYAsInt() {
        return (int) Math.round(y);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#setX(double)
     */
    @Override
    public void setX(double x) {
        this.x = (float) x;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#setY(double)
     */
    @Override
    public void setY(double y) {
        this.y = (float) y;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#set(components.generic.XYPoint)
     */
    @Override
    public void set(IPoint p) {
        this.x = (float) p.getX();
        this.y = (float) p.getY();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#getLengthTo(components.generic.IPoint)
     */
    @Override
    public double getLengthTo(final IPoint a) {

        if (a == null) {
            throw new IllegalArgumentException("Destination point is null");
        }

        // a2 = b2 + c2
        double dx = Math.abs(this.getX() - a.getX());
        double dy = Math.abs(this.getY() - a.getY());
        double dx2 = dx * dx;
        double dy2 = dy * dy;
        double length = Math.sqrt(dx2 + dy2);
        return length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#overlaps(components.generic.IPoint)
     */
    @Override
    public boolean overlaps(final IPoint a) {

        if (a == null) {
            throw new IllegalArgumentException("Destination point is null");
        }

        if (this.getXAsInt() == a.getXAsInt() && this.getYAsInt() == a.getYAsInt()) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#isAbove(components.generic.XYPoint)
     */
    @Override
    public boolean isAbove(IPoint p) {
        return y > p.getY();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#isBelow(components.generic.XYPoint)
     */
    @Override
    public boolean isBelow(IPoint p) {
        return y < p.getY();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#isLeftOf(components.generic.XYPoint)
     */
    @Override
    public boolean isLeftOf(IPoint p) {
        return x < p.getX();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#isRightOf(components.generic.XYPoint)
     */
    @Override
    public boolean isRightOf(IPoint p) {
        return x > p.getX();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#offset(double, double)
     */
    @Override
    public void offset(double x, double y) {
        this.setX(this.getX() + x);
        this.setY(this.getY() + y);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IPoint#overlapsPerfectly(components.generic.IPoint)
     */
    @Override
    public boolean overlapsPerfectly(final IPoint a) {

        if (a == null) {
            throw new IllegalArgumentException("Destination point is null");
        }

        if (this.getX() == a.getX() && this.getY() == a.getY()) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#toString()
     */
    @Override
    public String toString() {
        return this.getXAsInt() + "-" + this.getYAsInt();
    }

    @Override
    public Point2D toPoint2D() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#findAngle(components.generic.IPoint,
     * components.generic.IPoint)
     */
    @Override
    public double findAngle(IPoint a, IPoint c) {

        if (a == null || c == null) {
            throw new IllegalArgumentException("An input point is null in angle finding");
        }

        /*
         * Test of rotation and comparison to a horizontal axis From
         * http://stackoverflow.com/questions/3486172/angle-between-3-points
         */

        IPoint ab = IPoint.makeNew(x - a.getX(), y - a.getY());
        IPoint cb = IPoint.makeNew(x - c.getX(), y - c.getY());

        double dot = (ab.getX() * cb.getX() + ab.getY() * cb.getY()); // dot
                                                                      // product
        double cross = (ab.getX() * cb.getY() - ab.getY() * cb.getX()); // cross
                                                                        // product

        double alpha = Math.atan2(cross, dot);

        return Math.abs(alpha * 180 / Math.PI);

        /*
         * Copy of ImageJ angle code from ij.gui.PolygonRoi#getAngleAsString()
         */

        // float[] xpoints = { (float) a.getX(), (float) getX(), (float)
        // b.getX()};
        // float[] ypoints = { (float) a.getY(), (float) getY(), (float)
        // b.getY()};
        //
        // double angle1 = 0.0;
        // double angle2 = 0.0;
        //
        // angle1 = getFloatAngle(xpoints[0], ypoints[0], xpoints[1],
        // ypoints[1]);
        // angle2 = getFloatAngle(xpoints[1], ypoints[1], xpoints[2],
        // ypoints[2]);
        //
        // double degrees = Math.abs(180-Math.abs(angle1-angle2));
        // if (degrees>180.0)
        // degrees = 360.0-degrees;
        //
        // return degrees;

        /*
         * Test code - not working
         */

        // Use the cosine rule: a-b^2 = this-b^2 + this-a^2 - 2 * this-b *
        // this-a * cos (theta)

        // double ab = a.getLengthTo(b);
        // double bc = getLengthTo(b);
        // double ac = getLengthTo(a);
        //
        // double ab2cosT = Math.pow(bc,2) + Math.pow(ac,2) - Math.pow(ab,2);
        //
        // double cosT = ab2cosT / (2 * ac * bc);
        //
        // double t = Math.acos(cosT);
        // return Math.toDegrees(t);

        /*
         * OLD CODE - WORKING
         */

        // float[] xpoints = { (float) a.getX(), (float) getX(), (float)
        // b.getX()};
        // float[] ypoints = { (float) a.getY(), (float) getY(), (float)
        // b.getY()};
        // PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);
        // return roi.getAngle();
    }

    /**
     * Get the angle in degrees between the specified line and a horizontal
     * line. Copied from ij.gui.Roi#getFloatAngle()
     * 
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    private double getFloatAngle(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y1 - y2;
        return (180.0 / Math.PI) * Math.atan2(dy, dx);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#hashCode()
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FloatPoint other = (FloatPoint) obj;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        return true;
    }

	@Override
	public IPoint minus(IPoint p) {
		return new FloatPoint(x-p.getX(), y-p.getY());
	}

	@Override
	public IPoint plus(IPoint p) {
		return new FloatPoint(x+p.getX(), y+p.getY());
	}
	
	@Override
	public IPoint minus(double value) {
		return new FloatPoint(x-value, y-value);
	}

	@Override
	public IPoint plus(double value) {
		return new FloatPoint(x+value, y+value);
	}

}
