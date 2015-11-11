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
package components.generic;

import components.nuclear.NucleusStatistic;

public enum MeasurementScale {
	
	PIXELS ("Pixels"),
	MICRONS ("Microns");
	
	private String name;
	
	MeasurementScale(String name){
		this.name = name;
	}
	
	public String toString(){
		return this.name;
	}

	/**
	 * Get the appropriate chart y-label for the
	 * given statistic 
	 * @param stat
	 * @return
	 */
	public String yLabel(NucleusStatistic stat){
		String result = null;

		switch(stat){

		case AREA: 
			result = "Square "+name;
			break;
		case PERIMETER: 
			result = name;
			break;
		case MAX_FERET: 
			result = name;
			break;
		case MIN_DIAMETER: 
			result = name;
			break;
		case ASPECT: 
			result = "Aspect ratio (feret / min diameter)";
			break;
		case CIRCULARITY: 
			result = "Circularity";
			break;
		case VARIABILITY: 
			result = "Degrees per perimeter unit";
			break;
		case BOUNDING_HEIGHT:
			result = name;
			break;
		case BOUNDING_WIDTH:
			result = name;
			break;
		default:
			result = name;
			break;
		}

		return result;
	}

}