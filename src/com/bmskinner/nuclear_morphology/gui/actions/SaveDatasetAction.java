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
import java.util.concurrent.CountDownLatch;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.main.MainWindow;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;

import ij.io.SaveDialog;

public class SaveDatasetAction extends SingleDatasetResultAction {

    private File saveFile = null;
    
    private static final String PROGRESS_BAR_LABEL = "Saving dataset";

    /**
     * Constructor to save the current dataset. This gives programmatic access
     * to saving to non-default locations, without needing to use the save
     * dialog
     * 
     * @param dataset the dataset to save
     * @param saveFile the location to save to
     * @param mw the main window, to access program logger
     * @param doneSignal a latch to hold threads until the save is complete
     */
    public SaveDatasetAction(@NonNull IAnalysisDataset dataset, File saveFile, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, CountDownLatch doneSignal) {
        super(dataset, PROGRESS_BAR_LABEL, acceptor, eh);
        setLatch(doneSignal);
        this.setProgressBarIndeterminate();
    }

    /**
     * Default constructor to save the current dataset
     * 
     * @param dataset the dataset to save
     * @param mw the main window, to access program logger
     * @param doneSignal a latch to hold threads until the save is complete
     * @param chooseSaveLocation save to the default dataset save file, or choose another location
     */
    public SaveDatasetAction(@NonNull IAnalysisDataset dataset, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, CountDownLatch doneSignal,
            boolean chooseSaveLocation) {
        super(dataset, PROGRESS_BAR_LABEL, acceptor, eh);
        if(doneSignal!=null)
        	setLatch(doneSignal);

        this.setProgressBarIndeterminate();

        if (chooseSaveLocation) {

        	try {
        		saveFile = eh.getInputSupplier().requestFileSave(dataset.getSavePath().getParentFile(), dataset.getName(), "nmd");
			} catch (RequestCancelledException e) {
				cancel();
				return;
			}
        } else {
            saveFile = dataset.getSavePath();
        }

    }
    
    public SaveDatasetAction(List<IAnalysisDataset> list, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh, CountDownLatch doneSignal) {
        super(list, PROGRESS_BAR_LABEL, acceptor, eh);
        this.setLatch(doneSignal);
        this.setProgressBarIndeterminate();
        dataset = DatasetListManager.getInstance().getRootParent(dataset);
        saveFile = dataset.getSavePath();
    }

    @Override
    public void run() {

        if (saveFile != null) {
            log("Saving as " + saveFile.getAbsolutePath() + "...");
            long length = saveFile.exists() ? saveFile.length() : 0;
            IAnalysisMethod m = new DatasetExportMethod(dataset, saveFile);
            worker = new DefaultAnalysisWorker(m, length);
            worker.addPropertyChangeListener(this);
            ThreadManager.getInstance().submit(worker);
        } else {
            this.finished();
        }
    }

    @Override
    public void finished() {
    	
    	Thread thr = new Thread(()->{

    		// update the stored hashcode for the dataset
    		DatasetListManager.getInstance().updateHashCode(dataset);
    		
    		// if no list was provided, or no more entries remain, finish
    		if (!hasRemainingDatasetsToProcess()) {
    			countdownLatch();
    			SaveDatasetAction.super.finished();
    		} else { // otherwise analyse the next item in the list
    			cancel(); // remove progress bar
    			new SaveDatasetAction(getRemainingDatasetsToProcess(), progressAcceptors.get(0), eh, getLatch().get()).run();
    		}
    	});

        thr.start();
    }
}
