package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.ContextEnabled;
import com.bmskinner.nuclear_morphology.gui.actions.NeutrophilAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.PopulationImportAction;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.gui.dialogs.MainOptionsDialog;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;

public class MainWindowMenuBar extends JMenuBar implements ContextEnabled {
	
	final private SignalChangeEventHandler sh;
	final private InterfaceEventHandler ih;
	final private DatasetEventHandler dh;
	final private MainView mw;
	
	final private JPanel monitorPanel;
	
	private JMenu contextMenu;
	
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
		JMenuItem i1 = new JMenuItem("Fluorescent nuclei");
		i1.addActionListener(e-> new NewAnalysisAction(mw.getProgressAcceptor(), mw.getEventHandler()).run() );
		JMenuItem i2 = new JMenuItem("H&E granulocytes");
		i2.addActionListener(e-> new NeutrophilAnalysisAction(mw.getProgressAcceptor(), mw.getEventHandler()).run() );
		newMenu.add(i1);
		newMenu.add(i2);
		menu.add(newMenu);
		
		JMenuItem newWorkspace = new JMenuItem("New workspace");
		newWorkspace.addActionListener(e-> sh.fireSignalChangeEvent(SignalChangeEvent.NEW_WORKSPACE));
		menu.add(newWorkspace);
		
		JMenuItem openDataset = new JMenuItem("Open dataset");
		openDataset.addActionListener(e-> sh.fireSignalChangeEvent(SignalChangeEvent.IMPORT_DATASET_PREFIX) );
		menu.add(openDataset);
		
		JMenuItem openWorkspace = new JMenuItem("Open workspace");
		openWorkspace.addActionListener(e-> sh.fireSignalChangeEvent(SignalChangeEvent.IMPORT_WORKSPACE_PREFIX));
		menu.add(openWorkspace);
		
		

		JMenuItem save = new JMenuItem("Save datasets");
		save.addActionListener(e-> sh.fireSignalChangeEvent(SignalChangeEvent.SAVE_ALL_DATASETS));
		menu.add(save);
		
		JMenuItem saveWorkspaces = new JMenuItem("Save workspaces");
		saveWorkspaces.addActionListener(e-> sh.fireSignalChangeEvent(SignalChangeEvent.EXPORT_WORKSPACE));
		menu.add(saveWorkspaces);
		
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

	@Override
	public void updateSelectionContext(int nObjects) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateSelectionContext(ActiveTypeContext type) {
		switch(type) {
			case DATASET: contextMenu = createDatasetMenu(); break;
			case CLUSTER_GROUP: contextMenu = createClusterGroupMenu(); break;
			case WORKSPACE: contextMenu = createWorkspaceMenu(); break;
		}
		
	}

}
