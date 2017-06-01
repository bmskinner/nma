/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.components.panels;

import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class ProfileMarkersOptionsPanel extends EnumeratedOptionsPanel {

    private JCheckBox checkBox       = new JCheckBox("Show markers");
    private JCheckBox hideRawProfile = new JCheckBox("Hide profiles");

    public ProfileMarkersOptionsPanel() {
        super();

        // checkbox to select raw or normalised profiles
        checkBox.setSelected(true);
        checkBox.addActionListener(this);
        this.add(checkBox);

        // checkbox to show or hide individual nucleus profiles
        hideRawProfile.setSelected(false);
        hideRawProfile.addActionListener(this);
        this.add(hideRawProfile);

    }

    /**
     * Test if the options panel is set to show profile markers
     * 
     * @return
     */
    public boolean showMarkers() {
        return this.checkBox.isSelected();
    }

    /**
     * Test if the options panel is set to hide raw profiles
     * 
     * @return
     */
    public boolean isHideProfiles() {
        return hideRawProfile.isSelected();
    }

    public void setEnabled(boolean b) {

        this.checkBox.setEnabled(b);
        this.hideRawProfile.setEnabled(b);
    }

}
