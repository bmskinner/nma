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

import ij.gui.Roi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import utility.Constants.BorderTag;
import utility.Equation;
import utility.Utils;

import components.CellCollection.NucleusType;
import components.generic.BooleanProfile;
import components.generic.Profile;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.NucleusBorderPoint;
import components.nuclei.RoundNucleus;

public class RodentSpermNucleus
extends SpermNucleus
{

	private static final long serialVersionUID = 1L;
	

	private List<NucleusBorderPoint> hookRoi;
	private List<NucleusBorderPoint> humpRoi;

	// Requires a sperm nucleus object to construct from
	public RodentSpermNucleus(RoundNucleus n) throws Exception{
		super(n);
		// this.findPointsAroundBorder();
	}

	
	public RodentSpermNucleus (Roi roi, File file, int number, double[] position) { // construct from an roi
		super(roi, file, number, position);
	}
	
  	@Override
	public String getReferencePoint(){
		return NucleusType.RODENT_SPERM.getPoint(BorderTag.REFERENCE_POINT);
	}
  	
  	@Override
	public String getOrientationPoint(){
  		return NucleusType.RODENT_SPERM.getPoint(BorderTag.ORIENTATION_POINT);

	}

	/*
    Identify key points: tip, estimated tail position
	 */
	@Override
	public void findPointsAroundBorder() throws Exception{

		// find tip - use the least angle method
		int tipIndex = this.getAngleProfile().getIndexOfMin();
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
		NucleusBorderPoint spermTail2 = findTailPointFromMinima();
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
    NucleusBorderPoint spermTail1 = this.findTailByNarrowestWidthMethod();
    this.addTailEstimatePosition(spermTail1);
    // addBorderTag("spermTail1", this.getIndex(spermTail1));


    /*
      Given distinct methods for finding a tail,
      take a position between them on roi
    */
    int consensusTailIndex = this.getPositionBetween(spermTail2, spermTail1);
    NucleusBorderPoint consensusTail = this.getBorderPoint(consensusTailIndex);
    // consensusTailIndex = this.getPositionBetween(consensusTail, spermTail1);

    // this.setInitialConsensusTail(consensusTail);

    // addBorderTag("initialConsensusTail", consensusTailIndex);

    setBorderTag(BorderTag.ORIENTATION_POINT, consensusTailIndex);

    setBorderTag(BorderTag.INTERSECTION_POINT, this.getIndex(this.findOppositeBorder(consensusTail)));
  }

  /*
    -----------------------
    Qualities of a sperm head
    -----------------------
  */

  public boolean isHookSide(XYPoint p){
    if(Utils.createPolygon(hookRoi).contains( (float)p.getX(), (float)p.getY() ) ){
      return true;
    } else { 
      return false;
    }
  }


  public boolean isHumpSide(XYPoint p){
    if(Utils.createPolygon(humpRoi).contains( (float)p.getX(), (float)p.getY() ) ){
      return true;
    } else { 
      return false;
    }
  }    
  
  /*
    Checks if the smoothed array nuclear shape profile has the acrosome to the rear of the array
    If acrosome is at the beginning:
      returns true
    else returns false
    counts the number of points above 180 degrees in each half of the array
  */
  public boolean isProfileOrientationOK() throws Exception{

    int frontPoints = 0;
    int rearPoints = 0;

    Profile profile = this.getAngleProfile(BorderTag.REFERENCE_POINT);

    int midPoint = (int) (this.getLength()/2) ;
    for(int i=0; i<this.getLength();i++){ // integrate points over 180

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
  public NucleusBorderPoint findTailPointFromMinima() throws Exception{
  
    // we cannot be sure that the greatest distance between two points will be the endpoints
    // because the hook may begin to curve back on itself. We supplement this basic distance with
    // the distances of each point from the centre of mass. The points with the combined greatest
    // distance are both far from each other and far from the centre, and are a more robust estimate
    // of the true ends of the signal
    double tipToCoMDistance = this.getBorderTag(BorderTag.REFERENCE_POINT).getLengthTo(this.getCentreOfMass());
    BooleanProfile array = this.getAngleProfile().getLocalMinima(5);

    double maxDistance = 0;
    NucleusBorderPoint tail = this.getBorderTag(BorderTag.REFERENCE_POINT); // start at tip, move round

    for(int i=0; i<array.size();i++){
      if(array.get(i)==true){
            
        double distanceAcrossCoM = tipToCoMDistance + this.getCentreOfMass().getLengthTo(getPoint(i));
        double distanceBetweenEnds = this.getBorderTag(BorderTag.REFERENCE_POINT).getLengthTo(getPoint(i));
        
        double totalDistance = distanceAcrossCoM + distanceBetweenEnds;

        if(totalDistance > maxDistance){
          maxDistance = totalDistance;
          tail = getPoint(i);
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
  public NucleusBorderPoint findTailByNarrowestWidthMethod(){

    // Find the narrowest point around the CoM
    // For a position in teh roi, draw a line through the CoM to the intersection point
    // Measure the length; if < min length..., store equation and border(s)

    double minDistance = this.getFeret();
    NucleusBorderPoint reference = this.getBorderTag(BorderTag.REFERENCE_POINT);

    for(int i=0;i<this.getLength();i++){

      NucleusBorderPoint p = this.getPoint(i);
      NucleusBorderPoint opp = this.findOppositeBorder(p);
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
    NucleusBorderPoint tail = new NucleusBorderPoint(0,0);
    for(int i=0;i<this.getLength();i++){

      NucleusBorderPoint p = this.getBorderPoint(i);
      double angle = findAngleBetweenXYPoints(reference, this.getCentreOfMass(), p);
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
  private int findIntersectionPointForNuclearSplit(){
    // test if each point from the tail intersects the splitting line
    // determine the coordinates of the point intersected as int
    // for each xvalue of each point in array, get the line y value
    // at the point the yvalues are closest and not the tail point is the intersesction
    Equation lineEquation = new Equation(this.getCentreOfMass(), this.getBorderTag(BorderTag.ORIENTATION_POINT));
    double minDeltaY = 100;
    int minDeltaYIndex = 0;

    for(int i = 0; i<this.getLength();i++){
        double x = this.getBorderPoint(i).getX();
        double y = this.getBorderPoint(i).getY();
        double yOnLine = lineEquation.getY(x);

        double distanceToTail = this.getBorderPoint(i).getLengthTo(this.getBorderTag(BorderTag.ORIENTATION_POINT));

        double deltaY = Math.abs(y - yOnLine);
        if(deltaY < minDeltaY && distanceToTail > this.getFeret()/2){ // exclude points too close to the tail
          minDeltaY = deltaY;
          minDeltaYIndex = i;
        }
    }
    return minDeltaYIndex;
  }

  public void splitNucleusToHeadAndHump(){

    int intersectionPointIndex = findIntersectionPointForNuclearSplit();
    // this.intersectionPoint = this.getBorderPoint( intersectionPointIndex );
    this.setBorderTag(BorderTag.INTERSECTION_POINT, intersectionPointIndex );

    // get an array of points from tip to tail
    List<NucleusBorderPoint> roi1 = new ArrayList<NucleusBorderPoint>(0);
    List<NucleusBorderPoint> roi2 = new ArrayList<NucleusBorderPoint>(0);
    boolean changeRoi = false;

    for(int i = 0; i<this.getLength();i++){

      int currentIndex = Utils.wrapIndex(this.getBorderIndex(BorderTag.ORIENTATION_POINT)+i, this.getLength()); // start at the tail, and go around the array
      
      NucleusBorderPoint p = getPoint(currentIndex);

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
    for(int i=0;i<roi1.size();i++){
      if(roi1.get(i).overlaps(this.getBorderTag(BorderTag.REFERENCE_POINT))){
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
  public void calculateSignalAnglesFromPoint(NucleusBorderPoint p){

    super.calculateSignalAnglesFromPoint(p);

    // update signal angles with hook or hump side

    for( int i : signalCollection.getSignalGroups()){
    	List<NuclearSignal> signals = signalCollection.getSignals(i);

    	if(!signals.isEmpty()){

    		for(NuclearSignal n : signals){

    			// hook or hump?
    			double angle = n.getAngle();
    			if( this.isHookSide(n.getCentreOfMass()) ){ 
    				angle = 360 - angle;
    			}
    			n.setAngle(angle);
    		}
    	}
    }
  }
}