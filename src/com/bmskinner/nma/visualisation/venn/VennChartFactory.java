package com.bmskinner.nma.visualisation.venn;

import java.awt.Color;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.ChartComponents;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.venn.VennChartDataset.Label;

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

			// Draw circles
			List<IAnalysisDataset> allDatasets = options.getDatasets();

			for (VennShape c : d.getCircles()) {
				int colourIndex = allDatasets.indexOf(c.dataset());
				Color datasetColour = c.dataset().getDatasetColour()
						.orElse(ColourSelecter.getColor(colourIndex));

				plot.addAnnotation(c.toAnnotation(ColourSelecter.makeTransparent(datasetColour, 30),
						datasetColour,
						ChartComponents.MARKER_STROKE));
			}

			// Add shared counts and labels
			for (Label a : d.getLabels()) {
				plot.addAnnotation(new XYTextAnnotation(a.label(), a.x(), a.y()));
			}

			applyDefaultAxisOptions(chart);
			return chart;

		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error making venn chart: " + e.getMessage(), e);
			return createErrorChart();
		}
	}

}
