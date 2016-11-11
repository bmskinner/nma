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
import java.awt.Paint;
import java.util.List;
import java.util.UUID;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import components.active.generic.UnavailableSignalGroupException;

import analysis.AnalysisDataset;
import analysis.IAnalysisDataset;
import charting.datasets.ChartDatasetCreationException;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.datasets.NucleusDatasetCreator;
import charting.options.ChartOptions;

/**
 * This factory creates boxplot charts. It uses a singleton pattern to allow the loggable
 * interface to be used
 * @author bms41
 *
 */
public class BoxplotChartFactory extends AbstractChartFactory {

	public BoxplotChartFactory(ChartOptions o){
		super(o);
	}
	
	/**
	 * Create an empty boxplot
	 * @return
	 */
	public static JFreeChart makeEmptyChart(){
		
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, 
				null, 
				null,
				new DefaultBoxAndWhiskerCategoryDataset(), 
				false);	
		
		formatBoxplot(boxplot);
		return boxplot;
	}
	
	public JFreeChart createStatisticBoxplot() {
		
		if(!options.hasDatasets()){
			return makeEmptyChart();
		}
		
		PlottableStatistic stat = options.getStat();
		
		finest("Creating boxplot for "+stat);
		
		if(stat.getClass()==NucleusStatistic.class){
			return createNucleusStatisticBoxplot();
		}
		
		if(stat.getClass()==SignalStatistic.class){
			return createSignalStatisticBoxplot();
		}
		
		if(stat.getClass()==SegmentStatistic.class){
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
		if(options.getDatasets()!=null){
			 try {
				ds = new NucleusDatasetCreator().createBoxplotDataset(options);
			} catch (ChartDatasetCreationException e) {
				fine("Error creating boxplot", e);
				return makeErrorChart();
			}
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
	private JFreeChart createSegmentBoxplot() {

		SegmentStatistic stat = (SegmentStatistic) options.getStat();
		
		BoxAndWhiskerCategoryDataset ds;
		try {
			ds = new NucleusDatasetCreator().createSegmentStatDataset(options);
		} catch (ChartDatasetCreationException e) {
			fine("Error creating boxplot", e);
			return makeErrorChart();
		}
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

				Paint color = options.getDatasets().get(series).getDatasetColour() == null 
						? ColourSelecter.getColor(series)
								: options.getDatasets().get(series).getDatasetColour();

						renderer.setSeriesPaint(series, color);
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
	 * @param options
	 * @return
	 * @throws Exception
	 */
	private JFreeChart createSignalStatisticBoxplot(){
		
		BoxAndWhiskerCategoryDataset ds;
		try {
			ds = new NuclearSignalDatasetCreator().createSignalStatisticBoxplotDataset(options);
		} catch (ChartDatasetCreationException e) {
			return makeErrorChart();
		}

		
		JFreeChart boxplot = ChartFactory.createBoxAndWhiskerChart(null, 
				null, 
				options.getStat().label(options.getScale()), 
				ds, 
				false);
		
		formatBoxplot(boxplot);
		
		CategoryPlot plot = boxplot.getCategoryPlot();
		
		plot.getDomainAxis().setCategoryMargin(0.10);
		plot.getDomainAxis().setLowerMargin(0.05);
		plot.getDomainAxis().setUpperMargin(0.05);
		
		BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();
		renderer.setItemMargin(0.05);
		renderer.setMaximumBarWidth(0.5);



		
		int series=0;
		for(int column=0; column<ds.getColumnCount(); column++){
	
			// The column is the dataset
//			String datasetName = ds.getColumnKey(column).toString();
//			log("Looking at dataset "+datasetName);
			IAnalysisDataset d  = options.getDatasets().get(column);
						
			for(int row=0; row<ds.getRowCount(); row++){
												
//				log("Series "+series);
				String name = (String) ds.getRowKey(row);
//				log("Looking at row "+name);
				
				UUID signalGroup = getSignalGroupFromLabel(name);
				
				// Not every dataset will have every row.
				if(d.getCollection().hasSignalGroup(signalGroup)){
					Paint color = ColourSelecter.getColor(row);
					try {


					color = d.getCollection().getSignalGroup(signalGroup).hasColour()
							    ? d.getCollection().getSignalGroup(signalGroup).getGroupColour()
								: color;
							    
					   
					} catch (UnavailableSignalGroupException e){
	        			fine("Signal group "+signalGroup+" is not present in collection", e);
	        		} finally {
	        			renderer.setSeriesPaint(series, color);
	        			series++;
	        		}
				}

				
			}		
		}
		return boxplot;
	}
		
	/**
	 * Apply the default formatting to a boxplot with list
	 * @param boxplot
	 */
	private void formatBoxplotChart(JFreeChart boxplot, List<IAnalysisDataset> list){
		formatBoxplot(boxplot);
		CategoryPlot plot = boxplot.getCategoryPlot();
		BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();
		
		for(int i=0;i<plot.getDataset().getRowCount();i++){
			
			IAnalysisDataset d = list.get(i);

			Paint color = d.hasDatasetColour()
						? d.getDatasetColour()
						: ColourSelecter.getColor(i);
						
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
