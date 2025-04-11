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
import java.awt.Paint;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.signals.ISignalGroup;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.datasets.SignalViolinDatasetCreator;
import com.bmskinner.nma.visualisation.datasets.ViolinCategoryDataset;
import com.bmskinner.nma.visualisation.datasets.ViolinDatasetCreator;
import com.bmskinner.nma.visualisation.options.ChartOptions;

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
        } catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Error making violin chart: %s".formatted(e.getMessage()), e);
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
    		final ViolinCategoryDataset ds = new SignalViolinDatasetCreator(options).createSignalColocalisationViolinDataset();
    		final String scaleString = options.getScale().toString().toLowerCase();

    		final JFreeChart chart = createViolinChart(null, null, "Distance between signal pairs (" + scaleString + ")", ds,
    				false);

    		final CategoryPlot plot = chart.getCategoryPlot();
    		final ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

    		for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {
    			for (int series=0; series<plot.getDataset(datasetIndex).getRowCount(); series++) {

    				renderer.setSeriesPaint(series, Color.LIGHT_GRAY);
    				renderer.setSeriesOutlinePaint(series, Color.BLACK);
    			}
    		}
    		return chart;
    	} catch (final ChartDatasetCreationException e) {
    		LOGGER.log(Level.SEVERE, "Error creating volin dataset", e);
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

        final CategoryAxis categoryAxis = new CategoryAxis(categoryAxisLabel);
        final NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        valueAxis.setAutoRangeIncludesZero(false);

        final ViolinRenderer renderer = new ViolinRenderer();

        final CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, renderer);
        return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    }

    /**
     * Create a violin plot for whole cell data
     * 
     * @return
     */
    private JFreeChart createCellStatisticPlot() {

        ViolinCategoryDataset ds = null;
        if (!options.hasDatasets())
			return createEmptyChart();
		try {
		    ds = new ViolinDatasetCreator(options)
		            .createPlottableStatisticViolinDataset(CellularComponent.WHOLE_CELL);
		} catch (final ChartDatasetCreationException e) {
		    LOGGER.log(Level.SEVERE, "Error making chart dataset", e);
		    return createErrorChart();
		}

        final JFreeChart chart = createViolinChart(null, null, options.getMeasurement().label(options.getScale()), ds, false);

        final CategoryPlot plot = chart.getCategoryPlot();
        final ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);

        for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {

            for (int series = 0; series < plot.getDataset(datasetIndex).getRowCount(); series++) {

                renderer.setSeriesVisibleInLegend(series, false);
                final Paint color = options.getDatasets().get(series)
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
            } catch (final ChartDatasetCreationException e) {
				LOGGER.log(Level.SEVERE, "Error making chart dataset: %s".formatted(e.getMessage()), e);
                return createErrorChart();
            }


        final JFreeChart chart = createViolinChart(null, null, options.getMeasurement().label(options.getScale()), ds, false);

        final CategoryPlot plot = chart.getCategoryPlot();
        final ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);

        for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {

            for (int series = 0; series < plot.getDataset(datasetIndex).getRowCount(); series++) {

                renderer.setSeriesVisibleInLegend(series, false);
                
                final Paint color = options.getDatasets().get(series)
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

    /**
     * Create a signal boxplot with the given options
     * 
     * @param options
     * @return
     * @throws Exception
     */
    private synchronized JFreeChart createSignalStatisticPlot() {

        ViolinCategoryDataset ds = null;
        if (!options.hasDatasets())
			return createEmptyChart();
		try {
		    ds = new ViolinDatasetCreator(options)
		            .createPlottableStatisticViolinDataset(CellularComponent.NUCLEAR_SIGNAL);
		} catch (final ChartDatasetCreationException e) {
		    LOGGER.log(Level.SEVERE, "Error making chart dataset", e);
		    return createErrorChart();
		}

        final JFreeChart chart = createViolinChart(null, null, options.getMeasurement().label(options.getScale()), ds, false);

        final CategoryPlot plot = chart.getCategoryPlot();

        plot.getDomainAxis().setCategoryMargin(0.10);
        plot.getDomainAxis().setLowerMargin(0.05);
        plot.getDomainAxis().setUpperMargin(0.05);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);

        final ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();
        renderer.setItemMargin(0.05);
        renderer.setMaximumBarWidth(0.5);

        int series = 0;
        for (int column = 0; column < ds.getColumnCount(); column++) {

            final IAnalysisDataset d = options.getDatasets().get(column);

            for (int row = 0; row < ds.getRowCount(); row++) {

                final String name = (String) ds.getRowKey(row);
                final UUID signalGroup = getSignalGroupFromLabel(name);
                
                final Optional<ISignalGroup> g = d.getCollection().getSignalGroup(signalGroup);
                if(g.isPresent()){
                	final Paint colour = g.get().getGroupColour().orElse(ColourSelecter.getColor(row));
                	renderer.setSeriesPaint(series, colour);
                }
                series++;
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
     * @param ds the dataset
     * @return
     */
    private synchronized JFreeChart createSegmentPlot() {

        ViolinCategoryDataset ds = null;
        if (options.hasDatasets()) {
            try {
                ds = new ViolinDatasetCreator(options)
                        .createPlottableStatisticViolinDataset(CellularComponent.NUCLEAR_BORDER_SEGMENT);
            } catch (final ChartDatasetCreationException e) {
                LOGGER.log(Level.SEVERE, "Error creating volin dataset", e);
                return createErrorChart();
            }
        }

        final JFreeChart chart = createViolinChart(null, null, options.getMeasurement().label(options.getScale()), ds, false);

        final CategoryPlot plot = chart.getCategoryPlot();
        final ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

        for (int datasetIndex = 0; datasetIndex < plot.getDatasetCount(); datasetIndex++) {

            for (int series = 0; series < plot.getDataset(datasetIndex).getRowCount(); series++) {

            	final Paint color = options.getDatasets().get(series)
                		.getDatasetColour().orElse(ColourSelecter.getColor(series));

                renderer.setSeriesPaint(series, color);
                renderer.setSeriesOutlinePaint(series, Color.BLACK);
            }

        }

        return chart;
    }

}
