package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.charting.charts.ScatterChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.gui.components.ImageThumbnailGenerator;
import com.bmskinner.nuclear_morphology.gui.components.panels.ClusterGroupSelectionPanel;

/**
 * Display tSNE results. This is a temporary class for testing.
 * It can display any 2D charts - currently testing PCA
 * @author ben
 * @since 1.16.0
 *
 */
public class TsneDialog extends MessagingDialog {

	private static final String COLOUR_BY_LBL       = "Colour by:";
	private static final String COLOUR_MERGE_SOURCE = "Merge sources";
	private static final String COLOUR_CLUSTERS     = "Clusters";
	private static final String COLOUR_NONE         = "None";
	private final IAnalysisDataset dataset;
	private final IClusterGroup group;
	private final ExportableChartPanel chartPanel = new ExportableChartPanel(ScatterChartFactory.createEmptyChart());
	
	public TsneDialog(final @NonNull IAnalysisDataset dataset, final @NonNull IClusterGroup group) {
		this.dataset = dataset;
		this.group = group;

		chartPanel.addChartMouseListener(new ImageThumbnailGenerator(chartPanel, ImageThumbnailGenerator.COLOUR_GREYSCALE));

		updateTitle();
		updateChart(ColourByType.CLUSTER, group);
		setLayout(new BorderLayout());

		add(createHeader(), BorderLayout.NORTH);

		add(chartPanel, BorderLayout.CENTER);
		
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setVisible(true);	
	}
	
	public enum ColourByType {
		NONE, CLUSTER, MERGE_SOURCE;
	}


	private JPanel createHeader() {
		JPanel panel = new JPanel(new FlowLayout());		
		
		// How should cells be coloured?
		
		final ButtonGroup colourGroup = new ButtonGroup();
		JRadioButton byNoneBtn = new JRadioButton(COLOUR_NONE);
		JRadioButton byClusterBtn = new JRadioButton(COLOUR_CLUSTERS);
		JRadioButton byMergeSourceBtn = new JRadioButton(COLOUR_MERGE_SOURCE);
		colourGroup.add(byNoneBtn);
		colourGroup.add(byClusterBtn);
		colourGroup.add(byMergeSourceBtn);
		
		byClusterBtn.setSelected(true);
		
		ClusterGroupSelectionPanel clustersBox = new ClusterGroupSelectionPanel(dataset.getClusterGroups());
		clustersBox.setEnabled(group!=null);
		clustersBox.setSelectedGroup(group);
		
		
		ActionListener colourListener = e ->{
			ColourByType type = byNoneBtn.isSelected() ? ColourByType.NONE : byClusterBtn.isSelected() ? ColourByType.CLUSTER : ColourByType.MERGE_SOURCE;
			clustersBox.setEnabled(byClusterBtn.isSelected());
			updateChart(type, clustersBox.getSelectedItem());
		};

		byNoneBtn.addActionListener(colourListener);
		byClusterBtn.addActionListener(colourListener);
		byMergeSourceBtn.addActionListener(colourListener);
		clustersBox.addActionListener(colourListener);
		
		panel.add(new JLabel(COLOUR_BY_LBL));
		panel.add(byNoneBtn);
		panel.add(byClusterBtn);
		panel.add(clustersBox);
		panel.add(byMergeSourceBtn);		
		return panel;
	}

	private void updateChart(ColourByType type, IClusterGroup colourGroup) {
		chartPanel.setChart(ScatterChartFactory.createTsneChart(dataset, type, group, colourGroup));
	}
	
	private void updateTitle() {
		setTitle("Dimensional reduction for "+dataset.getName()+": "+group.getName());		
	}


}
