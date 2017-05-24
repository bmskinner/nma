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
import java.util.function.Predicate;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * A filterer that filters cell collections to remove obvious outliers
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


		// Make the predicate for the stats
		// Fails if outside the given range
		Predicate<ICell> pred = new Predicate<ICell>() {
			@Override
			public boolean test(ICell t) {

				for(Nucleus n : t.getNuclei()){

					for(PlottableStatistic stat : stats){
						double med;
						try {
							med = collection.getMedianStatistic(stat, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
						} catch (Exception e) {
							stack("Cannot get median stat", e);
							return false;
						}

						double max = med * delta;
						double min = med / delta;

						double value = n.getStatistic(stat);

						if(value > max || value < min ){
							return false;
						}
					}

				}
				return true;
			}

		};


		// Test each cell for the predicate
		Iterator<ICell> it  = collection.getCells().iterator();

		while(it.hasNext()){
			ICell c = it.next();

			if( ! pred.test(c)){

				if(failCollection!=null){
					failCollection.addCell(new DefaultCell(c));
				}
				collection.removeCell(c);
				it.remove();
				
			}

		}


		fine("Remaining: "+collection.size()+" nuclei");

	}
	
////	@Override 
//	public ICellCollection filter(ICellCollection collection, Predicate pred){
//		
//		return collection.filter(pred);
//	}
	
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
		
		try {
			
			//TODO - this fails on converted collections from (at least) 1.13.0 with no profiles in aggregate
			collection.getProfileManager().copyCollectionOffsets(filtered);
			collection.getSignalManager().copySignalGroups(filtered);
			
		} catch (ProfileException e) {
			warn("Error copying collection offsets");
			stack("Error in offsetting", e);
		}
		
		finer("Filter on "+stat+" gave "+filtered.size()+" cells");
		return filtered;
	}

	
}
