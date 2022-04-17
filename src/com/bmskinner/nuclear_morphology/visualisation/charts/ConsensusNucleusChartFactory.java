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
package com.bmskinner.nuclear_morphology.visualisation.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.components.mesh.Mesh;
import com.bmskinner.nuclear_morphology.components.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.visualisation.ChartComponents;
import com.bmskinner.nuclear_morphology.visualisation.datasets.AbstractDatasetCreator;
import com.bmskinner.nuclear_morphology.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.visualisation.datasets.NucleusDatasetCreator;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;

/**
 * Methods to make charts with a consensus nucleus.
 */
public class ConsensusNucleusChartFactory extends AbstractChartFactory {

	private static final Logger LOGGER = Logger.getLogger(ConsensusNucleusChartFactory.class.getName());

	private static final String MULTIPLE_DATASETS_NO_CONSENSUS_ERROR = "No consensus in dataset(s)";

	public ConsensusNucleusChartFactory(@NonNull ChartOptions o) {
		super(o);
	}

	/**
	 * Create an empty chart as a placeholder for nucleus outlines and consensus
	 * chart panels.
	 * 
	 * @return an empty chart
	 */
	public static JFreeChart createEmptyChart() {
		JFreeChart chart = AbstractChartFactory.createEmptyChart();
		chart.getXYPlot().addRangeMarker(ChartComponents.CONSENSUS_ZERO_MARKER);
		chart.getXYPlot().addDomainMarker(ChartComponents.CONSENSUS_ZERO_MARKER);
		return chart;
	}

	/**
	 * Test if any of the datasets have a consensus nucleus.
	 * 
	 * @return true if a dataset has a consensus nucleus.
	 */
	public boolean hasConsensusNucleus() {
		return options.getDatasets().stream().anyMatch(d -> d.getCollection().hasConsensus());
	}

	/**
	 * Create a consensus chart from the given dataset. Gives an empty chart if
	 * null.
	 * 
	 * @param ds the dataset
	 * @return a chart
	 */
	private JFreeChart makeConsensusChart(XYDataset ds) {
		JFreeChart chart = null;
		if (ds == null) {
			chart = createEmptyChart();
		} else {
			chart = ChartFactory.createXYLineChart(null, null, null, ds, PlotOrientation.VERTICAL,
					DEFAULT_CREATE_LEGEND, DEFAULT_CREATE_TOOLTIPS, DEFAULT_CREATE_URLS);
		}
		formatConsensusChart(chart);
		return chart;
	}

	/**
	 * Create the consensus chart for the given options.
	 * 
	 */
	public JFreeChart makeConsensusChart() {

		if (!options.hasDatasets())
			return createEmptyChart();

		if (options.isMultipleDatasets()) {
			boolean oneHasConsensus = options.getDatasets().stream().anyMatch(d -> d.getCollection().hasConsensus());
			if (oneHasConsensus)
				return makeMultipleConsensusChart();
			return createTextAnnotatedEmptyChart(MULTIPLE_DATASETS_NO_CONSENSUS_ERROR);
		}

		// Single dataset mesh chart
		if (options.isShowMesh()) {
			try {
				Mesh mesh = new DefaultMesh(options.firstDataset().getCollection().getConsensus(),
						options.getMeshSize());
				return new OutlineChartFactory(options).createMeshChart(mesh, 0.5);
			} catch (ChartCreationException | MeshCreationException | MissingLandmarkException
					| ComponentCreationException e) {
				LOGGER.log(Loggable.STACK, "Error making mesh chart", e);
				return createErrorChart();
			}
		}

		// Single dataset segment chart
		if (options.firstDataset().getCollection().hasConsensus())
			return makeSegmentedConsensusChart(options.firstDataset());

		return createEmptyChart();
	}

	/**
	 * Apply basic formatting to the chart; set the backgound colour, add the
	 * markers and set the ranges.
	 * 
	 * @param chart th chart to format
	 */
	private void formatConsensusChart(JFreeChart chart) {
		chart.getPlot().setBackgroundPaint(Color.WHITE);
		chart.getXYPlot().getDomainAxis().setVisible(false);
		chart.getXYPlot().getRangeAxis().setVisible(false);
		chart.getXYPlot().addRangeMarker(ChartComponents.CONSENSUS_ZERO_MARKER);
		chart.getXYPlot().addDomainMarker(ChartComponents.CONSENSUS_ZERO_MARKER);

		int range = 50;
		chart.getXYPlot().getDomainAxis().setRange(-range, range);
		chart.getXYPlot().getRangeAxis().setRange(-range, range);
	}

	/**
	 * Create a consenusus chart for the given nucleus collection. This chart draws
	 * the nucleus border in black. There are no IQRs or segments.
	 * 
	 * @return the consensus chart
	 */
	public JFreeChart makeNucleusBareOutlineChart() {
		CellularComponent component = options.hasComponent() ? options.getComponent().get(0) : null;

		if (component == null) {
			IAnalysisDataset dataset = options.firstDataset();

			if (!dataset.getCollection().hasConsensus())
				return createTextAnnotatedEmptyChart(MULTIPLE_DATASETS_NO_CONSENSUS_ERROR);

			try {
				component = dataset.getCollection().getConsensus();
			} catch (MissingLandmarkException | ComponentCreationException e) {
				LOGGER.log(Loggable.STACK, "Error creating outline", e);
				return createErrorChart();
			}
		}

		XYDataset ds;
		try {
			ds = new NucleusDatasetCreator(options).createBareNucleusOutline(component);
		} catch (ChartDatasetCreationException e) {
			LOGGER.log(Loggable.STACK, "Error creating boxplot", e);
			return createErrorChart();
		}
		JFreeChart chart = makeConsensusChart(ds);

		double max = getConsensusChartRange(component);

		XYPlot plot = chart.getXYPlot();

		plot.getDomainAxis().setRange(-max, max);
		plot.getRangeAxis().setRange(-max, max);

		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesStroke(i, new BasicStroke(3));
			plot.getRenderer().setSeriesPaint(i, Color.BLACK);
			plot.getRenderer().setSeriesVisibleInLegend(i, false);
		}
		return chart;
	}

	/**
	 * Create a consenusus chart for the given nucleus collection. This chart draws
	 * the nucleus border in black. There are no IQRs or segments. The OP is drawn
	 * as a blue square in series 1 of dataset 0. If you don't want this, use
	 * {@link ConsensusNucleusChartFactory#makeNucleusBareOutlineChart}
	 * 
	 * @return the consensus chart
	 */
	public JFreeChart makeNucleusOutlineChart() {

		CellularComponent component = options.hasComponent() ? options.getComponent().get(0) : null;

		if (component == null) {
			IAnalysisDataset dataset = options.firstDataset();

			if (!dataset.getCollection().hasConsensus())
				return createTextAnnotatedEmptyChart(MULTIPLE_DATASETS_NO_CONSENSUS_ERROR);

			try {
				component = dataset.getCollection().getConsensus();
			} catch (MissingLandmarkException | ComponentCreationException e) {
				LOGGER.log(Loggable.STACK, "Error creating outline", e);
				return createErrorChart();
			}
		}

		XYDataset ds;
		try {
			ds = new NucleusDatasetCreator(options).createBareNucleusOutline(component);
		} catch (ChartDatasetCreationException e) {
			LOGGER.log(Loggable.STACK, "Error creating annotated nucleus outline: " + e.getMessage(), e);
			return createErrorChart();
		}
		JFreeChart chart = makeConsensusChart(ds);

		double max = getConsensusChartRange(component);

		XYPlot plot = chart.getXYPlot();

		plot.getDomainAxis().setRange(-max, max);
		plot.getRangeAxis().setRange(-max, max);

		XYLineAndShapeRenderer rend = new XYLineAndShapeRenderer();
		rend.setSeriesLinesVisible(0, true);
		rend.setSeriesShapesVisible(0, false);

		rend.setSeriesLinesVisible(1, false);
		rend.setSeriesShapesVisible(1, true);

		rend.setSeriesVisibleInLegend(0, Boolean.FALSE);
		rend.setSeriesStroke(0, new BasicStroke(3));
		rend.setSeriesPaint(0, Color.BLACK);

		rend.setSeriesVisibleInLegend(1, Boolean.FALSE);
		rend.setSeriesPaint(1, Color.BLUE);
		plot.setRenderer(rend);
		return chart;
	}

	/**
	 * Get the maximum absolute range of the axes of the chart for the given
	 * component
	 * 
	 * @param component the component to find the range for
	 * @return the maximum range value
	 */
	private double getConsensusChartRange(CellularComponent component) {
		double maxX = Math.max(Math.abs(component.getMinX()), Math.abs(component.getMaxX()));
		double maxY = Math.max(Math.abs(component.getMinY()), Math.abs(component.getMaxY()));

		// ensure that the scales for each axis are the same
		double max = Math.max(maxX, maxY);

		// ensure there is room for expansion of the target nucleus due to IQR
		max *= 1.25;
		return max;
	}

	/**
	 * Get the maximum absolute range of the axes of the chart given the existing
	 * datasets. The minimum returned value will be 1. Checks the range for each
	 * dataset's consensus nucleus.
	 * 
	 * @return the chart maximum range value
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 */
	private double getConsensusChartRange() throws MissingLandmarkException, ComponentCreationException {

		double max = 1;
		for (IAnalysisDataset dataset : options.getDatasets()) {
			if (dataset.getCollection().hasConsensus()) {
				double datasetMax = getConsensusChartRange(dataset.getCollection().getConsensus());
				max = datasetMax > max ? datasetMax : max;
			}
		}
		return max;
	}

	/**
	 * Create a consensus nucleus chart with IQR and segments drawn on it.
	 * 
	 * @param dataset the dataset to draw
	 * @return a chart
	 */
	private JFreeChart makeSegmentedConsensusChart(@NonNull IAnalysisDataset dataset) {

		ICellCollection collection = dataset.getCollection();
		try {
			XYDataset ds = new NucleusDatasetCreator(options).createSegmentedNucleusOutline(collection);

			JFreeChart chart = makeConsensusChart(ds);
			double max = getConsensusChartRange(dataset.getCollection().getConsensus());

			XYPlot plot = chart.getXYPlot();
			plot.setDataset(0, ds);
			plot.getDomainAxis().setRange(-max, max);
			plot.getRangeAxis().setRange(-max, max);
			formatConsensusChartSeries(plot);

			return chart;
		} catch (ChartDatasetCreationException | MissingLandmarkException | ComponentCreationException e) {
			LOGGER.fine("Unable to make segmented outline, creating base outline instead: " + e.getMessage());
			return makeNucleusOutlineChart();
		}

	}

	/**
	 * Format the series colours for a consensus nucleus.
	 * 
	 * @param plot the chart plot
	 */
	private void formatConsensusChartSeries(XYPlot plot) {

		XYDataset ds = plot.getDataset();

		for (int i = 0; i < plot.getSeriesCount(); i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, false);
			String name = (String) ds.getSeriesKey(i);

			// colour the segments
			if (name.startsWith(AbstractDatasetCreator.SEGMENT_SERIES_PREFIX)) {
				int segIndex = AbstractChartFactory.getIndexFromLabel(name);
				Paint colour = ColourSelecter.getColor(segIndex);
				plot.getRenderer().setSeriesPaint(i, colour);
				plot.getRenderer().setSeriesStroke(i, ChartComponents.MARKER_STROKE);
			}

			if (name.startsWith(AbstractDatasetCreator.TAG_PREFIX)) {
				plot.getRenderer().setSeriesStroke(i, ChartComponents.LANDMARK_STROKE);
				plot.getRenderer().setSeriesPaint(i, Color.BLUE);
			}
		}

	}

	/**
	 * Create a chart with multiple consensus nuclei.
	 * 
	 * @return a chart
	 */
	private JFreeChart makeMultipleConsensusChart() {
		// multiple nuclei
		try {
			XYDataset ds = new NucleusDatasetCreator(options).createMultiNucleusOutline();
			JFreeChart chart = makeConsensusChart(ds);

			formatConsensusChart(chart);

			XYPlot plot = chart.getXYPlot();

			double max = getConsensusChartRange();

			plot.getDomainAxis().setRange(-max, max);
			plot.getRangeAxis().setRange(-max, max);

			for (int i = 0; i < plot.getSeriesCount(); i++) {
				plot.getRenderer().setSeriesVisibleInLegend(i, false);
				plot.getRenderer().setSeriesStroke(i, ChartComponents.MARKER_STROKE);

				Paint colour = options.getDatasets().get(i).getDatasetColour().orElse(ColourSelecter.getColor(i));
				plot.getRenderer().setSeriesPaint(i, colour);
			}
			return chart;

		} catch (ChartDatasetCreationException | MissingLandmarkException | ComponentCreationException e) {
			LOGGER.log(Loggable.STACK, "Error making consensus dataset", e);
			return createErrorChart();
		}
	}
}
