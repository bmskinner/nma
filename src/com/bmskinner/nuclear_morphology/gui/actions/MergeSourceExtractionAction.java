/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.MergeSourceExtractionMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.main.MainWindow;
import com.bmskinner.nuclear_morphology.main.EventHandler;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

public class MergeSourceExtractionAction extends MultiDatasetResultAction {
    
    private static final String PROGRESS_BAR_LABEL = "Extracting merge source";
    
    /**
     * Refold the given selected dataset
     */
    public MergeSourceExtractionAction(List<IAnalysisDataset> datasets, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(datasets, PROGRESS_BAR_LABEL, acceptor, eh);
    }

    @Override
    public void run() {
        this.setProgressBarIndeterminate();
        
        IAnalysisMethod m = new MergeSourceExtractionMethod(datasets);

        worker = new DefaultAnalysisWorker(m);
        worker.addPropertyChangeListener(this);

        this.setProgressMessage(PROGRESS_BAR_LABEL);
        ThreadManager.getInstance().submit(worker);

    }

    @Override
    public void finished() {
        
        setProgressBarVisible(false);

        try {

            IAnalysisResult r = worker.get();

            for(IAnalysisDataset d : r.getDatasets()){
                getDatasetEventHandler().fireDatasetEvent(DatasetEvent.ADD_DATASET, d);
            }

        } catch (InterruptedException e) {
            warn("Unable to extract merge source" + e.getMessage());
            stack("Unable to extract merge source", e);
            return;
        } catch (ExecutionException e) {
            warn("Unable to extract merge source" + e.getMessage());
            stack("Unable to extract merge source", e);
            return;
        }
        
        super.finished();
    }

}
