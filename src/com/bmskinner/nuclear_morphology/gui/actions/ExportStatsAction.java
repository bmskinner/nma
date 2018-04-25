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

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.DatasetStatsExporter;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

/**
 * The action for exporting stats from datasets
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class ExportStatsAction extends MultiDatasetResultAction {

    private static final String PROGRESS_LBL = "Exporting stats";

    public ExportStatsAction(final List<IAnalysisDataset> datasets, final MainWindow mw) {
        super(datasets, PROGRESS_LBL, mw);
    }

    @Override
    public void run() {

        File file = FileSelector.chooseStatsExportFile(datasets);

        if (file == null) {
            cancel();
            return;
        }

        IAnalysisMethod m = new DatasetStatsExporter(file, datasets);
        worker = new DefaultAnalysisWorker(m, datasets.size());
        worker.addPropertyChangeListener(this);
        this.setProgressMessage("Exporting stats");
        ThreadManager.getInstance().submit(worker);

    }

}
