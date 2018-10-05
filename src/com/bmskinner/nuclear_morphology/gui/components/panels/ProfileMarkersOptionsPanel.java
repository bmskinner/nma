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

import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class ProfileMarkersOptionsPanel extends EnumeratedOptionsPanel {

    private JCheckBox checkBox       = new JCheckBox("Show tags");
    private JCheckBox showNucleiBox  = new JCheckBox("Show nuclear profiles");

    public ProfileMarkersOptionsPanel() {
        super();

        // checkbox to select raw or normalised profiles
        checkBox.setSelected(true);
        checkBox.addActionListener(this);
        this.add(checkBox);

        // checkbox to show or hide individual nucleus profiles
        showNucleiBox.setSelected(true);
        showNucleiBox.addActionListener(this);
        this.add(showNucleiBox);

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
    public boolean isShowNuclei() {
        return showNucleiBox.isSelected();
    }

    public void setEnabled(boolean b) {

        this.checkBox.setEnabled(b);
        this.showNucleiBox.setEnabled(b);
    }

}
