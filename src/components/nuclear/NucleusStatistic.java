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
package components.nuclear;

import components.generic.MeasurementScale;

/**
   * These are the values that we can make boxplots from
   *
   */
  public enum NucleusStatistic {
	  AREA ("Area", StatisticDimension.AREA),
	  PERIMETER("Perimeter", StatisticDimension.LENGTH),
	  MAX_FERET("Max feret", StatisticDimension.LENGTH),
	  MIN_DIAMETER("Min diameter", StatisticDimension.LENGTH),
	  ASPECT("Aspect", StatisticDimension.DIMENSIONLESS),
	  CIRCULARITY("Circularity", StatisticDimension.DIMENSIONLESS),
	  VARIABILITY("Variability", StatisticDimension.DIMENSIONLESS), 
	  BOUNDING_HEIGHT ("Bounding height", StatisticDimension.LENGTH), 
	  BOUNDING_WIDTH ("Bounding width", StatisticDimension.LENGTH);

	  private String name;
	  private StatisticDimension dimension;

	  NucleusStatistic(String name, StatisticDimension dimension){
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
			  default:
				  break;

		  }
		  return result;
	  }
	  
	  public enum StatisticDimension {
		  
		  AREA, LENGTH, DIMENSIONLESS
	  }
  }