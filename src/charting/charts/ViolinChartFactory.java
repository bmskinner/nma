package charting.charts;

import gui.components.ColourSelecter;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
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
	
	private static JFreeChart createViolinChart(String title,
            String categoryAxisLabel, String valueAxisLabel,
            ViolinCategoryDataset dataset, boolean legend) {
        
        CategoryAxis categoryAxis = new CategoryAxis(categoryAxisLabel);
        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        valueAxis.setAutoRangeIncludesZero(false);
        
        ViolinRenderer renderer = new ViolinRenderer();
//        renderer.setToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
           
        CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, 
                renderer);
        return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, 
                legend);
    } 

	private JFreeChart createNucleusStatisticPlot(ChartOptions options) {
		
		ViolinCategoryDataset ds = null;
		if(options.hasDatasets()){
			 ds = ViolinDatasetCreator.getInstance().createNucleusStatisticViolinDataset(options);
		}
		
		JFreeChart chart = createViolinChart(null, null, options.getStat().label(options.getScale()), ds, true);
		
//		log("Making violin chart");
		
		CategoryPlot plot = chart.getCategoryPlot();
		ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();

		for(int datasetIndex = 0; datasetIndex< plot.getDatasetCount(); datasetIndex++){

			for(int series=0;series<plot.getDataset(datasetIndex).getRowCount();series++){

				renderer.setSeriesVisibleInLegend(series, false);
				Color color = options.getDatasets().get(series).getDatasetColour() == null 
						? ColourSelecter.getSegmentColor(series)
								: options.getDatasets().get(series).getDatasetColour();

						renderer.setSeriesPaint(series, color);
						renderer.setSeriesOutlinePaint(series, Color.BLACK);
			}
		}
		
		if(ds.hasProbabilities()){
			plot.getRangeAxis().setRange(ds.getProbabiltyRange());
		}
				
		return chart;
		
	}

}
