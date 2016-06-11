package charting.charts;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.data.xy.XYDataset;

import charting.ChartComponents;
import charting.datasets.ScatterChartDatasetCreator;
import charting.options.ChartOptions;
import gui.components.ColourSelecter;
import gui.components.ColourSelecter.ColourSwatch;
import stats.NucleusStatistic;
import stats.PlottableStatistic;

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
	public JFreeChart createEmptyScatterChart(){
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
			return createEmptyScatterChart();
		}
		
		if(options.getStats().size()!=2){
			return createEmptyScatterChart();
		}
		
		PlottableStatistic firstStat = options.getStat();
		
		for(PlottableStatistic stat : options.getStats()){
			if( ! stat.getClass().equals(firstStat.getClass())){
				fine("Statistic classes are different");
				return createEmptyScatterChart();
			}
		}
		
		if(firstStat.getClass().equals(NucleusStatistic.class)){
			return createNucleusStatisticScatterChart(options);
		}
		
		return createEmptyScatterChart();
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
		
		DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
		renderer.setBaseShapesVisible(true);
		renderer.setBaseShape(ChartComponents.DEFAULT_POINT_SHAPE);
		plot.setRenderer(renderer);
		
		int seriesCount = plot.getDataset().getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			
			renderer.setSeriesVisibleInLegend(i, false);
			renderer.setSeriesLinesVisible(i, false);
			renderer.setSeriesShape(i, ChartComponents.DEFAULT_POINT_SHAPE);
			
			Color colour = ColourSelecter.getSegmentColor(i);
			
			if(options.getDatasets().get(i).hasDatasetColour()){
				colour = options.getDatasets().get(i).getDatasetColour();
			}
								
			renderer.setSeriesPaint(i, colour);
			 
		}	
		return chart;
	}
}
