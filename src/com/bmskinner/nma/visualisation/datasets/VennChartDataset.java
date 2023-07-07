package com.bmskinner.nma.visualisation.datasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.DefaultXYDataset;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

@SuppressWarnings("serial")
public class VennChartDataset extends DefaultXYDataset {

	private static final Logger LOGGER = Logger.getLogger(VennChartDataset.class.getName());

	private static final int X_OFFSET = 4;

	// Sentinal values allow the autoscale to work even though the circles drawn are
	// outside the range of values in this dataset
	private static final double SENTINAL_X_MIN = -1.2;
	private static final double SENTINAL_Y_MIN = -1.2;
	private static final double SENTINAL_Y_MAX = 1.5;

	private static final double Y_START = 0;

	private static final double DEFAULT_RADIUS = 0.7;
	private static final double SUBSET_RADIUS = 0.17;

	/**
	 * Store the distinct clusters of datasets with shared cells
	 */
	private Map<Comparable<?>, List<IAnalysisDataset>> clusters = new HashMap<>();

	/**
	 * Store the radii of Venn circles
	 */
	private Map<Comparable<?>, List<Double>> radii = new HashMap<>();

	/**
	 * The locations of annotations with the shared counts
	 */
	private List<Label> labels = new ArrayList<>();

	/**
	 * Store locations for count strings
	 * 
	 * @author bs19022
	 *
	 */
	public record Label(double x, double y, String label) {
		@Override
		public String toString() {
			return x + ", " + y + ": " + label;
		}
	}

	/**
	 * Create with datasets to be plotted
	 * 
	 * @param datasets
	 */
	public VennChartDataset(List<IAnalysisDataset> datasets) {
		super();
		for (IAnalysisDataset d : datasets) {
			addDataset(d);
		}

		// Add the centre points of the datasets in each cluster as a series
		createSeries();
	}

	/**
	 * We can only display a cluster of up to 3 circles. Checks the dataset can be
	 * drawn.
	 * 
	 * @return true if we can draw the dataset, false otherwise
	 */
	public boolean isValid() {
		return clusters.values().stream().allMatch(l -> l.size() <= 3);
	}

	public double getRadius(int series, int item) {
		Comparable<?> key = this.getSeriesKey(series);
		return radii.get(key).get(item);
	}

	/**
	 * Get the analysis datasets in the given cluster
	 * 
	 * @param clusterKey
	 * @return
	 */
	public List<IAnalysisDataset> getDatasets(Comparable<?> clusterKey) {
		return clusters.get(clusterKey);
	}

	public double getMaxDomainValue() {
		return DatasetUtils.findMaximumDomainValue(this).doubleValue() + 1.4;
	}

	public List<Label> getLabels() {
		return labels;
	}

	private Map<String, Integer> calculateCounts(List<IAnalysisDataset> cluster) {

		Map<String, Integer> result = new HashMap<>();

		if (cluster.size() == 1) {
			result.put("d1", cluster.get(0).getCollection().size());
		}

		if (cluster.size() == 2) {
			int d1d2 = cluster.get(0).getCollection().countShared(cluster.get(1));
			int d1 = cluster.get(0).getCollection().size() - d1d2;
			int d2 = cluster.get(1).getCollection().size() - d1d2;

			result.put("d1d2", d1d2);
			result.put("d1", d1);
			result.put("d2", d2);
		}

		if (cluster.size() == 3) {
			Set<UUID> d1 = cluster.get(0).getCollection().getCellIDs();
			Set<UUID> d2 = cluster.get(1).getCollection().getCellIDs();
			Set<UUID> d3 = cluster.get(2).getCollection().getCellIDs();

			Set<UUID> d1d2d3 = new HashSet<>(d1);
			d1d2d3.retainAll(d2);
			d1d2d3.retainAll(d3);

			Set<UUID> d1d2 = new HashSet<>(d1);
			d1d2.retainAll(d2);
			d1d2.removeAll(d1d2d3);

			Set<UUID> d1d3 = new HashSet<>(d1);
			d1d3.retainAll(d3);
			d1d3.removeAll(d1d2d3);

			Set<UUID> d1s = new HashSet<>(d1);
			d1s.removeAll(d1d2d3);
			d1s.removeAll(d1d2);
			d1s.removeAll(d1d3);

			Set<UUID> d2d3 = new HashSet<>(d2);
			d2d3.retainAll(d3);
			d2d3.removeAll(d1d2d3);

			Set<UUID> d2s = new HashSet<>(d2);
			d2s.removeAll(d1d2);
			d2s.removeAll(d2d3);
			d2s.removeAll(d1d2d3);

			Set<UUID> d3s = new HashSet<>(d3);
			d3s.removeAll(d1d3);
			d3s.removeAll(d2d3);
			d3s.removeAll(d1d2d3);

			result.put("d1d2d3", d1d2d3.size());
			result.put("d1d2", d1d2.size());
			result.put("d1d3", d1d3.size());
			result.put("d1", d1s.size());
			result.put("d2d3", d2d3.size());
			result.put("d2", d2s.size());
			result.put("d3", d3s.size());
		}

		return result;
	}

	/**
	 * Create the shared nucleus count labels for each cluster
	 * 
	 * @param counts  the number of shared cells for each dataset combination
	 * @param cluster the datasets to create labels for
	 * @param xOffset the location of the datasets in the plot
	 */
//	private void createLabels(Map<String, Integer> counts, List<IAnalysisDataset> cluster,
//			double xOffset) {
//
//		if (cluster.size() == 1) {
//			labels.add(new Label(xOffset, 0, String.valueOf(counts.get("d1"))));
//		}
//
//		if (cluster.size() == 2) {
//			labels.add(new Label(xOffset - 0.1, 0, String.valueOf(counts.get("d1"))));
//			labels.add(new Label(xOffset + 1.1, 0, String.valueOf(counts.get("d2"))));
//			labels.add(new Label(xOffset + 0.5, 0, String.valueOf(counts.get("d1d2"))));
//		}
//
//		if (cluster.size() == 3) {
//
//			// Are we making flat or folded chart (do all datasets share cells)?
//			if ((counts.get("d1d2") == 0 || counts.get("d1d3") == 0 || counts.get("d2d3") == 0)
//					&& counts.get("d1d2d3") == 0) {
//
//				if (counts.get("d1d2") == 0) { // d3 is in the middle
//					labels.add(new Label(xOffset + 0.5, 0, String.valueOf(counts.get("d1d3"))));
//					labels.add(new Label(xOffset - 0.3, 0, String.valueOf(counts.get("d1"))));
//					labels.add(new Label(xOffset + 1.5, 0, String.valueOf(counts.get("d2d3"))));
//					labels.add(new Label(xOffset + 2.2, 0, String.valueOf(counts.get("d2"))));
//					labels.add(new Label(xOffset + 1, 0, String.valueOf(counts.get("d3"))));
//				}
//
//				if (counts.get("d1d3") == 0) { // d2 is in the middle
//					labels.add(new Label(xOffset + 0.5, 0, String.valueOf(counts.get("d1d2"))));
//					labels.add(new Label(xOffset - 0.3, 0, String.valueOf(counts.get("d1"))));
//					labels.add(new Label(xOffset + 1.5, 0, String.valueOf(counts.get("d2d3"))));
//					labels.add(new Label(xOffset + 2.2, 0, String.valueOf(counts.get("d3"))));
//					labels.add(new Label(xOffset + 1, 0, String.valueOf(counts.get("d2"))));
//				}
//
//				if (counts.get("d2d3") == 0) { // d1 is in the middle
//					labels.add(new Label(xOffset + 0.5, 0, String.valueOf(counts.get("d1d2"))));
//					labels.add(new Label(xOffset - 0.3, 0, String.valueOf(counts.get("d2"))));
//					labels.add(new Label(xOffset + 1.5, 0, String.valueOf(counts.get("d1d3"))));
//					labels.add(new Label(xOffset + 2.2, 0, String.valueOf(counts.get("d3"))));
//					labels.add(new Label(xOffset + 1, 0, String.valueOf(counts.get("d1"))));
//				}
//
//			} else {
//
//				labels.add(new Label(xOffset + 0.5, 0.2, String.valueOf(counts.get("d1d2d3"))));
//				labels.add(new Label(xOffset + 0.5, -0.3, String.valueOf(counts.get("d1d2"))));
//				labels.add(new Label(xOffset + 0.1, 0.3, String.valueOf(counts.get("d1d3"))));
//				labels.add(new Label(xOffset - 0.3, -0.3, String.valueOf(counts.get("d1"))));
//				labels.add(new Label(xOffset + 0.9, 0.3, String.valueOf(counts.get("d2d3"))));
//				labels.add(new Label(xOffset + 1.3, -0.3, String.valueOf(counts.get("d2"))));
//				labels.add(new Label(xOffset + 0.5, 1, String.valueOf(counts.get("d3"))));
//			}
//		}
//
//	}

	/**
	 * Create the Venn circle centroids for a cluster
	 * 
	 * @param counts  the number of shared cells for each dataset combination
	 * @param cluster the datasets to create centroids for
	 * @param xOffset the location of the datasets in the plot
	 */
	private void createCentroids(Map<String, Integer> counts, List<IAnalysisDataset> cluster,
			double xStart, Comparable<?> key) {

		double[][] datasetPos = new double[2][cluster.size()];

		// Set the centre for each dataset circle in the cluster
		if (cluster.size() == 1) {
			datasetPos = createOneDatasetCentroids(counts, cluster, xStart, key);
		}

		if (cluster.size() == 2) {
			datasetPos = createTwoDatasetCentroids(counts, cluster, xStart, key);
		}

		if (cluster.size() == 3) {
			datasetPos = createThreeDatasetCentroids(counts, cluster, xStart, key);
		}

		this.addSeries(key, datasetPos);

		// Add dataset name annotations
//		for (int i = 0; i < datasetPos[0].length; i++) {
//			double x = datasetPos[0][i];
//			double y = datasetPos[1][i] == 0 ? i % 2 == 0 ? -1 : -0.8 : 1.5;
//			labels.add(new Label(x, y, cluster.get(i).getName()));
//		}
	}

	private double[][] createOneDatasetCentroids(Map<String, Integer> counts,
			List<IAnalysisDataset> cluster,
			double xStart, Comparable<?> key) {
		List<Double> radiusList = radii.computeIfAbsent(key, k -> new ArrayList<>());
		double[][] datasetPos = new double[2][cluster.size()];
		datasetPos[0] = new double[] { xStart };
		datasetPos[1] = new double[] { Y_START };
		radiusList.add(DEFAULT_RADIUS);
		labels.add(new Label(xStart, 0, String.valueOf(counts.get("d1"))));
		return datasetPos;
	}

	private double[][] createTwoDatasetCentroids(Map<String, Integer> counts,
			List<IAnalysisDataset> cluster,
			double xStart, Comparable<?> key) {
		return layoutDoubleFlat(counts, cluster, xStart, key, 1, 2);
	}

	private double[][] layoutDoubleFlat(Map<String, Integer> counts,
			List<IAnalysisDataset> cluster,
			double xStart, Comparable<?> key, int a, int b) {

		List<Double> radiusList = radii.computeIfAbsent(key, k -> new ArrayList<>());
		double[][] datasetPos = new double[2][cluster.size()];

		String da = "d" + a;
		String db = "d" + b;
		String dab = a < b ? da + db : db + da;

		if (counts.get(da) == 0) { // a entirely within b
			datasetPos[0] = new double[] { xStart + 0.5, xStart + 1 };
			datasetPos[1] = new double[] { Y_START, Y_START };
			radiusList.add(SUBSET_RADIUS);
			radiusList.add(DEFAULT_RADIUS);
			labels.add(new Label(xStart + 1.1, 0, String.valueOf(counts.get(db))));
			labels.add(new Label(xStart + 0.5, 0, String.valueOf(counts.get(dab))));
			labels.add(new Label(xStart + 1.1, -1, cluster.get(b - 1).getName()));
			labels.add(new Label(xStart + 0.5, -0.8, cluster.get(a - 1).getName()));

		} else if (counts.get(db) == 0) { // b entirely within a
			datasetPos[0] = new double[] { xStart, xStart + 0.5 };
			datasetPos[1] = new double[] { Y_START, Y_START };
			radiusList.add(DEFAULT_RADIUS);
			radiusList.add(SUBSET_RADIUS);
			labels.add(new Label(xStart, 0, String.valueOf(counts.get(da))));
			labels.add(new Label(xStart + 0.5, 0, String.valueOf(counts.get(dab))));
			labels.add(new Label(xStart, -1, cluster.get(a - 1).getName()));
			labels.add(new Label(xStart + 0.5, -0.8, cluster.get(b - 1).getName()));

		} else { // some shared

			datasetPos[0] = new double[] { xStart, xStart + 1, xStart + 2 };
			datasetPos[1] = new double[] { Y_START, Y_START, Y_START };

			radiusList.add(DEFAULT_RADIUS);
			radiusList.add(DEFAULT_RADIUS);

			labels.add(new Label(xStart - 0.3, 0, String.valueOf(counts.get(da))));
			labels.add(new Label(xStart + 0.5, 0, String.valueOf(counts.get(dab))));
			labels.add(new Label(xStart + 1, 0, String.valueOf(counts.get(db))));

			labels.add(new Label(xStart - 0.3, -1, cluster.get(a - 1).getName()));
			labels.add(new Label(xStart + 1, -1, cluster.get(b - 1).getName()));

		}
		return datasetPos;
	}

	private double[][] createThreeDatasetCentroids(Map<String, Integer> counts,
			List<IAnalysisDataset> cluster,
			double xStart, Comparable<?> key) {
		List<Double> radiusList = radii.computeIfAbsent(key, k -> new ArrayList<>());
		double[][] datasetPos = new double[2][cluster.size()];

		if ((counts.get("d1d2") == 0 || counts.get("d1d3") == 0 || counts.get("d2d3") == 0)
				&& counts.get("d1d2d3") == 0) {

			if (counts.get("d1d2") == 0) { // d3 is in the middle
				datasetPos = layoutTripleFlat(counts, cluster, xStart, key, 1, 3, 2);
			}

			if (counts.get("d1d3") == 0) { // d2 is in the middle
				datasetPos = layoutTripleFlat(counts, cluster, xStart, key, 1, 2, 3);
			}

			if (counts.get("d2d3") == 0) { // d1 is in the middle
				datasetPos = layoutTripleFlat(counts, cluster, xStart, key, 2, 1, 3);
//
//				radiusList.add(DEFAULT_RADIUS);
//
//				double d2x = counts.get("d2") == 0 ? xStart + 0.5 : xStart;
//				double d3x = counts.get("d3") == 0 ? xStart + 1.5 : xStart + 2;
//
//				if (counts.get("d2") == 0) {
//					radiusList.add(SUBSET_RADIUS);
//				} else {
//					radiusList.add(DEFAULT_RADIUS);
//					labels.add(new Label(xStart - 0.3, 0, String.valueOf(counts.get("d2"))));
//				}
//
//				if (counts.get("d3") == 0) {
//					radiusList.add(SUBSET_RADIUS);
//				} else {
//					radiusList.add(DEFAULT_RADIUS);
//					labels.add(new Label(xStart + 2.2, 0, String.valueOf(counts.get("d3"))));
//				}
//
//				datasetPos[0] = new double[] { xStart + 1, d2x, d3x };
//				datasetPos[1] = new double[] { Y_START, Y_START, Y_START };
//
//				labels.add(new Label(xStart + 0.5, 0, String.valueOf(counts.get("d1d2"))));
//				labels.add(new Label(xStart + 1.5, 0, String.valueOf(counts.get("d1d3"))));
//
//				labels.add(new Label(xStart + 1, 0, String.valueOf(counts.get("d1"))));
			}

		} else { // make a triangle
			radiusList.add(DEFAULT_RADIUS);
			radiusList.add(DEFAULT_RADIUS);
			radiusList.add(DEFAULT_RADIUS);
			datasetPos[0] = new double[] { xStart, xStart + 1, xStart + 0.5 };
			datasetPos[1] = new double[] { Y_START, Y_START, Y_START + 0.6 };
			labels.add(new Label(xStart + 0.5, 0.2, String.valueOf(counts.get("d1d2d3"))));
			labels.add(new Label(xStart + 0.5, -0.3, String.valueOf(counts.get("d1d2"))));
			labels.add(new Label(xStart + 0.1, 0.3, String.valueOf(counts.get("d1d3"))));
			labels.add(new Label(xStart - 0.3, -0.3, String.valueOf(counts.get("d1"))));
			labels.add(new Label(xStart + 0.9, 0.3, String.valueOf(counts.get("d2d3"))));
			labels.add(new Label(xStart + 1.3, -0.3, String.valueOf(counts.get("d2"))));
			labels.add(new Label(xStart + 0.5, 1, String.valueOf(counts.get("d3"))));

			labels.add(new Label(xStart - 0.3, -1, cluster.get(0).getName()));
			labels.add(new Label(xStart + 0.5, 1.4, cluster.get(1).getName()));
			labels.add(new Label(xStart + 1.3, -1, cluster.get(2).getName()));
		}
		return datasetPos;
	}

	private double[][] layoutTripleFlat(Map<String, Integer> counts,
			List<IAnalysisDataset> cluster,
			double xStart, Comparable<?> key, int a, int b, int c) {

		List<Double> radiusList = radii.computeIfAbsent(key, k -> new ArrayList<>());
		double[][] datasetPos = new double[2][cluster.size()];

		String da = "d" + a;
		String db = "d" + b;
		String dc = "d" + c;
		String dab = a < b ? da + db : db + da;
		String dbc = b < c ? db + dc : dc + db;

		double ax = counts.get(da) == 0 ? xStart + 0.5 : xStart;
		double cx = counts.get(dc) == 0 ? xStart + 1.5 : xStart + 2;

		datasetPos[0] = new double[] { ax, xStart + 1, cx };
		datasetPos[1] = new double[] { Y_START, Y_START, Y_START };

		radiusList.add(counts.get(da) == 0 ? SUBSET_RADIUS : DEFAULT_RADIUS);
		radiusList.add(DEFAULT_RADIUS);
		radiusList.add(counts.get(dc) == 0 ? SUBSET_RADIUS : DEFAULT_RADIUS);

		if (counts.get(da) > 0)
			labels.add(new Label(ax - 0.3, 0, String.valueOf(counts.get(da))));

		labels.add(new Label(xStart + 0.5, 0, String.valueOf(counts.get(dab))));
		labels.add(new Label(xStart + 1, 0, String.valueOf(counts.get(db))));
		labels.add(new Label(xStart + 1.5, 0, String.valueOf(counts.get(dbc))));

		if (counts.get(dc) > 0)
			labels.add(new Label(cx, 0, String.valueOf(counts.get(dc))));

		labels.add(new Label(ax - 0.3, -0.8, cluster.get(a - 1).getName()));
		labels.add(new Label(xStart + 1, -1, cluster.get(b - 1).getName()));
		labels.add(new Label(cx, -0.8, cluster.get(c - 1).getName()));

		// TODO: add subsetting for datasets entirely contained within another

		return datasetPos;
	}

	/**
	 * Add a new analysis dataset to this charting dataset
	 * 
	 * @param dataset
	 */
	private void addDataset(IAnalysisDataset dataset) {

		// Always declare a new cluster for first entry
		if (clusters.isEmpty()) {
			clusters.put("Cluster_" + clusters.size(), new ArrayList<>(Arrays.asList(dataset)));
			return;
		}

		// Check if we can add the dataset to an existing cluster
		boolean wasAdded = false;
		for (List<IAnalysisDataset> cluster : clusters.values()) {
			boolean addToCluster = cluster.stream()
					.anyMatch(d -> d.getCollection().countShared(dataset) > 0);

			if (addToCluster) {
				cluster.add(dataset);
				wasAdded = true;
				break; // only add to the first cluster with matches
			}
		}

		// If not, make a new cluster
		if (!wasAdded) {
			clusters.put("Cluster_" + clusters.size(), new ArrayList<>(Arrays.asList(dataset)));
		}

		// Check if we can collapse any clusters with the latest addition
		collapseClusters();
	}

	private void createSeries() {
		int xStart = 0;

		for (Entry<Comparable<?>, List<IAnalysisDataset>> entry : clusters.entrySet()) {
			List<IAnalysisDataset> datasets = entry.getValue();

			Map<String, Integer> counts = calculateCounts(datasets);

			createCentroids(counts, datasets, xStart, entry.getKey());

			xStart += X_OFFSET;
		}

		// Create sentinal points to allow aspect ratio scaling of chart without
		// clipping annotated Venn circle outlines. These are points outside the range
		// of the circles.
		Number xVal = DatasetUtils.findMaximumDomainValue(this);
		double xMax = xVal == null ? 1.2 : xVal.doubleValue() + 1.2;

		double[][] sentinals = new double[2][2];
		sentinals[0] = new double[] { SENTINAL_X_MIN, xMax };
		sentinals[1] = new double[] { SENTINAL_Y_MIN, SENTINAL_Y_MAX };
		addSeries("Sentinals", sentinals);
	}

	/**
	 * Identify clusters with shared nuclei and collapse them
	 * 
	 */
	private void collapseClusters() {

		if (clusters.size() == 1) {
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
						d -> entry2.getValue().stream()
								.anyMatch(d2 -> d.getCollection().countShared(d2) > 0));

				if (matchFound) {
					entry1.getValue().addAll(entry2.getValue());
					entry2.getValue().clear();
					includeInFinal.put(entry2.getKey(), false);
				}

				includeInFinal.putIfAbsent(entry1.getKey(), true);

			}
		}

		// Remove entry2 from consideration if absorbed into entry1
		for (Entry<Comparable<?>, Boolean> entry : includeInFinal.entrySet()) {
			if (entry.getValue()) {
				replacementClusters.put("Cluster_" + replacementClusters.size(),
						clusters.get(entry.getKey()));
			}
		}

		clusters.clear();
		clusters.putAll(replacementClusters);
	}
}
