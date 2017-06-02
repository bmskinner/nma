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

import ij.io.SaveDialog;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;

public class SaveDatasetAction extends SingleDatasetResultAction {

    private File saveFile = null;

    /**
     * Constructor to save the current dataset. This gives programmatic access
     * to saving to non-default locations, without needing to use the save
     * dialog
     * 
     * @param dataset
     *            the dataset to save
     * @param saveFile
     *            the location to save to
     * @param mw
     *            the main window, to access program logger
     * @param doneSignal
     *            a latch to hold threads until the save is complete
     */
    public SaveDatasetAction(IAnalysisDataset dataset, File saveFile, MainWindow mw, CountDownLatch doneSignal) {
        super(dataset, "Saving dataset", mw);
        setLatch(doneSignal);
        finest("Save dataset action created by explicit file location");

        this.setProgressBarIndeterminate();
        // worker = new PopulationExporter(dataset, saveFile);
        // worker.addPropertyChangeListener(this);
        //
        // ThreadManager.getInstance().submit(worker);
        // worker.execute();
    }

    /**
     * Default constructor to save the current dataset
     * 
     * @param dataset
     *            the dataset to save
     * @param mw
     *            the main window, to access program logger
     * @param doneSignal
     *            a latch to hold threads until the save is complete
     * @param chooseSaveLocation
     *            save to the default dataset save file, or choose another
     *            location
     */
    public SaveDatasetAction(IAnalysisDataset dataset, MainWindow mw, CountDownLatch doneSignal,
            boolean chooseSaveLocation) {
        super(dataset, "Saving dataset", mw);
        setLatch(doneSignal);
        finest("Save dataset action created by default or manual file location");
        this.setProgressBarIndeterminate();
        // File saveFile = null;
        if (chooseSaveLocation) {

            SaveDialog saveDialog = new SaveDialog("Save as...", dataset.getName(), ".nmd");

            String fileName = saveDialog.getFileName();
            String folderName = saveDialog.getDirectory();
            if (fileName != null && folderName != null) {
                saveFile = new File(folderName + File.separator + fileName);
            } else {
                this.finished();
            }

        } else {
            saveFile = dataset.getSavePath();
        }

    }

    @Override
    public void run() {

        if (saveFile != null) {
            log("Saving as " + saveFile.getAbsolutePath() + "...");

            IAnalysisMethod m = new DatasetExportMethod(dataset, saveFile);
            worker = new DefaultAnalysisWorker(m);

            worker.addPropertyChangeListener(this);
            ThreadManager.getInstance().submit(worker);
        } else {
            this.finished();
        }
    }

    @Override
    public void finished() {

        finer("Save action complete");
        finest("Removing save latch");
        this.countdownLatch();
        super.finished();

    }

}
