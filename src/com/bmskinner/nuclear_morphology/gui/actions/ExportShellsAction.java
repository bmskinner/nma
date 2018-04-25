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

import java.io.File;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.DatasetShellsExporter;
import com.bmskinner.nuclear_morphology.io.DatasetStatsExporter;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

/**
 * The action for exporting shell data from datasets
 * 
 * @author bms41
 * @since 1.13.8
 *
 */
public class ExportShellsAction extends MultiDatasetResultAction {

    private static final String PROGRESS_LBL = "Exporting stats";

    public ExportShellsAction(final List<IAnalysisDataset> datasets, final MainWindow mw) {
        super(datasets, PROGRESS_LBL, mw);
    }

    @Override
    public void run() {

        File file = FileSelector.chooseStatsExportFile(datasets);

        if (file == null) {
            cancel();
            return;
        }

        IAnalysisMethod m = new DatasetShellsExporter(file, datasets);
        worker = new DefaultAnalysisWorker(m, datasets.size());
        worker.addPropertyChangeListener(this);
        this.setProgressMessage("Exporting stats");
        ThreadManager.getInstance().submit(worker);

    }

}
