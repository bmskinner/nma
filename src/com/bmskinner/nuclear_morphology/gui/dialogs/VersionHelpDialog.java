package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.gui.main.MainView;
import com.bmskinner.nuclear_morphology.io.Io;

/**
 * A dialog that displays current version info and allows the user to check for updates
 * @author bms41
 * @since 1.15.0
 *
 */
public class VersionHelpDialog extends SettingsDialog {
	
	private static final String DIALOG_TITLE = "About";
	private static final String SITE_URL     = "https://bitbucket.org/bmskinner/nuclear_morphology/wiki/Home/";
	private static final String VIST_WEBSITE_LBL = "Visit website";
	
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
		 textBox.setFont(UIManager.getFont("Label.font"));
		 
		 String text = "Nuclear Morphology Analysis version "+Version.currentVersion()+Io.NEWLINE
				 + Io.NEWLINE
				 + "Help and tutorials are available at: "+Io.NEWLINE
				 + SITE_URL;
		 
		 textBox.setText(text);
		 textBox.setEditable(false);
		 textBox.setLineWrap(false);
		 
		 JPanel panel = new JPanel();
		 
		 panel.add(textBox);
		 return panel;
	 }

	 @Override
	 protected JPanel createFooter() {
		 JPanel panel = new JPanel(new FlowLayout());
		 
		 JButton websiteBtn = new JButton(VIST_WEBSITE_LBL);
		 		 
		 websiteBtn.addActionListener(e->browseToWebsite());
		 panel.add(websiteBtn);
		 return panel;
	 }
	 
	 private void browseToWebsite() {
		 Desktop desktop = Desktop.getDesktop();

		 if( !desktop.isSupported( Desktop.Action.BROWSE ) ) {
			 warn( "Desktop doesn't support the browse action" );
			 return;
		 }
		 try {
			 URI uri = new URI(SITE_URL);
			 desktop.browse(uri);
		 } catch (IOException e1) {
			 warn(e1.getMessage());
		 } catch (URISyntaxException e1) {
			 warn("Unable to parse URI: "+e1.getMessage());
		 }
	 }
}
