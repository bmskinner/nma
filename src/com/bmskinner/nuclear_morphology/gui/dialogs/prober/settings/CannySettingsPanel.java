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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;

/**
 * A panel that allows changes to be made to a CannyOptions
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class CannySettingsPanel extends SettingsPanel implements ActionListener {

    public static final double THRESHOLD_STEP_SIZE = 0.05;

    public static final double THRESHOLD_MIN     = 0;
    public static final double KERNEL_RADIUS_MIN = 0;

    public static final double LOW_THRESHOLD_MAX  = 10;
    public static final double HIGH_THRESHOLD_MAX = 20;
    public static final double KERNEL_RADIUS_MAX  = 20;

    public static final Integer CANNY_KERNEL_WIDTH_MIN  = Integer.valueOf(1);
    public static final Integer CANNY_KERNEL_WIDTH_MAX  = Integer.valueOf(50);
    public static final Integer CANNY_KERNEL_WIDTH_STEP = Integer.valueOf(1);

    public static final Integer CLOSING_RADIUS_MIN  = Integer.valueOf(1);
    public static final Integer CLOSING_RADIUS_MAX  = Integer.valueOf(100);
    public static final Integer CLOSING_RADIUS_STEP = Integer.valueOf(1);

    private static final String AUTO_THRESHOLD_ACTION = "CannyAutoThreshold";

    // private static final String AUTO_THRESHOLD_LBL = "Canny auto threshold";

    private static final String LOW_THRESHOLD_LBL  = "Canny low threshold";
    private static final String HIGH_THRESHOLD_LBL = "Canny high threshold";
    private static final String KERNEL_RADIUS_LBL  = "Canny kernel radius";
    private static final String KERNEL_WIDTH_LBL   = "Canny kernel width";
    private static final String CLOSING_RADIUS_LBL = "Gap closing radius";

    private JSpinner  cannyLowThreshold;
    private JSpinner  cannyHighThreshold;
    private JSpinner  cannyKernelRadius;
    private JSpinner  cannyKernelWidth;
    private JSpinner  closingObjectRadiusSpinner;
    private JCheckBox cannyAutoThresholdCheckBox;

    private ICannyOptions options;

    public CannySettingsPanel(final ICannyOptions options) {
        this.options = options;
        createSpinners();
        createPanel();
    }

    /**
     * Update the display to the options
     * 
     */
    @Override
	protected void update() {
        super.update();
        isUpdating = true;
        cannyLowThreshold.setValue((double) options.getLowThreshold());
        cannyHighThreshold.setValue((double) options.getHighThreshold());
        cannyKernelRadius.setValue((double) options.getKernelRadius());
        cannyKernelWidth.setValue(options.getKernelWidth());
        closingObjectRadiusSpinner.setValue(options.getClosingObjectRadius());

        cannyAutoThresholdCheckBox.setSelected(options.isCannyAutoThreshold());
        isUpdating = false;
    }

    public void set(final ICannyOptions options) {

        this.options.set(options);
        update();

    }

    /**
     * Create the spinners with the default options in the CannyOptions
     * CannyOptions must therefore have been assigned defaults
     */
    private void createSpinners() {

        cannyLowThreshold = new JSpinner(new SpinnerNumberModel(options.getLowThreshold(), THRESHOLD_MIN,
                LOW_THRESHOLD_MAX, THRESHOLD_STEP_SIZE));

        cannyHighThreshold = new JSpinner(new SpinnerNumberModel(options.getHighThreshold(), THRESHOLD_MIN,
                HIGH_THRESHOLD_MAX, THRESHOLD_STEP_SIZE));

        cannyKernelRadius = new JSpinner(new SpinnerNumberModel(options.getKernelRadius(), KERNEL_RADIUS_MIN,
                KERNEL_RADIUS_MAX, THRESHOLD_STEP_SIZE));

        cannyKernelWidth = new JSpinner(new SpinnerNumberModel(Integer.valueOf(options.getKernelWidth()),
                CANNY_KERNEL_WIDTH_MIN, CANNY_KERNEL_WIDTH_MAX, CANNY_KERNEL_WIDTH_STEP));

        closingObjectRadiusSpinner = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(options.getClosingObjectRadius()), CLOSING_RADIUS_MIN,
                        CLOSING_RADIUS_MAX, CLOSING_RADIUS_STEP));

        cannyAutoThresholdCheckBox = new JCheckBox("", false);
        cannyAutoThresholdCheckBox.setActionCommand(AUTO_THRESHOLD_ACTION);
        cannyAutoThresholdCheckBox.addActionListener(this);

        // add the change listeners
        cannyLowThreshold.addChangeListener(e -> {
            try {
                JSpinner j = (JSpinner) e.getSource();
                cannyLowThreshold.commitEdit();

                if ((Double) j.getValue() > (Double) cannyHighThreshold.getValue()) {
                    cannyLowThreshold.setValue(cannyHighThreshold.getValue());
                }
                Double doubleValue = (Double) j.getValue();
                options.setLowThreshold(doubleValue.floatValue());
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                warn("Parsing exception");
                stack("Parsing error in JSpinner", e1);
            }

        });

        // add the change listeners
        cannyHighThreshold.addChangeListener(e -> {
            try {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();

                if ((Double) j.getValue() < (Double) cannyLowThreshold.getValue()) {
                    j.setValue(cannyLowThreshold.getValue());
                }
                Double doubleValue = (Double) j.getValue();
                options.setHighThreshold(doubleValue.floatValue());
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                warn("Parsing exception");
                stack("Parsing error in JSpinner", e1);
            }

        });

        cannyKernelRadius.addChangeListener(e -> {
            try {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                Double doubleValue = (Double) j.getValue();
                options.setKernelRadius(doubleValue.floatValue());
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                warn("Parsing exception");
                stack("Parsing error in JSpinner", e1);
            }

        });

        cannyKernelWidth.addChangeListener(e -> {
            try {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                Integer value = (Integer) j.getValue();
                options.setKernelWidth(value.intValue());
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                warn("Parsing exception");
                stack("Parsing error in JSpinner", e1);
            }

        });

        closingObjectRadiusSpinner.addChangeListener(e -> {
            try {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                options.setClosingObjectRadius((Integer) j.getValue());
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                warn("Parsing exception");
                stack("Parsing error in JSpinner", e1);
            }

        });

    }

    private void createPanel() {

        this.setLayout(new GridBagLayout());

        List<JLabel> labelList = new ArrayList<JLabel>();
        List<JComponent> fieldList = new ArrayList<JComponent>();

        // labelList.add(new JLabel(AUTO_THRESHOLD_LBL));
        labelList.add(new JLabel(LOW_THRESHOLD_LBL));
        labelList.add(new JLabel(HIGH_THRESHOLD_LBL));
        labelList.add(new JLabel(KERNEL_RADIUS_LBL));
        labelList.add(new JLabel(KERNEL_WIDTH_LBL));
        labelList.add(new JLabel(CLOSING_RADIUS_LBL));

        JLabel[] labels = labelList.toArray(new JLabel[0]);

        // fieldList.add(cannyAutoThresholdCheckBox);
        fieldList.add(cannyLowThreshold);
        fieldList.add(cannyHighThreshold);
        fieldList.add(cannyKernelRadius);
        fieldList.add(cannyKernelWidth);
        fieldList.add(closingObjectRadiusSpinner);

        JComponent[] fields = fieldList.toArray(new JComponent[0]);

        addLabelTextRows(labels, fields, this);

    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);

        if (b) {
            cannyLowThreshold.setEnabled(!cannyAutoThresholdCheckBox.isSelected());
            cannyHighThreshold.setEnabled(!cannyAutoThresholdCheckBox.isSelected());
        } else {
            cannyLowThreshold.setEnabled(false);
            cannyHighThreshold.setEnabled(false);
        }

        cannyKernelRadius.setEnabled(b);
        cannyKernelWidth.setEnabled(b);
        closingObjectRadiusSpinner.setEnabled(b);

        cannyAutoThresholdCheckBox.setEnabled(b);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(AUTO_THRESHOLD_ACTION)) {

            if (cannyAutoThresholdCheckBox.isSelected()) {
                options.setCannyAutoThreshold(true);
                cannyLowThreshold.setEnabled(false);
                cannyHighThreshold.setEnabled(false);
            } else {
                options.setCannyAutoThreshold(false);
                cannyLowThreshold.setEnabled(true);
                cannyHighThreshold.setEnabled(true);
            }
        }

        fireOptionsChangeEvent();
    }
}
