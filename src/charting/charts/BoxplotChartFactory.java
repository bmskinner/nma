/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package charting.charts;

import gui.components.ColourSelecter;
import stats.NucleusStatistic;
import stats.PlottableStatistic;
import stats.SegmentStatistic;
import stats.SignalStatistic;

import java.awt.Color;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import analysis.AnalysisDataset;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.datasets.NucleusDatasetCreator;
import charting.options.ChartOptions;

public class BoxplotChartFactory extends AbstractChartFactory {
	
	/**
	 * Create an empty boxplot
	 * @return
	 */
	public static JFreeChart createEmptyBoxplot(){
		
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, 
				null, 
				null,
				new DefaultBoxAndWhiskerCategoryDataset(), 
				false);	
		
		formatBoxplot(boxplot);
		return boxplot;
	}
	
	public static JFreeChart createStatisticBoxplot(ChartOptions options) throws Exception{
		
		if(!options.hasDatasets()){
			return createEmptyBoxplot();
		}
		
		PlottableStatistic stat = options.getStat();
		
		if(stat.getClass()==NucleusStatistic.class){
			return createNucleusStatisticBoxplot(options);
		}
		
		if(stat.getClass()==SignalStatistic.class){
			return createSignalStatisticBoxplot(options);
		}
		
		if(stat.getClass()==SegmentStatistic.class){
			return createSegmentBoxplot(options);
		}
		
		return createEmptyBoxplot();
		
	}
	
	/*
	 * 
	 * PRIVATE METHODS
	 * 
	 */

	private static JFreeChart createNucleusStatisticBoxplot(ChartOptions options) throws Exception{
		
		BoxAndWhiskerCategoryDataset ds = null;
		if(options.getDatasets()!=null){
			 ds = NucleusDatasetCreator.createBoxplotDataset(options);
		}

		
		String yLabel = options.getStat().label(options.getScale());

		JFreeChart 	boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, yLabel, ds, false); 
		formatBoxplotChart(boxplotChart, options.getDatasets());
		return boxplotChart;
		
	}
	
	
	/**
	 * Create a segment length boxplot for the given segment name
	 * @param ds the dataset
	 * @return
	 */
	private static JFreeChart createSegmentBoxplot(ChartOptions options) throws Exception {

		SegmentStatistic stat = (SegmentStatistic) options.getStat();
		
		BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createSegmentStatDataset(options);
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, 
				null, 
				"Segment "+stat.label(options.getScale())
				, ds
				, false);	
		
		formatBoxplot(boxplot);
		CategoryPlot plot = boxplot.getCategoryPlot();
		
		for(int datasetIndex = 0; datasetIndex< plot.getDatasetCount(); datasetIndex++){

			BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();

			for(int series=0;series<plot.getDataset(datasetIndex).getRowCount();series++){

				Color color = options.getDatasets().get(series).getDatasetColour() == null 
						? ColourSelecter.getSegmentColor(series)
								: options.getDatasets().get(series).getDatasetColour();

						renderer.setSeriesPaint(series, color);
						renderer.setSeriesOutlinePaint(series, Color.BLACK);
			}

			renderer.setMeanVisible(false);
			plot.setRenderer(datasetIndex, renderer);
		}
			
		return boxplot;
	}
		
	/**
	 * Create a signal boxplot with the given options
	 * @param options
	 * @return
	 * @throws Exception
	 */
	private static JFreeChart createSignalStatisticBoxplot(ChartOptions options) throws Exception{
		
		BoxAndWhiskerCategoryDataset ds = NuclearSignalDatasetCreator.createSignalStatisticBoxplotDataset(options);

		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, 
				null, 
				options.getStat().label(options.getScale()), 
				ds, 
				false);
		
		formatBoxplot(boxplot);
		
		CategoryPlot plot = boxplot.getCategoryPlot();
		BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();

		for(int series=0;series<ds.getRowCount();series++){
			String name = (String) ds.getRowKey(series);
			int seriesGroup = getIndexFromLabel(name);

			Color color = options.firstDataset().getSignalGroupColour(seriesGroup) == null 
					? ColourSelecter.getSegmentColor(series)
							: options.firstDataset().getSignalGroupColour(seriesGroup);

					renderer.setSeriesPaint(series, color);
		}		
		return boxplot;
	}
		
	/**
	 * Apply the default formatting to a boxplot with list
	 * @param boxplot
	 */
	private static void formatBoxplotChart(JFreeChart boxplot, List<AnalysisDataset> list){
		formatBoxplot(boxplot);
		CategoryPlot plot = boxplot.getCategoryPlot();
		BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();
		
		for(int i=0;i<plot.getDataset().getRowCount();i++){
			
			AnalysisDataset d = list.get(i);

			Color color = d.hasDatasetColour()
						? d.getDatasetColour()
						: ColourSelecter.getSegmentColor(i);
						
						renderer.setSeriesPaint(i, color);
		}
	}
	
	/**
	 * Apply basic formatting to the charts, without any series added
	 * @param boxplot
	 */
	private static void formatBoxplot(JFreeChart boxplot){
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
