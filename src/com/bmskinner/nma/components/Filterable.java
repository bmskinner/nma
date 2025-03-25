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
package com.bmskinner.nma.components;

/**
 * Methods for filtering, dividing and combining collections
 * 
 * @author Ben Skinner
 * @since 1.13.4
 */
public interface Filterable {

    /**
     * Filter the collection on the given statistic
     * 
     * @param stat the stat to filter on
     * @param scale the measurement scale of the bounds
     * @param lower the lower bound for the stat
     * @param upper the upper bound for the stat
     * @return a new collection with only cells matching the filter
     */
//    ICellCollection filterCollection(@NonNull Measurement stat, MeasurementScale scale, double lower, double upper);

    /**
     * Filter the collection for cells that match the given predicate
     * 
     * @param predicate
     * @return
     */
//    ICellCollection filter(@NonNull Predicate<ICell> predicate);

}
