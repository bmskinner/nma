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
package gui.actions;

import gui.DatasetEvent;
import gui.DatasetEventListener;
import gui.InterfaceEvent;
import gui.InterfaceEventListener;
import gui.MainWindow;
import gui.ThreadManager;
import gui.DatasetEvent.DatasetMethod;
import gui.dialogs.ClusterTreeDialog;
import gui.dialogs.HierarchicalTreeSetupDialog;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import analysis.ClusteringOptions;
import analysis.nucleus.NucleusTreeBuilder;
import components.ClusterGroup;

public class BuildHierarchicalTreeAction extends ProgressableAction implements DatasetEventListener, InterfaceEventListener {

	public BuildHierarchicalTreeAction(AnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Building tree", mw);

		HierarchicalTreeSetupDialog clusterSetup = new HierarchicalTreeSetupDialog(mw, dataset);
		ClusteringOptions options = clusterSetup.getOptions();
//		Map<String, Object> options = clusterSetup.getOptions();

		if(clusterSetup.isReadyToRun()){ // if dialog was cancelled, skip

			worker = new NucleusTreeBuilder( dataset , options);
//			worker = new NeighbourJoiningTreeBuilder( dataset , mw.getProgramLogger());

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

		String newick = (((NucleusTreeBuilder) worker).getNewickTree());

		ClusteringOptions options =  ((NucleusTreeBuilder) worker).getOptions();


		int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;

		ClusterGroup group = new ClusterGroup("ClusterGroup_"+clusterNumber, options, newick);
//		group.addDataset(dataset);
		
		ClusterTreeDialog clusterPanel = new ClusterTreeDialog( dataset, group);
		clusterPanel.addDatasetEventListener(BuildHierarchicalTreeAction.this);
		clusterPanel.addInterfaceEventListener(this);

		cleanup(); // do not cancel, we need the MainWindow listener to remain attached 

	}


	@Override
	public void datasetEventReceived(DatasetEvent event) {
		log(Level.FINEST, "BuildHierarchicalTreeAction heard dataset event");
		if(event.method().equals(DatasetMethod.COPY_MORPHOLOGY)){
			fireDatasetEvent(DatasetMethod.COPY_MORPHOLOGY, event.getDatasets(), event.secondaryDataset());
		}
		
	}


	@Override
	public void interfaceEventReceived(InterfaceEvent event) {
		fireInterfaceEvent(event.method());
		
	}

}