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

import gui.MainWindow;
import gui.ThreadManager;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import gui.dialogs.ClusteringSetupDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import analysis.ClusteringOptions;
import analysis.nucleus.NucleusClusterer;
import analysis.profiles.ProfileManager;
import components.CellCollection;
import components.ClusterGroup;
import components.nuclear.SignalGroup;

public class ClusterAnalysisAction extends ProgressableAction {

	public ClusterAnalysisAction(AnalysisDataset dataset, MainWindow mw) {
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

		log(Level.INFO, "Found "+((NucleusClusterer) worker).getNumberOfClusters()+" clusters");

		String tree = (((NucleusClusterer) worker).getNewickTree());

		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		ClusteringOptions options =  ((NucleusClusterer) worker).getOptions();
		//int clusterNumber = dataset.getClusterGroups().size();
		log(Level.FINEST, "Getting group number");
		int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
		log(Level.FINEST, "Cluster group number chosen: "+clusterNumber);

		ClusterGroup group = new ClusterGroup("ClusterGroup_"+clusterNumber, options, tree);

		for(int cluster=0;cluster<((NucleusClusterer) worker).getNumberOfClusters();cluster++){

			CellCollection c = ((NucleusClusterer) worker).getCluster(cluster);

			if(c.hasCells()){
				finest("Cluster "+cluster+": "+c.getName());
				
				try {
					fine("Copying profiles to cluster");
					dataset.getCollection().getProfileManager().copyCollectionOffsets(c);
				} catch (Exception e) {
					logError("Error copying segments to cluster "+c.getName(), e);
				}
				
				//Copy signal groups
				for(UUID id  : dataset.getCollection().getSignalGroupIDs()){
					c.addSignalGroup(id, new SignalGroup(dataset.getCollection().getSignalGroup(id)));
					finest("Removing signal groups with no signals");
					if(c.getSignalManager().getSignalCount(id)==0){ // Signal group has no signals
						c.removeSignalGroup(id);
						finest("Removed signal group "+id.toString());
					}
					
				}
				
				
				group.addDataset(c);
				c.setName(group.getName()+"_"+c.getName());
				log(Level.FINEST, "Renamed cluster: "+c.getName());
				dataset.addChildCollection(c);
				
				
				// attach the clusters to their parent collection
				log(Level.INFO, "Cluster "+cluster+": "+c.getNucleusCount()+" nuclei");
				AnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
				clusterDataset.setRoot(false);
				list.add(clusterDataset);
			}


		}
		log(Level.FINE, "Profiles copied to all clusters");
		dataset.addClusterGroup(group);
		fireDatasetEvent(DatasetMethod.SAVE, dataset);
		fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
		super.finished();
		

	}
}