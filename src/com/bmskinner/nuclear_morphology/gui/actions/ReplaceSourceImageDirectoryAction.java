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

import ij.io.DirectoryChooser;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.MainWindow;

public class ReplaceSourceImageDirectoryAction extends SingleDatasetResultAction {

    public ReplaceSourceImageDirectoryAction(IAnalysisDataset dataset, MainWindow mw) {
        super(dataset, "Replacing images", mw);
        this.setProgressBarIndeterminate();

    }

    @Override
    public void run() {
        try {

            if (!dataset.hasMergeSources()) {

                DirectoryChooser localOpenDialog = new DirectoryChooser("Select new directory of images...");
                String folderName = localOpenDialog.getDirectory();

                if (folderName != null) {

                    File newFolder = new File(folderName);

                    log("Updating folder to " + folderName);

                    dataset.updateSourceImageDirectory(newFolder);

                    finished();

                } else {
                    log("Update cancelled");
                    cancel();
                }
            } else {
                warn("Dataset is a merge; cancelling");
                cancel();
            }

        } catch (Exception e) {
            error("Error in folder update: " + e.getMessage(), e);
        }
    }

    @Override
    public void finished() {
        // Do not use super.finished(), or it will trigger another save action
        fine("Folder update complete");
        cancel();
        getInterfaceEventHandler().removeInterfaceEventListener(mw.getEventHandler());
        getDatasetEventHandler().removeDatasetEventListener(mw.getEventHandler());
    }
}
