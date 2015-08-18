package no.nuclei;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import no.components.SignalCollection;
import no.components.XYPoint;
import no.components.Profile;
import no.components.NucleusBorderPoint;
import no.components.NucleusBorderSegment;
import no.components.NuclearSignal;

public interface Nucleus {

	// index values for selecting original positions
	// indices in  the originalPositions array
	public static final int X_BASE = 0;
	public static final int Y_BASE = 1;
	public static final int WIDTH = 2;
	public static final int HEIGHT = 3;

	public void findPointsAroundBorder();
	public void intitialiseNucleus(int angleProfileWindowSize);

	// public Nucleus copy();

	public UUID getID();

	public String getPath();

	public double[] getPosition();

	public void setPosition(double[] d);

	public File getSourceFile();

	public File getNucleusFolder();

	public String getImageName();

	public String getAnnotatedImagePath();

	public String getOriginalImagePath();

	public String getEnlargedImagePath();

	public String getImageNameWithoutExtension();

	public File getOutputFolder();

	public String getDirectory();

	public String getPathWithoutExtension();

	public int getNucleusNumber();

	public String getPathAndNumber();

	public XYPoint getCentreOfMass();

	public NucleusBorderPoint getPoint(int i);


	public double getArea();

	public double getFeret();

	public double getNarrowestDiameter();

	public double getPathLength();

	public double getPerimeter();

	public Profile getAngleProfile();

	public int getAngleProfileWindowSize();

	public Profile getDistanceProfile();

	public int getLength();
	
	
	/**
	 * Get the length of a pixel in metres
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

	public void setPathLength(double d);

	public void calculatePathLength();

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
	public NucleusBorderPoint findPointClosestToLocalMaximum(NucleusBorderPoint[] list);

	/*
    This will find the point in a list that is closest to any local minimum
    in the border profile, wherever that minimum may be
	 */
	public NucleusBorderPoint findPointClosestToLocalMinimum(NucleusBorderPoint[] list);


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

	//  public void annotateFeatures();

	public double findRotationAngle();

	public void calculateSignalAnglesFromPoint(NucleusBorderPoint p);
	public void exportSignalDistanceMatrix();

	/*
    Print key data to the image log file
    Overwrites any existing log
	 */   
	public void exportAngleProfile();

	public void exportSegments();


	public Map<String, Integer> getSegmentMap( );
	public Profile getSingleDistanceProfile();

	public void dumpInfo(int type);

	public Profile getAngleProfile(String pointType);

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
	 * Get the index of the border point with the given tag.
	 * If the point does not exist, returns -1
	 * @param s the tag name
	 * @return the index of the border in borderList
	 */
	public int getBorderIndex(String s);

	/**
	 * Get a set of all the tags present within this nucleus
	 * @return
	 */
	public Set<String> getTags();

	/**
	 * Get a list of all the segments within the nucleus
	 * @return
	 */
	public List<NucleusBorderSegment> getSegments();
	
	/**
	 * Create a list of segments offset to a reference point. This point
	 * will be the new zero index in the segment list
	 * @param pointType the border tag to offset against
	 */
	public List<NucleusBorderSegment> getSegments(String pointType);

	/**
	 * Get the segment with the given name
	 * @param s the name
	 * @return
	 */
	public NucleusBorderSegment getSegmentTag(String s);

	/**
	 * Set the name of the given NucleusBorderPoint
	 * @param name the new name
	 * @param i the index of the border point
	 */
	public void addBorderTag(String name, int i);

	/**
	 * Add a name to the given segment
	 * @param name the new name
	 * @param i the segment number
	 */
	public void addSegmentTag(String name, int i);

	/**
	 * Remove all segments from this nucleus
	 */
	public void clearSegments();

	/**
	 * Calculate a new angle profile with the given window size. It
	 * will replace the existing angle profile
	 * @param angleProfileWindowSize the window size
	 */
	public void calculateAngleProfile(int angleProfileWindowSize);


	/**
	 * Set the segmention of the nucleus to the given list
	 * @param newList the list of segments
	 */
	public void setSegments(List<NucleusBorderSegment> newList);
	
	
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
	
	public void addSegment(NucleusBorderSegment n);
	public void reverse();
	public String getOutputFolderName();
	public void updateSourceFolder(File newFolder);
}