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
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import utility.Constants;
import utility.Equation;
import utility.Stats;
import utility.Utils;
import no.analysis.ProfileSegmenter;
import no.components.NuclearSignal;
import no.components.NucleusBorderPoint;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.SignalCollection;
import no.components.XYPoint;
import no.export.TableExporter;
import no.imports.ImageImporter;


/**
 * @author bms41
 *
 */
public class RoundNucleus 
	implements no.nuclei.Nucleus, Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private UUID uuid;// = java.util.UUID.randomUUID();
	
	private Class<?> nucleusClass;
	

	// for debugging - use in calling dumpInfo()
	public static final int ALL_POINTS = 0;
	public static final int BORDER_POINTS = 1;
	public static final int BORDER_TAGS = 2;
	


	
	public static final String IMAGE_PREFIX = "export.";

	protected int nucleusNumber; // the number of the nucleus in the current image
	protected int failureCode = 0; // stores a code to explain why the nucleus failed filters

	protected int angleProfileWindowSize;

	// private double medianAngle; // the median interior angle
	protected double perimeter;   // the nuclear perimeter
	protected double pathLength;  // the angle path length - measures wibbliness in border
	protected double feret;       // the maximum diameter
	protected double area;        // the nuclear area

//	private String position; // the position of the centre of the ROI bounding rectangle in the original image as "x.y"
	
	protected double[] orignalPosition; // the xbase, ybase, width and height of the original bounding rectangle

	/*
		The following fields are part of the redesign of the whole system. Instead of storing border points within
		an AngleProfile, they will be part of the Nucleus. The Profiles can take any double[] of values, and
		manipulate them. BorderPoints can be combined into BorderSegments, which may overlap. No copies of the 
		BorderPoints are made; everything references the copy in the Nucleus. Given this, the points of interest 
		(now borderTags) need only to be indexes.
	*/
	protected Profile angleProfile; // 
	protected Profile distanceProfile; // holds distances through CoM to opposite border
	protected Profile singleDistanceProfile; // holds distances from CoM, not through CoM
	protected List<NucleusBorderPoint> borderList = new ArrayList<NucleusBorderPoint>(0); // eventually to replace angleProfile
	protected List<NucleusBorderSegment> segmentList = new ArrayList<NucleusBorderSegment>(0); // expansion for e.g acrosome
	protected Map<String, Integer> borderTags  = new HashMap<String, Integer>(0); // to replace borderPointsOfInterest; <tag, index>
	protected Map<String, Integer> segmentTags = new HashMap<String, Integer>(0);

	protected XYPoint centreOfMass;

	protected File sourceFile;    // the image from which the nucleus came
	protected File nucleusFolder; // the folder to store nucleus information
	protected String outputFolder;  // the top-level path in which to store outputs; has analysis date
	
	protected SignalCollection signalCollection = new SignalCollection();

	public RoundNucleus (Roi roi, File file, int number, double[] position) { // construct from an roi

		if(roi==null || file==null || Integer.valueOf(number)==null || position==null){
			throw new IllegalArgumentException("Nucleus constructor argument is null");
		}

		// convert the roi positions to a list of nucleus border points
		FloatPolygon polygon = roi.getInterpolatedPolygon(1,true);
		for(int i=0; i<polygon.npoints; i++){
			borderList.add(new NucleusBorderPoint( polygon.xpoints[i], polygon.ypoints[i]));
		}
		
		this.sourceFile      = file;
		this.nucleusNumber   = number;
		this.orignalPosition = position;
		this.uuid 			 = java.util.UUID.randomUUID();
	}

	public RoundNucleus(){
		// for subclasses to access
	}

	public RoundNucleus(RoundNucleus n){

		this.setID(n.getID());
		this.setPosition(n.getPosition());

		this.setSourceFile(n.getSourceFile());
		this.setOutputFolder(n.getOutputFolderName());
				
		this.setNucleusNumber(n.getNucleusNumber());
		this.setNucleusFolder(n.getNucleusFolder());
		
		this.setPerimeter(n.getPerimeter());
		this.setPathLength(n.getPathLength());
		this.setFeret(n.getFeret());
		this.setArea(n.getArea());
		this.setAngleProfile(n.getAngleProfile());
		this.setCentreOfMass(n.getCentreOfMass());
		
		this.setSignals(n.getSignalCollection());

		this.setDistanceProfile(n.getDistanceProfile());

		this.setBorderTags(n.getBorderTags());
		this.setBorderList(n.getBorderList());
		
		this.setSegmentMap(n.getSegmentMap());
		this.setSegments(n.getSegments());
		
		this.setAngleProfileWindowSize(n.getAngleProfileWindowSize());
		this.setSingleDistanceProfile(n.getSingleDistanceProfile());
	}

	/*
	* Finds the key points of interest around the border
	* of the Nucleus. Can use several different methods, and 
	* take a best-fit, or just use one. The default in a round 
	* nucleus is to get the longest diameter and set this as
	*  the head/tail axis.
	*/
	public void findPointsAroundBorder(){

		int tailIndex = this.getDistanceProfile().getIndexOfMax();
		NucleusBorderPoint tailPoint = this.getPoint(tailIndex);
		addBorderTag("tail", tailIndex);
    	addBorderTag("head", this.getIndex(this.findOppositeBorder(tailPoint)));
	}

	public void intitialiseNucleus(int angleProfileWindowSize){

		this.nucleusFolder = new File(this.getOutputFolder().getAbsolutePath()+File.separator+this.getImageNameWithoutExtension());

		if (!this.nucleusFolder.exists()) {
			try{
				this.nucleusFolder.mkdir();
			} catch(Exception e) {
				IJ.log("Failed to create directory"+this.nucleusFolder.toString()+": "+e.getMessage());
			}
		}

//		this.smoothedPolygon = roi.getInterpolatedPolygon(1,true);
//		for(int i=0; i<this.smoothedPolygon.npoints; i++){
//			borderList.add(new NucleusBorderPoint( this.smoothedPolygon.xpoints[i], this.smoothedPolygon.ypoints[i]));
//		}

		// calculate angle profile
		try{
			this.calculateAngleProfile(angleProfileWindowSize);
		} catch(Exception e){
			IJ.log("Cannot create angle profile: "+e);
		} 

		// calc distances around nucleus through CoM
		this.calculateDistanceProfile();
		this.calculateSingleDistanceProfile();
		this.calculatePathLength();

		this.calculateSignalDistancesFromCoM();
		this.calculateFractionalSignalDistancesFromCoM();
	}

	/*
		-----------------------
		Getters for basic values within nucleus
		-----------------------
	*/

	public UUID getID(){
		return this.uuid;
	}

	public String getPath(){
		return this.sourceFile.getAbsolutePath();
	}

	// defensive copy
	public double[] getPosition(){
		return this.orignalPosition;
	}

	public File getSourceFile(){
		return new File(this.sourceFile.getAbsolutePath());
	}

	public File getNucleusFolder(){
		return new File(this.nucleusFolder.getAbsolutePath());
	}

	public String getImageName(){
		return new String(this.sourceFile.getName());
	}

	public String getAnnotatedImagePath(){
		String outPath = this.nucleusFolder.getAbsolutePath()+
											File.separator+
											RoundNucleus.IMAGE_PREFIX+
											this.getNucleusNumber()+
											".annotated.tiff";
		return new String(outPath);
	}

	public String getOriginalImagePath(){
		String outPath = this.nucleusFolder.getAbsolutePath()+
											File.separator+
											RoundNucleus.IMAGE_PREFIX+
											this.getNucleusNumber()+
											".original.tiff";
		return new String(outPath);
	}

	public String getEnlargedImagePath(){
		String outPath = this.nucleusFolder.getAbsolutePath()+
											File.separator+
											RoundNucleus.IMAGE_PREFIX+
											this.getImageName()+"-"+
											this.getNucleusNumber()+
											".enlarged.tiff";
		return outPath;
	}

	public String getImageNameWithoutExtension(){
//		String extension = "";
		String trimmed = "";

		int i = this.getImageName().lastIndexOf('.');
		if (i > 0) {
//				extension = this.getImageName().substring(i+1);
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
		
//		String extension = "";
		String trimmed = "";

		int i = this.getPath().lastIndexOf('.');
		if (i > 0) {
//				extension = this.getPath().substring(i+1);
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

//	public FloatPolygon getPolygon(){
//		return this.smoothedPolygon;
//	}
	
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
	
	public List<NucleusBorderPoint> getOriginalBorderList(){
		List<NucleusBorderPoint> result = new ArrayList<NucleusBorderPoint>(0);
		for(NucleusBorderPoint p : borderList){
			result.add(new NucleusBorderPoint( p.getX() + orignalPosition[X_BASE], p.getY() + orignalPosition[Y_BASE]));
		}
		return result;
	}
	
	public int getAngleProfileWindowSize(){
		return this.angleProfileWindowSize;
	}

	public int getFailureCode(){
		return this.failureCode;
	}
		
	/* (non-Javadoc)
	 * Check if the given signal group contains signals
	 * @see no.nuclei.Nucleus#hasSignal(int)
	 */
	public boolean hasSignal(int signalGroup){
		return signalCollection.hasSignal(signalGroup);
	}
	
	public boolean hasSignal(){
		boolean result = false;
		for(int signalGroup : signalCollection.getSignalGroups()){
			if(this.hasSignal(signalGroup)){
				result = true;
			}
		}
		return result;
	}

	/*
		-----------------------
		Protected setters for subclasses
		-----------------------
	*/
	
	public void setID(UUID id){
		this.uuid = id;
	}

	public void setOutputFolder(String f){
		this.outputFolder = f;
	}

	public void setPosition(double[] p){
		this.orignalPosition = p;
	}

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
	
	protected void setSignals(SignalCollection collection){
		this.signalCollection = collection;
	}

//	public void setPolygon(FloatPolygon p){
//		this.smoothedPolygon = p;
//	}

//	protected void setRoi(Roi d){
//		this.roi = d;
//	}

	protected void setSourceFile(File d){
		this.sourceFile = d;
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

	public void setAngleProfileWindowSize(int i){
		this.angleProfileWindowSize = i;
	}

	public void setBorderList(List<NucleusBorderPoint> list){
		this.borderList = list;
	}
	
	public void setSegmentMap(Map<String, Integer> map){
		this.segmentTags = map;
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

	public int getSignalCount(){
		return this.signalCollection.numberOfSignals();
	}
	
	public int getSignalCount(int channel){
		if(signalCollection.hasSignal(channel)){
			return this.signalCollection.numberOfSignals(channel);
		} else {
			return 0;
		}
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
	
	public Set<Integer> getSignalGroups(){
		return signalCollection.getSignalGroups();
	}
	
	public List<List<NuclearSignal>> getSignals(){
		return this.signalCollection.getSignals();
	}
		

	public List<NuclearSignal> getSignals(int signalGroup){
		List<NuclearSignal> result = new ArrayList<NuclearSignal>(0);
		List<NuclearSignal> signals = this.signalCollection.getSignals(signalGroup);
		for( NuclearSignal n : signals){
			result.add(new NuclearSignal(n));
		}
		return result;
	}
	
	public SignalCollection getSignalCollection(){
		return this.signalCollection;
	}
	
	/**
	 * @param n the signal
	 * @param signalGroup the signal group to add to
	 */
	public void addSignal(NuclearSignal n, int signalGroup){
		this.signalCollection.addSignal(n, signalGroup);
	}


	 /*
		For each signal within the nucleus, calculate the distance to the nCoM
		and update the signal
	*/
	public void calculateSignalDistancesFromCoM(){
		
//		IJ.log("Getting signal distances");
//		this.signalCollection.print();
		for(List<NuclearSignal> signals : signalCollection.getSignals()){
//			IJ.log("    Found "+signals.size()+" signals in channel");
			if(!signals.isEmpty()){
				for(NuclearSignal n : signals){
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
	public void calculateFractionalSignalDistancesFromCoM(){

		this.calculateClosestBorderToSignals();

		for(List<NuclearSignal> signals : signalCollection.getSignals()){

			if(!signals.isEmpty()){

				for(NuclearSignal n : signals){

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

		for(List<NuclearSignal> signals : signalCollection.getSignals()){

			if(!signals.isEmpty()){

				for(NuclearSignal s : signals){

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

	public void updateSignalAngle(int channel, int signal, double angle){
		signalCollection.getSignals(channel).get(signal).setAngle(angle);
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
		int mid1 = Utils.wrapIndex( (int)Math.floor( (difference1/2)+minIndex ),
															this.getLength() );

		int mid2 = Utils.wrapIndex( (int)Math.floor( (difference2/2)+maxIndex ), 
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
			double angle = RoundNucleus.findAngleBetweenXYPoints(a, this.getCentreOfMass(), p); 
			if(Math.abs(90-angle)< Math.abs(90-bestAngle)){
				bestAngle = angle;
				orthgonalPoint = p;
			}
		}
		return orthgonalPoint;
	}
	
	// given a point ,find the String tag of the segment it belongs to 
	public String getSegmentOfPoint(int i){
		String segment = "";
		for(String s : this.getSegmentTags()){
			
			NucleusBorderSegment b = this.getSegmentTag(s);
			if(b.contains(i)){
				segment = s;
			}
		}
		return segment;
	}

	/*
		This will find the point in a list that is closest to any local maximum
		in the border profile, wherever that maximum may be
	*/
	public NucleusBorderPoint findPointClosestToLocalMaximum(NucleusBorderPoint[] list){

		Profile maxima = this.getAngleProfile().getLocalMaxima(5);
		NucleusBorderPoint closestPoint = new NucleusBorderPoint(0,0);
		double closestDistance = this.getPerimeter();

		for(NucleusBorderPoint p : list){
			for(int i=0; i<maxima.size();i++){
				if(maxima.get(i)==1){
					double distance = p.getLengthTo(getPoint(i));
					if(distance<closestDistance){
						closestPoint = p;
					}
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

		Profile minima = this.getAngleProfile().getLocalMinima(5);
		NucleusBorderPoint closestPoint = new NucleusBorderPoint(0,0);
		double closestDistance = this.getPerimeter();

		for(NucleusBorderPoint p : list){
			for(int i=0; i<minima.size();i++){
				if(minima.get(i)==1){
					double distance = p.getLengthTo(getPoint(i));
					if(distance<closestDistance){
						closestPoint = p;
					}
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
		double distance = Stats.max(distanceArray);
		int index = 0;
		for(int i = 0; i<this.getLength();i++){
			if(distanceArray[i] < distance){
				distance = distanceArray[i];
				index = i;
			}
		}
		return new NucleusBorderPoint(this.getPoint(index));
	}
	
	public double getNarrowestDiameter(){
		return Stats.min(this.distanceProfile.asArray());
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
//		this.updatePolygon();
	}

	public double getMedianDistanceBetweenPoints(){
		double[] distances = new double[this.borderList.size()];
		for(int i=0;i<this.borderList.size();i++){
			NucleusBorderPoint p = this.getPoint(i);
			NucleusBorderPoint next = this.getPoint( Utils.wrapIndex(i+1, this.borderList.size()));
			distances[i] = p.getLengthTo(next);
		}
		return Stats.quartile(distances, 50);
	}

	/*
		-----------------------
		Exporting data
		-----------------------
	*/
	
	public double findRotationAngle(){
		XYPoint end = new XYPoint(this.getBorderTag("tail").getXAsInt(),this.getBorderTag("tail").getYAsInt()-50);

	    double angle = findAngleBetweenXYPoints(end, this.getBorderTag("tail"), this.getCentreOfMass());

	    if(this.getCentreOfMass().getX() < this.getBorderTag("tail").getX()){
	      return angle;
	    } else {
	      return 0-angle;
	    }
	}

	// do not move this into SignalCollection - it is overridden in RodentSpermNucleus
	public void calculateSignalAnglesFromPoint(NucleusBorderPoint p){

		for( int signalGroup : signalCollection.getSignalGroups()){
			List<NuclearSignal> signals = signalCollection.getSignals(signalGroup);

			if(!signals.isEmpty()){

				for(NuclearSignal s : signals){
					double angle = findAngleBetweenXYPoints(p, this.getCentreOfMass(), s.getCentreOfMass());
					s.setAngle(angle);
				}
			}
		}
	}

	public void exportSignalDistanceMatrix(){
		signalCollection.exportDistanceMatrix(this.nucleusFolder);
	}

	/*
		Print key data to the image log file
		Overwrites any existing log
	*/   
	public void exportAngleProfile(){
		
		TableExporter logger = new TableExporter(this.getNucleusFolder());
		logger.addColumnHeading("X_INT");
		logger.addColumnHeading("Y_INT");
		logger.addColumnHeading("X_DOUBLE");
		logger.addColumnHeading("Y_DOUBLE");
		logger.addColumnHeading("INTERIOR_ANGLE");
		logger.addColumnHeading("IS_LOCAL_MIN");
		logger.addColumnHeading("IS_LOCAL_MAX");
		logger.addColumnHeading("ANGLE_DELTA");
		logger.addColumnHeading("NORMALISED_PROFILE_X");
		logger.addColumnHeading("SD_PROFILE");
		logger.addColumnHeading("IS_SD_MIN");
		logger.addColumnHeading("DISTANCE_PROFILE");
		logger.addColumnHeading("SEGMENT");
		
		Profile maxima = this.getAngleProfile().getLocalMaxima(5);
		Profile minima = this.getAngleProfile().getLocalMinima(5);
		Profile angleDeltas = this.getAngleProfile().calculateDeltas(2);
		Profile sdMinima = this.getSingleDistanceProfile().getLocalMinima(5);

		for(int i=0;i<this.getLength();i++){

			double normalisedX = ((double)i/(double)this.getLength())*100; // normalise to 100 length
			
			logger.addRow("X_INT"               , this.getBorderPoint(i).getXAsInt());
			logger.addRow("Y_INT"               , this.getPoint(i).getYAsInt());
			logger.addRow("X_DOUBLE"            , this.getPoint(i).getX());
			logger.addRow("Y_DOUBLE"            , this.getPoint(i).getY());
			logger.addRow("INTERIOR_ANGLE"      , this.getAngle(i) );
			logger.addRow("IS_LOCAL_MIN"        , minima.get(i));
			logger.addRow("IS_LOCAL_MAX"        , maxima.get(i));
			logger.addRow("ANGLE_DELTA"         , angleDeltas.get(i));
			logger.addRow("NORMALISED_PROFILE_X", normalisedX);
			logger.addRow("SD_PROFILE"          , this.singleDistanceProfile.get(i));
			logger.addRow("IS_SD_MIN"           , sdMinima.get(i));
			logger.addRow("DISTANCE_PROFILE"    , this.getDistance(i)	);
			logger.addRow("SEGMENT"             , this.getSegmentOfPoint(i));
			
		}
		logger.export(""+this.getNucleusNumber());
	}

	
	/**
	 * Export the individual segments in this nucleus
	 */
	public void exportSegments(){

		TableExporter logger = new TableExporter(this.getNucleusFolder());
		logger.addColumnHeading("SEGMENT");
		logger.addColumnHeading("START_INDEX");
		logger.addColumnHeading("END_INDEX");
		logger.addColumnHeading("PERIMETER_LENGTH");
		logger.addColumnHeading("DISTANCE_END_TO_END");

		for(NucleusBorderSegment seg :this.getSegments() ){
			logger.addRow("SEGMENT" , seg.getSegmentType());
			logger.addRow("PERIMETER_LENGTH" , seg.length(this.getLength()));
			logger.addRow("START_INDEX" , seg.getStartIndex());
			logger.addRow("END_INDEX" , seg.getEndIndex());

			double distance = this.getBorderPoint(seg.getEndIndex()).getLengthTo(this.getBorderPoint(seg.getStartIndex()));
			logger.addRow("DISTANCE_END_TO_END" , distance);
		}

		logger.export(this.getNucleusNumber()+".segments");
	}
	
	/**
	 * Export an image of the raw profile for this nucleus to
	 * the nucleus folder 
//	 */
//	public void exportProfilePlotImage(){
//		ProfileSegmenter segmenter = new ProfileSegmenter(this.getAngleProfile(), this.segmentList);
//		segmenter.draw(this.getNucleusFolder()+File.separator+RoundNucleus.IMAGE_PREFIX+this.nucleusNumber+".segments.tiff");
//	}

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

	public void setSingleDistanceProfile(Profile p){
		this.singleDistanceProfile = new Profile(p);
	}

	public Profile getSingleDistanceProfile(){
		return new Profile(this.singleDistanceProfile);
	}

	public void updatePoint(int i, double x, double y){
		this.borderList.get(i).setX(x);
		this.borderList.get(i).setY(y);
	}

	// Ensure only copies of border points get returned
	/* (non-Javadoc)
	 * @see no.nuclei.Nucleus#getBorderTag(java.lang.String)
	 */
	public NucleusBorderPoint getBorderTag(String s){
		NucleusBorderPoint result = new NucleusBorderPoint(0,0);
		if(this.getBorderIndex(s)>-1){
			result = new NucleusBorderPoint(this.borderList.get(this.getBorderIndex(s)));
		} else {
			return null;
		}
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
		if(s==null){
			throw new IllegalArgumentException("Requested tag is null");
		}
		if(!this.segmentTags.containsKey(s)){
			throw new IllegalArgumentException("Requested tag is not present: "+s);
		}
		return new NucleusBorderSegment(this.segmentList.get(this.segmentTags.get(s)));
	}

	public void addBorderTag(String name, int i){
		this.borderTags.put(name, i);
	}

	public void addSegmentTag(String name, int i){
		this.segmentTags.put(name, i);
	}
	
	public void addSegment(NucleusBorderSegment n){
		this.segmentList.add(n);
	}

	public Map<String, Integer> getSegmentMap( ){
		return this.segmentTags;
	}
	
	public Set<String> getSegmentTags(){
		return this.segmentTags.keySet();
	}
	
	public NucleusBorderSegment getSegment(int i){
		return this.segmentList.get(i);
	}
	
	public List<NucleusBorderSegment> getSegments(){
		return this.segmentList;
	}
	
	/**
	 * Create a list of segments offset to a reference point
	 * @param pointType the border tag to offset against
	 */
	public List<NucleusBorderSegment> getSegments(String pointType){
		if(pointType==null){
			throw new IllegalArgumentException("String or offset is null or empty");
		}
		
		if(!this.borderTags.containsKey(pointType)){
			throw new IllegalArgumentException("Point type does not exist in nucleus: "+pointType);
		}
		List<NucleusBorderSegment> referenceList =  getSegments();
		List<NucleusBorderSegment> result = new ArrayList<NucleusBorderSegment>(0);
		
		int offset = this.getBorderIndex(pointType); // this is our new zero
		for(NucleusBorderSegment s : referenceList){
			
			int newStart = Utils.wrapIndex( s.getStartIndex()- offset , this.getLength());
			int newEnd = Utils.wrapIndex( s.getEndIndex()- offset , this.getLength());
			
			NucleusBorderSegment c = new NucleusBorderSegment(newStart, newEnd);
			c.setSegmentType(s.getSegmentType());
			
			result.add(c);
		}
		
		return result;
	}
	
	public void setSegments(List<NucleusBorderSegment> segments){
		this.segmentList = segments;
	}
	
	public void clearSegments(){
		this.segmentList = new ArrayList<NucleusBorderSegment>(0);
		this.segmentTags = new HashMap<String, Integer>(0);
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

	private void calculateSingleDistanceProfile(){

		double[] profile = new double[this.getLength()];

		for(int i = 0; i<this.getLength();i++){

				NucleusBorderPoint p   = this.getPoint(i);
				profile[i] = p.getLengthTo(this.getCentreOfMass()); 
		}
		this.singleDistanceProfile = new Profile(profile);
	}

	public void calculateAngleProfile(int angleProfileWindowSize){

		double[] angles = new double[this.getLength()];

		for(int i=0; i<this.getLength();i++){

			int indexBefore = Utils.wrapIndex(i - angleProfileWindowSize, this.getLength());
			int indexAfter  = Utils.wrapIndex(i + angleProfileWindowSize, this.getLength());

			NucleusBorderPoint pointBefore = this.borderList.get(indexBefore);
			NucleusBorderPoint pointAfter  = this.borderList.get(indexAfter);
			NucleusBorderPoint point       = this.borderList.get(i);

			double angle = RoundNucleus.findAngleBetweenXYPoints(pointBefore, point, pointAfter);

			// IJ.log("Comparing points: "+angle);
			// IJ.log("    Before: ("+indexBefore+") "+pointBefore.getX()+"  "+pointBefore.getY());
			// IJ.log("    i     : ("+i          +") "+      point.getX()+"  "+      point.getY());
			// IJ.log("    After : ("+indexAfter +") "+ pointAfter.getX()+"  "+ pointAfter.getY());
			// IJ.log("");

			// find the halfway point between the first and last points.
				// is this within the roi?
				// if yes, keep min angle as interior angle
				// if no, 360-min is interior
			double midX = (pointBefore.getX()+pointAfter.getX())/2;
			double midY = (pointBefore.getY()+pointAfter.getY())/2;
			
			// create a polygon from the border list - we are not storing the polygon directly
			FloatPolygon polygon = Utils.createPolygon(this);
			
			if(polygon.contains( (float) midX, (float) midY)){
				angles[i] = angle;
			} else {
				angles[i] = 360-angle;
			}
		}
		this.setAngleProfile( new Profile(angles)  );
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
	
	public void updateSourceFolder(File newFolder){
		File oldFile = this.getSourceFile();
		String oldName = oldFile.getName();
		File newFile = new File(newFolder+File.separator+oldName);
		if(newFile.exists()){
			this.setSourceFile(newFile);
			this.setNucleusFolder(new File(this.getOutputFolder().getAbsolutePath()+File.separator+this.getImageNameWithoutExtension()));
		} else {
			throw new IllegalArgumentException("Cannot find file "+oldName+" in folder "+newFolder.getAbsolutePath());
		}
		
	}
}