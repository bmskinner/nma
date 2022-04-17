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
package com.bmskinner.nuclear_morphology.gui;

import java.awt.Component;
import java.util.Collection;

import javax.swing.JPopupMenu;

/**
 * The base class for popup menus
 * 
 * @author bms41
 * @since 1.14.0
 *
 */
public abstract class AbstractPopupMenu extends JPopupMenu {

	public static final String SOURCE_COMPONENT = "PopupMenu";

	protected AbstractPopupMenu() {
		super("Popup");
		createButtons();
		addButtons();
	}

	protected abstract void createButtons();

	protected abstract void addButtons();

	public abstract void updateSelectionContext(Collection<Object> objects);

	@Override
	public void setEnabled(boolean b) {
		for (Component c : this.getComponents()) {
			c.setEnabled(b);
		}
	}
}
