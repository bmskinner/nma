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
		return dimension.equals(StatisticDimension.DIMENSIONLESS);
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
		return PlottableStatistic.convert(value, factor, scale, dimension);
	}

	public String units(MeasurementScale scale){
		return PlottableStatistic.units(scale, dimension);
	}

	public PlottableStatistic[] getValues(){
		return SegmentStatistic.values();
	}

}
