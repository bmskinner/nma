package com.bmskinner.nma.gui.tabs.comparisons;

import java.awt.BorderLayout;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;
import com.bmskinner.nma.gui.tabs.ChartDetailPanel;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.VennChartFactory;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

public class VennChartPanel extends ChartDetailPanel {

	private static final Logger LOGGER = Logger.getLogger(VennChartPanel.class.getName());
	private static final String PANEL_TITLE_LBL = "Venn chart";

	private ExportableChartPanel chartPanel;

	public VennChartPanel() {
		super();

		setLayout(new BorderLayout());
		chartPanel = new ExportableChartPanel(VennChartFactory.createEmptyChart());
		chartPanel.setFixedAspectRatio(true);
		chartPanel.setPannable(true);
		this.add(chartPanel, BorderLayout.CENTER);
	}

	@Override
	public String getPanelTitle() {
		return PANEL_TITLE_LBL;
	}

	@Override
	protected synchronized void updateMultiple() {

		ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setFillConsensus(GlobalOptions.getInstance().isFillConsensus())
				.setScale(GlobalOptions.getInstance().getScale())
				.setSwatch(GlobalOptions.getInstance().getSwatch())
//				.setShowXAxis(false)
//				.setShowYAxis(false)
				.setTarget(chartPanel)
				.build();

		setChart(options);
	}

	@Override
	protected synchronized void updateSingle() {
		updateMultiple();
	}

	@Override
	protected synchronized void updateNull() {
		updateMultiple();
	}

	@Override
	public synchronized void setLoading() {
		super.setLoading();
		chartPanel.setChart(AbstractChartFactory.createLoadingChart());
	}

	@Override
	protected synchronized JFreeChart createPanelChartType(@NonNull ChartOptions options) {
		return new VennChartFactory(options).makeVennChart();
	}

}
