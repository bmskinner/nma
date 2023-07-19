package com.bmskinner.nma.visualisation.datasets;

import java.awt.Color;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.DefaultXYDataset;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

@SuppressWarnings("serial")
public class VennChartDataset extends DefaultXYDataset {

	private static final Logger LOGGER = Logger.getLogger(VennChartDataset.class.getName());

	public record VennCircle(IAnalysisDataset dataset, double x, double y, double rx, double ry) {

		public XYShapeAnnotation toAnnotation(Color fill, Color outline,
				Stroke stroke) {
			return new XYShapeAnnotation(
					new Ellipse2D.Double(x - rx, y - ry, x + rx,
							y + ry),
					stroke, outline, fill);
		}

	}

	private static final int X_OFFSET = 4;

	// Sentinal values allow the autoscale to work even though the circles drawn are
	// outside the range of values in this dataset
	private static final double SENTINAL_X_MIN = -1.2;
	private static final double SENTINAL_Y_MIN = -1.2;
	private static final double SENTINAL_Y_MAX = 1.5;

	private static final double Y_START = 0;

	private static final double DEFAULT_RADIUS = 0.7;
	private static final double HALF_RADIUS = 0.35;
	private static final double SUBSET_RADIUS = 0.17;

	/**
	 * Store the distinct clusters of datasets with shared cells
	 */
	private Map<Comparable<?>, List<IAnalysisDataset>> clusters = new HashMap<>();

	/**
	 * Store the radii of Venn circles
	 */
	private List<VennCircle> circles = new ArrayList<>();
	private Map<Comparable<?>, List<Double>> xRadii = new HashMap<>();
	private Map<Comparable<?>, List<Double>> yRadii = new HashMap<>();

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
		return clusters.values().stream().allMatch(l -> l.size() <= 4);
	}

	public List<VennCircle> getCircles() {
		return circles;
	}

	public double getXRadius(int series, int item) {
		Comparable<?> key = this.getSeriesKey(series);
		return xRadii.get(key).get(item);
	}

	public double getYRadius(int series, int item) {
		Comparable<?> key = this.getSeriesKey(series);
		return yRadii.get(key).get(item);
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

	/**
	 * Create the Venn circle centroids for a cluster
	 * 
	 * @param counts  the number of shared cells for each dataset combination
	 * @param cluster the datasets to create centroids for
	 * @param xOffset the location of the datasets in the plot
	 */
	private void createCentroids(Map<String, Integer> counts, List<IAnalysisDataset> cluster, double xStart,
			Comparable<?> key) {

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

		if (cluster.size() == 4) {
			datasetPos = createFourDatasetCentroids(counts, cluster, xStart, key);
		}

		this.addSeries(key, datasetPos);
	}

	private double[][] createOneDatasetCentroids(Map<String, Integer> counts, List<IAnalysisDataset> cluster,
			double xStart, Comparable<?> key) {
		List<Double> xRadiusList = xRadii.computeIfAbsent(key, k -> new ArrayList<>());
		List<Double> yRadiusList = yRadii.computeIfAbsent(key, k -> new ArrayList<>());

		circles.add(new VennCircle(cluster.get(0), xStart, Y_START, DEFAULT_RADIUS, DEFAULT_RADIUS));

		double[][] datasetPos = new double[2][cluster.size()];
		datasetPos[0] = new double[] { xStart };
		datasetPos[1] = new double[] { Y_START };
		xRadiusList.add(DEFAULT_RADIUS);
		yRadiusList.add(DEFAULT_RADIUS);
		labels.add(new Label(xStart, Y_START, String.valueOf(counts.get("d1"))));
		labels.add(new Label(xStart, -1, cluster.get(0).getName()));
		return datasetPos;
	}

	private double[][] createTwoDatasetCentroids(Map<String, Integer> counts, List<IAnalysisDataset> cluster,
			double xStart, Comparable<?> key) {
		List<Double> xRadiusList = xRadii.computeIfAbsent(key, k -> new ArrayList<>());
		List<Double> yRadiusList = yRadii.computeIfAbsent(key, k -> new ArrayList<>());
		double[][] datasetPos = new double[2][cluster.size()];

		String da = "d1";
		String db = "d2";
		String dab = da + db;

		if (counts.get(da) == 0) { // a entirely within b

			datasetPos[0] = new double[] { xStart + 0.5, xStart + 1 };
			datasetPos[1] = new double[] { Y_START, Y_START };
			xRadiusList.add(SUBSET_RADIUS);
			xRadiusList.add(DEFAULT_RADIUS);
			yRadiusList.add(SUBSET_RADIUS);
			yRadiusList.add(DEFAULT_RADIUS);
			labels.add(new Label(xStart + 1.1, 0, String.valueOf(counts.get(db))));
			labels.add(new Label(xStart + 0.5, 0, String.valueOf(counts.get(dab))));
			labels.add(new Label(xStart + 1.1, -1, cluster.get(1).getName()));
			labels.add(new Label(xStart + 0.5, -0.8, cluster.get(0).getName()));

		} else if (counts.get(db) == 0) { // b entirely within a
			datasetPos[0] = new double[] { xStart, xStart + 0.5 };
			datasetPos[1] = new double[] { Y_START, Y_START };
			xRadiusList.add(DEFAULT_RADIUS);
			xRadiusList.add(SUBSET_RADIUS);
			yRadiusList.add(DEFAULT_RADIUS);
			yRadiusList.add(SUBSET_RADIUS);

			labels.add(new Label(xStart, 0, String.valueOf(counts.get(da))));
			labels.add(new Label(xStart + 0.5, 0, String.valueOf(counts.get(dab))));
			labels.add(new Label(xStart, -1, cluster.get(0).getName()));
			labels.add(new Label(xStart + 0.5, -0.8, cluster.get(1).getName()));

		} else { // some shared

			datasetPos[0] = new double[] { xStart, xStart + 1, xStart + 2 };
			datasetPos[1] = new double[] { Y_START, Y_START, Y_START };

			xRadiusList.add(DEFAULT_RADIUS);
			xRadiusList.add(DEFAULT_RADIUS);
			yRadiusList.add(DEFAULT_RADIUS);
			yRadiusList.add(DEFAULT_RADIUS);
			labels.add(new Label(xStart - 0.3, 0, String.valueOf(counts.get(da))));
			labels.add(new Label(xStart + 0.5, 0, String.valueOf(counts.get(dab))));
			labels.add(new Label(xStart + 1, 0, String.valueOf(counts.get(db))));

			labels.add(new Label(xStart - 0.3, -1, cluster.get(0).getName()));
			labels.add(new Label(xStart + 1, -1, cluster.get(1).getName()));

		}
		return datasetPos;
	}

	private double[][] createThreeDatasetCentroids(Map<String, Integer> counts, List<IAnalysisDataset> cluster,
			double xStart, Comparable<?> key) {
		List<Double> xRadiusList = xRadii.computeIfAbsent(key, k -> new ArrayList<>());
		List<Double> yRadiusList = yRadii.computeIfAbsent(key, k -> new ArrayList<>());
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
			}

		} else { // make a triangle
			xRadiusList.add(DEFAULT_RADIUS);
			xRadiusList.add(DEFAULT_RADIUS);
			xRadiusList.add(DEFAULT_RADIUS);
			yRadiusList.add(DEFAULT_RADIUS);
			yRadiusList.add(DEFAULT_RADIUS);
			yRadiusList.add(DEFAULT_RADIUS);
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

	private double[][] createFourDatasetCentroids(Map<String, Integer> counts, List<IAnalysisDataset> cluster,
			double xStart, Comparable<?> key) {
		List<Double> xRadiusList = xRadii.computeIfAbsent(key, k -> new ArrayList<>());
		List<Double> yRadiusList = yRadii.computeIfAbsent(key, k -> new ArrayList<>());
		double[][] datasetPos = new double[2][cluster.size()];

		xRadiusList.add(HALF_RADIUS);
		xRadiusList.add(HALF_RADIUS);
		xRadiusList.add(DEFAULT_RADIUS);
		xRadiusList.add(DEFAULT_RADIUS);

		yRadiusList.add(DEFAULT_RADIUS);
		yRadiusList.add(DEFAULT_RADIUS);
		yRadiusList.add(HALF_RADIUS);
		yRadiusList.add(HALF_RADIUS);

		datasetPos[0] = new double[] { xStart, xStart + 0.5, xStart + 0.5, xStart + 0.5 };
		datasetPos[1] = new double[] { Y_START, Y_START, Y_START - 0.5, Y_START };

		labels.add(new Label(xStart + 0.25, -0.25, String.valueOf(counts.get("d1d2d3d4"))));

		labels.add(new Label(xStart + 0.25, -0.4, String.valueOf(counts.get("d1d2d3"))));
		labels.add(new Label(xStart + 0.25, 0.05, String.valueOf(counts.get("d1d2d4"))));
		labels.add(new Label(xStart + 0.1, -0.25, String.valueOf(counts.get("d1d3d4"))));
		labels.add(new Label(xStart + 0.5, -0.25, String.valueOf(counts.get("d2d3d4"))));

		labels.add(new Label(xStart + 0.25, 0.38, String.valueOf(counts.get("d1d2"))));
		labels.add(new Label(xStart, -0.5, String.valueOf(counts.get("d1d3"))));
		labels.add(new Label(xStart, 0.05, String.valueOf(counts.get("d1d4"))));
		labels.add(new Label(xStart + 0.5, -0.5, String.valueOf(counts.get("d2d3"))));
		labels.add(new Label(xStart + 0.5, 0.05, String.valueOf(counts.get("d2d4"))));
		labels.add(new Label(xStart + 0.88, -0.25, String.valueOf(counts.get("d3d4"))));

		labels.add(new Label(xStart, 0.38, String.valueOf(counts.get("d1"))));
		labels.add(new Label(xStart + 0.5, 0.38, String.valueOf(counts.get("d2"))));
		labels.add(new Label(xStart + 0.88, -0.5, String.valueOf(counts.get("d3"))));
		labels.add(new Label(xStart + 1, 0.05, String.valueOf(counts.get("d4"))));

		labels.add(new Label(xStart, -1, cluster.get(0).getName()));
		labels.add(new Label(xStart + 0.5, 0.9, cluster.get(1).getName()));
		labels.add(new Label(xStart + 0.75, -1, cluster.get(2).getName()));
		labels.add(new Label(xStart + 1.2, 0.5, cluster.get(3).getName()));

		return datasetPos;
	}

	/**
	 * Layout three circles with overlaps only between 1-2 and 2-3.
	 * 
	 * @param counts
	 * @param cluster
	 * @param xStart
	 * @param key
	 * @param a       the index of the first circle
	 * @param b       the index of the second circle
	 * @param c       the index of the third circle
	 * @return
	 */
	private double[][] layoutTripleFlat(Map<String, Integer> counts, List<IAnalysisDataset> cluster, double xStart,
			Comparable<?> key, int a, int b, int c) {

		List<Double> xRadiusList = xRadii.computeIfAbsent(key, k -> new ArrayList<>());
		List<Double> yRadiusList = yRadii.computeIfAbsent(key, k -> new ArrayList<>());
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

		xRadiusList.add(counts.get(da) == 0 ? SUBSET_RADIUS : DEFAULT_RADIUS);
		xRadiusList.add(DEFAULT_RADIUS);
		xRadiusList.add(counts.get(dc) == 0 ? SUBSET_RADIUS : DEFAULT_RADIUS);

		yRadiusList.add(counts.get(da) == 0 ? SUBSET_RADIUS : DEFAULT_RADIUS);
		yRadiusList.add(DEFAULT_RADIUS);
		yRadiusList.add(counts.get(dc) == 0 ? SUBSET_RADIUS : DEFAULT_RADIUS);

		if (counts.get(da) > 0)
			labels.add(new Label(ax - 0.3, 0, String.valueOf(counts.get(da))));

		labels.add(new Label(xStart + 0.5, 0, String.valueOf(counts.get(dab))));
		labels.add(new Label(xStart + 1, 0, String.valueOf(counts.get(db))));
		labels.add(new Label(xStart + 1.5, 0, String.valueOf(counts.get(dbc))));

		if (counts.get(dc) > 0)
			labels.add(new Label(cx, 0, String.valueOf(counts.get(dc))));

		labels.add(new Label(ax, -0.8, cluster.get(a - 1).getName()));
		labels.add(new Label(xStart + 1, -1, cluster.get(b - 1).getName()));
		labels.add(new Label(cx, -0.8, cluster.get(c - 1).getName()));
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
			boolean addToCluster = cluster.stream().anyMatch(d -> d.getCollection().countShared(dataset) > 0);

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

			VennCounter vc = new VennCounter(datasets);
			Map<String, Integer> counts = vc.getCounts();

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
						d -> entry2.getValue().stream().anyMatch(d2 -> d.getCollection().countShared(d2) > 0));

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
				replacementClusters.put("Cluster_" + replacementClusters.size(), clusters.get(entry.getKey()));
			}
		}

		clusters.clear();
		clusters.putAll(replacementClusters);
	}
}
