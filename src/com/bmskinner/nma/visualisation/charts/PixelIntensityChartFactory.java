package com.bmskinner.nma.visualisation.charts;

import java.awt.Color;
import java.awt.Paint;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.Range;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.visualisation.ChartComponents;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.datasets.ExportableBoxAndWhiskerCategoryDataset;
import com.bmskinner.nma.visualisation.datasets.PixelIntensityDatasetCreator;
import com.bmskinner.nma.visualisation.options.ChartOptions;

/**
 * Create charts covering pixel intensities within cells.
 * 
 */
public class PixelIntensityChartFactory extends AbstractChartFactory {

	private static final Logger LOGGER = Logger.getLogger(PixelIntensityChartFactory.class.getName());

	public PixelIntensityChartFactory(@NonNull ChartOptions o) {
		super(o);
	}

	/**
	 * Create a chart that summarises the total pixel intensities in each cell via a
	 * log2ratio CGH plot
	 * 
	 * @return
	 */
	public JFreeChart createPixelIntensityHistogram() {

		if (!options.hasDatasets())
			return createEmptyChart();

		try {

			final ExportableBoxAndWhiskerCategoryDataset ds = new PixelIntensityDatasetCreator(options)
					.createNucleusCGHDataset();

//			final ViolinRenderer renderer = new ViolinRenderer();
			final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
			renderer.setMeanVisible(false);
			renderer.setWhiskerWidth(0.05);
			renderer.setUseOutlinePaintForWhiskers(true);
			renderer.setItemMargin(0.05);
			renderer.setMaximumBarWidth(0.2);
			renderer.setDefaultOutlinePaint(Color.BLACK);
			renderer.setDefaultPaint(Color.LIGHT_GRAY);
//			final JFreeChart chart = ViolinChartFactory.createViolinChart(null, null,
//			null, ds, false);
//			final CategoryPlot plot = chart.getCategoryPlot();
			final NumberAxis valueAxis = new NumberAxis("Log2(Total pixel intensity ratio)");
			valueAxis.setAutoRangeIncludesZero(true);
			valueAxis.setDefaultAutoRange(new Range(-10, 10));
			final CategoryAxis categoryAxis = new CategoryAxis(null);
			final CategoryPlot plot = new CategoryPlot(ds, categoryAxis, valueAxis, renderer);
			final JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);

			plot.getDomainAxis().setCategoryMargin(0.10);



			plot.setRangeAxis(valueAxis);
			plot.setDomainGridlinesVisible(false);
			plot.setRangeGridlinesVisible(true);
			plot.addRangeMarker(ChartComponents.ZERO_MARKER);

			for (int row = 0; row < ds.getRowCount(); row++) {

				final IAnalysisDataset d = options.getDatasets().get(row);
				final Paint color = d.getDatasetColour().orElse(ColourSelecter.getColor(row));
				renderer.setSeriesPaint(row, color);
			}

//			if (ds.hasProbabilities() && ds.getProbabiltyRange().getLength() > 0) {
//				plot.getRangeAxis().setRange(ds.getProbabiltyRange());
//			}

			return chart;
		} catch (final ChartDatasetCreationException | IllegalArgumentException e) {
			LOGGER.log(Level.SEVERE, "Error making pixel intensity chart: %s".formatted(e.getMessage()), e);
			return createErrorChart();
		}
	}

}
