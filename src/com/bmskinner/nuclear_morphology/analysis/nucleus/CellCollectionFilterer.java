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

import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
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
	
	private final FilteringOptions options;
		
	/**
	 * Create with options describing what filters to apply
	 * @param options
	 */
	public CellCollectionFilterer(FilteringOptions options) {
		this.options = options;
	}
		
	/**
	 * Filter the collection using the internal filtering options
	 * @param collection the collection to filter
	 * @return the filtered collection
	 * @throws CollectionFilteringException
	 */
	public ICellCollection filter(ICellCollection collection)
			throws CollectionFilteringException {
		
		ICellCollection filtered = collection.filter(options.getPredicate(collection));

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
