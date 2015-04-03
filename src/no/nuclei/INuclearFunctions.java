package no.nuclei;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import no.components.SignalCollection;
import no.components.XYPoint;
import no.components.Profile;
import no.components.NucleusBorderPoint;
import no.components.NucleusBorderSegment;
import no.components.NuclearSignal;

public interface INuclearFunctions
{
  
	public void findPointsAroundBorder();
  public void intitialiseNucleus(int angleProfileWindowSize);

  // public Nucleus copy();

	public Roi getRoi();
  
	public String getPath();

	public String getPosition();

	public File getSourceFile();

	public File getNucleusFolder();
//	public ImagePlus getSourceImage();
	
	public ImageStack getImagePlanes();

	public ImagePlus getAnnotatedImage();

	public ImageStack getEnlargedPlanes();

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

	public FloatPolygon getPolygon();
	
	public double getArea();

	public double getFeret();
	
	public double getNarrowestDiameter();

	public double getPathLength();

	public double getPerimeter();

	public Profile getAngleProfile();

  public int getAngleProfileWindowSize();

	public Profile getDistanceProfile();

	public int getLength();

	public NucleusBorderPoint getBorderPoint(int i);

	public int getFailureCode();
	
	public boolean hasSignal(int channel);

	public boolean hasRedSignal();

	public boolean hasGreenSignal();

  public List<NucleusBorderPoint> getBorderList();


	
	/*
    -----------------------
    Protected setters for subclasses
    -----------------------
  */
  public void setOutputFolder(String f);

  public void setCentreOfMass(XYPoint d);

  public void setPolygon(FloatPolygon p);

  public void updateFailureCode(int i);

  public void setMinSignalSize(double d);

  public void setMaxSignalFraction(double d);

  public void setSignalThreshold(int i);

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

//  public int getRedSignalCount();
//
//  public int getGreenSignalCount();

  /*
    -----------------------
    Set miscellaneous features
    -----------------------
  */

  public void setPathLength(double d);

  public void calculatePathLength();


  /*
    -----------------------
    Process signals
    -----------------------
  */

//  public List<NuclearSignal> getRedSignals();
//
//  public List<NuclearSignal> getGreenSignals();

  public void addRedSignal(NuclearSignal n);

  public void addGreenSignal(NuclearSignal n);

//  public double[][] getSignalDistanceMatrix();

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

  public void annotateFeatures();

  public double findRotationAngle();

	public void calculateSignalAnglesFromPoint(NucleusBorderPoint p);
  public void exportSignalDistanceMatrix();

  /*
    Print key data to the image log file
    Overwrites any existing log
  */   
  public void exportAngleProfile();
  
  public void exportSegments();

  /*
    Export the current image state, with
    any annotations to export.nn.annotated.tiff
  */
  public void exportAnnotatedImage();

  public void annotateNucleusImage();
  
  public void exportProfilePlotImage();

  public void dumpInfo(int type);

  public Profile getAngleProfile(String pointType);

  public double getAngle(int index);

  public int getIndex(NucleusBorderPoint p);

  public double getDistance(int index);

  public void updatePoint(int i, double x, double y);

  public NucleusBorderPoint getBorderTag(String s);

  public int getBorderIndex(String s);

  public Set<String> getTags();
  
  public List<NucleusBorderSegment> getSegments();

  public NucleusBorderSegment getSegmentTag(String s);

  public void addBorderTag(String name, int i);

  public void addSegmentTag(String name, int i);

  public void clearSegments();

  public void calculateAngleProfile(int angleProfileWindowSize);

  public void updatePolygon();

  public void setSegments(List<NucleusBorderSegment> newList);
  public SignalCollection getSignalCollection();
  public int getSignalCount();
  public int getSignalCount(int channel);
  public List<NuclearSignal> getSignals(int channel);
  public List<List<NuclearSignal>> getSignals();
  public Set<Integer> getSignalChannels();
  public Map<String, Integer> getBorderTags();
  public void addSegment(NucleusBorderSegment n);
  public void reverse();
}