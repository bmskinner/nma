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
package com.bmskinner.nuclear_morphology.gui.components.panels;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;

@SuppressWarnings("serial")
public class MeasurementUnitSettingsPanel extends EnumeratedOptionsPanel {

    private Map<MeasurementScale, JRadioButton> map = new HashMap<MeasurementScale, JRadioButton>();

    /**
     * Create a panel with all available MeasurementScales as radio buttons
     * 
     */
    public MeasurementUnitSettingsPanel() {
        super();

        final ButtonGroup group = new ButtonGroup();

        for (MeasurementScale type : MeasurementScale.values()) {
            JRadioButton button = new JRadioButton(type.toString());
            button.setActionCommand(type.toString());
            button.addActionListener(this);
            this.add(button);
            group.add(button);
            map.put(type, button);
        }
        // Set the default
        map.get(GlobalOptions.getInstance().getScale()).setSelected(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        GlobalOptions.getInstance().setScale(getSelected());
        fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
    }

    /**
     * Get the currently selected scale
     * 
     * @return
     */
    private MeasurementScale getSelected() {
        for (MeasurementScale type : MeasurementScale.values()) {
            JRadioButton button = map.get(type);
            if (button.isSelected()) {
                return type;
            }
        }
        return null;
    }

    public void setEnabled(boolean b) {

        for (MeasurementScale type : MeasurementScale.values()) {
            map.get(type).setEnabled(b);
        }
    }
}
