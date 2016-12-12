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
package com.bmskinner.nuclear_morphology.stats;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;

/**
 * This interface is implemented by the enums describing statistical measures
 * that can be plotted in charts.
 * @author ben
 *
 */
public interface PlottableStatistic {

	/**
	 * Get the string representation (name) of the statistic. 
	 * @return
	 */
	public String toString();

	/**
	 * Test if the statistic has units
	 * @return
	 */
	public boolean isDimensionless();

	/**
	 * Get the dimension of the statistic (area, length, angle, none)
	 * @return
	 */
	public StatisticDimension getDimension();

	/**
	 * Get the label (name and units) for the stat
	 * @return
	 */
	public String label(MeasurementScale scale);


	/**
	 * Convert the input value (assumed to be pixels) using the given
	 * factor ( Nucleus.getScale() ) into the appropriate scale
	 * @param value the pixel measure
	 * @param factor the conversion factor to microns
	 * @param scale the desired scale
	 * @return
	 */
	public double convert(double value, double factor, MeasurementScale scale);
	
	/**
	 * Convert the length in pixels into a length in microns.
	 * Assumes that the scale is in pixels per micron
	 * @param pixels the number of pixels
	 * @param scale the size of a pixel in microns
	 * @return
	 */
	public static double micronLength(double pixels, double scale){
		double microns = pixels / scale;
		return microns;
	}

	/**
	 * Convert the area in pixels into an area in microns.
	 * Assumes that the scale is in pixels per micron
	 * @param pixels the number of pixels
	 * @param scale the size of a pixel in microns
	 * @return
	 */
	public static double micronArea(double pixels, double scale){
		double microns = pixels / (scale*scale);
		return microns;
	}	

	/**
	 * Get the appropriate units label for the statistic, based on its dimension.
	 * Eg. square units, units or nothing
	 * @param scale
	 * @return
	 */
	public String units(MeasurementScale scale);
	
	/**
	 * Calls the values() method of the underlying enum, allowing
	 * iteration access via the interface
	 * @return
	 */
	public PlottableStatistic[] getValues();
	
	public boolean equals(Object o);
	
	public int hashCode();
}

