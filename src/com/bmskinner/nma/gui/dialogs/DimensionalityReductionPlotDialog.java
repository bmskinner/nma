package com.bmskinner.nma.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.plot.XYPlot;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.gui.components.ImageThumbnailGenerator;
import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.charts.DimensionalityChartFactory;
import com.bmskinner.nma.visualisation.charts.ScatterChartFactory;

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

	private static final String HELP_LBL = "Scroll to zoom, click and drag the chart to move";
	private final IAnalysisDataset dataset;
	private final IClusterGroup group;

	private JSpinner imageSpinner;

	private static final double MAX_NUCLEI_PER_CLUSTER = 200;

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
		DimensionalityChartFactory.addAnnotatedNucleusImages(dataset, group, chartPanel.getChart(),
				((Double) imageSpinner.getValue()).intValue());
	}

	public enum ColourByType {
		NONE, CLUSTER, MERGE_SOURCE;
	}

	private JPanel createHeader() {
		JPanel panel = new JPanel(new FlowLayout());

		imageSpinner = createMaxImageSpinner();

		JCheckBox showImagesBox = new JCheckBox("Show images", true);
		showImagesBox.addActionListener(l -> {
			if (showImagesBox.isSelected()) {
				Runnable r = () -> DimensionalityChartFactory.addAnnotatedNucleusImages(dataset,
						group,
						chartPanel.getChart(), ((Double) imageSpinner.getValue()).intValue());
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

		imageSpinner.addChangeListener(e -> {
			try {
				imageSpinner.commitEdit();
				if (showImagesBox.isSelected()) {
					chartPanel.getChart().getXYPlot().getRenderer().removeAnnotations();
					Runnable r = () -> DimensionalityChartFactory.addAnnotatedNucleusImages(dataset,
							group,
							chartPanel.getChart(), ((Double) imageSpinner.getValue()).intValue());
					ThreadManager.getInstance().submit(r);
				}
			} catch (ParseException e1) {
				LOGGER.log(Loggable.STACK, "Error parsing input", e);
			}
		});

		panel.add(new JLabel(HELP_LBL));
		panel.add(showImagesBox);
		panel.add(new JLabel("Max images per cluster:"));
		panel.add(imageSpinner);
		panel.add(showPointsBox);

		return panel;
	}

	private JSpinner createMaxImageSpinner() {

		// The default number of images per cluster should depend on the number of
		// clusters
		double initialImages = Math.max(1,
				Math.min(MAX_NUCLEI_PER_CLUSTER / group.size(), MAX_NUCLEI_PER_CLUSTER));

		SpinnerNumberModel model = new SpinnerNumberModel((int) initialImages, 1,
				MAX_NUCLEI_PER_CLUSTER, 1);
		JSpinner spinner = new JSpinner(model);
		spinner.setToolTipText("Number of images to load per cluster");
		return spinner;
	}

	private void updateChart(ColourByType type) {
		XYPlot plot = chartPanel.getChart().getXYPlot();
		List<UUID> childIds = group.getUUIDs();
		for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
			IAnalysisDataset childDataset = dataset.getChildDataset(childIds.get(i));
			Paint colour = ColourByType.CLUSTER.equals(type)
					? childDataset.getDatasetColour().orElse(ColourSelecter.getColor(i))
					: Color.WHITE;
			plot.getRenderer().setSeriesPaint(i, colour);
		}
	}

	private void createChart(ColourByType type, IClusterGroup colourGroup) {
		chartPanel.setChart(
				DimensionalityChartFactory.createDimensionalityReductionChart(dataset, type, group,
						colourGroup));
	}

	private void updateTitle() {
		setTitle("Dimensional reduction for " + dataset.getName() + ": " + group.getName());
	}

}
