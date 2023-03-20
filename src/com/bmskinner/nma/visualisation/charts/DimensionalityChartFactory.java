package com.bmskinner.nma.visualisation.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import javax.swing.SwingUtilities;

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
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.charts.ScatterChartFactory.ScatterChartRenderer;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.datasets.ComponentOutlineDataset;
import com.bmskinner.nma.visualisation.datasets.ScatterChartDatasetCreator;
import com.bmskinner.nma.visualisation.image.AbstractImageFilterer;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.options.ChartOptions;

import ij.process.ImageProcessor;

public class DimensionalityChartFactory extends AbstractChartFactory {

	/**
	 * Number of images to be loaded per batch
	 */
	private static final int BATCH_SIZE = 50;
	private static final int MAX_NUCLEI_PER_CLUSTER = 200;

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

			List<UUID> clusterIds = colourGroup.getUUIDs();
			for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {

				// If we are colouring the points, use the dataset colour if set,
				// otherwise pick a sensible colour
				IAnalysisDataset childDataset = d.getChildDataset(clusterIds.get(i));
				Paint colour = type.equals(ColourByType.NONE) ? Color.WHITE
						: childDataset.hasDatasetColour() ? childDataset.getDatasetColour().get()
								: ColourSelecter.getColor(i);
				renderer.setSeriesPaint(i, colour);
			}

			// Add a legend
			chart.addLegend(new LegendTitle(plot));

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

		double xmax = -Double.MAX_VALUE;
		double xmin = Double.MAX_VALUE;
		double ymax = -Double.MAX_VALUE;
		double ymin = Double.MAX_VALUE;

		for (int i = 0; i < items; i++) {

			double x = chart.getXYPlot().getDataset(0).getXValue(dataset - 1, i);
			double y = chart.getXYPlot().getDataset(0).getYValue(dataset - 1, i);

			xmax = x > xmax ? x : xmax;
			xmin = x < xmin ? x : xmin;
			ymax = y > ymax ? y : ymax;
			ymin = y < ymin ? y : ymin;
		}

		double dx = xmax - xmin;
		double dy = ymax - ymin;
		return new Point2D.Double(xmin + (dx / 2), ymin + (dy / 2));
	}

	private record ConsensusCentroidLink(UUID datasetId, Point2D centroid, int datasetIndex) {
		double getY() {
			return centroid.getY();
		}
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
		List<ConsensusCentroidLink> leftCentroids = new ArrayList<>();
		List<ConsensusCentroidLink> rightCentroids = new ArrayList<>();
		int dataset = 1;
		for (UUID clusterId : plotGroup.getUUIDs()) {
			Point2D centroid = findCentroid(dataset, chart);
			ConsensusCentroidLink ccl = new ConsensusCentroidLink(clusterId, centroid, dataset);
			if (centroid.getX() < xRange.getCentralValue())
				leftCentroids.add(ccl);
			else
				rightCentroids.add(ccl);
			dataset++;
		}

		// Sort by y descending
		leftCentroids.sort(Comparator.comparingDouble(ConsensusCentroidLink::getY).reversed());
		rightCentroids.sort(Comparator.comparingDouble(ConsensusCentroidLink::getY).reversed());

		// Draw each consensus at a y location. Y values are steps of 1/n+1
		// to make even spacing
		int yOrder = 0;
		double separations = 1d / (leftCentroids.size() + 1);
		for (ConsensusCentroidLink ccl : leftCentroids) {
			plotConsensus(d, ccl, chart, scale, yOrder, separations);
			yOrder++;
		}

		yOrder = 0;
		separations = 1d / (rightCentroids.size() + 1);
		for (ConsensusCentroidLink ccl : rightCentroids) {
			plotConsensus(d, ccl, chart, scale, yOrder, separations);
			yOrder++;
		}
	}

	/**
	 * Draw the consensus nucleus at an appropriate position on the chart and add a
	 * line to the cluster centroid
	 * 
	 * @param d
	 * @param ccl
	 * @param chart
	 * @param scale
	 * @param index
	 * @param separations
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 * @throws ChartDatasetCreationException
	 */
	private static void plotConsensus(IAnalysisDataset d, ConsensusCentroidLink ccl,
			JFreeChart chart, double scale,
			int index, double separations)
			throws MissingLandmarkException, ComponentCreationException,
			ChartDatasetCreationException {

		if (!d.getChildDataset(ccl.datasetId()).getCollection().hasConsensus())
			return;

		IAnalysisDataset childDataset = d.getChildDataset(ccl.datasetId());
		Paint colour = childDataset.hasDatasetColour() ? childDataset.getDatasetColour().get()
				: ColourSelecter.getColor(ccl.datasetIndex() - 1);

		Range xRange = DatasetUtils.findDomainBounds(chart.getXYPlot().getDataset());
		Range yRange = DatasetUtils.findRangeBounds(chart.getXYPlot().getDataset());

		Nucleus n = d.getChildDataset(ccl.datasetId()).getCollection().getConsensus();

		// Place the consensus somewhere sensible
		n.setScale(scale);

		boolean isLeft = ccl.centroid().getX() < xRange.getCentralValue();

		double nx = isLeft ? (xRange.getLowerBound() - xRange.getLength() * 0.15)
				: (xRange.getUpperBound() + xRange.getLength() * 0.15);
		double fny = separations * (index + 1);
		double ny = (yRange.getUpperBound() - (yRange.getLength() * fny));

		n.moveCentreOfMass(new FloatPoint(nx * scale, ny * scale));

		// Make the consensus dataset. Use the micron scaling to force the point to fit
		// the umap
		ComponentOutlineDataset cd = new ComponentOutlineDataset(n, false,
				MeasurementScale.MICRONS);
		chart.getXYPlot().setDataset(ccl.datasetIndex(), cd);
		DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
		renderer.setDefaultLinesVisible(true);
		renderer.setDefaultShapesVisible(false);
		renderer.setDefaultSeriesVisibleInLegend(false);

		for (int i = 0; i < cd.getSeriesCount(); i++) {
			renderer.setSeriesPaint(i, colour);
			renderer.setSeriesStroke(i, new BasicStroke(2.0f));
		}
		chart.getXYPlot().setRenderer(ccl.datasetIndex(), renderer);

		// Get the x boundary for the line
		double xBound = isLeft
				? DatasetUtils.findDomainBounds(cd).getUpperBound() + (xRange.getLength() * 0.01)
				: DatasetUtils.findDomainBounds(cd).getLowerBound() - (xRange.getLength() * 0.01);

		// Get the y boundaries fro the line
		Range yRangeCd = DatasetUtils.findRangeBounds(cd);

		// Draw a line from the consensus to the centroid of the cluster
		XYLineAnnotation line = new XYLineAnnotation(ccl.centroid().getX(), ccl.centroid().getY(),
				xBound, ny,
				new BasicStroke(2.0f), colour);
		chart.getXYPlot().addAnnotation(line);

		// Make a line defining the x bound
		XYLineAnnotation xline = new XYLineAnnotation(xBound, yRangeCd.getUpperBound(), xBound,
				yRangeCd.getLowerBound(),
				new BasicStroke(2.0f), colour);
		chart.getXYPlot().addAnnotation(xline);
	}

	/**
	 * Draw the given nuclei on the chart
	 * 
	 * @param d
	 * @param plotGroup
	 * @param chart
	 */
	public static void addAnnotatedNucleusImages(IAnalysisDataset d, IClusterGroup plotGroup,
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

		// Scale the images to the dimensions of the chart
		// Large datasets should have smaller nuclei
		Range xRange = DatasetUtils.findDomainBounds(chart.getXYPlot().getDataset());
		Range yRange = DatasetUtils.findRangeBounds(chart.getXYPlot().getDataset());

		double scale = Math.max(xRange.getLength(), yRange.getLength())
				* Math.log10(d.getCollection().size());

		int dataset = 0;

		// Add each cluster group nuclei
		for (UUID id : plotGroup.getUUIDs()) {
			final int index = dataset;
			IAnalysisDataset childDataset = d.getChildDataset(id);
			List<Nucleus> nList = new ArrayList<>();
			nList.addAll(childDataset.getCollection().getNuclei());

			final Color colour = childDataset.getDatasetColour()
					.orElse(ColourSelecter.getColor(dataset));

			// If the number of nuclei is high, there is no point drawing them all
			// so pick a random subset
			if (nList.size() > MAX_NUCLEI_PER_CLUSTER) {
				Collections.shuffle(nList);
				nList = nList.subList(0, MAX_NUCLEI_PER_CLUSTER);
			}

			final List<Nucleus> batchList = nList;

			// Add in batches to allow the user to see they are loading
			IntStream.range(0, (batchList.size() + BATCH_SIZE - 1) / BATCH_SIZE)
					.mapToObj(i -> batchList.subList(i * BATCH_SIZE,
							Math.min(batchList.size(), (i + 1) * BATCH_SIZE)))
					.forEach(batch -> processBatch(batch, d, plotGroup, chart, prefix1, prefix2,
							colour, scale));

			dataset++;
		}
	}

	/**
	 * Add a batch of nucleus images to the chart
	 * 
	 * @param list      the nuclei to add
	 * @param d         the dataset the nuclei belong to
	 * @param plotGroup the cluster group to plot (for colour)
	 * @param chart     the chart to add the nuclei to
	 * @param prefix1   the measurement name prefix for x axis
	 * @param prefix2   the measurement name prefix for y axis
	 * @param index     the dataset index
	 * @param scale     the nucleus scale
	 */
	private static synchronized void processBatch(List<Nucleus> list, IAnalysisDataset d,
			IClusterGroup plotGroup,
			JFreeChart chart, String prefix1, String prefix2, Color col, double scale) {

		// Disable notifications while the batch is processed
		chart.setNotify(false);
		List<XYDataImageAnnotation> anns = new ArrayList<>();

		for (Nucleus n : list) {
			anns.add(createDimensionalityReductionImageAnnotation(n, prefix1 + plotGroup.getId(),
					prefix2 + plotGroup.getId(), chart.getXYPlot(), scale,
					col));
		}

		try {
			SwingUtilities.invokeAndWait(() -> {
				for (XYDataImageAnnotation ann : anns) {
					chart.getXYPlot().getRenderer().addAnnotation(ann, Layer.FOREGROUND);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			LOGGER.log(Loggable.STACK, "Error adding annotation to chart", e);
		}

		chart.setNotify(true);
	}

	/**
	 * 
	 * 
	 * @param nuclei
	 * @param xStatName
	 * @param yStatName
	 * @return
	 */
	private static XYDataImageAnnotation createDimensionalityReductionImageAnnotation(Nucleus n,
			String xStatName,
			String yStatName, XYPlot plot, double scaleFactor, Color col) {

		Measurement dim1 = n.getMeasurements().stream().filter(s -> s.name().equals(xStatName))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"No measurement called " + xStatName));
		Measurement dim2 = n.getMeasurements().stream().filter(s -> s.name().equals(yStatName))
				.findFirst().orElseThrow(() -> new IllegalArgumentException(
						"No measurement called " + yStatName));
		double x = n.getMeasurement(dim1);
		double y = n.getMeasurement(dim2);

		Range xRange = DatasetUtils.findDomainBounds(plot.getDataset());
		Range yRange = DatasetUtils.findRangeBounds(plot.getDataset());

		double xmax = xRange.getUpperBound();
		double xmin = xRange.getLowerBound();
		double ymin = yRange.getLowerBound();
		double ymax = yRange.getUpperBound();

		ImageProcessor ip = ImageAnnotator.drawBorder(
				ImageImporter.importFullImageTo24bitGreyscale(n), n,
				col);

		ip = AbstractImageFilterer.crop(ip, n);
		ip.flipVertical(); // Y axis needs inverting
		ip = AbstractImageFilterer.orientImage(ip, n);

		BufferedImage image = ip.getBufferedImage();

		// Make the image partly transparent
		BufferedImage tmpImg = new BufferedImage(image.getWidth(), image.getHeight(),
				BufferedImage.TYPE_INT_ARGB);

		int borderCol = col.getRGB();

		for (int by = 0; by < image.getHeight(); by++) {
			for (int bx = 0; bx < image.getWidth(); bx++) {
				int argb = image.getRGB(bx, by);
				if (argb == borderCol) { // ignore pixels that are part of the nucleus outline
					int alpha = 255; // set full opaque
					argb &= 0x00ffffff; // remove old alpha info
					argb |= (alpha << 24); // add new alpha info

				} else {
					int blue = (argb >> 8) & 0xff;// isolate green channel from ARGB
					int alpha = 255 - blue; // make alpha vary with blue intensity (RGB greyscale,
											// so
											// blue should correlate
											// well)
					argb &= 0x00ffffff; // remove old alpha info
					argb |= (alpha << 24); // add new alpha info
				}

				tmpImg.setRGB(bx, by, argb);
			}
		}
		image = tmpImg;

		// the image needs to be scaled to fit in the dimensionally reduced
		// coordinates without overlapping nuclei too much
		double xr = ((xmax - xmin) / scaleFactor);
		double yr = ((ymax - ymin) / scaleFactor);
		double xrh = xr / 2;
		double yrh = yr / 2;

		return new XYDataImageAnnotation(image, x - xrh, y - yrh, xr,
				yr, true);

	}
}
