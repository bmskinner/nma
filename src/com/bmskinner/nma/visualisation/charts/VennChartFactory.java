package com.bmskinner.nma.visualisation.charts;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.visualisation.ChartComponents;
import com.bmskinner.nma.visualisation.datasets.VennChartDataset;
import com.bmskinner.nma.visualisation.datasets.VennChartDataset.Label;
import com.bmskinner.nma.visualisation.options.ChartOptions;

public class VennChartFactory extends AbstractChartFactory {

	private static final Logger LOGGER = Logger.getLogger(VennChartFactory.class.getName());

	public VennChartFactory(@NonNull ChartOptions o) {
		super(o);
	}

	public JFreeChart makeVennChart() {
		if (!options.hasDatasets())
			return createEmptyChart();

		try {

			VennChartDataset d = new VennChartDataset(options.getDatasets());

			if (!d.isValid())
				return createTextAnnotatedEmptyChart(
						"Cannot display more than four overlapping datasets");

			JFreeChart chart = ChartFactory.createScatterPlot(null, null, null, d,
					PlotOrientation.VERTICAL,
					DEFAULT_CREATE_LEGEND, DEFAULT_CREATE_TOOLTIPS, DEFAULT_CREATE_URLS);

			XYPlot plot = chart.getXYPlot();
			plot.setBackgroundPaint(Color.WHITE);

			// Hide the points
			XYLineAndShapeRenderer rend = new XYLineAndShapeRenderer();
			rend.setDefaultLinesVisible(false);
			rend.setDefaultShapesVisible(false);
			rend.setDefaultSeriesVisibleInLegend(false);
			rend.setUseFillPaint(false);
			plot.setRenderer(rend);

			// Draw filled circles at the series item locations
			List<IAnalysisDataset> allDatasets = options.getDatasets();

			for (int series = 0; series < d.getSeriesCount(); series++) {
				if (d.getSeriesKey(series).equals("Sentinals")) // no annotations of these points
					continue;

				List<IAnalysisDataset> datasets = d.getDatasets(d.getSeriesKey(series));

				// Add each of the venn circles
				for (int item = 0; item < d.getItemCount(series); item++) {

					// Check the colour of the dataset from the original selection
					IAnalysisDataset dataset = datasets.get(item);
					int colourIndex = allDatasets.indexOf(dataset);
					Color datasetColour = dataset.getDatasetColour()
							.orElse(ColourSelecter.getColor(colourIndex));

					double x = d.getXValue(series, item);
					double y = d.getYValue(series, item);
					double xRadius = d.getXRadius(series, item);
					double yRadius = d.getYRadius(series, item);

					// Draw a filled shape as backround
					XYShapeAnnotation a = new XYShapeAnnotation(
							new Ellipse2D.Double(x - xRadius, y - yRadius, xRadius + xRadius,
									yRadius + yRadius),
							null, null,
							ColourSelecter.makeTransparent(datasetColour, 30));
					plot.addAnnotation(a);
				}

			}

			// Draw the strokes above the transparent fills
			for (int series = 0; series < d.getSeriesCount(); series++) {
				if (d.getSeriesKey(series).equals("Sentinals")) // no annotations of these points
					continue;

				List<IAnalysisDataset> datasets = d.getDatasets(d.getSeriesKey(series));

				// Add each of the venn circles
				for (int item = 0; item < d.getItemCount(series); item++) {

					// Check the colour of the dataset from the original selection
					IAnalysisDataset dataset = datasets.get(item);
					int colourIndex = allDatasets.indexOf(dataset);
					Color datasetColour = dataset.getDatasetColour()
							.orElse(ColourSelecter.getColor(colourIndex));

					double x = d.getXValue(series, item);
					double y = d.getYValue(series, item);
					double xRadius = d.getXRadius(series, item);
					double yRadius = d.getYRadius(series, item);

					// Draw an unfilled circle
					XYShapeAnnotation a = new XYShapeAnnotation(
							new Ellipse2D.Double(x - xRadius, y - yRadius, xRadius + xRadius,
									yRadius + yRadius),
							ChartComponents.MARKER_STROKE, datasetColour);
					plot.addAnnotation(a);
				}

			}

			// Add shared counts and labels
			for (Label a : d.getLabels()) {
				plot.addAnnotation(new XYTextAnnotation(a.label(), a.x(), a.y()));
			}

			applyDefaultAxisOptions(chart);
			return chart;

		} catch (Exception e) {
			return createErrorChart();
		}
	}

}
