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
package com.bmskinner.nuclear_morphology.components.stats;

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
	String toString();

	/**
	 * Test if the statistic has units
	 * @return
	 */
	boolean isDimensionless();

	/**
	 * Get the dimension of the statistic (area, length, angle, none)
	 * @return
	 */
	StatisticDimension getDimension();

	/**
	 * Get the label (name and units) for the stat
	 * @return
	 */
	String label(MeasurementScale scale);


	/**
	 * Convert the input value (assumed to be pixels) using the given
	 * factor ( Nucleus.getScale() ) into the appropriate scale
	 * @param value the pixel measure
	 * @param factor the conversion factor to microns
	 * @param scale the desired scale
	 * @return
	 */
	double convert(double value, double factor, MeasurementScale scale);
	
	/**
	 * Get the appropriate units label for the statistic, based on its dimension.
	 * Eg. square units, units or nothing
	 * @param scale
	 * @return
	 */
	String units(MeasurementScale scale);
	
	/**
	 * Convert the length in pixels into a length in microns.
	 * Assumes that the scale is in pixels per micron
	 * @param pixels the number of pixels
	 * @param scale the size of a pixel in microns
	 * @return
	 */
	static double micronLength(double pixels, double scale){
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
	static double micronArea(double pixels, double scale){
		double microns = pixels / (scale*scale);
		return microns;
	}	
	
	/**
	   * Convert the input value (assumed to be pixels) using the given
	   * factor ( CellularComponent.getScale() ) into the appropriate scale
	   * @param value the pixel measure
	   * @param factor the conversion factor to microns
	   * @param scale the desired scale
	   * @param dim the dimension of the statistic
	   * @return the converted value
	   */
	static double convert(double value, double factor, MeasurementScale scale, StatisticDimension dim){
		double result = value;

		switch(scale){
		case MICRONS:
		{
			switch(dim){
			case AREA:
				result = PlottableStatistic.micronArea(value, factor);
				break;
			case DIMENSIONLESS:
				break;
			case LENGTH:
				result = PlottableStatistic.micronLength(value, factor);
				break;
			case ANGLE:
				break;
			default:
				break;

			}
		}
		break;
		case PIXELS:
			break;
		default:
			break;
		}
		return result;
	}

	  /**
	   * Create a units label for the given scale and dimension
	   * @param scale
	   * @param dim
	   * @return
	   */
	  static String units(MeasurementScale scale, StatisticDimension dim){
		  String result = "";
		  switch(dim){

		  case AREA:
			  result = "square "+scale.toString().toLowerCase();
			  break;
		  case DIMENSIONLESS:
			  break;
		  case LENGTH:
			  result = scale.toString().toLowerCase();
			  break;
		  case ANGLE:
			  result = "degrees";
			  break;
		  default:
			  break;

		  }
		  return result;
	  }
	  	
	/**
	 * Calls the values() method of the underlying enum, allowing
	 * iteration access via the interface
	 * @return
	 */
	PlottableStatistic[] getValues();
}

