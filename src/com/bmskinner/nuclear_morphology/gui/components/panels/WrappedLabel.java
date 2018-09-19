package com.bmskinner.nuclear_morphology.gui.components.panels;

import java.awt.Color;

import javax.swing.JTextArea;
import javax.swing.UIManager;

/**
 * An extension to a JTextArea that allows for 
 * word wrapped labels
 * @author ben
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
