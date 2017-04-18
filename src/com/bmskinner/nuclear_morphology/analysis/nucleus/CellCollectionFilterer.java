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

package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * A filterer that filters cell collections
 * @author bms41
 * @since 1.13.5
 *
 */
public class CellCollectionFilterer extends Filterer<ICellCollection>{

	@Override
	public void removeOutliers(ICellCollection collection, ICellCollection failCollection, double delta) throws CollectionFilteringException {

	List<PlottableStatistic> stats = new ArrayList<>();
	stats.add(PlottableStatistic.AREA);
	stats.add(PlottableStatistic.PERIMETER);
	stats.add(PlottableStatistic.PATH_LENGTH);
	stats.add(PlottableStatistic.MAX_FERET);
	
//	double medianArrayLength = collection.getMedianArrayLength();
		
	try {
	Iterator<ICell> it  = collection.getCells().iterator();
	
	while(it.hasNext()){
		ICell c = it.next();

		Nucleus n = c.getNucleus();

		boolean reject = false;
		for(PlottableStatistic stat : stats){
			double med = collection.getMedianStatistic(stat, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
			double max = med * delta;
			double min = med / delta;

			double value = n.getStatistic(stat);

			if(value > max || value < min ){
				reject = true;
			}
		}
		
//		if(n.getBorderLength() > medianArrayLength * delta
//				|| n.getBorderLength() < medianArrayLength / delta ){
//			reject = true;
//	    }
		
		if(reject){
			if(failCollection!=null){
				failCollection.addCell(new DefaultCell(c));
			}
			it.remove();
		}

	}
	
	} catch(Exception e){
		stack(e);
		throw new CollectionFilteringException("Error getting median stats", e);
	}

    fine("Remaining: "+collection.size()+" nuclei");
    
  }
	
	/**
	 * Filter the given collection to retain cells in which the given statistic is within the lower and
	 * upper bounds inclusive.
	 * @param collection the collection to filter
	 * @param stat the statistic to filter on
	 * @param lower the lower bound
	 * @param upper the upper bound
	 * @return a new cell collection with copies of the original cells
	 * @throws CollectionFilteringException
	 */
	@Override
	public ICellCollection filter(ICellCollection collection, PlottableStatistic stat, double lower, double upper)
			throws CollectionFilteringException {

		
		finer("Filtering on "+stat);
		ICellCollection filtered = collection.filterCollection(stat,
						MeasurementScale.PIXELS, 
						lower, upper);
		
		if(filtered == null){
			throw new CollectionFilteringException("No collection returned");
		}
		
		if( ! filtered.hasCells()){
			throw new CollectionFilteringException("No cells returned for "+stat);
		}
		
		finer("Filter on "+stat+" gave "+filtered.size()+" cells");
		return filtered;
	}

	
}
