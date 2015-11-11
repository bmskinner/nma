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
package components;

import java.io.Serializable;
import java.util.UUID;

public class Mitochondrion implements Serializable, CellularComponent {

	private static final long serialVersionUID = 1L;
	private UUID uuid;
	protected double[] orignalPosition; // the xbase, ybase, width and height of the original bounding rectangle
	
	public Mitochondrion(){
		this.uuid = java.util.UUID.randomUUID();
	}
	
	public Mitochondrion(Mitochondrion m){
		this.uuid = java.util.UUID.randomUUID();
		this.orignalPosition = m.getPosition();
	} 

	public UUID getID() {	
		return this.uuid;
	}

	public double[] getPosition() {
		return this.orignalPosition;
	}

	@Override
	public double getArea() {
		// TODO Auto-generated method stub
		return 0;
	}
}
