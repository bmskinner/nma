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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.charting.ChartComponents;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.charts.HistogramDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.charts.NuclearHistogramDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.charts.SignalHistogramDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Methods for creating histograms from chart options
 * 
 * @author bms41
 *
 */
public class HistogramChartFactory extends AbstractChartFactory {
	
	private static final Logger LOGGER = Logger.getLogger(HistogramChartFactory.class.getName());
	
	private static final boolean HISTOGRAM_CREATE_LEGEND = true;
	private static final boolean HISTOGRAM_CREATE_TOOLTIP = true;

    public HistogramChartFactory(ChartOptions o) {
        super(o);
    }

    /**
     * Create an empty histogram
     * 
     * @return a chart with no data
     */
    public static JFreeChart createEmptyHistogram() {

        JFreeChart chart = ChartFactory.createHistogram(null, null, null, null, 
        		PlotOrientation.VERTICAL, HISTOGRAM_CREATE_LEGEND, HISTOGRAM_CREATE_TOOLTIP,
                DEFAULT_CREATE_URLS);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        XYBarRenderer rend = new XYBarRenderer();
        rend.setBarPainter(new StandardXYBarPainter());
        rend.setShadowVisible(false);
        plot.setRenderer(rend);
        return chart;
    }

    /**
     * Create a histogram from a histogram dataset and apply basic formatting
     * 
     * @param ds the dataset to use
     * @param xLabel the label of the x axis
     * @param yLabel the label of the y axis
     * @return a histogram
     */
    public static JFreeChart createHistogram(HistogramDataset ds, String xLabel, String yLabel) {

        JFreeChart chart = ChartFactory.createHistogram(null, xLabel, yLabel, ds, 
        		PlotOrientation.VERTICAL, HISTOGRAM_CREATE_LEGEND, HISTOGRAM_CREATE_TOOLTIP,
        		DEFAULT_CREATE_URLS);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        XYBarRenderer rend = new XYBarRenderer();
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

    public JFreeChart createStatisticHistogram(String component) {

        LOGGER.finest( "Creating stats histogram for " + component + ": " + options.getStat());

        if (!options.hasDatasets()) {
            return createEmptyHistogram();
        }

        if (CellularComponent.NUCLEUS.equals(component)) {
            return createNuclearStatsHistogram();
        }

        if (CellularComponent.NUCLEAR_SIGNAL.equals(component)) {
            return createSignalStatisticHistogram();
        }

        if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)) {
            return createSegmentStatisticHistogram();
        }

        return createEmptyHistogram();

    }

    /**
     * Create a histogram from a list of values
     * 
     * @param list
     * @return
     * @throws Exception
     */
    public static JFreeChart createRandomSampleHistogram(List<Double> list) throws ChartDatasetCreationException {
        HistogramDataset ds = HistogramDatasetCreator.createHistogramDatasetFromList(list);
        JFreeChart chart = createHistogram(ds, "Magnitude difference between populations", "Observed instances");
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
        XYDataset ds = new NuclearHistogramDatasetCreator(options).createDensityDatasetFromList(list, 0.0001);
        String xLabel = "Magnitude difference between populations";
        JFreeChart chart = ChartFactory.createXYLineChart(null, xLabel, "Probability", ds, PlotOrientation.VERTICAL,
                true, true, false);
        XYPlot plot = chart.getXYPlot();
        for (int j = 0; j < ds.getSeriesCount(); j++) {
            plot.getRenderer().setSeriesVisibleInLegend(j, false);
        }

        plot.setBackgroundPaint(Color.WHITE);
        chart.getXYPlot().addDomainMarker(new ValueMarker(1, Color.BLACK, ChartComponents.MARKER_STROKE));

        return chart;
    }

    /*
     * 
     * PRIVATE METHODS
     * 
     */

    /**
     * Create a signal angle histogram for a dataset
     * 
     * @param options the ChartOptions
     * @return
     * @throws Exception
     */
    private JFreeChart createSignalStatisticHistogram() {

        if (options.isUseDensity()) {
            return createSignalDensityStatsChart();
        }

        Measurement stat = options.getStat();

        List<HistogramDataset> list;
        try {
            list = options.hasDatasets() ? new SignalHistogramDatasetCreator(options)
                    .createSignalStatisticHistogramDataset(options.getDatasets(), stat, options.getScale()) : null;
        } catch (ChartDatasetCreationException e) {
            LOGGER.log(Loggable.STACK, "Error making signal dataset", e);
            return createErrorChart();
        }

        if (list == null) {
            return createErrorChart();
        }
        // Make a histogram from the first dataset.

        JFreeChart chart = createHistogram(list.get(0), stat.label(options.getScale()), "Count");

        if (options.hasDatasets()) {

            XYPlot plot = chart.getXYPlot();
            if (stat.equals(Measurement.ANGLE)) {
                plot.getDomainAxis().setRange(0, 360);
            }

            int datasetCount = 0;
            for (HistogramDataset ds : list) {

                plot.setDataset(datasetCount, ds);

                IAnalysisDataset d = options.getDatasets().get(datasetCount);

                XYBarRenderer rend = new XYBarRenderer();
                rend.setBarPainter(new StandardXYBarPainter());
                rend.setShadowVisible(false);

                plot.setRenderer(datasetCount, rend);

                int seriesCount = ds.getSeriesCount();

                for (int j = 0; j < seriesCount; j++) {

                    String name = ds.getSeriesKey(j).toString();

                    UUID signalGroup = getSignalGroupFromLabel(name);

                    rend.setSeriesVisibleInLegend(j, false);
                    rend.setSeriesStroke(j, ChartComponents.MARKER_STROKE);
                    
                    Optional<ISignalGroup> g = d.getCollection().getSignalGroup(signalGroup);
                    if(g.isPresent()){
                    	Paint colour = g.get().getGroupColour().orElse(ColourSelecter.getColor(j));
                    	rend.setSeriesPaint(j, colour);
                    }
                }
                datasetCount++;
            }

        }
        return chart;
    }

    /**
     * Create a density line chart with nuclear statistics. It is used to
     * replace the histograms when the 'Use density' box is ticked in the
     * Nuclear chart histogram panel
     * 
     * @param ds
     *            the histogram dataset
     * @param list
     *            the analysis datasets used to create the histogrom
     * @param xLabel
     *            the x axis label
     * @return
     * @throws Exception
     */
    private JFreeChart createSignalDensityStatsChart() {

        if (!options.hasDatasets()) {
            return createEmptyHistogram();
        }

        List<DefaultXYDataset> list;
        try {
            list = new SignalHistogramDatasetCreator(options).createSignalDensityHistogramDataset();
        } catch (ChartDatasetCreationException e) {
            return createErrorChart();
        }

        String xLabel = options.getStat().label(options.getScale());
        JFreeChart chart = ChartFactory.createXYLineChart(null, xLabel, "Probability", list.get(0),
                PlotOrientation.VERTICAL, true, true, false);

        XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(Color.WHITE);

        if (!list.isEmpty()) {

            int datasetCount = 0;
            for (DefaultXYDataset ds : list) {

                plot.setDataset(datasetCount, ds);
                XYLineAndShapeRenderer rend = new XYLineAndShapeRenderer();
                rend.setDefaultLinesVisible(true);
                rend.setDefaultShapesVisible(false);
                plot.setRenderer(datasetCount, rend);

                for (int j = 0; j < ds.getSeriesCount(); j++) {

                    rend.setSeriesVisibleInLegend(j, false);
                    rend.setSeriesStroke(j, ChartComponents.MARKER_STROKE);

                    String seriesKey = ds.getSeriesKey(j).toString();
                    UUID signalGroup = getSignalGroupFromLabel(seriesKey);

                    IAnalysisDataset d = options.getDatasets().get(datasetCount);
                    Optional<ISignalGroup> g = d.getCollection().getSignalGroup(signalGroup);
                    if(g.isPresent()){
                    	Paint colour = g.get().getGroupColour().orElse(ColourSelecter.getColor(j));
                    	rend.setSeriesPaint(j, colour);
                    }
                }
                datasetCount++;
            }
            setDomainRange(plot, list);
        }

        return chart;
    }

    /**
     * Create a histogram with nuclear statistics
     * 
     * @param ds
     *            the histogram dataset
     * @param list
     *            the analysis datasets used to create the histogrom
     * @param xLabel
     *            the x axis label
     * @return
     */
    private JFreeChart createNuclearStatsHistogram() {

        if (options.isUseDensity())
            return createNuclearDensityStatsChart();

        if (!options.hasDatasets())
            return createEmptyHistogram();

        HistogramDataset ds;

        try {
            ds = new NuclearHistogramDatasetCreator(options).createNuclearStatsHistogramDataset();
        } catch (ChartDatasetCreationException e) {
            return createErrorChart();
        }

        String xLabel = options.getStat().label(options.getScale());

        JFreeChart chart = createHistogram(ds, xLabel, "Nuclei");

        XYPlot plot = chart.getXYPlot();

        setDomainRange(plot, ds);

        for (int j = 0; j < ds.getSeriesCount(); j++) {

            plot.getRenderer().setSeriesVisibleInLegend(j, false);
            plot.getRenderer().setSeriesStroke(j, ChartComponents.MARKER_STROKE);

            String seriesKey = (String) ds.getSeriesKey(j);
            String seriesName = seriesKey.replaceFirst(options.getStat().toString() + "_", "");

            for (IAnalysisDataset dataset : options.getDatasets()) {

                if (seriesName.equals(dataset.getName())) {
                	Paint colour = options.getDatasets().get(j)
                    		.getDatasetColour().orElse(ColourSelecter.getColor(j));

                    plot.getRenderer().setSeriesPaint(j, colour);

                }
            }

        }
        return chart;
    }

    /**
     * Create a density line chart with nuclear statistics. It is used to
     * replace the histograms when the 'Use density' box is ticked in the
     * Nuclear chart histogram panel
     * 
     * @param ds
     *            the histogram dataset
     * @param list
     *            the analysis datasets used to create the histogrom
     * @param xLabel
     *            the x axis label
     * @return
     * @throws Exception
     */
    private JFreeChart createNuclearDensityStatsChart() {

        if (!options.hasDatasets()) {
            return createEmptyHistogram();
        }

        XYDataset ds;
        try {
            ds = new NuclearHistogramDatasetCreator(options).createNuclearDensityHistogramDataset();
        } catch (ChartDatasetCreationException e) {
            return createErrorChart();
        }

        String xLabel = options.getStat().label(options.getScale());
        JFreeChart chart = ChartFactory.createXYLineChart(null, xLabel, "Probability", ds, PlotOrientation.VERTICAL,
                true, true, false);

        XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(Color.WHITE);

        setDomainRange(plot, ds);

        for (int j = 0; j < ds.getSeriesCount(); j++) {

            plot.getRenderer().setSeriesVisibleInLegend(j, false);
            plot.getRenderer().setSeriesStroke(j, ChartComponents.MARKER_STROKE);

            String seriesKey = (String) ds.getSeriesKey(j);
            String seriesName = seriesKey.replaceFirst(options.getStat().toString() + "_", "");

            for (IAnalysisDataset dataset : options.getDatasets()) {
                if (seriesName.equals(dataset.getName())) {
                	Paint colour = dataset.getDatasetColour().orElse(ColourSelecter.getColor(j));
                    plot.getRenderer().setSeriesPaint(j, colour);
                }
            }

        }
        return chart;
    }

    /**
     * Create a histogram with segment lengths
     * 
     * @param options
     *            the HistogramOptions.
     * @param segName
     *            the segment to plot
     * @return
     */
    private JFreeChart createSegmentStatisticHistogram() {

        if (options.isUseDensity()) {
            return createSegmentLengthDensityChart();
        }

        if (!options.hasDatasets()) {
            return createEmptyHistogram();
        }

        HistogramDataset ds;

        try {
            ds = new NuclearHistogramDatasetCreator(options).createSegmentLengthHistogramDataset();
        } catch (ChartDatasetCreationException e) {
            return createErrorChart();
        }

        JFreeChart chart = createHistogram(ds,
                IProfileSegment.SEGMENT_PREFIX + options.getSegPosition() + " length (" + options.getScale() + ")",
                "Nuclei");

        if (ds != null && options.hasDatasets()) {

            XYPlot plot = chart.getXYPlot();

            setDomainRange(plot, ds);

            for (int j = 0; j < ds.getSeriesCount(); j++) {

                plot.getRenderer().setSeriesVisibleInLegend(j, false);
                plot.getRenderer().setSeriesStroke(j, ChartComponents.MARKER_STROKE);

                String seriesKey = (String) ds.getSeriesKey(j);
                String seriesName = seriesKey
                        .replaceFirst(IProfileSegment.SEGMENT_PREFIX + options.getSegPosition() + "_", "");

                for (IAnalysisDataset dataset : options.getDatasets()) {

                    if (seriesName.equals(dataset.getName())) {
                    	Paint colour = dataset.getDatasetColour().orElse(ColourSelecter.getColor(j));
                        plot.getRenderer().setSeriesPaint(j, colour);

                    }
                }

            }
        }
        return chart;
    }

    /**
     * Create a density line chart with nuclear statistics. It is used to
     * replace the histograms when the 'Use density' box is ticked in the
     * Nuclear chart histogram panel
     * 
     * @param ds
     *            the histogram dataset
     * @param list
     *            the analysis datasets used to create the histogrom
     * @param xLabel
     *            the x axis label
     * @return
     * @throws Exception
     */
    private JFreeChart createSegmentLengthDensityChart() {

        if (!options.hasDatasets()) {

            return createEmptyHistogram();

        }

        XYDataset ds;

        try {
            ds = new NuclearHistogramDatasetCreator(options).createSegmentLengthDensityDataset();
        } catch (ChartDatasetCreationException e) {
            return createErrorChart();
        }

        String xLabel = "Seg_" + options.getSegPosition() + " length (" + options.getScale() + ")";
        JFreeChart chart = ChartFactory.createXYLineChart(null, xLabel, "Probability", ds, PlotOrientation.VERTICAL,
                true, true, false);

        XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(Color.WHITE);

        if (ds != null && options.hasDatasets()) {

            setDomainRange(plot, ds);

            for (int j = 0; j < ds.getSeriesCount(); j++) {

                plot.getRenderer().setSeriesVisibleInLegend(j, false);
                plot.getRenderer().setSeriesStroke(j, ChartComponents.MARKER_STROKE);

                String seriesKey = (String) ds.getSeriesKey(j);
                String seriesName = seriesKey.replaceFirst("Seg_" + options.getSegPosition() + "_", "");

                for (IAnalysisDataset dataset : options.getDatasets()) {

                    if (seriesName.equals(dataset.getName())) {
                    	Paint colour = dataset.getDatasetColour().orElse(ColourSelecter.getColor(j));
                        plot.getRenderer().setSeriesPaint(j, colour);
                    }
                }

            }

        }
        return chart;
    }

    /**
     * Update the range of the plot domain axis to the min and max values within
     * the given dataset
     * 
     * @param plot
     * @param ds
     */
    private void setDomainRange(XYPlot plot, XYDataset ds) {

        Number max = DatasetUtils.findMaximumDomainValue(ds);
        Number min = DatasetUtils.findMinimumDomainValue(ds);
        if (max.doubleValue() > min.doubleValue()) { // stop if 0 and 0 or no
                                                     // values found
            plot.getDomainAxis().setRange(min.doubleValue(), max.doubleValue());
        }
    }

    /**
     * Update the range of the plot domain axis to the min and max values within
     * the given dataset
     * 
     * @param plot
     * @param ds
     */
    private void setDomainRange(XYPlot plot, List<DefaultXYDataset> list) {

        Range r = plot.getDomainAxis().getRange();

        for (XYDataset ds : list) {
            Number maxX = DatasetUtils.findMaximumDomainValue(ds);
            Number minX = DatasetUtils.findMinimumDomainValue(ds);

            Range sub = new Range(minX.doubleValue(), maxX.doubleValue());

            r = Range.combine(sub, r);

        }

        plot.getDomainAxis().setRange(r);

    }

}
