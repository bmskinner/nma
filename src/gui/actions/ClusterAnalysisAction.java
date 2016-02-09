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
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import gui.dialogs.ClusteringSetupDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import analysis.ClusteringOptions;
import analysis.ProfileManager;
import analysis.nucleus.NucleusClusterer;
import components.CellCollection;
import components.ClusterGroup;

public class ClusterAnalysisAction extends ProgressableAction {

	public ClusterAnalysisAction(AnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Cluster analysis", mw);

		ClusteringSetupDialog clusterSetup = new ClusteringSetupDialog(mw, dataset);
		ClusteringOptions options = clusterSetup.getOptions();
		//Map<String, Object> options = clusterSetup.getOptions();

		if(clusterSetup.isReadyToRun()){ // if dialog was cancelled, skip

			worker = new NucleusClusterer( dataset , options , mw.getProgramLogger());

			worker.addPropertyChangeListener(this);
			worker.execute();

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

		programLogger.log(Level.INFO, "Found "+((NucleusClusterer) worker).getNumberOfClusters()+" clusters");

		String tree = (((NucleusClusterer) worker).getNewickTree());

		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		ClusteringOptions options =  ((NucleusClusterer) worker).getOptions();
		//int clusterNumber = dataset.getClusterGroups().size();
		programLogger.log(Level.FINEST, "Getting group number");
		int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
		programLogger.log(Level.FINEST, "Cluster group number chosen: "+clusterNumber);

		ClusterGroup group = new ClusterGroup("ClusterGroup_"+clusterNumber, options, tree);

		for(int cluster=0;cluster<((NucleusClusterer) worker).getNumberOfClusters();cluster++){

			CellCollection c = ((NucleusClusterer) worker).getCluster(cluster);

			if(c.hasCells()){
				programLogger.log(Level.FINEST, "Cluster "+cluster+": "+c.getName());
				
				try {
					programLogger.log(Level.FINE, "Copying profiles to cluster");
					dataset.getCollection().getProfileManager().copyCollectionOffsets(c);
				} catch (Exception e) {
					programLogger.log(Level.SEVERE, "Error copying segments to cluster "+c.getName(), e);
				}
				group.addDataset(c);
				c.setName(group.getName()+"_"+c.getName());
				programLogger.log(Level.FINEST, "Renamed cluster: "+c.getName());
				dataset.addChildCollection(c);
				
				
				// attach the clusters to their parent collection
				programLogger.log(Level.INFO, "Cluster "+cluster+": "+c.getNucleusCount()+" nuclei");
				AnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
				clusterDataset.setRoot(false);
				list.add(clusterDataset);
			}


		}
		programLogger.log(Level.FINE, "Profiles copied to all clusters");
		dataset.addClusterGroup(group);
		fireDatasetEvent(DatasetMethod.SAVE, dataset);
		super.finished();
		

	}
}