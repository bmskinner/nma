/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.SignalChangeListener;
import com.bmskinner.nuclear_morphology.gui.actions.FishRemappingAction;

@SuppressWarnings("serial")
public class PopulationListPopupMenu extends JPopupMenu {
	
	public static final String SOURCE_COMPONENT = "PopupMenu"; 
	
	public static final String SAVE_AS_LBL = "Save nmd as..."; 

	JMenuItem mergeMenuItem;
	JMenuItem curateMenuItem;
	
	JMenuItem deleteMenuItem;
	
	JMenuItem booleanMenuItem;
	
	JMenuItem saveMenuItem = new JMenuItem( new AbstractAction(SAVE_AS_LBL){
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("SaveCollectionAction");				
		}
	});
	

	JMenuItem saveCellsMenuItem = new JMenuItem( new AbstractAction("Save cell locations"){
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("SaveCellLocations");				
		}
	});
	
	JMenuItem relocateMenuItem = new JMenuItem( new AbstractAction("Relocate cells"){
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("RelocateCellsAction");				
		}
	});
	
	JMenuItem exportStatsMenuItem;
	
	JMenuItem moveUpMenuItem;
	
	JMenuItem moveDownMenuItem;
	
	JMenuItem replaceFolderMenuItem = new JMenuItem( new AbstractAction("Change folder"){
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("ChangeNucleusFolderAction");				
		}
	});
		
	JMenuItem addNuclearSignalMenuItem = new JMenuItem( new AbstractAction("Add nuclear signal"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("AddNuclearSignalAction");	
		}
	});
	
	JMenuItem fishRemappinglMenuItem = new JMenuItem( new AbstractAction("Post-FISH mapping"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("PostFISHRemappingAction");	
		}
	});
			
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
		this.add(curateMenuItem);
		
		this.addSeparator();
		
		this.add(saveMenuItem);
		
		this.addSeparator();
		
		this.add(saveCellsMenuItem);
		this.add(relocateMenuItem);

		this.addSeparator();
		
		this.add(replaceFolderMenuItem);
		this.add(exportStatsMenuItem);

		this.addSeparator();

		this.add(addNuclearSignalMenuItem);
		this.add(fishRemappinglMenuItem);

    }
	
	public void setDeleteString(String s){
		deleteMenuItem.setText(s);
	}
	
	public void createButtons(){
		
		moveUpMenuItem = new JMenuItem("Move up");
		moveUpMenuItem.addActionListener( e -> {
			fireSignalChangeEvent("MoveDatasetUpAction");	
		});
		
		moveDownMenuItem = new JMenuItem("Move down");
		moveDownMenuItem.addActionListener( e -> {
			
			fireSignalChangeEvent("MoveDatasetDownAction");	
		});
		
		
		mergeMenuItem = new JMenuItem("Merge");
		mergeMenuItem.addActionListener( e -> {
			fireSignalChangeEvent("MergeCollectionAction");	
		});
		
		curateMenuItem = new JMenuItem("Curate");
		curateMenuItem.addActionListener( e -> {
			fireSignalChangeEvent("CurateCollectionAction");	
		});
				
		deleteMenuItem = new JMenuItem("Delete");
		deleteMenuItem.addActionListener( e -> {
			fireSignalChangeEvent("DeleteCollectionAction");	
		});
		
		booleanMenuItem = new JMenuItem("Boolean");
		booleanMenuItem.addActionListener( e -> {
			fireSignalChangeEvent("DatasetArithmeticAction");	
		});
		
		exportStatsMenuItem = new JMenuItem("Export stats");
		exportStatsMenuItem.addActionListener( e -> {
			fireSignalChangeEvent(SignalChangeEvent.EXPORT_STATS);	
		});
		
		
		
	}
	
	public void setEnabled(boolean b){
		for(Component c : this.getComponents()){
			c.setEnabled(b);
		}	
	}
		
	public void enableMerge(){
		mergeMenuItem.setEnabled(true);
	}
	
	public void disableMerge(){
		mergeMenuItem.setEnabled(false);
	}
	
	public void enableCurate(){
		curateMenuItem.setEnabled(true);
	}
	
	public void disableCurate(){
		curateMenuItem.setEnabled(false);
	}
	
	public void enableDelete(){
		deleteMenuItem.setEnabled(true);
	}
	
	public void disableDelete(){
		deleteMenuItem.setEnabled(false);
	}
	
	public void enableBoolean(){
		booleanMenuItem.setEnabled(true);
	}
	
	public void disableBoolean(){
		booleanMenuItem.setEnabled(false);
	}
	
	public void enableSave(){
		saveMenuItem.setEnabled(true);
	}
	
	public void disableSave(){
		saveMenuItem.setEnabled(false);
	}
	
	public void enableSaveCells(){
		saveCellsMenuItem.setEnabled(true);
	}
	
	public void disableSaveCells(){
		saveCellsMenuItem.setEnabled(false);
	}
	
	
	public void enableMenuUp(){
		moveUpMenuItem.setEnabled(true);
	}
	
	public void disableMenuUp(){
		moveUpMenuItem.setEnabled(false);
	}
	
	public void enableMenuDown(){
		moveDownMenuItem.setEnabled(true);
	}
	
	public void disableMenuDown(){
		moveDownMenuItem.setEnabled(false);
	}
	
	public void enableReplaceFolder(){
		replaceFolderMenuItem.setEnabled(true);
	}
	
	public void disableReplaceFolder(){
		replaceFolderMenuItem.setEnabled(false);
	}

	
	public void setRelocateCellsEnabled(boolean b){
		relocateMenuItem.setEnabled(b);
	}


	public void setAddNuclearSignalEnabled(boolean b){
		addNuclearSignalMenuItem.setEnabled(b);
	}
	
	public void setFishRemappingEnabled(boolean b){
		this.fishRemappinglMenuItem.setEnabled(b);
	}
	
	public void setExportStatsEnabled(boolean b){
		this.exportStatsMenuItem.setEnabled(b);
	}
	
	public synchronized void addSignalChangeListener( SignalChangeListener l ) {
        listeners.add( l );
    }
    
    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
        listeners.remove( l );
    }
     
    private synchronized void fireSignalChangeEvent(String message) {
        SignalChangeEvent event = new SignalChangeEvent( this, message, SOURCE_COMPONENT);
        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
        }
    }
}
