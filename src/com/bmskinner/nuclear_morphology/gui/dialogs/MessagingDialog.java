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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.Dialog;

import javax.swing.JDialog;

import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;

/**
 * This extension to a JDialog can fire DatasetEvents and InterfaceEvents to
 * registered listeners. Used to communicate dataset updates and chart recache
 * requests from dialogs which modify datasets.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class MessagingDialog extends JDialog {

	protected final InputSupplier inputSupplier = new DefaultInputSupplier();

	public MessagingDialog() {
		super();
	}

	/**
	 * Create with the given Dialog as a parent. Use null to make this dialog have a
	 * taskbar icon
	 * 
	 * @param d the parent. Can be null
	 */
	public MessagingDialog(Dialog d) {
		super(d);
	}
}
