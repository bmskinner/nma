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

import java.util.function.Predicate;

import com.bmskinner.nuclear_morphology.components.Filterable;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * The filterer filters any collection implementing the Filterable interface.
 * 
 * @author bms41
 * @param <E> the collection to be filtered
 * @since 1.13.5
 */
public abstract class Filterer<E extends Filterable, K>{

    public static final int FAILURE_THRESHOLD = 1;
    public static final int FAILURE_FERET     = 2;
    public static final int FAILURE_ARRAY     = 4;
    public static final int FAILURE_AREA      = 8;
    public static final int FAILURE_PERIM     = 16;
    public static final int FAILURE_OTHER     = 32;
    public static final int FAILURE_SIGNALS   = 64;

    /**
     * Create a collection without elements more than <i>delta</i> proportion
     * away from the median of the element filtering statistics. Formally,
     * includes all elements of the collection for which <i>statistic</i> <=
     * (median_statistic * delta) && <i>statistic</i> >= (median_statistic /
     * delta)
     * 
     * @param collection the collection to be filtered
     * @param failCollection  the collection to store failed nuclei. Can be null.
     * @param delta the variability from the median allowed
     * @return
     */
    public abstract void removeOutliers(E collection, E failCollection, double delta)
            throws CollectionFilteringException;

    /**
     * Filter the given collection to retain nuclei in which the given
     * statistic is within the lower and upper bounds inclusive.
     * 
     * @param collection the collection to filter
     * @param stat the statistic to filter on
     * @param lower the lower bound
     * @param upper the upper bound
     * @return a new cell collection with copies of the original cells
     * @throws CollectionFilteringException
     */
    public abstract E filter(E collection, PlottableStatistic stat, double lower, double upper, MeasurementScale scale)
            throws CollectionFilteringException;
    
    /**
     * Filter the given collection to retain nuclei in which the given
     * statistic is within the lower and upper bounds inclusive.
     * 
     * @param collection the collection to filter
     * @param component the cellular component to be filtered
     * @param stat the statistic to filter on
     * @param lower the lower bound
     * @param upper the upper bound
     * @return a new cell collection with copies of the original cells
     * @throws CollectionFilteringException
     */
    public abstract E filter(E collection, String component, PlottableStatistic stat, double lower, double upper, MeasurementScale scale)
            throws CollectionFilteringException;
    
    /**
     * Filter the collection with the given predicate
     * @param collection
     * @param pred
     * @return
     * @throws CollectionFilteringException 
     */
    public abstract E filter(E collection, Predicate<K> pred) throws CollectionFilteringException;

    /**
     * Thrown when a cell collection cannot be filtered
     * 
     * @author bms41
     * @since 1.13.3
     *
     */
    public static class CollectionFilteringException extends Exception {
        private static final long serialVersionUID = 1L;

        public CollectionFilteringException() {
            super();
        }

        public CollectionFilteringException(String message) {
            super(message);
        }

        public CollectionFilteringException(String message, Throwable cause) {
            super(message, cause);
        }

        public CollectionFilteringException(Throwable cause) {
            super(cause);
        }

    }

}
