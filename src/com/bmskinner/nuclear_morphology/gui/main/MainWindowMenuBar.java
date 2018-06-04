package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEventHandler;
import com.bmskinner.nuclear_morphology.gui.ContextEnabled;
import com.bmskinner.nuclear_morphology.gui.DatasetEventHandler;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.InterfaceEventHandler;
import com.bmskinner.nuclear_morphology.gui.actions.NeutrophilAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.PopulationImportAction;
import com.bmskinner.nuclear_morphology.gui.dialogs.MainOptionsDialog;
import com.bmskinner.nuclear_morphology.main.GlobalOptions;

public class MainWindowMenuBar extends JMenuBar implements ContextEnabled {
	
	private SignalChangeEventHandler sh;
	private InterfaceEventHandler ih;
	private DatasetEventHandler dh;
	private MainView mw;
	
	private JMenu contextMenu;
	
	public MainWindowMenuBar(MainView mw) {
		super();
		this.mw = mw;
		sh = new SignalChangeEventHandler(this);
		sh.addSignalChangeListener(mw.getEventHandler());
		
		ih = new InterfaceEventHandler(this);
		ih.addInterfaceEventListener(mw.getEventHandler());
		
		dh = new DatasetEventHandler(this);
		dh.addDatasetEventListener(mw.getEventHandler());
		
		add(createFileMenu());
        add(createEditMenu());
        add(createViewMenu());
        contextMenu = createDatasetMenu();

        add(Box.createGlue());
        add(new JLabel("Task queue:"));
        add(Box.createHorizontalStrut(10));
        TaskListMonitor t = new TaskListMonitor();
        t.setPreferredSize(new Dimension(100, t.getPreferredSize().height));
        t.setBorder(BorderFactory.createBevelBorder(1));
        add(t);
        add(Box.createHorizontalStrut(50));
        add(new JLabel("Memory:"));
        add(Box.createHorizontalStrut(10));
        MemoryIndicator m = new MemoryIndicator();
        m.setPreferredSize(new Dimension(100, m.getPreferredSize().height));
        m.setBorder(BorderFactory.createBevelBorder(1));
        add(m);
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
		
		JMenuItem openDataset = new JMenuItem("Open dataset");
		openDataset.addActionListener(e-> sh.fireSignalChangeEvent(SignalChangeEvent.IMPORT_DATASET_PREFIX) );
		menu.add(openDataset);
		
		JMenuItem openWorkspace = new JMenuItem("Open workspace");
		openWorkspace.addActionListener(e-> sh.fireSignalChangeEvent(SignalChangeEvent.IMPORT_WORKSPACE_PREFIX));
		menu.add(openWorkspace);
		
		JMenuItem save = new JMenuItem("Save all");
		save.addActionListener(e-> sh.fireSignalChangeEvent(SignalChangeEvent.SAVE_ALL_DATASETS));
		menu.add(save);
		
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
