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
package com.bmskinner.nma.gui.components.panels;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public abstract class EnumeratedOptionsPanel extends JPanel implements ActionListener {

	protected List<ActionListener> listeners = new ArrayList<>();

	public EnumeratedOptionsPanel() {
		this.setLayout(new FlowLayout());
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		for (ActionListener a : listeners) {
			a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, e.getActionCommand()) {
			});
		}
	}

	public void addActionListener(ActionListener a) {
		this.listeners.add(a);
	}

	public synchronized void removeActionListener(ActionListener l) {
		listeners.remove(l);
	}
}
