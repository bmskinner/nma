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
package com.bmskinner.nuclear_morphology.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.main.MainDragAndDropTarget;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

/**
 * The log panel is where logging messages are displayed. It also holds progress
 * bars from actions. For historical reasons, it also houses the console and contains
 * the console code.
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class LogPanel extends DetailPanel implements ProgressBarAcceptor {

    private static final String SHOW_CONSOLE_ACTION = "ShowConsole";
    
    private final EventHandler eh;

    private JTextPane textArea = new JTextPane();
    
    private final Console console = new Console();

    private JPanel logPanel;      // messages and errors
    private JPanel progressPanel; // progress bars for analyses
    private SimpleAttributeSet attrs; // the styling attributes

    public LogPanel(@NonNull InputSupplier context, @NonNull EventHandler eh) {
    	super(context);
    	this.eh = eh;
    	this.setLayout(new BorderLayout());
    	this.logPanel = createLogPanel();
    	
    	addDatasetEventListener(eh);
    	addInterfaceEventListener(eh);
    	addSignalChangeListener(eh);
    	textArea.setDropTarget(new MainDragAndDropTarget(eh));
    	this.add(logPanel, BorderLayout.CENTER);
    }
    
    @Override
    public String getPanelTitle(){
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

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0),
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
    
    private void listDatasets() {
        int i = 0;
        for (IAnalysisDataset d : DatasetListManager.getInstance().getAllDatasets()) {
            String type = d.getCollection().isReal() ? "Real" : "Virtual";
            log(i + "\t" + d.getName()+"\t"+type);
            i++;
        }
    }

    private void killAllTasks() {

        log("Threads running in the JVM:");
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread t : threadSet) {
            log("Thread " + t.getId() + ": " + t.getState());
            t.interrupt();
        }

    }
    
    private void listTasks() {
    	log(ThreadManager.getInstance().toString());
    }

   

    /**
     * Print the given string
     * @param s
     */
    public void print(String s) {
        StyledDocument doc = textArea.getStyledDocument();

        Runnable r = () -> {
            try {
                doc.insertString(doc.getLength(), s, attrs);
            } catch (BadLocationException e) {
                logIJ(s);
                logIJ("Requested insert at " + e.offsetRequested() + " in document of " + doc.getLength());
                logToImageJ("Error appending to log panel", e);
            }
        };
        SwingUtilities.invokeLater(r);

    }
    
    /**
     * Print the given string with a new line
     * @param s
     */
    public void println(String s) {
    	print(s+System.getProperty("line.separator"));
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
        List<JProgressBar> result = new ArrayList<JProgressBar>();
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

    

    private void validateDatasets() {

        DatasetValidator v = new DatasetValidator();
        for (IAnalysisDataset d : DatasetListManager.getInstance().getRootDatasets()) {
            log("Validating " + d.getName() + "...");
            if (!v.validate(d)) {
                for (String s : v.getErrors()) {
                    warn(s);
                }
            } else {
                log("Dataset OK");
            }

        }

    }

    @Override
    public void update(List<IAnalysisDataset> list) {
        // Does nothing, no datasets are displayed.
        // Using DetailPanel only for signalling access

    }
    
    class WrapEditorKit extends StyledEditorKit {
        ViewFactory defaultFactory=new WrapColumnFactory();
        public ViewFactory getViewFactory() {
            return defaultFactory;
        }
 
    }
 
    class WrapColumnFactory implements ViewFactory {
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
    
    /**
     * Allow typed commands for functions that are not ready to expose through the GUI 
     * @author ben
     *
     */
    private class Console extends JPanel implements ActionListener {
    	 private JTextField console = new JTextField();
    	 private Map<String, InterfaceMethod> commandMap = new HashMap<String, InterfaceMethod>();

    	private int historyIndex = -1;
    	private List<String> history = new LinkedList<>();
    	
        private static final String NEXT_HISTORY_ACTION = "Next";
        private static final String PREV_HISTORY_ACTION = "Prev";

        private static final String HIST_CMD  = "history";
        private static final String CHECK_CMD = "check";
        private static final String HELP_CMD  = "help";
        private static final String CLEAR_CMD = "clear";
        private static final String THROW_CMD = "throw";
        private static final String LIST_CMD  = "list";
        private static final String KILL_CMD  = "kill";
        private static final String REPAIR_CMD  = "unfuck";
        private static final String TASKS_CMD  = "tasks";
        private static final String HASH_CMD  = "check hash";
        private static final String EXPORT_CMD = "export xml";

        private final Map<String, Runnable> runnableCommands = new HashMap<>();

    	{
    		commandMap.put("list selected", InterfaceMethod.LIST_SELECTED_DATASETS);
    		commandMap.put("recache charts", InterfaceMethod.RECACHE_CHARTS);
    		commandMap.put("refresh", InterfaceMethod.UPDATE_PANELS);
    		commandMap.put("nucleus history", InterfaceMethod.DUMP_LOG_INFO);
    		commandMap.put("info", InterfaceMethod.INFO);
    	}
    	
    	public Console() {
    		setLayout(new BorderLayout());
    		makeCommandList();
    		Font font = new Font("Monospaced", Font.PLAIN, 13);
    		console.setFont(font);
            add(console, BorderLayout.CENTER);
            setVisible(false);
            console.addActionListener(this);

            console.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                    PREV_HISTORY_ACTION);

            console.getActionMap().put(PREV_HISTORY_ACTION, new PrevHistoryAction());

            console.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                    NEXT_HISTORY_ACTION);
            console.getActionMap().put(NEXT_HISTORY_ACTION, new NextHistoryAction());
    	}
    	
    	/**
    	 * Toggle the visibilty of the console
    	 */
    	public void toggle() {
    		if (isVisible()) {
                setVisible(false);
            } else {
            	setVisible(true);
            	console.setText(null);
                console.grabFocus();
                console.requestFocus();
                console.requestFocusInWindow();
            }
            revalidate();
            repaint();
    	}
    	
    	 /**
         * Make the list of local commands to run
         */
        private void makeCommandList() {
        	runnableCommands.put(HIST_CMD, () -> {
                log("History: ");
                for (String s : history)
                    log("\t"+s);
            });
        	
        	
        	runnableCommands.put(HASH_CMD, ()->{
        		Set<IAnalysisDataset> datasets = DatasetListManager.getInstance().getAllDatasets();
        		
        		for(IAnalysisDataset d : datasets)
        			log(d.getName()+": "+d.hashCode());
        		
        	});
        	
            
            runnableCommands.put(HELP_CMD, () -> {
                log("Available commands: ");
                for (String key : commandMap.keySet()) {
                    InterfaceMethod im = commandMap.get(key);
                    log(" " + key + " - " + im.toString());
                }
                log(" check - validate the open root datasets");
                log(" list  - list the open root datasets");
                log(" export xml - export the selected dataset in XML format");
                log(" tasks - list the current task list");
                log(" "+HASH_CMD+" - print the hashes of the selected datasets");
            });
           
            runnableCommands.put(THROW_CMD, () -> {
                log("Throwing exception");
                try {
                    throw new IllegalArgumentException("Throwing an exception");
                } catch (Exception e) {
                    error("Caught expected exception", e);
                }
            });
            
            runnableCommands.put(CHECK_CMD,  () -> validateDatasets());
            runnableCommands.put(CLEAR_CMD,  () -> clear());
            runnableCommands.put(LIST_CMD,   () -> listDatasets());
            runnableCommands.put(KILL_CMD,   () -> killAllTasks());
            runnableCommands.put(TASKS_CMD,  () -> listTasks());
            runnableCommands.put(REPAIR_CMD, () -> getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFPAIR_SEGMENTATION, DatasetListManager.getInstance().getSelectedDatasets()));
            runnableCommands.put(EXPORT_CMD, () -> getSignalChangeEventHandler().fireSignalChangeEvent(SignalChangeEvent.EXPORT_XML_DATASET));
        }
    	
        /**
         * Run the given command from the console
         * 
         * @param command
         */
        private void runCommand(String command) {
        	if (commandMap.containsKey(command)) {
                getInterfaceEventHandler().fire(InterfaceEvent.of(this, commandMap.get(command)));
            } else {

                if (runnableCommands.containsKey(command)) {
                    runnableCommands.get(command).run();
                } else {
                    log("Command not recognised");
                }

            }
        }
    	
    	/*
         * Listener for the console
         */
        @Override
        public void actionPerformed(ActionEvent e) {

            if (e.getSource().equals(console)) {
                log(console.getText());
                history.add(console.getText());
                historyIndex=history.size();
                runCommand(console.getText());
                console.setText("");
            }
        }
    	
    	private class PrevHistoryAction extends AbstractAction {

            public PrevHistoryAction() {
                super("Prev");
            }

            @Override
    		public void actionPerformed(ActionEvent e) {
            	if (history.isEmpty())
                    return;
                if (historyIndex > -1) {
                	historyIndex--;
                }
                if (historyIndex == -1) {
                    console.setText("");
                    return;
                }
                console.setText(history.get(historyIndex));
            }
        }

        private class NextHistoryAction extends AbstractAction {

            public NextHistoryAction() {
                super("Next");
            }

            @Override
    		public void actionPerformed(ActionEvent e) {
            	if (history.isEmpty()) {
            		historyIndex=0;
                    return;
            	}
                if (historyIndex >= history.size()-1)
                    return;
                historyIndex++;
                console.setText(history.get(historyIndex));

            }
        }
    }
}
