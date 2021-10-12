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

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Filters cell collections on measured values
 * 
 * @author bms41
 * @since 1.13.5
 *
 */
public class CellCollectionFilterer {
	
	private static final Logger LOGGER = Logger.getLogger(CellCollectionFilterer.class.getName());	
	
	/**
	 * Create a new collection containing cells that are in both collections.
	 * The rulesets for the first collection will be applied on the assumption
	 * that they are the same in both collections
	 * @param c1 the first collection and source of rulesets for the result
	 * @param c2 the second collection
	 * @return
	 */
	public static ICellCollection and(ICellCollection c1, ICellCollection c2) {			
		ICellCollection result = new DefaultCellCollection(c1.getRuleSetCollection(), "AND operation", UUID.randomUUID());
		
		c1.stream().filter(c->c2.contains(c)).forEach(c->result.add(c.duplicate()));

		return result;
	}
	

	/**
	 * Create a new collection containing cells that are in either collection.
	 * The rulesets for the first collection will be applied on the assumption
	 * that they are the same in both collections
	 * @param c1 the first collection and source of rulesets for the result
	 * @param c2 the second collection
	 * @return
	 */
	public static ICellCollection or(ICellCollection c1, ICellCollection c2) {			
		ICellCollection result = new DefaultCellCollection(c1.getRuleSetCollection(), "OR operation", UUID.randomUUID());
		
		c1.stream().forEach(c->result.add(c.duplicate()));
		c2.stream().forEach(c->result.add(c.duplicate()));
		
		return result;
	}
	
	/**
	 * Create a new collection containing cells in collection one that are 
	 * not in collection two
	 * The rulesets for the first collection will be applied on the assumption
	 * that they are the same in both collections
	 * @param c1 the first collection and source of rulesets for the result
	 * @param c2 the second collection
	 * @return
	 */
	public static ICellCollection not(ICellCollection c1, ICellCollection c2) {			
		ICellCollection result = new DefaultCellCollection(c1.getRuleSetCollection(), "NOT operation", UUID.randomUUID());
		
		c1.stream().filter(c->!c2.contains(c)).forEach(c->result.add(c.duplicate()));
		
		return result;
	}
	
	/**
	 * Create a new collection containing cells that are not shared between
	 * the collection (exclusive or).
	 * The rulesets for the first collection will be applied on the assumption
	 * that they are the same in both collections
	 * @param c1 the first collection and source of rulesets for the result
	 * @param c2 the second collection
	 * @return
	 */
	public static ICellCollection xor(ICellCollection c1, ICellCollection c2) {			
		ICellCollection result = new DefaultCellCollection(c1.getRuleSetCollection(), "XOR operation", UUID.randomUUID());
		
		c1.stream().filter(c->!c2.contains(c)).forEach(c->result.add(c.duplicate()));
		c2.stream().filter(c->!c1.contains(c)).forEach(c->result.add(c.duplicate()));
		
		return result;
	}
	
	/**
	 * Filter the collection using the given filtering options
	 * @param collection the collection to filter
	 * @param options the filtering options to use
	 * @return the filtered collection
	 * @throws CollectionFilteringException
	 */
	public static ICellCollection filter(ICellCollection collection, FilteringOptions options)
			throws CollectionFilteringException {
		return filter(collection, options.getPredicate(collection));
		
	}
	
	/**
	 * Filter the collection using the given cell predicate
	 * @param collection the collection to filter
	 * @param pred the predicate determining which cells are valids
	 * @return the filtered collection
	 * @throws CollectionFilteringException
	 */
	public static ICellCollection filter(ICellCollection collection, Predicate<ICell> pred) throws CollectionFilteringException {
		String newName = "Filtered_" + pred.toString();

		ICellCollection subCollection = new DefaultCellCollection(collection, newName);

		List<ICell> list = collection.parallelStream()
				.filter(pred)
				.collect(Collectors.toList());

		subCollection.addAll(list);

		if (!subCollection.hasCells()) {
			LOGGER.warning("No cells passed filter");
			throw new CollectionFilteringException("No collection returned");
		}

		try {
			subCollection.createProfileCollection();
			collection.getProfileManager().copySegmentsAndLandmarksTo(subCollection);
			collection.getSignalManager().copySignalGroupsTo(subCollection);

		} catch (ProfileException | MissingProfileException e) {
			LOGGER.warning("Error copying collection offsets");
			LOGGER.log(Loggable.STACK, "Error in offsetting", e);
			throw new CollectionFilteringException(e);
		}		
        return subCollection;
	}
		
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
