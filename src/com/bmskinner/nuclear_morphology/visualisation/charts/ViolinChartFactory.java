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

import java.awt.Color;
import java.awt.Paint;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.visualisation.datasets.SignalViolinDatasetCreator;
import com.bmskinner.nuclear_morphology.visualisation.datasets.ViolinCategoryDataset;
import com.bmskinner.nuclear_morphology.visualisation.datasets.ViolinDatasetCreator;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;

public class ViolinChartFactory extends AbstractChartFactory {
	
	private static final Logger LOGGER = Logger.getLogger(ViolinChartFactory.class.getName());

    public ViolinChartFactory(@NonNull final ChartOptions o) {
        super(o);
    }
    /**
     * Create a statistic plot for the given component.
     * 
     * @param component the component. Specified defaults are in
     *            {@link CellularComponent}
     * @return
     */
    public synchronized JFreeChart createStatisticPlot(String component) {
        if (!options.hasDatasets()) 
            return createEmptyChart();

        try {  	
        	switch(component) {
        		case CellularComponent.WHOLE_CELL: return createCellStatisticPlot();
        		case CellularComponent.NUCLEUS: return createNucleusStatisticPlot();
        		case CellularComponent.NUCLEAR_SIGNAL: return createSignalStatisticPlot();
        		case CellularComponent.NUCLEAR_BORDER_SEGMENT: return createSegmentPlot();
        		default: return createEmptyChart();
        	}
        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error making violin chart", e);
            return createErrorChart();
        }
    }

    /**
     * Create a segment length boxplot for the given segment name
     * 
     * @param ds the dataset
     * @return
     */
    public synchronized JFreeChart createSignalColocalisationViolinChart() {
    	if(!options.hasDatasets())
    		return createEmptyChart();

    	try {
    		ViolinCategoryDataset ds = new SignalViolinDatasetCreator(options).createSignalColocalisationViolinDataset();
    		String scaleString = options.getScale().toString().toLowerCase();

    		JFreeChart chart = createViolinChart(null, null, "Distance between signal pairs (" + scaleString + ")", ds,
    				false);

    		CategoryPlot plot = chart.getCategoryPlot();
    		ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

    		for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {
    			for (int series=0; series<plot.getDataset(datasetIndex).getRowCount(); series++) {

    				renderer.setSeriesPaint(series, Color.LIGHT_GRAY);
    				renderer.setSeriesOutlinePaint(series, Color.BLACK);
    			}
    		}
    		return chart;
    	} catch (ChartDatasetCreationException e) {
    		LOGGER.log(Loggable.STACK, "Error creating volin dataset", e);
    		return createErrorChart();
    	}
    }

    /*
     * 
     * PRIVATE METHODS
     * 
     */

    private static synchronized JFreeChart createViolinChart(String title, String categoryAxisLabel, String valueAxisLabel,
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
                LOGGER.log(Loggable.STACK, "Error making chart dataset", e);
                return createErrorChart();
            }
        } else {
            return createEmptyChart();
        }

        JFreeChart chart = createViolinChart(null, null, options.getMeasurement().label(options.getScale()), ds, false);

        CategoryPlot plot = chart.getCategoryPlot();
        ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);

        for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {

            for (int series = 0; series < plot.getDataset(datasetIndex).getRowCount(); series++) {

                renderer.setSeriesVisibleInLegend(series, false);
                Paint color = options.getDatasets().get(series)
                		.getDatasetColour().orElse(ColourSelecter.getColor(series));

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
    	
    	if(!options.hasDatasets())
    		return createEmptyChart();

        ViolinCategoryDataset ds = null;

            try {
                ds = new ViolinDatasetCreator(options).createPlottableStatisticViolinDataset(CellularComponent.NUCLEUS);
            } catch (ChartDatasetCreationException e) {
                LOGGER.log(Loggable.STACK, "Error making chart dataset", e);
                return createErrorChart();
            }


        JFreeChart chart = createViolinChart(null, null, options.getMeasurement().label(options.getScale()), ds, false);

        CategoryPlot plot = chart.getCategoryPlot();
        ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);

        for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {

            for (int series = 0; series < plot.getDataset(datasetIndex).getRowCount(); series++) {

                renderer.setSeriesVisibleInLegend(series, false);
                
                Paint color = options.getDatasets().get(series)
                		.getDatasetColour().orElse(ColourSelecter.getColor(series));

                renderer.setSeriesPaint(series, color);
                renderer.setSeriesOutlinePaint(series, Color.BLACK);
            }
        }

        if (ds.hasProbabilities())
            plot.getRangeAxis().setRange(ds.getProbabiltyRange());

        return chart;

    }

    /**
     * Create a signal boxplot with the given options
     * 
     * @param options
     * @return
     * @throws Exception
     */
    private synchronized JFreeChart createSignalStatisticPlot() {

        ViolinCategoryDataset ds = null;
        if (options.hasDatasets()) {
            try {
                ds = new ViolinDatasetCreator(options)
                        .createPlottableStatisticViolinDataset(CellularComponent.NUCLEAR_SIGNAL);
            } catch (ChartDatasetCreationException e) {
                LOGGER.log(Loggable.STACK, "Error making chart dataset", e);
                return createErrorChart();
            }
        } else {
            return createEmptyChart();
        }

        JFreeChart chart = createViolinChart(null, null, options.getMeasurement().label(options.getScale()), ds, false);

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

            IAnalysisDataset d = options.getDatasets().get(column);

            for (int row = 0; row < ds.getRowCount(); row++) {

                String name = (String) ds.getRowKey(row);
                UUID signalGroup = getSignalGroupFromLabel(name);
                
                Optional<ISignalGroup> g = d.getCollection().getSignalGroup(signalGroup);
                if(g.isPresent()){
                	Paint colour = g.get().getGroupColour().orElse(ColourSelecter.getColor(row));
                	renderer.setSeriesPaint(series, colour);
                }
                series++;
            }
        }

        if (ds.hasProbabilities())
            plot.getRangeAxis().setRange(ds.getProbabiltyRange());

        return chart;
    }

    /**
     * Create a segment length boxplot for the given segment name
     * 
     * @param ds the dataset
     * @return
     */
    private synchronized JFreeChart createSegmentPlot() {

        ViolinCategoryDataset ds = null;
        if (options.hasDatasets()) {
            try {
                ds = new ViolinDatasetCreator(options)
                        .createPlottableStatisticViolinDataset(CellularComponent.NUCLEAR_BORDER_SEGMENT);
            } catch (ChartDatasetCreationException e) {
                LOGGER.log(Loggable.STACK, "Error creating volin dataset", e);
                return createErrorChart();
            }
        }

        JFreeChart chart = createViolinChart(null, null, options.getMeasurement().label(options.getScale()), ds, false);

        CategoryPlot plot = chart.getCategoryPlot();
        ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

        for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {

            for (int series = 0; series < plot.getDataset(datasetIndex).getRowCount(); series++) {

            	Paint color = options.getDatasets().get(series)
                		.getDatasetColour().orElse(ColourSelecter.getColor(series));

                renderer.setSeriesPaint(series, color);
                renderer.setSeriesOutlinePaint(series, Color.BLACK);
            }

        }

        return chart;
    }

}
