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
package com.bmskinner.nma.analysis.nucleus;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.DefaultCellCollection;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.logging.Loggable;

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
	 * Create a new collection containing cells that are in all collections. The
	 * rulesets for the first collection will be applied on the assumption that they
	 * are the same in all collections
	 * 
	 * @param collections the collections and source of rulesets for the result
	 * @return
	 */
	public static ICellCollection and(@NonNull List<ICellCollection> collections)
			throws ComponentCreationException {
		ICellCollection c0 = collections.get(0);

		ICellCollection result = new DefaultCellCollection(c0.getRuleSetCollection(),
				"AND operation", UUID.randomUUID());
		for (ICell c : c0) {
			if (collections.stream().allMatch(col -> col.contains(c)))
				result.add(c.duplicate());
		}
		return result;
	}

	/**
	 * Create a new collection containing cells that are in any collection. The
	 * rulesets for the first collection will be applied on the assumption that they
	 * are the same in both collections
	 * 
	 * @param collections the collections and source of rulesets for the result
	 * @return
	 * @throws ComponentCreationException
	 */
	public static ICellCollection or(@NonNull List<ICellCollection> collections)
			throws ComponentCreationException {
		ICellCollection c0 = collections.get(0);
		ICellCollection result = new DefaultCellCollection(c0.getRuleSetCollection(),
				"OR operation", UUID.randomUUID());

		// Add cells from each source dataset
		for (ICellCollection d : collections) {
			for (ICell c : d) {
				if (!result.contains(c))
					result.add(c.duplicate());
			}
		}
		return result;
	}

	/**
	 * Create a new collection containing cells in the first collection that are not
	 * in any of the other collections. The rulesets for the first collection will
	 * be applied on the assumption that they are the same in all collections.
	 * 
	 * @param collections the collections and source of rulesets for the result
	 * @return
	 * @throws ComponentCreationException
	 */
	public static ICellCollection not(@NonNull List<ICellCollection> collections)
			throws ComponentCreationException {
		ICellCollection c0 = collections.get(0);
		ICellCollection result = new DefaultCellCollection(c0.getRuleSetCollection(),
				"NOT operation", UUID.randomUUID());

		List<ICellCollection> otherCollections = collections.subList(1, collections.size());

		// Add cells only if not in any of the other datasets
		for (ICell c : c0) {
			if (otherCollections.stream().noneMatch(col -> col.contains(c)))
				result.add(c.duplicate());
		}

		return result;
	}

	/**
	 * Create a new collection containing cells that are not shared between the
	 * collections (exclusive or). The rulesets for the first collection will be
	 * applied on the assumption that they are the same in both collections
	 * 
	 * @param collections the collections and source of rulesets for the result
	 * @return
	 * @throws ComponentCreationException
	 */
	public static ICellCollection xor(@NonNull List<ICellCollection> collections)
			throws ComponentCreationException {
		ICellCollection c0 = collections.get(0);
		ICellCollection result = new DefaultCellCollection(c0.getRuleSetCollection(),
				"XOR operation", UUID.randomUUID());

		for (ICellCollection d : collections) {

			// Get the collections other than the current collection
			List<ICellCollection> otherCollections = collections.stream().filter(c -> !c.equals(d))
					.toList();

			// Add cells that are not in any of the other datasets
			for (ICell c : d) {
				if (otherCollections.stream().noneMatch(col -> col.contains(c)))
					result.add(c.duplicate());
			}
		}
		return result;
	}

	/**
	 * Filter the collection using the given cell predicate
	 * 
	 * @param collection the collection to filter
	 * @param pred       the predicate determining which cells are valid
	 * @return the filtered collection
	 * @throws CollectionFilteringException
	 */
	public static ICellCollection filter(ICellCollection collection, Predicate<ICell> pred)
			throws CollectionFilteringException {
		String newName = "Filtered_" + pred.toString();

		ICellCollection subCollection = new DefaultCellCollection(collection, newName);

		List<ICell> list = collection.parallelStream()
				.filter(pred)
				.collect(Collectors.toList());

		subCollection.addAll(list);

		if (!subCollection.hasCells()) {
			LOGGER.warning("No cells passed filter");
			throw new CollectionFilteringException("No cells passed filter");
		}

		try {
			subCollection.getProfileCollection().calculateProfiles();
			collection.getProfileManager().copySegmentsAndLandmarksTo(subCollection);
			collection.getSignalManager().copySignalGroupsTo(subCollection);

		} catch (MissingDataException | SegmentUpdateException e) {
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
