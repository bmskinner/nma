/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * A filterer that filters cell collections on measured values
 * 
 * @author bms41
 * @since 1.13.5
 *
 */
public class CellCollectionFilterer extends Filterer<ICellCollection, ICell> {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);
	
    @Override
    public void removeOutliers(ICellCollection collection, ICellCollection failCollection, double delta)
            throws CollectionFilteringException {

        List<PlottableStatistic> stats = new ArrayList<>();
        stats.add(PlottableStatistic.AREA);
        stats.add(PlottableStatistic.PERIMETER);
        stats.add(PlottableStatistic.PATH_LENGTH);
        stats.add(PlottableStatistic.MAX_FERET);

        // Make the predicate for the stats
        // Fails if outside the given range
        Predicate<ICell> pred = (t) -> {
        	for (Nucleus n : t.getNuclei()) {
        		for (PlottableStatistic stat : stats) {
        			double med;
        			try {
        				med = collection.getMedian(stat, CellularComponent.NUCLEUS,
        						MeasurementScale.PIXELS);
        			} catch (Exception e) {
        				LOGGER.log(Loggable.STACK, "Cannot get median stat", e);
        				return false;
        			}
        			double max = med * delta;
        			double min = med / delta;

        			double value = n.getStatistic(stat);

        			if (value > max || value < min)
        				return false;
        		}
        	}
        	return true;
        };

        // Test each cell for the predicate
        Iterator<ICell> it = collection.getCells().iterator();
        while (it.hasNext()) {
            ICell c = it.next();
            if (!pred.test(c)) {
                if (failCollection != null)
                    failCollection.addCell(new DefaultCell(c));
                it.remove();
            }
        }
        LOGGER.fine("Remaining: " + collection.size() + " nuclei");
    }

    @Override
    public ICellCollection filter(ICellCollection collection, Predicate<ICell> pred) throws CollectionFilteringException {
    	 ICellCollection filtered = collection.filter(pred);

         if (filtered == null || !filtered.hasCells())
             throw new CollectionFilteringException("No collection returned");
         try {
             collection.getProfileManager().copyCollectionOffsets(filtered);
             collection.getSignalManager().copySignalGroups(filtered);

         } catch (ProfileException e) {
             LOGGER.warning("Error copying collection offsets");
             LOGGER.log(Loggable.STACK, "Error in offsetting", e);
         }
         return filtered;
    }

    @Override
    public ICellCollection filter(ICellCollection collection, PlottableStatistic stat, double lower, double upper, MeasurementScale scale)
            throws CollectionFilteringException {
    	return filter(collection, CellularComponent.NUCLEUS, stat, lower, upper, scale);
    }
    
    @Override
    public ICellCollection filter(ICellCollection collection, String component, PlottableStatistic stat, double lower, double upper, MeasurementScale scale)
            throws CollectionFilteringException {
    	
    	FilteringOptions op = new DefaultFilteringOptions();
    	op.addMinimumThreshold(stat, component, scale, lower);
    	op.addMaximumThreshold(stat, component, scale, upper);
    	
    	Predicate<ICell> pred = op.getPredicate(collection);         
        return filter(collection, pred);
    }
}
