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


import ij.gui.Roi;
import ij.process.FloatPolygon;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import stats.NucleusStatistic;
import stats.PlottableStatistic;
import stats.SignalStatistic;
import stats.Stats;
import utility.Constants;
import utility.Utils;
import components.AbstractCellularComponent;
import components.CellularComponent;
import components.generic.BorderTag;
import components.generic.Equation;
import components.generic.MeasurementScale;
import components.generic.Profile;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.NucleusType;
import components.nuclear.SignalCollection;


/**
 * @author bms41
 *
 */
public class RoundNucleus extends AbstractCellularComponent
	implements Nucleus, Serializable {

	private static final long serialVersionUID = 1L;
		
	protected int nucleusNumber; // the number of the nucleus in the current image
	protected int failureCode = 0; // stores a code to explain why the nucleus failed filters

	protected int angleProfileWindowSize;

	protected double pathLength;  // the angle profile path length - measures wibbliness in border
	
	protected Map<ProfileType, SegmentedProfile> profileMap = new HashMap<ProfileType, SegmentedProfile>();
	
	protected List<NucleusBorderSegment> segmentList = new ArrayList<NucleusBorderSegment>(0); // expansion for e.g acrosome
	protected Map<BorderTag, Integer>    borderTags  = new HashMap<BorderTag, Integer>(0); // to replace borderPointsOfInterest; <tag, index>
	protected Map<String, Integer>       segmentTags = new HashMap<String, Integer>(0);


	protected File nucleusFolder; // the folder to store nucleus information e.g. /Testing/2015-11-24_10:00:00/1/
	protected String outputFolder;  // the top-level path in which to store outputs; has analysis date e.g. /Testing/2015-11-24_10:00:00
	
	
	protected SignalCollection signalCollection = new SignalCollection();
	
	private transient Map<BorderTag, Rectangle> boundingRectangles = new HashMap<BorderTag, Rectangle>(); // cache the bounding rectange to save time

	protected transient Nucleus verticalNucleus = null; // cache the vertically rotated nucleus
	
	
	private transient StringBuffer log = new StringBuffer();
	
	public RoundNucleus (Roi roi, File file, int number, double[] position) { // construct from an roi
		super(roi);
		if(file==null || Integer.valueOf(number)==null || position==null){
			throw new IllegalArgumentException("Nucleus constructor argument is null");
		}
		
		this.setSourceFile(file);
		this.setPosition(position);
		this.nucleusNumber   = number;
	}

	public RoundNucleus(){
		super();
	}

	public RoundNucleus(Nucleus n) throws Exception {
		super(n);

		this.setOutputFolder(n.getOutputFolderName());
				
		this.setNucleusNumber(n.getNucleusNumber());
		this.setNucleusFolder(n.getNucleusFolder());
				
		this.setSignals( new SignalCollection(n.getSignalCollection()));
		
		this.setAngleProfileWindowSize(n.getAngleProfileWindowSize());
		
		for(ProfileType type : ProfileType.values()){
			if(n.hasProfile(type)){
				this.profileMap.put(type, n.getProfile(type));
			}
		}


		this.setBorderTags(n.getBorderTags());

	}
		
	public Nucleus duplicate(){
		try {

			RoundNucleus duplicate = new RoundNucleus(this);
			return duplicate;			
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

		int index = this.getProfile(ProfileType.DISTANCE).getIndexOfMax();
		
		// Make the reference point at the widest axis
		setBorderTag(BorderTag.REFERENCE_POINT, index);
				
//		BorderPoint tailPoint = this.getBorderPoint(tailIndex);
		setBorderTag(BorderTag.ORIENTATION_POINT, index);
		
		
//    	setBorderTag(BorderTag.REFERENCE_POINT, this.getBorderIndex(this.findOppositeBorder(tailPoint)));
	}
	

	public void intitialiseNucleus(int angleProfileWindowSize) throws Exception {

		this.nucleusFolder = new File(this.getOutputFolder().getAbsolutePath()+File.separator+this.getImageNameWithoutExtension());

		if (!this.nucleusFolder.exists()) {
			this.nucleusFolder.mkdir();
		}


		// calculate profiles

		this.setAngleProfileWindowSize(angleProfileWindowSize);
		
		calculateProfiles();
		

		this.calculateSignalDistancesFromCoM();
		this.calculateFractionalSignalDistancesFromCoM();
	}
	
	public void calculateProfiles() throws Exception{
		
		/*
		 * All these calculations operate on the same border point order
		 */
		
		this.profileMap.put(ProfileType.REGULAR, this.calculateAngleProfile());

		// calc distances around nucleus through CoM
		this.profileMap.put(ProfileType.DISTANCE, this.calculateDistanceProfile());

		this.profileMap.put(ProfileType.SINGLE_DISTANCE, this.calculateSingleDistanceProfile());
	}

	/*
		-----------------------
		Getters for basic values within nucleus
		-----------------------
	*/
	


	public File getNucleusFolder(){
		return new File(this.nucleusFolder.getAbsolutePath());
	}


	public String getAnnotatedImagePath(){
		String outPath = this.nucleusFolder.getAbsolutePath()+
											File.separator+
											Constants.IMAGE_PREFIX+
											this.getNucleusNumber()+
											".annotated.tiff";
		return new String(outPath);
	}

	public String getImageNameWithoutExtension(){

		String trimmed = "";

		int i = this.getSourceFileName().lastIndexOf('.');
		if (i > 0) {
				trimmed   = this.getSourceFileName().substring(0,i);
		}
		return trimmed;
	}

	public String getOutputFolderName(){
		return this.outputFolder;
	}
			
	
	public File getOutputFolder(){
		return new File(this.getSourceFolder()+File.separator+this.outputFolder);
	}


	public String getPathWithoutExtension(){
		
//		String extension = "";
		String trimmed = "";

		int i = this.getSourceFileName().lastIndexOf('.');
		if (i > 0) {
//				extension = this.getPath().substring(i+1);
				trimmed = this.getSourceFileName().substring(0,i);
		}
		return trimmed;
	}  

	public int getNucleusNumber(){
		return this.nucleusNumber;
	}
	
	public String getNameAndNumber(){
		return this.getSourceFileName()+"-"+this.getNucleusNumber();
	}

	public String getPathAndNumber(){
		return this.getSourceFile()+File.separator+this.nucleusNumber;
	}


	public BorderPoint getPoint(BorderTag tag){	
		int index = this.getBorderIndex(tag);
		return this.getBorderPoint(index);
	}
	
	@Override
	protected double calculateStatistic(PlottableStatistic stat) throws Exception{
		
		if(stat.getClass().isAssignableFrom(NucleusStatistic.class)){
			return calculateStatistic( (NucleusStatistic) stat);
		} else {
			throw new IllegalArgumentException("Statistic type inappropriate for nucleus: "+stat.getClass().getName());
		}
		
	}
	
	protected double calculateStatistic(NucleusStatistic stat) throws Exception{
		
		double result = 0;
		switch(stat){
		
		case AREA:
			result = this.getStatistic(stat);
			break;
		case ASPECT:
			result = this.getAspectRatio();
			break;
		case CIRCULARITY:
			result = this.getCircularity();
			break;
		case MAX_FERET:
			result = this.getStatistic(stat);
			break;
		case MIN_DIAMETER:
			result = this.getNarrowestDiameter();
			break;
		case PERIMETER:
			result = this.getStatistic(stat);
			break;
		case VARIABILITY:
			break;
		case BOUNDING_HEIGHT:
			result = this.getBoundingRectangle(BorderTag.ORIENTATION_POINT).getHeight();
			break;
		case BOUNDING_WIDTH:
			result = this.getBoundingRectangle(BorderTag.ORIENTATION_POINT).getWidth();
			break;
		case OP_RP_ANGLE:
			result = Utils.findAngleBetweenXYPoints(this.getBorderTag(BorderTag.REFERENCE_POINT), this.getCentreOfMass(), this.getBorderTag(BorderTag.ORIENTATION_POINT));
			break;
		default:
			break;
	
		}
		return result;
	}
	
//	
//	public double getStatistic(NucleusStatistic stat, MeasurementScale scale) throws Exception{
//		double result = 0;
//		
//		switch(stat){
//			
//			case AREA:
//				result = this.getArea();
//				break;
//			case ASPECT:
//				result = this.getAspectRatio();
//				break;
//			case CIRCULARITY:
//				result = this.getCircularity();
//				break;
//			case MAX_FERET:
//				result = this.getFeret();
//				break;
//			case MIN_DIAMETER:
//				result = this.getNarrowestDiameter();
//				break;
//			case PERIMETER:
//				result = this.getPerimeter();
//				break;
//			case VARIABILITY:
//				break;
//			case BOUNDING_HEIGHT:
//				result = this.getBoundingRectangle(BorderTag.ORIENTATION_POINT).getHeight();
//				break;
//			case BOUNDING_WIDTH:
//				result = this.getBoundingRectangle(BorderTag.ORIENTATION_POINT).getWidth();
//				break;
//			case OP_RP_ANGLE:
//				result = RoundNucleus.findAngleBetweenXYPoints(this.getBorderTag(BorderTag.REFERENCE_POINT), this.getCentreOfMass(), this.getBorderTag(BorderTag.ORIENTATION_POINT));
//				break;
//			default:
//				result = 0;
//				break;
//		
//		}
//		
//		result = stat.convert(result, this.getScale(), scale);
//
//		return result;
//		
//	}
		
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
	 * Find the bounding rectangle of the Nucleus. If the TopVertical and
	 * BottomVertical points have been set, these will be used. Otherwise,
	 * the given point is moved to directly below the CoM
	 * @param point the point to put at the bottom. Overridden if TOP_  and BOTTOM_ are set
	 * @return
	 * @throws Exception
	 */
	protected Rectangle calculateBoundingRectangle(BorderTag point) throws Exception{
		ConsensusNucleus testw = new ConsensusNucleus( this, NucleusType.ROUND);

		if(this.hasBorderTag(BorderTag.TOP_VERTICAL) && this.hasBorderTag(BorderTag.BOTTOM_VERTICAL)){
			
			BorderPoint[] points = getBorderPointsForVerticalAlignment();

			testw.alignPointsOnVertical(points[0], points[1] );
			
		} else {
			testw.rotatePointToBottom(testw.getBorderTag(point));
		}

		
		FloatPolygon pw = testw.createPolygon();
		return pw.getBounds();
	}
	

	public BorderPoint[] getBorderPointsForVerticalAlignment(){
		BorderPoint topPoint =  this.getBorderTag(BorderTag.TOP_VERTICAL);
		BorderPoint bottomPoint =this.getBorderTag(BorderTag.BOTTOM_VERTICAL);
		
		// Find the best line across the region
//		Profile region = this.getAngleProfile().getSubregion(this.getBorderIndex(BorderTag.TOP_VERTICAL), this.getBorderIndex(BorderTag.BOTTOM_VERTICAL));
		
		List<BorderPoint> pointsInRegion = new ArrayList<BorderPoint>();
		int startIndex = Math.min(this.getBorderIndex(BorderTag.TOP_VERTICAL),  this.getBorderIndex(BorderTag.BOTTOM_VERTICAL));
		int endIndex = Math.max(this.getBorderIndex(BorderTag.TOP_VERTICAL),  this.getBorderIndex(BorderTag.BOTTOM_VERTICAL));
		for(int index = startIndex; index < endIndex; index++ ){
			pointsInRegion.add(this.getBorderPoint(index));
		}
//		IJ.log(this.getNameAndNumber()+": "+ pointsInRegion.size());
		
		// Make an equation covering the region
		Equation eq = new Equation(topPoint, bottomPoint);
//		IJ.log("Equation: "+eq.toString());
		// Find the orthogonal
		Equation bottomOrth = eq.getPerpendicular(bottomPoint);
		Equation topOrth = eq.getPerpendicular(topPoint);
//		IJ.log("Bottom: "+bottomOrth.toString());
//		IJ.log("Top:    "+topOrth.toString());
		
		double minScore = Double.MAX_VALUE;
		// With variation about the orthogonal, test sum-of-squares
		// Make new points to align at based on best fit to the region
		
		int variation = 10;
		double movementSize = 1;
		double bestI = 0;
		double bestJ = 0;
		
		for(int i=-variation; i<variation;i+=movementSize){
			// change the posiiton of the top point
			XYPoint iPoint = topOrth.getPointOnLine(topPoint, i);
			
			for(int j=-variation; j<variation;j+=movementSize){
				// change the posiiton of the bottom point
				XYPoint jPoint = bottomOrth.getPointOnLine(bottomPoint, j);
				
				// make a new equation for the line
				Equation newLine = new Equation(iPoint, jPoint);
				
				// Check the fit of the line aginst the profile region
				// Build a line from the border points in the region
				// Test each point's distance to the equation line
				double score = 0;
				for(XYPoint p : pointsInRegion){
					score += Math.pow(newLine.getClosestDistanceToPoint(p), 2);
				}
//				IJ.log("i="+i+" : j="+j+" : Score: "+score);
				if(score<minScore){
					minScore = score;
					topPoint = new BorderPoint(iPoint);
					bottomPoint = new BorderPoint(jPoint);
					bestI = i;
					bestJ = j;
				}
			}

		}
		
		movementSize = 0.1;
		for(double i=bestI-0.9; i<bestI+1;i+=movementSize){
			// change the posiiton of the top point
			XYPoint iPoint = topOrth.getPointOnLine(topPoint, i);
			
			for(double j=bestJ-0.9; i<bestJ+1;i+=movementSize){
				// change the posiiton of the bottom point
				XYPoint jPoint = bottomOrth.getPointOnLine(bottomPoint, j);
				
				// make a new equation for the line
				Equation newLine = new Equation(iPoint, jPoint);
				
				// Check the fit of the line aginst the profile region
				// Build a line from the border points in the region
				// Test each point's distance to the equation line
				double score = 0;
				for(XYPoint p : pointsInRegion){
					score += Math.pow(newLine.getClosestDistanceToPoint(p), 2);
				}
//				IJ.log("i="+i+" : j="+j+" : Score: "+score);
				if(score<minScore){
					minScore = score;
					topPoint = new BorderPoint(iPoint);
					bottomPoint = new BorderPoint(jPoint);
				}
			}

		}
		
		return new BorderPoint[] {topPoint, bottomPoint};
	}
	
		
	public double getCircularity() throws Exception{
		double perim2 = Math.pow(this.getStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS), 2);
		return (4 * Math.PI) * (this.getStatistic(NucleusStatistic.AREA, MeasurementScale.PIXELS) / perim2);
	}
	
	public double getAspectRatio() {
		try {
			return this.getBoundingRectangle(BorderTag.ORIENTATION_POINT).getHeight() / this.getBoundingRectangle(BorderTag.ORIENTATION_POINT).getWidth();
		} catch(Exception e){
			return 0;
		}
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
	


	public void setOutputFolder(String f){
		this.outputFolder = f;
	}
	
	protected void setSignals(SignalCollection collection){
		this.signalCollection = collection;
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

	public void setAngleProfileWindowSize(int i) throws Exception{
		this.angleProfileWindowSize = i;
		this.profileMap.put(ProfileType.REGULAR, this.calculateAngleProfile());
	}

		
	public void setSegmentMap(Map<String, Integer> map){
		this.segmentTags = map;
	}
		

	/*
		-----------------------
		Get aggregate values
		-----------------------
	*/

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



	public double getPathLength() throws Exception{
		double pathLength = 0;

		XYPoint prevPoint = new XYPoint(0,0);
		 
		for (int i=0; i<this.getBorderLength();i++ ) {
				double normalisedX = ((double)i/(double)this.getBorderLength())*100; // normalise to 100 length

				// calculate the path length as if it were a border
				
				Profile angleProfile = this.getProfile(ProfileType.REGULAR);
				
				XYPoint thisPoint = new XYPoint(normalisedX, angleProfile.get(i));
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
					n.setStatistic(SignalStatistic.DISTANCE_FROM_COM, distance); //.setDistanceFromCoM(distance);
				}
			}
		}
	}

	/*
		Calculate the distance from the nuclear centre of
		mass as a fraction of the distance from the nuclear CoM, through the 
		signal CoM, to the nuclear border
	*/
	public void calculateFractionalSignalDistancesFromCoM() throws Exception{

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

					for(int j = 0; j<getBorderLength();j++){
						double x = this.getBorderPoint(j).getX();
						double y = this.getBorderPoint(j).getY();
						double yOnLine = eq.getY(x);
						double distanceToSignal = this.getBorderPoint(j).getLengthTo(n.getCentreOfMass()); // fetch

						double deltaY = Math.abs(y - yOnLine);
						// find the point closest to the line; this could find either intersection
						// hence check it is as close as possible to the signal CoM also
						if(deltaY < minDeltaY && distanceToSignal < minDistanceToSignal){
							minDeltaY = deltaY;
							minDeltaYIndex = j;
							minDistanceToSignal = distanceToSignal;
						}
					}
					BorderPoint borderPoint = this.getBorderPoint(minDeltaYIndex);
					double nucleusCoMToBorder = borderPoint.getLengthTo(this.getCentreOfMass());
					double signalCoMToNucleusCoM = this.getCentreOfMass().getLengthTo(n.getCentreOfMass());
					double fractionalDistance = signalCoMToNucleusCoM / nucleusCoMToBorder;
					n.setStatistic(SignalStatistic.FRACT_DISTANCE_FROM_COM, fractionalDistance);
				}
			}
		}
	}

	/*
		Go through the signals in the nucleus, and find the point on
		the nuclear ROI that is closest to the signal centre of mass.
	 */
	private void calculateClosestBorderToSignals() throws Exception{

		for(List<NuclearSignal> signals : signalCollection.getSignals()){

			if(!signals.isEmpty()){

				for(NuclearSignal s : signals){

					int minIndex = 0;
					double minDistance = this.getStatistic(NucleusStatistic.MAX_FERET, MeasurementScale.PIXELS);

					for(int j = 0; j<getBorderLength();j++){
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
		signalCollection.getSignals(channel).get(signal).setStatistic(SignalStatistic.ANGLE, angle);
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
	public int getPositionBetween(BorderPoint pointA, BorderPoint pointB){

		int a = 0;
		int b = 0;
		// find the indices that correspond on the array
		for(int i = 0; i<this.getBorderLength(); i++){
				if(this.getBorderPoint(i).overlaps(pointA)){
					a = i;
				}
				if(this.getBorderPoint(i).overlaps(pointB)){
					b = i;
				}
		}

		// find the higher and lower index of a and b
		int maxIndex = a > b ? a : b;
		int minIndex = a > b ? b : a;

		// there are two midpoints between any points on a ring; we want to take the 
		// midpoint that is in the smaller segment.

		int difference1 = maxIndex - minIndex;
		int difference2 = this.getBorderLength() - difference1;

		// get the midpoint
		int mid1 = AbstractCellularComponent.wrapIndex( (int)Math.floor( (difference1/2)+minIndex ),
															this.getBorderLength() );

		int mid2 = AbstractCellularComponent.wrapIndex( (int)Math.floor( (difference2/2)+maxIndex ), 
															this.getBorderLength() );

		return difference1 < difference2 ? mid1 : mid2;
	}

	// For a position in the roi, draw a line through the CoM and get the intersection point
	public BorderPoint findOppositeBorder(BorderPoint p){

		int minDeltaYIndex = 0;
		double minAngle = 180;

		for(int i = 0; i<this.getBorderLength();i++){
			double angle = Utils.findAngleBetweenXYPoints(p, this.getCentreOfMass(), this.getBorderPoint(i));
			if(Math.abs(180 - angle) < minAngle){
				minDeltaYIndex = i;
				minAngle = 180 - angle;
			}
		}
		return this.getBorderPoint(minDeltaYIndex);
	}

	/*
		From the point given, create a line to the CoM. Measure angles from all 
		other points. Pick the point closest to 90 degrees. Can then get opposite
		point. Defaults to input point if unable to find point.
	*/
	public BorderPoint findOrthogonalBorderPoint(BorderPoint a){

		BorderPoint orthgonalPoint = a;
		double bestAngle = 0;

		for(int i=0;i<this.getBorderLength();i++){

			BorderPoint p = this.getBorderPoint(i);
			double angle = Utils.findAngleBetweenXYPoints(a, this.getCentreOfMass(), p); 
			if(Math.abs(90-angle)< Math.abs(90-bestAngle)){
				bestAngle = angle;
				orthgonalPoint = p;
			}
		}
		return orthgonalPoint;
	}
	

	// find the point with the narrowest diameter through the CoM
	// Uses the distance profile
	public BorderPoint getNarrowestDiameterPoint() throws Exception{

		int index = this.getProfile(ProfileType.DISTANCE).getIndexOfMin();

		return new BorderPoint(this.getBorderPoint(index));
	}
	
	public double getNarrowestDiameter() throws Exception{
		return Stats.min(this.getProfile(ProfileType.DISTANCE).asArray());
	}


	/*
		-----------------------
		Exporting data
		-----------------------
	*/
	
	public double findRotationAngle(){
		
		double angle;
		if(this.hasBorderTag(BorderTag.TOP_VERTICAL) && this.hasBorderTag(BorderTag.BOTTOM_VERTICAL)){
//			IJ.log("Calculating rotation angle via TopVertical");
			XYPoint end = new XYPoint(this.getBorderTag(BorderTag.BOTTOM_VERTICAL).getXAsInt(),this.getBorderTag(BorderTag.BOTTOM_VERTICAL).getYAsInt()-50);
			angle = Utils.findAngleBetweenXYPoints(end, this.getBorderTag(BorderTag.BOTTOM_VERTICAL), this.getBorderTag(BorderTag.TOP_VERTICAL));

			
		} else {
//			IJ.log("Calculating rotation angle via OrientationPoint");
			// Make a point directly below the orientation point
			XYPoint end = new XYPoint(this.getBorderTag(BorderTag.ORIENTATION_POINT).getXAsInt(),this.getBorderTag(BorderTag.ORIENTATION_POINT).getYAsInt()-50);

		    angle = Utils.findAngleBetweenXYPoints(end, this.getBorderTag(BorderTag.ORIENTATION_POINT), this.getCentreOfMass());

		}
		
	    if(this.getCentreOfMass().getX() < this.getBorderTag(BorderTag.ORIENTATION_POINT).getX()){
	      return angle;
	    } else {
	      return 0-angle;
	    }
	}

	// do not move this into SignalCollection - it is overridden in RodentSpermNucleus
	public void calculateSignalAnglesFromPoint(BorderPoint p) throws Exception {

		for( int signalGroup : signalCollection.getSignalGroups()){
			
			if(signalCollection.hasSignal(signalGroup)){
				
			
				List<NuclearSignal> signals = signalCollection.getSignals(signalGroup);

				for(NuclearSignal s : signals){

					double angle = Utils.findAngleBetweenXYPoints(p, this.getCentreOfMass(), s.getCentreOfMass());
					s.setStatistic(SignalStatistic.ANGLE, angle);

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
	public String dumpInfo(int type){
		String result = "";
		result += "Dumping nucleus info: "+this.getNameAndNumber()+"\n";
		result += "    Border length: "+this.getBorderLength()+"\n";
		result += "    CoM: "+this.getCentreOfMass().toString()+"\n";
		if(type==ALL_POINTS || type==BORDER_POINTS){
			result += "    Border:\n";
			for(int i=0; i<this.getBorderLength(); i++){
				BorderPoint p = this.getBorderPoint(i);
				result += "      Index "+i+": "+p.getX()+"\t"+p.getY()+"\t"+this.getBorderTag(i)+"\n";
			}
		}
		if(type==ALL_POINTS || type==BORDER_TAGS){
			result += "    Points of interest:\n";
			Map<BorderTag, Integer> pointHash = this.getBorderTags();

			for(BorderTag s : pointHash.keySet()){
			 BorderPoint p = getBorderPoint(pointHash.get(s));
			 result += "    "+s+": "+p.getX()+"    "+p.getY()+" at index "+pointHash.get(s)+"\n";
			}
		}
		return result;
	}

	/*
		-----------------------
		Methods for the new architecture only
		-----------------------
	*/

	public SegmentedProfile getProfile(ProfileType type) throws Exception {
		if(this.hasProfile(type)){
			return new SegmentedProfile(this.profileMap.get(type));
		} else {
			throw new IllegalArgumentException("Profile type "+type+" is not found in this nucleus");
		}
	}
	
	public boolean hasProfile(ProfileType type){
		if(this.profileMap.containsKey(type)){
			return true;
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see no.nuclei.Nucleus#getAngleProfile(java.lang.String)
	 * Returns a copy
	 */
	public SegmentedProfile getProfile(ProfileType type, BorderTag tag) throws Exception{
		
		// fetch the index of the pointType (the new zero)
		int pointIndex = this.borderTags.get(tag);
		
		SegmentedProfile profile = null;
		if(this.hasProfile(type)){
			
			// offset the angle profile to start at the pointIndex
			profile =  new SegmentedProfile(this.getProfile(type).offset(pointIndex));
			
		}

//		IJ.log("Nucleus "+this.getNameAndNumber()+" : "+type +" : "+tag+" : "+profile.get(0));
		return profile;
	}
	
	/**
	 * Update the profile of the given type. Since only franken profiles are 
	 * not calculated internally, the other profiles just replace the segment list.
	 * @param type
	 * @param profile
	 * @throws Exception
	 */
	public void setProfile(ProfileType type, SegmentedProfile profile) throws Exception{
		if(profile==null){
			throw new IllegalArgumentException("Error setting nucleus profile: type "+type+" is null");
		}
		if(type.equals(ProfileType.FRANKEN)){
			this.profileMap.put(type, profile);
		} else {
			this.profileMap.get(type).setSegments(profile.getSegments());
		}
//		
	}
	
	
	/**
	 * 
	 * @param p
	 * @param pointType
	 * @throws Exception
	 */
	public void setProfile(ProfileType type, BorderTag tag, SegmentedProfile p) throws Exception{
		
		// fetch the index of the pointType (the zero of the input profile)
		int pointIndex = this.borderTags.get(tag);
		
		// remove the offset from the profile, by setting the profile to start from the pointIndex
		this.setProfile(type, new SegmentedProfile(p).offset(-pointIndex));
	}

	
	public BorderPoint getBorderTag(BorderTag tag){
		BorderPoint result = new BorderPoint(0,0);
		if(this.getBorderIndex(tag)>-1){
			result = this.getBorderPoint((this.getBorderIndex(tag)));
//			result = new BorderPoint(this.getBorderPoint((this.getBorderIndex(tag))));
		} else {
			return null;
		}
		return result;
	}
	
	public BorderPoint getBorderPoint(BorderTag tag){
		return getBorderTag(tag) ;
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
		
		// The intersection point should always be opposite the orientation point
		if(tag.equals(BorderTag.ORIENTATION_POINT)){
			int intersectionIndex = this.getBorderIndex(this.findOppositeBorder( this.getBorderPoint(i) ));
			this.setBorderTag(BorderTag.INTERSECTION_POINT, intersectionIndex);
		}
	}
	
	public void setBorderTag(BorderTag reference, BorderTag tag, int i){
		int newIndex = getOffsetBorderIndex(reference, i);
		this.setBorderTag(tag, newIndex);
	}
	
	
		
	public boolean hasBorderTag(BorderTag tag){
		if(this.borderTags.containsKey(tag)){
			return true;
		} else {
			return false;
		}
	}
	
	public boolean hasBorderTag( int index){
				
		if(this.borderTags.containsValue(index)){
			return true;
		} else {
			return false;
		}
	}
	
	public boolean hasBorderTag(BorderTag tag, int index){
				
		// remove offset
		int newIndex = getOffsetBorderIndex(tag, index);
		return this.hasBorderTag(newIndex);
	}
	
	public int getOffsetBorderIndex(BorderTag reference, int index){
		if(this.getBorderIndex(reference)>-1){
			int newIndex =  AbstractCellularComponent.wrapIndex( index+this.getBorderIndex(reference) , this.getBorderLength() );
			return newIndex;
		}
		return -1;
	}
	
	public BorderTag getBorderTag(BorderTag tag, int index){
		int newIndex = getOffsetBorderIndex(tag, index);
		return this.getBorderTag(newIndex);
	}
	
	public BorderTag getBorderTag(int index){

		for(BorderTag b : this.borderTags.keySet()){
			if(this.borderTags.get(b)==index){
				return b;
			}
		}
		return null;
	}
	
	

	public void setSegmentStartLock(boolean lock, UUID segID){
		if(segID==null){
			throw new IllegalArgumentException("Requested seg id is null");
		}
		for(SegmentedProfile p : this.profileMap.values()){
			
			if(p.hasSegment(segID)){
				p.getSegment(segID).setStartPositionLocked(lock);
			}
		}
	}

	private SegmentedProfile calculateDistanceProfile() throws Exception {

		double[] profile = new double[this.getBorderLength()];
			
		int index = 0;
		Iterator<BorderPoint> it = this.getBorderList().iterator();
		while(it.hasNext()){

			BorderPoint point = it.next();
			BorderPoint opp = findOppositeBorder(point);

			profile[index++] = point.getLengthTo(opp); 
			
		}

		return new SegmentedProfile(profile);
	}

	private SegmentedProfile calculateSingleDistanceProfile() throws Exception{

		double[] profile = new double[this.getBorderLength()];
		
		int index = 0;
		Iterator<BorderPoint> it = this.getBorderList().iterator();
		while(it.hasNext()){

			BorderPoint point = it.next();
			profile[index++] = point.getLengthTo(this.getCentreOfMass()); 
			
		}

		return new SegmentedProfile(profile);
	}

	protected SegmentedProfile calculateAngleProfile() throws Exception{

		List<NucleusBorderSegment> segments = null;
		
		// store segments to reapply later
		if(this.hasProfile(ProfileType.REGULAR)){
			if(this.getProfile(ProfileType.REGULAR).hasSegments()){
				segments = this.getProfile(ProfileType.REGULAR).getSegments();
			}
		}
		
		double[] angles = new double[this.getBorderLength()];

//		for(int i=0; i<this.getBorderLength();i++){
		
		int index = 0;
		Iterator<BorderPoint> it = this.getBorderList().iterator();
		while(it.hasNext()){

			BorderPoint point = it.next();
			BorderPoint pointBefore = point.prevPoint(angleProfileWindowSize);
			BorderPoint pointAfter  = point.nextPoint(angleProfileWindowSize);

			double angle = Utils.findAngleBetweenXYPoints(pointBefore, point, pointAfter);

			// find the halfway point between the first and last points.
				// is this within the roi?
				// if yes, keep min angle as interior angle
				// if no, 360-min is interior
			double midX = (pointBefore.getX()+pointAfter.getX())/2;
			double midY = (pointBefore.getY()+pointAfter.getY())/2;
			
			// create a polygon from the border list - we are not storing the polygon directly
//			FloatPolygon polygon = this.createPolygon();
			if(this.createPolygon().contains((float) midX, (float) midY)){
			
//			if(polygon.contains( (float) midX, (float) midY)){
				angles[index] = angle;
			} else {
				angles[index] = 360-angle;
			}
			index++;
		}
		SegmentedProfile newProfile = new SegmentedProfile(angles);
		if(segments!=null){
			newProfile.setSegments(segments);
		}
		return newProfile;
	}


	public void reverse() throws Exception{
		
		for(ProfileType type : profileMap.keySet()){

			SegmentedProfile profile = profileMap.get(type);
			profile.reverse();
			profileMap.put(type, profile);
		}
		
		List<BorderPoint> reversed = new ArrayList<BorderPoint>(0);
		for(int i=this.getBorderLength()-1; i>=0;i--){
			reversed.add(this.getBorderPoint(i));
		}
		this.setBorderList(reversed);

		// replace the tag positions also
		Set<BorderTag> keys = borderTags.keySet();
		for( BorderTag s : keys){
			int index = borderTags.get(s);
			int newIndex = this.getBorderLength() - index - 1; // if was 0, will now be <length-1>; if was length-1, will be 0
			setBorderTag(s, newIndex);
		}
	}
	
	public void flipAngleProfile()throws Exception{
		
		SegmentedProfile profile = profileMap.get(ProfileType.REGULAR);
		profile.reverse();
		profileMap.put(ProfileType.REGULAR, profile);
	}
	
	public void updateSourceFolder(File newFolder) throws Exception {
		File oldFile = this.getSourceFile();
		String oldName = oldFile.getName();
		File newFile = new File(newFolder+File.separator+oldName);
		if(newFile.exists()){
			this.setSourceFile(newFile);
			this.setNucleusFolder(new File(this.getOutputFolder().getAbsolutePath()+File.separator+this.getImageNameWithoutExtension()));
		} else {
			throw new Exception("Cannot find file "+oldName+" in folder "+newFolder.getAbsolutePath());
		}
		
	}
	


	
	

	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	    in.defaultReadObject();
	    this.boundingRectangles = new HashMap<BorderTag, Rectangle>();	  
	    log = new StringBuffer();
	}

	@Override
	public boolean equals(CellularComponent c) {
		if(c.getClass()==this.getClass()){
			if(this.getID().equals(c.getID())){
				return true;
			} else {
				return false;
			}
		} else {
			
			return false;
			
		}
	}
	
	public void updateVerticallyRotatedNucleus(){
		this.verticalNucleus = null;
		this.getVerticallyRotatedNucleus();
	}
	
	public Nucleus getVerticallyRotatedNucleus(){
		if(this.verticalNucleus==null){
			verticalNucleus = this.duplicate();
			
			if(this.hasBorderTag(BorderTag.TOP_VERTICAL) && this.hasBorderTag(BorderTag.BOTTOM_VERTICAL)){
				// Rotate vertical
				BorderPoint[] points = verticalNucleus.getBorderPointsForVerticalAlignment();
				verticalNucleus.alignPointsOnVertical(points[0], points[1] );

			} else {
				
				verticalNucleus.rotatePointToBottom(verticalNucleus.getBorderPoint(BorderTag.ORIENTATION_POINT));

			}
			// Ensure all nuclei have overlapping centres of mass
			verticalNucleus.moveCentreOfMass(new XYPoint(0,0));
		}
		return verticalNucleus;
	}
	
	/**
	 * Test the effect on a given point's position of rotating the nucleus 
	 * @param point the point of interest
	 * @param angle the angle of rotation
	 * @return the new position
	 */
	private XYPoint getPositionAfterRotation(BorderPoint point, double angle){
		
		// get the angle from the tail to the vertical axis
		double tailAngle = Utils.findAngleBetweenXYPoints( point, 
				this.getCentreOfMass(), 
				new XYPoint(this.getCentreOfMass().getX(),-10));
		if(point.getX()<this.getCentreOfMass().getX()){
			tailAngle = 360-tailAngle; // correct for measuring the smallest angle
		}
		// get a copy of the new bottom point
		XYPoint p = new XYPoint( point.getX(), point.getY() );

		// get the distance from the bottom point to the CoM
		double distance = p.getLengthTo(this.getCentreOfMass());

		// add the suggested rotation amount
		double newAngle = tailAngle + angle;

		// get the new X and Y coordinates of the point after rotation
		double newX = Utils.getXComponentOfAngle(distance, newAngle) + this.getCentreOfMass().getX();
		double newY = Utils.getYComponentOfAngle(distance, newAngle) + this.getCentreOfMass().getY();
		return new XYPoint(newX, newY);
	}
	
	/**
	 * Rotate the nucleus so that the given point is directly 
	 * below the centre of mass
	 * @param bottomPoint
	 */
	public void rotatePointToBottom(BorderPoint bottomPoint){

		// find the angle to rotate
		double angleToRotate 	= 0;

		// start with a high distance from the central vertical line
		double distanceFromZero = 180;

		// Go around in a circle
		for(int angle=0;angle<360;angle++){

			XYPoint newPoint = getPositionAfterRotation(bottomPoint, angle);

			// get the absolute distance from the vertical
			double distanceFromCoM = Math.abs(newPoint.getX()-this.getCentreOfMass().getX());
			// if the new x position is closer to the central vertical line
			// AND the y position is below zero
			// this is a better rotation
			if( distanceFromCoM < distanceFromZero && newPoint.getY() < this.getCentreOfMass().getY()){
				angleToRotate = angle;
				distanceFromZero = distanceFromCoM;
			}
		}
		
		// Now we have the int angle.
		// Test 0.05 degree increments for the degree each side
				
		for(double angle=angleToRotate-0.9;angle<angleToRotate+0.9;angle+=0.05){

			XYPoint newPoint = getPositionAfterRotation(bottomPoint, angle);

			// get the absolute distance from the vertical
			double distanceFromCoM = Math.abs(newPoint.getX()-this.getCentreOfMass().getX());
			// if the new x position is closer to the central vertical line
			// AND the y position is below zero
			// this is a better rotation
			if( distanceFromCoM < distanceFromZero && newPoint.getY() < this.getCentreOfMass().getY()){
				angleToRotate = angle;
				distanceFromZero = distanceFromCoM;
						}
		}
		this.rotate(angleToRotate);
	}
	
	/**
	 * Given two points in the nucleus, rotate the nucleus so that they are vertical.
	 * @param topPoint the point to have the higher Y value
	 * @param bottomPoint the point to have the lower Y value
	 */
	public void alignPointsOnVertical(BorderPoint topPoint, BorderPoint bottomPoint){
		
		/*
		 * If the points are already aligned vertically, the rotation should not have any effect
		 */
		double angleToRotate 	= 0;
				
		/*
		 *  Get the angle from the vertical of the line between the points.
		 *  
		 *  This is the line running from top (T) to bottom (B), then up
		 *  to the y position of the top at the X position of the bottom (V)
		 * 
		 *     T V
		 *      \|
		 *       B
		 * 
		 */
		double angleToBeat = Utils.findAngleBetweenXYPoints( topPoint, 
				bottomPoint, 
				new XYPoint(bottomPoint.getX(),topPoint.getY()));
		
		for(int angle=0;angle<360;angle++){
			
			XYPoint newTop 		= getPositionAfterRotation(topPoint, angle);
			XYPoint newBottom 	= getPositionAfterRotation(bottomPoint, angle);
			
			// Test that the top point is still on the top; no point getting
			// angles for the rotations where the top has moved to the bottom 
			if(newTop.getY() > newBottom.getY()){
				
				double newAngle = Utils.findAngleBetweenXYPoints( newTop, 
						newBottom, 
						new XYPoint(newBottom.getX(),newTop.getY()));
				
				/*
				 * We want to minimise the angle between the points, whereupon
				 * they are vertically aligned. 
				 */

				if( newAngle < angleToBeat ){
					angleToBeat = newAngle;
					angleToRotate = angle;
				}
			}

		}
		
		
		// Now we have the int angle.
		// Test 0.05 degree increments for the degree each side
		
		for(double angle=angleToRotate-0.9;angle<angleToRotate+0.9;angle+=0.05){
			
			XYPoint newTop 		= getPositionAfterRotation(topPoint, angle);
			XYPoint newBottom 	= getPositionAfterRotation(bottomPoint, angle);
	
			double newAngle = Utils.findAngleBetweenXYPoints( newTop, 
					newBottom, 
					new XYPoint(newBottom.getX(),newTop.getY()));

			/*
			 * We want to minimise the angle between the points, whereupon
			 * they are vertically aligned. 
			 */

			if( newAngle < angleToBeat ){
				angleToBeat = newAngle;
				angleToRotate = angle;
			}
		}
		
		
		this.rotate(angleToRotate);
	}
		
	/**
	 * Rotate the nucleus by the given amount around the centre of mass
	 * @param angle
	 */
	public void rotate(double angle){
		
		if(angle!=0){

			for(int i=0; i<this.getBorderLength(); i++){
				XYPoint p = this.getBorderPoint(i);


				// get the distance from this point to the centre of mass
				double distance = p.getLengthTo(this.getCentreOfMass());

				// get the angle between the centre of mass (C), the point (P) and a
				// point directly under the centre of mass (V)

				/*
				 *      C
				 *      |\  
				 *      V P
				 * 
				 */
				double oldAngle = Utils.findAngleBetweenXYPoints( p, 
						this.getCentreOfMass(), 
						new XYPoint(this.getCentreOfMass().getX(),-10));


				if(p.getX()<this.getCentreOfMass().getX()){
					oldAngle = 360-oldAngle;
				}

				double newAngle = oldAngle + angle;
				double newX = Utils.getXComponentOfAngle(distance, newAngle) + this.getCentreOfMass().getX();
				double newY = Utils.getYComponentOfAngle(distance, newAngle) + this.getCentreOfMass().getY();

				this.updateBorderPoint(i, newX, newY);
			}
			
			// Also update signal locations
			// TODO: rotates, but does not get location correct. Is there an offset?
//			for (int signalGroup : this.getSignalGroups()){
//				for(NuclearSignal s : this.getSignals(signalGroup)){
//					for(int i = 0; i<s.getBorderSize(); i++){
//						XYPoint p = s.getBorderPoint(i);
//
//
//						// get the distance from this point to the centre of mass
//						double distance = p.getLengthTo(this.getCentreOfMass());
//						
//						double oldAngle = RoundNucleus.findAngleBetweenXYPoints( p, 
//								this.getCentreOfMass(), 
//								new XYPoint(this.getCentreOfMass().getX(),-10));
//
//
//						if(p.getX()<this.getCentreOfMass().getX()){
//							oldAngle = 360-oldAngle;
//						}
//
//						double newAngle = oldAngle + angle;
//						double newX = Utils.getXComponentOfAngle(distance, newAngle) + this.getCentreOfMass().getX();
//						double newY = Utils.getYComponentOfAngle(distance, newAngle) + this.getCentreOfMass().getY();
//
//						s.updateBorderPoint(i, newX, newY);
//					}
//					
//				}
//			}
		}
	}
	
	/**
	 * Store an internal record of loggable activity
	 * @param message
	 */
	public void log(String message){
		log.append(message+System.getProperty("line.separator"));
	}
	
	/**
	 * Fetch the current nucleus log
	 * @return
	 */
	public String printLog(){
		
		return this.getNameAndNumber()+"\n"+log.toString();
	}
	
	

	
}