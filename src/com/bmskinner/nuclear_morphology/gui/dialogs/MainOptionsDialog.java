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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.main.MainView;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelHandler;
import com.bmskinner.nuclear_morphology.logging.Loggable;

@SuppressWarnings("serial")
public class MainOptionsDialog extends SettingsDialog implements ActionListener {

    private static final String DIALOG_TITLE = "Options";
    
    private static final Logger LOGGER = Logger.getLogger(MainOptionsDialog.class.getName());

    private JComboBox<Level>        programLevelBox;
    private JComboBox<Level>        consoleLevelBox;
    
    private JCheckBox               refoldOverrideBox;
    private JCheckBox               antiAliasBox;
    private JCheckBox               convertDatasetsBox; // should opened
                                                        // datasets be converted
                                                        // to the latest version
    
    private JCheckBox               showDebugInterfaceBox;

    public MainOptionsDialog(final MainView mw) {
        super((Frame) mw, false);

        // this.mw = mw;
        this.setLayout(new BorderLayout());
        this.setTitle(DIALOG_TITLE);

        // The date and time the program was built
        // this.add( new JLabel(Constants.BUILD), BorderLayout.NORTH);

        this.add(createMainPanel(), BorderLayout.CENTER);
        this.add(createFooter(), BorderLayout.SOUTH);

        this.setLocationRelativeTo(null);
        this.setMinimumSize(new Dimension(100, 70));
        this.pack();
        this.setVisible(true);

    }

    /**
     * Create the panel footer, with just a close button buttons
     * 
     * @return
     */
    @Override
    protected JPanel createFooter() {

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("Close");
        okButton.addActionListener(e -> {
            setVisible(false);
            dispose();
        });
        panel.add(okButton);

        return panel;
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        List<JLabel> labels = new ArrayList<>();
        List<Component> fields = new ArrayList<>();

        JLabel logLabel = new JLabel("Program log level");
        Level[] levelArray = { Level.INFO, Loggable.STACK, Level.FINE, Level.FINER, Level.FINEST };
        programLevelBox = new JComboBox<>(levelArray);
        
        Handler[] handlers = LOGGER.getHandlers();
        for(Handler h : handlers)
        	if(h instanceof LogPanelHandler)
        		programLevelBox.setSelectedItem(h.getLevel());

        programLevelBox.addActionListener(this);

        labels.add(logLabel);
        fields.add(programLevelBox);
        
        
        JLabel consoleLogLabel = new JLabel("Console log level");
        Level[] consoleLevelArray = { Level.INFO, Loggable.STACK, Level.FINE, Level.FINER, Level.FINEST };
        consoleLevelBox = new JComboBox<>(consoleLevelArray);
        for(Handler h : handlers)
        	if(h instanceof ConsoleHandler)
        		consoleLevelBox.setSelectedItem(h.getLevel());
        consoleLevelBox.addActionListener(this);
        if(System.console()!=null) { // if the gui is launched with not console, don't make the option
        	labels.add(consoleLogLabel);
        	fields.add(consoleLevelBox);
        }

        
        JLabel overrideRefoldLabel = new JLabel("Refold override");
        refoldOverrideBox = new JCheckBox((String) null, GlobalOptions.getInstance().isOverrideRefold());
        refoldOverrideBox.addActionListener(this);

        labels.add(overrideRefoldLabel);
        fields.add(refoldOverrideBox);

        JLabel antiAliasLabel = new JLabel("Antialiasing");
        antiAliasBox = new JCheckBox((String) null, GlobalOptions.getInstance().isAntiAlias());
        antiAliasBox.addActionListener(this);

        labels.add(antiAliasLabel);
        fields.add(antiAliasBox);

        JLabel convertDatasetsLabel = new JLabel("Convert datasets");
        convertDatasetsBox = new JCheckBox((String) null, GlobalOptions.getInstance().isConvertDatasets());
        convertDatasetsBox.addActionListener(this);

        labels.add(convertDatasetsLabel);
        fields.add(convertDatasetsBox);
        
        JLabel showDebugInterfacLabel = new JLabel("Show debug UI");
        showDebugInterfaceBox = new JCheckBox((String) null, GlobalOptions.getInstance().getBoolean(GlobalOptions.IS_DEBUG_INTERFACE_KEY));
        showDebugInterfaceBox.addActionListener(this);
        
        labels.add(showDebugInterfacLabel);
        fields.add(showDebugInterfaceBox);
        
        this.addLabelTextRows(labels, fields, layout, panel);
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
    	
    	Handler[] handlers = LOGGER.getHandlers();
    	    	
        Level programLevel = (Level) programLevelBox.getSelectedItem();
        Level consoleLevel = (Level) consoleLevelBox.getSelectedItem();
        for(Handler h : handlers) {
        	if(h instanceof ConsoleHandler)
        		h.setLevel(consoleLevel);
        	if(h instanceof LogPanelHandler)
        		h.setLevel(programLevel);
        }


        boolean antiAlias = antiAliasBox.isSelected();
        if (GlobalOptions.getInstance().isAntiAlias() != antiAlias) {
            GlobalOptions.getInstance().setAntiAlias(antiAlias);
            fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
        }

        boolean convertDatasets = convertDatasetsBox.isSelected();
        if (GlobalOptions.getInstance().isConvertDatasets() != convertDatasets) {
            GlobalOptions.getInstance().setConvertDatasets(convertDatasets);
        }
        
        boolean overrideRefold = refoldOverrideBox.isSelected();
        if (GlobalOptions.getInstance().isOverrideRefold() != overrideRefold) {
            GlobalOptions.getInstance().setOverrideRefold(overrideRefold);
        }
        
        boolean isShowDebug = showDebugInterfaceBox.isSelected();
        GlobalOptions.getInstance().setBoolean(GlobalOptions.IS_DEBUG_INTERFACE_KEY, isShowDebug);;


    }

}
