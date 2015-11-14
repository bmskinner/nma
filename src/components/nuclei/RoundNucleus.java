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
/*
	-----------------------
	NUCLEUS CLASS
	-----------------------
	Contains the variables for storing a nucleus,
	plus the functions for calculating aggregate stats
	within a nucleus
*/  
package components.nuclei;

import ij.IJ;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import utility.Constants;
import utility.Equation;
import utility.Stats;
import utility.Utils;
import components.CellularComponent;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.Profile;
import components.generic.SegmentedProfile;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.NucleusBorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.NucleusStatistic;
import components.nuclear.NucleusType;
import components.nuclear.SignalCollection;


/**
 * @author bms41
 *
 */
public class RoundNucleus 
	implements components.nuclei.Nucleus, CellularComponent, Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private UUID uuid;// = java.util.UUID.randomUUID();
	
	private Class<?> nucleusClass;

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
	protected SegmentedProfile angleProfile = null; // 
	protected Profile distanceProfile; // holds distances through CoM to opposite border
	protected Profile singleDistanceProfile; // holds distances from CoM, not through CoM
	protected List<NucleusBorderPoint> borderList = new ArrayList<NucleusBorderPoint>(0); // eventually to replace angleProfile
	protected List<NucleusBorderSegment> segmentList = new ArrayList<NucleusBorderSegment>(0); // expansion for e.g acrosome
	protected Map<BorderTag, Integer> borderTags  = new HashMap<BorderTag, Integer>(0); // to replace borderPointsOfInterest; <tag, index>
	protected Map<String, Integer> segmentTags = new HashMap<String, Integer>(0);

	protected XYPoint centreOfMass;

	protected File sourceFile;    // the image from which the nucleus came
	protected File nucleusFolder; // the folder to store nucleus information
	protected String outputFolder;  // the top-level path in which to store outputs; has analysis date
	
	protected double scale = 1; // allow conversion between pixels and SI units. The length of a pixel in microns
	
	protected SignalCollection signalCollection = new SignalCollection();
	
	private transient Map<BorderTag, Rectangle> boundingRectangles = new HashMap<BorderTag, Rectangle>(); // cache the bounding rectange to save time

	public RoundNucleus (Roi roi, File file, int number, double[] position) { // construct from an roi

		if(roi==null || file==null || Integer.valueOf(number)==null || position==null){
			throw new IllegalArgumentException("Nucleus constructor argument is null");
		}

		// convert the roi positions to a list of nucleus border points
		FloatPolygon polygon = roi.getInterpolatedPolygon(1,true);
		
		for(int i=0; i<polygon.npoints; i++){
			NucleusBorderPoint point = new NucleusBorderPoint( polygon.xpoints[i], polygon.ypoints[i]);
			
			if(i>0){
				point.setPrevPoint(borderList.get(i-1));
				point.prevPoint().setNextPoint(point);
			}
			borderList.add(point);
		}
		// link endpoints
		borderList.get(borderList.size()-1).setNextPoint(borderList.get(0));
		borderList.get(0).setNextPoint(borderList.get(borderList.size()-1));
		
		this.sourceFile      = file;
		this.nucleusNumber   = number;
		this.orignalPosition = position;
		this.uuid 			 = java.util.UUID.randomUUID();
	}

	public RoundNucleus(){
		// for subclasses to access
	}

	public RoundNucleus(Nucleus n) throws Exception {

		this.setID(n.getID());
		this.setPosition(n.getPosition());

		this.setSourceFile(n.getSourceFile());
		this.setOutputFolder(n.getOutputFolderName());
				
		this.setNucleusNumber(n.getNucleusNumber());
		this.setNucleusFolder(n.getNucleusFolder());
		
		this.setPerimeter(n.getPerimeter());
		this.setFeret(n.getFeret());
		this.setArea(n.getArea());

		this.setCentreOfMass(n.getCentreOfMass());
		
		this.setSignals( new SignalCollection(n.getSignalCollection()));

		this.setDistanceProfile(n.getDistanceProfile());
		this.setAngleProfile(n.getAngleProfile());

		this.setBorderTags(n.getBorderTags());
		this.setBorderList(n.getBorderList());
				
		this.setAngleProfileWindowSize(n.getAngleProfileWindowSize());
		this.setSingleDistanceProfile(n.getSingleDistanceProfile());
		
		this.setScale(n.getScale());

	}
	
	public Nucleus duplicate(){
		try {
			
			RoundNucleus duplicate = new RoundNucleus();
			
			duplicate.setID(this.getID());
			duplicate.setPosition(this.getPosition());

			duplicate.setSourceFile(this.getSourceFile());
			duplicate.setOutputFolder(this.getOutputFolderName());
					
			duplicate.setNucleusNumber(this.getNucleusNumber());
			duplicate.setNucleusFolder(this.getNucleusFolder());
			
			duplicate.setPerimeter(this.getPerimeter());
			duplicate.setFeret(this.getFeret());
			duplicate.setArea(this.getArea());

			duplicate.setCentreOfMass(this.getCentreOfMass());
			
			duplicate.setSignals( new SignalCollection(this.getSignalCollection()));

			duplicate.setDistanceProfile(this.getDistanceProfile());
			duplicate.setAngleProfile(this.getAngleProfile());

			duplicate.setBorderTags(this.getBorderTags());
			duplicate.setBorderList(this.getBorderList());
					
			duplicate.setAngleProfileWindowSize(this.getAngleProfileWindowSize());
			duplicate.setSingleDistanceProfile(this.getSingleDistanceProfile());
			
			duplicate.setScale(this.getScale());
			
			return duplicate;
//			return new RoundNucleus(this);
		} catch (Exception e) {
			return null;
		}
	}

	/*
	* Finds the key points of interest around the border
	* of the Nucleus. Can use several different methods, and 
	* take a best-fit, or just use one. The default in a round 
	* nucleus is to get the longest diameter and set this as
	*  the head/tail axis.
	*/
	public void findPointsAroundBorder() throws Exception{

		int tailIndex = this.getDistanceProfile().getIndexOfMax();
		NucleusBorderPoint tailPoint = this.getPoint(tailIndex);
		setBorderTag(BorderTag.ORIENTATION_POINT, tailIndex);
    	setBorderTag(BorderTag.REFERENCE_POINT, this.getIndex(this.findOppositeBorder(tailPoint)));
	}

	public void intitialiseNucleus(int angleProfileWindowSize) throws Exception {

		this.nucleusFolder = new File(this.getOutputFolder().getAbsolutePath()+File.separator+this.getImageNameWithoutExtension());

		if (!this.nucleusFolder.exists()) {
			this.nucleusFolder.mkdir();
		}


		// calculate angle profile
		this.calculateAngleProfile(angleProfileWindowSize);

		// calc distances around nucleus through CoM
		this.calculateDistanceProfile();
		this.calculateSingleDistanceProfile();

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
											Constants.IMAGE_PREFIX+
											this.getNucleusNumber()+
											".annotated.tiff";
		return new String(outPath);
	}

	public String getOriginalImagePath(){
		String outPath = this.nucleusFolder.getAbsolutePath()+
											File.separator+
											Constants.IMAGE_PREFIX+
											this.getNucleusNumber()+
											".original.tiff";
		return new String(outPath);
	}

	public String getEnlargedImagePath(){
		String outPath = this.nucleusFolder.getAbsolutePath()+
											File.separator+
											Constants.IMAGE_PREFIX+
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
	
	public String getNameAndNumber(){
		return this.getImageName()+"-"+this.getNucleusNumber();
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
	
	public NucleusBorderPoint getPoint(BorderTag tag){
		int index = this.getBorderIndex(tag);
		return new NucleusBorderPoint(this.borderList.get(index));
	}
	
	public String getReferencePoint(){
		return NucleusType.ROUND.getPoint(BorderTag.REFERENCE_POINT);
	}
	
	public String getOrientationPoint(){
		return NucleusType.ROUND.getPoint(BorderTag.ORIENTATION_POINT);
	}
	
	public double getStatistic(NucleusStatistic stat, MeasurementScale scale) throws Exception{
		double result = 0;
		
		switch(stat){
			
			case AREA:
				result = this.getArea();
				break;
			case ASPECT:
				result = this.getAspectRatio();
				break;
			case CIRCULARITY:
				result = this.getCircularity();
				break;
			case MAX_FERET:
				result = this.getFeret();
				break;
			case MIN_DIAMETER:
				result = this.getNarrowestDiameter();
				break;
			case PERIMETER:
				result = this.getPerimeter();
				break;
			case VARIABILITY:
				break;
			case BOUNDING_HEIGHT:
				result = this.getBoundingRectangle(BorderTag.ORIENTATION_POINT).getHeight();
				break;
			case BOUNDING_WIDTH:
				result = this.getBoundingRectangle(BorderTag.ORIENTATION_POINT).getWidth();
				break;
			default:
				break;
		
		}
		
//		if(stat.isDimensionless()){
//			return result;
//		} else {
			result = stat.convert(result, this.getScale(), scale);
			
//			if(scale.equals(MeasurementScale.MICRONS)){
//				result = Utils.micronLength(result, this.getScale());
//			}
//		}
		return result;
		
	}
	
	/**
	 * Get the cached bounding rectangle for the nucleus. If not present,
	 * the rectangle is calculated and stored
	 * @param point the border point to place at the bottom
	 * @return
	 * @throws Exception
	 */
	public Rectangle getBoundingRectangle(BorderTag point) throws Exception{
		
		if(this.boundingRectangles == null){
			boundingRectangles = new HashMap<BorderTag, Rectangle>();
			boundingRectangles.put(point, calculateBoundingRectangle(point));
		}
		if(! this.boundingRectangles.containsKey(point)){
			boundingRectangles.put(point, calculateBoundingRectangle(point));
		}
		
		return boundingRectangles.get(point);
	}
	
	/**
	 * Find the bounding rectangle of the Nucleus from the given orientation
	 * @param point
	 * @return
	 * @throws Exception
	 */
	protected Rectangle calculateBoundingRectangle(BorderTag point) throws Exception{
		ConsensusNucleus testw = new ConsensusNucleus( this, NucleusType.ROUND);
		testw.rotatePointToBottom(testw.getBorderTag(point));
		FloatPolygon pw = Utils.createPolygon(testw);
		return pw.getBounds();
	}
	
	
	public double getArea(){
		return this.area;
	}
	
	public double getCircularity(){
		double perim2 = Math.pow(this.getPerimeter(), 2);
		return (4 * Math.PI) * (this.getArea() / perim2);
	}
	
	public double getAspectRatio(){
		return this.getFeret() / this.getNarrowestDiameter();
	}

	public double getFeret(){
		return this.feret;
	}

	public double getPerimeter(){
		return this.perimeter;
	}

	public int getLength(){
		return this.borderList.size();
	}
	
	public double getScale(){
		return this.scale;
	}
	
	public void setScale(double scale){
		this.scale = scale;
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
		
		// ensure the new border list is linked properly
		for(int i=0; i<list.size(); i++){
			NucleusBorderPoint p = list.get(i);
			if(i>0){
				p.setPrevPoint(list.get(i-1));
				p.prevPoint().setNextPoint(p);
			}
		}
		list.get(list.size()-1).setNextPoint(list.get(0));
		list.get(0).setNextPoint(list.get(list.size()-1));
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



	public double getPathLength(){
		double pathLength = 0;

		XYPoint prevPoint = new XYPoint(0,0);
		 
		for (int i=0; i<this.getLength();i++ ) {
				double normalisedX = ((double)i/(double)this.getLength())*100; // normalise to 100 length

				// calculate the path length as if it were a border
				XYPoint thisPoint = new XYPoint(normalisedX,this.getAngle(i));
				pathLength += thisPoint.getLengthTo(prevPoint);
				prevPoint = thisPoint;
		}
		return pathLength;
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

		int index = this.distanceProfile.getIndexOfMin();
//		double[] distanceArray = this.distanceProfile.asArray();
//		double distance = Stats.max(distanceArray);
//		int index = 0;
//		for(int i = 0; i<this.getLength();i++){
//			if(distanceArray[i] < distance){
//				distance = distanceArray[i];
//				index = i;
//			}
//		}
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
		XYPoint end = new XYPoint(this.getBorderTag(BorderTag.ORIENTATION_POINT).getXAsInt(),this.getBorderTag(BorderTag.ORIENTATION_POINT).getYAsInt()-50);

	    double angle = findAngleBetweenXYPoints(end, this.getBorderTag(BorderTag.ORIENTATION_POINT), this.getCentreOfMass());

	    if(this.getCentreOfMass().getX() < this.getBorderTag(BorderTag.ORIENTATION_POINT).getX()){
	      return angle;
	    } else {
	      return 0-angle;
	    }
	}

	// do not move this into SignalCollection - it is overridden in RodentSpermNucleus
	public void calculateSignalAnglesFromPoint(NucleusBorderPoint p) throws Exception {

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
			Map<BorderTag, Integer> pointHash = this.getBorderTags();

			for(BorderTag s : pointHash.keySet()){
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

	public SegmentedProfile getAngleProfile() throws Exception{
		return new SegmentedProfile(this.angleProfile);
	}

	/* (non-Javadoc)
	 * @see no.nuclei.Nucleus#getAngleProfile(java.lang.String)
	 * Returns a copy
	 */
	public SegmentedProfile getAngleProfile(BorderTag tag) throws Exception{
		
		// fetch the index of the pointType (the new zero)
		int pointIndex = this.borderTags.get(tag);
		
		// offset the angle profile to start at the pointIndex
		return new SegmentedProfile(this.angleProfile.offset(pointIndex));
	}

	public void setAngleProfile(SegmentedProfile p) throws Exception{
		this.angleProfile = new SegmentedProfile(p);
	}
	
	/**
	 * 
	 * @param p
	 * @param pointType
	 * @throws Exception
	 */
	public void setAngleProfile(SegmentedProfile p, BorderTag tag) throws Exception{
		// fetch the index of the pointType (the zero of the input profile)
		int pointIndex = this.borderTags.get(tag);
		// remove the offset from the profile, by setting the profile to start from the pointIndex
		this.angleProfile = new SegmentedProfile(p).offset(-pointIndex);
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
	
	public void updatePoint(int i, XYPoint p){
		this.updatePoint(i, p.getX(), p.getY());
	}
	
	public NucleusBorderPoint getBorderTag(BorderTag tag){
		NucleusBorderPoint result = new NucleusBorderPoint(0,0);
		if(this.getBorderIndex(tag)>-1){
			result = new NucleusBorderPoint(this.borderList.get(this.getBorderIndex(tag)));
		} else {
			return null;
		}
		return result;
	}
		
	public Map<BorderTag, Integer> getBorderTags(){
		Map<BorderTag, Integer> result = new HashMap<BorderTag, Integer>();
		for(BorderTag b : borderTags.keySet()){
			result.put(b,  borderTags.get(b));
		}
		return result;
	}
	
	public void setBorderTags(Map<BorderTag, Integer> m){
		this.borderTags = m;
	}
	
	public int getBorderIndex(BorderTag tag){
		int result = -1;
		if(this.borderTags.containsKey(tag)){
			result = this.borderTags.get(tag);
		}
		return result;
	}
	
	
	public void setBorderTag(BorderTag tag, int i){
		this.borderTags.put(tag, i);
	}
		
	public boolean hasBorderTag(BorderTag tag){
		if(this.borderTags.containsKey(tag)){
			return true;
		} else {
			return false;
		}
	}

	private void calculateDistanceProfile(){

		double[] profile = new double[this.getLength()];

		for(int i = 0; i<this.getLength();i++){

				NucleusBorderPoint p   = this.getPoint(i);
				NucleusBorderPoint opp = findOppositeBorder(p);

				profile[i] = p.getLengthTo(opp); 
//				p.setDistanceAcrossCoM(p.getLengthTo(opp)); // LEGACY
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

	public void calculateAngleProfile(int angleProfileWindowSize) throws Exception{

		List<NucleusBorderSegment> segments = null;
		// store segments to reapply later
		if(this.angleProfile!=null){
			if(this.getAngleProfile().hasSegments()){
				segments = this.getAngleProfile().getSegments();
			}
		}
		
		double[] angles = new double[this.getLength()];

		for(int i=0; i<this.getLength();i++){

			int indexBefore = Utils.wrapIndex(i - angleProfileWindowSize, this.getLength());
			int indexAfter  = Utils.wrapIndex(i + angleProfileWindowSize, this.getLength());

			NucleusBorderPoint pointBefore = this.borderList.get(indexBefore);
			NucleusBorderPoint pointAfter  = this.borderList.get(indexAfter);
			NucleusBorderPoint point       = this.borderList.get(i);

			double angle = RoundNucleus.findAngleBetweenXYPoints(pointBefore, point, pointAfter);

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
		SegmentedProfile newProfile = new SegmentedProfile(angles);
		if(segments!=null){
			newProfile.setSegments(segments);
		}
		this.setAngleProfile( newProfile  );
		this.setAngleProfileWindowSize(angleProfileWindowSize);
	}

	public void reverse() throws Exception{
		SegmentedProfile aProfile = this.getAngleProfile();
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

		// replace the tag positions also
		Set<BorderTag> keys = borderTags.keySet();
		for( BorderTag s : keys){
			int index = borderTags.get(s);
			int newIndex = this.getLength() - index - 1; // if was 0, will now be <length-1>; if was length-1, will be 0
			setBorderTag(s, newIndex);
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
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	    in.defaultReadObject();
	    this.boundingRectangles = new HashMap<BorderTag, Rectangle>();
	}
}