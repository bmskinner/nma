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
import stats.SignalStatistic;
import java.io.IOException;
import java.io.Serializable;

import components.AbstractCellularComponent;
import components.generic.XYPoint;

/**
 * A NuclearSignal is a region within a nucleus of interest, such as a chromosome paint. 
 * It is detected from an image, and thus implements CellularComponent
 * @author ben
 *
 */
public class NuclearSignal extends AbstractCellularComponent implements Serializable {

	private static final long serialVersionUID = 1L;

	private int closestNuclearBorderPoint;
	
	public NuclearSignal(Roi roi, double area, double feret, double perimeter, XYPoint centreOfMass){
		super(roi);
		
		this.setStatistic(SignalStatistic.AREA, area);
		this.setStatistic(SignalStatistic.MAX_FERET, feret);
		this.setStatistic(SignalStatistic.PERIMETER, perimeter);
		
		/*
	    Assuming the signal were a perfect circle of area equal
	    to the measured area, get the radius for that circle
		 */
		this.setStatistic(SignalStatistic.RADIUS,  Math.sqrt(area/Math.PI));
		
		
		this.setCentreOfMass(centreOfMass);
	}

	/**
	 * Create a copy of the given signal
	 * @param n
	 */
	public NuclearSignal(NuclearSignal n){
		super(n);

		this.closestNuclearBorderPoint = n.closestNuclearBorderPoint;

	}	

	/**
	 * Get the index of the closest point in the nuclear 
	 * periphery to this signal
	 * @return
	 */
	public int getClosestBorderPoint(){
		return this.closestNuclearBorderPoint;
	}

	/**
	 * Set the index of the closest point in the nuclear 
	 * periphery to this signal
	 * @return
	 */
	public void setClosestBorderPoint(int p){
		this.closestNuclearBorderPoint = p;
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//		finest("\tReading nuclear signal");
		in.defaultReadObject();
//		finest("\tRead nuclear signal");
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//		finest("\t\tWriting nuclear signal");
		out.defaultWriteObject();
//		finest("\t\tWrote nuclear signal");
	}

	@Override
	public void alignVertically() {
		return;
		
	}
}