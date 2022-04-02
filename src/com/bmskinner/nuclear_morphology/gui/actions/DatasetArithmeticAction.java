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

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.nucleus.CellCollectionFilterer;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.VirtualDataset;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
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
	
	private static final Logger LOGGER = Logger.getLogger(DatasetArithmeticAction.class.getName());

    private static final @NonNull String PROGRESS_LBL = "Dataset arithmetic";
    
    private File saveFile;

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
            	
            	saveFile = eh.getInputSupplier().requestFolder("Select new directory of images...");

            	IAnalysisDataset datasetOne = dialog.getDatasetOne();
                IAnalysisDataset datasetTwo = dialog.getDatasetTwo();
                DatasetArithmeticOperation operation = dialog.getOperation();

                LOGGER.info("Performing " + operation + " on datasets");
                // prepare a new collection

                ICellCollection newCollection = null;

                switch (operation) {
                case AND: // present in both
                	newCollection =  CellCollectionFilterer.and(datasetOne.getCollection(), datasetTwo.getCollection());

                    newCollection.setSharedCount(datasetOne.getCollection(), newCollection.size());
                    newCollection.setSharedCount(datasetTwo.getCollection(), newCollection.size());

                    datasetOne.getCollection().setSharedCount(newCollection, newCollection.size());
                    datasetTwo.getCollection().setSharedCount(newCollection, newCollection.size());
                    break;
                case NOT: // present in one, not present in two
                	newCollection =  CellCollectionFilterer.not(datasetOne.getCollection(), datasetTwo.getCollection());

                    newCollection.setSharedCount(datasetOne.getCollection(), newCollection.size());
                    newCollection.setSharedCount(datasetTwo.getCollection(), 0);

                    datasetOne.getCollection().setSharedCount(newCollection, newCollection.size());
                    datasetTwo.getCollection().setSharedCount(newCollection, 0);

                    break;
                case OR: // present in either (merge)
                	newCollection =  CellCollectionFilterer.or(datasetOne.getCollection(), datasetTwo.getCollection());
                	newCollection.setSharedCount(datasetOne.getCollection(), datasetOne.getCollection().size());
                    newCollection.setSharedCount(datasetTwo.getCollection(), datasetTwo.getCollection().size());

                    datasetOne.getCollection().setSharedCount(newCollection, datasetOne.getCollection().size());
                    datasetTwo.getCollection().setSharedCount(newCollection, datasetTwo.getCollection().size());
                	
                    break;
                case XOR: // present in either but not both
                	newCollection =  CellCollectionFilterer.xor(datasetOne.getCollection(), datasetTwo.getCollection());
                    break;
                default:
                    break;
                }

                makeNewDataset(newCollection);

            }

        } catch(RequestCancelledException e1) {
        	// User request cancelled
        	LOGGER.fine("User cancelled dataset arithmetic action");
        	
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

            if (newCollection instanceof VirtualDataset) {

            	IAnalysisDataset root = DatasetListManager.getInstance().getRootParent(newCollection);
                try {
					root.getCollection().getProfileManager().copySegmentsAndLandmarksTo(newCollection);
					root.addChildCollection(newCollection);
	                getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
				} catch (ProfileException | MissingProfileException e) {
					LOGGER.warning("Error: unable to complete operation");
				 LOGGER.log(Loggable.STACK, "Error copying profile offsets", e);
				}

            } else {
                newDataset = new DefaultAnalysisDataset(newCollection, saveFile);
                getDatasetEventHandler().fireDatasetEvent(DatasetEvent.MORPHOLOGY_ANALYSIS_ACTION, newDataset);
            }
        } else {
            LOGGER.info("No populations returned");
        }
    }
}
