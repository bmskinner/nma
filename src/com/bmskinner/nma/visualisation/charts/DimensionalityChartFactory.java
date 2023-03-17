package com.bmskinner.nma.visualisation.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYDataImageAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.Layer;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.dialogs.DimensionalityReductionPlotDialog.ColourByType;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.stats.Stats;
import com.bmskinner.nma.visualisation.charts.ScatterChartFactory.ScatterChartRenderer;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.datasets.ComponentOutlineDataset;
import com.bmskinner.nma.visualisation.datasets.ScatterChartDatasetCreator;
import com.bmskinner.nma.visualisation.image.AbstractImageFilterer;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.options.ChartOptions;

import ij.process.ImageProcessor;

public class DimensionalityChartFactory extends AbstractChartFactory {

	private static final Logger LOGGER = Logger
			.getLogger(DimensionalityChartFactory.class.getName());

	/**
	 * Create with options describing the chart to be built
	 * 
	 * @param o
	 */
	public DimensionalityChartFactory(@NonNull ChartOptions o) {
		super(o);
	}

	/**
	 * Temporary method to create tSNE plots
	 * 
	 * @param r
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public static JFreeChart createDimensionalityReductionChart(IAnalysisDataset d,
			ColourByType type,
			IClusterGroup plotGroup, IClusterGroup colourGroup) {

		try {
			XYDataset ds = ScatterChartDatasetCreator.createDimensionalityReductionScatterDataset(d,
					type, plotGroup,
					colourGroup);

			boolean isUMAP = plotGroup.getOptions().get()
					.getBoolean(HashOptions.CLUSTER_USE_UMAP_KEY);
			boolean isTsne = plotGroup.getOptions().get()
					.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY);
			boolean isPca = plotGroup.getOptions().get()
					.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY);

			String prefix = isUMAP ? "UMAP " : isTsne ? "t-SNE " : "PC";

			String xLabel = prefix + "1";
			String yLabel = prefix + "2";

			JFreeChart chart = createBaseXYChart(xLabel, yLabel, ds);

			XYPlot plot = chart.getXYPlot();

			NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
			yAxis.setAutoRangeIncludesZero(false);

			XYItemRenderer renderer = new ScatterChartRenderer();
			plot.setRenderer(renderer);

			for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
				Paint colour = ColourSelecter.getColor(i);
				renderer.setSeriesPaint(i, colour);
			}

			// Add a legend
			chart.addLegend(new LegendTitle(plot));

			if (d.getCollection().size() < 5000) { // otherwise it will get too crowded
				LOGGER.fine("Adding annotated nucleus images");
				addAnnotatedNucleusImages(d, plotGroup, chart);
				LOGGER.fine("Annotated nucleus images complete");
			}

			addClusterGroupConsensusNuclei(d, plotGroup, chart);

			return chart;
		} catch (ChartDatasetCreationException | MissingLandmarkException
				| ComponentCreationException e) {
			return createErrorChart();
		}
	}

	/**
	 * Find the centroid of the points in the given dataset
	 * 
	 * @param dataset
	 * @param chart
	 * @return
	 */
	private static Point2D findCentroid(int dataset, JFreeChart chart) {

		// Find the centroid of the cluster
		// Each cluster group is a series in the first dataset
		int items = chart.getXYPlot().getDataset(0).getItemCount(dataset - 1);
		double[] xvals = new double[items];
		double[] yvals = new double[items];
		for (int i = 0; i < items; i++) {
			xvals[i] = chart.getXYPlot().getDataset(0).getXValue(dataset - 1, i);
			yvals[i] = chart.getXYPlot().getDataset(0).getYValue(dataset - 1, i);
		}
		double xcent = Stats.quartile(xvals, Stats.MEDIAN);
		double ycent = Stats.quartile(yvals, Stats.MEDIAN);

		return new Point2D.Double(xcent, ycent);
	}

	/**
	 * Add the consenusus nuclei of the clusters
	 * 
	 * @param d
	 * @param plotGroup
	 * @param chart
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 * @throws ChartDatasetCreationException
	 */
	private static void addClusterGroupConsensusNuclei(IAnalysisDataset d,
			IClusterGroup plotGroup, JFreeChart chart)
			throws MissingLandmarkException, ComponentCreationException,
			ChartDatasetCreationException {

		// Choose a sensible scale for the consensus nuclei based on the
		// range of the plot

		Range xRange = DatasetUtils.findDomainBounds(chart.getXYPlot().getDataset());
		Range yRange = DatasetUtils.findRangeBounds(chart.getXYPlot().getDataset());

		double scale = 1200 / Math.max(xRange.getLength(), yRange.getLength());

		// Calculate centroids for sorting consenusus nuclei
		Map<UUID, Point2D> clusterCentroids = new HashMap<>();
		int dataset = 1;
		for (UUID clusterId : plotGroup.getUUIDs()) {
			clusterCentroids.put(clusterId, findCentroid(dataset++, chart));
		}

		dataset = 1;
		for (UUID clusterId : plotGroup.getUUIDs()) {

			Point2D centroid = clusterCentroids.get(clusterId);

			Nucleus n = d.getChildDataset(clusterId).getCollection().getConsensus();

			// Place the consensus somewhere sensible
			n.setScale(scale);

			double nx = centroid.getX() < xRange.getCentralValue()
					? (xRange.getLowerBound() - xRange.getLength() * 0.1)
					: (xRange.getUpperBound() + xRange.getLength() * 0.1);
			double ny = (yRange.getUpperBound()
					- dataset * (yRange.getLength() * (1d / plotGroup.size())));

			n.moveCentreOfMass(new FloatPoint(nx * scale, ny * scale));

			// Make the consensus dataset. Use the micron scaling to force the point to fit
			// the umap
			ComponentOutlineDataset cd = new ComponentOutlineDataset(n, false,
					MeasurementScale.MICRONS);

			chart.getXYPlot().setDataset(dataset, cd);
			DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
			renderer.setDefaultLinesVisible(true);
			renderer.setDefaultShapesVisible(false);
			renderer.setDefaultSeriesVisibleInLegend(false);

			for (int i = 0; i < cd.getSeriesCount(); i++) {
				renderer.setSeriesPaint(i, ColourSelecter.getColor(dataset - 1));
				renderer.setSeriesStroke(i, new BasicStroke(2.0f));
			}
			chart.getXYPlot().setRenderer(dataset, renderer);

			// Draw a line from the consensus to the centroid of the cluster
			XYLineAnnotation line = new XYLineAnnotation(centroid.getX(), centroid.getY(),
					nx, ny, new BasicStroke(2.0f), ColourSelecter.getColor(dataset - 1));
			chart.getXYPlot().addAnnotation(line);
			dataset++;
		}
	}

	private static void addAnnotatedNucleusImages(IAnalysisDataset d, IClusterGroup plotGroup,
			JFreeChart chart) {

		boolean isUMAP = plotGroup.getOptions().get()
				.getBoolean(HashOptions.CLUSTER_USE_UMAP_KEY);
		boolean isTsne = plotGroup.getOptions().get()
				.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY);
		boolean isPca = plotGroup.getOptions().get()
				.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY);

		String prefix1 = isUMAP ? Measurement.UMAP_1.name().replace(" ", "_") + "_"
				: isTsne ? "TSNE_1_" : "PC1_";
		String prefix2 = isUMAP ? Measurement.UMAP_2.name().replace(" ", "_") + "_"
				: isTsne ? "TSNE_2_" : "PC2_";

		XYPlot plot = chart.getXYPlot();

		double scale = Math.sqrt(d.getCollection().size());
		d.getCollection().getNuclei().parallelStream().forEach(n -> {
			createDimensionalityReductionImageAnnotation(n, prefix1 + plotGroup.getId(),
					prefix2 + plotGroup.getId(), plot, scale);
		});

	}

	/**
	 * 
	 * 
	 * @param nuclei
	 * @param xStatName
	 * @param yStatName
	 * @return
	 */
	private static void createDimensionalityReductionImageAnnotation(Nucleus n,
			String xStatName,
			String yStatName, XYPlot plot, double scaleFactor) {

		Measurement dim1 = n.getMeasurements().stream().filter(s -> s.name().equals(xStatName))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"No measurement called " + xStatName));
		Measurement dim2 = n.getMeasurements().stream().filter(s -> s.name().equals(yStatName))
				.findFirst().orElseThrow(() -> new IllegalArgumentException(
						"No measurement called " + yStatName));
		double x = n.getMeasurement(dim1);
		double y = n.getMeasurement(dim2);

		double xmax = plot.getDomainAxis().getRange().getUpperBound();
		double xmin = plot.getDomainAxis().getRange().getLowerBound();
		double ymin = plot.getRangeAxis().getRange().getLowerBound();
		double ymax = plot.getRangeAxis().getRange().getUpperBound();

		ImageProcessor ip = ImageAnnotator.drawBorder(
				ImageImporter.importFullImageTo24bitGreyscale(n), n,
				Color.ORANGE);

		ip = AbstractImageFilterer.crop(ip, n);
		ip.flipVertical(); // Y axis needs inverting
		ip = AbstractImageFilterer.orientImage(ip, n);

		BufferedImage image = ip.getBufferedImage();

		// Make the image partly transparent
		BufferedImage tmpImg = new BufferedImage(image.getWidth(), image.getHeight(),
				BufferedImage.TYPE_INT_ARGB);

		for (int by = 0; by < image.getHeight(); by++) {
			for (int bx = 0; bx < image.getWidth(); bx++) {
				int argb = image.getRGB(bx, by);
				int blue = argb & 0xff;// isolate blue channel from ARGB
				int alpha = 255 - blue; // make alpha vary with blue intensity (RGB greyscale, so
										// blue should correlate
										// well)
				argb &= 0x00ffffff; // remove old alpha info
				argb |= (alpha << 24); // add new alpha info

				tmpImg.setRGB(bx, by, argb);
			}
		}
		image = tmpImg;

		// Scale to the dimensionally reduced coordinates
		int iw = image.getWidth();
		int ih = image.getHeight();
		double aspect = (double) iw / ih;

		// allow each image to be at most 1/20 of image
		double xr = ((xmax - xmin) / scaleFactor) * aspect;
		double yr = ((ymax - ymin) / scaleFactor);
		double xrh = xr / 2;
		double yrh = yr / 2;

		XYDataImageAnnotation ann = new XYDataImageAnnotation(image, x - xrh, y - yrh, xr,
				yr, true);
		plot.getRenderer().addAnnotation(ann, Layer.FOREGROUND);

	}
}
