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


package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.dialogs.DatasetArithmeticSetupDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.DatasetArithmeticSetupDialog.DatasetArithmeticOperation;
import com.bmskinner.nuclear_morphology.main.EventHandler;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

public class DatasetArithmeticAction extends MultiDatasetResultAction {

    private IAnalysisDataset datasetOne = null;

    private static final String PROGRESS_LBL = "Dataset arithmetic";

    public DatasetArithmeticAction(List<IAnalysisDataset> list, @NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
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

                datasetOne = dialog.getDatasetOne();
                IAnalysisDataset datasetTwo = dialog.getDatasetTwo();
                DatasetArithmeticOperation operation = dialog.getOperation();

                log("Performing " + operation + " on datasets");
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
            error("Error in dataset arithmetic", e1);

        } finally {
            cancel();
        }
    }

    private void makeNewDataset(ICellCollection newCollection) {
        if (newCollection != null && newCollection.size() > 0) {

            log("Found " + newCollection.size() + " cells");
            IAnalysisDataset newDataset;

            if (newCollection instanceof VirtualCellCollection) {

                IAnalysisDataset root = ((VirtualCellCollection) newCollection).getRootParent();

                try {
					root.getCollection().getProfileManager().copyCollectionOffsets(newCollection);
					root.addChildCollection(newCollection);
	                newDataset = root.getChildDataset(newCollection.getID());
	                getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.REFRESH_POPULATIONS);
				} catch (ProfileException e) {
					warn("Error: unable to complete operation");
					fine("Error copying profile offsets", e);
				}

            } else {
                newDataset = new DefaultAnalysisDataset(newCollection);
                newDataset.setRoot(true);
                int flag = SingleDatasetResultAction.ADD_POPULATION;
                flag |= SingleDatasetResultAction.SAVE_DATASET;
                flag |= SingleDatasetResultAction.ASSIGN_SEGMENTS;
                RunProfilingAction pr = new RunProfilingAction(newDataset, flag, progressAcceptors.get(0), eh);
                log("Running morphology analysis...");
                ThreadManager.getInstance().execute(pr);
            }

            

        } else {
            log("No populations returned");
        }
    }
}
