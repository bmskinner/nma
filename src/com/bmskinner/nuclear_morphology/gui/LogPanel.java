/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui;

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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.io.Importer;
import com.bmskinner.nuclear_morphology.main.DatasetListManager;
import com.bmskinner.nuclear_morphology.main.Nuclear_Morphology_Analysis;

/**
 * The log panel is where logging messages are displayed. It also holds progress
 * bars from actions,
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class LogPanel extends DetailPanel implements ActionListener {

    private static final String SHOW_CONSOLE_ACTION = "ShowConsole";
    private static final String NEXT_HISTORY_ACTION = "Next";
    private static final String PREV_HISTORY_ACTION = "Prev";

    private static final String CHECK_CMD = "check";
    private static final String HELP_CMD  = "help";
    private static final String CLEAR_CMD = "clear";
    private static final String THROW_CMD = "throw";
    private static final String LIST_CMD  = "list";
    private static final String KILL_CMD  = "kill";

    private static final Map<String, Runnable> LOCAL_CMDS = new HashMap<>();

    private JTextPane textArea = new JTextPane();

    private JPanel logPanel;      // messages and errors
    private JPanel progressPanel; // progress bars for analyses

    private JTextField console = new JTextField();

    private SimpleAttributeSet attrs; // the styling attributes

    private Map<String, InterfaceMethod> commandMap = new HashMap<String, InterfaceMethod>();

    int          historyIndex = -1;
    List<String> history      = new LinkedList<String>();

    {
        commandMap.put("list selected", InterfaceMethod.LIST_SELECTED_DATASETS);
        commandMap.put("recache charts", InterfaceMethod.RECACHE_CHARTS);
        commandMap.put("refresh", InterfaceMethod.UPDATE_PANELS);
        commandMap.put("nucleus history", InterfaceMethod.DUMP_LOG_INFO);
        commandMap.put("info", InterfaceMethod.INFO);
    }

    private void listDatasets() {
        int i = 0;
        for (IAnalysisDataset d : DatasetListManager.getInstance().getAllDatasets()) {
            log(i + "\t" + d.getName());
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

    @Override
    public void setChartsAndTablesLoading() {
    }

    public LogPanel() {
        super();
        this.setLayout(new BorderLayout());
        this.logPanel = createLogPanel();
        makeCommandList();
        this.add(logPanel, BorderLayout.CENTER);
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

        textArea.setEditable(false);
        textArea.setBackground(SystemColor.menu);

        scrollPane.setViewportView(textArea);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.add(scrollPane, BorderLayout.CENTER);

        progressPanel = new JPanel();

        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
        panel.add(progressPanel, BorderLayout.NORTH);

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0),
                SHOW_CONSOLE_ACTION); // grave accent new Character('\u0065')
        this.getActionMap().put(SHOW_CONSOLE_ACTION, new ShowConsoleAction());

        console.setFont(font);
        panel.add(console, BorderLayout.SOUTH);
        console.setVisible(false);
        console.addActionListener(this);

        console.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                PREV_HISTORY_ACTION);

        console.getActionMap().put(PREV_HISTORY_ACTION, new PrevHistoryAction());

        console.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                NEXT_HISTORY_ACTION);
        console.getActionMap().put(NEXT_HISTORY_ACTION, new NextHistoryAction());

        // Need an extra drop target for file opening as well as in the main
        // window
        DropTarget dropTarget = makePanelDropTarget();
        textArea.setDropTarget(dropTarget);

        return panel;
    }

    /**
     * Make the list of local commands to run
     */
    private void makeCommandList() {
        LOCAL_CMDS.put(CHECK_CMD, () -> {
            validateDatasets();
        });
        LOCAL_CMDS.put(HELP_CMD, () -> {
            log("Available commands: ");
            for (String key : commandMap.keySet()) {
                InterfaceMethod im = commandMap.get(key);
                log(" " + key + " - " + im.toString());
            }
            log(" build - show the version info ");
            log(" check - validate the open root datasets");
            log(" list  - list the open root datasets");
        });
        LOCAL_CMDS.put(CLEAR_CMD, () -> {
            clear();
        });
        LOCAL_CMDS.put(THROW_CMD, () -> {
            log("Throwing exception");
            try {
                throw new IllegalArgumentException("Throwing an exception");
            } catch (Exception e) {
                error("Caught expected exception", e);
            }
        });
        LOCAL_CMDS.put(LIST_CMD, () -> {
            listDatasets();
        });
        LOCAL_CMDS.put(KILL_CMD, () -> {
            killAllTasks();
        });
    }

    private DropTarget makePanelDropTarget() {
        DropTarget d = new DropTarget() {

            @Override
            public synchronized void drop(DropTargetDropEvent dtde) {

                try {
                    fine("Drop event heard");
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    Transferable t = dtde.getTransferable();

                    Set<File> fileList = new HashSet<File>();

                    // Check that what was provided is a list
                    if (t.getTransferData(DataFlavor.javaFileListFlavor) instanceof List<?>) {

                        // Check that what is in the list is files
                        List<?> tempList = (List<?>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        for (Object o : tempList) {
                            fine("Checking dropped object");

                            if (o instanceof File) {
                                fine("Object is a file");
                                fileList.add((File) o);
                            }
                        }

                        // Open the files - we open *.nmd files and analyse
                        // directories

                        for (File f : fileList) {
                            fine("Checking dropped file");
                            if (f.getName().endsWith(Importer.SAVE_FILE_EXTENSION)) {
                                finer("Opening file " + f.getAbsolutePath());

                                getSignalChangeEventHandler().fireSignalChangeEvent("Open|" + f.getAbsolutePath());

                            }
                            
                            if (f.getName().endsWith(Importer.WRK_FILE_EXTENSION)) {
                                fine("File is wrk");
                                getSignalChangeEventHandler().fireSignalChangeEvent("Wrk|" + f.getAbsolutePath());

                            }

                            if (f.isDirectory()) {
                                // Pass to new analysis
                                getSignalChangeEventHandler().fireSignalChangeEvent("New|" + f.getAbsolutePath());

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
     * Set the text of the console
     * 
     * @param s
     */
    private void setConsoleText(String s) {
        console.setText(s);
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
    public List<JProgressBar> getProgressBars() {
        List<JProgressBar> result = new ArrayList<JProgressBar>();
        for (Component c : progressPanel.getComponents()) {
            if (c.getClass().isInstance(JProgressBar.class)) {
                result.add((JProgressBar) c);
            }

        }
        return result;
    }

    public void addProgressBar(JProgressBar progressBar) {
        progressPanel.add(progressBar);
    }

    public void removeProgressBar(JProgressBar progressBar) {
        progressPanel.remove(progressBar);
    }

    private class ShowConsoleAction extends AbstractAction {

        public ShowConsoleAction() {
            super("Show console");
        }

        public void actionPerformed(ActionEvent e) {
            finest("Button pressed: " + e.getActionCommand());
            if (console.isVisible()) {
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

    private class PrevHistoryAction extends AbstractAction {

        public PrevHistoryAction() {
            super("Prev");
        }

        public void actionPerformed(ActionEvent e) {

            if (history.isEmpty()) {
                return;
            }

            if (historyIndex == history.size()) {
                return;
            }

            historyIndex++;
            console.setText(history.get(historyIndex));

        }
    }

    private class NextHistoryAction extends AbstractAction {

        public NextHistoryAction() {
            super("Next");
        }

        public void actionPerformed(ActionEvent e) {
            if (history.isEmpty()) {
                return;
            }
            if (historyIndex < 0) {
                return;
            }
            if (historyIndex == 0) {
                console.setText("");
                return;
            }

            historyIndex--;
            console.setText(history.get(historyIndex));

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
            // historyIndex=0;
            runCommand(console.getText());

            console.setText("");
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

    /**
     * Run the given command from the console
     * 
     * @param command
     */
    private void runCommand(String command) {
    	if (commandMap.containsKey(command)) {
            getInterfaceEventHandler().fireInterfaceEvent(commandMap.get(command));
        } else {

            if (LOCAL_CMDS.containsKey(command)) {
                LOCAL_CMDS.get(command).run();
            } else {
                log("Command not recognised");
            }

        }
    }

    @Override
    public void update(List<IAnalysisDataset> list) {
        // Does nothing, no datasets are displayed.
        // Using DetailPanel only for signalling access

    }

    /*
     * 
     * Copied from
     * https://tips4java.wordpress.com/2008/10/15/limit-lines-in-document/ A
     * class to control the maximum number of lines to be stored in a Document
     *
     * Excess lines can be removed from the start or end of the Document
     * depending on your requirement.
     *
     * a) if you append text to the Document, then you would want to remove
     * lines from the start. b) if you insert text at the beginning of the
     * Document, then you would want to remove lines from the end.
     */
    public class LimitLinesDocumentListener implements DocumentListener {
        private int     maximumLines;
        private boolean isRemoveFromStart;

        /*
         * Specify the number of lines to be stored in the Document. Extra lines
         * will be removed from the start of the Document.
         */
        public LimitLinesDocumentListener(int maximumLines) {
            this(maximumLines, true);
        }

        /*
         * Specify the number of lines to be stored in the Document. Extra lines
         * will be removed from the start or end of the Document, depending on
         * the boolean value specified.
         */
        public LimitLinesDocumentListener(int maximumLines, boolean isRemoveFromStart) {
            setLimitLines(maximumLines);
            this.isRemoveFromStart = isRemoveFromStart;
        }

        /*
         * Return the maximum number of lines to be stored in the Document
         */
        public int getLimitLines() {
            return maximumLines;
        }

        /*
         * Set the maximum number of lines to be stored in the Document
         */
        public void setLimitLines(int maximumLines) {
            if (maximumLines < 1) {
                String message = "Maximum lines must be greater than 0";
                throw new IllegalArgumentException(message);
            }

            this.maximumLines = maximumLines;
        }

        // Handle insertion of new text into the Document

        public void insertUpdate(final DocumentEvent e) {
            // Changes to the Document can not be done within the listener
            // so we need to add the processing to the end of the EDT

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    removeLines(e);
                }
            });
        }

        public void removeUpdate(DocumentEvent e) {
        }

        public void changedUpdate(DocumentEvent e) {
        }

        /*
         * Remove lines from the Document when necessary
         */
        private void removeLines(DocumentEvent e) {
            // The root Element of the Document will tell us the total number
            // of line in the Document.

            Document document = e.getDocument();
            Element root = document.getDefaultRootElement();

            while (root.getElementCount() > maximumLines) {
                if (isRemoveFromStart) {
                    removeFromStart(document, root);
                } else {
                    removeFromEnd(document, root);
                }
            }
        }

        /*
         * Remove lines from the start of the Document
         */
        private void removeFromStart(Document document, Element root) {
            Element line = root.getElement(0);
            int end = line.getEndOffset();

            try {

                document.remove(0, end);
            } catch (BadLocationException ble) {

                logToImageJ("Error removing lines", ble);
            }
        }

        /*
         * Remove lines from the end of the Document
         */
        private void removeFromEnd(Document document, Element root) {
            // We use start minus 1 to make sure we remove the newline
            // character of the previous line

            Element line = root.getElement(root.getElementCount() - 1);
            int start = line.getStartOffset();
            int end = line.getEndOffset();

            try {
                document.remove(start - 1, end - start);
            } catch (BadLocationException ble) {
                logToImageJ("Error removing lines", ble);
            }
        }
    }

}
