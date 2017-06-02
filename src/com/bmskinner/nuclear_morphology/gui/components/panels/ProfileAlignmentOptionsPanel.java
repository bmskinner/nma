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

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;

@SuppressWarnings("serial")
public class ProfileAlignmentOptionsPanel extends EnumeratedOptionsPanel {

    private Map<ProfileAlignment, JRadioButton> map          = new HashMap<ProfileAlignment, JRadioButton>();
    private JCheckBox                           normCheckBox = new JCheckBox("Normalised");                  // to
                                                                                                             // toggle
                                                                                                             // raw
                                                                                                             // or
                                                                                                             // normalised
                                                                                                             // segment
                                                                                                             // profiles
                                                                                                             // in
                                                                                                             // segmentsProfileChartPanel

    public ProfileAlignmentOptionsPanel() {
        super();

        // checkbox to select raw or normalised profiles
        normCheckBox.setSelected(true);
        normCheckBox.addActionListener(this);
        this.add(normCheckBox);

        final ButtonGroup group = new ButtonGroup();

        for (ProfileAlignment type : ProfileAlignment.values()) {
            JRadioButton button = new JRadioButton(type.toString());
            button.setActionCommand(type.toString());
            button.addActionListener(this);
            button.setEnabled(false);
            this.add(button);
            group.add(button);
            map.put(type, button);
        }
        // Set the default
        map.get(ProfileAlignment.LEFT).setSelected(true);
    }

    public ProfileAlignment getSelected() {
        for (ProfileAlignment type : ProfileAlignment.values()) {
            JRadioButton button = map.get(type);
            if (button.isSelected()) {
                return type;
            }
        }
        return null;
    }

    public boolean isNormalised() {
        return this.normCheckBox.isSelected();
    }

    public void setEnabled(boolean b) {

        normCheckBox.setEnabled(b);
        if (b == false) {
            for (ProfileAlignment type : ProfileAlignment.values()) {
                map.get(type).setEnabled(b);
            }
        }
        if (b == true) {
            if (normCheckBox.isSelected()) {
                for (ProfileAlignment type : ProfileAlignment.values()) {
                    map.get(type).setEnabled(false);
                }
            } else {
                for (ProfileAlignment type : ProfileAlignment.values()) {
                    map.get(type).setEnabled(true);
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);

        if (normCheckBox.isSelected()) {

            for (ProfileAlignment type : ProfileAlignment.values()) {
                map.get(type).setEnabled(false);
            }

        } else {
            for (ProfileAlignment type : ProfileAlignment.values()) {
                map.get(type).setEnabled(true);
            }
        }
    }

    public enum ProfileAlignment {

        LEFT("Left"), RIGHT("Right");

        private String name;

        ProfileAlignment(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }

}
