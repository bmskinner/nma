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

import ij.process.FloatPolygon;

import java.io.IOException;
import java.io.Serializable;

import analysis.profiles.ProfileCreator;
import components.generic.ISegmentedProfile;
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
	public void calculateProfiles() {
		
		/*
		 * The CurveRefolder currently only uses the angle profile
		 * so ignore the others to speed refolding
		 */
		ProfileCreator creator = new ProfileCreator(this);

		ISegmentedProfile profile = creator.createProfile(ProfileType.ANGLE);
				
		profileMap.put(ProfileType.ANGLE, profile);

	}
	
	@Override
	public FloatPolygon createOriginalPolygon(){
		return this.createPolygon();
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConsensusNucleus other = (ConsensusNucleus) obj;
		if (type != other.type)
			return false;
		return true;
	}
	
	

}
