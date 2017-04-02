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
package com.bmskinner.nuclear_morphology.components;

import ij.process.FloatPolygon;

import java.awt.Shape;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.detection.Mask;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.logging.Loggable;

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
				Rotatable,
				Statistical {
	
	// Standard components
	static final String WHOLE_CELL  = "Cell";
	static final String ACROSOME    = "Acrosome";
	static final String NUCLEUS     = "Nucleus";
	static final String CYTOPLASM   = "Cytoplasm";
	static final String SPERM_TAIL  = "SpermTail";
	static final String NUCLEAR_SIGNAL  = "NuclearSignal";
	static final String NUCLEAR_BORDER_SEGMENT = "NuclearBorderSegment";	
				
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
	boolean equals(CellularComponent c);
	
	
	/**
	 * Create a defensive copy of this object
	 * @return
	 */
	CellularComponent duplicate();
	
	
	/**
	 * Should the border be smoothed when calculating the interpolated
	 * border list
	 * @return
	 */
	boolean isSmoothByDefault();
	
	
	
	
	
	/**
	 * If any stats are listed as uncalcualted, attempt to calculate them
	 */
	void updateDependentStats();
	

	
	/**
	 * Get the number of pixels per micron in the source image
	 * @return
	 */
	double getScale();
	
	/**
	 * Set the number of pixels per micron in the source image
	 * @param scale
	 */
	void setScale(double scale);
	
	/**
	 * Get the position of the centre of mass of the component
	 * @return
	 */
	IPoint getCentreOfMass();

	/**
	 * Get the position of the centre of mass of the component
	 * within the original source image
	 * @return
	 */
	IPoint getOriginalCentreOfMass();
	
	/*
	 * 
	 * BORDER POINTS
	 * 
	 */

	
	/**
	 * Get the border point at the given index (not a copy)
	 * @param i
	 * @return
	 */
	IBorderPoint getBorderPoint(int i) throws UnavailableBorderPointException;
	
	/**
	 * Get a copy of the original (non-offset) border point at the given index
	 * @param i
	 * @return
	 */
	IBorderPoint getOriginalBorderPoint(int i) throws UnavailableBorderPointException;
	
	/**
	 * Get the index of the given point in the border list
	 * @param p
	 * @return
	 */
	int getBorderIndex(IBorderPoint p);

//	public double getDistance(int index);

	/**
	 * Update the border point at the given index to the 
	 * given x y coordinates
	 * @param i
	 * @param x
	 * @param y
	 */
	void updateBorderPoint(int i, double x, double y);
	
	/**
	 * Update the border point at the given index to the 
	 * given x y coordinates
	 * @param i the index
	 * @param p the new postion
	 */
	void updateBorderPoint(int i, IPoint p);
	
	/**
	 * Get the length of the angle profile in index units
	 * @return
	 */
	int getBorderLength();
	
	/**
	 * Get a copy of the component border points in the border list
	 * @return
	 */
	public List<IBorderPoint> getBorderList();
	
	/**
	 * Set the border points in the object border
	 * @param list
	 */
//	public void setBorderList(List<IBorderPoint> list);
	
	/**
	 * Get a copy of the nucleus border points in the border list
	 * offset to their original coordinates in the source image
	 * @return
	 */
	List<IBorderPoint> getOriginalBorderList() throws UnavailableBorderPointException;

	
	/**
	 * Test if the given point is within the offset nucleus
	 * @param p
	 * @return
	 */
	boolean containsPoint(IPoint p);
	
	/**
	 * Test if the given point is within the offset nucleus
	 * @param p
	 * @return
	 */
	boolean containsPoint(int x, int y);
	
	/**
	 * Test if the given point is within the object. This uses
	 * the original coordinates of the object within its source
	 * image
	 * @param p
	 * @return
	 */
	boolean containsOriginalPoint(IPoint p);
	
			
	
	/**
	 * Get the maximum x value from the positions of border points
	 * @return
	 */
	double getMaxX();

	/**
	 * Get the minimum x value from the positions of border points
	 * @return
	 */
	double getMinX();

	/**
	 * Get the maximum y value from the positions of border points
	 * @return
	 */
	double getMaxY();

	/**
	 * Get the minimum y value from the positions of border points
	 * @return
	 */
	double getMinY();
	
	/**
	 * Flip the nucleus on the x-axis (horizontally) about the given point
	 * @param p the point with the x coordinate to flip on
	 */
	void flipXAroundPoint(IPoint p);

	
	/**
	 * Get the median distance between each pair of border points
	 * @return
	 */
	double getMedianDistanceBetweenPoints();
	
	/**
	 * Translate the XY coordinates of each border point so that
	 * the nuclear centre of mass is at the given point
	 * @param point the new centre of mass
	 */
	void moveCentreOfMass(IPoint point);
	
	/**
	 * Translate the XY coordinates of each border point and the centre
	 * of mass by the given amount in the x and y axes
	 * @param xOffset the amount to move the border in the x-dimension
	 * @param yOffset the amount to move the border in the y-dimension
	 */
	void offset(double xOffset, double yOffset);
	

	/**
	 * Wrap arrays. If an index falls of the end, it is returned to the start and vice versa
	 * @param i the index
	 * @return the index within the border list
	 */
	int wrapIndex(int i);
	
	/**
	 * Create a polygon with the same position as the component
	 * in its source image.
	 * @return
	 */
	FloatPolygon createOriginalPolygon();
	
	/**
	 * Create a shape (in this case a Path2D encompassing 
	 * the border points of the component)
	 * @return
	 */
	Shape toShape();
	
	/**
	 * Create a shape (in this case a Path2D encompassing 
	 * the border points of the component) at the position 
	 * of the shape in its source image
	 * @return
	 */
	Shape toOriginalShape();
	
	 /**
	 * Turn a list of border points into a polygon. 
	 * @param list the list of border points
	 * @return
	 */
	FloatPolygon createPolygon();
	

	
	/**
	 * Create a boolean mask, in which true is within the component and false is outside
	 * the component, for an image centred on the nuclear centre of mass, of the
	 * given size
	 * @param height the height of the mask
	 * @param width the width of the mask
	 * @return a mask of size width * height
	 */
	Mask getBooleanMask(int height, int width);
	
	/**
	 * Create a boolean mask, in which true is within the nucleus and false is outside
	 * the component, for the original source image of the component
	 * @return a mask
	 */
	Mask getSourceBooleanMask();
	
	/*
    For two NucleusBorderPoints in a Nucleus, find the point that lies halfway between them
    Used for obtaining a consensus between potential tail positions
	 */
	int getPositionBetween(IBorderPoint pointA, IBorderPoint pointB);


	/**
	 * For a position in the roi, draw a line through the CoM and get the intersection point
	 * @param p
	 * @return
	 * @throws UnavailableBorderPointException
	 */
	IBorderPoint findOppositeBorder(IBorderPoint p) throws UnavailableBorderPointException;

	/*
    From the point given, create a line to the CoM. Measure angles from all 
    other points. Pick the point closest to 90 degrees. Can then get opposite
    point. Defaults to input point if unable to find point.
	 */
	IBorderPoint findOrthogonalBorderPoint(IBorderPoint a) throws UnavailableBorderPointException;
	
	
	/**
	 * Find the border point in this object that is closest to 
	 * the given XYPoint
	 * @param p
	 * @return
	 */
	IBorderPoint findClosestBorderPoint(IPoint p) throws UnavailableBorderPointException;

		
	/**
	 * Reverse the border outline of this object, including the
	 * roi points used to reconstruct it from file
	 * 
	 */
	void reverse();
	
}
