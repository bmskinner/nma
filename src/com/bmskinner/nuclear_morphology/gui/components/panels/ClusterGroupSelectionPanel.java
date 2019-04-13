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

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;

import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.gui.dialogs.ClusterTreeDialog;

/**
 * A panel with a drop down list of cluster groups, specified in the
 * constructor. Add an actionlistener to the panel, and access the selected
 * dataset via the getSelectedDataset() method.
 * @since 1.13.8
 *
 */
public class ClusterGroupSelectionPanel extends EnumeratedOptionsPanel {

    JComboBox<IClusterGroup> box;

    public ClusterGroupSelectionPanel(final List<IClusterGroup> list) {
        
        box = new JComboBox<>();
        for (IClusterGroup d : list) {
            box.addItem(d);
        }

        if(!list.isEmpty())
            box.setSelectedItem(list.get(0));

        box.addActionListener(this);

        this.add(box);
    }

    public IClusterGroup getSelectedItem() {
        return (IClusterGroup) box.getSelectedItem();
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        box.setEnabled(b);
    }
    
    
    /**
     * Clear the selection. 
     */
    public void setSelectionNull(){
        List<ActionListener> oldListeners = new ArrayList<>(listeners);
        listeners.clear();
        box.setSelectedIndex(-1);
        listeners = oldListeners;
    }

    public void setSelectionIndex(int i) {

        if (i > box.getItemCount() - 1) {
            return;
        }
        if (i < 0) {
            return;
        }
        box.setSelectedItem(i);
    }

    public void setSelectedGroup(IClusterGroup d) {

        box.setSelectedItem(d);
    }
}
