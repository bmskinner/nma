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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;

@Deprecated
public class AddTailStainAction extends SingleDatasetResultAction {
	
	private static final @NonNull String PROGRESS_BAR_LABEL = "Tail detection";

    public AddTailStainAction(IAnalysisDataset dataset, @NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
        super(dataset, PROGRESS_BAR_LABEL, acceptor, eh);

    }

    @Override
    public void run() {
//        try {
//
//            TailDetectionSettingsDialog analysisSetup = new TailDetectionSettingsDialog(dataset.getAnalysisOptions());
//
//            final int channel = analysisSetup.getChannel();
//
//            DirectoryChooser openDialog = new DirectoryChooser("Select directory of tubulin images...");
//            String folderName = openDialog.getDirectory();
//
//            if (folderName == null) {
//                this.cancel();
//                return; // user cancelled
//            }
//
//            final File folder = new File(folderName);
//
//            if (!folder.isDirectory()) {
//                this.cancel();
//                return;
//            }
//            if (!folder.exists()) {
//                this.cancel();
//                return; // check folder is ok
//            }
//
//            IAnalysisMethod m = new TailDetectionMethod(dataset, folder, channel);
//
//            worker = new DefaultAnalysisWorker(m, dataset.getCollection().size());
//
//            // worker = new TubulinTailDetector(dataset, folder, channel);
//            worker.addPropertyChangeListener(this);
//            this.setProgressMessage("Tail detection:" + dataset.getName());
//            ThreadManager.getInstance().submit(worker);
//        } catch (Exception e) {
//            this.cancel();
//            error("Error in tail analysis", e);
//
//        }
    }
}
