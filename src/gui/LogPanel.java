/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultCaret;

import analysis.AnalysisDataset;
import components.nuclei.Nucleus;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import gui.tabs.DetailPanel;
import ij.io.DirectoryChooser;

public class LogPanel extends DetailPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private JTextArea textArea = new JTextArea();
	
	private JPanel 					logPanel;				// messages and errors
	private JPanel 					progressPanel;			// progress bars for analyses
	
	private JTextField				console = new JTextField();
	
//	private Logger programLogger;

	public LogPanel(Logger programLogger) {
		super(programLogger);
		this.setLayout(new BorderLayout());
		this.logPanel = createLogPanel();
		this.add(logPanel, BorderLayout.CENTER);
	}
	
	/**
	 * Create the log panel for updates
	 * @return a scrollable panel
	 */
	private JPanel createLogPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		textArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
		DefaultCaret caret = (DefaultCaret)textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		scrollPane.setViewportView(textArea);
		textArea.setBackground(SystemColor.menu);
		textArea.setEditable(false);
		textArea.setRows(9);
		textArea.setColumns(40);
		
		
		panel.add(scrollPane, BorderLayout.CENTER);

		progressPanel = new JPanel();
		progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
		panel.add(progressPanel, BorderLayout.NORTH);
		
		this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0),
				"ShowConsole"); // grave accent new Character('\u0065')
		this.getActionMap().put("ShowConsole",
				new ShowConsoleAction());
		
		panel.add(console, BorderLayout.SOUTH);
		console.setVisible(false);
		console.addActionListener(this);

		return panel;
	}
	
	/**
	 * Standard log - append a newline
	 * @param s the string to log
	 */
	public void log(String s){
		logc(s+"\n");
	}
	
	public void print(String s){
		textArea.append(s);
	}
	
	/**
	 * Continuous log - do not append a newline
	 * @param s the string to log
	 */
	public void logc(String s){
		textArea.append(s);
	}
	
	public void addProgressBar(JProgressBar progressBar){
		progressPanel.add(progressBar);
	}
	
	public void removeProgressBar(JProgressBar progressBar){
		progressPanel.remove(progressBar);
	}
	
	@SuppressWarnings("serial")
	class ShowConsoleAction extends AbstractAction {

		public ShowConsoleAction() {
			super("Show console");
		}

		public void actionPerformed(ActionEvent e) {
			programLogger.log(Level.FINEST, "Button pressed: "+e.getActionCommand());
			if(console.isVisible()){
				console.setVisible(false);
			} else {
				console.setText("");
				console.setVisible(true);
			}
			revalidate();
			repaint();
		}
	}

	/*
	 * Listener for the console
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
//		programLogger.log(Level.FINEST, "Log panel detected action: "+e.getActionCommand());
//		programLogger.log(Level.FINEST, "Log panel detected action: "+e.getSource().toString());
		if(e.getSource().equals(console)){
//			programLogger.log(Level.FINEST, "Entering text at console");
			this.log(console.getText());
			runCommand(console.getText());
			console.setText("");
		}
		
	}
	
	private void runCommand(String command){
		
		if(command.equals("list datasets")){
			fireInterfaceEvent(InterfaceMethod.LIST_DATASETS);
		}
		
		if(command.equals("list selected")){
			fireInterfaceEvent(InterfaceMethod.LIST_SELECTED_DATASETS);
		}
		
		if(command.equals("unfuck")){
			fireInterfaceEvent(InterfaceMethod.RESEGMENT_SELECTED_DATASET);
		}
		
	}

	@Override
	public void update(List<AnalysisDataset> list) {
		//Does nothing, no datasets are displayed. 
		// Using DetailPanel only for signalling access
		
	}

}
