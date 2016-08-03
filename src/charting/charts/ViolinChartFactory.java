package charting.charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import charting.datasets.NucleusDatasetCreator;
import charting.datasets.ViolinDatasetCreator;
import charting.options.ChartOptions;
import logging.Loggable;
import stats.NucleusStatistic;
import stats.PlottableStatistic;
import stats.SegmentStatistic;
import stats.SignalStatistic;

public class ViolinChartFactory implements Loggable {
	
private static ViolinChartFactory instance = null;
	
	private ViolinChartFactory(){

	}
	
	public static ViolinChartFactory getInstance(){
		if(instance==null){
			instance = new ViolinChartFactory();
		}
		return instance;
	}
	
	public JFreeChart makeEmptyChart(){
		
		return BoxplotChartFactory.getInstance().makeEmptyChart(); 
	}
	
	public JFreeChart createStatisticPlot(ChartOptions options) {
		
		if(!options.hasDatasets()){
			return makeEmptyChart();
		}
		
		PlottableStatistic stat = options.getStat();
		
		finest("Creating boxplot for "+stat);
		
		if(stat.getClass()==NucleusStatistic.class){
			return createNucleusStatisticPlot(options);
		}
		
//		if(stat.getClass()==SignalStatistic.class){
//			return createSignalStatisticBoxplot(options);
//		}
//		
//		if(stat.getClass()==SegmentStatistic.class){
//			return createSegmentBoxplot(options);
//		}
		
		return makeEmptyChart();
		
	}
	
	/*
	 * 
	 * PRIVATE METHODS
	 * 
	 */

	private JFreeChart createNucleusStatisticPlot(ChartOptions options) {
		
		ViolinCategoryDataset ds = null;
		if(options.getDatasets()!=null){
			 ds = ViolinDatasetCreator.getInstance().createNucleusStatisticViolinDataset(options);
		}
		
		CategoryPlot plot = new CategoryPlot();
		plot.setDataset(ds);

		
		ViolinRenderer rend = new ViolinRenderer();
		
		plot.setRenderer(rend);


		JFreeChart chart = new JFreeChart(plot);
		return chart;
		
	}

}
