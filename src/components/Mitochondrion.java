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

import java.io.IOException;

import components.nuclear.BorderPoint;

public class Mitochondrion extends AbstractCellularComponent {

	private static final long serialVersionUID = 1L;
	
	public Mitochondrion(){
		super();
	}
	
	public Mitochondrion(final Mitochondrion m){
		super(m);
	} 
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		finest("\tWriting mitochondrion");
		out.defaultWriteObject();
		finest("\tWrote mitochondrion");
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		finest("Reading mitochondrion");
		in.defaultReadObject();
		finest("Read mitochondrion"); 
	}

	@Override
	public void alignVertically() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public CellularComponent duplicate() {
		return new Mitochondrion(this);
	}

}
