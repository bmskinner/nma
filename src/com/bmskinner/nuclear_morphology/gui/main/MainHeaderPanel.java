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
package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.gui.actions.ImportDatasetAction;
import com.bmskinner.nuclear_morphology.gui.actions.NeutrophilAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.components.panels.MeasurementUnitSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.MainOptionsDialog;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;

@SuppressWarnings("serial")
public class MainHeaderPanel extends JPanel {
	
	private static final Logger LOGGER = Logger.getLogger(MainHeaderPanel.class.getName());

    private static final String NEW_ANALYSIS_LBL   = "New analysis";
    private static final String NEW_STANDARD_LBL   = "Fluorescent nuclei";
    private static final String NEW_NEUTROPHIL_LBL = "Neutrophils";

    private static final String LOAD_DATASET_LBL   = "Open dataset";
    private static final String SAVE_ALL_LBL       = "Save all";
    private static final String SAVE_WORKSPACE_LBL = "Save workspace";
    private static final String OPTIONS_LBL        = "Options";
    
    private static final String TASK_QUEUE_LBL    = "Task queue:";
    private static final String MEMORY_LBL        = "Memory:";

    private MainView mw;

    public MainHeaderPanel(MainView mw) {
    	
        this.mw = mw;
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        JPanel btnPanel = createHeaderButtonPanel();

        add(btnPanel, c);
        
        
        JPanel memPanel = createMemoryPanel();
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_END;
        c.weightx = 0.5;
        add(memPanel, c);
        
    }

    /**
     * Create the panel of primary buttons.
     */
    private JPanel createMemoryPanel() {
        JPanel memPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));        

        TaskListMonitor t = new TaskListMonitor();
        t.setBorder(BorderFactory.createBevelBorder(1));
        memPanel.add(new JLabel(TASK_QUEUE_LBL));
        memPanel.add(t);
        
        MemoryIndicator m = new MemoryIndicator();
        m.setBorder(BorderFactory.createBevelBorder(1));
        memPanel.add(new JLabel(MEMORY_LBL));
        memPanel.add(m);
        return memPanel;
    }

    /**
     * Create the panel of primary buttons
     */
    private JPanel createHeaderButtonPanel() {
        
        JPanel panel = new JPanel(new FlowLayout());       

        JButton btnNewAnalysis = new JButton(NEW_ANALYSIS_LBL);

        if (Version.currentVersion().isNewerThan(Version.v_1_13_4)) {

            final JPopupMenu popup = new JPopupMenu();

            popup.add(new JMenuItem(new AbstractAction(NEW_STANDARD_LBL) {
                public void actionPerformed(ActionEvent e) {
                    Runnable r = new NewAnalysisAction(mw.getProgressAcceptor(), mw.getEventHandler());
                    r.run();
                }
            }));
            popup.add(new JMenuItem(new AbstractAction(NEW_NEUTROPHIL_LBL) {
                public void actionPerformed(ActionEvent e) {
                    Runnable r = new NeutrophilAnalysisAction(mw.getProgressAcceptor(), mw.getEventHandler());
                    r.run();
                }
            }));

            btnNewAnalysis.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    popup.show(btnNewAnalysis, 0, btnNewAnalysis.getBounds().height);

                }
            });
        } else {
            btnNewAnalysis.addActionListener(e -> {
                Runnable r = new NewAnalysisAction(mw.getProgressAcceptor(), mw.getEventHandler());
                r.run();
            });
        }

        panel.add(btnNewAnalysis);

        // ---------------
        // load saved dataset button
        // ---------------

        JButton btnLoadSavedDataset = new JButton(LOAD_DATASET_LBL);

        btnLoadSavedDataset.addActionListener(e -> {
            Runnable r = new ImportDatasetAction(mw.getProgressAcceptor(), mw.getEventHandler());
            r.run();
        });

        panel.add(btnLoadSavedDataset);

        // ---------------
        // save button
        // ---------------

        JButton btnSavePopulation = new JButton(SAVE_ALL_LBL);
        btnSavePopulation.addActionListener(e -> {
            LOGGER.info("Saving root populations...");
            mw.getEventHandler().saveRootDatasets();
        });

        panel.add(btnSavePopulation);

        // ---------------
        // save workspace button
        // ---------------

        JButton btnSaveWorkspace = new JButton(SAVE_WORKSPACE_LBL);
        btnSaveWorkspace.addActionListener(e -> {
            mw.getEventHandler().eventReceived(
                    new SignalChangeEvent(this, SignalChangeEvent.EXPORT_WORKSPACE, this.getClass().getName()));
        });

        panel.add(btnSaveWorkspace);

        JButton optionsButton = new JButton(OPTIONS_LBL);
        optionsButton.addActionListener( e -> {
                    MainOptionsDialog dialog = new MainOptionsDialog(mw);
                    dialog.addInterfaceEventListener(mw.getEventHandler());
                });
        panel.add(optionsButton);

        MeasurementUnitSettingsPanel unitsPanel = new MeasurementUnitSettingsPanel();
        unitsPanel.addInterfaceEventListener(mw.getEventHandler());
        panel.add(unitsPanel);
        
        return panel;

    }

}
