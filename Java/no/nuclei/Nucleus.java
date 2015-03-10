/*
	-----------------------
	NUCLEUS CLASS
	-----------------------
	Contains the variables for storing a nucleus,
	plus the functions for calculating aggregate stats
	within a nucleus
*/  
package no.nuclei;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.ProgressBar;
import ij.gui.TextRoi;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;
import ij.measure.ResultsTable;
import ij.measure.SplineFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Color;
import java.awt.geom.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.HashMap;
import no.analysis.Detector;
import no.collections.NucleusCollection;
import no.utility.*;
import no.components.*;


public class Nucleus 
	implements no.nuclei.INuclearFunctions
{

	public static final int RED_CHANNEL   = 0;
	public static final int GREEN_CHANNEL = 1;
	public static final int BLUE_CHANNEL  = 2;
	public static final int NOT_RED_CHANNEL  = 3;
	public static final int NOT_GREEN_CHANNEL  = 4;

	// for debugging - use in calling dumpInfo()
	public static final int ALL_POINTS = 0;
	public static final int BORDER_POINTS = 1;
	public static final int BORDER_TAGS = 2;


	// Values for deciding whether an object is a signal
	private int    signalThreshold = 70;
	private double minSignalSize  = 5; // how small can a signal be
	private double maxSignalFraction = 0.5; // allow up to 50% of nucleus to be signal

	public static final String IMAGE_PREFIX = "export.";

	private int nucleusNumber; // the number of the nucleus in the current image
	private int failureCode = 0; // stores a code to explain why the nucleus failed filters

	private int angleProfileWindowSize;

	// private double medianAngle; // the median interior angle
	private double perimeter;   // the nuclear perimeter
	private double pathLength;  // the angle path length - measures wibbliness in border
	private double feret;       // the maximum diameter
	private double area;        // the nuclear area

	private String position; // the position of the centre of the ROI bounding rectangle in the original image as "x.y"

	// private AngleProfile angleProfile; // the border points of the nucleus, and associated angles

	/*
		The following fields are part of the redesign of the whole system. Instead of storing border points within
		an AngleProfile, they will be part of the Nucleus. The Profiles can take any double[] of values, and
		manipulate them. BorderPoints can be combined into BorderSegments, which may overlap. No copies of the 
		BorderPoints are made; everything references the copy in the Nucleus. Given this, the points of interest 
		(now borderTags) need only to be indexes.
	*/
	private Profile angleProfile; // 
	private Profile distanceProfile; // 
	private List<NucleusBorderPoint> borderList = new ArrayList<NucleusBorderPoint>(0); // eventually to replace angleProfile
	private List<NucleusBorderSegment> segmentList = new ArrayList<NucleusBorderSegment>(0); // expansion for e.g acrosome
	private Map<String, Integer> borderTags  = new HashMap<String, Integer>(0); // to replace borderPointsOfInterest; <tag, index>
	private Map<String, Integer> segmentTags = new HashMap<String, Integer>(0);

	private XYPoint centreOfMass;

	// store points of interest around the border e.g. heads, tails, any other features of note
	// these are mutable
	// private HashMap<String, NucleusBorderPoint> borderPointsOfInterest = new HashMap<String, NucleusBorderPoint>();

	private File sourceFile;    // the image from which the nucleus came
	private File nucleusFolder; // the folder to store nucleus information
	// private File profileLog;    // unused. Store output if needed
	private String outputFolder;  // the top-level path in which to store outputs; has analysis date
	
	private Roi roi; // the original ROI

	private ImagePlus sourceImage;    // a copy of the input nucleus. Not to be altered
	private ImagePlus annotatedImage; // a copy of the input nucleus for annotating
	private ImagePlus enlargedImage; // a copy of the input nucleus for use in later reanalyses that need a particle detector

	private List<NuclearSignal> redSignals   = new ArrayList<NuclearSignal>(0); // an array to hold any signals detected
	private List<NuclearSignal> greenSignals = new ArrayList<NuclearSignal>(0); // an array to hold any signals detected

	private FloatPolygon smoothedPolygon; // the interpolated polygon; source of XYPoint[] smoothedArray // can probably be removed

	// private double[] distanceProfile; // diameter through the CoM for each point

	// private Map<String, Double> differencesToMedianProfile = new HashMap<String, Double>(); // store the difference between curves; move to collection

	private double[][] distancesBetweenSignals; // the distance between all signals as a matrix
	
	public Nucleus (Roi roi, File file, ImagePlus image, ImagePlus enlarged, int number, String position) { // construct from an roi

		// assign main features
		this.roi             = roi;
		this.sourceImage     = image;
		this.annotatedImage  = image; // NEEDS TO BE A COPY
		this.enlargedImage   = enlarged;
		this.sourceFile      = file;
		this.nucleusNumber   = number;
		this.position        = position;
	}

	public Nucleus(){
		// for subclasses to access
	}

	public Nucleus(Nucleus n){
		this.setRoi(n.getRoi());
		this.setPosition(n.getPosition());
		this.setSourceImage(n.getSourceImage());
		this.setSourceFile(n.getSourceFile());
		this.setAnnotatedImage(n.getAnnotatedImage());
		this.setEnlargedImage(n.getEnlargedImage());
		this.setNucleusNumber(n.getNucleusNumber());
		this.setNucleusFolder(n.getNucleusFolder());
		this.setPerimeter(n.getPerimeter());
		this.setPathLength(n.getPathLength());
		this.setFeret(n.getFeret());
		this.setArea(n.getArea());
		this.setAngleProfile(n.getAngleProfile());
		this.setCentreOfMass(n.getCentreOfMass());
		this.setRedSignals(n.getRedSignals());
		this.setGreenSignals(n.getGreenSignals());
		this.setPolygon(n.getPolygon());
		this.setDistanceProfile(n.getDistanceProfile());
		this.setSignalDistanceMatrix(n.getSignalDistanceMatrix());
		this.setBorderTags(n.getBorderTags());
		this.setOutputFolder(n.getOutputFolderName());
		this.setBorderList(n.getBorderList());
	}

	public void findPointsAroundBorder(){
	}

	public void intitialiseNucleus(int angleProfileWindowSize){

		this.nucleusFolder = new File(this.getOutputFolder().getAbsolutePath()+File.separator+this.getImageNameWithoutExtension());

		if (!this.nucleusFolder.exists()) {
			try{
				this.nucleusFolder.mkdir();
			} catch(Exception e) {
				IJ.log("Failed to create directory: "+e);
				IJ.log("Attempt: "+this.nucleusFolder.toString());
			}
		}

		try{
			String outPath = this.getOriginalImagePath();
			IJ.saveAsTiff(this.sourceImage, outPath);

			outPath = this.getEnlargedImagePath();
			IJ.saveAsTiff(this.enlargedImage, outPath);
		 } catch(Exception e){
				IJ.log("Error saving original images: "+e);
		 }

		this.smoothedPolygon = roi.getInterpolatedPolygon(1,true);
		for(int i=0; i<this.smoothedPolygon.npoints; i++){
			borderList.add(new NucleusBorderPoint( this.smoothedPolygon.xpoints[i], this.smoothedPolygon.ypoints[i]));
		}

		// calculate angle profile
		try{
			this.calculateAngleProfile(angleProfileWindowSize);
		 } catch(Exception e){
			 IJ.log("Cannot create angle profile: "+e);
		 } 

		 // calc distances around nucleus through CoM
		 this.calculateDistanceProfile();
		 this.calculatePathLength();
	}

	// find and measure signals. Call after constructor to allow alteration of 
	// thresholding and size parameters
	public void detectSignalsInNucleus(){
		this.measureSignalsInNucleus();
		this.calculateSignalDistancesFromCoM();
		this.calculateFractionalSignalDistancesFromCoM();
	}

	/*
		-----------------------
		Getters for basic values within nucleus
		-----------------------
	*/

	// public INuclearFunctions copy(){
	// 	return new Nucleus(this);
	// }

	public Roi getRoi(){
		return this.roi;
	}

	public String getPath(){
		return this.sourceFile.getAbsolutePath();
	}

	// defensive copy
	public String getPosition(){
		return new String(this.position);
	}

	public File getSourceFile(){
		return new File(this.sourceFile.getAbsolutePath());
	}

	public File getNucleusFolder(){
		return new File(this.nucleusFolder.getAbsolutePath());
	}

	public ImagePlus getSourceImage(){
		return this.sourceImage;
	}

	public ImagePlus getAnnotatedImage(){
		return this.annotatedImage;
	}

	public ImagePlus getEnlargedImage(){
		return this.enlargedImage;
	}

	public String getImageName(){
		return new String(this.sourceFile.getName());
	}

	public String getAnnotatedImagePath(){
		String outPath = this.nucleusFolder.getAbsolutePath()+
											File.separator+
											this.IMAGE_PREFIX+
											this.getNucleusNumber()+
											".annotated.tiff";
		return new String(outPath);
	}

	public String getOriginalImagePath(){
		String outPath = this.nucleusFolder.getAbsolutePath()+
											File.separator+
											this.IMAGE_PREFIX+
											this.getNucleusNumber()+
											".original.tiff";
		return new String(outPath);
	}

	public String getEnlargedImagePath(){
		String outPath = this.nucleusFolder.getAbsolutePath()+
											File.separator+
											this.IMAGE_PREFIX+
											this.getNucleusNumber()+
											".enlarged.tiff";
		return outPath;
	}

	public String getImageNameWithoutExtension(){
		String extension = "";
		String trimmed = "";

		int i = this.getImageName().lastIndexOf('.');
		if (i > 0) {
				extension = this.getImageName().substring(i+1);
				trimmed   = this.getImageName().substring(0,i);
		}
		return trimmed;
	}

	public String getOutputFolderName(){
		return this.outputFolder;
	}

	public File getOutputFolder(){
		return new File(this.getDirectory()+File.separator+this.outputFolder);
	}

	public String getDirectory(){
		return this.sourceFile.getParent();
	}

	public String getPathWithoutExtension(){
		
		String extension = "";
		String trimmed = "";

		int i = this.getPath().lastIndexOf('.');
		if (i > 0) {
				extension = this.getPath().substring(i+1);
				trimmed = this.getPath().substring(0,i);
		}
		return trimmed;
	}  

	public int getNucleusNumber(){
		return this.nucleusNumber;
	}

	public String getPathAndNumber(){
		return this.sourceFile+File.separator+this.nucleusNumber;
	}

	public XYPoint getCentreOfMass(){
		return new XYPoint(this.centreOfMass.getX(), this.centreOfMass.getY());
	}

	public NucleusBorderPoint getPoint(int i){
		return new NucleusBorderPoint(this.borderList.get(i));
	}

	public FloatPolygon getPolygon(){
		return this.smoothedPolygon;
	}
	
	public double getArea(){
		return this.area;
	}

	public double getFeret(){
		return this.feret;
	}

	public double getPathLength(){
		return this.pathLength;
	}

	public double getPerimeter(){
		return this.perimeter;
	}

	public int getLength(){
		return this.borderList.size();
	}

	public NucleusBorderPoint getBorderPoint(int i){
		return new NucleusBorderPoint(this.getPoint(i));
	}

	public List<NucleusBorderPoint> getBorderList(){
		List<NucleusBorderPoint> result = new ArrayList<NucleusBorderPoint>(0);
		for(NucleusBorderPoint n : borderList){
			result.add(new NucleusBorderPoint(n));
		}
		return result;
	}

	public int getAngleProfileWindowSize(){
		return this.angleProfileWindowSize;
	}

	public int getFailureCode(){
		return this.failureCode;
	}

	public boolean hasRedSignal(){
		if(this.getRedSignalCount()>0){
			return true;
		} else {
			return false;
		}
	}

	public boolean hasGreenSignal(){
		if(this.getGreenSignalCount()>0){
			return true;
		} else {
			return false;
		}
	}

	/*
		-----------------------
		Protected setters for subclasses
		-----------------------
	*/

	public void setOutputFolder(String f){
		this.outputFolder = f;
	}

	public void setPosition(String p){
		this.position = p;
	}

	// protected void setMedianAngle(double d){
	//   this.medianAngle = d;
	// }

	public void setPerimeter(double d){
		this.perimeter = d;
	}

	public void setFeret(double d){
		this.feret = d;
	}

	public void setArea(double d){
		this.area = d;
	}


	public void setCentreOfMass(XYPoint d){
		this.centreOfMass = new XYPoint(d);
	}

	protected void setRedSignals(List<NuclearSignal> d){
		this.redSignals = d;
	}

	protected void setGreenSignals(List<NuclearSignal> d){
		this.greenSignals = d;
	}

	public void setPolygon(FloatPolygon p){
		this.smoothedPolygon = p;
	}


	protected void setSignalDistanceMatrix(double[][] d){
		this.distancesBetweenSignals = d;
	}

	protected void setRoi(Roi d){
		this.roi = d;
	}

	protected void setSourceImage(ImagePlus d){
		this.sourceImage = d.duplicate();
	}

	protected void setSourceFile(File d){
		this.sourceFile = d;
	}

	protected void setAnnotatedImage(ImagePlus d){
		this.annotatedImage = d.duplicate();
	}

	protected void setEnlargedImage(ImagePlus d){
		this.enlargedImage = d.duplicate();
	}

	protected void setNucleusNumber(int d){
		this.nucleusNumber = d;
	}

	protected void setNucleusFolder(File d){
		this.nucleusFolder = d;
	}

	public void updateFailureCode(int i){
		this.failureCode = this.failureCode | i;
	}

	public void setMinSignalSize(double d){
		this.minSignalSize = d;
	}

	public void setMaxSignalFraction(double d){
		this.maxSignalFraction = d;
	}

	public void setSignalThreshold(int i){
		this.signalThreshold = i;
	}

	public void setAngleProfileWindowSize(int i){
		this.angleProfileWindowSize = i;
	}

	public void setBorderList(List<NucleusBorderPoint> list){
		this.borderList = list;
	}

	/*
		-----------------------
		Get aggregate values
		-----------------------
	*/
	public double getMaxX(){
		double d = 0;
		for(int i=0;i<getLength();i++){
			if(this.borderList.get(i).getX()>d){
				d = this.borderList.get(i).getX();
			}
		}
		return d;
	}

	public double getMinX(){
		double d = getMaxX();
		for(int i=0;i<getLength();i++){
			if(this.borderList.get(i).getX()<d){
				d = this.borderList.get(i).getX();
			}
		}
		return d;
	}

	public double getMaxY(){
		double d = 0;
		for(int i=0;i<getLength();i++){
			if(this.borderList.get(i).getY()>d){
				d = this.borderList.get(i).getY();
			}
		}
		return d;
	}

	public double getMinY(){
		double d = getMaxY();
		for(int i=0;i<getLength();i++){
			if(this.borderList.get(i).getY()<d){
				d = this.borderList.get(i).getY();
			}
		}
		return d;
	}

	public int getRedSignalCount(){
		return redSignals.size();
	}

	public int getGreenSignalCount(){
		return greenSignals.size();
	}

	/*
		-----------------------
		Set miscellaneous features
		-----------------------
	*/

	public void setPathLength(double d){
		this.pathLength = d;
	}

	public void calculatePathLength(){
		double pathLength = 0;

		XYPoint prevPoint = new XYPoint(0,0);
		 
		for (int i=0; i<this.getLength();i++ ) {
				double normalisedX = ((double)i/(double)this.getLength())*100; // normalise to 100 length

				// calculate the path length as if it were a border
				XYPoint thisPoint = new XYPoint(normalisedX,this.getAngle(i));
				pathLength += thisPoint.getLengthTo(prevPoint);
				prevPoint = thisPoint;
		}
		this.setPathLength(pathLength);
	}


	/*
		-----------------------
		Process and fetch signals
		-----------------------
	*/

	private void measureSignalsInNucleus(){

		// find the signals
		// within nuclear roi, analyze particles in colour channels
		for(int i=0;i<2;i++){

			int channel = i;

			Detector detector = new Detector();
			detector.setMaxSize(this.getArea() * this.maxSignalFraction);
			detector.setMinSize(this.minSignalSize);
			detector.setMinCirc(0);
			detector.setMaxCirc(1);
			detector.setThreshold(this.signalThreshold);
			detector.setChannel(channel);
			detector.run(this.sourceImage);
			Map<Roi, HashMap<String, Double>> map = detector.getRoiMap();

			Set<Roi> keys = map.keySet();
			for( Roi r : keys){
				Map<String, Double> values = map.get(r);
				NuclearSignal n = new NuclearSignal( r, 
													 values.get("Area"), 
													 values.get("Feret"), 
													 values.get("Perim"), 
													 new XYPoint(values.get("XM"), values.get("YM")));

				if(i==RED_CHANNEL)
					this.addRedSignal(n);

				if(i==GREEN_CHANNEL)
					this.addGreenSignal(n);
			}
		} 
	}

	public List<NuclearSignal> getRedSignals(){
		List<NuclearSignal> result = new ArrayList<NuclearSignal>(0);
		for( NuclearSignal n : this.redSignals){
			result.add(new NuclearSignal(n));
		}
		return result;
	}

	public List<NuclearSignal> getGreenSignals(){
		List<NuclearSignal> result = new ArrayList<NuclearSignal>(0);
		for( NuclearSignal n : this.greenSignals){
			result.add(new NuclearSignal(n));
		}
		return result;
	}

	public void addRedSignal(NuclearSignal n){
		this.redSignals.add(n);
	}

	public void addGreenSignal(NuclearSignal n){
		this.greenSignals.add(n);
	}

	 /*
		For each signal within the nucleus, calculate the distance to the nCoM
		and update the signal
	*/
	private void calculateSignalDistancesFromCoM(){

		List<List<NuclearSignal>> signals = new ArrayList<List<NuclearSignal>>(0);
		signals.add(redSignals);
		signals.add(greenSignals);

		for( List<NuclearSignal> signalGroup : signals ){

			if(signalGroup.size()>0){
				for(int i=0;i<signalGroup.size();i++){
					NuclearSignal n = signalGroup.get(i);

					double distance = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());
					n.setDistanceFromCoM(distance);
				}
			}
		}
	}

	/*
		Calculate the distance from the nuclear centre of
		mass as a fraction of the distance from the nuclear CoM, through the 
		signal CoM, to the nuclear border
	*/
	private void calculateFractionalSignalDistancesFromCoM(){

		this.calculateClosestBorderToSignals();
		List<List<NuclearSignal>> signals = new ArrayList<List<NuclearSignal>>(0);
		signals.add(redSignals);
		signals.add(greenSignals);

		for( List<NuclearSignal> signalGroup : signals ){
		
			if(signalGroup.size()>0){
				for(int i=0;i<signalGroup.size();i++){
					NuclearSignal n = signalGroup.get(i);

					// get the line equation
					Equation eq = new Equation(n.getCentreOfMass(), this.getCentreOfMass());

					// using the equation, get the y postion on the line for each X point around the roi
					double minDeltaY = 100;
					int minDeltaYIndex = 0;
					double minDistanceToSignal = 1000;

					for(int j = 0; j<getLength();j++){
							double x = this.getPoint(j).getX();
							double y = this.getPoint(j).getY();
							double yOnLine = eq.getY(x);
							double distanceToSignal = this.getPoint(j).getLengthTo(n.getCentreOfMass()); // fetch

							double deltaY = Math.abs(y - yOnLine);
							// find the point closest to the line; this could find either intersection
							// hence check it is as close as possible to the signal CoM also
							if(deltaY < minDeltaY && distanceToSignal < minDistanceToSignal){
								minDeltaY = deltaY;
								minDeltaYIndex = j;
								minDistanceToSignal = distanceToSignal;
							}
					}
					NucleusBorderPoint borderPoint = this.getBorderPoint(minDeltaYIndex);
					double nucleusCoMToBorder = borderPoint.getLengthTo(this.getCentreOfMass());
					double signalCoMToNucleusCoM = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());
					double fractionalDistance = signalCoMToNucleusCoM / nucleusCoMToBorder;
					n.setFractionalDistanceFromCoM(fractionalDistance);
				}
			}
		}
	}

	/*
		Go through the signals in the nucleus, and find the point on
		the nuclear ROI that is closest to the signal centre of mass.
	*/
	private void calculateClosestBorderToSignals(){

		List<List<NuclearSignal>> signals = new ArrayList<List<NuclearSignal>>(0);
		signals.add(redSignals);
		signals.add(greenSignals);

		for( List<NuclearSignal> signalGroup : signals ){
		
			if(signalGroup.size()>0){
				for(NuclearSignal s : signalGroup){

					int minIndex = 0;
					double minDistance = this.getFeret();

					for(int j = 0; j<getLength();j++){
						XYPoint p = this.getBorderPoint(j);
						double distance = p.getLengthTo(s.getCentreOfMass());

						// find the point closest to the CoM
						if(distance < minDistance){
							minIndex = j;
							minDistance = distance;
						}
					}
					s.setClosestBorderPoint(minIndex);
				}
			}
		}
	}

	public double[][] getSignalDistanceMatrix(){
		this.calculateDistancesBetweenSignals();
		return this.distancesBetweenSignals;
	}

	private void calculateDistancesBetweenSignals(){

		// create a matrix to hold the data
		// needs to be between every signal and every other signal, irrespective of colour
		int matrixSize = this.getRedSignalCount()+this.getGreenSignalCount();

		this.distancesBetweenSignals = new double[matrixSize][matrixSize];

		// go through the red signals
		for(int i=0;i<this.getRedSignalCount();i++){

			XYPoint aCoM = this.redSignals.get(i).getCentreOfMass();

			// compare to all red
			for(int j=0; j<getRedSignalCount();j++){

				XYPoint bCoM = this.redSignals.get(j).getCentreOfMass();
				this.distancesBetweenSignals[i][j] = aCoM.getLengthTo(bCoM);
			}

			// compare to all green
			for(int j=0; j<getGreenSignalCount();j++){

				int k = j+this.getRedSignalCount(); // offset for matrix

				XYPoint bCoM = this.greenSignals.get(j).getCentreOfMass();

				double distance = 
				this.distancesBetweenSignals[i][k] = aCoM.getLengthTo(bCoM);
			}
		}

		// go through the green signals
		for(int i=0;i<this.getGreenSignalCount();i++){

			int m = i+this.getRedSignalCount(); // offset for matrix

			XYPoint aCoM = this.greenSignals.get(i).getCentreOfMass();

			// and compare to all red
			for(int j=0; j<getRedSignalCount();j++){

				XYPoint bCoM = this.redSignals.get(j).getCentreOfMass();
				this.distancesBetweenSignals[m][j] = aCoM.getLengthTo(bCoM);
			}

			// compare to all green
			for(int j=0; j<getGreenSignalCount();j++){

				int k = j+this.getRedSignalCount(); // offset for matrix

				XYPoint bCoM = this.greenSignals.get(j).getCentreOfMass();

				double distance = 
				this.distancesBetweenSignals[m][k] = aCoM.getLengthTo(bCoM);
			}
		}
	}

	
	/*
		-----------------------
		Determine positions of points
		-----------------------
	*/

	/*
		For two NucleusBorderPoints in a Nucleus, find the point that lies halfway between them
		Used for obtaining a consensus between potential tail positions. Ensure we choose the
		smaller distance
	*/
	public int getPositionBetween(NucleusBorderPoint pointA, NucleusBorderPoint pointB){

		int a = 0;
		int b = 0;
		// find the indices that correspond on the array
		for(int i = 0; i<this.getLength(); i++){
				if(this.getPoint(i).overlaps(pointA)){
					a = i;
				}
				if(this.getPoint(i).overlaps(pointB)){
					b = i;
				}
		}

		// find the higher and lower index of a and b
		int maxIndex = a > b ? a : b;
		int minIndex = a > b ? b : a;

		// there are two midpoints between any points on a ring; we want to take the 
		// midpoint that is in the smaller segment.

		int difference1 = maxIndex - minIndex;
		int difference2 = this.getLength() - difference1;

		// get the midpoint
		int mid1 = NuclearOrganisationUtility.wrapIndex( (int)Math.floor( (difference1/2)+minIndex ),
															this.getLength() );

		int mid2 = NuclearOrganisationUtility.wrapIndex( (int)Math.floor( (difference2/2)+maxIndex ), 
															this.getLength() );

		return difference1 < difference2 ? mid1 : mid2;
	}

	// For a position in the roi, draw a line through the CoM and get the intersection point
	public NucleusBorderPoint findOppositeBorder(NucleusBorderPoint p){

		int minDeltaYIndex = 0;
		double minAngle = 180;

		for(int i = 0; i<this.getLength();i++){
			double angle = findAngleBetweenXYPoints(p, this.getCentreOfMass(), this.getPoint(i));
			if(Math.abs(180 - angle) < minAngle){
				minDeltaYIndex = i;
				minAngle = 180 - angle;
			}
		}
		return this.getPoint(minDeltaYIndex);
	}

	/*
		From the point given, create a line to the CoM. Measure angles from all 
		other points. Pick the point closest to 90 degrees. Can then get opposite
		point. Defaults to input point if unable to find point.
	*/
	public NucleusBorderPoint findOrthogonalBorderPoint(NucleusBorderPoint a){

		NucleusBorderPoint orthgonalPoint = a;
		double bestAngle = 0;

		for(int i=0;i<this.getLength();i++){

			NucleusBorderPoint p = this.getBorderPoint(i);
			double angle = Nucleus.findAngleBetweenXYPoints(a, this.getCentreOfMass(), p); 
			if(Math.abs(90-angle)< Math.abs(90-bestAngle)){
				bestAngle = angle;
				orthgonalPoint = p;
			}
		}
		return orthgonalPoint;
	}

	/*
		This will find the point in a list that is closest to any local maximum
		in the border profile, wherever that maximum may be
	*/
	public NucleusBorderPoint findPointClosestToLocalMaximum(NucleusBorderPoint[] list){

		List<Integer> maxima = this.getAngleProfile().getLocalMaxima(5);
		NucleusBorderPoint closestPoint = new NucleusBorderPoint(0,0);
		double closestDistance = this.getPerimeter();

		for(NucleusBorderPoint p : list){
			for(int m : maxima){
				double distance = p.getLengthTo(getPoint(m));
				if(distance<closestDistance){
					closestPoint = p;
				}
			}
		}
		return closestPoint;
	}

		/*
		This will find the point in a list that is closest to any local minimum
		in the border profile, wherever that minimum may be
	*/
	public NucleusBorderPoint findPointClosestToLocalMinimum(NucleusBorderPoint[] list){

		List<Integer> minima = this.getAngleProfile().getLocalMinima(5);
		NucleusBorderPoint closestPoint = new NucleusBorderPoint(0,0);
		double closestDistance = this.getPerimeter();

		for(NucleusBorderPoint p : list){
			for(int m : minima){
				double distance = p.getLengthTo(getPoint(m));
				if(distance<closestDistance){
					closestPoint = p;
				}
			}
		}
		return closestPoint;
	}


	/*
		Given three XYPoints, measure the angle a-b-c
			a   c
			 \ /
				b
	*/
	public static double findAngleBetweenXYPoints(XYPoint a, XYPoint b, XYPoint c){

		float[] xpoints = { (float) a.getX(), (float) b.getX(), (float) c.getX()};
		float[] ypoints = { (float) a.getY(), (float) b.getY(), (float) c.getY()};
		PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);
	 return roi.getAngle();
	}

	// find the point with the narrowest diameter through the CoM
	// Uses the distance profile
	public NucleusBorderPoint getNarrowestDiameterPoint(){

		double[] distanceArray = this.distanceProfile.asArray();
		double distance = NuclearOrganisationUtility.getMax(distanceArray);
		int index = 0;
		for(int i = 0; i<this.getLength();i++){
			if(distanceArray[i] < distance){
				distance = distanceArray[i];
				index = i;
			}
		}
		return new NucleusBorderPoint(this.getPoint(index));
	}

	public double[] getNormalisedProfilePositions(){
		double[] d = new double[this.getLength()];
		for(int i=0;i<this.getLength();i++){
			d[i] = ( (double)i / (double)this.getLength() ) * 100;
		}
		return d;
	}

	public double[] getRawProfilePositions(){
		double[] d = new double[this.getLength()];
		for(int i=0;i<this.getLength();i++){
			d[i] = i;
		}
		return d;
	}

	/*
		Flip the X positions of the border points around an X position
	*/
	public void flipXAroundPoint(XYPoint p){

		double xCentre = p.getX();
		// for(int i = 0; i<this.borderList.size();i++){
		for(NucleusBorderPoint n : borderList){
			double dx = xCentre - n.getX();
			double xNew = xCentre + dx;
			n.setX(xNew);
		}
	}

	public double getMedianDistanceBetweenPoints(){
		double[] distances = new double[this.borderList.size()];
		for(int i=0;i<this.borderList.size();i++){
			NucleusBorderPoint p = this.getPoint(i);
			NucleusBorderPoint next = this.getPoint( NuclearOrganisationUtility.wrapIndex(i+1, this.borderList.size()));
			distances[i] = p.getLengthTo(next);
		}
		return NuclearOrganisationUtility.quartile(distances, 50);
	}

	/*
		-----------------------
		Exporting data
		-----------------------
	*/

	public void annotateFeatures(){
	}
	
	public double findRotationAngle(){
		return 0;
	}

	public void calculateSignalAnglesFromPoint(NucleusBorderPoint p){
		List<List<NuclearSignal>> signals = new ArrayList<List<NuclearSignal>>(0);
		signals.add(this.getRedSignals());
		signals.add(this.getGreenSignals());

		for( List<NuclearSignal> signalGroup : signals ){

			if(signalGroup.size()>0){

				for(int i=0;i<signalGroup.size();i++){
					NuclearSignal n = signalGroup.get(i);
					double angle = findAngleBetweenXYPoints(p, this.getCentreOfMass(), n.getCentreOfMass());

					// set the final angle
					n.setAngle(angle);
				}
			}
		}
	}

	public void exportSignalDistanceMatrix(){

		this.calculateDistancesBetweenSignals();

		File f = new File(this.nucleusFolder+File.separator+"signalDistanceMatrix.txt");
		if(f.exists()){
			f.delete();
		}

		int matrixSize = this.getRedSignalCount()+this.getGreenSignalCount();

		// Prepare the header line and append to file
		String outLine = "RED\t";
		for(int i=0;i<this.getRedSignalCount();i++){
			outLine = outLine + i + "\t";
		}
		outLine += "GREEN\t"; // distinguish red from green signals in headers
		
		for(int i=0;i<this.getGreenSignalCount();i++){
			outLine = outLine + i + "\t";
		}
		
		// IJ.append(outLine+"\n", f.getAbsolutePath());
		outLine += "\r\n";

		// for each row
		for(int i=0;i<this.getRedSignalCount();i++){
			// for each column of red
			outLine += i+"\t";
			for(int j=0; j<getRedSignalCount();j++){
				outLine += this.distancesBetweenSignals[i][j]+"\t";
			}
			outLine += "|\t";
			// for each column of green
			for(int j=0; j<getGreenSignalCount();j++){
				int k = j+this.getRedSignalCount();
				outLine += this.distancesBetweenSignals[i][k]+"\t";
			}
			// next line
			outLine += "\r\n";
		}
		// add separator line
		outLine += "GREEN\t";
		for(int i=0; i<matrixSize;i++){
			outLine += "--\t";
		}
		 outLine += "\r\n";

		// add green signals
		// for each row
		for(int i=0;i<this.getGreenSignalCount();i++){

			outLine += i+"\t";
			int m = i+this.getRedSignalCount(); // offset for matrix

			// for each column of red
			for(int j=0; j<getRedSignalCount();j++){
				outLine += this.distancesBetweenSignals[m][j]+"\t";
			}
			outLine += "|\t";
			// for each column of green
			for(int j=0; j<getGreenSignalCount();j++){
				int k = j+this.getRedSignalCount();
				outLine += this.distancesBetweenSignals[m][k]+"\t";
			}
			// next line
			outLine += "\r\n";
		}
		IJ.append(outLine, f.getAbsolutePath());
	}

	/*
		Print key data to the image log file
		Overwrites any existing log
	*/   
	public void exportAngleProfile(){

		File f = new File(this.getNucleusFolder().getAbsolutePath()+
											File.separator+
											this.getNucleusNumber()+
											".txt");
		if(f.exists()){
			f.delete();
		}

		String outLine =  "X_INT\t"+
											"Y_INT\t"+
											"X_DOUBLE\t"+
											"Y_DOUBLE\t"+
											"INTERIOR_ANGLE\t"+
											"MIN_ANGLE\t"+
											"INTERIOR_ANGLE_DELTA\t"+
											"INTERIOR_ANGLE_DELTA_SMOOTHED\t"+
											"BLOCK_POSITION\t"+
											"BLOCK_NUMBER\t"+
											"IS_LOCAL_MIN\t"+
											"IS_LOCAL_MAX\t"+
											"IS_MIDPOINT\t"+
											"IS_BLOCK\t"+
											"NORMALISED_PROFILE_X\t"+
											"DISTANCE_PROFILE\r\n";

		for(int i=0;i<this.getLength();i++){

			double normalisedX = ((double)i/(double)this.getLength())*100; // normalise to 100 length
			
			outLine +=  this.getBorderPoint(i).getXAsInt()                      +"\t"+
									this.getBorderPoint(i).getYAsInt()                      +"\t"+
									this.getBorderPoint(i).getX()                           +"\t"+
									this.getBorderPoint(i).getY()                           +"\t"+
									this.getBorderPoint(i).getInteriorAngle()               +"\t"+
									this.getBorderPoint(i).getMinAngle()                    +"\t"+
									this.getBorderPoint(i).getInteriorAngleDelta()          +"\t"+
									this.getBorderPoint(i).getInteriorAngleDeltaSmoothed()  +"\t"+
									this.getBorderPoint(i).getPositionWithinBlock()         +"\t"+
									this.getBorderPoint(i).getBlockNumber()                 +"\t"+
									this.getBorderPoint(i).isLocalMin()                     +"\t"+
									this.getBorderPoint(i).isLocalMax()                     +"\t"+
									this.getBorderPoint(i).isMidpoint()                     +"\t"+
									this.getBorderPoint(i).isBlock()                        +"\t"+
									normalisedX                                             +"\t"+
									this.getBorderPoint(i).getDistanceAcrossCoM()           +"\r\n";
		}
		IJ.append( outLine, f.getAbsolutePath());
	}

	/*
		Export the current image state, with
		any annotations to export.nn.annotated.tiff
	*/
	public void exportAnnotatedImage(){
		String outPath = this.getAnnotatedImagePath();
		IJ.saveAsTiff(annotatedImage, outPath);
	}

	/*
		Annotate image with ROIs
		CoMs of nucleus and signals
		Narrowest diameter across nucleus
	*/
	public void annotateNucleusImage(){ 

		try{

			ImageProcessor ip = this.annotatedImage.getProcessor();

			// draw the features of interest
			
			// draw the outline of the nucleus
			ip.setColor(Color.BLUE);
			ip.setLineWidth(1);
			ip.draw(this.getRoi());


			// draw the CoM
			ip.setColor(Color.MAGENTA);
			ip.setLineWidth(5);
			ip.drawDot(this.getCentreOfMass().getXAsInt(),  this.getCentreOfMass().getYAsInt());

			
			//   SIGNALS
			ip.setLineWidth(3);
			ip.setColor(Color.RED);
			List<NuclearSignal> redSignals = this.getRedSignals();
			if(redSignals.size()>0){
				for(int j=0; j<redSignals.size();j++){
					NuclearSignal s = redSignals.get(j);
					ip.setLineWidth(3);
					ip.drawDot(s.getCentreOfMass().getXAsInt(), s.getCentreOfMass().getYAsInt());
					ip.setLineWidth(1);
					ip.draw(s.getRoi());
				}
			}

			ip.setColor(Color.GREEN);
			List<NuclearSignal> greenSignals = this.getGreenSignals();
			if(redSignals.size()>0){
				for(int j=0; j<greenSignals.size();j++){
					NuclearSignal s = greenSignals.get(j);
					ip.setLineWidth(3);
					ip.setLineWidth(1);
					ip.draw(s.getRoi());
				}
			}

			// The narrowest part of the nucleus
			ip.setLineWidth(1);
			ip.setColor(Color.MAGENTA);
			NucleusBorderPoint narrow1 = this.getNarrowestDiameterPoint();
			NucleusBorderPoint narrow2 = this.findOppositeBorder(narrow1);
			ip.drawLine(narrow1.getXAsInt(), narrow1.getYAsInt(), narrow2.getXAsInt(), narrow2.getYAsInt());

		} catch(Exception e){
			IJ.log("Error annotating nucleus: "+e);
		}
	}

	 /*
		Get a readout of the state of the nucleus
		Used only for debugging
	*/
	public void dumpInfo(int type){
		IJ.log("Dumping nucleus info:");
		IJ.log("    CoM: "+this.getCentreOfMass().getX()+", "+this.getCentreOfMass().getY());
		if(type==ALL_POINTS || type==BORDER_POINTS){
			IJ.log("    Border:");
			for(int i=0; i<this.getLength(); i++){
				NucleusBorderPoint p = this.getBorderPoint(i);
				IJ.log("      Index "+i+": "+p.getX()+"    "+p.getY());
			}
		}
		if(type==ALL_POINTS || type==BORDER_TAGS){
			IJ.log("    Points of interest:");
			Map<String, Integer> pointHash = this.getBorderTags();
			Set<String> keys = pointHash.keySet();
			for(String s : keys){
			 NucleusBorderPoint p = getPoint(pointHash.get(s));
			 IJ.log("    "+s+": "+p.getX()+"    "+p.getY()+" at index "+pointHash.get(s));
			}
		}
	}

	/*
		-----------------------
		Methods for the new architecture only
		-----------------------
	*/

	public Profile getAngleProfile(){
		return new Profile(this.angleProfile);
	}

	// returns a copy
	public Profile getAngleProfile(String pointType){ // USE getAngleProfile
		int offset = this.borderTags.get(pointType);
		return new Profile(this.angleProfile.offset(offset));
	}

	public void setAngleProfile(Profile p){
		this.angleProfile = new Profile(p);
	}

	public double getAngle(int index){
		return this.angleProfile.get(index);
	}

	public int getIndex(NucleusBorderPoint p){
		int i = 0;
		for(NucleusBorderPoint n : borderList){
			if( n.getX()==p.getX() && n.getY()==p.getY()){
				return i;
			}
			i++;
		}
		IJ.log("Error: cannot find border point in Nucleus.getIndex()");
		return -1; // default if no match found
	}

	public Profile getDistanceProfile(){
		return new Profile(this.distanceProfile);
	}

	public void setDistanceProfile(Profile p){
		this.distanceProfile = new Profile(p);
	}

	public double getDistance(int index){
		return this.distanceProfile.get(index);
	}

	public void updatePoint(int i, double x, double y){
		this.borderList.get(i).setX(x);
		this.borderList.get(i).setY(y);
	}

	// Ensure only copies of border points get returned
	public NucleusBorderPoint getBorderTag(String s){
		NucleusBorderPoint result = new NucleusBorderPoint(0,0);
		if(this.getBorderIndex(s)>-1){
			result = new NucleusBorderPoint(this.borderList.get(this.getBorderIndex(s)));
		}
		// } else {
		// 	IJ.log("    Error: cannot find border tag in Nucleus.getBorderTag(\""+s+"\")");
		// }
		return result;
	}

	public Map<String, Integer> getBorderTags(){
		return this.borderTags;
	}

	public void setBorderTags(Map<String, Integer> m){
		this.borderTags = m;
	}

	public int getBorderIndex(String s){
		int result = -1;
		if(this.borderTags.containsKey(s)){
			result = this.borderTags.get(s);
		}
		return result;
	}

	public Set<String> getTags(){
		return this.borderTags.keySet();
	}

	public NucleusBorderSegment getSegmentTag(String s){
		return new NucleusBorderSegment(this.segmentList.get(this.segmentTags.get(s)));
	}

	public void addBorderTag(String name, int i){
		this.borderTags.put(name, i);
	}

	public void addSegmentTag(String name, int i){
		this.segmentTags.put(name, i);
	}

	private void calculateDistanceProfile(){

		double[] profile = new double[this.getLength()];

		for(int i = 0; i<this.getLength();i++){

				NucleusBorderPoint p   = this.getPoint(i);
				NucleusBorderPoint opp = findOppositeBorder(p);

				profile[i] = p.getLengthTo(opp); 
				p.setDistanceAcrossCoM(p.getLengthTo(opp)); // LEGACY
		}
		this.distanceProfile = new Profile(profile);
	}

	public void calculateAngleProfile(int angleProfileWindowSize){

		double[] angles = new double[this.getLength()];

		for(int i=0; i<this.getLength();i++){
		// while(borderList.hasNext()){

			int indexBefore = NuclearOrganisationUtility.wrapIndex(i - angleProfileWindowSize, this.getLength());
			int indexAfter  = NuclearOrganisationUtility.wrapIndex(i + angleProfileWindowSize, this.getLength());

			NucleusBorderPoint pointBefore = this.borderList.get(indexBefore);
			NucleusBorderPoint pointAfter  = this.borderList.get(indexAfter);
			NucleusBorderPoint point       = this.borderList.get(i);

			double angle = Nucleus.findAngleBetweenXYPoints(pointBefore, point, pointAfter);

			// find the halfway point between the first and last points.
				// is this within the roi?
				// if yes, keep min angle as interior angle
				// if no, 360-min is interior
			double midX = (pointBefore.getX()+pointAfter.getX())/2;
			double midY = (pointBefore.getY()+pointAfter.getY())/2;

			if(this.getPolygon().contains( (float) midX, (float) midY)){
				angles[i] = angle;
			} else {
				angles[i] = 360-angle;
			}
		}
		this.angleProfile = new Profile(angles);
		this.setAngleProfileWindowSize(angleProfileWindowSize);
	}

	public void reverse(){
		Profile aProfile = this.getAngleProfile();
		aProfile.reverse();
		this.setAngleProfile(aProfile);

		Profile dProfile = this.getDistanceProfile();
		dProfile.reverse();
		this.setDistanceProfile(dProfile);

		List<NucleusBorderPoint> reversed = new ArrayList<NucleusBorderPoint>(0);
		for(int i=borderList.size()-1; i>=0;i--){
			reversed.add(borderList.get(i));
		}
		this.borderList = reversed;

		// replace the tag posiitons also
		Set<String> keys = borderTags.keySet();
		for( String s : keys){
			int index = borderTags.get(s);
			int newIndex = this.getLength() - index - 1; // if was 0, will now be <length-1>; if was length-1, will be 0
			addBorderTag(s, newIndex);
		}
	}

}