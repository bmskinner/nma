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
package com.bmskinner.nuclear_morphology.components.generic;

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
    private static final double EPSILON = 0.000001;

    /**
     * Construct from float values
     * @param x
     * @param y
     */
    public FloatPoint(float x, float y) {
        super(x, y);
    }

    /**
     * Construct from double values. These will be converted to floats.
     * @param x
     * @param y
     */
    public FloatPoint(double x, double y) {
        super((float) x, (float) y);
    }
    
    /**
     * Create from an existing point
     * @param p
     */
    public FloatPoint(@NonNull FloatPoint p){
    	super(p.x, p.y);
    }

    /**
     * Create from an existing point
     * @param p
     */
    public FloatPoint(@NonNull IPoint p) {    	
        this(p.getX(), p.getY());
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
        if (p==null)
            throw new IllegalArgumentException("Destination point is null");
        this.x = (float) p.getX();
        this.y = (float) p.getY();
    }
    
    @Override
    public void set(@NonNull Point2D p) {
    	if (p==null)
            throw new IllegalArgumentException("Destination point is null");
        this.x = (float) p.getX();
        this.y = (float) p.getY();
    }
    
    private double getLengthTo(final FloatPoint a) {
    	 // a2 = b2 + c2
        double dx = x - a.x;
        double dy = y - a.y;
        double dx2 = dx * dx;
        double dy2 = dy * dy;
        double length = Math.sqrt(dx2 + dy2);
        return length;
    }

    @Override
    public double getLengthTo(@NonNull final IPoint a) {

        if (a instanceof FloatPoint)
            return getLengthTo((FloatPoint)a);

        // a2 = b2 + c2
        double dx = x - a.getX();
        double dy = y - a.getY();
        double dx2 = dx * dx;
        double dy2 = dy * dy;
        double length = Math.sqrt(dx2 + dy2);
        return length;
    }
    
    @Override
    public boolean overlaps(@NonNull final IPoint a) {
        return(this.getXAsInt() == a.getXAsInt() && this.getYAsInt() == a.getYAsInt());
    }

    @Override
    public boolean isAbove(@NonNull IPoint p) {
        if (p==null)
            throw new IllegalArgumentException("Point is null");
        return y > p.getY();
    }

    @Override
    public boolean isBelow(@NonNull IPoint p) {
        if (p==null)
            throw new IllegalArgumentException("Point is null");
        return y < p.getY();
    }

    @Override
    public boolean isLeftOf(@NonNull IPoint p) {
        if (p==null)
            throw new IllegalArgumentException("Point is null");
        return x < p.getX();
    }

    @Override
    public boolean isRightOf(@NonNull IPoint p) {
        if (p==null)
            throw new IllegalArgumentException("Point is null");
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
        	return overlapsPerfectly((FloatPoint)a);
        return (this.getX() == a.getX() && this.getY() == a.getY());
    }


    @Override
    public String toString() {
    	DecimalFormat df = new DecimalFormat("#0.##");
    	return df.format(getX()) + " - " + df.format(getY());
    }

    @Override
    public Point2D toPoint2D() {
        return this;
    }

    @Override
    public double findSmallestAngle(@NonNull IPoint a, @NonNull IPoint c) {

        if (a == null || c == null)
            throw new IllegalArgumentException("An input point is null in angle finding");
        
        if(a instanceof FloatPoint && c instanceof FloatPoint)
        	return findSmallestAngle( (FloatPoint)a, (FloatPoint)c);

        /*
         * Test of rotation and comparison to a horizontal axis From
         * http://stackoverflow.com/questions/3486172/angle-between-3-points
         * 
         * The vectors are rotated so one is on the xaxis, at which point atan2 does the rest
         */
        
        IPoint ab = IPoint.makeNew(x - a.getX(), y - a.getY());
        IPoint cb = IPoint.makeNew(x - c.getX(), y - c.getY());

        double dot = (ab.getX() * cb.getX() + ab.getY() * cb.getY()); // dot product
        double cross = (ab.getX() * cb.getY() - ab.getY() * cb.getX()); // cross product

        double alpha = Math.atan2(cross, dot);

        return Math.abs(alpha * 180 / Math.PI);
    }
    
    private double findSmallestAngle(@NonNull FloatPoint a, @NonNull FloatPoint c) {

    	float abx = x-a.x;
    	float aby = y-a.y;
    	float cbx = x-c.x;
    	float cby = y-c.y;

    	double dot = (abx * cbx + aby * cby); // dot product
    	double cross = (abx * cby - aby * cbx); // cross product
    	double alpha = Math.atan2(cross, dot);
    	return Math.abs(alpha * 180 / Math.PI);
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
//        System.out.println("Angle: "+angle);
        
        double neg = 0-angle;
//        System.out.println("Negated angle: "+neg);
        
        double mod = (neg+360)%360;
//        System.out.println("Mod angle: "+mod);
        return mod;
//        return (360+angle)%360;
//        return Math.abs();
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
	public IPoint minus(@NonNull IPoint p) {
	    if (p == null)
            throw new IllegalArgumentException("Point is null");
		return new FloatPoint(x-p.getX(), y-p.getY());
	}

	@Override
	public IPoint plus(@NonNull IPoint p) {
	    if (p == null)
            throw new IllegalArgumentException("Point is null");
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
