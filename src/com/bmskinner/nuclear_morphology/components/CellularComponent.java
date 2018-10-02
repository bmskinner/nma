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


package com.bmskinner.nuclear_morphology.components;

import java.awt.Shape;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.detection.Mask;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * This interface provides the basic methods for a component of a cell - an
 * object with a border, a source image, and a size in microns.
 * 
 * @author bms41
 * @since 1.11.0
 *
 */
public interface CellularComponent extends Imageable, Serializable, Loggable, Rotatable, Statistical {

    // Standard components
    static final String WHOLE_CELL             = "Cell";
    static final String ACROSOME               = "Acrosome";
    static final String NUCLEUS                = "Nucleus";
    static final String CYTOPLASM              = "Cytoplasm";
    static final String SPERM_TAIL             = "SpermTail";
    static final String NUCLEAR_SIGNAL         = "NuclearSignal";
    static final String NUCLEAR_LOBE           = "NuclearLobe";
    static final String NUCLEAR_BORDER_SEGMENT = "NuclearBorderSegment";
    
    /**
     * Get the UUID of the object
     * 
     * @return
     */
    @NonNull UUID getID();

    /**
     * Create a defensive copy of this object
     * 
     * @return
     */
    CellularComponent duplicate();

    /**
     * Should the border be smoothed when calculating the interpolated border
     * list
     * 
     * @return
     */
    boolean isSmoothByDefault();

    /**
     * If any stats are listed as uncalculated, attempt to calculate them
     */
    void updateDependentStats();

    /**
     * Get the number of pixels per micron in the source image
     * 
     * @return
     */
    double getScale();

    /**
     * Set the number of pixels per micron in the source image
     * 
     * @param scale
     */
    void setScale(double scale);

    /**
     * Get the position of the centre of mass of the component
     * 
     * @return
     */
    IPoint getCentreOfMass();

    /**
     * Get the position of the centre of mass of the component within the
     * original source image
     * 
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
     * 
     * @param i
     * @return
     */
    IBorderPoint getBorderPoint(int i) throws UnavailableBorderPointException;

    /**
     * Get a copy of the original (non-offset) border point at the given index
     * 
     * @param i
     * @return
     */
    IBorderPoint getOriginalBorderPoint(int i) throws UnavailableBorderPointException;

    /**
     * Get the index of the given point in the border list
     * 
     * @param p
     * @return
     */
    int getBorderIndex(@NonNull IBorderPoint p);

    // public double getDistance(int index);

    /**
     * Update the border point at the given index to the given x y coordinates
     * 
     * @param i
     * @param x
     * @param y
     */
    void updateBorderPoint(int i, double x, double y);

    /**
     * Update the border point at the given index to the given x y coordinates
     * 
     * @param i  the index
     * @param p the new postion
     */
    void updateBorderPoint(int i, @NonNull IPoint p);

    /**
     * Get the length of the object border - equivalent to the length
     * of the angle profile - in index units
     * 
     * @return
     */
    int getBorderLength();

    /**
     * Get a copy of the component border points in the border list
     * 
     * @return
     */
    public List<IBorderPoint> getBorderList();

    /**
     * Set the border points in the object border
     * 
     * @param list
     */
    // public void setBorderList(List<IBorderPoint> list);

    /**
     * Get a copy of the nucleus border points in the border list offset to
     * their original coordinates in the source image
     * 
     * @return
     */
    List<IBorderPoint> getOriginalBorderList() throws UnavailableBorderPointException;

    /**
     * Test if the given point is within the offset nucleus
     * 
     * @param p
     * @return
     */
    boolean containsPoint(IPoint p);

    /**
     * Test if the given point is within the offset nucleus
     * 
     * @param p
     * @return
     */
    boolean containsPoint(int x, int y);

    /**
     * Test if the given point is within the object. This uses the original
     * coordinates of the object within its source image
     * 
     * @param p
     * @return
     */
    boolean containsOriginalPoint(IPoint p);

    /**
     * Get the maximum x value from the positions of border points
     * 
     * @return
     */
    double getMaxX();

    /**
     * Get the minimum x value from the positions of border points
     * 
     * @return
     */
    double getMinX();

    /**
     * Get the maximum y value from the positions of border points
     * 
     * @return
     */
    double getMaxY();

    /**
     * Get the minimum y value from the positions of border points
     * 
     * @return
     */
    double getMinY();

    /**
     * Flip the nucleus on the x-axis (horizontally) about the given point
     * 
     * @param p the point with the x coordinate to flip on
     */
    void flipXAroundPoint(@NonNull IPoint p);

    /**
     * Get the median distance between each pair of border points
     * 
     * @return
     */
    double getMedianDistanceBetweenPoints();

    /**
     * Translate the XY coordinates of each border point so that the nuclear
     * centre of mass is at the given point
     * 
     * @param point the new centre of mass
     */
    void moveCentreOfMass(@NonNull IPoint point);

    /**
     * Translate the XY coordinates of each border point and the centre of mass
     * by the given amount in the x and y axes
     * 
     * @param xOffset the amount to move the border in the x-dimension
     * @param yOffset the amount to move the border in the y-dimension
     */
    void offset(double xOffset, double yOffset);

    /**
     * Wrap border indexes. If an index falls of the end, it is returned to the start
     * and vice versa
     * 
     * @param i the index
     * @return the index within the border list
     */
    int wrapIndex(int i);
    
    /**
     * Wrap border indexes. If an index falls of the end, it is returned to the start
     * and vice versa
     * 
     * @param i the index
     * @return the index within the border list
     */
    double wrapIndex(double d);

    /**
     * Turn a list of border points into a polygon. These have the
     * positions of the points as they exist in the border list, not
     * in the original image.
     * 
     * @param list
     *            the list of border points
     * @return
     */
    FloatPolygon toPolygon();

    /**
     * Create a polygon with the same position as the component in its source
     * image.
     * 
     * @return
     */
    FloatPolygon toOriginalPolygon();

    /**
     * Create a shape (in this case a Path2D encompassing the border points of
     * the component)
     * 
     * @return
     */
    Shape toShape();

    /**
     * Create a shape (in this case a Path2D encompassing the border points of
     * the component) at the given scale
     * 
     * @param scale the measurement scale for the component
     * @return
     */
    Shape toShape(MeasurementScale scale);

    /**
     * Create a shape (in this case a Path2D encompassing the border points of
     * the component) at the position of the shape in its source image
     * 
     * @return
     */
    Shape toOriginalShape();

    /**
     * Create an ImageJ ROI encompassing the component
     * 
     * @return
     */
    Roi toRoi();

    /**
     * Create an ImageJ ROI encompassing the component with the same position as
     * the component in its source image.
     * 
     * @return
     */
    Roi toOriginalRoi();

    /**
     * Create a boolean mask, in which true is within the component and false is
     * outside the component, for an image centred on the nuclear centre of
     * mass, of the given size
     * 
     * @param height the height of the mask
     * @param width the width of the mask
     * @return a mask of size width * height
     */
    Mask getBooleanMask(int height, int width);

    /**
     * Create a boolean mask, in which true is within the nucleus and false is
     * outside the component, for the original source image of the component
     * 
     * @return a mask
     */
    Mask getSourceBooleanMask();

    /*
     * For two points in the object border, find the point that lies
     * halfway between them. Used for obtaining a consensus between potential
     * tail positions
     */
    int getPositionBetween(@NonNull IBorderPoint pointA, @NonNull IBorderPoint pointB);

    /**
     * For a position in the roi, draw a line through the CoM and get the
     * intersection point
     * 
     * @param p
     * @return
     * @throws UnavailableBorderPointException
     */
    IBorderPoint findOppositeBorder(@NonNull IBorderPoint p) throws UnavailableBorderPointException;

    /**
     * @param a the point to draw to the centre of mass
     * @return the orthogonal border point or input point if no other point was
     *         found
     * @throws UnavailableBorderPointException
     *             if the input point is not found in the component border
     */
    IBorderPoint findOrthogonalBorderPoint(@NonNull IBorderPoint a) throws UnavailableBorderPointException;

    /**
     * Find the border point in this object that is closest to the given XYPoint
     * 
     * @param p
     * @return
     */
    IBorderPoint findClosestBorderPoint(@NonNull IPoint p) throws UnavailableBorderPointException;
    
    /**
     * Create the border list from the stored int[] points. Mimics the internal
     * makeBorderList, but allows spline fitting to be disabled. This is used
     * in converting datasets from pre-1.14.0. Splines are fit by default in 
     * 1.14.0 onwards, which causes issues deserialising 1.13.8 and earlier datasets.
     * Disable spline fitting to display these earlier formats properly.
     * 
     * @param useSplineFitting should spline fitting be used
     */
    public void refreshBorderList(boolean useSplineFitting);

    /**
     * Reverse the border outline of this object, including the roi points used
     * to reconstruct it from file
     * 
     */
    void reverse();

    /**
     * Wrap arrays. If an index falls of the end, it is returned to the start
     * and vice versa
     * 
     * @param i  the index
     * @param length the array length
     * @return the index within the array
     */
    static int wrapIndex(int i, int length) {
        if (i < 0) // Recurse until positive
            return wrapIndex(length + i, length);
        if (i < length) // if not wrapping
            return i;
        return i % length;
    }

    /**
     * Wrap arrays for doubles. This is used in interpolation. If an index falls
     * of the end, it is returned to the start and vice versa
     * 
     * @param i the index
     * @param length the array length
     * @return the index within the array
     */
    static double wrapIndex(double i, int length) {
    	if (i < 0) // Recurse until positive
            return wrapIndex(length + i, length);
        if (i < length) // if not wrapping
            return i;
        return i % length;
    }
    
    /**
	 * Offset an array to start from the 
	 * given index
	 * 
	 * @param arr the array
	 * @param j the offset to apply; the new start index
	 * @return the offset array
	 */
	static float[] offset(float[] arr, int j) {
		float[] newArray = new float[arr.length];
		
		int newStartIndex = CellularComponent.wrapIndex(j, arr.length);
		int nElements = arr.length-newStartIndex;
		
		// Copy from the new start index to the end of the array 
		System.arraycopy(arr, newStartIndex, newArray, 0, nElements);
		// copy from the start of the array to the new start index
		System.arraycopy(arr, 0, newArray, nElements, newStartIndex);
		return newArray;
	}
	
	/**
	 * Get the sliding window offset of array 1 that best matches array 2. The
	 * arrays must be the same length
	 * 
	 * @param arr1
	 * @param arr2
	 * @return
	 */
	static int getBestFitOffset(float[] arr1, float[] arr2) {
		return getBestFitOffset(arr1, arr2, 0, arr1.length);
	}
	
	/**
	 * Get the sliding window offset of array 1 that best matches array 2. The
	 * arrays must be the same length. The best offset within the specified range
	 * of indexes will be returned.
	 * 
	 * @param arr1
	 * @param arr2
	 * @minOffset the minimum offset to apply
	 * @maxOffset the maximum offset to apply
	 * @return
	 */
	static int getBestFitOffset(float[] arr1, float[] arr2, int minOffset, int maxOffset) {
		if(arr1.length!=arr2.length)
			throw new IllegalArgumentException("Arrays must be equal length");
		double bestScore = Double.MAX_VALUE;
		int bestIndex = 0;

		for (int i=minOffset; i<maxOffset; i++) {
			double score = squareDifference(CellularComponent.offset(arr1, i), arr2);
			if (score < bestScore) {
				bestScore = score;
				bestIndex = i;
				
			}
		}
		return bestIndex;
	}
	
	/**
	 * Calculate the absolute square difference between two arrays of equal
	 * length. Note - array lengths are not checked.
	 * 
	 * @param arr1
	 * @param arr2
	 * @return
	 */
	static double squareDifference(float[] arr1, float[] arr2) {
		double difference = 0;
		for (int j = 0; j < arr1.length; j++) {
			difference += Math.pow(arr1[j] - arr2[j], 2);
		}
		return difference;
	}
	
	/**
	 * Calculate the absolute square difference between two arrays of equal
	 * length. Note - array lengths are not checked.
	 * 
	 * @param arr1
	 * @param arr2
	 * @return
	 */
	static double squareDifference(double[] arr1, double[] arr2) {
		double difference = 0;
		for (int j = 0; j < arr1.length; j++) {
			difference += Math.pow(arr1[j] - arr2[j], 2);
		}
		return difference;
	}
}
