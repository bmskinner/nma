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
package com.bmskinner.nuclear_morphology.charting.charts;

import java.awt.Color;
import java.awt.Paint;
import java.text.DecimalFormat;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.TextAnchor;

import com.bmskinner.nuclear_morphology.charting.ChartComponents;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.NucleusDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ProfileDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ProfileDatasetCreator.ProfileChartDataset;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementDimension;
import com.bmskinner.nuclear_morphology.components.profiles.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.stats.DipTester;
import com.bmskinner.nuclear_morphology.stats.SignificanceTest;

/**
 * Generate charts for various morphology profile parameters. 
 * @author ben
 *
 */
public class MorphologyChartFactory extends AbstractChartFactory {

	private static final String POSITION_LBL = "Position";
	private static final String PROBABILITY_LBL = "Probability";

	public MorphologyChartFactory(@NonNull ChartOptions o) {
		super(o);
	}

	/**
	 * Create a chart showing the angle values at the given normalised profile
	 * position within the AnalysisDataset. The chart holds two chart datasets:
	 * 0 is the probabililty density function. 1 is the actual values as dots on
	 * the x-axis
	 * 
	 * @param position
	 * @param list
	 * * @param type
	 * @return
	 * @throws Exception
	 */
	private JFreeChart createModalityChart(Double position, List<IAnalysisDataset> list, ProfileType type)
			throws ChartCreationException {

		JFreeChart chart = createBaseXYChart();

		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setLabel(type.getLabel());
		plot.getRangeAxis().setLabel(PROBABILITY_LBL);

		if (type.getDimension().equals(MeasurementDimension.ANGLE)) {
			plot.getDomainAxis().setRange(0, 360);
		}

		plot.addDomainMarker(new ValueMarker(180, Color.BLACK, ChartComponents.MARKER_STROKE));

		int datasetCount = 0;
		int iteration = 0;
		for (IAnalysisDataset dataset : list) {

			XYDataset ds = new NucleusDatasetCreator(options).createModalityProbabililtyDataset(position, dataset,
					type);
			XYDataset values = new NucleusDatasetCreator(options).createModalityValuesDataset(position, dataset, type);

			Paint colour = dataset.getDatasetColour().orElse(ColourSelecter.getColor(iteration));

			plot.setDataset(datasetCount, ds);

			XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
			plot.setRenderer(datasetCount, lineRenderer);
			int seriesCount = plot.getDataset(datasetCount).getSeriesCount();
			for (int i = 0; i < seriesCount; i++) {

				lineRenderer.setSeriesPaint(i, colour);
				lineRenderer.setSeriesStroke(i, ChartComponents.MARKER_STROKE);
				lineRenderer.setSeriesVisibleInLegend(i, false);
			}

			datasetCount++;

			plot.setDataset(datasetCount, values);

			// draw the individual points
			XYLineAndShapeRenderer shapeRenderer = new XYLineAndShapeRenderer(false, true);

			plot.setRenderer(datasetCount, shapeRenderer);
			seriesCount = plot.getDataset(datasetCount).getSeriesCount();
			for (int i = 0; i < seriesCount; i++) {
				shapeRenderer.setSeriesPaint(i, colour);
				shapeRenderer.setSeriesStroke(i, ChartComponents.MARKER_STROKE);
				shapeRenderer.setSeriesVisibleInLegend(i, false);
			}
			datasetCount++;
			iteration++;
		}

		return chart;
	}

	/**
	 * Create a chart showing the modality profiles for the given options
	 * @return a modality chart, or a chart with an error label if the data could not be found
	 */
	public JFreeChart createModalityProfileChart() {

		XYDataset ds;
		try {
			ds = new NucleusDatasetCreator(options).createModalityProfileDataset();
		} catch (ChartDatasetCreationException e) {
			return createErrorChart();
		}

		JFreeChart chart = createBaseXYChart();

		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setRange(0, 100);
		plot.getDomainAxis().setLabel(POSITION_LBL);
		plot.getRangeAxis().setRange(0, 1);
		plot.getRangeAxis().setLabel(PROBABILITY_LBL);
		plot.setDataset(ds);

		for (int i = 0; i < options.getDatasets().size(); i++) {

			IAnalysisDataset dataset = options.getDatasets().get(i);

			Paint colour = dataset.getDatasetColour().orElse(ColourSelecter.getColor(i));

			plot.getRenderer().setSeriesPaint(i, colour);
			plot.getRenderer().setSeriesStroke(i, ChartComponents.MARKER_STROKE);
			plot.getRenderer().setSeriesVisibleInLegend(i, false);
		}
		applyDefaultAxisOptions(chart);
		return chart;
	}

	public JFreeChart createModalityPositionChart() {

		if (!options.hasDatasets()) {
			return createEmptyChart();
		}

		JFreeChart chart;
		try {
			chart = createModalityChart(options.getModalityPosition(), options.getDatasets(), options.getType());
		} catch (Exception e) {
			return createErrorChart();
		}
		XYPlot plot = chart.getXYPlot();

		double yMax = 0;
		DecimalFormat df = new DecimalFormat("#0.000");

		for (int i = 0; i < plot.getDatasetCount(); i++) {

			// Ensure annotation is placed in the right y position
			double y = DatasetUtilities.findMaximumRangeValue(plot.getDataset(i)).doubleValue();
			yMax = y > yMax ? y : yMax;

		}

		int index = 0;
		for (IAnalysisDataset dataset : options.getDatasets()) {

			// Do the stats testing
			double pvalue;
			try {
				pvalue = new DipTester(dataset.getCollection()).getPValueForPositon(options.getModalityPosition(),
						options.getType());
			} catch (Exception e) {
				return createErrorChart();
			}

			// Add the annotation
			double yPos = yMax - (index * (yMax / 20));
			String statisticalTesting = "p(unimodal) = " + df.format(pvalue);
			if (pvalue < SignificanceTest.FIVE_PERCENT_SIGNIFICANCE_LEVEL) {
				statisticalTesting = "* " + statisticalTesting;
			}
			XYTextAnnotation annotation = new XYTextAnnotation(statisticalTesting, 355, yPos);

			// Set the text colour
			Paint colour = dataset.getDatasetColour().orElse(ColourSelecter.getColor(index));
			annotation.setPaint(colour);
			annotation.setTextAnchor(TextAnchor.TOP_RIGHT);
			plot.addAnnotation(annotation);
			index++;
		}
		applyDefaultAxisOptions(chart);
		return chart;
	}

	/**
	 * Create a blank chart with default formatting for probability values
	 * across a normalised profile
	 * 
	 * @return
	 */
	public static JFreeChart makeBlankProbabililtyChart() {
		JFreeChart chart = ChartFactory.createXYLineChart(null, POSITION_LBL, PROBABILITY_LBL, null,
				PlotOrientation.VERTICAL, true, true, false);

		XYPlot plot = chart.getXYPlot();

		plot.setBackgroundPaint(Color.WHITE);
		plot.getDomainAxis().setRange(0, 100);
		plot.getRangeAxis().setRange(0, 1);
		return chart;
	}

	/**
	 * Create a Kruskal-Wallis probability chart comparing two datasets.
	 * 
	 * @return
	 */
	public JFreeChart makeKruskalWallisChart() {

		ProfileChartDataset kruskalDataset = null;

		ProfileDatasetCreator creator = new ProfileDatasetCreator(options);

		ProfileChartDataset firstProfileDataset;
		ProfileChartDataset secondProfileDataset;
		try {
			kruskalDataset = creator.createProfileDataset();
			firstProfileDataset = creator.createProfileDataset();
			secondProfileDataset = creator.createProfileDataset();

		} catch (ChartDatasetCreationException e) {
			return createErrorChart();
		}

		JFreeChart chart = ChartFactory.createXYLineChart(null, POSITION_LBL, PROBABILITY_LBL, null,
				PlotOrientation.VERTICAL, true, true, false);

		XYPlot plot = chart.getXYPlot();

		plot.setBackgroundPaint(Color.WHITE);
		plot.getDomainAxis().setRange(0, 100);

		LogAxis rangeAxis = new LogAxis(PROBABILITY_LBL);
		rangeAxis.setBase(10);
		DecimalFormat df = new DecimalFormat();
		df.applyPattern("0.#E0");
		rangeAxis.setNumberFormatOverride(df);
		rangeAxis.setStandardTickUnits(new StandardTickUnitSource());

		plot.setRangeAxis(rangeAxis);

		NumberAxis angleAxis = new NumberAxis("Angle");
		angleAxis.setRange(0, 360);

		plot.setRangeAxis(0, rangeAxis);
		plot.setRangeAxis(1, angleAxis);

		plot.setDataset(0, kruskalDataset.getLines());
		plot.setDataset(1, firstProfileDataset.getLines());
		plot.setDataset(2, secondProfileDataset.getLines());

		XYItemRenderer logRenderer = new XYLineAndShapeRenderer(true, false);
		logRenderer.setSeriesPaint(0, Color.BLACK);
		logRenderer.setSeriesVisibleInLegend(0, false);
		logRenderer.setSeriesStroke(0, ChartComponents.MARKER_STROKE);

		XYItemRenderer angleRendererOne = new XYLineAndShapeRenderer(true, false);
		Paint colorOne = options.firstDataset().getDatasetColour().orElse(ColourSelecter.getColor(0));

		angleRendererOne.setSeriesPaint(0, colorOne);
		angleRendererOne.setSeriesVisibleInLegend(0, false);
		angleRendererOne.setSeriesStroke(0, ChartComponents.MARKER_STROKE);

		XYItemRenderer angleRendererTwo = new XYLineAndShapeRenderer(true, false);
		Paint colorTwo = options.getDatasets().get(1).getDatasetColour().orElse(ColourSelecter.getColor(1));
		angleRendererTwo.setSeriesPaint(0, colorTwo);
		angleRendererTwo.setSeriesVisibleInLegend(0, false);
		angleRendererTwo.setSeriesStroke(0, ChartComponents.MARKER_STROKE);

		plot.setRenderer(0, logRenderer);
		plot.setRenderer(1, angleRendererOne);
		plot.setRenderer(2, angleRendererTwo);

		plot.mapDatasetToRangeAxis(0, 0);
		plot.mapDatasetToRangeAxis(1, 1);
		plot.mapDatasetToRangeAxis(2, 1);

		return chart;
	}

	/**
	 * Create a chart showing the effect of a boolean profile on a profile
	 * 
	 * @param p
	 * @param limits
	 * @return
	 */
	public JFreeChart createBooleanProfileChart(IProfile p, BooleanProfile limits) {

		XYDataset ds;
		try {
			ds = new NucleusDatasetCreator(options).createBooleanProfileDataset(p, limits);
		} catch (ChartDatasetCreationException e) {
			return createErrorChart();
		}

		JFreeChart chart = ChartFactory.createXYLineChart(null, POSITION_LBL, "Angle", ds, PlotOrientation.VERTICAL, true,
				true, false);

		XYPlot plot = chart.getXYPlot();

		plot.setBackgroundPaint(Color.WHITE);

		plot.addRangeMarker(new ValueMarker(180, Color.BLACK, ChartComponents.MARKER_STROKE));

		DefaultXYItemRenderer rend = new DefaultXYItemRenderer();
		rend.setBaseShapesVisible(true);
		rend.setBaseShape(ChartComponents.DEFAULT_POINT_SHAPE);
		rend.setSeriesPaint(0, Color.BLACK);
		rend.setSeriesVisibleInLegend(0, false);
		rend.setSeriesPaint(1, Color.LIGHT_GRAY);
		rend.setSeriesVisibleInLegend(1, false);
		rend.setSeriesLinesVisible(0, false);
		rend.setSeriesShape(0, ChartComponents.DEFAULT_POINT_SHAPE);
		rend.setSeriesLinesVisible(1, false);
		rend.setSeriesShape(1, ChartComponents.DEFAULT_POINT_SHAPE);

		plot.setRenderer(rend);

		return chart;
	}
}
