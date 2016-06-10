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
  ASYMMETRIC NUCLEUS CLASS
  -----------------------
  Contains the variables for storing a non-circular nucleus.
  They have a head and a tail, hence can be oriented
  in one axis.

  A tail is the point determined via profile analysis. The
  head is assigned as the point opposite through the CoM.
*/  
package components.nuclei;


import ij.gui.Roi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import utility.Utils;
import components.generic.BorderTag;
import components.generic.XYPoint;
import components.nuclear.BorderPoint;

public class AsymmetricNucleus
  extends RoundNucleus
{

	private static final long serialVersionUID = 1L;
	private transient List<BorderPoint> tailEstimatePoints = new ArrayList<BorderPoint>(0); // holds the points considered to be sperm tails before filtering

  public AsymmetricNucleus(Nucleus n) {
    super(n);
  }
  
  protected AsymmetricNucleus(){
	  super();
  }

  public AsymmetricNucleus (Roi roi, File file, int number, double[] position) { // construct from an roi
		super(roi, file, number, position);
	}
  
  @Override
  public Nucleus duplicate(){
	  try {

		  AsymmetricNucleus duplicate = new AsymmetricNucleus(this);
		  return duplicate;

	  } catch (Exception e) {
		  return null;
	  }
  }

  /*
    -----------------------
    Get nucleus features
    -----------------------
  */
  
  public List<BorderPoint> getEstimatedTailPoints(){
    return this.tailEstimatePoints;
  }

  /*
    -----------------------
    Set nucleus features
    -----------------------
  */

  protected void addTailEstimatePosition(BorderPoint p){
    this.tailEstimatePoints.add(p);
  }

  /*
    Find the angle that the nucleus must be rotated to make the CoM-tail vertical.
    Uses the angle between [sperm tail x,0], sperm tail, and sperm CoM
    Returns an angle
  */
  @Override
  public double findRotationAngle(){
	  XYPoint end = new XYPoint(this.getPoint(BorderTag.ORIENTATION_POINT).getXAsInt(),this.getPoint(BorderTag.ORIENTATION_POINT).getYAsInt()-50);

	  double angle = Utils.findAngleBetweenXYPoints(end, this.getPoint(BorderTag.ORIENTATION_POINT), this.getCentreOfMass());

	  if(this.getCentreOfMass().getX() < this.getPoint(BorderTag.ORIENTATION_POINT).getX()){
		  return angle;
	  } else {
		  return 0-angle;
	  }
  }
  
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	  finest("\tReading asymmetric nucleus");
	  in.defaultReadObject();
	  tailEstimatePoints = new ArrayList<BorderPoint>(0);
	  finest("\tRead asymmetric nucleus");
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
	  finest("\tWriting asymmetric nucleus");
	  out.defaultWriteObject();

		// Now ensure we don't recurse over the BorderList
//			
//					
//		for(BorderPoint p : tailEstimatePoints){
//			out.writeBoolean(true); // Another point awaits
//			out.writeDouble(p.getX());
//			out.writeDouble(p.getY());
//		}
//		out.writeBoolean(false);
	  
	  finest("\tWrote asymmetric nucleus");
  }
}