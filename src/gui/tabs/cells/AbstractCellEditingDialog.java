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

package gui.tabs.cells;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JOptionPane;

import analysis.AnalysisDataset;
import analysis.IAnalysisDataset;
import components.Cell;
import components.ICell;
import components.active.DefaultCell;
import gui.DatasetEvent;
import gui.dialogs.MessagingDialog;

/**
 * This dialog contins a CellViewModel and is used as a base for any 
 * dialogs that must edit a cell - this includes the segmentation and
 * border point editing dialogs.
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractCellEditingDialog extends MessagingDialog {
	
	protected ICell cell = null;
	protected ICell workingCell;
	protected IAnalysisDataset dataset = null;
			
	private boolean hasChanged = false;
	
	protected CellViewModel cellModel; // allow changes to be propagated back to the other panels
	
	public AbstractCellEditingDialog(final CellViewModel model){
		super( null );
		this.cellModel = model;
		
		createUI();
		
		
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		this.pack();
		
		
		this.addWindowListener(new WindowAdapter() {
			
			public void windowClosing(WindowEvent e) {
				
				if(cellHasChanged()){
					requestSaveOption();
				} 
				setVisible(false);
			}
		});
		
		this.setLocationRelativeTo(null);
		this.setModal(false);
		
	}
		
	/**
	 * Load the given cell and datast (the cell must belong to the dataset)
	 * and display the dialog
	 * @param cell
	 * @param dataset
	 */
	public void load(final ICell cell, final IAnalysisDataset dataset){

		if(cell==null || dataset==null){
			throw new IllegalArgumentException("Cell or dataset is null");
		}
		setCellChanged(false);
		this.cell = cell;
		this.dataset = dataset;
		this.workingCell = new DefaultCell(cell);
		workingCell.getNucleus().setLocked(false);

		this.setTitle("Editing "+cell.getNucleus().getNameAndNumber());
	}

	
	/**
	 * Check if the active cell has been edited
	 * @return
	 */
	protected boolean cellHasChanged(){
		return hasChanged;
	}
	
	
	/**
	 * Set whether the active cell has been edited
	 * @param b
	 */
	protected void setCellChanged(boolean b){
		hasChanged = b;
	}
	
	protected abstract void createUI();
	
	protected abstract void updateCharts(ICell cell);
	
	protected void requestSaveOption(){
		
		Object[] options = { "Save changes" , "Discard changes" };
		int save = JOptionPane.showOptionDialog(AbstractCellEditingDialog.this,
				"Save changes to cell?", 
				"Save cell?",
				JOptionPane.DEFAULT_OPTION, 
				JOptionPane.QUESTION_MESSAGE,
				null, options, options[0]);

		// Replace the input cell with the working cell
		if(save==0){
			
			workingCell.getNucleus().setLocked(true); // Prevent further changes without unlocking
			dataset.getCollection().replaceCell(workingCell);

			// Trigger a dataset update and reprofiling
			dataset.getCollection().createProfileCollection();
			cellModel.swapCell(workingCell);

			
			fireDatasetEvent(DatasetEvent.REFRESH_CACHE, dataset);
		} 
	}

}
