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

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.gui.dialogs.MainOptionsDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.VersionHelpDialog;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEventHandler;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.UpdateChecker;

/**
 * Menu bar for the main window
 * @author Ben Skinner
 *
 */
public class MainWindowMenuBar extends JMenuBar  {
	
	private static final Logger LOGGER = Logger.getLogger(MainWindowMenuBar.class.getName());
	
	private static final String TASK_QUEUE_LBL = "Task queue:";
	private static final String MEMORY_LBL     = "Memory:";
	
	private static final String FILE_MENU_LBL          = "File";
	private static final String NEW_ANALYSIS_MENU_LBL  = "New analysis";
	private static final String NEW_ANALYSIS_CUSTOM_LBL = "Use custom detection options";
	private static final String NEW_ANALYSIS_CUSTOM_TOOLTIP  = "Configure the nucleus detection options yourself";
	private static final String NEW_ANALYSIS_SAVED_LBL  = "Use saved detection options";
	private static final String NEW_ANALYSIS_SAVED_TOOLTIP  = "Use options saved in a file for automatic nucleus detection";
	private static final String NEW_WORKSPACE_LBL  = "New workspace";
	private static final String OPEN_MENU_LBL  = "Open";
	private static final String OPEN_DATASET_LBL  = "Open dataset";
	private static final String OPEN_WORKSPACE_LBL = "Open workspace";
	
	private static final String VIEW_MENU_LBL     = "View";
	private static final String CHECK_FOR_UPDATES_ITEM_LBL = "Check for updates";
	private static final String OPEN_LOG_DIR_LBL = "Open config directory";
	private static final String ABOUT_ITEM_LBL = "About";
	private static final String HELP_MENU_LBL = "Help";
	private static final String TASK_MONITOR_ITEM_LBL = "Task monitor";
	private static final String FILL_CONSENSUS_ITEM_LBL = "Fill consensus";
	private static final String SWATCH_ITEM_LBL = "Swatch";
	private static final String SCALE_ITEM_LBL = "Scale";
	private static final String OPTIONS_LBL = "Options";
	private static final String EDIT_MENU_LBL = "Edit";
	private static final String EXIT_LBL = "Exit";
	private static final String SAVE_WORKSPACES_LBL = "Save workspaces";
	private static final String SAVE_DATASETS_LBL = "Save datasets";
	private static final String OPEN_CONFIG_FILE_LBL = "Open config file";
	
	final private SignalChangeEventHandler sh;
	final private InterfaceEventHandler ih;
	final private DatasetEventHandler dh;
	final private MainView mw;
	
	final private JPanel monitorPanel;
	
	private JMenu contextMenu;
	
	private MenuFactory fact = new MenuFactory();
	
	
	private class MenuFactory {
		public MenuFactory() {}
		
		public JMenuItem createSignalChangeMenuItem(String label, String action) {
			return createSignalChangeMenuItem(label, action, null);
		}
		
		public JMenuItem createSignalChangeMenuItem(String label, String action, @Nullable String tooltip) {
			JMenuItem item = new JMenuItem(label);
			item.addActionListener(e-> sh.fireSignalChangeEvent(action));
			if(tooltip!=null)
				item.setToolTipText(tooltip);
			return item;
		}
	}
	
	public MainWindowMenuBar(MainView mw) {
		super();
		this.mw = mw;
		sh = new SignalChangeEventHandler(this);
		sh.addListener(mw.getEventHandler());
		
		ih = new InterfaceEventHandler(this);
		ih.addListener(mw.getEventHandler());
		
		dh = new DatasetEventHandler(this);
		dh.addListener(mw.getEventHandler());
		
		add(createFileMenu());
        add(createEditMenu());
        add(createViewMenu());
        add(createHelpMenu());
        contextMenu = createDatasetMenu();

        add(Box.createGlue());
        monitorPanel = createMonitorPanel();
        add(monitorPanel);
	}
	
	private JPanel createMonitorPanel() {
		JPanel monitorPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		monitorPanel.add(new JLabel(TASK_QUEUE_LBL));
        monitorPanel.add(Box.createHorizontalStrut(5));
        TaskListMonitor t = new TaskListMonitor();
        t.setPreferredSize(new Dimension(100, t.getPreferredSize().height));
        t.setBorder(BorderFactory.createBevelBorder(1));
        monitorPanel.add(t);
        monitorPanel.add(Box.createHorizontalStrut(10));
        monitorPanel.add(new JLabel(MEMORY_LBL));
        monitorPanel.add(Box.createHorizontalStrut(5));
        MemoryIndicator m = new MemoryIndicator();
        m.setPreferredSize(new Dimension(100, m.getPreferredSize().height));
        m.setBorder(BorderFactory.createBevelBorder(1));
        monitorPanel.add(m);
        monitorPanel.setVisible(false);
        monitorPanel.setOpaque(false);
        return monitorPanel;
	}
		
	private JMenu createFileMenu() {
		JMenu menu = new JMenu(FILE_MENU_LBL);
		
		JMenu newMenu = new JMenu(NEW_ANALYSIS_MENU_LBL);
		
		JMenuItem i1 = new JMenuItem(NEW_ANALYSIS_CUSTOM_LBL);
		i1.setToolTipText(NEW_ANALYSIS_CUSTOM_TOOLTIP);
		i1.addActionListener(e-> new NewAnalysisAction(mw.getProgressAcceptor(), mw.getEventHandler()).run() );
		newMenu.add(i1);
		
		newMenu.add(fact.createSignalChangeMenuItem(NEW_ANALYSIS_SAVED_LBL, 
				SignalChangeEvent.IMPORT_WORKFLOW_PREFIX, NEW_ANALYSIS_SAVED_TOOLTIP));
		menu.add(newMenu);
		
		
		menu.add(fact.createSignalChangeMenuItem(NEW_WORKSPACE_LBL, SignalChangeEvent.NEW_WORKSPACE));
		
		
		JMenu openMenu = new JMenu(OPEN_MENU_LBL);
		
		openMenu.add(fact.createSignalChangeMenuItem(OPEN_DATASET_LBL, SignalChangeEvent.IMPORT_DATASET_PREFIX));
		openMenu.add(fact.createSignalChangeMenuItem(OPEN_WORKSPACE_LBL, SignalChangeEvent.IMPORT_WORKSPACE_PREFIX));
		menu.add(openMenu);
		
		menu.add(fact.createSignalChangeMenuItem(SAVE_DATASETS_LBL, SignalChangeEvent.SAVE_ALL_DATASETS));
		menu.add(fact.createSignalChangeMenuItem(SAVE_WORKSPACES_LBL, SignalChangeEvent.EXPORT_WORKSPACE));

		
		
		JMenuItem exit = new JMenuItem(EXIT_LBL);
		exit.addActionListener(e-> {
			for(WindowListener l : mw.getWindowListeners()) {
				if(l instanceof MainWindowCloseAdapter)
					l.windowClosing(new WindowEvent((Window) mw, WindowEvent.WINDOW_CLOSING));
			}
		});
		menu.add(exit);
		
		return menu;
	}
	
	private JMenu createEditMenu() {
		JMenu menu = new JMenu(EDIT_MENU_LBL);
		
		JMenuItem i1 = new JMenuItem(OPTIONS_LBL);
		i1.addActionListener( e -> {
            MainOptionsDialog dialog = new MainOptionsDialog(mw);
            dialog.addInterfaceEventListener(mw.getEventHandler());
        });
		menu.add(i1);
		
		JMenuItem configFileItem = new JMenuItem(OPEN_CONFIG_FILE_LBL);
		configFileItem.addActionListener(e->{
			try {
				Desktop.getDesktop().open(Io.getConfigFile());
			} catch (IOException e1) {
				LOGGER.log(Level.SEVERE, "Unable to open config file", e);
			}
		});
		
		menu.add(configFileItem);
		return menu;
	}
	
	private JMenu createViewMenu() {
		JMenu menu = new JMenu(VIEW_MENU_LBL);
		
		JMenu scaleMenu = new JMenu(SCALE_ITEM_LBL);
		
		ButtonGroup g = new ButtonGroup();
		for(MeasurementScale m : MeasurementScale.values()) {
			JMenuItem j = new JRadioButtonMenuItem(m.toString());
			g.add(j);
			if(m.equals(GlobalOptions.getInstance().getScale())) // default config file scale
				j.setSelected(true);
			j.addActionListener( e -> {
				GlobalOptions.getInstance().setScale(m);
				ih.fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
			});
			scaleMenu.add(j);
			
		}
		menu.add(scaleMenu);
		
		JMenu swatchMenu = new JMenu(SWATCH_ITEM_LBL);
		ButtonGroup swatchGroup = new ButtonGroup();
		for(ColourSwatch c : ColourSwatch.values()) {
			JMenuItem j = new JRadioButtonMenuItem(c.toString());
			swatchGroup.add(j);
			if(c.equals(GlobalOptions.getInstance().getSwatch())) // default config file scale
				j.setSelected(true);
			j.addActionListener( e -> {
				GlobalOptions.getInstance().setSwatch(c);;
				ih.fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
			});
			swatchMenu.add(j);
			
		}
		menu.add(swatchMenu);
		
		
		JCheckBoxMenuItem fillConsensusItem = new JCheckBoxMenuItem(FILL_CONSENSUS_ITEM_LBL, GlobalOptions.getInstance().isFillConsensus());
		fillConsensusItem.addActionListener( e -> {
			GlobalOptions.getInstance().setFillConsensus(fillConsensusItem.isSelected());
			ih.fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
		});
		menu.add(fillConsensusItem);
		
		JCheckBoxMenuItem monitorItem = new JCheckBoxMenuItem(TASK_MONITOR_ITEM_LBL, false);
		monitorItem.addActionListener( e -> monitorPanel.setVisible(!monitorPanel.isVisible()));
		menu.add(monitorItem);
		
		return menu;
	}
	
	private JMenu createHelpMenu() {
		JMenu menu = new JMenu(HELP_MENU_LBL);
		
		JMenuItem aboutItem = new JMenuItem(ABOUT_ITEM_LBL);
		aboutItem.addActionListener(e-> new VersionHelpDialog(mw));
		menu.add(aboutItem);
		
		
		JMenuItem checkItem = new JMenuItem(CHECK_FOR_UPDATES_ITEM_LBL);
		checkItem.addActionListener(e-> {
			Runnable r = () ->{
				Version v = UpdateChecker.fetchLatestVersion();
				if(v.isNewerThan(Version.currentVersion())) {
					JOptionPane.showMessageDialog(this, "A new version - "+v+" - is available!", "Update found!", JOptionPane.INFORMATION_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(this, "You have the latest version: "+Version.currentVersion(), "No updates", JOptionPane.INFORMATION_MESSAGE);
				}
			};
			ThreadManager.getInstance().submit(r);
		});
		menu.add(checkItem);
		
		
		JMenuItem logItem = new JMenuItem(OPEN_LOG_DIR_LBL);
		logItem.addActionListener(e-> {
			Runnable r = () ->{
				try {
					Desktop.getDesktop().open(Io.getConfigDir());
				} catch(IOException ex) {
					LOGGER.log(Level.SEVERE, "Unable to open folder", ex);
					JOptionPane.showMessageDialog(this, "Unable to open log folder: "+Io.getConfigDir().getAbsolutePath(), "Error opening log folder", JOptionPane.ERROR_MESSAGE);
				}
			};
			ThreadManager.getInstance().submit(r);
		});
		menu.add(logItem);
		
		
		return menu;
	}
	
	private JMenu createDatasetMenu() {
		return new JMenu("Dataset");
	}

}
