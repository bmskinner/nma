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
package components.nuclei;

import java.io.IOException;
import java.io.Serializable;

import components.generic.ProfileType;
import components.nuclear.NucleusType;

/**
 * This holds methods for manipulatiing a refolded consensus nucleus
 *
 */
public class ConsensusNucleus extends RoundNucleus implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private NucleusType type;
	
	
	public ConsensusNucleus(Nucleus n, NucleusType type) {
		
		super(n);
		this.type = type;
	}
	
	public NucleusType getType(){
		return this.type;
	}
	
	@Override
	public void calculateProfiles() throws Exception{

		this.profileMap.put(ProfileType.REGULAR, this.calculateAngleProfile());

	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		finest("\tReading consensus nucleus");
		in.defaultReadObject();
		finest("\tRead consensus nucleus");
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		finest("\tWriting consensus nucleus");
		out.defaultWriteObject();
		finest("\tWrote consensus nucleus");
	}

}
