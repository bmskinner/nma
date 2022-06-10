package com.bmskinner.nma.gui.tabs.signals;

import java.awt.BorderLayout;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.signals.IShellResult;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.ImageThumbnailGenerator;
import com.bmskinner.nma.gui.events.ChartSetEventListener;
import com.bmskinner.nma.gui.events.ConsensusUpdatedListener;
import com.bmskinner.nma.gui.events.NuclearSignalUpdatedListener;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.gui.tabs.ChartDetailPanel;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.OutlineChartFactory;
import com.bmskinner.nma.visualisation.charts.panels.ConsensusNucleusChartPanel;
import com.bmskinner.nma.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

public class SignalConsensusPanel extends ChartDetailPanel
		implements ChartSetEventListener, ConsensusUpdatedListener, NuclearSignalUpdatedListener {
	private static final Logger LOGGER = Logger.getLogger(SignalsOverviewPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Signal consensus";

	/** Consensus nucleus with signals overlaid */
	private ConsensusNucleusChartPanel chartPanel;

	/** consensus chart and signal visibility checkboxes */
	private JPanel consensusAndCheckboxPanel;

	/** Signal visibility checkbox panel */
	private JPanel checkboxPanel;

	/** Launch signal merging */
	private JButton mergeButton;

	/** Show signal radius or just CoM */
	boolean isShowRadii = false;

	/** Messages to clarify when UI is disabled */
	private JLabel headerText;

	/**
	 * Create with an input supplier
	 * 
	 * @param inputSupplier the input supplier
	 */
	public SignalConsensusPanel() {
		super();

		setLayout(new BorderLayout());

		consensusAndCheckboxPanel = createConsensusPanel();
		add(consensusAndCheckboxPanel, BorderLayout.CENTER);

		uiController.addConsensusUpdatedListener(this);
		uiController.addNuclearSignalUpdatedListener(this);

	}

	@Override
	public String getPanelTitle() {
		return PANEL_TITLE_LBL;
	}

	private JPanel createConsensusPanel() {

		final JPanel panel = new JPanel(new BorderLayout());
		JFreeChart chart = OutlineChartFactory.createEmptyChart();

		// the chart is inside a chartPanel; the chartPanel is inside a JPanel
		// this allows a checkbox panel to be added to the JPanel later
		chartPanel = new ConsensusNucleusChartPanel(chart);// {
		panel.add(chartPanel, BorderLayout.CENTER);
		chartPanel.setFillConsensus(false);

		chartPanel.addChartMouseListener(
				new ImageThumbnailGenerator(chartPanel));

		checkboxPanel = createSignalCheckboxPanel();

		panel.add(checkboxPanel, BorderLayout.NORTH);

		return panel;
	}

	/**
	 * Create the checkboxes that set each signal channel visible or not
	 */
	private JPanel createSignalCheckboxPanel() {
		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		mergeButton = new JButton(Labels.Signals.MERGE_BTN_LBL);
		mergeButton.addActionListener(e -> {
			UserActionController.getInstance().userActionEventReceived(new UserActionEvent(this,
					UserActionEvent.MERGE_SIGNALS_ACTION, activeDataset()));
		});
		mergeButton.setEnabled(false);
		panel.add(mergeButton);

		JCheckBox showAnnotationsBox = new JCheckBox(Labels.Signals.SHOW_SIGNAL_RADII_LBL,
				isShowRadii);
		showAnnotationsBox.addActionListener(e -> {
			isShowRadii = showAnnotationsBox.isSelected();
			refreshCache(getDatasets());
		});
		panel.add(showAnnotationsBox);
		showAnnotationsBox.setEnabled(this.hasDatasets());

		if (isSingleDataset()) {

			for (UUID signalGroup : activeDataset().getCollection().getSignalGroupIDs()) {
				if (signalGroup == null)
					continue;

				if (signalGroup.equals(IShellResult.RANDOM_SIGNAL_ID))
					continue;

				// get the status within each dataset
				boolean visible = activeDataset().getCollection().getSignalGroup(signalGroup).get()
						.isVisible();

				String name = activeDataset().getCollection().getSignalManager()
						.getSignalGroupName(signalGroup);

				// make a checkbox for each signal group in the dataset
				JCheckBox box = new JCheckBox(name, visible);

				// Don't enable when the consensus is missing
				box.setEnabled(activeDataset().getCollection().hasConsensus());

				box.addActionListener(e -> {
					activeDataset().getCollection().getSignalGroup(signalGroup).get()
							.setVisible(box.isSelected());
					uiController.fireNuclearSignalUpdated(activeDataset());
				});
				panel.add(box);

			}

		}

		headerText = new JLabel("");
		headerText.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		panel.add(headerText);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

		return panel;
	}

	private void updateCheckboxPanel() {
		if (isSingleDataset()) {

			// make a new panel for the active dataset
			consensusAndCheckboxPanel.remove(checkboxPanel);
			checkboxPanel = createSignalCheckboxPanel();

			// add this new panel
			consensusAndCheckboxPanel.add(checkboxPanel, BorderLayout.NORTH);
			consensusAndCheckboxPanel.revalidate();
			consensusAndCheckboxPanel.repaint();
			consensusAndCheckboxPanel.setVisible(true);

			// Only allow merge in root datasets
			if (activeDataset().isRoot()
					&& activeDataset().getCollection().getSignalManager().getSignalGroupCount() > 1)
				mergeButton.setEnabled(true);

		}

		if (isMultipleDatasets()) {
			mergeButton.setEnabled(false);
		}
	}

	private void updateSignalConsensusChart() {

		// The options do not hold which signal groups are visible
		// so we must invalidate the cache whenever they change
		this.clearCache(getDatasets());

		ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setShowWarp(false)
				.setScale(GlobalOptions.getInstance().getScale())
				.setTarget(chartPanel)
				.setShowXAxis(false)
				.setShowYAxis(false)
				.setShowAnnotations(isShowRadii).build();

		setChart(options);
	}

	@Override
	protected void updateSingle() {
		updateMultiple();

	}

	@Override
	protected void updateMultiple() {

		updateCheckboxPanel();
		updateSignalConsensusChart();
	}

	@Override
	protected void updateNull() {
		updateMultiple();

	}

	@Override
	public void setLoading() {
		super.setLoading();
		chartPanel.setChart(AbstractChartFactory.createLoadingChart());
	}

	@Override
	protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
		return new OutlineChartFactory(options).makeSignalOutlineChart();
	}

	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		((ExportableChartPanel) e.getSource()).restoreAutoBounds();

	}

	@Override
	public void consensusUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void consensusUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void consensusFillStateUpdated() {
		// We only use outlines here, so no effect needed
	}

	@Override
	public void nuclearSignalUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void nuclearSignalUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}
}