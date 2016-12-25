/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.charting.charts;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.util.UUID;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.charting.ChartComponents;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.ScatterChartDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.SignalXYDataset;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;

public class ScatterChartFactory extends AbstractChartFactory {
	
	public ScatterChartFactory(ChartOptions o){
		super(o);
	}
	
	/**
	 * Create a blank scatter chart
	 * @return
	 */
	public static JFreeChart makeEmptyChart(){
		JFreeChart chart = ChartFactory.createXYLineChart(null,
				null, null, null); 
		chart.getXYPlot().setBackgroundPaint(Color.WHITE);
		return chart;
	}
	
	/**
	 * Create a scatter plot of two nucleus statistics
	 * @param options
	 * @return
	 */
	public JFreeChart createScatterChart(){
		
		if( ! options.hasDatasets()){
			return makeEmptyChart();
		}
		
		if(options.getStats().size()!=2){
			return makeEmptyChart();
		}
		
		PlottableStatistic firstStat = options.getStat();
		
		for(PlottableStatistic stat : options.getStats()){
			if( ! stat.getClass().equals(firstStat.getClass())){
				fine("Statistic classes are different");
				return makeEmptyChart();
			}
		}
		
		if(firstStat instanceof NucleusStatistic){
			return createNucleusStatisticScatterChart();
		}
		
		if(firstStat instanceof SignalStatistic){
			return createSignalStatisticScatterChart();
		}
		
		return makeEmptyChart();
	}
	
	
	/**
	 * Create a scatter plot of two nucleus statistics
	 * @param options
	 * @return
	 */
	private JFreeChart createNucleusStatisticScatterChart(){
				
		XYDataset ds;
		try {
			ds = new ScatterChartDatasetCreator(options).createScatterDataset();
		} catch (ChartDatasetCreationException e) {
			stack("Error creating scatter dataset", e);
			return makeErrorChart();
		}
		
		String xLabel = options.getStat(0).label(options.getScale());
		String yLabel = options.getStat(1).label(options.getScale());

		JFreeChart chart = createBaseXYChart(xLabel, yLabel, ds);
		
		XYPlot plot = chart.getXYPlot();
		
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setAutoRangeIncludesZero(false);
		
		XYItemRenderer renderer = new ScatterChartRenderer();
		plot.setRenderer(renderer);

		applySingleXYDatasetColours(plot);
		
		return chart;
	}
	
	/**
	 * Create a scatter plot of two nucleus statistics
	 * @param options
	 * @return
	 */
	private JFreeChart createSignalStatisticScatterChart(){
				
		SignalXYDataset ds;
		try {
			ds = (SignalXYDataset) new ScatterChartDatasetCreator(options).createScatterDataset();
		} catch (ChartDatasetCreationException e) {
			stack("Error creating scatter dataset", e);
			return makeErrorChart();
		}
		
		String xLabel = options.getStat(0).label(options.getScale());
		String yLabel = options.getStat(1).label(options.getScale());
		
		JFreeChart chart = createBaseXYChart(xLabel, yLabel, ds);
		
		XYPlot plot = chart.getXYPlot();
		
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setAutoRangeIncludesZero(false);
		
		XYItemRenderer renderer = new ScatterChartRenderer();
		
		plot.setRenderer(renderer);
		
		int seriesCount = plot.getDataset().getSeriesCount();
		
		for (int i = 0; i < seriesCount; i++) {
			
			String seriesKey = ds.getSeriesKey(i).toString();
			ds.getSignalGroup(seriesKey);
			
			IAnalysisDataset d = ds.getDataset(seriesKey);
			UUID id = ds.getSignalId(seriesKey);
			
			Paint colour = ColourSelecter.getColor(i);
	
			try {

				if(d.getCollection().hasSignalGroup(id)){
					colour = d.getCollection().getSignalGroup(id).hasColour()
							? d.getCollection().getSignalGroup(id).getGroupColour()
									: colour;
				}
			} catch (UnavailableSignalGroupException e){
				stack("Signal group "+id+" is not present in collection", e);
			}


			renderer.setSeriesPaint(i, colour);
			 
		}	
		return chart;
	}
	
	/**
	 * Overrides the methods of the DefaultXYItemRenderer to use a consistent
	 * point shape and not display lines.
	 * @author ben
	 * @since 1.13.4
	 *
	 */
	private class ScatterChartRenderer extends DefaultXYItemRenderer {

		private static final long serialVersionUID = 1L;

		public ScatterChartRenderer(){
			super();
			setBaseShapesVisible(true);
			setBaseShape(ChartComponents.DEFAULT_POINT_SHAPE);	
		}
		
		@Override
		public Boolean getSeriesLinesVisible(int series){
			return false;
		}
		
		@Override
		public Boolean getSeriesVisibleInLegend(int series){
			return false;
		}
		
		@Override
		public Shape getSeriesShape(int series){
			return this.getBaseShape();
		}
		
		
		
		
	}
}
