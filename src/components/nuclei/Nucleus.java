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

import gui.components.MeasurementUnitSettingsPanel.MeasurementScale;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import utility.Constants.BorderTag;
import components.generic.Profile;
import components.generic.SegmentedProfile;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.NucleusBorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.SignalCollection;

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

	public String getPath();

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

	public NucleusBorderPoint getPoint(int i);


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
	public SegmentedProfile getAngleProfile(String pointType) throws Exception;

	
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
	public void setAngleProfile(SegmentedProfile p, String pointType) throws Exception;
	
	

	public int getAngleProfileWindowSize();

	public Profile getDistanceProfile();

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

	//  public void setPolygon(FloatPolygon p);

	public void updateFailureCode(int i);


	public void setBorderList(List<NucleusBorderPoint> list);

	/*
    -----------------------
    Get aggregate values
    -----------------------
	 */
	public double getMaxX();

	public double getMinX();

	public double getMaxY();

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

	/*
    This will find the point in a list that is closest to any local maximum
    in the border profile, wherever that maximum may be
	 */
//	public NucleusBorderPoint findPointClosestToLocalMaximum(NucleusBorderPoint[] list) throws Exception;

	/*
    This will find the point in a list that is closest to any local minimum
    in the border profile, wherever that minimum may be
	 */
//	public NucleusBorderPoint findPointClosestToLocalMinimum(NucleusBorderPoint[] list) throws Exception;


	// find the point with the narrowest diameter through the CoM
	// Uses the distance profile
	public NucleusBorderPoint getNarrowestDiameterPoint();

	public void flipXAroundPoint(XYPoint p);

	public double getMedianDistanceBetweenPoints();

	/*
    -----------------------
    Exporting data
    -----------------------
	 */

	public double findRotationAngle();

	public void calculateSignalAnglesFromPoint(NucleusBorderPoint p);
	public void exportSignalDistanceMatrix();

	public Profile getSingleDistanceProfile();

	public void dumpInfo(int type);

	public double getAngle(int index);

	public int getIndex(NucleusBorderPoint p);

	public double getDistance(int index);

	public void updatePoint(int i, double x, double y);

	/**
	 * Fetch the NucleusBorderPoint associated with the given
	 * tag name. If the tag does not exist, returns null
	 * @param s the tag name
	 * @return the border point with this tag
	 */
	public NucleusBorderPoint getBorderTag(String s);
	
	/**
	 * Fetch the NucleusBorderPoint associated with the given
	 * tag. If the tag does not exist, returns null
	 * @param tag the tag
	 * @return the border point with this tag
	 */
//	public NucleusBorderPoint getBorderTag(BorderTag tag);

	/**
	 * Get the index of the border point with the given tag.
	 * If the point does not exist, returns -1
	 * @param s the tag name
	 * @return the index of the border in borderList
	 */
	public int getBorderIndex(String s);
	
	/**
	 * Get the index of the border point with the given tag.
	 * If the point does not exist, returns -1
	 * @param tag the tag
	 * @return the index of the border in borderList
	 */
//	public int getBorderIndex(BorderTag tag);

	/**
	 * Get a set of all the tags present within this nucleus
	 * @return
	 */
	public Set<String> getTags();
	
	/**
	 * Check if the nucleus has the given border tag
	 * @param tag
	 * @return
	 */
	public boolean hasBorderTag(String tag);

	/**
	 * Set the name of the given NucleusBorderPoint
	 * @param name the new name
	 * @param i the index of the border point
	 */
	public void addBorderTag(String name, int i);
	
	/**
	 * Set the name of the given NucleusBorderPoint
	 * @param tag the new tag to use as a name
	 * @param i the index of the border point
	 */
//	public void addBorderTag(BorderTag tag, int i);

//	/**
//	 * Add a name to the given segment
//	 * @param name the new name
//	 * @param i the segment number
//	 */
//	public void addSegmentTag(String name, int i);
//
//	/**
//	 * Remove all segments from this nucleus
//	 */
//	public void clearSegments();

	/**
	 * Calculate a new angle profile with the given window size. It
	 * will replace the existing angle profile. It takes the point, 
	 * a point <windowSize> behind and <windowSize> ahead, and calculates
	 * the interior angle between them.
	 * @param angleProfileWindowSize the window size
	 * @throws Exception 
	 */
	public void calculateAngleProfile(int angleProfileWindowSize) throws Exception;


	/**
	 * Set the segmention of the nucleus to the given list
	 * @param newList the list of segments
	 */
//	public void setSegments(List<NucleusBorderSegment> newList);
	
	
	public SignalCollection getSignalCollection();
	
	
	public int getSignalCount();
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
	
	public Map<String, Integer> getBorderTags();
	
//	public void addSegment(NucleusBorderSegment n);
	public void reverse() throws Exception;
	public String getOutputFolderName();
	public void updateSourceFolder(File newFolder);
}