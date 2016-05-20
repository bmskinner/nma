/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
import java.awt.Color;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.jfree.chart.JFreeChart;

import utility.Constants;
import charting.options.ChartOptions;
import charting.options.TableOptions;
import analysis.AnalysisDataset;
import gui.InterfaceEvent.InterfaceMethod;
import gui.tabs.DetailPanel;

public class LogPanel extends DetailPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private JTextPane  textArea = new JTextPane();
	
	private JPanel 					logPanel;				// messages and errors
	private JPanel 					progressPanel;			// progress bars for analyses
	
	private JTextField				console = new JTextField();
	
	private SimpleAttributeSet attrs; // the styling attributes
	
	private Map<String, InterfaceMethod> commandMap = new HashMap<String, InterfaceMethod>();
	
	{
		commandMap.put("list datasets", InterfaceMethod.LIST_DATASETS);
		commandMap.put("list selected", InterfaceMethod.LIST_SELECTED_DATASETS);
		commandMap.put("unfuck", InterfaceMethod.RESEGMENT_SELECTED_DATASET);
		commandMap.put("recache charts", InterfaceMethod.RECACHE_CHARTS);
		commandMap.put("clear", InterfaceMethod.CLEAR_LOG_WINDOW);
		commandMap.put("refresh", InterfaceMethod.UPDATE_PANELS);
		commandMap.put("dump info", InterfaceMethod.DUMP_LOG_INFO);
		
		
	}
	
	private Map<Integer, AnalysisDataset> datasetMap = new HashMap<Integer, AnalysisDataset>();
	
//	private Logger programLogger;

	public LogPanel() {
		super();
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
		
		Font font = new Font("Monospaced", Font.PLAIN, 13);

		
		// Set the wrapped line indent
		StyledDocument doc = textArea.getStyledDocument();
		
		attrs = new SimpleAttributeSet();
        StyleConstants.setFirstLineIndent(attrs, -70);
        StyleConstants.setLeftIndent(     attrs, 70);
        StyleConstants.setFontFamily(     attrs, font.getFamily());
        StyleConstants.setFontSize(       attrs, font.getSize());
        StyleConstants.setForeground(     attrs, Color.BLACK);
        StyleConstants.setBackground(     attrs, SystemColor.menu);
        StyleConstants.setItalic(         attrs, false);
        StyleConstants.setBold(           attrs, false);

        doc.setParagraphAttributes(0, doc.getLength()+1, attrs, true);
        doc.setCharacterAttributes(0, doc.getLength()+1, attrs, true);
        
        
		DefaultCaret caret = (DefaultCaret)textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		textArea.setEditable(false);
		textArea.setBackground(SystemColor.menu);
		
		scrollPane.setViewportView(textArea);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				
		
		panel.add(scrollPane, BorderLayout.CENTER);

		progressPanel = new JPanel();
		progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
		panel.add(progressPanel, BorderLayout.NORTH);
		
		this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0),
				"ShowConsole"); // grave accent new Character('\u0065')
		this.getActionMap().put("ShowConsole",
				new ShowConsoleAction());
		
		console.setFont(font);
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
		print(s+"\n");
	}
	
	public void print(String s){
		StyledDocument doc = textArea.getStyledDocument();

		try {
			
			doc.insertString(doc.getLength(), s, attrs );
			
		} catch (BadLocationException e) {
			logToImageJ("Error appending to log panel", e);
		}

	}
	
	public void clear(){
		textArea.setText(null);
	}
	
	/**
	 * Continuous log - do not append a newline
	 * @param s the string to log
	 */
	public void logc(String s){
		print(s);
//		StyledDocument doc = (StyledDocument) textArea.getDocument();
//		SimpleAttributeSet keyWord = new SimpleAttributeSet();
//		try {
//			doc.insertString(doc.getLength(), s, keyWord );
//		} catch (BadLocationException e) {
//			error("Error appending", e);
//		}
//		textArea.append(s);
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
			log(Level.FINEST, "Button pressed: "+e.getActionCommand());
			if(console.isVisible()){
				console.setVisible(false);
			} else {
				console.setText(null);
				console.setVisible(true);
				console.grabFocus();
				console.requestFocus();
				console.requestFocusInWindow();
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
//		log(Level.FINEST, "Log panel detected action: "+e.getActionCommand());
//		log(Level.FINEST, "Log panel detected action: "+e.getSource().toString());
		if(e.getSource().equals(console)){
//			log(Level.FINEST, "Entering text at console");
			log(Level.INFO, console.getText());
			runCommand(console.getText());
			console.setText("");
		}
		
	}
	
	private void runCommand(String command){
		
		if(commandMap.containsKey(command)){
			fireInterfaceEvent(commandMap.get(command));
		} else {
			
			switch(command){
			
				case "help": {
					log(Level.INFO, "Available commands: ");
					for(String key : commandMap.keySet()){
						InterfaceMethod im = commandMap.get(key);
						log(Level.INFO, " "+key+" - "+im.toString());
					}
					log(Level.INFO, " build - show the version info ");
					break;
				}
				
				case "build":{
					log(Level.INFO, "This version built at:");
					log(Level.INFO, Constants.BUILD);
					break;
				}
				
				default: {
					log(Level.INFO, "Command not recognised");
					break;
				}
			}
						
//			if(command.equals("help")){
//				log(Level.INFO, "Available commands: ");
//				for(String key : commandMap.keySet()){
//					log(Level.INFO, " "+key);
//				}
//				
//			} else {
//				log(Level.INFO, "Command not recognised");
//			}
		}		
	}

	@Override
	public void update(List<AnalysisDataset> list) {
		//Does nothing, no datasets are displayed. 
		// Using DetailPanel only for signalling access
		
	}

	@Override
	protected void updateSingle() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void updateMultiple() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void updateNull() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}

}
