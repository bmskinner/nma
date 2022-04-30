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
package com.bmskinner.nma.analysis;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.InputSupplier;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.actions.ExportDatasetAction;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.logging.Loggable;

/**
 * Handle the deletion of selected datasets in the populations panel
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DatasetDeleter extends MultipleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(DatasetDeleter.class.getName());

	private static final String SAVE_LBL = "Save";
	private static final String DISPOSE_LBL = "Don't save";

	private static final String TITLE_LBL = "Close dataset?";
	private static final String WARNING_LBL = "<html>'%s' has changed since last save.<br>Save before closing?</html>";

	private InputSupplier is = new DefaultInputSupplier();

	public DatasetDeleter(@NonNull IAnalysisDataset dataset) {
		super(dataset);
	}

	public DatasetDeleter(@NonNull List<IAnalysisDataset> datasets) {
		super(datasets);
	}

	@Override
	public IAnalysisResult call() throws Exception {

		if (datasets == null || datasets.isEmpty())
			return new DefaultAnalysisResult(datasets);

		try {
			CountDownLatch l = new CountDownLatch(1);
			saveRootDatasets(datasets, l);

			Deque<UUID> list = unique(datasets);

			deleteDatasetsInList(list);
			DatasetListManager.getInstance().refreshClusters();
			UIController.getInstance().fireDatasetDeleted(datasets);
		} catch (Exception e) {
			LOGGER.warning("Error deleting dataset");
			LOGGER.log(Loggable.STACK, "Error deleting dataset", e);
		}
		return new DefaultAnalysisResult(datasets);
	}

	private void saveRootDatasets(List<IAnalysisDataset> datasets, CountDownLatch c) throws InterruptedException {

		for (IAnalysisDataset d : datasets) {
			if (d.isRoot() && DatasetListManager.getInstance().hashCodeChanged(d)) {

				try {
					String[] buttonLabels = { DISPOSE_LBL, SAVE_LBL };
					int option = is.requestOptionAllVisible(buttonLabels, String.format(WARNING_LBL, d.getName()),
							TITLE_LBL);
					if (option == 0) // don't save
						continue;

					CountDownLatch l = new CountDownLatch(1);
					Runnable task = new ExportDatasetAction(d,
							UserActionController.getInstance().getProgressBarAcceptor(), l, false);
					ThreadManager.getInstance().submit(task);
					LOGGER.fine("Waiting for " + d.getName());
					l.await();
					LOGGER.fine("Finished saving " + d.getName());
				} catch (RequestCancelledException e) {
					// No action needed
				} catch (InterruptedException e) {
					LOGGER.log(Loggable.STACK, "Error deleting dataset", e);
				}

			}
		}

	}

	/**
	 * Recursively delete datasets. Remove all datasets with no children from the
	 * list. Recurse until all dataset have been deleted.
	 * 
	 * @param ids the dataset IDs to delete
	 */
	private void deleteDatasetsInList(Deque<UUID> ids) {

		if (ids.isEmpty())
			return;

		UUID id = ids.removeFirst();

		IAnalysisDataset d = DatasetListManager.getInstance().getDataset(id);

		if (d.hasChildren()) {
			LOGGER.finer("Dataset " + d.getName() + " still has children");
			ids.addLast(id); // put at the end of the deque to be handled last
		} else {
			deleteDataset(d);
		}

		deleteDatasetsInList(ids);
	}

	/**
	 * Delete a single dataset
	 * 
	 * @param d
	 */
	private void deleteDataset(IAnalysisDataset d) {

		UUID id = d.getId();
		// remove the dataset from its parents
		for (IAnalysisDataset parent : DatasetListManager.getInstance().getAllDatasets()) {
			if (parent.hasDirectChild(id)) {
				parent.deleteChild(id);
			}
		}

		if (d.isRoot())
			DatasetListManager.getInstance().removeDataset(d);
	}

	/**
	 * Get the list of unique datasets that must be removed
	 * 
	 * @param list
	 * @return
	 */
	private Deque<UUID> unique(List<IAnalysisDataset> list) {
		Set<UUID> set = new HashSet<>();
		for (IAnalysisDataset d : list) {
			set.add(d.getId());

			if (d.hasChildren()) {
				// add all the children of a dataset
				for (UUID childID : d.getAllChildUUIDs()) {
					set.add(childID);
				}
			}
		}

		Deque<UUID> result = new ArrayDeque<>();
		result.addAll(set);

		return result;
	}

}
