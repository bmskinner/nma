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
import com.bmskinner.nma.visualisation.options.ChartOptions;

public class VennChartFactory extends AbstractChartFactory {

	private static final Logger LOGGER = Logger.getLogger(VennChartFactory.class.getName());

	public VennChartFactory(@NonNull ChartOptions o) {
		super(o);
	}

	public JFreeChart makeVennChart() {
		if (!options.hasDatasets())
			return createEmptyChart();

		VennChartDataset d = new VennChartDataset(options.getDatasets());
		
		LOGGER.info("Created venn chart with " + d.getSeriesCount() + " series");

		JFreeChart chart = ChartFactory.createScatterPlot(null, null, null, d, PlotOrientation.VERTICAL,
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

		// Draw annotated circles at the series item locations
		double radius = 0.7;
		for (int series = 0; series < d.getSeriesCount(); series++) {

			List<IAnalysisDataset> datasets = d.getDatasets(d.getSeriesKey(series));

			// Add each of the venn circles
			for(int item = 0; item<d.getItemCount(series); item++) {
				double x = d.getXValue(series, item);
				double y = d.getYValue(series, item);

				XYShapeAnnotation a = new XYShapeAnnotation(
						new Ellipse2D.Double(x - radius, y - radius, radius + radius, radius + radius),
						ChartComponents.MARKER_STROKE, ColourSelecter.getColor(item));
				plot.addAnnotation(a);

				double nameY = y == 0 ? -1 : 1.5;
				XYTextAnnotation t = new XYTextAnnotation(datasets.get(item).getName(), x, nameY);
				plot.addAnnotation(t);
			}

			// TODO: Add the shared counts to predetermined locations according to the
			// number of
			// datasets in the cluster
		}

		// Constrain plot to visible range
		plot.getDomainAxis().setRange(-1.4, d.getMaxDomainValue());
		plot.getRangeAxis().setRange(-2, 2);

		applyDefaultAxisOptions(chart);
		return chart;
	}

}
