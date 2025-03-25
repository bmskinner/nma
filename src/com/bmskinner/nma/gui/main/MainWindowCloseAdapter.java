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
package com.bmskinner.nma.gui.main;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.actions.ExportDatasetAction;
import com.bmskinner.nma.gui.actions.ExportWorkspaceAction;
import com.bmskinner.nma.gui.events.UserActionController;

/**
 * This window adapter is used to check if datasets have changed when the main
 * window is closed, and offers to save changed datasets before completing the
 * close.
 * 
 * @author Ben Skinner
 * @since 1.13.3
 *
 */
public class MainWindowCloseAdapter extends WindowAdapter {

	private static final Logger LOGGER = Logger.getLogger(MainWindowCloseAdapter.class.getName());

	private MainView mw;

	public MainWindowCloseAdapter(MainView mw) {
		super();
		this.mw = mw;
	}

	@Override
	public void windowClosing(WindowEvent e) {

		if (DatasetListManager.getInstance().hashCodeChanged()) {
			LOGGER.fine("Found changed hashcode for at least one dataset");
			for (IAnalysisDataset d : DatasetListManager.getInstance().getRootDatasets()) {
				LOGGER.fine(() -> "Dataset '%s' has hashcode in close adapter '%s'".formatted(
						d.getName(),
						d.hashCode()));
			}

			String[] options = { "Save and exit", "Exit without saving", "Do not exit" };

			try {
				int save = new DefaultInputSupplier().requestOptionAllVisible(options, 0,
						"Datasets or workspaces have changed since last save!",
						"Save datasets and workspaces?");

				switch (save) {
				case 0:
					saveAndClose();
					return;
				case 1:
					close();
					return;
				case 2:
					return;
				default:
					return;
				}

			} catch (RequestCancelledException e1) {
				return;
			}
		}
		close();
	}

	@Override
	public void windowClosed(WindowEvent e) {
		close();
	}

	public void close() {
		mw.dispose();
		LOGGER.config("Disposed GUI; quitting JVM");
		System.exit(0);
	}

	/**
	 * Save the root datasets, then dispose the frame. TODO: Rework to use the
	 * thread manager
	 */
	private void saveAndClose() {

		final CountDownLatch latch = new CountDownLatch(1);

		// Run saves
		Runnable r = () -> {

			for (IAnalysisDataset root : DatasetListManager.getInstance()
					.getUnsavedRootDatasets()) {
				final CountDownLatch cl = new CountDownLatch(1);
				Runnable task = new ExportDatasetAction(root,
						UserActionController.getInstance().getProgressBarAcceptor(), cl, false);
				new Thread(task).start();
				try {
					cl.await();
				} catch (InterruptedException e) {
					LOGGER.log(Level.SEVERE, "Interruption saving datasets", e);
				}
			}
			LOGGER.info("Changed root datasets saved");

			// Save all workspaces, not just changed workspaces;
			// just because the datasets are not changed, does
			// not mean the workspace is written to disk
			for (IWorkspace w : DatasetListManager.getInstance().getWorkspaces()) {
				Runnable wrkTask = new ExportWorkspaceAction(w,
						UserActionController.getInstance().getProgressBarAcceptor());
				wrkTask.run();
			}
			LOGGER.info("All workspaces saved");
			latch.countDown();
		};

		// Wait for saves to complete, then close the window
		Runnable s = () -> {
			try {
				latch.await();
			} catch (InterruptedException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
			close();
		};

		new Thread(r).start();
		new Thread(s).start();
	}

}
