/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.text.ParseException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;

@SuppressWarnings("serial")
public class ThresholdSettingsPanel extends DetectionSettingsPanel {

    private static final Integer MIN_RANGE = Integer.valueOf(0);
    private static final Integer MAX_RANGE = Integer.valueOf(255);
    private static final Integer STEP      = Integer.valueOf(1);

    private static final String THRESHOLD_LBL = "Threshold";

    private JSpinner thresholdSpinner;

    public ThresholdSettingsPanel(final IDetectionOptions options) {
        super(options);

        this.add(createPanel(), BorderLayout.CENTER);

    }

    private JPanel createPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        thresholdSpinner = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(options.getThreshold()), MIN_RANGE, MAX_RANGE, STEP));

        JLabel lbl = new JLabel(THRESHOLD_LBL);

        panel.add(lbl);
        panel.add(thresholdSpinner);

        thresholdSpinner.addChangeListener(e -> {
            try {
                thresholdSpinner.commitEdit();
                options.setThreshold(((Integer) thresholdSpinner.getValue()).intValue());
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                warn("Parsing error in JSpinner");
                stack("Parsing error in JSpinner", e1);
            }
        });

        return panel;
    }

    @Override
    protected void update() {
        super.update();
        isUpdating = true;
        thresholdSpinner.setValue(options.getThreshold());
        isUpdating = false;
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        thresholdSpinner.setEnabled(b);

    }

    @Override
    public void set(IDetectionOptions options) {
        this.options.set(options);
        update();

    }
}
