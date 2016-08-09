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
package charting.charts;

import java.awt.Color;
import java.util.UUID;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.data.xy.XYDataset;

import analysis.AnalysisDataset;
import charting.ChartComponents;
import charting.datasets.ScatterChartDatasetCreator;
import charting.options.ChartOptions;
import gui.components.ColourSelecter;
import stats.NucleusStatistic;
import stats.PlottableStatistic;
import stats.SignalStatistic;

public class ScatterChartFactory extends AbstractChartFactory {
	
	private static ScatterChartFactory instance = null;
	
	protected ScatterChartFactory(){}
	
	/**
	 * Fetch an instance of the factory
	 * @return
	 */
	public static ScatterChartFactory getInstance(){
		if(instance==null){
			instance = new ScatterChartFactory();
		}
		return instance;
	}
	
	/**
	 * Create a blank scatter chart
	 * @return
	 */
	public JFreeChart makeEmptyChart(){
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
	public JFreeChart createScatterChart(ChartOptions options){
		
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
		
		if(firstStat.getClass().equals(NucleusStatistic.class)){
			return createNucleusStatisticScatterChart(options);
		}
		
		if(firstStat.getClass().equals(SignalStatistic.class)){
			return createSignalStatisticScatterChart(options);
		}
		
		return makeEmptyChart();
	}
	
	
	/**
	 * Create a scatter plot of two nucleus statistics
	 * @param options
	 * @return
	 */
	private JFreeChart createNucleusStatisticScatterChart(ChartOptions options){
				
		XYDataset ds = ScatterChartDatasetCreator.getInstance().createNucleusScatterDataset(options);
		
		String xLabel = options.getStat(0).label(options.getScale());
		String yLabel = options.getStat(1).label(options.getScale());
		
		JFreeChart chart = ChartFactory.createXYLineChart(null, xLabel,
				yLabel,  ds);  
		
		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setAutoRangeIncludesZero(false);
		
		DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
		renderer.setBaseShapesVisible(true);
		renderer.setBaseShape(ChartComponents.DEFAULT_POINT_SHAPE);
		plot.setRenderer(renderer);
		
		int seriesCount = plot.getDataset().getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			
			renderer.setSeriesVisibleInLegend(i, false);
			renderer.setSeriesLinesVisible(i, false);
			renderer.setSeriesShape(i, ChartComponents.DEFAULT_POINT_SHAPE);
			
			Color colour = ColourSelecter.getColor(i);
			
			if(options.getDatasets().get(i).hasDatasetColour()){
				colour = options.getDatasets().get(i).getDatasetColour();
			}
								
			renderer.setSeriesPaint(i, colour);
			 
		}	
		return chart;
	}
	
	/**
	 * Create a scatter plot of two nucleus statistics
	 * @param options
	 * @return
	 */
	private JFreeChart createSignalStatisticScatterChart(ChartOptions options){
				
		XYDataset ds = ScatterChartDatasetCreator.getInstance().createSignalScatterDataset(options);
		
		String xLabel = options.getStat(0).label(options.getScale());
		String yLabel = options.getStat(1).label(options.getScale());
		
		JFreeChart chart = ChartFactory.createXYLineChart(null, xLabel,
				yLabel,  ds);  
		
		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);
		
		NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
		yAxis.setAutoRangeIncludesZero(false);
		
		DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
		renderer.setBaseShapesVisible(true);
		renderer.setBaseShape(ChartComponents.DEFAULT_POINT_SHAPE);
		plot.setRenderer(renderer);
		
		int seriesCount = plot.getDataset().getSeriesCount();
		
		for (int i = 0; i < seriesCount; i++) {
			
			renderer.setSeriesVisibleInLegend(i, false);
			renderer.setSeriesLinesVisible(i, false);
			renderer.setSeriesShape(i, ChartComponents.DEFAULT_POINT_SHAPE);
			
			String seriesName = ds.getSeriesKey(i).toString();
			String[] split = seriesName.split("\\|");
			String datasetName = split[0];
			UUID id = UUID.fromString(split[1]);
			
			Color colour = ColourSelecter.getColor(i);
			
			for(AnalysisDataset d : options.getDatasets()){
				if(d.getName().equals(datasetName)){
					colour = d.getCollection().getSignalGroup(id).hasColour()
							? d.getCollection().getSignalGroup(id).getGroupColour()
							: colour;
				}
			}				
			renderer.setSeriesPaint(i, colour);
			 
		}	
		return chart;
	}
}
