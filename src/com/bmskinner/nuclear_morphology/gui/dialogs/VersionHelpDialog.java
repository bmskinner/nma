package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.ScrollPane;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.gui.main.MainView;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.logging.Loggable;


/**
 * A dialog that displays current version info and allows the user to check for updates
 * @author bms41
 * @since 1.15.0
 *
 */
public class VersionHelpDialog extends SettingsDialog {
		
	private static final Logger LOGGER = Logger.getLogger(VersionHelpDialog.class.getName());
	
	private static final String DIALOG_TITLE = "About";
	private static final String SITE_URL     = "https://bitbucket.org/bmskinner/nuclear_morphology/wiki/Home/";
	private static final String VIST_WEBSITE_LBL = "Visit website";
	
	private static final Dimension PREF_SIZE = new Dimension(120, 300);

	 public VersionHelpDialog(final MainView mw) {
	        super((Frame) mw, false);

	        this.setLayout(new BorderLayout());
	        this.setTitle(DIALOG_TITLE);
	        
	        this.add(createMainPanel(), BorderLayout.CENTER);
	        this.add(createFooter(), BorderLayout.SOUTH);

	        this.setLocationRelativeTo(null);
	        this.setMinimumSize(new Dimension(100, 70));
	        
	        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	        this.setMaximumSize(new Dimension( (int)screenSize.getWidth(), (int)screenSize.getHeight()-200));
	        this.pack();
	        this.setLocationRelativeTo(null);
	        this.setVisible(true);
	    }
	 
	 private JPanel createMainPanel() {
		 JPanel panel = new JPanel(new BorderLayout());

		 JTabbedPane pane = new JTabbedPane(JTabbedPane.TOP);
		 pane.addTab("About", createAboutPanel());

		 JScrollPane licPane = createGPLLicensePanel();
		 JScrollPane depPane = createDependencyPanel();
		 
		 licPane.setPreferredSize(PREF_SIZE);
		 depPane.setPreferredSize(PREF_SIZE);

		 pane.addTab("License", licPane);
		 pane.addTab("Dependencies", depPane);
		 
		 panel.add(pane, BorderLayout.CENTER);	
		 return panel;
	 }
	 
	 private JTextArea createAboutPanel() {
		 JTextArea textBox = new JTextArea();
		 textBox.setFont(UIManager.getFont("Label.font"));
		 
		 String text = "Nuclear Morphology Analysis version "+Version.currentVersion()+Io.NEWLINE
				 + Io.NEWLINE
				 + "Help, tutorials, and the source code are available at: "+Io.NEWLINE
				 + SITE_URL;
		 
		 textBox.setText(text);
		 textBox.setEditable(false);
		 textBox.setLineWrap(false);
		 return textBox;
	 }
	 
	 private String readTextFile(String fileName) {
		 ClassLoader classLoader = ClassLoader.getSystemClassLoader();

		 URL urlToFile = classLoader.getResource(fileName);
		 String path = urlToFile.getPath();
		 path = path.replaceAll("%20", " "); // spaces converted to %20 in URL
		 LOGGER.fine("Path to license file: "+path);

		 File pathToLicenses = new File(path);
		 if(!pathToLicenses.exists())
			 LOGGER.fine("Cannot find "+pathToLicenses);

		 StringBuilder sb = new StringBuilder();
		 try (Stream<String> lines = Files.lines(pathToLicenses.toPath(), StandardCharsets.UTF_8)) {
			 lines.forEachOrdered(l->sb.append(l+Io.NEWLINE));
		 } catch(IOException e) {
			 LOGGER.fine("Cannot read license text");
			 sb.append("Unable to read license file");
		 }
		 return sb.toString();
	 }

	 private JScrollPane createGPLLicensePanel() {		 
		 JTextArea textBox = new JTextArea();
		 textBox.setFont(UIManager.getFont("Label.font"));

		 String fileName = "licenses/GNU-GPLv3.txt";

		 textBox.setText(readTextFile(fileName));
		 textBox.setEditable(false);
		 textBox.setLineWrap(true);
		 return new JScrollPane(textBox);
	 }
	 
	 
	 private JScrollPane createDependencyPanel() {		 
		 JTextArea textBox = new JTextArea();
		 textBox.setFont(UIManager.getFont("Label.font"));

		 String fileName = "licenses/DependencyLicenses.txt";
		 
		 textBox.setText(readTextFile(fileName));
		 textBox.setEditable(false);
		 textBox.setLineWrap(true);

		 return new JScrollPane(textBox);
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
			 LOGGER.warning( "Desktop doesn't support the browse action" );
			 return;
		 }
		 try {
			 URI uri = new URI(SITE_URL);
			 desktop.browse(uri);
		 } catch (IOException e1) {
			 LOGGER.warning(e1.getMessage());
		 } catch (URISyntaxException e1) {
			 LOGGER.warning("Unable to parse URI: "+e1.getMessage());
		 }
	 }
}
