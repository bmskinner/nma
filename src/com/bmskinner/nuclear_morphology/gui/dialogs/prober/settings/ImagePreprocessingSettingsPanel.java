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
package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Holds the Kuwahara and flattening settings for nucleus detection
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class ImagePreprocessingSettingsPanel extends DetectionSettingsPanel {
	
	private static final Logger LOGGER = Logger.getLogger(ImagePreprocessingSettingsPanel.class.getName());

    public static final Integer KUWAHARA_WIDTH_MIN  = Integer.valueOf(1);
    public static final Integer KUWAHARA_WIDTH_MAX  = Integer.valueOf(11);
    public static final Integer KUWAHARA_WIDTH_STEP = Integer.valueOf(2);

    public static final Integer FLATTEN_THRESHOLD_MIN  = Integer.valueOf(0);
    public static final Integer FLATTEN_THRESHOLD_MAX  = Integer.valueOf(255);
    public static final Integer FLATTEN_THRESHOLD_STEP = Integer.valueOf(1);

    private static final String USE_KUWAHARA_LBL          = "Kuwahara filter";
    private static final String FLATTEN_CHROMOCENTRES_LBL = "Flatten chromocentres";

    private static final String KUWAHARA_KERNEL_LBL      = "Kuwahara kernel";
    private static final String FLATTENING_THRESHOLD_LBL = "Flattening threshold";

    private JCheckBox useKuwaharaCheckBox;
    private JSpinner  kuwaharaRadiusSpinner;

    private JCheckBox flattenImageCheckBox;
    private JSpinner  flattenImageThresholdSpinner;

    private JCheckBox addBorderCheckBox;

    public ImagePreprocessingSettingsPanel(HashOptions options) {
    	super(options);
        createSpinners();
        createPanel();
    }

    /**
     * Update the display to the given options
     * 
     */
    @Override
	protected void update() {
        super.update();

        kuwaharaRadiusSpinner.setValue(options.getInt(HashOptions.KUWAHARA_RADIUS_INT));
        flattenImageThresholdSpinner.setValue(options.getInt(HashOptions.FLATTENING_THRESHOLD_INT));

        useKuwaharaCheckBox.setSelected(options.getBoolean(HashOptions.IS_USE_KUWAHARA));
        flattenImageCheckBox.setSelected(options.getBoolean(HashOptions.IS_USE_FLATTENING));

    }

    /**
     * Create the spinners with the default options in the CannyOptions
     * CannyOptions must therefore have been assigned defaults
     */
    private void createSpinners() {

        kuwaharaRadiusSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.KUWAHARA_RADIUS_INT)),
                KUWAHARA_WIDTH_MIN, KUWAHARA_WIDTH_MAX, KUWAHARA_WIDTH_STEP));

        flattenImageThresholdSpinner = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.FLATTENING_THRESHOLD_INT)), FLATTEN_THRESHOLD_MIN,
                        FLATTEN_THRESHOLD_MAX, FLATTEN_THRESHOLD_STEP));

        useKuwaharaCheckBox = new JCheckBox("", options.getBoolean(HashOptions.IS_USE_KUWAHARA));
        useKuwaharaCheckBox.addActionListener(e -> {
        	kuwaharaRadiusSpinner.setEnabled(useKuwaharaCheckBox.isSelected());
        	updateOptions(HashOptions.IS_USE_KUWAHARA, useKuwaharaCheckBox.isSelected());

        });

        flattenImageCheckBox = new JCheckBox("", options.getBoolean(HashOptions.IS_USE_FLATTENING));
        flattenImageCheckBox.addActionListener(e -> {
            flattenImageThresholdSpinner.setEnabled(flattenImageCheckBox.isSelected());
            updateOptions(HashOptions.IS_USE_FLATTENING, flattenImageCheckBox.isSelected());
        });

        // Add the border adding box
        addBorderCheckBox = new JCheckBox("", options.getBoolean(HashOptions.IS_CANNY_ADD_BORDER));
        addBorderCheckBox.addActionListener(e -> {
        	updateOptions(HashOptions.IS_CANNY_ADD_BORDER, addBorderCheckBox.isSelected());
        });

        flattenImageThresholdSpinner.addChangeListener(e -> {
            try {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                updateOptions(HashOptions.FLATTENING_THRESHOLD_INT, (Integer) j.getValue());
            } catch (ParseException e1) {
                LOGGER.warning("Parsing exception");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });

        kuwaharaRadiusSpinner.addChangeListener(e -> {
            try {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                Integer value = (Integer) j.getValue();

                if (value.intValue() % 2 == 0) { // even
                    // only odd values are allowed
                    j.setValue(value.intValue() - 1);

                } else {
                	updateOptions(HashOptions.KUWAHARA_RADIUS_INT, (Integer) j.getValue());
                }

            } catch (ParseException e1) {
                LOGGER.warning("Parsing exception");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });

    }

    private void createPanel() {

        this.setLayout(new GridBagLayout());

        List<JLabel> labelList = new ArrayList<>();
        List<JComponent> fieldList = new ArrayList<>();

        labelList.add(new JLabel(USE_KUWAHARA_LBL));
        labelList.add(new JLabel(KUWAHARA_KERNEL_LBL));
        labelList.add(new JLabel(FLATTEN_CHROMOCENTRES_LBL));
        labelList.add(new JLabel(FLATTENING_THRESHOLD_LBL));
        // labelList.add(new JLabel(ADD_BORDER_LBL));

        JLabel[] labels = labelList.toArray(new JLabel[0]);

        fieldList.add(useKuwaharaCheckBox);
        fieldList.add(kuwaharaRadiusSpinner);
        fieldList.add(flattenImageCheckBox);
        fieldList.add(flattenImageThresholdSpinner);
        // fieldList.add(addBorderCheckBox);

        JComponent[] fields = fieldList.toArray(new JComponent[0]);

        addLabelTextRows(labels, fields, this);

    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);

        if (b) {
            flattenImageThresholdSpinner.setEnabled(flattenImageCheckBox.isSelected());
            kuwaharaRadiusSpinner.setEnabled(useKuwaharaCheckBox.isSelected());
        } else {
            flattenImageThresholdSpinner.setEnabled(false);
            kuwaharaRadiusSpinner.setEnabled(false);
        }

        useKuwaharaCheckBox.setEnabled(b);
        flattenImageCheckBox.setEnabled(b);

    }

}
