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
import java.util.List;

import com.bmskinner.nuclear_morphology.components.DefaultWorkspace;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IWorkspace;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.WorkspaceExporter;

public class SaveWorkspaceAction extends VoidResultAction {

    private static final String PROGRESS_LBL = "Saving workspace";

    private final List<IAnalysisDataset> datasets;

    public SaveWorkspaceAction(final List<IAnalysisDataset> datasets, MainWindow mw) {
        super(PROGRESS_LBL, mw);
        this.datasets = datasets;

    }

    @Override
    public void run() {

        if (datasets.size() == 0) {
            cancel();
            return;
        }

        File file = FileSelector.chooseWorkspaceExportFile(datasets);

        if (file == null) {
            cancel();
            return;
        }

        // Get all datasets
        IWorkspace w = new DefaultWorkspace(file);

        for (IAnalysisDataset d : datasets) {
            w.add(d);
        }

        WorkspaceExporter exp = new WorkspaceExporter(w);
        exp.export();
        log("Exported workspace file to " + file.getAbsolutePath());

        this.cancel();

    }
}
