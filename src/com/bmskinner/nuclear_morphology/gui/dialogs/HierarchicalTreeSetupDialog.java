/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.classification.TreeBuildingMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.HierarchicalClusterMethod;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.panels.DimensionalReductionSelectionPanel;
import com.bmskinner.nuclear_morphology.gui.components.panels.ParameterSelectionPanel;

/**
 * The setup dialog for building hierarchical trees from dataset values.
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class HierarchicalTreeSetupDialog extends SubAnalysisSetupDialog {

	private static final String DIALOG_TITLE        = "Tree building options";
	protected static final String CLUSTER_METHOD_LBL  = "Distance method";
	private static final String INCLUDE_TSNE_LBL    = "Include tSNE";
	private static final String INCLUDE_PROFILE_LBL = "Include profiles";
	private static final String P_VALUE_LBL         = "  p(uni) = ";

	protected static final ProfileType DEFAULT_PROFILE_TYPE = ProfileType.ANGLE;

	protected final HashOptions options;

	public HierarchicalTreeSetupDialog(final @NonNull IAnalysisDataset dataset) {
		this(dataset, DIALOG_TITLE);
	}

	/**
	 * Constructor that does not make panel visible
	 * 
	 * @param mw
	 * @param title
	 */
	protected HierarchicalTreeSetupDialog(final @NonNull IAnalysisDataset dataset, final String title) {
		super(dataset, title);
		options = OptionsFactory.makeDefaultClusteringOptions(ClusteringMethod.HIERARCHICAL);
		createUI();

		// Don't display for subclasses
		if (this.getClass().equals(HierarchicalTreeSetupDialog.class)) {
			packAndDisplay();
		}

	}

	/**
	 * Set the default options
	 */
	@Override
	protected void setDefaults() {
		// set by options factory
	}

	@Override
	public IAnalysisMethod getMethod() {
		return new TreeBuildingMethod(getFirstDataset(), options);
	}
	
	@Override
	public HashOptions getOptions() {
		return options;
	}

	@Override
	protected void createUI() {
		getContentPane().add(createHeader(), BorderLayout.NORTH);
		getContentPane().add(createFooter(), BorderLayout.SOUTH);
		getContentPane().add(createOptionsPanel(), BorderLayout.CENTER);
	}

	private JPanel createOptionsPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(new ParameterSelectionPanel(getFirstDataset(), options));
		panel.add(new DimensionalReductionSelectionPanel(getFirstDataset(), options));
		panel.add(createClusterMethodPanel());
		return panel;
	}

	private JPanel createClusterMethodPanel() {
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);

		List<JLabel> labels = new ArrayList<>();
		List<Component> fields = new ArrayList<>();

		JComboBox<HierarchicalClusterMethod> clusterMethodBox = new JComboBox<>(HierarchicalClusterMethod.values());
		clusterMethodBox.setSelectedItem(HashOptions.DEFAULT_HIERARCHICAL_METHOD);
		clusterMethodBox.addActionListener(e ->  options.setString(HashOptions.CLUSTER_HIERARCHICAL_METHOD_KEY, 
				((HierarchicalClusterMethod) clusterMethodBox.getSelectedItem()).toString()));

		JLabel clusterLabel = new JLabel(CLUSTER_METHOD_LBL);
		clusterLabel.setToolTipText(Labels.Clusters.HIERARCHICAL_CLUSTER_METHOD);
		labels.add(clusterLabel);
		fields.add(clusterMethodBox);

		addLabelTextRows(labels, fields, layout, panel);
		panel.setBorder(BorderFactory.createTitledBorder("Clustering"));
		
		return panel;
	}
}
