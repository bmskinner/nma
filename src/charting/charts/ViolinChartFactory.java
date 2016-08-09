package charting.charts;

import gui.components.ColourSelecter;

import java.awt.Color;
import java.util.UUID;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import analysis.AnalysisDataset;
import charting.datasets.NuclearSignalDatasetCreator;
import charting.datasets.NucleusDatasetCreator;
import charting.datasets.ViolinDatasetCreator;
import charting.options.ChartOptions;
import logging.Loggable;
import stats.NucleusStatistic;
import stats.PlottableStatistic;
import stats.SegmentStatistic;
import stats.SignalStatistic;

public class ViolinChartFactory extends AbstractChartFactory implements Loggable {
	
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
		
		try {
		
			if(stat.getClass()==NucleusStatistic.class){
				return createNucleusStatisticPlot(options);
			}

			if(stat.getClass()==SignalStatistic.class){
				return createSignalStatisticPlot(options);
			}

			if(stat.getClass()==SegmentStatistic.class){
				return createSegmentPlot(options);
			}
		} catch(Exception e){
			error("Error making violin chart", e);
			return makeEmptyChart();
		}
		
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
		
		JFreeChart chart = createViolinChart(null, null, options.getStat().label(options.getScale()), ds, false);
		
//		log("Making violin chart");
		
		CategoryPlot plot = chart.getCategoryPlot();
		ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();
		
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(true);

		for(int datasetIndex = 0; datasetIndex< plot.getDatasetCount(); datasetIndex++){

			for(int series=0;series<plot.getDataset(datasetIndex).getRowCount();series++){

				renderer.setSeriesVisibleInLegend(series, false);
				Color color = options.getDatasets().get(series).getDatasetColour() == null 
						? ColourSelecter.getColor(series)
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
	
	/**
	 * Create a signal boxplot with the given options
	 * @param options
	 * @return
	 * @throws Exception
	 */
	private JFreeChart createSignalStatisticPlot(ChartOptions options){
		
		ViolinCategoryDataset ds = null;
		if(options.hasDatasets()){
			 ds = ViolinDatasetCreator.getInstance().createSignalStatisticViolinDataset(options);
		}
		
		JFreeChart chart = createViolinChart(null, 
				null, 
				options.getStat().label(options.getScale()), 
				ds, 
				false);
		
		
		CategoryPlot plot = chart.getCategoryPlot();
		
		plot.getDomainAxis().setCategoryMargin(0.10);
		plot.getDomainAxis().setLowerMargin(0.05);
		plot.getDomainAxis().setUpperMargin(0.05);
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(true);
		
		ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();
		renderer.setItemMargin(0.05);
		renderer.setMaximumBarWidth(0.5);



		
		int series=0;
		for(int column=0; column<ds.getColumnCount(); column++){
	
			// The column is the dataset
//			String datasetName = ds.getColumnKey(column).toString();
//			log("Looking at dataset "+datasetName);
			AnalysisDataset d  = options.getDatasets().get(column);
						
			for(int row=0; row<ds.getRowCount(); row++){
												
//				log("Series "+series);
				String name = (String) ds.getRowKey(row);
//				log("Looking at row "+name);
				
				UUID signalGroup = getSignalGroupFromLabel(name);
				
				// Not every dataset will have every row.
				if(d.getCollection().hasSignalGroup(signalGroup)){


					Color color = d.getCollection().getSignalGroup(signalGroup).hasColour()
							    ? d.getCollection().getSignalGroup(signalGroup).getGroupColour()
								: ColourSelecter.getColor(row);
							    
					    renderer.setSeriesPaint(series, color);
						series++;
				}

				
			}		
		}
		
		if(ds.hasProbabilities()){
			plot.getRangeAxis().setRange(ds.getProbabiltyRange());
		}
		
		return chart;
	}
	
	/**
	 * Create a segment length boxplot for the given segment name
	 * @param ds the dataset
	 * @return
	 */
	private JFreeChart createSegmentPlot(ChartOptions options) {
		
		ViolinCategoryDataset ds = null;
		if(options.hasDatasets()){
			 ds = ViolinDatasetCreator.getInstance().createSegmentStatisticDataset(options);
		}
		
		JFreeChart chart = createViolinChart(null, 
				null, 
				options.getStat().label(options.getScale()), 
				ds, 
				false);
		

		CategoryPlot plot = chart.getCategoryPlot();
		ViolinRenderer renderer = (ViolinRenderer) plot.getRenderer();
		
		for(int datasetIndex = 0; datasetIndex< plot.getDatasetCount(); datasetIndex++){

			for(int series=0;series<plot.getDataset(datasetIndex).getRowCount();series++){

				Color color = options.getDatasets().get(series).getDatasetColour() == null 
						? ColourSelecter.getColor(series)
								: options.getDatasets().get(series).getDatasetColour();

						renderer.setSeriesPaint(series, color);
						renderer.setSeriesOutlinePaint(series, Color.BLACK);
			}

		}
			
		return chart;
	}

}
