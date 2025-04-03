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
package com.bmskinner.nma.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nma.io.ConfigFileReader;
import com.bmskinner.nma.io.ConfigFileReader.RulesetEntry;

@SuppressWarnings("serial")
public class MainPreferencesDialog extends SettingsDialog implements ActionListener {

    private static final String DISPLAY_DEFAULTS_LBL = "Display defaults";
	private static final String PROGRAM_DEFAULT_LBL = "Program defaults";
	private static final String NEW_ANALYSIS_DEFAULTS_LBL = "New analysis defaults";

	private static final String DIALOG_TITLE = "Preferences";
    
    private static final Logger LOGGER = Logger.getLogger(MainPreferencesDialog.class.getName());
    
    private static final String SHOW_DEBUG_UI_LBL = "Show debug UI";
    private static final String USE_ANTIALIASING_LBL = "Antialiasing in charts";
    private static final String DEFAULT_RULESET_LBL = "Ruleset";
    private static final String DEFAULT_IMAGE_SCALE_LBL = "Image scale";
    private static final String DEFAULT_COLOUR_SWATCH_LBL = "Colour palette";
    private static final String DEFAULT_IMAGE_DIR_LBL = "Image directory";
    private static final String CHECK_FOR_UPDATES_LBL = "Check for updates on startup";
    private static final String DEFAULT_DISPLAY_SCALE_LBL = "Measurement units";
    private static final String DEFAULT_FILL_CONSENSUS_LBL = "Fill consensus nuclei";
    private static final String WARN_LOW_JVM_MEMORY_FRACTION_LBL = "Warn of low memory on startup";
        
    private final GlobalOptions oldOptions;
    private final GlobalOptions currentOptions;
    

    public MainPreferencesDialog() {
    	// Store a copy of the options at the point the dialog is created
    	oldOptions = GlobalOptions.getInstance().duplicate();
    	currentOptions = GlobalOptions.getInstance();

        this.setLayout(new BorderLayout());
        this.setTitle(DIALOG_TITLE);

        this.add(createMainPanel(), BorderLayout.CENTER);
        this.add(createFooter(), BorderLayout.SOUTH);

        this.setLocationRelativeTo(null);
        this.setMinimumSize(new Dimension(200, 100));
        this.pack();
        this.setVisible(true);

    }

    /**
     * Create the panel footer, with just a close button buttons
     * 
     * @return
     */
    @Override
    protected JPanel createFooter() {

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("Save and close");
        okButton.addActionListener(e -> {
        	if(!GlobalOptions.getInstance().equals(oldOptions)) {
        		// options have changed, write them to file
        		try {
        			LOGGER.fine("Writing changed options to config file");
					ConfigFileReader.writeGlobalOptionsToConfigFile();
				} catch (IOException e1) {
					LOGGER.log(Level.SEVERE, "Unable to save options to config file: %s".formatted(e1.getMessage()));
				}
        	}
            setVisible(false);
            dispose();
        });
        panel.add(okButton);

        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        BoxLayout bl = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(bl);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Checkboxes
        
        JCheckBox antiAliasBox = new JCheckBox((String) null, currentOptions.isAntiAlias());
        antiAliasBox.addActionListener(e -> currentOptions.setBoolean(GlobalOptions.IS_USE_ANTIALIASING, 
        		antiAliasBox.isSelected()));

        JCheckBox checkUpdatesBox = new JCheckBox("(notifies only, updates will not be downloaded)", currentOptions.getBoolean(GlobalOptions.ALLOW_UPDATE_CHECK_KEY));
        checkUpdatesBox.addActionListener(e -> currentOptions.setBoolean(GlobalOptions.ALLOW_UPDATE_CHECK_KEY, 
        		checkUpdatesBox.isSelected()));
        
        JCheckBox consensusBox = new JCheckBox((String) null, currentOptions.getBoolean(GlobalOptions.DEFAULT_FILL_CONSENSUS_KEY));
        consensusBox.addActionListener(e -> currentOptions.setBoolean(GlobalOptions.DEFAULT_FILL_CONSENSUS_KEY, 
        		consensusBox.isSelected()));
        
        JCheckBox memoryBox = new JCheckBox("(less than half system memory available)", currentOptions.getBoolean(GlobalOptions.WARN_LOW_JVM_MEMORY_FRACTION));
        memoryBox.addActionListener(e -> currentOptions.setBoolean(GlobalOptions.WARN_LOW_JVM_MEMORY_FRACTION, 
        		memoryBox.isSelected()));
        
        
        // Comboboxes
        RulesetEntry[] availableRules = ConfigFileReader.getAvailableRulesets();
        JComboBox<RulesetEntry> rulesetBox = new JComboBox<>(availableRules);
        rulesetBox.addActionListener(e -> {
			RulesetEntry selected = (RulesetEntry) rulesetBox.getSelectedItem();
			currentOptions.setString(GlobalOptions.DEFAULT_RULESET_KEY, selected.rsc().getName());
		});
        
        
        JComboBox<ColourSwatch> paletteBox = new JComboBox<>(ColourSwatch.values());
        paletteBox.setSelectedItem(currentOptions.getSwatch());
        paletteBox.addActionListener(e -> {
			currentOptions.setSwatch((ColourSwatch) paletteBox.getSelectedItem());
		});
        
        JComboBox<MeasurementScale> measurementScaleBox = new JComboBox<>(MeasurementScale.values());
        measurementScaleBox.setSelectedItem(currentOptions.getDisplayScale());
        measurementScaleBox.addActionListener(e -> {
			currentOptions.setDisplayScale((MeasurementScale) measurementScaleBox.getSelectedItem());
		});
        
        // Spinners
        
        JSpinner scaleSpinner = new JSpinner(new SpinnerNumberModel(
        		currentOptions.getImageScale(), 0, 100, 0.1));
        
        scaleSpinner.addChangeListener(e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				scaleSpinner.commitEdit();
				currentOptions.setImageScale((Double)j.getValue());
			} catch (ParseException e1) {
				LOGGER.log(Level.SEVERE, "Parsing error in image scale: %s".formatted(e1.getMessage()), e1);
			}
        });
        
        // File selector
        
		JTextField  defaultDirBox = new JTextField(currentOptions.getDefaultDir().getAbsolutePath());
		defaultDirBox.setEditable(false);
		defaultDirBox.setEnabled(false);
		defaultDirBox.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				// Image directory may have been specified; if not, request from user

				try {
					File f = new DefaultInputSupplier().requestFolder(GlobalOptions.getInstance().getDefaultDir());
					currentOptions.setFile(GlobalOptions.DEFAULT_DIR_KEY, f);
					defaultDirBox.setText(f.getAbsolutePath());
				} catch (RequestCancelledException ex) {
					// user cancelled
				}
			}
		});
		
		
        // Layout display options
        
		JPanel displayPanel = new JPanel();
		GridBagLayout gl = new GridBagLayout();
		displayPanel.setLayout(gl);
		List<JLabel> displayLabels = new ArrayList<>();
		List<Component> displayFields = new ArrayList<>();
		
		displayPanel.setBorder(BorderFactory.createTitledBorder(DISPLAY_DEFAULTS_LBL));

        displayLabels.add(new JLabel(USE_ANTIALIASING_LBL));
        displayFields.add(antiAliasBox);
        
        displayLabels.add(new JLabel(DEFAULT_FILL_CONSENSUS_LBL));
        displayFields.add(consensusBox);
        
        displayLabels.add(new JLabel(DEFAULT_COLOUR_SWATCH_LBL));
        displayFields.add(paletteBox);
        
        displayLabels.add(new JLabel(DEFAULT_DISPLAY_SCALE_LBL));
        displayFields.add(measurementScaleBox);
        
        addLabelTextRows(displayLabels, displayFields, gl, displayPanel);
		
		
        // Layout program options
        
        JPanel progamPanel = new JPanel();
		GridBagLayout progamgl = new GridBagLayout();
		progamPanel.setLayout(progamgl);
		List<JLabel> progamLabels = new ArrayList<>();
		List<Component> progamFields = new ArrayList<>();
		progamPanel.setBorder(BorderFactory.createTitledBorder(PROGRAM_DEFAULT_LBL));
        
		progamLabels.add(new JLabel(CHECK_FOR_UPDATES_LBL));
		progamFields.add(checkUpdatesBox);
        
        progamLabels.add(new JLabel(WARN_LOW_JVM_MEMORY_FRACTION_LBL));
        progamFields.add(memoryBox);
        addLabelTextRows(progamLabels, progamFields, progamgl, progamPanel);

        // Layout analysis setup options
        
        JPanel analysisPanel = new JPanel();
		GridBagLayout analysisGl = new GridBagLayout();
		analysisPanel.setLayout(progamgl);
		List<JLabel> analysisLabels = new ArrayList<>();
		List<Component> analysisFields = new ArrayList<>();
		analysisPanel.setBorder(BorderFactory.createTitledBorder(NEW_ANALYSIS_DEFAULTS_LBL));

        analysisLabels.add(new JLabel(DEFAULT_IMAGE_DIR_LBL));
        analysisFields.add(defaultDirBox);
		
		analysisLabels.add(new JLabel(DEFAULT_IMAGE_SCALE_LBL));
		analysisFields.add(scaleSpinner);
        
        analysisLabels.add(new JLabel(DEFAULT_RULESET_LBL));
        analysisFields.add(rulesetBox);

        addLabelTextRows(analysisLabels, analysisFields, analysisGl, analysisPanel);
        
        panel.add(progamPanel);
        Box.createVerticalStrut(10);
        panel.add(analysisPanel);
        Box.createVerticalStrut(10);
        panel.add(displayPanel);
        return panel;
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}
