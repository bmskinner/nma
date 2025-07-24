package com.bmskinner.nma.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.components.ImageThumbnailGenerator;
import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.DimensionalityChartFactory;

/**
 * Display tSNE results. This is a temporary class for testing. It can display
 * any 2D charts - currently testing PCA
 * 
 * @author Ben Skinner
 * @since 1.16.0
 *
 */
@SuppressWarnings("serial")
public class DimensionalityReductionPlotDialog extends MessagingDialog {

	private static final Logger LOGGER = Logger
			.getLogger(DimensionalityReductionPlotDialog.class.getName());

	private static final String HELP_LBL = "Scroll to zoom, click and drag the chart to move";
	private final IAnalysisDataset dataset;
	private final IClusterGroup group;

	private JSpinner imageSpinner;

	private JComboBox<ColourByType> colourBox;

	private JCheckBox showImagesBox;
	private JCheckBox showPointsBox;

	private static final double MAX_NUCLEI_PER_CLUSTER = 200;

	private final ExportableChartPanel chartPanel = new ExportableChartPanel(
			AbstractChartFactory.createEmptyChart());

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
	}

	public enum ColourByType {
		NONE("None"), CLUSTER("Cluster"), MERGE_SOURCE("Merge source");

		private final String name;

		private ColourByType(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private JPanel createHeader() {

		final JPanel panel = new JPanel();
		final BoxLayout bl = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(bl);



		colourBox = new JComboBox<ColourByType>(ColourByType.values());
		colourBox.setSelectedItem(ColourByType.CLUSTER);

		showPointsBox = new JCheckBox("Show points", true);
		showPointsBox.addActionListener(l -> {
			updateChart((ColourByType) colourBox.getSelectedItem());
		});

		showImagesBox = new JCheckBox("Show images", false);
		showImagesBox.addActionListener(l -> {
			updateChart((ColourByType) colourBox.getSelectedItem());
		});

		imageSpinner = createMaxImageSpinner();
		imageSpinner.addChangeListener(e -> {
			updateChart((ColourByType) colourBox.getSelectedItem());
		});

		colourBox.addActionListener(e -> {
			updateChart((ColourByType) colourBox.getSelectedItem());
		});

		final JPanel textPanel = new JPanel();
		textPanel.add(new JLabel(HELP_LBL));
		final JPanel btnPanel = new JPanel();

		btnPanel.add(new JLabel("Colour points by:"));
		btnPanel.add(colourBox);
		btnPanel.add(showPointsBox);
		btnPanel.add(showImagesBox);
		btnPanel.add(new JLabel("Max images per cluster:"));
		btnPanel.add(imageSpinner);

		panel.add(textPanel);
		panel.add(btnPanel);

		return panel;
	}

	private JSpinner createMaxImageSpinner() {

		// The default number of images per cluster should depend on the number of
		// clusters
		final double initialImages = Math.max(1,
				Math.min(MAX_NUCLEI_PER_CLUSTER / group.size(), MAX_NUCLEI_PER_CLUSTER));

		final SpinnerNumberModel model = new SpinnerNumberModel((int) initialImages, 1,
				MAX_NUCLEI_PER_CLUSTER, 1);
		final JSpinner spinner = new JSpinner(model);
		spinner.setToolTipText("Number of images to load per cluster");
		return spinner;
	}

	private void updateChart(ColourByType type) {
		final Runnable r = () -> {

			final JFreeChart chart = DimensionalityChartFactory.createDimensionalityReductionChart(dataset, type, group,
					group);

			final XYPlot plot = chart.getXYPlot();

//			createChart(type, group);
			// Override point colours if the are not to be displayed.
			// Keep the points so that axis scaling still works
			if (!showPointsBox.isSelected()) {
				for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
					plot.getRenderer().setSeriesPaint(i, Color.WHITE);
				}
			}

			plot.getRenderer().removeAnnotations();
			chartPanel.setChart(chart);

			if (showImagesBox.isSelected()) {
				DimensionalityChartFactory.addAnnotatedNucleusImages(dataset,
						group,
						type,
						chart, ((Double) imageSpinner.getValue()).intValue());
			}



		};
		ThreadManager.getInstance().submitUIUpdate(r);
	}

	private void createChart(ColourByType type, IClusterGroup colourGroup) {
		chartPanel.setChart(
				DimensionalityChartFactory.createDimensionalityReductionChart(dataset, type, group,
						colourGroup));
	}

	private void updateTitle() {
		setTitle("Dimensionality reduction for %s: %s".formatted(dataset.getName(), group.getName()));
	}

}
