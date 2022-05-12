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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.workspaces.DefaultWorkspace;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.io.GenericFileImporter;

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
				IWorkspace w = new DefaultWorkspace(file, doc.getRootElement());
				DatasetListManager.getInstance().addWorkspace(w);

				for (File dataFile : w.getFiles()) {
					if (!dataFile.exists())
						continue;

					// Try to load the dataset and wait for success
					CountDownLatch l = new CountDownLatch(1);
					new GenericFileImporter(dataFile, progressAcceptors.get(0), l,
							IAnalysisDataset.XML_ANALYSIS_DATASET).run();
					l.await();

					// TODO: We actually search for the dataset before it is
					// added to the manager. We need to wait a bit longer or add another latch
					Thread.sleep(5000); // this currently resolves the issue on small datasets

					// Find the dataset just added from file name
					Optional<IAnalysisDataset> added = DatasetListManager.getInstance()
							.getRootDatasets()
							.stream()
							.filter(d -> d.getSavePath().equals(dataFile)).findFirst();

					if (added.isEmpty()) {
						LOGGER.fine("Dataset not found to add to workspace");
						continue;
					}

					UIController.getInstance().fireDatasetAdded(w, added.get());
				}

			}
		} catch (InterruptedException e) {
			LOGGER.fine("Import workspace interrupted: " + e.getMessage());
		} finally {
			super.finished();
		}

	}
}
