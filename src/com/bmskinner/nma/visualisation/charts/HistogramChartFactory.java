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
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nma.visualisation.ChartComponents;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.datasets.HistogramDatasetCreator;
import com.bmskinner.nma.visualisation.datasets.NuclearHistogramDatasetCreator;
import com.bmskinner.nma.visualisation.options.ChartOptions;

/**
 * Methods for creating histograms from chart options
 * 
 * @author Ben Skinner
 *
 */
public class HistogramChartFactory extends AbstractChartFactory {

	private static final Logger LOGGER = Logger.getLogger(HistogramChartFactory.class.getName());

	private static final boolean HISTOGRAM_CREATE_LEGEND = true;
	private static final boolean HISTOGRAM_CREATE_TOOLTIP = true;

	public HistogramChartFactory(@NonNull ChartOptions o) {
		super(o);
	}

	/**
	 * Create an empty histogram
	 * 
	 * @return a chart with no data
	 */
	private static JFreeChart createEmptyHistogram() {

		final JFreeChart chart = ChartFactory.createHistogram(null, null, null, null, PlotOrientation.VERTICAL,
				HISTOGRAM_CREATE_LEGEND, HISTOGRAM_CREATE_TOOLTIP, DEFAULT_CREATE_URLS);

		final XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.white);
		final XYBarRenderer rend = new XYBarRenderer();
		rend.setBarPainter(new StandardXYBarPainter());
		rend.setShadowVisible(false);
		plot.setRenderer(rend);
		return chart;
	}

	/**
	 * Create a histogram from a histogram dataset and apply basic formatting
	 * 
	 * @param ds     the dataset to use
	 * @param xLabel the label of the x axis
	 * @param yLabel the label of the y axis
	 * @return a histogram
	 */
	private static JFreeChart createHistogram(HistogramDataset ds, String xLabel, String yLabel) {

		final JFreeChart chart = ChartFactory.createHistogram(null, xLabel, yLabel, ds, PlotOrientation.VERTICAL,
				HISTOGRAM_CREATE_LEGEND, HISTOGRAM_CREATE_TOOLTIP, DEFAULT_CREATE_URLS);

		final XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.white);
		final XYBarRenderer rend = new XYBarRenderer();
		rend.setBarPainter(new StandardXYBarPainter());
		rend.setShadowVisible(false);
		plot.setRenderer(rend);
		if (ds != null && ds.getSeriesCount() > 0) {
			for (int j = 0; j < ds.getSeriesCount(); j++) {
				plot.getRenderer().setSeriesVisibleInLegend(j, false);
			}
		}
		return chart;
	}

	/**
	 * Create a histogram from a list of values
	 * 
	 * @param list
	 * @return
	 * @throws Exception
	 */
	public static JFreeChart createRandomSampleHistogram(List<Double> list) throws ChartDatasetCreationException {
		final HistogramDataset ds = HistogramDatasetCreator.createHistogramDatasetFromList(list);
		final JFreeChart chart = createHistogram(ds, "Magnitude difference between populations", "Observed instances");
		chart.getXYPlot().addDomainMarker(new ValueMarker(1, Color.BLACK, ChartComponents.MARKER_STROKE));
		return chart;
	}

	/**
	 * Create a density chart from a list of values
	 * 
	 * @param list
	 * @return
	 * @throws ChartDatasetCreationException
	 * @throws Exception
	 */
	public JFreeChart createRandomSampleDensity(List<Double> list) throws ChartDatasetCreationException {
		final XYDataset ds = new NuclearHistogramDatasetCreator(options).createDensityDatasetFromList(list, 0.0001);
		final String xLabel = "Magnitude difference between populations";
		final JFreeChart chart = ChartFactory.createXYLineChart(null, xLabel, "Probability", ds, PlotOrientation.VERTICAL,
				true, true, false);
		final XYPlot plot = chart.getXYPlot();
		for (int j = 0; j < ds.getSeriesCount(); j++) {
			plot.getRenderer().setSeriesVisibleInLegend(j, false);
		}

		plot.setBackgroundPaint(Color.WHITE);
		chart.getXYPlot().addDomainMarker(new ValueMarker(1, Color.BLACK, ChartComponents.MARKER_STROKE));

		return chart;
	}

}
