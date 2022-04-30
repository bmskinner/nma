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

import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.logging.Loggable;

@SuppressWarnings("serial")
public class ColourThresholdingSettingsPanel extends SettingsPanel {
	
	private static final Logger LOGGER = Logger.getLogger(ColourThresholdingSettingsPanel.class.getName());

    public static final Integer THRESHOLD_MIN  = Integer.valueOf(0);
    public static final Integer THRESHOLD_MAX  = Integer.valueOf(255);
    public static final Integer THRESHOLD_STEP = Integer.valueOf(1);

    private static final String USE_THRESHOLD_LBL = "Threshold on colour";

    private static final String MIN_HUE_LBL = "Min hue";
    private static final String MAX_HUE_LBL = "Max hue";
    private static final String MIN_SAT_LBL = "Min saturation";
    private static final String MAX_SAT_LBL = "Max saturation";
    private static final String MIN_BRI_LBL = "Min brightness";
    private static final String MAX_BRI_LBL = "Max brightness";

    private JCheckBox useThresholdCheckBox;

    private JSpinner minHueSpinner;
    private JSpinner maxHueSpinner;
    private JSpinner minSatSpinner;
    private JSpinner maxSatSpinner;
    private JSpinner minBriSpinner;
    private JSpinner maxBriSpinner;

    private HashOptions options;

    public ColourThresholdingSettingsPanel(final HashOptions options) {

        this.options = options;

        createSpinners();
        createPanel();
    }

    /**
     * Create the spinners with the default options in the CannyOptions
     * CannyOptions must therefore have been assigned defaults
     */
    private void createSpinners() {

        // JXMultiThumbSlider sl = new JXMultiThumbSlider();

        minHueSpinner = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.MIN_HUE)), THRESHOLD_MIN,
                        THRESHOLD_MAX, THRESHOLD_STEP));

        maxHueSpinner = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.MAX_HUE)), THRESHOLD_MIN,
                        THRESHOLD_MAX, THRESHOLD_STEP));

        minSatSpinner = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.MIN_SAT)), THRESHOLD_MIN,
                        THRESHOLD_MAX, THRESHOLD_STEP));

        maxSatSpinner = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.MAX_SAT)), THRESHOLD_MIN,
                        THRESHOLD_MAX, THRESHOLD_STEP));

        minBriSpinner = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.MIN_BRI)), THRESHOLD_MIN,
                        THRESHOLD_MAX, THRESHOLD_STEP));

        maxBriSpinner = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.MAX_BRI)), THRESHOLD_MIN,
                        THRESHOLD_MAX, THRESHOLD_STEP));

        useThresholdCheckBox = new JCheckBox("", options.getBoolean(HashOptions.IS_USE_COLOUR_THRESHOLD));
        useThresholdCheckBox.addActionListener(e -> {
            boolean isActive = useThresholdCheckBox.isSelected();
            options.setBoolean(HashOptions.IS_USE_COLOUR_THRESHOLD, isActive);
            minHueSpinner.setEnabled(isActive);
            maxHueSpinner.setEnabled(isActive);
            minSatSpinner.setEnabled(isActive);
            maxSatSpinner.setEnabled(isActive);
            minBriSpinner.setEnabled(isActive);
            maxBriSpinner.setEnabled(isActive);
            fireOptionsChangeEvent();
        });

        minHueSpinner.addChangeListener(e -> {
            try {

                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                Integer value = (Integer) j.getValue();

                if (value.intValue() >= (int) maxHueSpinner.getValue()) {
                    j.setValue(value.intValue() - 1); // Cannot be above max

                }

                options.setInt(HashOptions.MIN_HUE, (int) j.getValue());
                options.setInt(HashOptions.MAX_HUE, (int) maxHueSpinner.getValue());

                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                LOGGER.warning("Parsing exception");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });

        maxHueSpinner.addChangeListener(e -> {
            try {

                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                Integer value = (Integer) j.getValue();

                if (value.intValue() <= (int) minHueSpinner.getValue()) {
                    j.setValue(value.intValue() + 1); // Cannot be above max

                }

                options.setInt(HashOptions.MIN_HUE, (int) minHueSpinner.getValue());
                options.setInt(HashOptions.MAX_HUE, (int) j.getValue());

                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                LOGGER.warning("Parsing exception");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });

        minSatSpinner.addChangeListener(e -> {
            try {

                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                Integer value = (Integer) j.getValue();

                if (value.intValue() >= (int) maxSatSpinner.getValue()) {
                    j.setValue(value.intValue() - 1); // Cannot be above max

                }
                
                options.setInt(HashOptions.MIN_SAT, (int) j.getValue());
                options.setInt(HashOptions.MAX_SAT, (int) maxSatSpinner.getValue());

                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                LOGGER.warning("Parsing exception");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });

        maxSatSpinner.addChangeListener(e -> {
            try {

                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                Integer value = (Integer) j.getValue();

                if (value.intValue() <= (int) minSatSpinner.getValue()) {
                    j.setValue(value.intValue() + 1); // Cannot be above max

                }
                
                options.setInt(HashOptions.MIN_SAT, (int) minSatSpinner.getValue());
                options.setInt(HashOptions.MAX_SAT, (int) j.getValue());
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                LOGGER.warning("Parsing exception");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });

        minBriSpinner.addChangeListener(e -> {
            try {

                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                Integer value = (Integer) j.getValue();

                if (value.intValue() >= (int) maxBriSpinner.getValue()) {
                    j.setValue(value.intValue() - 1); // Cannot be above max

                }

                options.setInt(HashOptions.MIN_BRI, (int) j.getValue());
                options.setInt(HashOptions.MAX_BRI, (int) maxBriSpinner.getValue());

                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                LOGGER.warning("Parsing exception");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });

        maxBriSpinner.addChangeListener(e -> {
            try {

                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                Integer value = (Integer) j.getValue();

                if (value.intValue() <= (int) minBriSpinner.getValue()) {
                    j.setValue(value.intValue() + 1); // Cannot be above max

                }

                options.setInt(HashOptions.MIN_BRI, (int) minBriSpinner.getValue());
                options.setInt(HashOptions.MAX_BRI, (int) j.getValue());

                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                LOGGER.warning("Parsing exception");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }

        });

    }

    private void createPanel() {

        this.setLayout(new GridBagLayout());

        List<JLabel> labelList = new ArrayList<JLabel>();
        List<JComponent> fieldList = new ArrayList<JComponent>();

        // labelList.add(new JLabel(USE_THRESHOLD_LBL));
        labelList.add(new JLabel(MIN_HUE_LBL));
        labelList.add(new JLabel(MAX_HUE_LBL));
        labelList.add(new JLabel(MIN_SAT_LBL));
        labelList.add(new JLabel(MAX_SAT_LBL));
        labelList.add(new JLabel(MIN_BRI_LBL));
        labelList.add(new JLabel(MAX_BRI_LBL));

        JLabel[] labels = labelList.toArray(new JLabel[0]);

        // fieldList.add(useThresholdCheckBox);
        fieldList.add(minHueSpinner);
        fieldList.add(maxHueSpinner);
        fieldList.add(minSatSpinner);
        fieldList.add(maxSatSpinner);
        fieldList.add(minBriSpinner);
        fieldList.add(maxBriSpinner);

        JComponent[] fields = fieldList.toArray(new JComponent[0]);

        addLabelTextRows(labels, fields, this);

    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);

        useThresholdCheckBox.setEnabled(b);
        minHueSpinner.setEnabled(b);
        maxHueSpinner.setEnabled(b);
        minSatSpinner.setEnabled(b);
        maxSatSpinner.setEnabled(b);
        minBriSpinner.setEnabled(b);
        maxBriSpinner.setEnabled(b);

    }

}
