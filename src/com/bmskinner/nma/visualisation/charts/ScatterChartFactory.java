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

import java.awt.Paint;
import java.awt.Shape;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.ChartComponents;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.datasets.ScatterChartDatasetCreator;
import com.bmskinner.nma.visualisation.datasets.SignalXYDataset;
import com.bmskinner.nma.visualisation.options.ChartOptions;

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
		try {
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
		} catch (ChartDatasetCreationException e) {
			LOGGER.log(Level.SEVERE, "Error creating scatter dataset", e);
			return createErrorChart();
		}
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
			LOGGER.log(Level.SEVERE, "Error creating scatter dataset", e);
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
	 * Overrides the methods of the DefaultXYItemRenderer to use a consistent point
	 * shape and not display lines.
	 * 
	 * @author ben
	 * @since 1.13.4
	 *
	 */
	public static class ScatterChartRenderer extends DefaultXYItemRenderer {

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
