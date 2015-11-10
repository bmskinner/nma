package gui.actions;

import gui.ClusteringSetupWindow;
import gui.MainWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import analysis.ClusteringOptions;
import analysis.nucleus.NucleusClusterer;

import components.CellCollection;
import components.ClusterGroup;

public class ClusterAnalysisAction extends ProgressableAction {

	public ClusterAnalysisAction(AnalysisDataset dataset, MainWindow mw) {
		super(dataset, "Cluster analysis", "Error in cluster analysis", mw);

		ClusteringSetupWindow clusterSetup = new ClusteringSetupWindow(mw);
		ClusteringOptions options = clusterSetup.getOptions();
		//Map<String, Object> options = clusterSetup.getOptions();

		if(clusterSetup.isReadyToRun()){ // if dialog was cancelled, skip

			//	worker = new NucleusClusterer(  (Integer) options.get("type"), dataset.getCollection() );
			worker = new NucleusClusterer( dataset , options );
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
			programLogger.log(Level.FINEST, "Cluster "+cluster+": "+c.getName());
			group.addDataset(c);
			c.setName(group.getName()+"_"+c.getName());
			programLogger.log(Level.FINEST, "Renamed cluster: "+c.getName());
			dataset.addChildCollection(c);
			// attach the clusters to their parent collection
			//	dataset.addCluster(c);

			programLogger.log(Level.INFO, "Cluster "+cluster+": "+c.getNucleusCount()+" nuclei");
			AnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
			clusterDataset.setRoot(false);
			list.add(clusterDataset);


		}
		dataset.addClusterGroup(group);
		programLogger.log(Level.FINEST, "Running new morphology analysis on cluster group");
		new MorphologyAnalysisAction(list, dataset, MainWindow.ADD_POPULATION, mw);

		cancel();

	}
}