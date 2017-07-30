/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.TreeBuildingMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.options.ClusteringOptions.HierarchicalClusterMethod;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.IMutableClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.stats.DipTester;

/**
 * The setup dialog for building hierarchical trees from dataset values.
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class HierarchicalTreeSetupDialog extends SubAnalysisSetupDialog implements ChangeListener {

    private static final String DIALOG_TITLE        = "Tree building options";
    private static final String CLUSTER_METHOD_LBL  = "Cluster method";
    private static final String INCLUDE_PROFILE_LBL = "Include profiles";
    private static final String INCLUDE_MESH_LBL    = "Include mesh faces";
    private static final String P_VALUE_LBL         = "  p(uni) = ";

    protected static final ProfileType DEFAULT_PROFILE_TYPE = ProfileType.ANGLE;

    protected JPanel headingPanel;
    protected JPanel optionsPanel;
    protected JPanel footerPanel;

    protected ButtonGroup profileButtonGroup;

    protected JComboBox<HierarchicalClusterMethod> clusterMethodBox;

    protected JCheckBox includeProfilesCheckBox;

    protected JCheckBox includeMeshCheckBox;

    protected Map<PlottableStatistic, JCheckBox> statBoxMap;

    protected Map<UUID, JCheckBox> segmentBoxMap;

    protected final IMutableClusteringOptions options;

    public HierarchicalTreeSetupDialog(final MainWindow mw, final IAnalysisDataset dataset) {

        // modal dialog
        this(mw, dataset, DIALOG_TITLE);
    }

    /**
     * Constructor that does not make panel visible
     * 
     * @param mw
     * @param title
     */
    protected HierarchicalTreeSetupDialog(final MainWindow mw, final IAnalysisDataset dataset, final String title) {
        super(mw, dataset, title);
        options = OptionsFactory.makeClusteringOptions();
        setDefaults();
        createUI();

        // Don't display for subclasses
        if (this.getClass().equals(HierarchicalTreeSetupDialog.class)) {
            packAndDisplay();
        }

    }

    /**
     * Set the default options
     */
    protected void setDefaults() {
        // options = OptionsFactory.makeClusteringOptions();
        options.setClusterNumber(IClusteringOptions.DEFAULT_MANUAL_CLUSTER_NUMBER);
        options.setHierarchicalMethod(IClusteringOptions.DEFAULT_HIERARCHICAL_METHOD);
        options.setIterations(IClusteringOptions.DEFAULT_EM_ITERATIONS);
        options.setUseSimilarityMatrix(IClusteringOptions.DEFAULT_USE_SIMILARITY_MATRIX);
        options.setIncludeProfile(IClusteringOptions.DEFAULT_INCLUDE_PROFILE);
        options.setProfileType(DEFAULT_PROFILE_TYPE);
        options.setIncludeMesh(IClusteringOptions.DEFAULT_INCLUDE_MESH);
    }

    protected JPanel createHeader() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    @Override
    public IAnalysisMethod getMethod() {
        IAnalysisMethod m = new TreeBuildingMethod(dataset, options);
        return m;
    }

    @Override
    protected void createUI() {

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

        JPanel optionsPanel = null;
        try {
            optionsPanel = createOptionsPanel();
        } catch (Exception e) {

            error("Error making options panel", e);
        }

        contentPanel.add(optionsPanel, BorderLayout.CENTER);
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        try {

            panel.add(createClusterMethodPanel());
            panel.add(createIncludePanel());

        } catch (Exception e) {
            error("Error making an options panel", e);
        }
        return panel;

    }

    private JPanel createClusterMethodPanel() {
        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        List<JLabel> labels = new ArrayList<JLabel>();
        List<Component> fields = new ArrayList<Component>();

        clusterMethodBox = new JComboBox<HierarchicalClusterMethod>(HierarchicalClusterMethod.values());
        clusterMethodBox.setSelectedItem(IClusteringOptions.DEFAULT_HIERARCHICAL_METHOD);
        clusterMethodBox.addActionListener(e -> {
            HierarchicalClusterMethod m = (HierarchicalClusterMethod) clusterMethodBox.getSelectedItem();
            options.setHierarchicalMethod(m);
        });

        JLabel clusterLabel = new JLabel(CLUSTER_METHOD_LBL);
        clusterLabel.setToolTipText(Labels.HIERARCHICAL_CLUSTER_METHOD);
        labels.add(clusterLabel);
        fields.add(clusterMethodBox);

        this.addLabelTextRows(labels, fields, layout, panel);
        return panel;
    }

    protected JPanel createIncludePanel() throws Exception {
        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        List<JLabel> labels = new ArrayList<JLabel>();
        List<Component> fields = new ArrayList<Component>();

        includeProfilesCheckBox = new JCheckBox(EMPTY_STRING);
        includeProfilesCheckBox.setSelected(IClusteringOptions.DEFAULT_INCLUDE_PROFILE);
        includeProfilesCheckBox.addChangeListener(this);

        labels.add(new JLabel(INCLUDE_PROFILE_LBL));
        fields.add(includeProfilesCheckBox);

        // Add selection of profile type
        for (ProfileType type : ProfileType.values()) {
            JRadioButton button = new JRadioButton(EMPTY_STRING, type.equals(ProfileType.ANGLE)); // set
                                                                                                  // angle
                                                                                                  // selected
            JLabel label = new JLabel(type.toString());
            profileButtonGroup.add(button);
            button.addActionListener(e -> {
                options.setProfileType(type);
            });

            labels.add(label);
            fields.add(button);
        }

        includeMeshCheckBox = new JCheckBox(EMPTY_STRING);
        includeMeshCheckBox.setSelected(IClusteringOptions.DEFAULT_INCLUDE_MESH);
        includeMeshCheckBox.addChangeListener(e -> {
            options.setIncludeMesh(includeMeshCheckBox.isSelected());
        });
        includeMeshCheckBox.setToolTipText(Labels.REQUIRES_CONSENSUS_LBL);

        JLabel meshLabel = new JLabel(INCLUDE_MESH_LBL);
        meshLabel.setToolTipText(Labels.REQUIRES_CONSENSUS_LBL);
        labels.add(meshLabel);
        fields.add(includeMeshCheckBox);

        DecimalFormat pf = new DecimalFormat("#0.000");
        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(dataset.getCollection().getNucleusType())) {

            String pval = "";
            try {
                double[] stats = dataset.getCollection().getRawValues(stat, CellularComponent.NUCLEUS,
                        MeasurementScale.PIXELS);
                double diptest = DipTester.getDipTestPValue(stats);
                pval = pf.format(diptest);
            } catch (Exception e) {
                warn("Error getting p-value");
                stack(e.getMessage(), e);
            }

            JCheckBox box = new JCheckBox(P_VALUE_LBL + pval, false);
            box.addChangeListener(e -> {
                options.setIncludeStatistic(stat, box.isSelected());
            });
            box.setForeground(Color.DARK_GRAY);

            JLabel label = new JLabel(stat.toString());
            labels.add(label);
            fields.add(box);
            statBoxMap.put(stat, box);
        }

        for (IBorderSegment s : dataset.getCollection().getProfileCollection().getSegments(Tag.REFERENCE_POINT)) {

            String pval = "";
            try {
                double[] stats = dataset.getCollection().getRawValues(PlottableStatistic.LENGTH,
                        CellularComponent.NUCLEAR_BORDER_SEGMENT, MeasurementScale.PIXELS, s.getID());
                double diptest = DipTester.getDipTestPValue(stats);
                pval = pf.format(diptest);
            } catch (Exception e) {
                error("Error getting p-value", e);
            }

            JCheckBox box = new JCheckBox(P_VALUE_LBL + pval);
            box.setForeground(Color.DARK_GRAY);
            box.setSelected(false);
            box.addChangeListener(this);
            JLabel label = new JLabel("Length of " + s.getName());
            labels.add(label);
            fields.add(box);
            segmentBoxMap.put(s.getID(), box);
        }

        this.addLabelTextRows(labels, fields, layout, panel);

        boolean hasConsensus = dataset.getCollection().hasConsensus();
        includeMeshCheckBox.setEnabled(hasConsensus); // using consensus for
                                                      // building attributes and
                                                      // template mesh
        meshLabel.setEnabled(hasConsensus);

        return panel;

    }

    @Override
    public void stateChanged(ChangeEvent e) {

        if (e.getSource() == includeProfilesCheckBox) {
            options.setIncludeProfile(includeProfilesCheckBox.isSelected());

            Enumeration<AbstractButton> buttons = profileButtonGroup.getElements();
            while (buttons.hasMoreElements()) {
                AbstractButton b = buttons.nextElement();
                b.setEnabled(includeProfilesCheckBox.isSelected());

            }
        }

        try {

            for (IBorderSegment s : dataset.getCollection().getProfileCollection().getSegments(Tag.REFERENCE_POINT)) {

                JCheckBox box = segmentBoxMap.get(s.getID());

                options.setIncludeSegment(s.getID(), box.isSelected());
            }
        } catch (Exception e1) {
            error("Error setting segment include data", e1);
        }

    }
}
