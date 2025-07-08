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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
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

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.dialogs.DimensionalityReductionPlotDialog.ColourByType;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.visualisation.charts.ScatterChartFactory.ScatterChartRenderer;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.datasets.ComponentOutlineDataset;
import com.bmskinner.nma.visualisation.datasets.ScatterChartDatasetCreator;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.image.ImageFilterer;
import com.bmskinner.nma.visualisation.options.ChartOptions;

import ij.process.ImageProcessor;

public class DimensionalityChartFactory extends AbstractChartFactory {

	/**
	 * Number of images to be loaded per batch
	 */
	private static final int BATCH_SIZE = 50;

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
			final XYDataset ds = ScatterChartDatasetCreator.createDimensionalityReductionScatterDataset(d,
					type, plotGroup,
					colourGroup);

			final boolean isUMAP = plotGroup.getOptions().get()
					.getBoolean(HashOptions.CLUSTER_USE_UMAP_KEY);
			final boolean isTsne = plotGroup.getOptions().get()
					.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY);
			final boolean isPca = plotGroup.getOptions().get()
					.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY);

			final String prefix = isUMAP ? "UMAP " : isTsne ? "t-SNE " : "PC";

			final String xLabel = prefix + "1";
			final String yLabel = prefix + "2";

			final JFreeChart chart = createBaseXYChart(xLabel, yLabel, ds);

			final XYPlot plot = chart.getXYPlot();

			final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
			yAxis.setAutoRangeIncludesZero(false);

			final XYItemRenderer renderer = new ScatterChartRenderer();
			plot.setRenderer(renderer);

			final List<UUID> clusterIds = colourGroup.getUUIDs();
			for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {

				// If we are colouring the points, use the dataset colour if set,
				// otherwise pick a sensible colour
				final IAnalysisDataset childDataset = d.getChildDataset(clusterIds.get(i));
				final Paint colour = type.equals(ColourByType.NONE) ? Color.WHITE
						: childDataset.hasDatasetColour() ? childDataset.getDatasetColour().get()
								: ColourSelecter.getColor(i);
				renderer.setSeriesPaint(i, colour);
			}

			// Add a legend
			chart.addLegend(new LegendTitle(plot));

			addClusterGroupConsensusNuclei(d, plotGroup, chart);

			return chart;
		} catch (ChartDatasetCreationException | MissingDataException
				| ComponentCreationException
				| SegmentUpdateException e) {
			LOGGER.log(Level.SEVERE, "Unable to make dimensionality reduction chart: %s".formatted(e.getMessage()), e);
			return createErrorChart();
		}
	}

	/**
	 * Draw the given nuclei on the chart
	 * 
	 * @param d
	 * @param plotGroup
	 * @param chart
	 */
	public static void addAnnotatedNucleusImages(IAnalysisDataset d, IClusterGroup plotGroup,
			JFreeChart chart,
			int maxImagePerCluster) {

		final boolean isUMAP = plotGroup.getOptions().get().getBoolean(HashOptions.CLUSTER_USE_UMAP_KEY);
		final boolean isTsne = plotGroup.getOptions().get().getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY);
		final boolean isPca = plotGroup.getOptions().get().getBoolean(HashOptions.CLUSTER_USE_PCA_KEY);

		final String prefix1 = isUMAP ? Measurement.UMAP_1.name().replace(" ", "_") + "_"
				: isTsne ? "TSNE_1_" : "PC1_";
		final String prefix2 = isUMAP ? Measurement.UMAP_2.name().replace(" ", "_") + "_"
				: isTsne ? "TSNE_2_" : "PC2_";

		// Scale the images to the dimensions of the chart
		// Large datasets should have smaller nuclei
		final Range xRange = DatasetUtils.findDomainBounds(chart.getXYPlot().getDataset());
		final Range yRange = DatasetUtils.findRangeBounds(chart.getXYPlot().getDataset());

		final double scale = Math.log10(d.getCollection().size()) * 4;

		int dataset = 0;

		// Add each cluster group nuclei
		for (final UUID id : plotGroup.getUUIDs()) {
			final int index = dataset;
			final IAnalysisDataset childDataset = d.getChildDataset(id);
			List<Nucleus> nList = new ArrayList<>();
			nList.addAll(childDataset.getCollection().getNuclei());

			final Color colour = childDataset.getDatasetColour()
					.orElse(ColourSelecter.getColor(dataset));

			// If the number of nuclei is high, there is no point drawing them all
			// so pick a random subset
			if (nList.size() > maxImagePerCluster) {
				Collections.shuffle(nList);
				nList = nList.subList(0, maxImagePerCluster);
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
	 * Find the centroid of the points in the given dataset
	 * 
	 * @param dataset
	 * @param chart
	 * @return
	 */
	private static Point2D findCentroid(int dataset, JFreeChart chart) {

		// Find the centre of mass of the cluster
		// Each cluster group is a series in the first dataset
		final int items = chart.getXYPlot().getDataset(0).getItemCount(dataset - 1);

//		double xmax = -Double.MAX_VALUE;
//		double xmin = Double.MAX_VALUE;
//		double ymax = -Double.MAX_VALUE;
//		double ymin = Double.MAX_VALUE;

		final double[] xvals = new double[items];
		final double[] yvals = new double[items];

		for (int i = 0; i < items; i++) {

			final double x = chart.getXYPlot().getDataset(0).getXValue(dataset - 1, i);
			final double y = chart.getXYPlot().getDataset(0).getYValue(dataset - 1, i);

			xvals[i] = x;
			yvals[i] = y;
//
//			xmax = x > xmax ? x : xmax;
//			xmin = x < xmin ? x : xmin;
//			ymax = y > ymax ? y : ymax;
//			ymin = y < ymin ? y : ymin;
		}

		final double xmean = DoubleStream.of(xvals).average().orElse(0);
		final double ymean = DoubleStream.of(yvals).average().orElse(0);

		return new Point2D.Double(xmean, ymean);

//		double dx = xmax - xmin;
//		double dy = ymax - ymin;
//		return new Point2D.Double(xmin + (dx / 2), ymin + (dy / 2));
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
	private static void addClusterGroupConsensusNuclei(IAnalysisDataset d, IClusterGroup plotGroup,
			JFreeChart chart)
			throws MissingLandmarkException, ComponentCreationException,
			ChartDatasetCreationException {

		// Choose a sensible scale for the consensus nuclei based on the
		// range of the plot

		final Range xRange = DatasetUtils.findDomainBounds(chart.getXYPlot().getDataset());
		final Range yRange = DatasetUtils.findRangeBounds(chart.getXYPlot().getDataset());

		final double scale = 1200 / Math.max(xRange.getLength(), yRange.getLength());
		LOGGER.fine("Domain is " + xRange.getLength() + "; Range is " + yRange.getLength()
				+ "; Scale is " + scale);

		// Calculate centroids for sorting consenusus nuclei
		final List<ConsensusCentroidLink> leftCentroids = new ArrayList<>();
		final List<ConsensusCentroidLink> rightCentroids = new ArrayList<>();
		int dataset = 1;
		for (final UUID clusterId : plotGroup.getUUIDs()) {
			final Point2D centroid = findCentroid(dataset, chart);
			final ConsensusCentroidLink ccl = new ConsensusCentroidLink(clusterId, centroid, dataset);
			if (centroid.getX() < xRange.getCentralValue()) {
				leftCentroids.add(ccl);
			} else {
				rightCentroids.add(ccl);
			}
			dataset++;
		}

		// Sort by y descending
		leftCentroids.sort(Comparator.comparingDouble(ConsensusCentroidLink::getY).reversed());
		rightCentroids.sort(Comparator.comparingDouble(ConsensusCentroidLink::getY).reversed());

		// Draw each consensus at a y location. Y values are steps of 1/n+1
		// to make even spacing
		int yOrder = 0;
		double separations = 1d / (leftCentroids.size() + 1);
		for (final ConsensusCentroidLink ccl : leftCentroids) {
			plotConsensus(d, ccl, chart, scale, yOrder, separations);
			yOrder++;
		}

		yOrder = 0;
		separations = 1d / (rightCentroids.size() + 1);
		for (final ConsensusCentroidLink ccl : rightCentroids) {
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

		final IAnalysisDataset childDataset = d.getChildDataset(ccl.datasetId());
		final Paint colour = childDataset.getDatasetColour()
				.orElse(ColourSelecter.getColor(ccl.datasetIndex() - 1));

		final Range xRange = DatasetUtils.findDomainBounds(chart.getXYPlot().getDataset());
		final Range yRange = DatasetUtils.findRangeBounds(chart.getXYPlot().getDataset());

		// Place the consensus somewhere sensible. Scale here has been chosen to reflect
		// the ranges of the plot; this should avoid making the consensus too small or
		// too large for the chart
		final Nucleus n = d.getChildDataset(ccl.datasetId()).getCollection().getConsensus();
		n.setScale(scale);

		final boolean isLeft = ccl.centroid().getX() < xRange.getCentralValue();

		final double nx = isLeft ? (xRange.getLowerBound() - xRange.getLength() * 0.15)
				: (xRange.getUpperBound() + xRange.getLength() * 0.15);
		final double fny = separations * (index + 1);
		final double ny = (yRange.getUpperBound() - (yRange.getLength() * fny));

		n.moveCentreOfMass(new FloatPoint(nx * scale, ny * scale));

		// Make the consensus dataset. Use the micron scaling to force the consensus to
		// fit the plot
		final ComponentOutlineDataset cd = new ComponentOutlineDataset(n, false,
				MeasurementScale.MICRONS);
		chart.getXYPlot().setDataset(ccl.datasetIndex(), cd);
		final DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
		renderer.setDefaultLinesVisible(true);
		renderer.setDefaultShapesVisible(false);
		renderer.setDefaultSeriesVisibleInLegend(false);

		for (int i = 0; i < cd.getSeriesCount(); i++) {
			renderer.setSeriesPaint(i, colour);
			renderer.setSeriesStroke(i, new BasicStroke(2.0f));
		}
		chart.getXYPlot().setRenderer(ccl.datasetIndex(), renderer);

		// Get the x boundary for the line
		final double xBound = isLeft
				? DatasetUtils.findDomainBounds(cd).getUpperBound() + (xRange.getLength() * 0.01)
				: DatasetUtils.findDomainBounds(cd).getLowerBound() - (xRange.getLength() * 0.01);

		// Get the y boundaries fro the line
		final Range yRangeCd = DatasetUtils.findRangeBounds(cd);

		// Draw a line from the consensus to the centroid of the cluster
		final XYLineAnnotation line = new XYLineAnnotation(ccl.centroid().getX(), ccl.centroid().getY(),
				xBound, ny,
				new BasicStroke(2.0f), colour);
		chart.getXYPlot().addAnnotation(line);

		// Make a line defining the x bound
		final XYLineAnnotation xline = new XYLineAnnotation(xBound, yRangeCd.getUpperBound(), xBound,
				yRangeCd.getLowerBound(), new BasicStroke(2.0f), colour);
		chart.getXYPlot().addAnnotation(xline);
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
		final List<XYDataImageAnnotation> anns = new ArrayList<>();
		try {
			for (final Nucleus n : list) {
				anns.add(
						createDimensionalityReductionImageAnnotation(n, prefix1 + plotGroup.getId(),
								prefix2 + plotGroup.getId(), chart.getXYPlot(), scale, col));
			}

			SwingUtilities.invokeAndWait(() -> {
				for (final XYDataImageAnnotation ann : anns) {
					chart.getXYPlot().getRenderer().addAnnotation(ann, Layer.FOREGROUND);
				}
			});
		} catch (InvocationTargetException | InterruptedException | MissingDataException
				| ComponentCreationException | SegmentUpdateException e) {
			LOGGER.log(Level.SEVERE, "Error adding annotation to chart", e);
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
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	private static XYDataImageAnnotation createDimensionalityReductionImageAnnotation(Nucleus n,
			String xStatName,
			String yStatName, XYPlot plot, double scaleFactor, Color col)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {

		final Measurement dim1 = n.getMeasurements().stream().filter(s -> s.name().equals(xStatName))
				.findFirst()
				.orElseThrow(
						() -> new IllegalArgumentException("No measurement called " + xStatName));
		final Measurement dim2 = n.getMeasurements().stream().filter(s -> s.name().equals(yStatName))
				.findFirst()
				.orElseThrow(
						() -> new IllegalArgumentException("No measurement called " + yStatName));
		final double x = n.getMeasurement(dim1);
		final double y = n.getMeasurement(dim2);

		final Range xRange = DatasetUtils.findDomainBounds(plot.getDataset());
		final Range yRange = DatasetUtils.findRangeBounds(plot.getDataset());

		final double xmax = xRange.getUpperBound();
		final double xmin = xRange.getLowerBound();
		final double ymin = yRange.getLowerBound();
		final double ymax = yRange.getUpperBound();

		ImageProcessor ip = ImageAnnotator
				.drawBorder(ImageImporter.importFullImageTo24bitGreyscale(n), n, col);

		ip = ImageFilterer.crop(ip, n);
		ip.flipVertical(); // Y axis needs inverting
		ip = ImageFilterer.orientImage(ip, n);

		BufferedImage image = ip.getBufferedImage();

		// Make the image partly transparent
		final BufferedImage tmpImg = new BufferedImage(image.getWidth(), image.getHeight(),
				BufferedImage.TYPE_INT_ARGB);

		final int borderCol = col.getRGB();

		for (int by = 0; by < image.getHeight(); by++) {
			for (int bx = 0; bx < image.getWidth(); bx++) {
				int argb = image.getRGB(bx, by);
				if (argb == borderCol) { // ignore pixels that are part of the nucleus outline
					final int alpha = 255; // set full opaque
					argb &= 0x00ffffff; // remove old alpha info
					argb |= (alpha << 24); // add new alpha info

				} else {
					final int blue = (argb >> 8) & 0xff;// isolate green channel from ARGB
					final int alpha = 255 - blue; // make alpha vary with blue intensity (RGB greyscale,
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
		// Note that the coordinates we draw on are a rectangle within the min and max
		// range of the data, so set the aspect ratio manually
		final double aspect = xRange.getLength() / yRange.getLength();

		final double xr = (((xmax - xmin) / scaleFactor)) / aspect;
		final double yr = ((ymax - ymin) / scaleFactor);
		final double xrh = xr / 2;
		final double yrh = yr / 2;

		return new XYDataImageAnnotation(image, x - xrh, y - yrh, xr, yr, true);

	}
}
