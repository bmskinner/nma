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
package com.bmskinner.nma.gui.dialogs;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JDialog;

import com.bmskinner.nma.core.InputSupplier;
import com.bmskinner.nma.gui.DefaultInputSupplier;

/**
 * This extension to a JDialog contains an input supplier for user interaction
 * 
 * @author Ben Skinner
 *
 */
@SuppressWarnings("serial")
public abstract class MessagingDialog extends JDialog {

	protected final InputSupplier inputSupplier = new DefaultInputSupplier();

	/**
	 * Create with no parent. The dialog will have a taskbar icon. Equivalent to
	 * {@code MessagingDialog((Dialog) null);}
	 */
	protected MessagingDialog() {
		super((Dialog) null);
	}

	/**
	 * Create with the given Dialog as a parent. The dialog will have a taskbar
	 * icon.
	 * 
	 * @param d the parent. Can be null
	 */
	protected MessagingDialog(Dialog d) {
		super(d);
	}

	/**
	 * Centre the dialog on the screen. Note this should be invoked after packing or
	 * location will be incorrect.
	 */
	public void centerOnScreen() {
		final int width = getWidth();
		final int height = getHeight();
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screenSize.width / 2) - (width / 2);
		int y = (screenSize.height / 2) - (height / 2);

		setLocation(x, y);
	}
}
