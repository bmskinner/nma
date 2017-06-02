/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.components.panels;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import com.bmskinner.nuclear_morphology.gui.RotationMode;

@SuppressWarnings("serial")
public class RotationSelectionSettingsPanel extends EnumeratedOptionsPanel {

    private Map<RotationMode, JRadioButton> map = new HashMap<RotationMode, JRadioButton>();

    public RotationSelectionSettingsPanel() {
        super();

        final ButtonGroup group = new ButtonGroup();

        for (RotationMode type : RotationMode.values()) {
            JRadioButton button = new JRadioButton(type.toString());
            button.setActionCommand(type.toString());
            button.addActionListener(this);
            this.add(button);
            group.add(button);
            map.put(type, button);
        }
        // Set the default
        map.get(RotationMode.ACTUAL).setSelected(true);
    }

    public RotationMode getSelected() {
        for (RotationMode type : RotationMode.values()) {
            JRadioButton button = map.get(type);
            if (button.isSelected()) {
                return type;
            }
        }
        return null;
    }

    public void setEnabled(boolean b) {

        for (RotationMode type : RotationMode.values()) {
            map.get(type).setEnabled(b);
        }
    }
}
