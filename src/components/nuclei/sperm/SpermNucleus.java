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
  SPERM NUCLEUS CLASS
  -----------------------
  Sperm have a head and a tail, hence can be oriented
  in one axis. This is inherited from the AsymmetricNucleus.
  Mostly empty for now, but analyses involving
  segments such as acrosomes may need common methods.
*/  
package components.nuclei.sperm;

import ij.gui.Roi;

import java.io.File;
import java.io.IOException;

import components.nuclei.AsymmetricNucleus;
import components.nuclei.Nucleus;

public class SpermNucleus extends AsymmetricNucleus {

	private static final long serialVersionUID = 1L;


	public SpermNucleus(Nucleus n) {
		super(n);
	}

	protected SpermNucleus(){
		super();
	}

	public SpermNucleus (Roi roi, File file, int number, double[] position) { // construct from an roi
		super(roi, file, number, position);
	}

	@Override
	public Nucleus duplicate(){
		try {
			SpermNucleus duplicate = new SpermNucleus(this);
			return duplicate;

		} catch (Exception e) {
			return null;
		}
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//		finest("\tReading sperm nucleus");
		in.defaultReadObject();
//		finest("\tRead sperm nucleus");
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//		finest("\tWriting sperm nucleus");
		out.defaultWriteObject();
//		finest("\tWrote sperm nucleus");
	}


}