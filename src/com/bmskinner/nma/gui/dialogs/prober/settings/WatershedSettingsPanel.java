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
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.logging.Loggable;

public class WatershedSettingsPanel extends DetectionSettingsPanel {
	
	private static final Logger LOGGER = Logger.getLogger(WatershedSettingsPanel.class.getName());

    private static final Integer DYNAMIC_MIN_RANGE = Integer.valueOf(1);
    private static final Integer DYNAMIC_MAX_RANGE = Integer.valueOf(255);
    private static final Integer DYNAMIC_STEP      = Integer.valueOf(1);

    private static final Integer EROSION_MIN_RANGE = Integer.valueOf(1);
    private static final Integer EROSION_MAX_RANGE = Integer.valueOf(100);
    private static final Integer EROSION_STEP      = Integer.valueOf(1);

    private static final String DYNAMIC_LBL = "Dynamic";
    private static final String EROSION_LBL = "Erosion";

    private JSpinner dynamicSpinner;
    private JSpinner erosionSpinner;

    public WatershedSettingsPanel(final HashOptions options) {
        super(options);

        this.add(createPanel(), BorderLayout.CENTER);

    }

    private JPanel createPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        List<JLabel> labelList = new ArrayList<>();
        List<JComponent> fieldList = new ArrayList<>();

        dynamicSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.DYNAMIC)),
                DYNAMIC_MIN_RANGE, DYNAMIC_MAX_RANGE, DYNAMIC_STEP));

        erosionSpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(options.getInt(HashOptions.EROSION)),
                EROSION_MIN_RANGE, EROSION_MAX_RANGE, EROSION_STEP));

        JLabel dynabmicLbl = new JLabel(DYNAMIC_LBL);

        labelList.add(dynabmicLbl);
        fieldList.add(dynamicSpinner);

        dynamicSpinner.addChangeListener(e -> {
            try {
                dynamicSpinner.commitEdit();
                options.setInt(HashOptions.DYNAMIC, ((Integer) dynamicSpinner.getValue()).intValue());
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                LOGGER.warning("Parsing error in JSpinner");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }
        });

        JLabel erosionLbl = new JLabel(EROSION_LBL);

        labelList.add(erosionLbl);
        fieldList.add(erosionSpinner);

        erosionSpinner.addChangeListener(e -> {
            try {
                erosionSpinner.commitEdit();
                options.setInt(HashOptions.EROSION, ((Integer) erosionSpinner.getValue()).intValue());
                fireOptionsChangeEvent();
            } catch (ParseException e1) {
                LOGGER.warning("Parsing error in JSpinner");
                LOGGER.log(Loggable.STACK, "Parsing error in JSpinner", e1);
            }
        });

        addLabelTextRows(labelList, fieldList, panel);

        return panel;
    }

    @Override
    protected void update() {
        super.update();
        isUpdating = true;
        dynamicSpinner.setValue(options.getInt(HashOptions.DYNAMIC));
        erosionSpinner.setValue(options.getInt(HashOptions.EROSION));
        isUpdating = false;
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        dynamicSpinner.setEnabled(b);
        erosionSpinner.setEnabled(b);

    }
}
