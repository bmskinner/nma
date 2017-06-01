/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusClusteringMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.ClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.options.ClusteringOptions.HierarchicalClusterMethod;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.MainWindow;

@SuppressWarnings("serial")
public class ClusteringSetupDialog extends HierarchicalTreeSetupDialog implements ActionListener {

    private static final String DIALOG_TITLE = "Clustering options";

    private static final String CLUSTER_METHOD_LBL = "Cluster method";
    private static final String CLUSTER_NUMBER_LBL = "Cluster number";
    private static final String EM_ITERATIONS_LBL  = "Iterations";
    private static final String EM_CLUSTERING_LBL  = "Expectation maximisation";
    private static final String HC_CLUSTERING_LBL  = "Hierarchical";

    private static final String HC_PANEL_KEY = "HierarchicalPanel";
    private static final String EM_PANEL_KEY = "EMPanel";

    private JPanel   cardPanel;
    private JSpinner clusterNumberSpinner;

    private JRadioButton clusterHierarchicalButton;
    private JRadioButton clusterEMButton;

    private JSpinner iterationsSpinner;

    // private final IMutableClusteringOptions options;

    public ClusteringSetupDialog(MainWindow mw, IAnalysisDataset dataset) {

        super(mw, dataset, DIALOG_TITLE);
        setDefaults();
        createUI();
        packAndDisplay();
    }

    @Override
    public IAnalysisMethod getMethod() {
        IAnalysisMethod m = new NucleusClusteringMethod(dataset, options);
        return m;
    }

    private JPanel createHierarchicalPanel() {
        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        List<JLabel> labels = new ArrayList<JLabel>();
        List<Component> fields = new ArrayList<Component>();

        clusterMethodBox = new JComboBox<HierarchicalClusterMethod>(HierarchicalClusterMethod.values());
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

        this.addLabelTextRows(labels, fields, layout, panel);
        return panel;
    }

    private JPanel createEMPanel() {

        JPanel panel = new JPanel();

        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        List<JLabel> labels = new ArrayList<JLabel>();
        List<Component> fields = new ArrayList<Component>();

        SpinnerModel model = new SpinnerNumberModel(IClusteringOptions.DEFAULT_EM_ITERATIONS, // initial
                                                                                              // value
                1, // min
                1000, // max
                1); // step

        iterationsSpinner = new JSpinner(model);
        iterationsSpinner.addChangeListener(e -> {
            JSpinner j = (JSpinner) e.getSource();
            try {
                j.commitEdit();
                options.setIterations((Integer) j.getValue());
            } catch (Exception e1) {
                warn("Error reading value in iterations field");
                stack(e1.getMessage(), e1);
            }
        });

        labels.add(new JLabel(EM_ITERATIONS_LBL));
        fields.add(iterationsSpinner);

        this.addLabelTextRows(labels, fields, layout, panel);
        return panel;
    }

    @Override
    protected void createUI() {

        setBounds(100, 100, 450, 300);
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPanel);

        profileButtonGroup = new ButtonGroup();
        statBoxMap = new HashMap<PlottableStatistic, JCheckBox>();
        segmentBoxMap = new HashMap<UUID, JCheckBox>();

        // ---------------
        // panel for text labels
        // ---------------
        headingPanel = createHeader();
        contentPanel.add(headingPanel, BorderLayout.NORTH);
        // ---------------
        // buttons at bottom
        // ---------------
        footerPanel = createFooter();
        contentPanel.add(footerPanel, BorderLayout.SOUTH);

        // ---------------
        // options in middle
        // ---------------
        optionsPanel = new JPanel();
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

        JPanel includePanel = null;
        try {
            includePanel = createIncludePanel();
        } catch (Exception e) {
            error("Error making incluude panel", e);
        }

        optionsPanel.add(methodPanel, BorderLayout.NORTH);
        optionsPanel.add(cardPanel, BorderLayout.CENTER);
        optionsPanel.add(includePanel, BorderLayout.SOUTH);

        contentPanel.add(optionsPanel, BorderLayout.CENTER);
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
