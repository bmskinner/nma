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

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.dialogs.ClusterTreeDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.HierarchicalTreeSetupDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nuclear_morphology.gui.events.ChartOptionsRenderedEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;

/**
 * Action for constructing hierarchical trees based on dataset parameters
 * 
 * @author ben
 *
 */
public class BuildHierarchicalTreeAction extends SingleDatasetResultAction
        implements EventListener {
	
	private static final Logger LOGGER = Logger.getLogger(BuildHierarchicalTreeAction.class.getName());

    private static final @NonNull String PROGRESS_BAR_LABEL = "Building tree";

    public BuildHierarchicalTreeAction(@NonNull IAnalysisDataset dataset, @NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
        super(dataset, PROGRESS_BAR_LABEL, acceptor, eh);
    }

    @Override
    public void run() {

        SubAnalysisSetupDialog clusterSetup = new HierarchicalTreeSetupDialog(dataset);

        if (clusterSetup.isReadyToRun()) { // if dialog was cancelled, skip
            IAnalysisMethod m = clusterSetup.getMethod();

            worker = new DefaultAnalysisWorker(m);
            worker.addPropertyChangeListener(this);
            ThreadManager.getInstance().submit(worker);

        } else {
            this.cancel();
        }
        clusterSetup.dispose();
    }

    /*
     * (non-Javadoc) Overrides because we need to carry out the morphology
     * reprofiling on each cluster
     * 
     * @see no.gui.MainWindow.ProgressableAction#finished()
     */
    @Override
    public void finished() {

        try {
            ClusterAnalysisResult r = (ClusterAnalysisResult) worker.get();

            ClusterTreeDialog clusterPanel = new ClusterTreeDialog(dataset, r.getGroup());
            clusterPanel.addDatasetEventListener(BuildHierarchicalTreeAction.this);
            clusterPanel.addInterfaceEventListener(this);

            cleanup(); // do not cancel, we need the MainWindow listener to
                       // remain attached

        } catch (InterruptedException | ExecutionException e) {
        	LOGGER.log(Level.SEVERE, "Error making cluster tree dialog", e);
        	Thread.currentThread().interrupt();
        }
    }

    @Override
    public void eventReceived(DatasetEvent event) {
        LOGGER.finest( "BuildHierarchicalTreeAction heard dataset event");
        if (event.method().equals(DatasetEvent.COPY_PROFILE_SEGMENTATION)) {
            getDatasetEventHandler().fireDatasetEvent(DatasetEvent.COPY_PROFILE_SEGMENTATION, event.getDatasets(), event.secondaryDataset());
        }

    }

    @Override
    public void eventReceived(InterfaceEvent event) {
        getInterfaceEventHandler().fireInterfaceEvent(event.method());

    }

	@Override
	public void eventReceived(DatasetUpdateEvent event) {
		// No action		
	}

	@Override
	public void eventReceived(SignalChangeEvent event) {
		// No action	
	}

	@Override
	public void eventReceived(ChartOptionsRenderedEvent event) {
		// No action	
	}

}
