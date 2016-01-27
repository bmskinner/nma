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

import stats.NucleusStatistic;
import stats.SignalStatistic;
import utility.Utils;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Equation;
import components.generic.MeasurementScale;
import components.generic.Profile;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.BorderPoint;
import components.nuclear.NucleusType;
import components.nuclear.SignalCollection;
import components.nuclei.Nucleus;
import components.nuclei.RoundNucleus;

public class RodentSpermNucleus
extends SpermNucleus
{

	private static final long serialVersionUID = 1L;
	

	private List<BorderPoint> hookRoi;
	private List<BorderPoint> humpRoi;
	
	private transient double hookLength = 0;
	private transient double bodyWidth = 0;

	// Requires a sperm nucleus object to construct from
	public RodentSpermNucleus(RoundNucleus n) throws Exception{
		super(n);
//		this.splitNucleusToHeadAndHump();
	}
	
	public RodentSpermNucleus(Nucleus n) throws Exception{
		super(n);
//		this.splitNucleusToHeadAndHump();
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
			duplicate.setHookRoi(this.getHookRoi());
			duplicate.setHumpRoi(this.getHumpRoi());
			return duplicate;
			
		} catch (Exception e) {
			return null;
		}
	}
	
	@Override
	protected double calculateStatistic(NucleusStatistic stat) throws Exception{
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
		if(tag.equals(BorderTag.TOP_VERTICAL) || tag.equals(BorderTag.BOTTOM_VERTICAL)){
			try {
				calculateHookOrBodyLength();
			} catch (Exception e) {
				this.hookLength = 0;
			    this.bodyWidth = 0;
			}
		}
		
	}
	
	protected double getHookOrBodyLength(boolean useHook) throws Exception{
		if(useHook){
			if(hookLength==0){
				calculateHookOrBodyLength();
			}
			return hookLength;
		} else {
			if(bodyWidth==0){
				calculateHookOrBodyLength();
			}
			return bodyWidth;
		}	
	}
	
	protected void calculateHookOrBodyLength() throws Exception{

		// Copy the nucleus
		
		RodentSpermNucleus testNucleus = (RodentSpermNucleus) this.duplicate();

		// Only proceed if the verticals have been set
		if(testNucleus!=null && testNucleus.hasBorderTag(BorderTag.TOP_VERTICAL) && testNucleus.hasBorderTag(BorderTag.BOTTOM_VERTICAL)){

			double vertX = testNucleus.getBorderTag(BorderTag.TOP_VERTICAL).getX();
//			IJ.log("Initial vertX:" +vertX);
			BorderPoint[] points = getBorderPointsForVerticalAlignment();
			// Rotate vertical
//			IJ.log(testNucleus.dumpInfo(Nucleus.BORDER_TAGS));
			testNucleus.alignPointsOnVertical(points[0], points[1] );
			
			// Ensure that the rois are correctly assigned
			testNucleus.splitNucleusToHeadAndHump();


			
			// Measure vertical to bounding hook side
			// Measure vertical to bounding hump side
			vertX = testNucleus.getBorderTag(BorderTag.TOP_VERTICAL).getX();
//			IJ.log("Final vertX:" +vertX);

			double maxBoundingX = testNucleus.getBoundingRectangle(BorderTag.REFERENCE_POINT).getMaxX();
			double minBoundingX = testNucleus.getBoundingRectangle(BorderTag.REFERENCE_POINT).getMinX();

			double distanceLeft = vertX - minBoundingX;
			double distanceRight = maxBoundingX - vertX;

			double distanceHook = 0;
			double distanceHump = 0;
			/*
			 * To determine if the point is hook or hump, take a value either
			 * size of the CoM, and test if it is closer to the desired bounding
			 * box value.
			 * 
			 * Since the nucleus is rotated, there should not be issues...
			 */

			XYPoint newLeftPoint = new XYPoint(testNucleus.getCentreOfMass().getX()-5, testNucleus.getCentreOfMass().getY());

			if(testNucleus.isHookSide( newLeftPoint) ){

				distanceHook = distanceLeft;
				distanceHump = distanceRight;

			} else {
				distanceHook = distanceRight;
				distanceHump = distanceLeft;
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
		List<BorderPoint> result = new ArrayList<BorderPoint>(0);
		for(BorderPoint n : hookRoi){
			result.add(new BorderPoint(n));
		}
		return result;
	}
	
	/**
	 * Get a copy of the points in the hook roi
	 * @return
	 */
	public List<BorderPoint> getHumpRoi(){
		List<BorderPoint> result = new ArrayList<BorderPoint>(0);
		for(BorderPoint n : humpRoi){
			result.add(new BorderPoint(n));
		}
		return result;
	}
	
	protected void setHookRoi(List<BorderPoint> list){
		this.hookRoi = list;
	}
	
	protected void setHumpRoi(List<BorderPoint> list){
		this.humpRoi = list;
	}
	
	/*
    Identify key points: tip, estimated tail position
	 */
	@Override
	public void findPointsAroundBorder() throws Exception{

		// find tip - use the least angle method
		int tipIndex = this.getProfile(ProfileType.REGULAR).getIndexOfMin();
		setBorderTag(BorderTag.REFERENCE_POINT, tipIndex);

		// decide if the profile is right or left handed; flip if needed
		// IJ.log("    Nucleus "+this.getNucleusNumber());
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

	/**
	 * Check if the given point is in the hook side of the nucleus
	 * @param p
	 * @return
	 */
	public boolean isHookSide(XYPoint p){
		if(containsPoint(p)){
			
			/*
			 * Are border list and hook hump rois offset the same?
			 * Yes.
			 * Both are using the offset positions, not original positions
			 */
						
			FloatPolygon poly = Utils.createPolygon(hookRoi);
//			IJ.log("Hook roi: "+ poly.getBounds().toString());
			if(poly.contains( (float)p.getX(), (float)p.getY() ) ){
//				IJ.log("Contains "+p.toString());
				return true;
			} else {
//				IJ.log("Not contains "+p.toString());
				return false;
			}
		} else {
			throw new IllegalArgumentException("Requested point is not in the nucleus: "+p.toString());
		}
	}


	/**
	 * Check if the given point is in the hump side of the nucleus
	 * @param p
	 * @return
	 */
	public boolean isHumpSide(XYPoint p){			
		if(isHookSide(p)){
			return false;
		} else {
			return true;
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
  private int findIntersectionPointForNuclearSplit() throws Exception{
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

  public void splitNucleusToHeadAndHump() throws Exception{
	  

    int intersectionPointIndex = findIntersectionPointForNuclearSplit();
    // this.intersectionPoint = this.getBorderPoint( intersectionPointIndex );
    this.setBorderTag(BorderTag.INTERSECTION_POINT, intersectionPointIndex );

    // get an array of points from tip to tail
    List<BorderPoint> roi1 = new ArrayList<BorderPoint>(0);
    List<BorderPoint> roi2 = new ArrayList<BorderPoint>(0);
    boolean changeRoi = false;

    for(int i = 0; i<this.getBorderLength();i++){

      int currentIndex = Utils.wrapIndex(this.getBorderIndex(BorderTag.ORIENTATION_POINT)+i, this.getBorderLength()); // start at the tail, and go around the array
      
      BorderPoint p = getBorderPoint(currentIndex);

      if(currentIndex != intersectionPointIndex && !changeRoi){   // starting at the tip, assign points to roi1
        roi1.add(p);
      }
      if(currentIndex==intersectionPointIndex && !changeRoi){ // until we hit the intersection point. Then, close the polygon of roi1 back to the tip. Switch to roi2
        roi1.add(p);
        roi1.add(this.getBorderTag(BorderTag.ORIENTATION_POINT));
        roi2.add(this.getBorderTag(BorderTag.INTERSECTION_POINT));
        changeRoi = true;
      }
      if(currentIndex != intersectionPointIndex && currentIndex != this.getBorderIndex(BorderTag.ORIENTATION_POINT) && changeRoi){   // continue with roi2, adjusting the index numbering as needed
        roi2.add(p);
      }

      if(currentIndex==this.getBorderIndex(BorderTag.ORIENTATION_POINT) && changeRoi){ // after reaching the tail again, close the polygon back to the intersection point
        roi2.add(this.getBorderTag(BorderTag.INTERSECTION_POINT));
      }

    }

    // default
    this.hookRoi = roi2;
    this.humpRoi = roi1;

    //    check if we need to swap
    for(BorderPoint point : roi1){
    	if(point.overlaps(this.getBorderTag(BorderTag.REFERENCE_POINT))){
    		this.hookRoi = roi1;
        	this.humpRoi = roi2;
        	break;
    	}
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

			for(BorderPoint p : hookRoi){
//				XYPoint p = this.getBorderPoint(i);


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

				p.setX(newX);
				p.setY(newY);
			}
			
			for(BorderPoint p : humpRoi){
//				XYPoint p = this.getBorderPoint(i);


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

				p.setX(newX);
				p.setY(newY);
			}
			super.rotate(angle);
		}
	}
  
  
  @Override
  public String dumpInfo(int type){
	  String result = super.dumpInfo(type);
	  
	  result += "  Hook roi:\n";
	  for(int i=0; i<hookRoi.size(); i++){
		  BorderPoint p = hookRoi.get(i);
		  result += "      Index "+i+": "+p.getX()+"\t"+p.getY()+"\n";
	  }
	  
	  result += "  Hump roi:\n";
	  for(int i=0; i<humpRoi.size(); i++){
		  BorderPoint p = humpRoi.get(i);
		  result += "      Index "+i+": "+p.getX()+"\t"+p.getY()+"\n";
	  }
	  
	  return result;
	  
  }
  
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	    in.defaultReadObject();
	    try {
	    	
	    	// calculate for new datasets
			calculateHookOrBodyLength();
						
		} catch (Exception e) {
		    this.hookLength = 0;
		    this.bodyWidth = 0;
		}

	}
}