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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Collection;

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

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ContextEnabled;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.gui.dialogs.MainOptionsDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.VersionHelpDialog;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEventHandler;
import com.bmskinner.nuclear_morphology.io.UpdateChecker;

public class MainWindowMenuBar extends JMenuBar  { //implements ContextEnabled
	
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
		monitorPanel.add(new JLabel("Task queue:"));
        monitorPanel.add(Box.createHorizontalStrut(5));
        TaskListMonitor t = new TaskListMonitor();
        t.setPreferredSize(new Dimension(100, t.getPreferredSize().height));
        t.setBorder(BorderFactory.createBevelBorder(1));
        monitorPanel.add(t);
        monitorPanel.add(Box.createHorizontalStrut(10));
        monitorPanel.add(new JLabel("Memory:"));
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
		JMenu menu = new JMenu("File");
		
		JMenu newMenu = new JMenu("New analysis");
		JMenuItem i1 = new JMenuItem("Use custom detection options");
		i1.setToolTipText("Configure the nucleus detection options yourself");
		i1.addActionListener(e-> new NewAnalysisAction(mw.getProgressAcceptor(), mw.getEventHandler()).run() );
		newMenu.add(i1);
		newMenu.add(fact.createSignalChangeMenuItem("Use saved detection options", 
				SignalChangeEvent.IMPORT_WORKFLOW_PREFIX, "Use options saved in a file for automatic nucleus detection"));
		menu.add(newMenu);
		
		
		menu.add(fact.createSignalChangeMenuItem("New workspace", SignalChangeEvent.NEW_WORKSPACE));
		
		
		JMenu openMenu = new JMenu("Open");
		
		openMenu.add(fact.createSignalChangeMenuItem("Open dataset", SignalChangeEvent.IMPORT_DATASET_PREFIX));
		openMenu.add(fact.createSignalChangeMenuItem("Open workspace", SignalChangeEvent.IMPORT_WORKSPACE_PREFIX));
		menu.add(openMenu);
		
		menu.add(fact.createSignalChangeMenuItem("Save datasets", SignalChangeEvent.SAVE_ALL_DATASETS));
		menu.add(fact.createSignalChangeMenuItem("Save workspaces", SignalChangeEvent.EXPORT_WORKSPACE));

		
		
		JMenuItem exit = new JMenuItem("Exit");
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
		JMenu menu = new JMenu("Edit");
		
		JMenuItem i1 = new JMenuItem("Options");
		i1.addActionListener( e -> {
            MainOptionsDialog dialog = new MainOptionsDialog(mw);
            dialog.addInterfaceEventListener(mw.getEventHandler());
        });
		menu.add(i1);
		return menu;
	}
	
	private JMenu createViewMenu() {
		JMenu menu = new JMenu("View");
		
		JMenu scaleMenu = new JMenu("Scale");
		
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
		
		JMenu swatchMenu = new JMenu("Swatch");
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
		
		
		JCheckBoxMenuItem fillConsensusItem = new JCheckBoxMenuItem("Fill consensus", GlobalOptions.getInstance().isFillConsensus());
		fillConsensusItem.addActionListener( e -> {
			GlobalOptions.getInstance().setFillConsensus(fillConsensusItem.isSelected());
			ih.fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
		});
		menu.add(fillConsensusItem);
		
		JCheckBoxMenuItem monitorItem = new JCheckBoxMenuItem("Task monitor", false);
		monitorItem.addActionListener( e -> monitorPanel.setVisible(!monitorPanel.isVisible()));
		menu.add(monitorItem);
		
		return menu;
	}
	
	private JMenu createHelpMenu() {
		JMenu menu = new JMenu("Help");
		
		JMenuItem aboutItem = new JMenuItem("About");
		aboutItem.addActionListener(e-> new VersionHelpDialog(mw));
		menu.add(aboutItem);
		
		
		JMenuItem checkItem = new JMenuItem("Check for updates");
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
		
		
		return menu;
	}
	
	
	
	private JMenu createWorkspaceMenu() {
		JMenu menu = new JMenu("Workspace");
		
		return menu;
	}
	
	private JMenu createClusterGroupMenu() {
		JMenu menu = new JMenu("Cluster");
		
		return menu;
	}
	
	private JMenu createDatasetMenu() {
		JMenu menu = new JMenu("Dataset");
		
		return menu;
	}

}
