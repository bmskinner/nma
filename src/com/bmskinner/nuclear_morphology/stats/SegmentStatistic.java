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

public enum SegmentStatistic implements PlottableStatistic {

	LENGTH      ("Length"      , StatisticDimension.LENGTH),
	DISPLACEMENT("Displacement", StatisticDimension.ANGLE);

	private String name;
	private StatisticDimension dimension;

	SegmentStatistic(String name, StatisticDimension dimension){
		this.name = name;
		this.dimension = dimension;
	}

	public String toString(){
		return this.name;
	}

	public boolean isDimensionless(){
		if(this.dimension.equals(StatisticDimension.DIMENSIONLESS)){
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get the dimension of the statistic (area, length, none)
	 * @return
	 */
	public StatisticDimension getDimension(){
		return this.dimension;
	}

	/**
	 * Get the label (name and units) for the stat
	 * @return
	 */
	public String label(MeasurementScale scale){
		String result = "";
		if(this.isDimensionless()){
			result = this.toString();
		} else {
			result = this.toString() +" ("+ this.units(scale) + ")";
		}
		return result;
	}

	/**
	 * Convert the input value (assumed to be pixels) using the given
	 * factor ( Nucleus.getScale() ) into the appropriate scale
	 * @param value the pixel measure
	 * @param factor the conversion factor to microns
	 * @param scale the desired scale
	 * @return
	 */
	public double convert(double value, double factor, MeasurementScale scale){
		double result = value;

		switch(scale){
		case MICRONS:
		{
			switch(this.dimension){
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

	public String units(MeasurementScale scale){
		String result = "";
		switch(dimension){

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

	public PlottableStatistic[] getValues(){
		return SegmentStatistic.values();
	}

}
