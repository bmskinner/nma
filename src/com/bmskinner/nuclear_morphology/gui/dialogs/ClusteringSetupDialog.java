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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.classification.NucleusClusteringMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.HierarchicalClusterMethod;

/**
 * Setup for clustering. It inherits the parameters for inclusion from the hierarchical tree
 * setup, and adds panels for clustering method
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class ClusteringSetupDialog extends HierarchicalTreeSetupDialog implements ActionListener {

    private static final String DIALOG_TITLE = "Clustering options";

    private static final String CLUSTER_NUMBER_LBL = "Number of clusters";
    private static final String EM_ITERATIONS_LBL  = "Iterations";
    private static final String EM_CLUSTERING_LBL  = "Expectation maximisation";
    private static final String HC_CLUSTERING_LBL  = "Hierarchical";

    private static final String HC_PANEL_KEY = "HierarchicalPanel";
    private static final String EM_PANEL_KEY = "EMPanel";

    private JPanel   cardPanel;
    private JSpinner clusterNumberSpinner;
    
    private JComboBox<HierarchicalClusterMethod> clusterMethodBox;

    private JRadioButton clusterHierarchicalButton;
    private JRadioButton clusterEMButton;

    public ClusteringSetupDialog(final @NonNull IAnalysisDataset dataset) {

        super(dataset, DIALOG_TITLE);
        setDefaults();
        createUI();
        packAndDisplay();
    }

    @Override
    public IAnalysisMethod getMethod() {
    	return new NucleusClusteringMethod(dataset, options);
    }

    private JPanel createHierarchicalPanel() {
        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        List<JLabel> labels = new ArrayList<>();
        List<Component> fields = new ArrayList<>();

        clusterMethodBox = new JComboBox<>(HierarchicalClusterMethod.values());
        clusterMethodBox.setSelectedItem(IClusteringOptions.DEFAULT_HIERARCHICAL_METHOD);
        clusterMethodBox.addActionListener(this);

        labels.add(new JLabel(CLUSTER_METHOD_LBL));
        fields.add(clusterMethodBox);

        SpinnerModel model = new SpinnerNumberModel(IClusteringOptions.DEFAULT_MANUAL_CLUSTER_NUMBER, // initial
                                                                                                      // value
                1, // min
                100, // max
                1); // step

        clusterNumberSpinner = new JSpinner(model);
        clusterNumberSpinner.setEnabled(true);

        labels.add(new JLabel(CLUSTER_NUMBER_LBL));
        fields.add(clusterNumberSpinner);

        clusterNumberSpinner.addChangeListener(e -> {
            JSpinner j = (JSpinner) e.getSource();
            try {
                j.commitEdit();
                options.setClusterNumber((Integer) j.getValue());

            } catch (Exception e1) {
                warn("Error reading value in cluster number field");
                stack(e1.getMessage(), e1);
            }
        });

        addLabelTextRows(labels, fields, layout, panel);
        return panel;
    }

    private JPanel createEMPanel() {

        JPanel panel = new JPanel();

        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        List<JLabel> labels = new ArrayList<>();
        List<Component> fields = new ArrayList<>();

        SpinnerModel model = new SpinnerNumberModel(IClusteringOptions.DEFAULT_EM_ITERATIONS, // initial
                                                                                              // value
                1, // min
                1000, // max
                1); // step

        JSpinner iterationsSpinner = new JSpinner(model);
        iterationsSpinner.addChangeListener(e -> {
            try {
            	iterationsSpinner.commitEdit();
                options.setIterations((Integer) iterationsSpinner.getValue());
            } catch (ParseException e1) {
            	stack("Error reading value in iterations field", e1);
            }
        });

        labels.add(new JLabel(EM_ITERATIONS_LBL));
        fields.add(iterationsSpinner);

        this.addLabelTextRows(labels, fields, layout, panel);
        return panel;
    }

    @Override
    protected void createUI() {
        getContentPane().add(createHeader(), BorderLayout.NORTH);
    	getContentPane().add(createFooter(), BorderLayout.SOUTH);

    	// ---------------
        // options in middle
        // ---------------
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BorderLayout());

        JPanel methodPanel = new JPanel(new FlowLayout());

        // Create the radio buttons.
        clusterHierarchicalButton = new JRadioButton(HC_CLUSTERING_LBL);
        clusterHierarchicalButton.setSelected(true);

        clusterEMButton = new JRadioButton(EM_CLUSTERING_LBL);

        // Group the radio buttons.
        ButtonGroup clusterTypeGroup = new ButtonGroup();
        clusterTypeGroup.add(clusterHierarchicalButton);
        clusterTypeGroup.add(clusterEMButton);

        clusterHierarchicalButton.addActionListener(this);
        clusterEMButton.addActionListener(this);

        cardPanel = new JPanel(new CardLayout());
        cardPanel.add(createHierarchicalPanel(), HC_PANEL_KEY);
        cardPanel.add(createEMPanel(), EM_PANEL_KEY);
        CardLayout cl = (CardLayout) (cardPanel.getLayout());
        cl.show(cardPanel, HC_PANEL_KEY);

        methodPanel.add(clusterHierarchicalButton);
        methodPanel.add(clusterEMButton);

        optionsPanel.add(methodPanel, BorderLayout.NORTH);
        optionsPanel.add(cardPanel, BorderLayout.CENTER);
        optionsPanel.add(createIncludePanel(), BorderLayout.SOUTH);

        getContentPane().add(optionsPanel, BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {

        // Set card panel based on selected radio button
        if (clusterHierarchicalButton.isSelected()) {

            CardLayout cl = (CardLayout) (cardPanel.getLayout());
            cl.show(cardPanel, HC_PANEL_KEY);

            options.setType(ClusteringMethod.HIERARCHICAL);

            clusterNumberSpinner.setEnabled(true);
            options.setClusterNumber((Integer) clusterNumberSpinner.getValue());
            options.setHierarchicalMethod((HierarchicalClusterMethod) clusterMethodBox.getSelectedItem());
        }

        if (clusterEMButton.isSelected()) {

            CardLayout cl = (CardLayout) (cardPanel.getLayout());
            cl.show(cardPanel, EM_PANEL_KEY);

            options.setType(ClusteringMethod.EM);
        }
    }
}
