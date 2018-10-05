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
/*
  -----------------------
  XY POINT CLASS
  -----------------------
  This class contains the X and Y coordinates of a point as doubles.
  Also contains methods for determining distance and overlap with other points
 */

package com.bmskinner.nuclear_morphology.components.generic;

import java.awt.geom.Point2D;
import java.io.Serializable;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

@Deprecated
public class XYPoint implements Serializable, Loggable, IPoint {

    private static final long serialVersionUID = 1L;
    protected double          x;
    protected double          y;

    /**
     * Constructor using doubles.
     *
     * @param x
     *            the x-coordinate
     * @param y
     *            the y-coordinate
     * @return An XYPoint at these coordinates
     */
    public XYPoint(final double x, final double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Constructor using XYPoint. Copies the x and y coordinates from the given
     * point
     *
     * @param p
     *            the XYPoint
     * @return An XYPoint at these coordinates
     */
    public XYPoint(final IPoint p) {
        this.x = p.getX();
        this.y = p.getY();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#getX()
     */
    @Override
    public double getX() {
        return this.x;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#getY()
     */
    @Override
    public double getY() {
        return this.y;
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
        this.x = x;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#setY(double)
     */
    @Override
    public void setY(double y) {
        this.y = y;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#set(components.generic.XYPoint)
     */
    @Override
    public void set(IPoint p) {
        this.x = p.getX();
        this.y = p.getY();
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
        // return new XYPoint(this.x+x, this.y+y);
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
        return this.getXAsInt() + "," + this.getYAsInt();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#asPoint()
     */
    @Override
    public Point2D toPoint2D() {
        return new Point2D.Double(x, y);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IPoint#findAngle(components.generic.IPoint,
     * components.generic.IPoint)
     */
    @Override
    public double findSmallestAngle(IPoint a, IPoint b) {
        float[] xpoints = { (float) a.getX(), (float) getX(), (float) b.getX() };
        float[] ypoints = { (float) a.getY(), (float) getY(), (float) b.getY() };
        PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);
        return roi.getAngle();
    }
    
    @Override
    public double findAbsoluteAngle(@NonNull IPoint start, @NonNull IPoint end) {

        if (start == null || end == null)
            throw new IllegalArgumentException("Input points cannot be null for angle calculation");
        IPoint ab = IPoint.makeNew(x - start.getX(), y - start.getY());
        IPoint cb = IPoint.makeNew(x - end.getX(), y - end.getY());

        double dot = (ab.getX() * cb.getX() + ab.getY() * cb.getY()); // dot product
        double cross = (ab.getX() * cb.getY() - ab.getY() * cb.getX()); // cross product

        double alpha = Math.atan2(cross, dot);
                
        double angle = alpha * 180 / Math.PI;        
        double neg = 0-angle;        
        double mod = (neg+360)%360;
        return mod;

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        XYPoint other = (XYPoint) obj;
        if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
            return false;
        if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
            return false;
        return true;
    }
    
    @Override
	public IPoint minus(IPoint p) {
		return new XYPoint(x-p.getX(), y-p.getY());
	}

	@Override
	public IPoint plus(IPoint p) {
		return new XYPoint(x+p.getX(), y+p.getY());
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
