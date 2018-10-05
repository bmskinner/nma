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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;

/**
 * Allow selection of signal groups within a dataset via a combo-box
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class SignalGroupSelectionPanel extends EnumeratedOptionsPanel {

    final JComboBox<SignalIDToGroup> box;

    public SignalGroupSelectionPanel(@NonNull final IAnalysisDataset d) {

        List<SignalIDToGroup> list = getGroups(d);

        box = new JComboBox<>(list.toArray(new SignalIDToGroup[0]));

        SignalManager m = d.getCollection().getSignalManager();
        if (m.hasSignals())
            box.setSelectedIndex(0);

        box.addActionListener(this);
        this.add(box);
    }

    public void setDataset(@NonNull final IAnalysisDataset d) {

        SignalManager m = d.getCollection().getSignalManager();
        if (!m.hasSignals()) {
            this.setEnabled(false);
            return;
        }

        List<SignalIDToGroup> list = getGroups(d);

        ComboBoxModel<SignalIDToGroup> model = new DefaultComboBoxModel<>(
                list.toArray(new SignalIDToGroup[0]));

        box.setModel(model);
        box.setSelectedIndex(0);

    }

    public boolean hasSelection() {
        return box.getSelectedItem() != null;
    }

    public ISignalGroup getSelectedGroup() {
        SignalIDToGroup temp = (SignalIDToGroup) box.getSelectedItem();
        return temp.getGroup();
    }

    public UUID getSelectedID() {
        SignalIDToGroup temp = (SignalIDToGroup) box.getSelectedItem();
        return temp.getId();
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        box.setEnabled(b);
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

    private List<SignalIDToGroup> getGroups(IAnalysisDataset d) {
        SignalManager m = d.getCollection().getSignalManager();
        Set<UUID> signalGroups = m.getSignalGroupIDs();
        List<SignalIDToGroup> list = new ArrayList<SignalIDToGroup>();
        for (UUID id : signalGroups) {

            if (id.equals(IShellResult.RANDOM_SIGNAL_ID)) {
                continue;
            }
            list.add(new SignalIDToGroup(id, d.getCollection().getSignalGroup(id).get()));
        }
        return list;
    }

    private class SignalIDToGroup {

        final private UUID         id;
        final private ISignalGroup group;

        public SignalIDToGroup(final UUID id, final ISignalGroup group) {
            this.id = id;
            this.group = group;
        }

        public UUID getId() {
            return id;
        }

        public ISignalGroup getGroup() {
            return group;
        }

        @Override
		public String toString() {
            return group.getGroupName();
        }

    }

}
