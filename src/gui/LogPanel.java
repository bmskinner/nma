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
import java.awt.Component;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.jfree.chart.JFreeChart;

import utility.Constants;
import charting.options.DefaultChartOptions;
import charting.options.DefaultTableOptions;
import analysis.AnalysisDataset;
import analysis.IAnalysisDataset;
import gui.InterfaceEvent.InterfaceMethod;
import gui.actions.NewAnalysisAction;
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
		commandMap.put("refresh", InterfaceMethod.UPDATE_PANELS);
		commandMap.put("nucleus history", InterfaceMethod.DUMP_LOG_INFO);
		commandMap.put("info", InterfaceMethod.INFO);
		commandMap.put("kill", InterfaceMethod.KILL_ALL_TASKS);
		
	}
	
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
	@SuppressWarnings("serial")
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
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				
		
		panel.add(scrollPane, BorderLayout.CENTER);

		progressPanel = new JPanel();
//		progressPanel = new ProgressBarPanel();
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
		
		// Need an extra drop target for file opening  as well as in the main window
		DropTarget dropTarget = makePanelDropTarget();
		textArea.setDropTarget(dropTarget);

		return panel;
	}
	
	private DropTarget makePanelDropTarget(){
		DropTarget d = new DropTarget(){

			@Override
			public synchronized void drop(DropTargetDropEvent dtde) {


				try {
					fine("Drop event heard");
					dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
					Transferable t = dtde.getTransferable();

					Set<File> fileList = new HashSet<File>();

					// Check that what was provided is a list
					if(t.getTransferData(DataFlavor.javaFileListFlavor) instanceof List<?>){

						// Check that what is in the list is files
						List<?> tempList = (List<?>) t.getTransferData(DataFlavor.javaFileListFlavor);
						for(Object o : tempList){
							fine("Checking dropped object");

							if(o instanceof File){
								fine("Object is a file");
								fileList.add( (File) o);
							}
						}

						// Open the files - we open *.nmd files and analyse directories

						for(File f : fileList){
							fine("Checking dropped file");
							if(f.getName().endsWith(Constants.SAVE_FILE_EXTENSION)){
								finer("Opening file "+f.getAbsolutePath());

								fireSignalChangeEvent("Open|"+f.getAbsolutePath());


							} else {
								finer("File is not nmd, ignoring");
							}

							if(f.isDirectory()){
								// Pass to new analysis
								fireSignalChangeEvent("New|"+f.getAbsolutePath());

							}

						}
					}


				} catch (UnsupportedFlavorException e) {
					error("Error in DnD", e);
				} catch (IOException e) {
					error("IO error in DnD", e);
				}

			}

		};
		return d;
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

		Runnable r = () -> {
			try {
				doc.insertString(doc.getLength(), s, attrs );
			} catch (BadLocationException e) {
				logIJ(s);
				logIJ("Requested insert at "+e.offsetRequested()+" in document of "+doc.getLength());
				logToImageJ("Error appending to log panel", e);
			}
		};
		SwingUtilities.invokeLater(r);

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
	}
	
	/**
	 * Get all the progress bars attached to the log panel
	 * @return
	 */
	public List<JProgressBar> getProgressBars(){
		List<JProgressBar> result = new ArrayList<JProgressBar>();
		for(Component c : progressPanel.getComponents()){
			if(c.getClass().isInstance(JProgressBar.class)){
				result.add((JProgressBar) c);
			}
			
		}
		return result;
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
			finest("Button pressed: "+e.getActionCommand());
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

		if(e.getSource().equals(console)){
			log(console.getText());
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
								
				case "clear":{
					clear();
					break;
				}
				
				default: {
					log(Level.INFO, "Command not recognised");
					break;
				}
			}
						
		}		
	}

	@Override
	public void update(List<IAnalysisDataset> list) {
		//Does nothing, no datasets are displayed. 
		// Using DetailPanel only for signalling access
		
	}

	
	@Override
	protected JFreeChart createPanelChartType(DefaultChartOptions options) throws Exception {
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(DefaultTableOptions options) throws Exception{
		return null;
	}
	
	/*
	 * 
	 * Copied from https://tips4java.wordpress.com/2008/10/15/limit-lines-in-document/
	 *  A class to control the maximum number of lines to be stored in a Document
	 *
	 *  Excess lines can be removed from the start or end of the Document
	 *  depending on your requirement.
	 *
	 *  a) if you append text to the Document, then you would want to remove lines
	 *     from the start.
	 *  b) if you insert text at the beginning of the Document, then you would
	 *     want to remove lines from the end.
	 */
	public class LimitLinesDocumentListener implements DocumentListener	{
		private int maximumLines;
		private boolean isRemoveFromStart;

		/*
		 *  Specify the number of lines to be stored in the Document.
		 *  Extra lines will be removed from the start of the Document.
		 */
		public LimitLinesDocumentListener(int maximumLines)
		{
			this(maximumLines, true);
		}

		/*
		 *  Specify the number of lines to be stored in the Document.
		 *  Extra lines will be removed from the start or end of the Document,
		 *  depending on the boolean value specified.
		 */
		public LimitLinesDocumentListener(int maximumLines, boolean isRemoveFromStart)
		{
			setLimitLines(maximumLines);
			this.isRemoveFromStart = isRemoveFromStart;
		}

		/*
		 *  Return the maximum number of lines to be stored in the Document
		 */
		public int getLimitLines()
		{
			return maximumLines;
		}

		/*
		 *  Set the maximum number of lines to be stored in the Document
		 */
		public void setLimitLines(int maximumLines)
		{
			if (maximumLines < 1)
			{
				String message = "Maximum lines must be greater than 0";
				throw new IllegalArgumentException(message);
			}

			this.maximumLines = maximumLines;
		}

		//  Handle insertion of new text into the Document

		public void insertUpdate(final DocumentEvent e)
		{
			//  Changes to the Document can not be done within the listener
			//  so we need to add the processing to the end of the EDT

			SwingUtilities.invokeLater( new Runnable()
			{
				public void run()
				{
					removeLines(e);
				}
			});
		}

		public void removeUpdate(DocumentEvent e) {}
		public void changedUpdate(DocumentEvent e) {}

		/*
		 *  Remove lines from the Document when necessary
		 */
		private void removeLines(DocumentEvent e)
		{
			//  The root Element of the Document will tell us the total number
			//  of line in the Document.

			Document document = e.getDocument();
			Element root = document.getDefaultRootElement();
			
			while (root.getElementCount() > maximumLines)
			{
				if (isRemoveFromStart)
				{
					removeFromStart(document, root);
				}
				else
				{
					removeFromEnd(document, root);
				}
			}
		}

		/*
		 *  Remove lines from the start of the Document
		 */
		private void removeFromStart(Document document, Element root)
		{
			Element line = root.getElement(0);
			int end = line.getEndOffset();

			try {
				
				document.remove(0, end);
			} catch(BadLocationException ble) {
				
				logToImageJ("Error removing lines", ble);
			}
		}

		/*
		 *  Remove lines from the end of the Document
		 */
		private void removeFromEnd(Document document, Element root)
		{
			//  We use start minus 1 to make sure we remove the newline
			//  character of the previous line

			Element line = root.getElement(root.getElementCount() - 1);
			int start = line.getStartOffset();
			int end = line.getEndOffset();

			try {
				document.remove(start - 1, end - start);
			} catch(BadLocationException ble) {
				logToImageJ("Error removing lines", ble);
			}
		}
	}

}
