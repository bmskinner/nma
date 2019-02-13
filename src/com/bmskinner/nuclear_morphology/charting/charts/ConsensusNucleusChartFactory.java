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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.charting.ChartComponents;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.NucleusDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;

/**
 * Methods to make charts with a consensus nucleus.
 */
public class ConsensusNucleusChartFactory extends AbstractChartFactory {
	
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
        return options.getDatasets().stream().anyMatch( d -> d.getCollection().hasConsensus());
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
            chart = ChartFactory.createXYLineChart(null, null, null, ds, 
            		PlotOrientation.VERTICAL, DEFAULT_CREATE_LEGEND, DEFAULT_CREATE_TOOLTIPS, DEFAULT_CREATE_URLS);
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
            boolean oneHasConsensus = options.getDatasets().stream().anyMatch(d->d.getCollection().hasConsensus());
            if (oneHasConsensus) 
                return makeMultipleConsensusChart();
			return createTextAnnotatedEmptyChart(MULTIPLE_DATASETS_NO_CONSENSUS_ERROR);
        }
        
        // Single dataset
        
        fine("Single dataset, making consenusus chart");
        
        if (options.isShowMesh()) {
        	try {
        		Mesh<Nucleus> mesh = new DefaultMesh(options.firstDataset().getCollection().getConsensus(),
        				options.getMeshSize());
        		return new OutlineChartFactory(options).createMeshChart(mesh, 0.5);
        	} catch (ChartCreationException | MeshCreationException e) {
        		stack("Error making mesh chart", e);
        		return createErrorChart();
        	}
        }
        return makeSegmentedConsensusChart(options.firstDataset());
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
     * Create a consenusus chart for the given nucleus collection. This chart
     * draws the nucleus border in black. There are no IQRs or segments.
     * 
     * @return the consensus chart
     */
    public JFreeChart makeNucleusBareOutlineChart() {
    	CellularComponent component = options.hasComponent() ? options.getComponent() : null;
    	
    	if(component==null) {
    		IAnalysisDataset dataset = options.firstDataset();

    		if (!dataset.getCollection().hasConsensus())
    			return createTextAnnotatedEmptyChart(MULTIPLE_DATASETS_NO_CONSENSUS_ERROR);

    		component = dataset.getCollection().getConsensus();
    	}

        XYDataset ds;
        try {
            ds = new NucleusDatasetCreator(options).createBareNucleusOutline(component);
        } catch (ChartDatasetCreationException e) {
            stack("Error creating boxplot", e);
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
     * Create a consenusus chart for the given nucleus collection. This chart
     * draws the nucleus border in black. There are no IQRs or segments. The OP
     * is drawn as a blue square in series 1 of dataset 0. If you don't want this, use
     * {@link ConsensusNucleusChartFactory#makeNucleusBareOutlineChart}
     * 
     * @return the consensus chart
     */
    public JFreeChart makeNucleusOutlineChart() {

    	CellularComponent component = options.hasComponent() ? options.getComponent() : null;
    	
    	if(component==null) {
    		IAnalysisDataset dataset = options.firstDataset();

    		if (!dataset.getCollection().hasConsensus())
    			return createTextAnnotatedEmptyChart(MULTIPLE_DATASETS_NO_CONSENSUS_ERROR);

    		component = dataset.getCollection().getConsensus();
    	}

        XYDataset ds;
        try {
            ds = new NucleusDatasetCreator(options).createAnnotatedNucleusOutline();
        } catch (ChartDatasetCreationException e) {
            stack("Error creating boxplot", e);
            return createErrorChart();
        }
        JFreeChart chart = makeConsensusChart(ds);

        double max = getConsensusChartRange(component);

        XYPlot plot = chart.getXYPlot();

        plot.getDomainAxis().setRange(-max, max);
        plot.getRangeAxis().setRange(-max, max);

        int seriesCount = plot.getSeriesCount();
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
        
        

//        for (int i = 0; i < seriesCount; i++) {
//            
//        }
        return chart;
    }

    
    /**
     * Get the maximum absolute range of the axes of the chart for
     * the given dataset.
     * 
     * @param dataset the dataset to range test
     * @return the maximum range value
     */
    private double getConsensusChartRange(CellularComponent component) {
        double maxX = Math.max(Math.abs(component.getMinX()),
                Math.abs(component.getMaxX()));
        double maxY = Math.max(Math.abs(component.getMinY()),
                Math.abs(component.getMaxY()));

        // ensure that the scales for each axis are the same
        double max = Math.max(maxX, maxY);

        // ensure there is room for expansion of the target nucleus due to IQR
        max *= 1.25;
        return max;
    }
    
    /**
     * Get the maximum absolute range of the axes of the chart. The minimum
     * returned value will be 1.
     * 
     * @return the chart maximum range in x or y
     */
    public double getconsensusChartRange() {

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

        if (!dataset.getCollection().hasConsensus())
        	return createTextAnnotatedEmptyChart(MULTIPLE_DATASETS_NO_CONSENSUS_ERROR);
        
        fine("Making segmented consenusus chart");
        XYDataset ds = null;

        ICellCollection collection = dataset.getCollection();
        try {
            ds = new NucleusDatasetCreator(options).createSegmentedNucleusOutline(collection);
        } catch (ChartDatasetCreationException e) {
            fine("Unable to make segmented outline, creating base outline instead: "+e.getMessage());
            return makeNucleusOutlineChart();
        }

        JFreeChart chart = makeConsensusChart(ds);
        double max = getConsensusChartRange(dataset.getCollection().getConsensus());

        XYPlot plot = chart.getXYPlot();
        plot.setDataset(0, ds);
        plot.getDomainAxis().setRange(-max, max);
        plot.getRangeAxis().setRange(-max, max);

        ColourSwatch swatch = GlobalOptions.getInstance().getSwatch();

        formatConsensusChartSeries(plot, true, swatch);

        return chart;
    }

    /**
     * Format the series colours for a consensus nucleus.
     * 
     * @param plot the chart plot
     * @param showIQR should the IQR be displayed
     * @param swatch the colour swatch
     */
    private void formatConsensusChartSeries(XYPlot plot, boolean showIQR, ColourSwatch swatch) {

        XYDataset ds = plot.getDataset();
        int seriesCount = plot.getSeriesCount();

        for (int i = 0; i < seriesCount; i++) {
            plot.getRenderer().setSeriesVisibleInLegend(i, false);
            String name = (String) ds.getSeriesKey(i);

            // colour the segments
            if (name.startsWith(NucleusDatasetCreator.SEGMENT_SERIES_PREFIX)) {

                plot.getRenderer().setSeriesStroke(i, ChartComponents.MARKER_STROKE);
                plot.getRenderer().setSeriesPaint(i, Color.BLACK);
            }

            // colour the quartiles
            if (name.startsWith(NucleusDatasetCreator.QUARTILE_SERIES_PREFIX)) {

                // get the segment component
                // The dataset series name is Q25_Seg_1 etc
                String segmentName = name.replaceAll("Q[2|7]5_", "");
                int segIndex = MorphologyChartFactory.getIndexFromLabel(segmentName);

                if (showIQR) {
                    plot.getRenderer().setSeriesStroke(i, ChartComponents.PROFILE_STROKE);
                    Paint colour = ColourSelecter.getColor(segIndex);
                    plot.getRenderer().setSeriesPaint(i, colour);

                } else {
                    plot.getRenderer().setSeriesVisible(i, false);
                }
            }
            
            if(name.startsWith(NucleusDatasetCreator.TAG_PREFIX)) {
            	plot.getRenderer().setSeriesStroke(i, new BasicStroke(8));
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
        XYDataset ds;
        try {
            ds = new NucleusDatasetCreator(options).createMultiNucleusOutline();
        } catch (ChartDatasetCreationException e) {
            fine("Error making consensus dataset", e);
            return createErrorChart();
        }
        JFreeChart chart = makeConsensusChart(ds);

        formatConsensusChart(chart);

        XYPlot plot = chart.getXYPlot();

        double max = getconsensusChartRange();

        plot.getDomainAxis().setRange(-max, max);
        plot.getRangeAxis().setRange(-max, max);

        int seriesCount = plot.getSeriesCount();

        for (int i = 0; i < seriesCount; i++) {
            plot.getRenderer().setSeriesVisibleInLegend(i, false);
            String name = (String) ds.getSeriesKey(i);
            plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));

            int index = MorphologyChartFactory.getIndexFromLabel(name);
            IAnalysisDataset d = options.getDatasets().get(index);

            // in this context, segment colour refers to the entire
            // dataset colour (they use the same pallates in ColourSelecter)
            Paint colour = options.getDatasets().get(i)
            		.getDatasetColour().orElse(ColourSelecter.getColor(i));

            // get the group id from the name, and make colour
            plot.getRenderer().setSeriesPaint(i, colour);
            if (name.startsWith(NucleusDatasetCreator.QUARTILE_SERIES_PREFIX)) {
                // make the IQR distinct from the median
                plot.getRenderer().setSeriesPaint(i, ((Color) colour).darker());
            }
        }
        return chart;
    }

    

}
