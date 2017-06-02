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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;

/**
 * Provides a drop down list with the displayable profile types.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class ProfileTypeOptionsPanel extends EnumeratedOptionsPanel {

    private JComboBox<ProfileType> profileTypeBox; // = new
                                                   // JComboBox<ProfileType>(ProfileType.values());

    public ProfileTypeOptionsPanel() {
        super();

        // FrankenProfiles cause problems for individual nuclei, so disable for
        // now
        DefaultComboBoxModel<ProfileType> model = new DefaultComboBoxModel<ProfileType>();

        for (ProfileType type : ProfileType.displayValues()) {
            model.addElement(type);
        }

        profileTypeBox = new JComboBox<ProfileType>(model);

        this.add(profileTypeBox);
        profileTypeBox.addActionListener(this);
        profileTypeBox.setSelectedItem(ProfileType.ANGLE);

    }

    public ProfileType getSelected() {
        return (ProfileType) profileTypeBox.getSelectedItem();
    }

    public void setEnabled(boolean b) {
        profileTypeBox.setEnabled(b);

    }
}
