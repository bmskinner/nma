/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package components.nuclei;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import components.generic.IPoint;
import components.generic.UnprofilableObjectException;
import components.nuclear.IBorderPoint;
import ij.gui.Roi;

/**
 * The class of non-round nuclei from which all other assymetric nuclei derive
 * @author ben
 * @since 1.13.3
 */
public abstract class AbstractAsymmetricNucleus extends DefaultNucleus {
	
	private static final long serialVersionUID = 1L;
	
	private transient List<IBorderPoint> tailEstimatePoints = new ArrayList<IBorderPoint>(3); // holds the points considered to be sperm tails before filtering
	protected transient boolean clockwiseRP = false; // is the original orientation of the nucleus with RP clockwise to the CoM, or not
	
	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public AbstractAsymmetricNucleus(Roi roi, IPoint centreOfMass, File f, int channel, int[] position, int number){
		super(roi, centreOfMass, f, channel, position, number );
	}

	protected AbstractAsymmetricNucleus(Nucleus n) throws UnprofilableObjectException {
		super(n);
	}


	public List<IBorderPoint> getEstimatedTailPoints(){
		return this.tailEstimatePoints;
	}

	protected void addTailEstimatePosition(IBorderPoint p){
		this.tailEstimatePoints.add(p);
	}

	@Override
	public boolean isClockwiseRP(){
		return this.clockwiseRP;
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		tailEstimatePoints = new ArrayList<IBorderPoint>(0);
		clockwiseRP = false;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();  
	}
}
