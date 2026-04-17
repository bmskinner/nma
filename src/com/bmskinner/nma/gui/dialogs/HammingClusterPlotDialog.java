package com.bmskinner.nma.gui.dialogs;

import java.awt.BorderLayout;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.gui.components.ImageThumbnailGenerator;
import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;

/**
 * Visualise Hamming clusters. We need to show the regions of profiles that were
 * clustered on, and the range of morphotypes within those regions.
 * 
 */
public class HammingClusterPlotDialog extends MessagingDialog {

	private static final Logger LOGGER = Logger.getLogger(HammingClusterPlotDialog.class.getName());

	private final IAnalysisDataset dataset;
	private final IClusterGroup group;

	private final ExportableChartPanel chartPanel = new ExportableChartPanel(
			AbstractChartFactory.createEmptyChart());

	public HammingClusterPlotDialog(final @NonNull IAnalysisDataset dataset,
			final @NonNull IClusterGroup group) {
		this.dataset = dataset;
		this.group = group;

		chartPanel.setFixedAspectRatio(true);
		chartPanel.setPannable(true);
		chartPanel.addChartMouseListener(new ImageThumbnailGenerator(chartPanel));

		updateTitle();
		createChart();
		setLayout(new BorderLayout());

		add(createHeader(), BorderLayout.NORTH);

		add(chartPanel, BorderLayout.CENTER);

		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	private JPanel createHeader() {

		final JPanel panel = new JPanel();
		final BoxLayout bl = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(bl);

		return panel;
	}

	private void createChart() {
		chartPanel.setChart(
				AbstractChartFactory.createEmptyChart());
	}

	private void updateTitle() {
		setTitle("Hamming clusters for %s: %s".formatted(dataset.getName(), group.getName()));
	}

}
