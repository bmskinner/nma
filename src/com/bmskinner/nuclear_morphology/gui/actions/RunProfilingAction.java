/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;

public class RunProfilingAction extends SingleDatasetResultAction {
	
	private static final String PROGRESS_BAR_LABEL = "Profiling";

    public RunProfilingAction(@NonNull final IAnalysisDataset dataset, int downFlag, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(dataset, PROGRESS_BAR_LABEL, acceptor, eh, downFlag);
    }

    public RunProfilingAction(@NonNull final List<IAnalysisDataset> list, int downFlag, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(list, PROGRESS_BAR_LABEL, acceptor, eh, downFlag);
    }

    public RunProfilingAction(@NonNull final IAnalysisDataset dataset, int downFlag, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, CountDownLatch latch) {
        super(dataset, PROGRESS_BAR_LABEL, acceptor, eh, downFlag);
        this.setLatch(latch);

    }

    public RunProfilingAction(@NonNull final List<IAnalysisDataset> list, int downFlag, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, CountDownLatch latch) {
        super(list, PROGRESS_BAR_LABEL, acceptor, eh, downFlag);
        this.setLatch(latch);

    }

    public void run() {
        runNewAnalysis();
    }

    private void runNewAnalysis() {
        try {
            String message = "Profiling: " + dataset.getName();
            fine("Beginning profliling action");

            this.setProgressMessage(message);
            IAnalysisMethod method = new DatasetProfilingMethod(dataset);
            worker = new DefaultAnalysisWorker(method);

            worker.addPropertyChangeListener(this);

            ThreadManager.getInstance().submit(worker);
        } catch (Exception e) {
            this.cancel();
            error("Error in morphology analysis", e);
        }
    }

    @Override
    public void finished() {

        // ensure the progress bar gets hidden even if it is not removed
        this.setProgressBarVisible(false);

        // The analysis takes place in a new thread to accomodate refolding.
        // See specific comment in RunSegmentationAction
        Runnable task = () -> {
            
            getDatasetEventHandler().fireDatasetEvent(DatasetEvent.SAVE, dataset);

            if ((downFlag & ADD_POPULATION) == ADD_POPULATION) {
                finest("Adding dataset to list manager");
                DatasetListManager.getInstance().addDataset(dataset);
                finest("Firing add dataset signal");
                getDatasetEventHandler().fireDatasetEvent(DatasetEvent.ADD_DATASET, dataset);

            }

            // if no list was provided, or no more entries remain,
            // call the finish
            if (!hasRemainingDatasetsToProcess()) {

                cancel();
                getInterfaceEventHandler().removeListener(eh);
                getDatasetEventHandler().removeListener(eh);
                //
                RunProfilingAction.this.countdownLatch();

            } else {
                // otherwise analyse the next item in the list
                cancel(); // remove progress bar

                Runnable p = new RunProfilingAction(getRemainingDatasetsToProcess(), downFlag, progressAcceptors.get(0), eh);
                p.run();

            }
            countdownLatch();
        };

        ThreadManager.getInstance().execute(task);

    }

}
