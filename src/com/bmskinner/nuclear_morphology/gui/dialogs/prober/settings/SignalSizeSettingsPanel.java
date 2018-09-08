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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;

/**
 * A size settings panel for signals, which replaces the max size with a max
 * fraction option.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class SignalSizeSettingsPanel extends DetectionSettingsPanel implements ChangeListener {

    private static final String MIN_SIZE_LBL = "Min area (pixels)";
    private static final String MAX_SIZE_LBL = "Max fraction";
    private static final String MIN_CIRC_LBL = "Min circ (pixels)";
    private static final String MAX_CIRC_LBL = "Min circ (pixels)";

    private static final Integer MIN_RANGE_SIZE = 1;
    private static final double  MIN_RANGE_CIRC = 0;
    private static final double  MAX_RANGE_CIRC = 1;

    private static final Integer SIZE_STEP_SIZE = 1;
    private static final double  CIRC_STEP_SIZE = 0.05;

    private JSpinner minSizeSpinner;
    private JSpinner maxSizeSpinner;
    private JSpinner minCircSpinner;
    private JSpinner maxCircSpinner;

    public SignalSizeSettingsPanel(final INuclearSignalOptions options) {
        super(options);
        this.add(createPanel(), BorderLayout.CENTER);

    }

    /**
     * Create the settings spinners based on the input options
     */
    private void createSpinners() {
    	INuclearSignalOptions op = (INuclearSignalOptions) options;

        minSizeSpinner = new JSpinner(
                new SpinnerNumberModel(new Integer((int) options.getMinSize()), MIN_RANGE_SIZE, null, SIZE_STEP_SIZE));

        maxSizeSpinner = new JSpinner(
                new SpinnerNumberModel(op.getMaxFraction(), MIN_RANGE_CIRC, MAX_RANGE_CIRC, CIRC_STEP_SIZE));

        minCircSpinner = new JSpinner(
                new SpinnerNumberModel(options.getMinCirc(), MIN_RANGE_CIRC, MAX_RANGE_CIRC, CIRC_STEP_SIZE));

        maxCircSpinner = new JSpinner(
                new SpinnerNumberModel(options.getMaxCirc(), MIN_RANGE_CIRC, MAX_RANGE_CIRC, CIRC_STEP_SIZE));

        Dimension dim = new Dimension(BOX_WIDTH, BOX_HEIGHT);
        minSizeSpinner.setPreferredSize(dim);
        maxSizeSpinner.setPreferredSize(dim);
        minCircSpinner.setPreferredSize(dim);
        maxCircSpinner.setPreferredSize(dim);

        minSizeSpinner.addChangeListener(this);
        maxSizeSpinner.addChangeListener(this);
        minCircSpinner.addChangeListener(this);
        maxCircSpinner.addChangeListener(this);
    }

    /**
     * Create the panel containing the settings spinners
     * 
     * @return
     */
    private JPanel createPanel() {

        this.createSpinners();

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());

        List<JLabel> labels = new ArrayList<JLabel>();

        labels.add(new JLabel(MIN_SIZE_LBL));
        labels.add(new JLabel(MAX_SIZE_LBL));
        labels.add(new JLabel(MIN_CIRC_LBL));
        labels.add(new JLabel(MAX_CIRC_LBL));

        List<Component> fields = new ArrayList<Component>();

        fields.add(minSizeSpinner);
        fields.add(maxSizeSpinner);
        fields.add(minCircSpinner);
        fields.add(maxCircSpinner);

        addLabelTextRows(labels, fields, panel);

        return panel;
    }

    /**
     * Update the spinners to current options values
     */
    @Override
    protected void update() {
        super.update();
        INuclearSignalOptions op = (INuclearSignalOptions) options;

        minSizeSpinner.setValue((int) options.getMinSize());
        maxSizeSpinner.setValue(op.getMaxFraction());
        minCircSpinner.setValue(options.getMinCirc());
        maxCircSpinner.setValue(options.getMaxCirc());
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        minSizeSpinner.setEnabled(b);
        maxSizeSpinner.setEnabled(b);
        minCircSpinner.setEnabled(b);
        maxCircSpinner.setEnabled(b);

    }
    
    /**
     * Set the options values and update the spinners to match
     * 
     * @param options
     */
    public void set(@NonNull INuclearSignalOptions options) {
    	this.options.set(options);
    	update();
    }

    @Override
    public void stateChanged(ChangeEvent e) {

    	INuclearSignalOptions op = (INuclearSignalOptions) options;

        try {

            if (e.getSource() == minSizeSpinner) {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();
                options.setMinSize((Integer) j.getValue());
            }

            if (e.getSource() == maxSizeSpinner) {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();

                op.setMaxFraction((Double) j.getValue());
            }

            if (e.getSource() == minCircSpinner) {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();

                // ensure never larger than max
                if ((Double) j.getValue() > (Double) maxCircSpinner.getValue()) {
                    j.setValue(maxCircSpinner.getValue());
                }

                options.setMinCirc((Double) j.getValue());
            }

            if (e.getSource() == maxCircSpinner) {
                JSpinner j = (JSpinner) e.getSource();
                j.commitEdit();

                // ensure never smaller than min
                if ((Double) j.getValue() < (Double) minCircSpinner.getValue()) {
                    j.setValue(minCircSpinner.getValue());
                }

                options.setMaxCirc((Double) j.getValue());
            }

            fireOptionsChangeEvent();

        } catch (ParseException e1) {
            stack("Parsing error in JSpinner", e1);
        }

    }

}
