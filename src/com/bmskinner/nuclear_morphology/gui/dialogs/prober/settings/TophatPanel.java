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

import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;

/**
 * Set the radius for top hat filtering
 * 
 * @author bms41
 * @since 1.13.5
 *
 */
@SuppressWarnings("serial")
public class TophatPanel extends SettingsPanel {

    public static final Integer TOPHAT_RADIUS_MIN  = Integer.valueOf(1);
    public static final Integer TOPHAT_RADIUS_MAX  = Integer.valueOf(100);
    public static final Integer TOPHAT_RADIUS_STEP = Integer.valueOf(1);

    private static final String RADIUS_LBL = "Top hat radius";

    private JSpinner radiusSpinner;

    private IDetectionOptions options;

    public TophatPanel(final IDetectionOptions options) {
        this.options = options;
        createSpinners();
        createPanel();
    }

    /**
     * Create the spinners with the default options in the CannyOptions
     * CannyOptions must therefore have been assigned defaults
     */
    private void createSpinners() {

        radiusSpinner = new JSpinner(
                new SpinnerNumberModel(Integer.valueOf(options.getInt(IDetectionOptions.TOP_HAT_RADIUS)),
                        TOPHAT_RADIUS_MIN, TOPHAT_RADIUS_MAX, TOPHAT_RADIUS_STEP));

        // add the change listeners
        radiusSpinner.addChangeListener(e -> {
            try {
                JSpinner j = (JSpinner) e.getSource();
                radiusSpinner.commitEdit();

                Integer value = (Integer) j.getValue();
                options.setInt(IDetectionOptions.TOP_HAT_RADIUS, value.intValue());
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                warn("Parsing exception");
                stack("Parsing error in JSpinner", e1);
            }

        });

    }

    private void createPanel() {

        try {

            this.setLayout(new GridBagLayout());

            List<JLabel> labelList = new ArrayList<JLabel>();
            List<JComponent> fieldList = new ArrayList<JComponent>();

            labelList.add(new JLabel(RADIUS_LBL));
            fieldList.add(radiusSpinner);

            addLabelTextRows(labelList, fieldList, this);
        } catch (Exception e) {
            error(e.getMessage(), e);
        }

    }

    /**
     * Update the display to the given options
     * 
     * @param options
     *            the options values to be used
     */
    protected void update() {
        super.update();
        isUpdating = true;
        radiusSpinner.setValue(options.getInt(IDetectionOptions.TOP_HAT_RADIUS));
        isUpdating = false;
    }

}
