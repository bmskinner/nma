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

import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class GenericCheckboxPanel extends EnumeratedOptionsPanel {

	private JCheckBox checkBox = new JCheckBox();

	/**
	 * Construct with a text label
	 * 
	 * @param label the text to display by the checkbox
	 */
	public GenericCheckboxPanel(String label) {
		super();
		checkBox.setSelected(false);
		checkBox.addActionListener(this);
		checkBox.setText(label);
		this.add(checkBox);

	}

	public boolean isSelected() {
		return this.checkBox.isSelected();
	}

	@Override
	public void setEnabled(boolean b) {
		checkBox.setEnabled(b);
	}

	public void setText(String s) {
		checkBox.setText(s);
	}

}
