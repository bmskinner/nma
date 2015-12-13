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
package components.nuclei;

import java.awt.Rectangle;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.Profile;
import components.generic.SegmentedProfile;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.NucleusBorderPoint;
import components.nuclear.SignalCollection;
import stats.NucleusStatistic;

public interface Nucleus {

	// index values for selecting original positions
	// indices in  the originalPositions array
	public static final int X_BASE = 0;
	public static final int Y_BASE = 1;
	public static final int WIDTH = 2;
	public static final int HEIGHT = 3;
	
	// for debugging - use in calling dumpInfo()
	public static final int ALL_POINTS = 0;
	public static final int BORDER_POINTS = 1;
	public static final int BORDER_TAGS = 2;

	public void findPointsAroundBorder() throws Exception;
	public void intitialiseNucleus(int angleProfileWindowSize) throws Exception;

	public UUID getID();

	/**
	 * Get the absolute path to the image containing the nucleus
	 * @return
	 */
	public String getPath();
	
	/**
	 * @return a copy of the data in this nucleus or null on Exception
	 */
	public Nucleus duplicate();

	/**
	 * Get the position of the nucleus in the 
	 * original image. The indexes in the double are
	 * 0 - X_BASE of the bounding box
	 * 1 - Y_BASE of the bounding box
	 * 2 - WIDTH of the bounding box
	 * 3 - HEIGHT of the bounding box
	 * @return
	 */
	public double[] getPosition();

	/**
	 * Set the position of the nucleus in the original
	 * image. See getPosition() for values to use.
	 * @param d
	 * @see getPosition()
	 */
	public void setPosition(double[] d);

	/**
	 * Get the absolute path of the original image
	 * @return
	 */
	public File getSourceFile();

	/**
	 * Get the absolute path to the folder containing
	 * the details of this nucleus. It will have the
	 * format /SourceDir/ImageName/
	 * @return
	 */
	public File getNucleusFolder();

	/**
	 * Get the name of the image the nucleus was found in
	 * @return
	 */
	public String getImageName();
	
	
	/**
	 * Get a representation of the nucleus name as
	 * the name of the image plus the number of the nucleus.
	 * For /P12.tiff nucleus 3 : P12.tiff-3
	 * @return
	 */
	public String getNameAndNumber();

	/**
	 * Get the absolute path to use to save annotated
	 * images of the nucleus as a string
	 * @return
	 */
	public String getAnnotatedImagePath();

	/**
	 * Get the absolute path to use to save a copy of
	 * the nucleus as a string
	 * @return
	 */
	public String getOriginalImagePath();

	/**
	 * Get the absolute path to use to save an enlarged region
	 * around the nucleus as a string
	 * @return
	 */
	public String getEnlargedImagePath();

	/**
	 * Get the name of the image the nucleus was found in
	 * without the file extension
	 * @return
	 */
	public String getImageNameWithoutExtension();

	/**
	 * Get the top level path for the analysis being performed.
	 * This is the folder with the analysis date and time.
	 * TODO: Deprecate. This should be only a feature of the collection.
	 * @return
	 */
	public File getOutputFolder();

	/**
	 * Get the directory in which the image containing
	 * the nucleus is found
	 * @return
	 */
	public String getDirectory();
	
	/**
	 * Get the name of the directory in which the image containing
	 * the nucleus is found. e.g. C:\Folder\ImageFolder\1.tiff
	 * will return ImageFolder
	 * @return
	 */
	public String getSourceDirectoryName();

	/**
	 * Get the absolute path to the image containing the nucleus
	 * without the file extension. Used when making paths to the 
	 * nucleus folder
	 * @return
	 */
	public String getPathWithoutExtension();

	/**
	 * Get the number of the nucleus in the image
	 * @return
	 */
	public int getNucleusNumber();

	public String getPathAndNumber();

	/**
	 * Get the position of the centre of mass of the nucleus
	 * @return
	 */
	public XYPoint getCentreOfMass();
	
	public String getOrientationPoint();
	
	public String getReferencePoint();
	
	/**
	 * Get the cached bounding rectangle for the nucleus. If not present,
	 * the rectangle is calculated and stored
	 * @param point the border point to place at the bottom
	 * @return
	 * @throws Exception
	 */
	public Rectangle getBoundingRectangle(BorderTag point) throws Exception;
	
	/**
	 * Get a copy of the border point mapped to the given tag
	 * @param tag
	 * @return
	 */
	public NucleusBorderPoint getPoint(BorderTag tag); 

	
	/**
	 * Get a copy of the border point at the given index
	 * @param i
	 * @return
	 */
	public NucleusBorderPoint getPoint(int i);


	/**
	 * Get the value of the given statistic for this nucleus.
	 * Note that NucleusStatistic.VARIABILILTY returns zero, 
	 * as this must be calculated at the collection level
	 * @param stat the statistic to fetch
	 * @param scale the units to return values in
	 * @return the value or zero if stat.equals(NucleusStatistic.VARIABILILTY)==true
	 * @throws Exception 
	 */
	public double getStatistic(NucleusStatistic stat, MeasurementScale scale) throws Exception;
	
	/**
	 * Get the area of the nucleus in pixels
	 * @return
	 */
	public double getArea();
	
	/**
	 * Get the maximum caliper diameter across the nucleus in pixels
	 * @return
	 */
	public double getFeret();
	
	/**
	 * Calculate the circularity using the formula:
	 * circularity = 4pi(area/perimeter^2)
	 * @return the circularity
	 */
	public double getCircularity();
	
	
	/**
	 * Get the feret divided by the narrowest diameter through the centre of mass
	 * @return
	 */
	public double getAspectRatio();
	

	/**
	 * Get the narrowest diameter through the centre of mass in pixels
	 * @return
	 */
	public double getNarrowestDiameter();

	public double getPathLength();

	/**
	 * Get the perimeter of the nucleus in pixels
	 * @return
	 */
	public double getPerimeter();

	/**
	 * Get a copy of the angle profile. The first index of the profile
	 * is the first border point in the border list. That is, there is no
	 * consistency to the order of values across multiple nuclei. If consistency
	 * is needed, specify a pointType
	 * @return
	 * @throws Exception 
	 */
	public SegmentedProfile getAngleProfile() throws Exception;
	
	/**
	 * Get a copy of the angle profile offset to start at the given point
	 * @param pointType the point to start at
	 * @return a copy of the segmented profile
	 * @throws Exception 
	 */
	public SegmentedProfile getAngleProfile(BorderTag tag) throws Exception;

	
	/**
	 * Update the angle profile to the given segmented profile
	 * @param profile
	 * @throws Exception 
	 */
	public void setAngleProfile(SegmentedProfile profile) throws Exception;
	
	/**
	 * Update the angle profile to the given segmented profile. The profile being used is offset
	 * to start at the given point type, and so the offset is removed before the profile is assigned
	 * @param p the profile
	 * @param pointType the border tag the profile begins from
	 * @throws Exception
	 */
	public void setAngleProfile(SegmentedProfile p, BorderTag tag) throws Exception;
	
	

	/**
	 * Get the size of the angle profile window in pixels
	 * @return
	 */
	public int getAngleProfileWindowSize();

	/**
	 * Fetch the distance profile through the centre of mass
	 * @return
	 */
	public Profile getDistanceProfile();

	/**
	 * Get the length of the angle profile in index units
	 * @return
	 */
	public int getLength();
	
	
	/**
	 * Get the length of a pixel in micrometres
	 * @return
	 */
	public double getScale();
	
	
	/**
	 * Set the length of a pixel in micrometres
	 */
	public void setScale(double scale);
	
	public NucleusBorderPoint getBorderPoint(int i);

	public int getFailureCode();

	/**
	 * Check if the given signal group has a signal in this nucleus
	 * @param channel the signal group
	 * @return
	 */
	public boolean hasSignal(int signalGroup);
	
	/**
	 * Check if any of the signal groups in the nucleus have a signal
	 * @return
	 */
	public boolean hasSignal();

	/**
	 * Get a copy of the nucleus border points in the border list
	 * @return
	 */
	public List<NucleusBorderPoint> getBorderList();
	
	/**
	 * Get a copy of the nucleus border points in the border list
	 * offset to their original coordinates in the source image
	 * @return
	 */
	public List<NucleusBorderPoint> getOriginalBorderList();

	public void calculateFractionalSignalDistancesFromCoM();
	public void calculateSignalDistancesFromCoM();

	/*
    -----------------------
    Protected setters for subclasses
    -----------------------
	 */
	
	public void setOutputFolder(String f);

	public void setCentreOfMass(XYPoint d);

	public void updateFailureCode(int i);


	public void setBorderList(List<NucleusBorderPoint> list);

	/*
    -----------------------
    Get aggregate values
    -----------------------
	 */
	
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

	/*
    -----------------------
    Set miscellaneous features
    -----------------------
	 */

//	public void setPathLength(double d);

//	public void calculatePathLength();

	public void setArea(double d);
	public void setFeret(double d);
	public void setPerimeter(double d);

	/*
    -----------------------
    Process signals
    -----------------------
	 */


	/*
    -----------------------
    Determine positions of points
    -----------------------
	 */

	/*
    For two NucleusBorderPoints in a Nucleus, find the point that lies halfway between them
    Used for obtaining a consensus between potential tail positions
	 */
	public int getPositionBetween(NucleusBorderPoint pointA, NucleusBorderPoint pointB);

	// For a position in the roi, draw a line through the CoM and get the intersection point
	public NucleusBorderPoint findOppositeBorder(NucleusBorderPoint p);

	/*
    From the point given, create a line to the CoM. Measure angles from all 
    other points. Pick the point closest to 90 degrees. Can then get opposite
    point. Defaults to input point if unable to find point.
	 */
	public NucleusBorderPoint findOrthogonalBorderPoint(NucleusBorderPoint a);

	/**
	 *  Find the point with the narrowest diameter through the CoM
	 *  using the distance profile
	 * @return
	 */
	public NucleusBorderPoint getNarrowestDiameterPoint();

	/**
	 * Flip the nucleus on the x-axis (horizontally) about the given point
	 * @param p the point with the x coordinate to flip on
	 */
	public void flipXAroundPoint(XYPoint p);

	public double getMedianDistanceBetweenPoints();

	/*
    -----------------------
    Exporting data
    -----------------------
	 */

	/**
	 * Calculate the angle that the nucleus must be rotated by. 
	 * If the BorderTags TOP_VERTICAL and BOTTOM_VERTICAL have been set, 
	 * the angle will align these points on the y-axis. Otherwise, the angle will
	 * place the orientation point directly below the centre of mass.
	 * @return
	 */
	public double findRotationAngle();

	/**
	 * Calculate the angle signal centres of mass make with the nucleus centre of mass
	 * and the given border point
	 * @param p the border point to orient from (the zero angle)
	 * @throws Exception
	 */
	public void calculateSignalAnglesFromPoint(NucleusBorderPoint p) throws Exception;
	
	public void exportSignalDistanceMatrix();

	public Profile getSingleDistanceProfile();

	public String dumpInfo(int type);

	public double getAngle(int index);

	
	/**
	 * Get the index of the given point in the border list
	 * @param p
	 * @return
	 */
	public int getIndex(NucleusBorderPoint p);

	public double getDistance(int index);

	/**
	 * Update the border point at the given index to the 
	 * given x y coordinates
	 * @param i
	 * @param x
	 * @param y
	 */
	public void updatePoint(int i, double x, double y);
	
	/**
	 * Update the border point at the given index to the 
	 * given x y coordinates
	 * @param i the index
	 * @param p the new postion
	 */
	public void updatePoint(int i, XYPoint p);


	/**
	 * Get the index of the border point with the given tag.
	 * If the point does not exist, returns -1
	 * @param s the tag
	 * @return the index of the border in borderList
	 */
	public int getBorderIndex(BorderTag tag);
	
	/**
	 * Get the tag at a given index, given a zero tag
	 * If there is no tag at the index, returns null
	 * @param tag the border tag with index zero
	 * @param index the index to fetch
	 * @return the border tag at the index
	 */
	public BorderTag getBorderTag(BorderTag tag, int index);
	
	/**
	 * Get the tag at the given raw index in the border list
	 * @param index
	 * @return the tag at the index, or null if no tag present
	 */
	public BorderTag getBorderTag(int index);

	
	/**
	 * Get a copy of the border point at the given tag
	 * @param tag
	 * @return
	 */
	public NucleusBorderPoint getBorderTag(BorderTag tag);
	
	/**
	 * Check if the nucleus has the given border tag
	 * @param tag
	 * @return
	 */
	public boolean hasBorderTag(BorderTag tag);
	
	/**
	 * Check if the nucleus has any border tag at the given index
	 * (offset from the provided tag)
	 * @param tag the border tag with index zero
	 * @param i the index to be tested
	 * @return true if a tag is present at the index
	 */
	public boolean hasBorderTag(BorderTag tag, int i);
	
	/**
	 * Check if the nucleus has any border tag at the given index
	 * in the raw border list
	 * @param i the index to be tested
	 * @return true if a tag is present at the index
	 */
	public boolean hasBorderTag( int index);
	
	/**
	 * Set the name of the given NucleusBorderPoint
	 * @param tag the new tag to use as a name
	 * @param i the index of the border point
	 */
	
	public void setBorderTag(BorderTag tag, int i);
	
	/**
	 * Set or update a border tag based on an index from a reference tag
	 * @param reference the border tag with index zero
	 * @param tag the new tag to use
	 * @param i the index of the border point relative to the reference
	 */
	public void setBorderTag(BorderTag reference, BorderTag tag, int i);


	/**
	 * Calculate a new angle profile with the given window size. It
	 * will replace the existing angle profile. It takes the point, 
	 * a point <windowSize> behind and <windowSize> ahead, and calculates
	 * the interior angle between them.
	 * @param angleProfileWindowSize the window size
	 * @throws Exception 
	 */
	public SegmentedProfile calculateAngleProfile(int angleProfileWindowSize) throws Exception;
	
	
	/**
	 * Get the signals in this nucleus
	 * @return
	 */
	public SignalCollection getSignalCollection();
	
	
	/**
	 * Get the number of signals in the nucleus
	 * @return
	 */
	public int getSignalCount();
	
	
	/**
	 * Get the number of signals in the given signal group
	 * @param signalGroup
	 * @return
	 */
	public int getSignalCount(int signalGroup);
	
	
	/**
	 * @param n the signal
	 * @param signalGroup the signal group to add to
	 */
	public void addSignal(NuclearSignal n, int signalGroup);
	
	
	/**
	 * Get a copy of the signals in the given signal group
	 * @param signalGroup the group
	 * @return a list of COPIES of the nuclear signals in the group
	 */
	public List<NuclearSignal> getSignals(int signalGroup);
	
	/**
	 * Get the signals in the nucleus by group as a list of lists
	 * @return the list of lists
	 */
	public List<List<NuclearSignal>> getSignals();
	
	/**
	 * Get the signal groups in the current signal collection
	 * @return the set of group ids
	 */
	public Set<Integer> getSignalGroups();
	
	/**
	 * Get a copy of the mapping of border tags to index positions within 
	 * the border list of the nucleus
	 * @return
	 */
	public Map<BorderTag, Integer> getBorderTags();
	
	/**
	 * Get the border index of a tag in the border list, 
	 * removing offset to a reference tag
	 * @param reference the border tag with index zero
	 * @param index the index to offset. Should be counting from the reference tag
	 * @return the offset index, or -1 if the reference tag is not present
	 */
	public int getOffsetBorderIndex(BorderTag reference, int index);
	

	/**
	 * Reverse the angle profile of the nucleus. Also reverses the distance
	 * profile, the border list and updates the border tags to the new positions
	 * @throws Exception
	 */
	public void reverse() throws Exception;
	
	
	/**
	 * Reverses the angle profile, without reversing the border points,
	 * distance profile or border tag indexes.
	 * @throws Exception
	 */
	public void flipAngleProfile()throws Exception;
	
	
	/**
	 * Get the name of the folder to store analysis specific data.
	 * This is the folder with the analysis date/time name.
	 * @return
	 */
	public String getOutputFolderName();
	
	
	/**
	 * Update the image source folder to the given new folder
	 * @param newFolder
	 * @throws Exception 
	 */
	public void updateSourceFolder(File newFolder) throws Exception;
}