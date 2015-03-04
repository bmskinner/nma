package no.nuclei;

import java.lang.*;
import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.FloatPolygon;

import no.components.XYPoint;
import no.components.AngleProfile;
import no.components.NucleusBorderPoint;
import no.components.NuclearSignal;

public interface INuclearFunctions
{
  
	public void findPointsAroundBorder();
	public Roi getRoi();
	public String getPath();

	public String getPosition();

	public File getSourceFile();

	public File getNucleusFolder();
	public ImagePlus getSourceImage();

	public ImagePlus getAnnotatedImage();

	public ImagePlus getEnlargedImage();

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

	public FloatPolygon getSmoothedPolygon();
	
	public double getArea();

	public double getFeret();

	public double getPathLength();

	public double getPerimeter();

	public AngleProfile getAngleProfile();

	public double[] getDistanceProfile();

	public int getLength();

	public double[] getInteriorAngles();

  public double[] getInteriorAngles(String pointType);

	public double getMedianInteriorAngle();

	public NucleusBorderPoint getBorderPoint(int i);

	public int getFailureCode();

	public boolean hasRedSignal();

	public boolean hasGreenSignal();


	public HashMap<String, NucleusBorderPoint> getBorderPointsOfInterest();

  public NucleusBorderPoint getBorderPointOfInterest(String name);

  public int getBorderIndexOfInterest(String name);

  public int getOffsetIndex(NucleusBorderPoint indexPoint, String referencePoint);

	
	/*
    -----------------------
    Protected setters for subclasses
    -----------------------
  */

  public void setCentreOfMass(XYPoint d);

  public void setPolygon(FloatPolygon p);

  public void updateFailureCode(int i);

  public void setMinSignalSize(double d);

  public void setMaxSignalFraction(double d);

  public void setSignalThreshold(int i);

  public void setBorderPointsOfInterest( HashMap<String, NucleusBorderPoint> b);

  /*
    -----------------------
    Get aggregate values
    -----------------------
  */
  public double getMaxX();

  public double getMinX();

  public double getMaxY();

  public double getMinY();

  public int getRedSignalCount();

  public int getGreenSignalCount();

  public double getMedianDistanceFromProfile();

  public double getDifferenceToMedianProfile(String pointType);

  /*
    -----------------------
    Set miscellaneous features
    -----------------------
  */

  public void setPathLength(double d);

  public void calculatePathLength();


  public void addBorderPointOfInterest(String name, NucleusBorderPoint p);

  public void addDifferenceToMedianProfile(String pointType, double value);

  /*
    -----------------------
    Process signals
    -----------------------
  */

  public ArrayList<NuclearSignal> getRedSignals();

  public ArrayList<NuclearSignal> getGreenSignals();

  public void addRedSignal(NuclearSignal n);

  public void addGreenSignal(NuclearSignal n);

  public double[][] getSignalDistanceMatrix();

  /*
    Find the difference to the given median
  */
  public double calculateDifferenceToProfile(double[] testProfile, String pointType);

  
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

  public double[] getNormalisedProfilePositions();

  public double[] getRawProfilePositions();

  /*
    -----------------------
    Exporting data
    -----------------------
  */

  public void annotateFeatures();

  public double findRotationAngle();

	public void calculateSignalAnglesFromPoint(NucleusBorderPoint p);
  public void exportSignalDistanceMatrix();

  /*
    Print key data to the image log file
    Overwrites any existing log
  */   
  public void exportAngleProfile();

  /*
    Export the current image state, with
    any annotations to export.nn.annotated.tiff
  */
  public void exportAnnotatedImage();

  public void annotateNucleusImage();

   public void dumpInfo();
}