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
package com.bmskinner.nuclear_morphology.gui.actions;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.FishRemappingProber;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;

/**
 * Compare morphology images with post-FISH images, and select nuclei into new
 * sub-populations
 */
public class FishRemappingAction extends SingleDatasetResultAction {

    private static final String PROGRESS_LBL      = "Remapping";
    
    private File fishDir;

    public FishRemappingAction(final List<IAnalysisDataset> datasets, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(datasets, PROGRESS_LBL, acceptor, eh);

    }

    @Override
    public void run() {
        try {

            if (dataset.hasMergeSources()) {
                warn("Cannot remap merged datasets");
                cancel();
                return;
            }

            fishDir = FileSelector.choosePostFISHDirectory(dataset);
            if (fishDir==null) {
                log("Remapping cancelled");
                cancel();
                return;
            }

            FishRemappingProber fishMapper = new FishRemappingProber(dataset, fishDir);

            if (fishMapper.isOk()) {

                log("Fetching collections...");
                final List<IAnalysisDataset> newList = fishMapper.getNewDatasets();

                if (newList.isEmpty()) {
                    log("No collections returned");
                    cancel();
                    return;
                }

                log("Reapplying morphology...");

                CountDownLatch latch = new CountDownLatch(1);
                new RunSegmentationAction(newList, dataset, NO_FLAG, progressAcceptors.get(0), eh, latch).run();
                new Thread( ()->{
                	try {
						latch.await();
						getDatasetEventHandler().fireDatasetEvent(DatasetEvent.ADD_DATASET, newList);
						finished();
					} catch (InterruptedException e) {
						
					}
                }).start();;
                

            } else {
                log("Remapping cancelled");
                cancel();
            }

        } catch (Exception e) {
            warn("Error in FISH remapping: " + e.getMessage());
            stack("Error in FISH remapping: " + e.getMessage(), e);
        }
    }

    @Override
    public void finished() {
        // Do not use super.finished(), or it will trigger another save action
        fine("FISH mapping complete");
        cancel();
        getInterfaceEventHandler().removeListener(eh);
        getDatasetEventHandler().removeListener(eh);
    }
}
