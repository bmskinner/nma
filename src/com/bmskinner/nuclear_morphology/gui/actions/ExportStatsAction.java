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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.DatasetShellsExporter;
import com.bmskinner.nuclear_morphology.io.DatasetSignalsExporter;
import com.bmskinner.nuclear_morphology.io.DatasetStatsExporter;

/**
 * The base action for exporting stats from datasets
 * 
 * @author bms41
 * @since 1.13.8
 *
 */
public abstract class ExportStatsAction extends MultiDatasetResultAction {

    public ExportStatsAction(@NonNull final List<IAnalysisDataset> datasets, @NonNull final String label, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(datasets, label, acceptor, eh);
    }
    
    /**
     * The action for exporting nuclear stats from datasets
     * 
     * @author bms41
     * @since 1.13.4
     *
     */
    public static class ExportNuclearStatsAction extends ExportStatsAction {

        private static final @NonNull String PROGRESS_LBL = "Exporting nuclear stats";

        public ExportNuclearStatsAction(@NonNull final List<IAnalysisDataset> datasets, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
            super(datasets, PROGRESS_LBL, acceptor, eh);
        }

        @Override
        public void run() {

            File file = FileSelector.chooseStatsExportFile(datasets, "stats");

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
    
    /**
     * The action for exporting shell data from datasets
     * 
     * @author bms41
     * @since 1.13.8
     *
     */
    public static class ExportShellsAction extends ExportStatsAction {

        private static final String PROGRESS_LBL = "Exporting shells";

        public ExportShellsAction(@NonNull final List<IAnalysisDataset> datasets, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
            super(datasets, PROGRESS_LBL, acceptor, eh);
        }

        @Override
        public void run() {

            File file = FileSelector.chooseStatsExportFile(datasets, "shells");

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
    
    /**
     * The action for exporting shell data from datasets
     * 
     * @author bms41
     * @since 1.13.8
     *
     */
    public static class ExportSignalsAction extends ExportStatsAction {

        private static final String PROGRESS_LBL = "Exporting signals";

        public ExportSignalsAction(@NonNull final List<IAnalysisDataset> datasets, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
            super(datasets, PROGRESS_LBL, acceptor, eh);
        }

        @Override
        public void run() {

            File file = FileSelector.chooseStatsExportFile(datasets, "signals");

            if (file == null) {
                cancel();
                return;
            }

            IAnalysisMethod m = new DatasetSignalsExporter(file, datasets);
            worker = new DefaultAnalysisWorker(m, datasets.size());
            worker.addPropertyChangeListener(this);
            this.setProgressMessage("Exporting stats");
            ThreadManager.getInstance().submit(worker);

        }

    }

}
