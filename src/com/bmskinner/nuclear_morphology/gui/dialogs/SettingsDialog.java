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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;

/**
 * Contains methods for laying out panels in settings dialog options
 */
@SuppressWarnings("serial")
public abstract class SettingsDialog extends JDialog {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    protected boolean readyToRun = false;
    private final List<EventListener> interfaceListeners = new ArrayList<>();

    protected static final String EMPTY_STRING = "";
    protected static final String OK_LBL       = "OK";
    protected static final String CANCEL_LBL   = "Cancel";

    protected String[] channelOptionStrings = { "Greyscale", "Red", "Green", "Blue" };

    /**
     * Constructor for generic dialogs not attached to a frame
     * 
     * @param programLogger
     */
    public SettingsDialog() {
        this.setLocationRelativeTo(null);
    }
    
    /**
     * Create a modal dialog with no parent frame
     * @param modal
     */
    public SettingsDialog(boolean modal) {
    	this();
    	this.setModal(modal);
    }

    /**
     * Constructor for dialogs attached to a frame
     * 
     * @param owner the frame
     * @param modal is the dialog modal
     */
    public SettingsDialog(Frame owner, boolean modal) {
        super(owner, modal);
        LOGGER.fine("Making settings dialog");
        this.setLocationRelativeTo(null);
    }

    public SettingsDialog(Dialog owner, boolean modal) {
        super(owner, modal);
        this.setLocationRelativeTo(null);
    }
    
    /**
     * Create an empty header panel
     * @return
     */
    protected JPanel createHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    /**
     * Create a panel footer, with OK and Cancel option buttons
     * 
     * @return
     */
    protected JPanel createFooter() {

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton(OK_LBL);
        okButton.addActionListener(e -> {
            readyToRun = true;
            setVisible(false);
        });

        panel.add(okButton);

        JButton cancelButton = new JButton(CANCEL_LBL);
        cancelButton.addActionListener(e -> {
            readyToRun = false;
            dispose();
        });

        panel.add(cancelButton);
        return panel;
    }

    /**
     * Add components to a container via a list
     * 
     * @param labels the list of labels
     * @param fields the list of components
     * @param gridbag the layout
     * @param container the container to add the labels and fields to
     */
    protected void addLabelTextRows(List<JLabel> labels, List<Component> fields, GridBagLayout gridbag,
            Container container) {
        JLabel[] labelArray = labels.toArray(new JLabel[0]);
        Component[] fieldArray = fields.toArray(new Component[0]);
        addLabelTextRows(labelArray, fieldArray, gridbag, container);
    }

    /**
     * Add components to a container via arrays
     * 
     * @param labels the list of labels
     * @param fields the list of components
     * @param gridbag the layout
     * @param container the container to add the labels and fields to
     */
    protected void addLabelTextRows(JLabel[] labels, Component[] fields, GridBagLayout gridbag, Container container) {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        int numLabels = labels.length;

        for (int i = 0; i < numLabels; i++) {
            c.gridwidth = 1; // next-to-last
            c.fill = GridBagConstraints.NONE; // reset to default
            c.weightx = 0.0; // reset to default
            container.add(labels[i], c);

            Dimension minSize = new Dimension(10, 5);
            Dimension prefSize = new Dimension(10, 5);
            Dimension maxSize = new Dimension(Short.MAX_VALUE, 5);
            c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last
            c.fill = GridBagConstraints.NONE; // reset to default
            c.weightx = 0.0; // reset to default
            container.add(new Box.Filler(minSize, prefSize, maxSize), c);

            c.gridwidth = GridBagConstraints.REMAINDER; // end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            container.add(fields[i], c);
        }
    }

    /**
     * Check if this dialog was cancelled or if the subsequent analysis can be
     * run
     * 
     * @return
     */
    public boolean isReadyToRun() {
        return this.readyToRun;
    }

    public synchronized void addInterfaceEventListener(EventListener l) {
        interfaceListeners.add(l);
    }

    public synchronized void removeInterfaceEventListener(EventListener l) {
        interfaceListeners.remove(l);
    }

    protected synchronized void fireInterfaceEvent(InterfaceMethod method) {

        InterfaceEvent event = new InterfaceEvent(this, method, this.getClass().getSimpleName());
        Iterator<EventListener> iterator = interfaceListeners.iterator();
        while (iterator.hasNext()) {
            iterator.next().eventReceived(event);
        }
    }
}
