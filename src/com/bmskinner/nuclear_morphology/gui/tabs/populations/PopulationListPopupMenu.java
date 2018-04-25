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


package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.SignalChangeListener;

@SuppressWarnings("serial")
public class PopulationListPopupMenu extends JPopupMenu {

    public static final String SOURCE_COMPONENT = "PopupMenu";

    private JMenuItem changeScaleItem;
    private JMenuItem mergeMenuItem;
    private JMenuItem curateMenuItem;
    private JMenuItem deleteMenuItem;
    private JMenuItem booleanMenuItem;
    private JMenuItem extractMenuItem;
    
    private JMenu exportSubMenu;
    private JMenuItem exportStatsMenuItem;
    private JMenuItem exportSignalsItem;
    private JMenuItem exportShellsItem;
    private JMenuItem saveCellsMenuItem;
    
    private JMenuItem saveMenuItem;

    private JMenuItem relocateMenuItem = new JMenuItem(new AbstractAction("Relocate cells") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            fireSignalChangeEvent("RelocateCellsAction");
        }
    });

   

    private JMenuItem moveUpMenuItem;
    private JMenuItem moveDownMenuItem;

    private JMenuItem replaceFolderMenuItem = new JMenuItem(new AbstractAction("Change folder") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            fireSignalChangeEvent("ChangeNucleusFolderAction");
        }
    });
    
    
    private JMenu addSubMenu;
    private JMenuItem addNuclearSignalMenuItem;

    private JMenuItem fishRemappinglMenuItem;

    private List<Object> listeners = new ArrayList<Object>();

    public PopulationListPopupMenu() {

        super("Popup");
        createButtons();

        this.add(moveUpMenuItem);
        this.add(moveDownMenuItem);

        this.addSeparator();

        this.add(mergeMenuItem);
        this.add(deleteMenuItem);
        this.add(booleanMenuItem);
//        this.add(extractMenuItem);
        this.add(curateMenuItem);

        this.addSeparator();

        this.add(saveMenuItem);

        this.addSeparator();

        this.add(relocateMenuItem);

        this.addSeparator();

        this.add(replaceFolderMenuItem);
        this.add(exportSubMenu);
        this.add(changeScaleItem);

        this.addSeparator();

        this.add(addSubMenu);
        

    }

    public void setDeleteString(String s) {
        deleteMenuItem.setText(s);
    }

    public void createButtons() {
    	
    	saveMenuItem = new JMenuItem(Labels.Populations.SAVE_AS_LBL);
    	saveMenuItem.addActionListener(e -> fireSignalChangeEvent("SaveCollectionAction"));


        moveUpMenuItem = new JMenuItem("Move up");
        moveUpMenuItem.addActionListener(e -> fireSignalChangeEvent("MoveDatasetUpAction"));

        moveDownMenuItem = new JMenuItem("Move down");
        moveDownMenuItem.addActionListener(e -> {

            fireSignalChangeEvent("MoveDatasetDownAction");
        });

        mergeMenuItem = new JMenuItem("Merge");
        mergeMenuItem.addActionListener(e -> {
            fireSignalChangeEvent("MergeCollectionAction");
        });

        curateMenuItem = new JMenuItem("Curate");
        curateMenuItem.addActionListener(e -> {
            fireSignalChangeEvent("CurateCollectionAction");
        });

        deleteMenuItem = new JMenuItem("Delete");
        deleteMenuItem.addActionListener(e -> {
            fireSignalChangeEvent("DeleteCollectionAction");
        });

        booleanMenuItem = new JMenuItem("Boolean");
        booleanMenuItem.addActionListener(e -> {
            fireSignalChangeEvent("DatasetArithmeticAction");
        });
        
        extractMenuItem = new JMenuItem("Extract cells");
        extractMenuItem.addActionListener(e->fireSignalChangeEvent(SignalChangeEvent.EXTRACT_SUBSET));
        

        changeScaleItem = new JMenuItem(Labels.Populations.CHANGE_SCALE_LBL);
        changeScaleItem.addActionListener(e -> fireSignalChangeEvent(SignalChangeEvent.CHANGE_SCALE));
        
        exportSubMenu = new JMenu(Labels.Populations.EXPORT);
        
        exportStatsMenuItem = new JMenuItem(Labels.Populations.EXPORT_STATS);
        exportStatsMenuItem.addActionListener(e -> fireSignalChangeEvent(SignalChangeEvent.EXPORT_STATS));
        
        exportSignalsItem = new JMenuItem(Labels.Populations.EXPORT_SIGNALS);
        exportSignalsItem.addActionListener(e -> fireSignalChangeEvent(SignalChangeEvent.EXPORT_SIGNALS));
        
        exportShellsItem = new JMenuItem(Labels.Populations.EXPORT_SHELLS);
        exportShellsItem.addActionListener(e -> fireSignalChangeEvent(SignalChangeEvent.EXPORT_SHELLS));
        
        saveCellsMenuItem = new JMenuItem(Labels.Populations.EXPORT_CELL_LOCS);
        saveCellsMenuItem.addActionListener(e -> fireSignalChangeEvent(SignalChangeEvent.EXPORT_CELL_LOCS));

        exportSubMenu.add(exportStatsMenuItem);
        exportSubMenu.add(exportSignalsItem);
        exportSubMenu.add(exportShellsItem);
        exportSubMenu.add(saveCellsMenuItem);
        
        addNuclearSignalMenuItem = new JMenuItem(Labels.Populations.ADD_NUCLEAR_SIGNAL_LBL);
        addNuclearSignalMenuItem.setToolTipText(Labels.Populations.ADD_NUCLEAR_SIGNAL_TIP);
        addNuclearSignalMenuItem.addActionListener(e -> fireSignalChangeEvent(SignalChangeEvent.ADD_NUCLEAR_SIGNAL));
        
        fishRemappinglMenuItem = new JMenuItem(Labels.Populations.POST_FISH_MAPPING_LBL);
        fishRemappinglMenuItem.addActionListener(e -> fireSignalChangeEvent(SignalChangeEvent.POST_FISH_MAPPING));
        
        addSubMenu = new JMenu(Labels.Populations.ADD);
        addSubMenu.add(addNuclearSignalMenuItem);
        addSubMenu.add(fishRemappinglMenuItem);

    }

    public void setEnabled(boolean b) {
        for (Component c : this.getComponents()) {
            c.setEnabled(b);
        }
    }
    
    public void updateSingle() {
    	deleteMenuItem.setEnabled(true);
    	booleanMenuItem.setEnabled(true);
    	extractMenuItem.setEnabled(true);
    	saveMenuItem.setEnabled(true);
    	curateMenuItem.setEnabled(true);
    	relocateMenuItem.setEnabled(true);
    	saveCellsMenuItem.setEnabled(true);
    	exportSubMenu.setEnabled(true);
    	exportStatsMenuItem.setEnabled(true);
    	exportSignalsItem.setEnabled(true);
    	exportShellsItem.setEnabled(true);
    	changeScaleItem.setEnabled(true);
    	addSubMenu.setEnabled(true);
    	
    	moveUpMenuItem.setEnabled(true);
    	moveDownMenuItem.setEnabled(true);
    	
    	
    	mergeMenuItem.setEnabled(false);
    	
    }
    
    public void updateMultiple(){
    	setEnabled(false);
    	changeScaleItem.setEnabled(true);
    	mergeMenuItem.setEnabled(true);
    	deleteMenuItem.setEnabled(true);
    	booleanMenuItem.setEnabled(true);
    	exportSubMenu.setEnabled(true);
    	exportShellsItem.setEnabled(true);
    	exportSignalsItem.setEnabled(true);
    	exportStatsMenuItem.setEnabled(true);

    }
    
    public void updateNull(){
    	setEnabled(false);
    }
    
    public void updateClusterGroup(){
    	setEnabled(false);
    	moveUpMenuItem.setEnabled(true);
    	moveDownMenuItem.setEnabled(true);
    }

    public void setAddNuclearSignalEnabled(boolean b) {
        addNuclearSignalMenuItem.setEnabled(b);
    }

    public void setFishRemappingEnabled(boolean b) {
        this.fishRemappinglMenuItem.setEnabled(b);
    }

    public synchronized void addSignalChangeListener(SignalChangeListener l) {
        listeners.add(l);
    }

    public synchronized void removeSignalChangeListener(SignalChangeListener l) {
        listeners.remove(l);
    }

    private synchronized void fireSignalChangeEvent(String message) {
        SignalChangeEvent event = new SignalChangeEvent(this, message, SOURCE_COMPONENT);
        Iterator<Object> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            ((SignalChangeListener) iterator.next()).signalChangeReceived(event);
        }
    }
}
