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

import java.awt.Color;
import java.awt.Paint;
import java.util.Optional;
import java.util.UUID;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;

import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.SignalViolinDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ViolinCategoryDataset;
import com.bmskinner.nuclear_morphology.charting.datasets.ViolinDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;

public class ViolinChartFactory extends AbstractChartFactory {

    public ViolinChartFactory(ChartOptions o) {
        super(o);
    }

    public JFreeChart makeEmptyChart() {

        return BoxplotChartFactory.makeEmptyChart();
    }

    /**
     * Create a statistic plot for the given component.
     * 
     * @param component
     *            the component. Specified defaults are in
     *            {@link CellularComponent}
     * @return
     */
    public JFreeChart createStatisticPlot(String component) {
        finest("Making violin plot for " + component);

        if (!options.hasDatasets()) {
            return makeEmptyChart();
        }

        try {

            if (CellularComponent.WHOLE_CELL.equals(component)) {
                return createCellStatisticPlot();
            }

            if (CellularComponent.NUCLEUS.equals(component)) {
                return createNucleusStatisticPlot();
            }

            if (CellularComponent.NUCLEAR_SIGNAL.equals(component)) {
                return createSignalStatisticPlot();
            }

            if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)) {
                return createSegmentPlot();
            }

        } catch (Exception e) {
            stack("Error making violin chart", e);
            return makeErrorChart();
        }

        fine("No chart of type " + component);

        return makeEmptyChart();

    }

    /**
     * Create a segment length boxplot for the given segment name
     * 
     * @param ds
     *            the dataset
     * @return
     */
    public JFreeChart createSignalColocalisationViolinChart() {

        ViolinCategoryDataset ds = null;
        if (options.hasDatasets()) {
            try {
                ds = new SignalViolinDatasetCreator(options).createSignalColocalisationViolinDataset();
            } catch (ChartDatasetCreationException e) {
                stack("Error creating volin dataset", e);
                return makeErrorChart();
            }
        }

        String scaleString = options.getScale().toString().toLowerCase();

        JFreeChart chart = createViolinChart(null, null, "Distance between signal pairs (" + scaleString + ")", ds,
                false);

        CategoryPlot plot = chart.getCategoryPlot();
        ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

        for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {

            for (int series = 0; series < plot.getDataset(datasetIndex).getRowCount(); series++) {

                renderer.setSeriesPaint(series, Color.LIGHT_GRAY);
                renderer.setSeriesOutlinePaint(series, Color.BLACK);
            }

        }

        return chart;
    }

    /*
     * 
     * PRIVATE METHODS
     * 
     */

    private static JFreeChart createViolinChart(String title, String categoryAxisLabel, String valueAxisLabel,
            ViolinCategoryDataset dataset, boolean legend) {

        CategoryAxis categoryAxis = new CategoryAxis(categoryAxisLabel);
        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        valueAxis.setAutoRangeIncludesZero(false);

        ViolinRenderer renderer = new ViolinRenderer();

        CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, renderer);
        return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    }

    /**
     * Create a violin plot for whole cell data
     * 
     * @return
     */
    private JFreeChart createCellStatisticPlot() {

        ViolinCategoryDataset ds = null;
        if (options.hasDatasets()) {
            try {
                ds = new ViolinDatasetCreator(options)
                        .createPlottableStatisticViolinDataset(CellularComponent.WHOLE_CELL);
            } catch (ChartDatasetCreationException e) {
                stack("Error making chart dataset", e);
                return makeErrorChart();
            }
        } else {
            return makeEmptyChart();
        }

        JFreeChart chart = createViolinChart(null, null, options.getStat().label(options.getScale()), ds, false);

        CategoryPlot plot = chart.getCategoryPlot();
        ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);

        for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {

            for (int series = 0; series < plot.getDataset(datasetIndex).getRowCount(); series++) {

                renderer.setSeriesVisibleInLegend(series, false);
                Paint color = options.getDatasets().get(series).getDatasetColour() == null
                        ? ColourSelecter.getColor(series) : options.getDatasets().get(series).getDatasetColour();

                renderer.setSeriesPaint(series, color);
                renderer.setSeriesOutlinePaint(series, Color.BLACK);
            }
        }

        if (ds.hasProbabilities()) {
            plot.getRangeAxis().setRange(ds.getProbabiltyRange());
        }

        return chart;

    }

    private JFreeChart createNucleusStatisticPlot() {

        ViolinCategoryDataset ds = null;
        if (options.hasDatasets()) {
            try {
                ds = new ViolinDatasetCreator(options).createPlottableStatisticViolinDataset(CellularComponent.NUCLEUS);
            } catch (ChartDatasetCreationException e) {
                stack("Error making chart dataset", e);
                return makeErrorChart();
            }
        } else {
            return makeEmptyChart();
        }

        JFreeChart chart = createViolinChart(null, null, options.getStat().label(options.getScale()), ds, false);

        // log("Making violin chart");

        CategoryPlot plot = chart.getCategoryPlot();
        ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);

        for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {

            for (int series = 0; series < plot.getDataset(datasetIndex).getRowCount(); series++) {

                renderer.setSeriesVisibleInLegend(series, false);
                Paint color = options.getDatasets().get(series).getDatasetColour() == null
                        ? ColourSelecter.getColor(series) : options.getDatasets().get(series).getDatasetColour();

                renderer.setSeriesPaint(series, color);
                renderer.setSeriesOutlinePaint(series, Color.BLACK);
            }
        }

        if (ds.hasProbabilities()) {
            plot.getRangeAxis().setRange(ds.getProbabiltyRange());
        }

        return chart;

    }

    /**
     * Create a signal boxplot with the given options
     * 
     * @param options
     * @return
     * @throws Exception
     */
    private JFreeChart createSignalStatisticPlot() {

        ViolinCategoryDataset ds = null;
        if (options.hasDatasets()) {
            try {
                ds = new ViolinDatasetCreator(options)
                        .createPlottableStatisticViolinDataset(CellularComponent.NUCLEAR_SIGNAL);
            } catch (ChartDatasetCreationException e) {
                stack("Error making chart dataset", e);
                return makeErrorChart();
            }
        } else {
            return makeEmptyChart();
        }

        JFreeChart chart = createViolinChart(null, null, options.getStat().label(options.getScale()), ds, false);

        CategoryPlot plot = chart.getCategoryPlot();

        plot.getDomainAxis().setCategoryMargin(0.10);
        plot.getDomainAxis().setLowerMargin(0.05);
        plot.getDomainAxis().setUpperMargin(0.05);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);

        ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();
        renderer.setItemMargin(0.05);
        renderer.setMaximumBarWidth(0.5);

        int series = 0;
        for (int column = 0; column < ds.getColumnCount(); column++) {

            // The column is the dataset
            // String datasetName = ds.getColumnKey(column).toString();
            // log("Looking at dataset "+datasetName);
            IAnalysisDataset d = options.getDatasets().get(column);

            for (int row = 0; row < ds.getRowCount(); row++) {

                // log("Series "+series);
                String name = (String) ds.getRowKey(row);
                // log("Looking at row "+name);

                UUID signalGroup = getSignalGroupFromLabel(name);

                // Not every dataset will have every row.
                if (d.getCollection().hasSignalGroup(signalGroup)) {
                    Paint color = ColourSelecter.getColor(row);
                    try {

                    	Optional<Color> c = d.getCollection().getSignalGroup(signalGroup).getGroupColour();
                    	if(c.isPresent())
                    		color = c.get();

                    } catch (UnavailableSignalGroupException e) {
                        fine("Signal group " + signalGroup + " is not present in collection", e);
                    } finally {
                        renderer.setSeriesPaint(series, color);
                        series++;
                    }
                }

            }
        }

        if (ds.hasProbabilities()) {
            plot.getRangeAxis().setRange(ds.getProbabiltyRange());
        }

        return chart;
    }

    /**
     * Create a segment length boxplot for the given segment name
     * 
     * @param ds
     *            the dataset
     * @return
     */
    private JFreeChart createSegmentPlot() {

        ViolinCategoryDataset ds = null;
        if (options.hasDatasets()) {
            try {
                ds = new ViolinDatasetCreator(options)
                        .createPlottableStatisticViolinDataset(CellularComponent.NUCLEAR_BORDER_SEGMENT);
            } catch (ChartDatasetCreationException e) {
                fine("Error creating volin dataset", e);
                return makeErrorChart();
            }
        }

        JFreeChart chart = createViolinChart(null, null, options.getStat().label(options.getScale()), ds, false);

        CategoryPlot plot = chart.getCategoryPlot();
        ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

        for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {

            for (int series = 0; series < plot.getDataset(datasetIndex).getRowCount(); series++) {

                Paint color = options.getDatasets().get(series).getDatasetColour() == null
                        ? ColourSelecter.getColor(series) : options.getDatasets().get(series).getDatasetColour();

                renderer.setSeriesPaint(series, color);
                renderer.setSeriesOutlinePaint(series, Color.BLACK);
            }

        }

        return chart;
    }

}
