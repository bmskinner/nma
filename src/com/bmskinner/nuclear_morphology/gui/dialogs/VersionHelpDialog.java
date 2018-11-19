package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.gui.main.MainView;

/**
 * A dialog that displays current version info and allows the user to check for updates
 * @author bms41
 * @since 1.15.0
 *
 */
public class VersionHelpDialog extends SettingsDialog {
	
	private static final String DIALOG_TITLE = "About";
	
	 public VersionHelpDialog(final MainView mw) {
	        super((Frame) mw, false);

	        this.setLayout(new BorderLayout());
	        this.setTitle(DIALOG_TITLE);
	        
	        this.add(createMainPanel(), BorderLayout.CENTER);
	        this.add(createFooter(), BorderLayout.SOUTH);

	        this.setLocationRelativeTo(null);
	        this.setMinimumSize(new Dimension(100, 70));
	        this.pack();
	        this.setVisible(true);

	    }
	 
	 private JPanel createMainPanel() {
		 
		 JTextArea textBox = new JTextArea();
		 textBox.setText("Nuclear Morphology Analysis version "+Version.currentVersion());
		 textBox.setEditable(false);
		 
		 JPanel panel = new JPanel();
		 
		 panel.add(textBox);
		 return panel;
	 }

}
