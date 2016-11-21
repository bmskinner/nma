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

package components.nuclear;

import java.io.Serializable;
import java.util.List;

import logging.Loggable;
import analysis.signals.ShellCounter.CountType;

/**
 * The interface for shell analysis results
 * @author bms41
 *
 */
public interface IShellResult extends Serializable, Loggable {

	/**
	 * Get the pixel intensity counts for the given type 
	 * @param type the counting type
	 * @return a list of counts per shell
	 */
	List<Integer> getPixelCounts(CountType type);

	/**
	 * Get the mean pixel proportions for the given type 
	 * @param type the counting type
	 * @return a list of proportions of signal per shell
	 */
	List<Double> getRawMeans(CountType type);

	/**
	 * Get the normalised pixel proportions for the given type 
	 * @param type the counting type
	 * @return a list of proportions of signal per shell
	 */
	List<Double> getNormalisedMeans(CountType type);

	/**
	 * Get the standard error of pixel proportions for the given type 
	 * @param type the counting type
	 * @return a list of proportions of signal per shell
	 */
	List<Double> getRawStandardErrors(CountType type);
	
	/**
	 * Get the standard error of pixel proportions for the given type 
	 * @param type the counting type
	 * @return a list of proportions of signal per shell
	 */
	List<Double> getNormalisedStandardErrors(CountType type);

	/**
	 * Get the raw chi square test value for the given type 
	 * @param type the counting type
	 * @return the result of a chi square test against equal proportions per shell
	 */
	double getRawChiSquare(CountType type);
	
	/**
	 * Get the normalised chi square test value for the given type 
	 * @param type the counting type
	 * @return the result of a chi square test against equal proportions per shell
	 */
	double getNormalisedChiSquare(CountType type);

	/**
	 * Get the raw chi square p-value for the given type 
	 * @param type the counting type
	 * @return the result of a chi square test against equal proportions per shell
	 */
	double getRawPValue(CountType type);
	
	/**
	 * Get the normalised chi square p-value for the given type 
	 * @param type the counting type
	 * @return the result of a chi square test against equal proportions per shell
	 */
	double getNormalisedPValue(CountType type);

	
	/**
	 * Get the number of shells in the shell result
	 * @return the shell count
	 */
	int getNumberOfShells();

	

}