/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.visualisation.charts;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYDataImageAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.Layer;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.dialogs.DimensionalityReductionPlotDialog.ColourByType;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.ChartComponents;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.datasets.ScatterChartDatasetCreator;
import com.bmskinner.nma.visualisation.datasets.SignalXYDataset;
import com.bmskinner.nma.visualisation.image.AbstractImageFilterer;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.options.ChartOptions;

import ij.process.ImageProcessor;

/**
 * Factory for creating scatter plots from a given options set
 * 
 * @author Ben Skinner
 *
 */
public class ScatterChartFactory extends AbstractChartFactory {

	private static final Logger LOGGER = Logger.getLogger(ScatterChartFactory.class.getName());

	/**
	 * Create with options describing the chart to be built
	 * 
	 * @param o
	 */
	public ScatterChartFactory(@NonNull ChartOptions o) {
		super(o);
	}

	/**
	 * Create a scatter plot of two nucleus statistics
	 * 
	 * @param options
	 * @return
	 */
	public JFreeChart createScatterChart(String component) {

		try {

			if (!options.hasDatasets())
				return createEmptyChart();

			if (options.getStats().size() != 2)
				return createTextAnnotatedEmptyChart("Only one variable selected");

			Measurement firstStat = options.getMeasurement();

			for (Measurement stat : options.getStats()) {
				if (!stat.getClass().equals(firstStat.getClass())) {
					LOGGER.fine("Statistic classes are different");
					return createTextAnnotatedEmptyChart("Variable classes are different");
				}
			}

			if (CellularComponent.NUCLEUS.equals(component))
				return createNucleusStatisticScatterChart();

			if (CellularComponent.NUCLEAR_SIGNAL.equals(component))
				return createSignalStatisticScatterChart();

		} catch (ChartDatasetCreationException e) {
			LOGGER.log(Loggable.STACK, e.getMessage(), e);
			return createErrorChart();
		}

		return createEmptyChart();
	}

	/**
	 * Create a scatter plot of two nucleus statistics
	 * 
	 * @return
	 */
	public JFreeChart createNucleusStatisticScatterChart() throws ChartDatasetCreationException {

		XYDataset ds = new ScatterChartDatasetCreator(options)
				.createScatterDataset(CellularComponent.NUCLEUS);

		String xLabel = options.getStat(0).label(options.getScale());
		String yLabel = options.getStat(1).label(options.getScale());

		JFreeChart chart = createBaseXYChart(xLabel, yLabel, ds);

		XYPlot plot = chart.getXYPlot();

		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setAutoRangeIncludesZero(false);

		XYItemRenderer renderer = new ScatterChartRenderer();
		plot.setRenderer(renderer);

		applySingleXYDatasetColours(plot);

		return chart;
	}

	/**
	 * Create a scatter plot of two nucleus statistics
	 * 
	 * @param options
	 * @return
	 */
	public JFreeChart createSignalStatisticScatterChart() {

		SignalXYDataset ds;
		try {
			ds = (SignalXYDataset) new ScatterChartDatasetCreator(options)
					.createScatterDataset(CellularComponent.NUCLEAR_SIGNAL);
		} catch (ChartDatasetCreationException e) {
			LOGGER.log(Loggable.STACK, "Error creating scatter dataset", e);
			return createErrorChart();
		}

		String xLabel = options.getStat(0).label(options.getScale());
		String yLabel = options.getStat(1).label(options.getScale());

		JFreeChart chart = createBaseXYChart(xLabel, yLabel, ds);

		XYPlot plot = chart.getXYPlot();

		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setAutoRangeIncludesZero(false);

		XYItemRenderer renderer = new ScatterChartRenderer();

		plot.setRenderer(renderer);

		int seriesCount = plot.getDataset().getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {

			String seriesKey = ds.getSeriesKey(i).toString();
			ds.getSignalGroup(seriesKey);

			IAnalysisDataset d = ds.getDataset(seriesKey);
			UUID id = ds.getSignalId(seriesKey);
			Optional<ISignalGroup> g = d.getCollection().getSignalGroup(id);
			if (g.isPresent()) {
				Paint colour = g.get().getGroupColour().orElse(ColourSelecter.getColor(i));
				renderer.setSeriesPaint(i, colour);
			}
		}
		return chart;
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
			return chart;
		} catch (ChartDatasetCreationException e) {
			return createErrorChart();
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
	 * TODO: Think about where to put this method
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
		BufferedImage tmpImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

		for (int by = 0; by < image.getHeight(); by++) {
			for (int bx = 0; bx < image.getWidth(); bx++) {
				int argb = image.getRGB(bx, by);
				int blue = (argb >> 0) & 0xff;// isolate blue channel from ARGB
				int alpha = 255 - blue; // make alpha vary with blue intensity (RGB greyscale, so blue should correlate
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

	/**
	 * Overrides the methods of the DefaultXYItemRenderer to use a consistent point
	 * shape and not display lines.
	 * 
	 * @author ben
	 * @since 1.13.4
	 *
	 */
	private static class ScatterChartRenderer extends DefaultXYItemRenderer {

		private static final long serialVersionUID = 1L;

		public ScatterChartRenderer() {
			super();
			setDefaultShapesVisible(true);
			setDefaultShape(ChartComponents.DEFAULT_POINT_SHAPE);
		}

		@Override
		public Boolean getSeriesLinesVisible(int series) {
			return false;
		}

		@Override
		public Boolean getSeriesVisibleInLegend(int series) {
			return false;
		}

		@Override
		public Shape getSeriesShape(int series) {
			return this.getDefaultShape();
		}

	}
}
