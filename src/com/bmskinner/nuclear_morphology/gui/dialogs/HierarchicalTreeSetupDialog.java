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
import com.bmskinner.nuclear_morphology.analysis.classification.TreeBuildingMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.HierarchicalClusterMethod;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.Labels;
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
    protected static final String CLUSTER_METHOD_LBL  = "Distance method";
    private static final String INCLUDE_TSNE_LBL    = "Include tSNE";
    private static final String INCLUDE_PROFILE_LBL = "Include profiles";
    private static final String P_VALUE_LBL         = "  p(uni) = ";

    protected static final ProfileType DEFAULT_PROFILE_TYPE = ProfileType.ANGLE;

    protected JPanel headingPanel;
    protected JPanel optionsPanel;
    protected JPanel footerPanel;

//    protected JComboBox<HierarchicalClusterMethod> clusterMethodBox;
    
    protected Map<PlottableStatistic, JCheckBox> statBoxMap;

    protected Map<UUID, JCheckBox> segmentBoxMap;

    protected final IClusteringOptions options;

    public HierarchicalTreeSetupDialog(final IAnalysisDataset dataset) {

        // modal dialog
        this(dataset, DIALOG_TITLE);
    }

    /**
     * Constructor that does not make panel visible
     * 
     * @param mw
     * @param title
     */
    protected HierarchicalTreeSetupDialog(final IAnalysisDataset dataset, final String title) {
        super(dataset, title);
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
    @Override
	protected void setDefaults() {
        options.setClusterNumber(IClusteringOptions.DEFAULT_MANUAL_CLUSTER_NUMBER);
        options.setHierarchicalMethod(IClusteringOptions.DEFAULT_HIERARCHICAL_METHOD);
        options.setIterations(IClusteringOptions.DEFAULT_EM_ITERATIONS);
        options.setUseSimilarityMatrix(IClusteringOptions.DEFAULT_USE_SIMILARITY_MATRIX);
        options.setIncludeProfile(IClusteringOptions.DEFAULT_INCLUDE_PROFILE);
        options.setProfileType(DEFAULT_PROFILE_TYPE);
        options.setIncludeMesh(IClusteringOptions.DEFAULT_INCLUDE_MESH);
        options.setBoolean(IClusteringOptions.USE_TSNE_KEY,  IClusteringOptions.DEFAULT_USE_TSNE);
    }

    protected JPanel createHeader() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    @Override
    public IAnalysisMethod getMethod() {
    	return new TreeBuildingMethod(dataset, options);
    }

    @Override
    protected void createUI() {

        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPanel);

        statBoxMap = new HashMap<>();
        segmentBoxMap = new HashMap<>();
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

        List<JLabel> labels = new ArrayList<>();
        List<Component> fields = new ArrayList<>();

        JComboBox<HierarchicalClusterMethod> clusterMethodBox = new JComboBox<>(HierarchicalClusterMethod.values());
        clusterMethodBox.setSelectedItem(IClusteringOptions.DEFAULT_HIERARCHICAL_METHOD);
        clusterMethodBox.addActionListener(e -> {
            options.setHierarchicalMethod( (HierarchicalClusterMethod) clusterMethodBox.getSelectedItem());
        });

        JLabel clusterLabel = new JLabel(CLUSTER_METHOD_LBL);
        clusterLabel.setToolTipText(Labels.Clusters.HIERARCHICAL_CLUSTER_METHOD);
        labels.add(clusterLabel);
        fields.add(clusterMethodBox);

        addLabelTextRows(labels, fields, layout, panel);
        return panel;
    }

    protected JPanel createIncludePanel() throws Exception {
        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        List<JLabel> labels = new ArrayList<>();
        List<Component> fields = new ArrayList<>();
        
        // Only allow the tSNE option if it has been calculated
        boolean hasTsne = dataset.getCollection().getNuclei().stream().allMatch(n->n.hasStatistic(PlottableStatistic.TSNE_X));
        String includeTsneString = hasTsne ? EMPTY_STRING : "  N/A";
        JCheckBox includeTsneCheckBox = new JCheckBox(includeTsneString);
        includeTsneCheckBox.setSelected(hasTsne);
        includeTsneCheckBox.setEnabled(hasTsne);
        includeTsneCheckBox.addChangeListener(e->{
        	options.setBoolean(IClusteringOptions.USE_TSNE_KEY, includeTsneCheckBox.isSelected());
        });
        labels.add(new JLabel(INCLUDE_TSNE_LBL));
        fields.add(includeTsneCheckBox); 
        
        // Only set as default if there are no tSNE results
        JCheckBox includeProfilesCheckBox = new JCheckBox(EMPTY_STRING);
        includeProfilesCheckBox.setSelected(!hasTsne);
        
        labels.add(new JLabel(INCLUDE_PROFILE_LBL));
        fields.add(includeProfilesCheckBox);

        // Add selection of profile type
        JComboBox<ProfileType> profileBox = new JComboBox<>(ProfileType.displayValues());
        profileBox.setSelectedItem(ProfileType.ANGLE);
        profileBox.setEnabled(!hasTsne);
        
        profileBox.addActionListener(e->options.setProfileType( (ProfileType) profileBox.getSelectedItem()));
        labels.add(new JLabel("Profile type"));
        fields.add(profileBox);
        
        includeProfilesCheckBox.addChangeListener(e->{
        	options.setIncludeProfile(includeProfilesCheckBox.isSelected());
        	profileBox.setEnabled(includeProfilesCheckBox.isSelected());
        });

        
        // Add the individual stats
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

        addLabelTextRows(labels, fields, layout, panel);
        return panel;
    }

    @Override
    public void stateChanged(ChangeEvent e) {

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
