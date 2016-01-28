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

import stats.StatisticDimension;

public enum ProfileType { 
	  REGULAR (       "Angle profile"          , "Angle"              , StatisticDimension.ANGLE), 
	  FRANKEN (       "Franken profile"        , "Angle"              , StatisticDimension.ANGLE),
	  DISTANCE(       "Distance profile"       , "Distance across CoM", StatisticDimension.LENGTH),
	  SINGLE_DISTANCE("Single distance profile", "Distance from CoM"  , StatisticDimension.LENGTH);
	  
	  private String name;
	  private String label;
	  private StatisticDimension dimension;
	  	  
	  ProfileType(String name, String label, StatisticDimension dimension){
		  this.name = name;
		  this.label = label;
		  this.dimension = dimension;
	  }
	  
	  public String toString(){
		  return this.name;
	  }
	  
	  public String getLabel(){
		  return this.label;
	  }
	  
	  public StatisticDimension getDimension(){
		  return this.dimension;
	  }
  }