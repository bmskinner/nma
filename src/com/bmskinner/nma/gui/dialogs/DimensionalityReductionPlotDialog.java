package com.bmskinner.nma.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.plot.XYPlot;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.components.ImageThumbnailGenerator;
import com.bmskinner.nma.visualisation.charts.DimensionalityChartFactory;
import com.bmskinner.nma.visualisation.charts.ScatterChartFactory;
import com.bmskinner.nma.visualisation.charts.panels.ExportableChartPanel;

/**
 * Display tSNE results. This is a temporary class for testing. It can display
 * any 2D charts - currently testing PCA
 * 
 * @author ben
 * @since 1.16.0
 *
 */
public class DimensionalityReductionPlotDialog extends MessagingDialog {

	private static final Logger LOGGER = Logger
			.getLogger(DimensionalityReductionPlotDialog.class.getName());

	private static final String COLOUR_BY_LBL = "Colour by:";
	private static final String COLOUR_MERGE_SOURCE = "Merge sources";
	private static final String COLOUR_CLUSTERS = "Clusters";
	private static final String COLOUR_NONE = "None";
	private final IAnalysisDataset dataset;
	private final IClusterGroup group;
	private final ExportableChartPanel chartPanel = new ExportableChartPanel(
			ScatterChartFactory.createEmptyChart());

	public DimensionalityReductionPlotDialog(final @NonNull IAnalysisDataset dataset,
			final @NonNull IClusterGroup group) {
		this.dataset = dataset;
		this.group = group;

		chartPanel.setFixedAspectRatio(true);
		chartPanel.setPannable(true);
		chartPanel.addChartMouseListener(new ImageThumbnailGenerator(chartPanel));

		updateTitle();
		createChart(ColourByType.CLUSTER, group);
		setLayout(new BorderLayout());

		add(createHeader(), BorderLayout.NORTH);

		add(chartPanel, BorderLayout.CENTER);

		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setVisible(true);

		// Run this after the chart is visible
		DimensionalityChartFactory.addAnnotatedNucleusImages(dataset, group, chartPanel.getChart());
	}

	public enum ColourByType {
		NONE, CLUSTER, MERGE_SOURCE;
	}

	private JPanel createHeader() {
		JPanel panel = new JPanel(new FlowLayout());

		JCheckBox showImagesBox = new JCheckBox("Show images", true);
		showImagesBox.addActionListener(l -> {
			if (showImagesBox.isSelected()) {
				Runnable r = () -> DimensionalityChartFactory.addAnnotatedNucleusImages(dataset,
						group,
						chartPanel.getChart());
				ThreadManager.getInstance().submit(r);
			} else {
				chartPanel.getChart().getXYPlot().getRenderer().removeAnnotations();
			}
		});

		JCheckBox showPointsBox = new JCheckBox("Show points", true);
		showPointsBox.addActionListener(l -> {
			if (showPointsBox.isSelected()) {
				updateChart(ColourByType.CLUSTER);
			} else {
				updateChart(ColourByType.NONE);
			}
		});

		panel.add(showImagesBox);
		panel.add(showPointsBox);

		return panel;
	}

	private void updateChart(ColourByType type) {
		XYPlot plot = chartPanel.getChart().getXYPlot();
		for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
			Paint colour = ColourByType.CLUSTER.equals(type) ? ColourSelecter.getColor(i)
					: Color.WHITE;
			plot.getRenderer().setSeriesPaint(i, colour);
		}
	}

	private void createChart(ColourByType type, IClusterGroup colourGroup) {
		chartPanel.setChart(
				DimensionalityChartFactory.createDimensionalityReductionChart(dataset, type,
						group, colourGroup));
	}

	private void updateTitle() {
		setTitle("Dimensional reduction for " + dataset.getName() + ": " + group.getName());
	}

}
