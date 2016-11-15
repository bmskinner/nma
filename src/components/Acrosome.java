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

public class Acrosome extends  AbstractCellularComponent implements IAcrosome {

	private static final long serialVersionUID = 1L;

	public Acrosome(){

	}
	
	public Acrosome(IAcrosome a){
		super(a);
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		finest("\tWriting acrosome");
		out.defaultWriteObject();
		finest("\tWrote acrosome");
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		finest("\tReading acrosome");
		in.defaultReadObject();
		finest("\tRead acrosome"); 
	}

	/* (non-Javadoc)
	 * @see components.IAcrosome#alignVertically()
	 */
	@Override
	public void alignVertically() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see components.IAcrosome#duplicate()
	 */
	@Override
	public IAcrosome duplicate() {
		return new Acrosome(this);
	}

	@Override
	public String getSourceFileNameWithoutExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSmoothByDefault() {
		// TODO Auto-generated method stub
		return false;
	}

}
