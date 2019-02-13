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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.dialogs.LobeDetectionSetupDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;

public class LobeDetectionAction extends SingleDatasetResultAction {

    private static final String PROGRESS_BAR_LABEL = "Detecting lobes";

    public LobeDetectionAction(IAnalysisDataset dataset, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(dataset, PROGRESS_BAR_LABEL, acceptor, eh);
    }

    @Override
    public void run() {
        fine("Getting lobe detection options");
        SubAnalysisSetupDialog setup = new LobeDetectionSetupDialog(dataset);

        if (setup.isReadyToRun()) { // if dialog was cancelled, skip

            log("Running lobe detection");
            IAnalysisMethod m = setup.getMethod();

            int maxProgress = dataset.getCollection().size();
            worker = new DefaultAnalysisWorker(m, maxProgress);

            worker.addPropertyChangeListener(this);
            ThreadManager.getInstance().submit(worker);

        } else {
            fine("Cancelling lobe detection");
            this.cancel();
        }
        setup.dispose();
    }

    @Override
    public void finished() {

        this.setProgressBarVisible(false);

        // try {
        // IAnalysisResult r = worker.get();
        //
        // } catch (InterruptedException | ExecutionException e) {
        // warn("Error in lobe detection");
        // stack(e.getMessage(), e);
        // }

        // fireDatasetEvent(DatasetEvent.SAVE, dataset);
        getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
        super.finished();

    }
}
