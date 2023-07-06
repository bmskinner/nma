package com.bmskinner.nma.visualisation.datasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.DefaultXYDataset;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

public class VennChartDataset extends DefaultXYDataset {

	private static final Logger LOGGER = Logger.getLogger(VennChartDataset.class.getName());

	/**
	 * Store the distinct clusters of datasets with shared cells
	 */
	private Map<Comparable<?>, List<IAnalysisDataset>> clusters = new HashMap<>();

	public VennChartDataset(List<IAnalysisDataset> datasets) {
		super();
		for (IAnalysisDataset d : datasets) {
			addDataset(d);
		}
		LOGGER.finer("Created venn chart with " + clusters.size() + " clusters");
		for (Entry<Comparable<?>, List<IAnalysisDataset>> entry : clusters.entrySet()) {
			LOGGER.finer(entry.getKey() + " has " + entry.getValue().size() + " datasets");
		}

		// Add the centre points of the datasets in each cluster as a series
		createSeries();
	}

	public void addDataset(IAnalysisDataset dataset) {

		LOGGER.info("Adding dataset " + dataset.getName());

		// Always declare a new cluster for first entry
		if (clusters.isEmpty()) {
			clusters.put("Cluster_" + clusters.size(), new ArrayList<>(Arrays.asList(dataset)));
			LOGGER.finer("Added dataset " + dataset.getName() + "to new cluster");
			return;
		}
		
		// Check if we can add the dataset to an existing cluster
		boolean wasAdded = false;
		for( List<IAnalysisDataset> cluster : clusters.values()) {
			boolean addToCluster = cluster.stream().anyMatch(d -> d.getCollection().countShared(dataset) > 0);

			if (addToCluster) {
				cluster.add(dataset);
				wasAdded = true;
				LOGGER.finer("Added dataset " + dataset.getName() + "to existing cluster");
			}
		}

		// If not, make a new cluster
		if (!wasAdded) {
			clusters.put("Cluster_" + clusters.size(), new ArrayList<>(Arrays.asList(dataset)));
			LOGGER.finer("Added dataset " + dataset.getName() + "to new cluster");
		}
		

		// Check if we can collapse any clusters with the latest addition
		collapseClusters();
	}

	public List<IAnalysisDataset> getDatasets(Comparable<?> clusterKey) {
		return clusters.get(clusterKey);
	}

	public double getMaxDomainValue() {
		return DatasetUtils.findMaximumDomainValue(this).doubleValue() + 1.4;
	}

	private void createSeries() {
		int xStart = 0;
		int xOffset = 10;
		int yStart = 0;

		for (Entry<Comparable<?>, List<IAnalysisDataset>> entry : clusters.entrySet()) {
			List<IAnalysisDataset> datasets = entry.getValue();

			LOGGER.finer("Creating series from " + datasets.size() + " datasets");
			double[][] datasetPos = new double[2][datasets.size()];

			// Set the centre for each dataset circle in the cluster

			switch (datasets.size()) {
			case 1:
				datasetPos[0] = new double[] { xStart };
				datasetPos[1] = new double[] { yStart };
				break;
			case 2:
				datasetPos[0] = new double[] { xStart, xStart + 1 };
				datasetPos[1] = new double[] { yStart, yStart };
				break;
			case 3:
				datasetPos[0] = new double[] { xStart, xStart + 1, xStart + 0.5 };
				datasetPos[1] = new double[] { yStart, yStart, yStart + 0.6 };
				break;
			case 4:
				datasetPos[0] = new double[] { xStart, xStart + 1, xStart, xStart + 1 };
				datasetPos[1] = new double[] { yStart, yStart, yStart + 0.6, yStart + 0.6 };
				break;
			default:
				datasetPos[0] = new double[] { xStart };
				datasetPos[1] = new double[] { yStart };
			}

			this.addSeries(entry.getKey(), datasetPos);
			
			xStart += xOffset;
		}
	}

	private void collapseClusters() {
		
		if(clusters.size()==1) {
			return;
		}
		
		Map<Comparable<?>, Boolean> includeInFinal = new HashMap<>();

		// Check if we can collapse any clusters with the latest addition
		Map<Comparable<?>, List<IAnalysisDataset>> replacementClusters = new HashMap<>();
		for (Entry<Comparable<?>, List<IAnalysisDataset>> entry1 : clusters.entrySet()) {
			for (Entry<Comparable<?>, List<IAnalysisDataset>> entry2 : clusters.entrySet()) {
				if (entry1.getKey().equals(entry2.getKey())) {
					continue;
				}

				boolean matchFound = entry1.getValue().stream().anyMatch(
						d -> entry2.getValue().stream().anyMatch(d2 -> d.getCollection().countShared(d2) > 0));

				if (matchFound) {
					entry1.getValue().addAll(entry2.getValue());
					includeInFinal.put(entry2.getKey(), false);
				}

				includeInFinal.putIfAbsent(entry1.getKey(), true);

			}
		}

		// Remove entry2 from consideration if absorbed into entry1
		for (Entry<Comparable<?>, Boolean> entry : includeInFinal.entrySet()) {
			if (entry.getValue()) {
				replacementClusters.put("Cluster_" + replacementClusters.size(), clusters.get(entry.getKey()));
			}
		}


		clusters.clear();
		clusters.putAll(replacementClusters);
	}
}
