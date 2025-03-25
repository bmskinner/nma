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
package com.bmskinner.nma.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.gui.dialogs.prober.OptionsChangeEvent;

/**
 * Builder for options panels. Use this to make the individual component
 * settings panels for a new analysis
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class ConstructableSettingsPanel extends SettingsPanel {

    public static final String  TOPHAT_LBL         = "Top hat filtering";
    public static final String  OBJECT_FINDING_LBL = "Object finding";
    public static final String  SIZE_SETTINGS_LBL  = "Filtering";
    public static final String  PROFILING_LBL      = "Landmark detection";
    public static final String  MISC_LBL           = "Other";
    public static final String  CHANNEL_LBL        = "Image";
    public static final String  PREPROCESSING_LBL  = "Preprocessing";
    public static final String  THRESHOLDING_LBL   = "Thresholding";
    private static final String RELOAD_LBL         = "Reload";

    private IAnalysisOptions options;

    private JPanel mainPanel;

    /**
     * Build with an options to store values
     * 
     * @param options
     */
    public ConstructableSettingsPanel(IAnalysisOptions options) {
        this.options = options;
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    }

    /**
     * Finalise the panel. Must be invoked before the panel is used.
     * 
     * @return
     */
    public ConstructableSettingsPanel build() {
        this.add(mainPanel, BorderLayout.CENTER);
        return this;
    }

    /**
     * Add a panel with a manual reload button
     * 
     * @return
     */
    public ConstructableSettingsPanel addReloadFooterPanel() {
        JPanel panel = new JPanel();
        JButton reloadBtn = new JButton(RELOAD_LBL);
        reloadBtn.addActionListener(e -> {
            fireProberReloadEvent();
        });

        panel.add(reloadBtn);
        this.add(panel, BorderLayout.SOUTH);
        return this;
    }

    /**
     * Add a panel switching between thresholding and edge detection
     * 
     * @param optionsKey
     *            the options subtype to select
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addEdgeThresholdSwitchPanel(String optionsKey) throws MissingOptionException {
        return addEdgeThresholdSwitchPanel(optionsKey, OBJECT_FINDING_LBL);
    }

    /**
     * Add a panel switching between thresholding and edge detection
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addEdgeThresholdSwitchPanel(String optionsKey, String label)
            throws MissingOptionException {
        Optional<HashOptions> subOptions = options.getDetectionOptions(optionsKey);
        if(!subOptions.isPresent())
        	return this;
        
        SettingsPanel panel = new EdgeThresholdSwitchPanel(subOptions.get());
        panel.setBorder(BorderFactory.createTitledBorder(label));
        this.addSubPanel(panel);
        mainPanel.add(panel);
        return this;
    }

    /**
     * Add a panel switching between thresholding and edge detection
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addColourThresholdWatershedSwitchPanel(String optionsKey)
            throws MissingOptionException {
        return addEdgeThresholdSwitchPanel(optionsKey, OBJECT_FINDING_LBL);
    }

    /**
     * Add a panel for threshold images
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addThresholdPanel(String optionsKey, String label) throws MissingOptionException {
        Optional<HashOptions> subOptions = options.getDetectionOptions(optionsKey);
        if(!subOptions.isPresent())
        	return this;
        SettingsPanel panel = new ThresholdSettingsPanel(subOptions.get());
        panel.setBorder(BorderFactory.createTitledBorder(label));
        this.addSubPanel(panel);
        mainPanel.add(panel);
        return this;
    }

    /**
     * Add a panel for threshold images with the default label
     * 
     * @param optionsKey
     *            the options subtype to select
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addThresholdPanel(String optionsKey) throws MissingOptionException {
        return addThresholdPanel(optionsKey, THRESHOLDING_LBL);
    }

    /**
     * Add a panel switching between thresholding and edge detection
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addColourThresholdWatershedSwitchPanel(String optionsKey, String label)
            throws MissingOptionException {
    	Optional<HashOptions> subOptions = options.getDetectionOptions(optionsKey);
        if(!subOptions.isPresent())
        	return this;
        SettingsPanel panel = new ColourThresholdWatershedSwitchPanel(subOptions.get());
        panel.setBorder(BorderFactory.createTitledBorder(label));
        this.addSubPanel(panel);
        mainPanel.add(panel);
        return this;
    }

    /**
     * Add a panel controlling top hat filtering with the default label
     * 
     * @param optionsKey
     *            the options subtype to select
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addTopHatPanel(String optionsKey) throws MissingOptionException {
        return addTopHatPanel(optionsKey, TOPHAT_LBL);
    }

    /**
     * Add a panel controlling top hat filtering
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addTopHatPanel(String optionsKey, String label) throws MissingOptionException {
    	Optional<HashOptions> subOptions = options.getDetectionOptions(optionsKey);
        if(!subOptions.isPresent())
        	return this;
        SettingsPanel panel = new TophatPanel(subOptions.get());
        panel.setBorder(BorderFactory.createTitledBorder(label));
        this.addSubPanel(panel);
        mainPanel.add(panel);
        return this;
    }

    /**
     * Add a panel for colour thresholding images
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addColorThresholdPanel(String optionsKey) throws MissingOptionException {
        return addColorThresholdPanel(optionsKey, THRESHOLDING_LBL);
    }

    /**
     * Add a panel for colour thresholding images
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addColorThresholdPanel(String optionsKey, String label)
            throws MissingOptionException {
    	Optional<HashOptions> subOptions = options.getDetectionOptions(optionsKey);
        if(!subOptions.isPresent())
        	return this;
        SettingsPanel panel = new ColourThresholdingSettingsPanel(subOptions.get());
        panel.setBorder(BorderFactory.createTitledBorder(label));
        this.addSubPanel(panel);
        mainPanel.add(panel);
        return this;
    }

    /**
     * Add a panel for preprocessing images and background removal
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addImageProcessingPanel(String optionsKey) throws MissingOptionException {
        return addImageProcessingPanel(optionsKey, PREPROCESSING_LBL);
    }

    /**
     * Add a panel for preprocessing images and background removal
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addImageProcessingPanel(String optionsKey, String label)
            throws MissingOptionException {
    	Optional<HashOptions> subOptions = options.getDetectionOptions(optionsKey);
        if(!subOptions.isPresent())
        	return this;
        SettingsPanel panel = new ImagePreprocessingSettingsPanel(subOptions.get());
        panel.setBorder(BorderFactory.createTitledBorder(label));
        this.addSubPanel(panel);
        mainPanel.add(panel);
        return this;
    }

    /**
     * Add a panel for component size and circularity
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addSizePanel(String optionsKey) throws MissingOptionException {
        return addSizePanel(optionsKey, SIZE_SETTINGS_LBL);
    }

    /**
     * Add a panel for component size and circularity
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addSizePanel(String optionsKey, String label) throws MissingOptionException {
    	Optional<HashOptions> subOptions = options.getDetectionOptions(optionsKey);
        if(!subOptions.isPresent())
        	return this;
        SettingsPanel panel = new ComponentSizeSettingsPanel(subOptions.get());
        panel.setBorder(BorderFactory.createTitledBorder(label));
        this.addSubPanel(panel);
        mainPanel.add(panel);
        return this;
    }

    /**
     * Add a panel for nucleus type and angle window size
     * 
     * @param optionsKey the options subtype to select
     * @param label the label to give the panel
     * @return
     */
    public ConstructableSettingsPanel addNucleusProfilePanel(String optionsKey) {
        return addNucleusProfilePanel(optionsKey, PROFILING_LBL);
    }

    /**
     * Add a panel for nucleus type and angle window size
     * 
     * @param optionsKey the options subtype to select
     * @param label the label to give the panel
     * @return
     */
    public ConstructableSettingsPanel addNucleusProfilePanel(String optionsKey, String label) {
        SettingsPanel panel = new NucleusProfileSettingsPanel(options);
        panel.setBorder(BorderFactory.createTitledBorder(label));
        this.addSubPanel(panel);
        mainPanel.add(panel);
        return this;
    }

    /**
     * Add a panel for image channel selection with the default label
     * 
     * @param optionsKey
     *            the options subtype to select
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addImageChannelPanel(String optionsKey) throws MissingOptionException {
        return addImageChannelPanel(optionsKey, CHANNEL_LBL);
    }

    /**
     * Add a panel for image channel selection
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addImageChannelPanel(String optionsKey, String label)
            throws MissingOptionException {
    	Optional<HashOptions> subOptions = options.getDetectionOptions(optionsKey);
        if(!subOptions.isPresent())
        	return this;
        SettingsPanel panel = new ImageChannelSettingsPanel(subOptions.get());
        panel.setBorder(BorderFactory.createTitledBorder(label));
        this.addSubPanel(panel);
        mainPanel.add(panel);
        return this;
    }

    /**
     * Add a panel for copying options values from open datasets
     * 
     * @param optionsKey
     *            the options subtype to select
     * @param label
     *            the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addCopyFromOpenPanel(String optionsKey) throws MissingOptionException {
        return addCopyFromOpenPanel(optionsKey, null);
    }

    /**
     * Add a panel for copying options values from open datasets
     * 
     * @param optionsKey the options subtype to select
     * @param label the label to give the panel
     * @return
     * @throws MissingOptionException
     */
    public ConstructableSettingsPanel addCopyFromOpenPanel(String optionsKey, String label)
            throws MissingOptionException {
    	Optional<HashOptions> subOptions = options.getDetectionOptions(optionsKey);
        if(!subOptions.isPresent())
        	return this;
        SettingsPanel panel = new CopyNucleusDetectionSettingsFromOpenDatasetPanel(options, subOptions.get());
        if (label != null) {
            panel.setBorder(BorderFactory.createTitledBorder(label));
        }
        this.addSubPanel(panel);
        mainPanel.add(panel);
        return this;
    }

    @Override
    public void optionsChangeEventReceived(OptionsChangeEvent e) {
        super.optionsChangeEventReceived(e);
        if (this.hasSubPanel((SettingsPanel) e.getSource())) {
            fireProberReloadEvent();
        }
    }
    
    /**
     * Set the options values and update the spinners to match
     * 
     * @param options
     */
    public void set(IAnalysisOptions options) {
    	this.options.set(options);
    	update();
    }

}
