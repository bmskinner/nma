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
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.dialogs.ClusteringSetupDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;


/**
 * Setup a clustering of the given dataset.
 * @author ben
 *
 */
public class ClusterAnalysisAction extends ProgressableAction {

	private static final String PROGRESS_BAR_LABEL = "Clustering cells";
	
	public ClusterAnalysisAction(IAnalysisDataset dataset, MainWindow mw) {
		super(dataset, PROGRESS_BAR_LABEL, mw);
	}
	
	@Override
	public void run(){

		SubAnalysisSetupDialog clusterSetup = new ClusteringSetupDialog(mw, dataset);

		if(clusterSetup.isReadyToRun()){ // if dialog was cancelled, skip
			IAnalysisMethod m = clusterSetup.getMethod();
			
			int maxProgress = dataset.getCollection().size() * 2;
			worker = new DefaultAnalysisWorker(m, maxProgress);
			
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

		this.setProgressBarVisible(false);
		
		
		try {
			ClusterAnalysisResult r = (ClusterAnalysisResult) worker.get();
			int size = r.getGroup().size();
			log("Found "+size+" clusters");
		} catch (InterruptedException | ExecutionException e) {
			warn("Error clustering");
			stack("Error clustering", e);
		}
		

		fireDatasetEvent(DatasetEvent.SAVE, dataset);
		fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
		super.finished();
		

	}
}