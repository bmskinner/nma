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
package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.dialogs.DatasetArithmeticSetupDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.DatasetArithmeticSetupDialog.DatasetArithmeticOperation;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Trigger methods to perform boolean operations on datasets
 * @author Ben Skinner
 *
 */
public class DatasetArithmeticAction extends MultiDatasetResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final @NonNull String PROGRESS_LBL = "Dataset arithmetic";

    public DatasetArithmeticAction(@NonNull List<IAnalysisDataset> list, @NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
        super(list, PROGRESS_LBL, acceptor, eh);
        this.setProgressBarIndeterminate();
    }

    @Override
    public void run() {
        try {

            /*
             * Make a dialog with a dropdown for dataset 1, operator, then
             * dropdown for dataset 2
             */

            DatasetArithmeticSetupDialog dialog = new DatasetArithmeticSetupDialog(datasets);

            if (dialog.isReadyToRun()) {

            	IAnalysisDataset datasetOne = dialog.getDatasetOne();
                IAnalysisDataset datasetTwo = dialog.getDatasetTwo();
                DatasetArithmeticOperation operation = dialog.getOperation();

                LOGGER.info("Performing " + operation + " on datasets");
                // prepare a new collection

                ICellCollection newCollection = null;

                switch (operation) {
                case AND: // present in both
                    newCollection = datasetOne.getCollection().and(datasetTwo.getCollection());

                    newCollection.setSharedCount(datasetOne.getCollection(), newCollection.size());
                    newCollection.setSharedCount(datasetTwo.getCollection(), newCollection.size());

                    datasetOne.getCollection().setSharedCount(newCollection, newCollection.size());
                    datasetTwo.getCollection().setSharedCount(newCollection, newCollection.size());
                    break;
                case NOT: // present in one, not present in two
                    newCollection = datasetOne.getCollection().not(datasetTwo.getCollection());

                    newCollection.setSharedCount(datasetOne.getCollection(), newCollection.size());
                    newCollection.setSharedCount(datasetTwo.getCollection(), 0);

                    datasetOne.getCollection().setSharedCount(newCollection, newCollection.size());
                    datasetTwo.getCollection().setSharedCount(newCollection, 0);

                    break;
                case OR: // present in either (merge)
                    newCollection = datasetOne.getCollection().or(datasetTwo.getCollection());
                    break;
                case XOR: // present in either but not both
                    newCollection = datasetOne.getCollection().xor(datasetTwo.getCollection());
                    break;
                default:
                    break;

                }

                makeNewDataset(newCollection);

            }

        } catch (Exception e1) {
            LOGGER.log(Loggable.STACK, "Error in dataset arithmetic", e1);

        } finally {
            cancel();
        }
    }

    private void makeNewDataset(ICellCollection newCollection) {
        if (newCollection != null && !newCollection.isEmpty()) {

            LOGGER.info("Found " + newCollection.size() + " cells");
            IAnalysisDataset newDataset;

            if (newCollection instanceof VirtualCellCollection) {

                IAnalysisDataset root = ((VirtualCellCollection) newCollection).getRootParent();

                try {
					root.getCollection().getProfileManager().copyCollectionOffsets(newCollection);
					root.addChildCollection(newCollection);
	                getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
				} catch (ProfileException e) {
					LOGGER.warning("Error: unable to complete operation");
				 LOGGER.log(Loggable.STACK, "Error copying profile offsets", e);
				}

            } else {
                newDataset = new DefaultAnalysisDataset(newCollection);
                newDataset.setRoot(true);
                getDatasetEventHandler().fireDatasetEvent(DatasetEvent.MORPHOLOGY_ANALYSIS_ACTION, newDataset);
            }
        } else {
            LOGGER.info("No populations returned");
        }
    }
}
