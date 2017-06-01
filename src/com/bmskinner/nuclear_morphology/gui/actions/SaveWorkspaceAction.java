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

import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;

import com.bmskinner.nuclear_morphology.analysis.DefaultWorkspace;
import com.bmskinner.nuclear_morphology.analysis.IWorkspace;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.io.Importer;
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

        File file = chooseExportFile();

        if (file == null) {
            cancel();
            return;
        }

        // log("Saving workspace...");

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

    private File chooseExportFile() {

        String fileName = null;
        File dir = null;
        if (datasets.size() == 1) {
            dir = datasets.get(0).getSavePath().getParentFile();
            fileName = datasets.get(0).getName() + Importer.WRK_FILE_EXTENSION;

        } else {
            fileName = "Workspace" + Importer.WRK_FILE_EXTENSION;
            dir = IAnalysisDataset.commonPathOfFiles(datasets);
            if (!dir.exists() || !dir.isDirectory()) {
                dir = new File(System.getProperty("user.home"));
            }
        }

        JFileChooser fc = new JFileChooser(dir);
        fc.setSelectedFile(new File(fileName));
        fc.setDialogTitle("Save workspace as");

        int returnVal = fc.showSaveDialog(fc);
        if (returnVal != 0) {
            return null; // user cancelled
        }

        File file = fc.getSelectedFile();

        // Add extension if needed
        if (!file.getAbsolutePath().endsWith(Importer.WRK_FILE_EXTENSION)) {
            file = new File(file.getAbsolutePath() + Importer.WRK_FILE_EXTENSION);
        }

        return file;
    }

}
