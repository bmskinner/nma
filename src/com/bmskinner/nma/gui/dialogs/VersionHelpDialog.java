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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.gui.main.MainView;
import com.bmskinner.nma.io.Io;

import ij.plugin.BrowserLauncher;

/**
 * A dialog that displays current version info and allows the user to check for
 * updates
 * 
 * @author Ben Skinner
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
		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Error creating help dialog", e);
		}
		this.setLocationRelativeTo(null);
		this.setMinimumSize(new Dimension(100, 70));

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setMaximumSize(
				new Dimension((int) screenSize.getWidth(), (int) screenSize.getHeight() - 200));
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

	private JPanel createMainPanel() {
		final JPanel panel = new JPanel(new BorderLayout());

		final JTabbedPane pane = new JTabbedPane(SwingConstants.TOP);
		pane.addTab(DIALOG_TITLE, createAboutPanel());

		final JScrollPane licPane = createGPLLicensePanel();
		final JScrollPane depPane = createDependencyPanel();

		licPane.setPreferredSize(PREF_SIZE);
		depPane.setPreferredSize(PREF_SIZE);

		pane.addTab("License", licPane);
		pane.addTab("Dependencies", depPane);

		panel.add(pane, BorderLayout.CENTER);
		return panel;
	}

	private JEditorPane createAboutPanel() {

		final JEditorPane textBox = new JEditorPane("text/html", "");

		final String text = """
				 <b>Nuclear Morphology Analysis version %s</b><p>

				 Help, tutorials, and the source code are available at:<br>
				 <a href=%s>%s</a><br>
				 (click the button below to open this link in your web browser)
				 <p>
				 A full guide to the software is included via Help > 'Open user guide'
				 <p>
				 If you use this in your research, please cite our papers!
				 <p>
				 <b>Morphology analysis:</b><br>
				 Skinner <i>et al.</i> (2019) <i>Biology of Reproduction</i>, 100(5), 1250-1260<br>
				 doi:10.1093/biolre/ioz013<p>

				 Skinner (2022) <i>Journal of Open Source Software</i>, 7(79), 4767<br>
				 doi:10.21105/joss.04767<p>

				 <b>Signal warping:</b><br>
				 Skinner <i>et al.</i> (2019) <i>Genes</i>, 10(2), 109<br>
				  doi:10.3390/genes10020109<br>

				""".formatted(Version.currentVersion(), SITE_URL, SITE_URL);
		textBox.setText(text);
		textBox.setEditable(false);
		return textBox;
	}

	private String readTextFile(String fileName) {
		// Cannot use a URI since it will break when packaged to exe
		final InputStream in = getClass().getResourceAsStream(fileName);
		final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		final StringBuilder sb = new StringBuilder();
		try (Stream<String> lines = reader.lines()) {
			lines.forEachOrdered(l -> sb.append(l + Io.NEWLINE));
		} catch (final Exception e) {
			LOGGER.fine("Cannot read license text");
			sb.append("Unable to read text file");
		}
		return sb.toString();
	}

	private JScrollPane createGPLLicensePanel() {
		final JTextArea textBox = new JTextArea();
		textBox.setFont(UIManager.getFont(LABEL_FONT));

		final String fileName = "/licenses/GNU-GPLv3.txt";

		textBox.setText(readTextFile(fileName));
		textBox.setEditable(false);
		textBox.setLineWrap(true);
		textBox.setCaretPosition(0);
		return new JScrollPane(textBox);
	}

	private JScrollPane createDependencyPanel() {
		final JTextArea textBox = new JTextArea();
		textBox.setFont(UIManager.getFont(LABEL_FONT));

		final String fileName = "/licenses/DependencyLicenses.txt";

		textBox.setText(readTextFile(fileName));
		textBox.setEditable(false);
		textBox.setLineWrap(true);
		textBox.setCaretPosition(0);
		return new JScrollPane(textBox);
	}

	@Override
	protected JPanel createFooter() {
		final JPanel panel = new JPanel(new FlowLayout());

		final JButton websiteBtn = new JButton(VIST_WEBSITE_LBL);

		websiteBtn.addActionListener(e -> browseToWebsite());
		panel.add(websiteBtn);
		return panel;
	}

	private void browseToWebsite() {
		final Desktop desktop = Desktop.getDesktop();

		try {
			final URI uri = new URI(SITE_URL);
			if (desktop.isSupported(Desktop.Action.BROWSE)) {
				desktop.browse(uri);
			} else {
				BrowserLauncher.openURL(uri.toString());
			}

		} catch (final IOException e1) {
			LOGGER.warning(e1.getMessage());
		} catch (final URISyntaxException e1) {
			LOGGER.warning("Unable to parse URI: " + e1.getMessage());
		}
	}
}
