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
package components;

import ij.process.FloatPolygon;
import logging.Loggable;

import java.awt.Shape;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import components.generic.IPoint;
import components.generic.MeasurementScale;
import components.nuclear.IBorderPoint;
import stats.PlottableStatistic;

/**
 * This interface provides the basic methods for a component
 * of a cell - an object with a border, a source image, and a
 * size in microns. 
 * @author bms41
 * @since 1.11.0
 *
 */
public interface CellularComponent 
		extends Imageable, 
				Serializable, 
				Loggable, 
				Rotatable {
			
	/**
	 * Get the UUID of the object
	 * @return
	 */
	public UUID getID();

	/**
	 * The pixel border added to the edges of the component
	 * when fetching and cropping its source image. This prevents
	 * the component nestling right up to the edges of the 
	 * resulting cropped image
	 */
	public static final int COMPONENT_BUFFER = 10;
	
	/**
	 * A string prepended to any exported image files, so that
	 * they will be ignored by the program on subsequent analyses.
	 * Not really used now, since image exports are not longer performed
	 * by default
	 */
	public static final String IMAGE_PREFIX = "export.";
		
		
	/**
	 * An equality check that relies solely on the component
	 * ids.
	 * @param c
	 * @return true if the components have the same ID
	 */
	public boolean equals(CellularComponent c);
	
	
	/**
	 * Create a defensive copy of this object
	 * @return
	 */
	CellularComponent duplicate();
	
	boolean smoothByDefault();
	
	
	
	/**
	 * Get the value of the given statistic for this nucleus.
	 * Note that NucleusStatistic.VARIABILILTY returns zero, 
	 * as this must be calculated at the collection level
	 * @param stat the statistic to fetch
	 * @param scale the units to return values in
	 * @return the value or zero if stat.equals(NucleusStatistic.VARIABILILTY)==true
	 * @throws Exception 
	 */
	public double getStatistic(PlottableStatistic stat, MeasurementScale scale);
	
	/**
	 * Get the value of the given {@link PlottableStatistic} for this nucleus.
	 * Note that {@link NucleusStatistic.VARIABILILTY} returns zero, 
	 * as this must be calculated at the collection level, not the object level. 
	 * This method converts exceptions from {@link CellularComponent#getStatistic()} into RuntimeExceptions,
	 *  so the method can be used in streams
	 * @param stat the statistic to fetch
	 * @param scale the units to return values in
	 * @return the value or zero if stat.equals( {@link NucleusStatistic.VARIABILILTY})==true
	 */
//	public double getSafeStatistic(PlottableStatistic stat, MeasurementScale scale);
	
	
	/**
	 * Get the statistic at the default scale ({@link MeasurementScale.PIXELS})
	 * @param stat
	 * @return
	 */
	public double getStatistic(PlottableStatistic stat);
	
	/**
	 * Set the statistic at the default scale ({@link MeasurementScale.PIXELS})
	 * @param stat
	 * @param d
	 */
	public void setStatistic(PlottableStatistic stat, double d);
	
	/**
	 * Get all the statistics in this object
	 * @return
	 */
	public PlottableStatistic[] getStatistics();
	

	
	/**
	 * Get the number of pixels per micron in the source image
	 * @return
	 */
	public double getScale();
	
	/**
	 * Set the number of pixels per micron in the source image
	 * @param scale
	 */
	public void setScale(double scale);
	
	/**
	 * Get the position of the centre of mass of the component
	 * @return
	 */
	public IPoint getCentreOfMass();

	/**
	 * Get the position of the centre of mass of the component
	 * within the original source image
	 * @return
	 */
	public IPoint getOriginalCentreOfMass();
	
	/*
	 * 
	 * BORDER POINTS
	 * 
	 */

	
	/**
	 * Get a copy of the border point at the given index
	 * @param i
	 * @return
	 */
	public IBorderPoint getBorderPoint(int i);
	
	/**
	 * Get a copy of the original (non-offset) border point at the given index
	 * @param i
	 * @return
	 */
	public IBorderPoint getOriginalBorderPoint(int i);
	
	/**
	 * Get the index of the given point in the border list
	 * @param p
	 * @return
	 */
	public int getBorderIndex(IBorderPoint p);

//	public double getDistance(int index);

	/**
	 * Update the border point at the given index to the 
	 * given x y coordinates
	 * @param i
	 * @param x
	 * @param y
	 */
	public void updateBorderPoint(int i, double x, double y);
	
	/**
	 * Update the border point at the given index to the 
	 * given x y coordinates
	 * @param i the index
	 * @param p the new postion
	 */
	public void updateBorderPoint(int i, IPoint p);
	
	/**
	 * Get the length of the angle profile in index units
	 * @return
	 */
	public int getBorderLength();
	
	/**
	 * Get a copy of the component border points in the border list
	 * @return
	 */
	public List<IBorderPoint> getBorderList();
	
	/**
	 * Set the border points in the object border
	 * @param list
	 */
	public void setBorderList(List<IBorderPoint> list);
	
	/**
	 * Get a copy of the nucleus border points in the border list
	 * offset to their original coordinates in the source image
	 * @return
	 */
	public List<IBorderPoint> getOriginalBorderList();

	
	/**
	 * Test if the given point is within the offset nucleus
	 * @param p
	 * @return
	 */
	public boolean containsPoint(IPoint p);
	
	/**
	 * Test if the given point is within the offset nucleus
	 * @param p
	 * @return
	 */
	public boolean containsPoint(int x, int y);
	
	/**
	 * Test if the given point is within the object. This uses
	 * the original coordinates of the object within its source
	 * image
	 * @param p
	 * @return
	 */
	public boolean containsOriginalPoint(IPoint p);
	
			
	
	/**
	 * Get the maximum x value from the positions of border points
	 * @return
	 */
	public double getMaxX();

	/**
	 * Get the minimum x value from the positions of border points
	 * @return
	 */
	public double getMinX();

	/**
	 * Get the maximum y value from the positions of border points
	 * @return
	 */
	public double getMaxY();

	/**
	 * Get the minimum y value from the positions of border points
	 * @return
	 */
	public double getMinY();
	
	/**
	 * Flip the nucleus on the x-axis (horizontally) about the given point
	 * @param p the point with the x coordinate to flip on
	 */
	public void flipXAroundPoint(IPoint p);

	public double getMedianDistanceBetweenPoints();
	
	/**
	 * Translate the XY coordinates of each border point so that
	 * the nuclear centre of mass is at the given point
	 * @param point the new centre of mass
	 */
	public void moveCentreOfMass(IPoint point);
	
	/**
	 * Translate the XY coordinates of each border point and the centre
	 * of mass by the given amount in the x and y axes
	 * @param xOffset the amount to move the border in the x-dimension
	 * @param yOffset the amount to move the border in the y-dimension
	 */
	public void offset(double xOffset, double yOffset);
	

	/**
	 * Wrap arrays. If an index falls of the end, it is returned to the start and vice versa
	 * @param i the index
	 * @return the index within the border list
	 */
	public int wrapIndex(int i);
	
	/**
	 * Create a polygon with the same position as the component
	 * in its source image.
	 * @return
	 */
	public FloatPolygon createOriginalPolygon();
	
	/**
	 * Create a shape (in this case a Path2D encompassing 
	 * the border points of the component)
	 * @return
	 */
	public Shape toShape();
	
	/**
	 * Create a shape (in this case a Path2D encompassing 
	 * the border points of the component) at the position 
	 * of the shape in its source image
	 * @return
	 */
	public Shape toOriginalShape();
	
	 /**
	 * Turn a list of border points into a polygon. 
	 * @param list the list of border points
	 * @return
	 */
	public FloatPolygon createPolygon();
	

	
	/**
	 * Create a boolean mask, in which 1 is within the nucleus and 0 is outside
	 * the nucleus, for an image centred on the nuclear centre of mass, of the
	 * given size
	 * @param height
	 * @param width
	 * @return
	 */
	public boolean[][] getBooleanMask(int height, int width);
	
	/*
    For two NucleusBorderPoints in a Nucleus, find the point that lies halfway between them
    Used for obtaining a consensus between potential tail positions
	 */
	public int getPositionBetween(IBorderPoint pointA, IBorderPoint pointB);

	// For a position in the roi, draw a line through the CoM and get the intersection point
	public IBorderPoint findOppositeBorder(IBorderPoint p);

	/*
    From the point given, create a line to the CoM. Measure angles from all 
    other points. Pick the point closest to 90 degrees. Can then get opposite
    point. Defaults to input point if unable to find point.
	 */
	public IBorderPoint findOrthogonalBorderPoint(IBorderPoint a);
	
	
	/**
	 * Find the border point in this object that is closest to 
	 * the given XYPoint
	 * @param p
	 * @return
	 */
	public IBorderPoint findClosestBorderPoint(IPoint p);

		
	/**
	 * Reverse the border outline of this object, including the
	 * roi points used to reconstruct it from file
	 * 
	 */
	void reverse();
	
}
