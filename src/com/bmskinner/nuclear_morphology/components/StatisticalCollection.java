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
package com.bmskinner.nuclear_morphology.components;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;

/**
 * Describes the methods for retrieving aggregate stats from collections of
 * objects implementing the Statistical interface
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public interface StatisticalCollection {

    /**
     * Force the given statistic to be recalculated
     * 
     * @param stat the statistic to recalculate
     * @param component the cellular component to fetch from
     */
    void clear(@NonNull Measurement stat, @NonNull String component);
    
    /**
     * Force the given statistic to be recalculated
     * 
     * @param stat the statistic to recalculate
     * @param component the cellular component to fetch from
     * @param id the sub-component id 
     */
    void clear(@NonNull Measurement stat, @NonNull String component, @NonNull UUID id);

    /**
     * Force the statistics at the given scale to be recalculated
     * 
     * @param scale the scale to recalculate
     */
    void clear(MeasurementScale scale);

    /**
     * Get the median value of the given stat in the collection
     * 
     * @param stat the statistic to fetch
     * @param component the cellular component to fetch from
     * @param scale the scale to convert values to
     * @return the median statistic value
     * @throws Exception
     */
    double getMedian(@NonNull Measurement stat, String component, MeasurementScale scale);

    /**
     * Get the median stat for a value with an ID - i.e. a nuclear signal or a
     * segment
     * 
     * @param stat
     * @param scale
     * @param id
     * @return
     * @throws Exception
     */
    double getMedian(@NonNull Measurement stat, String component, MeasurementScale scale, UUID id);
    
    
    /**
     * Get the minimum value of the given stat in the collection
     * @param stat
     * @param component
     * @param scale
     * @param id
     * @return the minumum or Statistical.ERROR_CALCULATING_STAT
     */
    double getMin(@NonNull Measurement stat, String component, MeasurementScale scale);
    
    /**
     * Get the minimum value of the given stat in the collection
     * @param stat
     * @param component
     * @param scale
     * @param id
     * @return the minumum or Statistical.ERROR_CALCULATING_STAT
     */
    double getMin(@NonNull Measurement stat, String component, MeasurementScale scale, UUID id);
    
    /**
     * Get the maximum value of the given stat in the collection
     * @param stat
     * @param component
     * @param scale
     * @param id
     * @return the maxumum or Statistical.ERROR_CALCULATING_STAT
     */
    double getMax(@NonNull Measurement stat, String component, MeasurementScale scale);
   
    /**
     * Get the maximum value of the given stat in the collection
     * @param stat
     * @param component
     * @param scale
     * @param id
     * @return the maxumum or Statistical.ERROR_CALCULATING_STAT
     */
    double getMax(@NonNull Measurement stat, String component, MeasurementScale scale, UUID id);
    
    

    /**
     * Get the raw values for the given stat for each object in the collection
     * 
     * @param stat the statistic to fetch
     * @param component the cellular component to fetch from
     * @param scale the scale to convert values to
     * @return the values in the collection
     */
    double[] getRawValues(@NonNull Measurement stat, String component, MeasurementScale scale);

    /**
     * Get the raw values for the given stat for each object in the collectionw
     * ith an ID - i.e. a nuclear signal or a segment
     * 
     * @param stat the statistic to fetch
     * @param component the cellular component to fetch from
     * @param scale the scale to convert values to
     * @param id the id of the compenent to fetch
     * @return the values in the collection
     */
    double[] getRawValues(@NonNull Measurement stat, String component, MeasurementScale scale, UUID id);
    
    
    
}
