package com.bmskinner.nma.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.core.ThreadManager;
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
		updateChart(ColourByType.CLUSTER, group);
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

		JCheckBox showImagesBox = new JCheckBox("Load images", true);
		showImagesBox.addActionListener(l -> {
			updateChart(ColourByType.CLUSTER, group);
			if (showImagesBox.isSelected()) {
				Runnable r = () -> DimensionalityChartFactory.addAnnotatedNucleusImages(dataset,
						group,
						chartPanel.getChart());
				ThreadManager.getInstance().submit(r);

			}

		});

		panel.add(showImagesBox);

//		// How should cells be coloured?
//		final ButtonGroup colourGroup = new ButtonGroup();
//		JRadioButton byNoneBtn = new JRadioButton(COLOUR_NONE);
//		JRadioButton byClusterBtn = new JRadioButton(COLOUR_CLUSTERS);
//		JRadioButton byMergeSourceBtn = new JRadioButton(COLOUR_MERGE_SOURCE);
//		colourGroup.add(byNoneBtn);
//		colourGroup.add(byClusterBtn);
//		colourGroup.add(byMergeSourceBtn);
//
//		byClusterBtn.setSelected(true);
//
//		ClusterGroupSelectionPanel clustersBox = new ClusterGroupSelectionPanel(
//				dataset.getClusterGroups());
//		clustersBox.setEnabled(group != null);
//		clustersBox.setSelectedGroup(group);
//
//		ActionListener colourListener = e -> {
//			ColourByType type = byNoneBtn.isSelected() ? ColourByType.NONE
//					: byClusterBtn.isSelected() ? ColourByType.CLUSTER : ColourByType.MERGE_SOURCE;
//			clustersBox.setEnabled(byClusterBtn.isSelected());
//			updateChart(type, clustersBox.getSelectedItem());
//		};
//
//		byNoneBtn.addActionListener(colourListener);
//		byClusterBtn.addActionListener(colourListener);
//		byMergeSourceBtn.addActionListener(colourListener);
//		clustersBox.addActionListener(colourListener);
//
//		panel.add(new JLabel(COLOUR_BY_LBL));
//		panel.add(byNoneBtn);
//		panel.add(byClusterBtn);
//		panel.add(clustersBox);
//		panel.add(byMergeSourceBtn);
		return panel;
	}

	private void updateChart(ColourByType type, IClusterGroup colourGroup) {
		chartPanel.setChart(
				DimensionalityChartFactory.createDimensionalityReductionChart(dataset, type,
						group, colourGroup));
	}

	private void updateTitle() {
		setTitle("Dimensional reduction for " + dataset.getName() + ": " + group.getName());
	}

}
