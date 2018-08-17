/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.charting.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.text.DecimalFormat;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.ChartComponents;
import com.bmskinner.nuclear_morphology.charting.datasets.CellDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.NucleusDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ProfileDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ProfileDatasetCreator.ProfileChartDataset;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.StatisticDimension;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.stats.DipTester;
import com.bmskinner.nuclear_morphology.stats.SignificanceTest;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class MorphologyChartFactory extends AbstractChartFactory {

    public MorphologyChartFactory(ChartOptions o) {
        super(o);
    }

    /**
     * Create a segment start XY position chart for multiple analysis datasets
     * 
     * @param options
     * @return
     * @throws Exception
     */
    private JFreeChart makeMultiSegmentStartPositionChart(ChartOptions options) {
        XYDataset positionDataset;
        try {
            positionDataset = new CellDatasetCreator(options).createPositionFeatureDataset();
        } catch (ChartDatasetCreationException e) {
            return makeErrorChart();
        }
        XYDataset nuclearOutlines;
        try {
            nuclearOutlines = new NucleusDatasetCreator(options).createMultiNucleusOutline();
        } catch (ChartDatasetCreationException e) {
            return makeErrorChart();
        }
        
        if (positionDataset == null || nuclearOutlines == null) {
            // a null dataset is returned if segment counts do not match
            return ConsensusNucleusChartFactory.makeEmptyChart();
        }

        JFreeChart chart = createBaseXYChart();

        XYPlot plot = chart.getXYPlot();

        plot.setDataset(0, positionDataset);

        /*
         * Points only for the segment positions
         */
        StandardXYToolTipGenerator tooltip = new StandardXYToolTipGenerator();
        XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer();
        pointRenderer.setBaseShapesVisible(true);
        pointRenderer.setBaseShape(ChartComponents.DEFAULT_POINT_SHAPE);
        pointRenderer.setBaseLinesVisible(false);
        pointRenderer.setBaseStroke(ChartComponents.QUARTILE_STROKE);
        pointRenderer.setBaseSeriesVisibleInLegend(false);
        pointRenderer.setBaseToolTipGenerator(tooltip);
        plot.setRenderer(0, pointRenderer);

        boolean hasConsensus = new ConsensusNucleusChartFactory(options).hasConsensusNucleus();
        if (hasConsensus) {
            // Find the bounds of the consensus nuclei in the options
            double max = new ConsensusNucleusChartFactory(options).getconsensusChartRange();
            plot.setDataset(1, nuclearOutlines);

            plot.getDomainAxis().setRange(-max, max);
            plot.getRangeAxis().setRange(-max, max);

            /*
             * Lines only for the consensus nuclei
             */
            XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer();
            lineRenderer.setBaseShapesVisible(false);
            lineRenderer.setBaseLinesVisible(true);
            lineRenderer.setBaseStroke(ChartComponents.QUARTILE_STROKE);
            lineRenderer.setBaseSeriesVisibleInLegend(false);
            plot.setRenderer(1, lineRenderer);

        } else {
            plot.getDomainAxis().setAutoRange(true);
            plot.getRangeAxis().setAutoRange(true);
        }
        plot.setBackgroundPaint(Color.WHITE);

        for (int j = 0; j < positionDataset.getSeriesCount(); j++) {

        	Paint profileColour = options.getDatasets().get(j).getDatasetColour().orElse(ColourSelecter.getColor(j));

            pointRenderer.setSeriesPaint(j, profileColour);
            pointRenderer.setSeriesShape(j, ChartComponents.DEFAULT_POINT_SHAPE);

            if (hasConsensus) {
                plot.getRenderer(1).setSeriesPaint(j, profileColour);
            }
        }
        applyAxisOptions(chart);
        return chart;

    }

    /**
     * Create a chart showing the angle values at the given normalised profile
     * position within the AnalysisDataset. The chart holds two chart datasets:
     * 0 is the probabililty density function. 1 is the actual values as dots on
     * the x-axis
     * 
     * @param position
     * @param dataset
     * @return
     * @throws Exception
     */
    private JFreeChart createModalityChart(Double position, List<IAnalysisDataset> list, ProfileType type)
            throws Exception {

        JFreeChart chart = createBaseXYChart();

        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setLabel(type.getLabel());
        plot.getRangeAxis().setLabel("Probability");

        if (type.getDimension().equals(StatisticDimension.ANGLE)) {
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
            return makeErrorChart();
        }

        JFreeChart chart = createBaseXYChart();

        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setRange(0, 100);
        plot.getDomainAxis().setLabel("Position");
        plot.getRangeAxis().setRange(0, 1);
        plot.getRangeAxis().setLabel("Probability");
        plot.setDataset(ds);

        for (int i = 0; i < options.getDatasets().size(); i++) {

            IAnalysisDataset dataset = options.getDatasets().get(i);

            Paint colour = dataset.getDatasetColour().orElse(ColourSelecter.getColor(i));

            plot.getRenderer().setSeriesPaint(i, colour);
            plot.getRenderer().setSeriesStroke(i, ChartComponents.MARKER_STROKE);
            plot.getRenderer().setSeriesVisibleInLegend(i, false);
        }
        applyAxisOptions(chart);
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
            return makeErrorChart();
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
                return makeErrorChart();
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
        applyAxisOptions(chart);
        return chart;
    }

    /**
     * Create a blank chart with default formatting for probability values
     * across a normalised profile
     * 
     * @return
     */
    public static JFreeChart makeBlankProbabililtyChart() {
        JFreeChart chart = ChartFactory.createXYLineChart(null, "Position", "Probability", null,
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
     * @param options
     *            the options to plot
     * @return
     * @throws Exception
     */
    public JFreeChart makeKruskalWallisChart(boolean frankenNormalise) {

    	ProfileChartDataset kruskalDataset = null;

        ProfileDatasetCreator creator = new ProfileDatasetCreator(options);

        ProfileChartDataset firstProfileDataset;
        ProfileChartDataset secondProfileDataset;
        try {
            if (frankenNormalise) {
                kruskalDataset = creator.createProfileDataset();
            } else {
                kruskalDataset = creator.createProfileDataset();
            }

            firstProfileDataset = creator.createProfileDataset();

            secondProfileDataset = creator.createProfileDataset();

        } catch (ChartDatasetCreationException e) {
            return makeErrorChart();
        }

        JFreeChart chart = ChartFactory.createXYLineChart(null, "Position", "Probability", null,
                PlotOrientation.VERTICAL, true, true, false);

        XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(Color.WHITE);
        plot.getDomainAxis().setRange(0, 100);

        LogAxis rangeAxis = new LogAxis("Probability");
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
            return makeErrorChart();
        }

        JFreeChart chart = ChartFactory.createXYLineChart(null, "Position", "Angle", ds, PlotOrientation.VERTICAL, true,
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
