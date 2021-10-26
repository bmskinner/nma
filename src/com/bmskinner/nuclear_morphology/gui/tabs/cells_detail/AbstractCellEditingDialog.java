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
package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.DefaultCell;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.gui.dialogs.MessagingDialog;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This dialog contins a CellViewModel and is used as a base for any dialogs
 * that must edit a cell - this includes the segmentation and border point
 * editing dialogs.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractCellEditingDialog extends MessagingDialog {
	
	private static final Logger LOGGER = Logger.getLogger(AbstractCellEditingDialog.class.getName());

    protected ICell            cell    = null;
    protected ICell            workingCell;
    protected IAnalysisDataset dataset = null;

    private boolean hasChanged = false;

    protected CellViewModel cellModel; // allow changes to be propagated back to
                                       // the other panels

    public AbstractCellEditingDialog(final CellViewModel model) {
        super(null);
        this.cellModel = model;

        createUI();

        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        this.pack();

        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {

                if (cellHasChanged()) {
                    requestSaveOption();
                }
                setVisible(false);
            }
        });

        this.setLocationRelativeTo(null);
        this.setModal(false);

    }

    /**
     * Load the given cell and datast (the cell must belong to the dataset) and
     * display the dialog
     * 
     * @param cell
     * @param dataset
     */
    public void load(final ICell cell, final IAnalysisDataset dataset) {

        if (cell == null || dataset == null) {
            throw new IllegalArgumentException("Cell or dataset is null");
        }
        setCellChanged(false);
        this.cell = cell;
        this.dataset = dataset;
        this.workingCell = new DefaultCell(cell);
        workingCell.getPrimaryNucleus().setLocked(false);

        this.setTitle("Editing " + cell.getPrimaryNucleus().getNameAndNumber());
    }

    /**
     * Check if the active cell has been edited
     * 
     * @return
     */
    protected boolean cellHasChanged() {
        return hasChanged;
    }

    /**
     * Set whether the active cell has been edited
     * 
     * @param b
     */
    protected void setCellChanged(boolean b) {
        hasChanged = b;
    }

    protected abstract void createUI();

    protected abstract void updateCharts(ICell cell);

    protected void requestSaveOption() {

        Object[] options = { "Save changes", "Discard changes" };
        int save = JOptionPane.showOptionDialog(AbstractCellEditingDialog.this, "Save changes to cell?", "Save cell?",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        // Replace the input cell with the working cell
        if (save == 0) {

            workingCell.getPrimaryNucleus().setLocked(true); // Prevent further changes
                                                      // without unlocking
            dataset.getCollection().replaceCell(workingCell);

            // Trigger a dataset update and reprofiling
            try {
				dataset.getCollection().createProfileCollection();
			} catch (ProfileException | MissingLandmarkException | MissingProfileException e) {
				LOGGER.warning("Unable to profile cell collection");
				LOGGER.log(Loggable.STACK, e.getMessage(), e);
			}
            cellModel.swapCell(workingCell);

            fireDatasetEvent(DatasetEvent.RECACHE_CHARTS, dataset);
        }
    }

}
