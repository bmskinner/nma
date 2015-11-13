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
package components.nuclear;

import ij.gui.Roi;
import ij.process.FloatPolygon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import components.generic.XYPoint;

/*
  -----------------------
  NUCLEUS SIGNAL CLASS
  -----------------------
  Contains the variables for storing a signal within the nucleus
*/  
public class NuclearSignal implements Serializable {

	private static final long serialVersionUID = 1L;
	private double area;
	private double perimeter;
	private double feret;
	private double angleFromReferencePoint;
	private double distanceFromCentreOfMass; // the absolute measured distance from the signal CoM to the nuclear CoM
	private double fractionalDistanceFromCoM; // the distance to the centre of mass as a fraction of the distance from the CoM to the closest border

	private XYPoint centreOfMass;
	private int closestNuclearBorderPoint;
	private String origin; // can store the image and nucleus the signal was found in

	private List<NucleusBorderPoint> borderList = new ArrayList<NucleusBorderPoint>(0); // replace ROI

	public NuclearSignal(Roi roi, double area, double feret, double perimeter, XYPoint centreOfMass, String origin){

		FloatPolygon polygon = roi.getInterpolatedPolygon(1,true);
		for(int i=0; i<polygon.npoints; i++){
			borderList.add(new NucleusBorderPoint( polygon.xpoints[i], polygon.ypoints[i]));
		}
		this.area = area;
		this.perimeter = perimeter;
		this.feret = feret;
		this.centreOfMass = centreOfMass;
		this.origin = origin;
	}

	/**
	 * Create a copy of the given signal
	 * @param n
	 */
	public NuclearSignal(NuclearSignal n){
		this.borderList = n.getBorder();
		this.area = n.getArea();
		this.perimeter = n.getPerimeter();
		this.feret = n.getFeret();
		this.centreOfMass = new XYPoint(n.getCentreOfMass());
		this.distanceFromCentreOfMass = n.getDistanceFromCoM();
		this.fractionalDistanceFromCoM = n.getFractionalDistanceFromCoM();
		this.angleFromReferencePoint = n.getAngle();
		this.closestNuclearBorderPoint = n.getClosestBorderPoint();
		this.origin = n.getOrigin();
	}

	/*
    -----------------------
    Getters for basic values within nucleus
    -----------------------
	 */

	/**
	 * Get a copy of the border points defining this signal
	 * @return
	 */
	public List<NucleusBorderPoint> getBorder(){
		List<NucleusBorderPoint> result = new ArrayList<NucleusBorderPoint>();
		for(NucleusBorderPoint p : borderList){
			result.add(new NucleusBorderPoint(p));
		}
		return result;
	}

	public double getArea(){
		return this.area;
	}

	public double getPerimeter(){
		return this.perimeter;
	}

	public double getFeret(){
		return this.feret;
	}

	public double getAngle(){
		return this.angleFromReferencePoint;
	}

	public double getDistanceFromCoM(){
		return this.distanceFromCentreOfMass;
	}

	public double getFractionalDistanceFromCoM(){
		return this.fractionalDistanceFromCoM;
	}

	public XYPoint getCentreOfMass(){
		return new XYPoint(this.centreOfMass);
	}

	public int getClosestBorderPoint(){
		return this.closestNuclearBorderPoint;
	}

	public String getOrigin(){
		return this.origin;
	}

	/*
    Assuming the signal were a perfect circle of area equal
    to the measured area, get the radius for that circle
	 */
	public double getRadius(){
		// r = sqrt(a/pi)
		return Math.sqrt(this.area/Math.PI);
	}

	/*
    -----------------------
    Setters for externally calculated values
    -----------------------
	 */
	public void setAngle(double d){
		this.angleFromReferencePoint = d;
	}

	public void setDistanceFromCoM(double d){
		this.distanceFromCentreOfMass = d;
	}

	public void setFractionalDistanceFromCoM(double d){
		this.fractionalDistanceFromCoM = d;
	}

	public void setClosestBorderPoint(int p){
		this.closestNuclearBorderPoint = p;
	}
}