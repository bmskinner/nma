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

import java.awt.Color;

import javax.swing.JTextArea;
import javax.swing.UIManager;

/**
 * An extension to a JTextArea that allows for 
 * word wrapped labels
 * @author Ben Skinner
 * @since 1.14.0
 *
 */
public class WrappedLabel extends JTextArea {
	
	public WrappedLabel(String text) {
		super(text);
	    setLineWrap(true);
	    setWrapStyleWord(true);
	    setEditable(false);
	    setFont(UIManager.getFont("Label.font"));
	    setBackground((Color) UIManager.get("Panel.background"));
	}
	

}
