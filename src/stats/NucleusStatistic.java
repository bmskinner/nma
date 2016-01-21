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
package stats;

import java.util.ArrayList;
import java.util.List;

import utility.Utils;
import components.generic.MeasurementScale;
import components.nuclear.NucleusType;

/**
   * These are the values that we can make boxplots from
   *
   */
  /**
 * @author ben
 *
 */
public enum NucleusStatistic implements PlottableStatistic {
	  AREA ("Area", StatisticDimension.AREA, new NucleusType[]{NucleusType.ROUND}),
	  PERIMETER("Perimeter", StatisticDimension.LENGTH, new NucleusType[]{NucleusType.ROUND}),
	  MAX_FERET("Max feret", StatisticDimension.LENGTH, new NucleusType[]{NucleusType.ROUND}),
	  MIN_DIAMETER("Min diameter", StatisticDimension.LENGTH, new NucleusType[]{NucleusType.ROUND}),
	  ASPECT("Aspect", StatisticDimension.DIMENSIONLESS, new NucleusType[]{NucleusType.ROUND}),
	  CIRCULARITY("Circularity", StatisticDimension.DIMENSIONLESS, new NucleusType[]{NucleusType.ROUND}),
	  VARIABILITY("Variability", StatisticDimension.DIMENSIONLESS, new NucleusType[]{NucleusType.ROUND}), 
	  BOUNDING_HEIGHT ("Bounding height", StatisticDimension.LENGTH, new NucleusType[]{NucleusType.ROUND}), 
	  BOUNDING_WIDTH ("Bounding width", StatisticDimension.LENGTH, new NucleusType[]{NucleusType.ROUND}),
	  OP_RP_ANGLE ("Angle between reference points", StatisticDimension.ANGLE, new NucleusType[]{NucleusType.ROUND}),
	  HOOK_LENGTH ("Length of hook", StatisticDimension.LENGTH, new NucleusType[]{NucleusType.RODENT_SPERM}),
	  BODY_WIDTH ("Width of body", StatisticDimension.LENGTH, new NucleusType[]{NucleusType.RODENT_SPERM});

	  private String name;
	  private StatisticDimension dimension;
	  private NucleusType[] applicableSuperType;

	  NucleusStatistic(String name, StatisticDimension dimension, NucleusType[] type){
		  this.name = name;
		  this.dimension = dimension;
		  this.applicableSuperType = type;
	  }

	  public String toString(){
		  return this.name;
	  }
	  
	  /**
	   * Get the types of nucleus for which the statistic applies
	 * @return an array of types
	 */
	public NucleusType[] getApplicableTypes(){
		  return this.applicableSuperType;
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
	   * Get the label (name and units) for the statistic
	   * @return
	   */
	  public String label(MeasurementScale scale){
		  String result = "";
		  
		  switch(this.dimension){
			  case DIMENSIONLESS:
				  result = this.toString();
				  break;
			  default:
				  result = this.toString() +" ("+ this.units(scale) + ")";
				  break;
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
		  	case MICRONS: {
				  switch(this.dimension){
					  case AREA:
						  result = Utils.micronArea(value, factor);
						  break;
					  case LENGTH:
						  result = Utils.micronLength(value, factor);
						  break;
					  default:
						  break;
	
				  }
				  break;
			  }
			  
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
			  case LENGTH:
				  result = scale.toString().toLowerCase();
				  break;
			  case ANGLE:
				  result = "degrees";
			  default:
				  break;

		  }
		  return result;
	  }
	  
	  public PlottableStatistic[] getValues(){
		  return NucleusStatistic.values();
	  }

	  /**
	   * Get all the statistics that apply to the given nucleus type
	   * @param type
	   * @return
	   */
	public NucleusStatistic[] values(NucleusType type){
		
		List<NucleusStatistic> result = new ArrayList<NucleusStatistic>();
		  for(NucleusStatistic stat : NucleusStatistic.values()){
			  
			  for(NucleusType t : stat.getApplicableTypes()){
				  if(t.equals(NucleusType.ROUND) && !result.contains(stat)){
					  result.add(stat);
				  }
				  
				  if(t.equals(type) && !result.contains(stat)){
					  result.add(stat);
				  }
			  }
			  
		  }
		  
		  return result.toArray(new NucleusStatistic[0]);
	  }
  }