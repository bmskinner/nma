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
package com.bmskinner.nma.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.main.MainDragAndDropTarget;
import com.bmskinner.nma.gui.tabs.DetailPanel;

/**
 * The log panel is where logging messages are displayed. It also holds progress
 * bars from actions. For historical reasons, it also houses the console and
 * contains the console code.
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class LogPanel extends DetailPanel implements ProgressBarAcceptor {

	private static final Logger LOGGER = Logger.getLogger(LogPanel.class.getName());

	private static final String SHOW_CONSOLE_ACTION = "ShowConsole";

	private JTextPane textArea = new JTextPane();

	private final Console console = new Console(this);

	private JPanel mainPanel; // messages and errors
	private JPanel progressPanel; // progress bars for analyses
	private SimpleAttributeSet attrs; // the styling attributes

	public LogPanel() {
		super();
		this.setLayout(new BorderLayout());
		this.mainPanel = createLogPanel();

		textArea.setDropTarget(new MainDragAndDropTarget());
		this.add(mainPanel, BorderLayout.CENTER);
	}

	@Override
	public String getPanelTitle() {
		return "Log panel";
	}

	/**
	 * Create the log panel for updates
	 * 
	 * @return a scrollable panel
	 */
	private JPanel createLogPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();

		createTextPane();
		scrollPane.setViewportView(textArea);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		panel.add(scrollPane, BorderLayout.CENTER);

		progressPanel = new JPanel();

		progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
		panel.add(progressPanel, BorderLayout.NORTH);
		panel.add(console, BorderLayout.SOUTH);

		this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0),
				SHOW_CONSOLE_ACTION);
		// Backup if F12 is mapped to something globally
		this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0),
				SHOW_CONSOLE_ACTION);

		this.getActionMap().put(SHOW_CONSOLE_ACTION, new ShowConsoleAction());
		return panel;
	}

	private void createTextPane() {
		textArea.setEditorKit(new WrapEditorKit());
		textArea.setEditable(false);
		textArea.setBackground(SystemColor.menu);
		Font font = new Font("Monospaced", Font.PLAIN, 13);

		// Set the wrapped line indent
		StyledDocument doc = textArea.getStyledDocument();
		attrs = new SimpleAttributeSet();
		StyleConstants.setFirstLineIndent(attrs, -70);
		StyleConstants.setLeftIndent(attrs, 70);
		StyleConstants.setFontFamily(attrs, font.getFamily());
		StyleConstants.setFontSize(attrs, font.getSize());
		StyleConstants.setForeground(attrs, Color.BLACK);
		StyleConstants.setBackground(attrs, SystemColor.menu);
		StyleConstants.setItalic(attrs, false);
		StyleConstants.setBold(attrs, false);

		doc.setParagraphAttributes(0, doc.getLength() + 1, attrs, true);
		doc.setCharacterAttributes(0, doc.getLength() + 1, attrs, true);

		DefaultCaret caret = (DefaultCaret) textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	}

	/**
	 * Print the given string
	 * 
	 * @param s
	 */
	public void print(String s) {
		StyledDocument doc = textArea.getStyledDocument();

		Runnable r = () -> {
			try {
				doc.insertString(doc.getLength(), s, attrs);
			} catch (BadLocationException e) {
				LOGGER.log(Level.SEVERE, "Error updating log panel", e);
			}
		};
		SwingUtilities.invokeLater(r);
	}

	/**
	 * Print the given string with a new line
	 * 
	 * @param s
	 */
	public void println(String s) {
		print(s + System.getProperty("line.separator"));
	}

	/**
	 * Clear the log window of all text
	 */
	public void clear() {
		textArea.setText(null);
	}

	/**
	 * Get all the progress bars attached to the log panel
	 * 
	 * @return
	 */
	@Override
	public List<JProgressBar> getProgressBars() {
		List<JProgressBar> result = new ArrayList<>();
		for (Component c : progressPanel.getComponents()) {
			if (c.getClass().isInstance(JProgressBar.class)) {
				result.add((JProgressBar) c);
			}

		}
		return result;
	}

	@Override
	public void addProgressBar(JProgressBar progressBar) {
		progressPanel.add(progressBar);
		revalidate();
		repaint();
	}

	@Override
	public void removeProgressBar(JProgressBar progressBar) {
		progressPanel.remove(progressBar);
		revalidate();
		repaint();
	}

	private class ShowConsoleAction extends AbstractAction {

		public ShowConsoleAction() {
			super("Show console");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			console.toggle();
		}
	}

	@Override
	public void update(List<IAnalysisDataset> list) {
		// Does nothing, no datasets are displayed.
		// Using DetailPanel only for signalling access
	}

	class WrapEditorKit extends StyledEditorKit {
		ViewFactory defaultFactory = new WrapColumnFactory();

		@Override
		public ViewFactory getViewFactory() {
			return defaultFactory;
		}

	}

	class WrapColumnFactory implements ViewFactory {
		@Override
		public View create(Element elem) {
			String kind = elem.getName();
			if (kind != null) {
				if (kind.equals(AbstractDocument.ContentElementName)) {
					return new WrapLabelView(elem);
				} else if (kind.equals(AbstractDocument.ParagraphElementName)) {
					return new ParagraphView(elem);
				} else if (kind.equals(AbstractDocument.SectionElementName)) {
					return new BoxView(elem, View.Y_AXIS);
				} else if (kind.equals(StyleConstants.ComponentElementName)) {
					return new ComponentView(elem);
				} else if (kind.equals(StyleConstants.IconElementName)) {
					return new IconView(elem);
				}
			}

			// default to text display
			return new LabelView(elem);
		}
	}

	class WrapLabelView extends LabelView {
		public WrapLabelView(Element elem) {
			super(elem);
		}

		@Override
		public float getMinimumSpan(int axis) {
			switch (axis) {
			case View.X_AXIS:
				return 0;
			case View.Y_AXIS:
				return super.getMinimumSpan(axis);
			default:
				throw new IllegalArgumentException("Invalid axis: " + axis);
			}
		}

	}
}
