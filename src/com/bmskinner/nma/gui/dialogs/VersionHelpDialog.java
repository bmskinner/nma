package com.bmskinner.nma.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.gui.main.MainView;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.logging.Loggable;

import ij.plugin.BrowserLauncher;

/**
 * A dialog that displays current version info and allows the user to check for
 * updates
 * 
 * @author bms41
 * @since 1.15.0
 *
 */
@SuppressWarnings("serial")
public class VersionHelpDialog extends SettingsDialog {

	private static final String LABEL_FONT = "Label.font";

	private static final Logger LOGGER = Logger.getLogger(VersionHelpDialog.class.getName());

	private static final String DIALOG_TITLE = "About";
	private static final String SITE_URL = "https://bitbucket.org/bmskinner/nuclear_morphology/wiki/Home/";
	private static final String VIST_WEBSITE_LBL = "Visit website";

	private static final Dimension PREF_SIZE = new Dimension(120, 300);

	public VersionHelpDialog(final MainView mw) {
		super((Frame) mw, false);

		this.setLayout(new BorderLayout());
		this.setTitle(DIALOG_TITLE);

		try {
			this.add(createMainPanel(), BorderLayout.CENTER);
			this.add(createFooter(), BorderLayout.SOUTH);
		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error creating help dialog", e);
		}
		this.setLocationRelativeTo(null);
		this.setMinimumSize(new Dimension(100, 70));

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setMaximumSize(
				new Dimension((int) screenSize.getWidth(), (int) screenSize.getHeight() - 200));
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

	private JPanel createMainPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		JTabbedPane pane = new JTabbedPane(SwingConstants.TOP);
		pane.addTab(DIALOG_TITLE, createAboutPanel());

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
		textBox.setFont(UIManager.getFont(LABEL_FONT));

		String text = "Nuclear Morphology Analysis version " + Version.currentVersion() + Io.NEWLINE
				+ Io.NEWLINE
				+ "Help, tutorials, and the source code are available at: " + Io.NEWLINE
				+ SITE_URL + Io.NEWLINE
				+ "(click the button below to open the website)" + Io.NEWLINE + Io.NEWLINE
				+ "A full guide to the software is included via Help > 'Open user guide'"
				+ Io.NEWLINE + Io.NEWLINE
				+ "If you use this in your research, please cite our papers!" + Io.NEWLINE
				+ Io.NEWLINE
				+ "Skinner et al. 2019: doi:10.1093/biolre/ioz013" + Io.NEWLINE
				+ "Skinner 2022: doi:10.21105/joss.04767";

		textBox.setText(text);
		textBox.setEditable(false);
		textBox.setLineWrap(false);
		return textBox;
	}

	private String readTextFile(String fileName) {
		// Cannot use a URI since it will break when packaged to exe
		InputStream in = getClass().getResourceAsStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		StringBuilder sb = new StringBuilder();
		try (Stream<String> lines = reader.lines()) {
			lines.forEachOrdered(l -> sb.append(l + Io.NEWLINE));
		} catch (Exception e) {
			LOGGER.fine("Cannot read license text");
			sb.append("Unable to read text file");
		}
		return sb.toString();
	}

	private JScrollPane createGPLLicensePanel() {
		JTextArea textBox = new JTextArea();
		textBox.setFont(UIManager.getFont(LABEL_FONT));

		String fileName = "/licenses/GNU-GPLv3.txt";

		textBox.setText(readTextFile(fileName));
		textBox.setEditable(false);
		textBox.setLineWrap(true);
		textBox.setCaretPosition(0);
		return new JScrollPane(textBox);
	}

	private JScrollPane createDependencyPanel() {
		JTextArea textBox = new JTextArea();
		textBox.setFont(UIManager.getFont(LABEL_FONT));

		String fileName = "/licenses/DependencyLicenses.txt";

		textBox.setText(readTextFile(fileName));
		textBox.setEditable(false);
		textBox.setLineWrap(true);
		textBox.setCaretPosition(0);
		return new JScrollPane(textBox);
	}

	@Override
	protected JPanel createFooter() {
		JPanel panel = new JPanel(new FlowLayout());

		JButton websiteBtn = new JButton(VIST_WEBSITE_LBL);

		websiteBtn.addActionListener(e -> browseToWebsite());
		panel.add(websiteBtn);
		return panel;
	}

	private void browseToWebsite() {
		Desktop desktop = Desktop.getDesktop();

		try {
			URI uri = new URI(SITE_URL);
			if (desktop.isSupported(Desktop.Action.BROWSE)) {
				desktop.browse(uri);
			} else {
				BrowserLauncher.openURL(uri.toString());
			}

		} catch (IOException e1) {
			LOGGER.warning(e1.getMessage());
		} catch (URISyntaxException e1) {
			LOGGER.warning("Unable to parse URI: " + e1.getMessage());
		}
	}
}
