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

import java.io.File;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalDetectionMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.SignalImageProber;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

/**
 * Show the setup screen to detect nuclear signals, and run a detection analysis
 * 
 * @author ben
 *
 */
public class AddNuclearSignalAction extends SingleDatasetResultAction {

    private File folder;
    
    private static final String PROGRESS_BAR_LABEL = "Signal detection";

    public AddNuclearSignalAction(IAnalysisDataset dataset, MainWindow mw) {
        super(dataset, PROGRESS_BAR_LABEL, mw);
    }

    @Override
    public void run() {
        try {

            folder = FileSelector.chooseFISHDirectory(dataset);
            if (folder==null) {
                cancel();
                return;
            }
            // add dialog for non-default detection options
            SignalImageProber analysisSetup = new SignalImageProber(dataset, folder);

            if (analysisSetup.isOk()) {

                INuclearSignalOptions options = analysisSetup.getOptions();

                IAnalysisMethod m = new SignalDetectionMethod(dataset, options, analysisSetup.getId());

                String name = dataset.getCollection().getSignalGroup(analysisSetup.getId()).getGroupName();

                worker = new DefaultAnalysisWorker(m, dataset.getCollection().size());

                this.setProgressMessage("Signal detection: " + name);
                worker.addPropertyChangeListener(this);
                ThreadManager.getInstance().submit(worker);
            } else {
                this.cancel();
                return;
            }

        } catch (Exception e) {
            this.cancel();
            warn("Error in signal detection");
            stack("Error in signal detection", e);
        }
    }

    @Override
    public void finished() {
        finer("Finished signal detection");
        this.cleanup(); // remove the property change listener
//        fireDatasetEvent(DatasetEvent.ADD_DATASET, dataset);
        
        getDatasetEventHandler().fireDatasetEvent(DatasetEvent.ADD_DATASET, dataset);

        cancel();

    }

}
