package com.bmskinner.nma.visualisation.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYDataImageAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.Layer;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nma.analysis.classification.DimensionalityReductionMethod;
import com.bmskinner.nma.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.dialogs.DimensionalityReductionPlotDialog.ColourByType;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.stats.Stats;
import com.bmskinner.nma.visualisation.charts.ScatterChartFactory.ScatterChartRenderer;
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
	 * Create dimensionality reduction plots with a given colouring scheme. A plot
	 * must have a cluster group that identifies cell locations. The colouring
	 * scheme may be based on a cluster group or on merge sources if the dataset is
	 * merged.
	 * 
	 * @param d           the dataset to plot
	 * @param type        the colour scheme to apply
	 * @param plotGroup   the cluster group for cell point locations
	 * @param colourGroup the cluster group for cell point colours
	 * @return
	 */
	public static JFreeChart createDimensionalityReductionChart(@NonNull IAnalysisDataset d,
			@NonNull ColourByType type, @NonNull IClusterGroup plotGroup, @Nullable IClusterGroup colourGroup) {

		try {
			final XYDataset ds = ScatterChartDatasetCreator.createDimensionalityReductionScatterDataset(d,
					type, plotGroup, colourGroup);

			final DimensionalityReductionMethod method = DimensionalityReductionMethod
					.fromClusterGroupOptions(plotGroup.getOptions().get());

			final String prefix = method.name();

			final String xLabel = prefix + "1";
			final String yLabel = prefix + "2";

			final JFreeChart chart = createBaseXYChart(xLabel, yLabel, ds);

			final XYPlot plot = chart.getXYPlot();

			final NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
			yAxis.setAutoRangeIncludesZero(false);

			final XYItemRenderer renderer = new ScatterChartRenderer();
			plot.setRenderer(renderer);

			// Set the series colours
			if (type.equals(ColourByType.MERGE_SOURCE)) {
				final UUID[] mergeIds = d.getMergeSourceIDs().toArray(new UUID[0]);
				for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {

					// Use the dataset colour if set, otherwise pick a sensible colour
					final IAnalysisDataset mergeSource = d.getMergeSource(mergeIds[i]);
					final Paint colour = mergeSource.getDatasetColour().orElse(ColourSelecter.getColor(i));
					renderer.setSeriesPaint(i, colour);
				}
			}

			if (type.equals(ColourByType.CLUSTER)) {
				final List<UUID> clusterIds = colourGroup.getUUIDs();
				for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {

					// Use the dataset colour if set, otherwise pick a sensible colour
					final IAnalysisDataset childDataset = d.getChildDataset(clusterIds.get(i));
					final Paint colour = childDataset.getDatasetColour().orElse(ColourSelecter.getColor(i));
					renderer.setSeriesPaint(i, colour);
				}
			}

			if (type.equals(ColourByType.NONE)) {
				for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
					renderer.setSeriesPaint(i, Color.BLACK);
				}
			}

			// Add a legend
			chart.addLegend(new LegendTitle(plot));

			addConsensusNuclei(d, plotGroup, type, chart);

			return chart;
		} catch (final Exception e) {
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
			ColourByType type,
			JFreeChart chart,
			int maxImagePerCluster) {

		final DimensionalityReductionMethod method = DimensionalityReductionMethod
				.fromClusterGroupOptions(plotGroup.getOptions().get());

		// Choose the array measurement to use
		final Measurement measurement = switch (method) {
		case PCA -> Measurement.makePrincipalComponent(plotGroup.getId());
		case TSNE -> Measurement.makeTSNE(plotGroup.getId());
		case UMAP -> Measurement.makeUMAP(plotGroup.getId());
		case NONE -> Measurement.makeUMAP(plotGroup.getId());
		};


		final double scale = Math.log10(d.getCollection().size()) * 4;

		if (type.equals(ColourByType.MERGE_SOURCE)) {
			int dataset = 0;
			for (final IAnalysisDataset mergeSource : d.getMergeSources()) {
				List<Nucleus> nuclei = new ArrayList<>(mergeSource.getCollection().getNuclei());
				final Color colour = ColourSelecter.getColor(dataset);
				// If the number of nuclei is high, there is no point drawing them all
				// so pick a random subset
				if (nuclei.size() > maxImagePerCluster) {
					Collections.shuffle(nuclei);
					nuclei = nuclei.subList(0, maxImagePerCluster);
				}

				final List<Nucleus> batchList = nuclei;

				// Add in batches to allow the user to see they are loading
				IntStream.range(0, (batchList.size() + BATCH_SIZE - 1) / BATCH_SIZE)
						.mapToObj(i -> batchList.subList(i * BATCH_SIZE,
								Math.min(batchList.size(), (i + 1) * BATCH_SIZE)))
						.forEach(batch -> processBatch(batch, d, plotGroup, chart, measurement, 0, 1,
								colour, scale));

				dataset++;
			}
			return;
		}

		if (type.equals(ColourByType.CLUSTER) | type.equals(ColourByType.NONE)) {
			int dataset = 0;
			// Add each cluster group nuclei
			for (final UUID id : plotGroup.getUUIDs()) {
				final IAnalysisDataset childDataset = d.getChildDataset(id);
				List<Nucleus> nList = new ArrayList<>();
				nList.addAll(childDataset.getCollection().getNuclei());


				final Color colour = type.equals(ColourByType.NONE) ? Color.BLACK
						: childDataset.getDatasetColour()
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
						.forEach(batch -> processBatch(batch, d, plotGroup, chart, measurement, 0, 1,
								colour, scale));

				dataset++;
			}
		}
	}

	/**
	 * Find the centroid of the points in the given chart dataset
	 * 
	 * @param dataset
	 * @param chart
	 * @return
	 */
	private static Point2D findCentroid(int dataset, JFreeChart chart) {

		// Find the centroid of the cluster
		// Each cluster group is a series in the first dataset
		final int items = chart.getXYPlot().getDataset(0).getItemCount(dataset - 1);

		final double[] xvals = new double[items];
		final double[] yvals = new double[items];

		for (int i = 0; i < items; i++) {

			final double x = chart.getXYPlot().getDataset(0).getXValue(dataset - 1, i);
			final double y = chart.getXYPlot().getDataset(0).getYValue(dataset - 1, i);

			xvals[i] = x;
			yvals[i] = y;
		}

		final double xmedian = Stats.median(xvals);
		final double ymedian = Stats.median(yvals);

		return new Point2D.Double(xmedian, ymedian);
	}

	private record ConsensusLocation(IAnalysisDataset dataset, Point2D centroid, int datasetIndex, Color colour) {

		double x() {
			return centroid.getX();
		}
		double y() {
			return centroid.getY();
		}
	}

	/**
	 * Add the consenusus nuclei of the clusters
	 * 
	 * @param d
	 * @param plotGroup
	 * @param chart
	 * @throws Exception
	 */
	private static void addConsensusNuclei(IAnalysisDataset d, IClusterGroup plotGroup,
			ColourByType type,
			JFreeChart chart)
			throws Exception {

		// Choose a sensible scale for the consensus nuclei based on the
		// range of the plot

		final Range xRange = DatasetUtils.findDomainBounds(chart.getXYPlot().getDataset());
		final Range yRange = DatasetUtils.findRangeBounds(chart.getXYPlot().getDataset());

		final double scale = 1200 / Math.max(xRange.getLength(), yRange.getLength());
		LOGGER.finer("Domain is %s; Range is %s; Scale is %s".formatted(xRange.getLength(), yRange.getLength(), scale));

		// Calculate centroids for sorting consenusus nuclei
		final List<ConsensusLocation> leftCentroids = new ArrayList<>();
		final List<ConsensusLocation> rightCentroids = new ArrayList<>();

		// Are we showing the consensus of the cluster group or of merge sources?

		if (ColourByType.CLUSTER.equals(type)) {
			int dataset = 1;
			for (final UUID clusterId : plotGroup.getUUIDs()) {
				
				final IAnalysisDataset child = d.getChildDataset(clusterId);
				final Point2D centroid = findCentroid(dataset, chart);
				final Color colour = child.getDatasetColour().orElse(ColourSelecter.getColor(dataset - 1));
				final ConsensusLocation ccl = new ConsensusLocation(child, centroid, dataset, colour);
				if (centroid.getX() < xRange.getCentralValue()) {
					leftCentroids.add(ccl);
				} else {
					rightCentroids.add(ccl);
				}
				dataset++;
			}
		}

		if (ColourByType.MERGE_SOURCE.equals(type)) {
			int dataset = 1;

			for (final UUID mergeSourceId : d.getMergeSourceIDs()) {
				final IAnalysisDataset child = d.getMergeSource(mergeSourceId);
				final Point2D centroid = findCentroid(dataset, chart);
				final Color colour = child.getDatasetColour().orElse(ColourSelecter.getColor(dataset - 1));
				final ConsensusLocation ccl = new ConsensusLocation(child, centroid, dataset, colour);
				if (centroid.getX() < xRange.getCentralValue()) {
					leftCentroids.add(ccl);
				} else {
					rightCentroids.add(ccl);
				}
				dataset++;
			}
		}

		if (ColourByType.NONE.equals(type)) {
			int dataset = 1;

			for (final UUID clusterId : plotGroup.getUUIDs()) {
				final IAnalysisDataset child = d.getChildDataset(clusterId);
				final Point2D centroid = findCentroid(dataset, chart);
				final ConsensusLocation ccl = new ConsensusLocation(child, centroid, dataset, Color.BLACK);
				if (centroid.getX() < xRange.getCentralValue()) {
					leftCentroids.add(ccl);
				} else {
					rightCentroids.add(ccl);
				}
				dataset++;
			}
		}

		// Sort by y descending
		leftCentroids.sort(Comparator.comparingDouble(ConsensusLocation::y).reversed());
		rightCentroids.sort(Comparator.comparingDouble(ConsensusLocation::y).reversed());

		// Draw each consensus at a y location. Y values are steps of 1/n+1
		// to make even spacing
		int yOrder = 0;
		double separations = 1d / (leftCentroids.size() + 1);
		for (final ConsensusLocation ccl : leftCentroids) {
			plotConsensus(d, ccl, chart, scale, yOrder, separations);
			yOrder++;
		}

		yOrder = 0;
		separations = 1d / (rightCentroids.size() + 1);
		for (final ConsensusLocation ccl : rightCentroids) {
			plotConsensus(d, ccl, chart, scale, yOrder, separations);
			yOrder++;
		}
	}

	/**
	 * Draw the consensus nucleus at an appropriate position on the chart and add a
	 * line to the cluster centroid
	 * 
	 * @param parent      the dataset with all cells being displayed
	 * @param ccl         the centroid of the cluster being drawn
	 * @param chart
	 * @param scale
	 * @param index
	 * @param separations
	 * @throws Exception
	 */
	private static void plotConsensus(IAnalysisDataset parent, ConsensusLocation ccl,
			JFreeChart chart, double scale,
			int index, double separations)
			throws Exception {
		
		if (!ccl.dataset().getCollection().hasConsensus()) {
			LOGGER.fine("Dataset %s does not have a consensus for dimensionality chart, calculating"
					.formatted(ccl.dataset().getName()));
			new ConsensusAveragingMethod(ccl.dataset()).call();
		}

		final Range xRange = DatasetUtils.findDomainBounds(chart.getXYPlot().getDataset());
		final Range yRange = DatasetUtils.findRangeBounds(chart.getXYPlot().getDataset());

		// Place the consensus somewhere sensible. Scale here has been chosen to reflect
		// the ranges of the plot; this should avoid making the consensus too small or
		// too large for the chart
		final Nucleus n = ccl.dataset().getCollection().getConsensus();
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
			renderer.setSeriesPaint(i, ccl.colour());
			renderer.setSeriesStroke(i, new BasicStroke(2.0f));
		}
		chart.getXYPlot().setRenderer(ccl.datasetIndex(), renderer);

		// Get the x boundary for the line
		final double xBound = isLeft
				? DatasetUtils.findDomainBounds(cd).getUpperBound() + (xRange.getLength() * 0.01)
				: DatasetUtils.findDomainBounds(cd).getLowerBound() - (xRange.getLength() * 0.01);

		// Get the y boundaries for the line
		final Range yRangeCd = DatasetUtils.findRangeBounds(cd);

		// Draw a line from the consensus to the centroid of the cluster
		// First, a thick white line to give a space around the real line
		// The the narrower real line in the correct colour
		final XYLineAnnotation spacer = new XYLineAnnotation(ccl.centroid().getX(), ccl.centroid().getY(),
				xBound, ny,
				new BasicStroke(5.0f), Color.WHITE);
		final XYLineAnnotation line = new XYLineAnnotation(ccl.centroid().getX(), ccl.centroid().getY(),
				xBound, ny,
				new BasicStroke(2.0f), ccl.colour());

		final double spacerRadius = Math.min(xRange.getLength(), yRange.getLength()) / 100;
		final XYShapeAnnotation circleSpacer = new XYShapeAnnotation(
				new Ellipse2D.Double(ccl.centroid().getX() - spacerRadius,
						ccl.centroid().getY() - spacerRadius, spacerRadius + spacerRadius, spacerRadius + spacerRadius),
				null, null, Color.WHITE);

		final double circleRadius = spacerRadius * 0.75;
		final XYShapeAnnotation circle = new XYShapeAnnotation(
				new Ellipse2D.Double(ccl.centroid().getX() - circleRadius,
						ccl.centroid().getY() - circleRadius, circleRadius + circleRadius, circleRadius + circleRadius),
				null, null, ccl.colour());

		renderer.addAnnotation(circleSpacer);
		renderer.addAnnotation(spacer);
		renderer.addAnnotation(circle);
		renderer.addAnnotation(line);

		// Make a line defining the x bound
		// Does not need a spacer line, there is nothing else out here
		final XYLineAnnotation xline = new XYLineAnnotation(xBound, yRangeCd.getUpperBound(), xBound,
				yRangeCd.getLowerBound(), new BasicStroke(2.0f), ccl.colour());
		renderer.addAnnotation(xline);
	}

	/**
	 * Add a batch of nucleus images to the chart
	 * 
	 * @param list        the nuclei to add
	 * @param d           the dataset the nuclei belong to
	 * @param plotGroup   the cluster group to plot (for colour)
	 * @param chart       the chart to add the nuclei to
	 * @param measurement the array measurement to fetch
	 * @param index0      the index of the array for the x axis (0-indexed)
	 * @param index1      the index of the array for the y axis (0-indexed)
	 * @param col         the colour to draw the nuclei outlines
	 * @param scale       the nucleus scale
	 */
	private static synchronized void processBatch(List<Nucleus> list, IAnalysisDataset d,
			IClusterGroup plotGroup,
			JFreeChart chart, Measurement measurement, int index0, int index1, Color col, double scale) {

		// Disable notifications while the batch is processed
		chart.setNotify(false);

		final List<XYDataImageAnnotation> anns = new ArrayList<>();
		try {
			for (final Nucleus n : list) {
				anns.add(
						createDimensionalityReductionImageAnnotation(n, measurement, index0, index1, chart.getXYPlot(),
								scale, col));
			}

			for (final XYDataImageAnnotation ann : anns) {
				chart.getXYPlot().getRenderer().addAnnotation(ann, Layer.FOREGROUND);
			}
		} catch (MissingDataException
				| ComponentCreationException | SegmentUpdateException e) {
			LOGGER.log(Level.SEVERE, "Error adding annotation to chart", e);
		}

		chart.setNotify(true);
	}

	/**
	 * 
	 * 
	 * @param nuclei
	 * @param measurement the array measurement to fetch
	 * @param index0      the index of the array for the x axis (0-indexed)
	 * @param index1      the index of the array for the y axis (0-indexed)
	 * @param plot        the chart plot to annotate
	 * @param scaleFactor scaling to fit the plot
	 * @param col         the colour for the nucleus outline
	 * @return
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	private static XYDataImageAnnotation createDimensionalityReductionImageAnnotation(Nucleus n,
			Measurement measurement, int index0, int index1, XYPlot plot, double scaleFactor, Color col)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {

		final List<Double> values = n.getArrayMeasurement(measurement);

		final double x = values.get(index0);
		final double y = values.get(index1);

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
