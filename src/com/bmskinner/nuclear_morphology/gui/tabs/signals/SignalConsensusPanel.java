package com.bmskinner.nuclear_morphology.gui.tabs.signals;

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

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult;
import com.bmskinner.nuclear_morphology.components.signals.SignalManager;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ImageThumbnailGenerator;
import com.bmskinner.nuclear_morphology.gui.events.ChartSetEventListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.ConsensusUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.NuclearSignalUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.tabs.ChartDetailPanel;
import com.bmskinner.nuclear_morphology.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ConsensusNucleusChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptionsBuilder;

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
	boolean isShowAnnotations = false;

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

		chartPanel.addChartMouseListener(new ImageThumbnailGenerator(chartPanel, ImageThumbnailGenerator.COLOUR_RGB));

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
			LOGGER.finer("Firing merge signal action request");
//			getSignalChangeEventHandler().fireUserActionEvent(UserActionEvent.MERGE_SIGNALS_ACTION);
		});
		mergeButton.setEnabled(false);
		panel.add(mergeButton);

		JCheckBox showAnnotationsBox = new JCheckBox(Labels.Signals.SHOW_SIGNAL_RADII_LBL, isShowAnnotations);
		showAnnotationsBox.addActionListener(e -> {
			isShowAnnotations = showAnnotationsBox.isSelected();
			refreshCache(getDatasets());
		});
		panel.add(showAnnotationsBox);
		showAnnotationsBox.setEnabled(this.hasDatasets());

		if (isSingleDataset()) {

			for (UUID signalGroup : activeDataset().getCollection().getSignalGroupIDs()) {

				if (signalGroup.equals(IShellResult.RANDOM_SIGNAL_ID))
					continue;

				// get the status within each dataset
				boolean visible = activeDataset().getCollection().getSignalGroup(signalGroup).get().isVisible();

				String name = activeDataset().getCollection().getSignalManager().getSignalGroupName(signalGroup);

				// make a checkbox for each signal group in the dataset
				JCheckBox box = new JCheckBox(name, visible);

				// Don't enable when the consensus is missing
				box.setEnabled(activeDataset().getCollection().hasConsensus());

				box.addActionListener(e -> {
					activeDataset().getCollection().getSignalGroup(signalGroup).get().setVisible(box.isSelected());
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

//			if (activeDataset() != null && activeDataset().getCollection().hasConsensus()
//					&& activeDataset().getCollection().getSignalManager().hasSignals()) {
//				warpButton.setEnabled(true);
//			}

			if (activeDataset().getCollection().getSignalManager().getSignalGroupCount() > 1)
				mergeButton.setEnabled(true);

		}

		if (isMultipleDatasets()) {
			mergeButton.setEnabled(false);
			if (IAnalysisDataset.haveConsensusNuclei(getDatasets())) {

				// Check at least one of the selected datasets has signals
				String text = "";

				boolean hasSignals = false;
				for (IAnalysisDataset d : getDatasets()) {

					SignalManager m = d.getCollection().getSignalManager();
					if (m.hasSignals()) {
						hasSignals = true;
						break;
					}
				}

				// Segments need to match for mesh creation
				boolean segmentsMatch = IProfileSegment.segmentCountsMatch(getDatasets());

				if (!segmentsMatch) {
					text = "Segments do not match between datasets";
				}

//				if (hasSignals && segmentsMatch) {
//					warpButton.setEnabled(true);
//				} else {
//					warpButton.setEnabled(false);
//					headerText.setText(text);
//				}

			} else {
//				warpButton.setEnabled(false);
				headerText.setText("Datasets do not all have consensus");
			}
		}
	}

	private void updateSignalConsensusChart() {

		// The options do not hold which signal groups are visible
		// so we must invalidate the cache whenever they change
		this.clearCache(getDatasets());

		ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).setShowWarp(false)
				.setTarget(chartPanel).setShowAnnotations(isShowAnnotations).build();

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