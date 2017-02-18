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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;

/**
 * This interface is implemented by the enums describing statistical measures
 * that can be plotted in charts.
 * @author ben
 *
 */
public interface PlottableStatistic extends Serializable {
	
	
	// Old nucleus statistics
	static final PlottableStatistic AREA            = new GenericStatistic("Area", StatisticDimension.AREA);
	static final PlottableStatistic PERIMETER       = new GenericStatistic("Perimeter", StatisticDimension.LENGTH);
	static final PlottableStatistic MAX_FERET       = new GenericStatistic("Max feret", StatisticDimension.LENGTH);
	static final PlottableStatistic MIN_DIAMETER    = new GenericStatistic("Min diameter", StatisticDimension.LENGTH);
	static final PlottableStatistic ASPECT          = new GenericStatistic("Aspect", StatisticDimension.DIMENSIONLESS);
	static final PlottableStatistic CIRCULARITY     = new GenericStatistic("Circularity", StatisticDimension.DIMENSIONLESS);
	static final PlottableStatistic VARIABILITY     = new GenericStatistic("Variability", StatisticDimension.DIMENSIONLESS);
	static final PlottableStatistic BOUNDING_HEIGHT = new GenericStatistic("Bounding height", StatisticDimension.LENGTH);
	static final PlottableStatistic BOUNDING_WIDTH  = new GenericStatistic("Bounding width", StatisticDimension.LENGTH);
	static final PlottableStatistic OP_RP_ANGLE     = new GenericStatistic("Angle between reference points", StatisticDimension.ANGLE);
	static final PlottableStatistic HOOK_LENGTH     = new GenericStatistic("Length of hook", StatisticDimension.LENGTH);
	static final PlottableStatistic BODY_WIDTH      = new GenericStatistic("Width of body", StatisticDimension.LENGTH);

	// Old signal statistics minus overlaps with nucleus stats
	static final PlottableStatistic ANGLE           = new GenericStatistic("Angle", StatisticDimension.ANGLE);
	static final PlottableStatistic DISTANCE_FROM_COM      = new GenericStatistic("Distance from CoM", StatisticDimension.LENGTH);
	static final PlottableStatistic FRACT_DISTANCE_FROM_COM      = new GenericStatistic("Fractional distance from CoM", StatisticDimension.DIMENSIONLESS);
	static final PlottableStatistic RADIUS          = new GenericStatistic("Radius", StatisticDimension.LENGTH);

	// Old segment statistics
	static final PlottableStatistic LENGTH          = new GenericStatistic("Length", StatisticDimension.LENGTH);
	static final PlottableStatistic DISPLACEMENT    = new GenericStatistic("Displacement", StatisticDimension.ANGLE);

	
	/**
	 * Get stats for the given component. Use the keys in {@link CellularComponent}
	 * @param component the component to get stats for
	 * @return applicable stats, or null if the component was not recognised
	 */
	static PlottableStatistic[] getStats(String component){
		if(CellularComponent.NUCLEUS.equals(component)){
			return getNucleusStats().toArray(new PlottableStatistic[0]);
		}

		if(CellularComponent.NUCLEAR_SIGNAL.equals(component)){
			return getSignalStats().toArray(new PlottableStatistic[0]);
		}

		if(CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)){
			return getSegmentStats().toArray(new PlottableStatistic[0]);
		}
		return null;
	}
	
	/**
	 * ~Get stats for generic cellular components.
	 * @return
	 */
	static List<PlottableStatistic> getComponentStats(){
		List<PlottableStatistic> list = new ArrayList<PlottableStatistic>(12);
		list.add(AREA);
		list.add(PERIMETER);
		list.add(MAX_FERET);
		list.add(CIRCULARITY);
		return list;
	}
	
	/**
	 * Get stats for round nuclei 
	 * @return
	 */
	static List<PlottableStatistic> getNucleusStats(){
		return getRodentSpermNucleusStats();
	}
	
	/**
	 * Get stats for round nuclei 
	 * @return
	 */
	static List<PlottableStatistic> getNucleusStats(NucleusType type){
		
		switch(type){
		
		case ROUND:{
			return getRoundNucleusStats();
		}
		case NEUTROPHIL:
			return getRoundNucleusStats();
		case PIG_SPERM:
			return getRoundNucleusStats();
		case RODENT_SPERM:
			return getRodentSpermNucleusStats();
		default:
			return getRoundNucleusStats();
		
		}
	}
	
	static List<PlottableStatistic> getRoundNucleusStats(){

		List<PlottableStatistic> list = getComponentStats();
		list.add(MIN_DIAMETER);
		list.add(ASPECT);
		list.add(VARIABILITY);
		list.add(BOUNDING_HEIGHT);
		list.add(BOUNDING_WIDTH);
		return list;
	}
	
	/**
	 * Get stats for rodent sperm nuclei 
	 * @return
	 */
	static List<PlottableStatistic> getRodentSpermNucleusStats(){
		List<PlottableStatistic> list = getRoundNucleusStats();
		list.add(OP_RP_ANGLE);
		list.add(HOOK_LENGTH);
		list.add(BODY_WIDTH);
		return list;
	}
	
	
	
	/**
	 * Get stats for nuclear signals
	 * @return
	 */
	static List<PlottableStatistic> getSignalStats(){
		List<PlottableStatistic> list = getComponentStats();
		list.add(ANGLE);
		list.add(DISTANCE_FROM_COM);
		list.add(FRACT_DISTANCE_FROM_COM);
		list.add(RADIUS);
		return list;
	}
	
	/**
	 * Get stats for nuclear border segments
	 * @return
	 */
	static List<PlottableStatistic> getSegmentStats(){
		List<PlottableStatistic> list = new ArrayList<PlottableStatistic>(2);
		list.add(LENGTH);
		list.add(DISPLACEMENT);
		return list;
	}
	
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
	  	
}

