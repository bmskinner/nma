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
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DatasetMergeMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.signals.PairedSignalGroups;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.dialogs.DatasetMergingDialog;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.io.Io;

/**
 * Carry out a merge of datasets
 * 
 * @author ben
 *
 */
public class MergeCollectionAction extends MultiDatasetResultAction {

    private static final String PROGRESS_BAR_LABEL   = "Merging";
    private static final String DEFAULT_DATASET_NAME = "Merge_of_datasets";

    public MergeCollectionAction(final List<IAnalysisDataset> datasets, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(datasets, PROGRESS_BAR_LABEL, acceptor, eh);
    }

    @Override
    public void run() {

    	if (!datasetsAreMergeable(datasets)) {
    		cancel();
    		return;
    	}

    	// Try to find a sensible ancestor dir of the datasets as the save
    	// dir
    	// Otherwise default to the home dir
    	File dir = IAnalysisDataset.commonPathOfFiles(datasets);
    	if (!dir.exists() || !dir.isDirectory()) {
    		dir = GlobalOptions.getInstance().getDefaultDir();
    	}

    	try {
    		File saveFile = new DefaultInputSupplier().requestFileSave(dir, DEFAULT_DATASET_NAME, Io.SAVE_FILE_EXTENSION_NODOT);

    		// Check for signals in >1 dataset
    		int numSignals = 0;
    		for (IAnalysisDataset d : datasets) {
    			if (d.getCollection().getSignalManager().hasSignals())
    				numSignals++;
    		}

    		IAnalysisMethod m;

    		if (numSignals > 1) {

    			DatasetMergingDialog dialog = new DatasetMergingDialog(datasets);
    			PairedSignalGroups pairs = dialog.getPairedSignalGroups();

    			if (!pairs.isEmpty()) {
    				// User decided to merge signals
    				m = new DatasetMergeMethod(datasets, saveFile, pairs);
    			} else {
    				m = new DatasetMergeMethod(datasets, saveFile);
    			}
    		} else {
    			finest("No signal groups to merge");
    			// no signals to merge
    			m = new DatasetMergeMethod(datasets, saveFile);
    		}

    		worker = new DefaultAnalysisWorker(m, 100);
    		worker.addPropertyChangeListener(this);
    		ThreadManager.getInstance().submit(worker);

    	} catch (RequestCancelledException e) {
    		cancel();
    	}
    }

    /**
     * Check datasets are valid to be merged
     * 
     * @param datasets
     * @return
     */
    private boolean datasetsAreMergeable(List<IAnalysisDataset> datasets) {

    	if (datasets.size() == 2 && (datasets.get(0).hasChild(datasets.get(1)) || datasets.get(1).hasChild(datasets.get(0)))) {
    		warn("No. Merging parent and child is silly.");
    		return false;
    	}
    	return true;
    }

    @Override
    public void finished() {

        IAnalysisResult r;
        try {
            r = worker.get();
        } catch (InterruptedException | ExecutionException e) {
            warn("Error merging datasets");
            stack("Error merging datasets", e);
            this.cancel();
            return;
        }
        List<IAnalysisDataset> datasets = r.getDatasets();

        if (datasets != null && !datasets.isEmpty())
        	getDatasetEventHandler().fireDatasetEvent(DatasetEvent.MORPHOLOGY_ANALYSIS_ACTION, datasets);

        super.finished();
    }
}
