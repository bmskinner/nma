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

package com.bmskinner.nuclear_morphology.components;

import java.util.function.Predicate;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * Methods for filtering,  dividing and combining collections
 * @author bms41
 * @since 1.13.4
 */
public interface Filterable {
	
	/**
	 * Return a collection of cells present in both collections
	 * @param other the other collection
	 * @return
	 */
	ICellCollection and(ICellCollection collection);
	
	/**
	 * Return a collection of cells present this collection but not the other
	 * @param other the other collection
	 * @return
	 */
	ICellCollection not(ICellCollection collection);
	
	/**
	 * Return a collection of cells present this collection or the other but not both
	 * @param other the other collection
	 * @return a new collection with cells not shared between datasets
	 */
	ICellCollection xor(ICellCollection collection);
	
	/**
	 * Return a collection containing cell in either dataset. 
	 * Cells in both datasets are not duplicated. 
	 * @param collection the comparison dataset
	 * @return a new collection with cells from either dataset
	 */
	ICellCollection or(ICellCollection collection);
	
	/**
	 * Filter the collection on the given statistic
	 * @param stat the stat to filter on
	 * @param scale the measurement scale of the bounds
	 * @param lower the lower bound for the stat
	 * @param upper the upper bound for the stat
	 * @return a new collection with only cells matching the filter
	 */
	ICellCollection filterCollection(PlottableStatistic stat,
			MeasurementScale scale, double lower, double upper);
	
	/**
	 * Filter the collection for cells that
	 * match the given predicate
	 * @param predicate
	 * @return
	 */
	ICellCollection filter(Predicate<ICell> predicate);
	
}
