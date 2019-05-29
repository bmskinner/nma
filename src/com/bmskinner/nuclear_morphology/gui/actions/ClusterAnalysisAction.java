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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nuclear_morphology.analysis.classification.PrincipalComponentAnalysis;
import com.bmskinner.nuclear_morphology.analysis.classification.TsneMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.dialogs.ClusteringSetupDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;

/**
 * Setup a clustering of the given dataset.
 * 
 * @author ben
 *
 */
public class ClusterAnalysisAction extends SingleDatasetResultAction {

    private static final String PROGRESS_BAR_LABEL = "Clustering cells";

    public ClusterAnalysisAction(IAnalysisDataset dataset, @NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
        super(dataset, PROGRESS_BAR_LABEL, acceptor, eh);
    }

    @Override
    public void run() {

        SubAnalysisSetupDialog clusterSetup = new ClusteringSetupDialog(dataset);

        if (clusterSetup.isReadyToRun()) { // if dialog was cancelled, skip

        	boolean canRunClusteringDirectly = true;
        	if(clusterSetup.getOptions().getBoolean(IClusteringOptions.USE_TSNE_KEY)) {
        		canRunClusteringDirectly = false;
        		IAnalysisMethod m = new TsneMethod(dataset, clusterSetup.getOptions());
        		worker = new DefaultAnalysisWorker(m);
        		worker.addPropertyChangeListener(e->{
        			if(e.getPropertyName().equals(IAnalysisWorker.FINISHED_MSG)) {
        				runClustering((IClusteringOptions) clusterSetup.getOptions());
        			}
        		});
        		ThreadManager.getInstance().submit(worker);
        	}
        	
        	if(clusterSetup.getOptions().getBoolean(IClusteringOptions.USE_PCA_KEY)) {
        		canRunClusteringDirectly = false;
        		IAnalysisMethod m = new PrincipalComponentAnalysis(dataset, clusterSetup.getOptions());
        		worker = new DefaultAnalysisWorker(m);
        		worker.addPropertyChangeListener(e->{
        			if(e.getPropertyName().equals(IAnalysisWorker.FINISHED_MSG)) {
        				runClustering((IClusteringOptions) clusterSetup.getOptions());
        			}
        		});
        		ThreadManager.getInstance().submit(worker);
        	}
        	
        	if(canRunClusteringDirectly)
        		runClustering((IClusteringOptions) clusterSetup.getOptions());

        } else {
        	this.cancel();
        }
        clusterSetup.dispose();
    }
    
    private void runClustering(IClusteringOptions options) {
    	IAnalysisMethod m2 = new NucleusClusteringMethod(dataset, options);
		worker = new DefaultAnalysisWorker(m2);
		worker.addPropertyChangeListener(ClusterAnalysisAction.this);
		ThreadManager.getInstance().submit(worker);
    }

    @Override
    public void finished() {

        this.setProgressBarVisible(false);

        try {
            ClusterAnalysisResult r = (ClusterAnalysisResult) worker.get();
            int size = r.getGroup().size();
            log("Found " + size + " clusters");
        } catch (InterruptedException | ExecutionException e) {
            warn("Error clustering");
            stack("Error clustering", e);
        }

        getDatasetEventHandler().fireDatasetEvent(DatasetEvent.SAVE, dataset);
        getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
        super.finished();
    }
}
