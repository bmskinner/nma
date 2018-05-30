package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEventHandler;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.InterfaceEventHandler;
import com.bmskinner.nuclear_morphology.gui.actions.NeutrophilAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.PopulationImportAction;
import com.bmskinner.nuclear_morphology.gui.dialogs.MainOptionsDialog;
import com.bmskinner.nuclear_morphology.main.GlobalOptions;

public class MainWindowMenuBar extends JMenuBar {
	
	private SignalChangeEventHandler sh;
	private InterfaceEventHandler ih;
	private MainWindow mw;
	
	public MainWindowMenuBar(MainWindow mw) {
		super();
		this.mw = mw;
		sh = new SignalChangeEventHandler(this);
		sh.addSignalChangeListener(mw.getEventHandler());
		
		ih = new InterfaceEventHandler(this);
		ih.addInterfaceEventListener(mw.getEventHandler());
		
		add(createFileMenu());
        add(createEditMenu());
        add(createViewMenu());
	}
	
	private JMenu createFileMenu() {
		JMenu menu = new JMenu("File");
		
		JMenu newMenu = new JMenu("New analysis");
		JMenuItem i1 = new JMenuItem("Fluorescent nuclei");
		i1.addActionListener(e-> new NewAnalysisAction(mw.getLogPanel(), mw.getEventHandler()).run() );
		JMenuItem i2 = new JMenuItem("H&E granulocytes");
		i2.addActionListener(e-> new NeutrophilAnalysisAction(mw.getLogPanel(), mw.getEventHandler()).run() );
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
					l.windowClosing(new WindowEvent(mw, WindowEvent.WINDOW_CLOSING));
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

}
