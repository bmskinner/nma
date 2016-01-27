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
import stats.SignalStatistic;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import components.AbstractCellularComponent;
import components.CellularComponent;
import components.generic.XYPoint;

/*
  -----------------------
  NUCLEUS SIGNAL CLASS
  -----------------------
  Contains the variables for storing a signal within the nucleus
*/  
public class NuclearSignal extends AbstractCellularComponent implements Serializable {

	private static final long serialVersionUID = 1L;

	private int closestNuclearBorderPoint;

	private List<BorderPoint> borderList = new ArrayList<BorderPoint>(0); // replace ROI

	public NuclearSignal(Roi roi, double area, double feret, double perimeter, XYPoint centreOfMass, String origin){

		FloatPolygon polygon = roi.getInterpolatedPolygon(1,true);
		for(int i=0; i<polygon.npoints; i++){
			borderList.add(new BorderPoint( polygon.xpoints[i], polygon.ypoints[i]));
		}
		
		this.setStatistic(SignalStatistic.AREA, area);
		this.setStatistic(SignalStatistic.MAX_FERET, feret);
		this.setStatistic(SignalStatistic.PERIMETER, perimeter);
		
		/*
	    Assuming the signal were a perfect circle of area equal
	    to the measured area, get the radius for that circle
		 */
		this.setStatistic(SignalStatistic.RADIUS,  Math.sqrt(area/Math.PI));
	}

	/**
	 * Create a copy of the given signal
	 * @param n
	 */
	public NuclearSignal(NuclearSignal n){
		super(n);
		this.borderList = n.getBorder();
		this.closestNuclearBorderPoint = n.getClosestBorderPoint();

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
	public List<BorderPoint> getBorder(){
		List<BorderPoint> result = new ArrayList<BorderPoint>();
		for(BorderPoint p : borderList){
			result.add(new BorderPoint(p));
		}
		return result;
	}
	
	/**
	 * Get the number of border points
	 * @return
	 */
	public int getBorderSize(){
		return this.borderList.size();
	}
	
	/**
	 * Get a copy of the border point at the given index
	 * @param index
	 * @return
	 */
	public BorderPoint getBorderPoint(int index){
		return new BorderPoint(borderList.get(index));
	}
	
	/**
	 * Update the border point at the given index to a new position
	 * @param index
	 * @param newX
	 * @param newY
	 */
	public void updateBorderPoint(int index, double newX, double newY){
		this.borderList.get(index).setX(newX);
		this.borderList.get(index).setY(newY);
	}



	public int getClosestBorderPoint(){
		return this.closestNuclearBorderPoint;
	}


	public void setClosestBorderPoint(int p){
		this.closestNuclearBorderPoint = p;
	}
}