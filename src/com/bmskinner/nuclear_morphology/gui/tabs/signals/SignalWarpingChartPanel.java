package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.events.revamp.ConsensusUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.tabs.ChartDetailPanel;
import com.bmskinner.nuclear_morphology.visualisation.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ConsensusNucleusChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;

public class SignalWarpingChartPanel extends ChartDetailPanel implements ConsensusUpdatedListener {

	private ConsensusNucleusChartPanel chartPanel;

	public SignalWarpingChartPanel() {

		setLayout(new BorderLayout());

		JFreeChart chart = OutlineChartFactory.createEmptyChart();
		chartPanel = new ConsensusNucleusChartPanel(chart);
		add(chartPanel, BorderLayout.CENTER);
	}

	@Override
	protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
		return new OutlineChartFactory(options).makeSignalWarpChart();
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

}
