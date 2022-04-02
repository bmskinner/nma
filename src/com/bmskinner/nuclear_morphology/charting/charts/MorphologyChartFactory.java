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

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.charting.ChartComponents;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.NucleusDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ProfileDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ProfileDatasetCreator.ProfileChartDataset;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.profiles.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;

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
		rend.setDefaultShapesVisible(true);
		rend.setDefaultShape(ChartComponents.DEFAULT_POINT_SHAPE);
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
