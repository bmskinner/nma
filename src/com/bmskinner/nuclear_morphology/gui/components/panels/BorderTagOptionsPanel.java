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

import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import com.bmskinner.nuclear_morphology.components.profiles.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.profiles.Tag;
import com.bmskinner.nuclear_morphology.components.profiles.BorderTagObject.BorderTagType;

@SuppressWarnings("serial")
public class BorderTagOptionsPanel extends EnumeratedOptionsPanel {

    private Map<Tag, JRadioButton> map = new HashMap<>();

    public BorderTagOptionsPanel() {

        super();
        final ButtonGroup group = new ButtonGroup();

        for (BorderTagObject type : BorderTagObject.values(BorderTagType.CORE)) {
            JRadioButton button = new JRadioButton(type.toString());
            button.setActionCommand(type.toString());
            button.addActionListener(this);
            this.add(button);
            group.add(button);
            map.put(type, button);
        }
        // Set the default
        map.get(Tag.REFERENCE_POINT).setSelected(true);

    }

    public void setEnabled(boolean b) {
        for (Tag type : BorderTagObject.values(BorderTagType.CORE))
            map.get(type).setEnabled(b);
    }

    /**
     * Get the selected profile type, or null
     * 
     * @return
     */
    public Tag getSelected() {
        for (Tag type : BorderTagObject.values(BorderTagType.CORE)) {
            JRadioButton button = map.get(type);
            if (button.isSelected())
                return type;
        }
        return null;
    }

}
