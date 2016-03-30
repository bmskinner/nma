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
package gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class PopulationListPopupMenu extends JPopupMenu {
	
	public static final String SOURCE_COMPONENT = "PopupMenu"; 

	private static final long serialVersionUID = 1L;
	JMenuItem mergeMenuItem = new JMenuItem( new AbstractAction("Merge"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("MergeCollectionAction");				
		}
	});
	
	JMenuItem curateMenuItem = new JMenuItem( new AbstractAction("Curate"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("CurateCollectionAction");				
		}
	});
	
	JMenuItem deleteMenuItem = new JMenuItem( new AbstractAction("Delete"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("DeleteCollectionAction");				
		}
	});
	
	JMenuItem booleanMenuItem = new JMenuItem( new AbstractAction("Boolean operation"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("DatasetArithmeticAction");				
		}
	});
	JMenuItem saveMenuItem = new JMenuItem( new AbstractAction("Save as..."){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("SaveCollectionAction");				
		}
	});
	
	JMenuItem extractMenuItem = new JMenuItem( new AbstractAction("Extract nuclei"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("ExtractNucleiAction");				
		}
	});
	
	@SuppressWarnings("serial")
	JMenuItem saveCellsMenuItem = new JMenuItem( new AbstractAction("Save cell locations"){
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("SaveCellLocations");				
		}
	});
	
	@SuppressWarnings("serial")
	JMenuItem relocateMenuItem = new JMenuItem( new AbstractAction("Relocate cells"){
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("RelocateCellsAction");				
		}
	});
	
	
	
	JMenuItem moveUpMenuItem = new JMenuItem( new AbstractAction("Move up"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("MoveDatasetUpAction");				
		}
	});
	
	JMenuItem moveDownMenuItem = new JMenuItem( new AbstractAction("Move down"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("MoveDatasetDownAction");				
		}
	});
	
	JMenuItem replaceFolderMenuItem = new JMenuItem( new AbstractAction("Change folder"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("ChangeNucleusFolderAction");				
		}
	});
	
	JMenuItem exportStatsMenuItem = new JMenuItem( new AbstractAction("Export stats"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("ExportDatasetStatsAction");				
		}
	});
	JMenuItem applySegmentationMenuItem = new JMenuItem( new AbstractAction("Reapply profile"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("ReapplySegmentProfileAction");				
		}
	});
	
	JMenuItem addTailStainMenuItem = new JMenuItem( new AbstractAction("Add tail stain"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("AddTailStainAction");			
		}
	});
	
	JMenuItem addNuclearSignalMenuItem = new JMenuItem( new AbstractAction("Add nuclear signal"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("AddNuclearSignalAction");	
		}
	});
	
	
	JMenuItem performShellAnalysisMenuItem = new JMenuItem( new AbstractAction("Run shell analysis"){
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			fireSignalChangeEvent("NewShellAnalysisAction");	
		}
	});
	
	private List<Object> listeners = new ArrayList<Object>();
			
			
	public PopulationListPopupMenu() {
		
		super("Popup");
		
		this.add(moveUpMenuItem);
		this.add(moveDownMenuItem);
		this.addSeparator();
		this.add(mergeMenuItem);
		this.add(deleteMenuItem);
		this.add(booleanMenuItem);
		this.add(curateMenuItem);
		this.addSeparator();
		this.add(saveMenuItem);
		this.add(extractMenuItem);
		this.add(saveCellsMenuItem);
		this.add(relocateMenuItem);
		this.add(exportStatsMenuItem);
		this.addSeparator();
		this.add(replaceFolderMenuItem);
		this.add(applySegmentationMenuItem);
		this.addSeparator();
		this.add(addTailStainMenuItem);
		this.add(addNuclearSignalMenuItem);
		this.add(performShellAnalysisMenuItem);
    }
	
	public void enableAll(){
		
		
		for(Component c : this.getComponents()){
			c.setEnabled(true);
		}		
	}
	
	public void disableAll(){
		
		for(Component c : this.getComponents()){
			c.setEnabled(false);
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
	
	public void enableSplit(){
		booleanMenuItem.setEnabled(true);
	}
	
	public void disableSplit(){
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
	
	public void enableExtract(){
		extractMenuItem.setEnabled(true);
	}
	
	public void disableExtract(){
		extractMenuItem.setEnabled(false);
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
	
	public void enableExportStats(){
		exportStatsMenuItem.setEnabled(true);
	}
	
	public void disableExportStats(){
		exportStatsMenuItem.setEnabled(false);
	}
	
	public void enableRelocateCells(){
		relocateMenuItem.setEnabled(true);
	}
	
	public void disableRelocateCells(){
		relocateMenuItem.setEnabled(false);
	}
	
	public void enableApplySegmentation(){
		applySegmentationMenuItem.setEnabled(true);
	}
	
	public void disableApplySegmentation(){
		applySegmentationMenuItem.setEnabled(false);
	}
		
	public void enableAddTailStain(){
		addTailStainMenuItem.setEnabled(true);
	}
	
	public void disableAddTailStain(){
		addTailStainMenuItem.setEnabled(false);
	}
	
	public void enableAddNuclearSignal(){
		addNuclearSignalMenuItem.setEnabled(true);
	}
	
	public void disableAddNuclearSignal(){
		addNuclearSignalMenuItem.setEnabled(false);
	}
	
	public void enableRunShellAnalysis(){
		performShellAnalysisMenuItem.setEnabled(true);
	}
	
	public void disableRunShellAnalysis(){
		performShellAnalysisMenuItem.setEnabled(false);
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
