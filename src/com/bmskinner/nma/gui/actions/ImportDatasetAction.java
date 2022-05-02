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
package com.bmskinner.nma.gui.actions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom2.Document;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.io.DatasetImportMethod;

/**
 * Call an open dialog to choose a saved .nbd dataset. The opened dataset will
 * be added to the bottom of the dataset list.
 */
public class ImportDatasetAction extends VoidResultAction {

	private static final Logger LOGGER = Logger.getLogger(ImportDatasetAction.class.getName());

	private final Document doc;
	private static final @NonNull String PROGRESS_BAR_LABEL = "Building dataset...";

	/**
	 * Create an import action for the given main window. Specify the file to be
	 * opened.
	 * 
	 * @param mw   the main window to which a progress bar will be attached
	 * @param file the dataset file to open
	 */
	public ImportDatasetAction(@NonNull final ProgressBarAcceptor acceptor, @NonNull Document doc,
			@Nullable CountDownLatch latch) {
		super(PROGRESS_BAR_LABEL, acceptor);
		if (latch != null)
			setLatch(latch);
		this.doc = doc;
	}

	@Override
	public void run() {
		setProgressBarIndeterminate();

		if (doc != null) {

			try {
				IAnalysisMethod m = new DatasetImportMethod(doc);
				worker = new DefaultAnalysisWorker(m);
				worker.addPropertyChangeListener(this);

			} catch (Exception e) {
				LOGGER.warning("Unable to import file: " + e.getMessage());

				super.finished();
			}

			setProgressMessage(PROGRESS_BAR_LABEL);

			ThreadManager.getInstance().submit(worker);
		} else {
			LOGGER.fine("Open cancelled");
			super.finished();
		}
	}

	@Override
	public void finished() {

		setProgressBarVisible(false);
		try {

			IAnalysisResult r = worker.get();

			IAnalysisDataset dataset = r.getFirstDataset();

			// Save newly converted datasets
			if (r.getBoolean(DatasetImportMethod.WAS_CONVERTED_BOOL)) {
				UserActionController.getInstance()
						.userActionEventReceived(
								new UserActionEvent(this, UserActionEvent.SAVE, List.of(dataset)));
			}

			LOGGER.fine("Opened dataset: " + dataset.getName());

			UIController.getInstance().fireDatasetAdded(dataset);

		} catch (InterruptedException e) {
			LOGGER.warning(
					"Unable to unmarshall dataset '" + doc.getRootElement().getName() + "': "
							+ e.getMessage());
		} catch (ExecutionException e) {
			LOGGER.warning("Unable to unmarshall dataset '" + doc.getRootElement().getName() + "': "
					+ e.getMessage());
		} finally {
			if (getLatch().isPresent())
				getLatch().get().countDown();
			super.finished();
		}

	}

}
