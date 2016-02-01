package gui.tabs.signals;

import gui.components.HistogramsTabPanel;
import gui.components.SelectableChartPanel;

import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import stats.SignalStatistic;
import charting.charts.HistogramChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.generic.MeasurementScale;

@SuppressWarnings("serial")
public class SignalsHistogramPanel extends HistogramsTabPanel {
    	  	    	
	public SignalsHistogramPanel(Logger programLogger) throws Exception{
		super(programLogger);
		
		try {

			MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();
			Dimension preferredSize = new Dimension(400, 150);
			for(SignalStatistic stat : SignalStatistic.values()){

				ChartOptionsBuilder builder = new ChartOptionsBuilder();
				ChartOptions options = builder.setDatasets(null)
					.setLogger(programLogger)
					.setStatistic(stat)
					.setScale(scale)
					.setUseDensity(false)
					.setSignalGroup(0)
					.build();
				
				SelectableChartPanel panel = new SelectableChartPanel(HistogramChartFactory.createSignalStatisticHistogram(options), stat.toString());
				panel.setPreferredSize(preferredSize);
				panel.addSignalChangeListener(this);
				chartPanels.put(stat.toString(), panel);
				mainPanel.add(panel);

			}

		} catch(Exception e){
			programLogger.log(Level.SEVERE, "Error creating histogram panel", e);
		}
		
	}
	
	@Override
	protected void updateSingle() throws Exception {
		this.setEnabled(true);
		int signalGroup = 1; //TODO - get the number  of signal groups in the selected datasets, and iterate 
		
		MeasurementScale scale  = measurementUnitSettingsPanel.getSelected();
		boolean useDensity = useDensityPanel.isSelected();
		
		for(SignalStatistic stat : SignalStatistic.values()){
			SelectableChartPanel panel = chartPanels.get(stat.toString());

			JFreeChart chart = null;
			
			ChartOptionsBuilder builder = new ChartOptionsBuilder();
			ChartOptions options = builder.setDatasets(getDatasets())
				.setLogger(programLogger)
				.setStatistic(stat)
				.setScale(scale)
				.setUseDensity(useDensity)
				.setSignalGroup(signalGroup)
				.build();
			

			if(this.getChartCache().hasChart(options)){
				programLogger.log(Level.FINEST, "Using cached histogram: "+stat.toString());
				chart = getChartCache().getChart(options);

			} else { // No cache


				if(useDensity){
					//TODO - make the density chart
					chart = HistogramChartFactory.createSignalDensityStatsChart(options);
					getChartCache().addChart(options, chart);

				} else {
					chart = HistogramChartFactory.createSignalStatisticHistogram(options);
					getChartCache().addChart(options, chart);

				}
				programLogger.log(Level.FINEST, "Added cached histogram chart: "+stat);
			}

			XYPlot plot = (XYPlot) chart.getPlot();
			plot.setDomainPannable(true);
			plot.setRangePannable(true);

			panel.setChart(chart);
		}
		
		
	}

	@Override
	protected void updateMultiple() throws Exception {
		 updateNull();
		
	}

	@Override
	protected void updateNull() throws Exception {
		this.setEnabled(false);
		
	}
	
}
