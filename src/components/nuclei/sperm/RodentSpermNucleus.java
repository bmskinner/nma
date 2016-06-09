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
  RODENT SPERM NUCLEUS CLASS
  -----------------------
  Contains the variables for storing a rodentsperm nucleus.
  Sperm have a hook, hump and tip, hence can be oriented
  in two axes.
*/  
package components.nuclei.sperm;

import ij.IJ;
import ij.gui.Roi;
import ij.process.FloatPolygon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import analysis.profiles.ProfileIndexFinder;
import analysis.profiles.RuleSet;
import stats.NucleusStatistic;
import stats.SignalStatistic;
import utility.Utils;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Equation;
import components.generic.Profile;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.BorderPoint;
import components.nuclei.Nucleus;

public class RodentSpermNucleus extends SpermNucleus {

	private static final long serialVersionUID = 1L;
	
	private transient double hookLength = 0;
	private transient double bodyWidth  = 0;

	
	public RodentSpermNucleus(Nucleus n) {
		super(n);
		this.splitNucleusToHeadAndHump();
	}
	
	protected RodentSpermNucleus(){
		super();
	}
	
	public RodentSpermNucleus (Roi roi, File file, int number, double[] position) { // construct from an roi
		super(roi, file, number, position);
	}
	
	@Override
	public Nucleus duplicate(){
		try {
			RodentSpermNucleus duplicate = new RodentSpermNucleus(this);			
			return duplicate;
			
		} catch (Exception e) {
			return null;
		}
	}
	
	@Override
	protected double calculateStatistic(NucleusStatistic stat){
		double result = super.calculateStatistic(stat);
		
		switch(stat){
			
			case HOOK_LENGTH:
				result = getHookOrBodyLength(true);
				break;
			case BODY_WIDTH:
				result = getHookOrBodyLength(false);
				break;
			default:
				return result;
		
		}
		return result;
		
	}
	
	@Override
	public void setBorderTag(BorderTag tag, int i){
		super.setBorderTag(tag, i);
		
			
		// If the flat region moved, update the cached lengths 
		if( this.hasBorderTag(BorderTag.TOP_VERTICAL) && this.hasBorderTag(BorderTag.BOTTOM_VERTICAL)){

			if(tag.equals(BorderTag.TOP_VERTICAL) || tag.equals(BorderTag.BOTTOM_VERTICAL)){
				
				// Clear cached stats
				this.hookLength = 0;
				this.bodyWidth  = 0;
//				try {
//					
//					calculateHookOrBodyLength();
//					
//				} catch (Exception e) {
//					this.hookLength = 0;
//					this.bodyWidth  = 0;
//				}
			}
		}
		
	}
	
	private double getHookOrBodyLength(boolean useHook) {

		if(hookLength==0 || bodyWidth==0){
			calculateHookOrBodyLength();
		}
		
		if(useHook){
			return hookLength;
		} else {
			return bodyWidth;
		}	
	}
	
	private void calculateHookOrBodyLength() {
			
		// Copy the nucleus
		RodentSpermNucleus testNucleus = new RodentSpermNucleus( this); //.duplicate();

		// Only proceed if the verticals have been set
		if(testNucleus!=null && testNucleus.hasBorderTag(BorderTag.TOP_VERTICAL) 
				&& testNucleus.hasBorderTag(BorderTag.BOTTOM_VERTICAL)){

			
			/*
			 * Get the X position of the top vertical
			 */
			double vertX = testNucleus.getBorderTag(BorderTag.TOP_VERTICAL).getX();

			/*
			 * Rotate the nucleus to put vertical
			 */
			BorderPoint[] points = getBorderPointsForVerticalAlignment();
			testNucleus.alignPointsOnVertical(points[0], points[1] );
			

			
			vertX = testNucleus.getBorderTag(BorderTag.TOP_VERTICAL).getX();


			/*
			 * Find the x values in the bounding box of the 
			 * vertical nucleus. Using reference point here is ok, the method
			 * is using the TOP and BOTTOM points internally.
			 */
			double maxBoundingX = testNucleus.getBoundingRectangle(BorderTag.REFERENCE_POINT).getMaxX();
			double minBoundingX = testNucleus.getBoundingRectangle(BorderTag.REFERENCE_POINT).getMinX();

			/*
			 * Find the distance from the vertical X position to the min and max points of the 
			 * bounding box. VertX must lie between these points.
			 */
			double distanceLower  = vertX - minBoundingX;
			double distanceHigher = maxBoundingX - vertX;

			/*
			 * To determine if the point is hook or hump, take
			 * the X position of the tip. This must lie on the
			 * hook side of the vertX
			 */
			
			double distanceHook = 0;
			double distanceHump = 0;
			double referenceX   = testNucleus.getBorderTag(BorderTag.REFERENCE_POINT).getX();
			
			if(referenceX < vertX){
				distanceHook = distanceLower;
				distanceHump = distanceHigher;
			} else {
				distanceHook = distanceHigher;
				distanceHump = distanceLower;
			}
			
			this.hookLength = distanceHook;
			this.bodyWidth  = distanceHump;

		} else {
			this.hookLength = 0;
			this.bodyWidth 	= 0;
		}
		testNucleus = null;
	}
	
	/**
	 * Get a copy of the points in the hook roi
	 * @return
	 */
	public List<BorderPoint> getHookRoi(){	
		
		BorderPoint testPoint         = this.getBorderTag(BorderTag.REFERENCE_POINT);
		BorderPoint referencePoint    = this.getBorderTag(BorderTag.REFERENCE_POINT);
		BorderPoint interSectionPoint = this.getBorderTag(BorderTag.INTERSECTION_POINT);
		BorderPoint orientationPoint  = this.getBorderTag(BorderTag.ORIENTATION_POINT);
		
		List<BorderPoint> result = new ArrayList<BorderPoint>(0);

		
		/*
		 * Go from the reference point. We hit either the IP or
		 * the OP depending on direction. On hitting one,
		 * move to the other and continue until we're back at the RP
		 */
		
//		boolean hasHitPoint = false;
		int i=0;
		BorderPoint continuePoint = null;
		
		while(testPoint.hasNextPoint()){
			result.add(testPoint);
			
//			IJ.log("Test point :"+testPoint.toString());
			if( testPoint.overlapsPerfectly(interSectionPoint) ){
				continuePoint = orientationPoint;
//				IJ.log("Hit IP :"+testPoint.toString());
				break;
			}
			
			if( testPoint.overlapsPerfectly(orientationPoint) ){
				continuePoint = interSectionPoint;
//				IJ.log("Hit OP :"+testPoint.toString());
				break;
			}
			
			testPoint = testPoint.nextPoint();
			
			/*
			 * Only allow the loop to go around the nucleus once
			 */
			if( testPoint.overlapsPerfectly(referencePoint) ){
//				IJ.log("Hit RP :"+testPoint.toString());
				break;
			}
			
			
			i++;
			if(i>1000){
				IJ.log("Forced break");
				break;
			}
		}
		
		if(continuePoint==null){
			IJ.log("Error getting roi - IP and OP not found");
			return result;
		}
		
		/*
		 * Continue until we're back at the RP
		 */
		while(continuePoint.hasNextPoint()){
			result.add(continuePoint);
//			IJ.log("Continue point :"+continuePoint.toString());
			if( continuePoint.overlapsPerfectly(referencePoint.prevPoint()) ){
				break;
			}
			
			continuePoint = continuePoint.nextPoint();
			i++;
			if(i>2000){
				IJ.log("Forced break for continue point");
				break;
			}
		}
		return result;
		
	}
			
    @Override
	public int identifyBorderTagIndex(BorderTag tag){
		
		int result = 0;
		switch(tag){
		
			case REFERENCE_POINT: 
			try {
				
				// The RP in mouse sperm is index with the minimum angle
				
				result = this.getProfile(ProfileType.REGULAR).getIndexOfMin();
			} catch (Exception e) {
				error("Error detecting RP in nucleus", e);
				result = 0;
			}
				break;
			default:
				break;
		}
		return result;
		
	}
    
	
	/*
    Identify key points: tip, estimated tail position
	 */
	@Override
	public void findPointsAroundBorder() throws Exception{

		
		RuleSet rpSet = RuleSet.mouseSpermRPRuleSet();
		Profile p     = this.getProfile(rpSet.getType());
		ProfileIndexFinder f = new ProfileIndexFinder();
		int tipIndex = f.identifyIndex(p, rpSet);
		
		
		// find tip - use the least angle method
//		int tipIndex = identifyBorderTagIndex(BorderTag.REFERENCE_POINT);
		setBorderTag(BorderTag.REFERENCE_POINT, tipIndex);

		// decide if the profile is right or left handed; flip if needed

		if(!this.isProfileOrientationOK()){
			this.reverse(); // reverses all profiles, border array and tagged points
		}  


		/*
      Find the tail point using multiple independent methods. 
      Find a consensus point

      Method 1: Use the list of local minima to detect the tail corner
                This is the corner furthest from the tip.
                Can be confused as to which side of the sperm head is chosen
		 */  
		BorderPoint spermTail2 = findTailPointFromMinima();
		this.addTailEstimatePosition(spermTail2);
		// addBorderTag("spermTail2", this.getIndex(spermTail2));

    /*
      Method 2: Look at the 2nd derivative - rate of change of angles
                Perform a 5win average smoothing of the deltas
                Count the number of consecutive >1 degree blocks
                Wide block far from tip = tail
    */  
    // NucleusBorderPoint spermTail3 = this.findTailFromDeltas();
    // this.addTailEstimatePosition(spermTail3);

    /*    
      Method 3: Find the narrowest diameter around the nuclear CoM
                Draw a line orthogonal, and pick the intersecting border points
                The border furthest from the tip is the tail
    */  
    BorderPoint spermTail1 = this.findTailByNarrowestWidthMethod();
    this.addTailEstimatePosition(spermTail1);
    // addBorderTag("spermTail1", this.getIndex(spermTail1));


    /*
      Given distinct methods for finding a tail,
      take a position between them on roi
    */
    int consensusTailIndex = this.getPositionBetween(spermTail2, spermTail1);
    BorderPoint consensusTail = this.getBorderPoint(consensusTailIndex);
    // consensusTailIndex = this.getPositionBetween(consensusTail, spermTail1);

    // this.setInitialConsensusTail(consensusTail);

    // addBorderTag("initialConsensusTail", consensusTailIndex);

    setBorderTag(BorderTag.ORIENTATION_POINT, consensusTailIndex);

    setBorderTag(BorderTag.INTERSECTION_POINT, this.getBorderIndex(this.findOppositeBorder(consensusTail)));
  }

	
	private FloatPolygon createRoiPolygon(List<BorderPoint> list){
		float[] xpoints = new float[list.size()+1];
		float[] ypoints = new float[list.size()+1];

		for(int i=0;i<list.size();i++){
			BorderPoint p = list.get(i);
			xpoints[i] = (float) p.getX();
			ypoints[i] = (float) p.getY();
		}

		// Ensure the polygon is closed
		xpoints[list.size()] = (float) list.get(0).getX();
		ypoints[list.size()] = (float) list.get(0).getY();

		return new FloatPolygon(xpoints, ypoints);
	}
	
	/**
	 * Check if the given point is in the hook side of the nucleus
	 * @param p
	 * @return
	 */
	public boolean isHookSide(XYPoint p){
		if(containsPoint(p)){

			/*
			 * Find out which side has been captured. The hook side
			 * has the reference point
			 */

			FloatPolygon poly = createRoiPolygon(getHookRoi());
			
			if(poly.contains((float)p.getX(), (float)p.getY() )){
				return true;
				
			} else {
				return false;
			}
			
		} else {
			throw new IllegalArgumentException("Requested point is not in the nucleus: "+p.toString());
		}
	}

	/**
	 * Checks if the smoothed array nuclear shape profile has the 
	 * acrosome to the rear of the array. 
	 * Counts the number of points above 180 degrees in each half of the array.
	 * @return true if acrosome is at the beginning of the profile
	 * @throws Exception
	 */
	public boolean isProfileOrientationOK() throws Exception{

		int frontPoints = 0;
		int rearPoints = 0;

		Profile profile = this.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT);

		int midPoint = (int) (this.getBorderLength()/2) ;
		for(int i=0; i<this.getBorderLength();i++){ // integrate points over 180

			if(i<midPoint){
				frontPoints += profile.get(i);
			}
			if(i>midPoint){
				rearPoints  += profile.get(i);
			}
		}

		if(frontPoints > rearPoints){ // if the maxIndex is closer to the end than the beginning
			return true;
		} else{ 
			return false;
		}
	}
	
	/*
	 * Test if the nucleus, after rotating to vertical, has the hook to the left
	 * or to the right
	 */
	public boolean isPointingLeft(){
		
		Nucleus testNucleus = this.getVerticallyRotatedNucleus();
		

		/*
		 * Get the X position of the reference point
		 */
		double vertX = testNucleus.getBorderTag(BorderTag.REFERENCE_POINT).getX();

		/*
		 * If the reference point is left of the centre of mass, 
		 * the nucleus is pointing left
		 */
		
		if(vertX < testNucleus.getCentreOfMass().getX() ){
			return true;
		} else {
			return false;
		}
		
	}
	
	@Override
	public Nucleus getVerticallyRotatedNucleus(){
		
		/*
		 * Ensure the nucleus is cached
		 */
		super.getVerticallyRotatedNucleus();
		
		/*
		 * Get the X position of the reference point
		 */
		double vertX = verticalNucleus.getBorderTag(BorderTag.REFERENCE_POINT).getX();

		/*
		 * If the reference point is left of the centre of mass, 
		 * the nucleus is pointing left. If not, flip thw nucleus
		 */
		
		if(vertX > verticalNucleus.getCentreOfMass().getX() ){
			verticalNucleus.flipXAroundPoint(verticalNucleus.getCentreOfMass());
			verticalNucleus.moveCentreOfMass(new XYPoint(0,0));
		}
		
		return verticalNucleus;
	}

  /*
    -----------------------
    Methods for detecting the tail
    -----------------------
  */

  /*
    Detect the tail based on a list of local minima in an NucleusBorderPoint array.
    The putative tail is the point furthest from the sum of the distances from the CoM and the tip
  */
  public BorderPoint findTailPointFromMinima() throws Exception{
  
    // we cannot be sure that the greatest distance between two points will be the endpoints
    // because the hook may begin to curve back on itself. We supplement this basic distance with
    // the distances of each point from the centre of mass. The points with the combined greatest
    // distance are both far from each other and far from the centre, and are a more robust estimate
    // of the true ends of the signal
    double tipToCoMDistance = this.getBorderTag(BorderTag.REFERENCE_POINT).getLengthTo(this.getCentreOfMass());
    BooleanProfile array = this.getProfile(ProfileType.REGULAR).getLocalMinima(5);

    double maxDistance = 0;
    BorderPoint tail = this.getBorderTag(BorderTag.REFERENCE_POINT); // start at tip, move round

    for(int i=0; i<array.size();i++){
      if(array.get(i)==true){
            
        double distanceAcrossCoM = tipToCoMDistance + this.getCentreOfMass().getLengthTo(getBorderPoint(i));
        double distanceBetweenEnds = this.getBorderTag(BorderTag.REFERENCE_POINT).getLengthTo(getBorderPoint(i));
        
        double totalDistance = distanceAcrossCoM + distanceBetweenEnds;

        if(totalDistance > maxDistance){
          maxDistance = totalDistance;
          tail = getBorderPoint(i);
        }
      }
    }
    return tail;
  }


  /*
    This is a method for finding a tail point independent of local minima:
      Find the narrowest diameter around the nuclear CoM
      Draw a line orthogonal, and pick the intersecting border points
      The border furthest from the tip is the tail
  */
  public BorderPoint findTailByNarrowestWidthMethod() throws Exception{

    // Find the narrowest point around the CoM
    // For a position in teh roi, draw a line through the CoM to the intersection point
    // Measure the length; if < min length..., store equation and border(s)

    double minDistance = this.getStatistic(NucleusStatistic.MAX_FERET);
    BorderPoint reference = this.getBorderTag(BorderTag.REFERENCE_POINT);

    for(int i=0;i<this.getBorderLength();i++){

      BorderPoint p = this.getBorderPoint(i);
      BorderPoint opp = this.findOppositeBorder(p);
      double distance = p.getLengthTo(opp);

      if(distance<minDistance){
        minDistance = distance;
        reference = p;
      }
    }
//    this.minFeretPoint1 = reference;
//    this.minFeretPoint2 = this.findOppositeBorder(reference);
    
    // Using the point, draw a line from teh CoM to the border. Measure the angle to an intersection point
    // if close to 90, and the distance to the tip > CoM-tip, keep the point
    // return the best point
    double difference = 90;
    BorderPoint tail = new BorderPoint(0,0);
    for(int i=0;i<this.getBorderLength();i++){

      BorderPoint p = this.getBorderPoint(i);
      double angle = Utils.findAngleBetweenXYPoints(reference, this.getCentreOfMass(), p);
      if(  Math.abs(90-angle)<difference && 
          p.getLengthTo(this.getBorderTag(BorderTag.REFERENCE_POINT)) > this.getCentreOfMass().getLengthTo( this.getBorderTag(BorderTag.REFERENCE_POINT) ) ){
        difference = 90-angle;
        tail = p;
      }
    }
    return tail;
  }

  /*
    -----------------------
    Methods for dividing the nucleus to hook
    and hump sides
    -----------------------
  */

  /*
    In order to split the nuclear roi into hook and hump sides,
    we need to get an intersection point of the line through the 
    tail and centre of mass with the opposite border of the nucleus.
  */
  private int findIntersectionPointForNuclearSplit() {
    // test if each point from the tail intersects the splitting line
    // determine the coordinates of the point intersected as int
    // for each xvalue of each point in array, get the line y value
    // at the point the yvalues are closest and not the tail point is the intersesction
    Equation lineEquation = new Equation(this.getCentreOfMass(), this.getBorderTag(BorderTag.ORIENTATION_POINT));
    double minDeltaY = 100;
    int minDeltaYIndex = 0;

    for(int i = 0; i<this.getBorderLength();i++){
        double x = this.getBorderPoint(i).getX();
        double y = this.getBorderPoint(i).getY();
        double yOnLine = lineEquation.getY(x);

        double distanceToTail = this.getBorderPoint(i).getLengthTo(this.getBorderTag(BorderTag.ORIENTATION_POINT));

        double deltaY = Math.abs(y - yOnLine);
        if(deltaY < minDeltaY && distanceToTail > this.getStatistic(NucleusStatistic.MAX_FERET)/2){ // exclude points too close to the tail
          minDeltaY = deltaY;
          minDeltaYIndex = i;
        }
    }
    return minDeltaYIndex;
  }

  public void splitNucleusToHeadAndHump() {

	  if(!this.hasBorderTag(BorderTag.INTERSECTION_POINT)){
		  int index = findIntersectionPointForNuclearSplit();
		  this.setBorderTag(BorderTag.INTERSECTION_POINT, index );
	  } 
  }

  /*
    -----------------------
    Methods for measuring signal positions
    -----------------------
  */

  // needs to override AsymmetricNucleus version because hook/hump
  @Override
  public void calculateSignalAnglesFromPoint(BorderPoint p) throws Exception {


	  super.calculateSignalAnglesFromPoint(p);

	  if(this.hasSignal()){
		  
//		  IJ.log(this.dumpInfo(BORDER_TAGS));

		  // update signal angles with hook or hump side
		  for( int i : signalCollection.getSignalGroups()){

			  if(signalCollection.hasSignal(i)){

				  List<NuclearSignal> signals = signalCollection.getSignals(i);

				  for(NuclearSignal n : signals){

					  /*
					   * Angle begins from the orientation point 
					   */

					  double angle = n.getStatistic(SignalStatistic.ANGLE);

					  try{
						  // This com is offset, not original
						  XYPoint com = n.getCentreOfMass();

						  // These rois are offset, not original
						  if( this.isHookSide(com) ){ 
							  angle = 360 - angle;
//							  IJ.log("Signal com is hookside");
						  } 
//						  IJ.log("Signal com: "  +com.toString());
//						  IJ.log("Signal angle: "+angle);
					  } catch(Exception e){
						  // IJ.log(this.getNameAndNumber()+": Error detected: falling back on default angle: "+e.getMessage());
					  } finally {

						  n.setStatistic(SignalStatistic.ANGLE, angle);

					  }
				  }
			  }
		  }
	  }
  }
  
  @Override
  public void rotate(double angle){
				
		if(angle!=0){

//			for(BorderPoint p : hookRoi){
////				XYPoint p = this.getBorderPoint(i);
//
//
//				// get the distance from this point to the centre of mass
//				double distance = p.getLengthTo(this.getCentreOfMass());
//
//				// get the angle between the centre of mass (C), the point (P) and a
//				// point directly under the centre of mass (V)
//
//				/*
//				 *      C
//				 *      |\  
//				 *      V P
//				 * 
//				 */
//				double oldAngle = Utils.findAngleBetweenXYPoints( p, 
//						this.getCentreOfMass(), 
//						new XYPoint(this.getCentreOfMass().getX(),-10));
//
//
//				if(p.getX()<this.getCentreOfMass().getX()){
//					oldAngle = 360-oldAngle;
//				}
//
//				double newAngle = oldAngle + angle;
//				double newX = Utils.getXComponentOfAngle(distance, newAngle) + this.getCentreOfMass().getX();
//				double newY = Utils.getYComponentOfAngle(distance, newAngle) + this.getCentreOfMass().getY();
//
//				p.setX(newX);
//				p.setY(newY);
//			}
//			
//			for(BorderPoint p : humpRoi){
////				XYPoint p = this.getBorderPoint(i);
//
//
//				// get the distance from this point to the centre of mass
//				double distance = p.getLengthTo(this.getCentreOfMass());
//
//				// get the angle between the centre of mass (C), the point (P) and a
//				// point directly under the centre of mass (V)
//
//				/*
//				 *      C
//				 *      |\  
//				 *      V P
//				 * 
//				 */
//				double oldAngle = Utils.findAngleBetweenXYPoints( p, 
//						this.getCentreOfMass(), 
//						new XYPoint(this.getCentreOfMass().getX(),-10));
//
//
//				if(p.getX()<this.getCentreOfMass().getX()){
//					oldAngle = 360-oldAngle;
//				}
//
//				double newAngle = oldAngle + angle;
//				double newX = Utils.getXComponentOfAngle(distance, newAngle) + this.getCentreOfMass().getX();
//				double newY = Utils.getYComponentOfAngle(distance, newAngle) + this.getCentreOfMass().getY();
//
//				p.setX(newX);
//				p.setY(newY);
//			}
			super.rotate(angle);
		}
	}
  
  
  @Override
  public String dumpInfo(int type){
	  String result = super.dumpInfo(type);
	  
//	  result += "  Hook roi:\n";
//	  for(int i=0; i<hookRoi.size(); i++){
//		  BorderPoint p = hookRoi.get(i);
//		  result += "      Index "+i+": "+p.getX()+"\t"+p.getY()+"\n";
//	  }
//	  
//	  result += "  Hump roi:\n";
//	  for(int i=0; i<humpRoi.size(); i++){
//		  BorderPoint p = humpRoi.get(i);
//		  result += "      Index "+i+": "+p.getX()+"\t"+p.getY()+"\n";
//	  }
	  
	  return result;
	  
  }
  
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	  finest("\tReading rodent sperm nucleus");
	  in.defaultReadObject();
	  this.hookLength = 0;
	  this.bodyWidth = 0;
	  finest("\tRead rodent sperm nucleus");
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
	  finest("\tWriting rodent sperm nucleus");
	  out.defaultWriteObject();
	  finest("\tWrote rodent sperm nucleus");
  }
  
//  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//	    in.defaultReadObject();
////	    try {
////						
////		} catch (Exception e) {
//		    this.hookLength = 0;
//		    this.bodyWidth = 0;
////		}
//
//	}
}