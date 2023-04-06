package com.bmskinner.nma.gui.tabs.signals.warping;

import java.awt.BorderLayout;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.signals.IWarpedSignal;
import com.bmskinner.nma.gui.components.panels.ConsensusNucleusChartPanel;
import com.bmskinner.nma.gui.events.ConsensusUpdatedListener;
import com.bmskinner.nma.gui.tabs.ChartDetailPanel;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.OutlineChartFactory;
import com.bmskinner.nma.visualisation.charts.WarpedSignalChartFactory;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

public class SignalWarpingChartPanel extends ChartDetailPanel
		implements ConsensusUpdatedListener, WarpedSignalSelectionChangeListener {

	private static final Logger LOGGER = Logger.getLogger(SignalWarpingChartPanel.class.getName());

	private ConsensusNucleusChartPanel chartPanel;

	public SignalWarpingChartPanel() {

		setLayout(new BorderLayout());

		JFreeChart chart = OutlineChartFactory.createEmptyChart();
		chartPanel = new ConsensusNucleusChartPanel(chart);
		add(chartPanel, BorderLayout.CENTER);
	}

	@Override
	protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
		return new WarpedSignalChartFactory(options).makeSignalWarpChart();
	}

	@Override
	public void setLoading() {
	}

	@Override
	protected void updateSingle() {
		chartPanel.setChart(AbstractChartFactory.createEmptyChart());
	}

	@Override
	protected void updateMultiple() {
		chartPanel.setChart(AbstractChartFactory.createEmptyChart());
	}

	@Override
	protected void updateNull() {
		chartPanel.setChart(AbstractChartFactory.createEmptyChart());
	}

	@Override
	public void consensusUpdated(List<IAnalysisDataset> datasets) {
		// No action to saved warp images
	}

	@Override
	public void consensusUpdated(IAnalysisDataset dataset) {
		// No action to saved warp images
	}

	@Override
	public void consensusFillStateUpdated() {
		// No action to saved warp images
	}

	@Override
	public void warpedSignalSelectionChanged(List<IWarpedSignal> images) {
		if (images.isEmpty()) {
			chartPanel.setChart(AbstractChartFactory.createEmptyChart());
		} else {
			ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setShowXAxis(false)
					.setShowYAxis(false)
					.setShowBounds(false)
					.setFillConsensus(false)
					.setTarget(chartPanel)
					.setWarpedSignals(images)
					.build();
			setChart(options);
		}
	}

	@Override
	public void warpedSignalVisualisationChanged(List<IWarpedSignal> images) {
		if (images.isEmpty()) {
			chartPanel.setChart(AbstractChartFactory.createEmptyChart());
		} else {
			ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setShowXAxis(false)
					.setShowYAxis(false)
					.setShowBounds(false)
					.setFillConsensus(false)
					.setTarget(chartPanel)
					.setWarpedSignals(images)
					.build();

			this.cache.clear(images);
			setChart(options);
		}
	}

}
