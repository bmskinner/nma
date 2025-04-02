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

    private static final String DIALOG_TITLE = "Preferences";
    
    private static final Logger LOGGER = Logger.getLogger(MainPreferencesDialog.class.getName());
    
    private static final String SHOW_DEBUG_UI_LBL = "Show debug UI";
    private static final String USE_ANTIALIASING_LBL = "Use antialiasing in charts";
    private static final String DEFAULT_RULESET_LBL = "Default ruleset";
    private static final String DEFAULT_IMAGE_SCALE_LBL = "Default image scale";
    private static final String DEFAULT_COLOUR_SWATCH_LBL = "Default colour palette";
    private static final String DEFAULT_IMAGE_DIR_LBL = "Default image directory";
    private static final String CHECK_FOR_UPDATES_LBL = "Check for updates on startup";
    private static final String DEFAULT_DISPLAY_SCALE_LBL = "Default display scale";
    private static final String DEFAULT_FILL_CONSENSUS_LBL = "Default filling consensus";
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
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        List<JLabel> labels = new ArrayList<>();
        List<Component> fields = new ArrayList<>();

        // Checkboxes
        
        JCheckBox antiAliasBox = new JCheckBox((String) null, currentOptions.isAntiAlias());
        antiAliasBox.addActionListener(e -> currentOptions.setBoolean(GlobalOptions.IS_USE_ANTIALIASING, 
        		antiAliasBox.isSelected()));

//        JCheckBox showDebugInterfaceBox = new JCheckBox((String) null, currentOptions.getBoolean(GlobalOptions.IS_DEBUG_INTERFACE_KEY));
//        showDebugInterfaceBox.addActionListener(e -> currentOptions.setBoolean(GlobalOptions.IS_DEBUG_INTERFACE_KEY, 
//        		showDebugInterfaceBox.isSelected()));
        
        
        JCheckBox checkUpdatesBox = new JCheckBox((String) null, currentOptions.getBoolean(GlobalOptions.ALLOW_UPDATE_CHECK_KEY));
        checkUpdatesBox.addActionListener(e -> currentOptions.setBoolean(GlobalOptions.ALLOW_UPDATE_CHECK_KEY, 
        		checkUpdatesBox.isSelected()));
        
        JCheckBox consensusBox = new JCheckBox((String) null, currentOptions.getBoolean(GlobalOptions.DEFAULT_FILL_CONSENSUS_KEY));
        consensusBox.addActionListener(e -> currentOptions.setBoolean(GlobalOptions.DEFAULT_FILL_CONSENSUS_KEY, 
        		consensusBox.isSelected()));
        
        JCheckBox memoryBox = new JCheckBox((String) null, currentOptions.getBoolean(GlobalOptions.WARN_LOW_JVM_MEMORY_FRACTION));
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
        
        // Layout
        labels.add(new JLabel(CHECK_FOR_UPDATES_LBL));
        fields.add(checkUpdatesBox);
        
        labels.add(new JLabel(WARN_LOW_JVM_MEMORY_FRACTION_LBL));
        fields.add(memoryBox);
        
        labels.add(new JLabel(USE_ANTIALIASING_LBL));
        fields.add(antiAliasBox);

        labels.add(new JLabel(DEFAULT_FILL_CONSENSUS_LBL));
        fields.add(consensusBox);
        
        labels.add(new JLabel(DEFAULT_COLOUR_SWATCH_LBL));
        fields.add(paletteBox);
        
        labels.add(new JLabel(DEFAULT_DISPLAY_SCALE_LBL));
        fields.add(measurementScaleBox);
        
        labels.add(new JLabel(DEFAULT_IMAGE_SCALE_LBL));
        fields.add(scaleSpinner);
        
        labels.add(new JLabel(DEFAULT_RULESET_LBL));
        fields.add(rulesetBox);
        
        labels.add(new JLabel(DEFAULT_IMAGE_DIR_LBL));
        fields.add(defaultDirBox);
        
        
        this.addLabelTextRows(labels, fields, layout, panel);
        return panel;
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}
