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
import stats.SegmentStatistic;

import java.awt.Color;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import components.generic.MeasurementScale;
import analysis.AnalysisDataset;
import charting.ChartComponents;
import charting.datasets.NucleusDatasetCreator;

public class BoxplotChartFactory {
	
	/**
	 * Create an empty boxplot
	 * @return
	 */
	public static JFreeChart makeEmptyBoxplot(){
		
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	
		formatBoxplot(boxplot);
		return boxplot;
	}
	
	
	public static JFreeChart createNucleusStatisticBoxplot(BoxplotChartOptions options) throws Exception{
		
		BoxAndWhiskerCategoryDataset ds = null;
		if(options.getDatasets()!=null){
			 ds = NucleusDatasetCreator.createBoxplotDataset(options);
		}
		NucleusStatistic stat = (NucleusStatistic) options.getStat();
		String yLabel = options.getScale().yLabel(stat);

		JFreeChart 	boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, yLabel, ds, false); 
		formatBoxplotChart(boxplotChart, options.getDatasets());
		return boxplotChart;
		
	}
	
	
	/**
	 * Create a segment length boxplot for the given segment name
	 * @param ds the dataset
	 * @return
	 */
	public static JFreeChart makeSegmentBoxplot(String segName, List<AnalysisDataset> list, MeasurementScale scale, SegmentStatistic stat) throws Exception {

		if(list==null){
			return makeEmptyBoxplot();
		}
		
		BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createSegmentStatDataset(list, segName, scale, stat);
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, null, "Segment "+stat.toString()+" ("+scale.toString()+")", ds, false);	
		
		formatBoxplot(boxplot);
		CategoryPlot plot = boxplot.getCategoryPlot();
				
		if(list!=null && !list.isEmpty()){
						
			for(int datasetIndex = 0; datasetIndex< plot.getDatasetCount(); datasetIndex++){
			
				CategoryDataset dataset = plot.getDataset(datasetIndex);
				BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
				
				for(int series=0;series<plot.getDataset(datasetIndex).getRowCount();series++){

//					int segIndex = MorphologyChartFactory.getIndexFromLabel(segName);
					
					Color color = list.get(series).getDatasetColour() == null 
							? ColourSelecter.getSegmentColor(series)
							: list.get(series).getDatasetColour();

					renderer.setSeriesPaint(series, color);
					renderer.setSeriesOutlinePaint(series, Color.BLACK);
				}
				
				renderer.setMeanVisible(false);
				plot.setRenderer(datasetIndex, renderer);
			}
		}		
		return boxplot;
	}
	
	/**
	 * Create and format a boxplot based on a dataset
	 * @param ds the dataset
	 * @return
	 */
	public static JFreeChart makeSegmentBoxplot(BoxAndWhiskerCategoryDataset ds, List<AnalysisDataset> list){
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, null, "Index length difference\nto segment in median", ds, false);	
		
		
		if(ds==null || list==null){
			return makeEmptyBoxplot();
		}
		
		formatBoxplot(boxplot);
		CategoryPlot plot = boxplot.getCategoryPlot();
				
		if(list!=null && !list.isEmpty()){
						
			for(int datasetIndex = 0; datasetIndex< plot.getDatasetCount(); datasetIndex++){
			
				CategoryDataset dataset = plot.getDataset(datasetIndex);
				BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
				
				for(int series=0;series<plot.getDataset(datasetIndex).getRowCount();series++){

					String segName = (String) dataset.getRowKey(series);
					int segIndex = MorphologyChartFactory.getIndexFromLabel(segName);
					
					Color color = list.get(0).getSwatch().color(segIndex);
//					Color color = ColourSelecter.getOptimisedColor(segIndex);
					renderer.setSeriesPaint(series, color);
//					renderer.setSeriesFillPaint(series, color);
					renderer.setSeriesOutlinePaint(series, Color.BLACK);
				}
				
				renderer.setMeanVisible(false);
				renderer.setItemMargin(0.08);
				renderer.setMaximumBarWidth(0.10);
				plot.setRenderer(datasetIndex, renderer);
			}
		}
		
		ValueMarker zeroMarker =
	              new ValueMarker(0.00, Color.black, ChartComponents.PROFILE_STROKE);

	      plot.addRangeMarker(zeroMarker);
		
		return boxplot;
	}
	
	public static JFreeChart makeSignalAreaBoxplot(BoxAndWhiskerCategoryDataset ds, AnalysisDataset dataset){
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false);
		formatBoxplot(boxplot);
		
		CategoryPlot plot = boxplot.getCategoryPlot();
		BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();

		for(int series=0;series<ds.getRowCount();series++){
			String name = (String) ds.getRowKey(series);
			int seriesGroup = MorphologyChartFactory.getIndexFromLabel(name);

			Color color = dataset.getSignalGroupColour(seriesGroup) == null 
					? ColourSelecter.getSegmentColor(series)
							: dataset.getSignalGroupColour(seriesGroup);

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
