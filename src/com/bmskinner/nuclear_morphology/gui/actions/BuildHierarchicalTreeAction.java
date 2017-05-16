/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.concurrent.ExecutionException;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetEventListener;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEventListener;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.dialogs.ClusterTreeDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.HierarchicalTreeSetupDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;

/**
 * Action for constructing hierarchical trees based on dataset parameters
 * @author ben
 *
 */
public class BuildHierarchicalTreeAction extends SingleDatasetResultAction implements DatasetEventListener, InterfaceEventListener {

	private static final String PROGRESS_BAR_LABEL = "Building tree";
	
	public BuildHierarchicalTreeAction(IAnalysisDataset dataset, MainWindow mw) {
		super(dataset, PROGRESS_BAR_LABEL, mw);
	}
	
	@Override
	public void run(){
		
		SubAnalysisSetupDialog clusterSetup = new HierarchicalTreeSetupDialog(mw, dataset);

		if(clusterSetup.isReadyToRun()){ // if dialog was cancelled, skip
			IAnalysisMethod m = clusterSetup.getMethod();//new TreeBuildingMethod(dataset, options);

			int maxProgress = dataset.getCollection().size() * 2;
			worker = new DefaultAnalysisWorker(m);
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);

		} else {
			this.cancel();
		}
		clusterSetup.dispose();
	}


	/* (non-Javadoc)
	 * Overrides because we need to carry out the morphology reprofiling
	 * on each cluster
	 * @see no.gui.MainWindow.ProgressableAction#finished()
	 */
	@Override
	public void finished() {
		
		try {
			ClusterAnalysisResult r = (ClusterAnalysisResult) worker.get();

			ClusterTreeDialog clusterPanel = new ClusterTreeDialog( dataset, r.getGroup());
			clusterPanel.addDatasetEventListener(BuildHierarchicalTreeAction.this);
			clusterPanel.addInterfaceEventListener(this);

			cleanup(); // do not cancel, we need the MainWindow listener to remain attached 

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	@Override
	public void datasetEventReceived(DatasetEvent event) {
		finest("BuildHierarchicalTreeAction heard dataset event");
		if(event.method().equals(DatasetEvent.COPY_PROFILE_SEGMENTATION)){
			fireDatasetEvent(DatasetEvent.COPY_PROFILE_SEGMENTATION, event.getDatasets(), event.secondaryDataset());
		}
		
	}


	@Override
	public void interfaceEventReceived(InterfaceEvent event) {
		fireInterfaceEvent(event.method());
		
	}

}