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
package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

@SuppressWarnings("serial")
public class NucleusStatisticFetchingTask extends AbstractStatisticFetchingTask {
	
	public NucleusStatisticFetchingTask(Nucleus[] nuclei, PlottableStatistic stat, MeasurementScale scale){
		this(nuclei, stat, scale, 0,nuclei.length );
	}

	protected NucleusStatisticFetchingTask(Nucleus[] nuclei, PlottableStatistic stat, MeasurementScale scale, int low, int high) {
		super(nuclei, stat, scale, low, high );
	}
	
	@Override
	protected double[] compute() {
		double[] result = new double[0];
		
		 if (high - low < THRESHOLD)
				try {
					result = getStatistics();
					return result;
				} catch (Exception e) {
					 warn("Error fetching statistic "+stat);
					 log(Level.FINE, "Error fetching statistic "+stat, e);
				}
		     else {
		    	 int mid = (low + high) >>> 1; // Unsigned bit shift (0 to leftmost position)

		    	 List<NucleusStatisticFetchingTask> tasks = new ArrayList<NucleusStatisticFetchingTask>();
		    	 
		    	 try {
		    		 NucleusStatisticFetchingTask task1 = new NucleusStatisticFetchingTask(nuclei, stat, scale, low, mid);


		    		 NucleusStatisticFetchingTask task2 = new NucleusStatisticFetchingTask(nuclei, stat, scale, mid, high);

		    		 tasks.add(task1);
		    		 tasks.add(task2);

		    		 
		    		 ForkJoinTask.invokeAll(tasks);
		    		 
		    		 result = concat(task1.join(), task2.join());
		    		 return result;
		    		 
		    	 } catch (Exception e) {
		    		 warn("Error fetching statistic "+stat);
		    		 log(Level.FINE, "Error fetching statistic "+stat, e);
		    	 }

		     }
		 return result;
	}
	

	/**
	   * Get the stats of the nuclei in this collection as
	   * an array
	   * @return
	 * @throws Exception 
	   */
	  private double[] getStatistics() throws Exception{
		  double[] result = new double[high-low];

		  finest("Fetching statistic "+stat+" for "+result.length+" nuclei");
		 
		  for(int i=0, j=low; j<high; i++, j++){
			  result[i] = nuclei[j].getStatistic(stat, scale);
		  }

		  return result;

	  }
	
	

}
