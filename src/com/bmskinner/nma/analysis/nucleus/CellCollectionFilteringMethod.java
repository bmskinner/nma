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
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.datasets.VirtualDataset;
import com.bmskinner.nma.components.options.FilteringOptions;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.logging.Loggable;

/**
 * Perform filtering of collections on arbitrary predicates
 * 
 * @author ben
 * @since 1.14.0
 *
 */
public class CellCollectionFilteringMethod extends MultipleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger
			.getLogger(CellCollectionFilteringMethod.class.getName());

	private final FilteringOptions options;
	private final Predicate<ICell> pred;
	private final String newCollectionName;

	/**
	 * Create with filtering options relating to measurements of the cells
	 * 
	 * @param datasets the dataset to filter
	 * @param op       the filtering options
	 * @param name     the name for the filtered dataset
	 */
	public CellCollectionFilteringMethod(@NonNull IAnalysisDataset dataset,
			@NonNull FilteringOptions op, @NonNull String name) {
		this(List.of(dataset), op, name);
	}

	/**
	 * Create with a custom cell predicate. Use for more complex filters than the
	 * filtering options interface allows for.
	 * 
	 * @param datasets the dataset to filter
	 * @param op       the predicate for a cell
	 * @param name     the name for the filtered dataset
	 */
	public CellCollectionFilteringMethod(@NonNull IAnalysisDataset dataset,
			@NonNull Predicate<ICell> pred, @NonNull String name) {
		this(List.of(dataset), pred, name);
	}

	/**
	 * Create with filtering options relating to measurements of the cells
	 * 
	 * @param datasets the datasets to filter
	 * @param op       the filtering options
	 * @param name     the name for the filtered dataset
	 */
	public CellCollectionFilteringMethod(@NonNull List<IAnalysisDataset> datasets,
			@NonNull FilteringOptions op, @NonNull String name) {
		super(datasets);
		this.options = op;
		this.pred = null;
		newCollectionName = name;
	}

	/**
	 * Create with a custom cell predicate. Use for more complex filters than the
	 * filtering options interface allows for.
	 * 
	 * @param datasets the datasets to filter
	 * @param op       the predicate for a cell
	 * @param name     the name for the filtered dataset
	 */
	public CellCollectionFilteringMethod(@NonNull List<IAnalysisDataset> datasets,
			@NonNull Predicate<ICell> pred, @NonNull String name) {
		super(datasets);
		this.pred = pred;
		this.options = null;
		newCollectionName = name;
	}

	@Override
	public IAnalysisResult call() throws Exception {

		for (IAnalysisDataset d : datasets) {

			// If a predicate was provided, use it, otherwise create from the filtering
			// options on measurements
			Predicate<ICell> p = pred != null ? pred : options.getPredicate(d.getCollection());

			LOGGER.fine(() -> "Filtering %s on %s".formatted(d.getName(), p.toString()));

			// Filter using the given predicate
			ICellCollection filtered = CellCollectionFilterer.filter(d.getCollection(), p);

			if (filtered == null) {
				LOGGER.fine("No collection returned from filtering");
				continue;
			}

			if (!filtered.hasCells()) {
				LOGGER.info("No cells passed filter for " + d.getName());
				continue;
			}

			if (filtered.size() == d.getCollection().size()) {
				LOGGER.info("Skipping; all cells passed filter for " + d.getName());
				continue;
			}

			LOGGER.fine(() -> "Filtering returned " + filtered.size() + " cells");

			// there are cells, and they are not just the parent dataset

			ICellCollection v = new VirtualDataset(d, newCollectionName, null);
			v.addAll(filtered);
			try {
				d.getCollection().getProfileManager().copySegmentsAndLandmarksTo(v);
				d.getCollection().getSignalManager().copySignalGroupsTo(v);

			} catch (ProfileException e) {
				LOGGER.warning("Error copying collection offsets");
				LOGGER.log(Loggable.STACK, "Error in offsetting", e);
			}
			d.addChildCollection(v);
		}
		LOGGER.fine("Filtering completed for all datasets");
		return new DefaultAnalysisResult(datasets);
	}

}
