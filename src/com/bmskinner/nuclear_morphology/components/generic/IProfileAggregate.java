/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.components.generic;

import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;

public interface IProfileAggregate {

	void addValues(IProfile yvalues) throws ProfileException;

	/**
	 * Get the size of the bins covering the range 0-100
	 * @return
	 */
	double getBinSize();

	int length();

	/**
	 * Get the x-axis positions of the centre of each bin.
	 * @return the Profile of positions
	 */
	IProfile getXPositions();

	/**
	 * Get the number of values within each bin as a profile
	 * @return
	 */
	//	public Profile getNumberOfPoints(){
	//		double[] result = new double[length];
	//
	//		for(int i=0;i<length;i++){
	//			double x = xPositions[i];
	//			result[i] = aggregate.containsKey(x) ? aggregate.get(x).size() : 0;
	//		}
	//		return new Profile(result);
	//	}

	IProfile getMedian() throws ProfileException;

	IProfile getQuartile(double quartile) throws ProfileException;

	/**
	 * Get the angle values at the given position in the aggragate
	 * from all nuclei
	 * @param position the position to search. Must be between 0 and the length of the aggregate.
	 * @return an unsorted array of the values at the given position
	 * @throws Exception 
	 */
	double[] getValuesAtPosition(double position);

	List<Double> getXKeyset();

	String toString();

}