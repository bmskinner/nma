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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.workspaces.DefaultWorkspace;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.io.DatasetImportMethod;
import com.bmskinner.nma.io.XMLImportMethod;

public class ImportWorkspaceAction extends VoidResultAction {

	private static final Logger LOGGER = Logger.getLogger(ImportWorkspaceAction.class.getName());

	private final Document doc;
	private final File file;
	private static final @NonNull String PROGRESS_BAR_LABEL = "Opening workspace...";

	/**
	 * Create an import action for the given main window. Specify the file to be
	 * opened.
	 * 
	 * @param mw   the main window to which a progress bar will be attached
	 * @param file the workspace file to open
	 */
	public ImportWorkspaceAction(@NonNull final ProgressBarAcceptor acceptor,
			@NonNull Document doc, @NonNull File file) {
		super(PROGRESS_BAR_LABEL, acceptor);
		this.doc = doc;
		this.file = file;
	}

	@Override
	public void run() {
		setProgressBarIndeterminate();
		setProgressMessage(PROGRESS_BAR_LABEL);

		try {

			if (doc != null) {
				final IWorkspace w = new DefaultWorkspace(file, doc.getRootElement());
				DatasetListManager.getInstance().addWorkspace(w);

				final List<IAnalysisDataset> newDatasets = new ArrayList<>();

				for (final File dataFile : w.getFiles()) {
					if (!dataFile.exists()) {
						continue;
					}

					// Try to load the dataset and wait for success
					// First read the XML file
					final XMLImportMethod method = new XMLImportMethod(dataFile);
					worker = new DefaultAnalysisWorker(method);
					ThreadManager.getInstance().submit(worker);

					try {
						worker.get();

						// Now unmarshall the XML file into a dataset
						final Document datasetDoc = method.getXMLDocument();
						final IAnalysisMethod importMethod = new DatasetImportMethod(datasetDoc);
						worker = new DefaultAnalysisWorker(importMethod);
						ThreadManager.getInstance().submit(worker);

						final IAnalysisResult r = worker.get();
						final IAnalysisDataset d = r.getFirstDataset();

						// Update the save file to the file we just opened, in case it has moved
						d.setSavePath(dataFile);
						newDatasets.add(d);
						LOGGER.fine("Imported %s".formatted(d.getName()));

					} catch (final ExecutionException e) {
						LOGGER.warning("Unable to import dataset: %s".formatted(e.getMessage()));
					} catch (final Exception e) {
						LOGGER.log(Level.SEVERE, "Error in import: %s".formatted(e.getMessage()), e);
						throw e;
					}
				}


				UIController.getInstance().fireDatasetAdded(newDatasets);

			}
		} catch (final InterruptedException e) {
			LOGGER.fine("Import workspace interrupted: " + e.getMessage());

		} finally {
			super.finished();
		}

	}
}
