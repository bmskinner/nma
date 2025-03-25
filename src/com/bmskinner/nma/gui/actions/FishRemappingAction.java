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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.gui.dialogs.prober.FishRemappingProber;
import com.bmskinner.nma.gui.events.UIController;

/**
 * Compare morphology images with post-FISH images, and select nuclei into new
 * sub-populations
 */
public class FishRemappingAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(FishRemappingAction.class.getName());

	private static final String PROGRESS_LBL = "Remapping";

	public FishRemappingAction(@NonNull final List<IAnalysisDataset> datasets,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_LBL, acceptor);

	}

	@Override
	public void run() {

		if (dataset == null)
			return;
		try {

			if (dataset.hasMergeSources()) {
				LOGGER.warning("Cannot remap merged datasets");
				cancel();
				return;
			}

			File fishDir = FileSelector.choosePostFISHDirectory(dataset);
			if (fishDir == null) {
				LOGGER.info("Remapping cancelled");
				cancel();
				return;
			}

			FishRemappingProber fishMapper = new FishRemappingProber(dataset, fishDir);

			if (fishMapper.isOk()) {

				LOGGER.info("Fetching collections...");
				final List<IAnalysisDataset> newList = fishMapper.getNewDatasets();

				if (newList.isEmpty()) {
					LOGGER.info("No collections returned");
					cancel();
					return;
				}

				LOGGER.info("Reapplying morphology...");

				CountDownLatch latch = new CountDownLatch(1);
				new RunSegmentationAction(newList, dataset, progressAcceptors.get(0),
						latch).run();
				new Thread(() -> {
					try {
						latch.await();
						UIController.getInstance().fireDatasetAdded(newList);
						finished();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}).start();

			} else {
				LOGGER.info("Remapping cancelled");
				cancel();
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error in FISH remapping: %s".formatted(e.getMessage()), e);
		}
	}

	@Override
	public void finished() {
		// Do not use super.finished(), or it will trigger another save action
		LOGGER.fine("FISH mapping complete");
		cancel();
	}
}
