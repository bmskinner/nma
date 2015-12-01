/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
import gui.MainWindow;
import gui.DatasetEvent.DatasetMethod;
import gui.components.ClusterTreeDialog;
import gui.dialogs.HierarchicalTreeSetupDialog;
import gui.tabs.ClusterDetailPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import analysis.ClusteringOptions;
import analysis.nucleus.NucleusClusterer;
import analysis.nucleus.NucleusTreeBuilder;
import components.CellCollection;
import components.ClusterGroup;

public class BuildHierarchicalTreeAction extends ProgressableAction  {

	public BuildHierarchicalTreeAction(AnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Cluster analysis", "Error in cluster analysis", mw);

		HierarchicalTreeSetupDialog clusterSetup = new HierarchicalTreeSetupDialog(mw);
		ClusteringOptions options = clusterSetup.getOptions();
		//Map<String, Object> options = clusterSetup.getOptions();

		if(clusterSetup.isReadyToRun()){ // if dialog was cancelled, skip

			//	worker = new NucleusClusterer(  (Integer) options.get("type"), dataset.getCollection() );
			worker = new NucleusTreeBuilder( dataset , options , mw.getProgramLogger());
			//	((NucleusClusterer) worker).setClusteringOptions(options);

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

//		programLogger.log(Level.INFO, "Found "+((NucleusClusterer) worker).getNumberOfClusters()+" clusters");

		String tree = (((NucleusTreeBuilder) worker).getNewickTree());
		
//		programLogger.log(Level.INFO, tree);

//		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		ClusteringOptions options =  ((NucleusTreeBuilder) worker).getOptions();
//		//int clusterNumber = dataset.getClusterGroups().size();
//		programLogger.log(Level.FINEST, "Getting group number");
		int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
//		programLogger.log(Level.FINEST, "Cluster group number chosen: "+clusterNumber);
//
		ClusterGroup group = new ClusterGroup("ClusterGroup_"+clusterNumber, options, tree);
		
		ClusterTreeDialog clusterPanel = new ClusterTreeDialog(programLogger, dataset, group);
//		clusterPanel.addDatasetEventListener(BuildHierarchicalTreeAction.this);
//
//		for(int cluster=0;cluster<((NucleusClusterer) worker).getNumberOfClusters();cluster++){
//
//			CellCollection c = ((NucleusClusterer) worker).getCluster(cluster);
//
//			if(c.hasCells()){
//				programLogger.log(Level.FINEST, "Cluster "+cluster+": "+c.getName());
//				group.addDataset(c);
//				c.setName(group.getName()+"_"+c.getName());
//				programLogger.log(Level.FINEST, "Renamed cluster: "+c.getName());
//				dataset.addChildCollection(c);
//				
//				
//				// attach the clusters to their parent collection
//				programLogger.log(Level.INFO, "Cluster "+cluster+": "+c.getNucleusCount()+" nuclei");
//				AnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
//				clusterDataset.setRoot(false);
//				list.add(clusterDataset);
//			}
//
//
//		}
//		dataset.addClusterGroup(group);

		cancel();

	}


//	@Override
//	public void datasetEventReceived(DatasetEvent event) {
//		// TODO Auto-generated method stub
//		if(event.method()==DatasetMethod.COPY_MORPHOLOGY){
//			fireDatasetEvent(DatasetMethod.COPY_MORPHOLOGY, event.getDatasets(), event.secondaryDataset());
//		}
//		
//	}
}