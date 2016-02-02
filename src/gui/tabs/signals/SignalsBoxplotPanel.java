package gui.tabs.signals;

import gui.components.ExportableChartPanel;
import gui.tabs.BoxplotsTabPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JScrollPane;

import org.jfree.chart.JFreeChart;

import stats.NucleusStatistic;
import stats.SignalStatistic;
import components.generic.MeasurementScale;
import charting.charts.BoxplotChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;

@SuppressWarnings("serial")
public class SignalsBoxplotPanel extends BoxplotsTabPanel {

	public SignalsBoxplotPanel(Logger logger){
		super(logger);
		createUI();
	}

	private void createUI(){

		this.setLayout(new BorderLayout());
		Dimension preferredSize = new Dimension(200, 300);

		for(SignalStatistic stat : SignalStatistic.values()){

			ChartOptionsBuilder builder = new ChartOptionsBuilder();
			ChartOptions options = builder.setDatasets(null)
					.setLogger(programLogger)
					.setStatistic(stat)
					.setScale(MeasurementScale.PIXELS)
					.build();

			JFreeChart chart = null;
			try {
				chart = BoxplotChartFactory.createSignalStatisticBoxplot(options);
			} catch (Exception e) {
				programLogger.log(Level.SEVERE, "Error creating boxplots panel", e);
			}

			ExportableChartPanel panel = new ExportableChartPanel(chart);
			panel.setPreferredSize(preferredSize);
			chartPanels.put(stat.toString(), panel);
			mainPanel.add(panel);
		}

		// add the scroll pane to the tab
		scrollPane  = new JScrollPane(mainPanel);
		this.add(scrollPane, BorderLayout.CENTER);

		measurementUnitSettingsPanel.addActionListener(this);
		measurementUnitSettingsPanel.setEnabled(false);
		this.add(measurementUnitSettingsPanel, BorderLayout.NORTH);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		update(getDatasets());

	}

	@Override
	protected void updateSingle() throws Exception {
		updateMultiple();

	}

	@Override
	protected void updateMultiple() throws Exception {
		measurementUnitSettingsPanel.setEnabled(true);
		MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();

		for(SignalStatistic stat : SignalStatistic.values()){

			ExportableChartPanel panel = chartPanels.get(stat.toString());

			JFreeChart chart = null;

			ChartOptionsBuilder builder = new ChartOptionsBuilder();
			ChartOptions options = builder.setDatasets(getDatasets())
					.setLogger(programLogger)
					.setStatistic(stat)
					.setScale(scale)
					.build();

			if(getChartCache().hasChart(options)){
				programLogger.log(Level.FINEST, "Using cached boxplot chart: "+stat.toString());
				chart = getChartCache().getChart(options);

			} else { // No cache

				chart = BoxplotChartFactory.createSignalStatisticBoxplot(options);
				getChartCache().addChart(options, chart);
				programLogger.log(Level.FINEST, "Added cached boxplot chart: "+stat.toString());
			}

			panel.setChart(chart);
		}

	}

	@Override
	protected void updateNull() throws Exception {
		updateMultiple();
		measurementUnitSettingsPanel.setEnabled(false);
	}

}

