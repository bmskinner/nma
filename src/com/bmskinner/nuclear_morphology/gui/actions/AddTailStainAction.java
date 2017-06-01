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
package com.bmskinner.nuclear_morphology.gui.actions;

import ij.io.DirectoryChooser;

import java.io.File;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.tail.TailDetectionMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.dialogs.TailDetectionSettingsDialog;

public class AddTailStainAction extends SingleDatasetResultAction {

    public AddTailStainAction(IAnalysisDataset dataset, MainWindow mw) {
        super(dataset, "Tail detection", mw);

    }

    @Override
    public void run() {
        try {

            TailDetectionSettingsDialog analysisSetup = new TailDetectionSettingsDialog(dataset.getAnalysisOptions());

            final int channel = analysisSetup.getChannel();

            DirectoryChooser openDialog = new DirectoryChooser("Select directory of tubulin images...");
            String folderName = openDialog.getDirectory();

            if (folderName == null) {
                this.cancel();
                return; // user cancelled
            }

            final File folder = new File(folderName);

            if (!folder.isDirectory()) {
                this.cancel();
                return;
            }
            if (!folder.exists()) {
                this.cancel();
                return; // check folder is ok
            }

            IAnalysisMethod m = new TailDetectionMethod(dataset, folder, channel);

            worker = new DefaultAnalysisWorker(m, dataset.getCollection().size());

            // worker = new TubulinTailDetector(dataset, folder, channel);
            worker.addPropertyChangeListener(this);
            this.setProgressMessage("Tail detection:" + dataset.getName());
            ThreadManager.getInstance().submit(worker);
        } catch (Exception e) {
            this.cancel();
            error("Error in tail analysis", e);

        }
    }
}