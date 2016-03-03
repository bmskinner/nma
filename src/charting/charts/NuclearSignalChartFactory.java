package charting.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.UUID;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.category.CategoryDataset;

import charting.datasets.NuclearSignalDatasetCreator;
import charting.options.ChartOptions;

public class NuclearSignalChartFactory  extends AbstractChartFactory {
	
	
	private static JFreeChart createEmptyShellChart(){
		JFreeChart shellsChart = ChartFactory.createBarChart(null, "Shell", "Percent", null);
		shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
		return shellsChart;
	}
	
	public static JFreeChart createShellChart(ChartOptions options){
		
		if( ! options.hasDatasets()){
			return createEmptyShellChart();
		}
		
		CategoryDataset ds = NuclearSignalDatasetCreator.createShellBarChartDataset(options.getDatasets());
		JFreeChart chart = ChartFactory.createBarChart(null, "Outer <--- Shell ---> Interior", "Percent", ds);
		chart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		chart.getCategoryPlot().getRangeAxis().setRange(0,100);
		StatisticalBarRenderer rend = new StatisticalBarRenderer();
		rend.setBarPainter(new StandardBarPainter());
		rend.setShadowVisible(false);
		rend.setErrorIndicatorPaint(Color.black);
		rend.setErrorIndicatorStroke(new BasicStroke(2));
		chart.getCategoryPlot().setRenderer(rend);

		for (int j = 0; j < ds.getRowCount(); j++) {
			rend.setSeriesVisibleInLegend(j, Boolean.FALSE);
			rend.setSeriesStroke(j, new BasicStroke(2));
			UUID signalGroup = getSignalGroupFromLabel( (String) ds.getRowKey((j)));
			Color colour = options.firstDataset().getSignalGroupColour(signalGroup);
			rend.setSeriesPaint(j, colour);
		}	

		return chart;
	}

}
