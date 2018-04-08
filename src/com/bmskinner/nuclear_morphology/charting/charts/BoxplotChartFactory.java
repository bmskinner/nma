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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.NuclearSignalDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.NucleusDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;

/**
 * This factory creates boxplot charts.
 * 
 * @author bms41
 *
 */
public class BoxplotChartFactory extends AbstractChartFactory {

    public BoxplotChartFactory(ChartOptions o) {
        super(o);
    }

    /**
     * Create an empty boxplot
     * 
     * @return
     */
    public static JFreeChart makeEmptyChart() {

        JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null,
                new DefaultBoxAndWhiskerCategoryDataset(), false);

        formatBoxplot(boxplot);
        return boxplot;
    }

    public JFreeChart createStatisticBoxplot(String component) {

        if (!options.hasDatasets()) {
            return makeEmptyChart();
        }

        if (CellularComponent.NUCLEUS.equals(component)) {
            return createNucleusStatisticBoxplot();
        }

        if (CellularComponent.NUCLEAR_SIGNAL.equals(component)) {
            return createSignalStatisticBoxplot();
        }

        if (CellularComponent.NUCLEAR_BORDER_SEGMENT.equals(component)) {
            return createSegmentBoxplot();
        }

        return makeEmptyChart();

    }

    /*
     * 
     * PRIVATE METHODS
     * 
     */

    private JFreeChart createNucleusStatisticBoxplot() {

        BoxAndWhiskerCategoryDataset ds = null;
        if (options.getDatasets() != null) {
            try {
                ds = new NucleusDatasetCreator(options).createBoxplotDataset();
            } catch (ChartDatasetCreationException e) {
                fine("Error creating boxplot", e);
                return makeErrorChart();
            }
        }

        String yLabel = options.getStat().label(options.getScale());

        JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, yLabel, ds, false);
        formatBoxplotChart(boxplotChart, options.getDatasets());
        return boxplotChart;

    }

    /**
     * Create a segment length boxplot for the given segment name
     * 
     * @param ds
     *            the dataset
     * @return
     */
    private JFreeChart createSegmentBoxplot() {

        PlottableStatistic stat = options.getStat();

        BoxAndWhiskerCategoryDataset ds;
        try {
            ds = new NucleusDatasetCreator(options).createSegmentStatDataset();
        } catch (ChartDatasetCreationException e) {
            fine("Error creating boxplot", e);
            return makeErrorChart();
        }
        JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, null,
                "Segment " + stat.label(options.getScale()), ds, false);

        formatBoxplot(boxplot);
        CategoryPlot plot = boxplot.getCategoryPlot();

        for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {

            BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();

            for (int series = 0; series < plot.getDataset(datasetIndex).getRowCount(); series++) {

            	Paint colour = options.getDatasets().get(series)
                		.getDatasetColour().orElse(ColourSelecter.getColor(series));

                renderer.setSeriesPaint(series, colour);
                renderer.setSeriesOutlinePaint(series, Color.BLACK);
            }

            renderer.setMeanVisible(false);
            renderer.setUseOutlinePaintForWhiskers(true);
            plot.setRenderer(datasetIndex, renderer);
        }

        return boxplot;
    }

    /**
     * Create a signal boxplot with the given options
     * 
     * @param options
     * @return
     * @throws Exception
     */
    private JFreeChart createSignalStatisticBoxplot() {

        BoxAndWhiskerCategoryDataset ds;
        try {
            ds = new NuclearSignalDatasetCreator(options).createSignalStatisticBoxplotDataset();
        } catch (ChartDatasetCreationException e) {
            return makeErrorChart();
        }

        JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, null,
                options.getStat().label(options.getScale()), ds, false);

        formatBoxplot(boxplot);

        CategoryPlot plot = boxplot.getCategoryPlot();

        plot.getDomainAxis().setCategoryMargin(0.10);
        plot.getDomainAxis().setLowerMargin(0.05);
        plot.getDomainAxis().setUpperMargin(0.05);

        BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();
        renderer.setItemMargin(0.05);
        renderer.setMaximumBarWidth(0.5);

        int series = 0;
        for (int column = 0; column < ds.getColumnCount(); column++) {

            // The column is the dataset
            // String datasetName = ds.getColumnKey(column).toString();
            // log("Looking at dataset "+datasetName);
            IAnalysisDataset d = options.getDatasets().get(column);

            for (int row = 0; row < ds.getRowCount(); row++) {
                String name = (String) ds.getRowKey(row);

                UUID signalGroup = getSignalGroupFromLabel(name);

                Optional<ISignalGroup> g = d.getCollection().getSignalGroup(signalGroup);
                if(g.isPresent()){

                	Paint color = g.get().getGroupColour().orElse(ColourSelecter.getColor(row));
                	renderer.setSeriesPaint(series, color);
                	series++;

                }

            }
        }
        return boxplot;
    }

    /**
     * Apply the default formatting to a boxplot with list
     * 
     * @param boxplot
     */
    private void formatBoxplotChart(JFreeChart boxplot, List<IAnalysisDataset> list) {
        formatBoxplot(boxplot);
        CategoryPlot plot = boxplot.getCategoryPlot();
        BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();

        for (int i = 0; i < plot.getDataset().getRowCount(); i++) {

            IAnalysisDataset d = list.get(i);

            Paint colour = options.getDatasets().get(i)
            		.getDatasetColour().orElse(ColourSelecter.getColor(i));

            renderer.setSeriesPaint(i, colour);
        }
    }

    /**
     * Apply basic formatting to the charts, without any series added
     * 
     * @param boxplot
     */
    private static void formatBoxplot(JFreeChart boxplot) {
        CategoryPlot plot = boxplot.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        plot.setRenderer(renderer);
        renderer.setUseOutlinePaintForWhiskers(true);
        renderer.setBaseOutlinePaint(Color.BLACK);
        renderer.setBaseFillPaint(Color.LIGHT_GRAY);
        renderer.setMeanVisible(false);
    }

}
