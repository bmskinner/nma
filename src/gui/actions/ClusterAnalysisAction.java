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
import gui.MainWindow;
import gui.ThreadManager;
import gui.InterfaceEvent.InterfaceMethod;
import gui.dialogs.ClusteringSetupDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import utility.Constants;
import analysis.ClusteringOptions;
import analysis.IAnalysisDataset;
import analysis.nucleus.NucleusClusterer;
import analysis.profiles.ProfileException;
import components.ClusterGroup;
import components.ICellCollection;
import components.IClusterGroup;
import components.nuclear.SignalGroup;


public class ClusterAnalysisAction extends ProgressableAction {

	public ClusterAnalysisAction(IAnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Cluster analysis", mw);

		ClusteringSetupDialog clusterSetup = new ClusteringSetupDialog(mw, dataset);
		ClusteringOptions options = clusterSetup.getOptions();
		//Map<String, Object> options = clusterSetup.getOptions();

		if(clusterSetup.isReadyToRun()){ // if dialog was cancelled, skip

			worker = new NucleusClusterer( dataset , options );

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
		log("Found "+((NucleusClusterer) worker).getNumberOfClusters()+" clusters");

		String tree = (((NucleusClusterer) worker).getNewickTree());

		List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
		ClusteringOptions options =  ((NucleusClusterer) worker).getOptions();

		finest("Getting group number");
		int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
		finest("Cluster group number chosen: "+clusterNumber);

		IClusterGroup group = new ClusterGroup(Constants.CLUSTER_GROUP_PREFIX+"_"+clusterNumber, options, tree);

		for(int cluster=0;cluster<((NucleusClusterer) worker).getNumberOfClusters();cluster++){

			ICellCollection c = ((NucleusClusterer) worker).getCluster(cluster);

			if(c.hasCells()){
				finest("Cluster "+cluster+": "+c.getName());
				
				try {
					dataset.getCollection().getProfileManager().copyCollectionOffsets(c);
				} catch (ProfileException e) {
					warn("Error copying collection offsets");
					fine("Error in offsetting", e);
				}

				
				
				group.addDataset(c);
				c.setName(group.getName()+"_"+c.getName());

				dataset.addChildCollection(c);
								
				
				// attach the clusters to their parent collection
				log("Cluster "+cluster+": "+c.size()+" nuclei");
				IAnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
				clusterDataset.setRoot(false);
				
				// set shared counts
				c.setSharedCount(dataset.getCollection(), c.size());
				dataset.getCollection().setSharedCount(c, c.size());
				
				list.add(clusterDataset);
			}


		}
		fine("Profiles copied to all clusters");
		dataset.addClusterGroup(group);
		fireDatasetEvent(DatasetEvent.SAVE, dataset);
		fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
		super.finished();
		

	}
}