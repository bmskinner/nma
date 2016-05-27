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

import components.CellularComponent;
import components.generic.BorderTag;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclear.NuclearSignal;
import components.nuclear.BorderPoint;
import components.nuclear.SignalCollection;

/**
 * A Nucleus is the interface to all the possible types of nuclei that will be
 * used. 
 * @author bms41
 *
 */
public interface Nucleus extends CellularComponent {
	
	// for debugging - use in calling dumpInfo()
	public static final int ALL_POINTS = 0;
	public static final int BORDER_POINTS = 1;
	public static final int BORDER_TAGS = 2;

	public void findPointsAroundBorder() throws Exception;
	public void intitialiseNucleus(int angleProfileWindowSize) throws Exception;
	
	/**
	 * @return a copy of the data in this nucleus or null on Exception
	 */
	public Nucleus duplicate();

	/**
	 * Get the absolute path to the folder containing
	 * the details of this nucleus. It will have the
	 * format /SourceDir/ImageName/
	 * @return
	 */
	public File getNucleusFolder();
	
	
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
	 * Get the cached bounding rectangle for the nucleus. If not present,
	 * the rectangle is calculated and stored.
	 * 
	 * If the TopVertical and
	 * BottomVertical points have been set, these will be used for vertical alignment. Otherwise,
	 * the given point is moved to directly below the CoM
	 * @param point the point to put at the bottom. Overridden if TOP_  and BOTTOM_ are set
	 * @return
	 * @throws Exception
	 */
	public Rectangle getBoundingRectangle(BorderTag point) throws Exception;
	
	
	/**
	 * Detect the points that can be used for vertical alignment.These are based on the
	 * BorderTags TOP_VERTICAL and BOTTOM_VETICAL. The actual points returned are not
	 * necessarily on the border of the nucleus; a bibble correction is performed on the
	 * line drawn between the two border points, minimising the sum-of-squares to each border
	 * point within the region covered by the line. 
	 * @return
	 */
	public BorderPoint[] getBorderPointsForVerticalAlignment();
	

	/**
	 * Get the narrowest diameter through the centre of mass in pixels
	 * @return
	 * @throws Exception 
	 */
	public double getNarrowestDiameter() throws Exception;

	/**
	 * Calculate the distance from point to point around the 
	 * periphery of the nucleus.
	 * @return
	 * @throws Exception
	 */
	public double getPathLength() throws Exception;

	

	/**
	 * Get the size of the angle profile window in pixels
	 * @return
	 */
	public int getAngleProfileWindowSize();
	
	/**
	 * Set the angle profile window size, and recalculate the angle
	 * profile
	 * @param i
	 * @throws Exception
	 */
	public void setAngleProfileWindowSize(int i) throws Exception;


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
	
	public void calculateFractionalSignalDistancesFromCoM() throws Exception;
	public void calculateSignalDistancesFromCoM();

	/*
    -----------------------
    Protected setters for subclasses
    -----------------------
	 */
	
	public void setOutputFolder(String f);

	/*
    -----------------------
    Determine positions of points
    -----------------------
	 */

	/*
    For two NucleusBorderPoints in a Nucleus, find the point that lies halfway between them
    Used for obtaining a consensus between potential tail positions
	 */
	public int getPositionBetween(BorderPoint pointA, BorderPoint pointB);

	// For a position in the roi, draw a line through the CoM and get the intersection point
	public BorderPoint findOppositeBorder(BorderPoint p);

	/*
    From the point given, create a line to the CoM. Measure angles from all 
    other points. Pick the point closest to 90 degrees. Can then get opposite
    point. Defaults to input point if unable to find point.
	 */
	public BorderPoint findOrthogonalBorderPoint(BorderPoint a);

	/**
	 *  Find the point with the narrowest diameter through the CoM
	 *  using the distance profile
	 * @return
	 * @throws Exception 
	 */
	public BorderPoint getNarrowestDiameterPoint() throws Exception;


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
	public void calculateSignalAnglesFromPoint(BorderPoint p) throws Exception;
	
	public void exportSignalDistanceMatrix();

	
	/**
	 * Get a copy of the angle profile. The first index of the profile
	 * is the first border point in the border list. That is, there is no
	 * consistency to the order of values across multiple nuclei. If consistency
	 * is needed, specify a pointType
	 * @return
	 * @throws Exception 
	 */
	public SegmentedProfile getProfile(ProfileType type) throws Exception;
	
	/**
	 * Get a copy of the angle profile offset to start at the given point
	 * @param pointType the point to start at
	 * @param tag the tag to offset the profile to
	 * @return a copy of the segmented profile
	 * @throws Exception 
	 */
	public SegmentedProfile getProfile(ProfileType type, BorderTag tag) throws Exception;
	
		
	/**
	 * Set the profile for the given type, offsetting to a border tag
	 * @param type
	 * @param profile
	 */
	public void setProfile(ProfileType type, BorderTag tag, SegmentedProfile profile) throws Exception;
	
	/**
	 * Set the profile for the given type
	 * @param type
	 * @param profile
	 * @throws Exception 
	 */
	public void setProfile(ProfileType regular, SegmentedProfile nucleusProfile) throws Exception;
	
	/**
	 * Test if the given type of profile is available
	 * @param type
	 * @return
	 */
	public boolean hasProfile(ProfileType type);
	

	public String dumpInfo(int type);

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
	 * Get a copy of the border point at the given tag,
	 * or null if the tag is not present
	 * @param tag
	 * @return
	 */
	public BorderPoint getBorderTag(BorderTag tag);
	
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
//	public SegmentedProfile calculateAngleProfile(int angleProfileWindowSize) throws Exception;
	public void calculateProfiles() throws Exception;
	
	
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
	 * Get a copy of the border point mapped to the given tag
	 * @param tag
	 * @return
	 */
	public BorderPoint getBorderPoint(BorderTag tag); 
	
	/**
	 * Get the border index of a tag in the border list, 
	 * removing offset to a reference tag
	 * @param reference the border tag with index zero
	 * @param index the index to offset. Should be counting from the reference tag
	 * @return the offset index, or -1 if the reference tag is not present
	 */
	public int getOffsetBorderIndex(BorderTag reference, int index);
	
	/**
	 * Set the lock on this segment of all the profile types
	 * @param lock
	 * @param segID
	 */
	public void setSegmentStartLock(boolean lock, UUID segID);

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
	public void updateSourceFolder(File newFolder);
	
	/**
	 * Get the reason for a nucleus failing checks
	 * @return
	 */
	public int getFailureCode();
	
	/**
	 * Add a failure code to the nucleus
	 * @param failureFeret
	 */
	public void updateFailureCode(int failCode);
	
	
	/*
	 * Rotations
	 */
	
	/**
	 * Given two points in the nucleus, rotate the nucleus so that they are vertical.
	 * @param topPoint the point to have the higher Y value
	 * @param bottomPoint the point to have the lower Y value
	 */
	public void alignPointsOnVertical(BorderPoint topPoint, BorderPoint bottomPoint);
	
	/**
	 * Rotate the nucleus so that the given point is directly 
	 * below the centre of mass
	 * @param bottomPoint
	 */
	public void rotatePointToBottom(BorderPoint bottomPoint);
	
	
	/**
	 * Fetch the vertically oriented copy of the nucleus. Calculate if 
	 * needed. The vertical alignment with be by TOP_VERTICAL and BOTTOM_VERTICAL
	 * if available, otherwise the ORIENTATION_POINT will be rotated below the CoM
	 * @return
	 */
	public Nucleus getVerticallyRotatedNucleus();
	
	
	/**
	 * Invalidate the existing cached vertically rotated nucleus,
	 * and recalculate.
	 */
	public void updateVerticallyRotatedNucleus();
	
	/**
	 * Rotate the nucleus by the given amount around the centre of mass
	 * @param angle
	 */
	public void rotate(double angle);
	
	
	/**
	 * Store an internal record of loggable activity
	 * @param message
	 */
	public void log(String message);
	
	/**
	 * Fetch the current nucleus log
	 * @return
	 */
	public String printLog();
		
}